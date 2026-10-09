Set-StrictMode -Version Latest

$script:LinuxUnixSocketPathBytes = 108
$script:LinuxChromeSocketSafetyMarginBytes = 16
$script:MaximumLinuxChromeTempRootBytes = 48
$script:CertificationOwnershipMarker = ".test-lens-certification-owned"

function Get-TestLensCertificationTempRootPath {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)][string]$RunnerTemp,
        [Parameter(Mandatory)][bool]$Linux,
        [string]$RandomId = ([guid]::NewGuid().ToString("N"))
    )

    if ([string]::IsNullOrWhiteSpace($RunnerTemp)) { throw "Runner temp root must not be blank" }
    if ($RandomId -notmatch '^[a-fA-F0-9]{8,32}$') { throw "Certification temp random id must contain 8-32 hexadecimal characters" }
    if (-not $Linux) { return Join-Path $RunnerTemp ("test-lens-s15-temp-" + $RandomId) }

    # Linux sockaddr_un.sun_path is 108 bytes. Reserve space for Chromium's
    # org.chromium.Chromium.<random>/SingletonSocket subtree plus a 16-byte margin.
    $path = "/tmp/tl-" + $RandomId.Substring(0, 8).ToLowerInvariant()
    $rootBytes = [Text.Encoding]::UTF8.GetByteCount($path)
    $projected = $path + "/org.chromium.Chromium." + ("x" * 16) + "/SingletonSocket"
    $projectedBytes = [Text.Encoding]::UTF8.GetByteCount($projected)
    $safeSocketBudget = $script:LinuxUnixSocketPathBytes - $script:LinuxChromeSocketSafetyMarginBytes
    if ($rootBytes -gt $script:MaximumLinuxChromeTempRootBytes -or $projectedBytes -gt $safeSocketBudget) {
        throw "Linux certification temp root exceeds the Chrome Unix-socket path budget; rootBytes=$rootBytes; projectedSocketBytes=$projectedBytes; safeSocketBudget=$safeSocketBudget"
    }
    return $path
}

function New-TestLensCertificationTempRoot {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)][string]$RunnerTemp,
        [Parameter(Mandatory)][bool]$Linux
    )

    for ($attempt = 0; $attempt -lt 5; $attempt++) {
        $path = Get-TestLensCertificationTempRootPath -RunnerTemp $RunnerTemp -Linux $Linux
        if (Test-Path -LiteralPath $path) { continue }
        [IO.Directory]::CreateDirectory($path) | Out-Null
        $item = Get-Item -LiteralPath $path -Force
        if ($item.LinkType) { throw "Certification temp root must not be a symbolic link" }
        if ($Linux) {
            & chmod 700 -- $path
            if ($LASTEXITCODE -ne 0) { throw "Could not restrict Linux certification temp-root permissions" }
        }
        [IO.File]::WriteAllText((Join-Path $path $script:CertificationOwnershipMarker), "owned")
        $probe = Join-Path $path "write-probe"
        [IO.File]::WriteAllText($probe, "ok")
        Remove-Item -LiteralPath $probe -Force
        return (Resolve-Path -LiteralPath $path).Path
    }
    throw "Could not allocate a unique certification temp root"
}

function Remove-TestLensCertificationTempRoot {
    [CmdletBinding()]
    param([Parameter(Mandatory)][string]$Path)

    if (-not (Test-Path -LiteralPath $Path -PathType Container)) { return }
    $item = Get-Item -LiteralPath $Path -Force
    if ($item.LinkType) { throw "Refusing to remove a symlinked certification temp root" }
    if (-not (Test-Path -LiteralPath (Join-Path $item.FullName $script:CertificationOwnershipMarker) -PathType Leaf)) {
        throw "Refusing to remove a temp root without the certification ownership marker"
    }
    Remove-Item -LiteralPath $item.FullName -Recurse -Force -ErrorAction SilentlyContinue
}

