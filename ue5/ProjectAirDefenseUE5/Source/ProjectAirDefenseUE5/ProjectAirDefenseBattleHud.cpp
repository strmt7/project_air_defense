#include "ProjectAirDefenseBattleHud.h"

#include "Engine/Canvas.h"
#include "Engine/Engine.h"
#include "EngineUtils.h"
#include "ProjectAirDefenseBattleManager.h"
#include "ProjectAirDefenseCityCameraPawn.h"
#include "ProjectAirDefensePlayerController.h"

namespace {
constexpr float PanelMargin = 24.0f;
constexpr float CardPadding = 18.0f;
constexpr float DrawerGap = 14.0f;
constexpr float ButtonGap = 12.0f;
constexpr float DrawerInnerGap = 16.0f;
constexpr float FloatingPanelShadowOffset = 7.0f;
constexpr float PrimaryButtonStepMeters = 220.0f;
constexpr float PrimaryZoomStepMeters = 260.0f;
constexpr float PrimaryAltitudeStepMeters = 150.0f;
constexpr float PrimaryYawStepDegrees = 9.0f;
constexpr float PrimaryPitchStepDegrees = 5.0f;

FName HitBoxName(const TCHAR* RawName) {
  return FName(RawName);
}

FLinearColor TacticalFill() {
  return FLinearColor(0.03f, 0.06f, 0.10f, 0.68f);
}

FLinearColor TacticalAccent() {
  return FLinearColor(0.26f, 0.86f, 0.98f, 0.96f);
}

FLinearColor ActionFill() {
  return FLinearColor(0.04f, 0.08f, 0.12f, 0.82f);
}

FLinearColor ActionAccent() {
  return FLinearColor(0.93f, 0.73f, 0.31f, 0.95f);
}
}

