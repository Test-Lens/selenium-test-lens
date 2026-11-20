package io.github.testlens;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LocatorObservationFatalAuditTest {
    @Test void vmAndLinkageFailuresInOptionalDisplayRemainFatal() {
        for (Error failure : fatalErrors()) {
            By by = new By() {
                @Override public List<WebElement> findElements(SearchContext context) { return List.of(); }
                @Override public String toString() { throw failure; }
            };
            assertSame(failure, assertThrows(Error.class, () -> LocatorObservationMetadata.safeDisplay(by)));
            assertSame(failure, assertThrows(Error.class, () -> LocatorObservationMetadata.locator(by, "")));
        }
    }

    @Test void vmAndLinkageFailuresInRemoteParametersRemainFatal() {
        for (Error failure : fatalErrors()) {
            assertSame(failure, assertThrows(Error.class,
                    () -> LocatorObservationMetadata.locator(new BrokenRemoteBy(failure), "")));
        }
    }

    private static Error[] fatalErrors() {
        return new Error[]{new OutOfMemoryError("synthetic, no memory exhaustion"),
                new StackOverflowError("synthetic"), new LinkageError("binary incompatibility"), new ThreadDeath()};
    }

    private static final class BrokenRemoteBy extends By implements By.Remotable {
        private final Error failure;
        BrokenRemoteBy(Error failure) { this.failure = failure; }
        @Override public Parameters getRemoteParameters() { throw failure; }
        @Override public List<WebElement> findElements(SearchContext context) { return List.of(); }
        @Override public String toString() { return "custom locator"; }
    }
}
