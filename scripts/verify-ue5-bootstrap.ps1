param(
    [string]$Project = "ue5/ProjectAirDefenseUE5/ProjectAirDefenseUE5.uproject",
    [string]$EditorCmd = "C:/Program Files/Epic Games/UE_5.7/Engine/Binaries/Win64/UnrealEditor-Cmd.exe",
    [string]$LogPath = "benchmark-results/ue5-headless-bootstrap.log",
    [int]$RunSeconds = 45,
    [string]$Map = ""
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

$arguments = @($projectPath)
if ($Map -ne "") {
    $arguments += $Map
}
$arguments += @(
    "-game",
    "-nosplash",
    "-unattended",
    "-nullrhi",
    "-stdout",
    "-abslog=$logFilePath"
)

$process = Start-Process -FilePath $editorCmdPath -ArgumentList $arguments -PassThru -WindowStyle Hidden
try {
    $finished = $process.WaitForExit($RunSeconds * 1000)
    if (-not $finished -and -not $process.HasExited) {
        Stop-Process -Id $process.Id -Force
        $process.WaitForExit()
    }
}
finally {
    if (-not $process.HasExited) {
        Stop-Process -Id $process.Id -Force
    }
}

if (-not (Test-Path $logFilePath)) {
    throw "Expected log file was not created: $logFilePath"
}

$logText = Get-Content $logFilePath -Raw
$requiredMarkers = @(
    "Game class is 'ProjectAirDefenseGameMode'",
    "[ProjectAirDefense] Bootstrapped pilot city scene",
    "tileset.json",
    "LogCesium: Loading tileset from URL file:///"
)

$missingMarkers = @()
foreach ($marker in $requiredMarkers) {
    if (-not $logText.Contains($marker)) {
        $missingMarkers += $marker
    }
}

if ($missingMarkers.Count -gt 0) {
    throw ("UE5 bootstrap verification failed. Missing log markers: " + ($missingMarkers -join "; "))
}

Write-Output "Verified UE5 bootstrap log: $logFilePath"
