package io.github.testlens.consumer;

import io.github.testlens.TestLens;
import io.github.testlens.TestLensOptions;
import io.github.testlens.core.logging.UiTestLensLogEntry;
import io.github.testlens.core.logging.UiTestLensLogger;
import io.github.testlens.core.redaction.RedactionPolicy;
import io.github.testlens.core.trace.TraceEvent;
import io.github.testlens.core.trace.UiTestLensSession;
import io.github.testlens.junit5.TestLensExtension;
import io.github.testlens.selenium.assertions.UiPageExpect;
import io.github.testlens.selenium.locator.UiLocator;
import io.github.testlens.testng.TestLensTestNg;
import io.github.testlens.testng.TestLensTestNgListener;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublishedArtifactsSmokeTest {
    @Test
    void loadsPublishedApiAndRunsCoreDiagnosticsWithoutAWebDriver() throws Exception {
        String canary = "consumer-gate-secret-4f07";
        RedactionPolicy policy = RedactionPolicy.builder().secret(canary).build();
        assertEquals("token=[REDACTED]", policy.redact("token=" + canary));

        TestLensOptions options = TestLensOptions.builder().redactionPolicy(policy).build();
        assertEquals(policy, options.redactionPolicy());
        assertNotNull(TestLens.class);
        assertNotNull(UiLocator.class);
        assertNotNull(UiPageExpect.class);

        TestLensExtension extension = TestLensExtension.builder(() -> null).lensOptions(options).build();
        assertNotNull(extension);
        assertNotNull(new TestLensTestNgListener());
        assertTrue(TestLensTestNg.class.isAnnotation());
        assertNotNull(Class.forName("io.github.testlens.react.ReactSupport"));

        UiTestLensSession session = UiTestLensSession.start("consumer " + canary,
                io.github.testlens.core.trace.RetryOutcomePolicy.REPORT_ONLY, 0, policy);
        session.addEvent(TraceEvent.info("consumer", "password=" + canary));
        assertFalse(session.metadata().name().contains(canary));
        assertFalse(session.events().stream().anyMatch(event -> event.message().contains(canary)));

        List<UiTestLensLogEntry> entries = new ArrayList<>();
        UiTestLensLogger.builder().redactionPolicy(policy).sink(entries::add).build()
                .info("Authorization: Bearer " + canary);
        assertEquals(1, entries.size());
        assertFalse(entries.get(0).message().contains(canary));
        assertTrue(entries.get(0).message().contains("[REDACTED]"));

        List.of(
                "io.github.testlens.TestLens",
                "io.github.testlens.react.ReactSupport",
                "io.github.testlens.junit5.TestLensExtension",
                "io.github.testlens.testng.TestLensTestNgListener"
        ).forEach(name -> assertNotNull(load(name)));
    }

    private static Class<?> load(String name) {
        try {
            return Class.forName(name, true, PublishedArtifactsSmokeTest.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError | java.util.ServiceConfigurationError failure) {
            throw new AssertionError("Published API failed to load: " + name, failure);
        }
    }
}
