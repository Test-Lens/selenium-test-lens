package io.github.testlens.application.tooling.ai.workflow.runner;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Subprocess fixture. It deliberately has no dependency on the runner implementation. */
public final class ExternalAgentFixtureMain {
    private static final String VALID = """
            {"schemaVersion":1,"resultType":"TEST_PLAN","payload":{"header":{"schemaVersion":1,"status":"READY","evidence":["external"],"limitations":[],"confidence":"AI_PROPOSED"},"scenarios":[]}}
            """;

    private ExternalAgentFixtureMain() { }

    public static void main(String[] args) throws Exception {
        String mode = args[0];
        String input = new String(System.in.readAllBytes(), StandardCharsets.UTF_8);
        if (!input.contains("TEST_LENS_EXTERNAL_AGENT_V1")) System.exit(20);
        switch (mode) {
            case "valid" -> System.out.print(VALID);
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
}
