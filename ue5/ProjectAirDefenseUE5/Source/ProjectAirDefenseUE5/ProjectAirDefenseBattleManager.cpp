#include "ProjectAirDefenseBattleManager.h"

#include "Camera/PlayerCameraManager.h"
#include "Components/InstancedStaticMeshComponent.h"
#include "Components/SceneComponent.h"
#include "Components/StaticMeshComponent.h"
#include "Engine/StaticMesh.h"
#include "GameFramework/PlayerController.h"
#include "Materials/MaterialInterface.h"
#include "ProjectAirDefenseGameUserSettings.h"
#include "ProjectAirDefenseRuntimeSettings.h"
#include "UObject/ConstructorHelpers.h"

namespace {
constexpr double MetersToUnrealUnits = 100.0;
// Persistence tuning: longer trail and blast lifetimes make smoke plumes and
// explosion afterburns read as real ordnance instead of flashes. The per-frame
// spawn caps keep pooled draw counts bounded even on crowded waves.
constexpr double AutoBlastVisualSeconds = 2.4;
constexpr double TrailVisualSeconds = 1.9;
// Slender missile proportions: game-scaled but proportionally near PAC-3 and SCUD-B class geometry.
// Real PAC-3 is 5.2 m by 0.255 m (L/D ~= 20); real SCUD-B class threats are ~11 m by ~0.9 m (L/D ~= 12).
// Game-scale visibility requires enlarged markers at district-camera distance;
// positions and guidance stay physical, while bodies and plumes are visual telemetry.
constexpr double ThreatMarkerRadiusMeters = 1.6;
constexpr double ThreatMarkerLengthMeters = 26.0;
constexpr double InterceptorMarkerRadiusMeters = 1.0;
constexpr double InterceptorMarkerLengthMeters = 16.0;
constexpr double ThreatSmokeRadiusMeters = 0.62;
constexpr double ThreatSmokeLengthMeters = 38.0;
constexpr double InterceptorSmokeRadiusMeters = 0.46;
constexpr double InterceptorSmokeLengthMeters = 30.0;
constexpr double ThreatExhaustRadiusMeters = 0.34;
constexpr double ThreatExhaustLengthMeters = 22.0;
constexpr double InterceptorExhaustRadiusMeters = 0.26;
constexpr double InterceptorExhaustLengthMeters = 18.0;
constexpr double LaunchPlumeVisualSeconds = 1.6;
constexpr double LaunchPlumeCoreRadiusMeters = 6.0;
constexpr double LaunchPlumeSmokeRadiusMeters = 3.2;
constexpr double LaunchPlumeSmokeHalfHeightMeters = 12.0;
constexpr double LaunchPlumeCameraCullMeters = 140.0;
constexpr double BlastCoreRadiusScale = 0.28;
constexpr double HostileBlastCoreMaxRadiusMeters = 88.0;
constexpr double InterceptBlastCoreMaxRadiusMeters = 18.0;
constexpr double BlastShockwaveHalfHeightMeters = 1.6;
constexpr double HostileBlastSmokeMaxRadiusMeters = 165.0;
constexpr double InterceptBlastSmokeMaxRadiusMeters = 24.0;
constexpr double BlastDebrisMaxRadiusMeters = 120.0;
constexpr int32 BlastSmokeLobeCount = 4;
// M901 Patriot launcher station dimensions (game-scale; PAC-3 configuration).
// Real M901 trailer is approximately 10 m long and supports four canisters that each hold four PAC-3 missiles.
// HEMTT M983 cab is approximately 9.9 m by 2.6 m by 2.9 m but is represented here as a compact cab block for silhouette.
constexpr double LauncherTrailerLengthMeters = 10.5;
constexpr double LauncherTrailerWidthMeters = 3.0;
constexpr double LauncherTrailerHeightMeters = 1.2;
constexpr double LauncherCabLengthMeters = 3.2;
constexpr double LauncherCabWidthMeters = 2.6;
constexpr double LauncherCabHeightMeters = 2.8;
constexpr double LauncherCabForwardOffsetMeters = 5.4;
constexpr double LauncherCanisterLengthMeters = 6.0;
constexpr double LauncherCanisterCrossSectionMeters = 0.72;
constexpr double LauncherCanisterElevationDegrees = 20.0;
constexpr double LauncherCanisterLateralHalfSpacingMeters = 0.55;
constexpr double LauncherCanisterVerticalSpacingMeters = 0.82;
constexpr double LauncherCanisterRearBaseOffsetMeters = -4.4;
constexpr double LauncherCanisterBaseHeightMeters = 1.45;
constexpr double DistrictBeaconRadiusMeters = 7.0;
constexpr double DistrictBeaconMinHalfHeightMeters = 8.0;
constexpr double DistrictBeaconMaxHalfHeightMeters = 32.0;
constexpr int32 MaxSimulationStepsPerFrame = 4;
// Higher trail and blast pool sizes support denser smoke and longer explosion
// afterburns without exceeding mobile GPU budgets. Per-frame spawn caps keep
// worst-case frame-time spikes under control during heavy salvos.
constexpr int32 MaxTrailVisuals = 320;
constexpr int32 MaxBlastVisuals = 64;
constexpr int32 MaxLaunchPlumeVisuals = 32;
constexpr int32 MaxTrailVisualSpawnsPerFrame = 40;
constexpr int32 MaxBlastVisualSpawnsPerFrame = 10;
constexpr int32 DistrictDamageFloorCount = 12;
constexpr int32 DistrictDamageScarMaxLayers = 5;
constexpr double DistrictDamageScarMinRadiusMeters = 28.0;
constexpr double DistrictDamageScarMaxRadiusMeters = 135.0;
constexpr double DistrictDamageScarLayerHeightMeters = 0.35;
constexpr int32 MinGraphicsQualityLevel = 0;
constexpr int32 MaxGraphicsQualityLevel = 4;
constexpr EProjectAirDefenseAntiAliasingMethod AntiAliasingMethods[] = {
    EProjectAirDefenseAntiAliasingMethod::None,
    EProjectAirDefenseAntiAliasingMethod::FXAA,
    EProjectAirDefenseAntiAliasingMethod::TAA,
    EProjectAirDefenseAntiAliasingMethod::TSR,
    EProjectAirDefenseAntiAliasingMethod::SMAA,
};

int32 ClampGraphicsQualityLevel(int32 QualityLevel) {
  return FMath::Clamp(QualityLevel, MinGraphicsQualityLevel, MaxGraphicsQualityLevel);
}

void ApplyAndSaveGraphicsSettings(UProjectAirDefenseGameUserSettings& Settings) {
  Settings.ApplyNonResolutionSettings();
  Settings.SaveSettings();
}

int32 AntiAliasingMethodIndex(EProjectAirDefenseAntiAliasingMethod Method) {
  for (int32 Index = 0; Index < UE_ARRAY_COUNT(AntiAliasingMethods); ++Index) {
    if (AntiAliasingMethods[Index] == Method) {
      return Index;
    }
  }
  return 0;
}

double ClampFiniteDouble(double Value, double Fallback, double MinValue, double MaxValue) {
  const double Candidate = FMath::IsFinite(Value) ? Value : Fallback;
  return FMath::Clamp(Candidate, MinValue, MaxValue);
}

double ClampVisualDimensionMeters(double Value) {
  constexpr double MinVisualDimensionMeters = 0.01;
  constexpr double MaxVisualDimensionMeters = 10000.0;
  return ClampFiniteDouble(Value, MinVisualDimensionMeters, MinVisualDimensionMeters, MaxVisualDimensionMeters);
}

FVector ScaleSphereToRadius(double RadiusMeters) {
  const double RadiusUnrealUnits = ClampVisualDimensionMeters(RadiusMeters) * MetersToUnrealUnits;
  return FVector(RadiusUnrealUnits / 50.0);
}

FVector ScaleCylinder(double RadiusMeters, double HalfHeightMeters) {
  const double RadiusUnrealUnits = ClampVisualDimensionMeters(RadiusMeters) * MetersToUnrealUnits;
  const double HalfHeightUnrealUnits = ClampVisualDimensionMeters(HalfHeightMeters) * MetersToUnrealUnits;
  return FVector(
      RadiusUnrealUnits / 50.0,
      RadiusUnrealUnits / 50.0,
      HalfHeightUnrealUnits / 100.0);
}

FVector ScaleCone(double RadiusMeters, double LengthMeters) {
  const double RadiusUnrealUnits = ClampVisualDimensionMeters(RadiusMeters) * MetersToUnrealUnits;
  const double LengthUnrealUnits = ClampVisualDimensionMeters(LengthMeters) * MetersToUnrealUnits;
  return FVector(
      RadiusUnrealUnits / 50.0,
      RadiusUnrealUnits / 50.0,
      LengthUnrealUnits / 100.0);
}

FVector ScaleBox(double WidthMeters, double DepthMeters, double HeightMeters) {
  const double WidthUnrealUnits = ClampVisualDimensionMeters(WidthMeters) * MetersToUnrealUnits;
  const double DepthUnrealUnits = ClampVisualDimensionMeters(DepthMeters) * MetersToUnrealUnits;
  const double HeightUnrealUnits = ClampVisualDimensionMeters(HeightMeters) * MetersToUnrealUnits;
  return FVector(
      WidthUnrealUnits / 100.0,
      DepthUnrealUnits / 100.0,
      HeightUnrealUnits / 100.0);
}

FLinearColor ThreatColor(const FProjectAirDefenseThreatState& Threat) {
  switch (Threat.ThreatType) {
  case EProjectAirDefenseThreatType::Ballistic:
    return Threat.bIsTracked ? FLinearColor(1.0f, 0.42f, 0.18f, 1.0f)
                             : FLinearColor(0.9f, 0.16f, 0.1f, 1.0f);
  case EProjectAirDefenseThreatType::Glide:
    return Threat.bIsTracked ? FLinearColor(1.0f, 0.55f, 0.24f, 1.0f)
                             : FLinearColor(0.95f, 0.24f, 0.14f, 1.0f);
  case EProjectAirDefenseThreatType::Cruise:
    return Threat.bIsTracked ? FLinearColor(1.0f, 0.82f, 0.22f, 1.0f)
                             : FLinearColor(0.95f, 0.54f, 0.14f, 1.0f);
  }
  return FLinearColor(1.0f, 0.28f, 0.22f, 1.0f);
}

FLinearColor InterceptorColor() {
  return FLinearColor(0.18f, 0.92f, 1.0f, 1.0f);
}

FRotator MarkerRotationFromVelocity(const FVector& Velocity) {
  return FRotationMatrix::MakeFromZ(Velocity.GetSafeNormal()).Rotator();
}

} // namespace

