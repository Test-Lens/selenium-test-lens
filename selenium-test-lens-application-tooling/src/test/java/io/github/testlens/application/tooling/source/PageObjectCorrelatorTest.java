package io.github.testlens.application.tooling.source;

import io.github.testlens.application.model.ApplicationModel;
import io.github.testlens.selector.tooling.ExistingProjectIndex;
import io.github.testlens.selector.tooling.ExistingProjectIndexer;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageObjectCorrelatorTest {
    @Test void correlatesUniqueSelectorWithPageContextAndPreservesEvidence(){
        var result=new PageObjectCorrelator().correlate(model(List.of(element("LOGIN_SUBMIT","loginButton","css","[data-testid='login-submit']"))),
                index(List.of(source("source-login","class-login","loginButton","decl-login","css","[data-testid='login-submit']"))),CorrelationOverrides.none());
        var match=result.elements().get(0);
        assertEquals(PageObjectCorrelation.State.STRONG,match.state());
        assertEquals("decl-login",match.sourceDeclarationRef());
        assertTrue(match.evidence().contains(PageObjectCorrelation.Evidence.NORMALIZED_SELECTOR_MATCH));
        assertTrue(match.evidence().contains(PageObjectCorrelation.Evidence.SAME_PAGE_IDENTITY));
    }
    @Test void keepsSameLookingSelectorsAmbiguousWithoutContext(){
        var sources=List.of(source("save-a","class-a","saveButton","decl-a","css","button.save"),source("save-b","class-b","saveButton","decl-b","css","button.save"));
        var result=new PageObjectCorrelator().correlate(model(List.of(element("SAVE","save","css","button.save"))),index(sources),CorrelationOverrides.none());
        assertEquals(PageObjectCorrelation.State.AMBIGUOUS,result.elements().get(0).state());
        assertEquals(List.of("decl-a","decl-b"),result.elements().get(0).conflicts());
    }
    @Test void stableIdOverrideWinsAndUnknownOverrideIsConflict(){
        var source=source("source-login","class-login","loginButton","decl-login","css","#different");
        var exact=new PageObjectCorrelator().correlate(model(List.of(element("LOGIN_SUBMIT","loginButton","css","#submit"))),index(List.of(source)),
                new CorrelationOverrides(Map.of("class-login","LOGIN"),Map.of("source-login","LOGIN_SUBMIT")));
        assertEquals(PageObjectCorrelation.State.EXACT,exact.elements().get(0).state());
        var conflict=new PageObjectCorrelator().correlate(model(List.of(element("LOGIN_SUBMIT","loginButton","css","#submit"))),index(List.of(source)),
                new CorrelationOverrides(Map.of(),Map.of("missing","LOGIN_SUBMIT")));
        assertEquals(PageObjectCorrelation.State.CONFLICT,conflict.elements().get(0).state());
    }
    private static ApplicationModel model(List<ApplicationModel.ElementModel> elements){
        var provenance=ApplicationModel.Provenance.observed("obs","default",List.of());
        var page=new ApplicationModel.PageModel("LOGIN","LoginPage",new ApplicationModel.PageIdentity("/login","/login","Login",List.of(),"fp",ApplicationModel.EvidenceSource.OBSERVED),List.of(),List.of("fp"),List.of(),List.of(),elements,List.of(),List.of(),provenance,List.of());
        return new ApplicationModel(1,"app","App","test",Instant.EPOCH,List.of(page),List.of(),List.of(),new ApplicationModel.Coverage(ApplicationModel.Completeness.COMPLETE_FOR_OBSERVED_CONTEXT,1,1,0,elements.size(),elements.size(),0,elements.size(),0,false),List.of(),provenance);
    }
    private static ApplicationModel.ElementModel element(String id,String name,String strategy,String value){
        var selector=new ApplicationModel.SelectorProjection(strategy,value,"candidate-"+id,"SAME_TARGET","true",true,List.of("STABLE"),1,List.of(),List.of(),ApplicationModel.EvidenceSource.LIVE_CANDIDATE_ANALYSIS);
        return new ApplicationModel.ElementModel(id,name,ApplicationModel.ElementType.BUTTON,"button",name,name,List.of(ApplicationModel.Action.CLICK),selector,List.of(),ApplicationModel.SelectorQuality.VERIFIED,null,"fp-"+id,ApplicationModel.Provenance.observed("obs","default",List.of()),List.of());
    }
    private static ExistingProjectIndex.ElementEntry source(String id,String owner,String name,String declaration,String strategy,String value){return new ExistingProjectIndex.ElementEntry(id,owner,name,declaration,strategy,new ExistingProjectIndex.ValueProjection("RESOLVED",ExistingProjectIndexer.selectorValueFingerprint(value)),new ExistingProjectIndex.SourceRange(1,1,1,10,0,9));}
    private static ExistingProjectIndex index(List<ExistingProjectIndex.ElementEntry> elements){
        var classes=elements.stream().map(value->value.ownerClassId()).distinct().map(id->new ExistingProjectIndex.ClassEntry(id,"src/"+id+".java",id.equals("class-login")?"example.LoginPage":"example.Other",id.equals("class-login")?"LoginPage":"Other",ExistingProjectIndex.ClassClassification.PAGE_OBJECT,ExistingProjectIndex.Origin.HAND_WRITTEN,List.of())).toList();
        return new ExistingProjectIndex(1,"project",classes,elements,List.of(),List.of(),List.of(),ExistingProjectIndex.Completeness.COMPLETE,List.of(),new ExistingProjectIndex.Metrics(1,classes.size(),elements.size(),classes.size(),elements.size(),0,0,0,classes.size()));
    }
}
