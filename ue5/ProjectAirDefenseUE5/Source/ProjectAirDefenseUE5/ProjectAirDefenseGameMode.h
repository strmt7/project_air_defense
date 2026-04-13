#pragma once

#include "CoreMinimal.h"
#include "EngineUtils.h"
#include "GameFramework/GameModeBase.h"
#include "ProjectAirDefenseGameMode.generated.h"

class ACesium3DTileset;
class ACesiumGeoreference;
class ACesiumSunSky;

UCLASS()
class PROJECTAIRDEFENSEUE5_API AProjectAirDefenseGameMode : public AGameModeBase {
  GENERATED_BODY()

public:
  AProjectAirDefenseGameMode();

  virtual void BeginPlay() override;

private:
  void BootstrapPilotScene();
  FString ResolveRepoRelativePath(const FString& RelativePath) const;
  FString FindTilesetJson() const;
  FString BuildFileUri(const FString& AbsolutePath) const;

  template <typename TActorType>
  TActorType* FindExistingActor() const {
    UWorld* World = this->GetWorld();
    if (World == nullptr) {
      return nullptr;
    }
    for (TActorIterator<TActorType> It(World); It; ++It) {
      return *It;
    }
    return nullptr;
  }
};
