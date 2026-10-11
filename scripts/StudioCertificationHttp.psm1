Set-StrictMode -Version 2.0

$script:MaximumResponseBytes = 65536
$script:ManifestReadinessReasonCodes = @(
    "PAGE_OBJECT_SOURCE_INDEX_INCOMPLETE", "PAGE_OBJECT_REQUIRED_CLASS_MISSING",
    "PAGE_OBJECT_REQUIRED_API_UNRESOLVED", "PAGE_OBJECT_CONSTRUCTION_UNRESOLVED",
    "PAGE_OBJECT_CORRELATION_INSUFFICIENT", "PAGE_OBJECT_MANIFEST_TRUNCATED",
    "PAGE_OBJECT_FINGERPRINT_MISMATCH", "PAGE_OBJECT_MANIFEST_STALE"
)
$script:SourceVerificationReasonCodes = @(
    "PAGE_OBJECT_MANIFEST_MISSING", "PAGE_OBJECT_MANIFEST_INCOMPLETE", "PAGE_OBJECT_MANIFEST_STALE", "PAGE_OBJECT_API_SOURCE_PARSE_FAILED",
    "PAGE_OBJECT_API_METHOD_NOT_IN_MANIFEST", "PAGE_OBJECT_API_RECEIVER_UNRESOLVED",
    "PAGE_OBJECT_API_METHOD_SIGNATURE_MISMATCH"
) + $script:ManifestReadinessReasonCodes
$script:SourceVerificationReasonCodes = @($script:SourceVerificationReasonCodes | Sort-Object -Unique)

function Read-BoundedHttpBody([System.IO.Stream]$Stream) {
    if ($null -eq $Stream) { return ,([byte[]]@()) }
    $output = New-Object System.IO.MemoryStream
    try {
        $buffer = New-Object byte[] 4096
        $total = 0
        while (($read = $Stream.Read($buffer, 0, $buffer.Length)) -gt 0) {
            $total += $read
            if ($total -gt $script:MaximumResponseBytes) { throw "HTTP_RESPONSE_OVERSIZE" }
            $output.Write($buffer, 0, $read)
        }
        return ,$output.ToArray()
    } finally {
        $output.Dispose()
    }
}

function Get-ExactProperty($Value, [string]$Name) {
    if ($null -eq $Value) { return $null }
    $property = $Value.PSObject.Properties[$Name]
    if ($null -eq $property) { return $null }
    return $property.Value
}

function Get-KnownDiagnosticCode($Value) {
    if (-not ($Value -is [string]) -or $Value.Length -gt 64) { return "UNKNOWN" }
    $known = @(
        "INVALID_REQUEST", "STATE_CONFLICT", "RESUME_UNAVAILABLE", "OPERATION_IN_PROGRESS",
        "BROWSER_SESSION_FAILED", "BROWSER_UNAVAILABLE", "OPERATION_FAILED",
        "AGENT_NOT_AVAILABLE", "AGENT_TIMEOUT", "AGENT_PROCESS_FAILED", "AGENT_OUTPUT_INVALID",
        "AGENT_CONTEXT_REJECTED", "PAGE_OBJECT_CAPABILITY_MISSING", "PAGE_OBJECT_EXTENSION_PROPOSAL",
        "PAGE_OBJECT_CAPABILITY_CONTEXT_CONFLICT", "PLAN_PAGE_OBJECT_INCOMPATIBLE",
        "PAGE_OBJECT_API_SYNTAX_INVALID", "PAGE_OBJECT_API_INVALID_REFERENCE_STRUCTURE",
        "PAGE_OBJECT_API_METHOD_SEPARATOR_MISMATCH", "PAGE_OBJECT_API_PARAMETER_TYPE_FORMAT",
        "PAGE_OBJECT_API_METHOD_MISSING", "PAGE_OBJECT_API_ARITY_MISMATCH",
        "PAGE_OBJECT_API_AMBIGUOUS_OVERLOAD", "PAGE_OBJECT_API_DECLARING_CLASS_MISMATCH",
        "PAGE_OBJECT_API_UNTRUSTED_REFERENCE", "PAGE_OBJECT_API_REFERENCE_REQUIRED",
        "PAGE_OBJECT_API_DECLARED_REFERENCE_UNUSED", "PAGE_OBJECT_API_UNDECLARED_REFERENCE",
        "PAGE_OBJECT_API_DECLARATION_OMITTED", "PAGE_OBJECT_API_SOURCE_PARSE_FAILED",
        "PAGE_OBJECT_API_METHOD_NOT_IN_MANIFEST", "PAGE_OBJECT_API_RECEIVER_UNRESOLVED",
        "PAGE_OBJECT_API_METHOD_SIGNATURE_MISMATCH", "PAGE_OBJECT_API_SOURCE_USAGE_UNVERIFIED",
        "PAGE_OBJECT_MANIFEST_MISSING", "PAGE_OBJECT_MANIFEST_STALE",
        "MALFORMED_JSON", "ROOT_STRUCTURE_INVALID", "RESULT_VARIANT_INVALID",
        "SCHEMA_VERSION_UNSUPPORTED", "REQUIRED_FIELD_MISSING", "FIELD_TYPE_INVALID",
        "ARRAY_ELEMENT_INVALID", "ROLE_TYPE_MISMATCH", "DOMAIN_MAPPING_FAILED", "RULE_ID_INVALID",
        "REQUIRED_FINDING_MISSING", "DECISION_CONTRADICTORY", "TRUNCATED_JSON",
        "SCHEMA_INVALID", "DOMAIN_INVALID",
        "PROCESS_START_FAILED", "PROCESS_TIMEOUT", "PROCESS_NONZERO_EXIT", "PROCESS_IO_FAILED", "DIAGNOSTIC_OVERFLOW",
        "OUTPUT_EMPTY", "OUTPUT_OVERSIZE", "COMMAND_SERIALIZATION_FAILED", "INPUT_OVERSIZE",
        "BOUNDARY_FAILURE", "MULTIPLE_SECURITY_FINDINGS", "REDACTION_DISABLED", "REDACTION_FAILURE", "UNREDACTED_TEXT",
        "CALLER_CANARY_PRESENT", "FORBIDDEN_ARTIFACT_REFERENCE", "SOURCE_SENSITIVE_CONTENT",
        "SENSITIVE_KEY_VALUE", "SENSITIVE_JSON_KEY_VALUE", "AUTHORIZATION_CREDENTIAL",
        "CREDENTIAL_URL", "JWT", "LITERAL_SECRET", "UNSPECIFIED_FAILURE"
    )
    $known += $script:SourceVerificationReasonCodes
    $known += $script:ManifestReadinessReasonCodes
    if ($known -contains $Value) { return $Value }
    return "UNKNOWN"
}

