#include "ProjectAirDefenseGameMode.h"

#include "Cesium3DTileset.h"
#include "CesiumGeoreference.h"
#include "CesiumGlobeAnchorComponent.h"
#include "CesiumSunSky.h"
#include "HighResScreenshot.h"
#include "HAL/FileManager.h"
#include "Kismet/KismetSystemLibrary.h"
#include "Misc/CommandLine.h"
#include "Engine/ExponentialHeightFog.h"
#include "Components/DirectionalLightComponent.h"
#include "Components/ExponentialHeightFogComponent.h"
#include "Components/SkyLightComponent.h"
#include "Engine/PostProcessVolume.h"
#include "GameFramework/HUD.h"
#include "GameFramework/WorldSettings.h"
#include "Dom/JsonObject.h"
#include "Misc/Paths.h"
#include "Misc/Parse.h"
#include "Misc/FileHelper.h"
#include "ProjectAirDefenseBattleManager.h"
#include "ProjectAirDefenseCityCameraPawn.h"
#include "ProjectAirDefensePlayerController.h"
#include "ProjectAirDefenseRuntimeSettings.h"
#include "Serialization/JsonReader.h"
#include "Serialization/JsonSerializer.h"

namespace {
constexpr const TCHAR* ProjectCategory = TEXT("ProjectAirDefense");

double WrapSolarTime(double SolarTime) {
  double WrappedTime = FMath::Fmod(SolarTime, 24.0);
  if (WrappedTime < 0.0) {
    WrappedTime += 24.0;
  }
  return WrappedTime;
}

double DaylightAlphaForSolarTime(double SolarTime) {
  const double SunHeight = FMath::Cos((WrapSolarTime(SolarTime) - 12.0) * UE_DOUBLE_PI / 12.0);
  const double RawAlpha = FMath::Clamp((SunHeight + 0.08) / 1.08, 0.0, 1.0);
  return RawAlpha * RawAlpha * (3.0 - 2.0 * RawAlpha);
}

double ResolveTimeScaleMaxHoursPerMinute(const UProjectAirDefenseRuntimeSettings* Settings) {
  return Settings == nullptr ? 12.0 : FMath::Max(Settings->TimeScaleMaxHoursPerMinute, 0.25);
}

double ResolveTimeScaleResumeHoursPerMinute(const UProjectAirDefenseRuntimeSettings* Settings) {
  const double MaxScale = ResolveTimeScaleMaxHoursPerMinute(Settings);
  const double PreferredScale =
      Settings != nullptr && Settings->TimeScaleDefaultHoursPerMinute > 0.0
          ? Settings->TimeScaleDefaultHoursPerMinute
          : (Settings == nullptr ? 1.0 : Settings->TimeScaleStepHoursPerMinute);
  return FMath::Clamp(PreferredScale, 0.25, MaxScale);
}
}

AProjectAirDefenseGameMode::AProjectAirDefenseGameMode() {
  this->PrimaryActorTick.bCanEverTick = true;
  this->DefaultPawnClass = AProjectAirDefenseCityCameraPawn::StaticClass();
  this->PlayerControllerClass = AProjectAirDefensePlayerController::StaticClass();
  this->HUDClass = AHUD::StaticClass();
}

void AProjectAirDefenseGameMode::BeginPlay() {
  Super::BeginPlay();
  this->BootstrapPilotScene();
  this->ConfigureVerificationCapture();
}

void AProjectAirDefenseGameMode::Tick(float DeltaSeconds) {
  Super::Tick(DeltaSeconds);

  if (this->RuntimeSunSky == nullptr || FMath::IsNearlyZero(this->RuntimeTimeScaleHoursPerMinute)) {
    return;
  }

  this->RuntimeSolarTime =
      WrapSolarTime(this->RuntimeSolarTime + static_cast<double>(DeltaSeconds) * this->RuntimeTimeScaleHoursPerMinute / 60.0);
  this->ApplyRuntimeSolarLighting();
}

void AProjectAirDefenseGameMode::AdjustSolarTime(double DeltaHours) {
  this->RuntimeSolarTime = WrapSolarTime(this->RuntimeSolarTime + DeltaHours);
  this->ApplyRuntimeSolarLighting();
}

void AProjectAirDefenseGameMode::SetSolarTime(double SolarTimeHours) {
  this->RuntimeSolarTime = WrapSolarTime(SolarTimeHours);
  this->ApplyRuntimeSolarLighting();
}

