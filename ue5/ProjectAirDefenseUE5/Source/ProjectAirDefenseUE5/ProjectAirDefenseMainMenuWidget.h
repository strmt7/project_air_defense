#pragma once

#include "CoreMinimal.h"
#include "Blueprint/UserWidget.h"
#include "ProjectAirDefenseMainMenuWidget.generated.h"

class AProjectAirDefensePlayerController;
class UButton;
class UTextBlock;

UCLASS()
class PROJECTAIRDEFENSEUE5_API UProjectAirDefenseMainMenuWidget : public UUserWidget {
  GENERATED_BODY()

public:
  void BindController(AProjectAirDefensePlayerController* InController);
  void RefreshGraphicsSummary();

protected:
  virtual void NativeOnInitialized() override;

private:
  UFUNCTION()
  void HandleStartPressed();

  UFUNCTION()
  void HandleGraphicsPressed();

  void BuildWidgetTree();
  AProjectAirDefensePlayerController* ResolveController() const;

  TWeakObjectPtr<AProjectAirDefensePlayerController> OwningAirDefenseController;

  UPROPERTY(Transient)
  TObjectPtr<UButton> StartButton;

  UPROPERTY(Transient)
  TObjectPtr<UButton> GraphicsButton;

  UPROPERTY(Transient)
  TObjectPtr<UTextBlock> GraphicsSummaryText;

  bool bTreeBuilt = false;
};