function Get-StaticDiagnosticCategory([string]$Code, [string]$ReasonCode, [int]$HttpStatus) {
    $security = @("MULTIPLE_SECURITY_FINDINGS", "REDACTION_DISABLED", "REDACTION_FAILURE", "UNREDACTED_TEXT",
        "CALLER_CANARY_PRESENT", "FORBIDDEN_ARTIFACT_REFERENCE", "SOURCE_SENSITIVE_CONTENT",
        "SENSITIVE_KEY_VALUE", "SENSITIVE_JSON_KEY_VALUE", "AUTHORIZATION_CREDENTIAL",
        "CREDENTIAL_URL", "JWT", "LITERAL_SECRET")
    $decoder = @("MALFORMED_JSON", "ROOT_STRUCTURE_INVALID", "RESULT_VARIANT_INVALID",
        "SCHEMA_VERSION_UNSUPPORTED", "REQUIRED_FIELD_MISSING", "FIELD_TYPE_INVALID",
        "ARRAY_ELEMENT_INVALID", "ROLE_TYPE_MISMATCH", "DOMAIN_MAPPING_FAILED", "RULE_ID_INVALID",
        "REQUIRED_FINDING_MISSING", "DECISION_CONTRADICTORY", "TRUNCATED_JSON",
        "SCHEMA_INVALID", "DOMAIN_INVALID")
    $process = @("PROCESS_START_FAILED", "PROCESS_NONZERO_EXIT", "DIAGNOSTIC_OVERFLOW", "OUTPUT_EMPTY",
        "OUTPUT_OVERSIZE", "COMMAND_SERIALIZATION_FAILED", "INPUT_OVERSIZE", "BOUNDARY_FAILURE")
    $outputBoundary = @("DIAGNOSTIC_OVERFLOW", "OUTPUT_EMPTY", "OUTPUT_OVERSIZE", "BOUNDARY_FAILURE")
    if ($security -contains $ReasonCode) { return "SECURITY_GATE" }
    if ($decoder -contains $ReasonCode) { return "DECODER" }
    if ($ReasonCode -like "PAGE_OBJECT_API_*" -or $ReasonCode -like "PAGE_OBJECT_MANIFEST_*") {
        return "PAGE_OBJECT_CONTRACT"
    }
    if ($Code -like "PAGE_OBJECT_*" -or $ReasonCode -like "PAGE_OBJECT_*" -or
            $ReasonCode -eq "PLAN_PAGE_OBJECT_INCOMPATIBLE") { return "PAGE_OBJECT_CAPABILITY" }
    if ($Code -eq "AGENT_TIMEOUT" -or $ReasonCode -eq "PROCESS_TIMEOUT") { return "TIMEOUT" }
    if ($ReasonCode -eq "PROCESS_IO_FAILED" -and $Code -eq "AGENT_OUTPUT_INVALID") {
        return "OUTPUT_BOUNDARY"
    }
    if ($outputBoundary -contains $ReasonCode) { return "OUTPUT_BOUNDARY" }
    if ($Code -like "AGENT_*" -or $process -contains $ReasonCode) { return "AGENT_PROCESS" }
    if ($Code -eq "STATE_CONFLICT" -or $Code -eq "RESUME_UNAVAILABLE" -or
            $Code -eq "OPERATION_IN_PROGRESS" -or $HttpStatus -eq 409) { return "STATE_CONFLICT" }
    if ($Code -eq "INVALID_REQUEST" -or $HttpStatus -eq 400 -or $HttpStatus -eq 422) { return "CONTRACT" }
    if ($Code -like "BROWSER_*") { return "BROWSER" }
    return "UNKNOWN"
}

