#include "ProjectAirDefenseMainMenuWidget.h"

#include "Blueprint/WidgetTree.h"
#include "Components/Border.h"
#include "Components/Button.h"
#include "Components/HorizontalBox.h"
#include "Components/HorizontalBoxSlot.h"
#include "Components/Overlay.h"
#include "Components/OverlaySlot.h"
#include "Components/SafeZone.h"
#include "Components/SizeBox.h"
#include "Components/Spacer.h"
#include "Components/TextBlock.h"
#include "Components/VerticalBox.h"
#include "Components/VerticalBoxSlot.h"
#include "Brushes/SlateRoundedBoxBrush.h"
#include "ProjectAirDefenseBattleManager.h"
#include "ProjectAirDefensePlayerController.h"
#include "ProjectAirDefenseTouchButton.h"
#include "Styling/AppStyle.h"

namespace {
constexpr float PanelPadding = 26.0f;
constexpr float ButtonGap = 14.0f;
constexpr float ChipGap = 10.0f;
constexpr float PanelCornerRadius = 18.0f;
constexpr float ButtonCornerRadius = 12.0f;
constexpr float ChipCornerRadius = 10.0f;
constexpr const TCHAR* MainMenuTitleText = TEXT("PROJECT AIR DEFENSE");

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

UButton* CreatePrimaryButton(
    UWidgetTree* WidgetTree,
    const FString& Label,
    const FLinearColor& FillColor,
    UTextBlock*& OutLabel) {
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
      FLinearColor(0.62f, 0.72f, 0.80f, 0.42f),
      1.0f));
  ButtonStyle.SetHovered(FSlateRoundedBoxBrush(
      HoverColor,
      ButtonCornerRadius,
      FLinearColor(0.78f, 0.90f, 1.0f, 0.66f),
      1.5f));
  ButtonStyle.SetPressed(FSlateRoundedBoxBrush(
      PressedColor,
      ButtonCornerRadius,
      FLinearColor(0.48f, 0.64f, 0.75f, 0.62f),
      1.0f));
  ButtonStyle.SetNormalPadding(FMargin(0.0f));
  ButtonStyle.SetPressedPadding(FMargin(0.0f));
  Button->SetStyle(ButtonStyle);
  Button->SetColorAndOpacity(FLinearColor::White);
  Button->SetClickMethod(EButtonClickMethod::MouseDown);

  UBorder* PaddingBorder = WidgetTree->ConstructWidget<UBorder>();
  PaddingBorder->SetBrushColor(FLinearColor::Transparent);
  PaddingBorder->SetPadding(FMargin(18.0f, 16.0f));

  OutLabel = CreateText(WidgetTree, Label, 24, FLinearColor::White, ETextJustify::Center);
  PaddingBorder->SetContent(OutLabel);
  Button->AddChild(PaddingBorder);
  return Button;
}

UBorder* CreatePanel(UWidgetTree* WidgetTree, const FLinearColor& FillColor) {
  UBorder* Border = WidgetTree->ConstructWidget<UBorder>();
  Border->SetBrush(FSlateRoundedBoxBrush(
      FillColor,
      PanelCornerRadius,
      FLinearColor(0.62f, 0.74f, 0.82f, 0.22f),
      1.0f));
  Border->SetPadding(FMargin(PanelPadding));
  return Border;
}

UBorder* CreateChip(UWidgetTree* WidgetTree, const FString& Label, const FLinearColor& FillColor) {
  UBorder* Border = WidgetTree->ConstructWidget<UBorder>();
  Border->SetBrush(FSlateRoundedBoxBrush(
      FillColor,
      ChipCornerRadius,
      FLinearColor(0.75f, 0.86f, 0.92f, 0.18f),
      1.0f));
  Border->SetPadding(FMargin(12.0f, 6.0f));
  Border->SetContent(CreateText(
      WidgetTree,
      Label,
      18,
      FLinearColor(0.92f, 0.97f, 1.0f, 1.0f),
      ETextJustify::Center));
  return Border;
}
} // namespace

void UProjectAirDefenseMainMenuWidget::BindController(AProjectAirDefensePlayerController* InController) {
  this->OwningAirDefenseController = InController;
  this->RefreshGraphicsSummary();
}

void UProjectAirDefenseMainMenuWidget::RefreshGraphicsSummary() {
  if (this->GraphicsSummaryText == nullptr) {
    return;
  }

  FString Summary = TEXT("QUALITY AUTO");
  if (AProjectAirDefensePlayerController* Controller = this->ResolveController()) {
    if (AProjectAirDefenseBattleManager* BattleManager = Controller->FindBattleManager()) {
      Summary = BattleManager->BuildGraphicsSummaryText();
    }
  }
  this->GraphicsSummaryText->SetText(FText::FromString(Summary));
}