AProjectAirDefenseBattleManager::AProjectAirDefenseBattleManager() {
  this->PrimaryActorTick.bCanEverTick = true;
  this->SceneRoot = CreateDefaultSubobject<USceneComponent>(TEXT("SceneRoot"));
  this->SetRootComponent(this->SceneRoot);

  static ConstructorHelpers::FObjectFinder<UStaticMesh> SphereMeshFinder(
      TEXT("/Engine/BasicShapes/Sphere.Sphere"));
  static ConstructorHelpers::FObjectFinder<UStaticMesh> CubeMeshFinder(
      TEXT("/Engine/BasicShapes/Cube.Cube"));
  static ConstructorHelpers::FObjectFinder<UStaticMesh> CylinderMeshFinder(
      TEXT("/Engine/BasicShapes/Cylinder.Cylinder"));
  static ConstructorHelpers::FObjectFinder<UStaticMesh> ConeMeshFinder(
      TEXT("/Engine/BasicShapes/Cone.Cone"));
  static ConstructorHelpers::FObjectFinder<UMaterialInterface> BasicShapeMaterialFinder(
      TEXT("/Engine/BasicShapes/BasicShapeMaterial.BasicShapeMaterial"));

  this->SphereMesh = SphereMeshFinder.Object;
  this->CubeMesh = CubeMeshFinder.Object;
  this->CylinderMesh = CylinderMeshFinder.Object;
  this->ConeMesh = ConeMeshFinder.Object;
  this->BasicShapeMaterial = BasicShapeMaterialFinder.Object;
}

void AProjectAirDefenseBattleManager::BeginPlay() {
  Super::BeginPlay();
  if (this->bBattlefieldInitialized) {
    this->RebuildSimulation();
    this->SyncStaticVisuals();
  }
}

void AProjectAirDefenseBattleManager::Tick(float DeltaSeconds) {
  Super::Tick(DeltaSeconds);

  if (!this->bBattlefieldInitialized || !this->Simulation) {
    return;
  }

  const UProjectAirDefenseRuntimeSettings* Settings = GetDefault<UProjectAirDefenseRuntimeSettings>();
  this->TimeSinceBeginPlaySeconds += DeltaSeconds;
  if (Settings != nullptr && Settings->bAutoStartFirstWave && !this->bAutoStartedFirstWave &&
      !this->Simulation->IsWaveInProgress()) {
    if (this->TimeSinceBeginPlaySeconds >= Settings->AutoStartFirstWaveDelaySeconds) {
      this->StartNextWave();
      this->bAutoStartedFirstWave = true;
    }
  }

  const double FrameDeltaSeconds = ClampFiniteDouble(DeltaSeconds, 0.0, 0.0, 0.25);
  const double FixedStepSeconds =
      Settings == nullptr
          ? 0.05
          : ClampFiniteDouble(Settings->SimulationFixedStepSeconds, 0.05, 0.01, 0.2);
  this->StepAccumulatorSeconds =
      FMath::Min(this->StepAccumulatorSeconds + FrameDeltaSeconds, FixedStepSeconds * MaxSimulationStepsPerFrame);
  this->TrailVisualSpawnsThisFrame = 0;
  this->BlastVisualSpawnsThisFrame = 0;
  while (this->StepAccumulatorSeconds >= FixedStepSeconds) {
    this->StepAccumulatorSeconds -= FixedStepSeconds;
    this->ElapsedBattleSeconds += FixedStepSeconds;
    const FProjectAirDefenseStepEvents Events = this->Simulation->Step(FixedStepSeconds);
    this->ApplyStepEvents(Events);
  }

  this->SyncDynamicVisuals();
  this->UpdateBlastVisuals(FrameDeltaSeconds);
  this->UpdateTrailVisuals(FrameDeltaSeconds);
  this->UpdateLaunchPlumeVisuals(FrameDeltaSeconds);
  this->RefreshTransientVisualInstances();
}

void AProjectAirDefenseBattleManager::InitializeBattlefield(
    const FVector& InWorldFocusPoint,
    double InTilesetRadiusMeters) {
  this->WorldFocusPoint = InWorldFocusPoint;
  this->TilesetRadiusMeters = InTilesetRadiusMeters;
  this->bBattlefieldInitialized = true;
  this->BuildDistrictCells();
  this->RebuildSimulation();
  this->SyncStaticVisuals();
}

bool AProjectAirDefenseBattleManager::StartNextWave() {
  return this->Simulation ? this->Simulation->StartNextWave() : false;
}

void AProjectAirDefenseBattleManager::CycleDoctrine() {
  if (!this->Simulation) {
    return;
  }

  EProjectAirDefenseDefenseDoctrine NextDoctrine = EProjectAirDefenseDefenseDoctrine::Disciplined;
  switch (this->Simulation->GetSettings().Doctrine) {
  case EProjectAirDefenseDefenseDoctrine::Disciplined:
    NextDoctrine = EProjectAirDefenseDefenseDoctrine::Adaptive;
    break;
  case EProjectAirDefenseDefenseDoctrine::Adaptive:
    NextDoctrine = EProjectAirDefenseDefenseDoctrine::ShieldWall;
    break;
  case EProjectAirDefenseDefenseDoctrine::ShieldWall:
    NextDoctrine = EProjectAirDefenseDefenseDoctrine::Disciplined;
    break;
  }
  this->Simulation->SetDoctrine(NextDoctrine);
}

void AProjectAirDefenseBattleManager::IncreaseOverallQuality() {
  const UProjectAirDefenseGameUserSettings* Settings =
      UProjectAirDefenseGameUserSettings::GetProjectAirDefenseGameUserSettings();
  const int32 CurrentLevel =
      Settings == nullptr || Settings->GetOverallScalabilityLevel() < 0
          ? 3
          : Settings->GetOverallScalabilityLevel();
  this->SetOverallQualityLevel(CurrentLevel + 1);
}

void AProjectAirDefenseBattleManager::DecreaseOverallQuality() {
  const UProjectAirDefenseGameUserSettings* Settings =
      UProjectAirDefenseGameUserSettings::GetProjectAirDefenseGameUserSettings();
  const int32 CurrentLevel =
      Settings == nullptr || Settings->GetOverallScalabilityLevel() < 0
          ? 3
          : Settings->GetOverallScalabilityLevel();
  this->SetOverallQualityLevel(CurrentLevel - 1);
}

void AProjectAirDefenseBattleManager::SetOverallQualityLevel(int32 QualityLevel) {
  if (UProjectAirDefenseGameUserSettings* Settings =
          UProjectAirDefenseGameUserSettings::GetProjectAirDefenseGameUserSettings()) {
    const int32 ClampedQualityLevel = ClampGraphicsQualityLevel(QualityLevel);
    const int32 CurrentLevel =
        Settings->GetOverallScalabilityLevel() < 0 ? 3 : Settings->GetOverallScalabilityLevel();
    if (CurrentLevel == ClampedQualityLevel) {
      return;
    }
    Settings->SetOverallScalabilityLevel(ClampedQualityLevel);
    ApplyAndSaveGraphicsSettings(*Settings);
  }
}

void AProjectAirDefenseBattleManager::CycleAntiAliasingMethod() {
  if (UProjectAirDefenseGameUserSettings* Settings =
          UProjectAirDefenseGameUserSettings::GetProjectAirDefenseGameUserSettings()) {
    const int32 CurrentIndex = AntiAliasingMethodIndex(Settings->GetPreferredAntiAliasingMethod());
    this->SetAntiAliasingMethod(AntiAliasingMethods[(CurrentIndex + 1) % UE_ARRAY_COUNT(AntiAliasingMethods)]);
  }
}

void AProjectAirDefenseBattleManager::SetAntiAliasingMethod(EProjectAirDefenseAntiAliasingMethod Method) {
  if (UProjectAirDefenseGameUserSettings* Settings =
          UProjectAirDefenseGameUserSettings::GetProjectAirDefenseGameUserSettings()) {
    if (Settings->GetPreferredAntiAliasingMethod() == Method) {
      return;
    }
    Settings->SetPreferredAntiAliasingMethod(Method);
    ApplyAndSaveGraphicsSettings(*Settings);
  }
}

