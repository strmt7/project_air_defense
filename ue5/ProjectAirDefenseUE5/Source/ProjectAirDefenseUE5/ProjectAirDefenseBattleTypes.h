#pragma once

#include "CoreMinimal.h"

enum class EProjectAirDefenseBlastKind : uint8 {
  HostileImpact,
  Intercept,
};

enum class EProjectAirDefenseThreatType : uint8 {
  Ballistic,
  Glide,
  Cruise,
};

enum class EProjectAirDefenseDefenseDoctrine : uint8 {
  Disciplined,
  Adaptive,
  ShieldWall,
};

struct FProjectAirDefenseDefenseSettings {
  double EngagementRangeMeters = 2150.0;
  double InterceptorSpeedMetersPerSecond = 560.0;
  double LaunchCooldownSeconds = 0.46;
  double BlastRadiusMeters = 62.0;
  EProjectAirDefenseDefenseDoctrine Doctrine = EProjectAirDefenseDefenseDoctrine::ShieldWall;
  // Interceptor flight-phase tuning. Boost makes the missile climb out of the
  // canister before committing to terminal pursuit. Terminal-phase turn rate
  // sharpens the end-game maneuver the way a real seeker does when the target
  // is inside its field of view.
  double InterceptorBoostSeconds = 0.75;
  double InterceptorBoostVerticalBias = 0.62;
  double TerminalPhaseThresholdSeconds = 1.35;
  double TerminalTurnRateMultiplier = 1.85;
  // Proportional-navigation constant N. Industry standard is N=4 (Palumbo
  // 2018, JHU APL Technical Digest). Lower values are more sluggish; higher
  // values amplify seeker noise into command chatter.
  double InterceptorProportionalNavConstant = 4.0;
  // Miss-distance kill probability. At zero miss a hit is nearly certain; at
  // the fuse envelope the kill chance falls to the fuse-floor value, and
  // outside the fuse envelope the interceptor cannot kill at all. The roll
  // replaces the old deterministic "inside fuse means instant kill" gate.
  double KillProbabilityAtZeroMiss = 0.96;
  double KillProbabilityFuseFloor = 0.70;
  // Seeker cone: interceptor can only detonate on a target that is within
  // this half-angle of its own velocity direction. Models the physical
  // constraint that a terminal seeker cannot see targets behind it.
  double SeekerConeDegrees = 80.0;

  // Clamps all tunables to physically reasonable ranges so no caller can
  // feed the simulation a negative probability, a zero engagement range, or
  // a floor that exceeds the zero-miss kill probability. Safe to call
  // repeatedly; idempotent after the first invocation.
  void Sanitize() {
    EngagementRangeMeters = FMath::Clamp(EngagementRangeMeters, 100.0, 20000.0);
    InterceptorSpeedMetersPerSecond = FMath::Clamp(InterceptorSpeedMetersPerSecond, 50.0, 5000.0);
    LaunchCooldownSeconds = FMath::Clamp(LaunchCooldownSeconds, 0.01, 30.0);
    BlastRadiusMeters = FMath::Clamp(BlastRadiusMeters, 1.0, 2000.0);
    InterceptorBoostSeconds = FMath::Clamp(InterceptorBoostSeconds, 0.0, 30.0);
    InterceptorBoostVerticalBias = FMath::Clamp(InterceptorBoostVerticalBias, 0.0, 1.0);
    TerminalPhaseThresholdSeconds = FMath::Clamp(TerminalPhaseThresholdSeconds, 0.0, 60.0);
    TerminalTurnRateMultiplier = FMath::Clamp(TerminalTurnRateMultiplier, 0.1, 20.0);
    InterceptorProportionalNavConstant = FMath::Clamp(InterceptorProportionalNavConstant, 1.0, 10.0);
    KillProbabilityAtZeroMiss = FMath::Clamp(KillProbabilityAtZeroMiss, 0.0, 1.0);
    KillProbabilityFuseFloor =
        FMath::Clamp(KillProbabilityFuseFloor, 0.0, KillProbabilityAtZeroMiss);
    SeekerConeDegrees = FMath::Clamp(SeekerConeDegrees, 1.0, 179.0);
  }
};

