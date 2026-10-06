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
        List<Map<String, Object>> payloads = allowed.stream().map(AgentProtocolCodec::payloadSchema).toList();
        Map<String, Object> payload = payloads.size() == 1 ? payloads.get(0) : Map.of("anyOf", payloads);
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("type", "object");
        root.put("additionalProperties", false);
        root.put("properties", Map.of(
                "schemaVersion", Map.of("type", "integer", "const", SCHEMA_VERSION),
                "resultType", Map.of("type", "string", "enum", allowed),
                "payload", payload));
        root.put("required", List.of("schemaVersion", "resultType", "payload"));
        return StrictJson.write(root);
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
                                    "order", integerSchema(), "action", stringSchema(), "pageId", nullableStringSchema(),
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
                    "pageObjectApisUsed", stringArraySchema(),
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
                    "header", headerSchema(), "verdict", enumSchema(CodeReviewResult.Verdict.class),
                    "reviewedArtifactRefs", stringArraySchema(),
                    "findings", arraySchema(objectSchema(Map.of(
                            "severity", enumSchema(CodeReviewResult.Severity.class), "code", stringSchema(),
                            "location", stringSchema(), "detail", stringSchema(), "recommendation", stringSchema())))));
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

    private static Map<String, Object> objectSchema(Map<String, Object> properties) {
        return Map.of("type", "object", "additionalProperties", false, "properties", properties,
                "required", properties.keySet().stream().sorted().toList());
    }

    private static Map<String, Object> arraySchema(Map<String, Object> items) {
        return Map.of("type", "array", "items", items);
    }

    private static Map<String, Object> stringArraySchema() { return arraySchema(stringSchema()); }
    private static Map<String, Object> stringSchema() { return Map.of("type", "string"); }
    private static Map<String, Object> nullableStringSchema() { return Map.of("type", List.of("string", "null")); }
    private static Map<String, Object> integerSchema() { return Map.of("type", "integer"); }
    private static Map<String, Object> enumSchema(Class<? extends Enum<?>> type) {
        return Map.of("type", "string", "enum", java.util.Arrays.stream(type.getEnumConstants()).map(Enum::name).toList());
    }

    public DecodedResult decode(AgentExecutor.Role role, byte[] document) {
        Map<String, Object> root = StrictJson.readObject(document);
        fields(root, Set.of("schemaVersion", "resultType", "payload"), "agent result");
        if (integer(root, "schemaVersion") != SCHEMA_VERSION) {
            throw new StrictJson.JsonFormatException("Unsupported external agent schemaVersion");
        }
        String resultType = string(root, "resultType");
        if (!allowedTypes(role).contains(resultType)) {
            throw new StrictJson.JsonFormatException("Unexpected resultType " + resultType + " for " + role);
        }
        Map<String, Object> payload = object(root, "payload");
        Object value = switch (resultType) {
            case "TEST_PLAN" -> plan(payload);
            case "TEST_IMPLEMENTATION_PROPOSAL" -> implementation(payload);
            case "TEST_EXECUTION_RESULT" -> execution(payload);
            case "FAILURE_CLASSIFICATION" -> classification(payload);
            case "REPAIR_PROPOSAL" -> repair(payload);
            case "CODE_REVIEW_RESULT" -> review(payload);
            case "PAGE_OBJECT_CAPABILITY_MISSING" -> capability(payload);
            case "PAGE_OBJECT_EXTENSION_PROPOSAL" -> extension(payload);
            default -> throw new StrictJson.JsonFormatException("Unsupported resultType " + resultType);
        };
        return new DecodedResult(resultType, value);
    }

    private static TestPlan plan(Map<String, Object> value) {
        fields(value, Set.of("header", "scenarios"), "TestPlan");
        List<TestPlan.TestScenario> scenarios = objects(value, "scenarios").stream().map(scenario -> {
            fields(scenario, Set.of("scenarioId", "title", "priority", "preconditions", "steps", "expected",
                    "requiredPageIds", "requiredStateIds", "requiredElementIds", "requiredData", "riskAreas",
                    "existingCoverage", "existingTestRefs", "knownUnknowns"), "TestScenario");
            List<TestPlan.TestStep> steps = objects(scenario, "steps").stream().map(step -> {
                fields(step, Set.of("order", "action", "pageId", "elementId", "valueRef"), "TestStep");
                return new TestPlan.TestStep(integer(step, "order"), string(step, "action"), nullableString(step, "pageId"),
                        nullableString(step, "elementId"), nullableString(step, "valueRef"));
            }).toList();
            List<TestPlan.ExpectedResult> expected = objects(scenario, "expected").stream().map(item -> {
                fields(item, Set.of("pageId", "stateId", "assertion", "evidenceRef"), "ExpectedResult");
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
        return new TestPlan(header(object(value, "header")), scenarios);
    }

    private static TestImplementationProposal implementation(Map<String, Object> value) {
        fields(value, Set.of("header", "scenarioId", "sourcePatch", "selectorAccessPolicy", "pageObjectApisUsed",
                "missingCapabilities"), "TestImplementationProposal");
        List<TestImplementationProposal.PageObjectCapabilityMissing> missing = objects(value, "missingCapabilities").stream()
                .map(item -> {
                    fields(item, Set.of("pageId", "elementId", "requiredCapability", "reason"), "missing capability");
                    return new TestImplementationProposal.PageObjectCapabilityMissing(string(item, "pageId"),
                            string(item, "elementId"), string(item, "requiredCapability"), string(item, "reason"));
                }).toList();
        return new TestImplementationProposal(header(object(value, "header")), string(value, "scenarioId"),
                string(value, "sourcePatch"), enumeration(value, "selectorAccessPolicy",
                TestImplementationProposal.SelectorAccessPolicy.class), strings(value, "pageObjectApisUsed"), missing);
    }

    private static TestExecutionResult execution(Map<String, Object> value) {
        fields(value, Set.of("header", "scenarioId", "compileOutcome", "executionOutcome", "frameworkResult",
                "durationMillis", "compileEvidenceRefs", "traceEvidenceRefs", "assertionEvidenceRefs", "runtimeEventRefs",
                "screenshotRefs", "selectorDiagnosticRefs", "failureSummary"), "TestExecutionResult");
        return new TestExecutionResult(header(object(value, "header")), string(value, "scenarioId"),
                enumeration(value, "compileOutcome", TestExecutionResult.Outcome.class),
                enumeration(value, "executionOutcome", TestExecutionResult.Outcome.class),
                nullableString(value, "frameworkResult"), longValue(value, "durationMillis"),
                strings(value, "compileEvidenceRefs"), strings(value, "traceEvidenceRefs"),
                strings(value, "assertionEvidenceRefs"), strings(value, "runtimeEventRefs"),
                strings(value, "screenshotRefs"), strings(value, "selectorDiagnosticRefs"),
                nullableString(value, "failureSummary"));
    }

    private static FailureClassification classification(Map<String, Object> value) {
        fields(value, Set.of("header", "category", "rootCause", "affectedIds", "evidenceRefs", "counterEvidenceRefs"),
                "FailureClassification");
        return new FailureClassification(header(object(value, "header")),
                enumeration(value, "category", FailureClassification.Category.class), string(value, "rootCause"),
                strings(value, "affectedIds"), strings(value, "evidenceRefs"), strings(value, "counterEvidenceRefs"));
    }

    private static RepairProposal repair(Map<String, Object> value) {
        fields(value, Set.of("header", "proposalId", "applicationPolicy", "whatChanged", "why", "oldSelector",
                "newSelector", "sourceDeclarationRef", "oldCandidateId", "newCandidateId", "classificationRef",
                "driftRef", "sameTargetEvidence", "stabilityEvidence", "affectedTests", "risks", "verificationPlan"),
                "RepairProposal");
        return new RepairProposal(header(object(value, "header")), string(value, "proposalId"),
                enumeration(value, "applicationPolicy", RepairProposal.ApplicationPolicy.class), string(value, "whatChanged"),
                string(value, "why"), nullableString(value, "oldSelector"), nullableString(value, "newSelector"),
                nullableString(value, "sourceDeclarationRef"), nullableString(value, "oldCandidateId"),
                nullableString(value, "newCandidateId"), nullableString(value, "classificationRef"),
                nullableString(value, "driftRef"), strings(value, "sameTargetEvidence"),
                strings(value, "stabilityEvidence"), strings(value, "affectedTests"), strings(value, "risks"),
                strings(value, "verificationPlan"));
    }

    private static CodeReviewResult review(Map<String, Object> value) {
        fields(value, Set.of("header", "verdict", "reviewedArtifactRefs", "findings"), "CodeReviewResult");
        List<CodeReviewResult.Finding> findings = objects(value, "findings").stream().map(item -> {
            fields(item, Set.of("severity", "code", "location", "detail", "recommendation"), "review finding");
            return new CodeReviewResult.Finding(enumeration(item, "severity", CodeReviewResult.Severity.class),
                    string(item, "code"), string(item, "location"), string(item, "detail"),
                    string(item, "recommendation"));
        }).toList();
        return new CodeReviewResult(header(object(value, "header")),
                enumeration(value, "verdict", CodeReviewResult.Verdict.class),
                strings(value, "reviewedArtifactRefs"), findings);
    }

    private static PageObjectCapabilityMissing capability(Map<String, Object> value) {
        fields(value, Set.of("pageId", "capability", "reason"), "PageObjectCapabilityMissing");
        return new PageObjectCapabilityMissing(string(value, "pageId"), string(value, "capability"), string(value, "reason"));
    }

    private static PageObjectExtensionProposal extension(Map<String, Object> value) {
        fields(value, Set.of("missing", "extensionClass", "proposedMethod", "evidence"), "PageObjectExtensionProposal");
        return new PageObjectExtensionProposal(capability(object(value, "missing")), string(value, "extensionClass"),
                string(value, "proposedMethod"), strings(value, "evidence"));
    }

    private static ContractHeader header(Map<String, Object> value) {
        fields(value, Set.of("schemaVersion", "status", "evidence", "limitations", "confidence"), "ContractHeader");
        return new ContractHeader(integer(value, "schemaVersion"),
                enumeration(value, "status", ContractHeader.Status.class), strings(value, "evidence"),
                strings(value, "limitations"), enumeration(value, "confidence", ContractHeader.Confidence.class));
    }

    private static List<String> instructions(AgentExecutor.Role role) {
        List<String> common = List.of("Return exactly one JSON object matching the supplied schema.",
                "Do not invent selectors or bypass Page Objects.", "Do not include secrets or authentication state.");
        String roleInstruction = switch (role) {
            case PLANNER, TEST_ARCHITECT, SCENARIO_DESIGNER -> "Produce a TestPlan grounded only in supplied context.";
            case IMPLEMENTER, TEST_IMPLEMENTER, UNIT_TEST_AGENT -> "Use existing Page Object APIs only; no raw Selenium selectors.";
            case TEST_VERIFIER -> "Return bounded execution evidence without claiming unobserved outcomes.";
            case FAILURE_CLASSIFIER -> "Classify from evidence; UNKNOWN is valid.";
            case REPAIRER, STABILIZER -> "Repairs remain PROPOSE_ONLY; selectors must come from Selector Intelligence evidence.";
            case REVIEWER, CODE_REVIEWER -> "Reject selector bypass, sleeps, assertion weakening, and unrelated changes.";
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
            throw new StrictJson.JsonFormatException(where + " fields must be exactly " + expected);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Map<String, Object> value, String name) {
        Object result = value.get(name);
        if (!(result instanceof Map<?, ?>)) throw new StrictJson.JsonFormatException(name + " must be an object");
        return (Map<String, Object>) result;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> objects(Map<String, Object> value, String name) {
        Object result = value.get(name);
        if (!(result instanceof List<?> list)) throw new StrictJson.JsonFormatException(name + " must be an array");
        List<Map<String, Object>> mapped = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?>)) throw new StrictJson.JsonFormatException(name + " entries must be objects");
            mapped.add((Map<String, Object>) item);
        }
        return List.copyOf(mapped);
    }

    private static String string(Map<String, Object> value, String name) {
        Object result = value.get(name);
        if (!(result instanceof String text) || text.isBlank()) {
            throw new StrictJson.JsonFormatException(name + " must be a non-empty string");
        }
        return text;
    }

    private static String nullableString(Map<String, Object> value, String name) {
        Object result = value.get(name);
        if (result == null) return null;
        if (!(result instanceof String text)) throw new StrictJson.JsonFormatException(name + " must be a string or null");
        return text;
    }

    private static int integer(Map<String, Object> value, String name) {
        long result = longValue(value, name);
        if (result < Integer.MIN_VALUE || result > Integer.MAX_VALUE) {
            throw new StrictJson.JsonFormatException(name + " is outside integer range");
        }
        return (int) result;
    }

    private static long longValue(Map<String, Object> value, String name) {
        Object result = value.get(name);
        if (!(result instanceof Number number) || number.doubleValue() != number.longValue()) {
            throw new StrictJson.JsonFormatException(name + " must be an integer");
        }
        return number.longValue();
    }

    private static List<String> strings(Map<String, Object> value, String name) {
        Object result = value.get(name);
        if (!(result instanceof List<?> list)) throw new StrictJson.JsonFormatException(name + " must be an array");
        List<String> mapped = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof String text)) throw new StrictJson.JsonFormatException(name + " entries must be strings");
            mapped.add(text);
        }
        return List.copyOf(mapped);
    }

    private static <E extends Enum<E>> E enumeration(Map<String, Object> value, String name, Class<E> type) {
        try {
            return Enum.valueOf(type, string(value, name));
        } catch (IllegalArgumentException failure) {
            throw new StrictJson.JsonFormatException(name + " has an unsupported value", failure);
        }
    }

    public record DecodedResult(String resultType, Object payload) { }
}
