package io.github.testlens.migration.tooling;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Bounded, cancellable, no-shell process boundary for explicitly authorized trusted run plans. */
public final class MigrationTrustedProcessRunner {
    public interface Cancellation{boolean cancelled();Cancellation NEVER=()->false;}
    public interface SecretResolver{SecretInjection resolve(String referenceName);}
    public record SecretInjection(String environmentName,char[] value){public SecretInjection{if(environmentName==null||!environmentName.matches("[A-Za-z_][A-Za-z0-9_]{0,127}"))throw new IllegalArgumentException("secret environment name");value=value==null?new char[0]:value.clone();}public char[]value(){return value.clone();}}
    public record LocalExecution(String executableRef,Path executable,Path workingDirectory,String worktreeBindingRef,SecretResolver secrets){public LocalExecution{if(executableRef==null||executableRef.isBlank()||executableRef.length()>512||executableRef.indexOf('\0')>=0)throw new IllegalArgumentException("executable ref");executable=executable.toAbsolutePath().normalize();workingDirectory=workingDirectory.toAbsolutePath().normalize();if(!worktreeBindingRef.matches("migration-worktree-binding-v1:sha256:[0-9a-f]{64}"))throw new IllegalArgumentException("worktree binding");}}
    record Raw(Status status,Integer exitCode,byte[]stdout,long stdoutCount,boolean stdoutTruncated,byte[]stderr,long stderrCount,boolean stderrTruncated,MigrationRunResult.ProcessTreeTermination termination,Instant started,Instant ended,List<String>issues){enum Status{FINISHED,TIMED_OUT,CANCELLED,START_FAILED,SAFE_STOP}}

