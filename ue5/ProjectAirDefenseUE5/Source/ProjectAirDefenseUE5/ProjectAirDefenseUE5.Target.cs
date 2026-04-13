using UnrealBuildTool;
using System.Collections.Generic;

public class ProjectAirDefenseUE5Target : TargetRules
{
    public ProjectAirDefenseUE5Target(TargetInfo Target) : base(Target)
    {
        Type = TargetType.Game;
        bOverrideBuildEnvironment = true;
        DefaultBuildSettings = BuildSettingsVersion.V6;
        IncludeOrderVersion = EngineIncludeOrderVersion.Latest;
        if (Platform == UnrealTargetPlatform.Win64)
        {
            GlobalDefinitions.Add("NOMINMAX=1");
            GlobalDefinitions.Add("WIN32_LEAN_AND_MEAN=1");
        }
        ExtraModuleNames.Add("ProjectAirDefenseUE5");
    }
}
