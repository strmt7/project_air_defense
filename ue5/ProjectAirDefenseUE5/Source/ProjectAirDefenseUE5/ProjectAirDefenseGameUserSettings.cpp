#include "ProjectAirDefenseGameUserSettings.h"

#include "HAL/IConsoleManager.h"

namespace {
constexpr int32 DefaultQualityLevel = 3;
constexpr int32 MinQualityLevel = 0;
constexpr int32 MaxQualityLevel = 4;
constexpr EConsoleVariableFlags GameSettingSetBy = ECVF_SetByGameOverride;

void SetConsoleVariableValue(const TCHAR* Name, int32 Value) {
  if (IConsoleVariable* ConsoleVariable = IConsoleManager::Get().FindConsoleVariable(Name)) {
    ConsoleVariable->Set(Value, GameSettingSetBy);
  }
}
} // namespace

UProjectAirDefenseGameUserSettings::UProjectAirDefenseGameUserSettings()
    : PreferredAntiAliasingMethod(EProjectAirDefenseAntiAliasingMethod::TSR),
      bAmbientOcclusionEnabled(true),
      bMotionBlurEnabled(false) {}

void UProjectAirDefenseGameUserSettings::SetToDefaults() {
  Super::SetToDefaults();
  this->PreferredAntiAliasingMethod = EProjectAirDefenseAntiAliasingMethod::TSR;
  this->bAmbientOcclusionEnabled = true;
  this->bMotionBlurEnabled = false;
  this->ApplyHighQualityDefaults();
}

void UProjectAirDefenseGameUserSettings::LoadSettings(bool bForceReload) {
  Super::LoadSettings(bForceReload);
  this->ClampProjectSpecificSettings();
  this->ApplyProjectSpecificCvars();
}

void UProjectAirDefenseGameUserSettings::ApplyNonResolutionSettings() {
  this->ClampProjectSpecificSettings();
  Super::ApplyNonResolutionSettings();
  this->ApplyProjectSpecificCvars();
  this->LogActiveGraphicsSettings();
}

void UProjectAirDefenseGameUserSettings::ValidateSettings() {
  Super::ValidateSettings();
  this->ClampProjectSpecificSettings();
}

void UProjectAirDefenseGameUserSettings::SetPreferredAntiAliasingMethod(
    EProjectAirDefenseAntiAliasingMethod InMethod) {
  this->PreferredAntiAliasingMethod = InMethod;
}

EProjectAirDefenseAntiAliasingMethod
UProjectAirDefenseGameUserSettings::GetPreferredAntiAliasingMethod() const {
  return this->PreferredAntiAliasingMethod;
}

void UProjectAirDefenseGameUserSettings::SetAmbientOcclusionEnabled(bool bEnabled) {
  this->bAmbientOcclusionEnabled = bEnabled;
}

bool UProjectAirDefenseGameUserSettings::IsAmbientOcclusionEnabled() const {
  return this->bAmbientOcclusionEnabled;
}

void UProjectAirDefenseGameUserSettings::SetMotionBlurEnabled(bool bEnabled) {
  this->bMotionBlurEnabled = bEnabled;
}

bool UProjectAirDefenseGameUserSettings::IsMotionBlurEnabled() const {
  return this->bMotionBlurEnabled;
}

void UProjectAirDefenseGameUserSettings::ApplyHighQualityDefaults() {
  this->SetOverallScalabilityLevel(DefaultQualityLevel);
  this->SetAntiAliasingQuality(DefaultQualityLevel);
  this->SetViewDistanceQuality(DefaultQualityLevel);
  this->SetShadowQuality(DefaultQualityLevel);
  this->SetGlobalIlluminationQuality(DefaultQualityLevel);
  this->SetReflectionQuality(DefaultQualityLevel);
  this->SetPostProcessingQuality(DefaultQualityLevel);
  this->SetTextureQuality(DefaultQualityLevel);
  this->SetFoliageQuality(DefaultQualityLevel);
  this->SetShadingQuality(DefaultQualityLevel);
  this->SetVSyncEnabled(false);
  this->SetDynamicResolutionEnabled(false);
}

UProjectAirDefenseGameUserSettings*
UProjectAirDefenseGameUserSettings::GetProjectAirDefenseGameUserSettings() {
  return Cast<UProjectAirDefenseGameUserSettings>(UGameUserSettings::GetGameUserSettings());
}

void UProjectAirDefenseGameUserSettings::ApplyProjectSpecificCvars() const {
  SetConsoleVariableValue(
      TEXT("r.AntiAliasingMethod"),
      static_cast<int32>(this->PreferredAntiAliasingMethod));
  SetConsoleVariableValue(
      TEXT("r.DefaultFeature.AmbientOcclusion"),
      this->bAmbientOcclusionEnabled ? 1 : 0);
  SetConsoleVariableValue(
      TEXT("r.DefaultFeature.MotionBlur"),
      this->bMotionBlurEnabled ? 1 : 0);
}

void UProjectAirDefenseGameUserSettings::ClampProjectSpecificSettings() {
  this->SetAntiAliasingQuality(FMath::Clamp(this->GetAntiAliasingQuality(), MinQualityLevel, MaxQualityLevel));
  this->SetViewDistanceQuality(FMath::Clamp(this->GetViewDistanceQuality(), MinQualityLevel, MaxQualityLevel));
  this->SetShadowQuality(FMath::Clamp(this->GetShadowQuality(), MinQualityLevel, MaxQualityLevel));
  this->SetGlobalIlluminationQuality(FMath::Clamp(this->GetGlobalIlluminationQuality(), MinQualityLevel, MaxQualityLevel));
  this->SetReflectionQuality(FMath::Clamp(this->GetReflectionQuality(), MinQualityLevel, MaxQualityLevel));
  this->SetPostProcessingQuality(FMath::Clamp(this->GetPostProcessingQuality(), MinQualityLevel, MaxQualityLevel));
  this->SetTextureQuality(FMath::Clamp(this->GetTextureQuality(), MinQualityLevel, MaxQualityLevel));
  this->SetFoliageQuality(FMath::Clamp(this->GetFoliageQuality(), MinQualityLevel, MaxQualityLevel));
  this->SetShadingQuality(FMath::Clamp(this->GetShadingQuality(), MinQualityLevel, MaxQualityLevel));
}

void UProjectAirDefenseGameUserSettings::LogActiveGraphicsSettings() const {
  UE_LOG(
      LogTemp,
      Log,
      TEXT("[ProjectAirDefense] Graphics settings applied: overall=%d aaMethod=%d aaQuality=%d gi=%d reflections=%d post=%d view=%d shadows=%d textures=%d foliage=%d shading=%d ao=%d motionBlur=%d"),
      this->GetOverallScalabilityLevel(),
      static_cast<int32>(this->PreferredAntiAliasingMethod),
      this->GetAntiAliasingQuality(),
      this->GetGlobalIlluminationQuality(),
      this->GetReflectionQuality(),
      this->GetPostProcessingQuality(),
      this->GetViewDistanceQuality(),
      this->GetShadowQuality(),
      this->GetTextureQuality(),
      this->GetFoliageQuality(),
      this->GetShadingQuality(),
      this->bAmbientOcclusionEnabled ? 1 : 0,
      this->bMotionBlurEnabled ? 1 : 0);
}