void AProjectAirDefenseGameMode::AdjustTimeScale(double DeltaHoursPerMinute) {
  const UProjectAirDefenseRuntimeSettings* Settings = GetDefault<UProjectAirDefenseRuntimeSettings>();
  this->RuntimeTimeScaleHoursPerMinute =
      FMath::Clamp(
          this->RuntimeTimeScaleHoursPerMinute + DeltaHoursPerMinute,
          0.0,
          ResolveTimeScaleMaxHoursPerMinute(Settings));
}

void AProjectAirDefenseGameMode::SetTimeScale(double HoursPerMinute) {
  const UProjectAirDefenseRuntimeSettings* Settings = GetDefault<UProjectAirDefenseRuntimeSettings>();
  this->RuntimeTimeScaleHoursPerMinute =
      FMath::Clamp(HoursPerMinute, 0.0, ResolveTimeScaleMaxHoursPerMinute(Settings));
}

void AProjectAirDefenseGameMode::ToggleTimeCycle() {
  const UProjectAirDefenseRuntimeSettings* Settings = GetDefault<UProjectAirDefenseRuntimeSettings>();
  this->RuntimeTimeScaleHoursPerMinute =
      FMath::IsNearlyZero(this->RuntimeTimeScaleHoursPerMinute) ? ResolveTimeScaleResumeHoursPerMinute(Settings) : 0.0;
}

double AProjectAirDefenseGameMode::GetSolarTime() const {
  return WrapSolarTime(this->RuntimeSolarTime);
}

double AProjectAirDefenseGameMode::GetTimeScale() const {
  return this->RuntimeTimeScaleHoursPerMinute;
}

double AProjectAirDefenseGameMode::GetTimeScaleMax() const {
  return ResolveTimeScaleMaxHoursPerMinute(GetDefault<UProjectAirDefenseRuntimeSettings>());
}

