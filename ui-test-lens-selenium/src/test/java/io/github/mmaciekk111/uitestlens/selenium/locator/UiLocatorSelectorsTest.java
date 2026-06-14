package io.github.mmaciekk111.uitestlens.selenium.locator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiLocatorSelectorsTest {

    @Test
    void xpathLiteralHandlesSimpleText() {
        assertEquals("'Save'", UiLocatorSelectors.xpathLiteral("Save"));
    }

    @Test
    void xpathLiteralHandlesApostrophe() {
        assertEquals("\"John's\"", UiLocatorSelectors.xpathLiteral("John's"));
    }

    @Test
    void xpathLiteralHandlesQuotes() {
        assertEquals("'\"quoted\"'", UiLocatorSelectors.xpathLiteral("\"quoted\""));
    }

    @Test
    void xpathLiteralHandlesApostropheAndQuotes() {
        String literal = UiLocatorSelectors.xpathLiteral("John's \"quoted\" value");

        assertTrue(literal.startsWith("concat("));
        assertTrue(literal.contains("\"'\""));
        assertTrue(literal.contains("'\"'"));
    }

    @Test
    void cssAttributeEqualsEscapesAttributeValue() {
        assertEquals("[data-testid='save\\'button']", UiLocatorSelectors.cssAttributeEquals("data-testid", "save'button"));
    }

    @Test
    void normalizeSpaceExpressionWrapsExpression() {
        assertEquals("normalize-space(.)", UiLocatorSelectors.normalizeSpaceExpression("."));
    }
}
