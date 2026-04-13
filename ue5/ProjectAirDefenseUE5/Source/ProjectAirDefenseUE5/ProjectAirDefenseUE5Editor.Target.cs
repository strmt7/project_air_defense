using UnrealBuildTool;
using System.Collections.Generic;

public class ProjectAirDefenseUE5EditorTarget : TargetRules
{
    public ProjectAirDefenseUE5EditorTarget(TargetInfo Target) : base(Target)
    {
        Type = TargetType.Editor;
        DefaultBuildSettings = BuildSettingsVersion.V5;
        IncludeOrderVersion = EngineIncludeOrderVersion.Latest;
        ExtraModuleNames.Add("ProjectAirDefenseUE5");
    }
}
