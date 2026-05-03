#include "ProjectAirDefensePlayerController.h"

#include "Containers/Ticker.h"
#include "EngineUtils.h"
#include "HighResScreenshot.h"
#include "InputCoreTypes.h"
#include "Kismet/KismetSystemLibrary.h"
#include "Misc/Parse.h"
#include "Misc/Paths.h"
#include "ProjectAirDefenseBattleManager.h"
#include "ProjectAirDefenseBattleWidget.h"
#include "ProjectAirDefenseCityCameraPawn.h"
#include "ProjectAirDefenseGameMode.h"
#include "ProjectAirDefenseMainMenuWidget.h"
#include "ProjectAirDefenseRuntimeSettings.h"

namespace {
FString VerificationScreenshotPath() {
  return FPaths::ConvertRelativePathToFull(
      FPaths::Combine(
          FPaths::ProjectSavedDir(),
          TEXT("Screenshots"),
          TEXT("Windows"),
          TEXT("ProjectAirDefenseVerification.png")));
}
} // namespace

AProjectAirDefensePlayerController::AProjectAirDefensePlayerController() {
  this->bEnableClickEvents = true;
  this->bEnableTouchEvents = true;
  this->bEnableMouseOverEvents = false;
  this->bShowMouseCursor = false;
}

void AProjectAirDefensePlayerController::BeginPlay() {
  Super::BeginPlay();
  this->BuildWidgets();
  this->RefreshVisibleUi();
  this->ApplyVerificationLaunchFlags();
}

void AProjectAirDefensePlayerController::SetupInputComponent() {
  Super::SetupInputComponent();
  if (this->InputComponent == nullptr) {
    return;
  }

  this->InputComponent->BindKey(EKeys::Escape, IE_Pressed, this, &AProjectAirDefensePlayerController::HandleToggleSystemsMenu);
  this->InputComponent->BindKey(EKeys::Enter, IE_Pressed, this, &AProjectAirDefensePlayerController::HandlePrimaryMenuAction);
  this->InputComponent->BindKey(EKeys::SpaceBar, IE_Pressed, this, &AProjectAirDefensePlayerController::HandleStartWave);
  this->InputComponent->BindKey(EKeys::Tab, IE_Pressed, this, &AProjectAirDefensePlayerController::HandleCycleDoctrine);
  this->InputComponent->BindKey(EKeys::Equals, IE_Pressed, this, &AProjectAirDefensePlayerController::HandleIncreaseQuality);
  this->InputComponent->BindKey(EKeys::Hyphen, IE_Pressed, this, &AProjectAirDefensePlayerController::HandleDecreaseQuality);
  this->InputComponent->BindKey(EKeys::F5, IE_Pressed, this, &AProjectAirDefensePlayerController::HandleCycleAntiAliasing);
  this->InputComponent->BindKey(EKeys::F6, IE_Pressed, this, &AProjectAirDefensePlayerController::HandleToggleAmbientOcclusion);
  this->InputComponent->BindKey(EKeys::F7, IE_Pressed, this, &AProjectAirDefensePlayerController::HandleToggleMotionBlur);
  this->InputComponent->BindKey(EKeys::F4, IE_Pressed, this, &AProjectAirDefensePlayerController::HandleToggleRayTracing);
  this->InputComponent->BindKey(EKeys::F8, IE_Pressed, this, &AProjectAirDefensePlayerController::HandleCycleShadowQuality);
  this->InputComponent->BindKey(EKeys::F9, IE_Pressed, this, &AProjectAirDefensePlayerController::HandleCycleReflectionQuality);
  this->InputComponent->BindKey(EKeys::F10, IE_Pressed, this, &AProjectAirDefensePlayerController::HandleCyclePostProcessingQuality);
  this->InputComponent->BindKey(EKeys::LeftBracket, IE_Pressed, this, &AProjectAirDefensePlayerController::HandleTimeStepBack);
  this->InputComponent->BindKey(EKeys::RightBracket, IE_Pressed, this, &AProjectAirDefensePlayerController::HandleTimeStepForward);
  this->InputComponent->BindKey(EKeys::P, IE_Pressed, this, &AProjectAirDefensePlayerController::HandleToggleTimeCycle);
  this->InputComponent->BindKey(EKeys::F11, IE_Pressed, this, &AProjectAirDefensePlayerController::HandleCaptureVerificationScreenshot);
  this->InputComponent->BindKey(EKeys::F12, IE_Pressed, this, &AProjectAirDefensePlayerController::HandleRequestGracefulQuit);
}

