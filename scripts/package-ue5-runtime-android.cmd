@echo off
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0package-ue5-runtime-android.ps1" %*
endlocal
