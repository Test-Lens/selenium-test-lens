package io.github.testlens.studio;

import com.sun.net.httpserver.HttpServer;
import io.github.testlens.TestLens;
import io.github.testlens.application.tooling.ai.*;
import io.github.testlens.application.tooling.ai.workflow.*;
import io.github.testlens.application.mapper.ApplicationMapper;
import io.github.testlens.application.mapper.ApplicationMapperOptions;
import io.github.testlens.application.model.ApplicationModel;
import io.github.testlens.application.tooling.source.*;
import io.github.testlens.application.tooling.codegen.SelectorJavaExpression;
import io.github.testlens.core.trace.TraceStatus;
import io.github.testlens.core.trace.UiTestLensSession;
import io.github.testlens.selector.tooling.*;
import io.github.testlens.studio.transport.TestEngineeringStudioServer;
import io.github.testlens.studio.launcher.StudioLauncherService;
import io.github.testlens.studio.project.ProjectDiscovery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.*;

import static org.junit.jupiter.api.Assertions.*;

/** Real browser certification of the Studio shell over source indexing, mapping and S12 execution. */
class TestEngineeringStudioBrowserIT {
    private static final String PAGE_OBJECT = "src/test/java/example/LoginPage.java";
    private static final String GENERATED_BINARY = "generated.InvalidPasswordTest";

    @TempDir Path project;

