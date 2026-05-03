#include "ProjectAirDefenseBattleWidget.h"

#include "Blueprint/WidgetTree.h"
#include "Components/Border.h"
#include "Components/Button.h"
#include "Components/CheckBox.h"
#include "Components/HorizontalBox.h"
#include "Components/HorizontalBoxSlot.h"
#include "Components/Overlay.h"
#include "Components/OverlaySlot.h"
#include "Components/SafeZone.h"
#include "Components/SizeBox.h"
#include "Components/Slider.h"
#include "Components/TextBlock.h"
#include "Components/UniformGridPanel.h"
#include "Components/UniformGridSlot.h"
#include "Components/VerticalBox.h"
#include "Components/VerticalBoxSlot.h"
#include "Brushes/SlateRoundedBoxBrush.h"
#include "ProjectAirDefenseBattleManager.h"
#include "ProjectAirDefenseGameUserSettings.h"
#include "ProjectAirDefensePlayerController.h"
#include "ProjectAirDefenseRadarWidget.h"
#include "ProjectAirDefenseTouchButton.h"
#include "Styling/AppStyle.h"

namespace {
constexpr float UiMargin = 22.0f;
constexpr float PanelPadding = 18.0f;
constexpr float ButtonGap = 10.0f;
constexpr float DrawerGap = 14.0f;
constexpr float CardCornerRadius = 14.0f;
constexpr float ButtonCornerRadius = 12.0f;

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
  Border->SetBrush(FSlateRoundedBoxBrush(
      FillColor,
      CardCornerRadius,
      FLinearColor(0.64f, 0.76f, 0.84f, 0.22f),
      1.0f));
  Border->SetPadding(FMargin(Padding));
  return Border;
}

