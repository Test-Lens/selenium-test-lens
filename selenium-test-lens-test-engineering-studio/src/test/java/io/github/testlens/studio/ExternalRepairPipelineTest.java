package io.github.testlens.studio;

import io.github.testlens.application.tooling.ai.ContractHeader;
import io.github.testlens.application.tooling.ai.FailureClassification;
import io.github.testlens.application.tooling.ai.TestExecutionResult;
import io.github.testlens.application.model.ApplicationModel;
import io.github.testlens.application.tooling.json.ApplicationModelJson;
import io.github.testlens.application.tooling.json.StrictJson;
import io.github.testlens.selector.tooling.ExistingProjectIndexer;
import io.github.testlens.studio.browser.*;
import io.github.testlens.studio.project.ProjectDiscovery;
import io.github.testlens.studio.project.ProjectDescriptor;
import io.github.testlens.studio.workspace.StudioWorkspaceStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.junit.jupiter.api.Assertions.*;

class ExternalRepairPipelineTest {
    @TempDir Path project;

    @Test void productMismatchNeverStartsSelectorAnalysisOrOffersRepair(){
        AtomicBoolean browserOpened=new AtomicBoolean();
        BrowserSessionProvider browsers=new BrowserSessionProvider(){
            @Override public BrowserAvailability preflight(BrowserRequest request){return BrowserAvailability.AVAILABLE;}
            @Override public BrowserSession open(BrowserRequest request){browserOpened.set(true);throw new AssertionError("selector analysis must not run");}
        };
        BrowserRequest mapping=BrowserRequest.studioOwned(Purpose.MAPPING,Browser.CHROME);
        ExternalRepairPipeline pipeline=new ExternalRepairPipeline(project,List.of(project),List.of(),browsers,mapping,null);
        TestExecutionResult execution=failure(List.of("AssertionFailedError: expected true but was false"));

        FailureClassification classification=pipeline.classifier().classify(execution);

        assertEquals(FailureClassification.Category.PRODUCT_DEFECT,classification.category());
        assertFalse(browserOpened.get());
        assertTrue(pipeline.stabilizer().propose(classification,execution).isEmpty());
    }

    @Test void missingElementWithoutCorrelatedLiveEvidenceRemainsUnknown(){
        BrowserSessionProvider unavailable=new BrowserSessionProvider(){
            @Override public BrowserAvailability preflight(BrowserRequest request){return BrowserAvailability.NOT_AVAILABLE;}
            @Override public BrowserSession open(BrowserRequest request){throw new IllegalStateException("not available");}
        };
        ExternalRepairPipeline pipeline=new ExternalRepairPipeline(project,List.of(project),List.of(),unavailable,
                BrowserRequest.studioOwned(Purpose.MAPPING,Browser.CHROME),null);
        TestExecutionResult execution=failure(List.of("NoSuchElementException: old selector"));

        FailureClassification classification=pipeline.classifier().classify(execution);

        assertEquals(FailureClassification.Category.UNKNOWN,classification.category(),
                "exception type alone must not imply selector instability");
        assertTrue(pipeline.stabilizer().propose(classification,execution).isEmpty());
    }

    @Test void studioPipelineReadsConfiguredWorkspaceAndIgnoresConflictingDefault() throws Exception {
        var descriptor=configuredProject();
        var index=new ExistingProjectIndexer().index(new ExistingProjectIndexer.Request(
                project,descriptor.sourceRoots(),descriptor.classpathEntries()));
        StudioWorkspaceStore.atWorkspace(descriptor.workspaceDirectory()).write(
                StudioWorkspaceStore.ArtifactKind.APPLICATION_MODEL,
                StrictJson.readObject(new ApplicationModelJson().write(emptyModel())),
                index.projectFingerprint());
        Path wrong=project.resolve(".test-lens/project/application-model.json");
        Files.createDirectories(wrong.getParent());Files.writeString(wrong,"{not-json");
        AtomicBoolean browserOpened=new AtomicBoolean();
        BrowserSessionProvider marker=new BrowserSessionProvider(){
            @Override public BrowserAvailability preflight(BrowserRequest request){return BrowserAvailability.AVAILABLE;}
            @Override public BrowserSession open(BrowserRequest request){browserOpened.set(true);throw new IllegalStateException("CUSTOM_WORKSPACE_MODEL_LOADED");}
        };
        ExternalRepairPipeline pipeline=TestEngineeringStudio.externalRepairPipeline(descriptor,
                descriptor.classpathEntries(),marker,BrowserRequest.studioOwned(Purpose.MAPPING,Browser.CHROME));

        FailureClassification classification=pipeline.classifier().classify(
                failure(List.of("NoSuchElementException: old-login")));

        assertEquals(FailureClassification.Category.UNKNOWN,classification.category());
        assertTrue(browserOpened.get(),"valid configured model must reach live analysis");
        assertTrue(classification.counterEvidenceRefs().stream().anyMatch(value->value.contains("CUSTOM_WORKSPACE_MODEL_LOADED")));
    }

