[CmdletBinding()]
param(
    [switch]$ChildFailureProbe,
    [string]$BaseUri
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$modulePath = Join-Path $PSScriptRoot "StudioCertificationHttp.psm1"
if (-not (Test-Path -LiteralPath $modulePath -PathType Leaf)) {
    throw "Studio certification HTTP module is missing"
}
Import-Module $modulePath -Force

if ($ChildFailureProbe) {
    if ([string]::IsNullOrWhiteSpace($BaseUri)) { exit 90 }
    $terminal = Invoke-StudioCertificationJsonRequest -Method GET -Uri ($BaseUri + "/conflict") `
        -TimeoutSec 3 -Stage "CHILD_PROBE" -Provider "fixture"
    if ($terminal.success) { exit 91 }
    exit 23
}

if ($PSVersionTable.PSVersion.Major -ne 5) {
    throw "This regression must run in Windows PowerShell 5.1"
}

function Assert-True {
    param([bool]$Condition, [string]$Message)
    if (-not $Condition) { throw "Assertion failed: $Message" }
}

function Assert-Equal {
    param($Expected, $Actual, [string]$Message)
    if ($Expected -ne $Actual) {
        throw "Assertion failed: $Message; expected=$Expected; actual=$Actual"
    }
}

function Assert-NoUntrustedText {
    param($Value, [string]$Case)
    $serialized = $Value | ConvertTo-Json -Depth 10 -Compress
    Assert-True ($serialized -notmatch 'synthetic-secret') "$Case must not expose the synthetic secret"
    Assert-True ($serialized -notmatch 'caller-canary') "$Case must not expose the caller canary"
    Assert-True ($serialized -notmatch 'untrusted message') "$Case must not expose an untrusted message"
}

function Get-AvailablePort {
    $listener = New-Object Net.Sockets.TcpListener([Net.IPAddress]::Loopback, 0)
    try {
        $listener.Start()
        return ([Net.IPEndPoint]$listener.LocalEndpoint).Port
    } finally {
        $listener.Stop()
    }
}

function Wait-FixtureReady {
    param([int]$Port, [int]$TimeoutMilliseconds = 5000)
    $watch = [Diagnostics.Stopwatch]::StartNew()
    while ($watch.ElapsedMilliseconds -lt $TimeoutMilliseconds) {
        $probe = Invoke-StudioCertificationJsonRequest -Method GET -Uri "http://127.0.0.1:$Port/ok" `
            -TimeoutSec 1 -Stage "FIXTURE_READY" -Provider "fixture"
        if ($probe.success) { return }
        Start-Sleep -Milliseconds 25
    }
    throw "Local HTTP fixture did not start"
}

