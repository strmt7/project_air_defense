#include "ProjectAirDefenseCityCameraPawn.h"

#include "Camera/CameraComponent.h"
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

namespace {
constexpr float DefaultDistanceMeters = 3000.0f;
constexpr float DefaultMinDistanceMeters = 350.0f;
constexpr float DefaultMaxDistanceMeters = 12000.0f;
constexpr float DefaultYawDegrees = 18.0f;
constexpr float DefaultPitchDegrees = -28.0f;
constexpr float DefaultMinPitchDegrees = -80.0f;
constexpr float DefaultMaxPitchDegrees = -8.0f;
constexpr float DefaultPanSpeed = 2200.0f;
constexpr float DefaultVerticalSpeed = 1500.0f;
constexpr float DefaultRotationSpeed = 75.0f;
constexpr float DefaultZoomSpeed = 2200.0f;
} // namespace

AProjectAirDefenseCityCameraPawn::AProjectAirDefenseCityCameraPawn() {
  PrimaryActorTick.bCanEverTick = true;
  AutoPossessPlayer = EAutoReceiveInput::Player0;

  this->SceneRoot = CreateDefaultSubobject<USceneComponent>(TEXT("SceneRoot"));
  this->SetRootComponent(this->SceneRoot);

  this->Camera = CreateDefaultSubobject<UCameraComponent>(TEXT("Camera"));
  this->Camera->SetupAttachment(this->SceneRoot);
  this->Camera->bUsePawnControlRotation = false;

  this->FocusPoint = FVector::ZeroVector;
  this->DistanceMeters = DefaultDistanceMeters;
  this->MinDistanceMeters = DefaultMinDistanceMeters;
  this->MaxDistanceMeters = DefaultMaxDistanceMeters;
  this->OrbitYawDegrees = DefaultYawDegrees;
  this->OrbitPitchDegrees = DefaultPitchDegrees;
  this->MinPitchDegrees = DefaultMinPitchDegrees;
  this->MaxPitchDegrees = DefaultMaxPitchDegrees;
  this->PanSpeedMetersPerSecond = DefaultPanSpeed;
  this->VerticalSpeedMetersPerSecond = DefaultVerticalSpeed;
  this->RotationSpeedDegreesPerSecond = DefaultRotationSpeed;
  this->ZoomSpeedMetersPerSecond = DefaultZoomSpeed;
}

void AProjectAirDefenseCityCameraPawn::BeginPlay() {
  Super::BeginPlay();

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
  this->FocusPoint = FVector::ZeroVector;
  this->DistanceMeters = DefaultDistanceMeters;
  this->OrbitYawDegrees = DefaultYawDegrees;
  this->OrbitPitchDegrees = DefaultPitchDegrees;
  this->UpdateCameraTransform();
}

void AProjectAirDefenseCityCameraPawn::SetFocusPoint(const FVector& NewFocusPoint) {
  this->FocusPoint = NewFocusPoint;
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
  this->ZoomAction = NewObject<UInputAction>(this, TEXT("ZoomAction"));
  this->RaiseCameraAction = NewObject<UInputAction>(this, TEXT("RaiseCameraAction"));
  this->ResetAction = NewObject<UInputAction>(this, TEXT("ResetAction"));

  this->MoveForwardAction->ValueType = EInputActionValueType::Axis1D;
  this->MoveRightAction->ValueType = EInputActionValueType::Axis1D;
  this->RotateYawAction->ValueType = EInputActionValueType::Axis1D;
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
  const FVector Offset = OrbitRotation.Vector() * -this->DistanceMeters;
  const FVector CameraLocation = this->FocusPoint + Offset;

  this->SetActorLocation(this->FocusPoint);
  this->Camera->SetWorldLocation(CameraLocation);
  this->Camera->SetWorldRotation((this->FocusPoint - CameraLocation).Rotation());
}

void AProjectAirDefenseCityCameraPawn::MoveForward(const FInputActionValue& Value) {
  const float AxisValue = Value.Get<float>();
  if (FMath::IsNearlyZero(AxisValue)) {
    return;
  }

  const FRotator YawRotation(0.0f, this->OrbitYawDegrees, 0.0f);
  const FVector Forward = FRotationMatrix(YawRotation).GetUnitAxis(EAxis::X);
  this->FocusPoint += Forward * AxisValue * this->PanSpeedMetersPerSecond * this->GetWorld()->GetDeltaSeconds();
}

void AProjectAirDefenseCityCameraPawn::MoveRight(const FInputActionValue& Value) {
  const float AxisValue = Value.Get<float>();
  if (FMath::IsNearlyZero(AxisValue)) {
    return;
  }

  const FRotator YawRotation(0.0f, this->OrbitYawDegrees, 0.0f);
  const FVector Right = FRotationMatrix(YawRotation).GetUnitAxis(EAxis::Y);
  this->FocusPoint += Right * AxisValue * this->PanSpeedMetersPerSecond * this->GetWorld()->GetDeltaSeconds();
}

void AProjectAirDefenseCityCameraPawn::RotateYaw(const FInputActionValue& Value) {
  const float AxisValue = Value.Get<float>();
  if (FMath::IsNearlyZero(AxisValue)) {
    return;
  }

  this->OrbitYawDegrees += AxisValue * this->RotationSpeedDegreesPerSecond * this->GetWorld()->GetDeltaSeconds();
}

void AProjectAirDefenseCityCameraPawn::ZoomCamera(const FInputActionValue& Value) {
  const float AxisValue = Value.Get<float>();
  if (FMath::IsNearlyZero(AxisValue)) {
    return;
  }

  this->DistanceMeters = FMath::Clamp(
      this->DistanceMeters - AxisValue * this->ZoomSpeedMetersPerSecond * this->GetWorld()->GetDeltaSeconds(),
      this->MinDistanceMeters,
      this->MaxDistanceMeters);
}

void AProjectAirDefenseCityCameraPawn::RaiseCamera(const FInputActionValue& Value) {
  const float AxisValue = Value.Get<float>();
  if (FMath::IsNearlyZero(AxisValue)) {
    return;
  }

  this->FocusPoint.Z += AxisValue * this->VerticalSpeedMetersPerSecond * this->GetWorld()->GetDeltaSeconds();
}

void AProjectAirDefenseCityCameraPawn::ResetCameraAction(const FInputActionValue& Value) {
  if (Value.Get<bool>()) {
    this->ResetCamera();
  }
}