AProjectAirDefenseBattleManager* AProjectAirDefensePlayerController::FindBattleManager() const {
  UWorld* World = this->GetWorld();
  if (World == nullptr) {
    return nullptr;
  }
  for (TActorIterator<AProjectAirDefenseBattleManager> It(World); It; ++It) {
    return *It;
  }
  return nullptr;
}

AProjectAirDefenseCityCameraPawn* AProjectAirDefensePlayerController::FindCameraPawn() const {
  return Cast<AProjectAirDefenseCityCameraPawn>(this->GetPawn());
}

bool AProjectAirDefensePlayerController::IsSystemsMenuVisible() const {
  return this->bSystemsMenuVisible;
}

bool AProjectAirDefensePlayerController::IsBattleStarted() const {
  return this->bBattleStarted;
}

void AProjectAirDefensePlayerController::SetSystemsMenuVisible(bool bVisible) {
  this->bSystemsMenuVisible = this->bBattleStarted && bVisible;
  this->RefreshVisibleUi();
}

void AProjectAirDefensePlayerController::StartBattleExperience(
    bool bStartWaveImmediately,
    bool bOpenSystemsImmediately) {
  this->bBattleStarted = true;
  this->bSystemsMenuVisible = bOpenSystemsImmediately;
  this->RefreshVisibleUi();

  if (bStartWaveImmediately) {
    this->RequestStartWave();
  }
}

void AProjectAirDefensePlayerController::RequestStartWave() {
  if (AProjectAirDefenseBattleManager* BattleManager = this->FindBattleManager()) {
    BattleManager->StartNextWave();
  }
}

void AProjectAirDefensePlayerController::RequestCycleDoctrine() {
  if (AProjectAirDefenseBattleManager* BattleManager = this->FindBattleManager()) {
    BattleManager->CycleDoctrine();
  }
}

void AProjectAirDefensePlayerController::RequestCycleEngagementMode() {
  if (AProjectAirDefenseBattleManager* BattleManager = this->FindBattleManager()) {
    BattleManager->CycleEngagementMode();
  }
}

void AProjectAirDefensePlayerController::RequestCycleThreatPriority() {
  if (AProjectAirDefenseBattleManager* BattleManager = this->FindBattleManager()) {
    BattleManager->CycleThreatPriority();
  }
}

void AProjectAirDefensePlayerController::RequestCycleFireControlMode() {
  if (AProjectAirDefenseBattleManager* BattleManager = this->FindBattleManager()) {
    BattleManager->CycleFireControlMode();
  }
}

void AProjectAirDefensePlayerController::RequestSetEngagementRangeMeters(
    double EngagementRangeMeters) {
  if (AProjectAirDefenseBattleManager* BattleManager = this->FindBattleManager()) {
    BattleManager->SetEngagementRangeMeters(EngagementRangeMeters);
  }
}

void AProjectAirDefensePlayerController::RequestIncreaseQuality() {
  if (AProjectAirDefenseBattleManager* BattleManager = this->FindBattleManager()) {
    BattleManager->IncreaseOverallQuality();
  }
}

void AProjectAirDefensePlayerController::RequestDecreaseQuality() {
  if (AProjectAirDefenseBattleManager* BattleManager = this->FindBattleManager()) {
    BattleManager->DecreaseOverallQuality();
  }
}

void AProjectAirDefensePlayerController::RequestSetOverallQualityLevel(int32 QualityLevel) {
  if (AProjectAirDefenseBattleManager* BattleManager = this->FindBattleManager()) {
    BattleManager->SetOverallQualityLevel(QualityLevel);
  }
}

void AProjectAirDefensePlayerController::RequestCycleAntiAliasing() {
  if (AProjectAirDefenseBattleManager* BattleManager = this->FindBattleManager()) {
    BattleManager->CycleAntiAliasingMethod();
  }
}

