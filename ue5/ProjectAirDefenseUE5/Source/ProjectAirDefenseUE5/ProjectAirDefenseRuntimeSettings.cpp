#include "ProjectAirDefenseRuntimeSettings.h"

UProjectAirDefenseRuntimeSettings::UProjectAirDefenseRuntimeSettings() {
  this->bEnablePilotCityScene = true;
  this->PreferredSourceId = TEXT("helsinki_kalasatama_3dtiles");
  this->PilotManifestRelativePath = TEXT("../../data/ue5_city_pilot/helsinki_kalasatama/pilot_manifest.json");
  this->LocalTilesetRootRelativePath = TEXT("../../data/external/helsinki_kalasatama_3dtiles");
  this->TilesetJsonFileName = TEXT("tileset.json");
  this->OriginLatitude = 60.1875;
  this->OriginLongitude = 24.9769;
  this->OriginHeightMeters = 0.0;
  this->SolarTime = 21.25;
  this->Month = 11;
  this->Day = 12;
  this->MaximumScreenSpaceError = 8.0;
  this->MaximumCachedBytes = 1024LL * 1024LL * 1024LL;
  this->MaximumSimultaneousTileLoads = 32;
  this->LoadingDescendantLimit = 64;
}
