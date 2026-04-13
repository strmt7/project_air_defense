#include "ProjectAirDefenseGameMode.h"

#include "Cesium3DTileset.h"
#include "CesiumGeoreference.h"
#include "CesiumSunSky.h"
#include "HighResScreenshot.h"
#include "HAL/FileManager.h"
#include "Kismet/KismetSystemLibrary.h"
#include "Misc/CommandLine.h"
#include "Engine/ExponentialHeightFog.h"
#include "Components/ExponentialHeightFogComponent.h"
#include "Engine/PostProcessVolume.h"
#include "GameFramework/WorldSettings.h"
#include "Dom/JsonObject.h"
#include "Misc/Paths.h"
#include "Misc/Parse.h"
#include "Misc/FileHelper.h"
#include "ProjectAirDefenseBattleHud.h"
#include "ProjectAirDefenseBattleManager.h"
#include "ProjectAirDefenseCityCameraPawn.h"
#include "ProjectAirDefensePlayerController.h"
#include "ProjectAirDefenseRuntimeSettings.h"
#include "Serialization/JsonReader.h"
#include "Serialization/JsonSerializer.h"

namespace {
constexpr const TCHAR* ProjectCategory = TEXT("ProjectAirDefense");
}

AProjectAirDefenseGameMode::AProjectAirDefenseGameMode() {
  this->DefaultPawnClass = AProjectAirDefenseCityCameraPawn::StaticClass();
  this->PlayerControllerClass = AProjectAirDefensePlayerController::StaticClass();
  this->HUDClass = AProjectAirDefenseBattleHud::StaticClass();
}

