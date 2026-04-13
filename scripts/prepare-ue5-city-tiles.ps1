param(
    [string]$Archive = "data/external/downloads/Helsinki3D_MESH_Kalasatama_2017_3D_Tiles_ZIP.zip",
    [string]$ExtractRoot = "data/external/helsinki_kalasatama_3dtiles",
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
