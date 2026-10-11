package io.github.testlens.application.tooling.ai.workflow.runner;

import io.github.testlens.application.tooling.ai.CodeReviewResult;
import io.github.testlens.application.tooling.ai.ContractHeader;
import io.github.testlens.application.tooling.ai.FailureClassification;
import io.github.testlens.application.tooling.ai.RepairProposal;
import io.github.testlens.application.tooling.ai.TestExecutionResult;
import io.github.testlens.application.tooling.ai.TestImplementationProposal;
import io.github.testlens.application.tooling.ai.TestPlan;
import io.github.testlens.application.tooling.ai.workflow.AgentExecutor;
import io.github.testlens.application.tooling.ai.workflow.PageObjectCapabilityMissing;
import io.github.testlens.application.tooling.ai.workflow.PageObjectExtensionProposal;
import io.github.testlens.application.tooling.json.StrictJson;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Strict, versioned JSON protocol used at the external-agent process boundary. @since 0.5.0 */
public final class AgentProtocolCodec {
    public static final int SCHEMA_VERSION = 1;
    private static final int MAX_PAGE_OBJECT_API_REFERENCES = 128;
    private static final int MAX_PAGE_OBJECT_API_REFERENCE_LENGTH = 1024;

    public byte[] encodeCommand(AgentExecutor.AgentCommand command) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schemaVersion", SCHEMA_VERSION);
        root.put("contract", "TEST_LENS_EXTERNAL_AGENT_V1");
        root.put("role", command.role().name());
        root.put("runId", command.runId());
        root.put("immutableInstructions", instructions(command.role()));
        root.put("instructions", command.instructions());
        root.put("inputs", command.inputs());
        return StrictJson.write(root);
    }

    public byte[] outputSchema(AgentExecutor.Role role) {
        List<String> allowed = allowedTypes(role);
        if (allowed.size() == 1) return StrictJson.write(envelopeSchema(allowed.get(0)));
        return StrictJson.write(Map.of("type", "object", "additionalProperties", false,
                "properties", Map.of(
                        "schemaVersion", Map.of("type", "integer", "const", SCHEMA_VERSION),
                        "result", Map.of("anyOf",
                                allowed.stream().map(AgentProtocolCodec::resultVariantSchema).toList())),
                "required", List.of("schemaVersion", "result")));
    }

    private static Map<String, Object> envelopeSchema(String resultType) {
        return Map.of("type", "object", "additionalProperties", false, "properties", Map.of(
                "schemaVersion", Map.of("type", "integer", "const", SCHEMA_VERSION),
                "resultType", Map.of("type", "string", "const", resultType),
                "payload", payloadSchema(resultType)),
                "required", List.of("schemaVersion", "resultType", "payload"));
    }

    private static Map<String, Object> resultVariantSchema(String resultType) {
        return Map.of("type", "object", "additionalProperties", false, "properties", Map.of(
                        "resultType", Map.of("type", "string", "const", resultType),
                        "payload", payloadSchema(resultType)),
                "required", List.of("resultType", "payload"));
    }

    private static Map<String, Object> payloadSchema(String resultType) {
        return switch (resultType) {
            case "TEST_PLAN" -> objectSchema(Map.of(
                    "header", headerSchema(),
                    "scenarios", arraySchema(objectSchema(Map.ofEntries(
                            Map.entry("scenarioId", stringSchema()), Map.entry("title", stringSchema()),
                            Map.entry("priority", enumSchema(TestPlan.Priority.class)),
                            Map.entry("preconditions", stringArraySchema()),
                            Map.entry("steps", arraySchema(objectSchema(Map.of(
                                    "order", positiveIntegerSchema(), "action", stringSchema(), "pageId", nullableStringSchema(),
                                    "elementId", nullableStringSchema(), "valueRef", nullableStringSchema())))),
                            Map.entry("expected", arraySchema(objectSchema(Map.of(
                                    "pageId", nullableStringSchema(), "stateId", nullableStringSchema(),
                                    "assertion", stringSchema(), "evidenceRef", nullableStringSchema())))),
                            Map.entry("requiredPageIds", stringArraySchema()),
                            Map.entry("requiredStateIds", stringArraySchema()),
                            Map.entry("requiredElementIds", stringArraySchema()),
                            Map.entry("requiredData", stringArraySchema()),
                            Map.entry("riskAreas", stringArraySchema()),
                            Map.entry("existingCoverage", enumSchema(TestPlan.CoverageDisposition.class)),
                            Map.entry("existingTestRefs", stringArraySchema()),
                            Map.entry("knownUnknowns", stringArraySchema()))))));
            case "TEST_IMPLEMENTATION_PROPOSAL" -> objectSchema(Map.of(
                    "header", headerSchema(), "scenarioId", stringSchema(), "sourcePatch", stringSchema(),
                    "selectorAccessPolicy", enumSchema(TestImplementationProposal.SelectorAccessPolicy.class),
                    "pageObjectApisUsed", pageObjectApiArraySchema(),
                    "missingCapabilities", arraySchema(objectSchema(Map.of(
                            "pageId", stringSchema(), "elementId", stringSchema(),
                            "requiredCapability", stringSchema(), "reason", stringSchema())))));
            case "TEST_EXECUTION_RESULT" -> objectSchema(Map.ofEntries(
                    Map.entry("header", headerSchema()), Map.entry("scenarioId", stringSchema()),
                    Map.entry("compileOutcome", enumSchema(TestExecutionResult.Outcome.class)),
                    Map.entry("executionOutcome", enumSchema(TestExecutionResult.Outcome.class)),
                    Map.entry("frameworkResult", nullableStringSchema()), Map.entry("durationMillis", integerSchema()),
                    Map.entry("compileEvidenceRefs", stringArraySchema()), Map.entry("traceEvidenceRefs", stringArraySchema()),
                    Map.entry("assertionEvidenceRefs", stringArraySchema()), Map.entry("runtimeEventRefs", stringArraySchema()),
                    Map.entry("screenshotRefs", stringArraySchema()), Map.entry("selectorDiagnosticRefs", stringArraySchema()),
                    Map.entry("failureSummary", nullableStringSchema())));
            case "FAILURE_CLASSIFICATION" -> objectSchema(Map.of(
                    "header", headerSchema(), "category", enumSchema(FailureClassification.Category.class),
                    "rootCause", stringSchema(), "affectedIds", stringArraySchema(),
                    "evidenceRefs", stringArraySchema(), "counterEvidenceRefs", stringArraySchema()));
            case "REPAIR_PROPOSAL" -> objectSchema(Map.ofEntries(
                    Map.entry("header", headerSchema()), Map.entry("proposalId", stringSchema()),
                    Map.entry("applicationPolicy", enumSchema(RepairProposal.ApplicationPolicy.class)),
                    Map.entry("whatChanged", stringSchema()), Map.entry("why", stringSchema()),
                    Map.entry("oldSelector", nullableStringSchema()), Map.entry("newSelector", nullableStringSchema()),
                    Map.entry("sourceDeclarationRef", nullableStringSchema()),
                    Map.entry("oldCandidateId", nullableStringSchema()), Map.entry("newCandidateId", nullableStringSchema()),
                    Map.entry("classificationRef", nullableStringSchema()), Map.entry("driftRef", nullableStringSchema()),
                    Map.entry("sameTargetEvidence", stringArraySchema()), Map.entry("stabilityEvidence", stringArraySchema()),
                    Map.entry("affectedTests", stringArraySchema()), Map.entry("risks", stringArraySchema()),
                    Map.entry("verificationPlan", stringArraySchema())));
            case "CODE_REVIEW_RESULT" -> objectSchema(Map.of(
                    "header", reviewHeaderSchema(), "verdict", enumSchema(CodeReviewResult.Verdict.class),
                    "reviewedArtifactRefs", arraySchema(safeIdSchema()),
                    "findings", arraySchema(objectSchema(Map.of(
                            "category", enumSchema(CodeReviewResult.Category.class),
                            "severity", enumSchema(CodeReviewResult.Severity.class),
                            "ruleId", Map.of("type", "string", "enum", CodeReviewResult.allowedRuleIds()),
                            "artifactRef", safeIdSchema(),
                            "problemCategory", enumSchema(CodeReviewResult.ProblemCategory.class),
                            "requiredAction", enumSchema(CodeReviewResult.RequiredAction.class),
                            "safeLocation", enumSchema(CodeReviewResult.SafeLocation.class),
                            "evidenceAvailability", enumSchema(CodeReviewResult.EvidenceAvailability.class))))));
            case "PAGE_OBJECT_CAPABILITY_MISSING" -> objectSchema(Map.of(
                    "pageId", stringSchema(), "capability", stringSchema(), "reason", stringSchema()));
            case "PAGE_OBJECT_EXTENSION_PROPOSAL" -> objectSchema(Map.of(
                    "missing", payloadSchema("PAGE_OBJECT_CAPABILITY_MISSING"), "extensionClass", stringSchema(),
                    "proposedMethod", stringSchema(), "evidence", stringArraySchema()));
            default -> throw new IllegalArgumentException("Unsupported result type " + resultType);
        };
    }

    private static Map<String, Object> headerSchema() {
        return objectSchema(Map.of("schemaVersion", Map.of("type", "integer", "const", ContractHeader.SCHEMA_VERSION),
                "status", enumSchema(ContractHeader.Status.class), "evidence", stringArraySchema(),
                "limitations", stringArraySchema(), "confidence", enumSchema(ContractHeader.Confidence.class)));
    }

    private static Map<String, Object> reviewHeaderSchema() {
        return objectSchema(Map.of("schemaVersion", Map.of("type", "integer", "const", ContractHeader.SCHEMA_VERSION),
                "status", Map.of("type", "string", "enum", List.of(ContractHeader.Status.COMPLETED.name())),
                "evidence", stringArraySchema(), "limitations", stringArraySchema(),
                "confidence", enumSchema(ContractHeader.Confidence.class)));
    }

    private static Map<String, Object> objectSchema(Map<String, Object> properties) {
        return Map.of("type", "object", "additionalProperties", false, "properties", properties,
                "required", properties.keySet().stream().sorted().toList());
    }

    private static Map<String, Object> arraySchema(Map<String, Object> items) {
        return Map.of("type", "array", "items", items);
    }

    private static Map<String, Object> stringArraySchema() { return arraySchema(stringSchema()); }
    private static Map<String, Object> pageObjectApiArraySchema() {
        return Map.of("type", "array", "maxItems", MAX_PAGE_OBJECT_API_REFERENCES,
                "items", Map.of("type", "string", "minLength", 1,
                        "maxLength", MAX_PAGE_OBJECT_API_REFERENCE_LENGTH));
    }
    private static Map<String, Object> stringSchema() { return Map.of("type", "string", "minLength", 1); }

    private static Map<String, Object> safeIdSchema() {
        return Map.of("type", "string", "minLength", 1, "maxLength", 128,
                "pattern", "^[A-Za-z0-9][A-Za-z0-9._:-]{0,127}$");
    }
    private static Map<String, Object> nullableStringSchema() { return Map.of("type", List.of("string", "null")); }
    private static Map<String, Object> integerSchema() { return Map.of("type", "integer"); }
    private static Map<String, Object> positiveIntegerSchema() { return Map.of("type", "integer", "minimum", 1); }
    private static Map<String, Object> enumSchema(Class<? extends Enum<?>> type) {
        return Map.of("type", "string", "enum", java.util.Arrays.stream(type.getEnumConstants()).map(Enum::name).toList());
    }

    public DecodedResult decode(AgentExecutor.Role role, byte[] document) {
        Map<String, Object> root;
        try {
            root = StrictJson.readObject(document);
        } catch (RuntimeException malformed) {
            if ("JSON root must be an object".equals(malformed.getMessage())) {
                throw decoding(DecodeFailure.ROOT_STRUCTURE_INVALID, "$",
                        "External agent result root must be an object", malformed);
            }
            throw decoding(DecodeFailure.MALFORMED_JSON, "$", "Agent result is not valid JSON", malformed);
        }
        RootShape rootShape = rootShape(root);
        if (integer(root, "schemaVersion", "$.schemaVersion") != SCHEMA_VERSION) {
            throw decoding(DecodeFailure.SCHEMA_VERSION_UNSUPPORTED, "$.schemaVersion",
                    "Unsupported external agent schemaVersion");
        }
        Map<String, Object> result = rootShape == RootShape.NESTED
                ? object(root, "result", "$.result") : root;
        if (rootShape == RootShape.NESTED) fields(result, Set.of("resultType", "payload"), "$.result");
        String resultTypePath = rootShape == RootShape.NESTED ? "$.result.resultType" : "$.resultType";
        String payloadPath = rootShape == RootShape.NESTED ? "$.result.payload" : "$.payload";
        String resultType = string(result, "resultType", resultTypePath);
        if (!allowedTypes(role).contains(resultType)) {
            throw decoding(DecodeFailure.ROLE_TYPE_MISMATCH, resultTypePath,
                    "External agent resultType is not allowed for this role");
        }
        Map<String, Object> payload = object(result, "payload", payloadPath);
        if (payloadMatchesAnotherVariant(resultType, payload)) {
            throw decoding(DecodeFailure.RESULT_VARIANT_INVALID, payloadPath,
                    "External agent payload does not match its declared resultType");
        }
        Object value;
        try {
            value = switch (resultType) {
                case "TEST_PLAN" -> plan(payload, payloadPath);
                case "TEST_IMPLEMENTATION_PROPOSAL" -> implementation(payload, payloadPath);
                case "TEST_EXECUTION_RESULT" -> execution(payload, payloadPath);
                case "FAILURE_CLASSIFICATION" -> classification(payload, payloadPath);
                case "REPAIR_PROPOSAL" -> repair(payload, payloadPath);
                case "CODE_REVIEW_RESULT" -> review(payload, payloadPath);
                case "PAGE_OBJECT_CAPABILITY_MISSING" -> capability(payload, payloadPath);
                case "PAGE_OBJECT_EXTENSION_PROPOSAL" -> extension(payload, payloadPath);
                default -> throw decoding(DecodeFailure.RESULT_VARIANT_INVALID, resultTypePath,
                        "Unsupported external agent resultType");
            };
        } catch (StrictJson.JsonFormatException structured) {
            if (decodeMarker(structured) != null) throw structured;
            throw decoding(DecodeFailure.FIELD_TYPE_INVALID, payloadPath,
                    "External agent payload contains a field with an invalid type", structured);
        } catch (RuntimeException invalidDomain) {
            throw decoding(DecodeFailure.DOMAIN_MAPPING_FAILED, payloadPath,
                    "External agent payload violates the domain model", invalidDomain);
        }
        return new DecodedResult(resultType, value, payloadPath);
    }

    private static RootShape rootShape(Map<String, Object> root) {
        Set<String> fields = root.keySet();
        if (fields.equals(Set.of("schemaVersion", "resultType", "payload"))) return RootShape.FLAT;
        if (fields.equals(Set.of("schemaVersion", "result"))) return RootShape.NESTED;
        if (!root.containsKey("schemaVersion")
                && fields.stream().allMatch(Set.of("result", "resultType", "payload")::contains)) {
            throw decoding(DecodeFailure.REQUIRED_FIELD_MISSING, "$.schemaVersion",
                    "A required external agent field is missing");
        }
        boolean mixed = root.containsKey("result")
                && (root.containsKey("resultType") || root.containsKey("payload"));
        if (mixed) {
            throw decoding(DecodeFailure.ROOT_STRUCTURE_INVALID, "$",
                    "External agent result mixes incompatible envelope shapes");
        }
        if (fields.stream().anyMatch(field -> !Set.of(
                "schemaVersion", "result", "resultType", "payload").contains(field))) {
            throw decoding(DecodeFailure.ROOT_STRUCTURE_INVALID, "$",
                    "External agent result root has an invalid structure");
        }
        if (root.containsKey("result")) {
            throw decoding(DecodeFailure.REQUIRED_FIELD_MISSING, "$.result",
                    "The nested external agent envelope is incomplete");
        }
        if (!root.containsKey("resultType")) {
            throw decoding(DecodeFailure.REQUIRED_FIELD_MISSING, "$.resultType",
                    "A required external agent field is missing");
        }
        if (!root.containsKey("payload")) {
            throw decoding(DecodeFailure.REQUIRED_FIELD_MISSING, "$.payload",
                    "A required external agent field is missing");
        }
        throw decoding(DecodeFailure.ROOT_STRUCTURE_INVALID, "$",
                "External agent result root has an invalid structure");
    }

    private enum RootShape { FLAT, NESTED }

    private static boolean payloadMatchesAnotherVariant(String resultType, Map<String, Object> payload) {
        if (payload.keySet().equals(payloadFields(resultType))) return false;
        return knownResultTypes().stream().filter(candidate -> !candidate.equals(resultType))
                .anyMatch(candidate -> payload.keySet().equals(payloadFields(candidate)));
    }

    @SuppressWarnings("unchecked")
    private static Set<String> payloadFields(String resultType) {
        Map<String, Object> properties = (Map<String, Object>) payloadSchema(resultType).get("properties");
        return properties.keySet();
    }

    private static Set<String> knownResultTypes() {
        return Set.of("TEST_PLAN", "TEST_IMPLEMENTATION_PROPOSAL", "TEST_EXECUTION_RESULT",
                "FAILURE_CLASSIFICATION", "REPAIR_PROPOSAL", "CODE_REVIEW_RESULT",
                "PAGE_OBJECT_CAPABILITY_MISSING", "PAGE_OBJECT_EXTENSION_PROPOSAL");
    }

    private static TestPlan plan(Map<String, Object> value, String basePath) {
        fields(value, Set.of("header", "scenarios"), basePath);
        List<TestPlan.TestScenario> scenarios = objects(value, "scenarios").stream().map(scenario -> {
            fields(scenario, Set.of("scenarioId", "title", "priority", "preconditions", "steps", "expected",
                    "requiredPageIds", "requiredStateIds", "requiredElementIds", "requiredData", "riskAreas",
                    "existingCoverage", "existingTestRefs", "knownUnknowns"), "$.payload.scenarios[]");
            List<TestPlan.TestStep> steps = objects(scenario, "steps").stream().map(step -> {
                fields(step, Set.of("order", "action", "pageId", "elementId", "valueRef"),
                        "$.payload.scenarios[].steps[]");
                return new TestPlan.TestStep(integer(step, "order"), string(step, "action"), nullableString(step, "pageId"),
                        nullableString(step, "elementId"), nullableString(step, "valueRef"));
            }).toList();
            List<TestPlan.ExpectedResult> expected = objects(scenario, "expected").stream().map(item -> {
                fields(item, Set.of("pageId", "stateId", "assertion", "evidenceRef"),
                        "$.payload.scenarios[].expected[]");
                return new TestPlan.ExpectedResult(nullableString(item, "pageId"), nullableString(item, "stateId"),
                        string(item, "assertion"), nullableString(item, "evidenceRef"));
            }).toList();
            return new TestPlan.TestScenario(string(scenario, "scenarioId"), string(scenario, "title"),
                    enumeration(scenario, "priority", TestPlan.Priority.class), strings(scenario, "preconditions"), steps,
                    expected, strings(scenario, "requiredPageIds"), strings(scenario, "requiredStateIds"),
                    strings(scenario, "requiredElementIds"), strings(scenario, "requiredData"), strings(scenario, "riskAreas"),
                    enumeration(scenario, "existingCoverage", TestPlan.CoverageDisposition.class),
                    strings(scenario, "existingTestRefs"), strings(scenario, "knownUnknowns"));
        }).toList();
        return new TestPlan(header(object(value, "header", basePath + ".header"), basePath + ".header"), scenarios);
    }

    private static TestImplementationProposal implementation(Map<String, Object> value, String basePath) {
        fields(value, Set.of("header", "scenarioId", "sourcePatch", "selectorAccessPolicy", "pageObjectApisUsed",
                "missingCapabilities"), basePath);
        List<TestImplementationProposal.PageObjectCapabilityMissing> missing = objects(value, "missingCapabilities",
                basePath + ".missingCapabilities").stream()
                .map(item -> {
                    fields(item, Set.of("pageId", "elementId", "requiredCapability", "reason"),
                            basePath + ".missingCapabilities[]");
                    return new TestImplementationProposal.PageObjectCapabilityMissing(
                            string(item, "pageId", basePath + ".missingCapabilities[].pageId"),
                            string(item, "elementId", basePath + ".missingCapabilities[].elementId"),
                            string(item, "requiredCapability", basePath + ".missingCapabilities[].requiredCapability"),
                            string(item, "reason", basePath + ".missingCapabilities[].reason"));
                }).toList();
        return new TestImplementationProposal(
                header(object(value, "header", basePath + ".header"), basePath + ".header"),
                string(value, "scenarioId", basePath + ".scenarioId"),
                string(value, "sourcePatch", basePath + ".sourcePatch"),
                enumeration(value, "selectorAccessPolicy", TestImplementationProposal.SelectorAccessPolicy.class,
                        basePath + ".selectorAccessPolicy"),
                pageObjectReferences(value, basePath), missing);
    }

    private static TestExecutionResult execution(Map<String, Object> value, String basePath) {
        fields(value, Set.of("header", "scenarioId", "compileOutcome", "executionOutcome", "frameworkResult",
                "durationMillis", "compileEvidenceRefs", "traceEvidenceRefs", "assertionEvidenceRefs", "runtimeEventRefs",
                "screenshotRefs", "selectorDiagnosticRefs", "failureSummary"), basePath);
        return new TestExecutionResult(header(object(value, "header", basePath + ".header"), basePath + ".header"),
                string(value, "scenarioId"),
                enumeration(value, "compileOutcome", TestExecutionResult.Outcome.class),
                enumeration(value, "executionOutcome", TestExecutionResult.Outcome.class),
                nullableString(value, "frameworkResult"), longValue(value, "durationMillis"),
                strings(value, "compileEvidenceRefs"), strings(value, "traceEvidenceRefs"),
                strings(value, "assertionEvidenceRefs"), strings(value, "runtimeEventRefs"),
                strings(value, "screenshotRefs"), strings(value, "selectorDiagnosticRefs"),
                nullableString(value, "failureSummary"));
    }

    private static FailureClassification classification(Map<String, Object> value, String basePath) {
        fields(value, Set.of("header", "category", "rootCause", "affectedIds", "evidenceRefs", "counterEvidenceRefs"),
                basePath);
        return new FailureClassification(header(object(value, "header", basePath + ".header"), basePath + ".header"),
                enumeration(value, "category", FailureClassification.Category.class), string(value, "rootCause"),
                strings(value, "affectedIds"), strings(value, "evidenceRefs"), strings(value, "counterEvidenceRefs"));
    }

    private static RepairProposal repair(Map<String, Object> value, String basePath) {
        fields(value, Set.of("header", "proposalId", "applicationPolicy", "whatChanged", "why", "oldSelector",
                "newSelector", "sourceDeclarationRef", "oldCandidateId", "newCandidateId", "classificationRef",
                "driftRef", "sameTargetEvidence", "stabilityEvidence", "affectedTests", "risks", "verificationPlan"),
                basePath);
        return new RepairProposal(header(object(value, "header", basePath + ".header"), basePath + ".header"),
                string(value, "proposalId"),
                enumeration(value, "applicationPolicy", RepairProposal.ApplicationPolicy.class), string(value, "whatChanged"),
                string(value, "why"), nullableString(value, "oldSelector"), nullableString(value, "newSelector"),
                nullableString(value, "sourceDeclarationRef"), nullableString(value, "oldCandidateId"),
                nullableString(value, "newCandidateId"), nullableString(value, "classificationRef"),
                nullableString(value, "driftRef"), strings(value, "sameTargetEvidence"),
                strings(value, "stabilityEvidence"), strings(value, "affectedTests"), strings(value, "risks"),
                strings(value, "verificationPlan"));
    }

    private static CodeReviewResult review(Map<String, Object> value, String basePath) {
        fields(value, Set.of("header", "verdict", "reviewedArtifactRefs", "findings"), basePath);
        List<Map<String,Object>> findingValues=objects(value,"findings",basePath+".findings");
        List<CodeReviewResult.Finding> findings=new ArrayList<>();
        for(int index=0;index<findingValues.size();index++){
            Map<String,Object> item=findingValues.get(index);
            String findingPath=basePath+".findings["+index+"]";
            fields(item, Set.of("category", "severity", "ruleId", "artifactRef", "problemCategory",
                            "requiredAction", "safeLocation", "evidenceAvailability"),
                    findingPath);
            String ruleId=string(item,"ruleId",findingPath+".ruleId");
            if(!CodeReviewResult.allowedRuleIds().contains(ruleId)){
                throw decoding(DecodeFailure.RULE_ID_INVALID,findingPath+".ruleId",
                        "Reviewer ruleId is not in the closed registry");
            }
            findings.add(new CodeReviewResult.Finding(
                    enumeration(item,"category",CodeReviewResult.Category.class,findingPath+".category"),
                    enumeration(item,"severity",CodeReviewResult.Severity.class,findingPath+".severity"),
                    ruleId,string(item,"artifactRef",findingPath+".artifactRef"),
                    enumeration(item,"problemCategory",CodeReviewResult.ProblemCategory.class,
                            findingPath+".problemCategory"),
                    enumeration(item,"requiredAction",CodeReviewResult.RequiredAction.class,
                            findingPath+".requiredAction"),
                    enumeration(item,"safeLocation",CodeReviewResult.SafeLocation.class,
                            findingPath+".safeLocation"),
                    enumeration(item,"evidenceAvailability",CodeReviewResult.EvidenceAvailability.class,
                            findingPath+".evidenceAvailability")));
        }
        ContractHeader header=header(object(value, "header", basePath + ".header"), basePath + ".header");
        if(header.status()!=ContractHeader.Status.COMPLETED){
            throw decoding(DecodeFailure.DECISION_CONTRADICTORY,basePath+".header.status",
                    "Reviewer header status must be COMPLETED");
        }
        CodeReviewResult.Verdict verdict=enumeration(value,"verdict",CodeReviewResult.Verdict.class,
                basePath+".verdict");
        boolean hasError=findings.stream().anyMatch(finding->finding.severity()==CodeReviewResult.Severity.ERROR);
        if(verdict!=CodeReviewResult.Verdict.APPROVE&&!hasError){
            throw decoding(DecodeFailure.REQUIRED_FINDING_MISSING,basePath+".findings",
                    "A non-approval reviewer decision requires an ERROR finding");
        }
        if(verdict==CodeReviewResult.Verdict.APPROVE&&hasError){
            throw decoding(DecodeFailure.DECISION_CONTRADICTORY,basePath+".verdict",
                    "APPROVE cannot contain an ERROR finding");
        }
        return new CodeReviewResult(header,verdict,strings(value,"reviewedArtifactRefs",
                basePath+".reviewedArtifactRefs"),findings);
    }

    private static PageObjectCapabilityMissing capability(Map<String, Object> value, String basePath) {
        fields(value, Set.of("pageId", "capability", "reason"), basePath);
        return new PageObjectCapabilityMissing(
                string(value, "pageId", basePath + ".pageId"),
                string(value, "capability", basePath + ".capability"),
                string(value, "reason", basePath + ".reason"));
    }

    private static PageObjectExtensionProposal extension(Map<String, Object> value, String basePath) {
        fields(value, Set.of("missing", "extensionClass", "proposedMethod", "evidence"), basePath);
        return new PageObjectExtensionProposal(capability(object(value, "missing", basePath + ".missing"),
                basePath + ".missing"),
                string(value, "extensionClass", basePath + ".extensionClass"),
                string(value, "proposedMethod", basePath + ".proposedMethod"),
                strings(value, "evidence", basePath + ".evidence"));
    }

    private static ContractHeader header(Map<String, Object> value, String basePath) {
        fields(value, Set.of("schemaVersion", "status", "evidence", "limitations", "confidence"), basePath);
        return new ContractHeader(integer(value, "schemaVersion", basePath + ".schemaVersion"),
                enumeration(value, "status", ContractHeader.Status.class, basePath + ".status"),
                strings(value, "evidence", basePath + ".evidence"),
                strings(value, "limitations", basePath + ".limitations"),
                enumeration(value, "confidence", ContractHeader.Confidence.class, basePath + ".confidence"));
    }

    private static List<String> instructions(AgentExecutor.Role role) {
        List<String> common = List.of("Return exactly one JSON object matching the supplied schema.",
                "Do not invent selectors or bypass Page Objects.", "Do not include secrets or authentication state.");
        String roleInstruction = switch (role) {
            case PLANNER, TEST_ARCHITECT, SCENARIO_DESIGNER -> "Produce a TestPlan grounded only in supplied context.";
            case IMPLEMENTER, TEST_IMPLEMENTER, UNIT_TEST_AGENT -> "Use existing Page Object APIs only; no raw Selenium selectors. "
                    + "For pageObjectApisUsed, copy exact values from the supplied allowedPageObjectApiReferences list; "
                    + "do not add a return type, argument values, comments, or explanatory text. "
                    + "When the fingerprint-bound manifest and runtime contract provide a method and construction expression, "
                    + "use them instead of returning a capability refusal. "
                    + "For credential scenarios, do not embed or assign real credentials and do not create credential-named "
                    + "key/value variables; pass clearly synthetic test placeholders inline to the existing Page Object API. "
                    + "The complete sourcePatch must be unchanged by credential redaction: never emit password, token, secret, "
                    + "cookie, authorization, or session as a key followed by ':' or '=', including in declarations or comments.";
            case TEST_VERIFIER -> "Return bounded execution evidence without claiming unobserved outcomes.";
            case FAILURE_CLASSIFIER -> "Classify from evidence; UNKNOWN is valid.";
            case REPAIRER, STABILIZER -> "Repairs remain PROPOSE_ONLY; selectors must come from Selector Intelligence evidence.";
            case REVIEWER, CODE_REVIEWER -> "Reject selector bypass, sleeps, assertion weakening, and unrelated changes. "
                    + "Review only artifacts cataloged by the host-authored ReviewerEvidencePack. Copy every "
                    + "requiredReviewedArtifactRef exactly. For each finding, select one complete "
                    + "supportedFindingContract from that artifact and copy its category, ruleId, problemCategory, "
                    + "requiredAction, and safeLocation exactly. REQUEST_CHANGES and BLOCK require only typed ERROR "
                    + "findings supported by the supplied evidence; APPROVE must contain no findings. Do not return "
                    + "free-form detail.";
        };
        List<String> result = new ArrayList<>(common);
        result.add(roleInstruction);
        return List.copyOf(result);
    }

    private static List<String> allowedTypes(AgentExecutor.Role role) {
        return switch (role) {
            case PLANNER, TEST_ARCHITECT, SCENARIO_DESIGNER -> List.of("TEST_PLAN");
            case IMPLEMENTER, TEST_IMPLEMENTER, UNIT_TEST_AGENT -> List.of("TEST_IMPLEMENTATION_PROPOSAL",
                    "PAGE_OBJECT_CAPABILITY_MISSING", "PAGE_OBJECT_EXTENSION_PROPOSAL");
            case TEST_VERIFIER -> List.of("TEST_EXECUTION_RESULT");
            case FAILURE_CLASSIFIER -> List.of("FAILURE_CLASSIFICATION");
            case REPAIRER, STABILIZER -> List.of("FAILURE_CLASSIFICATION", "REPAIR_PROPOSAL");
            case REVIEWER, CODE_REVIEWER -> List.of("CODE_REVIEW_RESULT");
        };
    }

    private static void fields(Map<String, Object> value, Set<String> expected, String where) {
        if (!value.keySet().equals(expected)) {
            List<String> missing = expected.stream().filter(field -> !value.containsKey(field)).sorted().toList();
            if (!missing.isEmpty()) {
                throw decoding(DecodeFailure.REQUIRED_FIELD_MISSING, fieldLocation(where, missing.get(0)),
                        "A required external agent field is missing");
            }
            throw decoding("$".equals(where) ? DecodeFailure.ROOT_STRUCTURE_INVALID
                            : DecodeFailure.RESULT_VARIANT_INVALID, where,
                    "External agent object contains unexpected fields");
        }
    }

    private static String fieldLocation(String where, String field) {
        return "$".equals(where) ? "$." + field : where + "." + field;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Map<String, Object> value, String name) {
        return object(value, name, "$.payload." + name);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Map<String, Object> value, String name, String location) {
        if (!value.containsKey(name)) {
            throw decoding(DecodeFailure.REQUIRED_FIELD_MISSING, location,
                    "A required external agent field is missing");
        }
        Object result = value.get(name);
        if (!(result instanceof Map<?, ?>)) throw decoding(DecodeFailure.FIELD_TYPE_INVALID, location,
                "External agent field must be an object");
        return (Map<String, Object>) result;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> objects(Map<String, Object> value, String name) {
        return objects(value, name, "$.payload." + name);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> objects(Map<String, Object> value, String name, String location) {
        if (!value.containsKey(name)) {
            throw decoding(DecodeFailure.REQUIRED_FIELD_MISSING, location,
                    "A required external agent field is missing");
        }
        Object result = value.get(name);
        if (!(result instanceof List<?> list)) throw decoding(DecodeFailure.FIELD_TYPE_INVALID, location,
                "External agent field must be an array");
        List<Map<String, Object>> mapped = new ArrayList<>();
        for (int index = 0; index < list.size(); index++) {
            Object item = list.get(index);
            if (!(item instanceof Map<?, ?>)) throw decoding(DecodeFailure.ARRAY_ELEMENT_INVALID,
                    location + "[" + index + "]", "External agent array entry must be an object");
            mapped.add((Map<String, Object>) item);
        }
        return List.copyOf(mapped);
    }

    private static String string(Map<String, Object> value, String name) {
        return string(value, name, "$.payload." + name);
    }

    private static String string(Map<String, Object> value, String name, String location) {
        if (!value.containsKey(name)) {
            throw decoding(DecodeFailure.REQUIRED_FIELD_MISSING, location,
                    "A required external agent field is missing");
        }
        Object result = value.get(name);
        if (!(result instanceof String text)) {
            throw decoding(DecodeFailure.FIELD_TYPE_INVALID, location,
                    "External agent field must be a string");
        }
        if (text.isBlank()) {
            throw decoding(DecodeFailure.FIELD_TYPE_INVALID, location,
                    "External agent string field must not be blank");
        }
        return text;
    }

    private static String nullableString(Map<String, Object> value, String name) {
        return nullableString(value, name, "$.payload." + name);
    }

    private static String nullableString(Map<String, Object> value, String name, String location) {
        if (!value.containsKey(name)) {
            throw decoding(DecodeFailure.REQUIRED_FIELD_MISSING, location,
                    "A required external agent field is missing");
        }
        Object result = value.get(name);
        if (result == null) return null;
        if (!(result instanceof String text)) throw decoding(DecodeFailure.FIELD_TYPE_INVALID, location,
                "External agent field must be a string or null");
        return text;
    }

    private static int integer(Map<String, Object> value, String name) {
        return integer(value, name, "$.payload." + name);
    }

    private static int integer(Map<String, Object> value, String name, String location) {
        if (!value.containsKey(name)) {
            throw decoding(DecodeFailure.REQUIRED_FIELD_MISSING, location,
                    "A required external agent field is missing");
        }
        Object raw = value.get(name);
        if (!(raw instanceof Number number) || number.doubleValue() != number.longValue()
                || number.longValue() < Integer.MIN_VALUE || number.longValue() > Integer.MAX_VALUE) {
            throw decoding(DecodeFailure.FIELD_TYPE_INVALID, location,
                    "External agent field must be an integer");
        }
        return (int) number.longValue();
    }

    private static long longValue(Map<String, Object> value, String name) {
        return longValue(value, name, "$.payload." + name);
    }

    private static long longValue(Map<String, Object> value, String name, String location) {
        if (!value.containsKey(name)) {
            throw decoding(DecodeFailure.REQUIRED_FIELD_MISSING, location,
                    "A required external agent field is missing");
        }
        Object result = value.get(name);
        if (!(result instanceof Number number) || number.doubleValue() != number.longValue()) {
            throw decoding(DecodeFailure.FIELD_TYPE_INVALID, location,
                    "External agent field must be an integer");
        }
        return number.longValue();
    }

    private static List<String> strings(Map<String, Object> value, String name) {
        return strings(value, name, "$.payload." + name);
    }

    private static List<String> strings(Map<String, Object> value, String name, String location) {
        if (!value.containsKey(name)) {
            throw decoding(DecodeFailure.REQUIRED_FIELD_MISSING, location,
                    "A required external agent field is missing");
        }
        Object result = value.get(name);
        if (!(result instanceof List<?> list)) throw decoding(DecodeFailure.FIELD_TYPE_INVALID, location,
                "External agent field must be an array");
        List<String> mapped = new ArrayList<>();
        for (int index = 0; index < list.size(); index++) {
            Object item = list.get(index);
            if (!(item instanceof String text) || text.isBlank()) throw decoding(
                    DecodeFailure.ARRAY_ELEMENT_INVALID, location + "[" + index + "]",
                    "External agent array entry must be a non-empty string");
            mapped.add(text);
        }
        return List.copyOf(mapped);
    }

    private static List<String> pageObjectReferences(Map<String, Object> value, String basePath) {
        String location = basePath + ".pageObjectApisUsed";
        if (!value.containsKey("pageObjectApisUsed")) {
            throw decoding(DecodeFailure.REQUIRED_FIELD_MISSING, location,
                    "A required external agent field is missing");
        }
        Object result = value.get("pageObjectApisUsed");
        if (!(result instanceof List<?> list)) {
            throw decoding(DecodeFailure.FIELD_TYPE_INVALID, location,
                    "pageObjectApisUsed must be an array");
        }
        if (list.size() > MAX_PAGE_OBJECT_API_REFERENCES) {
            throw decoding(DecodeFailure.FIELD_TYPE_INVALID, location,
                    "pageObjectApisUsed exceeds its item limit");
        }
        List<String> mapped = new ArrayList<>();
        for (int index = 0; index < list.size(); index++) {
            Object item = list.get(index);
            if (!(item instanceof String text)) {
                throw decoding(DecodeFailure.ARRAY_ELEMENT_INVALID,
                        location + "[" + index + "]",
                        "pageObjectApisUsed entry must be a string");
            }
            if (text.isBlank() || text.length() > MAX_PAGE_OBJECT_API_REFERENCE_LENGTH) {
                throw decoding(DecodeFailure.FIELD_TYPE_INVALID,
                        location + "[" + index + "]",
                        "pageObjectApisUsed entry must be a bounded non-empty string");
            }
            mapped.add(text);
        }
        return List.copyOf(mapped);
    }

    private static <E extends Enum<E>> E enumeration(Map<String, Object> value, String name, Class<E> type) {
        return enumeration(value, name, type, "$.payload." + name);
    }

    private static <E extends Enum<E>> E enumeration(Map<String, Object> value, String name, Class<E> type,
                                                      String location) {
        String enumName = string(value, name, location);
        try {
            return Enum.valueOf(type, enumName);
        } catch (IllegalArgumentException failure) {
            throw decoding(DecodeFailure.FIELD_TYPE_INVALID, location,
                    "External agent field has an unsupported enum value", failure);
        }
    }

    public record DecodedResult(String resultType, Object payload, String payloadBasePath) { }

    enum DecodeFailure {
        MALFORMED_JSON,
        ROOT_STRUCTURE_INVALID,
        RESULT_VARIANT_INVALID,
        SCHEMA_VERSION_UNSUPPORTED,
        REQUIRED_FIELD_MISSING,
        FIELD_TYPE_INVALID,
        ARRAY_ELEMENT_INVALID,
        ROLE_TYPE_MISMATCH,
        DOMAIN_MAPPING_FAILED,
        RULE_ID_INVALID,
        REQUIRED_FINDING_MISSING,
        DECISION_CONTRADICTORY,
        /** @deprecated retained only for source compatibility with pre-0.5 diagnostics. */
        @Deprecated SCHEMA_INVALID,
        /** @deprecated retained only for source compatibility with pre-0.5 diagnostics. */
        @Deprecated DOMAIN_INVALID
    }

    private static StrictJson.JsonFormatException decoding(DecodeFailure failure, String location, String message) {
        return decoding(failure, location, message, null);
    }

    private static StrictJson.JsonFormatException decoding(DecodeFailure failure, String location, String message,
                                                            Throwable cause) {
        String diagnostic = "[" + failure.name() + "@" + location + "] " + message;
        return cause == null ? new StrictJson.JsonFormatException(diagnostic)
                : new StrictJson.JsonFormatException(diagnostic, cause);
    }

    static DecodeFailure decodeFailure(RuntimeException failure) {
        DecodeFailure decoded = decodeMarker(failure);
        return decoded == null ? DecodeFailure.ROOT_STRUCTURE_INVALID : decoded;
    }

    private static DecodeFailure decodeMarker(RuntimeException failure) {
        String message = failure.getMessage();
        if (message != null && message.startsWith("[")) {
            int marker = message.indexOf('@');
            if (marker > 1 && diagnosticMarkerEnd(message, marker) > marker + 1) {
                try {
                    return DecodeFailure.valueOf(message.substring(1, marker));
                } catch (IllegalArgumentException ignored) {
                    // Fall through to the closed schema-invalid default.
                }
            }
        }
        return null;
    }

    static String decodeLocation(RuntimeException failure) {
        String message = failure.getMessage();
        if (message != null && message.startsWith("[")) {
            int marker = message.indexOf('@');
            int end = diagnosticMarkerEnd(message, marker);
            if (marker > 0 && end > marker + 1) return message.substring(marker + 1, end);
        }
        return "result";
    }

    static String decodeDetail(RuntimeException failure) {
        String message = failure.getMessage();
        if (message == null || message.isBlank()) return "Structured output does not match the required contract";
        int separator = message.startsWith("[")
                ? diagnosticMarkerEnd(message, message.indexOf('@')) : -1;
        String detail = separator >= 0 ? message.substring(separator + 2) : message;
        detail = detail.replaceAll("[^A-Za-z0-9_.$,;:=\\[\\] ()-]", "?").trim();
        if (detail.isBlank()) return "Structured output does not match the required contract";
        return detail.length() <= 256 ? detail : detail.substring(0, 256);
    }

    private static int diagnosticMarkerEnd(String message, int categorySeparator) {
        if (message == null || !message.startsWith("[") || categorySeparator <= 1) return -1;
        int end = message.indexOf("] ", categorySeparator + 1);
        if (end <= categorySeparator + 1) return -1;
        String location = message.substring(categorySeparator + 1, end);
        if (!location.startsWith("$") && !location.equals("result") && !location.startsWith("result.")) return -1;
        for (int index = 0; index < location.length(); index++) {
            char value = location.charAt(index);
            if (!(Character.isLetterOrDigit(value) || value == '$' || value == '.' || value == '_'
                    || value == '[' || value == ']')) return -1;
        }
        return end;
    }
}