function Get-SafeStructuralLocation($Value) {
    if (-not ($Value -is [string]) -or $Value.Length -gt 160) { return "UNKNOWN" }
    if ($Value -eq '$' -or $Value -eq 'text' -or $Value -eq '$.decodedKey' -or
            $Value -eq '$.decodedString') { return $Value }
    $allowed = @(
        "boundary", "command", "result", "process.start", "process.wait", "process.exit", "process.io",
        "process.diagnostics", "process.executable", "$.schemaVersion", "$.resultType", "$.payload",
        "$.payload.sourcePatch", "$.payload.pageObjectApisUsed", "$.payload.pageObjectApisUsed[item]",
        "$.result", "$.result.resultType", "$.result.payload", "$.result.payload.sourcePatch",
        "$.result.payload.pageObjectApisUsed", "$.result.payload.pageObjectApisUsed[item]",
        "$.result.payload.capability", "$.result.payload.evidence[item]",
        "$.payload.capability", "$.payload.missing.capability",
        "$.context.pageObjectCapabilities", "$.context.pageObjectCapabilities.sourceFingerprint", "$.plan.scenarios", "$.decodedString",
        "$.multiple", "$.result"
    )
    if ($allowed -contains $Value) { return $Value }
    $segment = '(?:schemaVersion|result|resultType|payload|header|status|evidence|limitations|confidence|scenarioId|sourcePatch|selectorAccessPolicy|pageObjectApisUsed|missingCapabilities|pageId|elementId|requiredCapability|reason|capability|missing|extensionClass|proposedMethod|\[field\]|\[sensitive\])'
    if ($Value -match ('^\$(?:\.' + $segment + '(?:\[\d{1,4}\])?)+$')) {
        return [regex]::Replace($Value, '\[\d{1,4}\]', '[item]')
    }
    return "UNKNOWN"
}

function Get-SafeIdentifier($Value, [string]$Fallback = "UNKNOWN") {
    if ($Value -is [string] -and $Value -match '^[A-Za-z0-9._-]{1,96}$') { return $Value }
    return $Fallback
}

function Get-SafeAgentRole($Value) {
    $roles = @("PLANNER", "IMPLEMENTER", "FAILURE_CLASSIFIER", "REPAIRER", "REVIEWER",
        "TEST_ARCHITECT", "SCENARIO_DESIGNER", "TEST_IMPLEMENTER", "TEST_VERIFIER", "STABILIZER",
        "CODE_REVIEWER", "UNIT_TEST_AGENT", "AGENT")
    if ($Value -is [string] -and $roles -contains $Value) { return $Value }
    return "UNKNOWN"
}

function Get-SafeProcessStatus($Value) {
    $statuses = @("NOT_STARTED", "STARTING", "START_FAILED", "RUNNING", "TIMED_OUT", "EXITED")
    if ($Value -is [string] -and $statuses -contains $Value) { return $Value }
    return "UNKNOWN"
}

function Get-SafeCapabilityCategory($Value) {
    $categories = @("PAGE_OBJECT_METHOD_MISSING", "PAGE_OBJECT_CONSTRUCTION_MISSING",
        "BROWSER_SESSION_BINDING_MISSING", "TEST_SETUP_MISSING", "TEST_DATA_MISSING",
        "ASSERTION_INCOMPATIBLE", "CONTEXT_INCOMPLETE", "CAPABILITY_PRESENT_BUT_REJECTED", "UNKNOWN")
    if ($Value -is [string] -and $categories -contains $Value) { return $Value }
    return "UNKNOWN"
}

