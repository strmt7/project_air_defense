#include "ProjectAirDefenseBattleSimulation.h"

#include <limits>

namespace {
constexpr double ThreatTrailIntervalSeconds = 0.14;
constexpr double InterceptorTrailIntervalSeconds = 0.05;
constexpr int32 StartingCredits = 10000;
constexpr double StartingCityIntegrity = 100.0;
constexpr double WaveStartDelaySeconds = 0.4;
constexpr int32 WaveClearCreditBonus = 1800;
constexpr double ThreatTerminalAltitudeMeters = 90.0;
constexpr double TrackingEpsilon = 0.001;
constexpr int32 InterceptScoreBonus = 150;
constexpr int32 InterceptCreditBonus = 180;
constexpr double InterceptBlastScale = 2.2;
constexpr double InterceptorMaxAltitudeMeters = 4400.0;
constexpr double InterceptorMaxRangeMeters = 7000.0;
constexpr double LauncherMuzzleHeightMeters = 18.0;
constexpr double BlastVisualRadiusScale = 1.2;
constexpr double MinimumDistrictDamage = 8.0;
constexpr double ThreatGravityMetersPerSecondSquared = -55.0;
constexpr double ThreatImpactRadiusMeters = 110.0;
constexpr double ThreatFailsafeForwardMeters = 1800.0;
constexpr double ThreatSpawnDepthMeters = -3600.0;
constexpr double ThreatSpawnMinX = -1800.0;
constexpr double ThreatSpawnMaxX = 1800.0;
constexpr double ThreatSpawnMinAltitude = 1500.0;
constexpr double ThreatSpawnMaxAltitude = 2100.0;
constexpr double ThreatTargetOffsetMin = -80.0;
constexpr double ThreatTargetOffsetMax = 80.0;
constexpr double ThreatMinTravelSeconds = 6.5;
constexpr double ThreatMaxTravelSeconds = 11.5;
constexpr double ThreatMinHorizontalDistance = 1.0;
constexpr double MinimumSpawnIntervalSeconds = 0.9;
constexpr int32 BaseThreatCount = 7;
constexpr int32 AddedThreatsPerWave = 4;
constexpr double BaseLauncherReloadSeconds = 0.62;
constexpr double EngagementRangeMinMeters = 1200.0;
constexpr double EngagementRangeMaxMeters = 3400.0;
constexpr double LaunchCooldownMinSeconds = 0.16;
constexpr double LaunchCooldownMaxSeconds = 1.4;
constexpr double LauncherReloadMinSeconds = 0.2;
constexpr double LauncherReloadMaxSeconds = 1.3;
constexpr double BlastRadiusMinMeters = 56.0;
constexpr double BlastRadiusMaxMeters = 128.0;
constexpr double HostileDamageScale = 72.0;
constexpr double HostileCityIntegrityLoss = 12.0;
constexpr double CollapseIntegrityLoss = 6.0;
constexpr double DefenseDamageScale = 36.0;
constexpr int32 DistrictStructuralFloorCount = 12;
constexpr double AirburstGroundCouplingFloor = 0.08;
constexpr double FullGroundCouplingAltitudeMeters = 35.0;
constexpr double NoGroundCouplingAltitudeMeters = 650.0;
constexpr double StructuralDamageMultiplier = 1.22;
constexpr double CollapseFloorThreshold = 34.0;
constexpr double FullCollapseThreshold = 7.5;
constexpr double FallbackClosingSpeed = 280.0;
constexpr double FallbackVerticalWeight = 0.35;
constexpr double MinimumFallbackTimeToImpact = 0.4;
constexpr double MinimumTrackTimeToImpact = 0.25;
constexpr double MaximumTrackTimeToImpact = 30.0;
constexpr double TimeUrgencyReference = 10.0;
constexpr double AltitudeReferenceMeters = 2400.0;
constexpr double TerminalTrackThresholdMeters = -260.0;
constexpr double MaxInterceptLeadSeconds = 8.0;

struct FDoctrineProfile {
  double LaunchCadenceMultiplier = 1.0;
  double LauncherReloadMultiplier = 1.0;
  double EngagementRangeBiasMeters = 0.0;
  double FuseRadiusBiasMeters = 0.0;
  int32 MaxAssignmentsPerThreat = 1;
  double RecommitWindowSeconds = 4.5;
  double TurnRate = 4.4;
};

struct FMotionSample {
  FVector3d PositionMeters = FVector3d::ZeroVector;
  FVector3d VelocityMetersPerSecond = FVector3d::ZeroVector;
};

struct FThreatAssessmentWeights {
  double TimeUrgency = 5.4;
  double NormalizedDistance = 2.6;
  double CenterlineBias = 1.6;
  double AltitudeUrgency = 1.2;
  double TerminalBonus = 1.4;
  double AssignmentPenalty = 3.4;
};

struct FThreatProfileDefinition {
  EProjectAirDefenseThreatType ThreatType = EProjectAirDefenseThreatType::Ballistic;
  double SpawnDepthMeters = ThreatSpawnDepthMeters;
  double SpawnAltitudeMinMeters = ThreatSpawnMinAltitude;
  double SpawnAltitudeMaxMeters = ThreatSpawnMaxAltitude;
  double SpeedMinMetersPerSecond = 240.0;
  double SpeedMaxMetersPerSecond = 320.0;
  double GravityScale = 1.0;
  double GuidanceTurnRateDegreesPerSecond = 0.0;
  double DesiredAltitudeMinMeters = 0.0;
  double DesiredAltitudeMaxMeters = 0.0;
};

struct FBlastDamageSample {
  double Damage = 0.0;
  double BlastDistanceMeters = 0.0;
  double GroundCoupling = 0.0;
};

const FThreatAssessmentWeights ThreatAssessmentWeights;

FDoctrineProfile DoctrineProfile(EProjectAirDefenseDefenseDoctrine Doctrine) {
  switch (Doctrine) {
  case EProjectAirDefenseDefenseDoctrine::Disciplined:
    return {1.18, 1.14, -120.0, -6.0, 1, 3.2, 3.8};
  case EProjectAirDefenseDefenseDoctrine::Adaptive:
    return {1.0, 1.0, 0.0, 0.0, 1, 4.5, 4.4};
  case EProjectAirDefenseDefenseDoctrine::ShieldWall:
    return {0.90, 0.92, 120.0, 4.0, 2, 5.8, 4.7};
  }
  return {1.0, 1.0, 0.0, 0.0, 1, 4.5, 4.4};
}

double ClampScalar(double Value, double Min, double Max) {
  return FMath::Clamp(Value, Min, Max);
}

double ThreatsForWave(int32 Wave) {
  return static_cast<double>(BaseThreatCount + Wave * AddedThreatsPerWave);
}

double SpawnIntervalForWave(int32 Wave) {
  return FMath::Max(MinimumSpawnIntervalSeconds, 2.4 + static_cast<double>(Wave) * -0.08);
}

TPair<double, double> ThreatSpeedBandForWave(int32 Wave) {
  return {260.0 + static_cast<double>(Wave) * 12.0,
          340.0 + static_cast<double>(Wave) * 14.0};
}

double EffectiveEngagementRange(const FProjectAirDefenseDefenseSettings& Settings) {
  const FDoctrineProfile Profile = DoctrineProfile(Settings.Doctrine);
  return ClampScalar(
      Settings.EngagementRangeMeters + Profile.EngagementRangeBiasMeters,
      EngagementRangeMinMeters,
      EngagementRangeMaxMeters);
}

double EffectiveLaunchCooldown(const FProjectAirDefenseDefenseSettings& Settings) {
  const FDoctrineProfile Profile = DoctrineProfile(Settings.Doctrine);
  return ClampScalar(
      Settings.LaunchCooldownSeconds * Profile.LaunchCadenceMultiplier,
      LaunchCooldownMinSeconds,
      LaunchCooldownMaxSeconds);
}

double EffectiveLauncherReload(const FProjectAirDefenseDefenseSettings& Settings) {
  const FDoctrineProfile Profile = DoctrineProfile(Settings.Doctrine);
  return ClampScalar(
      BaseLauncherReloadSeconds * Profile.LauncherReloadMultiplier,
      LauncherReloadMinSeconds,
      LauncherReloadMaxSeconds);
}

double EffectiveBlastRadius(const FProjectAirDefenseDefenseSettings& Settings) {
  const FDoctrineProfile Profile = DoctrineProfile(Settings.Doctrine);
  return ClampScalar(
      Settings.BlastRadiusMeters + Profile.FuseRadiusBiasMeters,
      BlastRadiusMinMeters,
      BlastRadiusMaxMeters);
}

double EffectiveTurnRate(const FProjectAirDefenseDefenseSettings& Settings) {
  return DoctrineProfile(Settings.Doctrine).TurnRate;
}

double EstimateTimeToImpact(const FProjectAirDefenseThreatSnapshot& Threat) {
  FVector3d ToTarget = Threat.TargetPositionMeters - Threat.PositionMeters;
  const double Distance = FMath::Max(ToTarget.Length(), 1.0);
  const double VelocityMagnitudeSquared = Threat.VelocityMetersPerSecond.SquaredLength();
  const double ClosingSpeed =
      VelocityMagnitudeSquared <= TrackingEpsilon
          ? 0.0
          : FMath::Max(0.0, Threat.VelocityMetersPerSecond | ToTarget.GetSafeNormal());
  if (ClosingSpeed <= TrackingEpsilon) {
    const double FallbackDistance =
        FMath::Max(0.0, FMath::Abs(Threat.PositionMeters.Y)) +
        FMath::Max(0.0, Threat.PositionMeters.Z * FallbackVerticalWeight);
    return FMath::Clamp(
        FallbackDistance / FallbackClosingSpeed,
        MinimumFallbackTimeToImpact,
        MaximumTrackTimeToImpact);
  }
  return FMath::Clamp(
      Distance / ClosingSpeed,
      MinimumTrackTimeToImpact,
      MaximumTrackTimeToImpact);
}

double ScoreThreat(
    const FProjectAirDefenseThreatSnapshot& Threat,
    double EngagementRangeMeters,
    double TimeToImpactSeconds,
    int32 AssignedCount) {
  const double NormalizedDistance =
      1.0 - FMath::Clamp(Threat.PositionMeters.Length() / EngagementRangeMeters, 0.0, 1.0);
  const double CenterlineBias =
      1.0 - FMath::Clamp(FMath::Abs(Threat.PositionMeters.X) / EngagementRangeMeters, 0.0, 1.0);
  const double AltitudeUrgency =
      1.0 - FMath::Clamp(Threat.PositionMeters.Z / AltitudeReferenceMeters, 0.0, 1.0);
  const double TimeUrgency =
      FMath::Clamp(TimeUrgencyReference - TimeToImpactSeconds, -1.0, 10.0);
  const double TerminalBonus =
      Threat.PositionMeters.Y >= TerminalTrackThresholdMeters ? ThreatAssessmentWeights.TerminalBonus : 0.0;

  return TimeUrgency * ThreatAssessmentWeights.TimeUrgency +
         NormalizedDistance * ThreatAssessmentWeights.NormalizedDistance +
         CenterlineBias * ThreatAssessmentWeights.CenterlineBias +
         AltitudeUrgency * ThreatAssessmentWeights.AltitudeUrgency + TerminalBonus -
         static_cast<double>(AssignedCount) * ThreatAssessmentWeights.AssignmentPenalty;
}

bool ThreatHasPriorityOver(const FVector3d& Candidate, const FVector3d& Incumbent) {
  if (!FMath::IsNearlyEqual(Candidate.Y, Incumbent.Y)) {
    return Candidate.Y > Incumbent.Y;
  }
  if (!FMath::IsNearlyEqual(Candidate.Z, Incumbent.Z)) {
    return Candidate.Z < Incumbent.Z;
  }
  return FMath::Abs(Candidate.X) < FMath::Abs(Incumbent.X);
}

int32 AssignedCountFor(const FString& ThreatId, const TMap<FString, int32>& AssignmentCounts) {
  return AssignmentCounts.Contains(ThreatId) ? AssignmentCounts[ThreatId] : 0;
}

FString MakeThreatId(int32 Ordinal) {
  return FString::Printf(TEXT("T-%04d"), Ordinal);
}

FString MakeInterceptorId(int32 Ordinal) {
  return FString::Printf(TEXT("I-%04d"), Ordinal);
}

double BlastGroundCoupling(const FVector3d& ImpactPositionMeters) {
  const double AltitudeMeters = FMath::Max(ImpactPositionMeters.Z, 0.0);
  if (AltitudeMeters <= FullGroundCouplingAltitudeMeters) {
    return 1.0;
  }
  const double Alpha = FMath::Clamp(
      (AltitudeMeters - FullGroundCouplingAltitudeMeters) /
          (NoGroundCouplingAltitudeMeters - FullGroundCouplingAltitudeMeters),
      0.0,
      1.0);
  return FMath::Lerp(1.0, AirburstGroundCouplingFloor, Alpha);
}

int32 DamagedFloorsForStructuralIntegrity(double StructuralIntegrity) {
  const double DamageRatio = 1.0 - FMath::Clamp(StructuralIntegrity / 100.0, 0.0, 1.0);
  return FMath::Clamp(
      static_cast<int32>(FMath::CeilToDouble(DamageRatio * DistrictStructuralFloorCount)),
      0,
      DistrictStructuralFloorCount);
}

int32 CollapsedFloorsForStructuralIntegrity(double StructuralIntegrity) {
  if (StructuralIntegrity <= FullCollapseThreshold) {
    return DistrictStructuralFloorCount;
  }
  if (StructuralIntegrity >= CollapseFloorThreshold) {
    return 0;
  }
  const double CollapseRatio =
      (CollapseFloorThreshold - StructuralIntegrity) /
      (CollapseFloorThreshold - FullCollapseThreshold);
  return FMath::Clamp(
      static_cast<int32>(FMath::CeilToDouble(CollapseRatio * DistrictStructuralFloorCount)),
      0,
      DistrictStructuralFloorCount);
}

FBlastDamageSample ComputeDistrictDamage(
    const FProjectAirDefenseDistrictCell& DistrictCell,
    const FVector3d& ImpactPositionMeters,
    double RadiusMeters,
    bool bHostile) {
  const FVector2D CellGround(DistrictCell.LocalPositionMeters.X, DistrictCell.LocalPositionMeters.Y);
  const FVector2D ImpactGround(ImpactPositionMeters.X, ImpactPositionMeters.Y);
  const double Distance = FVector2D::Distance(CellGround, ImpactGround);
  const double EffectiveRadius = RadiusMeters + DistrictCell.RadiusMeters;
  FBlastDamageSample DamageSample;
  DamageSample.BlastDistanceMeters = Distance;
  DamageSample.GroundCoupling = BlastGroundCoupling(ImpactPositionMeters);
  if (Distance >= EffectiveRadius) {
    return DamageSample;
  }

  const double Scale = bHostile ? HostileDamageScale : DefenseDamageScale;
  const double Falloff = FMath::Square((EffectiveRadius - Distance) / EffectiveRadius);
  const double GroundFactor = FMath::Lerp(0.48, 1.0, DamageSample.GroundCoupling);
  DamageSample.Damage = FMath::Max(Falloff * Scale * GroundFactor, MinimumDistrictDamage);
  return DamageSample;
}

double CityIntegrityLoss(bool bHostile, bool bDestroyedDistrict) {
  double Loss = bHostile ? HostileCityIntegrityLoss : 0.0;
  if (bDestroyedDistrict) {
    Loss += CollapseIntegrityLoss;
  }
  return Loss;
}

double LinearInterceptTime(double B, double C) {
  if (FMath::Abs(B) < TrackingEpsilon) {
    return 0.0;
  }
  return FMath::Max((-C / B), 0.0);
}

double QuadraticInterceptTime(double A, double B, double C) {
  const double Discriminant = B * B - 4.0 * A * C;
  if (Discriminant <= 0.0) {
    return 0.0;
  }
  const double SqrtDiscriminant = FMath::Sqrt(Discriminant);
  const double T1 = (-B - SqrtDiscriminant) / (2.0 * A);
  const double T2 = (-B + SqrtDiscriminant) / (2.0 * A);
  double Best = TNumericLimits<double>::Max();
  if (T1 > 0.0) {
    Best = T1;
  }
  if (T2 > 0.0) {
    Best = FMath::Min(Best, T2);
  }
  return Best == TNumericLimits<double>::Max() ? 0.0 : Best;
}

FVector3d PredictInterceptPoint(
    const FVector3d& InterceptorPositionMeters,
    const FVector3d& TargetPositionMeters,
    const FVector3d& TargetVelocityMetersPerSecond,
    double InterceptorSpeedMetersPerSecond) {
  const FVector3d ToTarget = TargetPositionMeters - InterceptorPositionMeters;
  const double A = TargetVelocityMetersPerSecond.SquaredLength() -
                   FMath::Square(InterceptorSpeedMetersPerSecond);
  const double B = 2.0 * (ToTarget | TargetVelocityMetersPerSecond);
  const double C = ToTarget | ToTarget;
  double TimeSeconds = FMath::Abs(A) < TrackingEpsilon ? LinearInterceptTime(B, C)
                                                       : QuadraticInterceptTime(A, B, C);
  TimeSeconds = FMath::Clamp(TimeSeconds, 0.0, MaxInterceptLeadSeconds);
  return TargetPositionMeters + TargetVelocityMetersPerSecond * TimeSeconds;
}

bool ClosesWithinFuse(
    const FMotionSample& Interceptor,
    const FMotionSample& Target,
    double DeltaSeconds,
    double FuseRadiusMeters,
    double& OutMinSeparationMeters) {
  const FVector3d RelativePosition = Target.PositionMeters - Interceptor.PositionMeters;
  const FVector3d RelativeVelocity =
      Target.VelocityMetersPerSecond - Interceptor.VelocityMetersPerSecond;
  const double RelativeSpeedSquared = RelativeVelocity.SquaredLength();
  const double SampleTime =
      RelativeSpeedSquared <= 0.0001
          ? 0.0
          : FMath::Clamp(
                -((RelativePosition | RelativeVelocity) / RelativeSpeedSquared),
                0.0,
                DeltaSeconds);
  const FVector3d Separation = RelativePosition + RelativeVelocity * SampleTime;
  const double SeparationLength = Separation.Length();
  OutMinSeparationMeters = SeparationLength;
  return SeparationLength <= FuseRadiusMeters;
}

FVector3d ImpactPointFor(const FProjectAirDefenseThreatState& Threat) {
  const bool bReachedTarget =
      (Threat.PositionMeters - Threat.TargetPositionMeters).SquaredLength() <=
      FMath::Square(ThreatImpactRadiusMeters);
  if (bReachedTarget && Threat.PositionMeters.Z <= ThreatTerminalAltitudeMeters) {
    return Threat.TargetPositionMeters;
  }
  if (Threat.PositionMeters.Z <= 0.0) {
    return FVector3d(Threat.PositionMeters.X, Threat.PositionMeters.Y, 0.0);
  }
  if (Threat.PositionMeters.Y >= ThreatFailsafeForwardMeters) {
    return FVector3d(Threat.PositionMeters.X, Threat.PositionMeters.Y, FMath::Max(Threat.PositionMeters.Z, 0.0));
  }
  return FVector3d(std::numeric_limits<double>::quiet_NaN());
}

bool IsValidImpactPoint(const FVector3d& PositionMeters) {
  return !FMath::IsNaN(PositionMeters.X) && !FMath::IsNaN(PositionMeters.Y) && !FMath::IsNaN(PositionMeters.Z);
}

double GuidanceBlendAlpha(double DeltaSeconds, double GuidanceTurnRateDegreesPerSecond) {
  return FMath::Clamp((DeltaSeconds * GuidanceTurnRateDegreesPerSecond) / 60.0, 0.0, 1.0);
}

double TrackingRangeMultiplier(EProjectAirDefenseThreatType ThreatType) {
  switch (ThreatType) {
  case EProjectAirDefenseThreatType::Ballistic:
    return 1.0;
  case EProjectAirDefenseThreatType::Glide:
    return 0.86;
  case EProjectAirDefenseThreatType::Cruise:
    return 0.72;
  }
  return 1.0;
}

double DistanceToTargetGround(const FProjectAirDefenseThreatState& Threat) {
  const FVector2D PositionGround(Threat.PositionMeters.X, Threat.PositionMeters.Y);
  const FVector2D TargetGround(Threat.TargetPositionMeters.X, Threat.TargetPositionMeters.Y);
  return FVector2D::Distance(PositionGround, TargetGround);
}

double TerminalAltitudeForThreat(const FProjectAirDefenseThreatState& Threat) {
  switch (Threat.ThreatType) {
  case EProjectAirDefenseThreatType::Ballistic:
    return 0.0;
  case EProjectAirDefenseThreatType::Glide:
    return FMath::Lerp(
        0.0,
        Threat.DesiredAltitudeMeters,
        FMath::Clamp(DistanceToTargetGround(Threat) / 1400.0, 0.0, 1.0));
  case EProjectAirDefenseThreatType::Cruise:
    return FMath::Lerp(
        0.0,
        Threat.DesiredAltitudeMeters,
        FMath::Clamp(DistanceToTargetGround(Threat) / 900.0, 0.0, 1.0));
  }
  return 0.0;
}

FThreatProfileDefinition SelectThreatProfile(int32 Wave, FRandomStream& RandomStream) {
  const double Roll = RandomStream.FRand();
  const TPair<double, double> BaseSpeedBand = ThreatSpeedBandForWave(Wave);
  if (Wave >= 2 && Roll > 0.72) {
    return {
        EProjectAirDefenseThreatType::Cruise,
        -3200.0,
        260.0,
        420.0,
        FMath::Max(170.0, BaseSpeedBand.Key - 60.0),
        FMath::Max(230.0, BaseSpeedBand.Value - 70.0),
        0.0,
        18.0,
        220.0,
        320.0,
    };
  }
  if (Roll > 0.44) {
    return {
        EProjectAirDefenseThreatType::Glide,
        -3400.0,
        900.0,
        1450.0,
        FMath::Max(220.0, BaseSpeedBand.Key - 30.0),
        FMath::Max(280.0, BaseSpeedBand.Value - 20.0),
        0.32,
        9.0,
        260.0,
        420.0,
    };
  }
  return {
      EProjectAirDefenseThreatType::Ballistic,
      ThreatSpawnDepthMeters,
      ThreatSpawnMinAltitude,
      ThreatSpawnMaxAltitude,
      BaseSpeedBand.Key,
      BaseSpeedBand.Value,
      1.0,
      0.0,
      0.0,
      0.0,
  };
}

FVector3d InitialVelocityForThreat(
    const FThreatProfileDefinition& Profile,
    const FVector3d& StartMeters,
    const FVector3d& TargetMeters,
    double DesiredAltitudeMeters,
    FRandomStream& RandomStream) {
  if (Profile.ThreatType == EProjectAirDefenseThreatType::Ballistic) {
    const double HorizontalSpeed =
        RandomStream.FRandRange(Profile.SpeedMinMetersPerSecond, Profile.SpeedMaxMetersPerSecond);
    const FVector3d HorizontalDisplacement(TargetMeters.X - StartMeters.X, TargetMeters.Y - StartMeters.Y, 0.0);
    const double HorizontalDistance =
        FMath::Max(HorizontalDisplacement.Length(), ThreatMinHorizontalDistance);
    const double TravelTime =
        FMath::Clamp(HorizontalDistance / HorizontalSpeed, ThreatMinTravelSeconds, ThreatMaxTravelSeconds);
    const FVector3d Displacement = TargetMeters - StartMeters;
    FVector3d VelocityMetersPerSecond = Displacement / TravelTime;
    VelocityMetersPerSecond.Z -= 0.5 * ThreatGravityMetersPerSecondSquared * TravelTime;
    return VelocityMetersPerSecond;
  }
  const FVector3d AimPoint = TargetMeters + FVector3d(0.0, 0.0, DesiredAltitudeMeters);
  const double SpeedMetersPerSecond =
      RandomStream.FRandRange(Profile.SpeedMinMetersPerSecond, Profile.SpeedMaxMetersPerSecond);
  return (AimPoint - StartMeters).GetSafeNormal() * SpeedMetersPerSecond;
}

void UpdateThreatMotion(FProjectAirDefenseThreatState& Threat, double DeltaSeconds, const FVector3d& GravityMetersPerSecondSquared) {
  if (Threat.ThreatType == EProjectAirDefenseThreatType::Ballistic) {
    Threat.VelocityMetersPerSecond += GravityMetersPerSecondSquared * Threat.GravityScale * DeltaSeconds;
    Threat.PositionMeters += Threat.VelocityMetersPerSecond * DeltaSeconds;
    return;
  }

  const double AltitudeCommandMeters = TerminalAltitudeForThreat(Threat);
  const FVector3d GuidedAimPoint =
      Threat.TargetPositionMeters + FVector3d(0.0, 0.0, AltitudeCommandMeters);
  const FVector3d DesiredVelocity =
      (GuidedAimPoint - Threat.PositionMeters).GetSafeNormal() * Threat.CruiseSpeedMetersPerSecond;
  const double BlendAlpha = GuidanceBlendAlpha(DeltaSeconds, Threat.GuidanceTurnRateDegreesPerSecond);
  Threat.VelocityMetersPerSecond =
      FMath::Lerp(Threat.VelocityMetersPerSecond, DesiredVelocity, BlendAlpha).GetSafeNormal() *
      Threat.CruiseSpeedMetersPerSecond;
  if (Threat.GravityScale > TrackingEpsilon) {
    Threat.VelocityMetersPerSecond += GravityMetersPerSecondSquared * Threat.GravityScale * DeltaSeconds;
  }
  Threat.PositionMeters += Threat.VelocityMetersPerSecond * DeltaSeconds;
}
} // namespace