function Wait-TestLensStudioActionAvailable {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)]
        [string]$Action,

        [Parameter(Mandatory)]
        [scriptblock]$ReadState,

        [TimeSpan]$Timeout = [TimeSpan]::FromSeconds(30),

        [ValidateRange(1, 1000)]
        [int]$PollMilliseconds = 100
    )

    if ([string]::IsNullOrWhiteSpace($Action)) {
        throw "Action must not be blank"
    }
    if ($Timeout -le [TimeSpan]::Zero) {
        throw "Timeout must be positive"
    }

    $deadline = [DateTimeOffset]::UtcNow.Add($Timeout)
    $lastRunning = $null
    $lastActions = @()
    do {
        $state = & $ReadState
        if ($null -eq $state) {
            throw "Studio readiness state reader returned no state"
        }
        $lastRunning = [bool]$state.OperationRunning
        $lastActions = @($state.AvailableActions | Where-Object { $null -ne $_ } | ForEach-Object { [string]$_ })
        if (-not $lastRunning -and $lastActions -contains $Action) {
            return $state
        }

        $remaining = $deadline - [DateTimeOffset]::UtcNow
        if ($remaining -le [TimeSpan]::Zero) { break }
        $delay = [Math]::Min($PollMilliseconds, [Math]::Max(1, [int][Math]::Ceiling($remaining.TotalMilliseconds)))
        Start-Sleep -Milliseconds $delay
    } while ([DateTimeOffset]::UtcNow -lt $deadline)

    $actions = if ($lastActions.Count -eq 0) { "<none>" } else { [string]::Join(",", $lastActions) }
    throw "Timed out waiting for Studio action '$Action'; operationRunning=$lastRunning; availableActions=$actions; timeoutMs=$([long]$Timeout.TotalMilliseconds)"
}

function Protect-TestLensDiagnosticText {
    [CmdletBinding()]
    param(
        [AllowNull()][string]$Text,
        [ValidateRange(256, 32768)][int]$MaximumCharacters = 8192
    )

    if ([string]::IsNullOrEmpty($Text)) { return "<empty>" }
    $safe = $Text
    $safe = $safe -replace '(?i)(authorization|cookie|password|secret|token|api[_-]?key)(\s*[:=]\s*)[^\s,;]+', '$1$2[REDACTED]'
    $safe = $safe -replace 'eyJ[a-zA-Z0-9_-]{10,}\.[a-zA-Z0-9_-]{10,}\.[a-zA-Z0-9_-]{10,}', '[REDACTED_JWT]'
    if ($safe.Length -le $MaximumCharacters) { return $safe }
    return "...[tail truncated]" + $safe.Substring($safe.Length - $MaximumCharacters)
}

function Get-TestLensChromeDriverDiagnosticTail {
    [CmdletBinding()]
    param(
        [string]$Directory = $env:SE_BROWSER_DIAGNOSTICS_DIR,
        [ValidateRange(256, 32768)][int]$MaximumCharacters = 8192
    )

    if ([string]::IsNullOrWhiteSpace($Directory) -or -not (Test-Path -LiteralPath $Directory -PathType Container)) {
        return "ChromeDriver diagnostic log is unavailable"
    }
    $log = Get-ChildItem -LiteralPath $Directory -Filter 'chromedriver-*.log' -File -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTimeUtc -Descending | Select-Object -First 1
    if ($null -eq $log) { return "ChromeDriver diagnostic log was not created" }
    $tail = Protect-TestLensDiagnosticText -Text ([IO.File]::ReadAllText($log.FullName)) -MaximumCharacters $MaximumCharacters
    return "ChromeDriver verbose log tail ($($log.Name)):`n$tail"
}

