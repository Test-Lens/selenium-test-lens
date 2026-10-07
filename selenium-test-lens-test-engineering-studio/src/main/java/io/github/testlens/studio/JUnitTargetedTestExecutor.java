package io.github.testlens.studio;

import io.github.testlens.application.tooling.ai.workflow.CompiledTargetedTestExecutor;
import io.github.testlens.application.tooling.ai.workflow.TargetedJavaCompiler;
import io.github.testlens.application.tooling.ai.workflow.TargetedTestExecutor;
import io.github.testlens.core.redaction.RedactionPolicy;
import io.github.testlens.studio.browser.BrowserRequest;
import io.github.testlens.studio.browser.BrowserSessionProvider;
import java.net.URL;
import java.net.URLClassLoader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/** Tooling-side targeted JUnit 5 runner isolated behind a hard process deadline. */
final class JUnitTargetedTestExecutor implements CompiledTargetedTestExecutor {
    private static final RedactionPolicy REDACTION=RedactionPolicy.defaults();
    private final BrowserSessionProvider browsers;
    private final BrowserRequest browserRequest;
    private final List<Path> classpath;
    private final String startUrl;

    JUnitTargetedTestExecutor(BrowserSessionProvider browsers, BrowserRequest browserRequest,
                              List<Path> classpath, java.net.URI startUrl) {
        this.browsers=browsers;this.browserRequest=browserRequest;this.classpath=List.copyOf(classpath);
        this.startUrl=startUrl==null?"":startUrl.toString();
    }

    @Override public TargetedTestExecutor.ExecutionResult execute(TargetedTestExecutor.ExecutionRequest request,
                                                                  TargetedJavaCompiler.CompiledOutput output) throws Exception {
        long deadline=deadlineAfter(request.timeout());
        Path workspace=Files.createTempDirectory("test-lens-targeted-run-");
        Path classes=workspace.resolve("classes"),result=workspace.resolve("result.properties");
        Process process=null;
        BoundedCapture stdout=null,stderr=null;
        List<ProcessHandle> descendants=new ArrayList<>();
        try {
            try{browsers.getClass().getConstructor();}
            catch(NoSuchMethodException unavailable){return failed("BrowserSessionProvider used for targeted execution must expose a public no-argument constructor: "+browsers.getClass().getName());}
            output.writeTo(classes);
            if(expired(deadline))return timedOut(request.timeout());
            List<String> command=new ArrayList<>();
            command.add(javaExecutable().toString());command.add("-cp");command.add(join(executionClasspath(classes)));
            command.add(ForkedJUnitTargetedTestMain.class.getName());
            command.add(browsers.getClass().getName());command.add(browserRequest.purpose().name());
            command.add(browserRequest.browser().name());command.add(browserRequest.ownership().name());
            command.add(Boolean.toString(browserRequest.headless()));command.add(browserRequest.profileId());
            command.add(startUrl);command.add(request.testClass());command.add(result.toString());
            ProcessBuilder builder=new ProcessBuilder(command).directory(workspace.toFile());
            builder.environment().clear();
            allowEnvironment(builder,"PATH");allowEnvironment(builder,"Path");allowEnvironment(builder,"SYSTEMROOT");
            allowEnvironment(builder,"WINDIR");allowEnvironment(builder,"TEMP");allowEnvironment(builder,"TMP");
            if(expired(deadline))return timedOut(request.timeout());
            process=builder.start();
            stdout=BoundedCapture.start(process.getInputStream(),"test-lens-targeted-stdout");
            stderr=BoundedCapture.start(process.getErrorStream(),"test-lens-targeted-stderr");
            while(process.isAlive()) {
                trackDescendants(process,descendants);
                long remaining=remainingMillis(deadline);
                if(expired(deadline)) {
                    terminate(process,descendants);
                    stdout.await();stderr.await();
                    return timedOut(request.timeout());
                }
                process.waitFor(Math.min(100,remaining),TimeUnit.MILLISECONDS);
            }
            trackDescendants(process,descendants);
            stdout.await();stderr.await();
            if(!Files.isRegularFile(result))return failed("Targeted execution process exited "+process.exitValue()+"; stdout="+stdout.text()+"; stderr="+stderr.text());
            return readResult(result);
        } finally {
            if(process!=null)terminate(process,descendants);
            deleteTree(workspace);
        }
    }

    private List<Path> executionClasspath(Path classes) {
        LinkedHashSet<Path> paths=new LinkedHashSet<>();paths.add(classes);paths.addAll(classpath);
        String current=System.getProperty("java.class.path","");
        for(String entry:current.split(java.io.File.pathSeparator))if(!entry.isBlank())paths.add(Path.of(entry).toAbsolutePath().normalize());
        for(ClassLoader loader=Thread.currentThread().getContextClassLoader();loader!=null;loader=loader.getParent())
            if(loader instanceof URLClassLoader urls)for(URL url:urls.getURLs())if("file".equalsIgnoreCase(url.getProtocol()))
                try{paths.add(Path.of(url.toURI()).toAbsolutePath().normalize());}catch(Exception ignored){}
        return List.copyOf(paths);
    }

