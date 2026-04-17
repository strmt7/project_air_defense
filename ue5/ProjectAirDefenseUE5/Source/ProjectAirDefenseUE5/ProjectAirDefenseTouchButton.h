#pragma once

#include "CoreMinimal.h"
#include "Components/Button.h"
#include "ProjectAirDefenseTouchButton.generated.h"

UCLASS()
class PROJECTAIRDEFENSEUE5_API UProjectAirDefenseTouchButton : public UButton {
  GENERATED_BODY()

public:
  explicit UProjectAirDefenseTouchButton(const FObjectInitializer& ObjectInitializer);
};
