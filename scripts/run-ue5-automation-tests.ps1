param(
    [string]$Project = "ue5/ProjectAirDefenseUE5/ProjectAirDefenseUE5.uproject",
    [string]$EditorCmd = "C:/Program Files/Epic Games/UE_5.7/Engine/Binaries/Win64/UnrealEditor-Cmd.exe",
    [string]$TestFilter = "ProjectAirDefense.BattleSimulation",
    [string]$LogPath = "benchmark-results/ue5-automation-tests.log",
    [switch]$SkipBattleMonteCarloSmoke,
    [int]$MonteCarloSmokeRuns = 5,
    [int]$MonteCarloSmokeWaves = 1,
    [double]$MonteCarloSmokeSeconds = 12.0,
    [double]$MonteCarloSmokeStep = 0.05,
    [int]$MonteCarloSmokeSeed = 20260417,
    [ValidateSet("Disciplined", "Adaptive", "ShieldWall")]
    [string]$MonteCarloSmokeDoctrine = "ShieldWall",
    [double]$MonteCarloSmokeEngagementRange = 2150.0,
    [ValidateSet("Auto", "DoctrineDefault", "Single", "Pair", "Ripple")]
    [string]$MonteCarloSmokeEngagementMode = "Auto",
    [ValidateSet("Balanced", "Ballistic", "BallisticFirst", "Glide", "GlideFirst", "Cruise", "CruiseFirst", "Impact", "ClosestImpact")]
    [string]$MonteCarloSmokeThreatPriority = "Balanced",
    [ValidateSet("Early", "Balanced", "Terminal")]
    [string]$MonteCarloSmokeFireControl = "Balanced",
    [string]$MonteCarloSmokeReportPath = "benchmark-results/ue5-automation-monte-carlo-smoke.json",
    [string]$MonteCarloSmokeLogPath = "benchmark-results/ue5-automation-monte-carlo-smoke.log"
)

$ErrorActionPreference = "Stop"

function Resolve-AbsolutePath([string]$PathValue) {
    if ([System.IO.Path]::IsPathRooted($PathValue)) {
        return $PathValue
    }
    return Join-Path (Get-Location) $PathValue
}

function Get-JsonPropertyValue([object]$JsonObject, [string]$PropertyName) {
    $property = $JsonObject.PSObject.Properties[$PropertyName]
    if ($null -eq $property) {
        throw "UE5 battle Monte Carlo report is missing required field '$PropertyName'."
    }
    return $property.Value
}

function Format-InvariantNumber([object]$Value) {
    return [System.Convert]::ToString($Value, [System.Globalization.CultureInfo]::InvariantCulture)
}

function Convert-ToRequiredDouble([object]$Value, [string]$FieldName) {
    if ($null -eq $Value) {
        throw "UE5 battle Monte Carlo report field '$FieldName' is null."
    }

    try {
        return [System.Convert]::ToDouble($Value, [System.Globalization.CultureInfo]::InvariantCulture)
    } catch {
        throw "UE5 battle Monte Carlo report field '$FieldName' is not numeric: $Value"
    }
}

function Assert-JsonStringNotBlank([object]$JsonObject, [string]$PropertyName) {
    $value = Get-JsonPropertyValue $JsonObject $PropertyName
    if ([string]::IsNullOrWhiteSpace([string]$value)) {
        throw "UE5 battle Monte Carlo report field '$PropertyName' is blank."
    }
    return [string]$value
}

function Assert-JsonIntegerEquals([object]$JsonObject, [string]$PropertyName, [int]$ExpectedValue) {
    $actual = Convert-ToRequiredDouble (Get-JsonPropertyValue $JsonObject $PropertyName) $PropertyName
    if ([Math]::Abs($actual - $ExpectedValue) -gt 0.000001) {
        throw "UE5 battle Monte Carlo report field '$PropertyName' was $actual, expected $ExpectedValue."
    }
}

function Assert-JsonDoubleApproximatelyEquals([object]$JsonObject, [string]$PropertyName, [double]$ExpectedValue, [double]$Tolerance = 0.000001) {
    $actual = Convert-ToRequiredDouble (Get-JsonPropertyValue $JsonObject $PropertyName) $PropertyName
    if ([Math]::Abs($actual - $ExpectedValue) -gt $Tolerance) {
        throw "UE5 battle Monte Carlo report field '$PropertyName' was $actual, expected $ExpectedValue."
    }
}