    @Test void configuredWorkspaceMissingDoesNotFallBackToDefaultModel() throws Exception {
        var descriptor=configuredProject();
        var index=new ExistingProjectIndexer().index(new ExistingProjectIndexer.Request(
                project,descriptor.sourceRoots(),descriptor.classpathEntries()));
        new StudioWorkspaceStore(project).write(StudioWorkspaceStore.ArtifactKind.APPLICATION_MODEL,
                StrictJson.readObject(new ApplicationModelJson().write(emptyModel())),index.projectFingerprint());
        AtomicBoolean browserOpened=new AtomicBoolean();
        BrowserSessionProvider marker=new BrowserSessionProvider(){
            @Override public BrowserAvailability preflight(BrowserRequest request){return BrowserAvailability.AVAILABLE;}
            @Override public BrowserSession open(BrowserRequest request){browserOpened.set(true);throw new IllegalStateException("DEFAULT_MODEL_WAS_USED");}
        };
        ExternalRepairPipeline pipeline=TestEngineeringStudio.externalRepairPipeline(descriptor,
                descriptor.classpathEntries(),marker,BrowserRequest.studioOwned(Purpose.MAPPING,Browser.CHROME));

        FailureClassification classification=pipeline.classifier().classify(
                failure(List.of("NoSuchElementException: old-login")));

        assertFalse(browserOpened.get(),"configured workspace must not fall back to .test-lens");
        assertTrue(classification.counterEvidenceRefs().stream().anyMatch(value->value.contains("MODEL_MISSING")));
    }

    @Test void concreteMissingElementEvidenceIsNotHiddenByAssertionWrapper() throws Exception {
        var descriptor=configuredProject();
        var index=new ExistingProjectIndexer().index(new ExistingProjectIndexer.Request(
                project,descriptor.sourceRoots(),descriptor.classpathEntries()));
        StudioWorkspaceStore.atWorkspace(descriptor.workspaceDirectory()).write(
                StudioWorkspaceStore.ArtifactKind.APPLICATION_MODEL,
                StrictJson.readObject(new ApplicationModelJson().write(baselineModel())),index.projectFingerprint());
        BrowserSessionProvider browsers=new BrowserSessionProvider(){
            @Override public BrowserAvailability preflight(BrowserRequest request){return BrowserAvailability.AVAILABLE;}
            @Override public BrowserSession open(BrowserRequest request){return new BrowserSession(changedApplicationDriver(),Ownership.STUDIO_OWNED);}
        };
        ExternalRepairPipeline pipeline=TestEngineeringStudio.externalRepairPipeline(descriptor,
                descriptor.classpathEntries(),browsers,BrowserRequest.studioOwned(Purpose.MAPPING,Browser.CHROME));
        TestExecutionResult execution=failure(List.of(
                "AssertionFailedError: interaction failed",
                "NoSuchElementException: old-login"));

        FailureClassification classification=pipeline.classifier().classify(execution);

        assertEquals(FailureClassification.Category.SELECTOR_INSTABILITY,classification.category());
        assertTrue(pipeline.stabilizer().propose(classification,execution).isPresent());
        assertFalse(Files.exists(project.resolve(".test-lens/project/application-model.json")),
                "repair analysis must not create default workspace data");
    }