UButton* CreateButton(
    UWidgetTree* WidgetTree,
    const FString& Label,
    const FLinearColor& FillColor,
    UTextBlock*& OutLabel,
    int32 FontSize = 20) {
  UButton* Button = WidgetTree->ConstructWidget<UProjectAirDefenseTouchButton>();
  const FLinearColor HoverColor(
      FMath::Min(FillColor.R + 0.05f, 1.0f),
      FMath::Min(FillColor.G + 0.06f, 1.0f),
      FMath::Min(FillColor.B + 0.07f, 1.0f),
      FillColor.A);
  const FLinearColor PressedColor(
      FMath::Max(FillColor.R - 0.035f, 0.0f),
      FMath::Max(FillColor.G - 0.035f, 0.0f),
      FMath::Max(FillColor.B - 0.035f, 0.0f),
      FillColor.A);
  FButtonStyle ButtonStyle;
  ButtonStyle.SetNormal(FSlateRoundedBoxBrush(
      FillColor,
      ButtonCornerRadius,
      FLinearColor(0.62f, 0.72f, 0.80f, 0.40f),
      1.0f));
  ButtonStyle.SetHovered(FSlateRoundedBoxBrush(
      HoverColor,
      ButtonCornerRadius,
      FLinearColor(0.78f, 0.90f, 1.0f, 0.64f),
      1.5f));
  ButtonStyle.SetPressed(FSlateRoundedBoxBrush(
      PressedColor,
      ButtonCornerRadius,
      FLinearColor(0.48f, 0.64f, 0.75f, 0.60f),
      1.0f));
  ButtonStyle.SetNormalPadding(FMargin(0.0f));
  ButtonStyle.SetPressedPadding(FMargin(0.0f));
  Button->SetStyle(ButtonStyle);
  Button->SetColorAndOpacity(FLinearColor::White);
  Button->SetClickMethod(EButtonClickMethod::MouseDown);

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

FString AntiAliasingShortLabel(EProjectAirDefenseAntiAliasingMethod Method) {
  return AntiAliasingLabel(Method).Replace(TEXT("AA "), TEXT(""));
}

EProjectAirDefenseAntiAliasingMethod AntiAliasingMethodFromSlider(float Value) {
  const int32 Index = FMath::Clamp(FMath::RoundToInt(Value), 0, 4);
  switch (Index) {
  case 0:
    return EProjectAirDefenseAntiAliasingMethod::None;
  case 1:
    return EProjectAirDefenseAntiAliasingMethod::FXAA;
  case 2:
    return EProjectAirDefenseAntiAliasingMethod::TAA;
  case 3:
    return EProjectAirDefenseAntiAliasingMethod::TSR;
  case 4:
    return EProjectAirDefenseAntiAliasingMethod::SMAA;
  }
  return EProjectAirDefenseAntiAliasingMethod::TSR;
}

float SliderValueFromAntiAliasingMethod(EProjectAirDefenseAntiAliasingMethod Method) {
  switch (Method) {
  case EProjectAirDefenseAntiAliasingMethod::None:
    return 0.0f;
  case EProjectAirDefenseAntiAliasingMethod::FXAA:
    return 1.0f;
  case EProjectAirDefenseAntiAliasingMethod::TAA:
    return 2.0f;
  case EProjectAirDefenseAntiAliasingMethod::TSR:
    return 3.0f;
  case EProjectAirDefenseAntiAliasingMethod::SMAA:
    return 4.0f;
  }
  return 3.0f;
}

int32 SliderQualityLevel(float Value) {
  return FMath::Clamp(FMath::RoundToInt(Value), 0, 4);
}

FString QualityLevelLabel(int32 QualityLevel) {
  switch (FMath::Clamp(QualityLevel, 0, 4)) {
  case 0:
    return TEXT("LOW");
  case 1:
    return TEXT("MED");
  case 2:
    return TEXT("HIGH");
  case 3:
    return TEXT("EPIC");
  case 4:
    return TEXT("CINE");
  }
  return TEXT("AUTO");
}

FString SwitchLabel(bool bEnabled) {
  return bEnabled ? TEXT("ON") : TEXT("OFF");
}

FString SolarTimeLabel(double SolarTimeHours) {
  const int32 TotalMinutes = FMath::RoundToInt(FMath::Fmod(SolarTimeHours + 24.0, 24.0) * 60.0) % (24 * 60);
  return FString::Printf(TEXT("%02d:%02d"), TotalMinutes / 60, TotalMinutes % 60);
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
  if (this->TimeSummaryText != nullptr && Controller != nullptr) {
    this->TimeSummaryText->SetText(FText::FromString(Controller->BuildTimeSummaryText()));
  }

  const bool bWasRefreshingControls = this->bRefreshingControls;
  this->bRefreshingControls = true;

  if (Controller != nullptr) {
    const double SolarTimeHours = Controller->GetSolarTimeHours();
    const double TimeScaleHoursPerMinute = Controller->GetTimeScaleHoursPerMinute();
    const double MaxTimeScaleHoursPerMinute = Controller->GetTimeScaleMaxHoursPerMinute();
    if (this->TimeOfDaySlider != nullptr) {
      this->TimeOfDaySlider->SetValue(static_cast<float>(SolarTimeHours));
    }
    if (this->TimeOfDayValueText != nullptr) {
      this->TimeOfDayValueText->SetText(FText::FromString(SolarTimeLabel(SolarTimeHours)));
    }
    if (this->TimeScaleSlider != nullptr) {
      this->TimeScaleSlider->SetMaxValue(static_cast<float>(MaxTimeScaleHoursPerMinute));
      this->TimeScaleSlider->SetValue(static_cast<float>(TimeScaleHoursPerMinute));
    }
    if (this->TimeScaleValueText != nullptr) {
      this->TimeScaleValueText->SetText(FText::FromString(FString::Printf(TEXT("%.1fh/min"), TimeScaleHoursPerMinute)));
    }
    if (this->TimeCycleSwitch != nullptr) {
      this->TimeCycleSwitch->SetIsChecked(!FMath::IsNearlyZero(TimeScaleHoursPerMinute));
    }
    if (this->TimeCycleValueText != nullptr) {
      this->TimeCycleValueText->SetText(FText::FromString(
          FMath::IsNearlyZero(TimeScaleHoursPerMinute) ? TEXT("PAUSED") : TEXT("RUNNING")));
    }
  }

  if (const UProjectAirDefenseGameUserSettings* Settings =
          UProjectAirDefenseGameUserSettings::GetProjectAirDefenseGameUserSettings()) {
    const int32 OverallQuality =
        Settings->GetOverallScalabilityLevel() < 0 ? 3 : Settings->GetOverallScalabilityLevel();
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
    if (this->RayTracingButtonText != nullptr) {
      this->RayTracingButtonText->SetText(FText::FromString(
          Settings->IsRayTracingEnabled() ? TEXT("RT REQ") : TEXT("RT OFF")));
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
    if (this->OverallQualitySlider != nullptr) {
      this->OverallQualitySlider->SetValue(static_cast<float>(OverallQuality));
    }
    if (this->QualityValueText != nullptr) {
      this->QualityValueText->SetText(FText::FromString(QualityLevelLabel(OverallQuality)));
    }
    if (this->AaSlider != nullptr) {
      this->AaSlider->SetValue(SliderValueFromAntiAliasingMethod(Settings->GetPreferredAntiAliasingMethod()));
    }
    if (this->AaValueText != nullptr) {
      this->AaValueText->SetText(FText::FromString(AntiAliasingShortLabel(Settings->GetPreferredAntiAliasingMethod())));
    }
    if (this->AoSwitch != nullptr) {
      this->AoSwitch->SetIsChecked(Settings->IsAmbientOcclusionEnabled());
    }
    if (this->AoValueText != nullptr) {
      this->AoValueText->SetText(FText::FromString(SwitchLabel(Settings->IsAmbientOcclusionEnabled())));
    }
    if (this->BlurSwitch != nullptr) {
      this->BlurSwitch->SetIsChecked(Settings->IsMotionBlurEnabled());
    }
    if (this->BlurValueText != nullptr) {
      this->BlurValueText->SetText(FText::FromString(SwitchLabel(Settings->IsMotionBlurEnabled())));
    }
    if (this->RayTracingSwitch != nullptr) {
      this->RayTracingSwitch->SetIsChecked(Settings->IsRayTracingEnabled());
    }
    if (this->RayTracingValueText != nullptr) {
      this->RayTracingValueText->SetText(FText::FromString(Settings->IsRayTracingEnabled() ? TEXT("REQUESTED") : TEXT("OFF")));
    }
    if (this->ShadowSlider != nullptr) {
      this->ShadowSlider->SetValue(static_cast<float>(Settings->GetShadowQuality()));
    }
    if (this->ShadowValueText != nullptr) {
      this->ShadowValueText->SetText(FText::FromString(QualityLevelLabel(Settings->GetShadowQuality())));
    }
    if (this->ReflectionSlider != nullptr) {
      this->ReflectionSlider->SetValue(static_cast<float>(Settings->GetReflectionQuality()));
    }
    if (this->ReflectionValueText != nullptr) {
      this->ReflectionValueText->SetText(FText::FromString(QualityLevelLabel(Settings->GetReflectionQuality())));
    }
    if (this->PostSlider != nullptr) {
      this->PostSlider->SetValue(static_cast<float>(Settings->GetPostProcessingQuality()));
    }
    if (this->PostValueText != nullptr) {
      this->PostValueText->SetText(FText::FromString(QualityLevelLabel(Settings->GetPostProcessingQuality())));
    }
  }
  this->bRefreshingControls = bWasRefreshingControls;

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

void UProjectAirDefenseBattleWidget::HandleRayTracingPressed() {
  if (AProjectAirDefensePlayerController* Controller = this->ResolveController()) {
    Controller->RequestToggleRayTracing();
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

void UProjectAirDefenseBattleWidget::HandleOverallQualitySliderChanged(float Value) {
  if (this->bRefreshingControls) {
    return;
  }
  if (AProjectAirDefensePlayerController* Controller = this->ResolveController()) {
    Controller->RequestSetOverallQualityLevel(SliderQualityLevel(Value));
    this->RefreshFromRuntime();
  }
}

void UProjectAirDefenseBattleWidget::HandleAaSliderChanged(float Value) {
  if (this->bRefreshingControls) {
    return;
  }
  if (AProjectAirDefensePlayerController* Controller = this->ResolveController()) {
    Controller->RequestSetAntiAliasingMethod(AntiAliasingMethodFromSlider(Value));
    this->RefreshFromRuntime();
  }
}

void UProjectAirDefenseBattleWidget::HandleAoSwitchChanged(bool bIsChecked) {
  if (this->bRefreshingControls) {
    return;
  }
  if (AProjectAirDefensePlayerController* Controller = this->ResolveController()) {
    Controller->RequestSetAmbientOcclusionEnabled(bIsChecked);
    this->RefreshFromRuntime();
  }
}

void UProjectAirDefenseBattleWidget::HandleBlurSwitchChanged(bool bIsChecked) {
  if (this->bRefreshingControls) {
    return;
  }
  if (AProjectAirDefensePlayerController* Controller = this->ResolveController()) {
    Controller->RequestSetMotionBlurEnabled(bIsChecked);
    this->RefreshFromRuntime();
  }
}

void UProjectAirDefenseBattleWidget::HandleRayTracingSwitchChanged(bool bIsChecked) {
  if (this->bRefreshingControls) {
    return;
  }
  if (AProjectAirDefensePlayerController* Controller = this->ResolveController()) {
    Controller->RequestSetRayTracingEnabled(bIsChecked);
    this->RefreshFromRuntime();
  }
}

void UProjectAirDefenseBattleWidget::HandleShadowSliderChanged(float Value) {
  if (this->bRefreshingControls) {
    return;
  }
  if (AProjectAirDefensePlayerController* Controller = this->ResolveController()) {
    Controller->RequestSetShadowQualityLevel(SliderQualityLevel(Value));
    this->RefreshFromRuntime();
  }
}

void UProjectAirDefenseBattleWidget::HandleReflectionSliderChanged(float Value) {
  if (this->bRefreshingControls) {
    return;
  }
  if (AProjectAirDefensePlayerController* Controller = this->ResolveController()) {
    Controller->RequestSetReflectionQualityLevel(SliderQualityLevel(Value));
    this->RefreshFromRuntime();
  }
}

void UProjectAirDefenseBattleWidget::HandlePostSliderChanged(float Value) {
  if (this->bRefreshingControls) {
    return;
  }
  if (AProjectAirDefensePlayerController* Controller = this->ResolveController()) {
    Controller->RequestSetPostProcessingQualityLevel(SliderQualityLevel(Value));
    this->RefreshFromRuntime();
  }
}

void UProjectAirDefenseBattleWidget::HandleTimeOfDaySliderChanged(float Value) {
  if (this->bRefreshingControls) {
    return;
  }
  if (AProjectAirDefensePlayerController* Controller = this->ResolveController()) {
    Controller->RequestSetSolarTime(Value);
    this->RefreshFromRuntime();
  }
}

void UProjectAirDefenseBattleWidget::HandleTimeScaleSliderChanged(float Value) {
  if (this->bRefreshingControls) {
    return;
  }
  if (AProjectAirDefensePlayerController* Controller = this->ResolveController()) {
    Controller->RequestSetTimeScale(Value);
    this->RefreshFromRuntime();
  }
}

void UProjectAirDefenseBattleWidget::HandleTimeCycleSwitchChanged(bool bIsChecked) {
  if (this->bRefreshingControls) {
    return;
  }
  if (AProjectAirDefensePlayerController* Controller = this->ResolveController()) {
    const bool bIsRunning = !FMath::IsNearlyZero(Controller->GetTimeScaleHoursPerMinute());
    if (bIsChecked != bIsRunning) {
      Controller->RequestToggleTimeCycle();
    }
    this->RefreshFromRuntime();
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

void UProjectAirDefenseBattleWidget::HandleTimeBackPressed() {
  if (AProjectAirDefensePlayerController* Controller = this->ResolveController()) {
    Controller->RequestTimeStepBack();
  }
}

void UProjectAirDefenseBattleWidget::HandleTimeForwardPressed() {
  if (AProjectAirDefensePlayerController* Controller = this->ResolveController()) {
    Controller->RequestTimeStepForward();
  }
}

void UProjectAirDefenseBattleWidget::HandleTimeSlowerPressed() {
  if (AProjectAirDefensePlayerController* Controller = this->ResolveController()) {
    Controller->RequestSlowerTime();
  }
}

void UProjectAirDefenseBattleWidget::HandleTimeFasterPressed() {
  if (AProjectAirDefensePlayerController* Controller = this->ResolveController()) {
    Controller->RequestFasterTime();
  }
}

void UProjectAirDefenseBattleWidget::HandleTimePausePressed() {
  if (AProjectAirDefensePlayerController* Controller = this->ResolveController()) {
    Controller->RequestToggleTimeCycle();
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

  UTextBlock* MapCreditText =
      CreateText(this->WidgetTree, TEXT("\u00A9 City of Helsinki 3D Mesh, CC BY 4.0"), 18, FLinearColor(0.82f, 0.90f, 1.0f, 0.72f), ETextJustify::Right);
  AddOverlayChild(
      RootOverlay,
      MapCreditText,
      HAlign_Right,
      VAlign_Bottom,
      FMargin(0.0f, 0.0f, UiMargin, 82.0f));

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
      CreateText(this->WidgetTree, TEXT("GFX AUTO | AA ?"), 18, FLinearColor(0.94f, 0.97f, 1.0f, 1.0f));
  if (UVerticalBoxSlot* VerticalSlot = GraphicsPanel->AddChildToVerticalBox(this->GraphicsSummaryText)) {
    VerticalSlot->SetPadding(FMargin(0.0f, 0.0f, 0.0f, 12.0f));
  }

  auto AddSettingSlider =
      [this](
          UVerticalBox* Panel,
          const FString& Label,
          TObjectPtr<UTextBlock>& OutValueText,
          float MinValue,
          float MaxValue,
          float StepSize) -> USlider* {
    UVerticalBox* RowStack = this->WidgetTree->ConstructWidget<UVerticalBox>();
    if (UVerticalBoxSlot* RowSlot = Panel->AddChildToVerticalBox(RowStack)) {
      RowSlot->SetPadding(FMargin(0.0f, 0.0f, 0.0f, 10.0f));
    }

    UHorizontalBox* HeaderRow = this->WidgetTree->ConstructWidget<UHorizontalBox>();
    RowStack->AddChildToVerticalBox(HeaderRow);
    if (UHorizontalBoxSlot* LabelSlot = HeaderRow->AddChildToHorizontalBox(CreateText(
            this->WidgetTree,
            Label,
            17,
            FLinearColor(0.75f, 0.86f, 0.96f, 1.0f)))) {
      LabelSlot->SetSize(ESlateSizeRule::Fill);
    }
    OutValueText =
        CreateText(this->WidgetTree, TEXT("--"), 17, FLinearColor(0.98f, 0.82f, 0.46f, 1.0f), ETextJustify::Right);
    HeaderRow->AddChildToHorizontalBox(OutValueText);

    USlider* Slider = this->WidgetTree->ConstructWidget<USlider>();
    Slider->SetMinValue(MinValue);
    Slider->SetMaxValue(MaxValue);
    Slider->SetStepSize(StepSize);
    Slider->SetIndentHandle(false);
    if (UVerticalBoxSlot* SliderSlot = RowStack->AddChildToVerticalBox(Slider)) {
      SliderSlot->SetPadding(FMargin(0.0f, 4.0f, 0.0f, 0.0f));
    }
    return Slider;
  };

  auto AddSwitchRow =
      [this](UVerticalBox* Panel, const FString& Label, TObjectPtr<UTextBlock>& OutValueText) -> UCheckBox* {
    UHorizontalBox* Row = this->WidgetTree->ConstructWidget<UHorizontalBox>();
    if (UVerticalBoxSlot* RowSlot = Panel->AddChildToVerticalBox(Row)) {
      RowSlot->SetPadding(FMargin(0.0f, 2.0f, 0.0f, 10.0f));
    }
    if (UHorizontalBoxSlot* LabelSlot = Row->AddChildToHorizontalBox(CreateText(
            this->WidgetTree,
            Label,
            17,
            FLinearColor(0.75f, 0.86f, 0.96f, 1.0f)))) {
      LabelSlot->SetSize(ESlateSizeRule::Fill);
      LabelSlot->SetVerticalAlignment(VAlign_Center);
    }
    OutValueText =
        CreateText(this->WidgetTree, TEXT("OFF"), 17, FLinearColor(0.98f, 0.82f, 0.46f, 1.0f), ETextJustify::Right);
    if (UHorizontalBoxSlot* ValueSlot = Row->AddChildToHorizontalBox(OutValueText)) {
      ValueSlot->SetPadding(FMargin(0.0f, 0.0f, 12.0f, 0.0f));
      ValueSlot->SetVerticalAlignment(VAlign_Center);
    }
    UCheckBox* Switch = this->WidgetTree->ConstructWidget<UCheckBox>();
    Switch->SetCheckedState(ECheckBoxState::Unchecked);
    if (UHorizontalBoxSlot* SwitchSlot = Row->AddChildToHorizontalBox(Switch)) {
      SwitchSlot->SetVerticalAlignment(VAlign_Center);
    }
    return Switch;
  };

  this->OverallQualitySlider =
      AddSettingSlider(GraphicsPanel, TEXT("QUALITY"), this->QualityValueText, 0.0f, 4.0f, 1.0f);
  this->OverallQualitySlider->OnValueChanged.AddDynamic(this, &UProjectAirDefenseBattleWidget::HandleOverallQualitySliderChanged);
  this->AaSlider = AddSettingSlider(GraphicsPanel, TEXT("ANTI-ALIASING"), this->AaValueText, 0.0f, 4.0f, 1.0f);
  this->AaSlider->OnValueChanged.AddDynamic(this, &UProjectAirDefenseBattleWidget::HandleAaSliderChanged);
  this->AoSwitch = AddSwitchRow(GraphicsPanel, TEXT("AMBIENT OCCLUSION"), this->AoValueText);
  this->AoSwitch->OnCheckStateChanged.AddDynamic(this, &UProjectAirDefenseBattleWidget::HandleAoSwitchChanged);
  this->BlurSwitch = AddSwitchRow(GraphicsPanel, TEXT("MOTION BLUR"), this->BlurValueText);
  this->BlurSwitch->OnCheckStateChanged.AddDynamic(this, &UProjectAirDefenseBattleWidget::HandleBlurSwitchChanged);
  this->RayTracingSwitch = AddSwitchRow(GraphicsPanel, TEXT("RT REQUEST"), this->RayTracingValueText);
  this->RayTracingSwitch->OnCheckStateChanged.AddDynamic(this, &UProjectAirDefenseBattleWidget::HandleRayTracingSwitchChanged);
  this->ShadowSlider = AddSettingSlider(GraphicsPanel, TEXT("SHADOWS"), this->ShadowValueText, 0.0f, 4.0f, 1.0f);
  this->ShadowSlider->OnValueChanged.AddDynamic(this, &UProjectAirDefenseBattleWidget::HandleShadowSliderChanged);
  this->ReflectionSlider = AddSettingSlider(GraphicsPanel, TEXT("REFLECTIONS"), this->ReflectionValueText, 0.0f, 4.0f, 1.0f);
  this->ReflectionSlider->OnValueChanged.AddDynamic(this, &UProjectAirDefenseBattleWidget::HandleReflectionSliderChanged);
  this->PostSlider = AddSettingSlider(GraphicsPanel, TEXT("POST PROCESS"), this->PostValueText, 0.0f, 4.0f, 1.0f);
  this->PostSlider->OnValueChanged.AddDynamic(this, &UProjectAirDefenseBattleWidget::HandlePostSliderChanged);

  UVerticalBox* TimePanel = BuildDrawerPanel(TEXT("TIME"), FLinearColor(1.0f, 0.78f, 0.38f, 1.0f));
  this->TimeSummaryText =
      CreateText(this->WidgetTree, TEXT("14:00 | PAUSED"), 18, FLinearColor(0.94f, 0.97f, 1.0f, 1.0f));
  if (UVerticalBoxSlot* VerticalSlot = TimePanel->AddChildToVerticalBox(this->TimeSummaryText)) {
    VerticalSlot->SetPadding(FMargin(0.0f, 0.0f, 0.0f, 12.0f));
  }
  this->TimeOfDaySlider =
      AddSettingSlider(TimePanel, TEXT("TIME OF DAY"), this->TimeOfDayValueText, 0.0f, 24.0f, 0.25f);
  this->TimeOfDaySlider->OnValueChanged.AddDynamic(this, &UProjectAirDefenseBattleWidget::HandleTimeOfDaySliderChanged);
  this->TimeScaleSlider =
      AddSettingSlider(TimePanel, TEXT("DAY/NIGHT SPEED"), this->TimeScaleValueText, 0.0f, 12.0f, 0.25f);
  this->TimeScaleSlider->OnValueChanged.AddDynamic(this, &UProjectAirDefenseBattleWidget::HandleTimeScaleSliderChanged);
  this->TimeCycleSwitch = AddSwitchRow(TimePanel, TEXT("CYCLE"), this->TimeCycleValueText);
  this->TimeCycleSwitch->OnCheckStateChanged.AddDynamic(this, &UProjectAirDefenseBattleWidget::HandleTimeCycleSwitchChanged);

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
