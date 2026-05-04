param(
    [string]$Exe = "ue5/ProjectAirDefenseUE5/Binaries/Win64/ProjectAirDefenseUE5.exe",
    [string]$Project = "",
    [string]$LogPath = "benchmark-results/ue5-runtime.log",
    [string]$ScreenshotPath = "benchmark-results/ue5-runtime.png",
    [string]$StatePath = "",
    [int]$Width = 1600,
    [int]$Height = 900,
    [int]$DelaySeconds = 35,
    [int]$WindowX = 40,
    [int]$WindowY = 40,
    [string]$ProcessName,
    [string]$ExtraArgs = "",
    [string[]]$SendKeys = @(),
    [string]$SendKeysCsv = "",
    [int]$KeyDelayMilliseconds = 500,
    [int]$PostKeyDelaySeconds = 3,
    [int]$PostScreenshotAliveSeconds = 3,
    [int]$MutexWaitSeconds = 180,
    [switch]$KeepAliveAfterScreenshot,
    [switch]$AutoStartBattle,
    [switch]$ShowSystemsMenu
)

$ErrorActionPreference = "Stop"

function Resolve-AbsolutePath([string]$PathValue) {
    if ([System.IO.Path]::IsPathRooted($PathValue)) {
        return $PathValue
    }
    return Join-Path (Get-Location) $PathValue
}

function ConvertTo-IsoTimestamp([datetime]$Value) {
    return $Value.ToUniversalTime().ToString("o", [System.Globalization.CultureInfo]::InvariantCulture)
}

function Resolve-SavedRoot([string]$ResolvedExePath, [string]$ResolvedProjectPath) {
    if (-not [string]::IsNullOrWhiteSpace($ResolvedProjectPath)) {
        return Join-Path (Split-Path -Parent $ResolvedProjectPath) "Saved"
    }

    $exeDirectory = Split-Path -Parent $ResolvedExePath
    $projectName = [System.IO.Path]::GetFileNameWithoutExtension($ResolvedExePath)
    return Join-Path (Join-Path $exeDirectory $projectName) "Saved"
}

function New-CaptureMutex([string]$ResolvedExePath) {
    $normalizedPath = [System.IO.Path]::GetFullPath($ResolvedExePath).ToLowerInvariant()
    $pathBytes = [System.Text.Encoding]::UTF8.GetBytes($normalizedPath)
    $sha256 = [System.Security.Cryptography.SHA256]::Create()
    try {
        $hash = $sha256.ComputeHash($pathBytes)
    }
    finally {
        $sha256.Dispose()
    }

    $hashText = ([System.BitConverter]::ToString($hash)).Replace("-", "")
    return New-Object System.Threading.Mutex($false, "Local\ProjectAirDefenseUE5RuntimeCapture_$hashText")
}

$exePath = Resolve-AbsolutePath $Exe
$projectPath = if ([string]::IsNullOrWhiteSpace($Project)) { "" } else { Resolve-AbsolutePath $Project }
$logFilePath = Resolve-AbsolutePath $LogPath
$screenshotFilePath = Resolve-AbsolutePath $ScreenshotPath
$stateFilePath = if ([string]::IsNullOrWhiteSpace($StatePath)) { "" } else { Resolve-AbsolutePath $StatePath }
$savedRoot = Resolve-SavedRoot $exePath $projectPath
$resolvedProcessName = if ([string]::IsNullOrWhiteSpace($ProcessName)) {
    [System.IO.Path]::GetFileNameWithoutExtension($exePath)
}
else {
    $ProcessName
}

if (-not (Test-Path $exePath)) {
    throw "Game executable not found: $exePath"
}
if (-not [string]::IsNullOrWhiteSpace($projectPath) -and -not (Test-Path $projectPath)) {
    throw "Project file not found: $projectPath"
}

New-Item -ItemType Directory -Force -Path ([System.IO.Path]::GetDirectoryName($logFilePath)) | Out-Null
New-Item -ItemType Directory -Force -Path ([System.IO.Path]::GetDirectoryName($screenshotFilePath)) | Out-Null
if (-not [string]::IsNullOrWhiteSpace($stateFilePath)) {
    New-Item -ItemType Directory -Force -Path ([System.IO.Path]::GetDirectoryName($stateFilePath)) | Out-Null
}

if (Test-Path $logFilePath) {
    Remove-Item -LiteralPath $logFilePath -Force
}
if (Test-Path $screenshotFilePath) {
    Remove-Item -LiteralPath $screenshotFilePath -Force
}
if (-not [string]::IsNullOrWhiteSpace($stateFilePath) -and (Test-Path $stateFilePath)) {
    Remove-Item -LiteralPath $stateFilePath -Force
}

if (-not [string]::IsNullOrWhiteSpace($SendKeysCsv)) {
    $SendKeys = $SendKeysCsv.Split(',') | ForEach-Object { $_.Trim() } | Where-Object { $_ -ne "" }
}

$showSystemsMenu = $ShowSystemsMenu.IsPresent
$autoStartBattle = $AutoStartBattle.IsPresent
foreach ($keyStroke in $SendKeys) {
    if ($keyStroke -eq "{ESC}") {
        $showSystemsMenu = $true
        $autoStartBattle = $false
    }
    elseif ($keyStroke -eq "{ENTER}" -or $keyStroke -eq "{SPACE}") {
        $autoStartBattle = $true
    }
}

