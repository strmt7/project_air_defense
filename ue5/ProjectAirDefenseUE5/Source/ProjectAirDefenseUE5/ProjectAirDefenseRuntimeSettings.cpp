#include "ProjectAirDefenseRuntimeSettings.h"

namespace {
constexpr double DistrictHalfExtentDefaultMeters = 7000.0;
constexpr double DistrictHalfExtentMinMeters = 700.0;
constexpr double DistrictHalfExtentMaxMeters = 20000.0;
constexpr double DistrictTilesetCoverageDefaultRatio = 0.82;
constexpr double DistrictTilesetCoverageMinRatio = 0.25;
constexpr double DistrictTilesetCoverageMaxRatio = 1.0;
constexpr double DistrictMinHalfExtentDefaultMeters = 1600.0;
constexpr double DistrictCellRadiusDefaultMeters = 260.0;
constexpr double DistrictCellRadiusMinMeters = 80.0;
constexpr double DistrictCellRadiusMaxMeters = 1000.0;
constexpr double DistrictLauncherRingDefaultMeters = 2400.0;
constexpr double DistrictLauncherLateralDefaultMeters = 1000.0;
constexpr double DistrictLauncherLateralMaxMeters = 5000.0;
constexpr double TilesetRadiusMaxMeters = 50000.0;

double ClampFiniteDouble(double Value, double Fallback, double MinValue, double MaxValue) {
  const double Candidate = FMath::IsFinite(Value) ? Value : Fallback;
  return FMath::Clamp(Candidate, MinValue, MaxValue);
}
} // namespace

UProjectAirDefenseRuntimeSettings::UProjectAirDefenseRuntimeSettings() {
  this->bEnablePilotCityScene = true;
  this->PreferredSourceId = TEXT("helsinki_kalasatama_3dtiles");
  this->PilotManifestRelativePath = TEXT("");
  this->LocalTilesetRootRelativePath = TEXT("ExternalData/helsinki_kalasatama_3dtiles");
  this->TilesetJsonFileName = TEXT("tileset.json");
  this->RemoteTilesetUrl = TEXT("");
  this->RemoteTilesetFallbackRadiusMeters = 2665.9;
  this->RemoteTilesetCameraFrameRadiusMeters = 2800.0;
  this->OriginLatitude = 60.184482688386694;
  this->OriginLongitude = 24.97747229490553;
  this->OriginHeightMeters = 28.4;
  this->TimeZone = 2.0;
  this->SolarTime = 14.0;
  this->Month = 9;
  this->Day = 21;
  this->Year = 2025;
  this->bUseDaylightSavingTime = true;
  this->DirectionalLightIntensityLux = 48000.0;
  this->SkyLightIntensityScale = 1.08;
  this->NightDirectionalLightIntensityLux = 60.0;
  this->NightSkyLightIntensityScale = 0.58;
  this->TimeControlStepHours = 1.0;
  this->TimeScaleDefaultHoursPerMinute = 0.0;
  this->TimeScaleStepHoursPerMinute = 1.0;
  this->TimeScaleMaxHoursPerMinute = 12.0;
  this->FogInscatteringColor = FLinearColor(0.64f, 0.74f, 0.90f, 1.0f);
  this->DirectionalInscatteringColor = FLinearColor(1.00f, 0.88f, 0.70f, 1.0f);
  this->SkyAtmosphereContributionColor = FLinearColor(0.52f, 0.64f, 0.82f, 1.0f);
  this->FogDensity = 0.00065f;
  this->FogHeightFalloff = 0.16f;
  this->FogMaxOpacity = 0.38f;
  this->FogStartDistanceMeters = 2500.0f;
  this->FogCutoffDistanceMeters = 850000.0f;
  this->DirectionalInscatteringExponent = 18.0f;
  this->DirectionalInscatteringStartDistanceMeters = 2400.0f;
  this->bEnableVolumetricFog = false;
  this->VolumetricFogScatteringDistribution = 0.28f;
  this->VolumetricFogExtinctionScale = 1.25f;
  this->VolumetricFogViewDistanceMeters = 110000.0f;
  this->VolumetricFogStartDistanceMeters = 1200.0f;
  this->VolumetricFogNearFadeInDistanceMeters = 900.0f;
  this->PostExposureBias = -0.18f;
  this->PostBloomIntensity = 0.05f;
  this->PostVignetteIntensity = 0.08f;
  this->PostSaturation = 1.06f;
  this->PostContrast = 1.08f;
  this->CameraFieldOfViewDegrees = 34.0f;
  this->CameraDefaultDistanceMeters = 4500.0f;
  this->CameraMinDistanceMeters = 350.0f;
  this->CameraMaxDistanceMeters = 12000.0f;
  this->CameraDefaultYawDegrees = 88.0f;
  this->CameraDefaultPitchDegrees = -18.0f;
  this->CameraMinPitchDegrees = -80.0f;
  this->CameraMaxPitchDegrees = -8.0f;
  this->CameraPanSpeedMetersPerSecond = 2200.0f;
  this->CameraVerticalSpeedMetersPerSecond = 1500.0f;
  this->CameraRotationSpeedDegreesPerSecond = 75.0f;
  this->CameraZoomSpeedMetersPerSecond = 2200.0f;
  this->CameraFramedDistanceMultiplier = 1.50f;
  this->CameraFocusVerticalOffsetRatio = 0.05f;
  this->MaximumScreenSpaceError = 1.00;
  this->MaximumCachedBytes = 4LL * 1024LL * 1024LL * 1024LL;
  this->MaximumSimultaneousTileLoads = 96;
  this->LoadingDescendantLimit = 192;
  this->bAutoStartFirstWave = false;
  this->AutoStartFirstWaveDelaySeconds = 1.6;
  this->SimulationFixedStepSeconds = 0.05;
  this->DistrictHalfExtentMeters = 7000.0;
  this->DistrictTilesetCoverageRatio = 0.82;
  this->DistrictMinHalfExtentMeters = 1600.0;
  this->DistrictCellRadiusMeters = 260.0;
  this->LauncherRingDistanceMeters = 2400.0;
  this->LauncherLateralSpacingMeters = 1000.0;
}