void AProjectAirDefenseBattleManager::ToggleAmbientOcclusion() {
  if (UProjectAirDefenseGameUserSettings* Settings =
          UProjectAirDefenseGameUserSettings::GetProjectAirDefenseGameUserSettings()) {
    this->SetAmbientOcclusionEnabled(!Settings->IsAmbientOcclusionEnabled());
  }
}

void AProjectAirDefenseBattleManager::SetAmbientOcclusionEnabled(bool bEnabled) {
  if (UProjectAirDefenseGameUserSettings* Settings =
          UProjectAirDefenseGameUserSettings::GetProjectAirDefenseGameUserSettings()) {
    if (Settings->IsAmbientOcclusionEnabled() == bEnabled) {
      return;
    }
    Settings->SetAmbientOcclusionEnabled(bEnabled);
    ApplyAndSaveGraphicsSettings(*Settings);
  }
}

void AProjectAirDefenseBattleManager::ToggleMotionBlur() {
  if (UProjectAirDefenseGameUserSettings* Settings =
          UProjectAirDefenseGameUserSettings::GetProjectAirDefenseGameUserSettings()) {
    this->SetMotionBlurEnabled(!Settings->IsMotionBlurEnabled());
  }
}

void AProjectAirDefenseBattleManager::SetMotionBlurEnabled(bool bEnabled) {
  if (UProjectAirDefenseGameUserSettings* Settings =
          UProjectAirDefenseGameUserSettings::GetProjectAirDefenseGameUserSettings()) {
    if (Settings->IsMotionBlurEnabled() == bEnabled) {
      return;
    }
    Settings->SetMotionBlurEnabled(bEnabled);
    ApplyAndSaveGraphicsSettings(*Settings);
  }
}

void AProjectAirDefenseBattleManager::ToggleRayTracing() {
  if (UProjectAirDefenseGameUserSettings* Settings =
          UProjectAirDefenseGameUserSettings::GetProjectAirDefenseGameUserSettings()) {
    this->SetRayTracingEnabled(!Settings->IsRayTracingEnabled());
  }
}

void AProjectAirDefenseBattleManager::SetRayTracingEnabled(bool bEnabled) {
  if (UProjectAirDefenseGameUserSettings* Settings =
          UProjectAirDefenseGameUserSettings::GetProjectAirDefenseGameUserSettings()) {
    if (Settings->IsRayTracingEnabled() == bEnabled) {
      return;
    }
    Settings->SetRayTracingEnabled(bEnabled);
    ApplyAndSaveGraphicsSettings(*Settings);
  }
}

void AProjectAirDefenseBattleManager::CycleShadowQuality() {
  if (UProjectAirDefenseGameUserSettings* Settings =
          UProjectAirDefenseGameUserSettings::GetProjectAirDefenseGameUserSettings()) {
    this->SetShadowQualityLevel((Settings->GetShadowQuality() + 1) % (MaxGraphicsQualityLevel + 1));
  }
}

void AProjectAirDefenseBattleManager::SetShadowQualityLevel(int32 QualityLevel) {
  if (UProjectAirDefenseGameUserSettings* Settings =
          UProjectAirDefenseGameUserSettings::GetProjectAirDefenseGameUserSettings()) {
    const int32 ClampedQualityLevel = ClampGraphicsQualityLevel(QualityLevel);
    if (Settings->GetShadowQuality() == ClampedQualityLevel) {
      return;
    }
    Settings->SetShadowQuality(ClampedQualityLevel);
    ApplyAndSaveGraphicsSettings(*Settings);
  }
}

void AProjectAirDefenseBattleManager::CycleReflectionQuality() {
  if (UProjectAirDefenseGameUserSettings* Settings =
          UProjectAirDefenseGameUserSettings::GetProjectAirDefenseGameUserSettings()) {
    this->SetReflectionQualityLevel((Settings->GetReflectionQuality() + 1) % (MaxGraphicsQualityLevel + 1));
  }
}

void AProjectAirDefenseBattleManager::SetReflectionQualityLevel(int32 QualityLevel) {
  if (UProjectAirDefenseGameUserSettings* Settings =
          UProjectAirDefenseGameUserSettings::GetProjectAirDefenseGameUserSettings()) {
    const int32 ClampedQualityLevel = ClampGraphicsQualityLevel(QualityLevel);
    if (Settings->GetReflectionQuality() == ClampedQualityLevel) {
      return;
    }
    Settings->SetReflectionQuality(ClampedQualityLevel);
    ApplyAndSaveGraphicsSettings(*Settings);
  }
}

void AProjectAirDefenseBattleManager::CyclePostProcessingQuality() {
  if (UProjectAirDefenseGameUserSettings* Settings =
          UProjectAirDefenseGameUserSettings::GetProjectAirDefenseGameUserSettings()) {
    this->SetPostProcessingQualityLevel((Settings->GetPostProcessingQuality() + 1) % (MaxGraphicsQualityLevel + 1));
  }
}

void AProjectAirDefenseBattleManager::SetPostProcessingQualityLevel(int32 QualityLevel) {
  if (UProjectAirDefenseGameUserSettings* Settings =
          UProjectAirDefenseGameUserSettings::GetProjectAirDefenseGameUserSettings()) {
    const int32 ClampedQualityLevel = ClampGraphicsQualityLevel(QualityLevel);
    if (Settings->GetPostProcessingQuality() == ClampedQualityLevel) {
      return;
    }
    Settings->SetPostProcessingQuality(ClampedQualityLevel);
    ApplyAndSaveGraphicsSettings(*Settings);
  }
}

bool AProjectAirDefenseBattleManager::IsBattlefieldInitialized() const {
  return this->bBattlefieldInitialized && static_cast<bool>(this->Simulation);
}

FProjectAirDefenseRuntimeSnapshot AProjectAirDefenseBattleManager::BuildRuntimeSnapshot() const {
  return this->Simulation ? this->Simulation->BuildRuntimeSnapshot()
                                    : FProjectAirDefenseRuntimeSnapshot{};
}

FProjectAirDefenseRadarSnapshot AProjectAirDefenseBattleManager::BuildRadarSnapshot() const {
  FProjectAirDefenseRadarSnapshot Snapshot;
  Snapshot.ExtentMeters = FMath::Max(this->TilesetRadiusMeters * 0.5, 1.0);

  auto UpdateExtent = [&Snapshot](const FVector2d& PositionMeters, double RadiusMeters = 0.0) {
    Snapshot.ExtentMeters =
        FMath::Max(Snapshot.ExtentMeters, PositionMeters.Length() + RadiusMeters + 180.0);
  };

  const TArray<FProjectAirDefenseDistrictCell>& RadarDistrictCells =
      this->Simulation ? this->Simulation->GetDistrictCells() : this->DistrictCells;
  for (const FProjectAirDefenseDistrictCell& DistrictCell : RadarDistrictCells) {
    FProjectAirDefenseRadarDistrictSnapshot DistrictSnapshot;
    DistrictSnapshot.Id = DistrictCell.Id;
    DistrictSnapshot.LocalPositionMeters =
        FVector2d(DistrictCell.LocalPositionMeters.X, DistrictCell.LocalPositionMeters.Y);
    DistrictSnapshot.RadiusMeters = DistrictCell.RadiusMeters;
    DistrictSnapshot.Integrity = DistrictCell.Integrity;
    DistrictSnapshot.StructuralIntegrity = DistrictCell.StructuralIntegrity;
    DistrictSnapshot.DamagedFloors = DistrictCell.DamagedFloors;
    DistrictSnapshot.CollapsedFloors = DistrictCell.CollapsedFloors;
    DistrictSnapshot.bCollapsed = DistrictCell.bCollapsed;
    Snapshot.Districts.Add(DistrictSnapshot);
    UpdateExtent(DistrictSnapshot.LocalPositionMeters, DistrictSnapshot.RadiusMeters);
  }

  for (const FVector3d& LauncherPosition : this->LauncherPositionsMeters) {
    FProjectAirDefenseRadarLauncherSnapshot LauncherSnapshot;
    LauncherSnapshot.LocalPositionMeters = FVector2d(LauncherPosition.X, LauncherPosition.Y);
    Snapshot.Launchers.Add(LauncherSnapshot);
    UpdateExtent(LauncherSnapshot.LocalPositionMeters);
  }

  if (this->Simulation) {
    for (const FProjectAirDefenseThreatState& Threat : this->Simulation->GetThreats()) {
      FProjectAirDefenseRadarThreatSnapshot ThreatSnapshot;
      ThreatSnapshot.LocalPositionMeters = FVector2d(Threat.PositionMeters.X, Threat.PositionMeters.Y);
      ThreatSnapshot.ThreatType = Threat.ThreatType;
      ThreatSnapshot.bIsTracked = Threat.bIsTracked;
      Snapshot.Threats.Add(ThreatSnapshot);
      UpdateExtent(ThreatSnapshot.LocalPositionMeters);
    }
  }

  Snapshot.ExtentMeters = FMath::Max(Snapshot.ExtentMeters, 1400.0);
  return Snapshot;
}

