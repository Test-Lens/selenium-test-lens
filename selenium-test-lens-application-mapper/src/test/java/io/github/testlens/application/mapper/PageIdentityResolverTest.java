package io.github.testlens.application.mapper;

import io.github.testlens.core.redaction.RedactionPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class PageIdentityResolverTest {
    private static final List<String> TEST_ATTRIBUTES = List.of("data-testid", "data-test", "data-qa");
    private static final RedactionPolicy REDACTION = RedactionPolicy.defaults();

    @Test
    void sameUrlAndOverlappingMeaningfulSignalsAreStatesOfOnePage() {
        PageIdentityResolver resolver = resolver(ApplicationOverrides.empty());

        PageIdentityResolver.Resolution login = resolver.resolve(
                "https://example.test/account", "Account", List.of(input("username"), input("password")), TEST_ATTRIBUTES, REDACTION);
        PageIdentityResolver.Resolution validationError = resolver.resolve(
                "https://example.test/account", "Account", List.of(input("username"), button("sign-in")), TEST_ATTRIBUTES, REDACTION);

        assertEquals(login.pageId(), validationError.pageId());
    }

    @Test
    void sameUrlWithDisjointLoginAndDashboardSignalsCreatesSeparatePages() {
        PageIdentityResolver resolver = resolver(ApplicationOverrides.empty());

        PageIdentityResolver.Resolution login = resolver.resolve(
                "https://example.test/app", "Application", List.of(input("username"), button("sign-in")), TEST_ATTRIBUTES, REDACTION);
        PageIdentityResolver.Resolution dashboard = resolver.resolve(
                "https://example.test/app", "Application", List.of(button("open-customers"), button("user-menu")), TEST_ATTRIBUTES, REDACTION);

        assertNotEquals(login.pageId(), dashboard.pageId());
    }

    @Test
    void dynamicRouteInstancesWithSameStructuralFamilyResolveToOnePage() {
        PageIdentityResolver resolver = resolver(ApplicationOverrides.empty());
        String firstPattern = UrlPatternNormalizer.normalize("https://example.test/customers/12345", REDACTION);
        String secondPattern = UrlPatternNormalizer.normalize("https://example.test/customers/67890", REDACTION);

        PageIdentityResolver.Resolution first = resolver.resolve(
                firstPattern, "Customer details", List.of(input("customer-name"), button("save-customer")), TEST_ATTRIBUTES, REDACTION);
        PageIdentityResolver.Resolution second = resolver.resolve(
                secondPattern, "Customer details", List.of(input("customer-name"), button("save-customer")), TEST_ATTRIBUTES, REDACTION);

        assertEquals(firstPattern, secondPattern);
        assertEquals(first.pageId(), second.pageId());
    }

    @Test
    void explicitIdentityGroupOverridesUrlAndSignalDifferences() {
        PageIdentityResolver probe = resolver(ApplicationOverrides.empty());
        List<PageScanner.DiscoveredElement> compact = List.of(button("open-customer"));
        List<PageScanner.DiscoveredElement> expanded = List.of(input("search-customer"), button("open-customer"));
        String compactFingerprint = probe.resolve(
                "https://example.test/customers", "Customers", compact, TEST_ATTRIBUTES, REDACTION).observationFingerprint();
        String expandedFingerprint = probe.resolve(
                "https://example.test/customer/{id}", "Customer", expanded, TEST_ATTRIBUTES, REDACTION).observationFingerprint();
        ApplicationOverrides overrides = new ApplicationOverrides(
                ApplicationOverrides.SCHEMA_VERSION,
                Map.of(), Map.of(), Set.of(), Set.of(),
                Map.of(compactFingerprint, "customers-workspace", expandedFingerprint, "customers-workspace"),
                Map.of("customers-workspace", "Customers workspace"));
        PageIdentityResolver resolver = resolver(overrides);

        PageIdentityResolver.Resolution first = resolver.resolve(
                "https://example.test/customers", "Customers", compact, TEST_ATTRIBUTES, REDACTION);
        PageIdentityResolver.Resolution second = resolver.resolve(
                "https://example.test/customer/{id}", "Customer", expanded, TEST_ATTRIBUTES, REDACTION);

        assertEquals(first.pageId(), second.pageId());
        assertEquals("customers-workspace", first.identityGroup());
        assertEquals("customers-workspace", second.identityGroup());
    }

    @Test
    void identitiesAndFingerprintsAreIndependentOfElementAndObservationOrder() {
        List<PageScanner.DiscoveredElement> login = List.of(input("username"), button("sign-in"));
        List<PageScanner.DiscoveredElement> dashboard = List.of(button("user-menu"), button("open-customers"));
        PageIdentityResolver forward = resolver(ApplicationOverrides.empty());
        PageIdentityResolver.Resolution loginForward = forward.resolve(
                "https://example.test/login", "Login", login, TEST_ATTRIBUTES, REDACTION);
        PageIdentityResolver.Resolution dashboardForward = forward.resolve(
                "https://example.test/dashboard", "Dashboard", dashboard, TEST_ATTRIBUTES, REDACTION);
        PageIdentityResolver reverse = resolver(ApplicationOverrides.empty());
        PageIdentityResolver.Resolution dashboardReverse = reverse.resolve(
                "https://example.test/dashboard", "Dashboard", reversed(dashboard), TEST_ATTRIBUTES, REDACTION);
        PageIdentityResolver.Resolution loginReverse = reverse.resolve(
                "https://example.test/login", "Login", reversed(login), TEST_ATTRIBUTES, REDACTION);

        assertEquals(loginForward.pageId(), loginReverse.pageId());
        assertEquals(loginForward.observationFingerprint(), loginReverse.observationFingerprint());
        assertEquals(dashboardForward.pageId(), dashboardReverse.pageId());
        assertEquals(dashboardForward.observationFingerprint(), dashboardReverse.observationFingerprint());
    }

    @Test
    void distinctPreferredTestAttributesSeparateOtherwiseIndistinguishablePages() {
        PageIdentityResolver resolver = resolver(ApplicationOverrides.empty());
        String structuralHint = "button[3]|role=button|data-component=toolbar";

        PageIdentityResolver.Resolution customers = resolver.resolve(
                "https://example.test/app", "Application",
                List.of(landmark(structuralHint, Map.of("data-testid", "open-customers"))),
                TEST_ATTRIBUTES, REDACTION);
        PageIdentityResolver.Resolution settings = resolver.resolve(
                "https://example.test/app", "Application",
                List.of(landmark(structuralHint, Map.of("data-testid", "open-settings"))),
                TEST_ATTRIBUTES, REDACTION);

        assertNotEquals(customers.pageId(), settings.pageId());
    }

    @Test
    void stableSharedPreferredTestAttributeMergesPageStates() {
        PageIdentityResolver resolver = resolver(ApplicationOverrides.empty());

        PageIdentityResolver.Resolution compact = resolver.resolve(
                "https://example.test/app", "Application",
                List.of(landmark("button[1]|role=button", Map.of("data-testid", "user-menu"))),
                TEST_ATTRIBUTES, REDACTION);
        PageIdentityResolver.Resolution expanded = resolver.resolve(
                "https://example.test/app", "Application",
                List.of(landmark("button[8]|role=button", Map.of("data-testid", "user-menu"))),
                TEST_ATTRIBUTES, REDACTION);

        assertEquals(compact.pageId(), expanded.pageId());
    }

    @Test
    void configuredPreferredAttributeOrderSelectsTheIdentitySignal() {
        PageScanner.DiscoveredElement first = landmark("button[1]|role=button", Map.of(
                "data-testid", "customers-primary", "data-qa", "shared-navigation"));
        PageScanner.DiscoveredElement second = landmark("button[9]|role=button", Map.of(
                "data-testid", "settings-primary", "data-qa", "shared-navigation"));

        PageIdentityResolver qaFirst = resolver(ApplicationOverrides.empty());
        PageIdentityResolver.Resolution qaFirstPage = qaFirst.resolve(
                "https://example.test/app", "Application", List.of(first), List.of("data-qa", "data-testid"), REDACTION);
        PageIdentityResolver.Resolution qaFirstState = qaFirst.resolve(
                "https://example.test/app", "Application", List.of(second), List.of("data-qa", "data-testid"), REDACTION);
        PageIdentityResolver testIdFirst = resolver(ApplicationOverrides.empty());
        PageIdentityResolver.Resolution testIdFirstPage = testIdFirst.resolve(
                "https://example.test/app", "Application", List.of(first), List.of("data-testid", "data-qa"), REDACTION);
        PageIdentityResolver.Resolution testIdFirstOther = testIdFirst.resolve(
                "https://example.test/app", "Application", List.of(second), List.of("data-testid", "data-qa"), REDACTION);

        assertEquals(qaFirstPage.pageId(), qaFirstState.pageId(), "the first configured stable attribute is authoritative");
        assertNotEquals(testIdFirstPage.pageId(), testIdFirstOther.pageId(), "reversing preference must use data-testid instead");
    }

    @Test
    void preferredAttributeValuesAreRedactedBeforeIdentityFingerprinting() {
        RedactionPolicy policy = RedactionPolicy.builder().secret("tenant-secret-id").build();
        PageIdentityResolver secretResolver = resolver(ApplicationOverrides.empty());
        PageIdentityResolver redactedResolver = resolver(ApplicationOverrides.empty());

        PageIdentityResolver.Resolution secret = secretResolver.resolve(
                "https://example.test/app", "Application",
                List.of(landmark("button[1]|role=button", Map.of("data-testid", "tenant-secret-id"))),
                TEST_ATTRIBUTES, policy);
        PageIdentityResolver.Resolution alreadyRedacted = redactedResolver.resolve(
                "https://example.test/app", "Application",
                List.of(landmark("button[1]|role=button", Map.of("data-testid", "[REDACTED]"))),
                TEST_ATTRIBUTES, policy);
        PageIdentityResolver.Resolution publicValue = resolver(ApplicationOverrides.empty()).resolve(
                "https://example.test/app", "Application",
                List.of(landmark("button[1]|role=button", Map.of("data-testid", "public-navigation"))),
                TEST_ATTRIBUTES, policy);

        assertEquals(alreadyRedacted.observationFingerprint(), secret.observationFingerprint());
        assertEquals(alreadyRedacted.pageId(), secret.pageId());
        assertNotEquals(publicValue.observationFingerprint(), secret.observationFingerprint(),
                "redaction must retain a safe distinction between redacted and non-sensitive attribute values");
    }

    @Test
    void sensitivePreferredAttributeKeyUsesKeyAwareRedactionEvenForShortValues() {
        PageIdentityResolver rawResolver = resolver(ApplicationOverrides.empty());
        PageIdentityResolver redactedResolver = resolver(ApplicationOverrides.empty());
        List<String> sensitiveAttribute = List.of("data-token");

        PageIdentityResolver.Resolution raw = rawResolver.resolve(
                "https://example.test/app", "Application",
                List.of(landmark("button[1]|role=button", Map.of("data-token", "abc"))),
                sensitiveAttribute, REDACTION);
        PageIdentityResolver.Resolution explicitlyRedacted = redactedResolver.resolve(
                "https://example.test/app", "Application",
                List.of(landmark("button[1]|role=button", Map.of("data-token", "[REDACTED]"))),
                sensitiveAttribute, REDACTION);

        assertEquals(explicitlyRedacted.observationFingerprint(), raw.observationFingerprint(),
                "sensitive attribute names must invoke RedactionPolicy.redact(key, value)");
        assertEquals(explicitlyRedacted.pageId(), raw.pageId());
    }

    private static PageIdentityResolver resolver(ApplicationOverrides overrides) {
        return new PageIdentityResolver("application-1", overrides);
    }

    private static PageScanner.DiscoveredElement input(String testId) {
        return element("input", "textbox", testId);
    }

    private static PageScanner.DiscoveredElement button(String testId) {
        return element("button", "button", testId);
    }

    private static PageScanner.DiscoveredElement element(String tag, String role, String testId) {
        return new PageScanner.DiscoveredElement(
                null, null, false, tag, "", role, "", "", "", null, false, null,
                tag + "|" + role + "|" + testId, Map.of("data-testid", testId));
    }

    private static PageScanner.DiscoveredElement landmark(String structuralHint, Map<String, String> testAttributes) {
        return new PageScanner.DiscoveredElement(
                null, null, false, "button", "", "button", "", "", "", null, false, null,
                structuralHint, testAttributes);
    }

    private static List<PageScanner.DiscoveredElement> reversed(List<PageScanner.DiscoveredElement> elements) {
        List<PageScanner.DiscoveredElement> copy = new java.util.ArrayList<>(elements);
        java.util.Collections.reverse(copy);
        return copy;
    }
}