FString AProjectAirDefenseGameMode::BuildTimeSummaryText() const {
  const int32 TotalMinutes = FMath::RoundToInt(WrapSolarTime(this->RuntimeSolarTime) * 60.0) % (24 * 60);
  const int32 Hour = TotalMinutes / 60;
  const int32 Minute = TotalMinutes % 60;
  if (FMath::IsNearlyZero(this->RuntimeTimeScaleHoursPerMinute)) {
    return FString::Printf(TEXT("TIME %02d:%02d | PAUSED"), Hour, Minute);
  }
  return FString::Printf(TEXT("TIME %02d:%02d | %.1fh/min"), Hour, Minute, this->RuntimeTimeScaleHoursPerMinute);
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

  FString TilesetJson;
  FString TilesetUrl = Settings->RemoteTilesetUrl;
  const bool bUseRemoteTileset = !TilesetUrl.IsEmpty();
  if (TilesetUrl.IsEmpty()) {
    TilesetJson = this->FindTilesetJson();
  }
  if (TilesetUrl.IsEmpty() && TilesetJson.IsEmpty()) {
    UE_LOG(
        LogTemp,
        Warning,
        TEXT("[%s] No remote tileset URL configured and no local tileset.json found under %s"),
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
    if (SunSky->GlobeAnchor != nullptr) {
      SunSky->GlobeAnchor->SetGeoreference(Georeference);
      SunSky->GlobeAnchor->MoveToEarthCenteredEarthFixedPosition(FVector::ZeroVector);
    }
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
  if (TilesetUrl.IsEmpty()) {
    TilesetUrl = this->BuildFileUri(TilesetJson);
  }
  Tileset->SetUrl(TilesetUrl);
  Tileset->SetGeoreference(Georeference);
  Tileset->ShowCreditsOnScreen = false;
  Tileset->ApplyDpiScaling = EApplyDpiScaling::UseProjectDefault;
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
  double TilesetRadiusMeters = FMath::Max(Settings->RemoteTilesetFallbackRadiusMeters, 0.0);
  double CameraFrameRadiusMeters = TilesetRadiusMeters;
  if (!TilesetJson.IsEmpty()) {
    if (this->TryResolveTilesetFocusPoint(
            TilesetJson,
            *Georeference,
            CameraFocusPoint,
            TilesetRadiusMeters)) {
      CameraFrameRadiusMeters = TilesetRadiusMeters;
    } else {
      CameraFocusPoint = FVector::ZeroVector;
    }
  }
  if (bUseRemoteTileset && Settings->RemoteTilesetCameraFrameRadiusMeters > 0.0) {
    CameraFrameRadiusMeters =
        TilesetRadiusMeters > 0.0
            ? FMath::Min(Settings->RemoteTilesetCameraFrameRadiusMeters, TilesetRadiusMeters)
            : Settings->RemoteTilesetCameraFrameRadiusMeters;
  }

  APlayerController* PlayerController = World->GetFirstPlayerController();
  if (PlayerController != nullptr) {
    if (AProjectAirDefenseCityCameraPawn* CameraPawn =
            Cast<AProjectAirDefenseCityCameraPawn>(PlayerController->GetPawn())) {
      CameraPawn->FrameFocusPoint(
          CameraFocusPoint,
          static_cast<float>(CameraFrameRadiusMeters));
    }
  }

  if (AProjectAirDefenseBattleManager* RuntimeBattleManager = this->EnsureBattleManager()) {
    RuntimeBattleManager->InitializeBattlefield(CameraFocusPoint, TilesetRadiusMeters);
  }

  UE_LOG(
      LogTemp,
      Log,
      TEXT("[%s] Bootstrapped pilot city scene from %s (focus=%s gameplayRadius=%.2f m cameraFrameRadius=%.2f m)"),
      ProjectCategory,
      *TilesetUrl,
      *CameraFocusPoint.ToString(),
      TilesetRadiusMeters,
      CameraFrameRadiusMeters);
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

  const bool bAutoQuitAfterVerification =
      FParse::Param(FCommandLine::Get(), TEXT("ProjectAirDefenseAutoQuitAfterVerification"));
  const FString AbsoluteVerificationPath =
      FPaths::ConvertRelativePathToFull(VerificationPath);
  IFileManager::Get().MakeDirectory(
      *FPaths::GetPath(AbsoluteVerificationPath),
      true);

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

void AProjectAirDefenseGameMode::ApplyPilotSceneLighting(ACesiumSunSky& SunSky) {
  const UProjectAirDefenseRuntimeSettings* Settings = GetDefault<UProjectAirDefenseRuntimeSettings>();
  if (Settings == nullptr) {
    return;
  }

  this->RuntimeSunSky = &SunSky;
  this->RuntimeSolarTime = WrapSolarTime(Settings->SolarTime);
  this->RuntimeTimeScaleHoursPerMinute =
      FMath::Clamp(Settings->TimeScaleDefaultHoursPerMinute, 0.0, ResolveTimeScaleMaxHoursPerMinute(Settings));
  SunSky.TimeZone = Settings->TimeZone;
  SunSky.SolarTime = this->RuntimeSolarTime;
  SunSky.Month = Settings->Month;
  SunSky.Day = Settings->Day;
  SunSky.Year = Settings->Year;
  SunSky.UseDaylightSavingTime = Settings->bUseDaylightSavingTime;
  if (SunSky.DirectionalLight != nullptr) {
    SunSky.DirectionalLight->SetCastShadows(true);
  }
  if (SunSky.SkyLight != nullptr) {
    SunSky.SkyLight->SetRealTimeCapture(false);
  }
  this->ApplyRuntimeSolarLighting();
}

void AProjectAirDefenseGameMode::ApplyRuntimeSolarLighting() {
  const UProjectAirDefenseRuntimeSettings* Settings = GetDefault<UProjectAirDefenseRuntimeSettings>();
  if (Settings == nullptr || this->RuntimeSunSky == nullptr) {
    return;
  }

  this->RuntimeSunSky->SolarTime = this->RuntimeSolarTime;
  this->RuntimeSunSky->UpdateSun();

  const double DayAlpha = DaylightAlphaForSolarTime(this->RuntimeSolarTime);
  if (this->RuntimeSunSky->DirectionalLight != nullptr) {
    this->RuntimeSunSky->DirectionalLight->SetIntensity(
        FMath::Lerp(Settings->NightDirectionalLightIntensityLux, Settings->DirectionalLightIntensityLux, DayAlpha));
  }
  if (this->RuntimeSunSky->SkyLight != nullptr) {
    this->RuntimeSunSky->SkyLight->SetIntensity(
        FMath::Lerp(Settings->NightSkyLightIntensityScale, Settings->SkyLightIntensityScale, DayAlpha));
  }
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
  const bool bOverrideExposureBias = !FMath::IsNearlyZero(Settings->PostExposureBias);
  PostProcessSettings.bOverride_AutoExposureBias = bOverrideExposureBias;
  if (bOverrideExposureBias) {
    PostProcessSettings.AutoExposureBias = Settings->PostExposureBias;
  }
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