FString AProjectAirDefenseBattleManager::BuildGraphicsSummaryText() const {
  if (const UProjectAirDefenseGameUserSettings* Settings =
          UProjectAirDefenseGameUserSettings::GetProjectAirDefenseGameUserSettings()) {
    FString AALabel = TEXT("TSR");
    const int32 OverallLevel = Settings->GetOverallScalabilityLevel();
    FString OverallLabel = TEXT("AUTO");
    switch (OverallLevel) {
    case 0:
      OverallLabel = TEXT("LOW");
      break;
    case 1:
      OverallLabel = TEXT("MED");
      break;
    case 2:
      OverallLabel = TEXT("HIGH");
      break;
    case 3:
      OverallLabel = TEXT("EPIC");
      break;
    case 4:
      OverallLabel = TEXT("CINE");
      break;
    default:
      break;
    }
    switch (Settings->GetPreferredAntiAliasingMethod()) {
    case EProjectAirDefenseAntiAliasingMethod::None:
      AALabel = TEXT("NONE");
      break;
    case EProjectAirDefenseAntiAliasingMethod::FXAA:
      AALabel = TEXT("FXAA");
      break;
    case EProjectAirDefenseAntiAliasingMethod::TAA:
      AALabel = TEXT("TAA");
      break;
    case EProjectAirDefenseAntiAliasingMethod::TSR:
      AALabel = TEXT("TSR");
      break;
    case EProjectAirDefenseAntiAliasingMethod::SMAA:
      AALabel = TEXT("SMAA");
      break;
    }
    return FString::Printf(
        TEXT("GFX %s | %s | AO %s"),
        *OverallLabel,
        *AALabel,
        Settings->IsAmbientOcclusionEnabled() ? TEXT("ON") : TEXT("OFF"));
  }
  return TEXT("GFX AUTO | AA ?");
}

void AProjectAirDefenseBattleManager::RebuildSimulation() {
  if (!this->bBattlefieldInitialized) {
    return;
  }
  FProjectAirDefenseDefenseSettings Settings;
  this->Simulation = MakeUnique<FProjectAirDefenseBattleSimulation>(
      this->DistrictCells,
      this->LauncherPositionsMeters,
      Settings,
      4242);
  for (const TPair<FString, TObjectPtr<UStaticMeshComponent>>& Entry : this->ThreatVisuals) {
    if (Entry.Value != nullptr) {
      Entry.Value->DestroyComponent();
    }
  }
  for (const TPair<FString, TObjectPtr<UStaticMeshComponent>>& Entry : this->InterceptorVisuals) {
    if (Entry.Value != nullptr) {
      Entry.Value->DestroyComponent();
    }
  }
  this->ThreatVisuals.Empty();
  this->InterceptorVisuals.Empty();
  this->LastThreatWorldPositions.Empty();
  this->LastInterceptorWorldPositions.Empty();
  this->BlastVisuals.Empty();
  this->TrailVisuals.Empty();
  this->LaunchPlumeVisuals.Empty();
  this->RefreshTransientVisualInstances();
  this->ElapsedBattleSeconds = 0.0;
  this->StepAccumulatorSeconds = 0.0;
  this->TrailVisualSpawnsThisFrame = 0;
  this->BlastVisualSpawnsThisFrame = 0;
}

void AProjectAirDefenseBattleManager::BuildDistrictCells() {
  const UProjectAirDefenseRuntimeSettings* Settings = GetDefault<UProjectAirDefenseRuntimeSettings>();
  const FProjectAirDefenseDistrictRuntimeConfig DistrictConfig =
      BuildProjectAirDefenseDistrictRuntimeConfig(
          MakeProjectAirDefenseDistrictRuntimeInputs(Settings),
          this->TilesetRadiusMeters);

  this->DistrictCells.Empty();
  int32 Ordinal = 0;
  for (int32 YIndex = -1; YIndex <= 1; ++YIndex) {
    for (int32 XIndex = -1; XIndex <= 1; ++XIndex) {
      FProjectAirDefenseDistrictCell Cell;
      Cell.Id = FString::Printf(TEXT("D-%02d"), ++Ordinal);
      Cell.LocalPositionMeters =
          FVector3d(
              static_cast<double>(XIndex) * DistrictConfig.CellStepMeters,
              static_cast<double>(YIndex) * DistrictConfig.CellStepMeters,
              0.0);
      Cell.RadiusMeters = DistrictConfig.CellRadiusMeters;
      Cell.Integrity = 100.0;
      this->DistrictCells.Add(Cell);
    }
  }

  this->LauncherPositionsMeters = {
      FVector3d(-DistrictConfig.LauncherLateralSpacingMeters, -DistrictConfig.LauncherRingDistanceMeters, 0.0),
      FVector3d(0.0, -DistrictConfig.LauncherRingDistanceMeters - 120.0, 0.0),
      FVector3d(DistrictConfig.LauncherLateralSpacingMeters, -DistrictConfig.LauncherRingDistanceMeters, 0.0),
  };
}

