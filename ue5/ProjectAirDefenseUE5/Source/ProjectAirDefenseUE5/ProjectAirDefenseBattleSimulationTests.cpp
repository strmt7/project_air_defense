#include "ProjectAirDefenseBattleSimulation.h"

#if WITH_DEV_AUTOMATION_TESTS

#include "Misc/AutomationTest.h"

namespace {
TArray<FProjectAirDefenseDistrictCell> MakeDistrictCells() {
  TArray<FProjectAirDefenseDistrictCell> Cells;
  const double HalfExtentMeters = 1600.0;
  const double StepMeters = HalfExtentMeters * 0.9;
  int32 Ordinal = 0;
  for (int32 YIndex = -1; YIndex <= 1; ++YIndex) {
    for (int32 XIndex = -1; XIndex <= 1; ++XIndex) {
      FProjectAirDefenseDistrictCell Cell;
      Cell.Id = FString::Printf(TEXT("D-%02d"), ++Ordinal);
      Cell.LocalPositionMeters =
          FVector3d(static_cast<double>(XIndex) * StepMeters, static_cast<double>(YIndex) * StepMeters, 0.0);
      Cell.RadiusMeters = 220.0;
      Cell.Integrity = 100.0;
      Cells.Add(Cell);
    }
  }
  return Cells;
}

TArray<FVector3d> MakeLauncherPositions() {
  return {
      FVector3d(-950.0, -1700.0, 0.0),
      FVector3d(0.0, -1820.0, 0.0),
      FVector3d(950.0, -1700.0, 0.0),
  };
}

FProjectAirDefenseDefenseSettings MakeSettings(EProjectAirDefenseDefenseDoctrine Doctrine) {
  FProjectAirDefenseDefenseSettings Settings;
  Settings.Doctrine = Doctrine;
  return Settings;
}

FProjectAirDefenseBattleRunSummary RunSingleSimulation(
    int32 Seed,
    EProjectAirDefenseDefenseDoctrine Doctrine,
    int32 Waves,
    double MaxSecondsPerWave) {
  FProjectAirDefenseBattleSimulation Simulation(
      MakeDistrictCells(),
      MakeLauncherPositions(),
      MakeSettings(Doctrine),
      Seed);

  double SimulatedSeconds = 0.0;
  for (int32 WaveIndex = 0; WaveIndex < Waves; ++WaveIndex) {
    if (!Simulation.StartNextWave()) {
      break;
    }
    const int32 MaxSteps = static_cast<int32>(MaxSecondsPerWave / 0.05);
    for (int32 StepIndex = 0; StepIndex < MaxSteps; ++StepIndex) {
      Simulation.Step(0.05);
      SimulatedSeconds += 0.05;
      if (!Simulation.IsWaveInProgress() && Simulation.GetThreats().IsEmpty()) {
        break;
      }
      if (Simulation.IsGameOver()) {
        break;
      }
    }
    if (Simulation.IsGameOver()) {
      break;
    }
  }

  return Simulation.BuildRunSummary(Seed, SimulatedSeconds);
}
} // namespace

IMPLEMENT_SIMPLE_AUTOMATION_TEST(
    FProjectAirDefenseBattleSimulationAccountingTest,
    "ProjectAirDefense.BattleSimulation.AccountingConsistency",
    EAutomationTestFlags::EditorContext | EAutomationTestFlags::EngineFilter)

