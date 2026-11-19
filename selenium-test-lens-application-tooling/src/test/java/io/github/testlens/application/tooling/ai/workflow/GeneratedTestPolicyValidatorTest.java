package io.github.testlens.application.tooling.ai.workflow;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

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

    @ParameterizedTest(name = "rejects retry loop: {0}")
    @MethodSource("retryLoops")
    void rejectsRetryLoopsInEveryJavaLoopForm(String description, String body) {
        assertRejected(compilationUnit(body));
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> retryLoops() {
        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.of("for", "for (int attempt=0; attempt<3; attempt++) { runAgain(); }"),
                org.junit.jupiter.params.provider.Arguments.of("foreach", "for (String attempt : attempts) { retry(); }"),
                org.junit.jupiter.params.provider.Arguments.of("while single body", "while (failed()) retry();"),
                org.junit.jupiter.params.provider.Arguments.of("do while", "do { rerun(); } while (failed());"),
                org.junit.jupiter.params.provider.Arguments.of("nested", "for (int i=0;i<2;i++) { if (failed()) { retry(); } }"),
                org.junit.jupiter.params.provider.Arguments.of("long body", "for (int i=0;i<2;i++) { "
                        + "int a0=0,a1=1,a2=2,a3=3,a4=4,a5=5,a6=6,a7=7,a8=8,a9=9; "
                        + "int b0=0,b1=1,b2=2,b3=3,b4=4,b5=5,b6=6,b7=7,b8=8,b9=9; retry(); }"),
                org.junit.jupiter.params.provider.Arguments.of("structural attempt without retry names",
                        "for (int attempt=0;attempt<3;attempt++) { try { loginPage.submit(); break; } catch (RuntimeException ignored) { } }"));
    }

    @ParameterizedTest(name = "rejects swallowed assertion: {0}")
    @MethodSource("swallowedAssertions")
    void rejectsSwallowedAssertions(String description, String body) {
        assertRejected(compilationUnit(body));
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> swallowedAssertions() {
        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.of("AssertionError", "try { assertReady(); } catch (AssertionError ignored) { diagnose(); }"),
                org.junit.jupiter.params.provider.Arguments.of("qualified AssertionError", "try { assertReady(); } catch (java.lang.AssertionError ignored) { diagnose(); }"),
                org.junit.jupiter.params.provider.Arguments.of("Error", "try { assertReady(); } catch (Error ignored) { diagnose(); }"),
                org.junit.jupiter.params.provider.Arguments.of("Throwable", "try { assertReady(); } catch (Throwable ignored) { diagnose(); }"),
                org.junit.jupiter.params.provider.Arguments.of("empty catch", "try { assertReady(); } catch (AssertionError ignored) { }"),
                org.junit.jupiter.params.provider.Arguments.of("return", "try { assertReady(); } catch (AssertionError ignored) { return; }"),
                org.junit.jupiter.params.provider.Arguments.of("break", "for (;;) { try { assertReady(); } catch (AssertionError ignored) { break; } }"),
                org.junit.jupiter.params.provider.Arguments.of("continue", "for (int i=0;i<2;i++) { try { assertReady(); } catch (AssertionError ignored) { continue; } }"),
                org.junit.jupiter.params.provider.Arguments.of("multicatch", "try { assertReady(); } catch (IllegalStateException | AssertionError ignored) { diagnose(); }"),
                org.junit.jupiter.params.provider.Arguments.of("qualified multicatch", "try { assertReady(); } catch (IllegalStateException | java.lang.AssertionError ignored) { diagnose(); }"),
                org.junit.jupiter.params.provider.Arguments.of("inside loop", "for (int i=0;i<2;i++) { try { assertReady(); } catch (Throwable ignored) { diagnose(); } }"),
                org.junit.jupiter.params.provider.Arguments.of("conditional rethrow", "try { assertReady(); } catch (AssertionError failure) { diagnose(); if (failed()) throw failure; }"));
    }

    @Test void acceptsUnconditionalDiagnosticAndRethrowAndExceptionOnlyCatch() {
        assertAccepted(compilationUnit("try { assertReady(); } catch (AssertionError failure) { diagnose(); throw failure; }"));
        assertAccepted(compilationUnit("try { assertReady(); } catch (AssertionError failure) { diagnose(); throw new AssertionError(\"still failed\", failure); }"));
        assertAccepted(compilationUnit("try { assertReady(); } catch (Exception ignored) { diagnose(); }"));
        assertAccepted(compilationUnit("try { assertReady(); } catch (AssertionError failure) { try { diagnose(); throw failure; } finally { diagnose(); } }"));
        assertAccepted(compilationUnit("try { assertReady(); } catch (AssertionError failure) { synchronized (this) { diagnose(); throw failure; } }"));
    }

    @ParameterizedTest(name = "rejects exact Java sleep: {0}")
    @MethodSource("threadSleeps")
    void rejectsExactJavaThreadSleepVariants(String description, String source) {
        assertRejected(source);
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> threadSleeps() {
        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.of("qualified", compilationUnit("java.lang.Thread.sleep(1L);")),
                org.junit.jupiter.params.provider.Arguments.of("method reference", "package example; interface Sleeper { void sleep(long value) throws Exception; } final class GeneratedTest { Sleeper wait = Thread::sleep; }"),
                org.junit.jupiter.params.provider.Arguments.of("qualified method reference", "package example; interface Sleeper { void sleep(long value) throws Exception; } final class GeneratedTest { Sleeper wait = java.lang.Thread::sleep; }"),
                org.junit.jupiter.params.provider.Arguments.of("static import", "package example; import static java.lang.Thread.sleep; final class GeneratedTest { void test() throws Exception { sleep(1L); } }"),
                org.junit.jupiter.params.provider.Arguments.of("wildcard static import", "package example; import static java.lang.Thread.*; final class GeneratedTest { void test() throws Exception { sleep(1L); } }"));
    }

    @ParameterizedTest
    @MethodSource("retryAnnotations")
    void rejectsShortAndQualifiedRetryAnnotations(String source) { assertRejected(source); }

    static Stream<String> retryAnnotations() {
        return Stream.of("package example; @interface Retry {} final class GeneratedTest { @Retry void test() {} }",
                "package example; @interface Retryable {} final class GeneratedTest { @example.Retryable void test() {} }",
                "package example; final class GeneratedTest { @org.junit.jupiter.api.RepeatedTest(3) void test() {} }");
    }

    @Test void rejectsJavaThatCannotBeParsed() { assertRejected("package example; final class GeneratedTest { void test( }"); }

    @Test void permitsOrdinaryLoopsAssertionsAndPageObjectCalls() {
        assertAccepted(compilationUnit("for (String user : users) { loginPage.login(user, \"secret\"); } "
                + "org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, loginPage::submit);"));
    }

    @Test void doesNotTreatForeignSleepOrMethodReferenceTextAsThreadSleep() {
        assertAccepted("package example; final class GeneratedTest { static final class Clock { void sleep(long value) {} } "
                + "void test() { new Clock().sleep(1L); String documentation = \"Thread::sleep\"; } }");
    }

    @ParameterizedTest
    @MethodSource("staticByImports")
    void rejectsRawByFactoriesImportedStatically(String source) { assertRejected(source); }

    static Stream<String> staticByImports() {
        return Stream.of(
                "package example; import static org.openqa.selenium.By.id; final class GeneratedTest { void test() { Object locator=id(\"x\"); } }",
                "package example; import static org.openqa.selenium.By.*; final class GeneratedTest { void test() { Object locator=cssSelector(\".x\"); } }");
    }

    @Test void assertionSuppressionIsIndependentFromRetryLoopOption() {
        var defaults = GeneratedTestPolicyValidator.Policy.defaults();
        var withoutRetryLoops = new GeneratedTestPolicyValidator.Policy(defaults.forbiddenPathPrefixes(),
                defaults.maxCharacters(), defaults.blockRawBy(), defaults.blockDriverLookup(), defaults.blockSleeps(),
                defaults.blockJavascript(), defaults.blockRetryAnnotations(), false);
        var custom = new GeneratedTestPolicyValidator(withoutRetryLoops);
        assertTrue(custom.validate(Path.of("src/test/java/example/GeneratedTest.java"),
                compilationUnit("while (failed()) retry();")).accepted());
        var swallowed = custom.validate(Path.of("src/test/java/example/GeneratedTest.java"),
                compilationUnit("try { assertReady(); } catch (AssertionError ignored) { }"));
        assertEquals(List.of(GeneratedTestPolicyValidator.Rule.ASSERTION_FAILURE_SWALLOWED),
                swallowed.violations().stream().map(GeneratedTestPolicyValidator.Violation::rule).toList());
    }

    @Test void parseFailureAndBehaviorViolationHaveDistinctStableDiagnostics() {
        var parseFailure = validator.validate(Path.of("src/test/java/example/GeneratedTest.java"),
                "package example; final class GeneratedTest { void test( }");
        assertEquals(GeneratedTestPolicyValidator.Rule.SOURCE_PARSE_FAILED, parseFailure.violations().get(0).rule());
        assertFalse(parseFailure.violations().get(0).evidence().isBlank());

        var swallowed = validator.validate(Path.of("src/test/java/example/GeneratedTest.java"),
                compilationUnit("try { assertReady(); } catch (AssertionError ignored) { }"));
        var violation = swallowed.violations().stream()
                .filter(value -> value.rule() == GeneratedTestPolicyValidator.Rule.ASSERTION_FAILURE_SWALLOWED)
                .findFirst().orElseThrow();
        assertTrue(violation.line() > 0);
        assertTrue(violation.column() > 0);
        assertTrue(violation.evidence().length() <= 160);
        assertEquals(swallowed.violations().size(), swallowed.violations().stream().distinct().count());
    }

    @Test void sourceSizeLimitRejectsBeforeParsing() {
        var defaults = GeneratedTestPolicyValidator.Policy.defaults();
        var bounded = new GeneratedTestPolicyValidator(new GeneratedTestPolicyValidator.Policy(
                defaults.forbiddenPathPrefixes(), 20, defaults.blockRawBy(), defaults.blockDriverLookup(),
                defaults.blockSleeps(), defaults.blockJavascript(), defaults.blockRetryAnnotations(),
                defaults.blockLoops()));
        var result = bounded.validate(Path.of("src/test/java/example/GeneratedTest.java"),
                "not valid Java at all ".repeat(4));
        assertEquals(List.of(GeneratedTestPolicyValidator.Rule.SIZE_LIMIT),
                result.violations().stream().map(GeneratedTestPolicyValidator.Violation::rule).toList());
    }

    @Test void ignoresPolicyWordsInCommentsAndAllLiteralForms() {
        assertAccepted("""
                package example;
                final class GeneratedTest {
                  // Thread.sleep(1); while (true) retry(); catch (AssertionError ignored) {}
                  String text = "Thread.sleep retry AssertionError";
                  String block = \"""
                    do { rerun(); } while (true); @Retry
                    \""";
                  char value = '@';
                }
                """);
    }

    private static String compilationUnit(String body) {
        return "package example; final class GeneratedTest { static final class LoginPage { void login(String a,String b){} void submit(){} } LoginPage loginPage=new LoginPage(); String[] users = {}; String[] attempts = {}; "
                + "boolean failed(){return false;} void retry(){} void rerun(){} void runAgain(){} void diagnose(){} "
                + "void assertReady(){} void test() throws Exception { " + body + " } }";
    }

    private void assertRejected(String source) {
        var result = validator.validate(Path.of("src/test/java/example/GeneratedTest.java"), source);
        assertFalse(result.accepted(), () -> "expected rejection for: " + source);
    }

    private void assertAccepted(String source) {
        var result = validator.validate(Path.of("src/test/java/example/GeneratedTest.java"), source);
        assertTrue(result.accepted(), () -> result.violations().toString());
    }
}
