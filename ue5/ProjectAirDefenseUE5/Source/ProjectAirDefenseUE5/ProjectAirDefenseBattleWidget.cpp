#include "ProjectAirDefenseBattleWidget.h"

#include "Blueprint/WidgetTree.h"
#include "Components/Border.h"
#include "Components/Button.h"
#include "Components/HorizontalBox.h"
#include "Components/HorizontalBoxSlot.h"
#include "Components/Overlay.h"
#include "Components/OverlaySlot.h"
#include "Components/SafeZone.h"
#include "Components/SizeBox.h"
#include "Components/TextBlock.h"
#include "Components/UniformGridPanel.h"
#include "Components/UniformGridSlot.h"
#include "Components/VerticalBox.h"
#include "Components/VerticalBoxSlot.h"
#include "ProjectAirDefenseBattleManager.h"
#include "ProjectAirDefenseGameUserSettings.h"
#include "ProjectAirDefensePlayerController.h"
#include "ProjectAirDefenseRadarWidget.h"
#include "Styling/AppStyle.h"

namespace {
constexpr float UiMargin = 22.0f;
constexpr float PanelPadding = 18.0f;
constexpr float ButtonGap = 10.0f;
constexpr float DrawerGap = 14.0f;

const FName CameraPanUpAction(TEXT("CameraPanUp"));
const FName CameraPanDownAction(TEXT("CameraPanDown"));
const FName CameraPanLeftAction(TEXT("CameraPanLeft"));
const FName CameraPanRightAction(TEXT("CameraPanRight"));
const FName CameraZoomInAction(TEXT("CameraZoomIn"));
const FName CameraZoomOutAction(TEXT("CameraZoomOut"));
const FName CameraYawMinusAction(TEXT("CameraYawMinus"));
const FName CameraYawPlusAction(TEXT("CameraYawPlus"));
const FName CameraPitchMinusAction(TEXT("CameraPitchMinus"));
const FName CameraPitchPlusAction(TEXT("CameraPitchPlus"));

FSlateFontInfo MakeFont(int32 Size) {
  FSlateFontInfo FontInfo = FAppStyle::GetFontStyle(TEXT("NormalFont"));
  FontInfo.Size = Size;
  return FontInfo;
}

UTextBlock* CreateText(
    UWidgetTree* WidgetTree,
    const FString& Text,
    int32 Size,
    const FLinearColor& Color,
    ETextJustify::Type Justification = ETextJustify::Left) {
  UTextBlock* TextBlock = WidgetTree->ConstructWidget<UTextBlock>();
  TextBlock->SetText(FText::FromString(Text));
  TextBlock->SetFont(MakeFont(Size));
  TextBlock->SetColorAndOpacity(FSlateColor(Color));
  TextBlock->SetJustification(Justification);
  return TextBlock;
}

UBorder* CreateCard(UWidgetTree* WidgetTree, const FLinearColor& FillColor, float Padding = PanelPadding) {
  UBorder* Border = WidgetTree->ConstructWidget<UBorder>();
  Border->SetBrushColor(FillColor);
  Border->SetPadding(FMargin(Padding));
  return Border;
}

UButton* CreateButton(
    UWidgetTree* WidgetTree,
    const FString& Label,
    const FLinearColor& FillColor,
    UTextBlock*& OutLabel,
    int32 FontSize = 20) {
  UButton* Button = WidgetTree->ConstructWidget<UButton>();
  Button->SetBackgroundColor(FillColor);
  Button->SetColorAndOpacity(FLinearColor::White);
  Button->SetClickMethod(EButtonClickMethod::MouseDown);
  Button->IsFocusable = false;

  UBorder* PaddingBorder = WidgetTree->ConstructWidget<UBorder>();
  PaddingBorder->SetBrushColor(FLinearColor::Transparent);
  PaddingBorder->SetPadding(FMargin(14.0f, 12.0f));

  OutLabel = CreateText(WidgetTree, Label, FontSize, FLinearColor::White, ETextJustify::Center);
  PaddingBorder->SetContent(OutLabel);
  Button->AddChild(PaddingBorder);
  return Button;
}

void AddOverlayChild(
    UOverlay* Overlay,
    UWidget* Child,
    EHorizontalAlignment HorizontalAlignment,
    EVerticalAlignment VerticalAlignment,
    const FMargin& Padding) {
  if (UOverlaySlot* Slot = Overlay->AddChildToOverlay(Child)) {
    Slot->SetHorizontalAlignment(HorizontalAlignment);
    Slot->SetVerticalAlignment(VerticalAlignment);
    Slot->SetPadding(Padding);
  }
}

FString AntiAliasingLabel(EProjectAirDefenseAntiAliasingMethod Method) {
  switch (Method) {
  case EProjectAirDefenseAntiAliasingMethod::None:
    return TEXT("AA NONE");
  case EProjectAirDefenseAntiAliasingMethod::FXAA:
    return TEXT("AA FXAA");
  case EProjectAirDefenseAntiAliasingMethod::TAA:
    return TEXT("AA TAA");
  case EProjectAirDefenseAntiAliasingMethod::TSR:
    return TEXT("AA TSR");
  case EProjectAirDefenseAntiAliasingMethod::SMAA:
    return TEXT("AA SMAA");
  }
  return TEXT("AA ?");
}
} // namespace

