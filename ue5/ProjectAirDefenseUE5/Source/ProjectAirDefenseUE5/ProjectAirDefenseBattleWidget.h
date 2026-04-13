#pragma once

#include "CoreMinimal.h"
#include "Blueprint/UserWidget.h"
#include "ProjectAirDefenseBattleWidget.generated.h"

class AProjectAirDefensePlayerController;
class UButton;
class UTextBlock;
class UVerticalBox;
class UWidget;
class UProjectAirDefenseRadarWidget;

UCLASS()
class PROJECTAIRDEFENSEUE5_API UProjectAirDefenseBattleWidget : public UUserWidget {
  GENERATED_BODY()

public:
  void BindController(AProjectAirDefensePlayerController* InController);
  void RefreshFromRuntime();

protected:
  virtual void NativeOnInitialized() override;
  virtual void NativeDestruct() override;

private:
  UFUNCTION()
  void HandleSystemsPressed();

  UFUNCTION()
  void HandleDoctrinePressed();

  UFUNCTION()
  void HandleWavePressed();

  UFUNCTION()
  void HandleQualityMinusPressed();

  UFUNCTION()
  void HandleQualityPlusPressed();

  UFUNCTION()
  void HandleAaPressed();

  UFUNCTION()
  void HandleAoPressed();

  UFUNCTION()
  void HandleBlurPressed();

  UFUNCTION()
  void HandleShadowPressed();

  UFUNCTION()
  void HandleReflectionPressed();

  UFUNCTION()
  void HandlePostPressed();

  UFUNCTION()
  void HandleCameraUpPressed();

  UFUNCTION()
  void HandleCameraDownPressed();

  UFUNCTION()
  void HandleCameraLeftPressed();

  UFUNCTION()
  void HandleCameraRightPressed();

  UFUNCTION()
  void HandleCameraYawMinusPressed();

  UFUNCTION()
  void HandleCameraYawPlusPressed();

  UFUNCTION()
  void HandleCameraPitchMinusPressed();

  UFUNCTION()
  void HandleCameraPitchPlusPressed();

  UFUNCTION()
  void HandleCameraZoomMinusPressed();

  UFUNCTION()
  void HandleCameraZoomPlusPressed();

  UFUNCTION()
  void HandleCameraResetPressed();

  UFUNCTION()
  void HandleCameraHoldReleased();

  void BuildWidgetTree();
  AProjectAirDefensePlayerController* ResolveController() const;
  void SetDrawerVisibility(bool bVisible);
  void StartCameraHold(const FName& ActionName);
  void StopCameraHold();
  void StepCameraHoldAction(const FName& ActionName);

  TWeakObjectPtr<AProjectAirDefensePlayerController> OwningAirDefenseController;

  UPROPERTY(Transient)
  TObjectPtr<UTextBlock> LeftPrimaryText;

  UPROPERTY(Transient)
  TObjectPtr<UTextBlock> LeftSecondaryText;

  UPROPERTY(Transient)
  TObjectPtr<UTextBlock> RightPrimaryText;

  UPROPERTY(Transient)
  TObjectPtr<UTextBlock> RightSecondaryText;

  UPROPERTY(Transient)
  TObjectPtr<UTextBlock> SystemsButtonText;

  UPROPERTY(Transient)
  TObjectPtr<UTextBlock> DoctrineButtonText;

  UPROPERTY(Transient)
  TObjectPtr<UTextBlock> WaveButtonText;

  UPROPERTY(Transient)
  TObjectPtr<UTextBlock> TacticsSummaryText;

  UPROPERTY(Transient)
  TObjectPtr<UTextBlock> TacticsThreatText;

  UPROPERTY(Transient)
  TObjectPtr<UTextBlock> GraphicsSummaryText;

  UPROPERTY(Transient)
  TObjectPtr<UTextBlock> AaButtonText;

  UPROPERTY(Transient)
  TObjectPtr<UTextBlock> AoButtonText;

  UPROPERTY(Transient)
  TObjectPtr<UTextBlock> BlurButtonText;

  UPROPERTY(Transient)
  TObjectPtr<UTextBlock> ShadowButtonText;

  UPROPERTY(Transient)
  TObjectPtr<UTextBlock> ReflectionButtonText;

  UPROPERTY(Transient)
  TObjectPtr<UTextBlock> PostButtonText;

  UPROPERTY(Transient)
  TObjectPtr<UVerticalBox> SystemsDrawer;

  UPROPERTY(Transient)
  TObjectPtr<UProjectAirDefenseRadarWidget> RadarWidget;

  FTimerHandle RefreshTimerHandle;
  FTimerHandle CameraHoldTimerHandle;
  FName ActiveCameraHoldAction;
  bool bTreeBuilt = false;
};
