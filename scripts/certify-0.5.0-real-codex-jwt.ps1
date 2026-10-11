[CmdletBinding()]
param(
    [string]$Version = "0.5.0",
    [Parameter(Mandatory = $true)]
    [string]$CodexExecutable,
    [switch]$RunRealCodex,
    [switch]$PreflightOnly,
    [switch]$Firefox
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Import-Module (Join-Path $PSScriptRoot "StudioCertificationSupport.psm1") -Force
Import-Module (Join-Path $PSScriptRoot "StudioCertificationHttp.psm1") -Force
$stages = [System.Collections.Generic.List[object]]::new()
$modelCalls = @()
$failureDiagnostic = $null
$reviewEvidence = $null
$failedStage = $null
$status = "FAILED"
$runRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("test-lens-real-codex-cert-" + [guid]::NewGuid().ToString("N"))
$consumer = Join-Path $runRoot "consumer"
$mavenRepository = Join-Path $runRoot "m2"
$agentWorkspace = Join-Path $runRoot "agent-workspace"
$metricsFile = Join-Path $runRoot "agent-metrics.jsonl"
$fixture = $null
$studio = $null
$studioSession = $null
$savedEnvironment = @{}

function Invoke-MeasuredStage([string]$Name, [scriptblock]$Operation) {
    $script:failedStage = $Name
    $watch = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        $result = & $Operation
        $script:stages.Add([pscustomobject]@{ name = $Name; status = "PASS"; durationMillis = $watch.ElapsedMilliseconds })
        return $result
    } catch {
        $script:stages.Add([pscustomobject]@{ name = $Name; status = "FAILED"; durationMillis = $watch.ElapsedMilliseconds })
        throw
    } finally {
        $watch.Stop()
    }
}

function Test-WindowsFullyQualifiedPath([string]$Path) {
    return -not [string]::IsNullOrWhiteSpace($Path) -and
        $Path -match '^(?:[A-Za-z]:[\\/]|\\\\[^\\/]+[\\/][^\\/]+(?:[\\/]|$))'
}

function Stop-HarnessProcessTree($Process) {
    if ($null -eq $Process -or $Process.HasExited) { return }
    if ($env:OS -eq "Windows_NT") {
        & taskkill.exe /PID $Process.Id /T /F 2>$null | Out-Null
    } else {
        Stop-Process -Id $Process.Id -Force -ErrorAction SilentlyContinue
    }
}

