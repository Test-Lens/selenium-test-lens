package io.github.testlens.application.mapper;

import io.github.testlens.application.model.ApplicationModel;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SemanticNamingTest {
    @Test
    void prefersConfiguredTestAttributeAndProducesDeterministicMechanicalName() {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("data-testid", "checkout-submit");
        attributes.put("data-qa", "ignored-second-value");
        PageScanner.DiscoveredElement element = element("button", "submit", "button", "Place order", "Place order", attributes, false);

        assertEquals("checkoutSubmitButton", SemanticNaming.elementName(element, List.of(), "fingerprint", ApplicationOverrides.empty()));
        assertEquals(ApplicationModel.ElementType.BUTTON, SemanticNaming.type(element));
        assertEquals(List.of(ApplicationModel.Action.CLICK, ApplicationModel.Action.ASSERT),
                SemanticNaming.actions(ApplicationModel.ElementType.BUTTON, false));
    }

    @Test
    void explicitFingerprintOverrideWinsAndDisabledControlIsAssertionOnly() {
        PageScanner.DiscoveredElement element = element("input", "checkbox", "checkbox", "Marketing", "Marketing", Map.of(), true);
        ApplicationOverrides overrides = new ApplicationOverrides(
                ApplicationOverrides.SCHEMA_VERSION,
                Map.of(),
                Map.of("fp-1", "accept marketing"),
                java.util.Set.of(),
                java.util.Set.of());

        assertEquals("acceptMarketing", SemanticNaming.elementName(element, List.of(), "fp-1", overrides));
        assertEquals(ApplicationModel.ElementType.CHECKBOX, SemanticNaming.type(element));
        assertEquals(List.of(ApplicationModel.Action.ASSERT), SemanticNaming.actions(ApplicationModel.ElementType.CHECKBOX, true));
    }

    @Test
    void identifiersRemainValidAndStableForNumbersAndMissingSemantics() {
        assertEquals("X42Customers", SemanticNaming.javaIdentifier("42 customers", "Fallback"));
        assertEquals("fallback", SemanticNaming.lowerCamel("", "Fallback"));
        PageScanner.DiscoveredElement custom = element("date-picker", "", "", "", "", Map.of(), false);
        assertEquals(ApplicationModel.ElementType.CUSTOM, SemanticNaming.type(custom));
        assertEquals("datePicker", SemanticNaming.elementName(custom, List.of(), "fp", ApplicationOverrides.empty()));
    }

    @Test
    void containerNamesDoNotAbsorbDescendantOrUserText() {
        PageScanner.DiscoveredElement form = elementWithText(
                "form", "Alice Example alice@example.test Save");
        PageScanner.DiscoveredElement table = elementWithText(
                "table", "Customer One Customer Two secret-value");

        assertEquals("form", SemanticNaming.elementName(form, List.of(), "form-fp", ApplicationOverrides.empty()));
        assertEquals("table", SemanticNaming.elementName(table, List.of(), "table-fp", ApplicationOverrides.empty()));
    }

    private static PageScanner.DiscoveredElement element(
            String tag,
            String inputType,
            String role,
            String label,
            String accessibleName,
            Map<String, String> attributes,
            boolean disabled) {
        return new PageScanner.DiscoveredElement(
                null, null, false, tag, inputType, role, label, accessibleName, "", null, disabled, null, attributes);
    }

    private static PageScanner.DiscoveredElement elementWithText(String tag, String text) {
        return new PageScanner.DiscoveredElement(
                null, null, false, tag, "", "", null, "", text, null, false, null, Map.of());
    }
}
