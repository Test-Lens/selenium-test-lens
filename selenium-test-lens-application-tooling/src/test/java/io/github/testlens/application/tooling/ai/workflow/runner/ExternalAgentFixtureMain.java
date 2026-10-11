package io.github.testlens.application.tooling.ai.workflow.runner;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Subprocess fixture. It deliberately has no dependency on the runner implementation. */
public final class ExternalAgentFixtureMain {
    private static final String API_REFERENCE =
            "benchmark.LoginPage#invalidPasswordShowsExpectedError(String,String)";
    private static final String LEGACY_API_REFERENCE =
            "benchmark.LoginPage.invalidPasswordShowsExpectedError(String,String)";
    private static final String STRUCTURAL_JWT = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9."
            + "eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IlN5bnRoZXRpYyBVc2VyIiwiYWRtaW4iOnRydWV9."
            + "c3ludGhldGljLXNpZ25hdHVyZQ";
    private static final String VALID = """
            {"schemaVersion":1,"resultType":"TEST_PLAN","payload":{"header":{"schemaVersion":1,"status":"READY","evidence":["external"],"limitations":[],"confidence":"AI_PROPOSED"},"scenarios":[]}}
            """;
    private static final String IMPLEMENTATION_WITH_PASSWORD_IDENTIFIER = """
            {"schemaVersion":1,"result":{"resultType":"TEST_IMPLEMENTATION_PROPOSAL","payload":{"header":{"schemaVersion":1,"status":"READY","evidence":["external"],"limitations":[],"confidence":"AI_PROPOSED"},"scenarioId":"invalid-login","sourcePatch":"package fixture; class GeneratedTest { void test() { String password = \\"fixture-invalid\\"; new benchmark.LoginPage().invalidPasswordShowsExpectedError(\\"fixture-user\\", password); } }","selectorAccessPolicy":"PAGE_OBJECTS_ONLY","pageObjectApisUsed":["benchmark.LoginPage#invalidPasswordShowsExpectedError(String,String)"],"missingCapabilities":[]}}}
            """;
    private static final String CAPABILITY_MISSING = """
            {"schemaVersion":1,"result":{"resultType":"PAGE_OBJECT_CAPABILITY_MISSING","payload":{"pageId":"login","capability":"invalid password assertion","reason":"required public method is absent"}}}
            """;
    private static final String EXTENSION_PROPOSAL = """
            {"schemaVersion":1,"result":{"resultType":"PAGE_OBJECT_EXTENSION_PROPOSAL","payload":{"missing":{"pageId":"login","capability":"invalid password assertion","reason":"required public method is absent"},"extensionClass":"benchmark.LoginPageExtension","proposedMethod":"invalidPasswordShowsExpectedError(String,String)","evidence":["manifest"]}}}
            """;
    private static final String ALL_MANIFEST_REFERENCES = """
            {"schemaVersion":1,"result":{"resultType":"TEST_IMPLEMENTATION_PROPOSAL","payload":{"header":{"schemaVersion":1,"status":"READY","evidence":["external"],"limitations":[],"confidence":"AI_PROPOSED"},"scenarioId":"manifest-apis","sourcePatch":"package fixture; class GeneratedTest { void test() { benchmark.LoginPage page = new benchmark.LoginPage(); page.invalidPasswordShowsExpectedError(\\"fixture-user\\", \\"fixture-invalid\\"); page.recordAttempt(); page.checkStatus(\\"ready\\"); page.checkStatus(1); page.waitUntilReady(); } }","selectorAccessPolicy":"PAGE_OBJECTS_ONLY","pageObjectApisUsed":["benchmark.LoginPage#invalidPasswordShowsExpectedError(String,String)","benchmark.LoginPage#recordAttempt()","benchmark.LoginPage#checkStatus(String)","benchmark.LoginPage#checkStatus(int)","benchmark.LoginPage#waitUntilReady()"],"missingCapabilities":[]}}}
            """;

    private ExternalAgentFixtureMain() { }