function Start-StudioHost {
    $stdout = Join-Path $runRoot "studio.out.log"
    $stderr = Join-Path $runRoot "studio.err.log"
    $arguments = @("-Dmaven.repo.local=$mavenRepository", "-DtestLens.studio.openBrowser=false",
        "io.github.test-lens:test-lens-test-engineering-maven-plugin:${Version}:studio")
    $process = Start-Process -FilePath "mvn.cmd" -ArgumentList $arguments -WorkingDirectory $consumer `
        -RedirectStandardOutput $stdout -RedirectStandardError $stderr -WindowStyle Hidden -PassThru
    $deadline = [DateTime]::UtcNow.AddMinutes(3)
    do {
        Start-Sleep -Milliseconds 250
        if ($process.HasExited) { throw "Studio exited before startup" }
        $output = if (Test-Path -LiteralPath $stdout) { [string](Get-Content -LiteralPath $stdout -Raw -ErrorAction SilentlyContinue) } else { "" }
        $match = [regex]::Match($output, 'Test Engineering Studio:\s+(http://127\.0\.0\.1:\d+/)')
    } while (-not $match.Success -and [DateTime]::UtcNow -lt $deadline)
    if (-not $match.Success) { Stop-HarnessProcessTree $process; throw "Studio startup timed out" }
    return @{ Process = $process; Uri = $match.Groups[1].Value }
}

function Get-ExpectedAgentRole([string]$Action) {
    if ($Action -eq "GENERATE_PLAN") { return "TEST_ARCHITECT" }
    if ($Action -eq "GENERATE_IMPLEMENTATION") { return "TEST_IMPLEMENTER" }
    if ($Action -eq "RUN") { return "CODE_REVIEWER" }
    return "UNKNOWN"
}

function Get-LatestSafeAgentMetric([string]$Role) {
    if ($Role -eq "UNKNOWN" -or -not (Test-Path -LiteralPath $metricsFile -PathType Leaf)) { return $null }
    try {
        $values = @(Get-Content -LiteralPath $metricsFile | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } |
            ForEach-Object { $_ | ConvertFrom-Json -ErrorAction Stop } |
            Where-Object { $_.role -eq $Role })
        if ($values.Count -eq 0) { return $null }
        $value = $values[$values.Count - 1]
        if ($value.status -notin @("SUCCESS", "FAILED") -or [long]$value.inputBytes -lt 0 -or
                [long]$value.outputBytes -lt 0 -or [long]$value.durationMillis -lt 0) { return $null }
        return [pscustomobject]@{
            status = [string]$value.status
            inputBytes = [long]$value.inputBytes
            outputBytes = [long]$value.outputBytes
            durationMillis = [long]$value.durationMillis
        }
    } catch { return $null }
}

function Get-SafeWorkflowState($Value) {
    $known = @("CREATED", "PLAN_GENERATING", "PLAN_READY_FOR_REVIEW", "IMPLEMENTATION_GENERATING",
        "IMPLEMENTATION_READY_FOR_REVIEW", "EXECUTION_RUNNING", "VERIFICATION_RUNNING", "EXECUTION_FAILED",
        "REPAIR_READY_FOR_REVIEW", "REPAIR_REJECTED", "REPAIR_APPLIED_VERIFICATION_PENDING", "SUCCESS", "FAILED", "NEEDS_HUMAN_REVIEW",
        "AGENT_FAILED", "AGENT_EXECUTION_INTERRUPTED", "EXECUTION_INTERRUPTED", "CORRUPTED")
    if ($Value -is [string] -and $known -contains $Value) { return $Value }
    return "UNKNOWN"
}

function Get-SafeCorrelationId($Value) {
    if ($Value -is [string] -and $Value -match '^[A-Za-z0-9._-]{1,160}$') { return $Value }
    return "UNKNOWN"
}

function Invoke-StudioAction([string]$Origin, [string]$Token, [hashtable]$Body) {
    $action = if ($Body.action -is [string] -and $Body.action -match '^[A-Z_]{1,64}$') {
        [string]$Body.action
    } else { "UNKNOWN" }
    $response = Invoke-StudioCertificationJsonRequest -Method POST -Uri ($Origin + "api/actions") `
        -WebSession $script:studioSession -Headers @{ Origin = $Origin.TrimEnd('/'); "X-Test-Lens-Session" = $Token } `
        -Body $Body -TimeoutSec 900 -Stage $action -Provider "codex-cli"
    if ($response.success) { return $response.payload }

    $runIdValue = if ($Body.ContainsKey("runId")) { $Body["runId"] } else { $null }
    $runId = Get-SafeCorrelationId $runIdValue
    $workflowState = "UNKNOWN"
    $safeError = $response.error
    if ($runId -ne "UNKNOWN") {
        $workflowResponse = Invoke-StudioCertificationJsonRequest -Method GET `
            -Uri ($Origin + "api/workflow?runId=" + [uri]::EscapeDataString($runId)) `
            -WebSession $script:studioSession -TimeoutSec 15 -Stage "WORKFLOW_DIAGNOSTIC" -Provider "studio"
        if ($workflowResponse.success) {
            $workflowState = Get-SafeWorkflowState $workflowResponse.payload.status
            if ($safeError.code -eq "UNKNOWN" -and $null -ne $workflowResponse.payload.agentFailure) {
                $safeError = ConvertTo-StudioCertificationDiagnostic -HttpStatus $response.httpStatus `
                    -Body $workflowResponse.payload.agentFailure -Stage $action -Provider "codex-cli"
            }
        }
    }
    $expectedRole = Get-ExpectedAgentRole $action
    $metric = Get-LatestSafeAgentMetric $expectedRole
    $agentStatus = "UNKNOWN"
    if ($null -ne $metric) {
        $agentStatus = $metric.status
        if ([long]$safeError.inputBytes -eq 0) { $safeError.inputBytes = $metric.inputBytes }
        if ([long]$safeError.outputBytes -eq 0) { $safeError.outputBytes = $metric.outputBytes }
        if ([long]$safeError.durationMillis -eq 0) { $safeError.durationMillis = $metric.durationMillis }
    }
    $role = if ($safeError.role -eq "UNKNOWN") { $expectedRole } else { $safeError.role }
    $script:failureDiagnostic = [pscustomobject][ordered]@{
        stage = $action
        method = $response.method
        httpStatus = [int]$response.httpStatus
        code = $safeError.code
        reasonCode = $safeError.reasonCode
        category = $safeError.category
        location = $safeError.location
        role = $role
        provider = $safeError.provider
        agentStatus = $agentStatus
        processStatus = $safeError.processStatus
        exitCode = [int]$safeError.exitCode
        durationMillis = [long]$safeError.durationMillis
        inputBytes = [long]$safeError.inputBytes
        outputBytes = [long]$safeError.outputBytes
        capabilityCategory = $safeError.capabilityCategory
        sourceFingerprintMatched = [bool]$safeError.sourceFingerprintMatched
        validationStage = $safeError.validationStage
        ruleId = $safeError.ruleId
        problemCategory = $safeError.problemCategory
        astNodeKind = $safeError.astNodeKind
        receiverCategory = $safeError.receiverCategory
        manifestMatch = $safeError.manifestMatch
        fingerprintStatus = $safeError.fingerprintStatus
        signatureStatus = $safeError.signatureStatus
        workflowState = $workflowState
        correlationId = $runId
    }
    throw "Studio action failed with a sanitized diagnostic"
}

