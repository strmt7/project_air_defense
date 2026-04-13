#include "ProjectAirDefenseGameMode.h"

#include "Cesium3DTileset.h"
#include "CesiumGeoreference.h"
#include "CesiumSunSky.h"
#include "HAL/FileManager.h"
#include "Misc/Paths.h"
#include "ProjectAirDefenseCityCameraPawn.h"
#include "ProjectAirDefenseRuntimeSettings.h"

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
    SunSky->SolarTime = Settings->SolarTime;
    SunSky->Month = Settings->Month;
    SunSky->Day = Settings->Day;
    SunSky->UpdateSun();
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

  APlayerController* PlayerController = World->GetFirstPlayerController();
  if (PlayerController != nullptr) {
    if (AProjectAirDefenseCityCameraPawn* CameraPawn =
            Cast<AProjectAirDefenseCityCameraPawn>(PlayerController->GetPawn())) {
      CameraPawn->SetFocusPoint(FVector::ZeroVector);
    }
  }

  UE_LOG(
      LogTemp,
      Log,
      TEXT("[%s] Bootstrapped pilot city scene from %s"),
      ProjectCategory,
      *TilesetJson);
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