    private static TargetedTestExecutor.ExecutionResult failed(String evidence){return new TargetedTestExecutor.ExecutionResult(TargetedTestExecutor.Status.FAIL,0,List.of(bounded(evidence)));}
    private static TargetedTestExecutor.ExecutionResult timedOut(Duration timeout){return new TargetedTestExecutor.ExecutionResult(TargetedTestExecutor.Status.TIMED_OUT,0,
            List.of("TIMED_OUT: targeted execution exceeded "+timeout.toMillis()+" ms"));}
    private static Path javaExecutable(){return Path.of(System.getProperty("java.home"),"bin",isWindows()?"java.exe":"java");}
    private static boolean isWindows(){return System.getProperty("os.name","").toLowerCase(java.util.Locale.ROOT).contains("win");}
    private static String join(List<Path> paths){return paths.stream().map(Path::toString).collect(java.util.stream.Collectors.joining(java.io.File.pathSeparator));}
    private static long deadlineAfter(Duration timeout){long now=System.nanoTime(),sum=now+timeout.toNanos();return sum<0?Long.MAX_VALUE:sum;}
    private static boolean expired(long deadline){return System.nanoTime()>=deadline;}
    private static long remainingMillis(long deadline){return Math.max(1,TimeUnit.NANOSECONDS.toMillis(Math.max(1,deadline-System.nanoTime())));}
    private static void allowEnvironment(ProcessBuilder builder,String name){String value=System.getenv(name);if(value!=null)builder.environment().put(name,value);}
    private static void trackDescendants(Process process,List<ProcessHandle> descendants){
        process.toHandle().descendants().forEach(handle->{if(!descendants.contains(handle))descendants.add(handle);});
    }
    private static void terminate(Process process,List<ProcessHandle> descendants){
        trackDescendants(process,descendants);
        descendants.forEach(ProcessHandle::destroy);process.destroy();
        try{process.waitFor(500,TimeUnit.MILLISECONDS);}
        catch(InterruptedException interrupted){Thread.currentThread().interrupt();}
        trackDescendants(process,descendants);
        descendants.stream().filter(ProcessHandle::isAlive).forEach(ProcessHandle::destroyForcibly);
        if(process.isAlive())process.destroyForcibly();
        try{process.waitFor(2,TimeUnit.SECONDS);}
        catch(InterruptedException interrupted){Thread.currentThread().interrupt();}
    }
    static TargetedTestExecutor.ExecutionResult readResult(Path result){
        try{
            if(Files.size(result)>64*1024)return failed("Targeted execution result exceeded 65536 bytes");
            Properties values=new Properties();try(var in=Files.newInputStream(result)){values.load(in);}
            if(!"1".equals(values.getProperty("schemaVersion")))return failed("Targeted execution returned an unsupported result schema");
            TargetedTestExecutor.Status status=TargetedTestExecutor.Status.valueOf(values.getProperty("status","FAIL"));
            int tests=Integer.parseInt(values.getProperty("tests","0"));
            List<String> evidence=new ArrayList<>();int count=Math.min(20,Integer.parseInt(values.getProperty("evidence.count","0")));
            for(int i=0;i<count;i++)evidence.add(bounded(values.getProperty("evidence."+i,"")));
            return new TargetedTestExecutor.ExecutionResult(status,tests,evidence);
        }catch(Exception malformed){return failed("Malformed targeted execution result: "+malformed.getClass().getSimpleName());}
    }
    private static String bounded(String value){if(value==null)return "";String clean=REDACTION.redact(value).replaceAll("[\\r\\n]+"," ");return clean.length()<=1000?clean:clean.substring(0,1000);}
    private static void deleteTree(Path root){if(root==null||!Files.exists(root))return;try(Stream<Path> paths=Files.walk(root)){paths.sorted(java.util.Comparator.reverseOrder()).forEach(path->{try{Files.deleteIfExists(path);}catch(java.io.IOException ignored){}});}catch(java.io.IOException ignored){}}

    private static final class BoundedCapture {
        private static final int LIMIT=64*1024;
        private final ByteArrayOutputStream captured=new ByteArrayOutputStream(LIMIT);
        private final Thread reader;
        private BoundedCapture(InputStream input,String name){reader=new Thread(()->drain(input),name);reader.setDaemon(true);reader.start();}
        static BoundedCapture start(InputStream input,String name){return new BoundedCapture(input,name);}
        private void drain(InputStream input){try(input){byte[] buffer=new byte[4096];int read;long total=0;while((read=input.read(buffer))>=0){int keep=(int)Math.min(read,Math.max(0L,LIMIT-total));if(keep>0)captured.write(buffer,0,keep);total+=read;}}catch(java.io.IOException ignored){}}
        void await(){try{reader.join(2_000);}catch(InterruptedException interrupted){Thread.currentThread().interrupt();}}
        String text(){return bounded(captured.toString(StandardCharsets.UTF_8));}
    }
}