void AProjectAirDefenseGameMode::BeginPlay() {
  Super::BeginPlay();
  this->BootstrapPilotScene();
  this->ConfigureVerificationCapture();
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
  this->ApplyPilotSceneAtmosphere();

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

  if (AProjectAirDefenseBattleManager* RuntimeBattleManager = this->EnsureBattleManager()) {
    RuntimeBattleManager->InitializeBattlefield(CameraFocusPoint, TilesetRadiusMeters);
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

void AProjectAirDefenseGameMode::ConfigureVerificationCapture() {
  UWorld* World = this->GetWorld();
  if (World == nullptr) {
    return;
  }

  FString VerificationPath;
  if (!FParse::Value(
          FCommandLine::Get(),
          TEXT("ProjectAirDefenseVerificationPath="),
          VerificationPath) ||
      VerificationPath.IsEmpty()) {
    return;
  }

  double DelaySeconds = 0.0;
  FParse::Value(
      FCommandLine::Get(),
      TEXT("ProjectAirDefenseVerificationDelay="),
      DelaySeconds);
  DelaySeconds = FMath::Max(DelaySeconds, 0.0);

  const bool bShowSystemsMenu =
      FParse::Param(FCommandLine::Get(), TEXT("ProjectAirDefenseShowSystemsMenu"));
  const bool bAutoQuitAfterVerification =
      FParse::Param(FCommandLine::Get(), TEXT("ProjectAirDefenseAutoQuitAfterVerification"));
  const FString AbsoluteVerificationPath =
      FPaths::ConvertRelativePathToFull(VerificationPath);
  IFileManager::Get().MakeDirectory(
      *FPaths::GetPath(AbsoluteVerificationPath),
      true);

  if (AProjectAirDefensePlayerController* PlayerController =
          Cast<AProjectAirDefensePlayerController>(World->GetFirstPlayerController())) {
    PlayerController->SetSystemsMenuVisible(bShowSystemsMenu);
  }

  FTimerDelegate ScreenshotDelegate;
  ScreenshotDelegate.BindLambda([this, AbsoluteVerificationPath, bAutoQuitAfterVerification]() {
    FScreenshotRequest::RequestScreenshot(
        AbsoluteVerificationPath,
        true,
        false,
        false);

    if (!bAutoQuitAfterVerification) {
      return;
    }

    UWorld* InnerWorld = this->GetWorld();
    if (InnerWorld == nullptr) {
      return;
    }

    FTimerDelegate QuitDelegate;
    QuitDelegate.BindLambda([this]() {
      if (APlayerController* PlayerController = this->GetWorld() != nullptr
                                                    ? this->GetWorld()->GetFirstPlayerController()
                                                    : nullptr) {
        UKismetSystemLibrary::QuitGame(
            PlayerController,
            PlayerController,
            EQuitPreference::Quit,
            false);
      }
    });
    FTimerHandle QuitTimerHandle;
    InnerWorld->GetTimerManager().SetTimer(
        QuitTimerHandle,
        QuitDelegate,
        1.0f,
        false);
  });

  FTimerHandle ScreenshotTimerHandle;
  World->GetTimerManager().SetTimer(
      ScreenshotTimerHandle,
      ScreenshotDelegate,
      static_cast<float>(DelaySeconds),
      false);
}

AProjectAirDefenseBattleManager* AProjectAirDefenseGameMode::EnsureBattleManager() {
  if (this->BattleManager != nullptr) {
    return this->BattleManager;
  }

  UWorld* World = this->GetWorld();
  if (World == nullptr) {
    return nullptr;
  }

  if (AProjectAirDefenseBattleManager* Existing = this->FindExistingActor<AProjectAirDefenseBattleManager>()) {
    this->BattleManager = Existing;
    return Existing;
  }

  FActorSpawnParameters SpawnParameters;
  SpawnParameters.SpawnCollisionHandlingOverride = ESpawnActorCollisionHandlingMethod::AlwaysSpawn;
  this->BattleManager = World->SpawnActor<AProjectAirDefenseBattleManager>(SpawnParameters);
  return this->BattleManager;
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

void AProjectAirDefenseGameMode::ApplyPilotSceneAtmosphere() const {
  const UProjectAirDefenseRuntimeSettings* Settings = GetDefault<UProjectAirDefenseRuntimeSettings>();
  UWorld* World = this->GetWorld();
  if (Settings == nullptr || World == nullptr) {
    return;
  }

  AExponentialHeightFog* HeightFog = this->FindExistingActor<AExponentialHeightFog>();
  if (HeightFog == nullptr) {
    HeightFog = World->SpawnActor<AExponentialHeightFog>();
  }
  if (HeightFog != nullptr) {
    if (UExponentialHeightFogComponent* FogComponent = HeightFog->GetComponent()) {
      FogComponent->SetFogDensity(Settings->FogDensity);
      FogComponent->SetFogHeightFalloff(Settings->FogHeightFalloff);
      FogComponent->SetFogInscatteringColor(Settings->FogInscatteringColor);
      FogComponent->SetDirectionalInscatteringColor(Settings->DirectionalInscatteringColor);
      FogComponent->SetSkyAtmosphereAmbientContributionColorScale(
          Settings->SkyAtmosphereContributionColor);
      FogComponent->SetDirectionalInscatteringExponent(
          Settings->DirectionalInscatteringExponent);
      FogComponent->SetDirectionalInscatteringStartDistance(
          Settings->DirectionalInscatteringStartDistanceMeters * 100.0f);
      FogComponent->SetFogMaxOpacity(Settings->FogMaxOpacity);
      FogComponent->SetStartDistance(Settings->FogStartDistanceMeters * 100.0f);
      FogComponent->SetFogCutoffDistance(Settings->FogCutoffDistanceMeters * 100.0f);
      FogComponent->SetVolumetricFog(Settings->bEnableVolumetricFog);
      FogComponent->SetVolumetricFogScatteringDistribution(
          Settings->VolumetricFogScatteringDistribution);
      FogComponent->SetVolumetricFogExtinctionScale(
          Settings->VolumetricFogExtinctionScale);
      FogComponent->SetVolumetricFogDistance(
          Settings->VolumetricFogViewDistanceMeters * 100.0f);
      FogComponent->SetVolumetricFogStartDistance(
          Settings->VolumetricFogStartDistanceMeters * 100.0f);
      FogComponent->SetVolumetricFogNearFadeInDistance(
          Settings->VolumetricFogNearFadeInDistanceMeters * 100.0f);
    }
  }

  APostProcessVolume* PostProcessVolume = this->FindExistingActor<APostProcessVolume>();
  if (PostProcessVolume == nullptr) {
    PostProcessVolume = World->SpawnActor<APostProcessVolume>();
  }
  if (PostProcessVolume == nullptr) {
    return;
  }

  PostProcessVolume->bEnabled = true;
  PostProcessVolume->bUnbound = true;
  PostProcessVolume->BlendWeight = 1.0f;
  PostProcessVolume->Priority = 100.0f;

  FPostProcessSettings& PostProcessSettings = PostProcessVolume->Settings;
  PostProcessSettings.bOverride_AutoExposureBias = true;
  PostProcessSettings.AutoExposureBias = Settings->PostExposureBias;
  PostProcessSettings.bOverride_BloomIntensity = true;
  PostProcessSettings.BloomIntensity = Settings->PostBloomIntensity;
  PostProcessSettings.bOverride_VignetteIntensity = true;
  PostProcessSettings.VignetteIntensity = Settings->PostVignetteIntensity;
  PostProcessSettings.bOverride_ColorSaturation = true;
  PostProcessSettings.ColorSaturation = FVector4(
      Settings->PostSaturation,
      Settings->PostSaturation,
      Settings->PostSaturation,
      1.0f);
  PostProcessSettings.bOverride_ColorContrast = true;
  PostProcessSettings.ColorContrast = FVector4(
      Settings->PostContrast,
      Settings->PostContrast,
      Settings->PostContrast,
      1.0f);
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