function Assert-JsonDoubleInRange([object]$JsonObject, [string]$PropertyName, [double]$MinimumValue, [double]$MaximumValue) {
    $actual = Convert-ToRequiredDouble (Get-JsonPropertyValue $JsonObject $PropertyName) $PropertyName
    if (($actual -lt $MinimumValue) -or ($actual -gt $MaximumValue)) {
        throw "UE5 battle Monte Carlo report field '$PropertyName' was $actual, expected $MinimumValue..$MaximumValue."
    }
    return $actual
}

function Assert-JsonStringNormalizedEquals([object]$JsonObject, [string]$PropertyName, [string]$ExpectedValue) {
    $actual = Assert-JsonStringNotBlank $JsonObject $PropertyName
    $normalizedActual = ($actual -replace "[^A-Za-z]", "").ToLowerInvariant()
    $normalizedExpected = ($ExpectedValue -replace "[^A-Za-z]", "").ToLowerInvariant()
    if ($normalizedActual -ne $normalizedExpected) {
        throw "UE5 battle Monte Carlo report field '$PropertyName' was '$actual', expected $ExpectedValue."
    }
}

function Assert-BattleMonteCarloReport(
    [string]$ReportFilePath,
    [int]$ExpectedRuns,
    [int]$ExpectedWaves,
    [double]$ExpectedSeconds,
    [double]$ExpectedStep,
    [int]$ExpectedSeed,
    [string]$ExpectedDoctrine,
    [double]$ExpectedEngagementRange,
    [string]$ExpectedEngagementMode,
    [string]$ExpectedThreatPriority,
    [string]$ExpectedFireControl
) {
    if (-not (Test-Path $ReportFilePath)) {
        throw "UE5 battle Monte Carlo did not create report: $ReportFilePath"
    }

    try {
        $report = Get-Content -Raw -LiteralPath $ReportFilePath | ConvertFrom-Json
    } catch {
        throw "UE5 battle Monte Carlo report is not valid JSON: $ReportFilePath. $($_.Exception.Message)"
    }

    Assert-JsonStringNotBlank $report "engine" | Out-Null
    $simulation = Assert-JsonStringNotBlank $report "simulation"
    if ($simulation -ne "FProjectAirDefenseBattleSimulation") {
        throw "UE5 battle Monte Carlo report used simulation '$simulation', expected FProjectAirDefenseBattleSimulation."
    }

    Assert-JsonIntegerEquals $report "runs" $ExpectedRuns
    Assert-JsonIntegerEquals $report "waves" $ExpectedWaves
    Assert-JsonIntegerEquals $report "seed" $ExpectedSeed
    Assert-JsonDoubleApproximatelyEquals $report "secondsPerWave" $ExpectedSeconds
    Assert-JsonDoubleApproximatelyEquals $report "stepSeconds" $ExpectedStep

    Assert-JsonStringNormalizedEquals $report "doctrine" $ExpectedDoctrine
    Assert-JsonDoubleApproximatelyEquals $report "engagementRangeMeters" $ExpectedEngagementRange 0.000001

    $expectedEngagementModeLabel = switch ($ExpectedEngagementMode) {
        { $_ -in @("Auto", "DoctrineDefault") } { "AUTO SALVO"; break }
        default { $ExpectedEngagementMode }
    }
    $expectedThreatPriorityLabel = switch ($ExpectedThreatPriority) {
        { $_ -in @("Ballistic", "BallisticFirst") } { "BALLISTIC"; break }
        { $_ -in @("Glide", "GlideFirst") } { "GLIDE"; break }
        { $_ -in @("Cruise", "CruiseFirst") } { "CRUISE"; break }
        { $_ -in @("Impact", "ClosestImpact") } { "IMPACT"; break }
        default { "BALANCED" }
    }
    Assert-JsonStringNormalizedEquals $report "engagementMode" $expectedEngagementModeLabel
    Assert-JsonStringNormalizedEquals $report "threatPriority" $expectedThreatPriorityLabel
    Assert-JsonStringNormalizedEquals $report "fireControl" $ExpectedFireControl

    Assert-JsonDoubleInRange $report "averageInterceptRate" 0.0 1.0 | Out-Null
    Assert-JsonDoubleInRange $report "averageCityIntegrity" 0.0 100.0 | Out-Null

    $totalThreatsSpawned = Convert-ToRequiredDouble (Get-JsonPropertyValue $report "totalThreatsSpawned") "totalThreatsSpawned"
    if ($totalThreatsSpawned -le 0) {
        throw "UE5 battle Monte Carlo report field 'totalThreatsSpawned' was $totalThreatsSpawned, expected a non-zero smoke run."
    }

    foreach ($fieldName in @(
        "gameOverRuns",
        "totalThreatsIntercepted",
        "totalHostileImpacts",
        "totalInterceptorsLaunched"
    )) {
        $value = Convert-ToRequiredDouble (Get-JsonPropertyValue $report $fieldName) $fieldName
        if ($value -lt 0) {
            throw "UE5 battle Monte Carlo report field '$fieldName' was $value, expected a non-negative value."
        }
    }

    $runsDetail = Get-JsonPropertyValue $report "runsDetail"
    $runsDetailCount = @($runsDetail).Count
    if ($runsDetailCount -ne $ExpectedRuns) {
        throw "UE5 battle Monte Carlo report field 'runsDetail' had $runsDetailCount entries, expected $ExpectedRuns."
    }
}

