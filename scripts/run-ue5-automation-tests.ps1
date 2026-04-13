param(
    [string]$Project = "ue5/ProjectAirDefenseUE5/ProjectAirDefenseUE5.uproject",
    [string]$EditorCmd = "C:/Program Files/Epic Games/UE_5.7/Engine/Binaries/Win64/UnrealEditor-Cmd.exe",
    [string]$TestFilter = "ProjectAirDefense.BattleSimulation",
    [string]$LogPath = "benchmark-results/ue5-automation-tests.log"
)

$ErrorActionPreference = "Stop"

function Resolve-AbsolutePath([string]$PathValue) {
    if ([System.IO.Path]::IsPathRooted($PathValue)) {
        return $PathValue
    }
    return Join-Path (Get-Location) $PathValue
}

$projectPath = Resolve-AbsolutePath $Project
$editorCmdPath = Resolve-AbsolutePath $EditorCmd
$logFilePath = Resolve-AbsolutePath $LogPath

if (-not (Test-Path $projectPath)) {
    throw "Project file not found: $projectPath"
}
if (-not (Test-Path $editorCmdPath)) {
    throw "UnrealEditor-Cmd not found: $editorCmdPath"
}

New-Item -ItemType Directory -Force -Path ([System.IO.Path]::GetDirectoryName($logFilePath)) | Out-Null
if (Test-Path $logFilePath) {
    Remove-Item -LiteralPath $logFilePath -Force
}

$arguments = @(
    $projectPath,
    "-nosplash",
    "-unattended",
    "-nop4",
    "-nullrhi",
    "-stdout",
    "-ExecCmds=Automation RunTests $TestFilter; Quit",
    "-TestExit=Automation Test Queue Empty",
    "-abslog=$logFilePath"
)

& $editorCmdPath @arguments

if ($LASTEXITCODE -ne 0) {
    throw "UE automation tests failed. See log: $logFilePath"
}

Write-Output "UE automation tests passed: $logFilePath"
