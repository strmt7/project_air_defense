@echo off
set VERSION=%1
if "%VERSION%"=="" set VERSION=2.25.0
powershell -ExecutionPolicy Bypass -File .\scripts\install-cesium-for-unreal.ps1 -Version "%VERSION%"
