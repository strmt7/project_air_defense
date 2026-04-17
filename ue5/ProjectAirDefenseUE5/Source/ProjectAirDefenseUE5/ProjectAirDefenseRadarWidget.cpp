#include "ProjectAirDefenseRadarWidget.h"

#include "Blueprint/WidgetTree.h"
#include "Components/Border.h"
#include "Components/Overlay.h"
#include "Components/OverlaySlot.h"
#include "Components/TextBlock.h"
#include "ProjectAirDefenseBattleManager.h"
#include "ProjectAirDefensePlayerController.h"
#include "Rendering/DrawElements.h"
#include "Styling/AppStyle.h"

namespace {
constexpr float RadarPadding = 18.0f;
constexpr float SweepSpeedDegreesPerSecond = 70.0f;

FSlateFontInfo MakeFont(int32 Size) {
  FSlateFontInfo FontInfo = FAppStyle::GetFontStyle(TEXT("NormalFont"));
  FontInfo.Size = Size;
  return FontInfo;
}

void DrawPolyline(
    FSlateWindowElementList& OutDrawElements,
    int32 LayerId,
    const FGeometry& AllottedGeometry,
    const TArray<FVector2D>& Points,
    const FLinearColor& Color,
    float Thickness,
    bool bClosed = false) {
  if (Points.Num() < 2) {
    return;
  }
  TArray<FVector2D> LinePoints = Points;
  if (bClosed) {
    LinePoints.Add(Points[0]);
  }
  FSlateDrawElement::MakeLines(
      OutDrawElements,
      LayerId,
      AllottedGeometry.ToPaintGeometry(),
      LinePoints,
      ESlateDrawEffect::None,
      Color,
      true,
      Thickness);
}

TArray<FVector2D> BuildCirclePoints(const FVector2D& Center, float Radius, int32 Segments) {
  TArray<FVector2D> Points;
  Points.Reserve(Segments);
  for (int32 Index = 0; Index < Segments; ++Index) {
    const float Angle = (2.0f * PI * static_cast<float>(Index)) / static_cast<float>(Segments);
    Points.Add(Center + FVector2D(FMath::Cos(Angle), FMath::Sin(Angle)) * Radius);
  }
  return Points;
}

TArray<FVector2D> BuildDiamondPoints(const FVector2D& Center, float Radius) {
  return {
      Center + FVector2D(0.0f, -Radius),
      Center + FVector2D(Radius, 0.0f),
      Center + FVector2D(0.0f, Radius),
      Center + FVector2D(-Radius, 0.0f),
  };
}

FVector2D ProjectRadarPoint(const FVector2D& LocalMeters, const FVector2D& Center, float Radius, double ExtentMeters) {
  const double SafeExtent = FMath::Max(ExtentMeters, 1.0);
  const FVector2D Normalized = FVector2D(
      static_cast<float>(LocalMeters.Y / SafeExtent),
      static_cast<float>(-LocalMeters.X / SafeExtent));
  return Center + Normalized * Radius;
}

FLinearColor ThreatColor(EProjectAirDefenseThreatType ThreatType, bool bTracked) {
  switch (ThreatType) {
  case EProjectAirDefenseThreatType::Ballistic:
    return bTracked ? FLinearColor(1.0f, 0.78f, 0.32f, 1.0f) : FLinearColor(1.0f, 0.36f, 0.28f, 1.0f);
  case EProjectAirDefenseThreatType::Glide:
    return bTracked ? FLinearColor(1.0f, 0.64f, 0.30f, 1.0f) : FLinearColor(0.98f, 0.48f, 0.24f, 1.0f);
  case EProjectAirDefenseThreatType::Cruise:
    return bTracked ? FLinearColor(0.92f, 0.82f, 0.36f, 1.0f) : FLinearColor(0.92f, 0.58f, 0.24f, 1.0f);
  }
  return FLinearColor::Red;
}
} // namespace

