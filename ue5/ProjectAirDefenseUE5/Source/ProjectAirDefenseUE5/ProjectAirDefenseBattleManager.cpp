#include "ProjectAirDefenseBattleManager.h"

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
constexpr double TrailVisualSeconds = 2.8;
// Slender missile proportions: game-scaled but proportionally near PAC-3 and SCUD-B class geometry.
// Real PAC-3 is 5.2 m by 0.255 m (L/D ~= 20); real SCUD-B class threats are ~11 m by ~0.9 m (L/D ~= 12).
// Game-scale visibility requires a modest size boost, so these values oversize length and diameter
// by roughly 1.5-2x while preserving a recognizable slender silhouette.
constexpr double ThreatMarkerRadiusMeters = 0.9;
constexpr double ThreatMarkerLengthMeters = 14.0;
constexpr double InterceptorMarkerRadiusMeters = 0.55;
constexpr double InterceptorMarkerLengthMeters = 9.0;
constexpr double ThreatTrailRadiusMeters = 3.0;
constexpr double InterceptorTrailRadiusMeters = 2.0;
constexpr double BlastCoreRadiusScale = 0.28;
constexpr double BlastCoreMaxRadiusMeters = 88.0;
constexpr double BlastShockwaveHalfHeightMeters = 1.6;
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
constexpr int32 MaxTrailVisualSpawnsPerFrame = 40;
constexpr int32 MaxBlastVisualSpawnsPerFrame = 10;
constexpr int32 DistrictDamageFloorCount = 12;
constexpr double DistrictDamageTowerOffsetXMeters = 46.0;
constexpr double DistrictDamageTowerOffsetYMeters = -42.0;
constexpr double DistrictDamageFloorWidthMeters = 42.0;
constexpr double DistrictDamageFloorDepthMeters = 38.0;
constexpr double DistrictDamageFloorHeightMeters = 2.0;
constexpr double DistrictDamageFloorStepMeters = 3.35;
constexpr double DistrictDamageFloorBaseHeightMeters = 3.0;
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

FVector ScaleSphereToRadius(double RadiusMeters) {
  const double RadiusUnrealUnits = RadiusMeters * MetersToUnrealUnits;
  return FVector(RadiusUnrealUnits / 50.0);
}

FVector ScaleCylinder(double RadiusMeters, double HalfHeightMeters) {
  return FVector(
      (RadiusMeters * MetersToUnrealUnits) / 50.0,
      (RadiusMeters * MetersToUnrealUnits) / 50.0,
      (HalfHeightMeters * MetersToUnrealUnits) / 100.0);
}

FVector ScaleCone(double RadiusMeters, double LengthMeters) {
  return FVector(
      (RadiusMeters * MetersToUnrealUnits) / 50.0,
      (RadiusMeters * MetersToUnrealUnits) / 50.0,
      (LengthMeters * MetersToUnrealUnits) / 100.0);
}

FVector ScaleBox(double WidthMeters, double DepthMeters, double HeightMeters) {
  return FVector(
      (WidthMeters * MetersToUnrealUnits) / 100.0,
      (DepthMeters * MetersToUnrealUnits) / 100.0,
      (HeightMeters * MetersToUnrealUnits) / 100.0);
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

  const double FixedStepSeconds =
      Settings == nullptr ? 0.05 : FMath::Max(Settings->SimulationFixedStepSeconds, 0.01);
  this->StepAccumulatorSeconds =
      FMath::Min(this->StepAccumulatorSeconds + DeltaSeconds, FixedStepSeconds * MaxSimulationStepsPerFrame);
  this->TrailVisualSpawnsThisFrame = 0;
  this->BlastVisualSpawnsThisFrame = 0;
  while (this->StepAccumulatorSeconds >= FixedStepSeconds) {
    this->StepAccumulatorSeconds -= FixedStepSeconds;
    this->ElapsedBattleSeconds += FixedStepSeconds;
    const FProjectAirDefenseStepEvents Events = this->Simulation->Step(FixedStepSeconds);
    this->ApplyStepEvents(Events);
  }

  this->SyncDynamicVisuals();
  this->UpdateBlastVisuals(DeltaSeconds);
  this->UpdateTrailVisuals(DeltaSeconds);
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
        TEXT("%s  %s  AO %s  RT %s  SH %d  RF %d  PP %d"),
        *OverallLabel,
        *AALabel,
        Settings->IsAmbientOcclusionEnabled() ? TEXT("ON") : TEXT("OFF"),
        Settings->IsRayTracingEnabled() ? TEXT("REQ") : TEXT("OFF"),
        Settings->GetShadowQuality(),
        Settings->GetReflectionQuality(),
        Settings->GetPostProcessingQuality());
  }
  return TEXT("AUTO  AA ?  AO ?  RT ?  SH ?  RF ?  PP ?");
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
  this->RefreshTransientVisualInstances();
  this->ElapsedBattleSeconds = 0.0;
  this->StepAccumulatorSeconds = 0.0;
  this->TrailVisualSpawnsThisFrame = 0;
  this->BlastVisualSpawnsThisFrame = 0;
}