void AProjectAirDefensePlayerController::RequestSetAntiAliasingMethod(
    EProjectAirDefenseAntiAliasingMethod Method) {
  if (AProjectAirDefenseBattleManager* BattleManager = this->FindBattleManager()) {
    BattleManager->SetAntiAliasingMethod(Method);
  }
}

void AProjectAirDefensePlayerController::RequestToggleAmbientOcclusion() {
  if (AProjectAirDefenseBattleManager* BattleManager = this->FindBattleManager()) {
    BattleManager->ToggleAmbientOcclusion();
  }
}

void AProjectAirDefensePlayerController::RequestSetAmbientOcclusionEnabled(bool bEnabled) {
  if (AProjectAirDefenseBattleManager* BattleManager = this->FindBattleManager()) {
    BattleManager->SetAmbientOcclusionEnabled(bEnabled);
  }
}

void AProjectAirDefensePlayerController::RequestToggleMotionBlur() {
  if (AProjectAirDefenseBattleManager* BattleManager = this->FindBattleManager()) {
    BattleManager->ToggleMotionBlur();
  }
}

void AProjectAirDefensePlayerController::RequestSetMotionBlurEnabled(bool bEnabled) {
  if (AProjectAirDefenseBattleManager* BattleManager = this->FindBattleManager()) {
    BattleManager->SetMotionBlurEnabled(bEnabled);
  }
}

void AProjectAirDefensePlayerController::RequestToggleRayTracing() {
  if (AProjectAirDefenseBattleManager* BattleManager = this->FindBattleManager()) {
    BattleManager->ToggleRayTracing();
  }
}

void AProjectAirDefensePlayerController::RequestSetRayTracingEnabled(bool bEnabled) {
  if (AProjectAirDefenseBattleManager* BattleManager = this->FindBattleManager()) {
    BattleManager->SetRayTracingEnabled(bEnabled);
  }
}

void AProjectAirDefensePlayerController::RequestCycleShadowQuality() {
  if (AProjectAirDefenseBattleManager* BattleManager = this->FindBattleManager()) {
    BattleManager->CycleShadowQuality();
  }
}

void AProjectAirDefensePlayerController::RequestSetShadowQualityLevel(int32 QualityLevel) {
  if (AProjectAirDefenseBattleManager* BattleManager = this->FindBattleManager()) {
    BattleManager->SetShadowQualityLevel(QualityLevel);
  }
}

void AProjectAirDefensePlayerController::RequestCycleReflectionQuality() {
  if (AProjectAirDefenseBattleManager* BattleManager = this->FindBattleManager()) {
    BattleManager->CycleReflectionQuality();
  }
}

void AProjectAirDefensePlayerController::RequestSetReflectionQualityLevel(int32 QualityLevel) {
  if (AProjectAirDefenseBattleManager* BattleManager = this->FindBattleManager()) {
    BattleManager->SetReflectionQualityLevel(QualityLevel);
  }
}

void AProjectAirDefensePlayerController::RequestCyclePostProcessingQuality() {
  if (AProjectAirDefenseBattleManager* BattleManager = this->FindBattleManager()) {
    BattleManager->CyclePostProcessingQuality();
  }
}

void AProjectAirDefensePlayerController::RequestSetPostProcessingQualityLevel(int32 QualityLevel) {
  if (AProjectAirDefenseBattleManager* BattleManager = this->FindBattleManager()) {
    BattleManager->SetPostProcessingQualityLevel(QualityLevel);
  }
}

void AProjectAirDefensePlayerController::RequestCameraPan(float ForwardMeters, float RightMeters) {
  if (AProjectAirDefenseCityCameraPawn* CameraPawn = this->FindCameraPawn()) {
    CameraPawn->StepPan(ForwardMeters, RightMeters);
  }
}

void AProjectAirDefensePlayerController::RequestCameraYaw(float DeltaDegrees) {
  if (AProjectAirDefenseCityCameraPawn* CameraPawn = this->FindCameraPawn()) {
    CameraPawn->StepYaw(DeltaDegrees);
  }
}