FString ProjectAirDefenseDoctrineLabel(EProjectAirDefenseDefenseDoctrine Doctrine) {
  switch (Doctrine) {
  case EProjectAirDefenseDefenseDoctrine::Disciplined:
    return TEXT("DISCIPLINED");
  case EProjectAirDefenseDefenseDoctrine::Adaptive:
    return TEXT("ADAPTIVE");
  case EProjectAirDefenseDefenseDoctrine::ShieldWall:
    return TEXT("SHIELD WALL");
  }
  return TEXT("ADAPTIVE");
}

FString ProjectAirDefenseDoctrineSummary(EProjectAirDefenseDefenseDoctrine Doctrine) {
  switch (Doctrine) {
  case EProjectAirDefenseDefenseDoctrine::Disciplined:
    return TEXT("Single-shot spacing. Lowest burn rate.");
  case EProjectAirDefenseDefenseDoctrine::Adaptive:
    return TEXT("Balanced fire with late-track support.");
  case EProjectAirDefenseDefenseDoctrine::ShieldWall:
    return TEXT("Layered fire with critical-track double taps.");
  }
  return TEXT("Balanced fire with late-track support.");
}

FString ProjectAirDefenseThreatTypeLabel(EProjectAirDefenseThreatType ThreatType) {
  switch (ThreatType) {
  case EProjectAirDefenseThreatType::Ballistic:
    return TEXT("BALLISTIC");
  case EProjectAirDefenseThreatType::Glide:
    return TEXT("GLIDE");
  case EProjectAirDefenseThreatType::Cruise:
    return TEXT("CRUISE");
  }
  return TEXT("BALLISTIC");
}