FProjectAirDefenseDistrictRuntimeInputs
MakeProjectAirDefenseDistrictRuntimeInputs(const UProjectAirDefenseRuntimeSettings* Settings) {
  FProjectAirDefenseDistrictRuntimeInputs Inputs;
  if (Settings == nullptr) {
    return Inputs;
  }

  Inputs.DistrictHalfExtentMeters = Settings->DistrictHalfExtentMeters;
  Inputs.DistrictTilesetCoverageRatio = Settings->DistrictTilesetCoverageRatio;
  Inputs.DistrictMinHalfExtentMeters = Settings->DistrictMinHalfExtentMeters;
  Inputs.DistrictCellRadiusMeters = Settings->DistrictCellRadiusMeters;
  Inputs.LauncherRingDistanceMeters = Settings->LauncherRingDistanceMeters;
  Inputs.LauncherLateralSpacingMeters = Settings->LauncherLateralSpacingMeters;
  return Inputs;
}

FProjectAirDefenseDistrictRuntimeConfig BuildProjectAirDefenseDistrictRuntimeConfig(
    const FProjectAirDefenseDistrictRuntimeInputs& Inputs,
    double TilesetRadiusMeters) {
  FProjectAirDefenseDistrictRuntimeConfig Config;
  const double DefaultHalfExtentMeters = ClampFiniteDouble(
      Inputs.DistrictHalfExtentMeters,
      DistrictHalfExtentDefaultMeters,
      DistrictHalfExtentMinMeters,
      DistrictHalfExtentMaxMeters);
  const double MinHalfExtentMeters = FMath::Min(
      ClampFiniteDouble(
          Inputs.DistrictMinHalfExtentMeters,
          DistrictMinHalfExtentDefaultMeters,
          DistrictHalfExtentMinMeters,
          DistrictHalfExtentMaxMeters),
      DefaultHalfExtentMeters);
  const double TilesetCoverageRatio = ClampFiniteDouble(
      Inputs.DistrictTilesetCoverageRatio,
      DistrictTilesetCoverageDefaultRatio,
      DistrictTilesetCoverageMinRatio,
      DistrictTilesetCoverageMaxRatio);
  const double SanitizedTilesetRadiusMeters = ClampFiniteDouble(
      TilesetRadiusMeters,
      0.0,
      0.0,
      TilesetRadiusMaxMeters);

  Config.HalfExtentMeters =
      SanitizedTilesetRadiusMeters > 0.0
          ? FMath::Clamp(
                SanitizedTilesetRadiusMeters * TilesetCoverageRatio,
                MinHalfExtentMeters,
                DefaultHalfExtentMeters)
          : DefaultHalfExtentMeters;
  Config.CellStepMeters = Config.HalfExtentMeters * 0.9;

  const double CellRadiusMaxMeters =
      FMath::Max(DistrictCellRadiusMinMeters, FMath::Min(DistrictCellRadiusMaxMeters, Config.HalfExtentMeters * 0.35));
  Config.CellRadiusMeters = ClampFiniteDouble(
      Inputs.DistrictCellRadiusMeters,
      DistrictCellRadiusDefaultMeters,
      DistrictCellRadiusMinMeters,
      CellRadiusMaxMeters);

  const double LauncherRingMinMeters = Config.CellRadiusMeters + 300.0;
  const double LauncherRingMaxMeters =
      FMath::Min(DistrictHalfExtentMaxMeters, FMath::Max(3000.0, Config.HalfExtentMeters + 2000.0));
  Config.LauncherRingDistanceMeters = ClampFiniteDouble(
      Inputs.LauncherRingDistanceMeters,
      DistrictLauncherRingDefaultMeters,
      LauncherRingMinMeters,
      LauncherRingMaxMeters);
  Config.LauncherLateralSpacingMeters = ClampFiniteDouble(
      Inputs.LauncherLateralSpacingMeters,
      DistrictLauncherLateralDefaultMeters,
      0.0,
      FMath::Min(DistrictLauncherLateralMaxMeters, Config.HalfExtentMeters));
  return Config;
}