void UProjectAirDefenseRadarWidget::BindController(AProjectAirDefensePlayerController* InController) {
  this->OwningAirDefenseController = InController;
}

void UProjectAirDefenseRadarWidget::NativeOnInitialized() {
  Super::NativeOnInitialized();
  if (!this->bTreeBuilt) {
    this->BuildWidgetTree();
  }
}

int32 UProjectAirDefenseRadarWidget::NativePaint(
    const FPaintArgs& Args,
    const FGeometry& AllottedGeometry,
    const FSlateRect& MyCullingRect,
    FSlateWindowElementList& OutDrawElements,
    int32 LayerId,
    const FWidgetStyle& InWidgetStyle,
    bool bParentEnabled) const {
  LayerId = Super::NativePaint(
      Args,
      AllottedGeometry,
      MyCullingRect,
      OutDrawElements,
      LayerId,
      InWidgetStyle,
      bParentEnabled);

  const AProjectAirDefenseBattleManager* BattleManager = this->ResolveBattleManager();
  if (BattleManager == nullptr) {
    return LayerId;
  }

  const FProjectAirDefenseRadarSnapshot Snapshot = BattleManager->BuildRadarSnapshot();
  const FVector2D LocalSize = AllottedGeometry.GetLocalSize();
  const FVector2D RadarCenter(LocalSize.X * 0.5f, LocalSize.Y * 0.58f);
  const float RadarRadius = FMath::Max(
      FMath::Min(LocalSize.X, LocalSize.Y) * 0.5f - RadarPadding,
      24.0f);

  const TArray<FVector2D> OuterRing = BuildCirclePoints(RadarCenter, RadarRadius, 44);
  const TArray<FVector2D> InnerRing = BuildCirclePoints(RadarCenter, RadarRadius * 0.55f, 28);
  DrawPolyline(
      OutDrawElements,
      LayerId + 1,
      AllottedGeometry,
      OuterRing,
      FLinearColor(0.24f, 0.86f, 0.98f, 0.86f),
      1.9f,
      true);
  DrawPolyline(
      OutDrawElements,
      LayerId + 1,
      AllottedGeometry,
      InnerRing,
      FLinearColor(0.18f, 0.62f, 0.76f, 0.52f),
      1.2f,
      true);

  DrawPolyline(
      OutDrawElements,
      LayerId + 1,
      AllottedGeometry,
      {FVector2D(RadarCenter.X - RadarRadius, RadarCenter.Y), FVector2D(RadarCenter.X + RadarRadius, RadarCenter.Y)},
      FLinearColor(0.18f, 0.54f, 0.68f, 0.40f),
      1.0f);
  DrawPolyline(
      OutDrawElements,
      LayerId + 1,
      AllottedGeometry,
      {FVector2D(RadarCenter.X, RadarCenter.Y - RadarRadius), FVector2D(RadarCenter.X, RadarCenter.Y + RadarRadius)},
      FLinearColor(0.18f, 0.54f, 0.68f, 0.40f),
      1.0f);

  const float SweepRadians =
      FMath::DegreesToRadians(
          FMath::Fmod(this->GetWorld() == nullptr ? 0.0f : this->GetWorld()->GetTimeSeconds() * SweepSpeedDegreesPerSecond, 360.0f));
  const FVector2D SweepDirection(FMath::Cos(SweepRadians), FMath::Sin(SweepRadians));
  DrawPolyline(
      OutDrawElements,
      LayerId + 2,
      AllottedGeometry,
      {RadarCenter, RadarCenter + SweepDirection * RadarRadius},
      FLinearColor(0.32f, 0.96f, 1.0f, 0.55f),
      1.6f);

  for (const FProjectAirDefenseRadarDistrictSnapshot& District : Snapshot.Districts) {
    const FVector2D DistrictCenter =
        ProjectRadarPoint(District.LocalPositionMeters, RadarCenter, RadarRadius, Snapshot.ExtentMeters);
    const float IntegrityAlpha =
        static_cast<float>(FMath::Clamp(District.Integrity / 100.0, 0.25, 1.0));
    DrawPolyline(
        OutDrawElements,
        LayerId + 2,
        AllottedGeometry,
        BuildDiamondPoints(DistrictCenter, 4.2f),
        FLinearColor(0.34f, 0.92f, 0.88f, IntegrityAlpha * 0.88f),
        1.4f,
        true);
  }

  for (const FProjectAirDefenseRadarLauncherSnapshot& Launcher : Snapshot.Launchers) {
    const FVector2D Position =
        ProjectRadarPoint(Launcher.LocalPositionMeters, RadarCenter, RadarRadius, Snapshot.ExtentMeters);
    const float Size = 4.5f;
    DrawPolyline(
        OutDrawElements,
        LayerId + 3,
        AllottedGeometry,
        {
            Position + FVector2D(-Size, -Size),
            Position + FVector2D(Size, -Size),
            Position + FVector2D(Size, Size),
            Position + FVector2D(-Size, Size),
        },
        FLinearColor(0.18f, 0.92f, 1.0f, 0.92f),
        1.8f,
        true);
  }

  for (const FProjectAirDefenseRadarThreatSnapshot& Threat : Snapshot.Threats) {
    const FVector2D Position =
        ProjectRadarPoint(Threat.LocalPositionMeters, RadarCenter, RadarRadius, Snapshot.ExtentMeters);
    const float Size = Threat.bIsTracked ? 6.0f : 4.0f;
    const FLinearColor Color = ThreatColor(Threat.ThreatType, Threat.bIsTracked);
    DrawPolyline(
        OutDrawElements,
        LayerId + 4,
        AllottedGeometry,
        {Position + FVector2D(-Size, 0.0f), Position + FVector2D(Size, 0.0f)},
        Color,
        1.9f);
    DrawPolyline(
        OutDrawElements,
        LayerId + 4,
        AllottedGeometry,
        {Position + FVector2D(0.0f, -Size), Position + FVector2D(0.0f, Size)},
        Color,
        1.9f);
  }

  return LayerId + 4;
}

