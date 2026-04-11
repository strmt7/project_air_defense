param(
    [int]$Runs = 300,
    [int]$Waves = 1,
    [float]$Seconds = 48,
    [float]$Step = 0.05,
    [long]$Seed = 20260411,
    [float]$EngagementRange = 2825,
    [float]$InterceptorSpeed = 700,
    [float]$LaunchCooldown = 0.3,
    [float]$BlastRadius = 82
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
Push-Location $repoRoot
try {
    .\gradlew.bat :core:runBattleMonteCarlo "-Pruns=$Runs" "-Pwaves=$Waves" "-Pseconds=$Seconds" "-Pstep=$Step" "-Pseed=$Seed" "-PengagementRange=$EngagementRange" "-PinterceptorSpeed=$InterceptorSpeed" "-PlaunchCooldown=$LaunchCooldown" "-PblastRadius=$BlastRadius" --no-daemon --console=plain
}
finally {
    Pop-Location
}
