@echo off
setlocal
powershell -ExecutionPolicy Bypass -File "%~dp0run-ue5-mobile-ui-proof.ps1" %*