void UProjectAirDefenseBattleWidget::BindController(AProjectAirDefensePlayerController* InController) {
  this->OwningAirDefenseController = InController;
  if (this->RadarWidget != nullptr) {
    this->RadarWidget->BindController(InController);
  }
  this->RefreshFromRuntime();
}

void UProjectAirDefenseBattleWidget::RefreshFromRuntime() {
  AProjectAirDefensePlayerController* Controller = this->ResolveController();
  AProjectAirDefenseBattleManager* BattleManager =
      Controller == nullptr ? nullptr : Controller->FindBattleManager();
  if (BattleManager == nullptr) {
    return;
  }

  const FProjectAirDefenseRuntimeSnapshot Snapshot = BattleManager->BuildRuntimeSnapshot();
  if (this->LeftPrimaryText != nullptr) {
    const FString LeftPrimary =
        Snapshot.bWaveInProgress
            ? FString::Printf(
                  TEXT("W%d LIVE | %d HOSTILES"),
                  Snapshot.Wave,
                  Snapshot.VisibleThreats + Snapshot.RemainingThreatsInWave)
            : FString::Printf(TEXT("W%d READY"), Snapshot.Wave);
    this->LeftPrimaryText->SetText(FText::FromString(LeftPrimary));
  }
  if (this->LeftSecondaryText != nullptr) {
    this->LeftSecondaryText->SetText(FText::FromString(FString::Printf(
        TEXT("%s | TRACK %d"),
        *ProjectAirDefenseDoctrineLabel(Snapshot.Doctrine),
        Snapshot.TrackedThreats)));
  }
  if (this->RightPrimaryText != nullptr) {
    this->RightPrimaryText->SetText(FText::FromString(FString::Printf(
        TEXT("CITY %d%% | SCORE %d"),
        FMath::RoundToInt(Snapshot.CityIntegrity),
        Snapshot.Score)));
  }
  if (this->RightSecondaryText != nullptr) {
    this->RightSecondaryText->SetText(FText::FromString(FString::Printf(
        TEXT("CR %d | RNG %dm"),
        Snapshot.Credits,
        FMath::RoundToInt(Snapshot.EffectiveRangeMeters))));
  }
  if (this->SystemsButtonText != nullptr) {
    this->SystemsButtonText->SetText(
        FText::FromString(Controller != nullptr && Controller->IsSystemsMenuVisible() ? TEXT("CLOSE") : TEXT("SYSTEMS")));
  }
  if (this->DoctrineButtonText != nullptr) {
    this->DoctrineButtonText->SetText(FText::FromString(ProjectAirDefenseDoctrineLabel(Snapshot.Doctrine)));
  }
  if (this->WaveButtonText != nullptr) {
    this->WaveButtonText->SetText(
        FText::FromString(Snapshot.bWaveInProgress ? TEXT("LIVE") : TEXT("ENGAGE")));
  }
  if (this->TacticsSummaryText != nullptr) {
    this->TacticsSummaryText->SetText(FText::FromString(FString::Printf(
        TEXT("LIVE %d | TRACK %d | CITY %d%%"),
        Snapshot.VisibleThreats,
        Snapshot.TrackedThreats,
        FMath::RoundToInt(Snapshot.CityIntegrity))));
  }
  if (this->TacticsThreatText != nullptr) {
    this->TacticsThreatText->SetText(FText::FromString(FString::Printf(
        TEXT("B %d | G %d | C %d"),
        Snapshot.BallisticThreats,
        Snapshot.GlideThreats,
        Snapshot.CruiseThreats)));
  }
  if (this->GraphicsSummaryText != nullptr) {
    this->GraphicsSummaryText->SetText(FText::FromString(BattleManager->BuildGraphicsSummaryText()));
  }

  if (const UProjectAirDefenseGameUserSettings* Settings =
          UProjectAirDefenseGameUserSettings::GetProjectAirDefenseGameUserSettings()) {
    if (this->AaButtonText != nullptr) {
      this->AaButtonText->SetText(FText::FromString(AntiAliasingLabel(Settings->GetPreferredAntiAliasingMethod())));
    }
    if (this->AoButtonText != nullptr) {
      this->AoButtonText->SetText(FText::FromString(
          Settings->IsAmbientOcclusionEnabled() ? TEXT("AO ON") : TEXT("AO OFF")));
    }
    if (this->BlurButtonText != nullptr) {
      this->BlurButtonText->SetText(FText::FromString(
          Settings->IsMotionBlurEnabled() ? TEXT("BLUR ON") : TEXT("BLUR OFF")));
    }
    if (this->ShadowButtonText != nullptr) {
      this->ShadowButtonText->SetText(FText::FromString(
          FString::Printf(TEXT("SHADOW %d"), Settings->GetShadowQuality())));
    }
    if (this->ReflectionButtonText != nullptr) {
      this->ReflectionButtonText->SetText(FText::FromString(
          FString::Printf(TEXT("REFLECT %d"), Settings->GetReflectionQuality())));
    }
    if (this->PostButtonText != nullptr) {
      this->PostButtonText->SetText(FText::FromString(
          FString::Printf(TEXT("POST %d"), Settings->GetPostProcessingQuality())));
    }
  }

  this->SetDrawerVisibility(Controller != nullptr && Controller->IsSystemsMenuVisible());
}