    @Test void projectToReviewedPlanToRealFailureTrustedRepairAndVerifiedRerun() throws Exception {
        writeExistingProject();
        Path pageObjectClasses = project.resolve("target/page-object-classes");
        compilePageObject(pageObjectClasses);
        try (FixtureApplication app = new FixtureApplication()) {
            WebDriver target = driver(), studio = driver();
            try {
                target.get(app.uri());
                ApplicationMapper repairMapper = ApplicationMapper.start(target,
                        ApplicationMapperOptions.builder("Studio repair fixture").build());
                repairMapper.observe();
                ApplicationModel baselineModel = repairMapper.model();
                ExistingProjectIndex baselineIndex = indexProject();
                PageObjectCorrelation baselineCorrelation = new PageObjectCorrelator().correlate(
                        baselineModel, baselineIndex, CorrelationOverrides.none());
                ApplicationModel.ElementModel before = loginButton(baselineModel);
                PageObjectCorrelation.ElementCorrelation source = baselineCorrelation.elements().stream()
                        .filter(value -> value.applicationElementId().equals(before.elementId()))
                        .filter(value -> value.sourceElementId() != null)
                        .findFirst().orElseThrow();
                SourceImpact impact = new SourceImpactAnalyzer().analyze(
                        before.elementId(), baselineCorrelation, baselineIndex, 100);

                TargetedJavaCompiler.CompilationResult baselineGenerated = compileGenerated(pageObjectClasses);
                assertTrue(baselineGenerated.successful(), baselineGenerated.diagnostics().toString());
                WorkflowExecution baselineExecution = executeGenerated(target, app.uri(), pageObjectClasses,
                        baselineGenerated.output(), "studio generated baseline");
                assertTrue(baselineExecution.passed());
                assertNull(baselineExecution.failure());
                assertEquals(TraceStatus.PASSED, baselineExecution.session().metadata().status());
                assertTrue(baselineExecution.session().events().size() >= 3,
                        "generated test must execute the functional Page Object through observed Selenium");

                AtomicReference<ApplicationModel.ElementModel> after = new AtomicReference<>();
                AtomicReference<Throwable> workflowFailure = new AtomicReference<>();
                AtomicReference<FailureClassification> workflowClassification = new AtomicReference<>();
                AtomicReference<RepairProposal> workflowProposal = new AtomicReference<>();
                AtomicInteger workflowExecutions = new AtomicInteger();
                ScriptedAgentExecutor agents = scripted(plan(), implementation(), review());
                String fixtureClasspath=pageObjectClasses+System.getProperty("path.separator")+System.getProperty("java.class.path");
                ReviewableCoordinatorWorkflowGateway gateway = new ReviewableCoordinatorWorkflowGateway(agents,
                        replay -> coordinator(replay, target, app.uri(), pageObjectClasses, before, after,
                                source, impact, baselineIndex, workflowFailure, workflowClassification,
                                workflowProposal, workflowExecutions),
                        new GeneratedTestPolicyValidator(GeneratedTestPolicyValidator.Policy.defaults()),
                        ignored->Path.of("src/test/java/generated/InvalidPasswordTest.java"),
                        new TargetedRepairSourceCompiler(project,17,fixtureClasspath));
                Files.writeString(project.resolve("pom.xml"),"<project><modelVersion>4.0.0</modelVersion><artifactId>fixture-app</artifactId></project>");
                var descriptor=new ProjectDiscovery().discover(project);
                StudioLauncherService launcher=new StudioLauncherService(gateway,()->target);
                try (var launched=launcher.launch(descriptor,new StudioLauncherService.LaunchOptions(false))) {
                    TestEngineeringStudioService service=launched.service();studio.get(launched.uri().toString());
                    assertRealDomNavigation(studio);
                    click(studio, "Scan project"); awaitText(studio, "2 files scanned"); screenshot(studio, "project-overview");
                    click(studio, "Start mapping"); awaitText(studio, "pages mapped");
                    click(studio, "Correlate"); awaitText(studio, "elements correlated");

                    click(studio, "Requirements");
                    WebElement requirement = studio.findElement(By.id("requirement-input"));
                    requirement.sendKeys("Invalid password should display an error message");
                    click(studio, "Generate plan"); awaitText(studio, "Invalid password"); screenshot(studio, "requirement-plan");
                    click(studio, "Generate test"); awaitText(studio, "Proposed test");
                    assertEquals(io.github.testlens.studio.projection.StageStatus.PASS,service.workflow(null).implementation().stage().status(),service.workflow(null).implementation().policyResults().toString());
                    assertFalse(implementation().sourcePatch().contains("By."));
                    assertFalse(implementation().sourcePatch().contains("findElement"));
                    assertTrue(implementation().sourcePatch().contains("new LoginPage(driver).invalidPassword"));

                    app.changed.set(true);
                    target.get(app.uri());
                    repairMapper.observe();
                    after.set(loginButton(repairMapper.model()));
                    assertEquals(before.elementId(), after.get().elementId());
                    assertEquals(ApplicationModel.EvidenceSource.LIVE_CANDIDATE_ANALYSIS,
                            after.get().preferredSelector().source());

                    Path pageSource = project.resolve(PAGE_OBJECT);
                    String unchanged = Files.readString(pageSource);
                    click(studio,"Run");
                    new WebDriverWait(studio,Duration.ofSeconds(30)).until(
                            ignored->workflowExecutions.get()==1);
                    new WebDriverWait(studio,Duration.ofSeconds(30)).until(ignored->!service.operationRunning());
                    assertEquals(1,workflowExecutions.get(),studio.findElement(By.tagName("body")).getText());
                    assertInstanceOf(org.openqa.selenium.NoSuchElementException.class,workflowFailure.get(),studio.findElement(By.tagName("body")).getText());
                    new WebDriverWait(studio,Duration.ofSeconds(30)).until(ignored->workflowClassification.get()!=null);
                    assertEquals(FailureClassification.Category.SELECTOR_INSTABILITY,workflowClassification.get().category(),studio.findElement(By.tagName("body")).getText());
                    new WebDriverWait(studio,Duration.ofSeconds(30)).until(ignored->workflowProposal.get()!=null);
                    assertNotNull(workflowProposal.get(),studio.findElement(By.tagName("body")).getText());
                    awaitText(studio,"SELECTOR_INSTABILITY");
                    assertEquals(1, workflowExecutions.get());
                    assertInstanceOf(org.openqa.selenium.NoSuchElementException.class, workflowFailure.get());
                    assertEquals(FailureClassification.Category.SELECTOR_INSTABILITY,
                            workflowClassification.get().category());
                    assertEquals(RepairProposal.ApplicationPolicy.PROPOSE_ONLY,
                            workflowProposal.get().applicationPolicy());
                    assertEquals(PAGE_OBJECT, workflowProposal.get().sourceTarget().logicalPath());
                    assertEquals(1, service.repairHistory().size(),
                            "workflow must register the coordinator proposal without a test shortcut");

                    click(studio, "Proposed"); awaitText(studio, "Proposed repair"); screenshot(studio, "diagnosis-repair");
                    assertEquals(unchanged, Files.readString(pageSource), "proposal display must not mutate source");
                    assertTrue(studio.findElement(By.tagName("body")).getText().contains("old-login-button"));
                    assertTrue(studio.findElement(By.tagName("body")).getText().contains("login-submit"));
                    click(studio, "Approve and apply");
                    new WebDriverWait(studio, Duration.ofSeconds(20)).until(ignored -> {
                        try { return !Files.readString(pageSource).equals(unchanged); }
                        catch (Exception failure) { return false; }
                    });
                    String repaired = Files.readString(pageSource);
                    assertTrue(repaired.contains("login-submit"));
                    assertSurgicalSelectorReplacement(unchanged, repaired, workflowProposal.get());

                    awaitText(studio, "Run verification"); click(studio, "Run verification");
                    new WebDriverWait(studio, Duration.ofSeconds(30)).until(ignored ->
                            service.workflowHistory().stream().anyMatch(run -> "SUCCESS".equals(run.state())));
                    assertEquals(2, workflowExecutions.get(), "verification must rerun the exact generated test");
                    assertEquals(0, agents.remaining());
                    assertTrue(service.repairHistory().stream().anyMatch(repair ->
                            "APPROVED".equals(repair.decision()) && "APPLIED".equals(repair.applyStatus())));
                    click(studio, "Runs"); awaitText(studio, "PASS"); screenshot(studio, "verified-repair");
                }
            } finally { studio.quit(); target.quit(); }
        }
    }

