#pragma once

#include "CoreMinimal.h"
#include "Blueprint/UserWidget.h"
#include "ProjectAirDefenseRadarWidget.generated.h"

class AProjectAirDefenseBattleManager;
class AProjectAirDefensePlayerController;

UCLASS()
class PROJECTAIRDEFENSEUE5_API UProjectAirDefenseRadarWidget : public UUserWidget {
  GENERATED_BODY()

public:
  void BindController(AProjectAirDefensePlayerController* InController);

protected:
  virtual void NativeOnInitialized() override;
  virtual int32 NativePaint(
      const FPaintArgs& Args,
      const FGeometry& AllottedGeometry,
      const FSlateRect& MyCullingRect,
      FSlateWindowElementList& OutDrawElements,
      int32 LayerId,
      const FWidgetStyle& InWidgetStyle,
      bool bParentEnabled) const override;

private:
  void BuildWidgetTree();
  AProjectAirDefenseBattleManager* ResolveBattleManager() const;

  TWeakObjectPtr<AProjectAirDefensePlayerController> OwningAirDefenseController;
  bool bTreeBuilt = false;
};
