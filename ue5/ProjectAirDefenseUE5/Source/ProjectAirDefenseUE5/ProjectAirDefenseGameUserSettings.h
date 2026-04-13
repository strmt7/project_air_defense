#pragma once

#include "CoreMinimal.h"
#include "GameFramework/GameUserSettings.h"
#include "SceneUtils.h"
#include "ProjectAirDefenseGameUserSettings.generated.h"

UENUM(BlueprintType)
enum class EProjectAirDefenseAntiAliasingMethod : uint8 {
  None = AAM_None UMETA(DisplayName = "None"),
  FXAA = AAM_FXAA UMETA(DisplayName = "FXAA"),
  TAA = AAM_TemporalAA UMETA(DisplayName = "TAA"),
  TSR = AAM_TSR UMETA(DisplayName = "TSR"),
  SMAA = AAM_SMAA UMETA(DisplayName = "SMAA")
};

UCLASS(Config = GameUserSettings, ConfigDoNotCheckDefaults)
class PROJECTAIRDEFENSEUE5_API UProjectAirDefenseGameUserSettings : public UGameUserSettings {
  GENERATED_BODY()

public:
  UProjectAirDefenseGameUserSettings();

  virtual void SetToDefaults() override;
  virtual void LoadSettings(bool bForceReload = false) override;
  virtual void ApplyNonResolutionSettings() override;
  virtual void ValidateSettings() override;

  UFUNCTION(BlueprintCallable, Category = "Project Air Defense|Graphics")
  void SetPreferredAntiAliasingMethod(EProjectAirDefenseAntiAliasingMethod InMethod);

  UFUNCTION(BlueprintPure, Category = "Project Air Defense|Graphics")
  EProjectAirDefenseAntiAliasingMethod GetPreferredAntiAliasingMethod() const;

  UFUNCTION(BlueprintCallable, Category = "Project Air Defense|Graphics")
  void SetAmbientOcclusionEnabled(bool bEnabled);

  UFUNCTION(BlueprintPure, Category = "Project Air Defense|Graphics")
  bool IsAmbientOcclusionEnabled() const;

  UFUNCTION(BlueprintCallable, Category = "Project Air Defense|Graphics")
  void SetMotionBlurEnabled(bool bEnabled);

  UFUNCTION(BlueprintPure, Category = "Project Air Defense|Graphics")
  bool IsMotionBlurEnabled() const;

  UFUNCTION(BlueprintCallable, Category = "Project Air Defense|Graphics")
  void ApplyHighQualityDefaults();

  UFUNCTION(BlueprintPure, Category = "Project Air Defense|Graphics")
  static UProjectAirDefenseGameUserSettings* GetProjectAirDefenseGameUserSettings();

private:
  void ApplyProjectSpecificCvars() const;
  void ClampProjectSpecificSettings();
  void LogActiveGraphicsSettings() const;

  UPROPERTY(Config)
  EProjectAirDefenseAntiAliasingMethod PreferredAntiAliasingMethod;

  UPROPERTY(Config)
  bool bAmbientOcclusionEnabled;

  UPROPERTY(Config)
  bool bMotionBlurEnabled;
};