    private AgentWorkflowCoordinator coordinator(AgentExecutor executor,WebDriver target,String url,Path pageObjectClasses,
            ApplicationModel.ElementModel before,AtomicReference<ApplicationModel.ElementModel> after,
            PageObjectCorrelation.ElementCorrelation source,SourceImpact impact,ExistingProjectIndex baselineIndex,
            AtomicReference<Throwable> workflowFailure,AtomicReference<FailureClassification> workflowClassification,
            AtomicReference<RepairProposal> workflowProposal,AtomicInteger workflowExecutions) {
        compilePageObject(pageObjectClasses);
        String fixtureClasspath=pageObjectClasses+System.getProperty("path.separator")+System.getProperty("java.class.path");
        return new AgentWorkflowCoordinator(new TestEngineeringWorkflow(WorkflowPolicy.defaults()), executor,
                new TargetedJavaCompiler(), (request, output) -> {
                    workflowExecutions.incrementAndGet();
                    WorkflowExecution result=executeGenerated(target,url,pageObjectClasses,output,"studio workflow");
                    workflowFailure.set(result.failure());
                    List<String> evidence=new ArrayList<>();
                    evidence.add("trace:studio-workflow-"+result.session().id());
                    if(result.failure()!=null)evidence.add("exception:"+result.failure().getClass().getName());
                    return new TargetedTestExecutor.ExecutionResult(result.passed(),1,evidence);
                }, new GeneratedTestPolicyValidator(GeneratedTestPolicyValidator.Policy.defaults()),
                execution -> {Throwable observed=workflowFailure.get();FailureClassification.Category category=
                        observed instanceof org.openqa.selenium.NoSuchElementException
                                ? FailureClassification.Category.SELECTOR_INSTABILITY
                                : FailureClassification.Category.UNKNOWN;
                    FailureClassification classification=new FailureClassification(ready(),category,
                        category==FailureClassification.Category.SELECTOR_INSTABILITY
                                ? "The correlated hand-written selector no longer resolves the mapped target"
                                : "The browser failure did not provide selector-instability evidence",
                        execution.traceEvidenceRefs(),List.of(),List.of(before.elementId()));workflowClassification.set(classification);return classification;},
                (classification,execution)->{
                    if(classification.category()!=FailureClassification.Category.SELECTOR_INSTABILITY||after.get()==null)return Optional.empty();
                    RepairProposal proposal=new SelectorRepairPlanner().propose("studio-repair-login",classification,before,after.get(),source,impact,baselineIndex,
                            "trace:studio-real-failure","mapping:changed-login");workflowProposal.set(proposal);return Optional.of(proposal);},
                new AgentWorkflowCoordinator.Configuration(Path.of("src/test/java/generated/InvalidPasswordTest.java"),
                        GENERATED_BINARY, null, 17, fixtureClasspath, 10,
                        Duration.ofSeconds(15), List.of("generated.InvalidPasswordTest"), 2));
    }