FProjectAirDefenseStepEvents
FProjectAirDefenseBattleSimulation::FStepAccumulator::ToEvents() const {
  FProjectAirDefenseStepEvents Events;
  Events.SpawnedThreatIds = this->SpawnedThreatIds;
  Events.RemovedThreatIds = this->RemovedThreatIds;
  Events.LaunchedInterceptors = this->LaunchedInterceptors;
  Events.RemovedInterceptorIds = this->RemovedInterceptorIds;
  Events.TrailEvents = this->TrailEvents;
  Events.BlastEvents = this->BlastEvents;
  Events.DistrictDamageEvents = this->DistrictDamageEvents;
  Events.bWaveCleared = this->bWaveCleared;
  Events.bGameOver = this->bGameOverTriggered;
  return Events;
}

FProjectAirDefenseBattleSimulation::FProjectAirDefenseBattleSimulation(
    const TArray<FProjectAirDefenseDistrictCell>& InDistrictCells,
    const TArray<FVector3d>& InLauncherPositionsMeters,
    const FProjectAirDefenseDefenseSettings& InSettings,
    int32 InRandomSeed)
    : DistrictCells(InDistrictCells),
      LauncherPositionsMeters(InLauncherPositionsMeters),
      Settings(InSettings),
      RandomStream(InRandomSeed),
      GravityMetersPerSecondSquared(0.0, 0.0, ThreatGravityMetersPerSecondSquared),
      Credits(StartingCredits),
      CityIntegrity(StartingCityIntegrity) {
  this->LauncherReadyInSeconds.Init(0.0, FMath::Max(this->LauncherPositionsMeters.Num(), 1));
  if (this->LauncherPositionsMeters.IsEmpty()) {
    this->LauncherPositionsMeters.Add(FVector3d::ZeroVector);
  }

  FVector3d Sum = FVector3d::ZeroVector;
  for (const FVector3d& LauncherPosition : this->LauncherPositionsMeters) {
    Sum += FVector3d(LauncherPosition.X, LauncherPosition.Y, 0.0);
  }
  this->DefenseOriginMeters = Sum / static_cast<double>(this->LauncherPositionsMeters.Num());
}

