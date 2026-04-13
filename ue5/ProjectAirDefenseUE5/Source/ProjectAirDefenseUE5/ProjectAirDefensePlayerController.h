#pragma once

#include "CoreMinimal.h"
#include "GameFramework/PlayerController.h"
#include "ProjectAirDefensePlayerController.generated.h"

class AProjectAirDefenseBattleManager;

UCLASS()
class PROJECTAIRDEFENSEUE5_API AProjectAirDefensePlayerController : public APlayerController {
  GENERATED_BODY()

public:
  AProjectAirDefensePlayerController();

  virtual void SetupInputComponent() override;

  bool IsSystemsMenuVisible() const;
  void SetSystemsMenuVisible(bool bVisible);

private:
  AProjectAirDefenseBattleManager* FindBattleManager() const;
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

  bool bSystemsMenuVisible = false;
};
