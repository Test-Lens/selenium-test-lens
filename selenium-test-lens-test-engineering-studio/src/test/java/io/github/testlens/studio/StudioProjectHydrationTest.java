package io.github.testlens.studio;

import io.github.testlens.application.model.ApplicationModel;
import io.github.testlens.application.tooling.json.ApplicationModelJson;
import io.github.testlens.application.tooling.json.StrictJson;
import io.github.testlens.studio.workspace.StudioWorkspaceStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StudioProjectHydrationTest {
    @TempDir Path root;

    @Test void restartLoadsFullApplicationModelReindexesSourceAndRecomputesCorrelation() throws Exception {
        Path sourceRoot=root.resolve("src/test/java"),source=sourceRoot.resolve("LoginPage.java");Files.createDirectories(sourceRoot);
        Files.writeString(source,"import org.openqa.selenium.By; final class LoginPage { private final By loginButton = By.id(\"old-login\"); }");
        StudioWorkspaceStore store=new StudioWorkspaceStore(root);ApplicationModel model=model();
        store.write(StudioWorkspaceStore.ArtifactKind.APPLICATION_MODEL,StrictJson.readObject(new ApplicationModelJson().write(model)),"old-source-fingerprint");
        assertTrue(store.read(StudioWorkspaceStore.ArtifactKind.APPLICATION_MODEL).orElseThrow().get("projection") instanceof java.util.Map<?,?> projection&&projection.containsKey("applicationId"));

        var configuration=new TestEngineeringStudioService.Configuration(root,List.of(sourceRoot),List.of(),List.of("src/test/java"),"fixture");
        TestEngineeringStudioService restarted=new TestEngineeringStudioService(configuration,null,null);

        assertEquals(1,restarted.applicationOverview().counts().pages());
        assertEquals(1,restarted.projectOverview().source().files());
        assertEquals(1,restarted.correlations(0,10).total());
        assertEquals("STRONG",restarted.correlations(0,10).items().get(0).state());
        assertTrue(restarted.projectOverview().stage().limitations().stream().noneMatch(item->item.code().equals("WORKSPACE_HYDRATION_FAILED")));
    }

    @Test void corruptPersistedModelFailsClosedWithoutTrustingOldCorrelations() throws Exception {
        StudioWorkspaceStore store=new StudioWorkspaceStore(root);store.write(StudioWorkspaceStore.ArtifactKind.APPLICATION_MODEL,java.util.Map.of("not","a model"),"fp");
        var service=new TestEngineeringStudioService(new TestEngineeringStudioService.Configuration(root,List.of(),List.of(),List.of(),"fixture"),null,null);
        assertEquals(0,service.applicationOverview().counts().pages());
        assertEquals(0,service.correlations(0,10).total());
        assertTrue(service.projectOverview().stage().limitations().stream().anyMatch(item->item.code().equals("WORKSPACE_HYDRATION_FAILED")));
    }

    private static ApplicationModel model(){
        var selector=new ApplicationModel.SelectorProjection("id","old-login","observed-login","VERIFIED_IN_SCOPE","SAME_TARGET",true,List.of("STABLE"),1,List.of(),List.of(),ApplicationModel.EvidenceSource.LIVE_CANDIDATE_ANALYSIS);
        var element=new ApplicationModel.ElementModel("login-button","LoginButton",ApplicationModel.ElementType.BUTTON,"button","Sign in","Sign in",List.of(ApplicationModel.Action.CLICK),selector,List.of(),ApplicationModel.SelectorQuality.VERIFIED,null,"element-fp",provenance(),List.of());
        var page=new ApplicationModel.PageModel("login-page","Login",new ApplicationModel.PageIdentity("/login",null,"Login",List.of("login-button"),"page-fp",ApplicationModel.EvidenceSource.OBSERVED),List.of("Login"),List.of("page-fp"),List.of(),List.of(),List.of(element),List.of(),List.of(),provenance(),List.of());
        return new ApplicationModel(ApplicationModel.SCHEMA_VERSION,"fixture-app","fixture","test",Instant.EPOCH,List.of(page),List.of(),List.of(),new ApplicationModel.Coverage(ApplicationModel.Completeness.COMPLETE_FOR_OBSERVED_CONTEXT,1,1,0,1,1,0,1,0,false),List.of(),provenance());
    }
    private static ApplicationModel.Provenance provenance(){return ApplicationModel.Provenance.observed("fixture","restart-test",List.of("fixture"));}
}
