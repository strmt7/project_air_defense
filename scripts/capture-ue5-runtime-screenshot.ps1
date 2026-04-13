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
    [int]$PostKeyDelaySeconds = 3
)

$ErrorActionPreference = "Stop"

function Resolve-AbsolutePath([string]$PathValue) {
    if ([System.IO.Path]::IsPathRooted($PathValue)) {
        return $PathValue
    }
    return Join-Path (Get-Location) $PathValue
}

Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.Windows.Forms
Add-Type @"
using System;
using System.Runtime.InteropServices;
public static class Win32WindowCapture {
    [StructLayout(LayoutKind.Sequential)]
    public struct RECT {
        public int Left;
        public int Top;
        public int Right;
        public int Bottom;
    }

    [DllImport("user32.dll")]
    public static extern bool GetWindowRect(IntPtr hWnd, out RECT rect);

    [DllImport("user32.dll")]
    public static extern bool SetForegroundWindow(IntPtr hWnd);

    [DllImport("user32.dll")]
    public static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);
}
"@

$exePath = Resolve-AbsolutePath $Exe
$projectPath = if ([string]::IsNullOrWhiteSpace($Project)) { "" } else { Resolve-AbsolutePath $Project }
$logFilePath = Resolve-AbsolutePath $LogPath
$screenshotFilePath = Resolve-AbsolutePath $ScreenshotPath
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
    "-abslog=$logFilePath"
)
if (-not [string]::IsNullOrWhiteSpace($ExtraArgs)) {
    $arguments += $ExtraArgs
}

$process = Start-Process -FilePath $exePath -ArgumentList $arguments -PassThru

try {
    $windowHandle = [IntPtr]::Zero
    $windowProcess = $process
    for ($i = 0; $i -lt 120; $i++) {
        Start-Sleep -Milliseconds 500
        $process.Refresh()
        $candidateProcess = Get-Process -Name $resolvedProcessName -ErrorAction SilentlyContinue |
            Where-Object { $_.MainWindowHandle -ne 0 } |
            Sort-Object StartTime -Descending |
            Select-Object -First 1

        if ($null -ne $candidateProcess) {
            $windowProcess = $candidateProcess
            $windowHandle = [IntPtr]$candidateProcess.MainWindowHandle
            break
        }

        if ($process.HasExited -and $null -eq $candidateProcess) {
            throw "Game process exited early with code $($process.ExitCode)"
        }
    }

    if ($windowHandle -eq [IntPtr]::Zero) {
        throw "Timed out waiting for the game window"
    }

    [Win32WindowCapture]::ShowWindow($windowHandle, 9) | Out-Null
    [Win32WindowCapture]::SetForegroundWindow($windowHandle) | Out-Null

    Start-Sleep -Seconds $DelaySeconds
    foreach ($keyStroke in $SendKeys) {
        if (-not [string]::IsNullOrWhiteSpace($keyStroke)) {
            [System.Windows.Forms.SendKeys]::SendWait($keyStroke)
            Start-Sleep -Milliseconds $KeyDelayMilliseconds
        }
    }
    if ($SendKeys.Count -gt 0 -and $PostKeyDelaySeconds -gt 0) {
        Start-Sleep -Seconds $PostKeyDelaySeconds
    }

    $rect = New-Object Win32WindowCapture+RECT
    $usePrimaryScreenFallback = -not [Win32WindowCapture]::GetWindowRect($windowHandle, [ref]$rect)
    if (-not $usePrimaryScreenFallback) {
        $captureWidth = $rect.Right - $rect.Left
        $captureHeight = $rect.Bottom - $rect.Top
        $usePrimaryScreenFallback = ($captureWidth -le 0 -or $captureHeight -le 0)
    }

    if ($usePrimaryScreenFallback) {
        $screenBounds = [System.Windows.Forms.Screen]::PrimaryScreen.Bounds
        $captureLeft = $screenBounds.Left
        $captureTop = $screenBounds.Top
        $captureWidth = $screenBounds.Width
        $captureHeight = $screenBounds.Height
    }
    else {
        $captureLeft = $rect.Left
        $captureTop = $rect.Top
    }

    $bitmap = New-Object System.Drawing.Bitmap $captureWidth, $captureHeight
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    try {
        $graphics.CopyFromScreen($captureLeft, $captureTop, 0, 0, $bitmap.Size)
    }
    finally {
        $graphics.Dispose()
    }

    $bitmap.Save($screenshotFilePath, [System.Drawing.Imaging.ImageFormat]::Png)
    $bitmap.Dispose()
}
finally {
    $allProcesses = @($process)
    if ($null -ne $windowProcess -and $windowProcess.Id -ne $process.Id) {
        $allProcesses += $windowProcess
    }
    foreach ($ownedProcess in $allProcesses | Sort-Object Id -Unique) {
        if ($ownedProcess -ne $null -and -not $ownedProcess.HasExited) {
            Stop-Process -Id $ownedProcess.Id -Force
        }
    }
}

Write-Output "Captured UE5 runtime screenshot: $screenshotFilePath"
