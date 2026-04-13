#include "ProjectAirDefenseCityCameraPawn.h"

#include "Camera/CameraComponent.h"
#include "ProjectAirDefenseRuntimeSettings.h"
#include "EnhancedInputComponent.h"
#include "EnhancedInputSubsystems.h"
#include "InputAction.h"
#include "InputActionValue.h"
#include "InputMappingContext.h"
#include "InputModifiers.h"
#include "Kismet/GameplayStatics.h"
#include "Components/SceneComponent.h"
#include "Engine/LocalPlayer.h"
#include "GameFramework/PlayerController.h"
#include "ProjectAirDefensePlayerController.h"

namespace {
constexpr float UnrealUnitsPerMeter = 100.0f;

const UProjectAirDefenseRuntimeSettings* GetRuntimeSettings() {
  return GetDefault<UProjectAirDefenseRuntimeSettings>();
}

float MetersToUnrealUnits(float ValueInMeters) {
  return ValueInMeters * UnrealUnitsPerMeter;
}
} // namespace

AProjectAirDefenseCityCameraPawn::AProjectAirDefenseCityCameraPawn() {
  PrimaryActorTick.bCanEverTick = true;
  AutoPossessPlayer = EAutoReceiveInput::Player0;

  this->SceneRoot = CreateDefaultSubobject<USceneComponent>(TEXT("SceneRoot"));
  this->SetRootComponent(this->SceneRoot);

  this->Camera = CreateDefaultSubobject<UCameraComponent>(TEXT("Camera"));
  this->Camera->SetupAttachment(this->SceneRoot);
  this->Camera->bUsePawnControlRotation = false;
  this->ApplyRuntimeDefaults();
}

void AProjectAirDefenseCityCameraPawn::ApplyRuntimeDefaults() {
  const UProjectAirDefenseRuntimeSettings* Settings = GetRuntimeSettings();
  if (Settings == nullptr) {
    return;
  }

  this->Camera->SetFieldOfView(Settings->CameraFieldOfViewDegrees);
  this->FocusPoint = FVector::ZeroVector;
  this->DistanceUnrealUnits = MetersToUnrealUnits(Settings->CameraDefaultDistanceMeters);
  this->MinDistanceUnrealUnits = MetersToUnrealUnits(Settings->CameraMinDistanceMeters);
  this->MaxDistanceUnrealUnits = MetersToUnrealUnits(Settings->CameraMaxDistanceMeters);
  this->OrbitYawDegrees = Settings->CameraDefaultYawDegrees;
  this->OrbitPitchDegrees = Settings->CameraDefaultPitchDegrees;
  this->MinPitchDegrees = Settings->CameraMinPitchDegrees;
  this->MaxPitchDegrees = Settings->CameraMaxPitchDegrees;
  this->PanSpeedUnrealUnitsPerSecond = MetersToUnrealUnits(Settings->CameraPanSpeedMetersPerSecond);
  this->VerticalSpeedUnrealUnitsPerSecond =
      MetersToUnrealUnits(Settings->CameraVerticalSpeedMetersPerSecond);
  this->RotationSpeedDegreesPerSecond = Settings->CameraRotationSpeedDegreesPerSecond;
  this->ZoomSpeedUnrealUnitsPerSecond = MetersToUnrealUnits(Settings->CameraZoomSpeedMetersPerSecond);
}

void AProjectAirDefenseCityCameraPawn::BeginPlay() {
  Super::BeginPlay();

  this->ApplyRuntimeDefaults();
  this->InitializeInputMappingContext();
  this->UpdateCameraTransform();
}

void AProjectAirDefenseCityCameraPawn::Tick(float DeltaSeconds) {
  Super::Tick(DeltaSeconds);
  static_cast<void>(DeltaSeconds);
  this->UpdateCameraTransform();
}