void AProjectAirDefenseBattleManager::BuildDistrictCells() {
  const UProjectAirDefenseRuntimeSettings* Settings = GetDefault<UProjectAirDefenseRuntimeSettings>();
  const double DefaultHalfExtentMeters = Settings == nullptr ? 1050.0 : Settings->DistrictHalfExtentMeters;
  const double HalfExtentMeters = this->TilesetRadiusMeters > 0.0
                                      ? FMath::Clamp(this->TilesetRadiusMeters * 0.42, 700.0, DefaultHalfExtentMeters)
                                      : DefaultHalfExtentMeters;
  const double CellStepMeters = HalfExtentMeters * 0.9;
  const double CellRadiusMeters = Settings == nullptr ? 180.0 : Settings->DistrictCellRadiusMeters;
  const double LauncherRingDistanceMeters =
      Settings == nullptr ? 1450.0 : Settings->LauncherRingDistanceMeters;
  const double LauncherLateralSpacingMeters =
      Settings == nullptr ? 850.0 : Settings->LauncherLateralSpacingMeters;

  this->DistrictCells.Empty();
  int32 Ordinal = 0;
  for (int32 YIndex = -1; YIndex <= 1; ++YIndex) {
    for (int32 XIndex = -1; XIndex <= 1; ++XIndex) {
      FProjectAirDefenseDistrictCell Cell;
      Cell.Id = FString::Printf(TEXT("D-%02d"), ++Ordinal);
      Cell.LocalPositionMeters =
          FVector3d(static_cast<double>(XIndex) * CellStepMeters, static_cast<double>(YIndex) * CellStepMeters, 0.0);
      Cell.RadiusMeters = CellRadiusMeters;
      Cell.Integrity = 100.0;
      this->DistrictCells.Add(Cell);
    }
  }

  this->LauncherPositionsMeters = {
      FVector3d(-LauncherLateralSpacingMeters, -LauncherRingDistanceMeters, 0.0),
      FVector3d(0.0, -LauncherRingDistanceMeters - 120.0, 0.0),
      FVector3d(LauncherLateralSpacingMeters, -LauncherRingDistanceMeters, 0.0),
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
  DestroyInstancedComponent(this->HostileBlastCoreInstances);
  DestroyInstancedComponent(this->InterceptBlastCoreInstances);
  DestroyInstancedComponent(this->HostileBlastShockwaveInstances);
  DestroyInstancedComponent(this->InterceptBlastShockwaveInstances);
  this->DistrictVisuals.Empty();
  this->LauncherVisuals.Empty();
  this->DistrictStatusVisuals.Empty();
  this->DamagedDistrictFloorInstances =
      this->CreateInstancedMarker(TEXT("DistrictFloors-Damaged"), this->CubeMesh, FLinearColor(0.92f, 0.30f, 0.12f, 1.0f));
  this->HostileTrailInstances =
      this->CreateInstancedMarker(TEXT("Trail-Hot"), this->SphereMesh, FLinearColor(1.0f, 0.34f, 0.12f, 1.0f));
  this->InterceptorTrailInstances =
      this->CreateInstancedMarker(TEXT("Trail-Interceptor"), this->SphereMesh, FLinearColor(0.22f, 0.95f, 1.0f, 1.0f));
  this->HostileBlastCoreInstances =
      this->CreateInstancedMarker(TEXT("BlastCore-Hot"), this->SphereMesh, FLinearColor(1.0f, 0.48f, 0.12f, 1.0f));
  this->InterceptBlastCoreInstances =
      this->CreateInstancedMarker(TEXT("BlastCore-Interceptor"), this->SphereMesh, FLinearColor(0.22f, 0.95f, 1.0f, 1.0f));
  this->HostileBlastShockwaveInstances =
      this->CreateInstancedMarker(TEXT("BlastShockwave-Hot"), this->CylinderMesh != nullptr ? this->CylinderMesh : this->SphereMesh, FLinearColor(1.0f, 0.48f, 0.12f, 1.0f));
  this->InterceptBlastShockwaveInstances =
      this->CreateInstancedMarker(TEXT("BlastShockwave-Interceptor"), this->CylinderMesh != nullptr ? this->CylinderMesh : this->SphereMesh, FLinearColor(0.22f, 0.95f, 1.0f, 1.0f));

  const FLinearColor PatriotOliveColor(0.20f, 0.24f, 0.14f, 1.0f);
  const FLinearColor PatriotDarkOliveColor(0.12f, 0.14f, 0.09f, 1.0f);
  const FLinearColor PatriotCanisterColor(0.15f, 0.18f, 0.11f, 1.0f);

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
    const int32 FirstCollapsedFloor = DistrictDamageFloorCount - CollapsedFloors;
    const int32 FirstDamagedFloor = DistrictDamageFloorCount - DamagedFloors;
    for (int32 FloorIndex = 0; FloorIndex < DistrictDamageFloorCount; ++FloorIndex) {
      const bool bCollapsedFloor = FloorIndex >= FirstCollapsedFloor;
      if (bCollapsedFloor) {
        continue;
      }

      const bool bDamagedFloor = FloorIndex >= FirstDamagedFloor;
      if (!bDamagedFloor || this->DamagedDistrictFloorInstances == nullptr) {
        continue;
      }
      const double FloorCenterHeightMeters =
          DistrictDamageFloorBaseHeightMeters + static_cast<double>(FloorIndex) * DistrictDamageFloorStepMeters;
      const FVector FloorLocation =
          this->ToWorldPosition(
              DistrictCell.LocalPositionMeters +
              FVector3d(
                  DistrictDamageTowerOffsetXMeters,
                  DistrictDamageTowerOffsetYMeters,
                  FloorCenterHeightMeters));
      const FVector FloorScale =
          ScaleBox(
              DistrictDamageFloorWidthMeters,
              DistrictDamageFloorDepthMeters,
              bDamagedFloor ? DistrictDamageFloorHeightMeters * 0.72 : DistrictDamageFloorHeightMeters);
      this->DamagedDistrictFloorInstances->AddInstance(FTransform(FRotator::ZeroRotator, FloorLocation, FloorScale), true);
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

void AProjectAirDefenseBattleManager::RefreshTransientVisualInstances() {
  auto ClearInstances = [](TObjectPtr<UInstancedStaticMeshComponent>& Instances) {
    if (Instances != nullptr) {
      Instances->ClearInstances();
    }
  };
  ClearInstances(this->HostileTrailInstances);
  ClearInstances(this->InterceptorTrailInstances);
  ClearInstances(this->HostileBlastCoreInstances);
  ClearInstances(this->InterceptBlastCoreInstances);
  ClearInstances(this->HostileBlastShockwaveInstances);
  ClearInstances(this->InterceptBlastShockwaveInstances);

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
            FRotator::ZeroRotator,
            TrailVisual.WorldPosition,
            TrailVisual.BaseScale * FMath::Lerp(1.0, 0.18, Alpha)),
        true);
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
    if (ShockwaveInstances != nullptr) {
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
  }
}

void AProjectAirDefenseBattleManager::ApplyStepEvents(const FProjectAirDefenseStepEvents& Events) {
  for (const FProjectAirDefenseTrailEvent& TrailEvent : Events.TrailEvents) {
    this->SpawnTrailVisual(TrailEvent);
  }
  for (const FProjectAirDefenseInterceptorLaunchEvent& LaunchEvent : Events.LaunchedInterceptors) {
    FProjectAirDefenseTrailEvent LaunchTrailEvent;
    LaunchTrailEvent.PositionMeters = LaunchEvent.LauncherPositionMeters;
    LaunchTrailEvent.bHostile = false;
    this->SpawnTrailVisual(LaunchTrailEvent);
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

  const double RadiusMeters = TrailEvent.bHostile ? ThreatTrailRadiusMeters : InterceptorTrailRadiusMeters;
  ++this->TrailVisualSpawnsThisFrame;

  FTrailVisual TrailVisual;
  TrailVisual.WorldPosition = this->ToWorldPosition(TrailEvent.PositionMeters);
  TrailVisual.BaseScale = ScaleSphereToRadius(RadiusMeters);
  TrailVisual.LifetimeSeconds = TrailVisualSeconds;
  TrailVisual.bHostile = TrailEvent.bHostile;
  this->TrailVisuals.Add(TrailVisual);
}

void AProjectAirDefenseBattleManager::SpawnBlastVisual(const FProjectAirDefenseBlastEvent& BlastEvent) {
  if (this->BlastVisualSpawnsThisFrame >= MaxBlastVisualSpawnsPerFrame) {
    return;
  }
  while (this->BlastVisuals.Num() >= MaxBlastVisuals && !this->BlastVisuals.IsEmpty()) {
    this->BlastVisuals.RemoveAt(0);
  }

  const FVector WorldPosition = this->ToWorldPosition(BlastEvent.PositionMeters);
  const double CoreRadiusMeters =
      FMath::Min(BlastEvent.RadiusMeters * BlastCoreRadiusScale, BlastCoreMaxRadiusMeters);
  ++this->BlastVisualSpawnsThisFrame;

  FBlastVisual BlastVisual;
  BlastVisual.WorldPosition = WorldPosition;
  BlastVisual.CoreScale = ScaleSphereToRadius(CoreRadiusMeters);
  BlastVisual.ShockwaveScale = this->CylinderMesh != nullptr
                                   ? ScaleCylinder(BlastEvent.RadiusMeters, BlastShockwaveHalfHeightMeters)
                                   : ScaleSphereToRadius(BlastEvent.RadiusMeters);
  BlastVisual.LifetimeSeconds = AutoBlastVisualSeconds;
  BlastVisual.Kind = BlastEvent.Kind;
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
