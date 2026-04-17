#pragma once

#include "CoreMinimal.h"
#include "GameFramework/PlayerController.h"
#include "ProjectAirDefensePlayerController.generated.h"

class AProjectAirDefenseBattleManager;
class AProjectAirDefenseCityCameraPawn;
class AProjectAirDefenseGameMode;
class UProjectAirDefenseBattleWidget;
class UProjectAirDefenseMainMenuWidget;
enum class EProjectAirDefenseAntiAliasingMethod : uint8;

UCLASS()
class PROJECTAIRDEFENSEUE5_API AProjectAirDefensePlayerController : public APlayerController {
  GENERATED_BODY()

public:
  AProjectAirDefensePlayerController();

  virtual void BeginPlay() override;
  virtual void SetupInputComponent() override;

  AProjectAirDefenseBattleManager* FindBattleManager() const;
  AProjectAirDefenseCityCameraPawn* FindCameraPawn() const;
  bool IsSystemsMenuVisible() const;
  bool IsBattleStarted() const;
  void SetSystemsMenuVisible(bool bVisible);
  void StartBattleExperience(bool bStartWaveImmediately, bool bOpenSystemsImmediately);
  void RequestStartWave();
  void RequestCycleDoctrine();
  void RequestIncreaseQuality();
  void RequestDecreaseQuality();
  void RequestSetOverallQualityLevel(int32 QualityLevel);
  void RequestCycleAntiAliasing();
  void RequestSetAntiAliasingMethod(EProjectAirDefenseAntiAliasingMethod Method);
  void RequestToggleAmbientOcclusion();
  void RequestSetAmbientOcclusionEnabled(bool bEnabled);
  void RequestToggleMotionBlur();
  void RequestSetMotionBlurEnabled(bool bEnabled);
  void RequestToggleRayTracing();
  void RequestSetRayTracingEnabled(bool bEnabled);
  void RequestCycleShadowQuality();
  void RequestSetShadowQualityLevel(int32 QualityLevel);
  void RequestCycleReflectionQuality();
  void RequestSetReflectionQualityLevel(int32 QualityLevel);
  void RequestCyclePostProcessingQuality();
  void RequestSetPostProcessingQualityLevel(int32 QualityLevel);
  void RequestCameraPan(float ForwardMeters, float RightMeters);
  void RequestCameraYaw(float DeltaDegrees);
  void RequestCameraPitch(float DeltaDegrees);
  void RequestCameraZoom(float DeltaMeters);
  void RequestCameraAltitude(float DeltaMeters);
  void RequestCameraReset();
  void RequestTimeStepBack();
  void RequestTimeStepForward();
  void RequestTimeStep(double DeltaHours);
  void RequestSetSolarTime(double SolarTimeHours);
  void RequestSlowerTime();
  void RequestFasterTime();
  void RequestSetTimeScale(double HoursPerMinute);
  void RequestToggleTimeCycle();
  double GetSolarTimeHours() const;
  double GetTimeScaleHoursPerMinute() const;
  double GetTimeScaleMaxHoursPerMinute() const;
  FString BuildTimeSummaryText() const;

private:
  void BuildWidgets();
  void RefreshVisibleUi();
  void ApplyVerificationLaunchFlags();
  void ApplyInteractionMode();
  AProjectAirDefenseGameMode* FindProjectGameMode() const;
  double GetTimeScaleStepHoursPerMinute() const;
  bool StartBattleForShortcut(bool bStartWaveImmediately, bool bOpenSystemsImmediately);

  void HandleToggleSystemsMenu();
  void HandlePrimaryMenuAction();
  void HandleStartWave();
  void HandleCycleDoctrine();
  void HandleIncreaseQuality();
  void HandleDecreaseQuality();
  void HandleCycleAntiAliasing();
  void HandleToggleAmbientOcclusion();
  void HandleToggleMotionBlur();
  void HandleToggleRayTracing();
  void HandleCycleShadowQuality();
  void HandleCycleReflectionQuality();
  void HandleCyclePostProcessingQuality();
  void HandleTimeStepBack();
  void HandleTimeStepForward();
  void HandleToggleTimeCycle();
  void HandleCaptureVerificationScreenshot();
  void HandleRequestGracefulQuit();

  UPROPERTY(Transient)
  TObjectPtr<UProjectAirDefenseMainMenuWidget> MainMenuWidget;

  UPROPERTY(Transient)
  TObjectPtr<UProjectAirDefenseBattleWidget> BattleWidget;

  bool bBattleStarted = false;
  bool bSystemsMenuVisible = false;
  bool bVerificationFlagsApplied = false;
};
