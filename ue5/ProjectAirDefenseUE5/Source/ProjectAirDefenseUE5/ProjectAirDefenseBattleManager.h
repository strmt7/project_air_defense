#pragma once

#include "CoreMinimal.h"
#include "GameFramework/Actor.h"
#include "ProjectAirDefenseBattleSimulation.h"
#include "ProjectAirDefenseBattleManager.generated.h"

class UMaterialInterface;
class UInstancedStaticMeshComponent;
class UStaticMesh;
class UStaticMeshComponent;
class USceneComponent;
enum class EProjectAirDefenseAntiAliasingMethod : uint8;

UCLASS()
class PROJECTAIRDEFENSEUE5_API AProjectAirDefenseBattleManager : public AActor {
  GENERATED_BODY()

public:
  AProjectAirDefenseBattleManager();

  virtual void BeginPlay() override;
  virtual void Tick(float DeltaSeconds) override;

  void InitializeBattlefield(const FVector& InWorldFocusPoint, double InTilesetRadiusMeters);

  bool StartNextWave();
  void CycleDoctrine();
  void IncreaseOverallQuality();
  void DecreaseOverallQuality();
  void SetOverallQualityLevel(int32 QualityLevel);
  void CycleAntiAliasingMethod();
  void SetAntiAliasingMethod(EProjectAirDefenseAntiAliasingMethod Method);
  void ToggleAmbientOcclusion();
  void SetAmbientOcclusionEnabled(bool bEnabled);
  void ToggleMotionBlur();
  void SetMotionBlurEnabled(bool bEnabled);
  void ToggleRayTracing();
  void SetRayTracingEnabled(bool bEnabled);
  void CycleShadowQuality();
  void SetShadowQualityLevel(int32 QualityLevel);
  void CycleReflectionQuality();
  void SetReflectionQualityLevel(int32 QualityLevel);
  void CyclePostProcessingQuality();
  void SetPostProcessingQualityLevel(int32 QualityLevel);

  bool IsBattlefieldInitialized() const;
  FProjectAirDefenseRuntimeSnapshot BuildRuntimeSnapshot() const;
  FProjectAirDefenseRadarSnapshot BuildRadarSnapshot() const;
  FString BuildGraphicsSummaryText() const;

private:
  struct FBlastVisual {
    FVector WorldPosition = FVector::ZeroVector;
    FVector CoreScale = FVector::OneVector;
    FVector ShockwaveScale = FVector::OneVector;
    FVector SmokeScale = FVector::OneVector;
    FVector DebrisScale = FVector::OneVector;
    double AgeSeconds = 0.0;
    double LifetimeSeconds = 0.0;
    double SmokeRiseMeters = 0.0;
    EProjectAirDefenseBlastKind Kind = EProjectAirDefenseBlastKind::HostileImpact;
    bool bGroundImpact = false;
  };

  struct FTrailVisual {
    FVector WorldPosition = FVector::ZeroVector;
    FVector BaseScale = FVector::OneVector;
    FVector ExhaustScale = FVector::OneVector;
    FRotator Rotation = FRotator::ZeroRotator;
    double AgeSeconds = 0.0;
    double LifetimeSeconds = 0.0;
    bool bHostile = true;
  };

  struct FLaunchPlumeVisual {
    FVector WorldPosition = FVector::ZeroVector;
    FVector CoreScale = FVector::OneVector;
    FVector SmokeScale = FVector::OneVector;
    double AgeSeconds = 0.0;
    double LifetimeSeconds = 0.0;
  };

  void RebuildSimulation();
  void BuildDistrictCells();
  void SyncStaticVisuals();
  void SyncDynamicVisuals();
  void SyncDistrictDamageFloorVisuals(const TArray<FProjectAirDefenseDistrictCell>& LiveDistrictCells);
  void UpdateBlastVisuals(double DeltaSeconds);
  void UpdateTrailVisuals(double DeltaSeconds);
  void UpdateLaunchPlumeVisuals(double DeltaSeconds);
  void RefreshTransientVisualInstances();
  void ApplyStepEvents(const FProjectAirDefenseStepEvents& Events);
  void SpawnTrailVisual(const FProjectAirDefenseTrailEvent& TrailEvent);
  void SpawnLaunchPlumeVisual(const FVector3d& PositionMeters);
  void SpawnBlastVisual(const FProjectAirDefenseBlastEvent& BlastEvent);
  UStaticMeshComponent* CreateStaticMarker(
      const FString& DebugName,
      UStaticMesh* Mesh,
      const FVector& WorldLocation,
      const FVector& WorldScale,
      const FLinearColor& Color);
  UInstancedStaticMeshComponent* CreateInstancedMarker(
      const FString& DebugName,
      UStaticMesh* Mesh,
      const FLinearColor& Color);
  UStaticMeshComponent* EnsureDynamicMarker(
      TMap<FString, TObjectPtr<UStaticMeshComponent>>& MarkerMap,
      const FString& MarkerId,
      UStaticMesh* Mesh,
      const FLinearColor& Color);
  void RemoveStaleMarkers(
      TMap<FString, TObjectPtr<UStaticMeshComponent>>& MarkerMap,
      TMap<FString, FVector>& LastPositions,
      const TSet<FString>& LiveIds);
  void ApplyColor(UStaticMeshComponent* MeshComponent, const FLinearColor& Color) const;
  FVector ToWorldPosition(const FVector3d& LocalMeters) const;
  FLinearColor DistrictColorForIntegrity(double Integrity) const;