$projectPath = Resolve-AbsolutePath $Project
$editorCmdPath = Resolve-AbsolutePath $EditorCmd
$logFilePath = Resolve-AbsolutePath $LogPath
$monteCarloReportFilePath = Resolve-AbsolutePath $MonteCarloSmokeReportPath
$monteCarloLogFilePath = Resolve-AbsolutePath $MonteCarloSmokeLogPath

if (-not (Test-Path $projectPath)) {
    throw "Project file not found: $projectPath"
}
if (-not (Test-Path $editorCmdPath)) {
    throw "UnrealEditor-Cmd not found: $editorCmdPath"
}

New-Item -ItemType Directory -Force -Path ([System.IO.Path]::GetDirectoryName($logFilePath)) | Out-Null
if (Test-Path $logFilePath) {
    Remove-Item -LiteralPath $logFilePath -Force
}
if (-not $SkipBattleMonteCarloSmoke) {
    New-Item -ItemType Directory -Force -Path ([System.IO.Path]::GetDirectoryName($monteCarloReportFilePath)) | Out-Null
    New-Item -ItemType Directory -Force -Path ([System.IO.Path]::GetDirectoryName($monteCarloLogFilePath)) | Out-Null
    if (Test-Path $monteCarloReportFilePath) {
        Remove-Item -LiteralPath $monteCarloReportFilePath -Force
    }
    if (Test-Path $monteCarloLogFilePath) {
        Remove-Item -LiteralPath $monteCarloLogFilePath -Force
    }
}

$arguments = @(
    $projectPath,
    "-nosplash",
    "-unattended",
    "-nop4",
    "-nullrhi",
    "-stdout",
    "-ExecCmds=Automation RunTests $TestFilter; Quit",
    "-TestExit=Automation Test Queue Empty",
    "-abslog=$logFilePath"
)

& $editorCmdPath @arguments

if ($LASTEXITCODE -ne 0) {
    throw "UE automation tests failed. See log: $logFilePath"
}

Write-Output "UE automation tests passed: $logFilePath"

if ($SkipBattleMonteCarloSmoke) {
    Write-Output "UE5 battle Monte Carlo smoke skipped."
    return
}

$monteCarloArguments = @(
    $projectPath,
    "-nosplash",
    "-unattended",
    "-nop4",
    "-nullrhi",
    "-stdout",
    "-run=ProjectAirDefenseBattleMonteCarlo",
    "-Runs=$MonteCarloSmokeRuns",
    "-Waves=$MonteCarloSmokeWaves",
    "-Seconds=$(Format-InvariantNumber $MonteCarloSmokeSeconds)",
    "-Step=$(Format-InvariantNumber $MonteCarloSmokeStep)",
    "-Seed=$MonteCarloSmokeSeed",
    "-Doctrine=$MonteCarloSmokeDoctrine",
    "-EngagementRange=$(Format-InvariantNumber $MonteCarloSmokeEngagementRange)",
    "-EngagementMode=$MonteCarloSmokeEngagementMode",
    "-ThreatPriority=$MonteCarloSmokeThreatPriority",
    "-FireControl=$MonteCarloSmokeFireControl",
    "-Report=$monteCarloReportFilePath",
    "-abslog=$monteCarloLogFilePath"
)

& $editorCmdPath @monteCarloArguments

if ($LASTEXITCODE -ne 0) {
    throw "UE5 battle Monte Carlo smoke failed. See log: $monteCarloLogFilePath"
}

Assert-BattleMonteCarloReport `
    $monteCarloReportFilePath `
    $MonteCarloSmokeRuns `
    $MonteCarloSmokeWaves `
    $MonteCarloSmokeSeconds `
    $MonteCarloSmokeStep `
    $MonteCarloSmokeSeed `
    $MonteCarloSmokeDoctrine `
    $MonteCarloSmokeEngagementRange `
    $MonteCarloSmokeEngagementMode `
    $MonteCarloSmokeThreatPriority `
    $MonteCarloSmokeFireControl

Write-Output "UE5 battle Monte Carlo smoke passed: $monteCarloReportFilePath"
