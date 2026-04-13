param(
    [string]$Archive = "data/external/downloads/Helsinki3D_MESH_Kalasatama_2017_3D_Tiles_ZIP.zip",
    [string]$ExtractRoot = "data/external/helsinki_kalasatama_3dtiles",
    [string]$RuntimeStageRoot = "ue5/ProjectAirDefenseUE5/ExternalData/helsinki_kalasatama_3dtiles",
    [ValidateSet("Junction", "Mirror")]
    [string]$RuntimeStageMode = "Junction",
    [switch]$SkipExtract,
    [switch]$Overwrite
)

$ErrorActionPreference = "Stop"

function Resolve-AbsolutePath([string]$PathValue) {
    if ([System.IO.Path]::IsPathRooted($PathValue)) {
        return $PathValue
    }
    return Join-Path (Get-Location) $PathValue
}

$archivePath = Resolve-Path $Archive
$extractRootPath = Resolve-AbsolutePath $ExtractRoot
$runtimeStageRootPath = Resolve-AbsolutePath $RuntimeStageRoot

if ((Test-Path $extractRootPath) -and $Overwrite) {
    Remove-Item -LiteralPath $extractRootPath -Recurse -Force
}

New-Item -ItemType Directory -Force -Path $extractRootPath | Out-Null

if (-not $SkipExtract -and -not $Overwrite) {
    $existingRootTile = Get-ChildItem -Path $extractRootPath -Recurse -Filter "Tile_*.json" -ErrorAction SilentlyContinue |
        Select-Object -First 1
    if ($null -ne $existingRootTile) {
        $SkipExtract = $true
    }
}

if (-not $SkipExtract) {
    Expand-Archive -LiteralPath $archivePath -DestinationPath $extractRootPath -Force
}

py -3 .\tools\ue5_city_pipeline\build_master_tileset.py `
    --root $extractRootPath `
    --output (Join-Path $extractRootPath "tileset.json")

if (Test-Path $runtimeStageRootPath) {
    Remove-Item -LiteralPath $runtimeStageRootPath -Recurse -Force
}

$runtimeStageParent = Split-Path -Parent $runtimeStageRootPath
New-Item -ItemType Directory -Force -Path $runtimeStageParent | Out-Null

if ($RuntimeStageMode -eq "Junction") {
    New-Item -ItemType Junction -Path $runtimeStageRootPath -Target $extractRootPath | Out-Null
}
else {
    New-Item -ItemType Directory -Force -Path $runtimeStageRootPath | Out-Null
    $robocopyLog = Join-Path (Resolve-AbsolutePath "benchmark-results") "ue5-runtime-stage-sync.log"
    $null = robocopy $extractRootPath $runtimeStageRootPath /MIR /NFL /NDL /NJH /NJS /NP /R:1 /W:1 /LOG:$robocopyLog
    $robocopyExit = $LASTEXITCODE
    if ($robocopyExit -gt 7) {
        throw "robocopy failed while syncing runtime tiles into project ExternalData (exit $robocopyExit)"
    }
}
