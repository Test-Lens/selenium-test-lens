package io.github.testlens.selector.tooling;

import io.github.testlens.selector.engine.SelectorSubject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.github.testlens.selector.tooling.SelectorIndexModel.*;
import static org.junit.jupiter.api.Assertions.*;

class SelectorEngineAdapterTest {
    @Test void resolvedDynamicCustomAndCompositeDeclarationsAdaptWithoutJavaParserTypes(){
        DeclarationRecord resolved=declaration(ResolutionStatus.RESOLVED,new ResolvedLocator("id","save"),new LocatorExpression(ExpressionKind.SINGLE,"By.id(\"save\")",List.of(),List.of()));
        SelectorSubject subject=SelectorEngineAdapter.subjects(resolved).get(0);assertEquals("id",subject.strategy());assertEquals("save",subject.canonicalValue());assertEquals(SelectorSubject.InputTrust.SOURCE_CANONICAL,subject.inputTrust());
        DeclarationRecord dynamic=declaration(ResolutionStatus.DYNAMIC,null,new LocatorExpression(ExpressionKind.TEMPLATE,"By.id(\"row-\" + rowId)",List.of("rowId"),List.of()));
        assertEquals(SelectorSubject.SubjectKind.TEMPLATE,SelectorEngineAdapter.subjects(dynamic).get(0).kind());assertEquals(SelectorSubject.ValueState.UNAVAILABLE,SelectorEngineAdapter.subjects(dynamic).get(0).valueState());
        DeclarationRecord custom=declaration(ResolutionStatus.CUSTOM,null,new LocatorExpression(ExpressionKind.CUSTOM,"Locators.byTestId(\"save\")",List.of(),List.of()));
        assertEquals(SelectorSubject.SubjectKind.CUSTOM,SelectorEngineAdapter.subjects(custom).get(0).kind());
        LocatorExpressionChild first=new LocatorExpressionChild(new LocatorExpression(ExpressionKind.SINGLE,"id",List.of(),List.of()),new ResolvedLocator("id","a"),ResolutionStatus.RESOLVED);
        LocatorExpressionChild second=new LocatorExpressionChild(new LocatorExpression(ExpressionKind.SINGLE,"css",List.of(),List.of()),new ResolvedLocator("css selector",".b"),ResolutionStatus.RESOLVED);
        DeclarationRecord composite=declaration(ResolutionStatus.RESOLVED,null,new LocatorExpression(ExpressionKind.ALTERNATIVES,"FindAll",List.of(),List.of(first,second)));
        assertEquals(2,SelectorEngineAdapter.subjects(composite).size());
    }

    private static DeclarationRecord declaration(ResolutionStatus status,ResolvedLocator locator,LocatorExpression expression){return new DeclarationRecord(1,"java-decl-v1:sha256:"+"a".repeat(64),"java","module/src/test/A.java",new SourceRange(1,1,1,5,0,4),new DeclaringSymbol("METHOD","A","test()",null),DeclarationKind.BY_LOCAL,expression,locator,status,false,false,"sha256:"+"b".repeat(64));}
}