void UProjectAirDefenseRadarWidget::BuildWidgetTree() {
  if (this->WidgetTree == nullptr) {
    return;
  }

  this->bTreeBuilt = true;

  UBorder* RootBorder = this->WidgetTree->ConstructWidget<UBorder>();
  RootBorder->SetBrushColor(FLinearColor(0.03f, 0.07f, 0.11f, 0.78f));
  RootBorder->SetPadding(FMargin(10.0f));
  this->WidgetTree->RootWidget = RootBorder;

  UOverlay* Overlay = this->WidgetTree->ConstructWidget<UOverlay>();
  RootBorder->SetContent(Overlay);

  UTextBlock* Label = this->WidgetTree->ConstructWidget<UTextBlock>();
  Label->SetText(FText::FromString(TEXT("RADAR")));
  Label->SetFont(MakeFont(18));
  Label->SetColorAndOpacity(FSlateColor(FLinearColor(0.84f, 0.94f, 1.0f, 1.0f)));
  if (UOverlaySlot* LabelSlot = Overlay->AddChildToOverlay(Label)) {
    LabelSlot->SetHorizontalAlignment(HAlign_Left);
    LabelSlot->SetVerticalAlignment(VAlign_Top);
    LabelSlot->SetPadding(FMargin(4.0f, 0.0f, 0.0f, 0.0f));
  }
}

AProjectAirDefenseBattleManager* UProjectAirDefenseRadarWidget::ResolveBattleManager() const {
  AProjectAirDefensePlayerController* Controller =
      this->OwningAirDefenseController.IsValid()
          ? this->OwningAirDefenseController.Get()
          : Cast<AProjectAirDefensePlayerController>(this->GetOwningPlayer());
  return Controller == nullptr ? nullptr : Controller->FindBattleManager();
}
