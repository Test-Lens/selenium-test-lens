package io.github.testlens.application.tooling.ai.workflow;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class GeneratedTestPolicyValidatorTest {
    private final GeneratedTestPolicyValidator validator =
            new GeneratedTestPolicyValidator(GeneratedTestPolicyValidator.Policy.defaults());

    @Test void reportsAllDefaultCodeViolationsWithEvidence() {
        String source = """
                class GeneratedTest {
                  @Retry void test() throws Exception {
                    var element = driver.findElement(By.id("x"));
                    ((JavascriptExecutor) driver).executeScript("x");
                    Thread.sleep(5);
                    while (failed()) { rerun(); }
                  }
                }
                """;
        var result = validator.validate(Path.of("src/test/java/GeneratedTest.java"), source);
        assertFalse(result.accepted());
        assertTrue(result.violations().stream().anyMatch(v -> v.rule() == GeneratedTestPolicyValidator.Rule.RAW_BY));
        assertTrue(result.violations().stream().anyMatch(v -> v.rule() == GeneratedTestPolicyValidator.Rule.DRIVER_LOOKUP));
        assertTrue(result.violations().stream().anyMatch(v -> v.rule() == GeneratedTestPolicyValidator.Rule.JAVASCRIPT));
        assertTrue(result.violations().stream().anyMatch(v -> v.rule() == GeneratedTestPolicyValidator.Rule.THREAD_SLEEP));
        assertTrue(result.violations().stream().anyMatch(v -> v.rule() == GeneratedTestPolicyValidator.Rule.RETRY_ANNOTATION));
        assertTrue(result.violations().stream().anyMatch(v -> v.rule() == GeneratedTestPolicyValidator.Rule.RETRY_LOOP),
                () -> result.violations().toString());
        assertTrue(result.violations().stream().allMatch(v -> !v.evidence().isBlank()));
    }

    @Test void ignoresForbiddenTokensInsideCommentsStringsAndCharacters() {
        String source = """
                class SafeTest {
                  // driver.findElement(By.id("x")); Thread.sleep(1);
                  String documentation = "JavascriptExecutor executeScript while @Retry";
                  char marker = 'd';
                  void test() { for (int i = 0; i < 2; i++) loginPage.submit(); }
                }
                """;
        assertTrue(validator.validate(Path.of("src/test/java/SafeTest.java"), source).accepted());
    }

    @Test void enforcesForbiddenAndRequestAllowedPaths() {
        assertEquals(GeneratedTestPolicyValidator.Rule.FORBIDDEN_PATH,
                validator.validate(Path.of("src/main/java/Changed.java"), "class Changed {}").violations().get(0).rule());
        TestEngineeringRequest request = new TestEngineeringRequest(
                new TestEngineeringRequest.Requirement("x", List.of()), new TestEngineeringRequest.Scope(List.of(), List.of()),
                new TestEngineeringRequest.Framework("JUnit", "5", "jupiter"),
                new TestEngineeringRequest.Target("m", "T", "s"), List.of("generated-tests"),
                TestEngineeringRequest.ExecutionPolicy.DISABLED);
        assertTrue(validator.validate(request, Path.of("src/test/java/T.java"), "class T {}").violations().stream()
                .anyMatch(v -> v.rule() == GeneratedTestPolicyValidator.Rule.OUTSIDE_ALLOWED_PATHS));
    }
}
