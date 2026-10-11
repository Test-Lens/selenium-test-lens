package io.github.testlens.studio;

import io.github.testlens.application.mapper.ApplicationMapper;
import io.github.testlens.application.mapper.ApplicationMapperOptions;
import io.github.testlens.application.model.ApplicationModel;
import io.github.testlens.application.tooling.ai.ContractHeader;
import io.github.testlens.application.tooling.ai.FailureClassification;
import io.github.testlens.application.tooling.ai.TestExecutionResult;
import io.github.testlens.application.tooling.json.ApplicationModelJson;
import io.github.testlens.application.tooling.json.StrictJson;
import io.github.testlens.selector.tooling.ExistingProjectIndex;
import io.github.testlens.selector.tooling.ExistingProjectIndexer;
import io.github.testlens.studio.browser.Browser;
import io.github.testlens.studio.browser.BrowserRequest;
import io.github.testlens.studio.browser.DefaultLocalBrowserSessionProvider;
import io.github.testlens.studio.browser.Ownership;
import io.github.testlens.studio.browser.Purpose;
import io.github.testlens.studio.workspace.StudioWorkspaceStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Real-DOM certification that indistinguishable replacement controls fail closed. */
class ExternalRepairAmbiguityBrowserIT {
    @TempDir Path project;

    @Test void indistinguishableLiveControlsDoNotProduceTrustedRepair() throws Exception {
        Path sourceRoot=project.resolve("src/test/java");
        Path pageObject=sourceRoot.resolve("benchmark/LoginPage.java");
        Files.createDirectories(pageObject.getParent());
        Files.writeString(pageObject,"""
                package benchmark;
                import org.openqa.selenium.By;
                public final class LoginPage {
                    private static final By LOGIN = By.id("old-login-button");
                }
                """,StandardCharsets.UTF_8);
        String unchanged=Files.readString(pageObject);

        try(RunningFixture fixture=RunningFixture.start()) {
            WebDriver baseline=driver();
            try {
            baseline.get(fixture.uri().toString());
            baseline.findElement(By.id("username")).sendKeys("certification-user");
            baseline.findElement(By.id("password")).sendKeys("invalid-password");
            baseline.findElement(By.id("old-login-button")).click();
            assertTrue(baseline.findElement(By.id("error")).isDisplayed(),
                    "the baseline must exercise the invalid-login assertion");
            assertEquals("false",baseline.findElement(By.id("session-state"))
                    .getAttribute("data-authenticated"));

            baseline.get(fixture.uri().toString());
            ApplicationMapper mapper=ApplicationMapper.start(baseline,
                    ApplicationMapperOptions.builder("Self-healing ambiguity fixture").build());
            mapper.observe();
            ApplicationModel baselineModel=mapper.model();
            ExistingProjectIndex index=new ExistingProjectIndexer().index(
                    new ExistingProjectIndexer.Request(project,List.of(sourceRoot),List.of()));
            StudioWorkspaceStore workspace=StudioWorkspaceStore.atWorkspace(project.resolve(".lens-work"));
            workspace.write(StudioWorkspaceStore.ArtifactKind.APPLICATION_MODEL,
                    StrictJson.readObject(new ApplicationModelJson().write(baselineModel)),
                    index.projectFingerprint());

            fixture.setMode("ambiguous");
            baseline.get(fixture.uri().toString());
            assertTrue(baseline.findElements(By.id("old-login-button")).isEmpty(),
                    "selector drift must be a real Selenium lookup failure");
            List<WebElement> controls=baseline.findElements(By.cssSelector("button.account-action"));
            assertEquals(2,controls.size());
            assertIndistinguishable(controls.get(0),controls.get(1));

            ExternalRepairPipeline pipeline=new ExternalRepairPipeline(project,List.of(sourceRoot),List.of(),
                    new DefaultLocalBrowserSessionProvider(),
                    new BrowserRequest(Purpose.MAPPING,Browser.CHROME,Ownership.STUDIO_OWNED,true,
                            "ambiguity-it-"+UUID.randomUUID()),fixture.uri(),workspace);
            TestExecutionResult execution=missingElementExecution();

            FailureClassification classification=pipeline.classifier().classify(execution);

            assertEquals(FailureClassification.Category.UNKNOWN,classification.category(),
                    "two DOM-equivalent controls must not be promoted to selector instability");
            assertTrue(classification.counterEvidenceRefs().contains("SELECTOR_TARGET_AMBIGUOUS"),
                    "production analysis must complete far enough to identify the ambiguous replacement set");
            assertTrue(pipeline.stabilizer().propose(classification,execution).isEmpty(),
                    "an unresolved target must not create an approvable repair");
            assertEquals(unchanged,Files.readString(pageObject),
                    "analysis must not mutate the Page Object");
            }finally{baseline.quit();}
        }
    }