function Get-SafeSourceDiagnosticField([string]$Name, $Value) {
    $allowed = switch ($Name) {
        "validationStage" { @("SOURCE_API_VERIFICATION", "MANIFEST_PREFLIGHT") }
        "ruleId" { @("MANIFEST_REQUIRED", "MANIFEST_FINGERPRINT", "MANIFEST_READINESS", "SOURCE_PARSE", "METHOD_RECEIVER",
                "METHOD_MEMBERSHIP", "METHOD_SIGNATURE", "CONSTRUCTOR_SIGNATURE", "METHOD_REFERENCE_SIGNATURE") }
        "problemCategory" { @("MANIFEST", "SOURCE_SYNTAX", "RECEIVER_RESOLUTION", "METHOD_MEMBERSHIP",
                "SIGNATURE_RESOLUTION") }
        "astNodeKind" { @("MANIFEST", "COMPILATION_UNIT", "METHOD_CALL", "OBJECT_CREATION", "METHOD_REFERENCE") }
        "receiverCategory" { @("KNOWN_PAGE_OBJECT", "KNOWN_NON_PAGE_OBJECT", "UNRESOLVED", "NOT_APPLICABLE") }
        "manifestMatch" { @("MATCHED", "MISSING", "INCOMPLETE", "STALE", "OWNER_NOT_FOUND", "METHOD_NOT_FOUND", "NOT_EVALUATED") }
        "fingerprintStatus" { @("MATCHED", "MISMATCH", "NOT_EVALUATED") }
        "signatureStatus" { @("RESOLVED", "NO_MATCH", "AMBIGUOUS", "UNRESOLVED", "NOT_EVALUATED") }
        default { @() }
    }
    if ($Value -is [string] -and $allowed -contains $Value) { return $Value }
    return "UNKNOWN"
}

function Get-SafeManifestDiagnosticField([string]$Name, $Value) {
    $allowed = switch ($Name) {
        "manifestOriginStage" { @("SOURCE_CONTEXT_SLICE", "RESTORED_SNAPSHOT", "UNSPECIFIED") }
        "manifestReason" { @("READY", "SOURCE_INDEX_INCOMPLETE", "REQUIRED_CLASS_MISSING",
                "REQUIRED_API_UNRESOLVED", "CONSTRUCTION_UNRESOLVED", "CORRELATION_INSUFFICIENT",
                "MANIFEST_TRUNCATED", "FINGERPRINT_MISMATCH", "MANIFEST_STALE") }
        "sourceIndexCompleteness" { @("COMPLETE", "PARTIAL", "UNKNOWN") }
        "constructorStatus" { @("VERIFIED", "UNRESOLVED", "NOT_REQUIRED") }
        "inheritedApiStatus" { @("VERIFIED", "UNRESOLVED", "NOT_APPLICABLE") }
        "correlationStatus" { @("EXACT", "STRONG", "MIXED", "INSUFFICIENT") }
        "manifestTruncated" { @("TRUE", "FALSE") }
        "manifestSourcesChanged" { @("TRUE", "FALSE") }
        default { @() }
    }
    if ($Value -is [string] -and $allowed -contains $Value) { return $Value }
    return "UNKNOWN"
}

function Get-SourceVerificationReasonCodes {
    return @($script:SourceVerificationReasonCodes)
}

function Get-ManifestReadinessReasonCodes {
    return @($script:ManifestReadinessReasonCodes)
}

function Get-SafeLong($Value) {
    $number = 0L
    if ($null -ne $Value -and [long]::TryParse([string]$Value, [ref]$number) -and $number -ge 0) {
        return $number
    }
    return 0L
}

function Get-SafeExitCode($Value) {
    $number = -1
    if ($null -ne $Value -and [int]::TryParse([string]$Value, [ref]$number) -and
            $number -ge 0 -and $number -le 65535) { return $number }
    return -1
}

