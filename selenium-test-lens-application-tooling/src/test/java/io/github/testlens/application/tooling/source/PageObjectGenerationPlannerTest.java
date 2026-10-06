package io.github.testlens.application.tooling.source;

import io.github.testlens.selector.tooling.ExistingProjectIndex;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageObjectGenerationPlannerTest {
    @Test void reusesOneStrongExistingClassInsteadOfCreatingLoginPageTwo(){
        var clazz=new ExistingProjectIndex.ClassEntry("class-login","src/LoginPage.java","sample.LoginPage","LoginPage",ExistingProjectIndex.ClassClassification.PAGE_OBJECT,ExistingProjectIndex.Origin.HAND_WRITTEN,List.of());
        var index=new ExistingProjectIndex(1,"fp",List.of(clazz),List.of(),List.of(),List.of(),List.of(),ExistingProjectIndex.Completeness.COMPLETE,List.of(),new ExistingProjectIndex.Metrics(1,1,0,1,0,0,0,0,1));
        var correlation=new PageObjectCorrelation(1,List.of(new PageObjectCorrelation.ClassCorrelation("LOGIN","class-login",PageObjectCorrelation.State.STRONG,List.of(PageObjectCorrelation.Evidence.SAME_PAGE_IDENTITY),List.of(),List.of())),List.of(),PageObjectCorrelation.Completeness.COMPLETE,List.of(),new PageObjectCorrelation.Metrics(1,0,0,0,0,0,0,0,0));
        var decision=new PageObjectGenerationPlanner().decide("LOGIN",correlation,index);
        assertEquals(PageObjectGenerationPlanner.Action.REUSE_EXISTING,decision.action());
        assertEquals("sample.LoginPage",decision.qualifiedName());
    }

    @Test void ignoresAComponentWhenChoosingWhetherToGenerateAPageObject(){
        var component=new ExistingProjectIndex.ClassEntry("class-nav","src/TopNavigation.java","sample.TopNavigation","TopNavigation",ExistingProjectIndex.ClassClassification.COMPONENT,ExistingProjectIndex.Origin.HAND_WRITTEN,List.of());
        var index=new ExistingProjectIndex(1,"fp",List.of(component),List.of(),List.of(),List.of(),List.of(),ExistingProjectIndex.Completeness.COMPLETE,List.of(),new ExistingProjectIndex.Metrics(1,1,0,1,0,0,0,0,1));
        var correlation=new PageObjectCorrelation(1,List.of(new PageObjectCorrelation.ClassCorrelation("DASHBOARD","class-nav",PageObjectCorrelation.State.STRONG,List.of(PageObjectCorrelation.Evidence.SAME_PAGE_IDENTITY),List.of(),List.of())),List.of(),PageObjectCorrelation.Completeness.COMPLETE,List.of(),new PageObjectCorrelation.Metrics(1,0,0,0,0,0,0,0,0));
        assertEquals(PageObjectGenerationPlanner.Action.GENERATE,
                new PageObjectGenerationPlanner().decide("DASHBOARD",correlation,index).action());
    }
}