bool FProjectAirDefenseBattleSimulationAccountingTest::RunTest(const FString& Parameters) {
  FProjectAirDefenseBattleSimulation Simulation(
      MakeDistrictCells(),
      MakeLauncherPositions(),
      MakeSettings(EProjectAirDefenseDefenseDoctrine::ShieldWall),
      42);

  TestTrue(TEXT("first wave starts"), Simulation.StartNextWave());

  for (int32 Index = 0; Index < 700; ++Index) {
    Simulation.Step(0.05);
    if (!Simulation.IsWaveInProgress() && Simulation.GetThreats().IsEmpty()) {
      break;
    }
  }

  const FProjectAirDefenseBattleRunSummary Summary = Simulation.BuildRunSummary(1, 35.0);
  TestTrue(TEXT("threats spawned"), Summary.ThreatsSpawned > 0);
  TestTrue(TEXT("interceptors launched cover intercepts"), Summary.InterceptorsLaunched >= Summary.ThreatsIntercepted);
  TestTrue(
      TEXT("intercepts plus impacts bounded"),
      Summary.ThreatsIntercepted + Summary.HostileImpacts <= Summary.ThreatsSpawned);
  TestTrue(TEXT("city integrity bounded"), Summary.CityIntegrity >= 0.0 && Summary.CityIntegrity <= 100.0);
  return true;
}

IMPLEMENT_SIMPLE_AUTOMATION_TEST(
    FProjectAirDefenseBattleSimulationLeakageTest,
    "ProjectAirDefense.BattleSimulation.NonZeroLeakage",
    EAutomationTestFlags::EditorContext | EAutomationTestFlags::EngineFilter)

bool FProjectAirDefenseBattleSimulationLeakageTest::RunTest(const FString& Parameters) {
  double InterceptRateSum = 0.0;
  bool bSawLeak = false;
  bool bSawStrongHold = false;
  constexpr int32 Runs = 24;

  for (int32 RunIndex = 0; RunIndex < Runs; ++RunIndex) {
    const FProjectAirDefenseBattleRunSummary Summary =
        RunSingleSimulation(9000 + RunIndex, EProjectAirDefenseDefenseDoctrine::ShieldWall, 1, 48.0);
    InterceptRateSum += Summary.InterceptRate();
    bSawLeak = bSawLeak || Summary.HostileImpacts > 0;
    bSawStrongHold = bSawStrongHold || Summary.CityIntegrity >= 70.0;
  }

  const double AverageInterceptRate = InterceptRateSum / static_cast<double>(Runs);
  TestTrue(TEXT("expected at least one leak"), bSawLeak);
  TestTrue(TEXT("expected some strong holds"), bSawStrongHold);
  TestTrue(TEXT("average intercept rate above floor"), AverageInterceptRate > 0.45);
  TestTrue(TEXT("average intercept rate below perfection"), AverageInterceptRate < 0.98);
  return true;
}

IMPLEMENT_SIMPLE_AUTOMATION_TEST(
    FProjectAirDefenseBattleDoctrineComparisonTest,
    "ProjectAirDefense.BattleSimulation.DoctrineComparison",
    EAutomationTestFlags::EditorContext | EAutomationTestFlags::EngineFilter)

bool FProjectAirDefenseBattleDoctrineComparisonTest::RunTest(const FString& Parameters) {
  double DisciplinedInterceptRate = 0.0;
  double ShieldWallInterceptRate = 0.0;
  double DisciplinedIntegrity = 0.0;
  double ShieldWallIntegrity = 0.0;
  constexpr int32 Runs = 24;

  for (int32 RunIndex = 0; RunIndex < Runs; ++RunIndex) {
    const int32 Seed = 12000 + RunIndex;
    const FProjectAirDefenseBattleRunSummary Disciplined =
        RunSingleSimulation(Seed, EProjectAirDefenseDefenseDoctrine::Disciplined, 1, 48.0);
    const FProjectAirDefenseBattleRunSummary ShieldWall =
        RunSingleSimulation(Seed, EProjectAirDefenseDefenseDoctrine::ShieldWall, 1, 48.0);
    DisciplinedInterceptRate += Disciplined.InterceptRate();
    ShieldWallInterceptRate += ShieldWall.InterceptRate();
    DisciplinedIntegrity += Disciplined.CityIntegrity;
    ShieldWallIntegrity += ShieldWall.CityIntegrity;
  }

  DisciplinedInterceptRate /= static_cast<double>(Runs);
  ShieldWallInterceptRate /= static_cast<double>(Runs);
  DisciplinedIntegrity /= static_cast<double>(Runs);
  ShieldWallIntegrity /= static_cast<double>(Runs);

  TestTrue(TEXT("shield wall improves intercept rate"), ShieldWallInterceptRate > DisciplinedInterceptRate);
  TestTrue(TEXT("shield wall does not reduce city integrity"), ShieldWallIntegrity >= DisciplinedIntegrity);
  return true;
}