function New-SafeHttpDiagnostic([int]$HttpStatus, $ParsedBody, [string]$Stage, [string]$Provider) {
    $rawCode = Get-ExactProperty $ParsedBody "error"
    if ($null -eq $rawCode) { $rawCode = Get-ExactProperty $ParsedBody "code" }
    $code = Get-KnownDiagnosticCode $rawCode
    $reasonCode = Get-KnownDiagnosticCode (Get-ExactProperty $ParsedBody "reasonCode")
    $metadata = Get-ExactProperty $ParsedBody "metadata"
    $role = Get-SafeAgentRole (Get-ExactProperty $ParsedBody "stage")
    $providerId = Get-SafeIdentifier $Provider
    $processStatus = Get-SafeProcessStatus (Get-ExactProperty $metadata "processStatus")
    [pscustomobject][ordered]@{
        stage = Get-SafeIdentifier $Stage
        code = $code
        reasonCode = $reasonCode
        category = Get-StaticDiagnosticCategory $code $reasonCode $HttpStatus
        location = Get-SafeStructuralLocation (Get-ExactProperty $ParsedBody "location")
        role = $role
        provider = $providerId
        processStatus = $processStatus
        exitCode = Get-SafeExitCode (Get-ExactProperty $metadata "exitCode")
        durationMillis = Get-SafeLong (Get-ExactProperty $metadata "durationMillis")
        inputBytes = Get-SafeLong (Get-ExactProperty $metadata "inputBytes")
        outputBytes = Get-SafeLong (Get-ExactProperty $metadata "outputBytes")
        capabilityCategory = Get-SafeCapabilityCategory (Get-ExactProperty $ParsedBody "capabilityCategory")
        sourceFingerprintMatched = (Get-ExactProperty $ParsedBody "sourceFingerprintMatched") -eq $true
        validationStage = Get-SafeSourceDiagnosticField "validationStage" (Get-ExactProperty $metadata "validationStage")
        ruleId = Get-SafeSourceDiagnosticField "ruleId" (Get-ExactProperty $metadata "ruleId")
        problemCategory = Get-SafeSourceDiagnosticField "problemCategory" (Get-ExactProperty $metadata "problemCategory")
        astNodeKind = Get-SafeSourceDiagnosticField "astNodeKind" (Get-ExactProperty $metadata "astNodeKind")
        receiverCategory = Get-SafeSourceDiagnosticField "receiverCategory" (Get-ExactProperty $metadata "receiverCategory")
        manifestMatch = Get-SafeSourceDiagnosticField "manifestMatch" (Get-ExactProperty $metadata "manifestMatch")
        fingerprintStatus = Get-SafeSourceDiagnosticField "fingerprintStatus" (Get-ExactProperty $metadata "fingerprintStatus")
        signatureStatus = Get-SafeSourceDiagnosticField "signatureStatus" (Get-ExactProperty $metadata "signatureStatus")
        manifestOriginStage = Get-SafeManifestDiagnosticField "manifestOriginStage" (Get-ExactProperty $metadata "manifestOriginStage")
        manifestReason = Get-SafeManifestDiagnosticField "manifestReason" (Get-ExactProperty $metadata "manifestReason")
        sourceIndexCompleteness = Get-SafeManifestDiagnosticField "sourceIndexCompleteness" (Get-ExactProperty $metadata "sourceIndexCompleteness")
        selectedPageObjectCount = Get-SafeLong (Get-ExactProperty $metadata "selectedPageObjectCount")
        verifiedClassCount = Get-SafeLong (Get-ExactProperty $metadata "verifiedClassCount")
        verifiedMethodCount = Get-SafeLong (Get-ExactProperty $metadata "verifiedMethodCount")
        unresolvedClassCount = Get-SafeLong (Get-ExactProperty $metadata "unresolvedClassCount")
        constructorStatus = Get-SafeManifestDiagnosticField "constructorStatus" (Get-ExactProperty $metadata "constructorStatus")
        inheritedApiStatus = Get-SafeManifestDiagnosticField "inheritedApiStatus" (Get-ExactProperty $metadata "inheritedApiStatus")
        correlationStatus = Get-SafeManifestDiagnosticField "correlationStatus" (Get-ExactProperty $metadata "correlationStatus")
        manifestTruncated = Get-SafeManifestDiagnosticField "manifestTruncated" (Get-ExactProperty $metadata "manifestTruncated")
        manifestMaxClasses = Get-SafeLong (Get-ExactProperty $metadata "manifestMaxClasses")
        manifestMaxMethods = Get-SafeLong (Get-ExactProperty $metadata "manifestMaxMethods")
        manifestMaxCharacters = Get-SafeLong (Get-ExactProperty $metadata "manifestMaxCharacters")
        manifestSourcesChanged = Get-SafeManifestDiagnosticField "manifestSourcesChanged" (Get-ExactProperty $metadata "manifestSourcesChanged")
    }
}

function ConvertTo-StudioCertificationDiagnostic {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)][int]$HttpStatus,
        $Body,
        [string]$Stage = "UNKNOWN",
        [string]$Provider = "UNKNOWN"
    )
    return New-SafeHttpDiagnostic $HttpStatus $Body $Stage $Provider
}

