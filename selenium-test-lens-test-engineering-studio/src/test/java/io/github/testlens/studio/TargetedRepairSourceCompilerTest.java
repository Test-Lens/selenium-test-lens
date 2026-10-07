package io.github.testlens.studio;

import io.github.testlens.application.tooling.ai.ContractHeader;
import io.github.testlens.application.tooling.ai.RepairProposal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TargetedRepairSourceCompilerTest {
    @TempDir Path root;
    @Test void compilesExactAppliedHandwrittenSourceAndRejectsBrokenRepair() throws Exception {
        Path source=root.resolve("src/test/java/example/LoginPage.java");Files.createDirectories(source.getParent());
        Files.writeString(source,"package example; final class LoginPage { boolean repaired(){ return true; } }");
        TargetedRepairSourceCompiler compiler=new TargetedRepairSourceCompiler(root,17,System.getProperty("java.class.path"));
        assertDoesNotThrow(()->compiler.compile(proposal("src/test/java/example/LoginPage.java")));
        Files.writeString(source,"package example; final class LoginPage { broken syntax }");
        assertThrows(Exception.class,()->compiler.compile(proposal("src/test/java/example/LoginPage.java")));
    }
    @Test void installsVerifiedRepairBytecodeInExistingProjectTestOutput() throws Exception {
        Path sourceRoot=root.resolve("src/test/java"),output=root.resolve("target/test-classes");
        Path source=sourceRoot.resolve("example/LoginPage.java");Files.createDirectories(source.getParent());Files.createDirectories(output);
        Files.writeString(source,"package example; public final class LoginPage { public boolean repaired(){ return true; } }");
        String classpath=String.join(java.io.File.pathSeparator,System.getProperty("java.class.path"),output.toString());
        TargetedRepairSourceCompiler compiler=new TargetedRepairSourceCompiler(root,17,classpath,List.of(sourceRoot),List.of(output));
        compiler.compile(proposal("src/test/java/example/LoginPage.java"));
        assertTrue(Files.isRegularFile(output.resolve("example/LoginPage.class")));
    }
    private static RepairProposal proposal(String path){
        ContractHeader header=new ContractHeader(ContractHeader.SCHEMA_VERSION,ContractHeader.Status.READY,List.of(),List.of(),ContractHeader.Confidence.LIVE_VALIDATED);
        var target=new RepairProposal.SourceTarget("element","declaration",path,new RepairProposal.SourceRange(1,1,1,2,0,1),"sha256:file","sha256:declaration","EXACT","id","old");
        var replacement=new RepairProposal.SelectorEvidence("id","new","candidate","VERIFIED_IN_SCOPE","SAME_TARGET",true,List.of("STABLE"),"LIVE_CANDIDATE_ANALYSIS");
        return new RepairProposal(header,"repair",RepairProposal.ApplicationPolicy.PROPOSE_ONLY,"selector","reason","id:old","id:new","declaration","old","candidate","classification","drift",List.of(),List.of(),List.of(),List.of(),List.of(),target,replacement,List.of());
    }
}