bool FProjectAirDefenseBattleSimulation::StartNextWave() {
  if (this->bWaveInProgress || this->bGameOver) {
    return false;
  }
  this->bWaveInProgress = true;
  this->ThreatsRemainingInWave = static_cast<int32>(ThreatsForWave(this->Wave));
  this->SpawnTimerSeconds = WaveStartDelaySeconds;
  this->TimeSinceLastLaunchSeconds = EffectiveLaunchCooldown(this->Settings);
  return true;
}

void FProjectAirDefenseBattleSimulation::SetDoctrine(EProjectAirDefenseDefenseDoctrine InDoctrine) {
  this->Settings.Doctrine = InDoctrine;
}

FProjectAirDefenseStepEvents FProjectAirDefenseBattleSimulation::Step(double DeltaSeconds) {
  if (this->bGameOver) {
    FProjectAirDefenseStepEvents Events;
    Events.bGameOver = true;
    return Events;
  }

  FStepAccumulator Events;
  const double CurrentBlastRadiusMeters = EffectiveBlastRadius(this->Settings);

  this->UpdateWaveState(DeltaSeconds, Events);
  this->UpdateLauncherReloads(DeltaSeconds);
  this->UpdateThreats(DeltaSeconds, Events);
  this->UpdateInterceptors(DeltaSeconds, CurrentBlastRadiusMeters, Events);
  this->LaunchInterceptorIfReady(DeltaSeconds, Events);

  if (this->CityIntegrity <= 0.0) {
    this->bGameOver = true;
    Events.bGameOverTriggered = true;
  }

  return Events.ToEvents();
}

const TArray<FProjectAirDefenseDistrictCell>&
FProjectAirDefenseBattleSimulation::GetDistrictCells() const {
  return this->DistrictCells;
}

const TArray<FProjectAirDefenseThreatState>& FProjectAirDefenseBattleSimulation::GetThreats() const {
  return this->Threats;
}

const TArray<FProjectAirDefenseInterceptorState>&
FProjectAirDefenseBattleSimulation::GetInterceptors() const {
  return this->Interceptors;
}

const TArray<FVector3d>&
FProjectAirDefenseBattleSimulation::GetLauncherPositionsMeters() const {
  return this->LauncherPositionsMeters;
}

FProjectAirDefenseRuntimeSnapshot FProjectAirDefenseBattleSimulation::BuildRuntimeSnapshot() const {
  FProjectAirDefenseRuntimeSnapshot Snapshot;
  Snapshot.CityIntegrity = this->CityIntegrity;
  Snapshot.Score = this->Score;
  Snapshot.Credits = this->Credits;
  Snapshot.Wave = this->Wave;
  Snapshot.bWaveInProgress = this->bWaveInProgress;
  Snapshot.bGameOver = this->bGameOver;
  Snapshot.VisibleThreats = this->Threats.Num();
  Snapshot.RemainingThreatsInWave = this->ThreatsRemainingInWave;
  for (const FProjectAirDefenseThreatState& Threat : this->Threats) {
    if (Threat.bIsTracked) {
      ++Snapshot.TrackedThreats;
    }
    switch (Threat.ThreatType) {
    case EProjectAirDefenseThreatType::Ballistic:
      ++Snapshot.BallisticThreats;
      break;
    case EProjectAirDefenseThreatType::Glide:
      ++Snapshot.GlideThreats;
      break;
    case EProjectAirDefenseThreatType::Cruise:
      ++Snapshot.CruiseThreats;
      break;
    }
  }
  Snapshot.EffectiveRangeMeters = EffectiveEngagementRange(this->Settings);
  Snapshot.EffectiveFuseMeters = EffectiveBlastRadius(this->Settings);
  Snapshot.Doctrine = this->Settings.Doctrine;
  return Snapshot;
}

