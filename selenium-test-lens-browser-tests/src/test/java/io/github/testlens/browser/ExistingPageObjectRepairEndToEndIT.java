package io.github.testlens.browser;

import com.sun.net.httpserver.HttpServer;
import io.github.testlens.TestLens;
import io.github.testlens.application.mapper.ApplicationMapper;
import io.github.testlens.application.mapper.ApplicationMapperOptions;
import io.github.testlens.application.model.ApplicationModel;
import io.github.testlens.application.tooling.ai.EvidenceFailureClassifier;
import io.github.testlens.application.tooling.ai.FailureClassification;
import io.github.testlens.application.tooling.ai.RepairProposal;
import io.github.testlens.application.tooling.ai.SelectorRepairPlanner;
import io.github.testlens.application.tooling.ai.workflow.ArtifactEnvelope;
import io.github.testlens.application.tooling.ai.workflow.TargetedJavaCompiler;
import io.github.testlens.application.tooling.ai.workflow.TrustedRepairApplier;
import io.github.testlens.application.tooling.codegen.SelectorJavaExpression;
import io.github.testlens.application.tooling.source.CorrelationOverrides;
import io.github.testlens.application.tooling.source.PageObjectCorrelation;
import io.github.testlens.application.tooling.source.PageObjectCorrelator;
import io.github.testlens.application.tooling.source.SourceImpact;
import io.github.testlens.application.tooling.source.SourceImpactAnalyzer;
import io.github.testlens.core.trace.TraceStatus;
import io.github.testlens.core.trace.UiTestLensSession;
import io.github.testlens.selector.tooling.ExistingProjectIndex;
import io.github.testlens.selector.tooling.ExistingProjectIndexer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Certifies a real hand-written Page Object selector repair from browser failure through trusted apply. */
class ExistingPageObjectRepairEndToEndIT {
    private static final String PAGE_OBJECT = "src/main/java/fixture/repair/ExistingLoginPage.java";
    private static final String TEST_SOURCE = "src/test/java/fixture/repair/InvalidLoginTest.java";

    @TempDir Path temporary;

