param(
    [string]$Source = "helsinki_kalasatama_mesh",
    [string]$Output = "data/ue5_city_pilot/helsinki_kalasatama/pilot_manifest.json"
)

py -3 .\tools\ue5_city_pipeline\generate_import_manifest.py --source $Source --output $Output
