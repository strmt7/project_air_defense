#pragma once

#include "CoreMinimal.h"
#include "ProjectAirDefenseBattleTypes.h"

class FProjectAirDefenseBattleSimulation {
public:
  FProjectAirDefenseBattleSimulation(
      const TArray<FProjectAirDefenseDistrictCell>& InDistrictCells,
      const TArray<FVector3d>& InLauncherPositionsMeters,
      const FProjectAirDefenseDefenseSettings& InSettings,
      int32 InRandomSeed = 4242);

  bool StartNextWave();
  void SetDoctrine(EProjectAirDefenseDefenseDoctrine InDoctrine);
  FProjectAirDefenseStepEvents Step(double DeltaSeconds);

  const TArray<FProjectAirDefenseDistrictCell>& GetDistrictCells() const;
  const TArray<FProjectAirDefenseThreatState>& GetThreats() const;
  const TArray<FProjectAirDefenseInterceptorState>& GetInterceptors() const;
  const TArray<FVector3d>& GetLauncherPositionsMeters() const;
  FProjectAirDefenseRuntimeSnapshot BuildRuntimeSnapshot() const;
  FProjectAirDefenseBattleRunSummary BuildRunSummary(int32 RunIndex, double SimulatedSeconds) const;

  const FProjectAirDefenseDefenseSettings& GetSettings() const;

  int32 GetWave() const;
  int32 GetCredits() const;
  int32 GetScore() const;
  double GetCityIntegrity() const;
  bool IsWaveInProgress() const;
  bool IsGameOver() const;
  int32 GetThreatsRemainingInWave() const;

private:
  struct FStepAccumulator {
    TArray<FString> SpawnedThreatIds;
    TArray<FString> RemovedThreatIds;
    TArray<FProjectAirDefenseInterceptorLaunchEvent> LaunchedInterceptors;
    TArray<FString> RemovedInterceptorIds;
    TArray<FProjectAirDefenseTrailEvent> TrailEvents;
    TArray<FProjectAirDefenseBlastEvent> BlastEvents;
    TArray<FProjectAirDefenseDistrictDamageEvent> DistrictDamageEvents;
    bool bWaveCleared = false;
    bool bGameOverTriggered = false;

    FProjectAirDefenseStepEvents ToEvents() const;
  };

  void UpdateWaveState(double DeltaSeconds, FStepAccumulator& Events);
  void UpdateLauncherReloads(double DeltaSeconds);
  void UpdateThreats(double DeltaSeconds, FStepAccumulator& Events);
  void UpdateInterceptors(
      double DeltaSeconds,
      double EffectiveBlastRadiusMeters,
      FStepAccumulator& Events);
  void LaunchInterceptorIfReady(double DeltaSeconds, FStepAccumulator& Events);

  FProjectAirDefenseThreatState SpawnThreat();
  FProjectAirDefenseThreatState* SelectNextThreat();
  TOptional<FProjectAirDefenseInterceptorLaunchEvent> LaunchInterceptor(
      const FProjectAirDefenseThreatState& TargetThreat);
  void ResolveImpact(
      const FVector3d& PositionMeters,
      double RadiusMeters,
      bool bHostile,
      FStepAccumulator& Events);
  void ApplyDistrictImpact(
      FProjectAirDefenseDistrictCell& DistrictCell,
      double Damage,
      double BlastDistanceMeters,
      double GroundCoupling,
      const FVector3d& ImpactPositionMeters,
      FStepAccumulator& Events);

  FProjectAirDefenseThreatState* FindThreat(const FString& ThreatId);
  const FProjectAirDefenseThreatState* FindThreat(const FString& ThreatId) const;

  TArray<FProjectAirDefenseDistrictCell> DistrictCells;
  TArray<FProjectAirDefenseThreatState> Threats;
  TArray<FProjectAirDefenseInterceptorState> Interceptors;
  TArray<FVector3d> LauncherPositionsMeters;
  TArray<double> LauncherReadyInSeconds;
  TMap<FString, int32> ThreatEngagementAttempts;

  FProjectAirDefenseDefenseSettings Settings;
  FRandomStream RandomStream;
  FVector3d DefenseOriginMeters = FVector3d::ZeroVector;
  FVector3d GravityMetersPerSecondSquared = FVector3d::ZeroVector;

  int32 Credits = 10000;
  int32 Wave = 1;
  int32 Score = 0;
  double CityIntegrity = 100.0;
  bool bWaveInProgress = false;
  bool bGameOver = false;
  int32 ThreatsRemainingInWave = 0;
  int32 TotalThreatsSpawned = 0;
  int32 TotalThreatsIntercepted = 0;
  int32 TotalHostileImpacts = 0;
  int32 TotalInterceptorsLaunched = 0;
  int32 TotalDestroyedDistricts = 0;
  // Engagement diagnostics surfaced through FProjectAirDefenseBattleRunSummary.
  int32 TotalInterceptorsFuseRollMissed = 0;
  int32 TotalInterceptsInTerminalPhase = 0;
  double TotalMissDistanceMetersSum = 0.0;
  int32 TotalMissDistanceSamples = 0;
  // Per-threat-type accounting (indexed by static_cast<int32> of the enum).
  // Array size must stay synchronized with EProjectAirDefenseThreatType.
  static constexpr int32 ThreatTypeCount = 3;
  int32 TotalSpawnedByType[ThreatTypeCount] = {0, 0, 0};
  int32 TotalInterceptedByType[ThreatTypeCount] = {0, 0, 0};
  int32 TotalImpactedByType[ThreatTypeCount] = {0, 0, 0};
  double SpawnTimerSeconds = 0.0;
  double TimeSinceLastLaunchSeconds = 0.0;
  int32 NextThreatOrdinal = 1;
  int32 NextInterceptorOrdinal = 1;
};
