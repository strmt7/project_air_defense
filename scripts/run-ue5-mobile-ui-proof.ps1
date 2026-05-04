param(
    [string]$Exe = "packaged/Win64/ProjectAirDefenseUE5.exe",
    [string]$Project = "",
    [string]$OutputDir = "benchmark-results",
    [string]$ArtifactPrefix = "ue5-packaged",
    [int]$Width = 1600,
    [int]$Height = 900,
    [int]$MenuDelaySeconds = 12,
    [int]$BattleDelaySeconds = 14,
    [int]$SystemsDelaySeconds = 12,
    [long]$MinScreenshotBytes = 50000,
    [int]$MinDistinctSampleColors = 24,
    [double]$MinLuminanceRange = 12.0,
    [switch]$RequireMenuOcr,
    [switch]$RequireState,
    [switch]$ValidateOnly
)

$ErrorActionPreference = "Stop"

function Resolve-AbsolutePath([string]$PathValue) {
    if ([System.IO.Path]::IsPathRooted($PathValue)) {
        return $PathValue
    }
    return Join-Path $repoRoot $PathValue
}

function ConvertTo-IsoTimestamp([datetime]$Value) {
    return $Value.ToUniversalTime().ToString("o", [System.Globalization.CultureInfo]::InvariantCulture)
}

function Resolve-Tesseract {
    $candidates = @()
    if (-not [string]::IsNullOrWhiteSpace($env:TESSERACT_CMD)) {
        $candidates += $env:TESSERACT_CMD.Trim('"')
    }
    $command = Get-Command "tesseract" -ErrorAction SilentlyContinue
    if ($null -ne $command) {
        $candidates += $command.Source
    }
    $candidates += @(
        "C:\Program Files\Tesseract-OCR\tesseract.exe",
        "C:\Program Files (x86)\Tesseract-OCR\tesseract.exe"
    )

    foreach ($candidate in $candidates) {
        if (-not [string]::IsNullOrWhiteSpace($candidate) -and (Test-Path -LiteralPath $candidate)) {
            return $candidate
        }
    }
    return $null
}

function Get-ScreenshotStats([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path)) {
        throw "Screenshot not found: $Path"
    }

    Add-Type -AssemblyName System.Drawing
    $file = Get-Item -LiteralPath $Path
    $bitmap = [System.Drawing.Bitmap]::FromFile($Path)
    try {
        $sampleStepX = [Math]::Max(1, [int][Math]::Floor($bitmap.Width / 64))
        $sampleStepY = [Math]::Max(1, [int][Math]::Floor($bitmap.Height / 64))
        $colors = New-Object "System.Collections.Generic.HashSet[string]"
        $minLuminance = [double]::MaxValue
        $maxLuminance = [double]::MinValue
        $sampleCount = 0

        for ($y = 0; $y -lt $bitmap.Height; $y += $sampleStepY) {
            for ($x = 0; $x -lt $bitmap.Width; $x += $sampleStepX) {
                $pixel = $bitmap.GetPixel($x, $y)
                $quantizedColor = "{0:X2}{1:X2}{2:X2}" -f (($pixel.R -band 0xF0)), (($pixel.G -band 0xF0)), (($pixel.B -band 0xF0))
                $colors.Add($quantizedColor) | Out-Null
                $luminance = (0.2126 * $pixel.R) + (0.7152 * $pixel.G) + (0.0722 * $pixel.B)
                $minLuminance = [Math]::Min($minLuminance, $luminance)
                $maxLuminance = [Math]::Max($maxLuminance, $luminance)
                $sampleCount++
            }
        }

        return [pscustomobject]@{
            path = $Path
            bytes = $file.Length
            width = $bitmap.Width
            height = $bitmap.Height
            sampleCount = $sampleCount
            distinctSampleColors = $colors.Count
            minLuminance = [Math]::Round($minLuminance, 3)
            maxLuminance = [Math]::Round($maxLuminance, 3)
            luminanceRange = [Math]::Round($maxLuminance - $minLuminance, 3)
        }
    }
    finally {
        $bitmap.Dispose()
    }
}

function Assert-ScreenshotLooksRendered([object]$Stats, [string]$Name) {
    if ($Stats.bytes -lt $MinScreenshotBytes) {
        throw "$Name screenshot is too small to trust as rendered proof: $($Stats.bytes) bytes, expected at least $MinScreenshotBytes."
    }
    if ($Stats.width -lt 1 -or $Stats.height -lt 1) {
        throw "$Name screenshot has invalid dimensions: $($Stats.width)x$($Stats.height)."
    }
    if ($Stats.distinctSampleColors -lt $MinDistinctSampleColors) {
        throw "$Name screenshot has too little sampled color variation: $($Stats.distinctSampleColors), expected at least $MinDistinctSampleColors."
    }
    if ($Stats.luminanceRange -lt $MinLuminanceRange) {
        throw "$Name screenshot has too little luminance variation: $($Stats.luminanceRange), expected at least $MinLuminanceRange."
    }
}