void AProjectAirDefenseBattleManager::SyncStaticVisuals() {
  for (UStaticMeshComponent* Component : this->DistrictVisuals) {
    if (Component != nullptr) {
      Component->DestroyComponent();
    }
  }
  for (UStaticMeshComponent* Component : this->LauncherVisuals) {
    if (Component != nullptr) {
      Component->DestroyComponent();
    }
  }
  for (UStaticMeshComponent* Component : this->DistrictStatusVisuals) {
    if (Component != nullptr) {
      Component->DestroyComponent();
    }
  }
  auto DestroyInstancedComponent = [](TObjectPtr<UInstancedStaticMeshComponent>& Component) {
    if (Component != nullptr) {
      Component->DestroyComponent();
      Component = nullptr;
    }
  };
  DestroyInstancedComponent(this->DamagedDistrictFloorInstances);
  DestroyInstancedComponent(this->HostileTrailInstances);
  DestroyInstancedComponent(this->InterceptorTrailInstances);
  DestroyInstancedComponent(this->HostileExhaustInstances);
  DestroyInstancedComponent(this->InterceptorExhaustInstances);
  DestroyInstancedComponent(this->LaunchPlumeCoreInstances);
  DestroyInstancedComponent(this->LaunchPlumeSmokeInstances);
  DestroyInstancedComponent(this->HostileBlastCoreInstances);
  DestroyInstancedComponent(this->InterceptBlastCoreInstances);
  DestroyInstancedComponent(this->HostileBlastShockwaveInstances);
  DestroyInstancedComponent(this->InterceptBlastShockwaveInstances);
  DestroyInstancedComponent(this->HostileBlastSmokeInstances);
  DestroyInstancedComponent(this->InterceptBlastSmokeInstances);
  DestroyInstancedComponent(this->HostileBlastDebrisInstances);
  this->DistrictVisuals.Empty();
  this->LauncherVisuals.Empty();
  this->DistrictStatusVisuals.Empty();
  this->DamagedDistrictFloorInstances =
      this->CreateInstancedMarker(TEXT("DistrictFloors-Damaged"), this->CubeMesh, FLinearColor(0.92f, 0.30f, 0.12f, 1.0f));
  this->HostileTrailInstances =
      this->CreateInstancedMarker(TEXT("TrailSmoke-Hot"), this->CylinderMesh != nullptr ? this->CylinderMesh : this->SphereMesh, FLinearColor(0.86f, 0.84f, 0.78f, 1.0f));
  this->InterceptorTrailInstances =
      this->CreateInstancedMarker(TEXT("TrailSmoke-Interceptor"), this->CylinderMesh != nullptr ? this->CylinderMesh : this->SphereMesh, FLinearColor(0.88f, 0.91f, 0.88f, 1.0f));
  this->HostileExhaustInstances =
      this->CreateInstancedMarker(TEXT("Exhaust-Hot"), this->CylinderMesh != nullptr ? this->CylinderMesh : this->SphereMesh, FLinearColor(1.0f, 0.70f, 0.24f, 1.0f));
  this->InterceptorExhaustInstances =
      this->CreateInstancedMarker(TEXT("Exhaust-Interceptor"), this->CylinderMesh != nullptr ? this->CylinderMesh : this->SphereMesh, FLinearColor(1.0f, 0.94f, 0.62f, 1.0f));
  this->LaunchPlumeCoreInstances =
      this->CreateInstancedMarker(TEXT("LaunchPlume-Core"), this->SphereMesh, FLinearColor(1.0f, 0.58f, 0.16f, 1.0f));
  this->LaunchPlumeSmokeInstances =
      this->CreateInstancedMarker(
          TEXT("LaunchPlume-Smoke"),
          this->CylinderMesh != nullptr ? this->CylinderMesh : this->SphereMesh,
          FLinearColor(0.78f, 0.76f, 0.68f, 1.0f));
  this->HostileBlastCoreInstances =
      this->CreateInstancedMarker(TEXT("BlastCore-Hot"), this->SphereMesh, FLinearColor(1.0f, 0.48f, 0.12f, 1.0f));
  this->InterceptBlastCoreInstances =
      this->CreateInstancedMarker(TEXT("BlastCore-Interceptor"), this->SphereMesh, FLinearColor(1.0f, 0.82f, 0.38f, 1.0f));
  this->HostileBlastShockwaveInstances =
      this->CreateInstancedMarker(TEXT("BlastShockwave-Hot"), this->CylinderMesh != nullptr ? this->CylinderMesh : this->SphereMesh, FLinearColor(1.0f, 0.48f, 0.12f, 1.0f));
  this->InterceptBlastShockwaveInstances =
      this->CreateInstancedMarker(TEXT("BlastShockwave-Interceptor"), this->CylinderMesh != nullptr ? this->CylinderMesh : this->SphereMesh, FLinearColor(0.86f, 0.86f, 0.80f, 1.0f));
  this->HostileBlastSmokeInstances =
      this->CreateInstancedMarker(TEXT("BlastSmoke-Hot"), this->SphereMesh, FLinearColor(0.62f, 0.55f, 0.46f, 1.0f));
  this->InterceptBlastSmokeInstances =
      this->CreateInstancedMarker(TEXT("BlastSmoke-Interceptor"), this->SphereMesh, FLinearColor(0.72f, 0.72f, 0.66f, 1.0f));
  this->HostileBlastDebrisInstances =
      this->CreateInstancedMarker(TEXT("BlastDebris-Hot"), this->CubeMesh, FLinearColor(0.34f, 0.24f, 0.17f, 1.0f));

  const FLinearColor PatriotOliveColor(0.32f, 0.38f, 0.22f, 1.0f);
  const FLinearColor PatriotDarkOliveColor(0.22f, 0.26f, 0.15f, 1.0f);
  const FLinearColor PatriotCanisterColor(0.25f, 0.30f, 0.18f, 1.0f);

  // Build each launching station as a composite of engine-primitive cubes so
  // the silhouette reads as a recognizable M901 (trailer plus cab plus four
  // elevated PAC-3 canisters in a 2x2 cluster). District beacons remain
  // suppressed to avoid synthetic city geometry over the real mesh.
  if (this->CubeMesh != nullptr) {
    const double ElevationRadians = FMath::DegreesToRadians(LauncherCanisterElevationDegrees);
    const FVector CanisterForward(
        0.0, -FMath::Cos(ElevationRadians), FMath::Sin(ElevationRadians));
    const FRotator CanisterRotation = FRotationMatrix::MakeFromZ(CanisterForward).Rotator();
    const double CanisterHalfLengthMeters = LauncherCanisterLengthMeters * 0.5;

    auto AttachLauncherPart = [this](
                                  int32 LauncherIndex,
                                  const FString& Suffix,
                                  const FVector& LocalOffsetMeters,
                                  const FVector& Scale,
                                  const FRotator& Rotation,
                                  const FLinearColor& Color,
                                  const FVector& WorldBase) {
      const FString Name =
          FString::Printf(TEXT("Launcher-%d-%s"), LauncherIndex, *Suffix);
      const FVector WorldOffset(
          LocalOffsetMeters.X * MetersToUnrealUnits,
          LocalOffsetMeters.Y * MetersToUnrealUnits,
          LocalOffsetMeters.Z * MetersToUnrealUnits);
      UStaticMeshComponent* Component =
          this->CreateStaticMarker(Name, this->CubeMesh, WorldBase + WorldOffset, Scale, Color);
      if (Component != nullptr) {
        Component->SetWorldRotation(Rotation);
        this->LauncherVisuals.Add(Component);
      }
    };

    for (int32 LauncherIndex = 0;
         LauncherIndex < this->LauncherPositionsMeters.Num();
         ++LauncherIndex) {
      const FVector WorldBase = this->ToWorldPosition(this->LauncherPositionsMeters[LauncherIndex]);

      AttachLauncherPart(
          LauncherIndex,
          TEXT("Trailer"),
          FVector(0.0, 0.0, LauncherTrailerHeightMeters * 0.5),
          ScaleBox(LauncherTrailerWidthMeters, LauncherTrailerLengthMeters, LauncherTrailerHeightMeters),
          FRotator::ZeroRotator,
          PatriotOliveColor,
          WorldBase);

      AttachLauncherPart(
          LauncherIndex,
          TEXT("Cab"),
          FVector(0.0, LauncherCabForwardOffsetMeters, LauncherCabHeightMeters * 0.5),
          ScaleBox(LauncherCabWidthMeters, LauncherCabLengthMeters, LauncherCabHeightMeters),
          FRotator::ZeroRotator,
          PatriotDarkOliveColor,
          WorldBase);

      for (int32 Row = 0; Row < 2; ++Row) {
        for (int32 Col = 0; Col < 2; ++Col) {
          const double BaseX =
              (Col == 0 ? -LauncherCanisterLateralHalfSpacingMeters : LauncherCanisterLateralHalfSpacingMeters);
          const double BaseY = LauncherCanisterRearBaseOffsetMeters;
          const double BaseZ =
              LauncherCanisterBaseHeightMeters + static_cast<double>(Row) * LauncherCanisterVerticalSpacingMeters;
          const FVector BaseLocalMeters(BaseX, BaseY, BaseZ);
          const FVector CenterLocalMeters =
              BaseLocalMeters + CanisterForward * CanisterHalfLengthMeters;
          const FString Suffix = FString::Printf(TEXT("Canister-%d%d"), Row, Col);
          AttachLauncherPart(
              LauncherIndex,
              Suffix,
              CenterLocalMeters,
              ScaleBox(
                  LauncherCanisterCrossSectionMeters,
                  LauncherCanisterCrossSectionMeters,
                  LauncherCanisterLengthMeters),
              CanisterRotation,
              PatriotCanisterColor,
              WorldBase);
        }
      }
    }
  }
}

void AProjectAirDefenseBattleManager::SyncDynamicVisuals() {
  if (!this->Simulation) {
    return;
  }

  // Sub-step visual interpolation: the simulation advances in fixed steps
  // (typically 50 ms), but the renderer can run at 120 Hz or higher on modern
  // smartphones. Extrapolate each marker forward by the residual accumulator
  // time so motion stays fluid at any refresh rate instead of snapping at
  // simulation-step boundaries.
  const double VisualLeadSeconds = FMath::Max(this->StepAccumulatorSeconds, 0.0);
  TSet<FString> LiveThreatIds;
  for (const FProjectAirDefenseThreatState& Threat : this->Simulation->GetThreats()) {
    LiveThreatIds.Add(Threat.Id);
    const FLinearColor ThreatVisualColor = ThreatColor(Threat);
    UStaticMeshComponent* ThreatMarker =
        this->EnsureDynamicMarker(
            this->ThreatVisuals,
            Threat.Id,
            this->ConeMesh != nullptr ? this->ConeMesh : this->SphereMesh,
            ThreatVisualColor);
    if (ThreatMarker == nullptr) {
      continue;
    }
    this->ApplyColor(ThreatMarker, ThreatVisualColor);
    const FVector3d InterpolatedPosition =
        Threat.PositionMeters + Threat.VelocityMetersPerSecond * VisualLeadSeconds;
    const FVector WorldPosition = this->ToWorldPosition(InterpolatedPosition);
    ThreatMarker->SetWorldLocation(WorldPosition);
    ThreatMarker->SetWorldScale3D(
        this->ConeMesh != nullptr
            ? ScaleCone(ThreatMarkerRadiusMeters, ThreatMarkerLengthMeters)
            : ScaleSphereToRadius(ThreatMarkerRadiusMeters));
    ThreatMarker->SetWorldRotation(
        MarkerRotationFromVelocity(FVector(Threat.VelocityMetersPerSecond)));
    this->LastThreatWorldPositions.Add(Threat.Id, WorldPosition);
  }
  this->RemoveStaleMarkers(this->ThreatVisuals, this->LastThreatWorldPositions, LiveThreatIds);

  TSet<FString> LiveInterceptorIds;
  for (const FProjectAirDefenseInterceptorState& Interceptor : this->Simulation->GetInterceptors()) {
    LiveInterceptorIds.Add(Interceptor.Id);
    UStaticMeshComponent* InterceptorMarker =
        this->EnsureDynamicMarker(
            this->InterceptorVisuals,
            Interceptor.Id,
            this->ConeMesh != nullptr ? this->ConeMesh : this->SphereMesh,
            InterceptorColor());
    if (InterceptorMarker == nullptr) {
      continue;
    }
    const FVector3d InterpolatedInterceptorPosition =
        Interceptor.PositionMeters + Interceptor.VelocityMetersPerSecond * VisualLeadSeconds;
    const FVector WorldPosition = this->ToWorldPosition(InterpolatedInterceptorPosition);
    InterceptorMarker->SetWorldLocation(WorldPosition);
    InterceptorMarker->SetWorldScale3D(
        this->ConeMesh != nullptr
            ? ScaleCone(InterceptorMarkerRadiusMeters, InterceptorMarkerLengthMeters)
            : ScaleSphereToRadius(InterceptorMarkerRadiusMeters));
    InterceptorMarker->SetWorldRotation(
        MarkerRotationFromVelocity(FVector(Interceptor.VelocityMetersPerSecond)));
    this->LastInterceptorWorldPositions.Add(Interceptor.Id, WorldPosition);
  }
  this->RemoveStaleMarkers(
      this->InterceptorVisuals,
      this->LastInterceptorWorldPositions,
      LiveInterceptorIds);

  const TArray<FProjectAirDefenseDistrictCell>& LiveDistrictCells = this->Simulation->GetDistrictCells();
  for (int32 Index = 0; Index < LiveDistrictCells.Num() && Index < this->DistrictStatusVisuals.Num(); ++Index) {
    UStaticMeshComponent* DistrictMarker = this->DistrictStatusVisuals[Index];
    if (DistrictMarker == nullptr) {
      continue;
    }
    const FProjectAirDefenseDistrictCell& DistrictCell = LiveDistrictCells[Index];
    const double IntegrityRatio = FMath::Clamp(DistrictCell.Integrity / 100.0, 0.0, 1.0);
    const double HalfHeightMeters =
        FMath::Lerp(DistrictBeaconMinHalfHeightMeters, DistrictBeaconMaxHalfHeightMeters, IntegrityRatio);
    DistrictMarker->SetWorldLocation(
        this->ToWorldPosition(DistrictCell.LocalPositionMeters + FVector3d(0.0, 0.0, HalfHeightMeters)));
    DistrictMarker->SetWorldScale3D(
        this->CylinderMesh != nullptr
            ? ScaleCylinder(DistrictBeaconRadiusMeters, HalfHeightMeters)
            : ScaleSphereToRadius(DistrictBeaconRadiusMeters));
    this->ApplyColor(DistrictMarker, this->DistrictColorForIntegrity(DistrictCell.Integrity));
  }
  this->SyncDistrictDamageFloorVisuals(LiveDistrictCells);
}