    @Test void semanticallyEquivalentLiveCandidatesRemainAmbiguousAndFailClosed() throws Exception {
        var descriptor=configuredProject();
        var index=new ExistingProjectIndexer().index(new ExistingProjectIndexer.Request(
                project,descriptor.sourceRoots(),descriptor.classpathEntries()));
        StudioWorkspaceStore.atWorkspace(descriptor.workspaceDirectory()).write(
                StudioWorkspaceStore.ArtifactKind.APPLICATION_MODEL,
                StrictJson.readObject(new ApplicationModelJson().write(baselineModel())),index.projectFingerprint());
        BrowserSessionProvider browsers=new BrowserSessionProvider(){
            @Override public BrowserAvailability preflight(BrowserRequest request){return BrowserAvailability.AVAILABLE;}
            @Override public BrowserSession open(BrowserRequest request){return new BrowserSession(ambiguousApplicationDriver(),Ownership.STUDIO_OWNED);}
        };
        ExternalRepairPipeline pipeline=TestEngineeringStudio.externalRepairPipeline(descriptor,
                descriptor.classpathEntries(),browsers,BrowserRequest.studioOwned(Purpose.MAPPING,Browser.CHROME));
        TestExecutionResult execution=failure(List.of("NoSuchElementException: old-login"));

        FailureClassification classification=pipeline.classifier().classify(execution);

        assertEquals(FailureClassification.Category.UNKNOWN,classification.category(),
                "two equally plausible live targets must not be promoted to selector instability");
        assertTrue(pipeline.stabilizer().propose(classification,execution).isEmpty(),
                "an ambiguous target must never produce an applicable repair proposal");
    }

    @Test void staleAndUnreadableConfiguredModelsHaveDistinctDiagnostics() throws Exception {
        var descriptor=configuredProject();StudioWorkspaceStore custom=StudioWorkspaceStore.atWorkspace(descriptor.workspaceDirectory());
        custom.write(StudioWorkspaceStore.ArtifactKind.APPLICATION_MODEL,
                StrictJson.readObject(new ApplicationModelJson().write(emptyModel())),"stale-fingerprint");
        BrowserSessionProvider unused=new BrowserSessionProvider(){
            @Override public BrowserAvailability preflight(BrowserRequest request){return BrowserAvailability.AVAILABLE;}
            @Override public BrowserSession open(BrowserRequest request){throw new AssertionError("stale model must fail before browser open");}
        };
        ExternalRepairPipeline stale=TestEngineeringStudio.externalRepairPipeline(descriptor,
                descriptor.classpathEntries(),unused,BrowserRequest.studioOwned(Purpose.MAPPING,Browser.CHROME));
        var staleResult=stale.classifier().classify(failure(List.of("NoSuchElementException: old-login")));
        assertTrue(staleResult.counterEvidenceRefs().stream().anyMatch(value->value.contains("MODEL_STALE")));

        Files.writeString(descriptor.workspaceDirectory().resolve("project/application-model.json"),"{not-json");
        ExternalRepairPipeline unreadable=TestEngineeringStudio.externalRepairPipeline(descriptor,
                descriptor.classpathEntries(),unused,BrowserRequest.studioOwned(Purpose.MAPPING,Browser.CHROME));
        var unreadableResult=unreadable.classifier().classify(failure(List.of("NoSuchElementException: old-login")));
        assertTrue(unreadableResult.counterEvidenceRefs().stream().anyMatch(value->value.contains("MODEL_READ_FAILED")));
    }

    private ProjectDescriptor configuredProject() throws Exception {
        Path source=project.resolve("src/test/java");Files.createDirectories(source);
        Files.writeString(source.resolve("LoginPage.java"),"import org.openqa.selenium.By; final class LoginPage { private final By loginButton = By.id(\"old-login\"); }");
        Files.writeString(project.resolve("pom.xml"),"<project><modelVersion>4.0.0</modelVersion><artifactId>fixture</artifactId></project>");
        Files.createDirectories(project.resolve(".test-lens"));
        Files.writeString(project.resolve(".test-lens/project.json"),"{\"schemaVersion\":1,\"sourceRoots\":[\"src/test/java\"],\"workspaceDirectory\":\".lens-work\"}");
        return new ProjectDiscovery().discover(project);
    }

    private static ApplicationModel emptyModel(){
        return new ApplicationModel(ApplicationModel.SCHEMA_VERSION,"fixture","fixture","test",Instant.EPOCH,
                List.of(),List.of(),List.of(),new ApplicationModel.Coverage(
                ApplicationModel.Completeness.COMPLETE_FOR_OBSERVED_CONTEXT,0,0,0,0,0,0,0,0,false),
                List.of(),ApplicationModel.Provenance.observed("fixture","workspace-test",List.of("fixture")));
    }