void AProjectAirDefenseBattleHud::DrawHUD() {
  Super::DrawHUD();
  if (this->Canvas == nullptr) {
    return;
  }

  AProjectAirDefenseBattleManager* BattleManager = this->FindBattleManager();
  if (BattleManager == nullptr || !BattleManager->IsBattlefieldInitialized()) {
    return;
  }

  const FProjectAirDefenseRuntimeSnapshot Snapshot = BattleManager->BuildRuntimeSnapshot();
  AProjectAirDefensePlayerController* PlayerController = this->FindPlayerController();
  const bool bSystemsMenuVisible =
      PlayerController != nullptr && PlayerController->IsSystemsMenuVisible();
  const float LeftPanelWidth = FMath::Clamp(this->Canvas->ClipX * 0.23f, 272.0f, 338.0f);
  const float RightPanelWidth = FMath::Clamp(this->Canvas->ClipX * 0.19f, 246.0f, 300.0f);
  const float LeftPanelHeight = 88.0f;
  const float RightPanelHeight = 72.0f;
  const float BottomButtonHeight = FMath::Clamp(this->Canvas->ClipY * 0.072f, 58.0f, 76.0f);
  const float BottomButtonWidth = FMath::Clamp(this->Canvas->ClipX * 0.14f, 158.0f, 220.0f);
  const float DrawerHeight = FMath::Clamp(this->Canvas->ClipY * 0.30f, 228.0f, 296.0f);
  const float DrawerWidth =
      FMath::Clamp(this->Canvas->ClipX * 0.74f, 820.0f, 1100.0f);
  const float DrawerY = this->Canvas->ClipY - BottomButtonHeight - DrawerHeight - PanelMargin * 1.8f;
  const float DrawerX = (this->Canvas->ClipX - DrawerWidth) * 0.5f;

  const FString LeftTitle = Snapshot.bGameOver
                                ? TEXT("CITY LOST")
                                      : Snapshot.bWaveInProgress
                                      ? FString::Printf(
                                            TEXT("WAVE %d  LIVE  %d HOSTILES"),
                                            Snapshot.Wave,
                                            Snapshot.VisibleThreats + Snapshot.RemainingThreatsInWave)
                                      : FString::Printf(TEXT("WAVE %d  READY"), Snapshot.Wave);
  const FString DoctrineLine = FString::Printf(
      TEXT("%s  |  TRACKED %d"),
      *ProjectAirDefenseDoctrineLabel(Snapshot.Doctrine),
      Snapshot.TrackedThreats);
  const FString ThreatLine = FString::Printf(
      TEXT("B %d   G %d   C %d"),
      Snapshot.BallisticThreats,
      Snapshot.GlideThreats,
      Snapshot.CruiseThreats);

  const FString SummaryLine =
      FString::Printf(TEXT("CITY %d%%   SCORE %d"),
                      FMath::RoundToInt(Snapshot.CityIntegrity),
                      Snapshot.Score);
  const FString RangeLine = FString::Printf(
      TEXT("CR %d   RNG %dm   FUSE %dm"),
      Snapshot.Credits,
      FMath::RoundToInt(Snapshot.EffectiveRangeMeters),
      FMath::RoundToInt(Snapshot.EffectiveFuseMeters));

  const FVector2D LeftPosition(PanelMargin, PanelMargin);
  const FVector2D RightPosition(this->Canvas->ClipX - RightPanelWidth - PanelMargin, PanelMargin);
  this->DrawCard(
      LeftPosition,
      FVector2D(LeftPanelWidth, LeftPanelHeight),
      TacticalFill(),
      TacticalAccent());
  this->DrawCard(
      RightPosition,
      FVector2D(RightPanelWidth, RightPanelHeight),
      FLinearColor(0.03f, 0.06f, 0.10f, 0.72f),
      ActionAccent());

  this->DrawPanelText(
      LeftTitle,
      LeftPosition.X + CardPadding,
      LeftPosition.Y + 10.0f,
      FLinearColor(0.98f, 0.99f, 1.0f, 1.0f),
      1.08f);
  this->DrawPanelText(
      DoctrineLine,
      LeftPosition.X + CardPadding,
      LeftPosition.Y + 38.0f,
      FLinearColor(0.69f, 0.89f, 1.0f, 1.0f),
      0.96f);
  this->DrawPanelText(
      ThreatLine,
      LeftPosition.X + CardPadding,
      LeftPosition.Y + 63.0f,
      FLinearColor(0.80f, 0.85f, 0.90f, 1.0f),
      0.90f);

  this->DrawPanelText(
      SummaryLine,
      RightPosition.X + CardPadding,
      RightPosition.Y + 10.0f,
      FLinearColor(0.98f, 0.99f, 1.0f, 1.0f),
      1.04f);
  this->DrawPanelText(
      RangeLine,
      RightPosition.X + CardPadding,
      RightPosition.Y + 38.0f,
      FLinearColor(0.74f, 0.90f, 1.0f, 1.0f),
      0.94f);

  const float BottomButtonY = this->Canvas->ClipY - BottomButtonHeight - PanelMargin;
  const FVector2D SystemsButtonPosition(PanelMargin, BottomButtonY);
  const FVector2D DoctrineButtonPosition(
      this->Canvas->ClipX - PanelMargin - BottomButtonWidth * 2.0f - ButtonGap,
      BottomButtonY);
  const FVector2D WaveButtonPosition(
      this->Canvas->ClipX - PanelMargin - BottomButtonWidth,
      BottomButtonY);
  this->DrawActionButton(
      HitBoxName(TEXT("Systems")),
      SystemsButtonPosition,
      FVector2D(BottomButtonWidth, BottomButtonHeight),
      bSystemsMenuVisible ? TEXT("CLOSE") : TEXT("SYSTEMS"),
      TEXT("CAMERA  GRAPHICS"),
      ActionFill(),
      TacticalAccent());
  this->DrawActionButton(
      HitBoxName(TEXT("Doctrine")),
      DoctrineButtonPosition,
      FVector2D(BottomButtonWidth, BottomButtonHeight),
      *ProjectAirDefenseDoctrineLabel(Snapshot.Doctrine),
      TEXT("DEFENSE DOCTRINE"),
      ActionFill(),
      FLinearColor(0.79f, 0.63f, 0.99f, 0.96f));
  this->DrawActionButton(
      HitBoxName(TEXT("WaveAction")),
      WaveButtonPosition,
      FVector2D(BottomButtonWidth, BottomButtonHeight),
      Snapshot.bWaveInProgress ? TEXT("LIVE") : TEXT("ENGAGE"),
      Snapshot.bWaveInProgress ? TEXT("AUTOMATIC INTERCEPT") : TEXT("START NEXT WAVE"),
      ActionFill(),
      ActionAccent());

  if (!bSystemsMenuVisible) {
    return;
  }

  const float ColumnTop = DrawerY;
  const float ColumnWidth = (DrawerWidth - 44.0f - DrawerInnerGap * 2.0f) / 3.0f;
  const float TacticsX = DrawerX + 22.0f;
  const float CameraX = TacticsX + ColumnWidth + DrawerInnerGap;
  const float GraphicsX = CameraX + ColumnWidth + DrawerInnerGap;
  const float ColumnHeight = DrawerHeight;
  const FVector2D ColumnSize(ColumnWidth, ColumnHeight);
  const float SectionButtonHeight = 46.0f;
  const float CompactButtonHeight = 42.0f;
  const float CompactGap = 8.0f;
  const float QuarterButtonWidth = (ColumnWidth - CompactGap * 3.0f) / 4.0f;
  const float ThirdButtonWidth = (ColumnWidth - CompactGap * 2.0f) / 3.0f;
  const float HalfButtonWidth = (ColumnWidth - CompactGap) / 2.0f;

  this->DrawCard(
      FVector2D(TacticsX, ColumnTop),
      ColumnSize,
      FLinearColor(0.03f, 0.06f, 0.10f, 0.88f),
      FLinearColor(0.98f, 0.72f, 0.31f, 0.92f));
  this->DrawCard(
      FVector2D(CameraX, ColumnTop),
      ColumnSize,
      FLinearColor(0.03f, 0.06f, 0.10f, 0.88f),
      TacticalAccent());
  this->DrawCard(
      FVector2D(GraphicsX, ColumnTop),
      ColumnSize,
      FLinearColor(0.03f, 0.06f, 0.10f, 0.88f),
      FLinearColor(0.77f, 0.63f, 0.99f, 0.92f));

  this->DrawPanelText(TEXT("TACTICS"), TacticsX + 18.0f, ColumnTop + 10.0f, FLinearColor::White, 0.96f);
  this->DrawPanelText(
      FString::Printf(TEXT("LIVE %d   TRACK %d"), Snapshot.VisibleThreats, Snapshot.TrackedThreats),
      TacticsX + 18.0f,
      ColumnTop + 36.0f,
      FLinearColor(0.82f, 0.88f, 0.93f, 1.0f),
      0.88f);
  this->DrawPanelText(
      FString::Printf(TEXT("B %d   G %d   C %d"), Snapshot.BallisticThreats, Snapshot.GlideThreats, Snapshot.CruiseThreats),
      TacticsX + 18.0f,
      ColumnTop + 60.0f,
      FLinearColor(0.72f, 0.90f, 1.0f, 1.0f),
      0.88f);
  this->DrawActionButton(
      HitBoxName(TEXT("DrawerWaveAction")),
      FVector2D(TacticsX + 18.0f, ColumnTop + 92.0f),
      FVector2D(ColumnWidth - 36.0f, SectionButtonHeight),
      Snapshot.bWaveInProgress ? TEXT("BATTLE LIVE") : TEXT("ENGAGE"),
      Snapshot.bWaveInProgress ? TEXT("AUTO INTERCEPT ACTIVE") : TEXT("START NEXT WAVE"),
      ActionFill(),
      ActionAccent());
  this->DrawActionButton(
      HitBoxName(TEXT("DrawerDoctrine")),
      FVector2D(TacticsX + 18.0f, ColumnTop + 146.0f),
      FVector2D(ColumnWidth - 36.0f, SectionButtonHeight),
      *ProjectAirDefenseDoctrineLabel(Snapshot.Doctrine),
      TEXT("CHANGE DEFENSE DOCTRINE"),
      ActionFill(),
      FLinearColor(0.77f, 0.63f, 0.99f, 0.92f));
  this->DrawPanelText(
      BattleManager->BuildGraphicsSummaryText(),
      TacticsX + 18.0f,
      ColumnTop + 204.0f,
      FLinearColor(0.84f, 0.89f, 0.94f, 1.0f),
      0.86f);

  this->DrawPanelText(TEXT("CAMERA"), CameraX + 18.0f, ColumnTop + 10.0f, FLinearColor::White, 0.96f);
  const float CameraButtonTop = ColumnTop + 48.0f;
  this->DrawActionButton(
      HitBoxName(TEXT("CameraPanLeft")),
      FVector2D(CameraX + 18.0f, CameraButtonTop),
      FVector2D(QuarterButtonWidth, CompactButtonHeight),
      TEXT("PAN L"),
      TEXT(""),
      ActionFill(),
      TacticalAccent());
  this->DrawActionButton(
      HitBoxName(TEXT("CameraPanRight")),
      FVector2D(CameraX + 18.0f + (QuarterButtonWidth + CompactGap), CameraButtonTop),
      FVector2D(QuarterButtonWidth, CompactButtonHeight),
      TEXT("PAN R"),
      TEXT(""),
      ActionFill(),
      TacticalAccent());
  this->DrawActionButton(
      HitBoxName(TEXT("CameraPanUp")),
      FVector2D(CameraX + 18.0f + (QuarterButtonWidth + CompactGap) * 2.0f, CameraButtonTop),
      FVector2D(QuarterButtonWidth, CompactButtonHeight),
      TEXT("PAN U"),
      TEXT(""),
      ActionFill(),
      TacticalAccent());
  this->DrawActionButton(
      HitBoxName(TEXT("CameraPanDown")),
      FVector2D(CameraX + 18.0f + (QuarterButtonWidth + CompactGap) * 3.0f, CameraButtonTop),
      FVector2D(QuarterButtonWidth, CompactButtonHeight),
      TEXT("PAN D"),
      TEXT(""),
      ActionFill(),
      TacticalAccent());
  const float CameraRow2 = CameraButtonTop + CompactButtonHeight + CompactGap;
  this->DrawActionButton(HitBoxName(TEXT("CameraYawMinus")), FVector2D(CameraX + 18.0f, CameraRow2), FVector2D(HalfButtonWidth, CompactButtonHeight), TEXT("YAW -"), TEXT(""), ActionFill(), TacticalAccent());
  this->DrawActionButton(HitBoxName(TEXT("CameraYawPlus")), FVector2D(CameraX + 18.0f + HalfButtonWidth + CompactGap, CameraRow2), FVector2D(HalfButtonWidth, CompactButtonHeight), TEXT("YAW +"), TEXT(""), ActionFill(), TacticalAccent());
  const float CameraRow3 = CameraRow2 + CompactButtonHeight + CompactGap;
  this->DrawActionButton(HitBoxName(TEXT("CameraPitchMinus")), FVector2D(CameraX + 18.0f, CameraRow3), FVector2D(HalfButtonWidth, CompactButtonHeight), TEXT("TILT -"), TEXT(""), ActionFill(), TacticalAccent());
  this->DrawActionButton(HitBoxName(TEXT("CameraPitchPlus")), FVector2D(CameraX + 18.0f + HalfButtonWidth + CompactGap, CameraRow3), FVector2D(HalfButtonWidth, CompactButtonHeight), TEXT("TILT +"), TEXT(""), ActionFill(), TacticalAccent());
  const float CameraRow4 = CameraRow3 + CompactButtonHeight + CompactGap;
  this->DrawActionButton(HitBoxName(TEXT("CameraZoomMinus")), FVector2D(CameraX + 18.0f, CameraRow4), FVector2D(HalfButtonWidth, CompactButtonHeight), TEXT("ZOOM -"), TEXT(""), ActionFill(), TacticalAccent());
  this->DrawActionButton(HitBoxName(TEXT("CameraZoomPlus")), FVector2D(CameraX + 18.0f + HalfButtonWidth + CompactGap, CameraRow4), FVector2D(HalfButtonWidth, CompactButtonHeight), TEXT("ZOOM +"), TEXT(""), ActionFill(), TacticalAccent());
  const float CameraRow5 = CameraRow4 + CompactButtonHeight + CompactGap;
  this->DrawActionButton(HitBoxName(TEXT("CameraAltitudeMinus")), FVector2D(CameraX + 18.0f, CameraRow5), FVector2D(HalfButtonWidth, CompactButtonHeight), TEXT("ALT -"), TEXT(""), ActionFill(), TacticalAccent());
  this->DrawActionButton(HitBoxName(TEXT("CameraAltitudePlus")), FVector2D(CameraX + 18.0f + HalfButtonWidth + CompactGap, CameraRow5), FVector2D(HalfButtonWidth, CompactButtonHeight), TEXT("ALT +"), TEXT(""), ActionFill(), TacticalAccent());
  const float CameraRow6 = CameraRow5 + CompactButtonHeight + CompactGap;
  this->DrawActionButton(
      HitBoxName(TEXT("CameraReset")),
      FVector2D(CameraX + 18.0f, CameraRow6),
      FVector2D(ColumnWidth - 36.0f, CompactButtonHeight),
      TEXT("RESET VIEW"),
      TEXT(""),
      ActionFill(),
      TacticalAccent());

  this->DrawPanelText(TEXT("GRAPHICS"), GraphicsX + 18.0f, ColumnTop + 10.0f, FLinearColor::White, 0.96f);
  const float GraphicsRow1 = ColumnTop + 48.0f;
  this->DrawActionButton(HitBoxName(TEXT("GraphicsQualityMinus")), FVector2D(GraphicsX + 18.0f, GraphicsRow1), FVector2D(HalfButtonWidth, CompactButtonHeight), TEXT("QUALITY -"), TEXT(""), ActionFill(), FLinearColor(0.77f, 0.63f, 0.99f, 0.92f));
  this->DrawActionButton(HitBoxName(TEXT("GraphicsQualityPlus")), FVector2D(GraphicsX + 18.0f + HalfButtonWidth + CompactGap, GraphicsRow1), FVector2D(HalfButtonWidth, CompactButtonHeight), TEXT("QUALITY +"), TEXT(""), ActionFill(), FLinearColor(0.77f, 0.63f, 0.99f, 0.92f));
  const float GraphicsRow2 = GraphicsRow1 + CompactButtonHeight + CompactGap;
  this->DrawActionButton(HitBoxName(TEXT("GraphicsAA")), FVector2D(GraphicsX + 18.0f, GraphicsRow2), FVector2D(ThirdButtonWidth, CompactButtonHeight), TEXT("AA"), TEXT(""), ActionFill(), FLinearColor(0.77f, 0.63f, 0.99f, 0.92f));
  this->DrawActionButton(HitBoxName(TEXT("GraphicsAO")), FVector2D(GraphicsX + 18.0f + ThirdButtonWidth + CompactGap, GraphicsRow2), FVector2D(ThirdButtonWidth, CompactButtonHeight), TEXT("AO"), TEXT(""), ActionFill(), FLinearColor(0.77f, 0.63f, 0.99f, 0.92f));
  this->DrawActionButton(HitBoxName(TEXT("GraphicsMotionBlur")), FVector2D(GraphicsX + 18.0f + (ThirdButtonWidth + CompactGap) * 2.0f, GraphicsRow2), FVector2D(ThirdButtonWidth, CompactButtonHeight), TEXT("BLUR"), TEXT(""), ActionFill(), FLinearColor(0.77f, 0.63f, 0.99f, 0.92f));
  const float GraphicsRow3 = GraphicsRow2 + CompactButtonHeight + CompactGap;
  this->DrawActionButton(HitBoxName(TEXT("GraphicsShadows")), FVector2D(GraphicsX + 18.0f, GraphicsRow3), FVector2D(ThirdButtonWidth, CompactButtonHeight), TEXT("SHADOW"), TEXT(""), ActionFill(), FLinearColor(0.77f, 0.63f, 0.99f, 0.92f));
  this->DrawActionButton(HitBoxName(TEXT("GraphicsReflections")), FVector2D(GraphicsX + 18.0f + ThirdButtonWidth + CompactGap, GraphicsRow3), FVector2D(ThirdButtonWidth, CompactButtonHeight), TEXT("REFLECT"), TEXT(""), ActionFill(), FLinearColor(0.77f, 0.63f, 0.99f, 0.92f));
  this->DrawActionButton(HitBoxName(TEXT("GraphicsPost")), FVector2D(GraphicsX + 18.0f + (ThirdButtonWidth + CompactGap) * 2.0f, GraphicsRow3), FVector2D(ThirdButtonWidth, CompactButtonHeight), TEXT("POST"), TEXT(""), ActionFill(), FLinearColor(0.77f, 0.63f, 0.99f, 0.92f));
  this->DrawPanelText(
      TEXT("Tap to change the active visual tier."),
      GraphicsX + 18.0f,
      GraphicsRow3 + CompactButtonHeight + 14.0f,
      FLinearColor(0.84f, 0.89f, 0.94f, 1.0f),
      0.82f);
}