    private static void assertIndistinguishable(WebElement left,WebElement right){
        for(String attribute:List.of("id","name","class","type","role","aria-label","data-testid","onclick"))
            assertEquals(left.getAttribute(attribute),right.getAttribute(attribute),attribute);
        assertEquals(left.getTagName(),right.getTagName());
        assertEquals(left.getText(),right.getText());
        assertEquals(left.getAccessibleName(),right.getAccessibleName());
        assertEquals(left.getAriaRole(),right.getAriaRole());
        assertEquals(left.findElement(By.xpath("..")).getTagName(),right.findElement(By.xpath("..")).getTagName());
    }

    private static TestExecutionResult missingElementExecution(){
        List<String> evidence=List.of("NoSuchElementException: old-login-button");
        ContractHeader header=new ContractHeader(ContractHeader.SCHEMA_VERSION,ContractHeader.Status.FAILED,
                evidence,List.of(),ContractHeader.Confidence.OBSERVED);
        return new TestExecutionResult(header,"ambiguity-real-dom",TestExecutionResult.Outcome.PASS,
                TestExecutionResult.Outcome.FAIL,"tests=1",1,List.of("compile:pass"),evidence,List.of(),
                evidence,List.of(),evidence,"Targeted test failed");
    }

    private static WebDriver driver(){
        ChromeOptions options=TestEngineeringStudioBrowserIT.chromeOptions();
        return new ChromeDriver(options);
    }

    private static final class RunningFixture implements AutoCloseable {
        private final Process process;
        private final URI uri;

        private RunningFixture(Process process,URI uri){this.process=process;this.uri=uri;}
        static RunningFixture start()throws Exception{
            Path source=Path.of("..","scripts","fixtures","s15-external-consumer","src","test","java",
                    "example","FixtureApplication.java").toAbsolutePath().normalize();
            assertTrue(Files.isRegularFile(source),"certification fixture source must exist: "+source);
            Path classes=Files.createTempDirectory("test-lens-ambiguity-fixture-");
            JavaCompiler compiler=ToolProvider.getSystemJavaCompiler();
            assertNotNull(compiler,"a JDK compiler is required");
            assertEquals(0,compiler.run(null,null,null,"-d",classes.toString(),source.toString()));
            int port;
            try(ServerSocket socket=new ServerSocket(0)){port=socket.getLocalPort();}
            Path java=Path.of(System.getProperty("java.home"),"bin","java.exe");
            Process process=new ProcessBuilder(java.toString(),"-cp",classes.toString(),
                    "example.FixtureApplication",Integer.toString(port)).redirectErrorStream(true).start();
            URI uri=URI.create("http://127.0.0.1:"+port+"/");
            try{
                long deadline=System.nanoTime()+Duration.ofSeconds(15).toNanos();
                while(System.nanoTime()<deadline){
                    if(reachable(uri))return new RunningFixture(process,uri);
                    if(process.waitFor(100,TimeUnit.MILLISECONDS))
                        fail("fixture process exited before accepting requests");
                }
                fail("fixture did not accept requests within 15 seconds");return null;
            }catch(Throwable failure){process.destroyForcibly();process.waitFor(5,TimeUnit.SECONDS);throw failure;}
        }
        URI uri(){return uri;}
        void setMode(String mode)throws Exception{
            HttpURLConnection connection=(HttpURLConnection)uri.resolve(mode).toURL().openConnection();
            connection.setConnectTimeout(5000);connection.setReadTimeout(5000);
            assertEquals(200,connection.getResponseCode());connection.disconnect();
        }
        private static boolean reachable(URI uri){
            try{
                HttpURLConnection connection=(HttpURLConnection)uri.toURL().openConnection();
                connection.setConnectTimeout(250);connection.setReadTimeout(250);
                boolean ready=connection.getResponseCode()==200;connection.disconnect();return ready;
            }catch(Exception ignored){return false;}
        }
        @Override public void close()throws Exception{
            process.destroy();
            if(!process.waitFor(5,TimeUnit.SECONDS)){process.destroyForcibly();process.waitFor(5,TimeUnit.SECONDS);}
        }
    }
}