struct FProjectAirDefenseDistrictCell {
  FString Id;
  FVector3d LocalPositionMeters = FVector3d::ZeroVector;
  FVector3d LastDamageEpicenterMeters = FVector3d::ZeroVector;
  double RadiusMeters = 180.0;
  double Integrity = 100.0;
  double StructuralIntegrity = 100.0;
  int32 DamagedFloors = 0;
  int32 CollapsedFloors = 0;
  bool bCollapsed = false;
  bool bHasDamageEpicenter = false;
};

struct FProjectAirDefenseThreatState {
  FString Id;
  EProjectAirDefenseThreatType ThreatType = EProjectAirDefenseThreatType::Ballistic;
  FVector3d PositionMeters = FVector3d::ZeroVector;
  FVector3d TargetPositionMeters = FVector3d::ZeroVector;
  FVector3d VelocityMetersPerSecond = FVector3d::ZeroVector;
  double CruiseSpeedMetersPerSecond = 0.0;
  double GuidanceTurnRateDegreesPerSecond = 0.0;
  double GravityScale = 1.0;
  double DesiredAltitudeMeters = 0.0;
  bool bIsTracked = false;
  double TrailCooldownSeconds = 0.0;
};

struct FProjectAirDefenseInterceptorState {
  FString Id;
  FVector3d PositionMeters = FVector3d::ZeroVector;
  FVector3d VelocityMetersPerSecond = FVector3d::ZeroVector;
  FString TargetId;
  int32 LauncherIndex = 0;
  double TrailCooldownSeconds = 0.0;
  // Flight-age for boost/midcourse/terminal phase selection and last
  // line-of-sight unit vector for proportional-navigation rate estimation.
  double AgeSeconds = 0.0;
  FVector3d LastLineOfSightUnit = FVector3d::ZeroVector;
  bool bLineOfSightInitialized = false;
};

struct FProjectAirDefenseTrailEvent {
  FVector3d PositionMeters = FVector3d::ZeroVector;
  FVector3d VelocityMetersPerSecond = FVector3d::ZeroVector;
  bool bHostile = true;
};

struct FProjectAirDefenseBlastEvent {
  FVector3d PositionMeters = FVector3d::ZeroVector;
  double RadiusMeters = 0.0;
  double GroundCoupling = 0.0;
  double PeakDamage = 0.0;
  EProjectAirDefenseBlastKind Kind = EProjectAirDefenseBlastKind::HostileImpact;
  bool bGroundImpact = false;
};

struct FProjectAirDefenseDistrictDamageEvent {
  FString DistrictId;
  double Integrity = 100.0;
  double StructuralIntegrity = 100.0;
  double Damage = 0.0;
  double BlastDistanceMeters = 0.0;
  double GroundCoupling = 0.0;
  int32 DamagedFloors = 0;
  int32 CollapsedFloors = 0;
  bool bCollapsed = false;
  FVector3d EpicenterMeters = FVector3d::ZeroVector;
};

struct FProjectAirDefenseInterceptorLaunchEvent {
  FString InterceptorId;
  int32 LauncherIndex = 0;
  FVector3d LauncherPositionMeters = FVector3d::ZeroVector;
};

struct FProjectAirDefenseStepEvents {
  TArray<FString> SpawnedThreatIds;
  TArray<FString> RemovedThreatIds;
  TArray<FProjectAirDefenseInterceptorLaunchEvent> LaunchedInterceptors;
  TArray<FString> RemovedInterceptorIds;
  TArray<FProjectAirDefenseTrailEvent> TrailEvents;
  TArray<FProjectAirDefenseBlastEvent> BlastEvents;
  TArray<FProjectAirDefenseDistrictDamageEvent> DistrictDamageEvents;
  bool bWaveCleared = false;
  bool bGameOver = false;
};

struct FProjectAirDefenseBattleRunSummary {
  int32 RunIndex = 0;
  int32 WavesCompleted = 0;
  double CityIntegrity = 100.0;
  bool bGameOver = false;
  int32 Score = 0;
  int32 ThreatsSpawned = 0;
  int32 ThreatsIntercepted = 0;
  int32 HostileImpacts = 0;
  int32 InterceptorsLaunched = 0;
  int32 DestroyedDistricts = 0;
  double SimulatedSeconds = 0.0;
  // Engagement diagnostics: how many interceptors actually produced a kill,
  // how many came inside the fuse envelope but lost the probability roll,
  // how many reached terminal-phase guidance, and the mean miss distance in
  // meters of all attempts that closed within the fuse envelope.
  int32 InterceptorsKilledTarget = 0;
  int32 InterceptorsFuseRollMissed = 0;
  int32 InterceptsInTerminalPhase = 0;
  double MissDistanceSumMeters = 0.0;
  int32 MissDistanceSampleCount = 0;

