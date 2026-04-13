#include "ProjectAirDefenseBattleManager.h"

#include "Components/StaticMeshComponent.h"
#include "DrawDebugHelpers.h"
#include "Engine/StaticMesh.h"
#include "GameFramework/PlayerController.h"
#include "Materials/MaterialInterface.h"
#include "ProjectAirDefenseGameUserSettings.h"
#include "ProjectAirDefenseRuntimeSettings.h"
#include "UObject/ConstructorHelpers.h"

namespace {
constexpr double MetersToUnrealUnits = 100.0;
constexpr double AutoBlastVisualSeconds = 0.85;
constexpr double TrailVisualSeconds = 1.25;
constexpr double ThreatMarkerRadiusMeters = 5.5;
constexpr double ThreatMarkerLengthMeters = 30.0;
constexpr double InterceptorMarkerRadiusMeters = 3.8;
constexpr double InterceptorMarkerLengthMeters = 18.0;
constexpr double LauncherMarkerRadiusMeters = 22.0;
constexpr double LauncherMarkerHalfHeightMeters = 7.5;
constexpr double DistrictRingElevationMeters = 10.0;
constexpr double DistrictStatusStemMeters = 18.0;

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

FLinearColor ThreatColor() {
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
  this->StepAccumulatorSeconds += DeltaSeconds;
  while (this->StepAccumulatorSeconds >= FixedStepSeconds) {
    this->StepAccumulatorSeconds -= FixedStepSeconds;
    this->ElapsedBattleSeconds += FixedStepSeconds;
    const FProjectAirDefenseStepEvents Events = this->Simulation->Step(FixedStepSeconds);
    this->ApplyStepEvents(Events);
  }

  this->SyncDynamicVisuals();
  this->UpdateBlastVisuals(DeltaSeconds);
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
  if (UProjectAirDefenseGameUserSettings* Settings =
          UProjectAirDefenseGameUserSettings::GetProjectAirDefenseGameUserSettings()) {
    const int32 CurrentLevel =
        Settings->GetOverallScalabilityLevel() < 0 ? 3 : Settings->GetOverallScalabilityLevel();
    Settings->SetOverallScalabilityLevel(FMath::Clamp(CurrentLevel + 1, 0, 4));
    Settings->ApplyNonResolutionSettings();
    Settings->SaveSettings();
  }
}

void AProjectAirDefenseBattleManager::DecreaseOverallQuality() {
  if (UProjectAirDefenseGameUserSettings* Settings =
          UProjectAirDefenseGameUserSettings::GetProjectAirDefenseGameUserSettings()) {
    const int32 CurrentLevel =
        Settings->GetOverallScalabilityLevel() < 0 ? 3 : Settings->GetOverallScalabilityLevel();
    Settings->SetOverallScalabilityLevel(FMath::Clamp(CurrentLevel - 1, 0, 4));
    Settings->ApplyNonResolutionSettings();
    Settings->SaveSettings();
  }
}

void AProjectAirDefenseBattleManager::CycleAntiAliasingMethod() {
  if (UProjectAirDefenseGameUserSettings* Settings =
          UProjectAirDefenseGameUserSettings::GetProjectAirDefenseGameUserSettings()) {
    const int32 NextValue = (static_cast<int32>(Settings->GetPreferredAntiAliasingMethod()) + 1) % 5;
    Settings->SetPreferredAntiAliasingMethod(
        static_cast<EProjectAirDefenseAntiAliasingMethod>(NextValue));
    Settings->ApplyNonResolutionSettings();
    Settings->SaveSettings();
  }
}

void AProjectAirDefenseBattleManager::ToggleAmbientOcclusion() {
  if (UProjectAirDefenseGameUserSettings* Settings =
          UProjectAirDefenseGameUserSettings::GetProjectAirDefenseGameUserSettings()) {
    Settings->SetAmbientOcclusionEnabled(!Settings->IsAmbientOcclusionEnabled());
    Settings->ApplyNonResolutionSettings();
    Settings->SaveSettings();
  }
}

void AProjectAirDefenseBattleManager::ToggleMotionBlur() {
  if (UProjectAirDefenseGameUserSettings* Settings =
          UProjectAirDefenseGameUserSettings::GetProjectAirDefenseGameUserSettings()) {
    Settings->SetMotionBlurEnabled(!Settings->IsMotionBlurEnabled());
    Settings->ApplyNonResolutionSettings();
    Settings->SaveSettings();
  }
}

void AProjectAirDefenseBattleManager::CycleShadowQuality() {
  if (UProjectAirDefenseGameUserSettings* Settings =
          UProjectAirDefenseGameUserSettings::GetProjectAirDefenseGameUserSettings()) {
    Settings->SetShadowQuality((Settings->GetShadowQuality() + 1) % 5);
    Settings->ApplyNonResolutionSettings();
    Settings->SaveSettings();
  }
}

void AProjectAirDefenseBattleManager::CycleReflectionQuality() {
  if (UProjectAirDefenseGameUserSettings* Settings =
          UProjectAirDefenseGameUserSettings::GetProjectAirDefenseGameUserSettings()) {
    Settings->SetReflectionQuality((Settings->GetReflectionQuality() + 1) % 5);
    Settings->ApplyNonResolutionSettings();
    Settings->SaveSettings();
  }
}

void AProjectAirDefenseBattleManager::CyclePostProcessingQuality() {
  if (UProjectAirDefenseGameUserSettings* Settings =
          UProjectAirDefenseGameUserSettings::GetProjectAirDefenseGameUserSettings()) {
    Settings->SetPostProcessingQuality((Settings->GetPostProcessingQuality() + 1) % 5);
    Settings->ApplyNonResolutionSettings();
    Settings->SaveSettings();
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

  for (const FProjectAirDefenseDistrictCell& DistrictCell : this->DistrictCells) {
    FProjectAirDefenseRadarDistrictSnapshot DistrictSnapshot;
    DistrictSnapshot.Id = DistrictCell.Id;
    DistrictSnapshot.LocalPositionMeters =
        FVector2d(DistrictCell.LocalPositionMeters.X, DistrictCell.LocalPositionMeters.Y);
    DistrictSnapshot.RadiusMeters = DistrictCell.RadiusMeters;
    DistrictSnapshot.Integrity = DistrictCell.Integrity;
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
        TEXT("%s  %s  AO %s  SH %d  RF %d  PP %d"),
        *OverallLabel,
        *AALabel,
        Settings->IsAmbientOcclusionEnabled() ? TEXT("ON") : TEXT("OFF"),
        Settings->GetShadowQuality(),
        Settings->GetReflectionQuality(),
        Settings->GetPostProcessingQuality());
  }
  return TEXT("AUTO  AA ?  AO ?  SH ?  RF ?  PP ?");
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
  this->ThreatVisuals.Empty();
  this->InterceptorVisuals.Empty();
  this->LastThreatWorldPositions.Empty();
  this->LastInterceptorWorldPositions.Empty();
  this->BlastVisuals.Empty();
  this->ElapsedBattleSeconds = 0.0;
  this->StepAccumulatorSeconds = 0.0;
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
  this->DistrictVisuals.Empty();
  this->LauncherVisuals.Empty();

  for (int32 Index = 0; Index < this->LauncherPositionsMeters.Num(); ++Index) {
    UStaticMeshComponent* LauncherMarker = this->CreateStaticMarker(
        FString::Printf(TEXT("Launcher-%d"), Index),
        this->CylinderMesh != nullptr ? this->CylinderMesh : this->CubeMesh,
        this->ToWorldPosition(this->LauncherPositionsMeters[Index] + FVector3d(0.0, 0.0, LauncherMarkerHalfHeightMeters)),
        this->CylinderMesh != nullptr
            ? ScaleCylinder(LauncherMarkerRadiusMeters, LauncherMarkerHalfHeightMeters)
            : FVector(2.1f, 2.1f, 0.65f),
        FLinearColor(0.18f, 0.86f, 1.0f, 1.0f));
    if (LauncherMarker != nullptr) {
      this->LauncherVisuals.Add(LauncherMarker);
    }
  }
}

void AProjectAirDefenseBattleManager::SyncDynamicVisuals() {
  if (!this->Simulation) {
    return;
  }

  TSet<FString> LiveThreatIds;
  for (const FProjectAirDefenseThreatState& Threat : this->Simulation->GetThreats()) {
    LiveThreatIds.Add(Threat.Id);
    UStaticMeshComponent* ThreatMarker =
        this->EnsureDynamicMarker(
            this->ThreatVisuals,
            Threat.Id,
            this->ConeMesh != nullptr ? this->ConeMesh : this->SphereMesh,
            ThreatColor());
    if (ThreatMarker == nullptr) {
      continue;
    }
    const FVector WorldPosition = this->ToWorldPosition(Threat.PositionMeters);
    ThreatMarker->SetWorldLocation(WorldPosition);
    ThreatMarker->SetWorldScale3D(
        this->ConeMesh != nullptr
            ? ScaleCone(ThreatMarkerRadiusMeters, ThreatMarkerLengthMeters)
            : ScaleSphereToRadius(ThreatMarkerRadiusMeters));
    ThreatMarker->SetWorldRotation(
        MarkerRotationFromVelocity(FVector(Threat.VelocityMetersPerSecond)));
    if (const FVector* PreviousPosition = this->LastThreatWorldPositions.Find(Threat.Id)) {
      DrawDebugLine(
          this->GetWorld(),
          *PreviousPosition,
          WorldPosition,
          Threat.bIsTracked ? FColor::Orange : FColor::Red,
          false,
          TrailVisualSeconds,
          0,
          2.2f);
    }
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
    const FVector WorldPosition = this->ToWorldPosition(Interceptor.PositionMeters);
    InterceptorMarker->SetWorldLocation(WorldPosition);
    InterceptorMarker->SetWorldScale3D(
        this->ConeMesh != nullptr
            ? ScaleCone(InterceptorMarkerRadiusMeters, InterceptorMarkerLengthMeters)
            : ScaleSphereToRadius(InterceptorMarkerRadiusMeters));
    InterceptorMarker->SetWorldRotation(
        MarkerRotationFromVelocity(FVector(Interceptor.VelocityMetersPerSecond)));
    if (const FVector* PreviousPosition = this->LastInterceptorWorldPositions.Find(Interceptor.Id)) {
      DrawDebugLine(
          this->GetWorld(),
          *PreviousPosition,
          WorldPosition,
          FColor::Cyan,
          false,
          TrailVisualSeconds,
          0,
          2.0f);
    }
    this->LastInterceptorWorldPositions.Add(Interceptor.Id, WorldPosition);
  }
  this->RemoveStaleMarkers(
      this->InterceptorVisuals,
      this->LastInterceptorWorldPositions,
      LiveInterceptorIds);

  for (const FProjectAirDefenseDistrictCell& DistrictCell : this->Simulation->GetDistrictCells()) {
    const FVector DistrictCenter =
        this->ToWorldPosition(DistrictCell.LocalPositionMeters + FVector3d(0.0, 0.0, DistrictRingElevationMeters));
    const FColor DistrictColor = this->DistrictColorForIntegrity(DistrictCell.Integrity).ToFColor(true);
    DrawDebugCircle(
        this->GetWorld(),
        DistrictCenter,
        DistrictCell.RadiusMeters * MetersToUnrealUnits,
        40,
        DistrictColor,
        false,
        -1.0f,
        0,
        1.8f,
        FVector::RightVector,
        FVector::ForwardVector,
        false);
    DrawDebugLine(
        this->GetWorld(),
        DistrictCenter,
        DistrictCenter + FVector(0.0f, 0.0f, DistrictStatusStemMeters * MetersToUnrealUnits),
        DistrictColor,
        false,
        -1.0f,
        0,
        1.2f);
  }
}

void AProjectAirDefenseBattleManager::UpdateBlastVisuals(double DeltaSeconds) {
  int32 BlastIndex = 0;
  while (BlastIndex < this->BlastVisuals.Num()) {
    FBlastVisual& BlastVisual = this->BlastVisuals[BlastIndex];
    BlastVisual.RemainingSeconds -= DeltaSeconds;
    DrawDebugSphere(
        this->GetWorld(),
        BlastVisual.WorldPosition,
        BlastVisual.RadiusUnrealUnits,
        18,
        BlastVisual.Color,
        false,
        -1.0f,
        0,
        2.6f);
    if (BlastVisual.RemainingSeconds <= 0.0) {
      this->BlastVisuals.RemoveAt(BlastIndex);
      continue;
    }
    ++BlastIndex;
  }
}

void AProjectAirDefenseBattleManager::ApplyStepEvents(const FProjectAirDefenseStepEvents& Events) {
  for (const FProjectAirDefenseBlastEvent& BlastEvent : Events.BlastEvents) {
    FBlastVisual BlastVisual;
    BlastVisual.WorldPosition = this->ToWorldPosition(BlastEvent.PositionMeters);
    BlastVisual.RadiusUnrealUnits = BlastEvent.RadiusMeters * MetersToUnrealUnits;
    BlastVisual.RemainingSeconds = AutoBlastVisualSeconds;
    BlastVisual.Color = BlastEvent.Kind == EProjectAirDefenseBlastKind::HostileImpact
                            ? FColor(255, 128, 48)
                            : FColor(96, 255, 255);
    this->BlastVisuals.Add(BlastVisual);
  }
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