void AProjectAirDefensePlayerController::RequestCameraPitch(float DeltaDegrees) {
  if (AProjectAirDefenseCityCameraPawn* CameraPawn = this->FindCameraPawn()) {
    CameraPawn->StepPitch(DeltaDegrees);
  }
}

void AProjectAirDefensePlayerController::RequestCameraZoom(float DeltaMeters) {
  if (AProjectAirDefenseCityCameraPawn* CameraPawn = this->FindCameraPawn()) {
    CameraPawn->StepZoom(DeltaMeters);
  }
}

void AProjectAirDefensePlayerController::RequestCameraAltitude(float DeltaMeters) {
  if (AProjectAirDefenseCityCameraPawn* CameraPawn = this->FindCameraPawn()) {
    CameraPawn->StepAltitude(DeltaMeters);
  }
}

void AProjectAirDefensePlayerController::RequestCameraReset() {
  if (AProjectAirDefenseCityCameraPawn* CameraPawn = this->FindCameraPawn()) {
    CameraPawn->ResetCamera();
  }
}

void AProjectAirDefensePlayerController::RequestTimeStepBack() {
  const UProjectAirDefenseRuntimeSettings* Settings = GetDefault<UProjectAirDefenseRuntimeSettings>();
  this->RequestTimeStep(-(Settings == nullptr ? 1.0 : Settings->TimeControlStepHours));
}

void AProjectAirDefensePlayerController::RequestTimeStepForward() {
  const UProjectAirDefenseRuntimeSettings* Settings = GetDefault<UProjectAirDefenseRuntimeSettings>();
  this->RequestTimeStep(Settings == nullptr ? 1.0 : Settings->TimeControlStepHours);
}

void AProjectAirDefensePlayerController::RequestTimeStep(double DeltaHours) {
  if (AProjectAirDefenseGameMode* GameMode = this->FindProjectGameMode()) {
    GameMode->AdjustSolarTime(DeltaHours);
  }
}

void AProjectAirDefensePlayerController::RequestSetSolarTime(double SolarTimeHours) {
  if (AProjectAirDefenseGameMode* GameMode = this->FindProjectGameMode()) {
    GameMode->SetSolarTime(SolarTimeHours);
  }
}

void AProjectAirDefensePlayerController::RequestSlowerTime() {
  if (AProjectAirDefenseGameMode* GameMode = this->FindProjectGameMode()) {
    GameMode->AdjustTimeScale(-this->GetTimeScaleStepHoursPerMinute());
  }
}

void AProjectAirDefensePlayerController::RequestFasterTime() {
  if (AProjectAirDefenseGameMode* GameMode = this->FindProjectGameMode()) {
    GameMode->AdjustTimeScale(this->GetTimeScaleStepHoursPerMinute());
  }
}

void AProjectAirDefensePlayerController::RequestSetTimeScale(double HoursPerMinute) {
  if (AProjectAirDefenseGameMode* GameMode = this->FindProjectGameMode()) {
    GameMode->SetTimeScale(HoursPerMinute);
  }
}

void AProjectAirDefensePlayerController::RequestToggleTimeCycle() {
  if (AProjectAirDefenseGameMode* GameMode = this->FindProjectGameMode()) {
    GameMode->ToggleTimeCycle();
  }
}

double AProjectAirDefensePlayerController::GetSolarTimeHours() const {
  if (const AProjectAirDefenseGameMode* GameMode = this->FindProjectGameMode()) {
    return GameMode->GetSolarTime();
  }
  return 0.0;
}

double AProjectAirDefensePlayerController::GetTimeScaleHoursPerMinute() const {
  if (const AProjectAirDefenseGameMode* GameMode = this->FindProjectGameMode()) {
    return GameMode->GetTimeScale();
  }
  return 0.0;
}

double AProjectAirDefensePlayerController::GetTimeScaleMaxHoursPerMinute() const {
  if (const AProjectAirDefenseGameMode* GameMode = this->FindProjectGameMode()) {
    return GameMode->GetTimeScaleMax();
  }
  return 12.0;
}

