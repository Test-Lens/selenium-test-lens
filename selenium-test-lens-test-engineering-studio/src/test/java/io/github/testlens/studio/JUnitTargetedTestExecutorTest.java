package io.github.testlens.studio;

import io.github.testlens.application.tooling.ai.workflow.ArtifactEnvelope;
import io.github.testlens.application.tooling.ai.workflow.TargetedJavaCompiler;
import io.github.testlens.application.tooling.ai.workflow.TargetedTestExecutor;
import io.github.testlens.studio.browser.Browser;
import io.github.testlens.studio.browser.BrowserRequest;
import io.github.testlens.studio.browser.Ownership;
import io.github.testlens.studio.browser.Purpose;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JUnitTargetedTestExecutorTest {
    @TempDir Path temporaryDirectory;

    @Test void normalPassAndBrowserCleanup() throws Exception {
        String profile=profile("pass");TargetedTestExecutor.ExecutionResult result=executor(profile).execute(request("Fast",Duration.ofSeconds(5)),compiled("Fast","@org.junit.jupiter.api.Test void test() {}"));
        assertEquals(TargetedTestExecutor.Status.PASS,result.status());assertEquals(1,result.tests());
        assertEquals(List.of("OPEN","CLOSE"),Files.readAllLines(ForkedTestBrowserSessionProvider.events(profile)));
    }

    @Test void assertionAndOrdinarySeleniumFailuresRemainFail() throws Exception {
        TargetedTestExecutor.ExecutionResult assertion=executor(profile("assert")).execute(request("Assertion",Duration.ofSeconds(5)),compiled("Assertion","@org.junit.jupiter.api.Test void test() { throw new AssertionError(\"wrong product result\"); }"));
        assertEquals(TargetedTestExecutor.Status.FAIL,assertion.status());assertTrue(assertion.boundedEvidence().get(0).contains("AssertionError"));
        TargetedTestExecutor.ExecutionResult selenium=executor(profile("selenium")).execute(request("Missing",Duration.ofSeconds(5)),compiled("Missing","@org.junit.jupiter.api.Test void test() { throw new org.openqa.selenium.NoSuchElementException(\"old-login-button\"); }"));
        assertEquals(TargetedTestExecutor.Status.FAIL,selenium.status());assertTrue(selenium.boundedEvidence().get(0).contains("NoSuchElementException"));
    }

    @Test void killsInfiniteStaticInitializerAndNextRunSucceeds() throws Exception {
        JUnitTargetedTestExecutor executor=executor(profile("static"));
        TargetedTestExecutor.ExecutionResult timed=executor.execute(request("SlowInit",Duration.ofMillis(500)),
                compiled("SlowInit","static { if(System.nanoTime()!=0) while(true) { Thread.onSpinWait(); } } @org.junit.jupiter.api.Test void test() {}"));
        assertEquals(TargetedTestExecutor.Status.TIMED_OUT,timed.status());
        assertTrue(timed.boundedEvidence().get(0).startsWith("TIMED_OUT:"));
        TargetedTestExecutor.ExecutionResult next=executor.execute(request("NextPass",Duration.ofSeconds(2)),
                compiled("NextPass","@org.junit.jupiter.api.Test void test() {}"));
        assertEquals(TargetedTestExecutor.Status.PASS,next.status());
    }

    @Test void killsLoopsThatIgnoreOrSwallowInterruptsAndSlowSetup() throws Exception {
        assertTimedOut("Infinite","@org.junit.jupiter.api.Test void test() { while(true) { Thread.onSpinWait(); } }");
        assertTimedOut("Swallow","@org.junit.jupiter.api.Test void test() { while(true) { try { Thread.sleep(20); } catch (InterruptedException ignored) {} } }");
        assertTimedOut("SlowSetup","@org.junit.jupiter.api.BeforeEach void setup() { while(true) { Thread.onSpinWait(); } } @org.junit.jupiter.api.Test void test() {}");
    }

    @Test void killsSlowDiscovery() throws Exception {
        String profile=profile("slow-discovery");
        TargetedTestExecutor.ExecutionResult result=executor(profile).execute(
                request("SlowDiscovery",Duration.ofMillis(500)),
                compiled("SlowDiscovery","@org.junit.jupiter.api.Test void test() {}"));
        assertEquals(TargetedTestExecutor.Status.TIMED_OUT,result.status());
    }

    @Test void systemExitOnlyTerminatesChildAndNextRunSucceeds() throws Exception {
        JUnitTargetedTestExecutor executor=executor(profile("exit"));
        for(int code:Set.of(0,17)){
            TargetedTestExecutor.ExecutionResult exited=executor.execute(request("Exit"+code,Duration.ofSeconds(5)),compiled("Exit"+code,"@org.junit.jupiter.api.Test void test() { System.exit("+code+"); }"));
            assertEquals(TargetedTestExecutor.Status.FAIL,exited.status());assertTrue(exited.boundedEvidence().get(0).contains("exited "+code));
            assertEquals(TargetedTestExecutor.Status.PASS,executor.execute(request("After"+code,Duration.ofSeconds(5)),compiled("After"+code,"@org.junit.jupiter.api.Test void test() {}" )).status());
        }
    }

    @Test void crashAndLargeOutputAreBoundedAndRedacted() throws Exception {
        TargetedTestExecutor.ExecutionResult stdout=executor(profile("crash-stdout")).execute(request("CrashStdout",Duration.ofSeconds(5)),compiled("CrashStdout","@org.junit.jupiter.api.Test void test() { System.out.print(\"x\".repeat(200000)); Runtime.getRuntime().halt(23); }"));
        assertEquals(TargetedTestExecutor.Status.FAIL,stdout.status());assertTrue(stdout.boundedEvidence().get(0).length()<=1000);
        TargetedTestExecutor.ExecutionResult stderr=executor(profile("crash-stderr")).execute(request("CrashStderr",Duration.ofSeconds(5)),compiled("CrashStderr","@org.junit.jupiter.api.Test void test() { System.err.print(\"password=top-secret \".repeat(20000)); Runtime.getRuntime().halt(24); }"));
        assertEquals(TargetedTestExecutor.Status.FAIL,stderr.status());assertTrue(stderr.boundedEvidence().get(0).length()<=1000);assertFalse(stderr.boundedEvidence().get(0).contains("top-secret"));
    }

    @Test void crashCleansDescendantAndNextRunSucceeds() throws Exception {
        String profile=profile("crash-tree");Path pid=childPid(profile);
        String body="@org.junit.jupiter.api.Test void test() throws Exception { String executable=java.nio.file.Path.of(System.getProperty(\"java.home\"),\"bin\",System.getProperty(\"os.name\").toLowerCase().contains(\"win\")?\"java.exe\":\"java\").toString(); Process child=new ProcessBuilder(executable,\"-cp\",System.getProperty(\"java.class.path\"),\"io.github.testlens.studio.ForkedChildSleeper\").start(); java.nio.file.Files.writeString(java.nio.file.Path.of(System.getProperty(\"java.io.tmpdir\"),\"test-lens-\"+System.getProperty(\"testlens.targeted.profile\")+\".childpid\"),Long.toString(child.pid())); Thread.sleep(250); Runtime.getRuntime().halt(23); }";
        JUnitTargetedTestExecutor executor=executor(profile);
        assertEquals(TargetedTestExecutor.Status.FAIL,executor.execute(request("CrashTree",Duration.ofSeconds(5)),compiled("CrashTree",body)).status());
        assertChildStopped(pid);
        assertEquals(TargetedTestExecutor.Status.PASS,executor.execute(request("AfterCrash",Duration.ofSeconds(5)),compiled("AfterCrash","@org.junit.jupiter.api.Test void test() {}" )).status());
    }

    @Test void malformedProtocolIsRejected() throws Exception {
        Path result=temporaryDirectory.resolve("malformed.properties");Files.writeString(result,"schemaVersion=99\nstatus=PASS\ntests=1\n");
        assertEquals(TargetedTestExecutor.Status.FAIL,JUnitTargetedTestExecutor.readResult(result).status());
    }

    @Test void killsDescendantProcessAndRemovesTemporaryWorkspace() throws Exception {
        String profile=profile("tree");Path pid=childPid(profile);
        long before=countWorkspaces();
        String body="@org.junit.jupiter.api.Test void test() throws Exception { String executable=java.nio.file.Path.of(System.getProperty(\"java.home\"),\"bin\",System.getProperty(\"os.name\").toLowerCase().contains(\"win\")?\"java.exe\":\"java\").toString(); Process child=new ProcessBuilder(executable,\"-cp\",System.getProperty(\"java.class.path\"),\"io.github.testlens.studio.ForkedChildSleeper\").start(); java.nio.file.Files.writeString(java.nio.file.Path.of(System.getProperty(\"java.io.tmpdir\"),\"test-lens-\"+System.getProperty(\"testlens.targeted.profile\")+\".childpid\"),Long.toString(child.pid())); while(true){Thread.onSpinWait();} }";
        assertEquals(TargetedTestExecutor.Status.TIMED_OUT,executor(profile).execute(request("Tree",Duration.ofSeconds(1)),compiled("Tree",body)).status());
        assertChildStopped(pid);assertEquals(before,countWorkspaces());
    }

    @Test void providerPreparationSharesTheAbsoluteDeadline() throws Exception {assertEquals(TargetedTestExecutor.Status.TIMED_OUT,executor(profile("slow-open")).execute(request("Prepared",Duration.ofMillis(500)),compiled("Prepared","@org.junit.jupiter.api.Test void test() {}" )).status());}

    private static void assertTimedOut(String name,String body)throws Exception{assertEquals(TargetedTestExecutor.Status.TIMED_OUT,executor(profile(name)).execute(request(name,Duration.ofMillis(500)),compiled(name,body)).status());}
    private static JUnitTargetedTestExecutor executor(String profile){return new JUnitTargetedTestExecutor(new ForkedTestBrowserSessionProvider(),new BrowserRequest(Purpose.TEST_EXECUTION,Browser.CHROME,Ownership.STUDIO_OWNED,true,profile),List.of(),null);}
    private static String profile(String prefix){return prefix.toLowerCase()+"-"+UUID.randomUUID().toString().substring(0,8);}
    private static Path childPid(String profile){return Path.of(System.getProperty("java.io.tmpdir"),"test-lens-"+profile+".childpid");}
    private static void assertChildStopped(Path pid)throws Exception{long childPid=Long.parseLong(Files.readString(pid));for(int i=0;i<20&&ProcessHandle.of(childPid).map(ProcessHandle::isAlive).orElse(false);i++)Thread.sleep(50);assertFalse(ProcessHandle.of(childPid).map(ProcessHandle::isAlive).orElse(false));Files.deleteIfExists(pid);}
    private static long countWorkspaces()throws Exception{try(var paths=Files.list(Path.of(System.getProperty("java.io.tmpdir")))){return paths.filter(path->path.getFileName().toString().startsWith("test-lens-targeted-run-")).count();}}

    private static TargetedTestExecutor.ExecutionRequest request(String simpleName,Duration timeout) {
        return new TargetedTestExecutor.ExecutionRequest("deadline-"+simpleName,"deadline."+simpleName,List.of(),timeout);
    }

    private static TargetedJavaCompiler.CompiledOutput compiled(String simpleName,String body) {
        String source="package deadline; public class "+simpleName+" { "+body+" }";
        var unit=new TargetedJavaCompiler.SourceUnit(Path.of("deadline",simpleName+".java"),"deadline."+simpleName,source,ArtifactEnvelope.digest(new byte[0]));
        var result=new TargetedJavaCompiler().compile(new TargetedJavaCompiler.CompilationRequest(List.of(unit),17,System.getProperty("java.class.path"),20),Map.of());
        assertTrue(result.successful(),()->result.diagnostics().toString());
        return result.output();
    }
}
