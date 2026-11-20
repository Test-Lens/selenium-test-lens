package io.github.testlens.selenium.locator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LocatorObservationMetadataFatalRegressionTest {
    @ParameterizedTest
    @MethodSource("criticalErrors")
    void descriptionPropagatesCriticalErrorsFromDisplayUnchanged(Error fatal) {
        By by = new By() {
            @Override public List<WebElement> findElements(SearchContext context) { return List.of(); }
            @Override public String toString() { throw fatal; }
        };
        assertSame(fatal, assertThrows(Error.class, () -> UiLocatorDescription.of(by, "Target")));
    }

    @ParameterizedTest
    @MethodSource("criticalErrors")
    void descriptionPropagatesCriticalErrorsFromRemoteParametersUnchanged(Error fatal) {
        assertSame(fatal, assertThrows(Error.class,
                () -> UiLocatorDescription.of(new ThrowingRemoteBy(fatal), "Target")));
    }

    @Test
    void optionalAssertionErrorsStillUseOpaqueFallback() {
        By display = new By() {
            @Override public List<WebElement> findElements(SearchContext context) { return List.of(); }
            @Override public String toString() { throw new AssertionError("optional display unavailable"); }
        };
        assertTrue(UiLocatorDescription.of(display, "").displayName().startsWith("Locator "));
        assertEquals("remote locator", UiLocatorDescription.of(
                new ThrowingRemoteBy(new AssertionError("optional parameters unavailable")), "").displayName());
        assertEquals("CUSTOM_OPAQUE", LocatorObservationMetadata.observation(
                new ThrowingRemoteBy(new AssertionError("optional parameters unavailable")), "", "remote locator",
                "FIND_ONE", "RESOLVED", null, 0).get("testlens.selector.locator.supportKind"));
    }

    private static List<Error> criticalErrors() {
        return List.of(new InternalError("synthetic VM failure"), new ThreadDeath(),
                new LinkageError("synthetic linkage failure"));
    }

    private static final class ThrowingRemoteBy extends By implements By.Remotable {
        private final Error failure;
        private ThrowingRemoteBy(Error failure) { this.failure = failure; }
        @Override public List<WebElement> findElements(SearchContext context) { return List.of(); }
        @Override public Parameters getRemoteParameters() { throw failure; }
        @Override public String toString() { return "remote locator"; }
    }
}