void UProjectAirDefenseBattleWidget::NativeOnInitialized() {
  Super::NativeOnInitialized();
  if (!this->bTreeBuilt) {
    this->BuildWidgetTree();
  }

  if (UWorld* World = this->GetWorld()) {
    World->GetTimerManager().SetTimer(
        this->RefreshTimerHandle,
        this,
        &UProjectAirDefenseBattleWidget::RefreshFromRuntime,
        0.12f,
        true);
  }
}

void UProjectAirDefenseBattleWidget::NativeDestruct() {
  if (UWorld* World = this->GetWorld()) {
    World->GetTimerManager().ClearTimer(this->RefreshTimerHandle);
    World->GetTimerManager().ClearTimer(this->CameraHoldTimerHandle);
  }
  this->ActiveCameraHoldAction = NAME_None;
  Super::NativeDestruct();
}

void UProjectAirDefenseBattleWidget::HandleSystemsPressed() {
  if (AProjectAirDefensePlayerController* Controller = this->ResolveController()) {
    Controller->SetSystemsMenuVisible(!Controller->IsSystemsMenuVisible());
  }
}

void UProjectAirDefenseBattleWidget::HandleDoctrinePressed() {
  if (AProjectAirDefensePlayerController* Controller = this->ResolveController()) {
    Controller->RequestCycleDoctrine();
  }
}

void UProjectAirDefenseBattleWidget::HandleWavePressed() {
  if (AProjectAirDefensePlayerController* Controller = this->ResolveController()) {
    Controller->RequestStartWave();
  }
}

void UProjectAirDefenseBattleWidget::HandleQualityMinusPressed() {
  if (AProjectAirDefensePlayerController* Controller = this->ResolveController()) {
    Controller->RequestDecreaseQuality();
  }
}

void UProjectAirDefenseBattleWidget::HandleQualityPlusPressed() {
  if (AProjectAirDefensePlayerController* Controller = this->ResolveController()) {
    Controller->RequestIncreaseQuality();
  }
}

void UProjectAirDefenseBattleWidget::HandleAaPressed() {
  if (AProjectAirDefensePlayerController* Controller = this->ResolveController()) {
    Controller->RequestCycleAntiAliasing();
  }
}

void UProjectAirDefenseBattleWidget::HandleAoPressed() {
  if (AProjectAirDefensePlayerController* Controller = this->ResolveController()) {
    Controller->RequestToggleAmbientOcclusion();
  }
}

void UProjectAirDefenseBattleWidget::HandleBlurPressed() {
  if (AProjectAirDefensePlayerController* Controller = this->ResolveController()) {
    Controller->RequestToggleMotionBlur();
  }
}

void UProjectAirDefenseBattleWidget::HandleShadowPressed() {
  if (AProjectAirDefensePlayerController* Controller = this->ResolveController()) {
    Controller->RequestCycleShadowQuality();
  }
}

void UProjectAirDefenseBattleWidget::HandleReflectionPressed() {
  if (AProjectAirDefensePlayerController* Controller = this->ResolveController()) {
    Controller->RequestCycleReflectionQuality();
  }
}

void UProjectAirDefenseBattleWidget::HandlePostPressed() {
  if (AProjectAirDefensePlayerController* Controller = this->ResolveController()) {
    Controller->RequestCyclePostProcessingQuality();
  }
}

void UProjectAirDefenseBattleWidget::HandleCameraUpPressed() {
  this->StartCameraHold(CameraPanUpAction);
}

void UProjectAirDefenseBattleWidget::HandleCameraDownPressed() {
  this->StartCameraHold(CameraPanDownAction);
}

void UProjectAirDefenseBattleWidget::HandleCameraLeftPressed() {
  this->StartCameraHold(CameraPanLeftAction);
}

void UProjectAirDefenseBattleWidget::HandleCameraRightPressed() {
  this->StartCameraHold(CameraPanRightAction);
}

void UProjectAirDefenseBattleWidget::HandleCameraYawMinusPressed() {
  this->StartCameraHold(CameraYawMinusAction);
}

void UProjectAirDefenseBattleWidget::HandleCameraYawPlusPressed() {
  this->StartCameraHold(CameraYawPlusAction);
}

void UProjectAirDefenseBattleWidget::HandleCameraPitchMinusPressed() {
  this->StartCameraHold(CameraPitchMinusAction);
}