function ConvertTo-StudioCertificationWorkflowStages {
    [CmdletBinding()]
    param(
        [object[]]$Timeline = @(),
        [string]$FinalState = "UNKNOWN"
    )
    $definitions = @(
        [pscustomobject]@{ reportName = "ast-validation"; timelineNames = @("POLICY_VALIDATION"); successStatus = "PASS" },
        [pscustomobject]@{ reportName = "targeted-compile"; timelineNames = @("COMPILE_PASS", "COMPILE_FAIL"); successStatus = "PASS" },
        [pscustomobject]@{ reportName = "selenium-execute"; timelineNames = @("EXECUTION_PASS", "EXECUTION_FAIL"); successStatus = "PASS" },
        [pscustomobject]@{ reportName = "real-reviewer"; timelineNames = @("REVIEW", "REVIEW_CONTRACT"); successStatus = "APPROVE" }
    )
    $knownStatuses = @("PASS", "FAIL", "BLOCKED", "NOT_RUN", "PROPOSE_ONLY", "APPROVE", "REQUEST_CHANGES")
    $reported = New-Object System.Collections.Generic.List[object]
    $firstFailure = $null
    foreach ($definition in $definitions) {
        $matches = @($Timeline | Where-Object { $definition.timelineNames -contains $_.name })
        $entry = if ($matches.Count -eq 0) { $null } else { $matches[$matches.Count - 1] }
        $statusValue = "NOT_REACHED"
        $durationValue = 0L
        if ($null -ne $entry) {
            if ($entry.status -in $knownStatuses) { $statusValue = [string]$entry.status } else { $statusValue = "UNKNOWN" }
            try {
                $candidateDuration = [long]$entry.durationMillis
                if ($candidateDuration -ge 0) { $durationValue = $candidateDuration }
            } catch { $durationValue = 0L }
        }
        $reported.Add([pscustomobject]@{
            name = [string]$definition.reportName
            status = $statusValue
            durationMillis = $durationValue
        })
        if ($null -eq $firstFailure -and $statusValue -ne $definition.successStatus) {
            $firstFailure = [string]$definition.reportName
        }
    }
    if ($null -eq $firstFailure -and $FinalState -ne "SUCCESS") { $firstFailure = "run-workflow-result" }
    return [pscustomobject]@{
        successful = ($null -eq $firstFailure)
        failedStage = $firstFailure
        stages = $reported.ToArray()
    }
}

function Get-SafeReviewEnum($Value, [string[]]$Allowed) {
    if ($Value -is [string] -and $Allowed -contains $Value) { return $Value }
    return "UNKNOWN"
}

function Get-SafeReviewArtifactRef($Value) {
    if ($Value -is [string] -and $Value -match '^[A-Za-z0-9][A-Za-z0-9._:-]{0,127}$') { return $Value }
    return "UNKNOWN"
}