function Wait-ProjectAction([string]$Origin, [string]$Action, [TimeSpan]$Timeout = ([TimeSpan]::FromSeconds(30))) {
    $expected = $Action.ToLowerInvariant().Replace('_', '-')
    Wait-TestLensStudioActionAvailable -Action $Action -Timeout $Timeout -ReadState {
        $hostStatus = Invoke-RestMethod -Uri ($Origin + "api/status") -WebSession $script:studioSession
        $project = Invoke-RestMethod -Uri ($Origin + "api/project") -WebSession $script:studioSession
        [pscustomobject]@{
            OperationRunning = [bool]$hostStatus.operationRunning
            AvailableActions = @($project.stage.actions | Where-Object { $_.enabled -and $_.id -eq $expected } | ForEach-Object { $Action })
        }
    } | Out-Null
}

function Wait-WorkflowAction([string]$Origin, [string]$RunId, [string]$Action, [TimeSpan]$Timeout = ([TimeSpan]::FromSeconds(30))) {
    Wait-TestLensStudioActionAvailable -Action $Action -Timeout $Timeout -ReadState {
        $hostStatus = Invoke-RestMethod -Uri ($Origin + "api/status") -WebSession $script:studioSession
        $workflow = Invoke-RestMethod -Uri ($Origin + "api/workflow?runId=" + [uri]::EscapeDataString($RunId)) -WebSession $script:studioSession
        [pscustomobject]@{
            OperationRunning = [bool]$hostStatus.operationRunning
            AvailableActions = @($workflow.availableActions)
            WorkflowId = $workflow.runId
            WorkflowState = $workflow.status
        }
    } | Out-Null
}

