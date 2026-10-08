[CmdletBinding()]
param(
    [string]$Version = "0.5.0",
    [string]$MavenRepository,
    [switch]$SkipStage,
    [switch]$Firefox
)
$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Import-Module (Join-Path $PSScriptRoot "StudioCertificationSupport.psm1") -Force
$rootPom = [xml](Get-Content -Raw (Join-Path $repoRoot "pom.xml"))
$rootVersion = [string]$rootPom.project.version
if (-not $SkipStage -and $Version -ne $rootVersion) { throw "Requested certification version $Version does not match reactor version $rootVersion" }
if ([string]::IsNullOrWhiteSpace($MavenRepository)) {
    $MavenRepository = if ($SkipStage) { Join-Path $env:USERPROFILE ".m2/repository" } else { Join-Path ([System.IO.Path]::GetTempPath()) ("test-lens-s15-m2-" + [guid]::NewGuid().ToString("N")) }
}
$MavenRepository = [System.IO.Path]::GetFullPath($MavenRepository)
[System.IO.Directory]::CreateDirectory($MavenRepository) | Out-Null
if (-not $SkipStage) {
    & mvn.cmd "-Dmaven.repo.local=$MavenRepository" -DskipTests install
    if ($LASTEXITCODE -ne 0) { throw "Local 0.5.0 staging failed" }
}

$consumer = Join-Path ([System.IO.Path]::GetTempPath()) ("test-lens-s15-consumer-" + [guid]::NewGuid().ToString("N"))
[System.IO.Directory]::CreateDirectory($consumer) | Out-Null
Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot "fixtures/s15-external-consumer") -Force | ForEach-Object {
    Copy-Item -LiteralPath $_.FullName -Destination $consumer -Recurse
}
$consumerPom = Join-Path $consumer "pom.xml"
$consumerPomText = [System.IO.File]::ReadAllText($consumerPom).Replace("0.5.0", $Version)
[System.IO.File]::WriteAllText($consumerPom, $consumerPomText, [System.Text.UTF8Encoding]::new($false))
if ($Firefox) {
    $projectConfig = Join-Path $consumer ".test-lens/project.json"
    $text = [System.IO.File]::ReadAllText($projectConfig).Replace('"name": "CHROME"', '"name": "FIREFOX"')
    [System.IO.File]::WriteAllText($projectConfig, $text, [System.Text.UTF8Encoding]::new($false))
}
$listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, 0)
$listener.Start(); $fixturePort = ([System.Net.IPEndPoint]$listener.LocalEndpoint).Port; $listener.Stop()
$projectConfig = Join-Path $consumer ".test-lens/project.json"
$text = [System.IO.File]::ReadAllText($projectConfig).Replace("127.0.0.1:18181", "127.0.0.1:$fixturePort")
[System.IO.File]::WriteAllText($projectConfig, $text, [System.Text.UTF8Encoding]::new($false))
& mvn.cmd "-Dmaven.repo.local=$MavenRepository" -f (Join-Path $consumer "pom.xml") test-compile
if ($LASTEXITCODE -ne 0) { throw "External fixture compilation failed" }
$fixture = $null
$first = $null
$second = $null

function Stop-HarnessProcessTree($process) {
    if ($null -eq $process -or $process.HasExited) { return }
    if ($IsWindows -or $env:OS -eq "Windows_NT") {
        & taskkill.exe /PID $process.Id /T /F 2>$null | Out-Null
    } else {
        Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
    }
}

