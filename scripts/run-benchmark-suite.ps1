param(
    [string]$OutputDir = "",
    [int]$Runs = 300,
    [int]$Waves = 1,
    [float]$Seconds = 48,
    [float]$Step = 0.05,
    [long]$Seed = 20260411
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$gradle = Join-Path $repoRoot "gradlew.bat"
$adb = "C:\Users\strat\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$targetPackage = "com.airdefense.game.benchmark"
$targetActivity = "com.airdefense.game.AndroidLauncher"
$launchTargetExtra = "com.airdefense.game.extra.LAUNCH_TARGET"
$seedExtra = "com.airdefense.game.extra.BENCHMARK_SEED"
$battleLaunchTarget = "battle"
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"

if (-not (Test-Path $adb)) {
    throw "adb not found at $adb"
}

if ([string]::IsNullOrWhiteSpace($OutputDir)) {
    $OutputDir = Join-Path $repoRoot "benchmark-results\$timestamp"
}

$null = New-Item -ItemType Directory -Force -Path $OutputDir

$simulationJson = Join-Path $OutputDir "battle-monte-carlo.json"
$summaryMd = Join-Path $OutputDir "summary.md"
$summaryJson = Join-Path $OutputDir "summary.json"
$buildBenchmarksJson = Join-Path $OutputDir "build-benchmarks.json"
$runtimeHealthJson = Join-Path $OutputDir "runtime-health.json"

function Invoke-GradleTask {
    param(
        [string[]]$Tasks,
        [string[]]$Properties = @()
    )

    $output = & $gradle @Tasks @Properties --no-daemon --console=plain
    $output | Out-Host
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle failed for tasks: $($Tasks -join ' ')"
    }
}

function Measure-GradleTask {
    param(
        [string]$Name,
        [string[]]$Tasks
    )

    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    Invoke-GradleTask -Tasks $Tasks
    $stopwatch.Stop()

    [pscustomobject]@{
        name = $Name
        tasks = $Tasks
        seconds = [math]::Round($stopwatch.Elapsed.TotalSeconds, 3)
    }
}

function Get-XmlIssueCount {
    param(
        [string]$Path,
        [string]$TagName
    )

    if (-not (Test-Path $Path)) {
        return $null
    }

    $xml = [xml](Get-Content -Raw -Path $Path)
    return @($xml.SelectNodes("//$TagName")).Count
}

function Get-DependencyAdviceCount {
    param([string]$Path)

    if (-not (Test-Path $Path)) {
        return $null
    }

    $report = Get-Content -Raw -Path $Path | ConvertFrom-Json
    return @($report.dependencyAdvice).Count + @($report.pluginAdvice).Count + @($report.moduleAdvice).Count
}

function Get-RegexValue {
    param(
        [string]$Text,
        [string]$Pattern
    )

    $match = [regex]::Match($Text, $Pattern, [System.Text.RegularExpressions.RegexOptions]::Multiline)
    if ($match.Success) {
        return $match.Groups[1].Value.Trim()
    }

    return $null
}