function Remove-CertificationTempRoot([string]$Path) {
    if ([string]::IsNullOrWhiteSpace($Path) -or -not (Test-Path -LiteralPath $Path)) { return }
    $resolved = (Resolve-Path -LiteralPath $Path).Path
    $temporary = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath()).TrimEnd('\', '/')
    $parent = [System.IO.Path]::GetDirectoryName($resolved).TrimEnd('\', '/')
    if ($parent -ne $temporary -or [System.IO.Path]::GetFileName($resolved) -notlike "test-lens-real-codex-cert-*") {
        throw "Refusing to remove an unexpected path"
    }
    Remove-Item -LiteralPath $resolved -Recurse -Force
}

try {
    if (-not $RunRealCodex -and -not $PreflightOnly) {
        throw "Explicit -RunRealCodex opt-in or -PreflightOnly is required"
    }
    Invoke-MeasuredStage "environment-preflight" {
        $rootPom = [xml](Get-Content -Raw (Join-Path $repoRoot "pom.xml"))
        if ($Version -ne [string]$rootPom.project.version) { throw "Requested version does not match the reactor" }
        if (-not (Test-WindowsFullyQualifiedPath "C:\tools\codex.cmd") -or
                -not (Test-WindowsFullyQualifiedPath "\\server\share\codex.cmd") -or
                (Test-WindowsFullyQualifiedPath "tools\codex.cmd") -or
                (Test-WindowsFullyQualifiedPath "C:codex.cmd") -or
                (Test-WindowsFullyQualifiedPath "\tools\codex.cmd")) {
            throw "Windows path compatibility contract failed"
        }
        if (-not (Test-WindowsFullyQualifiedPath $CodexExecutable)) {
            throw "CodexExecutable must be an absolute path"
        }
        $script:CodexExecutable = [System.IO.Path]::GetFullPath($CodexExecutable)
        if (-not (Test-Path -LiteralPath $script:CodexExecutable -PathType Leaf)) {
            throw "CodexExecutable is unavailable"
        }
        if ($env:OS -eq "Windows_NT" -and
                -not [System.IO.Path]::GetExtension($script:CodexExecutable).Equals(".exe", [StringComparison]::OrdinalIgnoreCase)) {
            throw "CodexExecutable must be a native .exe compatible with Java ProcessBuilder"
        }
        if ($null -eq (Get-Command "mvn.cmd" -ErrorAction SilentlyContinue) -or
                $null -eq (Get-Command "java.exe" -ErrorAction SilentlyContinue)) {
            throw "Required Java or Maven executable is unavailable"
        }
        $probeDirectory = Join-Path $runRoot "preflight-probe"
        [System.IO.Directory]::CreateDirectory($probeDirectory) | Out-Null
        $probeFile = Join-Path $probeDirectory "utf8.txt"
        [System.IO.File]::WriteAllText($probeFile, "ok", [System.Text.UTF8Encoding]::new($false))
        if ([System.IO.File]::ReadAllText($probeFile) -ne "ok" -or
                [uri]::EscapeDataString("run id") -ne "run%20id") {
            throw ".NET file or URI compatibility probe failed"
        }
        $probeListener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, 0)
        try {
            $probeListener.Start()
            if (([System.Net.IPEndPoint]$probeListener.LocalEndpoint).Port -le 0) {
                throw ".NET socket compatibility probe failed"
            }
        } finally {
            $probeListener.Stop()
        }
        $null = & $script:CodexExecutable --version 2>$null
        if ($LASTEXITCODE -ne 0) { throw "CodexExecutable version check failed" }
    }
    if ($PreflightOnly) {
        $status = "PASS"
        $failedStage = $null
        return
    }

    [System.IO.Directory]::CreateDirectory($consumer) | Out-Null
    [System.IO.Directory]::CreateDirectory($mavenRepository) | Out-Null
    [System.IO.Directory]::CreateDirectory($agentWorkspace) | Out-Null
    Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot "fixtures/s15-external-consumer") -Force | ForEach-Object {
        Copy-Item -LiteralPath $_.FullName -Destination $consumer -Recurse
    }
    $consumerPom = Join-Path $consumer "pom.xml"
    [System.IO.File]::WriteAllText($consumerPom,
        [System.IO.File]::ReadAllText($consumerPom).Replace("0.5.0", $Version), [System.Text.UTF8Encoding]::new($false))
    $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, 0)
    $listener.Start(); $fixturePort = ([System.Net.IPEndPoint]$listener.LocalEndpoint).Port; $listener.Stop()
    $projectConfig = Join-Path $consumer ".test-lens/project.json"
    $configText = [System.IO.File]::ReadAllText($projectConfig).Replace("127.0.0.1:18181", "127.0.0.1:$fixturePort")
    if ($Firefox) { $configText = $configText.Replace('"name": "CHROME"', '"name": "FIREFOX"') }
    [System.IO.File]::WriteAllText($projectConfig, $configText, [System.Text.UTF8Encoding]::new($false))

    Invoke-MeasuredStage "stage-current-build" {
        & mvn.cmd "-Dmaven.repo.local=$mavenRepository" -DskipTests install *> (Join-Path $runRoot "stage.log")
        if ($LASTEXITCODE -ne 0) { throw "Current build staging failed" }
    }
    Invoke-MeasuredStage "compile-consumer" {
        & mvn.cmd "-Dmaven.repo.local=$mavenRepository" -f $consumerPom test-compile *> (Join-Path $runRoot "compile.log")
        if ($LASTEXITCODE -ne 0) { throw "External fixture compilation failed" }
    }

    foreach ($name in @("TEST_LENS_CERTIFICATION_REAL_CODEX", "TEST_LENS_CERTIFICATION_CODEX_EXECUTABLE",
            "TEST_LENS_CERTIFICATION_METRICS_FILE", "TEST_LENS_CERTIFICATION_STAGING_ROOT")) {
        $savedEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, "Process")
    }
    $env:TEST_LENS_CERTIFICATION_REAL_CODEX = "1"
    $env:TEST_LENS_CERTIFICATION_CODEX_EXECUTABLE = $CodexExecutable
    $env:TEST_LENS_CERTIFICATION_METRICS_FILE = $metricsFile
    $env:TEST_LENS_CERTIFICATION_STAGING_ROOT = $agentWorkspace

    Invoke-MeasuredStage "start-fixture" {
        $script:fixture = Start-Process -FilePath "java.exe" -ArgumentList @("-cp", (Join-Path $consumer "target/test-classes"), "example.FixtureApplication", "$fixturePort") `
            -WorkingDirectory $consumer -RedirectStandardOutput (Join-Path $runRoot "fixture.out.log") `
            -RedirectStandardError (Join-Path $runRoot "fixture.err.log") -WindowStyle Hidden -PassThru
        Start-Sleep -Milliseconds 500
        if ($script:fixture.HasExited) { throw "Fixture exited before startup" }
    }
    $script:studio = Invoke-MeasuredStage "start-studio" { Start-StudioHost }
    $landing = Invoke-WebRequest -UseBasicParsing -Uri $studio.Uri -SessionVariable studioSession
    $script:studioSession = $studioSession
    $token = [regex]::Match($landing.Content, '<meta name="test-lens-session" content="([^"]+)">').Groups[1].Value
    if ([string]::IsNullOrWhiteSpace($token)) { throw "Studio session bootstrap failed" }
    $configuration = Invoke-RestMethod -Uri ($studio.Uri + "api/config") -WebSession $studioSession
    if ($configuration.browserCapability.status -ne "AVAILABLE" -or $configuration.agentCapability.status -ne "AVAILABLE") {
        throw "Studio capability preflight failed"
    }

    Invoke-MeasuredStage "scan-project" { $null = Invoke-StudioAction $studio.Uri $token @{ action = "SCAN_PROJECT" } }
    Invoke-MeasuredStage "wait-map-application" { Wait-ProjectAction $studio.Uri "MAP_APPLICATION" }
    Invoke-MeasuredStage "current-page" { $null = Invoke-StudioAction $studio.Uri $token @{ action = "MAP_APPLICATION"; mode = "CURRENT_PAGE" } }
    $application = Invoke-RestMethod -Uri ($studio.Uri + "api/application") -WebSession $studioSession
    if (-not (@($application.pages | ForEach-Object { [string]$_.name }) | Where-Object { $_ -match '(?i)login' })) {
        throw "Current-page mapping did not observe Login"
    }
    Invoke-MeasuredStage "wait-correlate" { Wait-ProjectAction $studio.Uri "CORRELATE" }
    Invoke-MeasuredStage "correlate" { $null = Invoke-StudioAction $studio.Uri $token @{ action = "CORRELATE" } }
    $created = Invoke-MeasuredStage "create-requirement" {
        Invoke-StudioAction $studio.Uri $token @{ action = "CREATE_REQUIREMENT"; requirement = "Login with invalid password shows an error" }
    }
    $runId = [string]$created.result.runId
    if ([string]::IsNullOrWhiteSpace($runId)) { throw "Requirement did not create a workflow" }

    Invoke-MeasuredStage "wait-generate-plan" { Wait-WorkflowAction $studio.Uri $runId "GENERATE_PLAN" }
    Invoke-MeasuredStage "real-architect" { $null = Invoke-StudioAction $studio.Uri $token @{ action = "GENERATE_PLAN"; runId = $runId } }
    Invoke-MeasuredStage "wait-generate-implementation" { Wait-WorkflowAction $studio.Uri $runId "GENERATE_IMPLEMENTATION" }
    Invoke-MeasuredStage "real-implementer" { $null = Invoke-StudioAction $studio.Uri $token @{ action = "GENERATE_IMPLEMENTATION"; runId = $runId } }
    Invoke-MeasuredStage "wait-run" { Wait-WorkflowAction $studio.Uri $runId "RUN" }
    $run = Invoke-MeasuredStage "run-workflow-request" {
        Invoke-StudioAction $studio.Uri $token @{ action = "RUN"; runId = $runId }
    }
    $workflowStages = ConvertTo-StudioCertificationWorkflowStages -Timeline @($run.result.timeline) `
        -FinalState ([string]$run.result.finalState)
    $workflowResult = Invoke-StudioCertificationJsonRequest -Method GET `
        -Uri ($studio.Uri + "api/workflow?runId=" + [uri]::EscapeDataString($runId)) `
        -WebSession $studioSession -TimeoutSec 15 -Stage "REVIEW_EVIDENCE" -Provider "studio"
    if ($workflowResult.success) { $script:reviewEvidence = ConvertTo-StudioCertificationReviewEvidence $workflowResult.payload }
    foreach ($workflowStage in $workflowStages.stages) { $stages.Add($workflowStage) }
    if (-not $workflowStages.successful) {
        $failedStage = $workflowStages.failedStage
        throw "Targeted workflow was not successful"
    }

    Invoke-MeasuredStage "assert-model-calls" {
        if (-not (Test-Path -LiteralPath $metricsFile)) { throw "Agent metrics were not recorded" }
        $script:modelCalls = @(Get-Content -LiteralPath $metricsFile | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | ForEach-Object { $_ | ConvertFrom-Json })
        $expectedRoles = if ($modelCalls.Count -eq 3) {
            @("TEST_ARCHITECT", "TEST_IMPLEMENTER", "CODE_REVIEWER")
        } elseif ($modelCalls.Count -eq 5) {
            @("TEST_ARCHITECT", "TEST_IMPLEMENTER", "CODE_REVIEWER", "TEST_IMPLEMENTER", "CODE_REVIEWER")
        } else { throw "Expected one initial review or one bounded reviewer correction" }
        for ($index = 0; $index -lt $expectedRoles.Count; $index++) {
            $call = $modelCalls[$index]
            if ($call.role -ne $expectedRoles[$index] -or $call.status -ne "SUCCESS" -or
                    [long]$call.inputBytes -le 0 -or [long]$call.outputBytes -le 0 -or [long]$call.durationMillis -lt 0) {
                throw "Model-call telemetry did not match the certification contract"
            }
            $properties = @($call.psobject.Properties.Name | Sort-Object)
            if ([string]::Join(',', $properties) -ne "durationMillis,inputBytes,outputBytes,role,status") {
                throw "Model-call telemetry contained a disallowed field"
            }
        }
    }
    $status = "PASS"
    $failedStage = $null
} catch {
    if ($null -eq $failedStage) { $failedStage = "preflight" }
} finally {
    if ($null -ne $studio) { Stop-HarnessProcessTree $studio.Process }
    Stop-HarnessProcessTree $fixture
    foreach ($name in $savedEnvironment.Keys) {
        [Environment]::SetEnvironmentVariable($name, $savedEnvironment[$name], "Process")
    }
    if ($modelCalls.Count -eq 0 -and (Test-Path -LiteralPath $metricsFile)) {
        try {
            $modelCalls = @(Get-Content -LiteralPath $metricsFile | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | ForEach-Object {
                $value = $_ | ConvertFrom-Json
                [pscustomobject]@{ role = [string]$value.role; status = [string]$value.status; inputBytes = [long]$value.inputBytes; outputBytes = [long]$value.outputBytes; durationMillis = [long]$value.durationMillis }
            })
        } catch { $modelCalls = @() }
    }
    $summary = [ordered]@{
        certification = "0.5.0-real-codex-jwt"
        powershellVersion = $PSVersionTable.PSVersion.ToString()
        status = $status
        failedStage = $failedStage
        stages = @($stages)
        failureDiagnostic = $failureDiagnostic
        reviewEvidence = $reviewEvidence
        modelCalls = @($modelCalls | ForEach-Object {
            [ordered]@{ role = $_.role; status = $_.status; inputBytes = [long]$_.inputBytes; outputBytes = [long]$_.outputBytes; durationMillis = [long]$_.durationMillis }
        })
    }
    try { Remove-CertificationTempRoot $runRoot } catch { if ($status -eq "PASS") { $status = "FAILED"; $summary.status = $status; $summary.failedStage = "cleanup" } }
    Write-Output ($summary | ConvertTo-Json -Compress -Depth 6)
}

if ($status -ne "PASS") { exit 1 }
