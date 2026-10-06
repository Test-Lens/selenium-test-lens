package io.github.testlens.application.mapper;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationMapperOptionsTest {
    @Test
    void defaultsAreBoundedConservativeAndDoNotEnableExploration() {
        ApplicationMapperOptions options = ApplicationMapperOptions.builder("Example").build();

        assertEquals(ApplicationMapperOptions.Mode.CURRENT_PAGE, options.mode());
        assertEquals(ApplicationMapperOptions.ActionDecision.DENY,
                options.actionPolicy().evaluate(new ApplicationMapperOptions.SafeAction("p", "e", "link", "LINK", "https://example.test")));
        assertTrue(options.maxDiscoveredNodes() > options.maxActionableElements());
        assertTrue(options.maxActionableElements() >= options.maxCandidateAnalyses());
        assertEquals(List.of("data-testid", "data-test", "data-qa"), options.preferredTestAttributes());
    }

    @Test
    void candidateAnalysisLimitIsClampedToActionableElements() {
        ApplicationMapperOptions options = ApplicationMapperOptions.builder("Example")
                .maxActionableElements(7)
                .maxCandidateAnalyses(20)
                .build();

        assertEquals(7, options.maxCandidateAnalyses());
    }

    @Test
    void rejectsUnsafeAttributesAndOutOfRangeLimits() {
        assertThrows(IllegalArgumentException.class,
                () -> ApplicationMapperOptions.builder("Example").preferredTestAttributes(List.of("data-testid", "bad attr")));
        assertThrows(IllegalArgumentException.class,
                () -> ApplicationMapperOptions.builder("Example").maxDiscoveredNodes(0));
        assertThrows(IllegalArgumentException.class,
                () -> ApplicationMapperOptions.builder("Example").maxShadowDepth(9));
        assertThrows(IllegalArgumentException.class,
                () -> ApplicationMapperOptions.builder(" "));
    }

    @Test
    void typedMappingFailureKeepsStableCodeAndCause() {
        IllegalStateException cause = new IllegalStateException("browser gone");
        MappingException failure = new MappingException(MappingException.Code.BROWSER_SCRIPT_FAILED, "Unable to scan", cause);

        assertEquals(MappingException.Code.BROWSER_SCRIPT_FAILED, failure.code());
        assertSame(cause, failure.getCause());
        assertEquals("Unable to scan", failure.getMessage());
    }

    @Test
    void overrideSchemaIsVersioned() {
        assertThrows(IllegalArgumentException.class,
                () -> new ApplicationOverrides(2, null, null, null, null));
        assertEquals(ApplicationOverrides.SCHEMA_VERSION, ApplicationOverrides.empty().schemaVersion());
    }
}
