#include "ProjectAirDefenseRuntimeSettings.h"

UProjectAirDefenseRuntimeSettings::UProjectAirDefenseRuntimeSettings() {
  this->bEnablePilotCityScene = true;
  this->PreferredSourceId = TEXT("helsinki_kalasatama_3dtiles");
  this->PilotManifestRelativePath = TEXT("../../data/ue5_city_pilot/helsinki_kalasatama/pilot_manifest.json");
  this->LocalTilesetRootRelativePath = TEXT("ExternalData/helsinki_kalasatama_3dtiles");
  this->TilesetJsonFileName = TEXT("tileset.json");
  this->OriginLatitude = 60.1875;
  this->OriginLongitude = 24.9769;
  this->OriginHeightMeters = 0.0;
  this->TimeZone = 2.0;
  this->SolarTime = 17.0;
  this->Month = 9;
  this->Day = 21;
  this->Year = 2025;
  this->bUseDaylightSavingTime = true;
  this->DirectionalLightIntensityLux = 3000.0;
  this->SkyLightIntensityScale = 0.5;
  this->CameraFieldOfViewDegrees = 55.0f;
  this->CameraDefaultDistanceMeters = 2200.0f;
  this->CameraMinDistanceMeters = 350.0f;
  this->CameraMaxDistanceMeters = 20000.0f;
  this->CameraDefaultYawDegrees = 32.0f;
  this->CameraDefaultPitchDegrees = -52.0f;
  this->CameraMinPitchDegrees = -80.0f;
  this->CameraMaxPitchDegrees = -8.0f;
  this->CameraPanSpeedMetersPerSecond = 2200.0f;
  this->CameraVerticalSpeedMetersPerSecond = 1500.0f;
  this->CameraRotationSpeedDegreesPerSecond = 75.0f;
  this->CameraZoomSpeedMetersPerSecond = 2200.0f;
  this->CameraFramedDistanceMultiplier = 1.9f;
  this->CameraFocusVerticalOffsetRatio = 0.1f;
  this->MaximumScreenSpaceError = 8.0;
  this->MaximumCachedBytes = 1024LL * 1024LL * 1024LL;
  this->MaximumSimultaneousTileLoads = 32;
  this->LoadingDescendantLimit = 64;
}
