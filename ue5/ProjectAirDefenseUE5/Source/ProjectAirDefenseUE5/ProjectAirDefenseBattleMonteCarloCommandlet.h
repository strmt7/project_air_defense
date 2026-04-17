#pragma once

#include "Commandlets/Commandlet.h"
#include "CoreMinimal.h"
#include "ProjectAirDefenseBattleMonteCarloCommandlet.generated.h"

UCLASS()
class PROJECTAIRDEFENSEUE5_API UProjectAirDefenseBattleMonteCarloCommandlet : public UCommandlet {
  GENERATED_BODY()

public:
  UProjectAirDefenseBattleMonteCarloCommandlet();

  virtual int32 Main(const FString& Params) override;
};
