param(
    [string]$Project = "ue5/ProjectAirDefenseUE5/ProjectAirDefenseUE5.uproject",
    [string]$RunUat = "C:/Program Files/Epic Games/UE_5.7/Engine/Build/BatchFiles/RunUAT.bat",
    [string]$ArchiveDir = "packaged/Win64",
    [ValidateSet("Development", "Shipping", "DebugGame")]
    [string]$Configuration = "Development",
    [string]$Platform = "Win64",
    [switch]$KeepExistingArchive,
    [switch]$KeepStagedBuilds
)

$ErrorActionPreference = "Stop"

function Resolve-AbsolutePath([string]$PathValue) {
    if ([System.IO.Path]::IsPathRooted($PathValue)) {
        return $PathValue
    }
    return Join-Path (Get-Location) $PathValue
}

function Get-DirectorySizeGiB([string]$PathValue) {
    if (-not (Test-Path -LiteralPath $PathValue)) {
        return 0.0
    }

    $sizeBytes = (Get-ChildItem -LiteralPath $PathValue -Recurse -Force -File -ErrorAction SilentlyContinue |
        Measure-Object -Property Length -Sum).Sum
    return [math]::Round(($sizeBytes / 1GB), 3)
}

$projectPath = Resolve-AbsolutePath $Project
$runUatPath = Resolve-AbsolutePath $RunUat
$archiveDirPath = Resolve-AbsolutePath $ArchiveDir
$projectRoot = Split-Path -Parent $projectPath
$savedRoot = Join-Path $projectRoot "Saved"
$stagedBuildsPath = Join-Path $savedRoot "StagedBuilds"

if (-not (Test-Path -LiteralPath $projectPath)) {
    throw "Project file not found: $projectPath"
}
if (-not (Test-Path -LiteralPath $runUatPath)) {
    throw "RunUAT.bat not found: $runUatPath"
}

if (Test-Path -LiteralPath $stagedBuildsPath) {
    Remove-Item -LiteralPath $stagedBuildsPath -Recurse -Force
}

if ((Test-Path -LiteralPath $archiveDirPath) -and -not $KeepExistingArchive) {
    Remove-Item -LiteralPath $archiveDirPath -Recurse -Force
}

New-Item -ItemType Directory -Force -Path $archiveDirPath | Out-Null

$arguments = @(
    "BuildCookRun",
    "-project=$projectPath",
    "-platform=$Platform",
    "-clientconfig=$Configuration",
    "-build",
    "-cook",
    "-stage",
    "-package",
    "-pak",
    "-archive",
    "-archivedirectory=$archiveDirPath",
    "-installed",
    "-nop4",
    "-utf8output"
)

& $runUatPath @arguments
if ($LASTEXITCODE -ne 0) {
    throw "BuildCookRun failed with exit code $LASTEXITCODE"
}

$removedStagedBuilds = $false
if ((Test-Path -LiteralPath $stagedBuildsPath) -and -not $KeepStagedBuilds) {
    Remove-Item -LiteralPath $stagedBuildsPath -Recurse -Force
    $removedStagedBuilds = $true
}

$archiveGiB = Get-DirectorySizeGiB $archiveDirPath
$stagedGiB = Get-DirectorySizeGiB $stagedBuildsPath

Write-Output "Packaged UE5 runtime to $archiveDirPath ($archiveGiB GiB)."
if ($removedStagedBuilds) {
    Write-Output "Removed duplicate staged build directory: $stagedBuildsPath"
}
elseif (Test-Path -LiteralPath $stagedBuildsPath) {
    Write-Output "Retained staged build directory: $stagedBuildsPath ($stagedGiB GiB)."
}
