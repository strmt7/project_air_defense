#include "ProjectAirDefenseBattleSimulation.h"
#include "ProjectAirDefenseRuntimeSettings.h"

#if WITH_DEV_AUTOMATION_TESTS

#include "Misc/AutomationTest.h"

#include <limits>

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

int32 MaxAssignedInterceptorCount(
    const FProjectAirDefenseBattleSimulation& Simulation,
    FString* OutTargetId = nullptr) {
  TMap<FString, int32> AssignmentCounts;
  for (const FProjectAirDefenseInterceptorState& Interceptor : Simulation.GetInterceptors()) {
    if (!Interceptor.TargetId.IsEmpty()) {
      int32& Count = AssignmentCounts.FindOrAdd(Interceptor.TargetId);
      ++Count;
    }
  }

  int32 BestCount = 0;
  FString BestTargetId;
  for (const TPair<FString, int32>& Pair : AssignmentCounts) {
    if (Pair.Value > BestCount) {
      BestCount = Pair.Value;
      BestTargetId = Pair.Key;
    }
  }
  if (OutTargetId != nullptr) {
    *OutTargetId = BestTargetId;
  }
  return BestCount;
}

const FProjectAirDefenseThreatState* FindThreatById(
    const FProjectAirDefenseBattleSimulation& Simulation,
    const FString& ThreatId) {
  for (const FProjectAirDefenseThreatState& Threat : Simulation.GetThreats()) {
    if (Threat.Id == ThreatId) {
      return &Threat;
    }
  }
  return nullptr;
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
  // This test exercises the hostile-impact damage path directly; do not rely
  // on stochastic leakage from a merely weak defense profile.
  WeakerSettings.KillProbabilityAtZeroMiss = 0.0;
  WeakerSettings.KillProbabilityFuseFloor = 0.0;
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

namespace {
FProjectAirDefenseBattleRunSummary RunWithOverrideSettings(
    int32 Seed,
    const FProjectAirDefenseDefenseSettings& Settings,
    int32 Waves,
    double MaxSecondsPerWave) {
  FProjectAirDefenseBattleSimulation Simulation(
      MakeDistrictCells(), MakeLauncherPositions(), Settings, Seed);

  double SimulatedSeconds = 0.0;
  for (int32 WaveIndex = 0; WaveIndex < Waves; ++WaveIndex) {
    if (!Simulation.StartNextWave()) {
      break;
    }
    const int32 MaxSteps = static_cast<int32>(MaxSecondsPerWave / 0.05);
    for (int32 StepIndex = 0; StepIndex < MaxSteps; ++StepIndex) {
      Simulation.Step(0.05);
      SimulatedSeconds += 0.05;
      if ((!Simulation.IsWaveInProgress() && Simulation.GetThreats().IsEmpty()) ||
          Simulation.IsGameOver()) {
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
    FProjectAirDefenseBattleKillProbabilityZeroTest,
    "ProjectAirDefense.BattleSimulation.KillProbabilityZero",
    EAutomationTestFlags::EditorContext | EAutomationTestFlags::EngineFilter)

bool FProjectAirDefenseBattleKillProbabilityZeroTest::RunTest(const FString& Parameters) {
  // With kill probability clamped to zero at both miss-distance endpoints, every
  // fuse-closure roll must fail. The simulation must still launch interceptors
  // and still record miss-distance samples; only the kill count must stay at 0.
  FProjectAirDefenseDefenseSettings Settings =
      MakeSettings(EProjectAirDefenseDefenseDoctrine::Adaptive);
  Settings.KillProbabilityAtZeroMiss = 0.0;
  Settings.KillProbabilityFuseFloor = 0.0;

  const FProjectAirDefenseBattleRunSummary Summary =
      RunWithOverrideSettings(51234, Settings, 1, 48.0);

  TestTrue(TEXT("interceptors still launched"), Summary.InterceptorsLaunched > 0);
  TestEqual(TEXT("no kills when Pk is zero"), Summary.ThreatsIntercepted, 0);
  TestEqual(
      TEXT("InterceptorsKilledTarget mirrors ThreatsIntercepted"),
      Summary.InterceptorsKilledTarget,
      Summary.ThreatsIntercepted);
  TestTrue(
      TEXT("at least one fuse roll recorded as miss"),
      Summary.InterceptorsFuseRollMissed > 0);
  TestTrue(
      TEXT("miss-distance samples recorded when fuse closed"),
      Summary.MissDistanceSampleCount > 0);
  return true;
}

IMPLEMENT_SIMPLE_AUTOMATION_TEST(
    FProjectAirDefenseBattleKillProbabilityOneTest,
    "ProjectAirDefense.BattleSimulation.KillProbabilityOne",
    EAutomationTestFlags::EditorContext | EAutomationTestFlags::EngineFilter)

bool FProjectAirDefenseBattleKillProbabilityOneTest::RunTest(const FString& Parameters) {
  // With kill probability pinned to one at both endpoints, every fuse-closure
  // that survives the seeker-cone check must kill its target. No fuse roll may
  // register as a miss.
  FProjectAirDefenseDefenseSettings Settings =
      MakeSettings(EProjectAirDefenseDefenseDoctrine::Adaptive);
  Settings.KillProbabilityAtZeroMiss = 1.0;
  Settings.KillProbabilityFuseFloor = 1.0;

  const FProjectAirDefenseBattleRunSummary Summary =
      RunWithOverrideSettings(88442, Settings, 1, 48.0);

  TestTrue(TEXT("interceptors launched"), Summary.InterceptorsLaunched > 0);
  TestTrue(TEXT("at least one kill registered"), Summary.ThreatsIntercepted > 0);
  TestEqual(TEXT("no fuse-roll misses when Pk is one"), Summary.InterceptorsFuseRollMissed, 0);
  return true;
}

IMPLEMENT_SIMPLE_AUTOMATION_TEST(
    FProjectAirDefenseBattleTerminalPhaseTest,
    "ProjectAirDefense.BattleSimulation.TerminalPhaseActivation",
    EAutomationTestFlags::EditorContext | EAutomationTestFlags::EngineFilter)

bool FProjectAirDefenseBattleTerminalPhaseTest::RunTest(const FString& Parameters) {
  // Over a normal run, at least some kills must complete while the interceptor
  // is in proportional-navigation terminal phase. If the threshold or
  // activation logic regresses, this counter goes to zero.
  constexpr int32 RunCount = 8;
  int32 TerminalKillsTotal = 0;
  int32 AnyInterceptsTotal = 0;
  for (int32 RunIndex = 0; RunIndex < RunCount; ++RunIndex) {
    const FProjectAirDefenseBattleRunSummary Summary =
        RunSingleSimulation(
            70000 + RunIndex, EProjectAirDefenseDefenseDoctrine::Adaptive, 1, 48.0);
    TerminalKillsTotal += Summary.InterceptsInTerminalPhase;
    AnyInterceptsTotal += Summary.ThreatsIntercepted;
  }
  TestTrue(TEXT("intercepts occurred during run set"), AnyInterceptsTotal > 0);
  TestTrue(
      TEXT("terminal-phase PN still active in at least one kill"),
      TerminalKillsTotal > 0);
  TestTrue(
      TEXT("terminal-phase kills do not exceed total intercepts"),
      TerminalKillsTotal <= AnyInterceptsTotal);
  return true;
}

IMPLEMENT_SIMPLE_AUTOMATION_TEST(
    FProjectAirDefenseBattleShootLookShootTest,
    "ProjectAirDefense.BattleSimulation.ShootLookShootAdvantage",
    EAutomationTestFlags::EditorContext | EAutomationTestFlags::EngineFilter)

bool FProjectAirDefenseBattleShootLookShootTest::RunTest(const FString& Parameters) {
  // With a deliberately low per-shot kill probability, the two-interceptor
  // ShieldWall doctrine must outperform the one-interceptor Disciplined doctrine.
  // Analytically: if single-shot Pk = 0.5, combined kill chance per threat is
  // 1 - (1-0.5)^2 = 0.75, so ShieldWall's intercept rate should comfortably
  // exceed Disciplined's.
  FProjectAirDefenseDefenseSettings BaseSettings = MakeSettings(EProjectAirDefenseDefenseDoctrine::Disciplined);
  BaseSettings.KillProbabilityAtZeroMiss = 0.50;
  BaseSettings.KillProbabilityFuseFloor = 0.50;

  constexpr int32 RunCount = 16;
  double DisciplinedInterceptSum = 0.0;
  double ShieldWallInterceptSum = 0.0;
  for (int32 RunIndex = 0; RunIndex < RunCount; ++RunIndex) {
    FProjectAirDefenseDefenseSettings Disciplined = BaseSettings;
    Disciplined.Doctrine = EProjectAirDefenseDefenseDoctrine::Disciplined;
    FProjectAirDefenseDefenseSettings ShieldWall = BaseSettings;
    ShieldWall.Doctrine = EProjectAirDefenseDefenseDoctrine::ShieldWall;

    DisciplinedInterceptSum +=
        RunWithOverrideSettings(31000 + RunIndex, Disciplined, 1, 48.0).InterceptRate();
    ShieldWallInterceptSum +=
        RunWithOverrideSettings(31000 + RunIndex, ShieldWall, 1, 48.0).InterceptRate();
  }

  const double DisciplinedAverage = DisciplinedInterceptSum / static_cast<double>(RunCount);
  const double ShieldWallAverage = ShieldWallInterceptSum / static_cast<double>(RunCount);
  TestTrue(
      TEXT("ShieldWall intercept rate beats Disciplined at low Pk"),
      ShieldWallAverage > DisciplinedAverage + 0.05);
  TestTrue(
      TEXT("Disciplined average above the theoretical floor"),
      DisciplinedAverage > 0.15);
  TestTrue(
      TEXT("ShieldWall average below one (Pk is still 0.5)"),
      ShieldWallAverage < 0.99);
  return true;
}

IMPLEMENT_SIMPLE_AUTOMATION_TEST(
    FProjectAirDefenseBattleTacticalRangeFireControlTest,
    "ProjectAirDefense.BattleSimulation.TacticalRangeAndFireControl",
    EAutomationTestFlags::EditorContext | EAutomationTestFlags::EngineFilter)

bool FProjectAirDefenseBattleTacticalRangeFireControlTest::RunTest(const FString& Parameters) {
  FProjectAirDefenseDefenseSettings EarlyWide =
      MakeSettings(EProjectAirDefenseDefenseDoctrine::Adaptive);
  EarlyWide.EngagementRangeMeters =
      FProjectAirDefenseBattleSimulation::MaxConfigurableEngagementRangeMeters();
  EarlyWide.FireControlMode = EProjectAirDefenseFireControlMode::Early;
  EarlyWide.EngagementMode = EProjectAirDefenseEngagementMode::Single;

  FProjectAirDefenseDefenseSettings TerminalNarrow = EarlyWide;
  TerminalNarrow.EngagementRangeMeters =
      FProjectAirDefenseBattleSimulation::MinConfigurableEngagementRangeMeters();
  TerminalNarrow.FireControlMode = EProjectAirDefenseFireControlMode::Terminal;

  constexpr int32 RunCount = 10;
  int32 EarlyWideLaunches = 0;
  int32 TerminalNarrowLaunches = 0;
  for (int32 RunIndex = 0; RunIndex < RunCount; ++RunIndex) {
    const int32 Seed = 61000 + RunIndex;
    EarlyWideLaunches +=
        RunWithOverrideSettings(Seed, EarlyWide, 1, 48.0).InterceptorsLaunched;
    TerminalNarrowLaunches +=
        RunWithOverrideSettings(Seed, TerminalNarrow, 1, 48.0).InterceptorsLaunched;
  }

  TestTrue(TEXT("early wide control creates more launch opportunities"),
      EarlyWideLaunches > TerminalNarrowLaunches);
  TestTrue(TEXT("terminal narrow control still launches at least once"),
      TerminalNarrowLaunches > 0);
  return true;
}

IMPLEMENT_SIMPLE_AUTOMATION_TEST(
    FProjectAirDefenseBattleEngagementModeTest,
    "ProjectAirDefense.BattleSimulation.EngagementModeControlsSalvoBudget",
    EAutomationTestFlags::EditorContext | EAutomationTestFlags::EngineFilter)

bool FProjectAirDefenseBattleEngagementModeTest::RunTest(const FString& Parameters) {
  FProjectAirDefenseDefenseSettings Base =
      MakeSettings(EProjectAirDefenseDefenseDoctrine::ShieldWall);
  Base.EngagementRangeMeters =
      FProjectAirDefenseBattleSimulation::MaxConfigurableEngagementRangeMeters();
  Base.FireControlMode = EProjectAirDefenseFireControlMode::Early;
  Base.LaunchCooldownSeconds = 0.16;
  Base.KillProbabilityAtZeroMiss = 0.0;
  Base.KillProbabilityFuseFloor = 0.0;

  FProjectAirDefenseDefenseSettings Single = Base;
  Single.EngagementMode = EProjectAirDefenseEngagementMode::Single;
  FProjectAirDefenseDefenseSettings Pair = Base;
  Pair.EngagementMode = EProjectAirDefenseEngagementMode::Pair;
  FProjectAirDefenseDefenseSettings Ripple = Base;
  Ripple.EngagementMode = EProjectAirDefenseEngagementMode::Ripple;

  constexpr int32 RunCount = 8;
  int32 SingleLaunches = 0;
  int32 PairLaunches = 0;
  int32 RippleLaunches = 0;
  for (int32 RunIndex = 0; RunIndex < RunCount; ++RunIndex) {
    const int32 Seed = 62000 + RunIndex;
    SingleLaunches += RunWithOverrideSettings(Seed, Single, 1, 54.0).InterceptorsLaunched;
    PairLaunches += RunWithOverrideSettings(Seed, Pair, 1, 54.0).InterceptorsLaunched;
    RippleLaunches += RunWithOverrideSettings(Seed, Ripple, 1, 54.0).InterceptorsLaunched;
  }

  TestTrue(TEXT("pair mode launches more interceptors than single mode"),
      PairLaunches > SingleLaunches);
  TestTrue(TEXT("ripple mode launches more interceptors than pair mode"),
      RippleLaunches > PairLaunches);
  return true;
}

IMPLEMENT_SIMPLE_AUTOMATION_TEST(
    FProjectAirDefenseBattleInitialSalvoWindowTest,
    "ProjectAirDefense.BattleSimulation.EngagementModeCreatesInitialSalvo",
    EAutomationTestFlags::EditorContext | EAutomationTestFlags::EngineFilter)

bool FProjectAirDefenseBattleInitialSalvoWindowTest::RunTest(const FString& Parameters) {
  FProjectAirDefenseDefenseSettings PairSettings =
      MakeSettings(EProjectAirDefenseDefenseDoctrine::Adaptive);
  PairSettings.EngagementRangeMeters =
      FProjectAirDefenseBattleSimulation::MaxConfigurableEngagementRangeMeters();
  PairSettings.FireControlMode = EProjectAirDefenseFireControlMode::Early;
  PairSettings.EngagementMode = EProjectAirDefenseEngagementMode::Pair;
  PairSettings.LaunchCooldownSeconds = 0.16;
  PairSettings.KillProbabilityAtZeroMiss = 0.0;
  PairSettings.KillProbabilityFuseFloor = 0.0;

  FProjectAirDefenseDefenseSettings SingleSettings = PairSettings;
  SingleSettings.EngagementMode = EProjectAirDefenseEngagementMode::Single;

  FProjectAirDefenseBattleSimulation PairSimulation(
      MakeDistrictCells(), MakeLauncherPositions(), PairSettings, 72111);
  FProjectAirDefenseBattleSimulation SingleSimulation(
      MakeDistrictCells(), MakeLauncherPositions(), SingleSettings, 72111);
  TestTrue(TEXT("pair wave starts"), PairSimulation.StartNextWave());
  TestTrue(TEXT("single wave starts"), SingleSimulation.StartNextWave());

  bool bPairCreatedPreTerminalSalvo = false;
  int32 SingleMaxAssigned = 0;
  for (int32 StepIndex = 0; StepIndex < 120; ++StepIndex) {
    PairSimulation.Step(0.05);
    SingleSimulation.Step(0.05);

    FString PairTargetId;
    if (MaxAssignedInterceptorCount(PairSimulation, &PairTargetId) >= 2) {
      const FProjectAirDefenseThreatState* Threat =
          FindThreatById(PairSimulation, PairTargetId);
      bPairCreatedPreTerminalSalvo =
          Threat != nullptr && Threat->PositionMeters.Y < -1000.0;
      if (bPairCreatedPreTerminalSalvo) {
        break;
      }
    }
    SingleMaxAssigned =
        FMath::Max(SingleMaxAssigned, MaxAssignedInterceptorCount(SingleSimulation));
  }

  TestTrue(
      TEXT("pair mode creates a coordinated in-flight salvo before terminal range"),
      bPairCreatedPreTerminalSalvo);
  TestTrue(
      TEXT("single mode never assigns a second in-flight interceptor to one threat"),
      SingleMaxAssigned < 2);
  return true;
}

IMPLEMENT_SIMPLE_AUTOMATION_TEST(
    FProjectAirDefenseBattleThreatPriorityTest,
    "ProjectAirDefense.BattleSimulation.ThreatPriorityBiasesLaunchMix",
    EAutomationTestFlags::EditorContext | EAutomationTestFlags::EngineFilter)

bool FProjectAirDefenseBattleThreatPriorityTest::RunTest(const FString& Parameters) {
  FProjectAirDefenseDefenseSettings Base =
      MakeSettings(EProjectAirDefenseDefenseDoctrine::Adaptive);
  Base.EngagementRangeMeters =
      FProjectAirDefenseBattleSimulation::MaxConfigurableEngagementRangeMeters();
  Base.FireControlMode = EProjectAirDefenseFireControlMode::Early;
  Base.EngagementMode = EProjectAirDefenseEngagementMode::Ripple;
  Base.LaunchCooldownSeconds = 1.4;
  Base.KillProbabilityAtZeroMiss = 0.0;
  Base.KillProbabilityFuseFloor = 0.0;

  FProjectAirDefenseDefenseSettings BallisticPriority = Base;
  BallisticPriority.ThreatPriority = EProjectAirDefenseThreatPriority::BallisticFirst;
  FProjectAirDefenseDefenseSettings GlidePriority = Base;
  GlidePriority.ThreatPriority = EProjectAirDefenseThreatPriority::GlideFirst;

  static constexpr int32 BallisticIndex = static_cast<int32>(EProjectAirDefenseThreatType::Ballistic);
  static constexpr int32 GlideIndex = static_cast<int32>(EProjectAirDefenseThreatType::Glide);
  constexpr int32 RunCount = 24;
  int32 BallisticPriorityBallisticTargets = 0;
  int32 BallisticPriorityGlideTargets = 0;
  int32 GlidePriorityBallisticTargets = 0;
  int32 GlidePriorityGlideTargets = 0;
  for (int32 RunIndex = 0; RunIndex < RunCount; ++RunIndex) {
    const int32 Seed = 63000 + RunIndex;
    const FProjectAirDefenseBattleRunSummary BallisticSummary =
        RunWithOverrideSettings(Seed, BallisticPriority, 1, 32.0);
    const FProjectAirDefenseBattleRunSummary GlideSummary =
        RunWithOverrideSettings(Seed, GlidePriority, 1, 32.0);
    BallisticPriorityBallisticTargets += BallisticSummary.LaunchedAtByType[BallisticIndex];
    BallisticPriorityGlideTargets += BallisticSummary.LaunchedAtByType[GlideIndex];
    GlidePriorityBallisticTargets += GlideSummary.LaunchedAtByType[BallisticIndex];
    GlidePriorityGlideTargets += GlideSummary.LaunchedAtByType[GlideIndex];
  }

  AddInfo(FString::Printf(
      TEXT("priority saturated-target counts: ballisticPriority(ballistic=%d glide=%d) glidePriority(ballistic=%d glide=%d)"),
      BallisticPriorityBallisticTargets,
      BallisticPriorityGlideTargets,
      GlidePriorityBallisticTargets,
      GlidePriorityGlideTargets));
  TestTrue(
      TEXT("ballistic priority increases ballistic launcher assignments"),
      BallisticPriorityBallisticTargets > GlidePriorityBallisticTargets);
  TestTrue(
      TEXT("glide priority increases glide launcher assignments"),
      GlidePriorityGlideTargets > BallisticPriorityGlideTargets);
  return true;
}

IMPLEMENT_SIMPLE_AUTOMATION_TEST(
    FProjectAirDefenseBattleDeterminismTest,
    "ProjectAirDefense.BattleSimulation.DeterminismOnSameSeed",
    EAutomationTestFlags::EditorContext | EAutomationTestFlags::EngineFilter)

bool FProjectAirDefenseBattleDeterminismTest::RunTest(const FString& Parameters) {
  // Critical objective-benchmarking invariant: the seeded simulation must
  // produce the same summary counters across two independent runs. If the new
  // threat-motion uncertainty or fuse-roll randomness accidentally draws from
  // a non-seeded source, this test fails.
  const FProjectAirDefenseBattleRunSummary First =
      RunSingleSimulation(424242, EProjectAirDefenseDefenseDoctrine::Adaptive, 1, 48.0);
  const FProjectAirDefenseBattleRunSummary Second =
      RunSingleSimulation(424242, EProjectAirDefenseDefenseDoctrine::Adaptive, 1, 48.0);
  TestEqual(TEXT("threats spawned match"), First.ThreatsSpawned, Second.ThreatsSpawned);
  TestEqual(TEXT("threats intercepted match"), First.ThreatsIntercepted, Second.ThreatsIntercepted);
  TestEqual(TEXT("hostile impacts match"), First.HostileImpacts, Second.HostileImpacts);
  TestEqual(TEXT("fuse-roll misses match"), First.InterceptorsFuseRollMissed, Second.InterceptorsFuseRollMissed);
  TestEqual(
      TEXT("terminal-phase kills match"),
      First.InterceptsInTerminalPhase,
      Second.InterceptsInTerminalPhase);
  TestEqual(
      TEXT("miss-distance sample counts match"),
      First.MissDistanceSampleCount,
      Second.MissDistanceSampleCount);
  return true;
}

IMPLEMENT_SIMPLE_AUTOMATION_TEST(
    FProjectAirDefenseBattleStepDeltaHandlingTest,
    "ProjectAirDefense.BattleSimulation.StepDeltaHandling",
    EAutomationTestFlags::EditorContext | EAutomationTestFlags::EngineFilter)

bool FProjectAirDefenseBattleStepDeltaHandlingTest::RunTest(const FString& Parameters) {
  FProjectAirDefenseBattleSimulation NoopSimulation(
      MakeDistrictCells(),
      MakeLauncherPositions(),
      MakeSettings(EProjectAirDefenseDefenseDoctrine::Adaptive),
      81818);
  TestTrue(TEXT("noop wave starts"), NoopSimulation.StartNextWave());
  TestEqual(TEXT("negative delta produces no spawns"), NoopSimulation.Step(-0.25).SpawnedThreatIds.Num(), 0);
  TestEqual(
      TEXT("non-finite delta produces no spawns"),
      NoopSimulation.Step(std::numeric_limits<double>::infinity()).SpawnedThreatIds.Num(),
      0);
  TestEqual(
      TEXT("invalid deltas do not consume or rewind the spawn timer"),
      NoopSimulation.Step(0.45).SpawnedThreatIds.Num(),
      1);

  FProjectAirDefenseBattleSimulation BurstSimulation(
      MakeDistrictCells(),
      MakeLauncherPositions(),
      MakeSettings(EProjectAirDefenseDefenseDoctrine::Adaptive),
      81819);
  FProjectAirDefenseBattleSimulation IncrementalSimulation(
      MakeDistrictCells(),
      MakeLauncherPositions(),
      MakeSettings(EProjectAirDefenseDefenseDoctrine::Adaptive),
      81819);
  TestTrue(TEXT("burst wave starts"), BurstSimulation.StartNextWave());
  TestTrue(TEXT("incremental wave starts"), IncrementalSimulation.StartNextWave());
  const FProjectAirDefenseStepEvents BurstEvents = BurstSimulation.Step(0.5);
  int32 IncrementalSpawnCount = 0;
  for (int32 StepIndex = 0; StepIndex < 10; ++StepIndex) {
    IncrementalSpawnCount += IncrementalSimulation.Step(0.05).SpawnedThreatIds.Num();
  }

  TestEqual(
      TEXT("large delta substeps produce the same spawn count as incremental ticks"),
      BurstEvents.SpawnedThreatIds.Num(),
      IncrementalSpawnCount);
  TestEqual(
      TEXT("large delta substeps leave the same active threat count"),
      BurstSimulation.GetThreats().Num(),
      IncrementalSimulation.GetThreats().Num());
  TestEqual(
      TEXT("large delta substeps leave the same remaining wave count"),
      BurstSimulation.GetThreatsRemainingInWave(),
      IncrementalSimulation.GetThreatsRemainingInWave());
  if (!BurstSimulation.GetThreats().IsEmpty() && !IncrementalSimulation.GetThreats().IsEmpty()) {
    const double PositionDeltaMeters =
        (BurstSimulation.GetThreats()[0].PositionMeters -
         IncrementalSimulation.GetThreats()[0].PositionMeters)
            .Length();
    TestTrue(TEXT("large delta substep motion matches incremental motion"), PositionDeltaMeters < 0.001);
  }
  return true;
}

IMPLEMENT_SIMPLE_AUTOMATION_TEST(
    FProjectAirDefenseBattleSeekerConeTest,
    "ProjectAirDefense.BattleSimulation.SeekerConeGate",
    EAutomationTestFlags::EditorContext | EAutomationTestFlags::EngineFilter)

bool FProjectAirDefenseBattleSeekerConeTest::RunTest(const FString& Parameters) {
  // With a vanishingly narrow seeker cone, the terminal seeker almost never
  // gets a valid lock and intercepts collapse to near zero. A wide cone
  // restores normal performance. This exercises the new seeker-cone gate and
  // proves it is a live parameter, not ignored.
  FProjectAirDefenseDefenseSettings NarrowConeSettings =
      MakeSettings(EProjectAirDefenseDefenseDoctrine::Adaptive);
  NarrowConeSettings.SeekerConeDegrees = 2.0;
  FProjectAirDefenseDefenseSettings WideConeSettings =
      MakeSettings(EProjectAirDefenseDefenseDoctrine::Adaptive);
  WideConeSettings.SeekerConeDegrees = 170.0;

  constexpr int32 RunCount = 6;
  double NarrowInterceptSum = 0.0;
  double WideInterceptSum = 0.0;
  for (int32 RunIndex = 0; RunIndex < RunCount; ++RunIndex) {
    NarrowInterceptSum +=
        RunWithOverrideSettings(91000 + RunIndex, NarrowConeSettings, 1, 48.0).InterceptRate();
    WideInterceptSum +=
        RunWithOverrideSettings(91000 + RunIndex, WideConeSettings, 1, 48.0).InterceptRate();
  }
  const double NarrowAverage = NarrowInterceptSum / static_cast<double>(RunCount);
  const double WideAverage = WideInterceptSum / static_cast<double>(RunCount);
  TestTrue(
      TEXT("wide seeker cone outperforms narrow cone"),
      WideAverage > NarrowAverage + 0.15);
  TestTrue(TEXT("narrow cone still below reasonable ceiling"), NarrowAverage < 0.5);
  return true;
}

IMPLEMENT_SIMPLE_AUTOMATION_TEST(
    FProjectAirDefenseBattleSettingsSanitizationTest,
    "ProjectAirDefense.BattleSimulation.SettingsSanitization",
    EAutomationTestFlags::EditorContext | EAutomationTestFlags::EngineFilter)

bool FProjectAirDefenseBattleSettingsSanitizationTest::RunTest(const FString& Parameters) {
  // Confirm that grossly-out-of-range settings values are clamped to
  // physically sensible ranges by the simulation constructor. Protects the
  // rest of the codebase from assuming a caller-provided probability is in
  // [0, 1] or that a kill-probability floor is at or below the zero-miss
  // value.
  FProjectAirDefenseDefenseSettings Bad;
  Bad.EngagementRangeMeters = -1000.0;
  Bad.InterceptorSpeedMetersPerSecond = -50.0;
  Bad.LaunchCooldownSeconds = -5.0;
  Bad.BlastRadiusMeters = -10.0;
  Bad.InterceptorBoostSeconds = -2.0;
  Bad.InterceptorBoostVerticalBias = 5.0;
  Bad.TerminalPhaseThresholdSeconds = -0.5;
  Bad.TerminalTurnRateMultiplier = 0.0;
  Bad.InterceptorProportionalNavConstant = -2.0;
  Bad.KillProbabilityAtZeroMiss = -0.3;
  Bad.KillProbabilityFuseFloor = 5.0;
  Bad.SeekerConeDegrees = -90.0;
  Bad.EngagementMode = static_cast<EProjectAirDefenseEngagementMode>(255);
  Bad.ThreatPriority = static_cast<EProjectAirDefenseThreatPriority>(255);
  Bad.FireControlMode = static_cast<EProjectAirDefenseFireControlMode>(255);

  FProjectAirDefenseBattleSimulation Sim(MakeDistrictCells(), MakeLauncherPositions(), Bad, 1);
  const FProjectAirDefenseDefenseSettings& After = Sim.GetSettings();

  TestTrue(TEXT("engagement range clamped above zero"), After.EngagementRangeMeters >= 100.0);
  TestTrue(TEXT("interceptor speed clamped above zero"), After.InterceptorSpeedMetersPerSecond >= 50.0);
  TestTrue(TEXT("launch cooldown clamped above zero"), After.LaunchCooldownSeconds > 0.0);
  TestTrue(TEXT("blast radius clamped above zero"), After.BlastRadiusMeters > 0.0);
  TestTrue(TEXT("boost seconds non-negative"), After.InterceptorBoostSeconds >= 0.0);
  TestTrue(TEXT("boost vertical bias in [0,1]"),
      After.InterceptorBoostVerticalBias >= 0.0 && After.InterceptorBoostVerticalBias <= 1.0);
  TestTrue(TEXT("terminal threshold non-negative"), After.TerminalPhaseThresholdSeconds >= 0.0);
  TestTrue(TEXT("terminal turn multiplier above zero"), After.TerminalTurnRateMultiplier > 0.0);
  TestTrue(TEXT("proportional nav constant at least one"), After.InterceptorProportionalNavConstant >= 1.0);
  TestTrue(TEXT("kill probability at zero miss in [0,1]"),
      After.KillProbabilityAtZeroMiss >= 0.0 && After.KillProbabilityAtZeroMiss <= 1.0);
  TestTrue(TEXT("kill probability floor in [0,1]"),
      After.KillProbabilityFuseFloor >= 0.0 && After.KillProbabilityFuseFloor <= 1.0);
  TestTrue(
      TEXT("kill probability floor does not exceed zero-miss probability"),
      After.KillProbabilityFuseFloor <= After.KillProbabilityAtZeroMiss);
  TestTrue(TEXT("seeker cone degrees in [1, 179]"),
      After.SeekerConeDegrees >= 1.0 && After.SeekerConeDegrees <= 179.0);
  TestEqual(
      TEXT("invalid engagement mode sanitized"),
      static_cast<int32>(After.EngagementMode),
      static_cast<int32>(EProjectAirDefenseEngagementMode::DoctrineDefault));
  TestEqual(
      TEXT("invalid threat priority sanitized"),
      static_cast<int32>(After.ThreatPriority),
      static_cast<int32>(EProjectAirDefenseThreatPriority::Balanced));
  TestEqual(
      TEXT("invalid fire control mode sanitized"),
      static_cast<int32>(After.FireControlMode),
      static_cast<int32>(EProjectAirDefenseFireControlMode::Balanced));
  return true;
}

IMPLEMENT_SIMPLE_AUTOMATION_TEST(
    FProjectAirDefenseRuntimeDistrictConfigSanitizationTest,
    "ProjectAirDefense.RuntimeSettings.DistrictConfigSanitization",
    EAutomationTestFlags::EditorContext | EAutomationTestFlags::EngineFilter)

bool FProjectAirDefenseRuntimeDistrictConfigSanitizationTest::RunTest(const FString& Parameters) {
  FProjectAirDefenseDistrictRuntimeInputs Bad;
  Bad.DistrictHalfExtentMeters = -1.0;
  Bad.DistrictTilesetCoverageRatio = std::numeric_limits<double>::quiet_NaN();
  Bad.DistrictMinHalfExtentMeters = std::numeric_limits<double>::infinity();
  Bad.DistrictCellRadiusMeters = -10.0;
  Bad.LauncherRingDistanceMeters = std::numeric_limits<double>::infinity();
  Bad.LauncherLateralSpacingMeters = -100.0;

  const FProjectAirDefenseDistrictRuntimeConfig BadConfig =
      BuildProjectAirDefenseDistrictRuntimeConfig(
          Bad,
          std::numeric_limits<double>::infinity());
  TestTrue(TEXT("bad half extent remains finite"), FMath::IsFinite(BadConfig.HalfExtentMeters));
  TestTrue(TEXT("bad half extent clamped above minimum"), BadConfig.HalfExtentMeters >= 700.0);
  TestTrue(TEXT("bad cell radius clamped above minimum"), BadConfig.CellRadiusMeters >= 80.0);
  TestTrue(TEXT("bad launcher ring remains finite"), FMath::IsFinite(BadConfig.LauncherRingDistanceMeters));
  TestTrue(TEXT("bad launcher spacing non-negative"), BadConfig.LauncherLateralSpacingMeters >= 0.0);

  FProjectAirDefenseDistrictRuntimeInputs Defaults;
  const FProjectAirDefenseDistrictRuntimeConfig DefaultConfig =
      BuildProjectAirDefenseDistrictRuntimeConfig(Defaults, 3000.0);
  TestTrue(TEXT("tileset coverage frames the city"), DefaultConfig.HalfExtentMeters > 2400.0);
  TestTrue(TEXT("default launcher ring preserved"), DefaultConfig.LauncherRingDistanceMeters == 2400.0);
  TestTrue(TEXT("default cell radius preserved"), DefaultConfig.CellRadiusMeters == 260.0);
  return true;
}

IMPLEMENT_SIMPLE_AUTOMATION_TEST(
    FProjectAirDefenseBattleEmptyLauncherTest,
    "ProjectAirDefense.BattleSimulation.EmptyLauncherArrayTolerated",
    EAutomationTestFlags::EditorContext | EAutomationTestFlags::EngineFilter)

bool FProjectAirDefenseBattleEmptyLauncherTest::RunTest(const FString& Parameters) {
  // The constructor synthesizes a single origin-launcher when the caller
  // passes an empty array. This must remain stable: the simulation must still
  // step without crashing and must still spawn threats. No intercepts are
  // expected because the synthetic launcher has no real hardware behind it,
  // so we only assert on liveness invariants.
  FProjectAirDefenseBattleSimulation Sim(
      MakeDistrictCells(), TArray<FVector3d>(), MakeSettings(EProjectAirDefenseDefenseDoctrine::Adaptive), 77);
  TestTrue(TEXT("wave starts with empty launcher array"), Sim.StartNextWave());
  for (int32 Index = 0; Index < 200; ++Index) {
    Sim.Step(0.05);
  }
  const FProjectAirDefenseBattleRunSummary Summary = Sim.BuildRunSummary(0, 10.0);
  TestTrue(TEXT("threats still spawned"), Summary.ThreatsSpawned > 0);
  TestTrue(TEXT("no out-of-bounds integrity"),
      Summary.CityIntegrity >= 0.0 && Summary.CityIntegrity <= 100.0);
  return true;
}

#endif