function Start-Studio([string]$suffix) {
    $stdout = Join-Path $consumer ("studio-" + $suffix + ".out.log")
    $stderr = Join-Path $consumer ("studio-" + $suffix + ".err.log")
    $arguments = @("-Dmaven.repo.local=$MavenRepository", "-DtestLens.studio.openBrowser=false",
        "io.github.test-lens:test-lens-test-engineering-maven-plugin:${Version}:studio")
    $process = Start-Process -FilePath "mvn.cmd" -ArgumentList $arguments -WorkingDirectory $consumer `
        -RedirectStandardOutput $stdout -RedirectStandardError $stderr -WindowStyle Hidden -PassThru
    $deadline = [DateTime]::UtcNow.AddMinutes(3)
    do {
        Start-Sleep -Milliseconds 250
        if ($process.HasExited) { throw "Studio exited before startup: $((Get-Content -LiteralPath $stderr -Raw -ErrorAction SilentlyContinue))" }
        $output = if (Test-Path $stdout) { [string](Get-Content -LiteralPath $stdout -Raw -ErrorAction SilentlyContinue) } else { "" }
        $match = [regex]::Match($output, 'Test Engineering Studio:\s+(http://127\.0\.0\.1:\d+/)')
    } while (-not $match.Success -and [DateTime]::UtcNow -lt $deadline)
    if (-not $match.Success) { Stop-HarnessProcessTree $process; throw "Studio startup timed out" }
    return @{ Process = $process; Uri = $match.Groups[1].Value }
}

function Invoke-StudioAction($session, [string]$origin, [string]$token, [hashtable]$body) {
    Write-Host ("Studio action: " + $body.action)
    return Invoke-RestMethod -Uri ($origin + "api/actions") -Method Post -WebSession $session `
        -Headers @{ Origin = $origin.TrimEnd('/'); "X-Test-Lens-Session" = $token } -ContentType "application/json" -Body ($body | ConvertTo-Json -Compress)
}

function Wait-StudioActionAvailable($session, [string]$origin, [string]$action, [TimeSpan]$timeout = ([TimeSpan]::FromSeconds(30))) {
    $expected = $action.ToLowerInvariant().Replace('_', '-')
    Wait-TestLensStudioActionAvailable -Action $action -Timeout $timeout -ReadState {
        $status = Invoke-RestMethod -Uri ($origin + "api/status") -WebSession $session
        $project = Invoke-RestMethod -Uri ($origin + "api/project") -WebSession $session
        $available = @($project.stage.actions | Where-Object { $_.enabled -and $_.id -eq $expected } | ForEach-Object { $action })
        [pscustomobject]@{
            OperationRunning = [bool]$status.operationRunning
            AvailableActions = $available
        }
    } | Out-Null
}

