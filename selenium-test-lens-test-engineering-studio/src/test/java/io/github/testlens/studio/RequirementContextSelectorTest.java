package io.github.testlens.studio;

import io.github.testlens.application.model.ApplicationModel;
import io.github.testlens.application.tooling.source.PageObjectCorrelation;
import io.github.testlens.selector.tooling.ExistingProjectIndex;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RequirementContextSelectorTest {
    @Test void selectsRequirementRelevantPageInsteadOfFirstCanonicalPage() {
        ApplicationModel.PageModel customers=page("a-customers","Customers",element("customer-search","CustomerSearch"));
        ApplicationModel.PageModel login=page("z-login","Login",element("password-input","PasswordInput"));
        ApplicationModel app=new ApplicationModel(ApplicationModel.SCHEMA_VERSION,"app","fixture","test",Instant.EPOCH,
                List.of(customers,login),List.of(),List.of(),new ApplicationModel.Coverage(ApplicationModel.Completeness.COMPLETE_FOR_OBSERVED_CONTEXT,1,2,0,2,2,0,0,0,false),List.of(),provenance());
        ExistingProjectIndex source=new ExistingProjectIndex(ExistingProjectIndex.SCHEMA_VERSION,"fp",List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),
                ExistingProjectIndex.Completeness.COMPLETE,List.of(),new ExistingProjectIndex.Metrics(1,0,0,0,0,0,0,0,0));
        PageObjectCorrelation correlation=new PageObjectCorrelation(PageObjectCorrelation.SCHEMA_VERSION,List.of(),List.of(),PageObjectCorrelation.Completeness.COMPLETE,List.of(),new PageObjectCorrelation.Metrics(2,2,0,0,0,0,0,2,0));
        var selected=new RequirementContextSelector().select("Invalid password should display an error",app,source,correlation);
        assertEquals(List.of("z-login"),selected.pageIds());
        assertEquals(List.of("password-input"),selected.elementIds());
    }
    @Test void doesNotTreatMethodNameSubstringAsAnExactCapabilityMention() {
        ApplicationModel.PageModel login=page("login","Login",List.of(
                element("open-button","Open"),element("reopen-button","Reopen")));
        ApplicationModel app=new ApplicationModel(ApplicationModel.SCHEMA_VERSION,"app","fixture","test",Instant.EPOCH,
                List.of(login),List.of(),List.of(),new ApplicationModel.Coverage(ApplicationModel.Completeness.COMPLETE_FOR_OBSERVED_CONTEXT,1,1,0,2,2,0,0,0,false),List.of(),provenance());
        var owner=new ExistingProjectIndex.ClassEntry("class-login","src/test/java/LoginPage.java","sample.LoginPage","LoginPage",
                ExistingProjectIndex.ClassClassification.PAGE_OBJECT,ExistingProjectIndex.Origin.HAND_WRITTEN,List.of());
        var openElement=new ExistingProjectIndex.ElementEntry("source-open",owner.id(),"open","decl-open","id",
                new ExistingProjectIndex.ValueProjection("RESOLVED","fp-open"),null);
        var reopenElement=new ExistingProjectIndex.ElementEntry("source-reopen",owner.id(),"reopen","decl-reopen","id",
                new ExistingProjectIndex.ValueProjection("RESOLVED","fp-reopen"),null);
        var openMethod=new ExistingProjectIndex.MethodEntry("method-open",owner.id(),"open","open()",List.of(),"void",
                List.of(),List.of("decl-open"),List.of(),ExistingProjectIndex.MethodClassification.ACTION);
        ExistingProjectIndex source=new ExistingProjectIndex(ExistingProjectIndex.SCHEMA_VERSION,"fp",List.of(owner),
                List.of(openElement,reopenElement),List.of(openMethod),List.of(),List.of(),ExistingProjectIndex.Completeness.COMPLETE,
                List.of(),new ExistingProjectIndex.Metrics(1,1,2,1,2,1,0,0,1));
        PageObjectCorrelation correlation=new PageObjectCorrelation(PageObjectCorrelation.SCHEMA_VERSION,
                List.of(new PageObjectCorrelation.ClassCorrelation("login",owner.id(),PageObjectCorrelation.State.EXACT,List.of(),List.of(),List.of())),
                List.of(new PageObjectCorrelation.ElementCorrelation("login","open-button",openElement.id(),"decl-open",PageObjectCorrelation.State.EXACT,List.of(),List.of(),List.of()),
                        new PageObjectCorrelation.ElementCorrelation("login","reopen-button",reopenElement.id(),"decl-reopen",PageObjectCorrelation.State.EXACT,List.of(),List.of(),List.of())),
                PageObjectCorrelation.Completeness.COMPLETE,List.of(),new PageObjectCorrelation.Metrics(1,1,1,0,2,0,0,0,0));

        var selected=new RequirementContextSelector().select("Reopen LoginPage",app,source,correlation);
        assertEquals(List.of("reopen-button"),selected.elementIds());
    }
    private static ApplicationModel.PageModel page(String id,String name,ApplicationModel.ElementModel element){return new ApplicationModel.PageModel(id,name,
            new ApplicationModel.PageIdentity("/"+name.toLowerCase(),null,null,List.of(),"structure",ApplicationModel.EvidenceSource.OBSERVED),List.of(),List.of(),List.of(),List.of(),List.of(element),List.of(),List.of(),provenance(),List.of());}
    private static ApplicationModel.PageModel page(String id,String name,List<ApplicationModel.ElementModel>elements){return new ApplicationModel.PageModel(id,name,
            new ApplicationModel.PageIdentity("/"+name.toLowerCase(),null,null,List.of(),"structure",ApplicationModel.EvidenceSource.OBSERVED),List.of(),List.of(),List.of(),List.of(),elements,List.of(),List.of(),provenance(),List.of());}
    private static ApplicationModel.ElementModel element(String id,String name){return new ApplicationModel.ElementModel(id,name,ApplicationModel.ElementType.INPUT,"textbox",name,name,List.of(ApplicationModel.Action.TYPE),null,List.of(),ApplicationModel.SelectorQuality.UNAVAILABLE,null,"fingerprint-"+id,provenance(),List.of());}
    private static ApplicationModel.Provenance provenance(){return ApplicationModel.Provenance.observed("fixture","test",List.of("fixture"));}
}