void AProjectAirDefenseBattleManager::SyncDistrictDamageFloorVisuals(
    const TArray<FProjectAirDefenseDistrictCell>& LiveDistrictCells) {
  if (this->DamagedDistrictFloorInstances != nullptr) {
    this->DamagedDistrictFloorInstances->ClearInstances();
  }
  for (const FProjectAirDefenseDistrictCell& DistrictCell : LiveDistrictCells) {
    const int32 CollapsedFloors =
        FMath::Clamp(DistrictCell.CollapsedFloors, 0, DistrictDamageFloorCount);
    const int32 DamagedFloors =
        FMath::Clamp(DistrictCell.DamagedFloors, 0, DistrictDamageFloorCount);
    if (DamagedFloors <= 0 && CollapsedFloors <= 0) {
      continue;
    }
    if (this->DamagedDistrictFloorInstances == nullptr) {
      continue;
    }

    const double DamageSeverity =
        FMath::Clamp(
            static_cast<double>(DamagedFloors + CollapsedFloors) /
                static_cast<double>(DistrictDamageFloorCount),
            0.0,
            1.0);
    const int32 ScarLayers =
        FMath::Clamp(
            1 + FMath::CeilToInt(DamageSeverity * static_cast<double>(DistrictDamageScarMaxLayers - 1)),
            1,
            DistrictDamageScarMaxLayers);
    const double BaseScarRadiusMeters =
        FMath::Clamp(
            FMath::Lerp(DistrictDamageScarMinRadiusMeters, DistrictDamageScarMaxRadiusMeters, DamageSeverity),
            DistrictDamageScarMinRadiusMeters,
            DistrictCell.RadiusMeters * 0.72);
    const FVector3d ScarEpicenterMeters =
        DistrictCell.bHasDamageEpicenter ? DistrictCell.LastDamageEpicenterMeters : DistrictCell.LocalPositionMeters;
    const FRotator ScarRotation(
        0.0,
        static_cast<double>(GetTypeHash(DistrictCell.Id) % 90),
        0.0);

    for (int32 LayerIndex = 0; LayerIndex < ScarLayers; ++LayerIndex) {
      const double LayerAlpha =
          ScarLayers <= 1 ? 0.0 : static_cast<double>(LayerIndex) / static_cast<double>(ScarLayers - 1);
      const double LayerRadiusMeters =
          BaseScarRadiusMeters * FMath::Lerp(1.0, 0.56, LayerAlpha);
      const FVector ScarLocation =
          this->ToWorldPosition(
              ScarEpicenterMeters +
              FVector3d(0.0, 0.0, 0.12 + static_cast<double>(LayerIndex) * DistrictDamageScarLayerHeightMeters));
      const FVector ScarScale =
          ScaleBox(
              LayerRadiusMeters,
              LayerRadiusMeters * FMath::Lerp(0.78, 0.52, LayerAlpha),
              DistrictDamageScarLayerHeightMeters);
      this->DamagedDistrictFloorInstances->AddInstance(
          FTransform(ScarRotation, ScarLocation, ScarScale),
          true);
    }
  }
}

void AProjectAirDefenseBattleManager::UpdateBlastVisuals(double DeltaSeconds) {
  int32 BlastIndex = 0;
  while (BlastIndex < this->BlastVisuals.Num()) {
    FBlastVisual& BlastVisual = this->BlastVisuals[BlastIndex];
    BlastVisual.AgeSeconds += DeltaSeconds;
    const double Alpha =
        BlastVisual.LifetimeSeconds <= 0.0 ? 1.0 : BlastVisual.AgeSeconds / BlastVisual.LifetimeSeconds;
    if (Alpha >= 1.0) {
      this->BlastVisuals.RemoveAt(BlastIndex);
      continue;
    }
    ++BlastIndex;
  }
}

void AProjectAirDefenseBattleManager::UpdateTrailVisuals(double DeltaSeconds) {
  int32 TrailIndex = 0;
  while (TrailIndex < this->TrailVisuals.Num()) {
    FTrailVisual& TrailVisual = this->TrailVisuals[TrailIndex];
    TrailVisual.AgeSeconds += DeltaSeconds;
    const double Alpha =
        TrailVisual.LifetimeSeconds <= 0.0 ? 1.0 : TrailVisual.AgeSeconds / TrailVisual.LifetimeSeconds;
    if (Alpha >= 1.0) {
      this->TrailVisuals.RemoveAt(TrailIndex);
      continue;
    }
    ++TrailIndex;
  }
}

void AProjectAirDefenseBattleManager::UpdateLaunchPlumeVisuals(double DeltaSeconds) {
  int32 PlumeIndex = 0;
  while (PlumeIndex < this->LaunchPlumeVisuals.Num()) {
    FLaunchPlumeVisual& PlumeVisual = this->LaunchPlumeVisuals[PlumeIndex];
    PlumeVisual.AgeSeconds += DeltaSeconds;
    const double Alpha =
        PlumeVisual.LifetimeSeconds <= 0.0 ? 1.0 : PlumeVisual.AgeSeconds / PlumeVisual.LifetimeSeconds;
    if (Alpha >= 1.0) {
      this->LaunchPlumeVisuals.RemoveAt(PlumeIndex);
      continue;
    }
    ++PlumeIndex;
  }
}

