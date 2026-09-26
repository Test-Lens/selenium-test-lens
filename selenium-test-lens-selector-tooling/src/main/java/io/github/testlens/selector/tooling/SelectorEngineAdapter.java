package io.github.testlens.selector.tooling;

import io.github.testlens.selector.engine.SelectorSubject;

import java.util.ArrayList;
import java.util.List;

import static io.github.testlens.selector.tooling.SelectorIndexModel.*;

/** Keeps JavaParser/source-index types out of the JDK-only selector engine. */
final class SelectorEngineAdapter {
    private SelectorEngineAdapter() { }

    static List<SelectorSubject> subjects(DeclarationRecord declaration) {
        List<SelectorSubject> subjects = new ArrayList<>();
        if (declaration.resolvedLocator() != null) {
            subjects.add(resolved(declaration, declaration.resolvedLocator()));
        } else if (!declaration.locatorExpression().children().isEmpty()) {
            for (LocatorExpressionChild child : declaration.locatorExpression().children()) {
                if (child.resolvedLocator() != null) subjects.add(resolved(declaration, child.resolvedLocator()));
            }
        } else {
            SelectorSubject.SubjectKind kind = declaration.resolutionStatus() == ResolutionStatus.CUSTOM
                    ? SelectorSubject.SubjectKind.CUSTOM : SelectorSubject.SubjectKind.TEMPLATE;
            subjects.add(new SelectorSubject(kind, strategyFromExpression(declaration.locatorExpression()),
                    SelectorSubject.ValueState.UNAVAILABLE, null, declaration.declarationRef(), module(declaration.logicalPath()),
                    declaration.logicalPath(), symbol(declaration.declaringSymbol()), null, null, null,
                    SelectorSubject.InputTrust.SOURCE_CANONICAL, declaration.locatorExpression().normalizedExpression()));
        }
        return List.copyOf(subjects);
    }

    private static SelectorSubject resolved(DeclarationRecord d, ResolvedLocator locator) {
        return new SelectorSubject(SelectorSubject.SubjectKind.STATIC_DECLARATION, locator.strategy(),
                SelectorSubject.ValueState.KNOWN, locator.value(), d.declarationRef(), module(d.logicalPath()),
                d.logicalPath(), symbol(d.declaringSymbol()), null, null, null,
                SelectorSubject.InputTrust.SOURCE_CANONICAL, d.locatorExpression().normalizedExpression());
    }

    private static String symbol(DeclaringSymbol s) {
        if (s == null) return null;
        return String.join("#", value(s.qualifiedTypeName()), value(s.memberSignature()), value(s.localScopeFingerprint()));
    }
    private static String module(String path) { int slash=path.indexOf('/'); return slash<0?null:path.substring(0,slash); }
    private static String strategyFromExpression(LocatorExpression e) { return e.factorySymbol()==null?"":e.factorySymbol(); }
    private static String value(String value) { return value==null?"":value; }
}
