package io.github.testlens.studio;

import io.github.testlens.application.tooling.ai.RepairProposal;
import io.github.testlens.application.tooling.ai.workflow.AgentExecutor;
import io.github.testlens.application.tooling.ai.workflow.ArtifactEnvelope;
import io.github.testlens.application.tooling.ai.workflow.TargetedJavaCompiler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Recompiles the exact applied handwritten source target before repair verification. */
public final class TargetedRepairSourceCompiler implements ReviewableCoordinatorWorkflowGateway.RepairSourceCompiler {
    private static final int MAX_SOURCE_BYTES=2*1024*1024;
    private static final Pattern PACKAGE=Pattern.compile("(?m)^\\s*package\\s+([A-Za-z_$][A-Za-z0-9_$.]*)\\s*;");
    private final Path projectRoot;private final int release;private final String classpath;private final TargetedJavaCompiler compiler=new TargetedJavaCompiler();
    public TargetedRepairSourceCompiler(Path projectRoot,int release,String classpath){this.projectRoot=Objects.requireNonNull(projectRoot).toAbsolutePath().normalize();this.release=release;this.classpath=classpath==null?"":classpath;}
    @Override public void compile(RepairProposal proposal)throws AgentExecutor.AgentExecutionException{
        if(proposal==null||proposal.sourceTarget()==null)throw failed("Repair proposal has no trusted source target");
        Path relative;try{relative=Path.of(proposal.sourceTarget().logicalPath()).normalize();}catch(RuntimeException failure){throw failed("Repair source path is invalid");}
        Path source=projectRoot.resolve(relative).normalize();
        if(relative.isAbsolute()||relative.startsWith("..")||!source.startsWith(projectRoot)||Files.isSymbolicLink(source))throw failed("Repair source path is outside the configured project");
        try{
            Path realRoot=projectRoot.toRealPath(),realSource=source.toRealPath(LinkOption.NOFOLLOW_LINKS);if(!realSource.startsWith(realRoot)||Files.isSymbolicLink(realSource))throw failed("Repair source path crosses a symbolic link");
            long size=Files.size(source);if(size<1||size>MAX_SOURCE_BYTES)throw failed("Repair source size is outside compiler bounds");
            String content=Files.readString(source,StandardCharsets.UTF_8);String simple=source.getFileName().toString().replaceFirst("\\.java$","");Matcher matcher=PACKAGE.matcher(content);String binary=matcher.find()?matcher.group(1)+'.'+simple:simple;
            String fingerprint=ArtifactEnvelope.digest(content);var unit=new TargetedJavaCompiler.SourceUnit(relative,binary,content,fingerprint);
            var result=compiler.compile(new TargetedJavaCompiler.CompilationRequest(List.of(unit),release,classpath,50),Map.of(relative,content));
            if(!result.successful())throw failed("Repaired source compilation failed: "+result.diagnostics().stream().map(TargetedJavaCompiler.CompilationDiagnostic::code).distinct().toList());
        }catch(IOException failure){throw failed("Unable to read repaired source: "+failure.getClass().getSimpleName());}
    }
    private static AgentExecutor.AgentExecutionException failed(String message){return new AgentExecutor.AgentExecutionException(AgentExecutor.AgentFailureCode.AGENT_PROCESS_FAILED,message);}
}