FProjectAirDefenseBattleRunSummary
FProjectAirDefenseBattleSimulation::BuildRunSummary(int32 RunIndex, double SimulatedSeconds) const {
  static_assert(
      FProjectAirDefenseBattleSimulation::ThreatTypeCount ==
          FProjectAirDefenseBattleRunSummary::ThreatTypeCount,
      "Threat-type counters must stay synchronized between simulation and summary.");
  FProjectAirDefenseBattleRunSummary Summary;
  Summary.RunIndex = RunIndex;
  Summary.WavesCompleted = this->Wave - 1;
  Summary.CityIntegrity = this->CityIntegrity;
  Summary.bGameOver = this->bGameOver;
  Summary.Score = this->Score;
  Summary.ThreatsSpawned = this->TotalThreatsSpawned;
  Summary.ThreatsIntercepted = this->TotalThreatsIntercepted;
  Summary.HostileImpacts = this->TotalHostileImpacts;
  Summary.InterceptorsLaunched = this->TotalInterceptorsLaunched;
  Summary.DestroyedDistricts = this->TotalDestroyedDistricts;
  Summary.SimulatedSeconds = SimulatedSeconds;
  Summary.InterceptorsKilledTarget = this->TotalThreatsIntercepted;
  Summary.InterceptorsFuseRollMissed = this->TotalInterceptorsFuseRollMissed;
  Summary.InterceptsInTerminalPhase = this->TotalInterceptsInTerminalPhase;
  Summary.MissDistanceSumMeters = this->TotalMissDistanceMetersSum;
  Summary.MissDistanceSampleCount = this->TotalMissDistanceSamples;
  for (int32 TypeIndex = 0; TypeIndex < ThreatTypeCount; ++TypeIndex) {
    Summary.SpawnedByType[TypeIndex] = this->TotalSpawnedByType[TypeIndex];
    Summary.InterceptedByType[TypeIndex] = this->TotalInterceptedByType[TypeIndex];
    Summary.ImpactedByType[TypeIndex] = this->TotalImpactedByType[TypeIndex];
  }
  return Summary;
}

const FProjectAirDefenseDefenseSettings&
FProjectAirDefenseBattleSimulation::GetSettings() const {
  return this->Settings;
}

int32 FProjectAirDefenseBattleSimulation::GetWave() const {
  return this->Wave;
}

int32 FProjectAirDefenseBattleSimulation::GetCredits() const {
  return this->Credits;
}

int32 FProjectAirDefenseBattleSimulation::GetScore() const {
  return this->Score;
}

double FProjectAirDefenseBattleSimulation::GetCityIntegrity() const {
  return this->CityIntegrity;
}

bool FProjectAirDefenseBattleSimulation::IsWaveInProgress() const {
  return this->bWaveInProgress;
}

bool FProjectAirDefenseBattleSimulation::IsGameOver() const {
  return this->bGameOver;
}

int32 FProjectAirDefenseBattleSimulation::GetThreatsRemainingInWave() const {
  return this->ThreatsRemainingInWave;
}

void FProjectAirDefenseBattleSimulation::UpdateWaveState(
    double DeltaSeconds,
    FStepAccumulator& Events) {
  if (!this->bWaveInProgress) {
    return;
  }

  this->SpawnTimerSeconds -= DeltaSeconds;
  if (this->SpawnTimerSeconds <= 0.0 && this->ThreatsRemainingInWave > 0) {
    const FProjectAirDefenseThreatState Threat = this->SpawnThreat();
    Events.SpawnedThreatIds.Add(Threat.Id);
    --this->ThreatsRemainingInWave;
    this->SpawnTimerSeconds = SpawnIntervalForWave(this->Wave);
  }

  if (this->ThreatsRemainingInWave == 0 && this->Threats.IsEmpty()) {
    this->bWaveInProgress = false;
    ++this->Wave;
    this->Credits += WaveClearCreditBonus;
    Events.bWaveCleared = true;
  }
}

void FProjectAirDefenseBattleSimulation::UpdateLauncherReloads(double DeltaSeconds) {
  for (double& ReloadSeconds : this->LauncherReadyInSeconds) {
    ReloadSeconds = FMath::Max(0.0, ReloadSeconds - DeltaSeconds);
  }
}

void FProjectAirDefenseBattleSimulation::UpdateThreats(
    double DeltaSeconds,
    FStepAccumulator& Events) {
  int32 ThreatIndex = 0;
  while (ThreatIndex < this->Threats.Num()) {
    FProjectAirDefenseThreatState& Threat = this->Threats[ThreatIndex];
    // Per-type pre-motion uncertainty. Ballistic missiles inherit launch and
    // wind error as a slow yaw drift (contributing to real-world CEP around
    // the aim point). Glide and cruise threats overlay a larger random yaw
    // drift that reads as terminal evasion under defender fire. All drift is
    // driven by the seeded RandomStream so runs remain deterministic.
    const double YawJitterDegreesPerSecond =
        Threat.ThreatType == EProjectAirDefenseThreatType::Ballistic
            ? 0.45
            : Threat.ThreatType == EProjectAirDefenseThreatType::Glide ? 1.8
                                                                        : 2.8;
    const double YawJitterRadians = FMath::DegreesToRadians(YawJitterDegreesPerSecond) *
                                    DeltaSeconds * (2.0 * this->RandomStream.FRand() - 1.0);
    const double CosYaw = FMath::Cos(YawJitterRadians);
    const double SinYaw = FMath::Sin(YawJitterRadians);
    const double RotatedVelocityX =
        Threat.VelocityMetersPerSecond.X * CosYaw - Threat.VelocityMetersPerSecond.Y * SinYaw;
    const double RotatedVelocityY =
        Threat.VelocityMetersPerSecond.X * SinYaw + Threat.VelocityMetersPerSecond.Y * CosYaw;
    Threat.VelocityMetersPerSecond.X = RotatedVelocityX;
    Threat.VelocityMetersPerSecond.Y = RotatedVelocityY;
    UpdateThreatMotion(Threat, DeltaSeconds, this->GravityMetersPerSecondSquared);

    Threat.TrailCooldownSeconds -= DeltaSeconds;
    if (Threat.TrailCooldownSeconds <= 0.0) {
      FProjectAirDefenseTrailEvent TrailEvent;
      TrailEvent.PositionMeters = Threat.PositionMeters;
      TrailEvent.bHostile = true;
      Events.TrailEvents.Add(TrailEvent);
      Threat.TrailCooldownSeconds = ThreatTrailIntervalSeconds;
    }

    const FVector3d HorizontalThreat(Threat.PositionMeters.X, Threat.PositionMeters.Y, 0.0);
    const double TrackRangeSquared =
        FMath::Square(
            EffectiveEngagementRange(this->Settings) *
            TrackingRangeMultiplier(Threat.ThreatType));
    Threat.bIsTracked =
        (HorizontalThreat - this->DefenseOriginMeters).SquaredLength() < TrackRangeSquared;

    const FVector3d ImpactPointMeters = ImpactPointFor(Threat);
    if (!IsValidImpactPoint(ImpactPointMeters)) {
      ++ThreatIndex;
      continue;
    }

    this->ResolveImpact(ImpactPointMeters, ThreatImpactRadiusMeters, true, Events);
    ++this->TotalHostileImpacts;
    const int32 ImpactTypeIndex = static_cast<int32>(Threat.ThreatType);
    if (ImpactTypeIndex >= 0 && ImpactTypeIndex < ThreatTypeCount) {
      ++this->TotalImpactedByType[ImpactTypeIndex];
    }
    Events.RemovedThreatIds.Add(Threat.Id);
    this->Threats.RemoveAt(ThreatIndex);
  }
}

