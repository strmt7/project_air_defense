param(
    [int]$Runs = 300,
    [int]$Waves = 1,
    [float]$Seconds = 48,
    [float]$Step = 0.05,
    [long]$Seed = 20260411
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
Push-Location $repoRoot
try {
    .\gradlew.bat :core:runBattleMonteCarlo "-Pruns=$Runs" "-Pwaves=$Waves" "-Pseconds=$Seconds" "-Pstep=$Step" "-Pseed=$Seed" --no-daemon --console=plain
}
finally {
    Pop-Location
}