  UPROPERTY(VisibleAnywhere, Category = "Components")
  USceneComponent* SceneRoot;

  UPROPERTY()
  TObjectPtr<UStaticMesh> SphereMesh;

  UPROPERTY()
  TObjectPtr<UStaticMesh> CubeMesh;

  UPROPERTY()
  TObjectPtr<UStaticMesh> CylinderMesh;

  UPROPERTY()
  TObjectPtr<UStaticMesh> ConeMesh;

  UPROPERTY()
  TObjectPtr<UMaterialInterface> BasicShapeMaterial;

  UPROPERTY(Transient)
  TArray<TObjectPtr<UStaticMeshComponent>> DistrictVisuals;

  UPROPERTY(Transient)
  TArray<TObjectPtr<UStaticMeshComponent>> LauncherVisuals;

  UPROPERTY(Transient)
  TArray<TObjectPtr<UStaticMeshComponent>> DistrictStatusVisuals;

  UPROPERTY(Transient)
  TObjectPtr<UInstancedStaticMeshComponent> DamagedDistrictFloorInstances;

  UPROPERTY(Transient)
  TObjectPtr<UInstancedStaticMeshComponent> HostileTrailInstances;

  UPROPERTY(Transient)
  TObjectPtr<UInstancedStaticMeshComponent> InterceptorTrailInstances;

  UPROPERTY(Transient)
  TObjectPtr<UInstancedStaticMeshComponent> HostileExhaustInstances;

  UPROPERTY(Transient)
  TObjectPtr<UInstancedStaticMeshComponent> InterceptorExhaustInstances;

  UPROPERTY(Transient)
  TObjectPtr<UInstancedStaticMeshComponent> LaunchPlumeCoreInstances;

  UPROPERTY(Transient)
  TObjectPtr<UInstancedStaticMeshComponent> LaunchPlumeSmokeInstances;

  UPROPERTY(Transient)
  TObjectPtr<UInstancedStaticMeshComponent> HostileBlastCoreInstances;

  UPROPERTY(Transient)
  TObjectPtr<UInstancedStaticMeshComponent> InterceptBlastCoreInstances;

  UPROPERTY(Transient)
  TObjectPtr<UInstancedStaticMeshComponent> HostileBlastShockwaveInstances;

  UPROPERTY(Transient)
  TObjectPtr<UInstancedStaticMeshComponent> InterceptBlastShockwaveInstances;

  UPROPERTY(Transient)
  TObjectPtr<UInstancedStaticMeshComponent> HostileBlastSmokeInstances;

  UPROPERTY(Transient)
  TObjectPtr<UInstancedStaticMeshComponent> InterceptBlastSmokeInstances;

  UPROPERTY(Transient)
  TObjectPtr<UInstancedStaticMeshComponent> HostileBlastDebrisInstances;

  UPROPERTY(Transient)
  TMap<FString, TObjectPtr<UStaticMeshComponent>> ThreatVisuals;

  UPROPERTY(Transient)
  TMap<FString, TObjectPtr<UStaticMeshComponent>> InterceptorVisuals;

  TUniquePtr<FProjectAirDefenseBattleSimulation> Simulation;
  TArray<FProjectAirDefenseDistrictCell> DistrictCells;
  TArray<FVector3d> LauncherPositionsMeters;
  TArray<FBlastVisual> BlastVisuals;
  TArray<FTrailVisual> TrailVisuals;
  TArray<FLaunchPlumeVisual> LaunchPlumeVisuals;
  TMap<FString, FVector> LastThreatWorldPositions;
  TMap<FString, FVector> LastInterceptorWorldPositions;

  FVector WorldFocusPoint = FVector::ZeroVector;
  double TilesetRadiusMeters = 0.0;
  double StepAccumulatorSeconds = 0.0;
  double ElapsedBattleSeconds = 0.0;
  double TimeSinceBeginPlaySeconds = 0.0;
  int32 TrailVisualSpawnsThisFrame = 0;
  int32 BlastVisualSpawnsThisFrame = 0;
  bool bBattlefieldInitialized = false;
  bool bAutoStartedFirstWave = false;
};
