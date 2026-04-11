$ErrorActionPreference = "Stop"

$toolRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$requirementsPath = Join-Path $toolRoot "requirements.txt"
$visualQaPath = Join-Path $toolRoot "visual_qa.py"

Write-Host "Installing Python packages..."
py -3 -m pip install --disable-pip-version-check -r $requirementsPath

Write-Host "Installing Tesseract OCR..."
winget install --id tesseract-ocr.tesseract -e --accept-package-agreements --accept-source-agreements

Write-Host "Installing scrcpy..."
winget install --id Genymobile.scrcpy -e --accept-package-agreements --accept-source-agreements

Write-Host "Probing toolchain..."
py -3 $visualQaPath probe

Write-Host "Running self-test..."
py -3 $visualQaPath selftest