    private void writeExistingProject() throws Exception {
        Path source = project.resolve("src/test/java/example"); Files.createDirectories(source);
        Files.writeString(source.resolve("LoginPage.java"), """
                package example;
                import org.openqa.selenium.By;
                public final class LoginPage {
                  private final org.openqa.selenium.WebDriver driver;
                  private final By username = By.id("username");
                  private final By password = By.id("password");
                  private final By loginButton = By.id("old-login-button");
                  public LoginPage(org.openqa.selenium.WebDriver driver) { this.driver = driver; }
                  public boolean invalidPassword(String user, String passwordValue) {
                    driver.findElement(username).sendKeys(user);
                    driver.findElement(password).sendKeys(passwordValue);
                    driver.findElement(loginButton).click();
                    return driver.findElement(By.id("login-error")).isDisplayed();
                  }
                }
                """);
        Files.writeString(source.resolve("InvalidLoginTest.java"), """
                package example;
                import org.junit.jupiter.api.Test;
                final class InvalidLoginTest { @Test void invalidPassword() { } }
                """);
    }

    private static TestPlan plan() {
        var scenario = new TestPlan.TestScenario("invalid-password", "Invalid password", TestPlan.Priority.HIGH,
                List.of("Login page available"), List.of(new TestPlan.TestStep(1,"Submit invalid credentials",null,null,"INVALID_PASSWORD")),
                List.of(new TestPlan.ExpectedResult(null,null,"Error message visible","lens-trace")),
                List.of(),List.of(),List.of(),List.of("INVALID_PASSWORD"),List.of(),
                TestPlan.CoverageDisposition.EXTEND_EXISTING_TEST,List.of("InvalidLoginTest"),List.of());
        return new TestPlan(ready(), List.of(scenario));
    }
    private static TestImplementationProposal implementation() {
        return new TestImplementationProposal(ready(), "invalid-password",
                "package generated; import example.LoginPage; import org.openqa.selenium.WebDriver; public final class InvalidPasswordTest { public static boolean execute(WebDriver driver) { return new LoginPage(driver).invalidPassword(\"demo\", \"invalid\"); } }",
                TestImplementationProposal.SelectorAccessPolicy.PAGE_OBJECTS_ONLY,List.of("LoginPage.invalidPassword"),List.of());
    }
    private static CodeReviewResult review() { return new CodeReviewResult(ready(), CodeReviewResult.Verdict.APPROVE,List.of("implementation"),List.of()); }
    private static ContractHeader ready() { return new ContractHeader(ContractHeader.SCHEMA_VERSION,ContractHeader.Status.READY,List.of("real fixture"),List.of(),ContractHeader.Confidence.OBSERVED); }
    private static ScriptedAgentExecutor scripted(Object... values) { List<AgentExecutor.AgentResult> results=new ArrayList<>();for(int i=0;i<values.length;i++)results.add(new AgentExecutor.AgentResult("scripted-"+i,ArtifactEnvelope.create("artifact-"+i,"studio",List.of(),1,values[i])));return new ScriptedAgentExecutor(results); }