    private static ApplicationModel baselineModel(){
        var selector=new ApplicationModel.SelectorProjection("id","old-login","candidate-old","VERIFIED_IN_SCOPE","SAME_TARGET",true,List.of("STABLE"),1,List.of(),List.of(),ApplicationModel.EvidenceSource.LIVE_CANDIDATE_ANALYSIS);
        var element=new ApplicationModel.ElementModel("login-button","LoginButton",ApplicationModel.ElementType.BUTTON,"button","Sign in","Sign in",List.of(ApplicationModel.Action.CLICK),selector,List.of(),ApplicationModel.SelectorQuality.VERIFIED,null,"element-fp",ApplicationModel.Provenance.observed("fixture","workspace-test",List.of("fixture")),List.of());
        var page=new ApplicationModel.PageModel("login-page","Login",new ApplicationModel.PageIdentity("/login",null,"Login",List.of("login-button"),"page-fp",ApplicationModel.EvidenceSource.OBSERVED),List.of("Login"),List.of("page-fp"),List.of(),List.of(),List.of(element),List.of(),List.of(),ApplicationModel.Provenance.observed("fixture","workspace-test",List.of("fixture")),List.of());
        return new ApplicationModel(ApplicationModel.SCHEMA_VERSION,"fixture","fixture","test",Instant.EPOCH,List.of(page),List.of(),List.of(),new ApplicationModel.Coverage(ApplicationModel.Completeness.COMPLETE_FOR_OBSERVED_CONTEXT,1,1,0,1,1,0,1,0,false),List.of(),ApplicationModel.Provenance.observed("fixture","workspace-test",List.of("fixture")));
    }

    private static WebDriver changedApplicationDriver(){
        WebElement target=(WebElement)Proxy.newProxyInstance(ExternalRepairPipelineTest.class.getClassLoader(),new Class<?>[]{WebElement.class},(proxy,method,args)->switch(method.getName()){
            case "getText","getAccessibleName"->"Sign in";case "getAriaRole"->"button";case "getAttribute"->null;case "equals"->proxy==args[0];case "hashCode"->System.identityHashCode(proxy);case "toString"->"login-button";default->defaultValue(method.getReturnType());});
        return (WebDriver)Proxy.newProxyInstance(ExternalRepairPipelineTest.class.getClassLoader(),new Class<?>[]{WebDriver.class,JavascriptExecutor.class},(proxy,method,args)->switch(method.getName()){
            case "getCurrentUrl"->"https://example.test/login";case "getTitle"->"Login";case "getWindowHandle"->"window-1";
            case "findElements"->String.valueOf(args[0]).contains("old-login")?List.of():List.of(target);case "findElement"->target;
            case "executeScript"->mappingScript((String)args[0],target);default->defaultValue(method.getReturnType());});
    }

    private static WebDriver ambiguousApplicationDriver(){
        WebElement primary=element("login-primary");
        WebElement secondary=element("login-secondary");
        return (WebDriver)Proxy.newProxyInstance(ExternalRepairPipelineTest.class.getClassLoader(),
                new Class<?>[]{WebDriver.class,JavascriptExecutor.class},(proxy,method,args)->switch(method.getName()){
            case "getCurrentUrl"->"https://example.test/login";case "getTitle"->"Login";case "getWindowHandle"->"window-1";
            case "findElements"->{
                String selector=String.valueOf(args[0]);
                if(selector.contains("old-login"))yield List.of();
                if(selector.contains("login-primary"))yield List.of(primary);
                if(selector.contains("login-secondary"))yield List.of(secondary);
                yield List.of(primary,secondary);
            }
            case "findElement"->primary;
            case "executeScript"->ambiguousMappingScript((String)args[0],scriptTarget(args,primary),primary,secondary);
            default->defaultValue(method.getReturnType());
        });
    }

    private static WebElement element(String testId){
        return (WebElement)Proxy.newProxyInstance(ExternalRepairPipelineTest.class.getClassLoader(),
                new Class<?>[]{WebElement.class},(proxy,method,args)->switch(method.getName()){
            case "getText","getAccessibleName"->"Sign in";
            case "getAriaRole"->"button";
            case "getAttribute"->"data-testid".equals(args[0])?testId:null;
            case "equals"->proxy==args[0];case "hashCode"->System.identityHashCode(proxy);
            case "toString"->testId;default->defaultValue(method.getReturnType());
        });
    }

