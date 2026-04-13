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
  double SolarTime;

  UPROPERTY(EditAnywhere, Config, Category = "Lighting")
  int32 Month;

  UPROPERTY(EditAnywhere, Config, Category = "Lighting")
  int32 Day;

  UPROPERTY(EditAnywhere, Config, Category = "Tileset Quality")
  double MaximumScreenSpaceError;

  UPROPERTY(EditAnywhere, Config, Category = "Tileset Quality")
  int64 MaximumCachedBytes;

  UPROPERTY(EditAnywhere, Config, Category = "Tileset Quality")
  int32 MaximumSimultaneousTileLoads;

  UPROPERTY(EditAnywhere, Config, Category = "Tileset Quality")
  int32 LoadingDescendantLimit;
};
