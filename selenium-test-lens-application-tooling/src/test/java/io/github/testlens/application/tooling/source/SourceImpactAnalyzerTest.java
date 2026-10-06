package io.github.testlens.application.tooling.source;

import io.github.testlens.selector.tooling.ExistingProjectIndex;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SourceImpactAnalyzerTest {
    @Test
    void followsTransitiveMethodUsageToAffectedTests() {
        ExistingProjectIndex index = new ExistingProjectIndex(1, "project", List.of(), List.of(), List.of(
                method("method-login", List.of("decl-login")),
                method("method-workflow", List.of()),
                method("method-helper", List.of())
        ), List.of(
                test("test-login", "test-method-login"),
                test("test-unrelated", "test-method-unrelated")
        ), List.of(
                edge("workflow-login", ExistingProjectIndex.EdgeType.METHOD_TO_METHOD, "method-workflow", "method-login"),
                edge("helper-workflow", ExistingProjectIndex.EdgeType.METHOD_TO_METHOD, "method-helper", "method-workflow"),
                edge("test-login-helper", ExistingProjectIndex.EdgeType.TEST_TO_METHOD, "test-login", "method-helper"),
                edge("test-unrelated", ExistingProjectIndex.EdgeType.TEST_TO_METHOD, "test-unrelated", "other-method")
        ), ExistingProjectIndex.Completeness.COMPLETE, List.of(),
                new ExistingProjectIndex.Metrics(1, 1, 1, 0, 0, 3, 2, 4, 1));
        PageObjectCorrelation correlation = new PageObjectCorrelation(1, List.of(), List.of(
                new PageObjectCorrelation.ElementCorrelation("LOGIN", "LOGIN_SUBMIT", "source-login", "decl-login",
                        PageObjectCorrelation.State.STRONG,
                        List.of(PageObjectCorrelation.Evidence.NORMALIZED_SELECTOR_MATCH), List.of(), List.of())
        ), PageObjectCorrelation.Completeness.COMPLETE, List.of(),
                new PageObjectCorrelation.Metrics(1, 1, 1, 0, 1, 0, 0, 0, 0));

        SourceImpact impact = new SourceImpactAnalyzer().analyze("LOGIN_SUBMIT", correlation, index, 20);

        assertEquals(List.of("decl-login"), impact.sourceDeclarationRefs());
        assertEquals(List.of("method-helper", "method-login", "method-workflow"), impact.methodIds());
        assertEquals(List.of("test-login"), impact.testIds());
        assertEquals(List.of(), impact.limitations());
    }

    @Test
    void excludesProbableCorrelationsFromSourceImpact() {
        ExistingProjectIndex index = new ExistingProjectIndex(1, "project", List.of(), List.of(),
                List.of(method("method-login", List.of("decl-login"))), List.of(), List.of(),
                ExistingProjectIndex.Completeness.COMPLETE, List.of(),
                new ExistingProjectIndex.Metrics(1, 1, 1, 0, 0, 1, 0, 0, 1));
        PageObjectCorrelation correlation = new PageObjectCorrelation(1, List.of(), List.of(
                new PageObjectCorrelation.ElementCorrelation("LOGIN", "LOGIN_SUBMIT", "source-login", "decl-login",
                        PageObjectCorrelation.State.PROBABLE,
                        List.of(PageObjectCorrelation.Evidence.NORMALIZED_SELECTOR_MATCH), List.of(), List.of())
        ), PageObjectCorrelation.Completeness.COMPLETE, List.of(),
                new PageObjectCorrelation.Metrics(1, 1, 0, 0, 0, 1, 0, 0, 0));

        SourceImpact impact = new SourceImpactAnalyzer().analyze("LOGIN_SUBMIT", correlation, index, 20);

        assertEquals(List.of(), impact.sourceDeclarationRefs());
        assertEquals(List.of(), impact.methodIds());
        assertEquals(List.of(), impact.testIds());
    }

    private static ExistingProjectIndex.MethodEntry method(String id, List<String> declarations) {
        return new ExistingProjectIndex.MethodEntry(id, "class-login", id, id + "()", List.of(), "void",
                List.of(), declarations, List.of(), ExistingProjectIndex.MethodClassification.ACTION);
    }

    private static ExistingProjectIndex.TestEntry test(String id, String methodId) {
        return new ExistingProjectIndex.TestEntry(id, "class-test", methodId,
                ExistingProjectIndex.TestFramework.JUNIT5, List.of(), List.of(), List.of());
    }

    private static ExistingProjectIndex.Edge edge(String id, ExistingProjectIndex.EdgeType type,
                                                   String from, String to) {
        return new ExistingProjectIndex.Edge(id, type, from, to);
    }
}