function Invoke-TestLensChromeStartupSmoke {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)][string]$BrowserBinary,
        [Parameter(Mandatory)][string]$TempRoot,
        [switch]$NoSandbox,
        [TimeSpan]$Timeout = [TimeSpan]::FromSeconds(20)
    )

    $binary = (Resolve-Path -LiteralPath $BrowserBinary -ErrorAction Stop).Path
    if (-not (Test-Path -LiteralPath $binary -PathType Leaf)) { throw "Configured Chrome binary is not a file" }
    [IO.Directory]::CreateDirectory($TempRoot) | Out-Null
    $rootItem = Get-Item -LiteralPath $TempRoot -Force
    if ($rootItem.LinkType) { throw "Chrome smoke temp root must not be a symbolic link" }
    $root = (Resolve-Path -LiteralPath $TempRoot).Path
    $writeProbe = Join-Path $root ("write-probe-" + [guid]::NewGuid().ToString("N"))
    [IO.File]::WriteAllText($writeProbe, "ok")
    Remove-Item -LiteralPath $writeProbe -Force

    $profile = Join-Path $root ("chrome-profile-" + [guid]::NewGuid().ToString("N"))
    [IO.Directory]::CreateDirectory($profile) | Out-Null
    $resolvedProfile = (Resolve-Path -LiteralPath $profile).Path
    $rootPrefix = $root.TrimEnd([IO.Path]::DirectorySeparatorChar, [IO.Path]::AltDirectorySeparatorChar) + [IO.Path]::DirectorySeparatorChar
    if (-not $resolvedProfile.StartsWith($rootPrefix, [StringComparison]::Ordinal)) {
        throw "Chrome smoke profile escaped the controlled temp root"
    }

    $process = $null
    try {
        $version = Protect-TestLensDiagnosticText -Text ([string](& $binary --version 2>&1)) -MaximumCharacters 1024
        $start = [Diagnostics.ProcessStartInfo]::new()
        $start.FileName = $binary
        $start.UseShellExecute = $false
        $start.RedirectStandardOutput = $true
        $start.RedirectStandardError = $true
        $start.CreateNoWindow = $true
        $start.ArgumentList.Add("--headless=new")
        if ($NoSandbox) { $start.ArgumentList.Add("--no-sandbox") }
        $start.ArgumentList.Add("--user-data-dir=$resolvedProfile")
        $start.ArgumentList.Add("--dump-dom")
        $start.ArgumentList.Add("about:blank")

        $process = [Diagnostics.Process]::new()
        $process.StartInfo = $start
        if (-not $process.Start()) { throw "Configured Chrome process did not start" }
        $stdoutTask = $process.StandardOutput.ReadToEndAsync()
        $stderrTask = $process.StandardError.ReadToEndAsync()
        if (-not $process.WaitForExit([int]$Timeout.TotalMilliseconds)) {
            try { $process.Kill($true) } catch { Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue }
            $process.WaitForExit()
            throw "Direct Chrome smoke timed out after $([long]$Timeout.TotalMilliseconds) ms"
        }
        $stdout = Protect-TestLensDiagnosticText -Text $stdoutTask.GetAwaiter().GetResult() -MaximumCharacters 2048
        $stderr = Protect-TestLensDiagnosticText -Text $stderrTask.GetAwaiter().GetResult() -MaximumCharacters 8192
        if ($process.ExitCode -ne 0) {
            $runningOnLinux = [bool](Get-Variable -Name IsLinux -ValueOnly -ErrorAction SilentlyContinue)
            $missingLibraries = @()
            if ($runningOnLinux -and (Get-Command ldd -ErrorAction SilentlyContinue)) {
                $missingLibraries = @(& ldd $binary 2>&1 | Where-Object { "$_" -match 'not found' })
            }
            $sharedMemory = if ($runningOnLinux -and (Test-Path -LiteralPath '/dev/shm')) {
                Protect-TestLensDiagnosticText -Text ([string](& df -h /dev/shm 2>&1)) -MaximumCharacters 2048
            } else { "<not available>" }
            $missing = if ($missingLibraries.Count -eq 0) { "<none reported>" } else { [string]::Join("; ", $missingLibraries) }
            throw "Direct Chrome smoke failed; binary=$binary; version=$version; exitCode=$($process.ExitCode); stderr=$stderr; missingLibraries=$missing; devShm=$sharedMemory"
        }
        Write-Host "Direct Chrome smoke PASS"
        Write-Host "Chrome binary: $binary"
        Write-Host "Chrome version: $version"
        Write-Host "Chrome flags: --headless=new$(if ($NoSandbox) { ' --no-sandbox' })"
        Write-Host "Chrome profile: controlled temporary directory (writable, non-symlink)"
        if ($stderr -ne "<empty>") { Write-Host "Chrome bounded stderr: $stderr" }
        return [pscustomobject]@{ Binary = $binary; Version = $version; ExitCode = $process.ExitCode; StandardOutput = $stdout; StandardError = $stderr }
    } finally {
        if ($null -ne $process) { $process.Dispose() }
        Remove-Item -LiteralPath $profile -Recurse -Force -ErrorAction SilentlyContinue
    }
}

Export-ModuleMember -Function Get-TestLensCertificationTempRootPath, New-TestLensCertificationTempRoot, Remove-TestLensCertificationTempRoot, Wait-TestLensStudioActionAvailable, Protect-TestLensDiagnosticText, Get-TestLensChromeDriverDiagnosticTail, Invoke-TestLensChromeStartupSmoke