try {
    $fixture = Start-Process -FilePath "java.exe" -ArgumentList @("-cp", (Join-Path $consumer "target/test-classes"), "example.FixtureApplication", "$fixturePort") -WorkingDirectory $consumer -WindowStyle Hidden -PassThru
    Start-Sleep -Milliseconds 500
    $first = Start-Studio "first"
    try {
    $landing = Invoke-WebRequest -UseBasicParsing -Uri $first.Uri -SessionVariable studioSession
    $token = [regex]::Match($landing.Content, '<meta name="test-lens-session" content="([^"]+)">').Groups[1].Value
    if ([string]::IsNullOrWhiteSpace($token)) { throw "Studio session token bootstrap failed" }
    $config = Invoke-RestMethod -Uri ($first.Uri + "api/config") -WebSession $studioSession
    if ($config.browserCapability.status -ne "AVAILABLE") { throw "Browser preflight failed: $($config.browserCapability.status)" }
    if ($config.agentCapability.status -ne "AVAILABLE") { throw "Agent preflight failed: $($config.agentCapability.status)" }
    $null = Invoke-StudioAction $studioSession $first.Uri $token @{ action = "SCAN_PROJECT" }
    Wait-StudioActionAvailable $studioSession $first.Uri "MAP_APPLICATION"
    $null = Invoke-StudioAction $studioSession $first.Uri $token @{ action = "MAP_APPLICATION"; mode = "CURRENT_PAGE" }
    Wait-StudioActionAvailable $studioSession $first.Uri "CORRELATE"
    $null = Invoke-StudioAction $studioSession $first.Uri $token @{ action = "CORRELATE" }
    $created = Invoke-StudioAction $studioSession $first.Uri $token @{ action = "CREATE_REQUIREMENT"; requirement = "Login with invalid password shows an error" }
    $runId = $created.result.runId
    try { $null = Invoke-StudioAction $studioSession $first.Uri $token @{ action = "GENERATE_PLAN"; runId = $runId } }
    catch { $detail=Invoke-RestMethod -Uri ($first.Uri+"api/workflow?runId="+[uri]::EscapeDataString($runId)) -WebSession $studioSession; throw "GENERATE_PLAN failed; state=$($detail.state), freshness=$($detail.freshness), limitations=$([string]::Join(',',@($detail.limitations))); error=$($_.Exception.Message)" }
    $null = Invoke-StudioAction $studioSession $first.Uri $token @{ action = "GENERATE_IMPLEMENTATION"; runId = $runId }
    $run = Invoke-StudioAction $studioSession $first.Uri $token @{ action = "RUN"; runId = $runId }
    if ($run.result.finalState -ne "SUCCESS") { throw "External targeted execution did not succeed: $(($run | ConvertTo-Json -Depth 8 -Compress))" }

    $pageObject = Join-Path $consumer "src/test/java/example/LoginPage.java"
    $unchanged = [System.IO.File]::ReadAllText($pageObject)
    $null = Invoke-WebRequest -UseBasicParsing -Uri ("http://127.0.0.1:$fixturePort/change")
    $repairCreated = Invoke-StudioAction $studioSession $first.Uri $token @{ action = "CREATE_REQUIREMENT"; requirement = "Login selector regression must be repaired without weakening the test" }
    $repairRunId = $repairCreated.result.runId
    $null = Invoke-StudioAction $studioSession $first.Uri $token @{ action = "GENERATE_PLAN"; runId = $repairRunId }
    $null = Invoke-StudioAction $studioSession $first.Uri $token @{ action = "GENERATE_IMPLEMENTATION"; runId = $repairRunId }
    $null = Invoke-StudioAction $studioSession $first.Uri $token @{ action = "RUN"; runId = $repairRunId }
    $failed = Invoke-RestMethod -Uri ($first.Uri + "api/workflow?runId=" + [uri]::EscapeDataString($repairRunId)) -WebSession $studioSession
    if ($failed.diagnosis.category -ne "SELECTOR_INSTABILITY") { throw "Real selector failure was not classified as SELECTOR_INSTABILITY: $(($failed | ConvertTo-Json -Depth 12 -Compress))" }
    $proposal = $failed.repair
    if ($null -eq $proposal) { throw "External selector failure did not produce a RepairProposal" }
    if (-not ($failed.availableActions -contains "APPROVE_REPAIR")) { throw "External repair was not held for explicit approval" }
    if ([System.IO.File]::ReadAllText($pageObject) -ne $unchanged) { throw "Repair proposal mutated source before approval" }
    $applied = Invoke-StudioAction $studioSession $first.Uri $token @{ action = "APPROVE_REPAIR"; runId = $repairRunId; proposalId = $proposal.proposalId }
    if ($applied.result.applyStatus -ne "APPLIED") { throw "Trusted repair was not applied: $(($applied | ConvertTo-Json -Depth 8 -Compress))" }
    $repairedSource = [System.IO.File]::ReadAllText($pageObject)
    if ($repairedSource -eq $unchanged -or $repairedSource -match 'By\.id\("old-login-button"\)') { throw "Trusted apply did not surgically replace the old selector" }
    $verified = Invoke-StudioAction $studioSession $first.Uri $token @{ action = "RERUN"; runId = $repairRunId }
    if ($verified.result.finalState -ne "SUCCESS") { throw "Repaired same-test rerun did not pass: $(($verified | ConvertTo-Json -Depth 8 -Compress))" }
    } finally { Stop-HarnessProcessTree $first.Process }

    $second = Start-Studio "restart"
    try {
    $null = Invoke-WebRequest -UseBasicParsing -Uri $second.Uri -SessionVariable resumedSession
    $workflows = Invoke-RestMethod -Uri ($second.Uri + "api/workflows") -WebSession $resumedSession
    if (-not ($workflows | Where-Object { $_.runId -eq $runId })) { throw "Workflow was not restored after Studio restart" }
    if (-not ($workflows | Where-Object { $_.runId -eq $repairRunId -and $_.state -eq "SUCCESS" })) { throw "Verified repair workflow was not restored after Studio restart" }
    } finally { Stop-HarnessProcessTree $second.Process }
} finally {
    if ($null -ne $second) { Stop-HarnessProcessTree $second.Process }
    if ($null -ne $first) { Stop-HarnessProcessTree $first.Process }
    Stop-HarnessProcessTree $fixture
}

Write-Host "S15 external consumer certification PASS"
Write-Host "Consumer: $consumer"
Write-Host "Repository: $MavenRepository"