function Invoke-MenuOcr([string]$ScreenshotPath) {
    $tesseract = Resolve-Tesseract
    if ($null -eq $tesseract) {
        if ($RequireMenuOcr) {
            throw "Tesseract OCR was requested but tesseract.exe was not found."
        }
        return [pscustomobject]@{
            available = $false
            foundTitle = $false
            textPath = $null
        }
    }

    $textPath = [System.IO.Path]::ChangeExtension($ScreenshotPath, ".ocr.txt")
    $basePath = [System.IO.Path]::Combine(
        [System.IO.Path]::GetDirectoryName($textPath),
        [System.IO.Path]::GetFileNameWithoutExtension($textPath)
    )
    & $tesseract $ScreenshotPath $basePath --psm 11 | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "Tesseract OCR failed for menu screenshot: $ScreenshotPath"
    }

    $ocrText = if (Test-Path -LiteralPath $textPath) {
        Get-Content -Raw -LiteralPath $textPath
    } else {
        ""
    }
    $normalizedOcrText = ($ocrText.ToUpperInvariant() -replace "[^A-Z]", "")
    $foundTitle = $normalizedOcrText.Contains("PROJECTAIRDEFENSE") -or
        ($normalizedOcrText.Contains("PROJ") -and $normalizedOcrText.Contains("ECTAIRDEFENSE"))
    if ($RequireMenuOcr -and -not $foundTitle) {
        throw "Menu OCR did not find the invariant title 'PROJECT AIR DEFENSE'. OCR text: $ocrText"
    }

    return [pscustomobject]@{
        available = $true
        foundTitle = $foundTitle
        normalizedTitleFound = $foundTitle
        textPath = $textPath
    }
}

function Read-CaptureState([string]$Path, [string]$Name, [bool]$Required) {
    if (-not (Test-Path -LiteralPath $Path)) {
        if ($Required) {
            throw "$Name capture state file not found: $Path"
        }
        return $null
    }

    $state = Get-Content -Raw -LiteralPath $Path | ConvertFrom-Json
    if (-not $state.success) {
        throw "$Name capture state did not report success: $Path"
    }
    if (-not $state.screenshotFound) {
        throw "$Name capture state did not report a screenshot."
    }
    if (-not $state.processAliveAtScreenshot) {
        throw "$Name capture did not prove the process was alive when the screenshot appeared."
    }
    return $state
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$captureScript = Join-Path $PSScriptRoot "capture-ue5-runtime-screenshot.ps1"
$resolvedExe = Resolve-AbsolutePath $Exe
$resolvedProject = if ([string]::IsNullOrWhiteSpace($Project)) { "" } else { Resolve-AbsolutePath $Project }
$resolvedOutputDir = Resolve-AbsolutePath $OutputDir

New-Item -ItemType Directory -Force -Path $resolvedOutputDir | Out-Null

$captures = @(
    @{
        Name = "menu"
        DelaySeconds = $MenuDelaySeconds
        ExtraArgs = @()
    },
    @{
        Name = "battle"
        DelaySeconds = $BattleDelaySeconds
        ExtraArgs = @("-AutoStartBattle")
    },
    @{
        Name = "systems"
        DelaySeconds = $SystemsDelaySeconds
        ExtraArgs = @("-ShowSystemsMenu")
    }
)

foreach ($capture in $captures) {
    $name = $capture.Name
    $logPath = Join-Path $resolvedOutputDir "$ArtifactPrefix-$name-proof.log"
    $screenshotPath = Join-Path $resolvedOutputDir "$ArtifactPrefix-$name-proof.png"
    $statePath = Join-Path $resolvedOutputDir "$ArtifactPrefix-$name-proof-state.json"

    if (-not $ValidateOnly) {
        $arguments = @(
            "-ExecutionPolicy", "Bypass",
            "-File", $captureScript,
            "-Exe", $resolvedExe,
            "-LogPath", $logPath,
            "-ScreenshotPath", $screenshotPath,
            "-StatePath", $statePath,
            "-Width", [string]$Width,
            "-Height", [string]$Height,
            "-KeepAliveAfterScreenshot",
            "-PostScreenshotAliveSeconds", "2",
            "-DelaySeconds", [string]$capture.DelaySeconds
        )
        if (-not [string]::IsNullOrWhiteSpace($resolvedProject)) {
            $arguments += @("-Project", $resolvedProject)
        }
        $arguments += $capture.ExtraArgs

        & powershell @arguments
    }
}

$requireStateForValidation = $RequireState.IsPresent -or (-not $ValidateOnly.IsPresent)
$results = @()
foreach ($capture in $captures) {
    $name = $capture.Name
    $logPath = Join-Path $resolvedOutputDir "$ArtifactPrefix-$name-proof.log"
    $screenshotPath = Join-Path $resolvedOutputDir "$ArtifactPrefix-$name-proof.png"
    $statePath = Join-Path $resolvedOutputDir "$ArtifactPrefix-$name-proof-state.json"
    $state = Read-CaptureState $statePath $name $requireStateForValidation
    $stats = Get-ScreenshotStats $screenshotPath
    Assert-ScreenshotLooksRendered $stats $name

    $ocr = $null
    if ($name -eq "menu") {
        $ocr = Invoke-MenuOcr $screenshotPath
    }

    $results += [pscustomobject]@{
        name = $name
        screenshot = $screenshotPath
        log = $logPath
        state = if ($null -eq $state) { $null } else { $statePath }
        stateVerified = $null -ne $state
        screenshotStats = $stats
        menuOcr = $ocr
    }
}

$manifestPath = Join-Path $resolvedOutputDir "$ArtifactPrefix-ui-proof-manifest.json"
$manifest = [pscustomobject]@{
    generatedAt = ConvertTo-IsoTimestamp (Get-Date)
    validateOnly = $ValidateOnly.IsPresent
    exe = $resolvedExe
    project = if ([string]::IsNullOrWhiteSpace($resolvedProject)) { $null } else { $resolvedProject }
    outputDir = $resolvedOutputDir
    artifactPrefix = $ArtifactPrefix
    thresholds = [pscustomobject]@{
        minScreenshotBytes = $MinScreenshotBytes
        minDistinctSampleColors = $MinDistinctSampleColors
        minLuminanceRange = $MinLuminanceRange
    }
    captures = $results
}
$manifest | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $manifestPath -Encoding UTF8

Write-Output "UE5 mobile UI proof validated: $manifestPath"
