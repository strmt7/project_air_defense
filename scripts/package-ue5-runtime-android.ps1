param(
    [string]$Project = "ue5/ProjectAirDefenseUE5/ProjectAirDefenseUE5.uproject",
    [string]$RunUat = "C:/Program Files/Epic Games/UE_5.7/Engine/Build/BatchFiles/RunUAT.bat",
    [string]$ArchiveDir = "packaged/Android",
    [ValidateSet("Development", "Shipping", "DebugGame")]
    [string]$Configuration = "Development",
    [ValidateSet("ASTC", "ETC2")]
    [string]$TextureFormat = "ASTC",
    [switch]$PreflightOnly,
    [switch]$KeepExistingArchive,
    [switch]$KeepStagedBuilds
)

$ErrorActionPreference = "Stop"

function Resolve-AbsolutePath([string]$PathValue) {
    if ([System.IO.Path]::IsPathRooted($PathValue)) {
        return $PathValue
    }
    return Join-Path (Get-Location) $PathValue
}

function Get-DirectorySizeGiB([string]$PathValue) {
    if (-not (Test-Path -LiteralPath $PathValue)) {
        return 0.0
    }

    $sizeBytes = (Get-ChildItem -LiteralPath $PathValue -Recurse -Force -File -ErrorAction SilentlyContinue |
        Measure-Object -Property Length -Sum).Sum
    return [math]::Round(($sizeBytes / 1GB), 3)
}

function Get-FirstExistingDirectory([string[]]$PathValues) {
    foreach ($pathValue in $PathValues) {
        if ([string]::IsNullOrWhiteSpace($pathValue)) {
            continue
        }
        $resolvedPath = Resolve-AbsolutePath $pathValue
        if (Test-Path -LiteralPath $resolvedPath -PathType Container) {
            return $resolvedPath
        }
    }
    return $null
}

function Get-JavaExecutablePath() {
    if (-not [string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
        $javaHomeCandidate = Join-Path $env:JAVA_HOME "bin/java.exe"
        if (Test-Path -LiteralPath $javaHomeCandidate -PathType Leaf) {
            return $javaHomeCandidate
        }
    }

    $javaCommand = Get-Command java -ErrorAction SilentlyContinue
    if ($null -eq $javaCommand) {
        return $null
    }
    return $javaCommand.Source
}

function Get-JavaVersionText([string]$JavaExecutablePath) {
    $processInfo = [System.Diagnostics.ProcessStartInfo]::new()
    $processInfo.FileName = $JavaExecutablePath
    $processInfo.Arguments = "-version"
    $processInfo.RedirectStandardError = $true
    $processInfo.RedirectStandardOutput = $true
    $processInfo.UseShellExecute = $false

    $process = [System.Diagnostics.Process]::Start($processInfo)
    $standardOutput = $process.StandardOutput.ReadToEnd()
    $standardError = $process.StandardError.ReadToEnd()
    $process.WaitForExit()
    return "$standardError`n$standardOutput".Trim()
}

function Get-JavaMajorVersion([string]$VersionText, [string]$JavaExecutablePath) {
    $versionText = $VersionText
    if ($versionText -notmatch 'version "([0-9]+)(?:\.([0-9]+))?') {
        throw "Unable to parse Java version from $JavaExecutablePath. Output: $versionText"
    }

    $first = [int]$Matches[1]
    if ($first -eq 1 -and $Matches[2]) {
        return [int]$Matches[2]
    }
    return $first
}

function Assert-Java17() {
    $javaPath = Get-JavaExecutablePath
    if ([string]::IsNullOrWhiteSpace($javaPath)) {
        throw "JDK 17 not found. Install JDK 17, set JAVA_HOME to it, and put JAVA_HOME/bin before older Java entries."
    }

    $versionText = Get-JavaVersionText $javaPath
    $majorVersion = Get-JavaMajorVersion $versionText $javaPath
    if ($majorVersion -ne 17) {
        $versionLine = ($versionText -split "`r?`n" | Select-Object -First 1)
        throw "UE5 Android packaging requires JDK 17. Active Java is $javaPath ($versionLine). Set JAVA_HOME to JDK 17 before running this script."
    }
    return $javaPath
}

function Resolve-AndroidHome() {
    $androidHome = Get-FirstExistingDirectory @($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT)
    if ([string]::IsNullOrWhiteSpace($androidHome)) {
        throw "Android SDK not found. Set ANDROID_HOME or ANDROID_SDK_ROOT to the SDK root with platform-tools, android-36, and build-tools 36.0.0."
    }
    return $androidHome
}

function Assert-AndroidSdkComponents([string]$AndroidHome) {
    $requiredSdkFiles = @(
        @{ Label = "platform-tools/adb.exe"; Path = (Join-Path $AndroidHome "platform-tools/adb.exe") },
        @{ Label = "platforms/android-36/android.jar"; Path = (Join-Path $AndroidHome "platforms/android-36/android.jar") },
        @{ Label = "build-tools/36.0.0/aapt2.exe"; Path = (Join-Path $AndroidHome "build-tools/36.0.0/aapt2.exe") }
    )

    foreach ($item in $requiredSdkFiles) {
        if (-not (Test-Path -LiteralPath $item.Path -PathType Leaf)) {
            throw "Missing Android SDK component: $($item.Label). Install it with sdkmanager before packaging."
        }
    }
}

function Resolve-AndroidNdkPath([string]$AndroidHome) {
    $ndkCandidates = @(
        $env:NDKROOT,
        $env:ANDROID_NDK_HOME,
        (Join-Path $AndroidHome "ndk/25.1.8937393")
    )
    $checked = New-Object System.Collections.Generic.List[string]
    foreach ($candidate in $ndkCandidates) {
        if ([string]::IsNullOrWhiteSpace($candidate)) {
            continue
        }
        $resolvedCandidate = Resolve-AbsolutePath $candidate
        if (-not (Test-Path -LiteralPath $resolvedCandidate -PathType Container)) {
            $checked.Add("$resolvedCandidate (missing)")
            continue
        }

        $sourcePropertiesPath = Join-Path $resolvedCandidate "source.properties"
        if (-not (Test-Path -LiteralPath $sourcePropertiesPath -PathType Leaf)) {
            $checked.Add("$resolvedCandidate (missing source.properties)")
            continue
        }

        $revisionLine = Get-Content -LiteralPath $sourcePropertiesPath |
            Where-Object { $_ -match '^Pkg\.Revision\s*=\s*(.+)$' } |
            Select-Object -First 1
        $revision = if ($revisionLine -match '^Pkg\.Revision\s*=\s*(.+)$') { $Matches[1].Trim() } else { "" }
        if ($revision -eq "25.1.8937393") {
            return $resolvedCandidate
        }
        $checked.Add("$resolvedCandidate (Pkg.Revision=$revision)")
    }

    $checkedText = if ($checked.Count -gt 0) { $checked -join "; " } else { "no candidate paths" }
    throw "Android NDK r25b (25.1.8937393) not found. Checked: $checkedText. Install NDK 25.1.8937393 and set NDKROOT or ANDROID_NDK_HOME."
}

function Assert-UnrealAndroidTargetSupport([string]$EngineRoot) {
    $requiredEnginePaths = @(
        @{ Label = "UnrealBuildTool Android platform"; Path = (Join-Path $EngineRoot "Source/Programs/UnrealBuildTool/Platform/Android/UEBuildAndroid.cs") },
        @{ Label = "Android Gradle template files"; Path = (Join-Path $EngineRoot "Build/Android/Java") }
    )

    $missing = @()
    foreach ($item in $requiredEnginePaths) {
        if (-not (Test-Path -LiteralPath $item.Path)) {
            $missing += "$($item.Label): $($item.Path)"
        }
    }

    if ($missing.Count -gt 0) {
        throw "Unreal Engine Android target-platform files are incomplete. Enable the Android optional component for UE 5.7 in Epic Games Launcher. Missing: $($missing -join '; ')"
    }
}

$projectPath = Resolve-AbsolutePath $Project
$runUatPath = Resolve-AbsolutePath $RunUat
$archiveDirPath = Resolve-AbsolutePath $ArchiveDir
$projectRoot = Split-Path -Parent $projectPath
$savedRoot = Join-Path $projectRoot "Saved"
$stagedBuildsPath = Join-Path $savedRoot "StagedBuilds"

if (-not (Test-Path -LiteralPath $projectPath)) {
    throw "Project file not found: $projectPath"
}
if (-not (Test-Path -LiteralPath $runUatPath)) {
    throw "RunUAT.bat not found: $runUatPath"
}

$engineRoot = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $runUatPath))
$javaPath = Assert-Java17
$androidHomePath = Resolve-AndroidHome
Assert-AndroidSdkComponents $androidHomePath
$androidNdkPath = Resolve-AndroidNdkPath $androidHomePath
Assert-UnrealAndroidTargetSupport $engineRoot