void AProjectAirDefenseBattleManager::RefreshTransientVisualInstances() {
  auto ClearInstances = [](TObjectPtr<UInstancedStaticMeshComponent>& Instances) {
    if (Instances != nullptr) {
      Instances->ClearInstances();
    }
  };
  ClearInstances(this->HostileTrailInstances);
  ClearInstances(this->InterceptorTrailInstances);
  ClearInstances(this->HostileExhaustInstances);
  ClearInstances(this->InterceptorExhaustInstances);
  ClearInstances(this->LaunchPlumeCoreInstances);
  ClearInstances(this->LaunchPlumeSmokeInstances);
  ClearInstances(this->HostileBlastCoreInstances);
  ClearInstances(this->InterceptBlastCoreInstances);
  ClearInstances(this->HostileBlastShockwaveInstances);
  ClearInstances(this->InterceptBlastShockwaveInstances);
  ClearInstances(this->HostileBlastSmokeInstances);
  ClearInstances(this->InterceptBlastSmokeInstances);
  ClearInstances(this->HostileBlastDebrisInstances);

  for (const FTrailVisual& TrailVisual : this->TrailVisuals) {
    UInstancedStaticMeshComponent* TargetInstances =
        TrailVisual.bHostile ? this->HostileTrailInstances.Get() : this->InterceptorTrailInstances.Get();
    if (TargetInstances == nullptr) {
      continue;
    }
    const double Alpha =
        TrailVisual.LifetimeSeconds <= 0.0 ? 1.0 : TrailVisual.AgeSeconds / TrailVisual.LifetimeSeconds;
    TargetInstances->AddInstance(
        FTransform(
            TrailVisual.Rotation,
            TrailVisual.WorldPosition,
            TrailVisual.BaseScale * FMath::Lerp(0.92, 1.08, Alpha)),
        true);
    UInstancedStaticMeshComponent* ExhaustInstances =
        TrailVisual.bHostile ? this->HostileExhaustInstances.Get() : this->InterceptorExhaustInstances.Get();
    if (ExhaustInstances != nullptr && Alpha < 0.58) {
      const double ExhaustFade = FMath::Clamp(Alpha / 0.58, 0.0, 1.0);
      const FVector ExhaustScale =
          TrailVisual.ExhaustScale *
          FVector(
              FMath::Lerp(1.0, 0.45, ExhaustFade),
              FMath::Lerp(1.0, 0.45, ExhaustFade),
              FMath::Lerp(1.0, 0.70, ExhaustFade));
      ExhaustInstances->AddInstance(
          FTransform(
              TrailVisual.Rotation,
              TrailVisual.WorldPosition,
              ExhaustScale),
          true);
    }
  }

  bool bHasCameraLocation = false;
  FVector CameraLocation = FVector::ZeroVector;
  if (UWorld* World = this->GetWorld()) {
    if (APlayerController* PlayerController = World->GetFirstPlayerController()) {
      if (PlayerController->PlayerCameraManager != nullptr) {
        CameraLocation = PlayerController->PlayerCameraManager->GetCameraLocation();
        bHasCameraLocation = true;
      }
    }
  }

  for (const FLaunchPlumeVisual& PlumeVisual : this->LaunchPlumeVisuals) {
    if (bHasCameraLocation &&
        FVector::DistSquared(CameraLocation, PlumeVisual.WorldPosition) <
            FMath::Square(LaunchPlumeCameraCullMeters * MetersToUnrealUnits)) {
      continue;
    }
    const double Alpha =
        PlumeVisual.LifetimeSeconds <= 0.0 ? 1.0 : PlumeVisual.AgeSeconds / PlumeVisual.LifetimeSeconds;
    if (this->LaunchPlumeCoreInstances != nullptr && Alpha < 0.34) {
      const double CoreFade = FMath::Clamp(Alpha / 0.34, 0.0, 1.0);
      this->LaunchPlumeCoreInstances->AddInstance(
          FTransform(
              FRotator::ZeroRotator,
              PlumeVisual.WorldPosition + FVector(0.0, 0.0, 8.0 * MetersToUnrealUnits),
              PlumeVisual.CoreScale * FMath::Lerp(1.0, 0.35, CoreFade)),
          true);
    }
    if (this->LaunchPlumeSmokeInstances != nullptr) {
      const double SmokeEase = 1.0 - FMath::Square(1.0 - Alpha);
      for (int32 LobeIndex = 0; LobeIndex < 4; ++LobeIndex) {
        const double LobeAngle =
            static_cast<double>(LobeIndex) * 0.5 * UE_DOUBLE_PI + 0.25;
        const double LobeDistanceMeters =
            LobeIndex == 0 ? 0.0 : FMath::Lerp(4.0, 14.0, SmokeEase);
        const FVector LobeOffset(
            FMath::Cos(LobeAngle) * LobeDistanceMeters * MetersToUnrealUnits,
            FMath::Sin(LobeAngle) * LobeDistanceMeters * MetersToUnrealUnits,
            FMath::Lerp(2.0, 18.0, SmokeEase) * MetersToUnrealUnits);
        const double LobeScale =
            (LobeIndex == 0 ? 0.74 : 0.42) * FMath::Lerp(0.26, 0.82, SmokeEase);
        this->LaunchPlumeSmokeInstances->AddInstance(
            FTransform(
                FRotator::ZeroRotator,
                PlumeVisual.WorldPosition + LobeOffset,
                PlumeVisual.SmokeScale * LobeScale),
            true);
      }
    }
  }

  for (const FBlastVisual& BlastVisual : this->BlastVisuals) {
    const bool bHostile = BlastVisual.Kind == EProjectAirDefenseBlastKind::HostileImpact;
    UInstancedStaticMeshComponent* CoreInstances =
        bHostile ? this->HostileBlastCoreInstances.Get() : this->InterceptBlastCoreInstances.Get();
    UInstancedStaticMeshComponent* ShockwaveInstances =
        bHostile ? this->HostileBlastShockwaveInstances.Get() : this->InterceptBlastShockwaveInstances.Get();
    const double Alpha =
        BlastVisual.LifetimeSeconds <= 0.0 ? 1.0 : BlastVisual.AgeSeconds / BlastVisual.LifetimeSeconds;
    if (CoreInstances != nullptr) {
      CoreInstances->AddInstance(
          FTransform(
              FRotator::ZeroRotator,
              BlastVisual.WorldPosition,
              BlastVisual.CoreScale * FMath::Lerp(0.35, 1.0, Alpha)),
          true);
    }
    if (ShockwaveInstances != nullptr && bHostile) {
      ShockwaveInstances->AddInstance(
          FTransform(
              FRotator::ZeroRotator,
              BlastVisual.WorldPosition,
              FVector(
                  BlastVisual.ShockwaveScale.X * FMath::Lerp(0.2, 1.0, Alpha),
                  BlastVisual.ShockwaveScale.Y * FMath::Lerp(0.2, 1.0, Alpha),
                  BlastVisual.ShockwaveScale.Z)),
          true);
    }
    UInstancedStaticMeshComponent* SmokeInstances =
        bHostile ? this->HostileBlastSmokeInstances.Get() : this->InterceptBlastSmokeInstances.Get();
    if (SmokeInstances != nullptr) {
      const double SmokeEase = 1.0 - FMath::Square(1.0 - Alpha);
      const FVector SmokeCorePosition =
          BlastVisual.WorldPosition +
          FVector(0.0, 0.0, BlastVisual.SmokeRiseMeters * MetersToUnrealUnits * SmokeEase);
      const double SpreadMeters =
          bHostile ? FMath::Lerp(8.0, BlastVisual.bGroundImpact ? 48.0 : 28.0, SmokeEase)
                   : FMath::Lerp(3.0, 10.0, SmokeEase);
      const int32 SmokeLobeCount = bHostile ? BlastSmokeLobeCount : 2;
      const double SmokeScaleEnd = bHostile ? 1.28 : 0.42;
      const double SmokeScaleStart = bHostile ? 0.35 : 0.18;
      for (int32 LobeIndex = 0; LobeIndex < SmokeLobeCount; ++LobeIndex) {
        const double LobeAlpha =
            static_cast<double>(LobeIndex) / static_cast<double>(SmokeLobeCount);
        const double LobeAngle = LobeAlpha * 2.0 * UE_DOUBLE_PI + (bHostile ? 0.35 : 1.05);
        const double LobeOffsetMeters =
            LobeIndex == 0 ? 0.0 : SpreadMeters * FMath::Lerp(0.55, 1.0, LobeAlpha);
        const FVector LobeOffset(
            FMath::Cos(LobeAngle) * LobeOffsetMeters * MetersToUnrealUnits,
            FMath::Sin(LobeAngle) * LobeOffsetMeters * MetersToUnrealUnits,
            -static_cast<double>(LobeIndex) * 2.5 * MetersToUnrealUnits * SmokeEase);
        const double LobeScale =
            (LobeIndex == 0 ? 1.0 : FMath::Lerp(0.54, 0.76, LobeAlpha)) *
            FMath::Lerp(SmokeScaleStart, SmokeScaleEnd, SmokeEase);
        SmokeInstances->AddInstance(
            FTransform(
                FRotator::ZeroRotator,
                SmokeCorePosition + LobeOffset,
                BlastVisual.SmokeScale * LobeScale),
            true);
      }
    }
    if (bHostile && BlastVisual.bGroundImpact && this->HostileBlastDebrisInstances != nullptr) {
      this->HostileBlastDebrisInstances->AddInstance(
          FTransform(
              FRotator(0.0, FMath::Fmod(Alpha * 90.0, 90.0), 0.0),
              BlastVisual.WorldPosition,
              BlastVisual.DebrisScale * FMath::Lerp(0.85, 1.10, Alpha)),
          true);
    }
  }
}

void AProjectAirDefenseBattleManager::ApplyStepEvents(const FProjectAirDefenseStepEvents& Events) {
  for (const FProjectAirDefenseTrailEvent& TrailEvent : Events.TrailEvents) {
    this->SpawnTrailVisual(TrailEvent);
  }
  for (const FProjectAirDefenseInterceptorLaunchEvent& LaunchEvent : Events.LaunchedInterceptors) {
    FProjectAirDefenseTrailEvent LaunchTrailEvent;
    LaunchTrailEvent.PositionMeters = LaunchEvent.LauncherPositionMeters;
    LaunchTrailEvent.VelocityMetersPerSecond = FVector3d(0.0, 0.0, 1.0);
    LaunchTrailEvent.bHostile = false;
    this->SpawnTrailVisual(LaunchTrailEvent);
    this->SpawnLaunchPlumeVisual(LaunchEvent.LauncherPositionMeters);
  }
  for (const FProjectAirDefenseBlastEvent& BlastEvent : Events.BlastEvents) {
    this->SpawnBlastVisual(BlastEvent);
  }
}

void AProjectAirDefenseBattleManager::SpawnTrailVisual(const FProjectAirDefenseTrailEvent& TrailEvent) {
  if (this->TrailVisualSpawnsThisFrame >= MaxTrailVisualSpawnsPerFrame) {
    return;
  }
  while (this->TrailVisuals.Num() >= MaxTrailVisuals && !this->TrailVisuals.IsEmpty()) {
    this->TrailVisuals.RemoveAt(0);
  }

  const double SmokeRadiusMeters =
      TrailEvent.bHostile ? ThreatSmokeRadiusMeters : InterceptorSmokeRadiusMeters;
  const double SmokeHalfLengthMeters =
      (TrailEvent.bHostile ? ThreatSmokeLengthMeters : InterceptorSmokeLengthMeters) * 0.5;
  const double ExhaustRadiusMeters =
      TrailEvent.bHostile ? ThreatExhaustRadiusMeters : InterceptorExhaustRadiusMeters;
  const double ExhaustHalfLengthMeters =
      (TrailEvent.bHostile ? ThreatExhaustLengthMeters : InterceptorExhaustLengthMeters) * 0.5;
  const FVector TrailVelocity(TrailEvent.VelocityMetersPerSecond);
  const FVector TrailDirection =
      TrailVelocity.IsNearlyZero() ? FVector::UpVector : TrailVelocity.GetSafeNormal();
  ++this->TrailVisualSpawnsThisFrame;

  FTrailVisual TrailVisual;
  TrailVisual.WorldPosition = this->ToWorldPosition(TrailEvent.PositionMeters);
  TrailVisual.BaseScale =
      this->CylinderMesh != nullptr
          ? ScaleCylinder(SmokeRadiusMeters, SmokeHalfLengthMeters)
          : ScaleSphereToRadius(SmokeRadiusMeters);
  TrailVisual.ExhaustScale =
      this->CylinderMesh != nullptr
          ? ScaleCylinder(ExhaustRadiusMeters, ExhaustHalfLengthMeters)
          : ScaleSphereToRadius(ExhaustRadiusMeters);
  TrailVisual.Rotation = MarkerRotationFromVelocity(TrailDirection);
  TrailVisual.LifetimeSeconds = TrailVisualSeconds;
  TrailVisual.bHostile = TrailEvent.bHostile;
  this->TrailVisuals.Add(TrailVisual);
}