function Invoke-FixtureRequest {
    param(
        [string]$Path,
        [int]$TimeoutSec = 3,
        [string]$Stage = "GENERATE_IMPLEMENTATION"
    )
    return Invoke-StudioCertificationJsonRequest -Method GET -Uri ($script:FixtureBaseUri + $Path) `
        -TimeoutSec $TimeoutSec -Stage $Stage -Provider "fixture"
}

$port = Get-AvailablePort
$script:FixtureBaseUri = "http://127.0.0.1:$port"
$fixtureDirectory = Join-Path $PSScriptRoot "fixtures"
$server = Start-Process -FilePath "java.exe" -ArgumentList @("StudioDiagnosticHttpFixture.java", "$port") `
    -WorkingDirectory $fixtureDirectory -WindowStyle Hidden -PassThru

$passed = New-Object Collections.Generic.List[string]
try {
    Wait-FixtureReady -Port $port
    Write-Verbose "fixture-ready"

    $ok = Invoke-FixtureRequest "/ok"
    Assert-True $ok.success "HTTP 200 should succeed"
    Assert-Equal 200 $ok.httpStatus "HTTP 200 status"
    Assert-Equal "SUCCESS" $ok.payload.status "HTTP 200 payload"
    $passed.Add("200")

    $conflict = Invoke-FixtureRequest "/conflict"
    Assert-True (-not $conflict.success) "HTTP 409 should fail"
    Assert-Equal 409 $conflict.httpStatus "HTTP 409 status"
    Assert-Equal "STATE_CONFLICT" $conflict.error.code "409 domain code must be preserved"
    Assert-Equal "STATE_CONFLICT" $conflict.error.category "409 category"
    $passed.Add("409")
    Write-Verbose "basic-http-complete"

    $contract = Invoke-FixtureRequest "/contract"
    Assert-Equal 422 $contract.httpStatus "HTTP 422 status"
    Assert-Equal "AGENT_OUTPUT_INVALID" $contract.error.code "422 top-level domain code"
    Assert-Equal "PAGE_OBJECT_API_METHOD_MISSING" $contract.error.reasonCode "422 reason code"
    Assert-Equal "PAGE_OBJECT_CONTRACT" $contract.error.category "422 category"
    Assert-Equal "$.payload.pageObjectApisUsed[item]" $contract.error.location "422 static location"
    Assert-Equal "EXITED" $contract.error.processStatus "agent process status remains distinct from HTTP failure"
    $passed.Add("422")

    $decoder = Invoke-FixtureRequest "/decoder"
    Assert-Equal 502 $decoder.httpStatus "decoder HTTP status"
    Assert-Equal "AGENT_OUTPUT_INVALID" $decoder.error.code "decoder domain code"
    Assert-Equal "MALFORMED_JSON" $decoder.error.reasonCode "decoder reason"
    Assert-Equal "DECODER" $decoder.error.category "decoder category"

    $fieldType = ConvertTo-StudioCertificationDiagnostic -HttpStatus 502 -Stage "real-implementer" -Provider "codex-cli" -Body ([pscustomobject]@{
        error = "AGENT_OUTPUT_INVALID"
        stage = "TEST_IMPLEMENTER"
        reasonCode = "FIELD_TYPE_INVALID"
        location = '$.payload.pageObjectApisUsed'
        metadata = [pscustomobject]@{ processStatus = "EXITED"; exitCode = "0"; outputBytes = "83" }
    })
    Assert-Equal "FIELD_TYPE_INVALID" $fieldType.reasonCode "structured field reason"
    Assert-Equal "DECODER" $fieldType.category "structured contract failure must not be classified as process"
    Assert-Equal '$.payload.pageObjectApisUsed' $fieldType.location "structured field location"

    $nestedReference = ConvertTo-StudioCertificationDiagnostic -HttpStatus 422 -Stage "real-implementer" -Provider "codex-cli" -Body ([pscustomobject]@{
        error = "AGENT_OUTPUT_INVALID"
        stage = "TEST_IMPLEMENTER"
        reasonCode = "PAGE_OBJECT_API_INVALID_REFERENCE_STRUCTURE"
        location = '$.result.payload.pageObjectApisUsed[0]'
        metadata = [pscustomobject]@{ processStatus = "EXITED"; exitCode = "0"; outputBytes = "1479" }
    })
    Assert-Equal "PAGE_OBJECT_API_INVALID_REFERENCE_STRUCTURE" $nestedReference.reasonCode "nested reference reason"
    Assert-Equal "PAGE_OBJECT_CONTRACT" $nestedReference.category "nested reference category"
    Assert-Equal '$.result.payload.pageObjectApisUsed[item]' $nestedReference.location "nested reference location"

    $decoderReasons = @("MALFORMED_JSON", "ROOT_STRUCTURE_INVALID", "RESULT_VARIANT_INVALID",
        "SCHEMA_VERSION_UNSUPPORTED", "REQUIRED_FIELD_MISSING", "FIELD_TYPE_INVALID",
        "ARRAY_ELEMENT_INVALID", "ROLE_TYPE_MISMATCH", "DOMAIN_MAPPING_FAILED", "RULE_ID_INVALID",
        "REQUIRED_FINDING_MISSING", "DECISION_CONTRADICTORY", "SCHEMA_INVALID", "DOMAIN_INVALID",
        "TRUNCATED_JSON")
    $decoderLocations = @('$', '$.result', '$.result.payload', '$.schemaVersion',
        '$.result.payload.scenarioId', '$.result.payload.evidence[0]')
    foreach ($reason in $decoderReasons) {
        foreach ($location in $decoderLocations) {
            $diagnostic = ConvertTo-StudioCertificationDiagnostic -HttpStatus 502 -Stage "real-implementer" -Provider "codex-cli" -Body ([pscustomobject]@{
                error = "AGENT_OUTPUT_INVALID"; stage = "TEST_IMPLEMENTER"; reasonCode = $reason; location = $location
                metadata = [pscustomobject]@{ processStatus = "EXITED"; exitCode = "0"; outputBytes = "41" }
            })
            Assert-True ($diagnostic.reasonCode -ne "UNKNOWN") "decoder reason $reason must be preserved"
            Assert-True ($diagnostic.location -ne "UNKNOWN") "decoder location $location must be preserved"
            Assert-Equal "DECODER" $diagnostic.category "decoder failure must not become process failure"
        }
    }
    $codecPath = Join-Path $PSScriptRoot "../selenium-test-lens-application-tooling/src/main/java/io/github/testlens/application/tooling/ai/workflow/runner/AgentProtocolCodec.java"
    $codecSource = [IO.File]::ReadAllText((Resolve-Path -LiteralPath $codecPath))
    $decodeEnum = [regex]::Match($codecSource, 'enum DecodeFailure\s*\{(.*?)\n\s*\}',
        [Text.RegularExpressions.RegexOptions]::Singleline)
    Assert-True $decodeEnum.Success "decoder reason registry must be discoverable"
    $productionDecoderReasons = @([regex]::Matches($decodeEnum.Groups[1].Value,
        '\b[A-Z][A-Z_]{2,}\b') | ForEach-Object { $_.Value }) + @("TRUNCATED_JSON")
    Assert-Equal (($productionDecoderReasons | Sort-Object -Unique) -join ',') `
        (($decoderReasons | Sort-Object -Unique) -join ',') `
        "production decoder enum and sanitizer reason registries"

    $securityReasons = @("MULTIPLE_SECURITY_FINDINGS", "REDACTION_DISABLED", "REDACTION_FAILURE",
        "UNREDACTED_TEXT", "CALLER_CANARY_PRESENT", "SOURCE_SENSITIVE_CONTENT", "SENSITIVE_KEY_VALUE",
        "SENSITIVE_JSON_KEY_VALUE", "AUTHORIZATION_CREDENTIAL", "CREDENTIAL_URL", "JWT", "LITERAL_SECRET")
    $securityLocations = @('text', '$.decodedKey', '$.result.payload.sourcePatch',
        '$.result.payload.header.evidence[0]', '$.result.payload.missingCapabilities[0].reason')
    foreach ($reason in $securityReasons) {
        foreach ($location in $securityLocations) {
            $diagnostic = ConvertTo-StudioCertificationDiagnostic -HttpStatus 502 -Stage "real-implementer" -Provider "codex-cli" -Body ([pscustomobject]@{
                error = "AGENT_OUTPUT_INVALID"; stage = "TEST_IMPLEMENTER"; reasonCode = $reason; location = $location
                metadata = [pscustomobject]@{ processStatus = "EXITED"; exitCode = "0"; outputBytes = "43" }
            })
            Assert-True ($diagnostic.reasonCode -ne "UNKNOWN") "security reason $reason must be preserved"
            Assert-True ($diagnostic.location -ne "UNKNOWN") "security location $location must be preserved"
            Assert-Equal "SECURITY_GATE" $diagnostic.category "security failure must not become process failure"
        }
    }

    $pageObjectReasons = @("PAGE_OBJECT_API_INVALID_REFERENCE_STRUCTURE",
        "PAGE_OBJECT_API_METHOD_SEPARATOR_MISMATCH", "PAGE_OBJECT_API_PARAMETER_TYPE_FORMAT",
        "PAGE_OBJECT_API_METHOD_MISSING", "PAGE_OBJECT_API_ARITY_MISMATCH",
        "PAGE_OBJECT_API_AMBIGUOUS_OVERLOAD", "PAGE_OBJECT_API_DECLARING_CLASS_MISMATCH",
        "PAGE_OBJECT_API_UNTRUSTED_REFERENCE", "PAGE_OBJECT_API_REFERENCE_REQUIRED",
        "PAGE_OBJECT_API_DECLARED_REFERENCE_UNUSED", "PAGE_OBJECT_API_UNDECLARED_REFERENCE",
        "PAGE_OBJECT_API_SOURCE_USAGE_UNVERIFIED", "PAGE_OBJECT_MANIFEST_MISSING", "PAGE_OBJECT_MANIFEST_STALE")
    foreach ($reason in $pageObjectReasons) {
        $diagnostic = ConvertTo-StudioCertificationDiagnostic -HttpStatus 422 -Stage "real-implementer" -Provider "codex-cli" -Body ([pscustomobject]@{
            error = "AGENT_OUTPUT_INVALID"; stage = "TEST_IMPLEMENTER"; reasonCode = $reason
            location = '$.result.payload.pageObjectApisUsed[17]'
            metadata = [pscustomobject]@{ processStatus = "EXITED"; exitCode = "0"; outputBytes = "47" }
        })
        Assert-True ($diagnostic.reasonCode -ne "UNKNOWN") "Page Object reason $reason must be preserved"
        Assert-Equal '$.result.payload.pageObjectApisUsed[item]' $diagnostic.location "Page Object location"
        Assert-Equal "PAGE_OBJECT_CONTRACT" $diagnostic.category "Page Object failure must not become process failure"
    }

    $sourceVerificationReasons = @("PAGE_OBJECT_API_DECLARATION_OMITTED",
        "PAGE_OBJECT_API_SOURCE_PARSE_FAILED", "PAGE_OBJECT_API_METHOD_NOT_IN_MANIFEST",
        "PAGE_OBJECT_API_RECEIVER_UNRESOLVED", "PAGE_OBJECT_API_METHOD_SIGNATURE_MISMATCH")
    foreach ($reason in $sourceVerificationReasons) {
        $diagnostic = ConvertTo-StudioCertificationDiagnostic -HttpStatus 422 -Stage "real-implementer" -Provider "codex-cli" -Body ([pscustomobject]@{
            error = "AGENT_OUTPUT_INVALID"; stage = "TEST_IMPLEMENTER"; reasonCode = $reason
            location = '$.result.payload.sourcePatch'
            metadata = [pscustomobject]@{
                processStatus = "EXITED"; exitCode = "0"; outputBytes = "47"
                validationStage = "SOURCE_API_VERIFICATION"; ruleId = "METHOD_SIGNATURE"
                problemCategory = "SIGNATURE_RESOLUTION"; astNodeKind = "METHOD_CALL"
                receiverCategory = "KNOWN_PAGE_OBJECT"; manifestMatch = "MATCHED"
                fingerprintStatus = "MATCHED"; signatureStatus = "NO_MATCH"
            }
        })
        Assert-Equal $reason $diagnostic.reasonCode "source verification reason $reason must be preserved"
        Assert-Equal '$.result.payload.sourcePatch' $diagnostic.location "source verification location"
        Assert-Equal "PAGE_OBJECT_CONTRACT" $diagnostic.category "source verification failure must not become process failure"
        Assert-Equal "SOURCE_API_VERIFICATION" $diagnostic.validationStage "source verification stage"
        Assert-Equal "METHOD_SIGNATURE" $diagnostic.ruleId "source verification rule"
        Assert-Equal "SIGNATURE_RESOLUTION" $diagnostic.problemCategory "source verification category"
        Assert-Equal "METHOD_CALL" $diagnostic.astNodeKind "source verification AST node"
        Assert-Equal "KNOWN_PAGE_OBJECT" $diagnostic.receiverCategory "source verification receiver"
        Assert-Equal "MATCHED" $diagnostic.manifestMatch "source verification manifest match"
        Assert-Equal "MATCHED" $diagnostic.fingerprintStatus "source verification fingerprint"
        Assert-Equal "NO_MATCH" $diagnostic.signatureStatus "source verification signature"
    }

    $readinessReasons = Get-ManifestReadinessReasonCodes
    foreach ($reason in $readinessReasons) {
        $diagnostic = ConvertTo-StudioCertificationDiagnostic -HttpStatus 422 -Stage "manifest-preflight" -Provider "codex-cli" -Body ([pscustomobject]@{
            error = "PAGE_OBJECT_CAPABILITY_CONTEXT_CONFLICT"; stage = "TEST_ARCHITECT"; reasonCode = $reason
            location = '$.context.pageObjectCapabilities'
            metadata = [pscustomobject]@{
                processStatus = "NOT_STARTED"; validationStage = "MANIFEST_PREFLIGHT"; ruleId = "MANIFEST_READINESS"
                problemCategory = "MANIFEST"; astNodeKind = "MANIFEST"; receiverCategory = "NOT_APPLICABLE"
                manifestMatch = "INCOMPLETE"; fingerprintStatus = "MATCHED"; signatureStatus = "NOT_EVALUATED"
                manifestOriginStage = "SOURCE_CONTEXT_SLICE"; manifestReason = $reason.Substring("PAGE_OBJECT_".Length)
                sourceIndexCompleteness = "PARTIAL"; selectedPageObjectCount = "1"; verifiedClassCount = "1"
                verifiedMethodCount = "1"; unresolvedClassCount = "0"; constructorStatus = "VERIFIED"
                inheritedApiStatus = "NOT_APPLICABLE"; correlationStatus = "EXACT"; manifestTruncated = "FALSE"
                manifestMaxClasses = "24"; manifestMaxMethods = "160"; manifestMaxCharacters = "256000"
                manifestSourcesChanged = "FALSE"
            }
        })
        Assert-Equal $reason $diagnostic.reasonCode "manifest readiness reason $reason must be preserved"
        Assert-Equal '$.context.pageObjectCapabilities' $diagnostic.location "manifest readiness location"
        Assert-Equal "MANIFEST_PREFLIGHT" $diagnostic.validationStage "manifest readiness stage"
        Assert-Equal "MANIFEST_READINESS" $diagnostic.ruleId "manifest readiness rule"
        Assert-Equal "SOURCE_CONTEXT_SLICE" $diagnostic.manifestOriginStage "manifest origin"
        Assert-Equal 1 $diagnostic.selectedPageObjectCount "manifest selected Page Object count"
    }

    $readinessPath = Join-Path $PSScriptRoot "../selenium-test-lens-application-tooling/src/main/java/io/github/testlens/application/tooling/ai/PageObjectManifestReadiness.java"
    $readinessSource = [IO.File]::ReadAllText((Resolve-Path -LiteralPath $readinessPath))
    $statusEnum = [regex]::Match($readinessSource, 'public enum Status\s*\{(.*?)\}',
        [Text.RegularExpressions.RegexOptions]::Singleline)
    Assert-True $statusEnum.Success "manifest readiness status registry must be discoverable"
    $productionReadinessReasons = [regex]::Matches($statusEnum.Groups[1].Value, '[A-Z][A-Z_]+') | ForEach-Object {
        if ($_.Value -ne "READY") { "PAGE_OBJECT_" + $_.Value }
    } | Sort-Object
    Assert-Equal ($productionReadinessReasons -join ',') (($readinessReasons | Sort-Object) -join ',') `
        "production readiness enum and sanitizer reason registries"

    $validatorPath = Join-Path $PSScriptRoot "../selenium-test-lens-application-tooling/src/main/java/io/github/testlens/application/tooling/ai/workflow/runner/PageObjectApiReferenceValidator.java"
    $validatorSource = [IO.File]::ReadAllText((Resolve-Path -LiteralPath $validatorPath))
    $constantValues = @{}
    [regex]::Matches($validatorSource, 'public static final String\s+([A-Z_]+)\s*=\s*"([A-Z0-9_]+)";') | ForEach-Object {
        $constantValues[$_.Groups[1].Value] = $_.Groups[2].Value
    }
    $sourceSet = [regex]::Match($validatorSource, 'new LinkedHashSet<>\(Set\.of\((.*?)\)\)',
        [Text.RegularExpressions.RegexOptions]::Singleline)
    Assert-True $sourceSet.Success "validator source rejection registry must be discoverable"
    $javaReasons = @([regex]::Matches($sourceSet.Groups[1].Value, '[A-Z][A-Z_]+') | ForEach-Object {
        $constantValues[$_.Value]
    }) + $productionReadinessReasons | Sort-Object -Unique
    $sanitizerReasons = Get-SourceVerificationReasonCodes | Sort-Object
    Assert-Equal ($javaReasons -join ',') ($sanitizerReasons -join ',') "validator and sanitizer reason registries"

    foreach ($reason in @("DIAGNOSTIC_OVERFLOW", "OUTPUT_EMPTY", "OUTPUT_OVERSIZE", "BOUNDARY_FAILURE", "PROCESS_IO_FAILED")) {
        $location = if ($reason -eq "PROCESS_IO_FAILED") { "process.io" } elseif ($reason -eq "DIAGNOSTIC_OVERFLOW") { "process.diagnostics" } else { "result" }
        $diagnostic = ConvertTo-StudioCertificationDiagnostic -HttpStatus 502 -Stage "real-implementer" -Provider "codex-cli" -Body ([pscustomobject]@{
            error = "AGENT_OUTPUT_INVALID"; stage = "TEST_IMPLEMENTER"; reasonCode = $reason; location = $location
            metadata = [pscustomobject]@{ processStatus = "EXITED"; exitCode = "0"; outputBytes = "0" }
        })
        Assert-True ($diagnostic.reasonCode -ne "UNKNOWN") "output boundary reason $reason must be preserved"
        Assert-True ($diagnostic.location -ne "UNKNOWN") "output boundary location $location must be preserved"
        Assert-Equal "OUTPUT_BOUNDARY" $diagnostic.category "exit-zero output rejection must not become process failure"
    }
    $processIoFailure = ConvertTo-StudioCertificationDiagnostic -HttpStatus 502 -Stage "real-implementer" -Provider "codex-cli" -Body ([pscustomobject]@{
        error = "AGENT_PROCESS_FAILED"; stage = "TEST_IMPLEMENTER"; reasonCode = "PROCESS_IO_FAILED"; location = "process.io"
        metadata = [pscustomobject]@{ processStatus = "START_FAILED"; exitCode = "-1"; outputBytes = "0" }
    })
    Assert-Equal "PROCESS_IO_FAILED" $processIoFailure.reasonCode "process I/O reason must be preserved"
    Assert-Equal "AGENT_PROCESS" $processIoFailure.category "process I/O failure must remain a process failure"
    Assert-Equal "EXITED" $decoder.error.processStatus "Codex process completion must not be reported as request success"
    $passed.Add("decoder")

    $security = Invoke-FixtureRequest "/security"
    Assert-Equal "AGENT_OUTPUT_INVALID" $security.error.code "security gate domain code"
    Assert-Equal "JWT" $security.error.reasonCode "security gate reason"
    Assert-Equal "SECURITY_GATE" $security.error.category "security gate category"
    Assert-Equal "$.payload.sourcePatch" $security.error.location "security gate static location"
    $passed.Add("security")
    Write-Verbose "error-contract-complete"

    $capability = Invoke-FixtureRequest "/capability"
    Assert-True $capability.success "capability result is a valid HTTP response"
    Assert-Equal "PAGE_OBJECT_CAPABILITY_MISSING" $capability.payload.resultType "capability result must be preserved"
    $passed.Add("capability")
    Write-Verbose "capability-complete"

    $capabilityRefusal = Invoke-FixtureRequest "/capability-refusal"
    Assert-Equal 409 $capabilityRefusal.httpStatus "capability refusal HTTP status"
    Assert-Equal "PAGE_OBJECT_CAPABILITY_MISSING" $capabilityRefusal.error.code "capability refusal code"
    Assert-Equal "PAGE_OBJECT_CAPABILITY" $capabilityRefusal.error.category "capability refusal category"
    Assert-Equal "$.payload.capability" $capabilityRefusal.error.location "capability refusal static location"
    Assert-Equal "PAGE_OBJECT_METHOD_MISSING" $capabilityRefusal.error.capabilityCategory "safe capability classification"
    Assert-True $capabilityRefusal.error.sourceFingerprintMatched "fingerprint match must be preserved as a boolean"
    $passed.Add("capability-refusal")

    $extension = Invoke-FixtureRequest "/extension"
    Assert-True $extension.success "extension proposal is a valid HTTP response"
    Assert-Equal "PAGE_OBJECT_EXTENSION_PROPOSAL" $extension.payload.resultType "extension result must be preserved"
    $passed.Add("extension")
    Write-Verbose "extension-complete"

    $invalid = Invoke-FixtureRequest "/invalid-json"
    Assert-True (-not $invalid.success) "invalid JSON must fail"
    Assert-Equal 502 $invalid.httpStatus "invalid JSON preserves HTTP status"
    Assert-Equal "UNKNOWN" $invalid.error.code "invalid JSON has explicit unknown code"
    Assert-Equal "UNKNOWN" $invalid.error.reasonCode "invalid JSON has explicit unknown reason"
    Assert-Equal "UNKNOWN" $invalid.error.category "invalid JSON category"
    $passed.Add("invalid-json")
    Write-Verbose "invalid-json-complete"

    $empty = Invoke-FixtureRequest "/empty"
    Assert-Equal 502 $empty.httpStatus "empty response preserves HTTP status"
    Assert-Equal "UNKNOWN" $empty.error.code "empty response has explicit unknown code"
    Assert-Equal "UNKNOWN" $empty.error.category "empty response category"
    $passed.Add("empty")
    Write-Verbose "empty-complete"

    $untrusted = Invoke-FixtureRequest "/untrusted"
    Assert-Equal "MALFORMED_JSON" $untrusted.error.reasonCode "allowlisted reason survives hostile fields"
    Assert-Equal "UNKNOWN" $untrusted.error.location "dynamic location must be replaced"
    Assert-Equal "UNKNOWN" $untrusted.error.capabilityCategory "dynamic capability category must be replaced"
    Assert-True (-not $untrusted.error.sourceFingerprintMatched) "non-boolean fingerprint match must be rejected"
    Assert-NoUntrustedText $untrusted "untrusted error body"
    $passed.Add("redaction")
    Write-Verbose "payload-safety-complete"

    $snapshotDiagnostic = ConvertTo-StudioCertificationDiagnostic -HttpStatus 502 -Stage "GENERATE_IMPLEMENTATION" `
        -Provider "codex-cli" -Body ([pscustomobject]@{
            code = "AGENT_OUTPUT_INVALID"; stage = "TEST_IMPLEMENTER"; reasonCode = "SCHEMA_INVALID"
            location = "$.result"; metadata = [pscustomobject]@{ processStatus = "EXITED" }
        })
    Assert-Equal "AGENT_OUTPUT_INVALID" $snapshotDiagnostic.code "snapshot code schema must be preserved"
    Assert-Equal "DECODER" $snapshotDiagnostic.category "snapshot category"
    Assert-Equal "$.result" $snapshotDiagnostic.location "snapshot location"
    $passed.Add("snapshot-fallback")

    $successfulTimeline = ConvertTo-StudioCertificationWorkflowStages -FinalState "SUCCESS" -Timeline @(
        [pscustomobject]@{ name = "POLICY_VALIDATION"; status = "PASS"; durationMillis = 3 },
        [pscustomobject]@{ name = "COMPILE_PASS"; status = "PASS"; durationMillis = 5 },
        [pscustomobject]@{ name = "EXECUTION_PASS"; status = "PASS"; durationMillis = 7 },
        [pscustomobject]@{ name = "REVIEW"; status = "APPROVE"; durationMillis = 11 }
    )
    Assert-True $successfulTimeline.successful "successful workflow timeline must pass"
    Assert-Equal "APPROVE" $successfulTimeline.stages[3].status "review approval must remain a successful workflow status"
    Assert-Equal 11 $successfulTimeline.stages[3].durationMillis "review duration"
    $passed.Add("workflow-timeline-success")

    $failedTimeline = ConvertTo-StudioCertificationWorkflowStages -FinalState "NEEDS_HUMAN_REVIEW" -Timeline @(
        [pscustomobject]@{ name = "POLICY_VALIDATION"; status = "PASS"; durationMillis = 2 },
        [pscustomobject]@{ name = "COMPILE_FAIL"; status = "FAIL"; durationMillis = 13 }
    )
    Assert-True (-not $failedTimeline.successful) "failed workflow timeline must fail"
    Assert-Equal "targeted-compile" $failedTimeline.failedStage "compile failure must be the first blocker"
    Assert-Equal "FAIL" $failedTimeline.stages[1].status "compile failure status"
    Assert-Equal "NOT_REACHED" $failedTimeline.stages[2].status "Selenium must be reported as not reached"
    Assert-Equal "NOT_REACHED" $failedTimeline.stages[3].status "Reviewer must be reported as not reached"
    $passed.Add("workflow-timeline-failure")

    $reviewContractTimeline = ConvertTo-StudioCertificationWorkflowStages -FinalState "NEEDS_HUMAN_REVIEW" -Timeline @(
        [pscustomobject]@{ name = "POLICY_VALIDATION"; status = "PASS"; durationMillis = 2 },
        [pscustomobject]@{ name = "COMPILE_PASS"; status = "PASS"; durationMillis = 3 },
        [pscustomobject]@{ name = "EXECUTION_PASS"; status = "PASS"; durationMillis = 5 },
        [pscustomobject]@{ name = "REVIEW_CONTRACT"; status = "BLOCKED"; durationMillis = 7 }
    )
    Assert-True (-not $reviewContractTimeline.successful) "rejected reviewer contract must fail"
    Assert-Equal "real-reviewer" $reviewContractTimeline.failedStage "review contract must be the first blocker"
    Assert-Equal "BLOCKED" $reviewContractTimeline.stages[3].status "Reviewer must be reached and blocked"
    Assert-Equal 7 $reviewContractTimeline.stages[3].durationMillis "review contract duration"

    $reviewEvidence = ConvertTo-StudioCertificationReviewEvidence ([pscustomobject]@{
        review = [pscustomobject]@{
            verdict = "BLOCK"; reviewedArtifactRefs = @("implementation-1"); findings = @(
                [pscustomobject]@{ category = "REVIEW_UNSUBSTANTIATED"; severity = "ERROR";
                    ruleId = "ARTIFACT_REFERENCE_INVALID"; artifactRef = "implementation-1";
                    problemCategory = "REVIEW_CONTRACT"; requiredAction = "HUMAN_REVIEW";
                    location = "EXECUTION_EVIDENCE"; evidenceAvailability = "VERIFIED" })
            contractDiagnostic = [pscustomobject]@{ validationStage = "REVIEW_CONTRACT";
                reasonCode = "ARTIFACT_REFERENCE_INVALID"; location = '$.result.payload';
                category = "DOMAIN_VALIDATION"; contractVersion = 1; artifactRef = "implementation-1";
                evidenceAvailability = "VERIFIED" }
        }
        reviewHistory = @()
    })
    Assert-Equal "ARTIFACT_REFERENCE_INVALID" $reviewEvidence.latest.contractDiagnostic.reasonCode `
        "exact review contract reason must survive certification"
    Assert-Equal '$.result.payload' $reviewEvidence.latest.contractDiagnostic.location `
        "review contract location must remain structural"
    Assert-NoUntrustedText $reviewEvidence "review contract evidence"
    $passed.Add("review-contract-diagnostic")

    $disconnected = Invoke-FixtureRequest "/disconnect"
    Assert-True (-not $disconnected.success) "connection break must fail"
    Assert-Equal 0 $disconnected.httpStatus "connection break has no HTTP status"
    Assert-Equal "TRANSPORT_ERROR" $disconnected.error.code "connection break code"
    Assert-Equal "TRANSPORT" $disconnected.error.category "connection break category"
    $passed.Add("disconnect")
    Write-Verbose "transport-break-complete"

    $powershell = Join-Path $PSHOME "powershell.exe"
    Write-Verbose "starting-child-exit-probe"
    & $powershell -NoProfile -File $PSCommandPath -ChildFailureProbe -BaseUri $script:FixtureBaseUri
    $childExitCode = $LASTEXITCODE
    Assert-Equal 23 $childExitCode "terminal domain failure must produce a nonzero child exit"
    $passed.Add("terminal-exit")
    Write-Verbose "child-exit-probe-complete"

    # Keep the timeout case last because the deliberately stalled single-threaded fixture
    # cannot accept another request until its bounded delay completes.
    $timedOut = Invoke-FixtureRequest "/timeout" -TimeoutSec 1
    Write-Verbose "timeout-response-received"
    Assert-True (-not $timedOut.success) "timeout must fail"
    Assert-Equal 0 $timedOut.httpStatus "timeout has no HTTP status"
    Assert-Equal "TRANSPORT_TIMEOUT" $timedOut.error.code "timeout code"
    Assert-Equal "TIMEOUT" $timedOut.error.category "timeout category"
    $passed.Add("timeout")

    $report = [pscustomobject]@{
        status = "PASS"
        powershell = $PSVersionTable.PSVersion.ToString()
        cases = @($passed)
        count = $passed.Count
    }
    $reportJson = $report | ConvertTo-Json -Depth 4 -Compress
    $parsedReport = $reportJson | ConvertFrom-Json
    Assert-Equal "PASS" $parsedReport.status "emitted report must remain valid JSON"
    Assert-Equal $passed.Count $parsedReport.count "report case count"
    Assert-NoUntrustedText $report "report"
    Write-Host "Studio certification HTTP diagnostics: PASS ($($passed.Count) cases, Windows PowerShell $($PSVersionTable.PSVersion))"
} finally {
    if ($null -ne $server -and -not $server.HasExited) {
        & taskkill.exe /PID $server.Id /T /F 2>$null | Out-Null
    }
}