    private void compilePageObject(Path classes) {
        try {
            Files.createDirectories(classes);
            JavaCompiler compiler=ToolProvider.getSystemJavaCompiler();assertNotNull(compiler,"JDK compiler is required");
            int exit=compiler.run(null,null,null,"-classpath",System.getProperty("java.class.path"),"-d",classes.toString(),project.resolve(PAGE_OBJECT).toString());
            assertEquals(0,exit,"hand-written Page Object must compile");
        } catch(IOException failure){throw new AssertionError(failure);}
    }
    private TargetedJavaCompiler.CompilationResult compileGenerated(Path classes){
        String source=implementation().sourcePatch();Path path=Path.of("src/test/java/generated/InvalidPasswordTest.java");
        return new TargetedJavaCompiler().compile(new TargetedJavaCompiler.CompilationRequest(
                List.of(new TargetedJavaCompiler.SourceUnit(path,GENERATED_BINARY,source,ArtifactEnvelope.digest(""))),17,
                classes+System.getProperty("path.separator")+System.getProperty("java.class.path"),20),Map.of());
    }
    private WorkflowExecution executeGenerated(WebDriver raw,String url,Path classes,TargetedJavaCompiler.CompiledOutput output,String sessionName)throws Exception{
        raw.get(url);TestLens lens=TestLens.attach(raw);WebDriver observed=lens.observeDriver();UiTestLensSession session=lens.startSession(sessionName);
        Throwable failure=null;boolean passed=false;
        try(URLClassLoader pageObjects=new URLClassLoader(new java.net.URL[]{classes.toUri().toURL()},getClass().getClassLoader())){
            Class<?> test=output.loadClass(GENERATED_BINARY,pageObjects);Object result=test.getMethod("execute",WebDriver.class).invoke(null,observed);passed=Boolean.TRUE.equals(result);
            if(passed)lens.finishPassed();else{failure=new AssertionError("Login error was not visible");lens.finishFailed(failure);}
        }catch(InvocationTargetException invocation){failure=invocation.getCause();lens.finishFailed(failure);}
        return new WorkflowExecution(passed,failure,session);
    }
    private static void assertSurgicalSelectorReplacement(String original,String repaired,RepairProposal proposal){
        RepairProposal.SourceRange range=proposal.sourceTarget().range();String replacement=SelectorJavaExpression.byExpression(proposal.replacementEvidence().strategy(),proposal.replacementEvidence().value());
        String old=SelectorJavaExpression.byExpression(proposal.sourceTarget().oldStrategy(),proposal.sourceTarget().oldValue());int start=original.indexOf(old,range.startOffset());assertTrue(start>=range.startOffset()&&start<range.endOffsetExclusive());
        assertEquals(original.substring(0,start)+replacement+original.substring(start+old.length()),repaired,"trusted apply must change only the indexed selector expression");
    }
    private record WorkflowExecution(boolean passed,Throwable failure,UiTestLensSession session){}

