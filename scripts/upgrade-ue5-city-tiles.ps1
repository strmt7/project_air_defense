param(
    [string]$SourceRoot = "data/external/helsinki_kalasatama_3dtiles",
    [string]$TempUpgradeRoot = "data/external/helsinki_kalasatama_3dtiles_upgrade_tmp",
    [string]$LegacyBackupRoot = "data/external/helsinki_kalasatama_3dtiles_legacy_backup",
    [string]$TargetVersion = "1.0",
    [switch]$KeepLegacyBackup
)

$ErrorActionPreference = "Stop"

function Resolve-AbsolutePath([string]$PathValue) {
    if ([System.IO.Path]::IsPathRooted($PathValue)) {
        return $PathValue
    }
    return Join-Path (Get-Location) $PathValue
}

function Resolve-NpxCommandPath() {
    $preferredPath = "C:\Program Files\nodejs\npx.cmd"
    if (Test-Path -LiteralPath $preferredPath) {
        return $preferredPath
    }

    $whereResult = & where.exe npx.cmd 2>$null
    if ($LASTEXITCODE -eq 0 -and -not [string]::IsNullOrWhiteSpace($whereResult)) {
        return ($whereResult -split "`r?`n" | Select-Object -First 1).Trim()
    }

    throw "npx.cmd is not available. Install Node.js LTS so Cesium's 3d-tiles-tools can run."
}

function Add-NodeInstallToPath([string]$NpxPath) {
    $nodeDirectory = Split-Path -Parent $NpxPath
    if ([string]::IsNullOrWhiteSpace($nodeDirectory)) {
        return
    }
    if (-not ($env:Path -split ';' | Where-Object { $_ -eq $nodeDirectory })) {
        $env:Path = "$nodeDirectory;$env:Path"
    }
}

function Assert-CommandExists([string]$CommandName, [string]$InstallHint) {
    if ($null -eq (Get-Command $CommandName -ErrorAction SilentlyContinue)) {
        throw "$CommandName is not available. $InstallHint"
    }
}

function Ensure-TilesetJson([string]$RootPath) {
    $tilesetPath = Join-Path $RootPath "tileset.json"
    if (-not (Test-Path -LiteralPath $tilesetPath)) {
        py -3 .\tools\ue5_city_pipeline\build_master_tileset.py --root $RootPath --output $tilesetPath
    }
    return $tilesetPath
}

function Assert-UpgradedSampleGlbVersion([string]$UpgradedRootPath) {
    $sampleB3dm = Get-ChildItem -LiteralPath $UpgradedRootPath -Filter "*.b3dm" -File -Recurse |
        Select-Object -First 1 -ExpandProperty FullName
    if ([string]::IsNullOrWhiteSpace($sampleB3dm)) {
        throw "No upgraded b3dm files were produced under $UpgradedRootPath"
    }

    $scratchRoot = Join-Path (Resolve-AbsolutePath "scratch") "upgrade-validation"
    New-Item -ItemType Directory -Force -Path $scratchRoot | Out-Null
    $sampleGlb = Join-Path $scratchRoot "sample.glb"
    if (Test-Path -LiteralPath $sampleGlb) {
        Remove-Item -LiteralPath $sampleGlb -Force
    }

    try {
        $npxPath = Resolve-NpxCommandPath
        Add-NodeInstallToPath $npxPath
        & $npxPath --yes 3d-tiles-tools b3dmToGlb -i $sampleB3dm -o $sampleGlb | Out-Null

        $glbVersion = @'
import struct
import sys
path = sys.argv[1]
with open(path, "rb") as handle:
    magic, version, _ = struct.unpack("<4sII", handle.read(12))
print(magic.decode("ascii"), version)
'@ | py -3 - $sampleGlb

        if ($LASTEXITCODE -ne 0) {
            throw "Failed to inspect upgraded sample GLB version"
        }
        if ($glbVersion.Trim() -notmatch "^glTF 2$") {
            throw "Upgraded sample did not contain a glTF 2 payload: $glbVersion"
        }
    }
    finally {
        if (Test-Path -LiteralPath $scratchRoot) {
            Remove-Item -LiteralPath $scratchRoot -Recurse -Force
        }
    }
}

Assert-CommandExists "py" "Install Python 3 and ensure 'py' is on PATH."
$npxCommandPath = Resolve-NpxCommandPath
Add-NodeInstallToPath $npxCommandPath

$sourceRootPath = Resolve-AbsolutePath $SourceRoot
$tempUpgradeRootPath = Resolve-AbsolutePath $TempUpgradeRoot
$legacyBackupRootPath = Resolve-AbsolutePath $LegacyBackupRoot

if (-not (Test-Path -LiteralPath $sourceRootPath)) {
    throw "Source tiles root not found: $sourceRootPath"
}

$null = Ensure-TilesetJson $sourceRootPath

if (Test-Path -LiteralPath $tempUpgradeRootPath) {
    Remove-Item -LiteralPath $tempUpgradeRootPath -Recurse -Force
}
if (Test-Path -LiteralPath $legacyBackupRootPath) {
    Remove-Item -LiteralPath $legacyBackupRootPath -Recurse -Force
}

& $npxCommandPath --yes 3d-tiles-tools upgrade `
    -i $sourceRootPath `
    -o $tempUpgradeRootPath `
    --targetVersion $TargetVersion `
    --force

$upgradedTileset = Join-Path $tempUpgradeRootPath "tileset.json"
if (-not (Test-Path -LiteralPath $upgradedTileset)) {
    throw "Upgrade completed without producing $upgradedTileset"
}

Assert-UpgradedSampleGlbVersion $tempUpgradeRootPath

Move-Item -LiteralPath $sourceRootPath -Destination $legacyBackupRootPath
try {
    Move-Item -LiteralPath $tempUpgradeRootPath -Destination $sourceRootPath
}
catch {
    if (Test-Path -LiteralPath $sourceRootPath) {
        Remove-Item -LiteralPath $sourceRootPath -Recurse -Force
    }
    Move-Item -LiteralPath $legacyBackupRootPath -Destination $sourceRootPath
    throw
}

if (-not $KeepLegacyBackup) {
    Remove-Item -LiteralPath $legacyBackupRootPath -Recurse -Force
}

$sourceBytes = @'
from pathlib import Path
import sys
root = Path(sys.argv[1])
total = sum(path.stat().st_size for path in root.rglob("*") if path.is_file())
print(f"{total / 1024 / 1024 / 1024:.3f}")
'@ | py -3 - $sourceRootPath

if ($LASTEXITCODE -ne 0) {
    throw "Failed to measure upgraded dataset size"
}

Write-Output "Upgraded UE5 city tiles in place at $sourceRootPath ($($sourceBytes.Trim()) GiB)."
if ($KeepLegacyBackup) {
    Write-Output "Legacy backup retained at $legacyBackupRootPath"
}
