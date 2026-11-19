package io.github.testlens.application.tooling.ai.workflow;

import io.github.testlens.application.tooling.ai.ContractHeader;
import io.github.testlens.application.tooling.ai.TestImplementationProposal;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReviewerEvidenceTest {
    private static final String LOGIN_PAGE_OWNER = "example.GeneratedTest.LoginPage";

    @Test void verifiesEveryRequiredApiInsideJunitAssertionValue() {
        String source = source("assertTrue(page.first()); assertFalse(page.second());");
        assertEquals(ReviewerEvidence.RequiredApiAssertionStatus.VERIFIED,
                analyze(source, List.of(LOGIN_PAGE_OWNER + "#first()", LOGIN_PAGE_OWNER + "#second()"))
                        .requiredApiInsideAssertion());
    }

    @Test void oneAssertedApiDoesNotVerifyMultipleRequiredApis() {
        String source = source("assertTrue(page.first()); page.second();");
        assertEquals(ReviewerEvidence.RequiredApiAssertionStatus.NOT_VERIFIED,
                analyze(source, List.of(LOGIN_PAGE_OWNER + "#first()", LOGIN_PAGE_OWNER + "#second()"))
                        .requiredApiInsideAssertion());
    }

    @Test void customAssertPrefixedHelperIsNotJunitEvidence() {
        String source = source("assertWhatever(page.first());").replace("  @Test", "  void assertWhatever(boolean value) {}\n  @Test");
        var evidence = analyze(source, List.of(LOGIN_PAGE_OWNER + "#first()"));
        assertEquals(0, evidence.assertionCount());
        assertEquals(ReviewerEvidence.RequiredApiAssertionStatus.NOT_VERIFIED,
                evidence.requiredApiInsideAssertion());
    }

    @Test void messageSupplierDoesNotVerifyRequiredApi() {
        String source = source("assertTrue(true, () -> String.valueOf(page.first()));");
        assertEquals(ReviewerEvidence.RequiredApiAssertionStatus.NOT_VERIFIED,
                analyze(source, List.of(LOGIN_PAGE_OWNER + "#first()"))
                        .requiredApiInsideAssertion());
    }

    @Test void sameNameHelperCannotStandInForPageObjectReceiver() {
        String source = source("page.first(); assertTrue(first());")
                .replace("  @Test", "  boolean first() { return true; }\n  @Test");
        assertEquals(ReviewerEvidence.RequiredApiAssertionStatus.NOT_VERIFIED,
                analyze(source, List.of(LOGIN_PAGE_OWNER + "#first()"))
                        .requiredApiInsideAssertion());
    }

    @Test void localAssertTrueShadowsStaticJunitImport() {
        String source = source("assertTrue(page.first());")
                .replace("  @Test", "  void assertTrue(boolean value) {}\n  @Test");
        var evidence = analyze(source, List.of(LOGIN_PAGE_OWNER + "#first()"));
        assertEquals(0, evidence.assertionCount());
        assertEquals(ReviewerEvidence.RequiredApiAssertionStatus.NOT_VERIFIED,
                evidence.requiredApiInsideAssertion());
    }

    @Test void sameSimpleClassNameFromDifferentOwnerCannotSatisfyRequiredApi() {
        String source = source("page.first(); assertTrue(new Helper.LoginPage().first());")
                .replace("  @Test", """
                          static final class Helper {
                            static final class LoginPage { boolean first() { return true; } }
                          }
                          @Test""");

        assertEquals(ReviewerEvidence.RequiredApiAssertionStatus.NOT_VERIFIED,
                analyze(source, List.of(LOGIN_PAGE_OWNER + "#first()"))
                        .requiredApiInsideAssertion());
    }

    @Test void lowercaseNestedTypeCannotMasqueradeAsQualifiedExternalOwner() {
        String source = source("page.first(); assertTrue(new benchmark.LoginPage().first());")
                .replace("  @Test", """
                          static final class benchmark {
                            static final class LoginPage { boolean first() { return true; } }
                          }
                          @Test""");

        assertEquals(ReviewerEvidence.RequiredApiAssertionStatus.NOT_VERIFIED,
                analyze(source, List.of("benchmark.LoginPage#first()"))
                        .requiredApiInsideAssertion());
    }

    private static ReviewerEvidence.QualityEvidence analyze(String source, List<String> references) {
        return ReviewerEvidence.analyze(new TestImplementationProposal(ready(), "scenario", source,
                TestImplementationProposal.SelectorAccessPolicy.PAGE_OBJECTS_ONLY, references, List.of()), "fp");
    }

    private static String source(String body) {
        return """
                package example;
                import org.junit.jupiter.api.Test;
                import static org.junit.jupiter.api.Assertions.assertFalse;
                import static org.junit.jupiter.api.Assertions.assertTrue;
                final class GeneratedTest {
                  static final class LoginPage {
                    boolean first() { return true; }
                    boolean second() { return false; }
                  }
                  private final LoginPage page = new LoginPage();
                  @Test void verifiesRequirement() { %s }
                }
                """.formatted(body);
    }

    private static ContractHeader ready() {
        return new ContractHeader(ContractHeader.SCHEMA_VERSION, ContractHeader.Status.READY,
                List.of("fixture"), List.of(), ContractHeader.Confidence.OBSERVED);
    }
}