void FProjectAirDefenseBattleSimulation::UpdateInterceptors(
    double DeltaSeconds,
    double EffectiveBlastRadiusMeters,
    FStepAccumulator& Events) {
  const double BoostSeconds = FMath::Max(this->Settings.InterceptorBoostSeconds, 0.0);
  const double TerminalThresholdSeconds = FMath::Max(this->Settings.TerminalPhaseThresholdSeconds, 0.0);
  const double BaseTurnRate = EffectiveTurnRate(this->Settings);
  const double TerminalTurnRate =
      FMath::Max(BaseTurnRate * this->Settings.TerminalTurnRateMultiplier, BaseTurnRate);
  const double NavigationConstant =
      FMath::Max(this->Settings.InterceptorProportionalNavConstant, 1.0);
  const double SeekerConeCosine = FMath::Cos(FMath::DegreesToRadians(
      FMath::Clamp(this->Settings.SeekerConeDegrees, 1.0, 179.0)));
  const double InterceptorSpeedMetersPerSecond =
      FMath::Max(this->Settings.InterceptorSpeedMetersPerSecond, 1.0);

  int32 InterceptorIndex = 0;
  while (InterceptorIndex < this->Interceptors.Num()) {
    FProjectAirDefenseInterceptorState& Interceptor = this->Interceptors[InterceptorIndex];
    Interceptor.AgeSeconds += DeltaSeconds;
    FProjectAirDefenseThreatState* Target = this->FindThreat(Interceptor.TargetId);
    if (Target == nullptr) {
      Events.RemovedInterceptorIds.Add(Interceptor.Id);
      this->Interceptors.RemoveAt(InterceptorIndex);
      continue;
    }

    // Predicted aim point for midcourse pure-pursuit and boost-phase bias.
    const FVector3d AimPoint = PredictInterceptPoint(
        Interceptor.PositionMeters,
        Target->PositionMeters,
        Target->VelocityMetersPerSecond,
        InterceptorSpeedMetersPerSecond);

    const FVector3d LineOfSight = Target->PositionMeters - Interceptor.PositionMeters;
    const double RangeMeters = FMath::Max(LineOfSight.Length(), 1.0);
    const FVector3d LineOfSightUnit = LineOfSight / RangeMeters;
    const FVector3d RelativeVelocity =
        Interceptor.VelocityMetersPerSecond - Target->VelocityMetersPerSecond;
    const double ClosingSpeedMetersPerSecond =
        FMath::Max(RelativeVelocity | LineOfSightUnit, 1.0);
    const double TimeToGoSeconds = RangeMeters / ClosingSpeedMetersPerSecond;

    FVector3d DesiredDirection;
    double PhaseTurnRate = BaseTurnRate;
    bool bInTerminalPhase = false;

    if (Interceptor.AgeSeconds < BoostSeconds) {
      // Boost phase: climb out of the canister while already biasing toward
      // the predicted aim point. Models the motor-burn / tip-over profile so
      // the launch trajectory is not a pure flat shot.
      const double VerticalBias =
          FMath::Clamp(this->Settings.InterceptorBoostVerticalBias, 0.0, 1.0);
      const FVector3d ToAimUnit = (AimPoint - Interceptor.PositionMeters).GetSafeNormal();
      const FVector3d Up(0.0, 0.0, 1.0);
      FVector3d Blend = ToAimUnit * (1.0 - VerticalBias) + Up * VerticalBias;
      DesiredDirection = Blend.GetSafeNormal();
    } else if (TimeToGoSeconds <= TerminalThresholdSeconds &&
               Interceptor.bLineOfSightInitialized) {
      // Terminal phase: proportional-navigation guidance.
      // Commanded acceleration: a = N * V_c * (LOS-rate perpendicular to LOS).
      // Industry-standard navigation constant N = 4 (Palumbo, JHU APL).
      const FVector3d RawLosRate =
          (LineOfSightUnit - Interceptor.LastLineOfSightUnit) / FMath::Max(DeltaSeconds, 0.0001);
      const FVector3d LosRatePerpendicular =
          RawLosRate - LineOfSightUnit * (RawLosRate | LineOfSightUnit);
      const FVector3d CommandedAcceleration =
          LosRatePerpendicular * (NavigationConstant * ClosingSpeedMetersPerSecond);
      const FVector3d SteeredVelocity =
          Interceptor.VelocityMetersPerSecond + CommandedAcceleration * DeltaSeconds;
      DesiredDirection = SteeredVelocity.IsNearlyZero()
                              ? LineOfSightUnit
                              : SteeredVelocity.GetSafeNormal();
      PhaseTurnRate = TerminalTurnRate;
      bInTerminalPhase = true;
    } else {
      // Midcourse phase: pure pursuit against the predicted aim point.
      DesiredDirection = (AimPoint - Interceptor.PositionMeters).GetSafeNormal();
    }

    Interceptor.LastLineOfSightUnit = LineOfSightUnit;
    Interceptor.bLineOfSightInitialized = true;

    const FVector3d DesiredVelocity = DesiredDirection * InterceptorSpeedMetersPerSecond;
    const double LerpAlpha = FMath::Clamp(DeltaSeconds * PhaseTurnRate, 0.0, 1.0);
    FVector3d BlendedVelocity =
        FMath::Lerp(Interceptor.VelocityMetersPerSecond, DesiredVelocity, LerpAlpha);
    if (BlendedVelocity.SquaredLength() > TrackingEpsilon) {
      BlendedVelocity = BlendedVelocity.GetSafeNormal() * InterceptorSpeedMetersPerSecond;
    }
    Interceptor.VelocityMetersPerSecond = BlendedVelocity;
    Interceptor.PositionMeters += Interceptor.VelocityMetersPerSecond * DeltaSeconds;

    Interceptor.TrailCooldownSeconds -= DeltaSeconds;
    if (Interceptor.TrailCooldownSeconds <= 0.0) {
      FProjectAirDefenseTrailEvent TrailEvent;
      TrailEvent.PositionMeters = Interceptor.PositionMeters;
      TrailEvent.bHostile = false;
      Events.TrailEvents.Add(TrailEvent);
      Interceptor.TrailCooldownSeconds = InterceptorTrailIntervalSeconds;
    }

    const FMotionSample InterceptorSample{
        Interceptor.PositionMeters, Interceptor.VelocityMetersPerSecond};
    const FMotionSample ThreatSample{
        Target->PositionMeters, Target->VelocityMetersPerSecond};
    double MinSeparationMeters = TNumericLimits<double>::Max();
    const bool bClosedWithinFuse = ClosesWithinFuse(
        InterceptorSample, ThreatSample, DeltaSeconds, EffectiveBlastRadiusMeters, MinSeparationMeters);

    if (bClosedWithinFuse) {
      // Seeker cone: the terminal seeker cannot lock targets outside its
      // forward field of view. Outside the cone the interceptor flies past
      // without detonating and may still re-engage.
      const FVector3d ToTargetUnit =
          (Target->PositionMeters - Interceptor.PositionMeters).GetSafeNormal();
      const FVector3d ForwardUnit = Interceptor.VelocityMetersPerSecond.GetSafeNormal();
      const bool bWithinSeekerCone =
          ForwardUnit.IsNearlyZero() || (ForwardUnit | ToTargetUnit) >= SeekerConeCosine;

      this->TotalMissDistanceMetersSum += MinSeparationMeters;
      ++this->TotalMissDistanceSamples;

      if (bWithinSeekerCone) {
        // Kill probability: linear falloff from zero-miss Pk to the fuse-floor
        // Pk at the edge of the fuse envelope. This replaces the deterministic
        // "inside fuse means certain kill" gate with a richer miss-distance
        // model and makes shoot-look-shoot doctrines meaningful.
        const double MissRatio = FMath::Clamp(
            MinSeparationMeters / FMath::Max(EffectiveBlastRadiusMeters, 0.0001), 0.0, 1.0);
        const double KillProbability = FMath::Lerp(
            this->Settings.KillProbabilityAtZeroMiss,
            this->Settings.KillProbabilityFuseFloor,
            MissRatio);
        const double Roll = this->RandomStream.FRand();

        FProjectAirDefenseBlastEvent BlastEvent;
        BlastEvent.PositionMeters = Interceptor.PositionMeters;
        BlastEvent.RadiusMeters = EffectiveBlastRadiusMeters * InterceptBlastScale;
        BlastEvent.GroundCoupling = BlastGroundCoupling(BlastEvent.PositionMeters);
        BlastEvent.PeakDamage = 0.0;
        BlastEvent.Kind = EProjectAirDefenseBlastKind::Intercept;
        BlastEvent.bGroundImpact = BlastEvent.GroundCoupling >= 0.95;
        Events.BlastEvents.Add(BlastEvent);

        if (Roll <= KillProbability) {
          this->Score += InterceptScoreBonus;
          this->Credits += InterceptCreditBonus;
          ++this->TotalThreatsIntercepted;
          if (bInTerminalPhase) {
            ++this->TotalInterceptsInTerminalPhase;
          }
          const int32 KilledTypeIndex = static_cast<int32>(Target->ThreatType);
          if (KilledTypeIndex >= 0 && KilledTypeIndex < ThreatTypeCount) {
            ++this->TotalInterceptedByType[KilledTypeIndex];
          }
          const FString TargetId = Target->Id;
          Events.RemovedThreatIds.Add(TargetId);
          Events.RemovedInterceptorIds.Add(Interceptor.Id);
          this->Threats.RemoveAll([&TargetId](const FProjectAirDefenseThreatState& Threat) {
            return Threat.Id == TargetId;
          });
          this->Interceptors.RemoveAt(InterceptorIndex);
          continue;
        }

        // Fuse roll missed: interceptor self-destructs but the threat survives
        // for a potential second-salvo engagement under ShieldWall doctrine.
        ++this->TotalInterceptorsFuseRollMissed;
        Events.RemovedInterceptorIds.Add(Interceptor.Id);
        this->Interceptors.RemoveAt(InterceptorIndex);
        continue;
      }
      // Outside the seeker cone the interceptor stays alive and keeps guiding.
    }

    const FVector3d HorizontalInterceptor(
        Interceptor.PositionMeters.X, Interceptor.PositionMeters.Y, 0.0);
    if (Interceptor.PositionMeters.Z > InterceptorMaxAltitudeMeters ||
        (HorizontalInterceptor - this->DefenseOriginMeters).SquaredLength() >
            FMath::Square(InterceptorMaxRangeMeters)) {
      Events.RemovedInterceptorIds.Add(Interceptor.Id);
      this->Interceptors.RemoveAt(InterceptorIndex);
      continue;
    }

    ++InterceptorIndex;
  }
}

