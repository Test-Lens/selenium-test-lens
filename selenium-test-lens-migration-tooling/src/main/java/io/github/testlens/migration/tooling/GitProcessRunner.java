package io.github.testlens.migration.tooling;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Internal bounded process boundary. Arguments are never joined into a shell command. */
final class GitProcessRunner {
    static final int DEFAULT_OUTPUT_LIMIT=32*1024*1024;
    record Result(List<String>args,Path workingDirectory,byte[]stdout,byte[]stderr,int exitCode,boolean timedOut,
                  boolean interrupted,boolean outputTruncated,Duration duration){
        Result{args=List.copyOf(args);workingDirectory=workingDirectory.toAbsolutePath().normalize();stdout=stdout.clone();stderr=stderr.clone();}
    }
    Result run(List<String> command,Path directory,Map<String,String>environment,Duration timeout,int outputLimit)throws IOException{
        long started=System.nanoTime();ProcessBuilder builder=new ProcessBuilder(new ArrayList<>(command));
        builder.directory(directory.toFile());builder.redirectInput(ProcessBuilder.Redirect.from(nullDevice()));
        builder.environment().putAll(environment);Process process=builder.start();
        Bounded stdout=new Bounded(process.getInputStream(),outputLimit),stderr=new Bounded(process.getErrorStream(),outputLimit);
        Thread out=new Thread(stdout,"migration-git-stdout"),err=new Thread(stderr,"migration-git-stderr");out.setDaemon(true);err.setDaemon(true);out.start();err.start();
        boolean timedOut=false,interrupted=false;int exit=-1;
        try{if(!process.waitFor(timeout.toMillis(),TimeUnit.MILLISECONDS)){timedOut=true;terminate(process);}else exit=process.exitValue();}
        catch(InterruptedException e){interrupted=true;terminate(process);Thread.currentThread().interrupt();}
        join(out);join(err);
        return new Result(command,directory,stdout.bytes(),stderr.bytes(),exit,timedOut,interrupted,stdout.truncated||stderr.truncated,Duration.ofNanos(System.nanoTime()-started));
    }
    private static java.io.File nullDevice(){return new java.io.File(System.getProperty("os.name","").toLowerCase().contains("win")?"NUL":"/dev/null");}
    private static void terminate(Process process){process.descendants().forEach(ProcessHandle::destroy);process.destroy();try{if(!process.waitFor(2,TimeUnit.SECONDS)){process.descendants().forEach(ProcessHandle::destroyForcibly);process.destroyForcibly();}}catch(InterruptedException e){Thread.currentThread().interrupt();process.destroyForcibly();}}
    private static void join(Thread thread){try{thread.join(3000);}catch(InterruptedException e){Thread.currentThread().interrupt();}}
    private static final class Bounded implements Runnable{
        private final InputStream input;private final int limit;private final ByteArrayOutputStream output=new ByteArrayOutputStream();volatile boolean truncated;
        Bounded(InputStream input,int limit){this.input=input;this.limit=limit;}
        public void run(){byte[]buffer=new byte[8192];try(input){int n;while((n=input.read(buffer))>=0){int accepted=Math.min(n,Math.max(0,limit-output.size()));if(accepted>0)output.write(buffer,0,accepted);if(accepted<n)truncated=true;}}catch(IOException ignored){truncated=true;}}
        byte[]bytes(){return output.toByteArray();}
    }
}