    public static void main(String[] args) throws Exception {
        String mode = args[0];
        String input = new String(System.in.readAllBytes(), StandardCharsets.UTF_8);
        if (!input.contains("TEST_LENS_EXTERNAL_AGENT_V1")) System.exit(20);
        switch (mode) {
            case "valid" -> System.out.print(VALID);
            case "implementer-safe-password" -> System.out.print(IMPLEMENTATION_WITH_PASSWORD_IDENTIFIER);
            case "implementer-legacy-reference" -> System.out.print(
                    legacyFlat(IMPLEMENTATION_WITH_PASSWORD_IDENTIFIER.replace(API_REFERENCE, LEGACY_API_REFERENCE)));
            case "implementer-mixed-envelope" -> System.out.print(
                    IMPLEMENTATION_WITH_PASSWORD_IDENTIFIER.replace("{\"schemaVersion\":1,",
                            "{\"schemaVersion\":1,\"resultType\":\"TEST_IMPLEMENTATION_PROPOSAL\","));
            case "implementer-literal-secret" -> System.out.print(
                    IMPLEMENTATION_WITH_PASSWORD_IDENTIFIER.replace("fixture-invalid", "configured-secret-canary"));
            case "implementer-bearer" -> System.out.print(
                    IMPLEMENTATION_WITH_PASSWORD_IDENTIFIER.replace(
                            "fixture-invalid", "Authorization: Bearer synthetic-credential"));
            case "implementer-jwt" -> System.out.print(
                    IMPLEMENTATION_WITH_PASSWORD_IDENTIFIER.replace(
                            "fixture-invalid", STRUCTURAL_JWT));
            case "implementer-jwt-comment" -> System.out.print(
                    IMPLEMENTATION_WITH_PASSWORD_IDENTIFIER.replace(
                            "String password = \\\"fixture-invalid\\\";", "// " + STRUCTURAL_JWT));
            case "implementer-jwt-metadata" -> System.out.print(
                    IMPLEMENTATION_WITH_PASSWORD_IDENTIFIER.replace(API_REFERENCE, STRUCTURAL_JWT));
            case "implementer-missing-method" -> System.out.print(
                    IMPLEMENTATION_WITH_PASSWORD_IDENTIFIER.replace(
                            API_REFERENCE, "LoginPage.x(String,String)"));
            case "implementer-wrong-arity" -> System.out.print(
                    IMPLEMENTATION_WITH_PASSWORD_IDENTIFIER.replace(
                            API_REFERENCE, "LoginPage.invalidPasswordShowsExpectedError(String)"));
            case "implementer-equivalent-reference" -> System.out.print(
                    IMPLEMENTATION_WITH_PASSWORD_IDENTIFIER.replace(
                            API_REFERENCE,
                            "LoginPage#invalidPasswordShowsExpectedError(java.lang.String, String)"));
            case "implementer-simple-reference" -> System.out.print(
                    IMPLEMENTATION_WITH_PASSWORD_IDENTIFIER.replace(
                            API_REFERENCE, "LoginPage.invalidPasswordShowsExpectedError(String,String)"));
            case "implementer-invalid-reference" -> System.out.print(
                    IMPLEMENTATION_WITH_PASSWORD_IDENTIFIER.replace(API_REFERENCE, "not a Java API reference"));
            case "implementer-blank-reference" -> System.out.print(
                    IMPLEMENTATION_WITH_PASSWORD_IDENTIFIER.replace(API_REFERENCE, "   "));
            case "implementer-empty-references" -> System.out.print(
                    IMPLEMENTATION_WITH_PASSWORD_IDENTIFIER.replace("[\"" + API_REFERENCE + "\"]", "[]"));
            case "implementer-source-missing-method" -> System.out.print(
                    IMPLEMENTATION_WITH_PASSWORD_IDENTIFIER.replace(
                            "invalidPasswordShowsExpectedError", "doesNotExist"));
            case "implementer-source-wrong-signature" -> System.out.print(
                    IMPLEMENTATION_WITH_PASSWORD_IDENTIFIER.replace(
                            "invalidPasswordShowsExpectedError(\\\"fixture-user\\\", password)",
                            "invalidPasswordShowsExpectedError(true)"));
            case "implementer-source-unresolved-receiver" -> System.out.print(
                    IMPLEMENTATION_WITH_PASSWORD_IDENTIFIER.replace(
                            "new benchmark.LoginPage()", "provider().page()"));
            case "implementer-source-parse-failure" -> System.out.print(
                    IMPLEMENTATION_WITH_PASSWORD_IDENTIFIER.replace(
                            "package fixture; class GeneratedTest { void test() { String password = \\\"fixture-invalid\\\"; new benchmark.LoginPage().invalidPasswordShowsExpectedError(\\\"fixture-user\\\", password); } }",
                            "class GeneratedTest { void test( {"));
            case "implementer-missing-references" -> System.out.print(
                    IMPLEMENTATION_WITH_PASSWORD_IDENTIFIER.replace(
                            ",\"pageObjectApisUsed\":[\"" + API_REFERENCE + "\"]", ""));
            case "implementer-reference-string" -> System.out.print(
                    IMPLEMENTATION_WITH_PASSWORD_IDENTIFIER.replace(
                            "[\"" + API_REFERENCE + "\"]", "\"" + API_REFERENCE + "\""));
            case "implementer-reference-number" -> System.out.print(
                    IMPLEMENTATION_WITH_PASSWORD_IDENTIFIER.replace("[\"" + API_REFERENCE + "\"]", "[7]"));
            case "implementer-capability-missing" -> System.out.print(CAPABILITY_MISSING);
            case "implementer-extension-proposal" -> System.out.print(EXTENSION_PROPOSAL);
            case "implementer-capability-number" -> System.out.print(CAPABILITY_MISSING.replace(
                    "\"capability\":\"invalid password assertion\"", "\"capability\":7"));
            case "implementer-extension-evidence-number" -> System.out.print(EXTENSION_PROPOSAL.replace(
                    "\"evidence\":[\"manifest\"]", "\"evidence\":[7]"));
            case "implementer-all-manifest-references" -> System.out.print(ALL_MANIFEST_REFERENCES);
            case "implementer-capability-with-proposal-field" -> System.out.print(
                    CAPABILITY_MISSING.replace("}}", ",\"sourcePatch\":\"class GeneratedTest {}\"}}"));
            case "implementer-result-payload-mismatch" -> System.out.print(
                    IMPLEMENTATION_WITH_PASSWORD_IDENTIFIER.replace(
                            "TEST_IMPLEMENTATION_PROPOSAL", "PAGE_OBJECT_CAPABILITY_MISSING"));
            case "implementer-appended-secret" -> System.out.print(
                    IMPLEMENTATION_WITH_PASSWORD_IDENTIFIER.replace(
                            API_REFERENCE, API_REFERENCE + " configured-secret-canary"));
            case "implementer-canary" -> System.out.print(
                    IMPLEMENTATION_WITH_PASSWORD_IDENTIFIER.replace("fixture-invalid", "caller-canary"));
            case "file-valid" -> {
                Path output = Path.of(args[1]);
                Path schema = Path.of(args[2]);
                if (!Files.isRegularFile(schema) || Files.size(schema) == 0) System.exit(22);
                Files.writeString(output, VALID, StandardCharsets.UTF_8);
            }
            case "environment" -> {
                boolean isolated = Path.of("").toAbsolutePath().getFileName().toString().startsWith("test-lens-agent-");
                boolean allowed = "visible".equals(System.getenv("TEST_LENS_SAFE_ENV"));
                boolean blocked = System.getenv("TEST_LENS_BLOCKED_ENV") == null;
                if (!isolated || !allowed || !blocked) System.exit(23);
                System.out.print(VALID);
            }
            case "excerpt" -> {
                if (!input.contains("sources/LoginPage.java")
                        || !Files.readString(Path.of("sources", "LoginPage.java")).contains("class LoginPage")) {
                    System.exit(25);
                }
                System.out.print(VALID);
            }
            case "malformed" -> System.out.print("not-json");
            case "empty" -> { }
            case "truncated" -> System.out.print(VALID.substring(0, VALID.length() - 4));
            case "truncated-string" -> System.out.print("{\"schemaVersion\":1,\"payload\":\"unterminated}");
            case "unsafe-output" -> System.out.print("{\"password\":\"credential-like-value\"}");
            case "missing-field" -> System.out.print(VALID.replace("\"scenarios\":[]", "\"unexpected\":[]"));
            case "domain-invalid" -> System.out.print("""
                    {"schemaVersion":1,"resultType":"TEST_PLAN","payload":{"header":{"schemaVersion":1,"status":"READY","evidence":["external"],"limitations":[],"confidence":"AI_PROPOSED"},"scenarios":[{"scenarioId":"s","title":"t","priority":"HIGH","preconditions":[],"steps":[{"order":0,"action":"a","pageId":null,"elementId":null,"valueRef":null}],"expected":[],"requiredPageIds":[],"requiredStateIds":[],"requiredElementIds":[],"requiredData":[],"riskAreas":[],"existingCoverage":"NEW_TEST_REQUIRED","existingTestRefs":[],"knownUnknowns":[]}]}}
                    """);
            case "wrong-schema" -> System.out.print(VALID.replace("\"schemaVersion\":1", "\"schemaVersion\":2"));
            case "wrong-type" -> System.out.print(VALID.replace("TEST_PLAN", "CODE_REVIEW_RESULT"));
            case "oversize" -> System.out.print("x".repeat(8_192));
            case "stderr-oversize" -> {
                System.err.print("x".repeat(8_192));
                System.out.print(VALID);
            }
            case "sleep" -> {
                Thread.sleep(10_000);
                System.out.print(VALID);
            }
            case "exit" -> System.exit(7);
            default -> System.exit(24);
        }
    }

    private static String legacyFlat(String nested) {
        nested = nested.strip();
        String prefix = "{\"schemaVersion\":1,\"result\":{";
        if (!nested.startsWith(prefix) || !nested.endsWith("}}")) throw new IllegalArgumentException("nested");
        return "{\"schemaVersion\":1," + nested.substring(prefix.length(), nested.length() - 1);
    }
}
