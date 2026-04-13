#pragma once

#include "CoreMinimal.h"
#include "GameFramework/Pawn.h"
#include "ProjectAirDefenseCityCameraPawn.generated.h"

class UCameraComponent;
class UInputAction;
class UInputMappingContext;
struct FInputActionValue;

UCLASS()
class PROJECTAIRDEFENSEUE5_API AProjectAirDefenseCityCameraPawn : public APawn {
  GENERATED_BODY()

public:
  AProjectAirDefenseCityCameraPawn();

  virtual void BeginPlay() override;
  virtual void Tick(float DeltaSeconds) override;
  virtual void SetupPlayerInputComponent(UInputComponent* PlayerInputComponent) override;

  UFUNCTION(BlueprintCallable, Category = "Project Air Defense|Camera")
  void ResetCamera();

  UFUNCTION(BlueprintCallable, Category = "Project Air Defense|Camera")
  void SetFocusPoint(const FVector& NewFocusPoint);

private:
  void InitializeInputMappingContext();
  void UpdateCameraTransform();
  void MoveForward(const FInputActionValue& Value);
  void MoveRight(const FInputActionValue& Value);
  void RotateYaw(const FInputActionValue& Value);
  void ZoomCamera(const FInputActionValue& Value);
  void RaiseCamera(const FInputActionValue& Value);
  void ResetCameraAction(const FInputActionValue& Value);

  UPROPERTY(VisibleAnywhere, Category = "Components")
  USceneComponent* SceneRoot;

  UPROPERTY(VisibleAnywhere, Category = "Components")
  UCameraComponent* Camera;

  UPROPERTY(Transient)
  UInputMappingContext* InputMappingContext;

  UPROPERTY(Transient)
  UInputAction* MoveForwardAction;

  UPROPERTY(Transient)
  UInputAction* MoveRightAction;

  UPROPERTY(Transient)
  UInputAction* RotateYawAction;

  UPROPERTY(Transient)
  UInputAction* ZoomAction;

  UPROPERTY(Transient)
  UInputAction* RaiseCameraAction;

  UPROPERTY(Transient)
  UInputAction* ResetAction;

  UPROPERTY(EditAnywhere, Category = "Camera")
  FVector FocusPoint;

  UPROPERTY(EditAnywhere, Category = "Camera")
  float DistanceMeters;

  UPROPERTY(EditAnywhere, Category = "Camera")
  float MinDistanceMeters;

  UPROPERTY(EditAnywhere, Category = "Camera")
  float MaxDistanceMeters;

  UPROPERTY(EditAnywhere, Category = "Camera")
  float OrbitYawDegrees;

  UPROPERTY(EditAnywhere, Category = "Camera")
  float OrbitPitchDegrees;

  UPROPERTY(EditAnywhere, Category = "Camera")
  float MinPitchDegrees;

  UPROPERTY(EditAnywhere, Category = "Camera")
  float MaxPitchDegrees;

  UPROPERTY(EditAnywhere, Category = "Camera")
  float PanSpeedMetersPerSecond;

  UPROPERTY(EditAnywhere, Category = "Camera")
  float VerticalSpeedMetersPerSecond;

  UPROPERTY(EditAnywhere, Category = "Camera")
  float RotationSpeedDegreesPerSecond;

  UPROPERTY(EditAnywhere, Category = "Camera")
  float ZoomSpeedMetersPerSecond;
};
