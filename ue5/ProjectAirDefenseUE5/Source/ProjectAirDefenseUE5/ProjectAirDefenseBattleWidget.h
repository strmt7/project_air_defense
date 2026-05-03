#pragma once

#include "CoreMinimal.h"
#include "Blueprint/UserWidget.h"
#include "ProjectAirDefenseBattleWidget.generated.h"

class AProjectAirDefensePlayerController;
class UButton;
class UCheckBox;
class USlider;
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
  void HandleEngagementModePressed();

  UFUNCTION()
  void HandleThreatPriorityPressed();

  UFUNCTION()
  void HandleFireControlPressed();

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
  void HandleRayTracingPressed();

  UFUNCTION()
  void HandleShadowPressed();

  UFUNCTION()
  void HandleReflectionPressed();

  UFUNCTION()
  void HandlePostPressed();

  UFUNCTION()
  void HandleOverallQualitySliderChanged(float Value);

  UFUNCTION()
  void HandleEngagementRangeSliderChanged(float Value);

  UFUNCTION()
  void HandleAaSliderChanged(float Value);

  UFUNCTION()
  void HandleAoSwitchChanged(bool bIsChecked);

  UFUNCTION()
  void HandleBlurSwitchChanged(bool bIsChecked);

  UFUNCTION()
  void HandleRayTracingSwitchChanged(bool bIsChecked);

  UFUNCTION()
  void HandleShadowSliderChanged(float Value);

  UFUNCTION()
  void HandleReflectionSliderChanged(float Value);

  UFUNCTION()
  void HandlePostSliderChanged(float Value);

  UFUNCTION()
  void HandleTimeOfDaySliderChanged(float Value);

  UFUNCTION()
  void HandleTimeScaleSliderChanged(float Value);

  UFUNCTION()
  void HandleTimeCycleSwitchChanged(bool bIsChecked);

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
  void HandleTimeBackPressed();

  UFUNCTION()
  void HandleTimeForwardPressed();

  UFUNCTION()
  void HandleTimeSlowerPressed();

  UFUNCTION()
  void HandleTimeFasterPressed();

  UFUNCTION()
  void HandleTimePausePressed();

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
  TObjectPtr<UTextBlock> EngagementModeButtonText;

  UPROPERTY(Transient)
  TObjectPtr<UTextBlock> ThreatPriorityButtonText;

  UPROPERTY(Transient)
  TObjectPtr<UTextBlock> FireControlButtonText;

  UPROPERTY(Transient)
  TObjectPtr<UTextBlock> WaveButtonText;

  UPROPERTY(Transient)
  TObjectPtr<UTextBlock> TacticsSummaryText;

  UPROPERTY(Transient)
  TObjectPtr<UTextBlock> TacticsThreatText;

  UPROPERTY(Transient)
  TObjectPtr<UTextBlock> GraphicsSummaryText;

  UPROPERTY(Transient)
  TObjectPtr<UTextBlock> TimeSummaryText;

  UPROPERTY(Transient)
  TObjectPtr<UTextBlock> AaButtonText;

  UPROPERTY(Transient)
  TObjectPtr<UTextBlock> AoButtonText;

  UPROPERTY(Transient)
  TObjectPtr<UTextBlock> BlurButtonText;

  UPROPERTY(Transient)
  TObjectPtr<UTextBlock> RayTracingButtonText;

  UPROPERTY(Transient)
  TObjectPtr<UTextBlock> ShadowButtonText;

  UPROPERTY(Transient)
  TObjectPtr<UTextBlock> ReflectionButtonText;

  UPROPERTY(Transient)
  TObjectPtr<UTextBlock> PostButtonText;

  UPROPERTY(Transient)
  TObjectPtr<UTextBlock> QualityValueText;

  UPROPERTY(Transient)
  TObjectPtr<UTextBlock> EngagementRangeValueText;

  UPROPERTY(Transient)
  TObjectPtr<UTextBlock> AaValueText;

  UPROPERTY(Transient)
  TObjectPtr<UTextBlock> AoValueText;

  UPROPERTY(Transient)
  TObjectPtr<UTextBlock> BlurValueText;

  UPROPERTY(Transient)
  TObjectPtr<UTextBlock> RayTracingValueText;

  UPROPERTY(Transient)
  TObjectPtr<UTextBlock> ShadowValueText;

  UPROPERTY(Transient)
  TObjectPtr<UTextBlock> ReflectionValueText;

  UPROPERTY(Transient)
  TObjectPtr<UTextBlock> PostValueText;

  UPROPERTY(Transient)
  TObjectPtr<UTextBlock> TimeOfDayValueText;

  UPROPERTY(Transient)
  TObjectPtr<UTextBlock> TimeScaleValueText;

  UPROPERTY(Transient)
  TObjectPtr<UTextBlock> TimeCycleValueText;

  UPROPERTY(Transient)
  TObjectPtr<USlider> OverallQualitySlider;

  UPROPERTY(Transient)
  TObjectPtr<USlider> EngagementRangeSlider;

  UPROPERTY(Transient)
  TObjectPtr<USlider> AaSlider;

  UPROPERTY(Transient)
  TObjectPtr<USlider> ShadowSlider;

  UPROPERTY(Transient)
  TObjectPtr<USlider> ReflectionSlider;

  UPROPERTY(Transient)
  TObjectPtr<USlider> PostSlider;

  UPROPERTY(Transient)
  TObjectPtr<USlider> TimeOfDaySlider;

  UPROPERTY(Transient)
  TObjectPtr<USlider> TimeScaleSlider;

  UPROPERTY(Transient)
  TObjectPtr<UCheckBox> AoSwitch;

  UPROPERTY(Transient)
  TObjectPtr<UCheckBox> BlurSwitch;

  UPROPERTY(Transient)
  TObjectPtr<UCheckBox> RayTracingSwitch;

  UPROPERTY(Transient)
  TObjectPtr<UCheckBox> TimeCycleSwitch;

  UPROPERTY(Transient)
  TObjectPtr<UVerticalBox> SystemsDrawer;

  UPROPERTY(Transient)
  TObjectPtr<UProjectAirDefenseRadarWidget> RadarWidget;

  FTimerHandle RefreshTimerHandle;
  FTimerHandle CameraHoldTimerHandle;
  FName ActiveCameraHoldAction;
  bool bTreeBuilt = false;
  bool bRefreshingControls = false;
};