void AProjectAirDefenseBattleHud::NotifyHitBoxClick(FName BoxName) {
  Super::NotifyHitBoxClick(BoxName);
  this->HandleTouchAction(BoxName);
}

AProjectAirDefenseBattleManager* AProjectAirDefenseBattleHud::FindBattleManager() const {
  UWorld* World = this->GetWorld();
  if (World == nullptr) {
    return nullptr;
  }
  for (TActorIterator<AProjectAirDefenseBattleManager> It(World); It; ++It) {
    return *It;
  }
  return nullptr;
}

AProjectAirDefensePlayerController* AProjectAirDefenseBattleHud::FindPlayerController() const {
  UWorld* World = this->GetWorld();
  if (World == nullptr) {
    return nullptr;
  }
  return Cast<AProjectAirDefensePlayerController>(World->GetFirstPlayerController());
}

AProjectAirDefenseCityCameraPawn* AProjectAirDefenseBattleHud::FindCameraPawn() const {
  AProjectAirDefensePlayerController* PlayerController = this->FindPlayerController();
  if (PlayerController == nullptr) {
    return nullptr;
  }
  return Cast<AProjectAirDefenseCityCameraPawn>(PlayerController->GetPawn());
}

void AProjectAirDefenseBattleHud::HandleTouchAction(FName BoxName) {
  AProjectAirDefenseBattleManager* BattleManager = this->FindBattleManager();
  AProjectAirDefensePlayerController* PlayerController = this->FindPlayerController();
  AProjectAirDefenseCityCameraPawn* CameraPawn = this->FindCameraPawn();

  if (BoxName == HitBoxName(TEXT("Systems")) && PlayerController != nullptr) {
    PlayerController->SetSystemsMenuVisible(!PlayerController->IsSystemsMenuVisible());
    return;
  }
  if ((BoxName == HitBoxName(TEXT("WaveAction")) || BoxName == HitBoxName(TEXT("DrawerWaveAction"))) &&
      BattleManager != nullptr) {
    BattleManager->StartNextWave();
    return;
  }
  if ((BoxName == HitBoxName(TEXT("Doctrine")) || BoxName == HitBoxName(TEXT("DrawerDoctrine"))) &&
      BattleManager != nullptr) {
    BattleManager->CycleDoctrine();
    return;
  }
  if (BattleManager != nullptr) {
    if (BoxName == HitBoxName(TEXT("GraphicsQualityMinus"))) {
      BattleManager->DecreaseOverallQuality();
      return;
    }
    if (BoxName == HitBoxName(TEXT("GraphicsQualityPlus"))) {
      BattleManager->IncreaseOverallQuality();
      return;
    }
    if (BoxName == HitBoxName(TEXT("GraphicsAA"))) {
      BattleManager->CycleAntiAliasingMethod();
      return;
    }
    if (BoxName == HitBoxName(TEXT("GraphicsAO"))) {
      BattleManager->ToggleAmbientOcclusion();
      return;
    }
    if (BoxName == HitBoxName(TEXT("GraphicsMotionBlur"))) {
      BattleManager->ToggleMotionBlur();
      return;
    }
    if (BoxName == HitBoxName(TEXT("GraphicsShadows"))) {
      BattleManager->CycleShadowQuality();
      return;
    }
    if (BoxName == HitBoxName(TEXT("GraphicsReflections"))) {
      BattleManager->CycleReflectionQuality();
      return;
    }
    if (BoxName == HitBoxName(TEXT("GraphicsPost"))) {
      BattleManager->CyclePostProcessingQuality();
      return;
    }
  }
  if (CameraPawn == nullptr) {
    return;
  }
  if (BoxName == HitBoxName(TEXT("CameraPanLeft"))) {
    CameraPawn->StepPan(0.0f, -PrimaryButtonStepMeters);
  } else if (BoxName == HitBoxName(TEXT("CameraPanRight"))) {
    CameraPawn->StepPan(0.0f, PrimaryButtonStepMeters);
  } else if (BoxName == HitBoxName(TEXT("CameraPanUp"))) {
    CameraPawn->StepPan(PrimaryButtonStepMeters, 0.0f);
  } else if (BoxName == HitBoxName(TEXT("CameraPanDown"))) {
    CameraPawn->StepPan(-PrimaryButtonStepMeters, 0.0f);
  } else if (BoxName == HitBoxName(TEXT("CameraYawMinus"))) {
    CameraPawn->StepYaw(-PrimaryYawStepDegrees);
  } else if (BoxName == HitBoxName(TEXT("CameraYawPlus"))) {
    CameraPawn->StepYaw(PrimaryYawStepDegrees);
  } else if (BoxName == HitBoxName(TEXT("CameraPitchMinus"))) {
    CameraPawn->StepPitch(-PrimaryPitchStepDegrees);
  } else if (BoxName == HitBoxName(TEXT("CameraPitchPlus"))) {
    CameraPawn->StepPitch(PrimaryPitchStepDegrees);
  } else if (BoxName == HitBoxName(TEXT("CameraZoomMinus"))) {
    CameraPawn->StepZoom(PrimaryZoomStepMeters);
  } else if (BoxName == HitBoxName(TEXT("CameraZoomPlus"))) {
    CameraPawn->StepZoom(-PrimaryZoomStepMeters);
  } else if (BoxName == HitBoxName(TEXT("CameraAltitudeMinus"))) {
    CameraPawn->StepAltitude(-PrimaryAltitudeStepMeters);
  } else if (BoxName == HitBoxName(TEXT("CameraAltitudePlus"))) {
    CameraPawn->StepAltitude(PrimaryAltitudeStepMeters);
  } else if (BoxName == HitBoxName(TEXT("CameraReset"))) {
    CameraPawn->ResetCamera();
  }
}

