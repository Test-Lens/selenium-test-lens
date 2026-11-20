param(
    [Parameter(Mandatory)][string]$ChromeBinary,
    [Parameter(Mandatory)][string]$ChromeDriver,
    [Parameter(Mandatory)][ValidateSet('157.0.8103.78', '158.0.8127.36')][string]$ExpectedChrome,
    [string]$RepositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '../../../..')).Path
)
$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false
$repo = [IO.Path]::GetFullPath($RepositoryRoot)
$work = Join-Path $repo 'target/controlled-http-work'
$evidenceRoot = Join-Path $repo 'target/controlled-http-evidence'
New-Item -ItemType Directory -Force $work, $evidenceRoot | Out-Null
$cases = @()
foreach ($hostName in @('127.0.0.1', 'localhost')) {
    foreach ($mode in @('A', 'B', 'C')) {
        $cases += [pscustomobject]@{ case = "$hostName-$mode"; hostName = $hostName; mode = $mode; httpsControl = $false; status = 'NOTRUN'; exitCode = $null; reason = 'Setup did not complete' }
    }
}
foreach ($control in @($false, $true)) {
    $cases += [pscustomobject]@{ case = "staging.example.org-A-httpsControl-$control"; hostName = 'staging.example.org'; mode = 'A'; httpsControl = $control; status = 'NOTRUN'; exitCode = $null; reason = 'Setup did not complete' }
}
function Save-Outcomes { ConvertTo-Json -InputObject @($cases) -Depth 5 | Set-Content (Join-Path $evidenceRoot 'outcomes.json') }
function Quote-Argument([string]$value) { '"' + $value.Replace('"', '\"') + '"' }
function Remove-OwnedDriverLog([string]$path) {
    $resolved = [IO.Path]::GetFullPath($path)
    $ownedRoot = [IO.Path]::GetFullPath($work) + [IO.Path]::DirectorySeparatorChar
    if (-not $resolved.StartsWith($ownedRoot, [StringComparison]::OrdinalIgnoreCase)) { throw 'Driver log is outside owned workspace' }
    if (Test-Path -LiteralPath $resolved) {
        $item = Get-Item -LiteralPath $resolved -Force
        if ($item.PSIsContainer -or ($item.Attributes -band [IO.FileAttributes]::ReparsePoint)) { throw 'Driver log must be a regular owned file' }
        Remove-Item -LiteralPath $resolved -Force
    }
}
function Run-BoundedJava([string[]]$JavaArguments, [string]$LogPrefix, [int]$TimeoutSeconds) {
    $start = @{
        FilePath = $java; ArgumentList = (($JavaArguments | ForEach-Object { Quote-Argument $_ }) -join ' ')
        PassThru = $true; RedirectStandardOutput = "$LogPrefix.stdout.log"; RedirectStandardError = "$LogPrefix.stderr.log"
    }
    if ($IsWindows) { $start.WindowStyle = 'Hidden' }
    $child = Start-Process @start
    try {
        if (-not $child.WaitForExit($TimeoutSeconds * 1000)) { $child.Kill($true); $child.WaitForExit(); return 124 }
        return $child.ExitCode
    } finally { $child.Dispose() }
}
Save-Outcomes
$grid = $null
try {
    $manifests = @(Get-ChildItem (Join-Path $repo 'target/controlled-frozen-download') -Recurse -Filter artifact-sha256.json)
    if ($manifests.Count -ne 1) { throw 'Expected exactly one artifact-sha256 manifest' }
    $manifest = Get-Content -LiteralPath $manifests[0].FullName -Raw | ConvertFrom-Json
    $frozen = Join-Path $manifests[0].Directory.FullName 'frozen'
    $hashes = [ordered]@{}
    foreach ($name in @('lens', 'core', 'overlay')) {
        $expected = [string]$manifest.$name
        if ($expected -notmatch '^[0-9a-fA-F]{64}$') { throw "Invalid manifest hash: $name" }
        $actual = (Get-FileHash -LiteralPath (Join-Path $frozen "$name.jar") -Algorithm SHA256).Hash
        if ($actual -ne $expected) { throw "Frozen hash mismatch: $name" }
        $hashes[$name] = $actual
    }
    if ($hashes.lens -ne 'DD1D2D55E6D99FE1C47E5D030A30C378AB8F75574DC17E7809E886BB12EC1EE6') { throw 'Unexpected frozen Lens binary' }
    $legacy = Join-Path $frozen 'legacy.jar'
    if (-not (Test-Path -LiteralPath $legacy)) { throw 'Frozen legacy JAR missing' }
    $hashes.legacy = (Get-FileHash -LiteralPath $legacy -Algorithm SHA256).Hash
    $hashes | ConvertTo-Json | Set-Content (Join-Path $evidenceRoot 'artifact-sha256.json')
    $driverVersion = (& $ChromeDriver --version).Split(' ')[1]
    if ($LASTEXITCODE -ne 0 -or $driverVersion -ne $ExpectedChrome) { throw 'Installed ChromeDriver version mismatch' }
    $ChromeBinary = (Resolve-Path -LiteralPath $ChromeBinary).Path
    $ChromeDriver = (Resolve-Path -LiteralPath $ChromeDriver).Path
    $java = Join-Path $env:JAVA_HOME ('bin/java' + $(if ($IsWindows) { '.exe' } else { '' }))
    $javac = Join-Path $env:JAVA_HOME ('bin/javac' + $(if ($IsWindows) { '.exe' } else { '' }))
    $classpathFile = Join-Path $work 'classpath.txt'
    & mvn -B -q -f (Join-Path $repo 'consumer-tests/selenium-bidi/pom.xml') org.apache.maven.plugins:maven-dependency-plugin:3.7.0:build-classpath '-Dselenium.version=4.53.0' "-Dfrozen.dir=$frozen" "-Dlegacy.jar=$legacy" "-Drun.dir=$work" "-Dmdep.outputFile=$classpathFile" *> (Join-Path $work 'classpath-build.log')
    if ($LASTEXITCODE -ne 0) { throw 'Dependency classpath resolution failed' }
    $fullCp = (Get-Content -LiteralPath $classpathFile -Raw).Trim()
    $separator = [IO.Path]::PathSeparator
    $excluded = @('lens', 'core', 'overlay', 'legacy') | ForEach-Object { [IO.Path]::GetFullPath((Join-Path $frozen "$_.jar")) }
    $rawEntries = @($fullCp.Split($separator) | Where-Object { [IO.Path]::GetFullPath($_) -notin $excluded })
    if (-not $rawEntries.Count -or @($rawEntries | Where-Object { $_ -match 'test-lens|[/\\]frozen[/\\]' }).Count) { throw 'Raw classpath isolation failed' }
    $rawCp = $rawEntries -join $separator
    $rawClasses = Join-Path $work 'raw-classes'
    $lensClasses = Join-Path $work 'lens-classes'
    New-Item -ItemType Directory -Force $rawClasses, $lensClasses | Out-Null
    & $javac --release 17 -cp $rawCp -d $rawClasses (Join-Path $PSScriptRoot 'LayerProbe.java') *> (Join-Path $work 'raw-compile.log')
    if ($LASTEXITCODE -ne 0) { throw 'Raw probe compilation failed' }
    & $javac --release 17 -cp "$rawClasses$separator$fullCp" -d $lensClasses (Join-Path $PSScriptRoot 'LensProbeBridge.java') *> (Join-Path $work 'bridge-compile.log')
    if ($LASTEXITCODE -ne 0) { throw 'Lens bridge compilation failed' }
    $sourceHashes = [ordered]@{}
    foreach ($name in @('LayerProbe.java', 'LensProbeBridge.java', 'run-controlled-probes.ps1', 'scrub-controlled-cdp.mjs')) {
        $sourceFile = Join-Path $PSScriptRoot $name
        if (Test-Path -LiteralPath $sourceFile) { $sourceHashes[$name] = (Get-FileHash -LiteralPath $sourceFile -Algorithm SHA256).Hash }
    }
    $classHashes = [ordered]@{}
    foreach ($classRoot in @($rawClasses, $lensClasses)) {
        Get-ChildItem -LiteralPath $classRoot -Filter '*.class' -Recurse | ForEach-Object {
            $classHashes[$_.FullName.Substring($work.Length + 1).Replace('\', '/')] = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash
        }
    }
    @{ sources = $sourceHashes; compiledClasses = $classHashes } | ConvertTo-Json -Depth 5 | Set-Content (Join-Path $evidenceRoot 'probe-sha256.json')
    $gridDriverLog = Join-Path $work 'grid-chromedriver.log'
    $ready = $false
    try {
        $server = Join-Path $work 'selenium-server-4.53.0.jar'
        Invoke-WebRequest 'https://github.com/SeleniumHQ/selenium/releases/download/selenium-4.53.0/selenium-server-4.53.0.jar' -OutFile $server -TimeoutSec 120
        $gridStart = @{
            FilePath = $java
            ArgumentList = ((@("-Dwebdriver.chrome.driver=$ChromeDriver", '-Dwebdriver.chrome.verboseLogging=true', "-Dwebdriver.chrome.logfile=$gridDriverLog", '-jar', $server, 'standalone', '--host', '127.0.0.1', '--port', '4444', '--selenium-manager', 'false') | ForEach-Object { Quote-Argument $_ }) -join ' ')
            PassThru = $true; RedirectStandardOutput = (Join-Path $work 'grid.stdout.log'); RedirectStandardError = (Join-Path $work 'grid.stderr.log')
        }
        if ($IsWindows) { $gridStart.WindowStyle = 'Hidden' }
        $grid = Start-Process @gridStart
        $ready = $false
        $deadline = [DateTime]::UtcNow.AddSeconds(60)
        while ([DateTime]::UtcNow -lt $deadline) {
            if ($grid.HasExited) { break }
            try { $ready = (Invoke-RestMethod 'http://127.0.0.1:4444/status' -TimeoutSec 2).value.ready } catch { }
            if ($ready) { break }
            # Polling is limited to owned Grid startup; failed probe cases are never retried.
            Start-Sleep -Milliseconds 500
        }
    } catch {
        # Grid startup failure leaves C as NOTRUN; independent direct cases still run.
        $ready = $false
    }
    foreach ($case in $cases) {
        if ($case.mode -eq 'C' -and -not $ready) { $case.reason = 'Owned Grid did not become ready'; continue }
        $baseDisabledFeatures = $null
        if ($case.httpsControl) {
            # Feature provenance is useful even when the paired default navigation failed.
            $defaultEvidence = Join-Path $work 'staging.example.org-A-httpsControl-False/evidence.json'
            try {
                $baseline = Get-Content -LiteralPath $defaultEvidence -Raw | ConvertFrom-Json
                if ($baseline.featureCommandLineOutcome -ne 'CAPTURED' -or $baseline.actualDisabledFeatures -isnot [string]) {
                    throw 'Paired feature command line was not captured unambiguously'
                }
                $baseDisabledFeatures = $baseline.actualDisabledFeatures
            } catch {
                $case.reason = 'Paired default public A feature command line unavailable or ambiguous'
                Save-Outcomes
                continue
            }
        }
        $caseWork = Join-Path $work $case.case
        $caseEvidence = Join-Path $evidenceRoot $case.case
        New-Item -ItemType Directory -Force $caseWork, $caseEvidence | Out-Null
        $cp = if ($case.mode -eq 'A') { "$rawClasses$separator$rawCp" } else { "$rawClasses$separator$lensClasses$separator$fullCp" }
        $arguments = @("-Dchrome.binary=$ChromeBinary", "-Dwebdriver.chrome.driver=$ChromeDriver", "-Dprobe.host=$($case.hostName)", "-Dprobe.mode=$($case.mode)", "-Dprobe.output=$caseWork", "-Dprobe.httpsControl=$($case.httpsControl.ToString().ToLowerInvariant())", '-Dprobe.stimuli=50', '-Dprobe.headed=false', '-cp', $cp, 'LayerProbe')
        if ($case.mode -eq 'C') { $arguments = @('-Dprobe.grid=http://127.0.0.1:4444') + $arguments }
        if ($case.httpsControl) { $arguments = @("-Dprobe.baseDisabledFeatures=$baseDisabledFeatures") + $arguments }
        $case.reason = ''
        $driverLog = if ($case.mode -eq 'C') { $gridDriverLog } else { Join-Path $caseWork 'chromedriver.log' }
        $cdpInput = Join-Path $caseWork 'driver-log-not-created.log'
        try {
            Remove-OwnedDriverLog $driverLog
            $cdpInput = $driverLog
            if ($case.mode -ne 'C') { $arguments = @('-Dwebdriver.chrome.verboseLogging=true', "-Dwebdriver.chrome.logfile=$driverLog") + $arguments }
            $case.exitCode = Run-BoundedJava $arguments (Join-Path $caseWork 'java') 180
            $case.status = 'FAIL'
            $evidencePath = Join-Path $caseWork 'evidence.json'
            if (Test-Path -LiteralPath $evidencePath) {
                $actual = Get-Content -LiteralPath $evidencePath -Raw | ConvertFrom-Json
                $actualDriver = ([string]$actual.chromeDriverVersion).Split(' ')[0]
                if ($case.exitCode -eq 0 -and $actual.browserVersion -eq $ExpectedChrome -and $actualDriver -eq $ExpectedChrome -and $actual.seleniumVersion -eq '4.53.0' -and $actual.outcome -eq 'PASS') { $case.status = 'PASS' }
            }
            foreach ($fileName in @('evidence.json', 'lens-export.json')) {
                $file = Join-Path $caseWork $fileName
                if (Test-Path -LiteralPath $file) {
                    if ((Get-Item -LiteralPath $file).Length -gt 10MB) { $case.status = 'FAIL'; continue }
                    Copy-Item -LiteralPath $file -Destination (Join-Path $caseEvidence $fileName)
                }
            }
        } catch { $case.status = 'FAIL'; if ($null -eq $case.exitCode) { $case.exitCode = -1 } }
        # Scrub immediately: Grid may overwrite this private service log in its next session.
        & node (Join-Path $PSScriptRoot 'scrub-controlled-cdp.mjs') --input $cdpInput --output (Join-Path $caseEvidence 'cdp.json') *> (Join-Path $caseWork 'scrubber.log')
        if ($LASTEXITCODE -ne 0) { $case.status = 'FAIL'; $case.reason = 'CDP log scrubber failed' }
        # Raw Java/Grid streams stay outside uploaded evidence: no headers or cookies in artifacts.
        @{ case = $case.case; status = $case.status; exitCode = $case.exitCode; reason = $case.reason } | ConvertTo-Json | Set-Content (Join-Path $caseEvidence 'process.json')
        Save-Outcomes
    }
} finally {
    if ($null -ne $grid) {
        try { if (-not $grid.HasExited) { $grid.Kill($true); $grid.WaitForExit() } } finally { $grid.Dispose() }
    }
    Save-Outcomes
}
if (@($cases | Where-Object status -ne 'PASS').Count) { throw 'Controlled diagnostic contains FAIL or NOTRUN cases; see outcomes.json' }