function ConvertTo-StudioCertificationReviewEvidence {
    [CmdletBinding()]
    param($Workflow)
    $categories = @("ASSERTION_INSUFFICIENT", "REQUIREMENT_NOT_COVERED", "PAGE_OBJECT_MISUSE", "TEST_DATA_INVALID",
        "TEST_LIFECYCLE_PROBLEM", "EXECUTION_EVIDENCE_INSUFFICIENT", "UNSAFE_IMPLEMENTATION", "REVIEW_UNSUBSTANTIATED")
    $problemCategories = @("ASSERTION", "REQUIREMENT", "PAGE_OBJECT", "TEST_DATA", "LIFECYCLE", "EVIDENCE", "SAFETY", "REVIEW_CONTRACT")
    $actions = @("ADD_ASSERTION", "COVER_REQUIREMENT", "USE_VERIFIED_PAGE_OBJECT", "FIX_TEST_DATA", "FIX_TEST_LIFECYCLE",
        "PROVIDE_EXECUTION_EVIDENCE", "REMOVE_UNSAFE_IMPLEMENTATION", "HUMAN_REVIEW")
    $locations = @("GENERATED_TEST", "TEST_METHOD", "ASSERTION", "PAGE_OBJECT_CALL", "TEST_DATA", "LIFECYCLE", "EXECUTION_EVIDENCE")
    $availability = @("VERIFIED", "PARTIAL", "UNAVAILABLE")
    $verdicts = @("APPROVE", "REQUEST_CHANGES", "BLOCK")
    $safeReview = {
        param($Review)
        if ($null -eq $Review) { return $null }
        $findings = @($Review.findings | Select-Object -First 100 | ForEach-Object {
            [pscustomobject][ordered]@{
                category = Get-SafeReviewEnum $_.category $categories
                severity = Get-SafeReviewEnum $_.severity @("ERROR", "WARNING", "INFO")
                ruleId = Get-SafeReviewArtifactRef $_.ruleId
                artifactRef = Get-SafeReviewArtifactRef $_.artifactRef
                problemCategory = Get-SafeReviewEnum $_.problemCategory $problemCategories
                requiredAction = Get-SafeReviewEnum $_.requiredAction $actions
                location = Get-SafeReviewEnum $_.location $locations
                evidenceAvailability = Get-SafeReviewEnum $_.evidenceAvailability $availability
            }
        })
        return [pscustomobject][ordered]@{
            verdict = Get-SafeReviewEnum $Review.verdict $verdicts
            reviewedArtifactRefs = @($Review.reviewedArtifactRefs | Select-Object -First 100 | ForEach-Object { Get-SafeReviewArtifactRef $_ })
            findings = $findings
            contractDiagnostic = if ($null -eq $Review.contractDiagnostic) { $null } else {
                [pscustomobject][ordered]@{
                    validationStage = Get-SafeReviewEnum $Review.contractDiagnostic.validationStage @("REVIEW_CONTRACT")
                    reasonCode = Get-SafeReviewArtifactRef $Review.contractDiagnostic.reasonCode
                    location = if ($Review.contractDiagnostic.location -eq '$.result.payload') { '$.result.payload' } else { 'UNKNOWN' }
                    category = Get-SafeReviewEnum $Review.contractDiagnostic.category @("DOMAIN_VALIDATION")
                    contractVersion = Get-SafeLong $Review.contractDiagnostic.contractVersion
                    artifactRef = Get-SafeReviewArtifactRef $Review.contractDiagnostic.artifactRef
                    evidenceAvailability = Get-SafeReviewEnum $Review.contractDiagnostic.evidenceAvailability $availability
                }
            }
        }
    }
    $history = @($Workflow.reviewHistory | Select-Object -First 2 | ForEach-Object {
        [pscustomobject][ordered]@{
            implementationVersion = Get-SafeLong $_.implementationVersion
            implementationArtifactRef = Get-SafeReviewArtifactRef $_.implementationArtifactRef
            executionArtifactRef = Get-SafeReviewArtifactRef $_.executionArtifactRef
            reviewArtifactRef = Get-SafeReviewArtifactRef $_.reviewArtifactRef
            compileOutcome = Get-SafeReviewEnum $_.compileOutcome @("PASS", "FAIL", "NOT_RUN", "INCONCLUSIVE", "TIMED_OUT")
            executionOutcome = Get-SafeReviewEnum $_.executionOutcome @("PASS", "FAIL", "TIMED_OUT", "NOT_RUN", "INCONCLUSIVE")
            durationMillis = Get-SafeLong $_.durationMillis
            review = & $safeReview $_.review
        }
    })
    [pscustomobject][ordered]@{ latest = (& $safeReview $Workflow.review); history = $history }
}

