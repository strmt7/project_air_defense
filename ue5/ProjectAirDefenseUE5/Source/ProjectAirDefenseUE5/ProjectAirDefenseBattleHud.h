#pragma once

#include "CoreMinimal.h"
#include "GameFramework/HUD.h"
#include "ProjectAirDefenseBattleHud.generated.h"

class AProjectAirDefenseBattleManager;

UCLASS()
class PROJECTAIRDEFENSEUE5_API AProjectAirDefenseBattleHud : public AHUD {
  GENERATED_BODY()

public:
  virtual void DrawHUD() override;
  virtual void NotifyHitBoxClick(FName BoxName) override;

private:
  AProjectAirDefenseBattleManager* FindBattleManager() const;
  class AProjectAirDefensePlayerController* FindPlayerController() const;
  class AProjectAirDefenseCityCameraPawn* FindCameraPawn() const;
  void HandleTouchAction(FName BoxName);
  void DrawCard(
      const FVector2D& Position,
      const FVector2D& Size,
      const FLinearColor& FillColor,
      const FLinearColor& AccentColor) const;
  void DrawActionButton(
      FName BoxName,
      const FVector2D& Position,
      const FVector2D& Size,
      const FString& Label,
      const FString& SecondaryLabel,
      const FLinearColor& FillColor,
      const FLinearColor& AccentColor) const;
  void DrawDivider(float X, float Y, float Width, const FLinearColor& Color) const;
  void DrawPanelText(
      const FString& Text,
      float X,
      float Y,
      const FLinearColor& Color,
      float Scale = 1.0f) const;
};
