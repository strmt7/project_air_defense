#include "ProjectAirDefenseBattleMonteCarloCommandlet.h"

#include "HAL/FileManager.h"
#include "Misc/FileHelper.h"
#include "Misc/Paths.h"
#include "Policies/CondensedJsonPrintPolicy.h"
#include "ProjectAirDefenseBattleSimulation.h"
#include "Serialization/JsonSerializer.h"
#include "Serialization/JsonWriter.h"

namespace {
constexpr int32 DefaultRuns = 300;
constexpr int32 MaxRuns = 5000;
constexpr int32 DefaultWaves = 1;
constexpr int32 DefaultSeed = 20260411;
constexpr double DefaultSecondsPerWave = 48.0;
constexpr double DefaultStepSeconds = 0.05;

TArray<FProjectAirDefenseDistrictCell> MakeMonteCarloDistrictCells() {
  TArray<FProjectAirDefenseDistrictCell> Cells;
  constexpr double HalfExtentMeters = 1600.0;
  constexpr double StepMeters = HalfExtentMeters * 0.9;
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

TArray<FVector3d> MakeMonteCarloLaunchers() {
  return {
      FVector3d(-950.0, -1700.0, 0.0),
      FVector3d(0.0, -1820.0, 0.0),
      FVector3d(950.0, -1700.0, 0.0),
  };
}

EProjectAirDefenseDefenseDoctrine ParseDoctrine(const FString& Params) {
  FString DoctrineValue;
  if (!FParse::Value(*Params, TEXT("Doctrine="), DoctrineValue)) {
    return EProjectAirDefenseDefenseDoctrine::ShieldWall;
  }

  if (DoctrineValue.Equals(TEXT("Disciplined"), ESearchCase::IgnoreCase)) {
    return EProjectAirDefenseDefenseDoctrine::Disciplined;
  }
  if (DoctrineValue.Equals(TEXT("Adaptive"), ESearchCase::IgnoreCase)) {
    return EProjectAirDefenseDefenseDoctrine::Adaptive;
  }
  return EProjectAirDefenseDefenseDoctrine::ShieldWall;
}

FString ResolveReportPath(const FString& Params) {
  FString ReportPath;
  if (!FParse::Value(*Params, TEXT("Report="), ReportPath) || ReportPath.IsEmpty()) {
    ReportPath = FPaths::Combine(
        FPaths::ProjectDir(),
        TEXT("../../benchmark-results/ue5-battle-monte-carlo.json"));
  }
  return FPaths::ConvertRelativePathToFull(ReportPath);
}

FProjectAirDefenseBattleRunSummary RunSingleMonteCarloSimulation(
    int32 RunIndex,
    int32 Seed,
    int32 Waves,
    double SecondsPerWave,
    double StepSeconds,
    EProjectAirDefenseDefenseDoctrine Doctrine) {
  FProjectAirDefenseDefenseSettings Settings;
  Settings.Doctrine = Doctrine;
  FProjectAirDefenseBattleSimulation Simulation(
      MakeMonteCarloDistrictCells(),
      MakeMonteCarloLaunchers(),
      Settings,
      Seed);

  double SimulatedSeconds = 0.0;
  const int32 MaxStepsPerWave =
      FMath::Max(1, static_cast<int32>(FMath::CeilToDouble(SecondsPerWave / StepSeconds)));
  for (int32 WaveIndex = 0; WaveIndex < Waves; ++WaveIndex) {
    if (!Simulation.StartNextWave()) {
      break;
    }
    for (int32 StepIndex = 0; StepIndex < MaxStepsPerWave; ++StepIndex) {
      Simulation.Step(StepSeconds);
      SimulatedSeconds += StepSeconds;
      if ((!Simulation.IsWaveInProgress() && Simulation.GetThreats().IsEmpty()) ||
          Simulation.IsGameOver()) {
        break;
      }
    }
    if (Simulation.IsGameOver()) {
      break;
    }
  }

  return Simulation.BuildRunSummary(RunIndex, SimulatedSeconds);
}

TSharedRef<FJsonObject> SummaryToJsonObject(const FProjectAirDefenseBattleRunSummary& Summary) {
  TSharedRef<FJsonObject> Object = MakeShared<FJsonObject>();
  Object->SetNumberField(TEXT("runIndex"), Summary.RunIndex);
  Object->SetNumberField(TEXT("wavesCompleted"), Summary.WavesCompleted);
  Object->SetNumberField(TEXT("cityIntegrity"), Summary.CityIntegrity);
  Object->SetBoolField(TEXT("gameOver"), Summary.bGameOver);
  Object->SetNumberField(TEXT("score"), Summary.Score);
  Object->SetNumberField(TEXT("threatsSpawned"), Summary.ThreatsSpawned);
  Object->SetNumberField(TEXT("threatsIntercepted"), Summary.ThreatsIntercepted);
  Object->SetNumberField(TEXT("hostileImpacts"), Summary.HostileImpacts);
  Object->SetNumberField(TEXT("interceptorsLaunched"), Summary.InterceptorsLaunched);
  Object->SetNumberField(TEXT("destroyedDistricts"), Summary.DestroyedDistricts);
  Object->SetNumberField(TEXT("simulatedSeconds"), Summary.SimulatedSeconds);
  Object->SetNumberField(TEXT("interceptRate"), Summary.InterceptRate());
  Object->SetNumberField(TEXT("interceptorsKilledTarget"), Summary.InterceptorsKilledTarget);
  Object->SetNumberField(TEXT("interceptorsFuseRollMissed"), Summary.InterceptorsFuseRollMissed);
  Object->SetNumberField(TEXT("interceptsInTerminalPhase"), Summary.InterceptsInTerminalPhase);
  Object->SetNumberField(TEXT("averageMissDistanceMeters"), Summary.AverageMissDistanceMeters());
  Object->SetNumberField(TEXT("singleShotKillProbability"), Summary.SingleShotKillProbability());
  return Object;
}
} // namespace

UProjectAirDefenseBattleMonteCarloCommandlet::UProjectAirDefenseBattleMonteCarloCommandlet() {
  this->IsClient = false;
  this->IsEditor = false;
  this->IsServer = false;
  this->LogToConsole = true;
}

int32 UProjectAirDefenseBattleMonteCarloCommandlet::Main(const FString& Params) {
  int32 Runs = DefaultRuns;
  int32 Waves = DefaultWaves;
  int32 Seed = DefaultSeed;
  double SecondsPerWave = DefaultSecondsPerWave;
  double StepSeconds = DefaultStepSeconds;

  FParse::Value(*Params, TEXT("Runs="), Runs);
  FParse::Value(*Params, TEXT("Waves="), Waves);
  FParse::Value(*Params, TEXT("Seed="), Seed);
  FParse::Value(*Params, TEXT("Seconds="), SecondsPerWave);
  FParse::Value(*Params, TEXT("Step="), StepSeconds);

  Runs = FMath::Clamp(Runs, 1, MaxRuns);
  Waves = FMath::Clamp(Waves, 1, 12);
  SecondsPerWave = FMath::Clamp(SecondsPerWave, 5.0, 300.0);
  StepSeconds = FMath::Clamp(StepSeconds, 0.01, 0.2);
  const EProjectAirDefenseDefenseDoctrine Doctrine = ParseDoctrine(Params);
  const FString ReportPath = ResolveReportPath(Params);

  TArray<FProjectAirDefenseBattleRunSummary> Summaries;
  Summaries.Reserve(Runs);
  double InterceptRateSum = 0.0;
  double CityIntegritySum = 0.0;
  double MissDistanceSum = 0.0;
  double SingleShotKillProbabilitySum = 0.0;
  int32 MissDistanceRuns = 0;
  int32 SingleShotKillProbabilityRuns = 0;
  int32 GameOverRuns = 0;
  int32 TotalThreatsSpawned = 0;
  int32 TotalThreatsIntercepted = 0;
  int32 TotalHostileImpacts = 0;
  int32 TotalInterceptorsLaunched = 0;
  int32 TotalInterceptorsKilledTarget = 0;
  int32 TotalInterceptorsFuseRollMissed = 0;
  int32 TotalInterceptsInTerminalPhase = 0;

  for (int32 RunIndex = 0; RunIndex < Runs; ++RunIndex) {
    const FProjectAirDefenseBattleRunSummary Summary =
        RunSingleMonteCarloSimulation(
            RunIndex,
            Seed + RunIndex,
            Waves,
            SecondsPerWave,
            StepSeconds,
            Doctrine);
    Summaries.Add(Summary);
    InterceptRateSum += Summary.InterceptRate();
    CityIntegritySum += Summary.CityIntegrity;
    GameOverRuns += Summary.bGameOver ? 1 : 0;
    TotalThreatsSpawned += Summary.ThreatsSpawned;
    TotalThreatsIntercepted += Summary.ThreatsIntercepted;
    TotalHostileImpacts += Summary.HostileImpacts;
    TotalInterceptorsLaunched += Summary.InterceptorsLaunched;
    TotalInterceptorsKilledTarget += Summary.InterceptorsKilledTarget;
    TotalInterceptorsFuseRollMissed += Summary.InterceptorsFuseRollMissed;
    TotalInterceptsInTerminalPhase += Summary.InterceptsInTerminalPhase;
    if (Summary.MissDistanceSampleCount > 0) {
      MissDistanceSum += Summary.AverageMissDistanceMeters();
      ++MissDistanceRuns;
    }
    if (Summary.InterceptorsKilledTarget + Summary.InterceptorsFuseRollMissed > 0) {
      SingleShotKillProbabilitySum += Summary.SingleShotKillProbability();
      ++SingleShotKillProbabilityRuns;
    }
  }

  TSharedRef<FJsonObject> RootObject = MakeShared<FJsonObject>();
  RootObject->SetStringField(TEXT("engine"), TEXT("Unreal Engine 5"));
  RootObject->SetStringField(TEXT("simulation"), TEXT("FProjectAirDefenseBattleSimulation"));
  RootObject->SetNumberField(TEXT("runs"), Runs);
  RootObject->SetNumberField(TEXT("waves"), Waves);
  RootObject->SetNumberField(TEXT("seed"), Seed);
  RootObject->SetNumberField(TEXT("secondsPerWave"), SecondsPerWave);
  RootObject->SetNumberField(TEXT("stepSeconds"), StepSeconds);
  RootObject->SetStringField(TEXT("doctrine"), ProjectAirDefenseDoctrineLabel(Doctrine));
  RootObject->SetNumberField(TEXT("averageInterceptRate"), InterceptRateSum / static_cast<double>(Runs));
  RootObject->SetNumberField(TEXT("averageCityIntegrity"), CityIntegritySum / static_cast<double>(Runs));
  RootObject->SetNumberField(TEXT("gameOverRuns"), GameOverRuns);
  RootObject->SetNumberField(TEXT("totalThreatsSpawned"), TotalThreatsSpawned);
  RootObject->SetNumberField(TEXT("totalThreatsIntercepted"), TotalThreatsIntercepted);
  RootObject->SetNumberField(TEXT("totalHostileImpacts"), TotalHostileImpacts);
  RootObject->SetNumberField(TEXT("totalInterceptorsLaunched"), TotalInterceptorsLaunched);
  RootObject->SetNumberField(TEXT("totalInterceptorsKilledTarget"), TotalInterceptorsKilledTarget);
  RootObject->SetNumberField(TEXT("totalInterceptorsFuseRollMissed"), TotalInterceptorsFuseRollMissed);
  RootObject->SetNumberField(TEXT("totalInterceptsInTerminalPhase"), TotalInterceptsInTerminalPhase);
  RootObject->SetNumberField(
      TEXT("averageMissDistanceMeters"),
      MissDistanceRuns > 0 ? MissDistanceSum / static_cast<double>(MissDistanceRuns) : 0.0);
  RootObject->SetNumberField(
      TEXT("averageSingleShotKillProbability"),
      SingleShotKillProbabilityRuns > 0
          ? SingleShotKillProbabilitySum / static_cast<double>(SingleShotKillProbabilityRuns)
          : 0.0);

  TArray<TSharedPtr<FJsonValue>> RunValues;
  RunValues.Reserve(Summaries.Num());
  for (const FProjectAirDefenseBattleRunSummary& Summary : Summaries) {
    RunValues.Add(MakeShared<FJsonValueObject>(SummaryToJsonObject(Summary)));
  }
  RootObject->SetArrayField(TEXT("runsDetail"), RunValues);

  FString OutputJson;
  const TSharedRef<TJsonWriter<TCHAR, TCondensedJsonPrintPolicy<TCHAR>>> Writer =
      TJsonWriterFactory<TCHAR, TCondensedJsonPrintPolicy<TCHAR>>::Create(&OutputJson);
  if (!FJsonSerializer::Serialize(RootObject, Writer)) {
    UE_LOG(LogInit, Error, TEXT("Failed to serialize Monte Carlo report."));
    return 1;
  }

  IFileManager::Get().MakeDirectory(*FPaths::GetPath(ReportPath), true);
  if (!FFileHelper::SaveStringToFile(OutputJson, *ReportPath)) {
    UE_LOG(LogInit, Error, TEXT("Failed to write Monte Carlo report: %s"), *ReportPath);
    return 1;
  }

  UE_LOG(
      LogInit,
      Display,
      TEXT("UE5 battle Monte Carlo passed: runs=%d avgIntercept=%.3f avgIntegrity=%.2f report=%s"),
      Runs,
      InterceptRateSum / static_cast<double>(Runs),
      CityIntegritySum / static_cast<double>(Runs),
      *ReportPath);
  return 0;
}