    @Test
    void repairsARealFailedSelectorThroughLiveIntelligenceAndRerunsTheSameTest() throws Exception {
        Path workspace = copyFixture();
        ExistingProjectIndex baselineIndex = index(workspace);
        String originalPageSource = Files.readString(workspace.resolve(PAGE_OBJECT));
        TargetedJavaCompiler.CompilationResult baselineCompilation = compile(workspace);
        assertTrue(baselineCompilation.successful(), baselineCompilation.diagnostics().toString());

        try (RepairFixture application = RepairFixture.start()) {
            WebDriver driver = BrowserTestHarness.createDriver();
            try {
                driver.get(application.url());
                ApplicationMapper mapper = ApplicationMapper.start(driver,
                        ApplicationMapperOptions.builder("Existing repair fixture").build());
                mapper.observe();
                ApplicationModel baselineModel = mapper.model();
                ApplicationModel.ElementModel baselineButton = loginButton(baselineModel);
                assertEquals("old-login-button", baselineButton.preferredSelector().value());

                PageObjectCorrelation baselineCorrelation = correlate(baselineModel, baselineIndex);
                PageObjectCorrelation.ElementCorrelation buttonCorrelation = baselineCorrelation.elements().stream()
                        .filter(value -> value.applicationElementId().equals(baselineButton.elementId()))
                        .filter(value -> value.sourceDeclarationRef() != null)
                        .findFirst().orElseThrow();
                assertEquals(PageObjectCorrelation.State.EXACT, buttonCorrelation.state());

                Execution baseline = execute(driver, application.url(), baselineCompilation, "repair baseline");
                assertTrue(baseline.passed());
                assertEquals(TraceStatus.PASSED, baseline.session().metadata().status());
                assertTrue(baseline.session().events().size() >= 3);

                application.changed().set(true);
                Execution failed = execute(driver, application.url(), baselineCompilation, "repair failure");
                assertInstanceOf(NoSuchElementException.class, failed.failure());
                assertEquals(TraceStatus.FAILED, failed.session().metadata().status());
                assertTrue(failed.session().events().size() >= 3, "failed observed Selenium call must leave Lens evidence");
                assertTrue(failed.session().events().stream().anyMatch(event ->
                                event.attributes().getOrDefault("metadata.testlens.selector.locator.value", "")
                                        .contains("old-login-button")),
                        "Lens evidence must identify the real failing selector");
                assertTrue(failed.session().events().stream().anyMatch(event ->
                                event.status() == TraceStatus.FAILED
                                        && event.failure() != null
                                        && event.failure().exceptionType().endsWith("NoSuchElementException")),
                        "Lens evidence must retain the real Selenium failure");

                driver.get(application.url());
                mapper.observe();
                ApplicationModel changedModel = mapper.model();
                ApplicationModel.ElementModel changedButton = loginButton(changedModel);
                assertEquals(baselineButton.elementId(), changedButton.elementId(),
                        "selector drift must not change application element identity");
                assertNotEquals(baselineButton.preferredSelector().value(), changedButton.preferredSelector().value());
                assertEquals(ApplicationModel.EvidenceSource.LIVE_CANDIDATE_ANALYSIS,
                        changedButton.preferredSelector().source());
                assertEquals("VERIFIED_IN_SCOPE", changedButton.preferredSelector().validation());
                assertEquals("SAME_TARGET", changedButton.preferredSelector().sameTarget());
                assertTrue(changedButton.preferredSelector().unique());

                FailureClassification classification = classify(failed.failure(), baselineButton, changedButton);
                assertEquals(FailureClassification.Category.SELECTOR_INSTABILITY, classification.category());

                SourceImpact impact = new SourceImpactAnalyzer().analyze(
                        baselineButton.elementId(), baselineCorrelation, baselineIndex, 100);
                ExistingProjectIndex.ElementEntry sourceElement = baselineIndex.elements().stream()
                        .filter(value -> value.id().equals(buttonCorrelation.sourceElementId())).findFirst().orElseThrow();
                assertEquals("loginButton", sourceElement.name());
                assertEquals(List.of("invalidPassword"), impact.methodIds().stream()
                        .map(id -> methodName(baselineIndex, id)).distinct().toList());
                assertEquals(List.of("execute"), impact.testIds().stream()
                        .map(id -> testMethodName(baselineIndex, id)).distinct().toList());

                RepairProposal proposal = new SelectorRepairPlanner().propose("repair-login-button", classification,
                        baselineButton, changedButton, buttonCorrelation, impact, baselineIndex,
                        "trace:" + failed.session().id(), "mapping:changed-login");
                assertEquals(RepairProposal.ApplicationPolicy.PROPOSE_ONLY, proposal.applicationPolicy());
                assertEquals(PAGE_OBJECT, proposal.sourceTarget().logicalPath());
                assertEquals(ApplicationModel.EvidenceSource.LIVE_CANDIDATE_ANALYSIS.name(),
                        proposal.replacementEvidence().source());

                TrustedRepairApplier.ApplyResult applied = new TrustedRepairApplier().apply(workspace,
                        new TrustedRepairApplier.ApplyRequest(proposal, baselineIndex,
                                List.of("src/main/java"), true));
                assertEquals(TrustedRepairApplier.Status.APPLIED, applied.status());

                String repairedPageSource = Files.readString(workspace.resolve(PAGE_OBJECT));
                RepairProposal.SourceRange range = proposal.sourceTarget().range();
                String replacement = SelectorJavaExpression.byExpression(
                        proposal.replacementEvidence().strategy(), proposal.replacementEvidence().value());
                String oldExpression = SelectorJavaExpression.byExpression(
                        proposal.sourceTarget().oldStrategy(), proposal.sourceTarget().oldValue());
                assertNotNull(replacement);
                assertNotNull(oldExpression);
                int expressionStart = originalPageSource.indexOf(oldExpression, range.startOffset());
                assertTrue(expressionStart >= range.startOffset()
                        && expressionStart + oldExpression.length() <= range.endOffsetExclusive());
                assertEquals(originalPageSource.substring(0, expressionStart) + replacement
                                + originalPageSource.substring(expressionStart + oldExpression.length()),
                        repairedPageSource, "trusted apply must change only the indexed selector expression");

                ExistingProjectIndex repairedIndex = index(workspace);
                assertNotEquals(baselineIndex.projectFingerprint(), repairedIndex.projectFingerprint());
                TargetedJavaCompiler.CompilationResult repairedCompilation = compile(workspace);
                assertTrue(repairedCompilation.successful(), repairedCompilation.diagnostics().toString());
                Execution repaired = execute(driver, application.url(), repairedCompilation, "repair verified");
                assertTrue(repaired.passed());
                assertEquals(TraceStatus.PASSED, repaired.session().metadata().status());
                assertTrue(repaired.session().events().size() >= 3);
            } finally {
                driver.quit();
            }
        }
    }

    private PageObjectCorrelation correlate(ApplicationModel model, ExistingProjectIndex index) {
        String pageId = model.pages().get(0).pageId();
        String classId = index.classes().stream()
                .filter(value -> value.qualifiedName().equals("fixture.repair.ExistingLoginPage"))
                .map(ExistingProjectIndex.ClassEntry::id).findFirst().orElseThrow();
        return new PageObjectCorrelator().correlate(model, index,
                new CorrelationOverrides(Map.of(classId, pageId), Map.of()));
    }

    private static ApplicationModel.ElementModel loginButton(ApplicationModel model) {
        return model.pages().stream().flatMap(page -> page.elements().stream())
                .filter(element -> "Log in".equals(element.accessibleName()))
                .findFirst().orElseThrow();
    }

    private static FailureClassification classify(Throwable failure, ApplicationModel.ElementModel before,
                                                  ApplicationModel.ElementModel after) {
        return new EvidenceFailureClassifier().classify(new EvidenceFailureClassifier.Signals(
                false, List.of(), false, List.of(), false, List.of(), true,
                List.of("NoSuchElementException: " + failure.getMessage(),
                        "old candidate no longer resolves: " + before.preferredSelector().candidateId(),
                        "replacement live validation: " + after.preferredSelector().validation(),
                        "replacement same target: " + after.preferredSelector().sameTarget()),
                List.of(), false, List.of(), List.of(before.elementId())));
    }