void AProjectAirDefenseBattleManager::SpawnLaunchPlumeVisual(const FVector3d& PositionMeters) {
  while (this->LaunchPlumeVisuals.Num() >= MaxLaunchPlumeVisuals && !this->LaunchPlumeVisuals.IsEmpty()) {
    this->LaunchPlumeVisuals.RemoveAt(0);
  }

  FLaunchPlumeVisual PlumeVisual;
  PlumeVisual.WorldPosition = this->ToWorldPosition(PositionMeters);
  PlumeVisual.CoreScale = ScaleSphereToRadius(LaunchPlumeCoreRadiusMeters);
  PlumeVisual.SmokeScale =
      this->CylinderMesh != nullptr
          ? ScaleCylinder(LaunchPlumeSmokeRadiusMeters, LaunchPlumeSmokeHalfHeightMeters)
          : ScaleSphereToRadius(LaunchPlumeSmokeRadiusMeters);
  PlumeVisual.LifetimeSeconds = LaunchPlumeVisualSeconds;
  this->LaunchPlumeVisuals.Add(PlumeVisual);
}

void AProjectAirDefenseBattleManager::SpawnBlastVisual(const FProjectAirDefenseBlastEvent& BlastEvent) {
  if (this->BlastVisualSpawnsThisFrame >= MaxBlastVisualSpawnsPerFrame) {
    return;
  }
  while (this->BlastVisuals.Num() >= MaxBlastVisuals && !this->BlastVisuals.IsEmpty()) {
    this->BlastVisuals.RemoveAt(0);
  }

  const FVector WorldPosition = this->ToWorldPosition(BlastEvent.PositionMeters);
  const bool bHostile = BlastEvent.Kind == EProjectAirDefenseBlastKind::HostileImpact;
  const double GroundCoupling = FMath::Clamp(BlastEvent.GroundCoupling, 0.0, 1.0);
  const double CoreRadiusMeters =
      FMath::Min(
          BlastEvent.RadiusMeters * (bHostile ? BlastCoreRadiusScale : BlastCoreRadiusScale * 0.12),
          bHostile ? HostileBlastCoreMaxRadiusMeters : InterceptBlastCoreMaxRadiusMeters);
  const double SmokeRadiusMeters =
      FMath::Min(
          BlastEvent.RadiusMeters * (bHostile ? FMath::Lerp(0.28, 0.72, GroundCoupling) : 0.12),
          bHostile ? HostileBlastSmokeMaxRadiusMeters : InterceptBlastSmokeMaxRadiusMeters);
  const double DebrisRadiusMeters =
      FMath::Min(
          BlastEvent.RadiusMeters * FMath::Lerp(0.18, 0.54, GroundCoupling),
          BlastDebrisMaxRadiusMeters);
  ++this->BlastVisualSpawnsThisFrame;

  FBlastVisual BlastVisual;
  BlastVisual.WorldPosition = WorldPosition;
  BlastVisual.CoreScale = ScaleSphereToRadius(CoreRadiusMeters);
  BlastVisual.ShockwaveScale = this->CylinderMesh != nullptr
                                   ? ScaleCylinder(BlastEvent.RadiusMeters * (bHostile ? 1.0 : 0.20), BlastShockwaveHalfHeightMeters)
                                   : ScaleSphereToRadius(BlastEvent.RadiusMeters * (bHostile ? 1.0 : 0.20));
  BlastVisual.SmokeScale = ScaleSphereToRadius(SmokeRadiusMeters);
  BlastVisual.DebrisScale =
      ScaleBox(DebrisRadiusMeters, DebrisRadiusMeters * 0.72, FMath::Lerp(2.0, 7.0, GroundCoupling));
  BlastVisual.LifetimeSeconds =
      bHostile ? FMath::Lerp(AutoBlastVisualSeconds, 5.4, GroundCoupling) : 1.45;
  BlastVisual.SmokeRiseMeters = bHostile ? FMath::Lerp(18.0, 95.0, GroundCoupling) : 10.0;
  BlastVisual.Kind = BlastEvent.Kind;
  BlastVisual.bGroundImpact = BlastEvent.bGroundImpact || GroundCoupling > 0.65;
  this->BlastVisuals.Add(BlastVisual);
}

UStaticMeshComponent* AProjectAirDefenseBattleManager::CreateStaticMarker(
    const FString& DebugName,
    UStaticMesh* Mesh,
    const FVector& WorldLocation,
    const FVector& WorldScale,
    const FLinearColor& Color) {
  if (Mesh == nullptr) {
    return nullptr;
  }

  UStaticMeshComponent* MeshComponent =
      NewObject<UStaticMeshComponent>(this, *DebugName);
  MeshComponent->SetStaticMesh(Mesh);
  MeshComponent->SetCollisionEnabled(ECollisionEnabled::NoCollision);
  MeshComponent->SetMobility(EComponentMobility::Movable);
  MeshComponent->SetupAttachment(this->SceneRoot);
  MeshComponent->RegisterComponent();
  MeshComponent->SetWorldLocation(WorldLocation);
  MeshComponent->SetWorldScale3D(WorldScale);
  if (this->BasicShapeMaterial != nullptr) {
    MeshComponent->SetMaterial(0, this->BasicShapeMaterial);
  }
  this->ApplyColor(MeshComponent, Color);
  this->AddInstanceComponent(MeshComponent);
  return MeshComponent;
}

UInstancedStaticMeshComponent* AProjectAirDefenseBattleManager::CreateInstancedMarker(
    const FString& DebugName,
    UStaticMesh* Mesh,
    const FLinearColor& Color) {
  if (Mesh == nullptr) {
    return nullptr;
  }

  UInstancedStaticMeshComponent* MeshComponent =
      NewObject<UInstancedStaticMeshComponent>(this, *DebugName);
  MeshComponent->SetStaticMesh(Mesh);
  MeshComponent->SetCollisionEnabled(ECollisionEnabled::NoCollision);
  MeshComponent->SetMobility(EComponentMobility::Movable);
  MeshComponent->SetupAttachment(this->SceneRoot);
  MeshComponent->RegisterComponent();
  if (this->BasicShapeMaterial != nullptr) {
    MeshComponent->SetMaterial(0, this->BasicShapeMaterial);
  }
  this->ApplyColor(MeshComponent, Color);
  this->AddInstanceComponent(MeshComponent);
  return MeshComponent;
}

UStaticMeshComponent* AProjectAirDefenseBattleManager::EnsureDynamicMarker(
    TMap<FString, TObjectPtr<UStaticMeshComponent>>& MarkerMap,
    const FString& MarkerId,
    UStaticMesh* Mesh,
    const FLinearColor& Color) {
  if (TObjectPtr<UStaticMeshComponent>* ExistingComponent = MarkerMap.Find(MarkerId)) {
    return ExistingComponent->Get();
  }

  UStaticMeshComponent* MeshComponent =
      this->CreateStaticMarker(MarkerId, Mesh, FVector::ZeroVector, FVector::OneVector, Color);
  MarkerMap.Add(MarkerId, MeshComponent);
  return MeshComponent;
}

void AProjectAirDefenseBattleManager::RemoveStaleMarkers(
    TMap<FString, TObjectPtr<UStaticMeshComponent>>& MarkerMap,
    TMap<FString, FVector>& LastPositions,
    const TSet<FString>& LiveIds) {
  TArray<FString> MarkerIds;
  MarkerMap.GetKeys(MarkerIds);
  for (const FString& MarkerId : MarkerIds) {
    if (LiveIds.Contains(MarkerId)) {
      continue;
    }
    if (TObjectPtr<UStaticMeshComponent>* MeshComponent = MarkerMap.Find(MarkerId)) {
      if (*MeshComponent != nullptr) {
        (*MeshComponent)->DestroyComponent();
      }
    }
    MarkerMap.Remove(MarkerId);
    LastPositions.Remove(MarkerId);
  }
}

void AProjectAirDefenseBattleManager::ApplyColor(
    UStaticMeshComponent* MeshComponent,
    const FLinearColor& Color) const {
  if (MeshComponent == nullptr) {
    return;
  }
  MeshComponent->SetVectorParameterValueOnMaterials(TEXT("Color"), FVector(Color.R, Color.G, Color.B));
}

FVector AProjectAirDefenseBattleManager::ToWorldPosition(const FVector3d& LocalMeters) const {
  return this->WorldFocusPoint +
         FVector(
             LocalMeters.X * MetersToUnrealUnits,
             LocalMeters.Y * MetersToUnrealUnits,
             LocalMeters.Z * MetersToUnrealUnits);
}

FLinearColor AProjectAirDefenseBattleManager::DistrictColorForIntegrity(double Integrity) const {
  const float HealthRatio = static_cast<float>(FMath::Clamp(Integrity / 100.0, 0.0, 1.0));
  return FLinearColor(
      FMath::Lerp(1.0f, 0.16f, HealthRatio),
      FMath::Lerp(0.18f, 0.75f, HealthRatio),
      FMath::Lerp(0.12f, 1.0f, HealthRatio),
      1.0f);
}