void AProjectAirDefenseCityCameraPawn::SetupPlayerInputComponent(UInputComponent* PlayerInputComponent) {
  Super::SetupPlayerInputComponent(PlayerInputComponent);

  UEnhancedInputComponent* EnhancedInputComponent = Cast<UEnhancedInputComponent>(PlayerInputComponent);
  if (EnhancedInputComponent == nullptr) {
    return;
  }

  if (this->MoveForwardAction != nullptr) {
    EnhancedInputComponent->BindAction(
        this->MoveForwardAction,
        ETriggerEvent::Triggered,
        this,
        &AProjectAirDefenseCityCameraPawn::MoveForward);
  }
  if (this->MoveRightAction != nullptr) {
    EnhancedInputComponent->BindAction(
        this->MoveRightAction,
        ETriggerEvent::Triggered,
        this,
        &AProjectAirDefenseCityCameraPawn::MoveRight);
  }
  if (this->RotateYawAction != nullptr) {
    EnhancedInputComponent->BindAction(
        this->RotateYawAction,
        ETriggerEvent::Triggered,
        this,
        &AProjectAirDefenseCityCameraPawn::RotateYaw);
  }
  if (this->RotatePitchAction != nullptr) {
    EnhancedInputComponent->BindAction(
        this->RotatePitchAction,
        ETriggerEvent::Triggered,
        this,
        &AProjectAirDefenseCityCameraPawn::RotatePitch);
  }
  if (this->ZoomAction != nullptr) {
    EnhancedInputComponent->BindAction(
        this->ZoomAction,
        ETriggerEvent::Triggered,
        this,
        &AProjectAirDefenseCityCameraPawn::ZoomCamera);
  }
  if (this->RaiseCameraAction != nullptr) {
    EnhancedInputComponent->BindAction(
        this->RaiseCameraAction,
        ETriggerEvent::Triggered,
        this,
        &AProjectAirDefenseCityCameraPawn::RaiseCamera);
  }
  if (this->ResetAction != nullptr) {
    EnhancedInputComponent->BindAction(
        this->ResetAction,
        ETriggerEvent::Triggered,
        this,
        &AProjectAirDefenseCityCameraPawn::ResetCameraAction);
  }
}

void AProjectAirDefenseCityCameraPawn::ResetCamera() {
  this->ApplyRuntimeDefaults();
  this->UpdateCameraTransform();
}

void AProjectAirDefenseCityCameraPawn::SetFocusPoint(const FVector& NewFocusPoint) {
  this->FocusPoint = NewFocusPoint;
  this->UpdateCameraTransform();
}

void AProjectAirDefenseCityCameraPawn::FrameFocusPoint(
    const FVector& NewFocusPoint,
    float SuggestedRadiusMeters) {
  const UProjectAirDefenseRuntimeSettings* Settings = GetRuntimeSettings();
  const float VerticalOffsetRatio = Settings != nullptr ? Settings->CameraFocusVerticalOffsetRatio : 0.1f;
  const float DistanceMultiplier = Settings != nullptr ? Settings->CameraFramedDistanceMultiplier : 1.9f;
  const float SuggestedRadiusUnrealUnits = MetersToUnrealUnits(SuggestedRadiusMeters);

  this->FocusPoint =
      NewFocusPoint - FVector(0.0f, 0.0f, SuggestedRadiusUnrealUnits * VerticalOffsetRatio);
  const float FramedDistance = FMath::Clamp(
      SuggestedRadiusUnrealUnits * DistanceMultiplier,
      this->MinDistanceUnrealUnits,
      this->MaxDistanceUnrealUnits);
  this->DistanceUnrealUnits = FMath::Max(this->DistanceUnrealUnits, FramedDistance);
  this->UpdateCameraTransform();
}

void AProjectAirDefenseCityCameraPawn::StepPan(float ForwardMeters, float RightMeters) {
  const FRotator YawRotation(0.0f, this->OrbitYawDegrees, 0.0f);
  const FVector Forward = FRotationMatrix(YawRotation).GetUnitAxis(EAxis::X);
  const FVector Right = FRotationMatrix(YawRotation).GetUnitAxis(EAxis::Y);
  this->FocusPoint +=
      Forward * MetersToUnrealUnits(ForwardMeters) + Right * MetersToUnrealUnits(RightMeters);
  this->UpdateCameraTransform();
}

void AProjectAirDefenseCityCameraPawn::StepYaw(float DeltaDegrees) {
  this->OrbitYawDegrees += DeltaDegrees;
  this->UpdateCameraTransform();
}

void AProjectAirDefenseCityCameraPawn::StepPitch(float DeltaDegrees) {
  this->OrbitPitchDegrees =
      FMath::Clamp(this->OrbitPitchDegrees + DeltaDegrees, this->MinPitchDegrees, this->MaxPitchDegrees);
  this->UpdateCameraTransform();
}

void AProjectAirDefenseCityCameraPawn::StepZoom(float DeltaMeters) {
  this->DistanceUnrealUnits = FMath::Clamp(
      this->DistanceUnrealUnits + MetersToUnrealUnits(DeltaMeters),
      this->MinDistanceUnrealUnits,
      this->MaxDistanceUnrealUnits);
  this->UpdateCameraTransform();
}

