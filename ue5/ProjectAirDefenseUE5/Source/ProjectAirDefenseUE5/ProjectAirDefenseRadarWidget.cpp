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
constexpr float RadarPadding = 26.0f;
constexpr float SweepSpeedDegreesPerSecond = 70.0f;

// Tactical palette tuned for outdoor / high-ambient phone screens. The main
// menu uses a cool cyan title over a near-black panel; the radar mirrors that
// by leaning on saturated cyan for friendlies, amber/red for hostiles and a
// low-chroma slate for neutral chrome like the grid.
constexpr float RadarRingSegments = 48.0f;
constexpr float RadarInnerRingSegments = 32.0f;

FSlateFontInfo MakeFont(int32 Size, const TCHAR* Style = TEXT("NormalFont")) {
  FSlateFontInfo FontInfo = FAppStyle::GetFontStyle(Style);
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

  const TArray<FVector2D> OuterRing =
      BuildCirclePoints(RadarCenter, RadarRadius, static_cast<int32>(RadarRingSegments));
  const TArray<FVector2D> MidRing =
      BuildCirclePoints(RadarCenter, RadarRadius * 0.77f, static_cast<int32>(RadarInnerRingSegments));
  const TArray<FVector2D> InnerRing =
      BuildCirclePoints(RadarCenter, RadarRadius * 0.40f, static_cast<int32>(RadarInnerRingSegments));
  DrawPolyline(
      OutDrawElements,
      LayerId + 1,
      AllottedGeometry,
      OuterRing,
      FLinearColor(0.30f, 0.92f, 1.0f, 0.82f),
      1.8f,
      true);
  DrawPolyline(
      OutDrawElements,
      LayerId + 1,
      AllottedGeometry,
      MidRing,
      FLinearColor(0.22f, 0.74f, 0.90f, 0.40f),
      1.0f,
      true);
  DrawPolyline(
      OutDrawElements,
      LayerId + 1,
      AllottedGeometry,
      InnerRing,
      FLinearColor(0.18f, 0.62f, 0.80f, 0.32f),
      0.9f,
      true);

  DrawPolyline(
      OutDrawElements,
      LayerId + 1,
      AllottedGeometry,
      {FVector2D(RadarCenter.X - RadarRadius, RadarCenter.Y), FVector2D(RadarCenter.X + RadarRadius, RadarCenter.Y)},
      FLinearColor(0.22f, 0.60f, 0.74f, 0.32f),
      0.8f);
  DrawPolyline(
      OutDrawElements,
      LayerId + 1,
      AllottedGeometry,
      {FVector2D(RadarCenter.X, RadarCenter.Y - RadarRadius), FVector2D(RadarCenter.X, RadarCenter.Y + RadarRadius)},
      FLinearColor(0.22f, 0.60f, 0.74f, 0.32f),
      0.8f);

  const float SweepRadians =
      FMath::DegreesToRadians(
          FMath::Fmod(this->GetWorld() == nullptr ? 0.0f : this->GetWorld()->GetTimeSeconds() * SweepSpeedDegreesPerSecond, 360.0f));
  const FVector2D SweepDirection(FMath::Cos(SweepRadians), FMath::Sin(SweepRadians));
  // Leading edge: bright; trailing edge: dim, simulating a phosphor afterglow.
  DrawPolyline(
      OutDrawElements,
      LayerId + 2,
      AllottedGeometry,
      {RadarCenter, RadarCenter + SweepDirection * RadarRadius},
      FLinearColor(0.40f, 1.0f, 1.0f, 0.78f),
      2.0f);
  const FVector2D TrailDirection(
      FMath::Cos(SweepRadians - FMath::DegreesToRadians(14.0f)),
      FMath::Sin(SweepRadians - FMath::DegreesToRadians(14.0f)));
  DrawPolyline(
      OutDrawElements,
      LayerId + 2,
      AllottedGeometry,
      {RadarCenter, RadarCenter + TrailDirection * RadarRadius},
      FLinearColor(0.28f, 0.86f, 0.98f, 0.28f),
      1.3f);

  for (const FProjectAirDefenseRadarDistrictSnapshot& District : Snapshot.Districts) {
    const FVector2D DistrictCenter =
        ProjectRadarPoint(District.LocalPositionMeters, RadarCenter, RadarRadius, Snapshot.ExtentMeters);
    const float IntegrityRatio =
        static_cast<float>(FMath::Clamp(District.Integrity / 100.0, 0.0, 1.0));
    const float DamageRatio =
        static_cast<float>(FMath::Clamp(1.0 - District.StructuralIntegrity / 100.0, 0.0, 1.0));
    const bool bDamaged =
        District.DamagedFloors > 0 || District.CollapsedFloors > 0 || DamageRatio > 0.08f || District.bCollapsed;
    const FLinearColor DistrictColor =
        District.bCollapsed
            ? FLinearColor(1.0f, 0.24f, 0.18f, 0.96f)
            : bDamaged
            ? FLinearColor(1.0f, 0.68f, 0.28f, 0.92f)
            : FLinearColor(0.34f, 0.92f, 0.88f, FMath::Lerp(0.42f, 0.82f, IntegrityRatio));
    const float DistrictSize = District.bCollapsed ? 6.0f : bDamaged ? 5.3f : 4.0f;
    const float DistrictThickness = District.bCollapsed ? 2.2f : bDamaged ? 1.8f : 1.1f;
    DrawPolyline(
        OutDrawElements,
        LayerId + 2,
        AllottedGeometry,
        BuildDiamondPoints(DistrictCenter, DistrictSize),
        DistrictColor,
        DistrictThickness,
        true);
  }

  for (const FProjectAirDefenseRadarLauncherSnapshot& Launcher : Snapshot.Launchers) {
    const FVector2D Position =
        ProjectRadarPoint(Launcher.LocalPositionMeters, RadarCenter, RadarRadius, Snapshot.ExtentMeters);
    const float Size = 5.0f;
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
        FLinearColor(0.22f, 0.98f, 1.0f, 0.96f),
        2.0f,
        true);
    // Inner dot for legibility at small radar sizes.
    DrawPolyline(
        OutDrawElements,
        LayerId + 3,
        AllottedGeometry,
        {Position + FVector2D(-1.2f, 0.0f), Position + FVector2D(1.2f, 0.0f)},
        FLinearColor(0.40f, 1.0f, 1.0f, 1.0f),
        2.0f);
  }

  for (const FProjectAirDefenseRadarThreatSnapshot& Threat : Snapshot.Threats) {
    const FVector2D Position =
        ProjectRadarPoint(Threat.LocalPositionMeters, RadarCenter, RadarRadius, Snapshot.ExtentMeters);
    const float Size = Threat.bIsTracked ? 7.0f : 4.8f;
    const FLinearColor Color = ThreatColor(Threat.ThreatType, Threat.bIsTracked);
    if (!Threat.bIsTracked) {
      DrawPolyline(
          OutDrawElements,
          LayerId + 3,
          AllottedGeometry,
          BuildCirclePoints(Position, Size + 2.6f, 14),
          FLinearColor(Color.R, Color.G, Color.B, 0.26f),
          0.9f,
          true);
    }
    DrawPolyline(
        OutDrawElements,
        LayerId + 4,
        AllottedGeometry,
        {Position + FVector2D(-Size, 0.0f), Position + FVector2D(Size, 0.0f)},
        Color,
        2.1f);
    DrawPolyline(
        OutDrawElements,
        LayerId + 4,
        AllottedGeometry,
        {Position + FVector2D(0.0f, -Size), Position + FVector2D(0.0f, Size)},
        Color,
        2.1f);
    // Tracked threats get a containment ring so the player can tell "locked"
    // from "detected" at a glance.
    if (Threat.bIsTracked) {
      DrawPolyline(
          OutDrawElements,
          LayerId + 4,
          AllottedGeometry,
          BuildCirclePoints(Position, Size + 2.4f, 16),
          FLinearColor(Color.R, Color.G, Color.B, 0.75f),
          1.3f,
          true);
    }
  }

  return LayerId + 4;
}

void UProjectAirDefenseRadarWidget::BuildWidgetTree() {
  if (this->WidgetTree == nullptr) {
    return;
  }

  this->bTreeBuilt = true;

  // Deeper, more opaque panel with a subtle cyan cast to match the main-menu
  // glass-card look and lift the radar off the 3D city.
  UBorder* RootBorder = this->WidgetTree->ConstructWidget<UBorder>();
  RootBorder->SetBrushColor(FLinearColor(0.02f, 0.06f, 0.10f, 0.84f));
  RootBorder->SetPadding(FMargin(12.0f));
  this->WidgetTree->RootWidget = RootBorder;

  UOverlay* Overlay = this->WidgetTree->ConstructWidget<UOverlay>();
  RootBorder->SetContent(Overlay);

  // Title chip: brighter, heavier, anchored top-left for tight corner cluster.
  UTextBlock* Label = this->WidgetTree->ConstructWidget<UTextBlock>();
  Label->SetText(FText::FromString(TEXT("RADAR")));
  Label->SetFont(MakeFont(18));
  Label->SetColorAndOpacity(FSlateColor(FLinearColor(0.92f, 0.99f, 1.0f, 1.0f)));
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