void AProjectAirDefenseBattleHud::DrawCard(
    const FVector2D& Position,
    const FVector2D& Size,
    const FLinearColor& FillColor,
    const FLinearColor& AccentColor) const {
  const_cast<AProjectAirDefenseBattleHud*>(this)->DrawRect(
      FLinearColor(0.0f, 0.0f, 0.0f, 0.22f),
      Position.X + FloatingPanelShadowOffset,
      Position.Y + FloatingPanelShadowOffset,
      Size.X,
      Size.Y);
  const_cast<AProjectAirDefenseBattleHud*>(this)->DrawRect(
      FillColor,
      Position.X,
      Position.Y,
      Size.X,
      Size.Y);
  const_cast<AProjectAirDefenseBattleHud*>(this)->DrawRect(
      AccentColor,
      Position.X,
      Position.Y,
      6.0f,
      Size.Y);
}

void AProjectAirDefenseBattleHud::DrawActionButton(
    FName BoxName,
    const FVector2D& Position,
    const FVector2D& Size,
    const FString& Label,
    const FString& SecondaryLabel,
    const FLinearColor& FillColor,
    const FLinearColor& AccentColor) const {
  this->DrawCard(Position, Size, FillColor, AccentColor);
  const_cast<AProjectAirDefenseBattleHud*>(this)->AddHitBox(Position, Size, BoxName, true, 10);
  this->DrawPanelText(Label, Position.X + 14.0f, Position.Y + 10.0f, FLinearColor::White, 0.94f);
  if (!SecondaryLabel.IsEmpty()) {
    this->DrawPanelText(
        SecondaryLabel,
        Position.X + 14.0f,
        Position.Y + Size.Y - 22.0f,
        FLinearColor(0.82f, 0.88f, 0.93f, 1.0f),
        0.72f);
  }
}

void AProjectAirDefenseBattleHud::DrawDivider(
    float X,
    float Y,
    float Width,
    const FLinearColor& Color) const {
  const_cast<AProjectAirDefenseBattleHud*>(this)->DrawRect(Color, X, Y, Width, 1.0f);
}

void AProjectAirDefenseBattleHud::DrawPanelText(
    const FString& Text,
    float X,
    float Y,
    const FLinearColor& Color,
    float Scale) const {
  if (GEngine == nullptr) {
    return;
  }
  UFont* Font = GEngine->GetMediumFont();
  const_cast<AProjectAirDefenseBattleHud*>(this)->DrawText(Text, Color, X, Y, Font, Scale, false);
}