void AProjectAirDefenseCityCameraPawn::StepAltitude(float DeltaMeters) {
  this->FocusPoint.Z += MetersToUnrealUnits(DeltaMeters);
  this->UpdateCameraTransform();
}

void AProjectAirDefenseCityCameraPawn::InitializeInputMappingContext() {
  if (this->InputMappingContext != nullptr) {
    return;
  }

  APlayerController* PlayerController = Cast<APlayerController>(this->GetController());
  if (PlayerController == nullptr) {
    PlayerController = UGameplayStatics::GetPlayerController(this, 0);
  }
  if (PlayerController == nullptr) {
    return;
  }

  ULocalPlayer* LocalPlayer = PlayerController->GetLocalPlayer();
  if (LocalPlayer == nullptr) {
    return;
  }

  UEnhancedInputLocalPlayerSubsystem* InputSubsystem =
      LocalPlayer->GetSubsystem<UEnhancedInputLocalPlayerSubsystem>();
  if (InputSubsystem == nullptr) {
    return;
  }

  this->InputMappingContext = NewObject<UInputMappingContext>(this, TEXT("CityInspectionInputContext"));
  this->MoveForwardAction = NewObject<UInputAction>(this, TEXT("MoveForwardAction"));
  this->MoveRightAction = NewObject<UInputAction>(this, TEXT("MoveRightAction"));
  this->RotateYawAction = NewObject<UInputAction>(this, TEXT("RotateYawAction"));
  this->RotatePitchAction = NewObject<UInputAction>(this, TEXT("RotatePitchAction"));
  this->ZoomAction = NewObject<UInputAction>(this, TEXT("ZoomAction"));
  this->RaiseCameraAction = NewObject<UInputAction>(this, TEXT("RaiseCameraAction"));
  this->ResetAction = NewObject<UInputAction>(this, TEXT("ResetAction"));

  this->MoveForwardAction->ValueType = EInputActionValueType::Axis1D;
  this->MoveRightAction->ValueType = EInputActionValueType::Axis1D;
  this->RotateYawAction->ValueType = EInputActionValueType::Axis1D;
  this->RotatePitchAction->ValueType = EInputActionValueType::Axis1D;
  this->ZoomAction->ValueType = EInputActionValueType::Axis1D;
  this->RaiseCameraAction->ValueType = EInputActionValueType::Axis1D;
  this->ResetAction->ValueType = EInputActionValueType::Boolean;

  auto AddNegatedKey = [this](UInputAction* Action, const FKey& Key) {
    FEnhancedActionKeyMapping& Mapping = this->InputMappingContext->MapKey(Action, Key);
    UInputModifierNegate* Negate = NewObject<UInputModifierNegate>(this);
    Mapping.Modifiers.Add(Negate);
  };

  this->InputMappingContext->MapKey(this->MoveForwardAction, EKeys::W);
  this->InputMappingContext->MapKey(this->MoveForwardAction, EKeys::Up);
  AddNegatedKey(this->MoveForwardAction, EKeys::S);
  AddNegatedKey(this->MoveForwardAction, EKeys::Down);

  this->InputMappingContext->MapKey(this->MoveRightAction, EKeys::D);
  this->InputMappingContext->MapKey(this->MoveRightAction, EKeys::Right);
  AddNegatedKey(this->MoveRightAction, EKeys::A);
  AddNegatedKey(this->MoveRightAction, EKeys::Left);

  this->InputMappingContext->MapKey(this->RotateYawAction, EKeys::E);
  AddNegatedKey(this->RotateYawAction, EKeys::Q);

  this->InputMappingContext->MapKey(this->RotatePitchAction, EKeys::T);
  this->InputMappingContext->MapKey(this->RotatePitchAction, EKeys::NumPadEight);
  AddNegatedKey(this->RotatePitchAction, EKeys::G);
  AddNegatedKey(this->RotatePitchAction, EKeys::NumPadFive);

  this->InputMappingContext->MapKey(this->ZoomAction, EKeys::PageUp);
  this->InputMappingContext->MapKey(this->ZoomAction, EKeys::MouseScrollUp);
  AddNegatedKey(this->ZoomAction, EKeys::PageDown);
  AddNegatedKey(this->ZoomAction, EKeys::MouseScrollDown);

  this->InputMappingContext->MapKey(this->RaiseCameraAction, EKeys::R);
  AddNegatedKey(this->RaiseCameraAction, EKeys::F);

  this->InputMappingContext->MapKey(this->ResetAction, EKeys::Home);

  InputSubsystem->ClearAllMappings();
  InputSubsystem->AddMappingContext(this->InputMappingContext, 0);
}