$arguments = @()
if (-not [string]::IsNullOrWhiteSpace($projectPath)) {
    $arguments += $projectPath
}
$arguments += @(
    "-windowed",
    "-ResX=$Width",
    "-ResY=$Height",
    "-WinX=$WindowX",
    "-WinY=$WindowY",
    "-NoSplash",
    "-log",
    "-abslog=$logFilePath",
    "-ProjectAirDefenseVerificationPath=$screenshotFilePath",
    "-ProjectAirDefenseVerificationDelay=$DelaySeconds"
)
if (-not $KeepAliveAfterScreenshot) {
    $arguments += "-ProjectAirDefenseAutoQuitAfterVerification"
}
if ($showSystemsMenu) {
    $arguments += "-ProjectAirDefenseShowSystemsMenu"
}
elseif ($autoStartBattle) {
    $arguments += "-ProjectAirDefenseAutoStartBattle"
}
if (-not [string]::IsNullOrWhiteSpace($ExtraArgs)) {
    $arguments += $ExtraArgs
}

$captureMutex = New-CaptureMutex $exePath
$lockAcquired = $false
$process = $null
$captureState = [ordered]@{
    exe = $exePath
    project = if ([string]::IsNullOrWhiteSpace($projectPath)) { $null } else { $projectPath }
    log = $logFilePath
    screenshot = $screenshotFilePath
    processName = $resolvedProcessName
    processId = $null
    requestedMode = if ($showSystemsMenu) { "systems" } elseif ($autoStartBattle) { "battle" } else { "menu" }
    width = $Width
    height = $Height
    delaySeconds = $DelaySeconds
    keepAliveAfterScreenshot = $KeepAliveAfterScreenshot.IsPresent
    postScreenshotAliveSeconds = $PostScreenshotAliveSeconds
    startedAt = $null
    screenshotDetectedAt = $null
    completedAt = $null
    screenshotFound = $false
    processExitedBeforeScreenshot = $false
    processAliveAtScreenshot = $false
    processAliveAfterScreenshotWait = $false
    forcedShutdown = $false
    exitCode = $null
    success = $false
    failure = $null
}

try {
    $lockAcquired = $captureMutex.WaitOne([TimeSpan]::FromSeconds($MutexWaitSeconds))
    if (-not $lockAcquired) {
        throw "Timed out waiting for the UE5 runtime capture lock for $exePath. Another packaged/editor verification run is still active."
    }

    $process = Start-Process -FilePath $exePath -ArgumentList $arguments -PassThru
    $captureState.startedAt = ConvertTo-IsoTimestamp (Get-Date)
    $captureState.processId = $process.Id
    $processExited = $false
    $screenshotFound = $false

    $maxPollIterations = [Math]::Max(240, [int][Math]::Ceiling(($DelaySeconds + 45) * 2))
    for ($i = 0; $i -lt $maxPollIterations; $i++) {
        Start-Sleep -Milliseconds 500

        if (Test-Path $screenshotFilePath) {
            $file = Get-Item -LiteralPath $screenshotFilePath -ErrorAction SilentlyContinue
            if ($null -ne $file -and $file.Length -gt 0) {
                $screenshotFound = $true
                $captureState.screenshotFound = $true
                if ($null -eq $captureState.screenshotDetectedAt) {
                    $captureState.screenshotDetectedAt = ConvertTo-IsoTimestamp (Get-Date)
                }
            }
        }

        $process.Refresh()
        if ($process.HasExited) {
            $processExited = $true
            $captureState.exitCode = $process.ExitCode
            if ($screenshotFound) {
                break
            }
            $captureState.processExitedBeforeScreenshot = $true
            throw "Game process exited before writing the verification screenshot. Exit code: $($process.ExitCode)"
        }

        if ($screenshotFound) {
            $captureState.processAliveAtScreenshot = $true
            $postScreenshotPolls = [Math]::Max(1, $PostScreenshotAliveSeconds * 2)
            for ($j = 0; $j -lt $postScreenshotPolls; $j++) {
                Start-Sleep -Milliseconds 500
                $process.Refresh()
                if ($process.HasExited) {
                    $processExited = $true
                    $captureState.exitCode = $process.ExitCode
                    break
                }
            }
            break
        }
    }

    if (-not $screenshotFound) {
        throw "Timed out waiting for the verification screenshot: $screenshotFilePath"
    }

    if (-not $processExited) {
        $captureState.processAliveAfterScreenshotWait = $true
        Write-Warning "Game stayed alive after writing the verification screenshot; forcing process shutdown."
    }
    $captureState.success = $true
}
catch {
    $captureState.failure = $_.Exception.Message
    throw
}
finally {
    if ($process -ne $null -and -not $process.HasExited) {
        $captureState.forcedShutdown = $true
        Stop-Process -Id $process.Id -Force
    }
    if ($process -ne $null) {
        try {
            $process.Refresh()
            if ($process.HasExited -and $null -eq $captureState.exitCode) {
                $captureState.exitCode = $process.ExitCode
            }
        } catch {
            if ($null -eq $captureState.failure) {
                $captureState.failure = "Failed to refresh process state: $($_.Exception.Message)"
            }
        }
    }
    $captureState.completedAt = ConvertTo-IsoTimestamp (Get-Date)
    if (-not [string]::IsNullOrWhiteSpace($stateFilePath)) {
        $captureState | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $stateFilePath -Encoding UTF8
    }
    if ($lockAcquired -and $captureMutex -ne $null) {
        $captureMutex.ReleaseMutex() | Out-Null
    }
    if ($captureMutex -ne $null) {
        $captureMutex.Dispose()
    }
}

Write-Output "Captured UE5 runtime screenshot: $screenshotFilePath"
