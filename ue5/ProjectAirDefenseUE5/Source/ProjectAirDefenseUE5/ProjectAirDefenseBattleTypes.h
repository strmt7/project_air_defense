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
};

struct FProjectAirDefenseDistrictCell {
  FString Id;
  FVector3d LocalPositionMeters = FVector3d::ZeroVector;
  double RadiusMeters = 180.0;
  double Integrity = 100.0;
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
};

struct FProjectAirDefenseTrailEvent {
  FVector3d PositionMeters = FVector3d::ZeroVector;
  bool bHostile = true;
};

struct FProjectAirDefenseBlastEvent {
  FVector3d PositionMeters = FVector3d::ZeroVector;
  double RadiusMeters = 0.0;
  EProjectAirDefenseBlastKind Kind = EProjectAirDefenseBlastKind::HostileImpact;
};

struct FProjectAirDefenseDistrictDamageEvent {
  FString DistrictId;
  double Integrity = 100.0;
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

  double InterceptRate() const {
    return this->ThreatsSpawned == 0 ? 0.0
                                     : static_cast<double>(this->ThreatsIntercepted) /
                                           static_cast<double>(this->ThreatsSpawned);
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

FString ProjectAirDefenseDoctrineLabel(EProjectAirDefenseDefenseDoctrine Doctrine);
FString ProjectAirDefenseDoctrineSummary(EProjectAirDefenseDefenseDoctrine Doctrine);
FString ProjectAirDefenseThreatTypeLabel(EProjectAirDefenseThreatType ThreatType);
