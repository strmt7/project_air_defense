@echo off
set SOURCE=%1
set OUTPUT=%2
if "%SOURCE%"=="" set SOURCE=helsinki_kalasatama_mesh
if "%OUTPUT%"=="" set OUTPUT=data/ue5_city_pilot/helsinki_kalasatama/pilot_manifest.json
py -3 .\tools\ue5_city_pipeline\generate_import_manifest.py --source %SOURCE% --output %OUTPUT%
