#pragma once

#include "CoreMinimal.h"
#include "Engine/DeveloperSettings.h"
#include "ProjectAirDefenseRuntimeSettings.generated.h"

UCLASS(Config = Game, DefaultConfig, meta = (DisplayName = "Project Air Defense Runtime"))
class PROJECTAIRDEFENSEUE5_API UProjectAirDefenseRuntimeSettings : public UDeveloperSettings {
  GENERATED_BODY()

public:
  UProjectAirDefenseRuntimeSettings();

  UPROPERTY(EditAnywhere, Config, Category = "City Pilot")
  bool bEnablePilotCityScene;

  UPROPERTY(EditAnywhere, Config, Category = "City Pilot")
  FString PreferredSourceId;

  UPROPERTY(EditAnywhere, Config, Category = "City Pilot")
  FString PilotManifestRelativePath;

  UPROPERTY(EditAnywhere, Config, Category = "City Pilot")
  FString LocalTilesetRootRelativePath;

  UPROPERTY(EditAnywhere, Config, Category = "City Pilot")
  FString TilesetJsonFileName;

  UPROPERTY(EditAnywhere, Config, Category = "Georeference")
  double OriginLatitude;

  UPROPERTY(EditAnywhere, Config, Category = "Georeference")
  double OriginLongitude;

  UPROPERTY(EditAnywhere, Config, Category = "Georeference")
  double OriginHeightMeters;

  UPROPERTY(EditAnywhere, Config, Category = "Lighting")
  double TimeZone;

  UPROPERTY(EditAnywhere, Config, Category = "Lighting")
  double SolarTime;

  UPROPERTY(EditAnywhere, Config, Category = "Lighting")
  int32 Month;

  UPROPERTY(EditAnywhere, Config, Category = "Lighting")
  int32 Day;

  UPROPERTY(EditAnywhere, Config, Category = "Lighting")
  int32 Year;

  UPROPERTY(EditAnywhere, Config, Category = "Lighting")
  bool bUseDaylightSavingTime;

  UPROPERTY(EditAnywhere, Config, Category = "Lighting")
  double DirectionalLightIntensityLux;

  UPROPERTY(EditAnywhere, Config, Category = "Lighting")
  double SkyLightIntensityScale;

  UPROPERTY(EditAnywhere, Config, Category = "Atmosphere")
  FLinearColor FogInscatteringColor;

  UPROPERTY(EditAnywhere, Config, Category = "Atmosphere")
  FLinearColor DirectionalInscatteringColor;

  UPROPERTY(EditAnywhere, Config, Category = "Atmosphere")
  FLinearColor SkyAtmosphereContributionColor;

  UPROPERTY(EditAnywhere, Config, Category = "Atmosphere")
  float FogDensity;

  UPROPERTY(EditAnywhere, Config, Category = "Atmosphere")
  float FogHeightFalloff;

  UPROPERTY(EditAnywhere, Config, Category = "Atmosphere")
  float FogMaxOpacity;

  UPROPERTY(EditAnywhere, Config, Category = "Atmosphere")
  float FogStartDistanceMeters;

  UPROPERTY(EditAnywhere, Config, Category = "Atmosphere")
  float FogCutoffDistanceMeters;

  UPROPERTY(EditAnywhere, Config, Category = "Atmosphere")
  float DirectionalInscatteringExponent;

  UPROPERTY(EditAnywhere, Config, Category = "Atmosphere")
  float DirectionalInscatteringStartDistanceMeters;

  UPROPERTY(EditAnywhere, Config, Category = "Atmosphere")
  bool bEnableVolumetricFog;

  UPROPERTY(EditAnywhere, Config, Category = "Atmosphere")
  float VolumetricFogScatteringDistribution;

  UPROPERTY(EditAnywhere, Config, Category = "Atmosphere")
  float VolumetricFogExtinctionScale;

  UPROPERTY(EditAnywhere, Config, Category = "Atmosphere")
  float VolumetricFogViewDistanceMeters;