function Get-BattleFrameTelemetry {
    param([string]$LogText)

    $matches = [regex]::Matches(
        $LogText,
        "BattleFrame:\s+LIVE\s+(\d+)s\s+//\s+FPS\s+([0-9.]+)\s+//\s+FT\s+([0-9.]+)\/([0-9.]+)\/([0-9.]+)\s+//\s+Q\s+([A-Z]+)\s+//\s+FX(\d+)\s+//\s+T(\d+)\s+I(\d+)"
    )
    if ($matches.Count -eq 0) {
        return $null
    }

    $fpsSamples = @()
    $p95Samples = @()
    $maxSamples = @()
    foreach ($match in $matches) {
        $fpsSamples += [double]::Parse($match.Groups[2].Value, [System.Globalization.CultureInfo]::InvariantCulture)
        $p95Samples += [double]::Parse($match.Groups[4].Value, [System.Globalization.CultureInfo]::InvariantCulture)
        $maxSamples += [double]::Parse($match.Groups[5].Value, [System.Globalization.CultureInfo]::InvariantCulture)
    }

    $last = $matches[$matches.Count - 1]
    $averageFps = ($fpsSamples | Measure-Object -Average).Average

    return [pscustomobject]@{
        sampleCount = $matches.Count
        averageFps = [math]::Round($averageFps, 1)
        minFps = ($fpsSamples | Measure-Object -Minimum).Minimum
        maxFps = ($fpsSamples | Measure-Object -Maximum).Maximum
        liveSeconds = [int]$last.Groups[1].Value
        lastWindowFrameP50Ms = [double]::Parse($last.Groups[3].Value, [System.Globalization.CultureInfo]::InvariantCulture)
        lastWindowFrameP95Ms = [double]::Parse($last.Groups[4].Value, [System.Globalization.CultureInfo]::InvariantCulture)
        lastWindowFrameMaxMs = [double]::Parse($last.Groups[5].Value, [System.Globalization.CultureInfo]::InvariantCulture)
        worstWindowFrameP95Ms = ($p95Samples | Measure-Object -Maximum).Maximum
        worstWindowFrameMaxMs = ($maxSamples | Measure-Object -Maximum).Maximum
        qualityMode = if ($last.Groups[6].Success) { $last.Groups[6].Value } else { $null }
        effects = if ($last.Groups[7].Success) { [int]$last.Groups[7].Value } else { $null }
        threats = [int]$last.Groups[8].Value
        interceptors = [int]$last.Groups[9].Value
    }
}

