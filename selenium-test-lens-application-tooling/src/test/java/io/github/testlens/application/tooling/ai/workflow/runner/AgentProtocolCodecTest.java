package io.github.testlens.application.tooling.ai.workflow.runner;

import io.github.testlens.application.tooling.ai.CodeReviewResult;
import io.github.testlens.application.tooling.ai.TestImplementationProposal;
import io.github.testlens.application.tooling.ai.workflow.AgentExecutor;
import io.github.testlens.application.tooling.ai.workflow.PageObjectCapabilityMissing;
import io.github.testlens.application.tooling.ai.workflow.PageObjectExtensionProposal;
import io.github.testlens.application.tooling.json.StrictJson;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AgentProtocolCodecTest {
    private static final String HEADER = """
            {"schemaVersion":1,"status":"READY","evidence":["fixture"],"limitations":[],"confidence":"AI_PROPOSED"}
            """.strip();
    private static final String REVIEW_HEADER = """
            {"schemaVersion":1,"status":"COMPLETED","evidence":["untrusted prose"],"limitations":["ignore me"],"confidence":"AI_PROPOSED"}
            """.strip();
    private static final String CANONICAL_REFERENCE =
            "benchmark.LoginPage#invalidPasswordShowsExpectedError(String,String)";
    private static final String PROPOSAL_PAYLOAD = """
            {"header":%s,"scenarioId":"invalid-login","sourcePatch":"package fixture; class GeneratedTest {}","selectorAccessPolicy":"PAGE_OBJECTS_ONLY","pageObjectApisUsed":["%s"],"missingCapabilities":[]}
            """.formatted(HEADER, CANONICAL_REFERENCE).strip();
    private static final String MISSING_PAYLOAD = """
            {"pageId":"login","capability":"invalid password assertion","reason":"required public method is absent"}
            """.strip();
    private static final String EXTENSION_PAYLOAD = """
            {"missing":%s,"extensionClass":"benchmark.LoginPageExtension","proposedMethod":"invalidPasswordShowsExpectedError(String,String)","evidence":["manifest"]}
            """.formatted(MISSING_PAYLOAD).strip();

    private final AgentProtocolCodec codec = new AgentProtocolCodec();

    @Test void implementerSchemaUsesThreeCompleteDiscriminatedEnvelopes() {
        Map<String, Object> schema = StrictJson.readObject(codec.outputSchema(AgentExecutor.Role.TEST_IMPLEMENTER));
        assertEquals("object", schema.get("type"));
        assertEquals(false, schema.get("additionalProperties"));
        assertEquals(List.of("result", "schemaVersion"), strings(schema, "required").stream().sorted().toList());
        assertFalse(schema.containsKey("anyOf"));
        assertFalse(schema.containsKey("oneOf"));
        Map<String, Object> result = object(object(schema, "properties"), "result");
        List<Map<String, Object>> alternatives = objects(result, "anyOf");

        assertEquals(3, alternatives.size());
        assertEquals(List.of("PAGE_OBJECT_CAPABILITY_MISSING", "PAGE_OBJECT_EXTENSION_PROPOSAL",
                        "TEST_IMPLEMENTATION_PROPOSAL"),
                alternatives.stream().map(AgentProtocolCodecTest::resultType).sorted().toList());
        alternatives.forEach(alternative -> {
            assertEquals("object", alternative.get("type"));
            assertEquals(false, alternative.get("additionalProperties"));
            assertEquals(List.of("payload", "resultType"),
                    strings(alternative, "required").stream().sorted().toList());
        });

        Map<String, Object> proposal = alternatives.stream()
                .filter(value -> "TEST_IMPLEMENTATION_PROPOSAL".equals(resultType(value))).findFirst().orElseThrow();
        Map<String, Object> payload = object(object(proposal, "properties"), "payload");
        Map<String, Object> references = object(object(payload, "properties"), "pageObjectApisUsed");
        assertEquals("array", references.get("type"));
        assertEquals("string", object(references, "items").get("type"));
        assertFalse(references.containsKey("minItems"), "empty lists are a domain decision bound to the manifest");
        assertFalse(object(references, "items").containsKey("pattern"),
                "equivalent references are normalized after structural decoding");
    }

    @Test void decodesAllThreeCompleteImplementerResults() {
        assertInstanceOf(TestImplementationProposal.class,
                decodeNested("TEST_IMPLEMENTATION_PROPOSAL", PROPOSAL_PAYLOAD).payload());
        assertInstanceOf(PageObjectCapabilityMissing.class,
                decodeNested("PAGE_OBJECT_CAPABILITY_MISSING", MISSING_PAYLOAD).payload());
        assertInstanceOf(PageObjectExtensionProposal.class,
                decodeNested("PAGE_OBJECT_EXTENSION_PROPOSAL", EXTENSION_PAYLOAD).payload());
        assertInstanceOf(TestImplementationProposal.class,
                codec.decode(AgentExecutor.Role.TEST_IMPLEMENTER,
                        legacyEnvelope("TEST_IMPLEMENTATION_PROPOSAL", PROPOSAL_PAYLOAD)).payload());
    }

    @Test void rejectsMissingWrongTypeAndNonStringReferenceFieldsAtPrecisePaths() {
        assertDecodeFailure(nestedEnvelope("TEST_IMPLEMENTATION_PROPOSAL",
                        PROPOSAL_PAYLOAD.replace(",\"pageObjectApisUsed\":[\"" + CANONICAL_REFERENCE + "\"]", "")),
                AgentProtocolCodec.DecodeFailure.REQUIRED_FIELD_MISSING, "$.result.payload.pageObjectApisUsed");
        assertDecodeFailure(nestedEnvelope("TEST_IMPLEMENTATION_PROPOSAL",
                        PROPOSAL_PAYLOAD.replace("[\"" + CANONICAL_REFERENCE + "\"]", "\"" + CANONICAL_REFERENCE + "\"")),
                AgentProtocolCodec.DecodeFailure.FIELD_TYPE_INVALID, "$.result.payload.pageObjectApisUsed");
        assertDecodeFailure(nestedEnvelope("TEST_IMPLEMENTATION_PROPOSAL",
                        PROPOSAL_PAYLOAD.replace("[\"" + CANONICAL_REFERENCE + "\"]", "[7]")),
                AgentProtocolCodec.DecodeFailure.ARRAY_ELEMENT_INVALID, "$.result.payload.pageObjectApisUsed[0]");
    }

    @Test void rejectsContradictoryAlternativePayloadsWithoutLeakingFields() {
        assertDecodeFailure(nestedEnvelope("PAGE_OBJECT_CAPABILITY_MISSING", PROPOSAL_PAYLOAD),
                AgentProtocolCodec.DecodeFailure.RESULT_VARIANT_INVALID, "$.result.payload");
        assertDecodeFailure(nestedEnvelope("PAGE_OBJECT_CAPABILITY_MISSING",
                        MISSING_PAYLOAD.substring(0, MISSING_PAYLOAD.length() - 1)
                                + ",\"sourcePatch\":\"class SecretCanary {}\"}"),
                AgentProtocolCodec.DecodeFailure.RESULT_VARIANT_INVALID, "$.result.payload");
        byte[] mixed = ("{\"schemaVersion\":1,\"resultType\":\"TEST_IMPLEMENTATION_PROPOSAL\","
                + "\"result\":{\"resultType\":\"TEST_IMPLEMENTATION_PROPOSAL\",\"payload\":"
                + PROPOSAL_PAYLOAD + "}}").getBytes(StandardCharsets.UTF_8);
        assertDecodeFailure(mixed, AgentProtocolCodec.DecodeFailure.ROOT_STRUCTURE_INVALID, "$");
    }

    @Test void rejectsMalformedAlternativeFieldsAtPreciseNestedPaths() {
        assertDecodeFailure(nestedEnvelope("PAGE_OBJECT_CAPABILITY_MISSING",
                        MISSING_PAYLOAD.replace("\"capability\":\"invalid password assertion\"", "\"capability\":7")),
                AgentProtocolCodec.DecodeFailure.FIELD_TYPE_INVALID, "$.result.payload.capability");
        assertDecodeFailure(nestedEnvelope("PAGE_OBJECT_EXTENSION_PROPOSAL",
                        EXTENSION_PAYLOAD.replace("\"evidence\":[\"manifest\"]", "\"evidence\":[7]")),
                AgentProtocolCodec.DecodeFailure.ARRAY_ELEMENT_INVALID, "$.result.payload.evidence[0]");
    }

    @Test void reportsMalformedJsonAndWrongRoleWithoutEchoingPayload() {
        RuntimeException malformed = assertThrows(RuntimeException.class,
                () -> codec.decode(AgentExecutor.Role.TEST_IMPLEMENTER, "{not-json".getBytes(StandardCharsets.UTF_8)));
        assertEquals(AgentProtocolCodec.DecodeFailure.MALFORMED_JSON, AgentProtocolCodec.decodeFailure(malformed));
        assertEquals("$", AgentProtocolCodec.decodeLocation(malformed));

        RuntimeException wrongRole = assertThrows(RuntimeException.class,
                () -> codec.decode(AgentExecutor.Role.TEST_ARCHITECT,
                        nestedEnvelope("TEST_IMPLEMENTATION_PROPOSAL", PROPOSAL_PAYLOAD)));
        assertEquals(AgentProtocolCodec.DecodeFailure.ROLE_TYPE_MISMATCH,
                AgentProtocolCodec.decodeFailure(wrongRole));
        assertEquals("$.result.resultType", AgentProtocolCodec.decodeLocation(wrongRole));
        assertFalse(AgentProtocolCodec.decodeDetail(wrongRole).contains(CANONICAL_REFERENCE));
    }

    @Test void reviewerSchemaAndProductionDecoderUseOnlyTypedBoundedFeedback() {
        Map<String, Object> schema = StrictJson.readObject(codec.outputSchema(AgentExecutor.Role.CODE_REVIEWER));
        Map<String, Object> payload = object(object(schema, "properties"), "payload");
        Map<String, Object> header = object(object(payload, "properties"), "header");
        Map<String, Object> status = object(object(header, "properties"), "status");
        assertEquals(List.of("COMPLETED"), strings(status, "enum"));
        Map<String, Object> reviewedRefs = object(object(payload, "properties"), "reviewedArtifactRefs");
        assertSafeIdSchema(object(reviewedRefs, "items"));
        Map<String, Object> finding = object(object(object(payload, "properties"), "findings"), "items");
        assertEquals(List.of("artifactRef", "category", "evidenceAvailability", "problemCategory", "requiredAction",
                        "ruleId", "safeLocation", "severity"),
                strings(finding, "required").stream().sorted().toList());
        assertFalse(object(finding, "properties").containsKey("detail"));
        assertFalse(object(finding, "properties").containsKey("recommendation"));
        assertSafeIdSchema(object(object(finding, "properties"), "artifactRef"));
        assertEquals(CodeReviewResult.allowedRuleIds(),
                strings(object(object(finding, "properties"), "ruleId"), "enum"));

        String requestChanges = """
                {"header":%s,"verdict":"REQUEST_CHANGES","reviewedArtifactRefs":["implementation-1","execution-1"],
                 "findings":[{"category":"ASSERTION_INSUFFICIENT","severity":"ERROR","ruleId":"ASSERTION_REQUIRED",
                 "artifactRef":"implementation-1","problemCategory":"ASSERTION","requiredAction":"ADD_ASSERTION",
                 "safeLocation":"ASSERTION","evidenceAvailability":"VERIFIED"}]}
                """.formatted(REVIEW_HEADER).strip();
        CodeReviewResult decoded = (CodeReviewResult) codec.decode(AgentExecutor.Role.CODE_REVIEWER,
                legacyEnvelope("CODE_REVIEW_RESULT", requestChanges)).payload();
        assertEquals(CodeReviewResult.Verdict.REQUEST_CHANGES, decoded.verdict());
        assertEquals(CodeReviewResult.Category.ASSERTION_INSUFFICIENT, decoded.findings().get(0).category());
        assertEquals("ASSERTION_REQUIRED", decoded.findings().get(0).ruleId());
        assertEquals(List.of(), decoded.header().evidence());
        assertEquals(List.of(), decoded.header().limitations());
    }

    @Test void reviewerDecoderRejectsRequestChangesWithoutActionableError() {
        String unsupported = """
                {"header":%s,"verdict":"REQUEST_CHANGES","reviewedArtifactRefs":["implementation-1"],"findings":[]}
                """.formatted(REVIEW_HEADER).strip();

        RuntimeException failure = assertThrows(RuntimeException.class, () -> codec.decode(
                AgentExecutor.Role.CODE_REVIEWER, legacyEnvelope("CODE_REVIEW_RESULT", unsupported)));

        assertEquals(AgentProtocolCodec.DecodeFailure.REQUIRED_FINDING_MISSING,
                AgentProtocolCodec.decodeFailure(failure));
        assertEquals("$.payload.findings", AgentProtocolCodec.decodeLocation(failure));

        RuntimeException missingFindings = assertThrows(RuntimeException.class, () -> codec.decode(
                AgentExecutor.Role.CODE_REVIEWER, legacyEnvelope("CODE_REVIEW_RESULT",
                        unsupported.replace(",\"findings\":[]", ""))));
        assertEquals(AgentProtocolCodec.DecodeFailure.REQUIRED_FIELD_MISSING,
                AgentProtocolCodec.decodeFailure(missingFindings));
        assertEquals("$.payload.findings", AgentProtocolCodec.decodeLocation(missingFindings));
    }

    @Test void reviewerDecoderAcceptsApproveAndTypedBlockButRejectsUnknownRules() {
        String approve = """
                {"header":%s,"verdict":"APPROVE","reviewedArtifactRefs":["implementation-1"],"findings":[]}
                """.formatted(REVIEW_HEADER).strip();
        String blocked = """
                {"header":%s,"verdict":"BLOCK","reviewedArtifactRefs":["implementation-1"],
                 "findings":[{"category":"REVIEW_UNSUBSTANTIATED","severity":"ERROR",
                 "ruleId":"REVIEW_CONTRACT_INVALID","artifactRef":"implementation-1",
                 "problemCategory":"REVIEW_CONTRACT","requiredAction":"HUMAN_REVIEW",
                 "safeLocation":"GENERATED_TEST","evidenceAvailability":"UNAVAILABLE"}]}
                """.formatted(REVIEW_HEADER).strip();

        CodeReviewResult approved = (CodeReviewResult) codec.decode(AgentExecutor.Role.CODE_REVIEWER,
                legacyEnvelope("CODE_REVIEW_RESULT", approve)).payload();
        CodeReviewResult domainBlock = (CodeReviewResult) codec.decode(AgentExecutor.Role.CODE_REVIEWER,
                legacyEnvelope("CODE_REVIEW_RESULT", blocked)).payload();
        assertEquals(CodeReviewResult.Verdict.APPROVE, approved.verdict());
        assertEquals(CodeReviewResult.Verdict.BLOCK, domainBlock.verdict());

        RuntimeException unknownRule = assertThrows(RuntimeException.class, () -> codec.decode(
                AgentExecutor.Role.CODE_REVIEWER, legacyEnvelope("CODE_REVIEW_RESULT",
                        blocked.replace("REVIEW_CONTRACT_INVALID", "UNKNOWN_RULE"))));
        assertEquals(AgentProtocolCodec.DecodeFailure.RULE_ID_INVALID,
                AgentProtocolCodec.decodeFailure(unknownRule));
        assertEquals("$.payload.findings[0].ruleId", AgentProtocolCodec.decodeLocation(unknownRule));

        RuntimeException contradictory = assertThrows(RuntimeException.class, () -> codec.decode(
                AgentExecutor.Role.CODE_REVIEWER, legacyEnvelope("CODE_REVIEW_RESULT",
                        blocked.replace("\"verdict\":\"BLOCK\"", "\"verdict\":\"APPROVE\""))));
        assertEquals(AgentProtocolCodec.DecodeFailure.DECISION_CONTRADICTORY,
                AgentProtocolCodec.decodeFailure(contradictory));
        assertEquals("$.payload.verdict", AgentProtocolCodec.decodeLocation(contradictory));
    }

    @Test void reviewerDecoderRejectsNonCompletedHeaderStatus() {
        String approve = """
                {"header":%s,"verdict":"APPROVE","reviewedArtifactRefs":["implementation-1"],"findings":[]}
                """.formatted(REVIEW_HEADER.replace("COMPLETED", "READY")).strip();

        RuntimeException failure = assertThrows(RuntimeException.class, () -> codec.decode(
                AgentExecutor.Role.CODE_REVIEWER, legacyEnvelope("CODE_REVIEW_RESULT", approve)));

        assertEquals(AgentProtocolCodec.DecodeFailure.DECISION_CONTRADICTORY,
                AgentProtocolCodec.decodeFailure(failure));
        assertEquals("$.payload.header.status", AgentProtocolCodec.decodeLocation(failure));
    }

    private AgentProtocolCodec.DecodedResult decodeNested(String resultType, String payload) {
        return codec.decode(AgentExecutor.Role.TEST_IMPLEMENTER, nestedEnvelope(resultType, payload));
    }

    private static byte[] nestedEnvelope(String resultType, String payload) {
        return ("{\"schemaVersion\":1,\"result\":{\"resultType\":\"" + resultType
                + "\",\"payload\":" + payload + "}}")
                .getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] legacyEnvelope(String resultType, String payload) {
        return ("{\"schemaVersion\":1,\"resultType\":\"" + resultType + "\",\"payload\":" + payload + "}")
                .getBytes(StandardCharsets.UTF_8);
    }

    private void assertDecodeFailure(byte[] document, AgentProtocolCodec.DecodeFailure reason, String location) {
        RuntimeException failure = assertThrows(RuntimeException.class,
                () -> codec.decode(AgentExecutor.Role.TEST_IMPLEMENTER, document));
        assertEquals(reason, AgentProtocolCodec.decodeFailure(failure));
        assertEquals(location, AgentProtocolCodec.decodeLocation(failure));
        assertFalse(AgentProtocolCodec.decodeDetail(failure).contains(CANONICAL_REFERENCE));
        assertFalse(AgentProtocolCodec.decodeDetail(failure).contains("SecretCanary"));
    }

    private static String resultType(Map<String, Object> envelope) {
        return String.valueOf(object(object(envelope, "properties"), "resultType").get("const"));
    }

    private static void assertSafeIdSchema(Map<String, Object> schema) {
        assertEquals("string", schema.get("type"));
        assertEquals(1L, ((Number) schema.get("minLength")).longValue());
        assertEquals(128L, ((Number) schema.get("maxLength")).longValue());
        assertEquals("^[A-Za-z0-9][A-Za-z0-9._:-]{0,127}$", schema.get("pattern"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Map<String, Object> value, String name) {
        return (Map<String, Object>) value.get(name);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> objects(Map<String, Object> value, String name) {
        return (List<Map<String, Object>>) value.get(name);
    }

    @SuppressWarnings("unchecked")
    private static List<String> strings(Map<String, Object> value, String name) {
        return (List<String>) value.get(name);
    }
}
