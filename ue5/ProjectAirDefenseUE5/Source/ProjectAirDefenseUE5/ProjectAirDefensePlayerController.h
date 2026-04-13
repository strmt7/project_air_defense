#pragma once

#include "CoreMinimal.h"
#include "GameFramework/PlayerController.h"
#include "ProjectAirDefensePlayerController.generated.h"

class AProjectAirDefenseBattleManager;
class AProjectAirDefenseCityCameraPawn;
class UProjectAirDefenseBattleWidget;
class UProjectAirDefenseMainMenuWidget;

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
  void RequestCycleAntiAliasing();
  void RequestToggleAmbientOcclusion();
  void RequestToggleMotionBlur();
  void RequestCycleShadowQuality();
  void RequestCycleReflectionQuality();
  void RequestCyclePostProcessingQuality();
  void RequestCameraPan(float ForwardMeters, float RightMeters);
  void RequestCameraYaw(float DeltaDegrees);
  void RequestCameraPitch(float DeltaDegrees);
  void RequestCameraZoom(float DeltaMeters);
  void RequestCameraAltitude(float DeltaMeters);
  void RequestCameraReset();

private:
  void BuildWidgets();
  void RefreshVisibleUi();
  void ApplyVerificationLaunchFlags();
  void ApplyInteractionMode();

  void HandleToggleSystemsMenu();
  void HandlePrimaryMenuAction();
  void HandleStartWave();
  void HandleCycleDoctrine();
  void HandleIncreaseQuality();
  void HandleDecreaseQuality();
  void HandleCycleAntiAliasing();
  void HandleToggleAmbientOcclusion();
  void HandleToggleMotionBlur();
  void HandleCycleShadowQuality();
  void HandleCycleReflectionQuality();
  void HandleCyclePostProcessingQuality();
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