void FProjectAirDefenseBattleSimulation::LaunchInterceptorIfReady(
    double DeltaSeconds,
    FStepAccumulator& Events) {
  this->TimeSinceLastLaunchSeconds += DeltaSeconds;
  if (this->TimeSinceLastLaunchSeconds < EffectiveLaunchCooldown(this->Settings)) {
    return;
  }
  FProjectAirDefenseThreatState* TargetThreat = this->SelectNextThreat();
  if (TargetThreat == nullptr) {
    return;
  }
  const TOptional<FProjectAirDefenseInterceptorLaunchEvent> LaunchEvent =
      this->LaunchInterceptor(*TargetThreat);
  if (!LaunchEvent.IsSet()) {
    return;
  }
  Events.LaunchedInterceptors.Add(LaunchEvent.GetValue());
  this->TimeSinceLastLaunchSeconds = 0.0;
}

FProjectAirDefenseThreatState FProjectAirDefenseBattleSimulation::SpawnThreat() {
  TArray<int32> ViableTargetIndices;
  for (int32 Index = 0; Index < this->DistrictCells.Num(); ++Index) {
    if (this->DistrictCells[Index].Integrity > 0.0) {
      ViableTargetIndices.Add(Index);
    }
  }
  if (ViableTargetIndices.IsEmpty()) {
    for (int32 Index = 0; Index < this->DistrictCells.Num(); ++Index) {
      ViableTargetIndices.Add(Index);
    }
  }

  const int32 TargetIndex =
      ViableTargetIndices[this->RandomStream.RandRange(0, ViableTargetIndices.Num() - 1)];
  const FThreatProfileDefinition Profile = SelectThreatProfile(this->Wave, this->RandomStream);
  const FVector3d StartMeters(
      this->RandomStream.FRandRange(ThreatSpawnMinX, ThreatSpawnMaxX),
      Profile.SpawnDepthMeters,
      this->RandomStream.FRandRange(Profile.SpawnAltitudeMinMeters, Profile.SpawnAltitudeMaxMeters));
  const FVector3d TargetMeters =
      this->DistrictCells[TargetIndex].LocalPositionMeters +
      FVector3d(
          this->RandomStream.FRandRange(ThreatTargetOffsetMin, ThreatTargetOffsetMax),
          this->RandomStream.FRandRange(ThreatTargetOffsetMin, ThreatTargetOffsetMax),
          0.0);
  const double DesiredAltitudeMeters = this->RandomStream.FRandRange(
      Profile.DesiredAltitudeMinMeters,
      Profile.DesiredAltitudeMaxMeters);
  const FVector3d InitialVelocity =
      InitialVelocityForThreat(Profile, StartMeters, TargetMeters, DesiredAltitudeMeters, this->RandomStream);

  FProjectAirDefenseThreatState Threat;
  Threat.Id = MakeThreatId(this->NextThreatOrdinal++);
  Threat.ThreatType = Profile.ThreatType;
  Threat.PositionMeters = StartMeters;
  Threat.TargetPositionMeters = TargetMeters;
  Threat.VelocityMetersPerSecond = InitialVelocity;
  Threat.CruiseSpeedMetersPerSecond = InitialVelocity.Length();
  Threat.GuidanceTurnRateDegreesPerSecond = Profile.GuidanceTurnRateDegreesPerSecond;
  Threat.GravityScale = Profile.GravityScale;
  Threat.DesiredAltitudeMeters = DesiredAltitudeMeters;
  Threat.TrailCooldownSeconds = this->RandomStream.FRandRange(0.0, ThreatTrailIntervalSeconds);
  this->Threats.Add(Threat);
  ++this->TotalThreatsSpawned;
  const int32 TypeIndex = static_cast<int32>(Threat.ThreatType);
  if (TypeIndex >= 0 && TypeIndex < ThreatTypeCount) {
    ++this->TotalSpawnedByType[TypeIndex];
  }
  return Threat;
}

FProjectAirDefenseThreatState* FProjectAirDefenseBattleSimulation::SelectNextThreat() {
  const double EngagementRangeMeters = EffectiveEngagementRange(this->Settings);
  const double EngagementRangeSquared = FMath::Square(EngagementRangeMeters);
  TMap<FString, int32> AssignmentCounts;
  for (const FProjectAirDefenseInterceptorState& Interceptor : this->Interceptors) {
    if (!Interceptor.TargetId.IsEmpty()) {
      int32& Count = AssignmentCounts.FindOrAdd(Interceptor.TargetId);
      ++Count;
    }
  }

  FProjectAirDefenseThreatState* BestThreat = nullptr;
  double BestScore = -TNumericLimits<double>::Max();
  const FDoctrineProfile Profile = DoctrineProfile(this->Settings.Doctrine);
  for (FProjectAirDefenseThreatState& Threat : this->Threats) {
    const FVector3d HorizontalThreat(Threat.PositionMeters.X, Threat.PositionMeters.Y, 0.0);
    if (!Threat.bIsTracked ||
        (HorizontalThreat - this->DefenseOriginMeters).SquaredLength() > EngagementRangeSquared) {
      continue;
    }

    const int32 AssignedCount = AssignedCountFor(Threat.Id, AssignmentCounts);
    if (AssignedCount >= Profile.MaxAssignmentsPerThreat) {
      continue;
    }

    const FProjectAirDefenseThreatSnapshot Snapshot{
        Threat.Id,
        Threat.PositionMeters,
        Threat.VelocityMetersPerSecond,
        Threat.TargetPositionMeters,
    };
    const double TimeToImpactSeconds = EstimateTimeToImpact(Snapshot);
    if (AssignedCount > 0 && TimeToImpactSeconds > Profile.RecommitWindowSeconds) {
      continue;
    }

    const double ThreatScore =
        ScoreThreat(Snapshot, EngagementRangeMeters, TimeToImpactSeconds, AssignedCount);
    if (BestThreat == nullptr || ThreatScore > BestScore ||
        (FMath::IsNearlyEqual(ThreatScore, BestScore) &&
         ThreatHasPriorityOver(Threat.PositionMeters, BestThreat->PositionMeters))) {
      BestThreat = &Threat;
      BestScore = ThreatScore;
    }
  }
  return BestThreat;
}