    Raw run(MigrationRunPlan plan,LocalExecution local,MigrationAuthorization authorization,Cancellation cancellation){
        Instant started=Instant.now();List<String>issues=new ArrayList<>();List<MigrationAuthorization.Capability>missing=plan.requiredApprovals().stream().filter(x->!authorization.allows(x)).toList();if(!missing.isEmpty())return empty(Raw.Status.SAFE_STOP,started,List.of("APPROVAL_REQUIRED:"+missing));
        if(!local.executableRef().equals(plan.executableRef()))return empty(Raw.Status.SAFE_STOP,started,List.of("EXECUTABLE_TRUST_BINDING_CHANGED"));if(!local.worktreeBindingRef().equals(plan.trustedWorkingDirectoryBinding()))return empty(Raw.Status.SAFE_STOP,started,List.of("WORKTREE_BINDING_CHANGED"));
        if(!Files.isRegularFile(local.executable())||!Files.isDirectory(local.workingDirectory()))return empty(Raw.Status.START_FAILED,started,List.of("INVALID_EXECUTABLE_OR_WORKING_DIRECTORY"));
        if(isWindowsScript(local.executable()))return empty(Raw.Status.SAFE_STOP,started,List.of("WINDOWS_COMMAND_WRAPPER_UNSUPPORTED_WITHOUT_SHELL"));
        List<String>command=new ArrayList<>();command.add(local.executable().toString());command.addAll(plan.args());Process process;
        try{ProcessBuilder builder=new ProcessBuilder(command).directory(local.workingDirectory().toFile()).redirectInput(ProcessBuilder.Redirect.from(nullDevice()));plan.environmentOverlay().forEach((k,v)->builder.environment().put(k.processName(),v));for(String ref:plan.secretReferenceNames()){if(local.secrets()==null)return empty(Raw.Status.SAFE_STOP,started,List.of("SECRET_RESOLVER_REQUIRED:"+ref));SecretInjection injection=local.secrets().resolve(ref);if(injection==null)return empty(Raw.Status.SAFE_STOP,started,List.of("SECRET_REFERENCE_UNRESOLVED:"+ref));char[]value=injection.value();try{builder.environment().put(injection.environmentName(),new String(value));}finally{java.util.Arrays.fill(value,'\0');}}process=builder.start();}catch(IOException|RuntimeException e){return empty(Raw.Status.START_FAILED,started,List.of("PROCESS_START_FAILED:"+e.getClass().getSimpleName()));}
        Bounded out=new Bounded(process.getInputStream(),plan.outputBounds().stdoutBytes()),err=new Bounded(process.getErrorStream(),plan.outputBounds().stderrBytes());Thread ot=thread(out,"migration-run-stdout"),et=thread(err,"migration-run-stderr");ot.start();et.start();Raw.Status status=Raw.Status.FINISHED;MigrationRunResult.ProcessTreeTermination termination=MigrationRunResult.ProcessTreeTermination.NOT_REQUIRED;Integer exit=null;long deadline=System.nanoTime()+plan.timeout().toNanos();
        try{while(process.isAlive()){if(cancellation!=null&&cancellation.cancelled()){status=Raw.Status.CANCELLED;termination=terminate(process);break;}if(System.nanoTime()>=deadline){status=Raw.Status.TIMED_OUT;termination=terminate(process);break;}process.waitFor(Math.min(100,Math.max(1,Duration.ofNanos(Math.max(0,deadline-System.nanoTime())).toMillis())),TimeUnit.MILLISECONDS);}if(!process.isAlive())exit=process.exitValue();}catch(InterruptedException e){Thread.currentThread().interrupt();status=Raw.Status.CANCELLED;termination=terminate(process);issues.add("RUNNER_INTERRUPTED");}
        join(ot);join(et);if(process.isAlive()){termination=MigrationRunResult.ProcessTreeTermination.DESCENDANTS_REMAIN;issues.add("PROCESS_REMAINED_ALIVE");}
        return new Raw(status,exit,out.bytes(),out.total,out.truncated,err.bytes(),err.total,err.truncated,termination,started,Instant.now(),List.copyOf(issues));
    }
    private static Raw empty(Raw.Status status,Instant started,List<String>issues){return new Raw(status,null,new byte[0],0,false,new byte[0],0,false,MigrationRunResult.ProcessTreeTermination.NOT_STARTED,started,Instant.now(),issues);}
    private static Thread thread(Runnable task,String name){Thread t=new Thread(task,name);t.setDaemon(true);return t;}
    private static void join(Thread t){try{t.join(3_000);}catch(InterruptedException e){Thread.currentThread().interrupt();}}
    private static MigrationRunResult.ProcessTreeTermination terminate(Process p){List<ProcessHandle>desc=p.descendants().toList();desc.forEach(ProcessHandle::destroy);p.destroy();boolean forced=false;try{if(!p.waitFor(1,TimeUnit.SECONDS)){forced=true;desc.forEach(ProcessHandle::destroyForcibly);p.destroyForcibly();p.waitFor(2,TimeUnit.SECONDS);}}catch(InterruptedException e){Thread.currentThread().interrupt();forced=true;desc.forEach(ProcessHandle::destroyForcibly);p.destroyForcibly();}boolean alive=p.isAlive()||desc.stream().anyMatch(ProcessHandle::isAlive);return alive?MigrationRunResult.ProcessTreeTermination.DESCENDANTS_REMAIN:forced?MigrationRunResult.ProcessTreeTermination.FORCIBLY_TERMINATED:MigrationRunResult.ProcessTreeTermination.TERMINATED;}
    private static boolean isWindowsScript(Path p){if(!System.getProperty("os.name","").toLowerCase().contains("win"))return false;String n=p.getFileName().toString().toLowerCase();return n.endsWith(".cmd")||n.endsWith(".bat")||n.endsWith(".ps1");}
    private static java.io.File nullDevice(){return new java.io.File(System.getProperty("os.name","").toLowerCase().contains("win")?"NUL":"/dev/null");}
    private static final class Bounded implements Runnable{final InputStream in;final int limit;final ByteArrayOutputStream bytes=new ByteArrayOutputStream();volatile long total;volatile boolean truncated;Bounded(InputStream i,int l){in=i;limit=l;}public void run(){byte[]b=new byte[8192];try(in){int n;while((n=in.read(b))>=0){total+=n;int accepted=Math.min(n,Math.max(0,limit-bytes.size()));if(accepted>0)bytes.write(b,0,accepted);if(accepted<n)truncated=true;}}catch(IOException e){truncated=true;}}byte[]bytes(){return bytes.toByteArray();}}
}