void UProjectAirDefenseBattleWidget::HandleCameraPitchPlusPressed() {
  this->StartCameraHold(CameraPitchPlusAction);
}

void UProjectAirDefenseBattleWidget::HandleCameraZoomMinusPressed() {
  this->StartCameraHold(CameraZoomOutAction);
}

void UProjectAirDefenseBattleWidget::HandleCameraZoomPlusPressed() {
  this->StartCameraHold(CameraZoomInAction);
}

void UProjectAirDefenseBattleWidget::HandleCameraResetPressed() {
  if (AProjectAirDefensePlayerController* Controller = this->ResolveController()) {
    Controller->RequestCameraReset();
  }
}

void UProjectAirDefenseBattleWidget::HandleCameraHoldReleased() {
  this->StopCameraHold();
}

void UProjectAirDefenseBattleWidget::StartCameraHold(const FName& ActionName) {
  AProjectAirDefensePlayerController* Controller = this->ResolveController();
  UWorld* World = this->GetWorld();
  if (Controller == nullptr || World == nullptr) {
    return;
  }

  this->ActiveCameraHoldAction = ActionName;
  this->StepCameraHoldAction(ActionName);

  FTimerManager& TimerManager = World->GetTimerManager();
  TimerManager.ClearTimer(this->CameraHoldTimerHandle);
  FTimerDelegate RepeatDelegate;
  RepeatDelegate.BindWeakLambda(this, [this]() {
    if (this->ActiveCameraHoldAction == NAME_None) {
      return;
    }
    this->StepCameraHoldAction(this->ActiveCameraHoldAction);
  });
  TimerManager.SetTimer(
      this->CameraHoldTimerHandle,
      RepeatDelegate,
      0.12f,
      true,
      0.24f);
}

void UProjectAirDefenseBattleWidget::StopCameraHold() {
  this->ActiveCameraHoldAction = NAME_None;
  if (UWorld* World = this->GetWorld()) {
    World->GetTimerManager().ClearTimer(this->CameraHoldTimerHandle);
  }
}

void UProjectAirDefenseBattleWidget::StepCameraHoldAction(const FName& ActionName) {
  if (AProjectAirDefensePlayerController* Controller = this->ResolveController()) {
    if (ActionName == CameraPanUpAction) {
      Controller->RequestCameraPan(220.0f, 0.0f);
    } else if (ActionName == CameraPanDownAction) {
      Controller->RequestCameraPan(-220.0f, 0.0f);
    } else if (ActionName == CameraPanLeftAction) {
      Controller->RequestCameraPan(0.0f, -220.0f);
    } else if (ActionName == CameraPanRightAction) {
      Controller->RequestCameraPan(0.0f, 220.0f);
    } else if (ActionName == CameraZoomInAction) {
      Controller->RequestCameraZoom(-260.0f);
    } else if (ActionName == CameraZoomOutAction) {
      Controller->RequestCameraZoom(260.0f);
    } else if (ActionName == CameraYawMinusAction) {
      Controller->RequestCameraYaw(-7.5f);
    } else if (ActionName == CameraYawPlusAction) {
      Controller->RequestCameraYaw(7.5f);
    } else if (ActionName == CameraPitchMinusAction) {
      Controller->RequestCameraPitch(-4.0f);
    } else if (ActionName == CameraPitchPlusAction) {
      Controller->RequestCameraPitch(4.0f);
    }
  }
}

