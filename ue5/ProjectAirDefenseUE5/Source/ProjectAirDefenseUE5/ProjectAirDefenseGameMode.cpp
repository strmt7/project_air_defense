#include "ProjectAirDefenseGameMode.h"

#include "Cesium3DTileset.h"
#include "CesiumGeoreference.h"
#include "CesiumSunSky.h"
#include "GameFramework/WorldSettings.h"
#include "Dom/JsonObject.h"
#include "HAL/FileManager.h"
#include "Misc/Paths.h"
#include "Misc/FileHelper.h"
#include "ProjectAirDefenseCityCameraPawn.h"
#include "ProjectAirDefenseRuntimeSettings.h"
#include "Serialization/JsonReader.h"
#include "Serialization/JsonSerializer.h"

namespace {
constexpr const TCHAR* ProjectCategory = TEXT("ProjectAirDefense");
}

AProjectAirDefenseGameMode::AProjectAirDefenseGameMode() {
  this->DefaultPawnClass = AProjectAirDefenseCityCameraPawn::StaticClass();
}

void AProjectAirDefenseGameMode::BeginPlay() {
  Super::BeginPlay();
  this->BootstrapPilotScene();
}

void AProjectAirDefenseGameMode::BootstrapPilotScene() {
  const UProjectAirDefenseRuntimeSettings* Settings = GetDefault<UProjectAirDefenseRuntimeSettings>();
  if (Settings == nullptr || !Settings->bEnablePilotCityScene) {
    return;
  }

  UWorld* World = this->GetWorld();
  if (World == nullptr) {
    return;
  }

  if (AWorldSettings* WorldSettings = World->GetWorldSettings()) {
    WorldSettings->bEnableWorldBoundsChecks = false;
  }

  FString TilesetJson = this->FindTilesetJson();
  if (TilesetJson.IsEmpty()) {
    UE_LOG(
        LogTemp,
        Warning,
        TEXT("[%s] No local tileset.json found under %s"),
        ProjectCategory,
        *Settings->LocalTilesetRootRelativePath);
    return;
  }

  ACesiumGeoreference* Georeference = this->FindExistingActor<ACesiumGeoreference>();
  if (Georeference == nullptr) {
    Georeference = World->SpawnActor<ACesiumGeoreference>();
  }
  if (Georeference == nullptr) {
    return;
  }

  Georeference->SetOriginPlacement(EOriginPlacement::CartographicOrigin);
  Georeference->SetOriginLatitude(Settings->OriginLatitude);
  Georeference->SetOriginLongitude(Settings->OriginLongitude);
  Georeference->SetOriginHeight(Settings->OriginHeightMeters);

  ACesiumSunSky* SunSky = this->FindExistingActor<ACesiumSunSky>();
  if (SunSky == nullptr) {
    SunSky = World->SpawnActor<ACesiumSunSky>();
  }
  if (SunSky != nullptr) {
    this->ApplyPilotSceneLighting(*SunSky);
  }

  ACesium3DTileset* Tileset = this->FindExistingActor<ACesium3DTileset>();
  if (Tileset == nullptr) {
    Tileset = World->SpawnActor<ACesium3DTileset>();
  }
  if (Tileset == nullptr) {
    return;
  }

  Tileset->SetTilesetSource(ETilesetSource::FromUrl);
  Tileset->SetUrl(this->BuildFileUri(TilesetJson));
  Tileset->SetGeoreference(Georeference);
  Tileset->ShowCreditsOnScreen = false;
  Tileset->ApplyDpiScaling = EApplyDpiScaling::No;
  Tileset->MaximumScreenSpaceError = Settings->MaximumScreenSpaceError;
  Tileset->MaximumCachedBytes = Settings->MaximumCachedBytes;
  Tileset->MaximumSimultaneousTileLoads = Settings->MaximumSimultaneousTileLoads;
  Tileset->LoadingDescendantLimit = Settings->LoadingDescendantLimit;
  Tileset->PreloadAncestors = true;
  Tileset->PreloadSiblings = true;
  Tileset->ForbidHoles = true;
  Tileset->EnableFrustumCulling = true;
  Tileset->EnableFogCulling = false;
  Tileset->EnforceCulledScreenSpaceError = true;
  Tileset->CulledScreenSpaceError = 24.0;
  Tileset->UseLodTransitions = true;
  Tileset->LodTransitionLength = 0.35f;
  Tileset->SuspendUpdate = false;
  Tileset->RefreshTileset();

  FVector CameraFocusPoint = FVector::ZeroVector;
  double TilesetRadiusMeters = 0.0;
  if (!this->TryResolveTilesetFocusPoint(
          TilesetJson,
          *Georeference,
          CameraFocusPoint,
          TilesetRadiusMeters)) {
    CameraFocusPoint = FVector::ZeroVector;
  }

  APlayerController* PlayerController = World->GetFirstPlayerController();
  if (PlayerController != nullptr) {
    if (AProjectAirDefenseCityCameraPawn* CameraPawn =
            Cast<AProjectAirDefenseCityCameraPawn>(PlayerController->GetPawn())) {
      CameraPawn->FrameFocusPoint(
          CameraFocusPoint,
          static_cast<float>(TilesetRadiusMeters));
    }
  }

  UE_LOG(
      LogTemp,
      Log,
      TEXT("[%s] Bootstrapped pilot city scene from %s (focus=%s radius=%.2f m)"),
      ProjectCategory,
      *TilesetJson,
      *CameraFocusPoint.ToString(),
      TilesetRadiusMeters);
}

