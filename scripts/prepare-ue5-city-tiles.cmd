@echo off
set ARCHIVE=%1
set EXTRACTROOT=%2
if "%ARCHIVE%"=="" set ARCHIVE=data/external/downloads/Helsinki3D_MESH_Kalasatama_2017_3D_Tiles_ZIP.zip
if "%EXTRACTROOT%"=="" set EXTRACTROOT=data/external/helsinki_kalasatama_3dtiles
powershell -ExecutionPolicy Bypass -File .\scripts\prepare-ue5-city-tiles.ps1 -Archive "%ARCHIVE%" -ExtractRoot "%EXTRACTROOT%"