  double InterceptRate() const {
    return this->ThreatsSpawned == 0 ? 0.0
                                     : static_cast<double>(this->ThreatsIntercepted) /
                                           static_cast<double>(this->ThreatsSpawned);
  }

  double AverageMissDistanceMeters() const {
    return this->MissDistanceSampleCount == 0
               ? 0.0
               : this->MissDistanceSumMeters / static_cast<double>(this->MissDistanceSampleCount);
  }

  double SingleShotKillProbability() const {
    const int32 Attempts = this->InterceptorsKilledTarget + this->InterceptorsFuseRollMissed;
    return Attempts == 0
               ? 0.0
               : static_cast<double>(this->InterceptorsKilledTarget) / static_cast<double>(Attempts);
  }

  // Per-threat-type counters indexed by static_cast<int32> of
  // EProjectAirDefenseThreatType. Size must stay synchronized with the enum.
  static constexpr int32 ThreatTypeCount = 3;
  int32 SpawnedByType[ThreatTypeCount] = {0, 0, 0};
  int32 InterceptedByType[ThreatTypeCount] = {0, 0, 0};
  int32 ImpactedByType[ThreatTypeCount] = {0, 0, 0};

  double InterceptRateForType(int32 TypeIndex) const {
    if (TypeIndex < 0 || TypeIndex >= ThreatTypeCount) {
      return 0.0;
    }
    const int32 Spawned = this->SpawnedByType[TypeIndex];
    return Spawned == 0 ? 0.0
                        : static_cast<double>(this->InterceptedByType[TypeIndex]) /
                              static_cast<double>(Spawned);
  }
};

struct FProjectAirDefenseRuntimeSnapshot {
  double CityIntegrity = 100.0;
  int32 Score = 0;
  int32 Credits = 0;
  int32 Wave = 1;
  bool bWaveInProgress = false;
  bool bGameOver = false;
  int32 VisibleThreats = 0;
  int32 RemainingThreatsInWave = 0;
  int32 BallisticThreats = 0;
  int32 GlideThreats = 0;
  int32 CruiseThreats = 0;
  int32 TrackedThreats = 0;
  double EffectiveRangeMeters = 0.0;
  double EffectiveFuseMeters = 0.0;
  EProjectAirDefenseDefenseDoctrine Doctrine = EProjectAirDefenseDefenseDoctrine::Adaptive;
};

struct FProjectAirDefenseThreatSnapshot {
  FString Id;
  FVector3d PositionMeters = FVector3d::ZeroVector;
  FVector3d VelocityMetersPerSecond = FVector3d::ZeroVector;
  FVector3d TargetPositionMeters = FVector3d::ZeroVector;
};

struct FProjectAirDefenseRadarDistrictSnapshot {
  FString Id;
  FVector2d LocalPositionMeters = FVector2d::ZeroVector;
  double RadiusMeters = 0.0;
  double Integrity = 100.0;
  double StructuralIntegrity = 100.0;
  int32 DamagedFloors = 0;
  int32 CollapsedFloors = 0;
  bool bCollapsed = false;
};

struct FProjectAirDefenseRadarLauncherSnapshot {
  FVector2d LocalPositionMeters = FVector2d::ZeroVector;
};

struct FProjectAirDefenseRadarThreatSnapshot {
  FVector2d LocalPositionMeters = FVector2d::ZeroVector;
  EProjectAirDefenseThreatType ThreatType = EProjectAirDefenseThreatType::Ballistic;
  bool bIsTracked = false;
};

struct FProjectAirDefenseRadarSnapshot {
  TArray<FProjectAirDefenseRadarDistrictSnapshot> Districts;
  TArray<FProjectAirDefenseRadarLauncherSnapshot> Launchers;
  TArray<FProjectAirDefenseRadarThreatSnapshot> Threats;
  double ExtentMeters = 1.0;
};

FString ProjectAirDefenseDoctrineLabel(EProjectAirDefenseDefenseDoctrine Doctrine);
FString ProjectAirDefenseDoctrineSummary(EProjectAirDefenseDefenseDoctrine Doctrine);
FString ProjectAirDefenseThreatTypeLabel(EProjectAirDefenseThreatType ThreatType);
