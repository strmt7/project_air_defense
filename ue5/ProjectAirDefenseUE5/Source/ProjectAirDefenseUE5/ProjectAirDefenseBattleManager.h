#pragma once

#include "CoreMinimal.h"
#include "GameFramework/Actor.h"
#include "ProjectAirDefenseBattleSimulation.h"
#include "ProjectAirDefenseBattleManager.generated.h"

class UMaterialInterface;
class UStaticMesh;
class UStaticMeshComponent;
class USceneComponent;

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
  void CycleAntiAliasingMethod();
  void ToggleAmbientOcclusion();
  void ToggleMotionBlur();
  void CycleShadowQuality();
  void CycleReflectionQuality();
  void CyclePostProcessingQuality();

  bool IsBattlefieldInitialized() const;
  FProjectAirDefenseRuntimeSnapshot BuildRuntimeSnapshot() const;
  FString BuildGraphicsSummaryText() const;

private:
  struct FBlastVisual {
    FVector WorldPosition = FVector::ZeroVector;
    double RadiusUnrealUnits = 0.0;
    double RemainingSeconds = 0.0;
    FColor Color = FColor::Red;
  };

  void RebuildSimulation();
  void BuildDistrictCells();
  void SyncStaticVisuals();
  void SyncDynamicVisuals();
  void UpdateBlastVisuals(double DeltaSeconds);
  void ApplyStepEvents(const FProjectAirDefenseStepEvents& Events);
  UStaticMeshComponent* CreateStaticMarker(
      const FString& DebugName,
      UStaticMesh* Mesh,
      const FVector& WorldLocation,
      const FVector& WorldScale,
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
  TMap<FString, TObjectPtr<UStaticMeshComponent>> ThreatVisuals;

  UPROPERTY(Transient)
  TMap<FString, TObjectPtr<UStaticMeshComponent>> InterceptorVisuals;

  TUniquePtr<FProjectAirDefenseBattleSimulation> Simulation;
  TArray<FProjectAirDefenseDistrictCell> DistrictCells;
  TArray<FVector3d> LauncherPositionsMeters;
  TArray<FBlastVisual> BlastVisuals;
  TMap<FString, FVector> LastThreatWorldPositions;
  TMap<FString, FVector> LastInterceptorWorldPositions;

  FVector WorldFocusPoint = FVector::ZeroVector;
  double TilesetRadiusMeters = 0.0;
  double StepAccumulatorSeconds = 0.0;
  double ElapsedBattleSeconds = 0.0;
  double TimeSinceBeginPlaySeconds = 0.0;
  bool bBattlefieldInitialized = false;
  bool bAutoStartedFirstWave = false;
};
