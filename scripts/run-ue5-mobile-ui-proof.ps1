param(
    [string]$Exe = "packaged/Win64/ProjectAirDefenseUE5.exe",
    [string]$OutputDir = "benchmark-results",
    [int]$MenuDelaySeconds = 12,
    [int]$BattleDelaySeconds = 14,
    [int]$SystemsDelaySeconds = 12
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$captureScript = Join-Path $PSScriptRoot "capture-ue5-runtime-screenshot.ps1"
$resolvedExe = if ([System.IO.Path]::IsPathRooted($Exe)) {
    $Exe
} else {
    Join-Path $repoRoot $Exe
}
$resolvedOutputDir = if ([System.IO.Path]::IsPathRooted($OutputDir)) {
    $OutputDir
} else {
    Join-Path $repoRoot $OutputDir
}

New-Item -ItemType Directory -Force -Path $resolvedOutputDir | Out-Null

$captures = @(
    @{
        Name = "menu"
        DelaySeconds = $MenuDelaySeconds
        ExtraArgs = @()
    },
    @{
        Name = "battle"
        DelaySeconds = $BattleDelaySeconds
        ExtraArgs = @("-AutoStartBattle")
    },
    @{
        Name = "systems"
        DelaySeconds = $SystemsDelaySeconds
        ExtraArgs = @("-ShowSystemsMenu")
    }
)

foreach ($capture in $captures) {
    $name = $capture.Name
    $logPath = Join-Path $resolvedOutputDir "ue5-packaged-$name-proof.log"
    $screenshotPath = Join-Path $resolvedOutputDir "ue5-packaged-$name-proof.png"
    $arguments = @(
        "-ExecutionPolicy", "Bypass",
        "-File", $captureScript,
        "-Exe", $resolvedExe,
        "-LogPath", $logPath,
        "-ScreenshotPath", $screenshotPath,
        "-DelaySeconds", [string]$capture.DelaySeconds
    ) + $capture.ExtraArgs

    & powershell @arguments
}

Write-Output "Captured packaged UE5 mobile proof set under $resolvedOutputDir"
