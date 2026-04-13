param(
    [string]$Project = "ue5/ProjectAirDefenseUE5/ProjectAirDefenseUE5.uproject",
    [string]$EditorExe = "C:/Program Files/Epic Games/UE_5.7/Engine/Binaries/Win64/UnrealEditor.exe",
    [string]$LogPath = "benchmark-results/ue5-editor-runtime.log",
    [string]$ScreenshotPath = "benchmark-results/ue5-editor-runtime.png",
    [int]$Width = 1600,
    [int]$Height = 900,
    [int]$DelaySeconds = 25,
    [int]$WindowX = 40,
    [int]$WindowY = 40,
    [string[]]$SendKeys = @(),
    [string]$SendKeysCsv = "",
    [int]$KeyDelayMilliseconds = 500,
    [int]$PostKeyDelaySeconds = 3
)

$ErrorActionPreference = "Stop"

$captureScript = Join-Path (Split-Path -Parent $PSCommandPath) "capture-ue5-runtime-screenshot.ps1"
if (-not (Test-Path -LiteralPath $captureScript)) {
    throw "Capture script not found: $captureScript"
}

$captureArguments = @(
    "-ExecutionPolicy", "Bypass",
    "-File", $captureScript,
    "-Exe", $EditorExe,
    "-Project", $Project,
    "-ProcessName", "UnrealEditor",
    "-ExtraArgs", "-game",
    "-LogPath", $LogPath,
    "-ScreenshotPath", $ScreenshotPath,
    "-Width", "$Width",
    "-Height", "$Height",
    "-DelaySeconds", "$DelaySeconds",
    "-WindowX", "$WindowX",
    "-WindowY", "$WindowY",
    "-KeyDelayMilliseconds", "$KeyDelayMilliseconds",
    "-PostKeyDelaySeconds", "$PostKeyDelaySeconds"
)

if ($SendKeys.Count -gt 0) {
    $SendKeysCsv = ($SendKeys -join ",")
}
if (-not [string]::IsNullOrWhiteSpace($SendKeysCsv)) {
    $captureArguments += @("-SendKeysCsv", $SendKeysCsv)
}

& powershell @captureArguments

if ($LASTEXITCODE -ne 0) {
    throw "Editor runtime capture failed with exit code $LASTEXITCODE"
}