    private Execution execute(WebDriver raw, String url, TargetedJavaCompiler.CompilationResult compilation,
                              String sessionName) throws Exception {
        raw.get(url);
        TestLens lens = TestLens.attach(raw);
        WebDriver observed = lens.observeDriver();
        UiTestLensSession session = lens.startSession(sessionName);
        Throwable failure = null;
        boolean passed = false;
        try {
            Class<?> test = compilation.output().loadClass("fixture.repair.InvalidLoginTest", getClass().getClassLoader());
            Object result = test.getMethod("execute", WebDriver.class, String.class, String.class)
                    .invoke(null, observed, "fixture-user", "wrong-secret");
            passed = Boolean.TRUE.equals(result);
            if (passed) lens.finishPassed();
            else {
                failure = new AssertionError("Login error was not visible");
                lens.finishFailed(failure);
            }
        } catch (InvocationTargetException invocation) {
            failure = invocation.getCause();
            lens.finishFailed(failure);
        }
        return new Execution(passed, failure, session);
    }

    private TargetedJavaCompiler.CompilationResult compile(Path workspace) throws IOException {
        Path pagePath = Path.of(PAGE_OBJECT);
        Path testPath = Path.of(TEST_SOURCE);
        List<TargetedJavaCompiler.SourceUnit> sources = List.of(
                new TargetedJavaCompiler.SourceUnit(pagePath, "fixture.repair.ExistingLoginPage",
                        Files.readString(workspace.resolve(pagePath)), ArtifactEnvelope.digest("")),
                new TargetedJavaCompiler.SourceUnit(testPath, "fixture.repair.InvalidLoginTest",
                        Files.readString(workspace.resolve(testPath)), ArtifactEnvelope.digest("")));
        return new TargetedJavaCompiler().compile(new TargetedJavaCompiler.CompilationRequest(
                sources, 17, System.getProperty("java.class.path"), 20), Map.of());
    }

    private ExistingProjectIndex index(Path workspace) {
        List<Path> classpath = Arrays.stream(System.getProperty("java.class.path").split(
                        java.util.regex.Pattern.quote(System.getProperty("path.separator"))))
                .filter(value -> !value.isBlank()).map(Path::of).toList();
        return new ExistingProjectIndexer().index(new ExistingProjectIndexer.Request(workspace,
                List.of(workspace.resolve("src/main/java"), workspace.resolve("src/test/java")), classpath));
    }

    private Path copyFixture() throws Exception {
        URI fixtureUri = ExistingPageObjectRepairEndToEndIT.class
                .getResource("/repair-existing-project/").toURI();
        Path source = Path.of(fixtureUri);
        Path target = temporary.resolve("existing-project");
        try (var paths = Files.walk(source)) {
            for (Path path : paths.toList()) {
                Path destination = target.resolve(source.relativize(path).toString());
                if (Files.isDirectory(path)) Files.createDirectories(destination);
                else Files.copy(path, destination, StandardCopyOption.COPY_ATTRIBUTES);
            }
        }
        return target;
    }

    private static String methodName(ExistingProjectIndex index, String id) {
        return index.methods().stream().filter(value -> value.id().equals(id))
                .map(ExistingProjectIndex.MethodEntry::name).findFirst().orElse(id);
    }

    private static String testMethodName(ExistingProjectIndex index, String id) {
        ExistingProjectIndex.TestEntry test = index.tests().stream().filter(value -> value.id().equals(id))
                .findFirst().orElseThrow();
        return methodName(index, test.methodId());
    }

    private record Execution(boolean passed, Throwable failure, UiTestLensSession session) { }

    private record RepairFixture(HttpServer server, AtomicBoolean changed) implements AutoCloseable {
        static RepairFixture start() throws IOException {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            AtomicBoolean changed = new AtomicBoolean();
            RepairFixture fixture = new RepairFixture(server, changed);
            server.createContext("/login", exchange -> {
                byte[] body = fixture.html().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                exchange.getResponseHeaders().set("Cache-Control", "no-store");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream output = exchange.getResponseBody()) { output.write(body); }
            });
            server.start();
            return fixture;
        }

        String url() { return "http://127.0.0.1:" + server.getAddress().getPort() + "/login"; }

        private String html() {
            String selector = changed.get()
                    ? "data-testid='login-submit'"
                    : "id='old-login-button'";
            return """
                    <!doctype html><html><head><title>Login</title></head><body>
                    <main aria-label='Login'>
                      <label for='username'>Username</label><input id='username' autocomplete='username'>
                      <label for='password'>Password</label><input id='password' type='password' autocomplete='current-password'>
                      <button type='button' %s aria-label='Log in'>Log in</button>
                      <p id='login-error' role='alert' hidden>Invalid username or password</p>
                      <script>document.querySelector('button[aria-label="Log in"]').addEventListener('click',
                        () => document.getElementById('login-error').hidden = false);</script>
                    </main></body></html>
                    """.formatted(selector);
        }

        @Override public void close() { server.stop(0); }
    }
}