function Invoke-StudioCertificationJsonRequest {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)][ValidateSet("GET", "POST")][string]$Method,
        [Parameter(Mandatory = $true)][uri]$Uri,
        $WebSession,
        [hashtable]$Headers = @{},
        $Body,
        [ValidateRange(1, 900)][int]$TimeoutSec = 30,
        [string]$Stage = "UNKNOWN",
        [string]$Provider = "UNKNOWN"
    )
    $response = $null
    $status = 0
    $bytes = [byte[]]@()
    try {
        $request = [System.Net.HttpWebRequest]::Create($Uri)
        $request.Method = $Method
        $request.Accept = "application/json"
        $request.KeepAlive = $false
        $request.Timeout = $TimeoutSec * 1000
        $request.ReadWriteTimeout = $TimeoutSec * 1000
        if ($null -ne $WebSession -and $null -ne $WebSession.Cookies) {
            $request.CookieContainer = $WebSession.Cookies
        }
        foreach ($name in $Headers.Keys) { $request.Headers[[string]$name] = [string]$Headers[$name] }
        if ($Method -eq "POST") {
            $request.ContentType = "application/json"
            $json = if ($Body -is [string]) { $Body } else { $Body | ConvertTo-Json -Compress -Depth 12 }
            $content = [System.Text.Encoding]::UTF8.GetBytes($json)
            $request.ContentLength = $content.Length
            $stream = $request.GetRequestStream()
            try { $stream.Write($content, 0, $content.Length) } finally { $stream.Dispose() }
        }
        try {
            $response = [System.Net.HttpWebResponse]$request.GetResponse()
        } catch [System.Net.WebException] {
            if ($null -eq $_.Exception.Response) { throw }
            $response = [System.Net.HttpWebResponse]$_.Exception.Response
        }
        $status = [int]$response.StatusCode
        $bytes = Read-BoundedHttpBody $response.GetResponseStream()
    } catch [System.Net.WebException] {
        $transportCode = if ($_.Exception.Status -eq [System.Net.WebExceptionStatus]::Timeout) {
            "TRANSPORT_TIMEOUT"
        } else { "TRANSPORT_ERROR" }
        return [pscustomobject][ordered]@{
            success = $false; method = $Method; httpStatus = 0; payload = $null
            error = [pscustomobject][ordered]@{
                stage = Get-SafeIdentifier $Stage; code = $transportCode; reasonCode = "UNKNOWN"
                category = if ($transportCode -eq "TRANSPORT_TIMEOUT") { "TIMEOUT" } else { "TRANSPORT" }
                location = "UNKNOWN"; role = "UNKNOWN"; provider = Get-SafeIdentifier $Provider
                processStatus = "UNKNOWN"; exitCode = -1; durationMillis = 0L; inputBytes = 0L; outputBytes = 0L
                capabilityCategory = "UNKNOWN"; sourceFingerprintMatched = $false
                validationStage = "UNKNOWN"; ruleId = "UNKNOWN"; problemCategory = "UNKNOWN"
                astNodeKind = "UNKNOWN"; receiverCategory = "UNKNOWN"; manifestMatch = "UNKNOWN"
                fingerprintStatus = "UNKNOWN"; signatureStatus = "UNKNOWN"
            }
        }
    } catch {
        return [pscustomobject][ordered]@{
            success = $false; method = $Method; httpStatus = $status; payload = $null
            error = [pscustomobject][ordered]@{
                stage = Get-SafeIdentifier $Stage; code = "INVALID_HTTP_RESPONSE"; reasonCode = "UNKNOWN"
                category = "PROTOCOL"; location = "UNKNOWN"; role = "UNKNOWN"
                provider = Get-SafeIdentifier $Provider; processStatus = "UNKNOWN"
                exitCode = -1; durationMillis = 0L; inputBytes = 0L; outputBytes = 0L
                capabilityCategory = "UNKNOWN"; sourceFingerprintMatched = $false
                validationStage = "UNKNOWN"; ruleId = "UNKNOWN"; problemCategory = "UNKNOWN"
                astNodeKind = "UNKNOWN"; receiverCategory = "UNKNOWN"; manifestMatch = "UNKNOWN"
                fingerprintStatus = "UNKNOWN"; signatureStatus = "UNKNOWN"
            }
        }
    } finally {
        if ($null -ne $response) { $response.Dispose() }
    }

    $parsed = $null
    if ($bytes.Length -gt 0) {
        try { $parsed = [System.Text.Encoding]::UTF8.GetString($bytes) | ConvertFrom-Json -ErrorAction Stop }
        catch { $parsed = $null }
    }
    if ($status -ge 200 -and $status -lt 300 -and $null -ne $parsed) {
        return [pscustomobject][ordered]@{
            success = $true; method = $Method; httpStatus = $status; payload = $parsed; error = $null
        }
    }
    if ($status -ge 200 -and $status -lt 300) {
        return [pscustomobject][ordered]@{
            success = $false; method = $Method; httpStatus = $status; payload = $null
            error = [pscustomobject][ordered]@{
                stage = Get-SafeIdentifier $Stage; code = "INVALID_HTTP_JSON"; reasonCode = "UNKNOWN"
                category = "PROTOCOL"; location = "UNKNOWN"; role = "UNKNOWN"
                provider = Get-SafeIdentifier $Provider; processStatus = "UNKNOWN"
                exitCode = -1; durationMillis = 0L; inputBytes = 0L; outputBytes = 0L
                capabilityCategory = "UNKNOWN"; sourceFingerprintMatched = $false
                validationStage = "UNKNOWN"; ruleId = "UNKNOWN"; problemCategory = "UNKNOWN"
                astNodeKind = "UNKNOWN"; receiverCategory = "UNKNOWN"; manifestMatch = "UNKNOWN"
                fingerprintStatus = "UNKNOWN"; signatureStatus = "UNKNOWN"
            }
        }
    }
    return [pscustomobject][ordered]@{
        success = $false; method = $Method; httpStatus = $status; payload = $null
        error = New-SafeHttpDiagnostic $status $parsed $Stage $Provider
    }
}

Export-ModuleMember -Function Invoke-StudioCertificationJsonRequest, ConvertTo-StudioCertificationDiagnostic, ConvertTo-StudioCertificationWorkflowStages, ConvertTo-StudioCertificationReviewEvidence, Get-SourceVerificationReasonCodes, Get-ManifestReadinessReasonCodes
