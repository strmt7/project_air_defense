param(
    [string]$Source = "helsinki_kalasatama_3dtiles",
    [string]$Dest = "data/external",
    [switch]$DryRun,
    [switch]$Overwrite
)

$args = @(
    ".\tools\ue5_city_pipeline\download_city_source.py",
    "--source", $Source,
    "--dest", $Dest
)

if ($DryRun) { $args += "--dry-run" }
if ($Overwrite) { $args += "--overwrite" }

py -3 @args