void AProjectAirDefenseGameMode::ApplyPilotSceneLighting(ACesiumSunSky& SunSky) const {
  const UProjectAirDefenseRuntimeSettings* Settings = GetDefault<UProjectAirDefenseRuntimeSettings>();
  if (Settings == nullptr) {
    return;
  }

  SunSky.TimeZone = Settings->TimeZone;
  SunSky.SolarTime = Settings->SolarTime;
  SunSky.Month = Settings->Month;
  SunSky.Day = Settings->Day;
  SunSky.Year = Settings->Year;
  SunSky.UseDaylightSavingTime = Settings->bUseDaylightSavingTime;
  SunSky.DirectionalLight->SetIntensity(Settings->DirectionalLightIntensityLux);
  SunSky.SkyLight->SetIntensity(Settings->SkyLightIntensityScale);
  SunSky.UpdateSun();
}

FString AProjectAirDefenseGameMode::ResolveRepoRelativePath(const FString& RelativePath) const {
  const FString ProjectDir = FPaths::ConvertRelativePathToFull(FPaths::ProjectDir());
  const FString CombinedPath = FPaths::Combine(ProjectDir, RelativePath);
  return FPaths::ConvertRelativePathToFull(CombinedPath);
}

FString AProjectAirDefenseGameMode::FindTilesetJson() const {
  const UProjectAirDefenseRuntimeSettings* Settings = GetDefault<UProjectAirDefenseRuntimeSettings>();
  if (Settings == nullptr) {
    return FString();
  }

  const FString RootPath = this->ResolveRepoRelativePath(Settings->LocalTilesetRootRelativePath);
  const FString DirectCandidate = FPaths::Combine(RootPath, Settings->TilesetJsonFileName);
  if (FPaths::FileExists(DirectCandidate)) {
    return FPaths::ConvertRelativePathToFull(DirectCandidate);
  }
  TArray<FString> Candidates;
  IFileManager::Get().FindFilesRecursive(
      Candidates,
      *RootPath,
      *Settings->TilesetJsonFileName,
      true,
      false,
      false);
  Candidates.Sort();
  return Candidates.Num() > 0 ? Candidates[0] : FString();
}

FString AProjectAirDefenseGameMode::BuildFileUri(const FString& AbsolutePath) const {
  FString NormalizedPath = FPaths::ConvertRelativePathToFull(AbsolutePath);
  FPaths::NormalizeFilename(NormalizedPath);
  if (NormalizedPath.Len() >= 2 && NormalizedPath[1] == TEXT(':')) {
    NormalizedPath = TEXT("/") + NormalizedPath;
  }
  return TEXT("file://") + NormalizedPath;
}

bool AProjectAirDefenseGameMode::TryReadTilesetRootBoundingSphere(
    const FString& TilesetJsonPath,
    FVector& OutCenterEarthCenteredEarthFixed,
    double& OutRadiusMeters) const {
  FString JsonText;
  if (!FFileHelper::LoadFileToString(JsonText, *TilesetJsonPath)) {
    return false;
  }

  TSharedPtr<FJsonObject> TilesetObject;
  const TSharedRef<TJsonReader<>> Reader = TJsonReaderFactory<>::Create(JsonText);
  if (!FJsonSerializer::Deserialize(Reader, TilesetObject) || !TilesetObject.IsValid()) {
    return false;
  }

  const TSharedPtr<FJsonObject>* RootObject = nullptr;
  if (!TilesetObject->TryGetObjectField(TEXT("root"), RootObject) ||
      RootObject == nullptr ||
      !RootObject->IsValid()) {
    return false;
  }

  const TSharedPtr<FJsonObject>* BoundingVolumeObject = nullptr;
  if (!(*RootObject)->TryGetObjectField(TEXT("boundingVolume"), BoundingVolumeObject) ||
      BoundingVolumeObject == nullptr ||
      !BoundingVolumeObject->IsValid()) {
    return false;
  }

  const TArray<TSharedPtr<FJsonValue>>* SphereValues = nullptr;
  if (!(*BoundingVolumeObject)->TryGetArrayField(TEXT("sphere"), SphereValues) ||
      SphereValues == nullptr ||
      SphereValues->Num() != 4) {
    return false;
  }

  OutCenterEarthCenteredEarthFixed = FVector(
      static_cast<float>((*SphereValues)[0]->AsNumber()),
      static_cast<float>((*SphereValues)[1]->AsNumber()),
      static_cast<float>((*SphereValues)[2]->AsNumber()));
  OutRadiusMeters = (*SphereValues)[3]->AsNumber();
  return OutRadiusMeters > 0.0;
}

bool AProjectAirDefenseGameMode::TryResolveTilesetFocusPoint(
    const FString& TilesetJsonPath,
    const ACesiumGeoreference& Georeference,
    FVector& OutFocusPoint,
    double& OutRadiusMeters) const {
  FVector EarthCenteredEarthFixedCenter = FVector::ZeroVector;
  if (!this->TryReadTilesetRootBoundingSphere(
          TilesetJsonPath,
          EarthCenteredEarthFixedCenter,
          OutRadiusMeters)) {
    return false;
  }

  OutFocusPoint =
      Georeference.TransformEarthCenteredEarthFixedPositionToUnreal(
          EarthCenteredEarthFixedCenter);
  return !OutFocusPoint.ContainsNaN();
}