$javaHomePath = Split-Path -Parent (Split-Path -Parent $javaPath)
$env:JAVA_HOME = $javaHomePath
$env:ANDROID_HOME = $androidHomePath
$env:ANDROID_SDK_ROOT = $androidHomePath
$env:NDKROOT = $androidNdkPath
$env:ANDROID_NDK_HOME = $androidNdkPath
$env:PATH = "$javaHomePath\bin;$env:PATH"

if ($PreflightOnly) {
    Write-Output "UE5 Android packaging preflight passed."
    Write-Output "Java: $javaPath"
    Write-Output "Android SDK: $androidHomePath"
    Write-Output "Android NDK: $androidNdkPath"
    Write-Output "Unreal Engine root: $engineRoot"
    return
}

if (Test-Path -LiteralPath $stagedBuildsPath) {
    Remove-Item -LiteralPath $stagedBuildsPath -Recurse -Force
}

if ((Test-Path -LiteralPath $archiveDirPath) -and -not $KeepExistingArchive) {
    Remove-Item -LiteralPath $archiveDirPath -Recurse -Force
}

New-Item -ItemType Directory -Force -Path $archiveDirPath | Out-Null

$arguments = @(
    "BuildCookRun",
    "-project=$projectPath",
    "-platform=Android",
    "-cookflavor=$TextureFormat",
    "-clientconfig=$Configuration",
    "-build",
    "-cook",
    "-stage",
    "-package",
    "-pak",
    "-archive",
    "-archivedirectory=$archiveDirPath",
    "-nop4",
    "-utf8output"
)

& $runUatPath @arguments
if ($LASTEXITCODE -ne 0) {
    throw "Android BuildCookRun failed with exit code $LASTEXITCODE"
}

$removedStagedBuilds = $false
if ((Test-Path -LiteralPath $stagedBuildsPath) -and -not $KeepStagedBuilds) {
    Remove-Item -LiteralPath $stagedBuildsPath -Recurse -Force
    $removedStagedBuilds = $true
}

$archiveGiB = Get-DirectorySizeGiB $archiveDirPath

Write-Output "Packaged UE5 Android runtime to $archiveDirPath ($archiveGiB GiB)."
Write-Output "Texture format: $TextureFormat. Configuration: $Configuration."
if ($removedStagedBuilds) {
    Write-Output "Removed duplicate staged build directory: $stagedBuildsPath"
}