FString AProjectAirDefensePlayerController::BuildTimeSummaryText() const {
  if (const AProjectAirDefenseGameMode* GameMode = this->FindProjectGameMode()) {
    return GameMode->BuildTimeSummaryText();
  }
  return TEXT("TIME --:-- | PAUSED");
}

void AProjectAirDefensePlayerController::BuildWidgets() {
  if (!this->IsLocalController()) {
    return;
  }

  if (this->MainMenuWidget == nullptr) {
    this->MainMenuWidget =
        CreateWidget<UProjectAirDefenseMainMenuWidget>(this, UProjectAirDefenseMainMenuWidget::StaticClass());
    if (this->MainMenuWidget != nullptr) {
      this->MainMenuWidget->BindController(this);
      this->MainMenuWidget->AddToViewport(200);
    }
  }

  if (this->BattleWidget == nullptr) {
    this->BattleWidget =
        CreateWidget<UProjectAirDefenseBattleWidget>(this, UProjectAirDefenseBattleWidget::StaticClass());
    if (this->BattleWidget != nullptr) {
      this->BattleWidget->BindController(this);
      this->BattleWidget->AddToViewport(150);
    }
  }
}

void AProjectAirDefensePlayerController::RefreshVisibleUi() {
  if (this->MainMenuWidget != nullptr) {
    this->MainMenuWidget->SetVisibility(this->bBattleStarted ? ESlateVisibility::Collapsed : ESlateVisibility::Visible);
    this->MainMenuWidget->RefreshGraphicsSummary();
  }
  if (this->BattleWidget != nullptr) {
    this->BattleWidget->SetVisibility(this->bBattleStarted ? ESlateVisibility::Visible : ESlateVisibility::Collapsed);
    if (this->bBattleStarted) {
      this->BattleWidget->RefreshFromRuntime();
    }
  }
  this->ApplyInteractionMode();
}

void AProjectAirDefensePlayerController::ApplyVerificationLaunchFlags() {
  if (this->bVerificationFlagsApplied) {
    return;
  }
  this->bVerificationFlagsApplied = true;

  const bool bAutoStartBattle =
      FParse::Param(FCommandLine::Get(), TEXT("ProjectAirDefenseAutoStartBattle"));
  const bool bShowSystemsMenu =
      FParse::Param(FCommandLine::Get(), TEXT("ProjectAirDefenseShowSystemsMenu"));
  if (!bAutoStartBattle && !bShowSystemsMenu) {
    return;
  }

  FTSTicker::GetCoreTicker().AddTicker(
      FTickerDelegate::CreateWeakLambda(this, [this, bAutoStartBattle, bShowSystemsMenu](float) {
        this->StartBattleExperience(!bShowSystemsMenu && bAutoStartBattle, bShowSystemsMenu);
        return false;
      }),
      0.25f);
}

AProjectAirDefenseGameMode* AProjectAirDefensePlayerController::FindProjectGameMode() const {
  UWorld* World = this->GetWorld();
  return World == nullptr ? nullptr : World->GetAuthGameMode<AProjectAirDefenseGameMode>();
}

double AProjectAirDefensePlayerController::GetTimeScaleStepHoursPerMinute() const {
  const UProjectAirDefenseRuntimeSettings* Settings = GetDefault<UProjectAirDefenseRuntimeSettings>();
  return Settings == nullptr ? 1.0 : FMath::Max(Settings->TimeScaleStepHoursPerMinute, 0.25);
}

bool AProjectAirDefensePlayerController::StartBattleForShortcut(
    bool bStartWaveImmediately,
    bool bOpenSystemsImmediately) {
  if (this->bBattleStarted) {
    return false;
  }
  this->StartBattleExperience(bStartWaveImmediately, bOpenSystemsImmediately);
  return true;
}

void AProjectAirDefensePlayerController::HandleToggleSystemsMenu() {
  if (this->StartBattleForShortcut(false, true)) {
    return;
  }
  this->SetSystemsMenuVisible(!this->bSystemsMenuVisible);
}

void AProjectAirDefensePlayerController::HandlePrimaryMenuAction() {
  if (!this->bBattleStarted) {
    this->StartBattleExperience(true, false);
    return;
  }

  if (this->bSystemsMenuVisible) {
    this->SetSystemsMenuVisible(false);
    return;
  }

  if (AProjectAirDefenseBattleManager* BattleManager = this->FindBattleManager()) {
    if (!BattleManager->BuildRuntimeSnapshot().bWaveInProgress) {
      this->RequestStartWave();
    }
  }
}

