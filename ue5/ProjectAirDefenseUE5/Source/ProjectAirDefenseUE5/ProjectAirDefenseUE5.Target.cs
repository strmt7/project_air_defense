using UnrealBuildTool;
using System.Collections.Generic;

public class ProjectAirDefenseUE5Target : TargetRules
{
    public ProjectAirDefenseUE5Target(TargetInfo Target) : base(Target)
    {
        Type = TargetType.Game;
        DefaultBuildSettings = BuildSettingsVersion.V6;
        IncludeOrderVersion = EngineIncludeOrderVersion.Latest;
        ExtraModuleNames.Add("ProjectAirDefenseUE5");
    }
}
