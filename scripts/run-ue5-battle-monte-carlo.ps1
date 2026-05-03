param(
    [string]$Project = "ue5/ProjectAirDefenseUE5/ProjectAirDefenseUE5.uproject",
    [string]$EditorCmd = "C:/Program Files/Epic Games/UE_5.7/Engine/Binaries/Win64/UnrealEditor-Cmd.exe",
    [int]$Runs = 300,
    [int]$Waves = 1,
    [double]$Seconds = 48.0,
    [double]$Step = 0.05,
    [int]$Seed = 20260411,
    [ValidateSet("Disciplined", "Adaptive", "ShieldWall")]
    [string]$Doctrine = "ShieldWall",
    [double]$EngagementRange = 2150.0,
    [ValidateSet("Auto", "DoctrineDefault", "Single", "Pair", "Ripple")]
    [string]$EngagementMode = "Auto",
    [ValidateSet("Balanced", "Ballistic", "BallisticFirst", "Glide", "GlideFirst", "Cruise", "CruiseFirst", "Impact", "ClosestImpact")]
    [string]$ThreatPriority = "Balanced",
    [ValidateSet("Early", "Balanced", "Terminal")]
    [string]$FireControl = "Balanced",
    [string]$ReportPath = "benchmark-results/ue5-battle-monte-carlo.json",
    [string]$LogPath = "benchmark-results/ue5-battle-monte-carlo.log"
)

$ErrorActionPreference = "Stop"

function Resolve-AbsolutePath([string]$PathValue) {
    if ([System.IO.Path]::IsPathRooted($PathValue)) {
        return $PathValue
    }
    return Join-Path (Get-Location) $PathValue
}

function Format-InvariantNumber([object]$Value) {
    return [System.Convert]::ToString($Value, [System.Globalization.CultureInfo]::InvariantCulture)
}

$projectPath = Resolve-AbsolutePath $Project
$editorCmdPath = Resolve-AbsolutePath $EditorCmd
$reportFilePath = Resolve-AbsolutePath $ReportPath
$logFilePath = Resolve-AbsolutePath $LogPath

if (-not (Test-Path $projectPath)) {
    throw "Project file not found: $projectPath"
}
if (-not (Test-Path $editorCmdPath)) {
    throw "UnrealEditor-Cmd not found: $editorCmdPath"
}

New-Item -ItemType Directory -Force -Path ([System.IO.Path]::GetDirectoryName($reportFilePath)) | Out-Null
New-Item -ItemType Directory -Force -Path ([System.IO.Path]::GetDirectoryName($logFilePath)) | Out-Null
if (Test-Path $reportFilePath) {
    Remove-Item -LiteralPath $reportFilePath -Force
}
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
    "-run=ProjectAirDefenseBattleMonteCarlo",
    "-Runs=$Runs",
    "-Waves=$Waves",
    "-Seconds=$(Format-InvariantNumber $Seconds)",
    "-Step=$(Format-InvariantNumber $Step)",
    "-Seed=$Seed",
    "-Doctrine=$Doctrine",
    "-EngagementRange=$(Format-InvariantNumber $EngagementRange)",
    "-EngagementMode=$EngagementMode",
    "-ThreatPriority=$ThreatPriority",
    "-FireControl=$FireControl",
    "-Report=$reportFilePath",
    "-abslog=$logFilePath"
)

& $editorCmdPath @arguments

if ($LASTEXITCODE -ne 0) {
    throw "UE5 battle Monte Carlo failed. See log: $logFilePath"
}
if (-not (Test-Path $reportFilePath)) {
    throw "UE5 battle Monte Carlo did not create report: $reportFilePath"
}

Write-Output "UE5 battle Monte Carlo passed: $reportFilePath"
