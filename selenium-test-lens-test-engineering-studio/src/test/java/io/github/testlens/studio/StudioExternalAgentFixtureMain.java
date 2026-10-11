package io.github.testlens.studio;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Subprocess fixture for the Studio security boundary. */
public final class StudioExternalAgentFixtureMain {
    private static final Pattern TARGET = Pattern.compile("\\\"targetTestClass\\\":\\\"([A-Za-z0-9_.]+)\\\"");
    private static final String API_REFERENCE =
            "benchmark.LoginPage#invalidPasswordShowsExpectedError(String,String)";
    private static final String STRUCTURAL_JWT = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9."
            + "eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IlN5bnRoZXRpYyBVc2VyIiwiYWRtaW4iOnRydWV9."
            + "c3ludGhldGljLXNpZ25hdHVyZQ";

    private StudioExternalAgentFixtureMain() { }

    public static void main(String[] args) throws Exception {
        String input = new String(System.in.readAllBytes(), StandardCharsets.UTF_8);
        if (!input.contains("TEST_LENS_EXTERNAL_AGENT_V1")) System.exit(20);
        if ("capability-number".equals(args[0])) {
            System.out.print("{\"schemaVersion\":1,\"result\":{"
                    + "\"resultType\":\"PAGE_OBJECT_CAPABILITY_MISSING\",\"payload\":{"
                    + "\"pageId\":\"login\",\"capability\":7,\"reason\":\"missing\"}}}");
            return;
        }
        if ("extension-evidence-number".equals(args[0])) {
            System.out.print("{\"schemaVersion\":1,\"result\":{"
                    + "\"resultType\":\"PAGE_OBJECT_EXTENSION_PROPOSAL\",\"payload\":{"
                    + "\"missing\":{\"pageId\":\"login\",\"capability\":\"assertion\","
                    + "\"reason\":\"missing\"},\"extensionClass\":\"benchmark.LoginPageExtension\","
                    + "\"proposedMethod\":\"check()\",\"evidence\":[7]}}}");
            return;
        }
        Matcher matcher = TARGET.matcher(input);
        if (!matcher.find()) System.exit(21);
        String qualifiedName = matcher.group(1);
        int separator = qualifiedName.lastIndexOf('.');
        String packageName = separator < 0 ? "" : qualifiedName.substring(0, separator);
        String className = separator < 0 ? qualifiedName : qualifiedName.substring(separator + 1);
        String value = "secret".equals(args[0]) ? "configured-secret-canary" : "fixture-invalid";
        String source = (packageName.isBlank() ? "" : "package " + packageName + "; ")
                + "public final class " + className + " { @org.junit.jupiter.api.Test void generated() { "
                + "String inputValue = \"" + value + "\"; if (inputValue.isBlank()) throw new AssertionError(); "
                + "org.junit.jupiter.api.Assertions.assertTrue(new benchmark.LoginPage()"
                + ".invalidPasswordShowsExpectedError(\"fixture-user\", inputValue)); } }";
        if ("source-missing-method".equals(args[0])) {
            source = source.replace("invalidPasswordShowsExpectedError", "doesNotExist");
        } else if ("source-wrong-signature".equals(args[0])) {
            source = source.replace("invalidPasswordShowsExpectedError(\"fixture-user\", inputValue)",
                    "invalidPasswordShowsExpectedError(true)");
        } else if ("source-unresolved-receiver".equals(args[0])) {
            source = source.replace("new benchmark.LoginPage()", "provider().page()");
        } else if ("source-parse-failure".equals(args[0])) {
            source = "public final class " + className + " { void test( {";
        }
        String pageObjectApis = switch (args[0]) {
            case "jwt-metadata" -> "[\"" + STRUCTURAL_JWT + "\"]";
            case "legacy-equivalent" ->
                    "[\"LoginPage.invalidPasswordShowsExpectedError(java.lang.String, String)\"]";
            case "invalid-reference" -> "[\"not a Java API reference\"]";
            case "blank-reference" -> "[\"   \"]";
            case "empty-references", "secret" -> "[]";
            case "reference-string" -> "\"" + API_REFERENCE + "\"";
            case "reference-number" -> "[7]";
            default -> "[\"" + API_REFERENCE + "\"]";
        };
        String referenceField = "missing-references".equals(args[0]) ? ""
                : "\"pageObjectApisUsed\":" + pageObjectApis + ",";
        String resultType = "result-payload-mismatch".equals(args[0])
                ? "PAGE_OBJECT_CAPABILITY_MISSING" : "TEST_IMPLEMENTATION_PROPOSAL";
        String result = "\"resultType\":\"" + resultType + "\","
                + "\"payload\":{\"header\":{\"schemaVersion\":1,\"status\":\"READY\","
                + "\"evidence\":[\"external\"],\"limitations\":[],\"confidence\":\"AI_PROPOSED\"},"
                + "\"scenarioId\":\"invalid-login\",\"sourcePatch\":\"" + json(source) + "\","
                + "\"selectorAccessPolicy\":\"PAGE_OBJECTS_ONLY\"," + referenceField
                + "\"missingCapabilities\":[]}";
        if ("legacy-equivalent".equals(args[0])) {
            System.out.print("{\"schemaVersion\":1," + result + "}");
        } else if ("mixed-envelope".equals(args[0])) {
            System.out.print("{\"schemaVersion\":1,\"resultType\":\"TEST_IMPLEMENTATION_PROPOSAL\","
                    + "\"result\":{" + result + "}}");
        } else {
            System.out.print("{\"schemaVersion\":1,\"result\":{" + result + "}}");
        }
    }

    private static String json(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "\\r").replace("\n", "\\n");
    }
}
