package io.github.testlens.application.mapper;

import io.github.testlens.core.redaction.RedactionPolicy;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class UrlPatternNormalizerTest {
    @Test
    void normalizesNumericUuidAndHashPathSegmentsWithoutRetainingSecrets() {
        RedactionPolicy redaction = RedactionPolicy.defaults();

        assertEquals("https://example.test/customers/{id}",
                UrlPatternNormalizer.normalize("https://example.test/customers/12345?token=secret", redaction));
        assertEquals("https://example.test/customers/{id}",
                UrlPatternNormalizer.normalize("https://example.test/customers/550e8400-e29b-41d4-a716-446655440000", redaction));
        assertEquals("https://example.test/assets/{id}",
                UrlPatternNormalizer.normalize("https://example.test/assets/0123456789abcdef01234567", redaction));
    }

    @Test
    void keepsStableShortSegmentsAndProducesDeterministicOpaqueFallback() {
        RedactionPolicy redaction = RedactionPolicy.defaults();

        assertEquals("https://example.test/releases/42a", UrlPatternNormalizer.normalize("https://example.test/releases/42a", redaction));
        String first = UrlPatternNormalizer.normalize("not a valid URI %", redaction);
        String second = UrlPatternNormalizer.normalize("not a valid URI %", redaction);

        assertEquals(first, second);
        assertNotEquals("not a valid URI %", first);
    }

    @Test
    void pageNameOverrideUsesTheNormalizedPattern() {
        String pattern = UrlPatternNormalizer.normalize("https://example.test/customer/98765", RedactionPolicy.defaults());
        ApplicationOverrides overrides = new ApplicationOverrides(
                ApplicationOverrides.SCHEMA_VERSION,
                Map.of(pattern, "Customer details"),
                Map.of(),
                java.util.Set.of(),
                java.util.Set.of());

        assertEquals("CustomerDetails", SemanticNaming.pageName(pattern, "ignored", overrides));
    }
}
