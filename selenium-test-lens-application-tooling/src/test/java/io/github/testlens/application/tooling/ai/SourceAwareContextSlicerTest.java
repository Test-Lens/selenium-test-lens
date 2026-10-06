package io.github.testlens.application.tooling.ai;

import io.github.testlens.application.tooling.ToolingFixtures;
import io.github.testlens.application.tooling.source.PageObjectCorrelation;
import io.github.testlens.core.redaction.RedactionPolicy;
import io.github.testlens.selector.tooling.ExistingProjectIndex;
import io.github.testlens.selector.tooling.ExistingProjectIndexer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceAwareContextSlicerTest {
    private static final ContractHeader HEADER = new ContractHeader(1, ContractHeader.Status.READY,
            List.of("test"), List.of(), ContractHeader.Confidence.OBSERVED);

    @Test
    void includesOnlyCorrelatedLoginSourceAndNeverProjectsRawSelectorValue() {
        AgentTask task = new AgentTask(HEADER, AgentTask.TaskType.CREATE_TEST, "Test invalid login",
                List.of("login-page"), List.of(), List.of("login"), List.of());

        AgentContextPack pack = new ContextSlicer().slice(ToolingFixtures.model(), task, Map.of(), List.of(),
                RedactionPolicy.defaults(), new ContextSlicer.Limits(1, 20, 10, 0), index(), correlation(),
                ContextSlicer.SourceLimits.defaults());

        assertEquals(List.of("class-login"), pack.source().classes().stream()
                .map(AgentContextPack.PageObjectClassContext::classId).toList());
        assertEquals(List.of("decl-login"), pack.source().declarations().stream()
                .map(AgentContextPack.SourceDeclarationContext::declarationRef).toList());
        assertEquals(List.of("test-login"), pack.source().tests().stream()
                .map(AgentContextPack.ExistingTestContext::testId).toList());
        assertTrue(pack.source().methods().stream().allMatch(method -> method.ownerClassId().equals("class-login")));
        assertFalse(pack.source().classes().stream().anyMatch(value -> value.qualifiedName().contains("Customers")));

        String prompt = new PromptPackRenderer().render(PromptPackRenderer.Role.TEST_IMPLEMENTER, pack, 100_000);
        assertFalse(prompt.contains("[data-super-secret='raw-login-selector']"));
        assertTrue(prompt.contains(ExistingProjectIndexer.selectorValueFingerprint(
                "[data-super-secret='raw-login-selector']")));
    }

    @Test
    void reportsPartialContextWhenSourceMethodBudgetIsReached() {
        AgentTask task = new AgentTask(HEADER, AgentTask.TaskType.CREATE_TEST, "Test invalid login",
                List.of("login-page"), List.of(), List.of("login"), List.of());

        AgentContextPack pack = new ContextSlicer().slice(ToolingFixtures.model(), task, Map.of(), List.of(),
                RedactionPolicy.defaults(), new ContextSlicer.Limits(1, 20, 10, 0), index(), correlation(),
                new ContextSlicer.SourceLimits(2, 1, 2, 2, 100_000));

        assertEquals(1, pack.source().methods().size());
        assertEquals(AgentContextPack.Completeness.PARTIAL, pack.completeness());
        assertEquals(ContractHeader.Status.PARTIAL, pack.header().status());
        assertTrue(pack.excluded().stream().anyMatch(value -> value.kind().equals("PAGE_OBJECT_METHOD")
                && value.reason().equals("maxMethods reached")));
    }

    private static ExistingProjectIndex index() {
        var loginClass = clazz("class-login", "example.LoginPage", "LoginPage");
        var customersClass = clazz("class-customers", "example.CustomersPage", "CustomersPage");
        var loginElement = element("source-login", "class-login", "loginButton", "decl-login",
                "[data-super-secret='raw-login-selector']");
        var customersElement = element("source-customers", "class-customers", "search", "decl-customers",
                "[data-testid='customer-search']");
        var loginMethod = method("method-login", "class-login", "login()", List.of("decl-login"));
        var secondLoginMethod = method("method-submit", "class-login", "submit()", List.of("decl-login"));
        var customersMethod = method("method-search", "class-customers", "search(String)", List.of("decl-customers"));
        var loginTest = new ExistingProjectIndex.TestEntry("test-login", "class-login-test", "test-method-login",
                ExistingProjectIndex.TestFramework.JUNIT5, List.of("login"), List.of(), List.of("login"));
        var customerTest = new ExistingProjectIndex.TestEntry("test-customers", "class-customer-test", "test-method-customers",
                ExistingProjectIndex.TestFramework.JUNIT5, List.of("customers"), List.of(), List.of("search"));
        return new ExistingProjectIndex(1, "project", List.of(loginClass, customersClass),
                List.of(loginElement, customersElement), List.of(loginMethod, secondLoginMethod, customersMethod),
                List.of(loginTest, customerTest), List.of(
                new ExistingProjectIndex.Edge("edge-login-test", ExistingProjectIndex.EdgeType.TEST_TO_METHOD,
                        "test-login", "method-login"),
                new ExistingProjectIndex.Edge("edge-customer-test", ExistingProjectIndex.EdgeType.TEST_TO_METHOD,
                        "test-customers", "method-search")
        ), ExistingProjectIndex.Completeness.COMPLETE, List.of(),
                new ExistingProjectIndex.Metrics(1, 4, 2, 2, 2, 3, 2, 2, 4));
    }

    private static PageObjectCorrelation correlation() {
        return new PageObjectCorrelation(1, List.of(
                new PageObjectCorrelation.ClassCorrelation("login-page", "class-login",
                        PageObjectCorrelation.State.STRONG, List.of(PageObjectCorrelation.Evidence.SAME_PAGE_IDENTITY),
                        List.of(), List.of()),
                new PageObjectCorrelation.ClassCorrelation("dashboard-page", "class-customers",
                        PageObjectCorrelation.State.STRONG, List.of(PageObjectCorrelation.Evidence.SAME_PAGE_IDENTITY),
                        List.of(), List.of())
        ), List.of(
                new PageObjectCorrelation.ElementCorrelation("login-page", "login", "source-login", "decl-login",
                        PageObjectCorrelation.State.STRONG,
                        List.of(PageObjectCorrelation.Evidence.NORMALIZED_SELECTOR_MATCH), List.of(), List.of()),
                new PageObjectCorrelation.ElementCorrelation("dashboard-page", "menu", "source-customers", "decl-customers",
                        PageObjectCorrelation.State.STRONG,
                        List.of(PageObjectCorrelation.Evidence.NORMALIZED_SELECTOR_MATCH), List.of(), List.of())
        ), PageObjectCorrelation.Completeness.COMPLETE, List.of(),
                new PageObjectCorrelation.Metrics(2, 2, 2, 0, 2, 0, 0, 0, 0));
    }

    private static ExistingProjectIndex.ClassEntry clazz(String id, String qualifiedName, String simpleName) {
        return new ExistingProjectIndex.ClassEntry(id, "src/test/java/" + simpleName + ".java", qualifiedName,
                simpleName, ExistingProjectIndex.ClassClassification.PAGE_OBJECT,
                ExistingProjectIndex.Origin.HAND_WRITTEN, List.of());
    }

    private static ExistingProjectIndex.ElementEntry element(String id, String owner, String name,
                                                               String declaration, String rawValue) {
        return new ExistingProjectIndex.ElementEntry(id, owner, name, declaration, "css",
                new ExistingProjectIndex.ValueProjection("RESOLVED",
                        ExistingProjectIndexer.selectorValueFingerprint(rawValue)),
                new ExistingProjectIndex.SourceRange(1, 1, 1, 20, 0, 19));
    }

    private static ExistingProjectIndex.MethodEntry method(String id, String owner, String signature,
                                                            List<String> declarations) {
        return new ExistingProjectIndex.MethodEntry(id, owner, signature.substring(0, signature.indexOf('(')),
                signature, List.of(), "void", List.of("CLICK"), declarations, List.of(),
                ExistingProjectIndex.MethodClassification.ACTION);
    }
}
