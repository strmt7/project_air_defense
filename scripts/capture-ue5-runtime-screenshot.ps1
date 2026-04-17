param(
    [string]$Exe = "ue5/ProjectAirDefenseUE5/Binaries/Win64/ProjectAirDefenseUE5.exe",
    [string]$Project = "",
    [string]$LogPath = "benchmark-results/ue5-runtime.log",
    [string]$ScreenshotPath = "benchmark-results/ue5-runtime.png",
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
    [int]$MutexWaitSeconds = 180,
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

if (Test-Path $logFilePath) {
    Remove-Item -LiteralPath $logFilePath -Force
}
if (Test-Path $screenshotFilePath) {
    Remove-Item -LiteralPath $screenshotFilePath -Force
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
    "-ProjectAirDefenseVerificationDelay=$DelaySeconds",
    "-ProjectAirDefenseAutoQuitAfterVerification"
)
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

try {
    $lockAcquired = $captureMutex.WaitOne([TimeSpan]::FromSeconds($MutexWaitSeconds))
    if (-not $lockAcquired) {
        throw "Timed out waiting for the UE5 runtime capture lock for $exePath. Another packaged/editor verification run is still active."
    }

    $process = Start-Process -FilePath $exePath -ArgumentList $arguments -PassThru
    $processExited = $false
    $screenshotFound = $false

    $maxPollIterations = [Math]::Max(240, [int][Math]::Ceiling(($DelaySeconds + 45) * 2))
    for ($i = 0; $i -lt $maxPollIterations; $i++) {
        Start-Sleep -Milliseconds 500

        if (Test-Path $screenshotFilePath) {
            $file = Get-Item -LiteralPath $screenshotFilePath -ErrorAction SilentlyContinue
            if ($null -ne $file -and $file.Length -gt 0) {
                $screenshotFound = $true
            }
        }

        $process.Refresh()
        if ($process.HasExited) {
            $processExited = $true
            if ($screenshotFound) {
                break
            }
            throw "Game process exited before writing the verification screenshot. Exit code: $($process.ExitCode)"
        }

        if ($screenshotFound) {
            for ($j = 0; $j -lt 40; $j++) {
                Start-Sleep -Milliseconds 500
                $process.Refresh()
                if ($process.HasExited) {
                    $processExited = $true
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
        Write-Warning "Game stayed alive after writing the verification screenshot; forcing process shutdown."
    }
}
finally {
    if ($process -ne $null -and -not $process.HasExited) {
        Stop-Process -Id $process.Id -Force
    }
    if ($lockAcquired -and $captureMutex -ne $null) {
        $captureMutex.ReleaseMutex() | Out-Null
    }
    if ($captureMutex -ne $null) {
        $captureMutex.Dispose()
    }
}

Write-Output "Captured UE5 runtime screenshot: $screenshotFilePath"