    private static void click(WebDriver driver,String text) { new WebDriverWait(driver,Duration.ofSeconds(20)).until(d -> d.findElements(By.xpath("//button[normalize-space()="+quote(text)+"]")).stream().filter(WebElement::isEnabled).findFirst().orElse(null)).click(); }
    private static void assertRealDomNavigation(WebDriver driver) {
        List<WebElement> views = driver.findElements(By.cssSelector("nav [data-view]"));
        assertEquals(9, views.size(), "the complete Studio navigation must be present in the real DOM");
        WebElement previous = null;
        for (WebElement view : views) {
            view.click();
            assertEquals("page", view.getAttribute("aria-current"));
            assertEquals(1, driver.findElements(By.cssSelector("nav [data-view][aria-current='page']")).size(),
                    "exactly one real DOM navigation item must be current");
            if (previous != null) assertEquals("false", previous.getAttribute("aria-current"));
            previous = view;
        }
        WebElement back = views.get(views.size() - 2);
        back.click();
        assertEquals("page", back.getAttribute("aria-current"), "back navigation must select the prior view");
        assertEquals("false", previous.getAttribute("aria-current"));
        views.get(0).click();
        assertEquals("page", views.get(0).getAttribute("aria-current"), "rerender must retain the selected view");
    }
    private static void awaitText(WebDriver driver,String text) { new WebDriverWait(driver,Duration.ofSeconds(30)).until(d -> d.findElement(By.tagName("body")).getText().contains(text)); }
    private static String quote(String value) { return "'"+value.replace("'","")+"'"; }
    private static void screenshot(WebDriver driver,String name) throws Exception { Path out=Path.of("target","studio-screenshots",name+"-"+System.getProperty("studio.browser","chrome")+".png");Files.createDirectories(out.getParent());Files.write(out,((TakesScreenshot)driver).getScreenshotAs(OutputType.BYTES)); }
    private static WebDriver driver() {
        if ("firefox".equalsIgnoreCase(System.getProperty("studio.browser", "chrome"))) {
            return new FirefoxDriver(firefoxOptions());
        }
        return new ChromeDriver(chromeOptions());
    }

    static ChromeOptions chromeOptions() {
        ChromeOptions options = new ChromeOptions();
        String configuredBinary = System.getProperty("test.chrome.binary", "").trim();
        if (!configuredBinary.isEmpty()) options.setBinary(configuredBinary);
        return options.addArguments("--headless=new", "--window-size=1440,1000", "--disable-gpu", "--no-sandbox");
    }

    private static FirefoxOptions firefoxOptions() {
        FirefoxOptions options = new FirefoxOptions();
        String configuredBinary = System.getProperty("test.firefox.binary", "").trim();
        if (!configuredBinary.isEmpty()) options.setBinary(configuredBinary);
        return options.addArguments("-headless").addArguments("--width=1440", "--height=1000");
    }
    private ExistingProjectIndex indexProject(){return new ExistingProjectIndexer().index(new ExistingProjectIndexer.Request(project,List.of(project.resolve("src/test/java")),List.of()));}
    private static ApplicationModel.ElementModel loginButton(ApplicationModel model){List<ApplicationModel.ElementModel> elements=model.pages().stream().flatMap(page->page.elements().stream()).toList();return elements.stream().filter(element->element.preferredSelector()!=null&&(element.preferredSelector().value().contains("old-login-button")||element.preferredSelector().value().contains("login-submit"))).findFirst().orElseThrow(()->new AssertionError("Login button missing: "+elements.stream().map(value->value.semanticName()+"="+(value.preferredSelector()==null?"none":value.preferredSelector().value())).toList()));}

    private static final class FixtureApplication implements AutoCloseable {
        private final HttpServer server;
        private final AtomicBoolean changed=new AtomicBoolean();
        FixtureApplication() throws Exception { server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);server.createContext("/",exchange->{String button=changed.get()?"<button id='login-submit' onclick=\"document.getElementById('login-error').hidden=false\">Sign in</button>":"<button id='old-login-button' onclick=\"document.getElementById('login-error').hidden=false\">Sign in</button>";byte[] body=("""
                <!doctype html><html><head><title>Login</title></head><body><main aria-label="Login">
                <label>Username <input id="username"></label><label>Password <input id="password" type="password"></label>
                """+button+"""
                <p id="login-error" role="alert" hidden>Invalid username or password</p></main></body></html>
                """).getBytes(StandardCharsets.UTF_8);exchange.getResponseHeaders().set("Content-Type","text/html; charset=utf-8");exchange.sendResponseHeaders(200,body.length);exchange.getResponseBody().write(body);exchange.close();});server.start(); }
        String uri(){return "http://127.0.0.1:"+server.getAddress().getPort()+"/";}
        @Override public void close(){server.stop(0);}
    }
}