TOptional<FProjectAirDefenseInterceptorLaunchEvent>
FProjectAirDefenseBattleSimulation::LaunchInterceptor(
    const FProjectAirDefenseThreatState& TargetThreat) {
  int32 LauncherIndex = INDEX_NONE;
  double BestDistanceSquared = TNumericLimits<double>::Max();
  for (int32 Index = 0; Index < this->LauncherPositionsMeters.Num(); ++Index) {
    if (this->LauncherReadyInSeconds.IsValidIndex(Index) &&
        this->LauncherReadyInSeconds[Index] > 0.0) {
      continue;
    }
    const double DistanceSquared =
        (this->LauncherPositionsMeters[Index] - TargetThreat.PositionMeters).SquaredLength();
    if (DistanceSquared < BestDistanceSquared) {
      BestDistanceSquared = DistanceSquared;
      LauncherIndex = Index;
    }
  }
  if (LauncherIndex == INDEX_NONE) {
    return {};
  }

  const FVector3d LaunchPosition =
      this->LauncherPositionsMeters[LauncherIndex] + FVector3d(0.0, 0.0, LauncherMuzzleHeightMeters);
  const FVector3d AimPoint =
      PredictInterceptPoint(
          LaunchPosition,
          TargetThreat.PositionMeters,
          TargetThreat.VelocityMetersPerSecond,
          this->Settings.InterceptorSpeedMetersPerSecond);
  const FVector3d Velocity =
      (AimPoint - LaunchPosition).GetSafeNormal() * this->Settings.InterceptorSpeedMetersPerSecond;

  FProjectAirDefenseInterceptorState Interceptor;
  Interceptor.Id = MakeInterceptorId(this->NextInterceptorOrdinal++);
  Interceptor.PositionMeters = LaunchPosition;
  Interceptor.VelocityMetersPerSecond = Velocity;
  Interceptor.TargetId = TargetThreat.Id;
  Interceptor.LauncherIndex = LauncherIndex;
  Interceptor.TrailCooldownSeconds = 0.0;
  this->Interceptors.Add(Interceptor);
  ++this->TotalInterceptorsLaunched;
  this->LauncherReadyInSeconds[LauncherIndex] = EffectiveLauncherReload(this->Settings);

  FProjectAirDefenseInterceptorLaunchEvent LaunchEvent;
  LaunchEvent.InterceptorId = Interceptor.Id;
  LaunchEvent.LauncherIndex = LauncherIndex;
  LaunchEvent.LauncherPositionMeters = LaunchPosition;
  return LaunchEvent;
}

void FProjectAirDefenseBattleSimulation::ResolveImpact(
    const FVector3d& PositionMeters,
    double RadiusMeters,
    bool bHostile,
    FStepAccumulator& Events) {
  FProjectAirDefenseBlastEvent BlastEvent;
  BlastEvent.PositionMeters = PositionMeters;
  BlastEvent.RadiusMeters = RadiusMeters * BlastVisualRadiusScale;
  BlastEvent.GroundCoupling = BlastGroundCoupling(PositionMeters);
  BlastEvent.PeakDamage = (bHostile ? HostileDamageScale : DefenseDamageScale) * BlastEvent.GroundCoupling;
  BlastEvent.Kind = bHostile ? EProjectAirDefenseBlastKind::HostileImpact
                             : EProjectAirDefenseBlastKind::Intercept;
  BlastEvent.bGroundImpact = BlastEvent.GroundCoupling >= 0.95;
  Events.BlastEvents.Add(BlastEvent);

  for (FProjectAirDefenseDistrictCell& DistrictCell : this->DistrictCells) {
    const FBlastDamageSample DamageSample =
        ComputeDistrictDamage(DistrictCell, PositionMeters, RadiusMeters, bHostile);
    if (DamageSample.Damage <= 0.0) {
      continue;
    }
    this->ApplyDistrictImpact(
        DistrictCell,
        DamageSample.Damage,
        DamageSample.BlastDistanceMeters,
        DamageSample.GroundCoupling,
        PositionMeters,
        Events);
  }

  if (bHostile) {
    this->CityIntegrity =
        FMath::Max(0.0, this->CityIntegrity - CityIntegrityLoss(true, false));
  }
}

void FProjectAirDefenseBattleSimulation::ApplyDistrictImpact(
    FProjectAirDefenseDistrictCell& DistrictCell,
    double Damage,
    double BlastDistanceMeters,
    double GroundCoupling,
    const FVector3d& ImpactPositionMeters,
    FStepAccumulator& Events) {
  const double BeforeIntegrity = DistrictCell.Integrity;
  const double BeforeStructuralIntegrity = DistrictCell.StructuralIntegrity;
  const bool bWasCollapsed = DistrictCell.bCollapsed;
  const double AppliedDamage = FMath::Max(Damage, MinimumDistrictDamage);
  DistrictCell.Integrity =
      FMath::Max(0.0, DistrictCell.Integrity - AppliedDamage);
  const double StructuralDamage =
      AppliedDamage * StructuralDamageMultiplier * FMath::Lerp(0.55, 1.15, GroundCoupling);
  DistrictCell.StructuralIntegrity =
      FMath::Max(0.0, DistrictCell.StructuralIntegrity - StructuralDamage);
  DistrictCell.DamagedFloors =
      FMath::Max(DistrictCell.DamagedFloors, DamagedFloorsForStructuralIntegrity(DistrictCell.StructuralIntegrity));
  DistrictCell.CollapsedFloors =
      FMath::Max(DistrictCell.CollapsedFloors, CollapsedFloorsForStructuralIntegrity(DistrictCell.StructuralIntegrity));
  DistrictCell.bCollapsed =
      DistrictCell.StructuralIntegrity <= FullCollapseThreshold ||
      DistrictCell.Integrity <= 0.0 ||
      DistrictCell.CollapsedFloors >= DistrictStructuralFloorCount;
  if (DistrictCell.bCollapsed) {
    DistrictCell.CollapsedFloors = DistrictStructuralFloorCount;
    DistrictCell.DamagedFloors = DistrictStructuralFloorCount;
  }

  if (FMath::IsNearlyEqual(DistrictCell.Integrity, BeforeIntegrity) &&
      FMath::IsNearlyEqual(DistrictCell.StructuralIntegrity, BeforeStructuralIntegrity)) {
    return;
  }

  FProjectAirDefenseDistrictDamageEvent DamageEvent;
  DamageEvent.DistrictId = DistrictCell.Id;
  DamageEvent.Integrity = DistrictCell.Integrity;
  DamageEvent.StructuralIntegrity = DistrictCell.StructuralIntegrity;
  DamageEvent.Damage = AppliedDamage;
  DamageEvent.BlastDistanceMeters = BlastDistanceMeters;
  DamageEvent.GroundCoupling = GroundCoupling;
  DamageEvent.DamagedFloors = DistrictCell.DamagedFloors;
  DamageEvent.CollapsedFloors = DistrictCell.CollapsedFloors;
  DamageEvent.bCollapsed = DistrictCell.bCollapsed;
  DamageEvent.EpicenterMeters = ImpactPositionMeters;
  Events.DistrictDamageEvents.Add(DamageEvent);

  if (!bWasCollapsed && DistrictCell.bCollapsed) {
    ++this->TotalDestroyedDistricts;
    this->CityIntegrity =
        FMath::Max(0.0, this->CityIntegrity - CityIntegrityLoss(false, true));
  }
}

FProjectAirDefenseThreatState*
FProjectAirDefenseBattleSimulation::FindThreat(const FString& ThreatId) {
  return this->Threats.FindByPredicate([&](const FProjectAirDefenseThreatState& Threat) {
    return Threat.Id == ThreatId;
  });
}

const FProjectAirDefenseThreatState*
FProjectAirDefenseBattleSimulation::FindThreat(const FString& ThreatId) const {
  return this->Threats.FindByPredicate([&](const FProjectAirDefenseThreatState& Threat) {
    return Threat.Id == ThreatId;
  });
}
