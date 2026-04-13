@echo off
setlocal
set SOURCE=%1
if "%SOURCE%"=="" set SOURCE=helsinki_kalasatama_3dtiles
if "%SOURCE:~0,1%"=="-" set SOURCE=helsinki_kalasatama_3dtiles
py -3 .\tools\ue5_city_pipeline\download_city_source.py --source %SOURCE% --dest data/external --dry-run