void UProjectAirDefenseBattleWidget::BuildWidgetTree() {
  if (this->WidgetTree == nullptr) {
    return;
  }

  this->bTreeBuilt = true;

  USafeZone* SafeZone = this->WidgetTree->ConstructWidget<USafeZone>();
  UOverlay* RootOverlay = this->WidgetTree->ConstructWidget<UOverlay>();
  SafeZone->SetContent(RootOverlay);
  this->WidgetTree->RootWidget = SafeZone;

  UBorder* LeftCard = CreateCard(this->WidgetTree, FLinearColor(0.03f, 0.07f, 0.11f, 0.82f));
  UVerticalBox* LeftStack = this->WidgetTree->ConstructWidget<UVerticalBox>();
  LeftCard->SetContent(LeftStack);
  this->LeftPrimaryText =
      CreateText(this->WidgetTree, TEXT("W1 READY"), 23, FLinearColor(0.97f, 0.99f, 1.0f, 1.0f));
  this->LeftSecondaryText =
      CreateText(this->WidgetTree, TEXT("SHIELD WALL | TRACK 0"), 18, FLinearColor(0.72f, 0.91f, 1.0f, 1.0f));
  LeftStack->AddChildToVerticalBox(this->LeftPrimaryText);
  LeftStack->AddChildToVerticalBox(this->LeftSecondaryText);
  AddOverlayChild(
      RootOverlay,
      LeftCard,
      HAlign_Left,
      VAlign_Top,
      FMargin(UiMargin, UiMargin, 0.0f, 0.0f));

  UBorder* RightCard = CreateCard(this->WidgetTree, FLinearColor(0.03f, 0.07f, 0.11f, 0.82f));
  UVerticalBox* RightStack = this->WidgetTree->ConstructWidget<UVerticalBox>();
  RightCard->SetContent(RightStack);
  this->RightPrimaryText =
      CreateText(this->WidgetTree, TEXT("CITY 100% | SCORE 0"), 23, FLinearColor(0.97f, 0.99f, 1.0f, 1.0f));
  this->RightSecondaryText =
      CreateText(this->WidgetTree, TEXT("CR 10000 | RNG 0m"), 18, FLinearColor(0.98f, 0.82f, 0.46f, 1.0f));
  RightStack->AddChildToVerticalBox(this->RightPrimaryText);
  RightStack->AddChildToVerticalBox(this->RightSecondaryText);
  AddOverlayChild(
      RootOverlay,
      RightCard,
      HAlign_Right,
      VAlign_Top,
      FMargin(0.0f, UiMargin, UiMargin, 0.0f));

  USizeBox* RadarBox = this->WidgetTree->ConstructWidget<USizeBox>();
  RadarBox->SetWidthOverride(184.0f);
  RadarBox->SetHeightOverride(184.0f);
  this->RadarWidget = this->WidgetTree->ConstructWidget<UProjectAirDefenseRadarWidget>();
  RadarBox->AddChild(this->RadarWidget);
  AddOverlayChild(
      RootOverlay,
      RadarBox,
      HAlign_Left,
      VAlign_Bottom,
      FMargin(UiMargin, 0.0f, 0.0f, 110.0f));

  UTextBlock* SystemsLabel = nullptr;
  UButton* SystemsButton =
      CreateButton(this->WidgetTree, TEXT("SYSTEMS"), FLinearColor(0.08f, 0.15f, 0.22f, 0.96f), SystemsLabel, 22);
  SystemsButton->OnClicked.AddDynamic(this, &UProjectAirDefenseBattleWidget::HandleSystemsPressed);
  this->SystemsButtonText = SystemsLabel;
  AddOverlayChild(
      RootOverlay,
      SystemsButton,
      HAlign_Left,
      VAlign_Bottom,
      FMargin(UiMargin, 0.0f, 0.0f, UiMargin));

  UHorizontalBox* BottomActions = this->WidgetTree->ConstructWidget<UHorizontalBox>();
  AddOverlayChild(
      RootOverlay,
      BottomActions,
      HAlign_Right,
      VAlign_Bottom,
      FMargin(0.0f, 0.0f, UiMargin, UiMargin));

  UTextBlock* DoctrineLabel = nullptr;
  UButton* DoctrineButton =
      CreateButton(this->WidgetTree, TEXT("SHIELD WALL"), FLinearColor(0.11f, 0.10f, 0.18f, 0.96f), DoctrineLabel, 22);
  DoctrineButton->OnClicked.AddDynamic(this, &UProjectAirDefenseBattleWidget::HandleDoctrinePressed);
  this->DoctrineButtonText = DoctrineLabel;
  if (UHorizontalBoxSlot* HorizontalSlot = BottomActions->AddChildToHorizontalBox(DoctrineButton)) {
    HorizontalSlot->SetPadding(FMargin(0.0f, 0.0f, ButtonGap, 0.0f));
  }

  UTextBlock* WaveLabel = nullptr;
  UButton* WaveButton =
      CreateButton(this->WidgetTree, TEXT("ENGAGE"), FLinearColor(0.17f, 0.16f, 0.08f, 0.98f), WaveLabel, 22);
  WaveButton->OnClicked.AddDynamic(this, &UProjectAirDefenseBattleWidget::HandleWavePressed);
  this->WaveButtonText = WaveLabel;
  BottomActions->AddChildToHorizontalBox(WaveButton);

  UBorder* DrawerCard = CreateCard(this->WidgetTree, FLinearColor(0.03f, 0.07f, 0.11f, 0.88f), 20.0f);
  AddOverlayChild(
      RootOverlay,
      DrawerCard,
      HAlign_Center,
      VAlign_Bottom,
      FMargin(34.0f, 0.0f, 34.0f, 108.0f));
  this->SystemsDrawer = this->WidgetTree->ConstructWidget<UVerticalBox>();
  DrawerCard->SetContent(this->SystemsDrawer);

  UHorizontalBox* DrawerColumns = this->WidgetTree->ConstructWidget<UHorizontalBox>();
  this->SystemsDrawer->AddChildToVerticalBox(DrawerColumns);

  auto BuildDrawerPanel = [this, DrawerColumns](const FString& Title, const FLinearColor& Accent) -> UVerticalBox* {
    UBorder* Panel = CreateCard(this->WidgetTree, FLinearColor(0.05f, 0.09f, 0.14f, 0.94f), 16.0f);
    if (UHorizontalBoxSlot* Slot = DrawerColumns->AddChildToHorizontalBox(Panel)) {
      Slot->SetPadding(FMargin(0.0f, 0.0f, DrawerGap, 0.0f));
      Slot->SetSize(ESlateSizeRule::Fill);
    }
    UVerticalBox* Stack = this->WidgetTree->ConstructWidget<UVerticalBox>();
    Panel->SetContent(Stack);
    UTextBlock* Heading = CreateText(this->WidgetTree, Title, 22, Accent);
    if (UVerticalBoxSlot* HeadingSlot = Stack->AddChildToVerticalBox(Heading)) {
      HeadingSlot->SetPadding(FMargin(0.0f, 0.0f, 0.0f, 12.0f));
    }
    return Stack;
  };

  UVerticalBox* TacticsPanel = BuildDrawerPanel(TEXT("TACTICS"), FLinearColor(0.98f, 0.82f, 0.46f, 1.0f));
  this->TacticsSummaryText =
      CreateText(this->WidgetTree, TEXT("LIVE 0 | TRACK 0 | CITY 100%"), 19, FLinearColor(0.94f, 0.97f, 1.0f, 1.0f));
  this->TacticsThreatText =
      CreateText(this->WidgetTree, TEXT("B 0 | G 0 | C 0"), 18, FLinearColor(0.74f, 0.90f, 1.0f, 1.0f));
  if (UVerticalBoxSlot* VerticalSlot = TacticsPanel->AddChildToVerticalBox(this->TacticsSummaryText)) {
    VerticalSlot->SetPadding(FMargin(0.0f, 0.0f, 0.0f, 8.0f));
  }
  if (UVerticalBoxSlot* VerticalSlot = TacticsPanel->AddChildToVerticalBox(this->TacticsThreatText)) {
    VerticalSlot->SetPadding(FMargin(0.0f, 0.0f, 0.0f, 12.0f));
  }
  UButton* DrawerWaveButton =
      CreateButton(this->WidgetTree, TEXT("ENGAGE"), FLinearColor(0.17f, 0.16f, 0.08f, 0.98f), WaveLabel, 20);
  DrawerWaveButton->OnClicked.AddDynamic(this, &UProjectAirDefenseBattleWidget::HandleWavePressed);
  if (UVerticalBoxSlot* VerticalSlot = TacticsPanel->AddChildToVerticalBox(DrawerWaveButton)) {
    VerticalSlot->SetPadding(FMargin(0.0f, 0.0f, 0.0f, ButtonGap));
  }
  UButton* DrawerDoctrineButton =
      CreateButton(this->WidgetTree, TEXT("SHIELD WALL"), FLinearColor(0.11f, 0.10f, 0.18f, 0.96f), DoctrineLabel, 20);
  DrawerDoctrineButton->OnClicked.AddDynamic(this, &UProjectAirDefenseBattleWidget::HandleDoctrinePressed);
  TacticsPanel->AddChildToVerticalBox(DrawerDoctrineButton);

  UVerticalBox* CameraPanel = BuildDrawerPanel(TEXT("VIEW"), FLinearColor(0.26f, 0.86f, 0.98f, 1.0f));
  UUniformGridPanel* CameraGrid = this->WidgetTree->ConstructWidget<UUniformGridPanel>();
  CameraGrid->SetMinDesiredSlotWidth(118.0f);
  CameraGrid->SetMinDesiredSlotHeight(50.0f);
  CameraPanel->AddChildToVerticalBox(CameraGrid);

  auto AddGridButton = [this](UUniformGridPanel* Grid, int32 Row, int32 Column, const FString& Label, const FLinearColor& Color, UTextBlock*& LabelText) -> UButton* {
    UButton* Button = CreateButton(this->WidgetTree, Label, Color, LabelText, 18);
    if (UUniformGridSlot* Slot = Grid->AddChildToUniformGrid(Button, Row, Column)) {
      Slot->SetHorizontalAlignment(HAlign_Fill);
      Slot->SetVerticalAlignment(VAlign_Fill);
    }
    return Button;
  };

  UTextBlock* TempLabel = nullptr;
  UButton* PanUpButton = AddGridButton(CameraGrid, 0, 0, TEXT("UP"), FLinearColor(0.08f, 0.15f, 0.22f, 0.96f), TempLabel);
  PanUpButton->OnPressed.AddDynamic(this, &UProjectAirDefenseBattleWidget::HandleCameraUpPressed);
  PanUpButton->OnReleased.AddDynamic(this, &UProjectAirDefenseBattleWidget::HandleCameraHoldReleased);
  UButton* PanDownButton = AddGridButton(CameraGrid, 0, 1, TEXT("DOWN"), FLinearColor(0.08f, 0.15f, 0.22f, 0.96f), TempLabel);
  PanDownButton->OnPressed.AddDynamic(this, &UProjectAirDefenseBattleWidget::HandleCameraDownPressed);
  PanDownButton->OnReleased.AddDynamic(this, &UProjectAirDefenseBattleWidget::HandleCameraHoldReleased);
  UButton* PanLeftButton = AddGridButton(CameraGrid, 1, 0, TEXT("LEFT"), FLinearColor(0.08f, 0.15f, 0.22f, 0.96f), TempLabel);
  PanLeftButton->OnPressed.AddDynamic(this, &UProjectAirDefenseBattleWidget::HandleCameraLeftPressed);
  PanLeftButton->OnReleased.AddDynamic(this, &UProjectAirDefenseBattleWidget::HandleCameraHoldReleased);
  UButton* PanRightButton = AddGridButton(CameraGrid, 1, 1, TEXT("RIGHT"), FLinearColor(0.08f, 0.15f, 0.22f, 0.96f), TempLabel);
  PanRightButton->OnPressed.AddDynamic(this, &UProjectAirDefenseBattleWidget::HandleCameraRightPressed);
  PanRightButton->OnReleased.AddDynamic(this, &UProjectAirDefenseBattleWidget::HandleCameraHoldReleased);
  UButton* ZoomOutButton = AddGridButton(CameraGrid, 2, 0, TEXT("ZOOM OUT"), FLinearColor(0.08f, 0.15f, 0.22f, 0.96f), TempLabel);
  ZoomOutButton->OnPressed.AddDynamic(this, &UProjectAirDefenseBattleWidget::HandleCameraZoomMinusPressed);
  ZoomOutButton->OnReleased.AddDynamic(this, &UProjectAirDefenseBattleWidget::HandleCameraHoldReleased);
  UButton* ZoomInButton = AddGridButton(CameraGrid, 2, 1, TEXT("ZOOM IN"), FLinearColor(0.08f, 0.15f, 0.22f, 0.96f), TempLabel);
  ZoomInButton->OnPressed.AddDynamic(this, &UProjectAirDefenseBattleWidget::HandleCameraZoomPlusPressed);
  ZoomInButton->OnReleased.AddDynamic(this, &UProjectAirDefenseBattleWidget::HandleCameraHoldReleased);
  UButton* TurnLeftButton = AddGridButton(CameraGrid, 3, 0, TEXT("TURN L"), FLinearColor(0.08f, 0.15f, 0.22f, 0.96f), TempLabel);
  TurnLeftButton->OnPressed.AddDynamic(this, &UProjectAirDefenseBattleWidget::HandleCameraYawMinusPressed);
  TurnLeftButton->OnReleased.AddDynamic(this, &UProjectAirDefenseBattleWidget::HandleCameraHoldReleased);
  UButton* TurnRightButton = AddGridButton(CameraGrid, 3, 1, TEXT("TURN R"), FLinearColor(0.08f, 0.15f, 0.22f, 0.96f), TempLabel);
  TurnRightButton->OnPressed.AddDynamic(this, &UProjectAirDefenseBattleWidget::HandleCameraYawPlusPressed);
  TurnRightButton->OnReleased.AddDynamic(this, &UProjectAirDefenseBattleWidget::HandleCameraHoldReleased);
  UButton* TiltDownButton = AddGridButton(CameraGrid, 4, 0, TEXT("LOOK DN"), FLinearColor(0.08f, 0.15f, 0.22f, 0.96f), TempLabel);
  TiltDownButton->OnPressed.AddDynamic(this, &UProjectAirDefenseBattleWidget::HandleCameraPitchMinusPressed);
  TiltDownButton->OnReleased.AddDynamic(this, &UProjectAirDefenseBattleWidget::HandleCameraHoldReleased);
  UButton* TiltUpButton = AddGridButton(CameraGrid, 4, 1, TEXT("LOOK UP"), FLinearColor(0.08f, 0.15f, 0.22f, 0.96f), TempLabel);
  TiltUpButton->OnPressed.AddDynamic(this, &UProjectAirDefenseBattleWidget::HandleCameraPitchPlusPressed);
  TiltUpButton->OnReleased.AddDynamic(this, &UProjectAirDefenseBattleWidget::HandleCameraHoldReleased);
  AddGridButton(CameraGrid, 5, 0, TEXT("RESET"), FLinearColor(0.08f, 0.15f, 0.22f, 0.96f), TempLabel)->OnClicked.AddDynamic(this, &UProjectAirDefenseBattleWidget::HandleCameraResetPressed);

  UVerticalBox* GraphicsPanel = BuildDrawerPanel(TEXT("GRAPHICS"), FLinearColor(0.79f, 0.63f, 0.99f, 1.0f));
  this->GraphicsSummaryText =
      CreateText(this->WidgetTree, TEXT("AUTO  AA ?  AO ?"), 19, FLinearColor(0.94f, 0.97f, 1.0f, 1.0f));
  if (UVerticalBoxSlot* VerticalSlot = GraphicsPanel->AddChildToVerticalBox(this->GraphicsSummaryText)) {
    VerticalSlot->SetPadding(FMargin(0.0f, 0.0f, 0.0f, 12.0f));
  }
  UUniformGridPanel* GraphicsGrid = this->WidgetTree->ConstructWidget<UUniformGridPanel>();
  GraphicsGrid->SetMinDesiredSlotWidth(108.0f);
  GraphicsGrid->SetMinDesiredSlotHeight(48.0f);
  GraphicsPanel->AddChildToVerticalBox(GraphicsGrid);

  UTextBlock* QualityMinusLabel = nullptr;
  UButton* QualityMinus = CreateButton(this->WidgetTree, TEXT("QUALITY -"), FLinearColor(0.11f, 0.10f, 0.18f, 0.96f), QualityMinusLabel, 18);
  QualityMinus->OnClicked.AddDynamic(this, &UProjectAirDefenseBattleWidget::HandleQualityMinusPressed);
  GraphicsGrid->AddChildToUniformGrid(QualityMinus, 0, 0);
  UTextBlock* QualityPlusLabel = nullptr;
  UButton* QualityPlus = CreateButton(this->WidgetTree, TEXT("QUALITY +"), FLinearColor(0.11f, 0.10f, 0.18f, 0.96f), QualityPlusLabel, 18);
  QualityPlus->OnClicked.AddDynamic(this, &UProjectAirDefenseBattleWidget::HandleQualityPlusPressed);
  GraphicsGrid->AddChildToUniformGrid(QualityPlus, 0, 1);

  UTextBlock* AaLabel = nullptr;
  UButton* AaButton = CreateButton(this->WidgetTree, TEXT("AA TSR"), FLinearColor(0.11f, 0.10f, 0.18f, 0.96f), AaLabel, 18);
  this->AaButtonText = AaLabel;
  AaButton->OnClicked.AddDynamic(this, &UProjectAirDefenseBattleWidget::HandleAaPressed);
  GraphicsGrid->AddChildToUniformGrid(AaButton, 1, 0);
  UTextBlock* AoLabel = nullptr;
  UButton* AoButton = CreateButton(this->WidgetTree, TEXT("AO ON"), FLinearColor(0.11f, 0.10f, 0.18f, 0.96f), AoLabel, 18);
  this->AoButtonText = AoLabel;
  AoButton->OnClicked.AddDynamic(this, &UProjectAirDefenseBattleWidget::HandleAoPressed);
  GraphicsGrid->AddChildToUniformGrid(AoButton, 1, 1);
  UTextBlock* BlurLabel = nullptr;
  UButton* BlurButton = CreateButton(this->WidgetTree, TEXT("BLUR OFF"), FLinearColor(0.11f, 0.10f, 0.18f, 0.96f), BlurLabel, 18);
  this->BlurButtonText = BlurLabel;
  BlurButton->OnClicked.AddDynamic(this, &UProjectAirDefenseBattleWidget::HandleBlurPressed);
  GraphicsGrid->AddChildToUniformGrid(BlurButton, 2, 0);
  UTextBlock* ShadowLabel = nullptr;
  UButton* ShadowButton = CreateButton(this->WidgetTree, TEXT("SHADOW 0"), FLinearColor(0.11f, 0.10f, 0.18f, 0.96f), ShadowLabel, 18);
  this->ShadowButtonText = ShadowLabel;
  ShadowButton->OnClicked.AddDynamic(this, &UProjectAirDefenseBattleWidget::HandleShadowPressed);
  GraphicsGrid->AddChildToUniformGrid(ShadowButton, 2, 1);
  UTextBlock* ReflectionLabel = nullptr;
  UButton* ReflectionButton = CreateButton(this->WidgetTree, TEXT("REFLECT 0"), FLinearColor(0.11f, 0.10f, 0.18f, 0.96f), ReflectionLabel, 18);
  this->ReflectionButtonText = ReflectionLabel;
  ReflectionButton->OnClicked.AddDynamic(this, &UProjectAirDefenseBattleWidget::HandleReflectionPressed);
  GraphicsGrid->AddChildToUniformGrid(ReflectionButton, 3, 0);
  UTextBlock* PostLabel = nullptr;
  UButton* PostButton = CreateButton(this->WidgetTree, TEXT("POST 0"), FLinearColor(0.11f, 0.10f, 0.18f, 0.96f), PostLabel, 18);
  this->PostButtonText = PostLabel;
  PostButton->OnClicked.AddDynamic(this, &UProjectAirDefenseBattleWidget::HandlePostPressed);
  GraphicsGrid->AddChildToUniformGrid(PostButton, 3, 1);

  this->SetDrawerVisibility(false);
}

AProjectAirDefensePlayerController* UProjectAirDefenseBattleWidget::ResolveController() const {
  if (this->OwningAirDefenseController.IsValid()) {
    return this->OwningAirDefenseController.Get();
  }
  return Cast<AProjectAirDefensePlayerController>(this->GetOwningPlayer());
}

void UProjectAirDefenseBattleWidget::SetDrawerVisibility(bool bVisible) {
  if (this->SystemsDrawer == nullptr) {
    return;
  }
  this->SystemsDrawer->SetVisibility(bVisible ? ESlateVisibility::Visible : ESlateVisibility::Collapsed);
}
