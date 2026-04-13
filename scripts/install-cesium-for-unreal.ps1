param(
    [string]$Version = "2.25.0",
    [string]$ArchiveRoot = "data/external/downloads",
    [string]$ProjectPluginRoot = "ue5/ProjectAirDefenseUE5/Plugins",
    [switch]$Overwrite
)

$ErrorActionPreference = "Stop"

function Resolve-AbsolutePath([string]$PathValue) {
    if ([System.IO.Path]::IsPathRooted($PathValue)) {
        return $PathValue
    }
    return Join-Path (Get-Location) $PathValue
}

$archiveRootPath = Resolve-AbsolutePath $ArchiveRoot
$pluginRootPath = Resolve-AbsolutePath $ProjectPluginRoot
$archiveName = "CesiumForUnreal-57-v$Version.zip"
$archivePath = Join-Path $archiveRootPath $archiveName
$sourceUrl = "https://github.com/CesiumGS/cesium-unreal/releases/download/v$Version/$archiveName"

New-Item -ItemType Directory -Force -Path $archiveRootPath | Out-Null
New-Item -ItemType Directory -Force -Path $pluginRootPath | Out-Null

if (-not (Test-Path $archivePath)) {
    Invoke-WebRequest -Uri $sourceUrl -OutFile $archivePath
}

$pluginPath = Join-Path $pluginRootPath "CesiumForUnreal"
if ((Test-Path $pluginPath) -and $Overwrite) {
    Remove-Item -LiteralPath $pluginPath -Recurse -Force
}
if (-not (Test-Path $pluginPath)) {
    Expand-Archive -LiteralPath $archivePath -DestinationPath $pluginRootPath -Force
}

py -3 .\tools\ue5_city_pipeline\patch_cesium_for_unreal.py --plugin-root $pluginPath
