#include "ProjectAirDefenseTouchButton.h"

UProjectAirDefenseTouchButton::UProjectAirDefenseTouchButton(const FObjectInitializer& ObjectInitializer)
    : Super(ObjectInitializer) {
  this->InitIsFocusable(false);
}
