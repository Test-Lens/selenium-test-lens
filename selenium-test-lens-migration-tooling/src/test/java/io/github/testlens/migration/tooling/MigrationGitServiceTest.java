package io.github.testlens.migration.tooling;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MigrationGitServiceTest {
    @TempDir Path temp;

    @Test void cleanCheckpointRoundTripAndResumeAreDeterministic()throws Exception{
        Path repo=repo("clean");write(repo,"a.txt","one\n");commitAll(repo,"base");MigrationGitService service=new MigrationGitService();var inspected=service.inspect(repo.resolve("a.txt").getParent());
        assertEquals(MigrationGitPreflight.RepositoryStatus.DETECTED,inspected.preflight().repositoryStatus());assertEquals(MigrationGitPreflight.HeadState.NAMED_BRANCH,inspected.preflight().head().state());assertTrue(inspected.preflight().staged().isEmpty());assertEquals("git version 2",inspected.preflight().gitVersion().substring(0,13));
        Path external=temp.resolve("state");Files.createDirectory(external);MigrationStateStore store=MigrationStateStore.resolve(service,inspected,external);var config=MigrationCheckpoint.Configuration.create(MigrationIsolationPlan.Choice.USE_CURRENT_WORKTREE,MigrationCheckpoint.IntendedMode.PREFLIGHT_ONLY,store.storage());var one=MigrationCheckpoint.create(inspected.preflight(),inspected.sourceState(),config,List.of(),null,null);var two=MigrationCheckpoint.create(inspected.preflight(),inspected.sourceState(),config,List.of(),null,null);assertEquals(one.checkpointId(),two.checkpointId());assertArrayEquals(new MigrationCheckpointJson().write(one),new MigrationCheckpointJson().write(two));Path file=store.write(one);assertEquals(one,store.read(file));assertEquals(MigrationResumeValidator.Classification.VALID,new MigrationResumeValidator().validate(one,service.inspect(repo).preflight(),service.inspect(repo).sourceState(),List.of()).classification());assertFalse(new String(new MigrationCheckpointJson().write(one),StandardCharsets.UTF_8).contains(repo.toAbsolutePath().toString()));
    }

    @Test void dirtyRolesRenameDeleteAndUntrackedArePreserved()throws Exception{
        Path repo=repo("dirty");write(repo,"both.txt","base\n");write(repo,"delete.txt","gone\n");write(repo,"rename.txt","move\n");commitAll(repo,"base");write(repo,"both.txt","staged\n");git(repo,"add","--","both.txt");write(repo,"both.txt","working\n");Files.delete(repo.resolve("delete.txt"));git(repo,"mv","--","rename.txt","renamed.txt");write(repo,"new file.txt","new\n");
        var result=new MigrationGitService().inspect(repo);var p=result.preflight();assertTrue(p.staged().stream().anyMatch(x->x.logicalPath().equals("both.txt")));assertTrue(p.unstaged().stream().anyMatch(x->x.logicalPath().equals("both.txt")));assertTrue(p.unstaged().stream().anyMatch(x->x.logicalPath().equals("delete.txt")));assertTrue(p.untracked().stream().anyMatch(x->x.logicalPath().equals("new file.txt")));assertTrue(p.renamesAndCopies().stream().anyMatch(x->x.logicalPath().equals("renamed.txt")&&x.originalLogicalPath().equals("rename.txt")));var both=result.sourceState().files().stream().filter(x->x.logicalPath().equals("both.txt")).findFirst().orElseThrow();assertTrue(both.roles().containsAll(List.of("STAGED","UNSTAGED")));assertNotNull(both.index());assertNotNull(both.worktree());var renamed=result.sourceState().files().stream().filter(x->x.logicalPath().equals("renamed.txt")).findFirst().orElseThrow();assertEquals("rename.txt",renamed.relations().get(0).originalLogicalPath());var cp=checkpoint(result,MigrationCheckpoint.StateStorage.EXTERNAL_TOOL_ROOT);assertEquals(cp,new MigrationCheckpointJson().read(new MigrationCheckpointJson().write(cp)));
    }

    @Test void detachedAndUnbornAreHonest()throws Exception{
        Path unborn=repo("unborn");var u=new MigrationGitService().inspect(unborn);assertEquals(MigrationGitPreflight.HeadState.UNBORN,u.preflight().head().state());assertNull(u.preflight().head().commit());
        Path detached=repo("detached");write(detached,"a","x");commitAll(detached,"base");git(detached,"checkout","--detach","HEAD");var d=new MigrationGitService().inspect(detached);assertEquals(MigrationGitPreflight.HeadState.DETACHED,d.preflight().head().state());assertNull(d.preflight().head().branch());assertNotNull(d.preflight().head().commit());
    }

    @Test void realConflictIsReportedWithoutCleanup()throws Exception{
        Path repo=repo("conflict");write(repo,"a.txt","base\n");commitAll(repo,"base");String main=gitText(repo,"branch","--show-current");git(repo,"checkout","-b","side");write(repo,"a.txt","side\n");commitAll(repo,"side");git(repo,"checkout",main);write(repo,"a.txt","main\n");commitAll(repo,"main");assertNotEquals(0,gitExit(repo,"merge","side"));var p=new MigrationGitService().inspect(repo).preflight();assertTrue(p.ongoingOperations().contains(MigrationGitPreflight.GitOperation.MERGE));assertFalse(p.conflicted().isEmpty());assertEquals(MigrationIsolationPlan.Choice.MANUAL_RESOLUTION_REQUIRED,MigrationIsolationPlan.recommend(p).choice());assertTrue(Files.exists(repo.resolve(".git/MERGE_HEAD")));
    }

    @Test void fsmonitorAndPagerHelpersAreNotInvoked()throws Exception{
        Path repo=repo("hostile");write(repo,"a","x");commitAll(repo,"base");Path sentinel=temp.resolve("sentinel.txt"),helper=temp.resolve(isWindows()?"helper.cmd":"helper.sh");String script=isWindows()?"@echo off\r\necho hit>\""+sentinel+"\"\r\nexit /b 0\r\n":"#!/bin/sh\necho hit > '"+sentinel+"'\n";Files.writeString(helper,script);if(!isWindows())helper.toFile().setExecutable(true);git(repo,"config","core.fsmonitor",helper.toString());git(repo,"config","core.pager",helper.toString());new MigrationGitService().inspect(repo);assertFalse(Files.exists(sentinel));
    }

    @Test void stateRootUsesIgnoredTargetOrExternalFallbackWithoutDirtyingRepo()throws Exception{
        Path ignored=repo("ignored");write(ignored,".gitignore","target/\n");write(ignored,"a","x");commitAll(ignored,"base");MigrationGitService service=new MigrationGitService();var before=service.inspect(ignored);assertThrows(IllegalArgumentException.class,()->MigrationStateStore.resolve(service,before,before.localContext().gitCommonDir().resolve("migration-state")));MigrationStateStore local=MigrationStateStore.resolve(service,before,null);assertEquals(MigrationCheckpoint.StateStorage.IGNORED_PROJECT_TARGET,local.storage());var cp=checkpoint(before,local.storage());local.write(cp);var after=service.inspect(ignored);assertEquals(before.sourceState().sourceStateDigest(),after.sourceState().sourceStateDigest());
        Path unignored=repo("unignored");write(unignored,"a","x");commitAll(unignored,"base");String old=System.getProperty("user.home");Path home=temp.resolve("home");Files.createDirectory(home);try{System.setProperty("user.home",home.toString());var start=service.inspect(unignored);MigrationStateStore external=MigrationStateStore.resolve(service,start,null);assertEquals(MigrationCheckpoint.StateStorage.EXTERNAL_TOOL_ROOT,external.storage());external.write(checkpoint(start,external.storage()));assertFalse(Files.exists(unignored.resolve("target")));assertEquals(start.sourceState().sourceStateDigest(),service.inspect(unignored).sourceState().sourceStateDigest());}finally{System.setProperty("user.home",old);}
    }

    @Test void resumeReportsIndependentIndexWorkingUntrackedHeadArtifactAndBindingReasons()throws Exception{
        Path repo=repo("resume");write(repo,"a","one");commitAll(repo,"base");MigrationGitService service=new MigrationGitService();var start=service.inspect(repo);var artifact=new MigrationArtifactRef(MigrationArtifactRef.Type.BASELINE,1,"baseline-v1:sha256:"+"1".repeat(64),"base",MigrationArtifactRef.Completeness.COMPLETE);var config=MigrationCheckpoint.Configuration.create(MigrationIsolationPlan.Choice.USE_CURRENT_WORKTREE,MigrationCheckpoint.IntendedMode.BASELINE_PREPARATION,MigrationCheckpoint.StateStorage.EXTERNAL_TOOL_ROOT);var cp=MigrationCheckpoint.create(start.preflight(),start.sourceState(),config,List.of(artifact),null,null);
        write(repo,"a","two");git(repo,"add","--","a");write(repo,"a","three");write(repo,"u","x");var fresh=service.inspect(repo);var changed=new MigrationArtifactRef(MigrationArtifactRef.Type.BASELINE,1,"baseline-v1:sha256:"+"2".repeat(64),"base",MigrationArtifactRef.Completeness.COMPLETE);var result=new MigrationResumeValidator().validate(cp,fresh.preflight(),fresh.sourceState(),List.of(changed));assertTrue(result.reasons().contains(MigrationResumeValidator.Reason.INDEX_CHANGED));assertTrue(result.reasons().contains(MigrationResumeValidator.Reason.WORKING_FILE_CHANGED));assertTrue(result.reasons().contains(MigrationResumeValidator.Reason.UNTRACKED_CHANGED));assertTrue(result.reasons().contains(MigrationResumeValidator.Reason.INPUT_REPORT_CHANGED));assertEquals(MigrationResumeValidator.Classification.REQUIRES_REFRESH,result.classification());
    }

    @Test void untrackedOnlyChangeRequiresRefreshBecauseB1HasNoRelevanceModel()throws Exception{Path repo=repo("untracked-resume");write(repo,"a","one");commitAll(repo,"base");MigrationGitService service=new MigrationGitService();var start=service.inspect(repo);var cp=checkpoint(start,MigrationCheckpoint.StateStorage.EXTERNAL_TOOL_ROOT);write(repo,"new.txt","new");var fresh=service.inspect(repo);var result=new MigrationResumeValidator().validate(cp,fresh.preflight(),fresh.sourceState(),List.of());assertEquals(MigrationResumeValidator.Classification.REQUIRES_REFRESH,result.classification());assertEquals(List.of(MigrationResumeValidator.Reason.UNTRACKED_CHANGED),result.reasons());assertEquals(List.of("new.txt"),result.changedPaths());}

    @Test void failedOversizedWritePreservesPreviouslyStoredCheckpoint()throws Exception{Path repo=repo("atomic-failure");write(repo,"a","one");commitAll(repo,"base");MigrationGitService service=new MigrationGitService();var inspected=service.inspect(repo);Path root=temp.resolve("atomic-state");Files.createDirectory(root);MigrationStateStore store=MigrationStateStore.resolve(service,inspected,root);var valid=checkpoint(inspected,store.storage());Path validFile=store.write(valid);List<MigrationSourceStateFingerprint.FileState>files=new java.util.ArrayList<>();for(int i=0;i<MigrationSourceStateFingerprint.MAX_DIRTY_PATHS;i++)files.add(new MigrationSourceStateFingerprint.FileState("p"+i+"-"+"x".repeat(1_800),java.util.Set.of("UNTRACKED"),null,null));var oversizedSource=new MigrationSourceStateFingerprint(1,"migration-source-state-v1:sha256:"+"a".repeat(64),inspected.preflight().head().tree(),files,0,MigrationSourceStateFingerprint.Completeness.COMPLETE,List.of());var oversized=MigrationCheckpoint.create(inspected.preflight(),oversizedSource,MigrationCheckpoint.Configuration.create(MigrationIsolationPlan.Choice.USE_CURRENT_WORKTREE,MigrationCheckpoint.IntendedMode.PREFLIGHT_ONLY,store.storage()),List.of(),null,null);assertThrows(MigrationCheckpointJson.Format.class,()->store.write(oversized));assertEquals(valid,store.read(validFile));}

    @Test void branchValidationAndOperationAllowlistCannotMutate()throws Exception{Path repo=repo("branch");MigrationGitService s=new MigrationGitService();assertTrue(s.validBranchCandidate(repo,"migration/test-lens-headless"));assertFalse(s.validBranchCandidate(repo,"bad..name"));for(SafeGit.Operation op:SafeGit.Operation.values())assertFalse(List.of("reset","clean","restore","checkout","switch","add","commit","stash","branch","merge","rebase","fetch","pull","push").contains(op.name().toLowerCase()));}

    @Test void sourceChangeDuringHashingIsIncompleteThroughDeterministicSeam()throws Exception{
        Path repo=repo("hash-race");write(repo,"changing.txt","before");var inspected=new MigrationGitService().inspect(repo);
        var raced=new SourceStateFingerprinter(new SafeGit("git"),path->Files.writeString(path,"after-and-different-size",StandardCharsets.UTF_8))
                .fingerprint(inspected.preflight(),inspected.localContext());
        assertEquals(MigrationSourceStateFingerprint.Completeness.INCOMPLETE_SOURCE_STATE,raced.completeness());
        assertTrue(raced.issues().stream().anyMatch(x->x.startsWith("SOURCE_CHANGED_DURING_CHECKPOINT:")));
    }

    @Test void oversizedDirtyFileProducesIncompleteStateWithoutReadingIt()throws Exception{
        Path repo=repo("oversized");Path file=repo.resolve("large.bin");try(var channel=Files.newByteChannel(file,java.nio.file.StandardOpenOption.CREATE,java.nio.file.StandardOpenOption.WRITE)){channel.position(MigrationSourceStateFingerprint.MAX_FILE_BYTES);channel.write(java.nio.ByteBuffer.wrap(new byte[]{1}));}
        var result=new MigrationGitService().inspect(repo);assertEquals(MigrationSourceStateFingerprint.Completeness.INCOMPLETE_SOURCE_STATE,result.sourceState().completeness());assertTrue(result.sourceState().issues().stream().anyMatch(x->x.startsWith("FILE_HASH_LIMIT_EXCEEDED:")));
    }

    @Test void readOnlyWorkflowLeavesHeadIndexRefsAndWorktreesUnchanged()throws Exception{
        Path repo=repo("read-only-invariant");write(repo,"tracked.txt","base");commitAll(repo,"base");write(repo,"tracked.txt","dirty");write(repo,"untracked.txt","new");String before=snapshot(repo);MigrationGitService service=new MigrationGitService();var inspected=service.inspect(repo);Path state=temp.resolve("invariant-state");Files.createDirectory(state);MigrationStateStore store=MigrationStateStore.resolve(service,inspected,state);var checkpoint=checkpoint(inspected,store.storage());Path saved=store.write(checkpoint);var loaded=store.read(saved);var fresh=service.inspect(repo);new MigrationResumeValidator().validate(loaded,fresh.preflight(),fresh.sourceState(),List.of());assertEquals(before,snapshot(repo));
    }

    @Test void resumeClassifiesHeadBranchWorktreeAndMissingArtifactIndependently()throws Exception{
        Path repo=repo("resume-dimensions");write(repo,"a","one");commitAll(repo,"base");MigrationGitService service=new MigrationGitService();var start=service.inspect(repo);var artifact=new MigrationArtifactRef(MigrationArtifactRef.Type.BASELINE,1,"baseline-v1:sha256:"+"1".repeat(64),"base",MigrationArtifactRef.Completeness.COMPLETE);var config=MigrationCheckpoint.Configuration.create(MigrationIsolationPlan.Choice.USE_CURRENT_WORKTREE,MigrationCheckpoint.IntendedMode.BASELINE_PREPARATION,MigrationCheckpoint.StateStorage.EXTERNAL_TOOL_ROOT);var checkpoint=MigrationCheckpoint.create(start.preflight(),start.sourceState(),config,List.of(artifact),null,null);
        git(repo,"branch","-m","renamed-fixture");var renamed=service.inspect(repo);var branchResult=new MigrationResumeValidator().validate(checkpoint,renamed.preflight(),renamed.sourceState(),List.of(artifact));assertTrue(branchResult.reasons().contains(MigrationResumeValidator.Reason.BRANCH_CHANGED));assertTrue(new MigrationResumeValidator().validate(checkpoint,start.preflight(),start.sourceState(),List.of()).reasons().contains(MigrationResumeValidator.Reason.MISSING_ARTIFACT));
        write(repo,"b","two");commitAll(repo,"next");var advanced=service.inspect(repo);assertTrue(new MigrationResumeValidator().validate(checkpoint,advanced.preflight(),advanced.sourceState(),List.of(artifact)).reasons().contains(MigrationResumeValidator.Reason.HEAD_CHANGED));
        Path other=temp.resolve("resume-other-worktree");git(repo,"worktree","add","--detach",other.toString(),"HEAD");var otherState=service.inspect(other);var worktreeResult=new MigrationResumeValidator().validate(checkpoint,otherState.preflight(),otherState.sourceState(),List.of(artifact));assertTrue(worktreeResult.reasons().contains(MigrationResumeValidator.Reason.WORKTREE_CHANGED));assertEquals(MigrationResumeValidator.Classification.BLOCKED,worktreeResult.classification());
    }

    private MigrationCheckpoint checkpoint(MigrationGitService.Result r,MigrationCheckpoint.StateStorage storage){return MigrationCheckpoint.create(r.preflight(),r.sourceState(),MigrationCheckpoint.Configuration.create(MigrationIsolationPlan.Choice.USE_CURRENT_WORKTREE,MigrationCheckpoint.IntendedMode.PREFLIGHT_ONLY,storage),List.of(),null,null);}
    private Path repo(String name)throws Exception{Path p=temp.resolve(name);Files.createDirectory(p);git(p,"init");git(p,"config","user.email","test@example.invalid");git(p,"config","user.name","Test");return p;}
    private static void write(Path root,String name,String value)throws IOException{Path p=root.resolve(name);if(p.getParent()!=null)Files.createDirectories(p.getParent());Files.writeString(p,value,StandardCharsets.UTF_8);}
    private static void commitAll(Path p,String message)throws Exception{git(p,"add","--all");git(p,"commit","-m",message);}
    private static void git(Path p,String...args)throws Exception{int exit=gitExit(p,args);if(exit!=0)throw new AssertionError("git failed: "+String.join(" ",args));}
    private static int gitExit(Path p,String...args)throws Exception{var c=new java.util.ArrayList<String>();c.add("git");c.addAll(List.of(args));Process x=new ProcessBuilder(c).directory(p.toFile()).redirectErrorStream(true).start();x.getInputStream().transferTo(java.io.OutputStream.nullOutputStream());return x.waitFor();}
    private static String gitText(Path p,String...args)throws Exception{var c=new java.util.ArrayList<String>();c.add("git");c.addAll(List.of(args));Process x=new ProcessBuilder(c).directory(p.toFile()).redirectErrorStream(true).start();String out=new String(x.getInputStream().readAllBytes(),StandardCharsets.UTF_8).strip();assertEquals(0,x.waitFor());return out;}
    private static String snapshot(Path p)throws Exception{return gitText(p,"rev-parse","HEAD")+"\n"+gitText(p,"status","--porcelain=v2","--branch","--untracked-files=all")+"\n"+gitText(p,"show-ref")+"\n"+gitText(p,"worktree","list","--porcelain");}
    private static boolean isWindows(){return System.getProperty("os.name","").toLowerCase().contains("win");}
}
