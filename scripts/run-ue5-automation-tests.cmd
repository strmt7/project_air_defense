@echo off
setlocal
powershell -ExecutionPolicy Bypass -File "%~dp0run-ue5-automation-tests.ps1" %*
exit /b %errorlevel%