void AProjectAirDefensePlayerController::HandleStartWave() {
  if (this->StartBattleForShortcut(true, false)) {
    return;
  }
  this->RequestStartWave();
}

void AProjectAirDefensePlayerController::HandleCycleDoctrine() {
  if (this->StartBattleForShortcut(false, true)) {
    return;
  }
  this->RequestCycleDoctrine();
}

void AProjectAirDefensePlayerController::HandleIncreaseQuality() {
  if (this->StartBattleForShortcut(false, true)) {
    return;
  }
  this->RequestIncreaseQuality();
}

void AProjectAirDefensePlayerController::HandleDecreaseQuality() {
  if (this->StartBattleForShortcut(false, true)) {
    return;
  }
  this->RequestDecreaseQuality();
}

void AProjectAirDefensePlayerController::HandleCycleAntiAliasing() {
  if (this->StartBattleForShortcut(false, true)) {
    return;
  }
  this->RequestCycleAntiAliasing();
}

void AProjectAirDefensePlayerController::HandleToggleAmbientOcclusion() {
  if (this->StartBattleForShortcut(false, true)) {
    return;
  }
  this->RequestToggleAmbientOcclusion();
}

void AProjectAirDefensePlayerController::HandleToggleMotionBlur() {
  if (this->StartBattleForShortcut(false, true)) {
    return;
  }
  this->RequestToggleMotionBlur();
}

void AProjectAirDefensePlayerController::HandleToggleRayTracing() {
  if (this->StartBattleForShortcut(false, true)) {
    return;
  }
  this->RequestToggleRayTracing();
}

void AProjectAirDefensePlayerController::HandleCycleShadowQuality() {
  if (this->StartBattleForShortcut(false, true)) {
    return;
  }
  this->RequestCycleShadowQuality();
}

void AProjectAirDefensePlayerController::HandleCycleReflectionQuality() {
  if (this->StartBattleForShortcut(false, true)) {
    return;
  }
  this->RequestCycleReflectionQuality();
}

void AProjectAirDefensePlayerController::HandleCyclePostProcessingQuality() {
  if (this->StartBattleForShortcut(false, true)) {
    return;
  }
  this->RequestCyclePostProcessingQuality();
}

void AProjectAirDefensePlayerController::HandleTimeStepBack() {
  if (this->StartBattleForShortcut(false, true)) {
    return;
  }
  this->RequestTimeStepBack();
}

void AProjectAirDefensePlayerController::HandleTimeStepForward() {
  if (this->StartBattleForShortcut(false, true)) {
    return;
  }
  this->RequestTimeStepForward();
}

void AProjectAirDefensePlayerController::HandleToggleTimeCycle() {
  if (this->StartBattleForShortcut(false, true)) {
    return;
  }
  this->RequestToggleTimeCycle();
}

void AProjectAirDefensePlayerController::HandleCaptureVerificationScreenshot() {
  FScreenshotRequest::RequestScreenshot(
      VerificationScreenshotPath(),
      true,
      false,
      false);
}

void AProjectAirDefensePlayerController::HandleRequestGracefulQuit() {
  UKismetSystemLibrary::QuitGame(
      this,
      this,
      EQuitPreference::Quit,
      false);
}

void AProjectAirDefensePlayerController::ApplyInteractionMode() {
  if (!this->bBattleStarted) {
    FInputModeUIOnly InputMode;
    InputMode.SetLockMouseToViewportBehavior(EMouseLockMode::DoNotLock);
    this->SetInputMode(InputMode);
    this->bShowMouseCursor = true;
    return;
  }

  FInputModeGameAndUI InputMode;
  InputMode.SetLockMouseToViewportBehavior(EMouseLockMode::DoNotLock);
  InputMode.SetHideCursorDuringCapture(!this->bSystemsMenuVisible);
  this->SetInputMode(InputMode);
  this->bShowMouseCursor = this->bSystemsMenuVisible;
}