    private static WebElement scriptTarget(Object[] invocationArguments,WebElement fallback){
        if(invocationArguments!=null&&invocationArguments.length>1){
            if(invocationArguments[1] instanceof WebElement element)return element;
            if(invocationArguments[1] instanceof Object[] parameters&&parameters.length>0
                    &&parameters[0] instanceof WebElement element)return element;
        }
        return fallback;
    }

    private static Object ambiguousMappingScript(String script,WebElement target,WebElement primary,WebElement secondary){
        if(script.contains("const max=arguments[0]"))return Map.of(
                "elements",List.of(mappedButton(primary,"login-primary","main|button|primary"),
                        mappedButton(secondary,"login-secondary","main|button|secondary")),
                "regions",List.of(),"discovered",2,"truncated",false,"closedShadowPossible",0,
                "shadowRoots",0,"shadowDepthReached",0);
        String testId=target==secondary?"login-secondary":"login-primary";
        Map<String,Object> snapshot=new LinkedHashMap<>();snapshot.put("tagName","button");snapshot.put("id",null);
        snapshot.put("name",null);snapshot.put("classList",List.of("primary"));
        snapshot.put("testAttributes",Map.of("data-testid",testId));snapshot.put("ariaLabel","Sign in");
        snapshot.put("ancestors",List.of());snapshot.put("ownedNodes",List.of());
        snapshot.put("instrumentationNodesExcluded",0);snapshot.put("truncated",false);return snapshot;
    }

    private static Map<String,Object> mappedButton(WebElement element,String testId,String structuralHint){
        return Map.ofEntries(Map.entry("element",element),Map.entry("shadow",false),Map.entry("tag","button"),
                Map.entry("type","button"),Map.entry("role","button"),Map.entry("label","Sign in"),
                Map.entry("accessibleName","Sign in"),Map.entry("text","Sign in"),Map.entry("disabled",false),
                Map.entry("regionKey",""),Map.entry("structuralHint",structuralHint),
                Map.entry("testAttributes",Map.of("data-testid",testId)));
    }

    private static Object mappingScript(String script,WebElement target){
        if(script.contains("const max=arguments[0]"))return Map.of("elements",List.of(Map.ofEntries(Map.entry("element",target),Map.entry("shadow",false),Map.entry("tag","button"),Map.entry("type","button"),Map.entry("role","button"),Map.entry("label","Sign in"),Map.entry("accessibleName","Sign in"),Map.entry("text","Sign in"),Map.entry("disabled",false),Map.entry("regionKey",""),Map.entry("structuralHint","main|button|login"),Map.entry("testAttributes",Map.of("data-testid","login-submit")))),"regions",List.of(),"discovered",1,"truncated",false,"closedShadowPossible",0,"shadowRoots",0,"shadowDepthReached",0);
        Map<String,Object> snapshot=new LinkedHashMap<>();snapshot.put("tagName","button");snapshot.put("id",null);snapshot.put("name",null);snapshot.put("classList",List.of("primary"));snapshot.put("testAttributes",Map.of("data-testid","login-submit"));snapshot.put("ariaLabel","Sign in");snapshot.put("ancestors",List.of());snapshot.put("ownedNodes",List.of());snapshot.put("instrumentationNodesExcluded",0);snapshot.put("truncated",false);return snapshot;
    }

    private static Object defaultValue(Class<?> type){if(!type.isPrimitive())return null;if(type==boolean.class)return false;if(type==char.class)return '\0';if(type==byte.class)return(byte)0;if(type==short.class)return(short)0;if(type==int.class)return 0;if(type==long.class)return 0L;if(type==float.class)return 0F;if(type==double.class)return 0D;return null;}

    private static TestExecutionResult failure(List<String> evidence){
        ContractHeader header=new ContractHeader(ContractHeader.SCHEMA_VERSION,ContractHeader.Status.FAILED,
                evidence,List.of(),ContractHeader.Confidence.OBSERVED);
        return new TestExecutionResult(header,"scenario",TestExecutionResult.Outcome.PASS,
                TestExecutionResult.Outcome.FAIL,"tests=1",1,List.of("compile:pass"),evidence,List.of(),
                evidence,List.of(),evidence,"Targeted test failed");
    }
}