void AProjectAirDefenseCityCameraPawn::UpdateCameraTransform() {
  const FRotator OrbitRotation(this->OrbitPitchDegrees, this->OrbitYawDegrees, 0.0f);
  const FVector Offset = OrbitRotation.Vector() * -this->DistanceUnrealUnits;
  const FVector CameraLocation = this->FocusPoint + Offset;

  this->SetActorLocation(this->FocusPoint);
  this->Camera->SetWorldLocation(CameraLocation);
  this->Camera->SetWorldRotation((this->FocusPoint - CameraLocation).Rotation());
}

bool AProjectAirDefenseCityCameraPawn::IsCameraControlBlocked() const {
  const AProjectAirDefensePlayerController* PlayerController =
      Cast<AProjectAirDefensePlayerController>(this->GetController());
  return PlayerController != nullptr && PlayerController->IsSystemsMenuVisible();
}

void AProjectAirDefenseCityCameraPawn::MoveForward(const FInputActionValue& Value) {
  if (this->IsCameraControlBlocked()) {
    return;
  }
  const float AxisValue = Value.Get<float>();
  if (FMath::IsNearlyZero(AxisValue)) {
    return;
  }

  const FRotator YawRotation(0.0f, this->OrbitYawDegrees, 0.0f);
  const FVector Forward = FRotationMatrix(YawRotation).GetUnitAxis(EAxis::X);
  this->FocusPoint +=
      Forward * AxisValue * this->PanSpeedUnrealUnitsPerSecond * this->GetWorld()->GetDeltaSeconds();
}

void AProjectAirDefenseCityCameraPawn::MoveRight(const FInputActionValue& Value) {
  if (this->IsCameraControlBlocked()) {
    return;
  }
  const float AxisValue = Value.Get<float>();
  if (FMath::IsNearlyZero(AxisValue)) {
    return;
  }

  const FRotator YawRotation(0.0f, this->OrbitYawDegrees, 0.0f);
  const FVector Right = FRotationMatrix(YawRotation).GetUnitAxis(EAxis::Y);
  this->FocusPoint +=
      Right * AxisValue * this->PanSpeedUnrealUnitsPerSecond * this->GetWorld()->GetDeltaSeconds();
}

void AProjectAirDefenseCityCameraPawn::RotateYaw(const FInputActionValue& Value) {
  if (this->IsCameraControlBlocked()) {
    return;
  }
  const float AxisValue = Value.Get<float>();
  if (FMath::IsNearlyZero(AxisValue)) {
    return;
  }

  this->OrbitYawDegrees += AxisValue * this->RotationSpeedDegreesPerSecond * this->GetWorld()->GetDeltaSeconds();
}

void AProjectAirDefenseCityCameraPawn::RotatePitch(const FInputActionValue& Value) {
  if (this->IsCameraControlBlocked()) {
    return;
  }
  const float AxisValue = Value.Get<float>();
  if (FMath::IsNearlyZero(AxisValue)) {
    return;
  }

  this->OrbitPitchDegrees = FMath::Clamp(
      this->OrbitPitchDegrees +
          AxisValue * this->RotationSpeedDegreesPerSecond * this->GetWorld()->GetDeltaSeconds(),
      this->MinPitchDegrees,
      this->MaxPitchDegrees);
}

void AProjectAirDefenseCityCameraPawn::ZoomCamera(const FInputActionValue& Value) {
  if (this->IsCameraControlBlocked()) {
    return;
  }
  const float AxisValue = Value.Get<float>();
  if (FMath::IsNearlyZero(AxisValue)) {
    return;
  }

  this->DistanceUnrealUnits = FMath::Clamp(
      this->DistanceUnrealUnits -
          AxisValue * this->ZoomSpeedUnrealUnitsPerSecond * this->GetWorld()->GetDeltaSeconds(),
      this->MinDistanceUnrealUnits,
      this->MaxDistanceUnrealUnits);
}

void AProjectAirDefenseCityCameraPawn::RaiseCamera(const FInputActionValue& Value) {
  if (this->IsCameraControlBlocked()) {
    return;
  }
  const float AxisValue = Value.Get<float>();
  if (FMath::IsNearlyZero(AxisValue)) {
    return;
  }

  this->FocusPoint.Z +=
      AxisValue * this->VerticalSpeedUnrealUnitsPerSecond * this->GetWorld()->GetDeltaSeconds();
}

void AProjectAirDefenseCityCameraPawn::ResetCameraAction(const FInputActionValue& Value) {
  if (Value.Get<bool>()) {
    this->ResetCamera();
  }
}