void UProjectAirDefenseMainMenuWidget::NativeOnInitialized() {
  Super::NativeOnInitialized();
  if (!this->bTreeBuilt) {
    this->BuildWidgetTree();
  }
}

void UProjectAirDefenseMainMenuWidget::HandleStartPressed() {
  if (AProjectAirDefensePlayerController* Controller = this->ResolveController()) {
    Controller->StartBattleExperience(true, false);
  }
}

void UProjectAirDefenseMainMenuWidget::HandleGraphicsPressed() {
  if (AProjectAirDefensePlayerController* Controller = this->ResolveController()) {
    Controller->StartBattleExperience(false, true);
  }
}

void UProjectAirDefenseMainMenuWidget::BuildWidgetTree() {
  if (this->WidgetTree == nullptr) {
    return;
  }

  this->bTreeBuilt = true;

  USafeZone* SafeZone = this->WidgetTree->ConstructWidget<USafeZone>();
  UOverlay* RootOverlay = this->WidgetTree->ConstructWidget<UOverlay>();
  SafeZone->SetContent(RootOverlay);
  this->WidgetTree->RootWidget = SafeZone;

  UBorder* Surface = CreatePanel(this->WidgetTree, FLinearColor(0.03f, 0.07f, 0.11f, 0.82f));
  if (UOverlaySlot* SurfaceSlot = RootOverlay->AddChildToOverlay(Surface)) {
    SurfaceSlot->SetHorizontalAlignment(HAlign_Center);
    SurfaceSlot->SetVerticalAlignment(VAlign_Bottom);
    SurfaceSlot->SetPadding(FMargin(36.0f, 24.0f, 36.0f, 34.0f));
  }

  USizeBox* SurfaceBox = this->WidgetTree->ConstructWidget<USizeBox>();
  SurfaceBox->SetWidthOverride(760.0f);
  Surface->SetContent(SurfaceBox);

  UVerticalBox* Content = this->WidgetTree->ConstructWidget<UVerticalBox>();
  SurfaceBox->SetContent(Content);

  UTextBlock* TitleText =
      CreateText(this->WidgetTree, MainMenuTitleText, 40, FLinearColor(0.96f, 0.98f, 1.0f, 1.0f));
  if (UVerticalBoxSlot* VerticalSlot = Content->AddChildToVerticalBox(TitleText)) {
    VerticalSlot->SetPadding(FMargin(0.0f, 0.0f, 0.0f, 8.0f));
  }

  UTextBlock* SubtitleText = CreateText(
      this->WidgetTree,
      TEXT("3D city defense"),
      22,
      FLinearColor(0.73f, 0.90f, 1.0f, 1.0f));
  if (UVerticalBoxSlot* VerticalSlot = Content->AddChildToVerticalBox(SubtitleText)) {
    VerticalSlot->SetPadding(FMargin(0.0f, 0.0f, 0.0f, 18.0f));
  }

  UHorizontalBox* ChipRow = this->WidgetTree->ConstructWidget<UHorizontalBox>();
  if (UVerticalBoxSlot* VerticalSlot = Content->AddChildToVerticalBox(ChipRow)) {
    VerticalSlot->SetPadding(FMargin(0.0f, 0.0f, 0.0f, 22.0f));
  }
  UBorder* LiveChip = CreateChip(this->WidgetTree, TEXT("LIVE 3D CITY"), FLinearColor(0.08f, 0.17f, 0.24f, 0.96f));
  if (UHorizontalBoxSlot* ChipSlot = ChipRow->AddChildToHorizontalBox(LiveChip)) {
    ChipSlot->SetPadding(FMargin(0.0f, 0.0f, ChipGap, 0.0f));
  }
  UBorder* LandscapeChip = CreateChip(this->WidgetTree, TEXT("LANDSCAPE"), FLinearColor(0.10f, 0.10f, 0.18f, 0.96f));
  if (UHorizontalBoxSlot* ChipSlot = ChipRow->AddChildToHorizontalBox(LandscapeChip)) {
    ChipSlot->SetPadding(FMargin(0.0f, 0.0f, ChipGap, 0.0f));
  }
  ChipRow->AddChildToHorizontalBox(CreateChip(
      this->WidgetTree,
      TEXT("TOUCH HUD"),
      FLinearColor(0.10f, 0.16f, 0.13f, 0.96f)));

  UTextBlock* MissionText = CreateText(
      this->WidgetTree,
      TEXT("Real city mesh. Touch-first battle. No static backdrop."),
      21,
      FLinearColor(0.86f, 0.90f, 0.95f, 1.0f));
  MissionText->SetAutoWrapText(true);
  if (UVerticalBoxSlot* VerticalSlot = Content->AddChildToVerticalBox(MissionText)) {
    VerticalSlot->SetPadding(FMargin(0.0f, 0.0f, 0.0f, 22.0f));
  }

  UTextBlock* StartLabel = nullptr;
  this->StartButton = CreatePrimaryButton(
      this->WidgetTree,
      TEXT("START DEFENSE"),
      FLinearColor(0.08f, 0.17f, 0.24f, 0.98f),
      StartLabel);
  this->StartButton->OnClicked.AddDynamic(this, &UProjectAirDefenseMainMenuWidget::HandleStartPressed);
  if (UVerticalBoxSlot* VerticalSlot = Content->AddChildToVerticalBox(this->StartButton)) {
    VerticalSlot->SetPadding(FMargin(0.0f, 0.0f, 0.0f, ButtonGap));
  }

  UTextBlock* GraphicsLabel = nullptr;
  this->GraphicsButton = CreatePrimaryButton(
      this->WidgetTree,
      TEXT("SYSTEMS"),
      FLinearColor(0.10f, 0.10f, 0.18f, 0.98f),
      GraphicsLabel);
  this->GraphicsButton->OnClicked.AddDynamic(this, &UProjectAirDefenseMainMenuWidget::HandleGraphicsPressed);
  if (UVerticalBoxSlot* VerticalSlot = Content->AddChildToVerticalBox(this->GraphicsButton)) {
    VerticalSlot->SetPadding(FMargin(0.0f, 0.0f, 0.0f, 18.0f));
  }

  UHorizontalBox* FooterRow = this->WidgetTree->ConstructWidget<UHorizontalBox>();
  if (UVerticalBoxSlot* VerticalSlot = Content->AddChildToVerticalBox(FooterRow)) {
    VerticalSlot->SetPadding(FMargin(0.0f, 2.0f, 0.0f, 0.0f));
  }

  UBorder* RenderPanel = CreatePanel(this->WidgetTree, FLinearColor(0.04f, 0.09f, 0.14f, 0.72f));
  if (UHorizontalBoxSlot* FooterSlot = FooterRow->AddChildToHorizontalBox(RenderPanel)) {
    FooterSlot->SetSize(ESlateSizeRule::Fill);
    FooterSlot->SetPadding(FMargin(0.0f, 0.0f, 12.0f, 0.0f));
  }
  UVerticalBox* RenderContent = this->WidgetTree->ConstructWidget<UVerticalBox>();
  RenderPanel->SetContent(RenderContent);
  RenderContent->AddChildToVerticalBox(CreateText(
      this->WidgetTree,
      TEXT("RENDER"),
      18,
      FLinearColor(0.98f, 0.82f, 0.46f, 1.0f)));

  this->GraphicsSummaryText = CreateText(
      this->WidgetTree,
      TEXT("QUALITY AUTO"),
      20,
      FLinearColor(0.96f, 0.98f, 1.0f, 1.0f));
  this->GraphicsSummaryText->SetAutoWrapText(true);
  if (UVerticalBoxSlot* VerticalSlot = RenderContent->AddChildToVerticalBox(this->GraphicsSummaryText)) {
    VerticalSlot->SetPadding(FMargin(0.0f, 6.0f, 0.0f, 0.0f));
  }

  UBorder* SourcePanel = CreatePanel(this->WidgetTree, FLinearColor(0.04f, 0.09f, 0.14f, 0.72f));
  if (UHorizontalBoxSlot* FooterSlot = FooterRow->AddChildToHorizontalBox(SourcePanel)) {
    FooterSlot->SetSize(ESlateSizeRule::Fill);
  }
  UVerticalBox* SourceContent = this->WidgetTree->ConstructWidget<UVerticalBox>();
  SourcePanel->SetContent(SourceContent);
  SourceContent->AddChildToVerticalBox(CreateText(
      this->WidgetTree,
      TEXT("CITY"),
      18,
      FLinearColor(0.76f, 0.88f, 1.0f, 1.0f)));
  UTextBlock* SourceValue = CreateText(
      this->WidgetTree,
      TEXT("Helsinki Kalasatama 3D Tiles"),
      20,
      FLinearColor(0.86f, 0.90f, 0.95f, 1.0f));
  SourceValue->SetAutoWrapText(true);
  if (UVerticalBoxSlot* VerticalSlot = SourceContent->AddChildToVerticalBox(SourceValue)) {
    VerticalSlot->SetPadding(FMargin(0.0f, 6.0f, 0.0f, 0.0f));
  }

  this->RefreshGraphicsSummary();
}

AProjectAirDefensePlayerController* UProjectAirDefenseMainMenuWidget::ResolveController() const {
  if (this->OwningAirDefenseController.IsValid()) {
    return this->OwningAirDefenseController.Get();
  }
  return Cast<AProjectAirDefensePlayerController>(this->GetOwningPlayer());
}
