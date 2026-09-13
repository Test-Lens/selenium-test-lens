package io.github.testlens.allure;

import io.github.testlens.TestLensFinalizationResult;
import io.qameta.allure.testng.AllureTestNg;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.testng.TestNG;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AllureRunnerIntegrationTest {
    @TempDir Path temp;
    private static volatile TestLensFinalizationResult fixtureResult;
    private static volatile AllureAttachResult fixtureAttachResult;

    @Test
    void realAllureJupiterLifecycleOwnsContextAndWritesAttachments() throws Exception {
        Path results = Path.of("target", "allure-results");
        Set<Path> before = files(results);
        fixtureResult = AllureTestArtifacts.failed(temp.resolve("junit-evidence"), "JUNIT-ONLY");
        var request = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectClass(JupiterFixture.class)).build();
        var launcher = LauncherFactory.create(LauncherConfig.builder()
                .enableTestEngineAutoRegistration(true)
                .enableTestExecutionListenerAutoRegistration(true).build());
        launcher.execute(request);
        assertEquals(AllureAttachStatus.ATTACHED, fixtureAttachResult.status());
        assertRunnerResult(results, before, "fixture");
    }

    @Test
    void realAllureTestNgLifecycleOwnsContextAndWritesAttachments() throws Exception {
        Path results = Path.of("target", "allure-results");
        Set<Path> before = files(results);
        fixtureResult = AllureTestArtifacts.failed(temp.resolve("testng-evidence"), "TESTNG-ONLY");
        TestNG testng = new TestNG(false);
        testng.setUseDefaultListeners(false);
        testng.setTestClasses(new Class<?>[]{TestNgFixture.class});
        testng.addListener(new AllureTestNg());
        testng.run();
        assertFalse(testng.hasFailure());
        assertEquals(AllureAttachStatus.ATTACHED, fixtureAttachResult.status());
        assertRunnerResult(results, before, "TestNgFixture.fixture");
    }

    private static void assertRunnerResult(Path results, Set<Path> before, String testName) throws Exception {
        Set<Path> created = files(results);
        created.removeAll(before);
        String all;
        try (var stream = created.stream()) {
            all = stream.filter(path -> path.toString().endsWith("-result.json"))
                    .map(path -> { try { return Files.readString(path); } catch (Exception e) { throw new RuntimeException(e); } })
                    .filter(json -> json.contains(testName) && json.contains("Test Lens"))
                    .findFirst().orElseThrow();
        }
        assertTrue(all.contains("Test Lens — Diagnostic screenshot"));
        assertEquals(4, created.stream().filter(path -> path.getFileName().toString().contains("-attachment.")).count());
    }

    private static Set<Path> files(Path directory) throws Exception {
        if (!Files.isDirectory(directory)) return new java.util.HashSet<>();
        try (var stream = Files.list(directory)) {
            return new java.util.HashSet<>(stream.toList());
        }
    }

    public static class JupiterFixture {
        @org.junit.jupiter.api.Test
        void fixture() { fixtureAttachResult = AllureTestLens.attach(fixtureResult); }
    }

    public static class TestNgFixture {
        @org.testng.annotations.Test(testName = "testng fixture")
        public void fixture() { fixtureAttachResult = AllureTestLens.attach(fixtureResult); }
    }
}
