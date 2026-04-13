#include "ProjectAirDefensePlayerController.h"

#include "EngineUtils.h"
#include "HighResScreenshot.h"
#include "InputCoreTypes.h"
#include "Kismet/KismetSystemLibrary.h"
#include "Misc/Paths.h"
#include "ProjectAirDefenseBattleManager.h"

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
  this->InputComponent->BindKey(EKeys::F8, IE_Pressed, this, &AProjectAirDefensePlayerController::HandleCycleShadowQuality);
  this->InputComponent->BindKey(EKeys::F9, IE_Pressed, this, &AProjectAirDefensePlayerController::HandleCycleReflectionQuality);
  this->InputComponent->BindKey(EKeys::F10, IE_Pressed, this, &AProjectAirDefensePlayerController::HandleCyclePostProcessingQuality);
  this->InputComponent->BindKey(EKeys::F11, IE_Pressed, this, &AProjectAirDefensePlayerController::HandleCaptureVerificationScreenshot);
  this->InputComponent->BindKey(EKeys::F12, IE_Pressed, this, &AProjectAirDefensePlayerController::HandleRequestGracefulQuit);
}

bool AProjectAirDefensePlayerController::IsSystemsMenuVisible() const {
  return this->bSystemsMenuVisible;
}

void AProjectAirDefensePlayerController::SetSystemsMenuVisible(bool bVisible) {
  this->bSystemsMenuVisible = bVisible;
  this->ApplyInteractionMode();
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

void AProjectAirDefensePlayerController::HandleToggleSystemsMenu() {
  this->SetSystemsMenuVisible(!this->bSystemsMenuVisible);
}

void AProjectAirDefensePlayerController::HandlePrimaryMenuAction() {
  if (this->bSystemsMenuVisible) {
    this->SetSystemsMenuVisible(false);
  }
  if (AProjectAirDefenseBattleManager* BattleManager = this->FindBattleManager()) {
    if (!BattleManager->BuildRuntimeSnapshot().bWaveInProgress) {
      BattleManager->StartNextWave();
    }
  }
}

void AProjectAirDefensePlayerController::HandleStartWave() {
  if (AProjectAirDefenseBattleManager* BattleManager = this->FindBattleManager()) {
    BattleManager->StartNextWave();
  }
}

void AProjectAirDefensePlayerController::HandleCycleDoctrine() {
  if (AProjectAirDefenseBattleManager* BattleManager = this->FindBattleManager()) {
    BattleManager->CycleDoctrine();
  }
}

void AProjectAirDefensePlayerController::HandleIncreaseQuality() {
  if (AProjectAirDefenseBattleManager* BattleManager = this->FindBattleManager()) {
    BattleManager->IncreaseOverallQuality();
  }
}

void AProjectAirDefensePlayerController::HandleDecreaseQuality() {
  if (AProjectAirDefenseBattleManager* BattleManager = this->FindBattleManager()) {
    BattleManager->DecreaseOverallQuality();
  }
}

void AProjectAirDefensePlayerController::HandleCycleAntiAliasing() {
  if (AProjectAirDefenseBattleManager* BattleManager = this->FindBattleManager()) {
    BattleManager->CycleAntiAliasingMethod();
  }
}

void AProjectAirDefensePlayerController::HandleToggleAmbientOcclusion() {
  if (AProjectAirDefenseBattleManager* BattleManager = this->FindBattleManager()) {
    BattleManager->ToggleAmbientOcclusion();
  }
}

void AProjectAirDefensePlayerController::HandleToggleMotionBlur() {
  if (AProjectAirDefenseBattleManager* BattleManager = this->FindBattleManager()) {
    BattleManager->ToggleMotionBlur();
  }
}

void AProjectAirDefensePlayerController::HandleCycleShadowQuality() {
  if (AProjectAirDefenseBattleManager* BattleManager = this->FindBattleManager()) {
    BattleManager->CycleShadowQuality();
  }
}

void AProjectAirDefensePlayerController::HandleCycleReflectionQuality() {
  if (AProjectAirDefenseBattleManager* BattleManager = this->FindBattleManager()) {
    BattleManager->CycleReflectionQuality();
  }
}

void AProjectAirDefensePlayerController::HandleCyclePostProcessingQuality() {
  if (AProjectAirDefenseBattleManager* BattleManager = this->FindBattleManager()) {
    BattleManager->CyclePostProcessingQuality();
  }
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
  if (this->bSystemsMenuVisible) {
    FInputModeGameAndUI InputMode;
    InputMode.SetHideCursorDuringCapture(false);
    InputMode.SetLockMouseToViewportBehavior(EMouseLockMode::DoNotLock);
    this->SetInputMode(InputMode);
    this->bShowMouseCursor = true;
    return;
  }

  FInputModeGameOnly InputMode;
  this->SetInputMode(InputMode);
  this->bShowMouseCursor = false;
}