IMPLEMENT_SIMPLE_AUTOMATION_TEST(
    FProjectAirDefenseBattleThreatProfileMixTest,
    "ProjectAirDefense.BattleSimulation.ThreatProfileMix",
    EAutomationTestFlags::EditorContext | EAutomationTestFlags::EngineFilter)

bool FProjectAirDefenseBattleThreatProfileMixTest::RunTest(const FString& Parameters) {
  FProjectAirDefenseBattleSimulation Simulation(
      MakeDistrictCells(),
      MakeLauncherPositions(),
      MakeSettings(EProjectAirDefenseDefenseDoctrine::Adaptive),
      31415);

  TestTrue(TEXT("wave starts"), Simulation.StartNextWave());

  TSet<EProjectAirDefenseThreatType> SeenThreatTypes;
  for (int32 Index = 0; Index < 400; ++Index) {
    Simulation.Step(0.05);
    for (const FProjectAirDefenseThreatState& Threat : Simulation.GetThreats()) {
      SeenThreatTypes.Add(Threat.ThreatType);
    }
    if (SeenThreatTypes.Num() >= 2) {
      break;
    }
  }

  TestTrue(TEXT("seeded wave includes multiple threat behaviors"), SeenThreatTypes.Num() >= 2);
  return true;
}

IMPLEMENT_SIMPLE_AUTOMATION_TEST(
    FProjectAirDefenseBattleHostileImpactTest,
    "ProjectAirDefense.BattleSimulation.HostileImpact",
    EAutomationTestFlags::EditorContext | EAutomationTestFlags::EngineFilter)

bool FProjectAirDefenseBattleHostileImpactTest::RunTest(const FString& Parameters) {
  FProjectAirDefenseDefenseSettings WeakerSettings = MakeSettings(EProjectAirDefenseDefenseDoctrine::Disciplined);
  WeakerSettings.EngagementRangeMeters = 1700.0;
  WeakerSettings.InterceptorSpeedMetersPerSecond = 420.0;
  WeakerSettings.LaunchCooldownSeconds = 0.75;
  WeakerSettings.BlastRadiusMeters = 58.0;
  FProjectAirDefenseBattleSimulation Simulation(
      MakeDistrictCells(),
      MakeLauncherPositions(),
      WeakerSettings,
      88);

  TestTrue(TEXT("wave starts"), Simulation.StartNextWave());
  for (int32 Index = 0; Index < 400; ++Index) {
    const FProjectAirDefenseStepEvents Events = Simulation.Step(0.05);
    if (!Events.DistrictDamageEvents.IsEmpty()) {
      TestTrue(TEXT("hostile impact produces damage"), !Events.DistrictDamageEvents.IsEmpty());
      TestTrue(TEXT("hostile impact produces blast"), !Events.BlastEvents.IsEmpty());
      TestTrue(TEXT("city integrity decreased"), Simulation.GetCityIntegrity() < 100.0);
      TestTrue(
          TEXT("hostile impact produces structural damage"),
          Events.DistrictDamageEvents[0].StructuralIntegrity < 100.0);
      TestTrue(
          TEXT("hostile impact reports blast coupling"),
          Events.DistrictDamageEvents[0].GroundCoupling > 0.0);
      return true;
    }
  }

  AddError(TEXT("expected a hostile impact during the seeded run"));
  return false;
}

#endif
