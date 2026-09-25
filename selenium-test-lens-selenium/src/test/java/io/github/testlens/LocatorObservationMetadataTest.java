package io.github.testlens;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.locators.RelativeLocator;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LocatorObservationMetadataTest {
    @Test void standardByUsesOfficialRemoteParametersWithoutStrategyNormalization() {
        Map<By, String> values = Map.of(
                By.id("save"), "id",
                By.cssSelector("button.save"), "css selector",
                By.xpath("//button"), "xpath",
                By.name("save"), "name",
                By.className("save"), "class name",
                By.tagName("button"), "tag name",
                By.linkText("Save"), "link text",
                By.partialLinkText("Sav"), "partial link text");

        values.forEach((by, expected) -> {
            var locator = LocatorObservationMetadata.locator(by, "");
            assertEquals(expected, locator.strategy());
            assertEquals(((By.Remotable) by).getRemoteParameters().value(), locator.value());
            assertEquals("KNOWN", locator.valueState());
            assertEquals("STRUCTURED", locator.supportKind());
        });
    }

    @Test void unknownFutureRemoteStrategyWithScalarValueRemainsStructured() {
        By by = new FutureBy("future-value");
        var locator = LocatorObservationMetadata.locator(by, "Future");

        assertEquals("future-strategy", locator.strategy());
        assertEquals("future-value", locator.value());
        assertEquals("STRUCTURED", locator.supportKind());
    }

    @Test void relativeLocatorIsComplexWithoutSerializingItsObjectGraph() {
        By relative = RelativeLocator.with(By.tagName("button")).above(By.id("footer"));
        var locator = LocatorObservationMetadata.locator(relative, "");

        assertEquals("relative", locator.strategy());
        assertEquals("UNAVAILABLE", locator.valueState());
        assertEquals("REMOTE_COMPLEX", locator.supportKind());
        assertTrue(locator.value().isEmpty());
        assertFalse(locator.display().contains("footer"));
        assertTrue(locator.display().startsWith("Locator "));
    }

    @Test void opaqueThrowingDisplayIsBoundedAndNonFatal() {
        By throwing = new By() {
            @Override public List<WebElement> findElements(SearchContext context) { return List.of(); }
            @Override public String toString() { throw new AssertionError("token=do-not-retain"); }
        };

        var locator = LocatorObservationMetadata.locator(throwing, "");

        assertEquals("CUSTOM_OPAQUE", locator.supportKind());
        assertEquals("UNAVAILABLE", locator.valueState());
        assertFalse(locator.display().contains("do-not-retain"));
    }

    @Test void contextDepthIsHardBoundedAndMarkedPartial() {
        var context = LocatorObservationMetadata.root();
        for (int index = 0; index < 40; index++) {
            context = context.append(new LocatorObservationMetadata.Segment(
                    "FRAME", "KNOWN", null, "", "INDEX", String.valueOf(index)));
        }

        assertEquals(LocatorObservationMetadata.MAX_CONTEXT_SEGMENTS, context.segments().size());
        assertEquals("DRIVER_ROOT", context.segments().get(0).kind());
        assertEquals("PARTIAL", context.knowledge());
        assertEquals("39", context.segments().get(context.segments().size() - 1).referenceValue());
    }

    private static final class FutureBy extends By implements By.Remotable {
        private final String value;
        private FutureBy(String value) { this.value = value; }
        @Override public List<WebElement> findElements(SearchContext context) { return List.of(); }
        @Override public Parameters getRemoteParameters() { return new Parameters("future-strategy", value); }
        @Override public String toString() { return "future locator"; }
    }
}