  UPROPERTY(EditAnywhere, Config, Category = "Atmosphere")
  float VolumetricFogStartDistanceMeters;

  UPROPERTY(EditAnywhere, Config, Category = "Atmosphere")
  float VolumetricFogNearFadeInDistanceMeters;

  UPROPERTY(EditAnywhere, Config, Category = "Post Process")
  float PostExposureBias;

  UPROPERTY(EditAnywhere, Config, Category = "Post Process")
  float PostBloomIntensity;

  UPROPERTY(EditAnywhere, Config, Category = "Post Process")
  float PostVignetteIntensity;

  UPROPERTY(EditAnywhere, Config, Category = "Post Process")
  float PostSaturation;

  UPROPERTY(EditAnywhere, Config, Category = "Post Process")
  float PostContrast;

  UPROPERTY(EditAnywhere, Config, Category = "Camera")
  float CameraFieldOfViewDegrees;

  UPROPERTY(EditAnywhere, Config, Category = "Camera")
  float CameraDefaultDistanceMeters;

  UPROPERTY(EditAnywhere, Config, Category = "Camera")
  float CameraMinDistanceMeters;

  UPROPERTY(EditAnywhere, Config, Category = "Camera")
  float CameraMaxDistanceMeters;

  UPROPERTY(EditAnywhere, Config, Category = "Camera")
  float CameraDefaultYawDegrees;

  UPROPERTY(EditAnywhere, Config, Category = "Camera")
  float CameraDefaultPitchDegrees;

  UPROPERTY(EditAnywhere, Config, Category = "Camera")
  float CameraMinPitchDegrees;

  UPROPERTY(EditAnywhere, Config, Category = "Camera")
  float CameraMaxPitchDegrees;

  UPROPERTY(EditAnywhere, Config, Category = "Camera")
  float CameraPanSpeedMetersPerSecond;

  UPROPERTY(EditAnywhere, Config, Category = "Camera")
  float CameraVerticalSpeedMetersPerSecond;

  UPROPERTY(EditAnywhere, Config, Category = "Camera")
  float CameraRotationSpeedDegreesPerSecond;

  UPROPERTY(EditAnywhere, Config, Category = "Camera")
  float CameraZoomSpeedMetersPerSecond;

  UPROPERTY(EditAnywhere, Config, Category = "Camera")
  float CameraFramedDistanceMultiplier;

  UPROPERTY(EditAnywhere, Config, Category = "Camera")
  float CameraFocusVerticalOffsetRatio;

  UPROPERTY(EditAnywhere, Config, Category = "Tileset Quality")
  double MaximumScreenSpaceError;

  UPROPERTY(EditAnywhere, Config, Category = "Tileset Quality")
  int64 MaximumCachedBytes;

  UPROPERTY(EditAnywhere, Config, Category = "Tileset Quality")
  int32 MaximumSimultaneousTileLoads;

  UPROPERTY(EditAnywhere, Config, Category = "Tileset Quality")
  int32 LoadingDescendantLimit;

  UPROPERTY(EditAnywhere, Config, Category = "Gameplay")
  bool bAutoStartFirstWave;

  UPROPERTY(EditAnywhere, Config, Category = "Gameplay")
  double AutoStartFirstWaveDelaySeconds;

  UPROPERTY(EditAnywhere, Config, Category = "Gameplay")
  double SimulationFixedStepSeconds;

  UPROPERTY(EditAnywhere, Config, Category = "Gameplay")
  double DistrictHalfExtentMeters;

  UPROPERTY(EditAnywhere, Config, Category = "Gameplay")
  double DistrictCellRadiusMeters;

  UPROPERTY(EditAnywhere, Config, Category = "Gameplay")
  double LauncherRingDistanceMeters;

  UPROPERTY(EditAnywhere, Config, Category = "Gameplay")
  double LauncherLateralSpacingMeters;
};
