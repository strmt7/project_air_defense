@echo off
set SOURCE=%1
if "%SOURCE%"=="" set SOURCE=helsinki_kalasatama_mesh
py -3 .\tools\ue5_city_pipeline\download_city_source.py --source %SOURCE% --dest data/external --dry-run