function Capture-Screenshot {
    param([string]$Path)

    $process = Start-Process -FilePath $adb `
        -ArgumentList @("exec-out", "screencap", "-p") `
        -RedirectStandardOutput $Path `
        -Wait `
        -NoNewWindow `
        -PassThru
    if ($process.ExitCode -ne 0) {
        throw "Failed to capture screenshot"
    }
}

function Capture-RuntimeHealth {
    param(
        [string]$OutputDirectory,
        [long]$BenchmarkSeed
    )

    $launchLog = Join-Path $OutputDirectory "runtime-launch.txt"
    $gfxinfoPath = Join-Path $OutputDirectory "runtime-gfxinfo.txt"
    $meminfoPath = Join-Path $OutputDirectory "runtime-meminfo.txt"
    $cpuinfoPath = Join-Path $OutputDirectory "runtime-cpuinfo.txt"
    $logcatPath = Join-Path $OutputDirectory "runtime-logcat.txt"
    $crashLogPath = Join-Path $OutputDirectory "runtime-crash.txt"
    $screenshotPath = Join-Path $OutputDirectory "runtime-battle.png"

    & $adb shell input keyevent KEYCODE_WAKEUP | Out-Null
    & $adb shell wm dismiss-keyguard | Out-Null
    & $adb shell am force-stop $targetPackage | Out-Null
    & $adb logcat -c
    & $adb shell "dumpsys gfxinfo $targetPackage reset" | Out-Null

    $launchOutput = & $adb shell "am start -W -n $targetPackage/$targetActivity --es $launchTargetExtra $battleLaunchTarget --el $seedExtra $BenchmarkSeed"
    $launchOutput | Set-Content -Path $launchLog
    Start-Sleep -Seconds 15

    $gfxinfo = (& $adb shell "dumpsys gfxinfo $targetPackage") -join [Environment]::NewLine
    $meminfo = (& $adb shell "dumpsys meminfo $targetPackage") -join [Environment]::NewLine
    $cpuinfo = ((& $adb shell "dumpsys cpuinfo") | Select-String $targetPackage | ForEach-Object { $_.Line }) -join [Environment]::NewLine
    $logcat = (& $adb logcat -d -t 400) -join [Environment]::NewLine
    $crashLog = (& $adb logcat -d -b crash) -join [Environment]::NewLine
    $appPid = (& $adb shell "pidof -s $targetPackage").Trim()
    $battleFrameTelemetry = Get-BattleFrameTelemetry -LogText $logcat

    $gfxinfo | Set-Content -Path $gfxinfoPath
    $meminfo | Set-Content -Path $meminfoPath
    $cpuinfo | Set-Content -Path $cpuinfoPath
    $logcat | Set-Content -Path $logcatPath
    $crashLog | Set-Content -Path $crashLogPath
    Capture-Screenshot -Path $screenshotPath

    $launchTotalMs = Get-RegexValue -Text ($launchOutput -join [Environment]::NewLine) -Pattern "TotalTime:\s+(\d+)"
    $launchWaitMs = Get-RegexValue -Text ($launchOutput -join [Environment]::NewLine) -Pattern "WaitTime:\s+(\d+)"

    return [pscustomobject]@{
        package = $targetPackage
        pid = $appPid
        launchTotalMs = if ($launchTotalMs) { [int]$launchTotalMs } else { $null }
        launchWaitMs = if ($launchWaitMs) { [int]$launchWaitMs } else { $null }
        totalFramesRendered = Get-RegexValue -Text $gfxinfo -Pattern "Total frames rendered:\s+(\d+)"
        jankyFrames = Get-RegexValue -Text $gfxinfo -Pattern "Janky frames:\s+(\d+)"
        jankyFramesPercent = Get-RegexValue -Text $gfxinfo -Pattern "Janky frames:\s+\d+\s+\(([0-9.]+)%\)"
        frameTime50thPercentileMs = Get-RegexValue -Text $gfxinfo -Pattern "50th percentile:\s+(\d+)ms"
        frameTime90thPercentileMs = Get-RegexValue -Text $gfxinfo -Pattern "90th percentile:\s+(\d+)ms"
        frameTime95thPercentileMs = Get-RegexValue -Text $gfxinfo -Pattern "95th percentile:\s+(\d+)ms"
        frameTime99thPercentileMs = Get-RegexValue -Text $gfxinfo -Pattern "99th percentile:\s+(\d+)ms"
        totalPssKb = Get-RegexValue -Text $meminfo -Pattern "TOTAL PSS:\s+([\d,]+)"
        crashBufferEmpty = [string]::IsNullOrWhiteSpace($crashLog)
        battleFrameTelemetry = $battleFrameTelemetry
        screenshot = $screenshotPath
        launchLog = $launchLog
        gfxinfo = $gfxinfoPath
        meminfo = $meminfoPath
        cpuinfo = $cpuinfoPath
        logcat = $logcatPath
        crashLog = $crashLogPath
    }
}

function Get-MacrobenchmarkSnapshots {
    param([string]$Directory)

    $files = Get-ChildItem -Path $Directory -Filter "*-benchmarkData.json" -ErrorAction SilentlyContinue
    $snapshots = New-Object System.Collections.Generic.List[object]

    foreach ($file in $files) {
        try {
            $payload = Get-Content -Raw -Path $file.FullName | ConvertFrom-Json
            foreach ($benchmark in @($payload.benchmarks)) {
                $metricSnapshot = [ordered]@{}
                foreach ($metric in $benchmark.metrics.PSObject.Properties) {
                    $metricValue = $metric.Value
                    $metricSnapshot[$metric.Name] = [ordered]@{
                        minimum = $metricValue.minimum
                        maximum = $metricValue.maximum
                        median = $metricValue.median
                    }
                }

                $snapshots.Add([pscustomobject]@{
                    source = $file.FullName
                    name = $benchmark.name
                    className = $benchmark.className
                    totalRunTimeNs = $benchmark.totalRunTimeNs
                    metrics = [pscustomobject]$metricSnapshot
                })
            }
        } catch {
            $snapshots.Add([pscustomobject]@{
                source = $file.FullName
                parseError = $_.Exception.Message
            })
        }
    }

    return $snapshots
}

Push-Location $repoRoot
try {
    $buildBenchmarks = @(
        (Measure-GradleTask -Name "cleanAssembleBenchmark" -Tasks @("clean", ":android:assembleBenchmark")),
        (Measure-GradleTask -Name "warmAssembleBenchmark" -Tasks @(":android:assembleBenchmark")),
        (Measure-GradleTask -Name "standardsAudit" -Tasks @("benchmarkStandardsAudit"))
    )
    $buildBenchmarks | ConvertTo-Json -Depth 6 | Set-Content -Path $buildBenchmarksJson

    Invoke-GradleTask -Tasks @(
        ":core:test",
        ":core:runBattleMonteCarlo",
        ":android:assembleBenchmark",
        ":android:installBenchmark",
        ":android:printAppIdentity",
        ":benchmarks:detekt",
        ":android:detekt",
        ":core:detekt",
        ":android:lintBenchmark",
        ":core:projectHealth",
        ":android:projectHealth",
        ":benchmarks:projectHealth"
    ) -Properties @(
        "-Pruns=$Runs",
        "-Pwaves=$Waves",
        "-Pseconds=$Seconds",
        "-Pstep=$Step",
        "-Pseed=$Seed",
        "-PreportPath=$simulationJson"
    )

    Invoke-GradleTask -Tasks @(":benchmarks:connectedCheck")

    $localBenchmarkOutputDir = Join-Path $repoRoot "benchmarks\build\outputs\connected_android_test_additional_output"
    if (Test-Path $localBenchmarkOutputDir) {
        Get-ChildItem -Path $localBenchmarkOutputDir -Filter "*-benchmarkData.json" -Recurse -File `
            | ForEach-Object { Copy-Item -Path $_.FullName -Destination (Join-Path $OutputDir $_.Name) -Force }
    }

    & $adb shell find /storage/emulated/0/Android/ -name "*-benchmarkData.json" 2>$null `
        | ForEach-Object { $_.Trim() } `
        | Where-Object { $_ } `
        | ForEach-Object { & $adb pull $_ $OutputDir | Out-Null }

    $runtimeHealth = Capture-RuntimeHealth -OutputDirectory $OutputDir -BenchmarkSeed $Seed
    $runtimeHealth | ConvertTo-Json -Depth 6 | Set-Content -Path $runtimeHealthJson

    $benchmarkJsonFiles = Get-ChildItem -Path $OutputDir -Filter "*-benchmarkData.json" -ErrorAction SilentlyContinue
    $macrobenchmarkSnapshots = Get-MacrobenchmarkSnapshots -Directory $OutputDir
    $apk = Get-ChildItem -Path (Join-Path $repoRoot "android\build\outputs\apk\benchmark") -Recurse -Filter "*.apk" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    $apkBytes = if ($apk) { $apk.Length } else { 0 }

    $simulation = if (Test-Path $simulationJson) {
        Get-Content -Raw -Path $simulationJson | ConvertFrom-Json
    } else {
        $null
    }

    $reports = [pscustomobject]@{
        detektAndroid = Join-Path $repoRoot "android\build\reports\detekt\detekt.html"
        detektAndroidXml = Join-Path $repoRoot "android\build\reports\detekt\detekt.xml"
        detektCore = Join-Path $repoRoot "core\build\reports\detekt\detekt.html"
        detektCoreXml = Join-Path $repoRoot "core\build\reports\detekt\detekt.xml"
        detektBenchmarks = Join-Path $repoRoot "benchmarks\build\reports\detekt\detekt.html"
        detektBenchmarksXml = Join-Path $repoRoot "benchmarks\build\reports\detekt\detekt.xml"
        lintBenchmark = Join-Path $repoRoot "android\build\reports\lint-results-benchmark.html"
        lintBenchmarkXml = Join-Path $repoRoot "android\build\reports\lint-results-benchmark.xml"
        projectHealthAndroid = Join-Path $repoRoot "android\build\reports\dependency-analysis\project-health-report.txt"
        projectHealthAndroidJson = Join-Path $repoRoot "android\build\reports\dependency-analysis\final-advice.json"
        projectHealthCore = Join-Path $repoRoot "core\build\reports\dependency-analysis\project-health-report.txt"
        projectHealthCoreJson = Join-Path $repoRoot "core\build\reports\dependency-analysis\final-advice.json"
        projectHealthBenchmarks = Join-Path $repoRoot "benchmarks\build\reports\dependency-analysis\project-health-report.txt"
        projectHealthBenchmarksJson = Join-Path $repoRoot "benchmarks\build\reports\dependency-analysis\final-advice.json"
    }

    $standards = [pscustomobject]@{
        ktlintGatePassed = $true
        detektAndroidIssues = Get-XmlIssueCount -Path $reports.detektAndroidXml -TagName "error"
        detektCoreIssues = Get-XmlIssueCount -Path $reports.detektCoreXml -TagName "error"
        detektBenchmarksIssues = Get-XmlIssueCount -Path $reports.detektBenchmarksXml -TagName "error"
        lintBenchmarkIssues = Get-XmlIssueCount -Path $reports.lintBenchmarkXml -TagName "issue"
        dependencyAdviceAndroid = Get-DependencyAdviceCount -Path $reports.projectHealthAndroidJson
        dependencyAdviceCore = Get-DependencyAdviceCount -Path $reports.projectHealthCoreJson
        dependencyAdviceBenchmarks = Get-DependencyAdviceCount -Path $reports.projectHealthBenchmarksJson
    }

    $summaryObject = [pscustomobject]@{
        generatedAt = (Get-Date).ToString("o")
        outputDir = $OutputDir
        apkPath = $apk.FullName
        apkBytes = $apkBytes
        benchmarkJsonCount = @($benchmarkJsonFiles).Count
        buildBenchmarks = $buildBenchmarks
        runtimeHealth = $runtimeHealth
        simulation = $simulation
        macrobenchmarks = $macrobenchmarkSnapshots
        standards = $standards
        reports = $reports
    }

    $summaryObject | ConvertTo-Json -Depth 12 | Set-Content -Path $summaryJson

    $lines = @(
        "# Benchmark Suite Summary",
        "",
        "- Generated: $($summaryObject.generatedAt)",
        "- APK: $($summaryObject.apkPath)",
        "- APK bytes: $apkBytes",
        "- Macrobenchmark JSON files: $(@($benchmarkJsonFiles).Count)",
        "- Monte Carlo report: $simulationJson",
        "- Runtime screenshot: $($runtimeHealth.screenshot)",
        "",
        "## Build Timing",
        ""
    )

    foreach ($buildBenchmark in $buildBenchmarks) {
        $lines += "- $($buildBenchmark.name): $($buildBenchmark.seconds)s"
    }

    $lines += ""
    $lines += "## Runtime Health"
    $lines += ""
    $lines += "- PID during capture: $($runtimeHealth.pid)"
    $lines += "- Launch total/wait: $($runtimeHealth.launchTotalMs)ms / $($runtimeHealth.launchWaitMs)ms"
    $lines += "- Total frames rendered: $($runtimeHealth.totalFramesRendered)"
    $lines += "- Janky frames: $($runtimeHealth.jankyFrames) ($($runtimeHealth.jankyFramesPercent)%)"
    $lines += "- Frame percentiles (50/90/95/99): $($runtimeHealth.frameTime50thPercentileMs) / $($runtimeHealth.frameTime90thPercentileMs) / $($runtimeHealth.frameTime95thPercentileMs) / $($runtimeHealth.frameTime99thPercentileMs) ms"
    $lines += "- Total PSS: $($runtimeHealth.totalPssKb) KB"
    $lines += "- Crash buffer empty: $($runtimeHealth.crashBufferEmpty)"
    if ($runtimeHealth.battleFrameTelemetry) {
        $lines += "- Battle frame telemetry: avg $($runtimeHealth.battleFrameTelemetry.averageFps) FPS across $($runtimeHealth.battleFrameTelemetry.sampleCount) samples; last sample at $($runtimeHealth.battleFrameTelemetry.liveSeconds)s with T$($runtimeHealth.battleFrameTelemetry.threats) I$($runtimeHealth.battleFrameTelemetry.interceptors)"
        $lines += "- Last window frame times (p50/p95/max): $($runtimeHealth.battleFrameTelemetry.lastWindowFrameP50Ms) / $($runtimeHealth.battleFrameTelemetry.lastWindowFrameP95Ms) / $($runtimeHealth.battleFrameTelemetry.lastWindowFrameMaxMs) ms"
        $lines += "- Worst recorded window (p95/max): $($runtimeHealth.battleFrameTelemetry.worstWindowFrameP95Ms) / $($runtimeHealth.battleFrameTelemetry.worstWindowFrameMaxMs) ms"
        if ($runtimeHealth.battleFrameTelemetry.qualityMode) {
            $lines += "- Battle quality telemetry: $($runtimeHealth.battleFrameTelemetry.qualityMode) with FX$($runtimeHealth.battleFrameTelemetry.effects)"
        }
    }
    $lines += "- Note: `dumpsys gfxinfo` can undercount SurfaceView-based libGDX rendering, so BattleFrame log telemetry is the primary runtime frame-health signal."

    if ($simulation) {
        $lines += ""
        $lines += "## Simulation Snapshot"
        $lines += ""
        $lines += "- Runs: $($simulation.runs)"
        $lines += "- Game-over rate: $($simulation.gameOverRatePercent)%"
        $lines += "- Average city integrity: $($simulation.metrics.cityIntegrityPercent.average)%"
        $lines += "- Average intercept rate: $($simulation.metrics.interceptRatePercent.average)%"
        $lines += "- Average hostile impacts: $($simulation.metrics.hostileImpacts.average)"
    }

    if ($macrobenchmarkSnapshots.Count -gt 0) {
        $lines += ""
        $lines += "## Macrobenchmark Snapshot"
        $lines += ""
        foreach ($entry in $macrobenchmarkSnapshots) {
            if ($entry.parseError) {
                $lines += "- $($entry.source): parse error: $($entry.parseError)"
                continue
            }

            $metricNames = @($entry.metrics.PSObject.Properties.Name)
            $metricsSummary = foreach ($metricName in $metricNames) {
                $metric = $entry.metrics.$metricName
                "$metricName median=$($metric.median)"
            }
            $lines += "- $($entry.className).$($entry.name): $($metricsSummary -join '; ')"
        }
    }

    $lines += ""
    $lines += "## Standards Snapshot"
    $lines += ""
    $lines += "- Ktlint gate passed: $($standards.ktlintGatePassed)"
    $lines += "- Android detekt issues: $($standards.detektAndroidIssues)"
    $lines += "- Core detekt issues: $($standards.detektCoreIssues)"
    $lines += "- Benchmarks detekt issues: $($standards.detektBenchmarksIssues)"
    $lines += "- Lint issues: $($standards.lintBenchmarkIssues)"
    $lines += "- Android dependency advice items: $($standards.dependencyAdviceAndroid)"
    $lines += "- Core dependency advice items: $($standards.dependencyAdviceCore)"
    $lines += "- Benchmarks dependency advice items: $($standards.dependencyAdviceBenchmarks)"
    $lines += ""
    $lines += "## Reports"
    $lines += ""
    $lines += "- Android detekt: $($reports.detektAndroid)"
    $lines += "- Core detekt: $($reports.detektCore)"
    $lines += "- Benchmarks detekt: $($reports.detektBenchmarks)"
    $lines += "- Android lint: $($reports.lintBenchmark)"
    $lines += "- Android dependency health: $($reports.projectHealthAndroid)"
    $lines += "- Core dependency health: $($reports.projectHealthCore)"
    $lines += "- Benchmarks dependency health: $($reports.projectHealthBenchmarks)"
    $lines += "- Runtime health JSON: $runtimeHealthJson"
    $lines += "- Build timings JSON: $buildBenchmarksJson"

    $lines | Set-Content -Path $summaryMd
    Write-Output "[benchmark-suite] summary=$summaryMd"
    Write-Output "[benchmark-suite] json=$summaryJson"
}
finally {
    Pop-Location
}
