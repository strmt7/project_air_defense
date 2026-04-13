using UnrealBuildTool;

public class ProjectAirDefenseUE5 : ModuleRules
{
    public ProjectAirDefenseUE5(ReadOnlyTargetRules Target) : base(Target)
    {
        PCHUsage = PCHUsageMode.UseExplicitOrSharedPCHs;

        PublicDependencyModuleNames.AddRange(
            new[]
            {
                "Core",
                "CoreUObject",
                "Engine",
                "InputCore",
                "EnhancedInput",
                "DeveloperSettings",
                "Json",
                "JsonUtilities",
                "CesiumRuntime",
                "UMG",
                "Slate",
                "SlateCore"
            });
    }
}
