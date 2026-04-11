@echo off
setlocal
powershell -ExecutionPolicy Bypass -File "%~dp0run-benchmark-suite.ps1" %*
