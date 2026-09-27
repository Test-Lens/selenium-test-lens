package io.github.testlens.migration.tooling;

import io.github.testlens.selector.tooling.TrustedSelectorSourceProjection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MigrationApplyServiceTest {
    @TempDir Path temporary;

    @Test void approvedExactPlanAppliesThenRollsBackOriginalDirtyBytes() throws Exception {
        Fixture f=fixture("apply", "// user pre-existing content\n");
        var prepared=f.prepare();assertEquals(MigrationApplyService.PreparationStatus.PREPARED,prepared.status());
        MigrationApplyPlan plan=prepared.plan();byte[] original=Files.readAllBytes(f.source());
        var applied=f.service().apply(plan,f.repo(),f.store(),applyAuth(plan),null);
        assertEquals(MigrationApplyResult.Status.APPLIED,applied.status());
        assertTrue(Files.readString(f.source()).contains("By.cssSelector"));
        assertNotNull(applied.postApplyCheckpointRef());assertNotNull(applied.journalHeadDigest());
        assertEquals(plan.finalDiffDigest(),applied.finalDiffDigest());
        var rollback=f.service().rollback(applied.transactionId(),f.repo(),f.store(),rollbackAuth(plan,applied.transactionId()));
        assertEquals(MigrationRollbackResult.Status.ROLLED_BACK,rollback.status());
        assertArrayEquals(original,Files.readAllBytes(f.source()));
        var second=f.service().rollback(applied.transactionId(),f.repo(),f.store(),rollbackAuth(plan,applied.transactionId()));
        assertEquals(MigrationRollbackResult.Status.ROLLED_BACK,second.status());
        assertTrue(second.files().stream().allMatch(x->x.status()==MigrationRollbackResult.FileStatus.ALREADY_ORIGINAL));
    }

    @Test void approvalIsBoundToExactPlanAndSourceChangeAfterPrepareWritesNothing() throws Exception {
        Fixture f=fixture("stale", "");MigrationApplyPlan plan=f.prepare().plan();
        assertEquals(MigrationApplyResult.Status.APPROVAL_REQUIRED,f.service().apply(plan,f.repo(),f.store(),null,null).status());
        String changed=Files.readString(f.source())+"// external edit\n";Files.writeString(f.source(),changed,StandardCharsets.UTF_8);
        var result=f.service().apply(plan,f.repo(),f.store(),applyAuth(plan),null);
        assertEquals(MigrationApplyResult.Status.STALE_PRECONDITION,result.status());
        assertEquals(changed,Files.readString(f.source()));
    }

    @Test void repositoryApplyLockMakesSecondApplySafeStopWithoutWrites() throws Exception {
        Fixture f=fixture("lock", "");MigrationApplyPlan plan=f.prepare().plan();byte[] before=Files.readAllBytes(f.source());
        Path lockPath=f.store().root().resolve("apply/locks/"+plan.repositoryBindingRef().substring(plan.repositoryBindingRef().lastIndexOf(':')+1)+".lock");
        Files.createDirectories(lockPath.getParent());
        try(FileChannel channel=FileChannel.open(lockPath,StandardOpenOption.CREATE,StandardOpenOption.WRITE);var ignored=channel.lock()){
            var result=f.service().apply(plan,f.repo(),f.store(),applyAuth(plan),null);
            assertEquals(MigrationApplyResult.Status.APPLY_LOCK_UNAVAILABLE,result.status());
        }
        assertArrayEquals(before,Files.readAllBytes(f.source()));
    }

    @Test void writeFailureCanRemainPartialOrUseSeparatelyAuthorizedCompensation() throws Exception {
        Fixture f=fixture("failure", "");MigrationApplyPlan plan=f.prepare().plan();byte[] before=Files.readAllBytes(f.source());
        MigrationApplyService failing=new MigrationApplyService(new MigrationGitService(),new MigrationApplyService.Faults(){
            @Override public void beforeMove(Path target,int index)throws IOException{throw new IOException("injected");}
        });
        var result=failing.apply(plan,f.repo(),f.store(),applyAuth(plan),null);
        assertEquals(MigrationApplyResult.Status.WRITE_FAILED,result.status());assertArrayEquals(before,Files.readAllBytes(f.source()));
        assertEquals(MigrationRecoveryPlan.Situation.SAFE_TO_ABANDON,failing.inspectRecovery(result.transactionId(),f.repo(),f.store()).situation());
    }

    @Test void failureAfterReplacementIsImmediatelyCompensatedOnlyWithRollbackAuthority() throws Exception {
        Fixture f=fixture("compensate", "");MigrationApplyPlan plan=f.prepare().plan();byte[] before=Files.readAllBytes(f.source());
        MigrationApplyService failing=new MigrationApplyService(new MigrationGitService(),new MigrationApplyService.Faults(){
            private boolean once;
            @Override public void afterMove(Path target,int index)throws IOException{if(!once){once=true;throw new IOException("after-move");}}
        });
        var compensation=MigrationTargetedAuthorization.of(plan.applyPlanId(),plan.repositoryBindingRef(),plan.worktreeBindingRef(),"test-host",MigrationTargetedAuthorization.Capability.ROLLBACK_TOOL_CHANGES);
        var result=failing.apply(plan,f.repo(),f.store(),applyAuth(plan),compensation);
        assertEquals(MigrationApplyResult.Status.COMPENSATED,result.status());
        assertArrayEquals(before,Files.readAllBytes(f.source()));
        assertNotNull(result.journalHeadDigest());
    }

    @Test void corruptBackupOrPreparedJournalFailureAbortsBeforeAnySourceWrite() throws Exception {
        Fixture corrupt=fixture("corrupt-backup", "");MigrationApplyPlan corruptPlan=corrupt.prepare().plan();byte[] before=Files.readAllBytes(corrupt.source());
        Path backup=corrupt.store().root().resolve(corruptPlan.filePlans().get(0).originalArtifact().logicalRef().replace('/',java.io.File.separatorChar));Files.writeString(backup,"tampered",StandardCharsets.UTF_8);
        assertEquals(MigrationApplyResult.Status.ABORTED,corrupt.service().apply(corruptPlan,corrupt.repo(),corrupt.store(),applyAuth(corruptPlan),null).status());assertArrayEquals(before,Files.readAllBytes(corrupt.source()));
        assertEquals(MigrationRecoveryPlan.Situation.RECOVERY_REQUIRED,corrupt.service().inspectRecovery(MigrationApplyTransaction.id(corruptPlan),corrupt.repo(),corrupt.store()).situation());
        Fixture journal=fixture("prepared-failure", "");MigrationApplyPlan plan=journal.prepare().plan();byte[] journalBefore=Files.readAllBytes(journal.source());
        MigrationApplyService failing=new MigrationApplyService(new MigrationGitService(),new MigrationApplyService.Faults(){@Override public void beforeJournal(MigrationApplyTransaction.EventType type,long sequence)throws IOException{if(type==MigrationApplyTransaction.EventType.PREPARED)throw new IOException("injected");}});
        assertEquals(MigrationApplyResult.Status.ABORTED,failing.apply(plan,journal.repo(),journal.store(),applyAuth(plan),null).status());assertArrayEquals(journalBefore,Files.readAllBytes(journal.source()));
    }

    @Test void laterUserModificationBlocksOwnedRollback() throws Exception {
        Fixture f=fixture("user-edit", "");MigrationApplyPlan plan=f.prepare().plan();var applied=f.service().apply(plan,f.repo(),f.store(),applyAuth(plan),null);
        Files.writeString(f.source(),Files.readString(f.source())+"// later user edit\n",StandardCharsets.UTF_8);
        var rollback=f.service().rollback(applied.transactionId(),f.repo(),f.store(),rollbackAuth(plan,applied.transactionId()));
        assertEquals(MigrationRollbackResult.Status.ROLLBACK_BLOCKED,rollback.status());
        assertEquals(MigrationRollbackResult.FileStatus.USER_MODIFIED,rollback.files().get(0).status());
        assertTrue(Files.readString(f.source()).contains("later user edit"));
    }

    @Test void verificationOperationLockMakesConcurrentRollbackSafeStop() throws Exception {
        Fixture f=fixture("verification-lock","");MigrationApplyPlan plan=f.prepare().plan();var applied=f.service().apply(plan,f.repo(),f.store(),applyAuth(plan),null);byte[] proposed=Files.readAllBytes(f.source());
        try(MigrationTransactionOperationLock ignored=MigrationTransactionOperationLock.tryAcquire(f.store(),applied.transactionId())){
            assertNotNull(ignored);var rollback=f.service().rollback(applied.transactionId(),f.repo(),f.store(),rollbackAuth(plan,applied.transactionId()));
            assertEquals(MigrationRollbackResult.Status.ROLLBACK_BLOCKED,rollback.status());assertEquals(List.of("VERIFICATION_TRANSACTION_BUSY"),rollback.issues());
        }
        assertArrayEquals(proposed,Files.readAllBytes(f.source()));
    }

    @Test void decisionResolutionUsesExplicitSupersessionNotArrayOrder() throws Exception {
        Fixture f=fixture("decisions", "");MigrationProposal proposal=f.proposal();
        var approve=MigrationDecisionLedger.decide(proposal,f.checkpoint().checkpointId(),MigrationDecisionLedger.Decision.APPROVE_FOR_S11,"host",null,null);
        var rejectIndependent=MigrationDecisionLedger.decide(proposal,f.checkpoint().checkpointId(),MigrationDecisionLedger.Decision.REJECT,"host",null,null);
        var ambiguous=new MigrationDecisionResolver().resolve(MigrationDecisionLedger.create(List.of(rejectIndependent,approve)),proposal,f.checkpoint().checkpointId(),proposal.evidenceRefs());
        assertEquals(MigrationDecisionResolver.Status.DECISION_AMBIGUOUS,ambiguous.status());
        var reject=MigrationDecisionLedger.decide(proposal,f.checkpoint().checkpointId(),MigrationDecisionLedger.Decision.REJECT,"host",null,approve.decisionRef());
        var resolved=new MigrationDecisionResolver().resolve(MigrationDecisionLedger.create(List.of(reject,approve)),proposal,f.checkpoint().checkpointId(),proposal.evidenceRefs());
        assertEquals(MigrationDecisionResolver.Status.NOT_APPROVED,resolved.status());
    }

    @Test void strictApplyPlanCodecRejectsDuplicateAndTamperedContent() throws Exception {
        Fixture f=fixture("codec", "");MigrationApplyPlan plan=f.prepare().plan();MigrationApplyCodec codec=new MigrationApplyCodec();
        assertEquals(plan,codec.readPlan(codec.write(plan)));
        MigrationS10ArtifactReader s10=new MigrationS10ArtifactReader();MigrationArtifactJson jsonWriter=new MigrationArtifactJson();
        assertEquals(f.set(),s10.readProposalSet(jsonWriter.write(f.set())));
        assertEquals(f.ledger(),s10.readDecisionLedger(jsonWriter.write(f.ledger())));
        assertEquals(f.checkpoint(),s10.readCheckpoint(new MigrationCheckpointJson().write(f.checkpoint())));
        String json=new String(codec.write(plan),StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class,()->codec.readPlan(json.replaceFirst("\\{","{\"schemaVersion\":1,").getBytes(StandardCharsets.UTF_8)));
        assertThrows(IllegalArgumentException.class,()->codec.readPlan(json.replace(plan.finalDiffDigest(),"sha256:"+"0".repeat(64)).getBytes(StandardCharsets.UTF_8)));
    }

    @Test void dependencyRequiresExactTypedResolutionBoundToCheckpoint() throws Exception {
        Fixture f=fixture("dependency", "");var dependency=new MigrationProposalSet.Dependency(f.proposal().proposalId(),MigrationProposalSet.DependencyCode.REQUIRES_MANUAL_DECISION,null);
        var set=MigrationProposalSet.create(f.checkpoint().checkpointId(),null,List.of(f.proposal()),List.of(dependency),MigrationProposalSet.Completeness.COMPLETE,List.of());
        var absent=f.service().prepare(new MigrationApplyService.PreparationInput(f.repo(),set,f.ledger(),List.of(f.proposal().proposalId()),List.of(),f.checkpoint(),f.proposal().evidenceRefs(),f.store()));
        assertEquals(MigrationApplyService.PreparationStatus.DEPENDENCY_UNSATISFIED,absent.status());
        String semantic=MigrationApplyService.dependencySemanticRef(dependency);
        var resolution=MigrationDependencyResolution.create(f.proposal().proposalId(),dependency.code(),semantic,MigrationDependencyResolution.ResolutionType.TRUSTED_MANUAL_DECISION,f.checkpoint().checkpointId(),List.of(),null,"trusted-host",List.of());
        var prepared=f.service().prepare(new MigrationApplyService.PreparationInput(f.repo(),set,f.ledger(),List.of(f.proposal().proposalId()),List.of(resolution),f.checkpoint(),f.proposal().evidenceRefs(),f.store()));
        assertEquals(MigrationApplyService.PreparationStatus.PREPARED,prepared.status());
        assertEquals(List.of(resolution.resolutionId()),prepared.plan().dependencyResolutionRefs());
    }

    @Test void applyAndRollbackPreserveUtf8BomCrLfAndSupplementaryUnicode() throws Exception {
        String text="// 😀 before target\r\npackage p;\r\nimport org.openqa.selenium.By;\r\nclass Page { static final By SAVE = By.id(\"old\"); }\r\n";
        byte[] body=text.getBytes(StandardCharsets.UTF_8),bytes=new byte[body.length+3];bytes[0]=(byte)0xef;bytes[1]=(byte)0xbb;bytes[2]=(byte)0xbf;System.arraycopy(body,0,bytes,3,body.length);
        Fixture f=fixtureBytes("bom-crlf",bytes);MigrationApplyPlan plan=f.prepare().plan();var applied=f.service().apply(plan,f.repo(),f.store(),applyAuth(plan),null);
        assertEquals(MigrationApplyResult.Status.APPLIED,applied.status());byte[] changed=Files.readAllBytes(f.source());assertEquals((byte)0xef,changed[0]);assertTrue(new String(changed,3,changed.length-3,StandardCharsets.UTF_8).contains("\r\n"));
        f.service().rollback(applied.transactionId(),f.repo(),f.store(),rollbackAuth(plan,applied.transactionId()));assertArrayEquals(bytes,Files.readAllBytes(f.source()));
    }

    @Test void oneStaleFileInTenFileBatchCausesZeroWrites() throws Exception {
        Batch batch=batch("ten-file",10);MigrationApplyPlan plan=batch.service().prepare(new MigrationApplyService.PreparationInput(batch.repo(),batch.set(),batch.ledger(),batch.proposals().stream().map(MigrationProposal::proposalId).toList(),List.of(),batch.checkpoint(),List.of(),batch.store())).plan();
        byte[] first=Files.readAllBytes(batch.sources().get(0));String stale=Files.readString(batch.sources().get(6))+"// external\n";Files.writeString(batch.sources().get(6),stale,StandardCharsets.UTF_8);
        var result=batch.service().apply(plan,batch.repo(),batch.store(),applyAuth(plan),null);assertEquals(MigrationApplyResult.Status.STALE_PRECONDITION,result.status());
        assertArrayEquals(first,Files.readAllBytes(batch.sources().get(0)));assertEquals(stale,Files.readString(batch.sources().get(6)));assertFalse(Files.readString(batch.sources().get(9)).contains("cssSelector"));
    }

    @Test void verificationStartsOnlyFromAppliedExactSourceAndNeverReapplies() throws Exception {
        Fixture f=fixture("verify-handoff","");MigrationApplyPlan apply=f.prepare().plan();var applied=f.service().apply(apply,f.repo(),f.store(),applyAuth(apply),null);MigrationCheckpoint post=checkpoint(f.store(),applied.postApplyCheckpointRef());byte[] afterApply=Files.readAllBytes(f.source());Path ignored=f.repo().resolve("target/generated/report.txt");Files.createDirectories(ignored.getParent());Files.writeString(ignored,"expected build output");List<MigrationVerificationPlan.Requirement>requirements=MigrationVerificationPlan.requirements(List.of(f.proposal()));MigrationVerificationPlan plan=MigrationVerificationPlan.create(apply,applied.transactionId(),post.checkpointId(),post.sourceState().sourceStateDigest(),requirements,List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),null);var input=new MigrationVerificationService.Input(plan,apply,post,f.repo(),f.store(),java.util.Map.of(),java.util.Map.of(),java.util.Map.of(),MigrationAuthorization.of("host"),MigrationTrustedProcessRunner.Cancellation.NEVER,null);var result=new MigrationVerificationService().verify(input);assertEquals(MigrationVerificationResult.Status.BLOCKED,result.status());assertTrue(result.stageResults().stream().anyMatch(x->x.issues().contains("MISSING_TRUSTED_RUN_BINDING")));assertFalse(result.issues().stream().anyMatch(x->x.startsWith("POST_APPLY_SOURCE_CHANGED")));assertArrayEquals(afterApply,Files.readAllBytes(f.source()));
        MigrationVerificationPlan rerun=MigrationVerificationPlan.create(apply,applied.transactionId(),post.checkpointId(),post.sourceState().sourceStateDigest(),requirements,List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),plan.verificationPlanId());var rerunResult=new MigrationVerificationService().verify(new MigrationVerificationService.Input(rerun,apply,post,f.repo(),f.store(),java.util.Map.of(),java.util.Map.of(),java.util.Map.of(),MigrationAuthorization.of("host"),MigrationTrustedProcessRunner.Cancellation.NEVER,result.verificationResultId()));assertEquals(MigrationVerificationResult.Status.BLOCKED,rerunResult.status());assertArrayEquals(afterApply,Files.readAllBytes(f.source()));assertNotEquals(result.verificationResultId(),rerunResult.verificationResultId());
    }

    @Test void verificationBlocksWhenTouchedSourceChangedAfterApply() throws Exception {
        Fixture f=fixture("verify-stale","");MigrationApplyPlan apply=f.prepare().plan();var applied=f.service().apply(apply,f.repo(),f.store(),applyAuth(apply),null);MigrationCheckpoint post=checkpoint(f.store(),applied.postApplyCheckpointRef());List<MigrationVerificationPlan.Requirement>requirements=MigrationVerificationPlan.requirements(List.of(f.proposal()));MigrationVerificationPlan plan=MigrationVerificationPlan.create(apply,applied.transactionId(),post.checkpointId(),post.sourceState().sourceStateDigest(),requirements,List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),null);Files.writeString(f.source(),Files.readString(f.source())+"// user edit\n",StandardCharsets.UTF_8);var result=new MigrationVerificationService().verify(new MigrationVerificationService.Input(plan,apply,post,f.repo(),f.store(),java.util.Map.of(),java.util.Map.of(),java.util.Map.of(),MigrationAuthorization.of("host"),MigrationTrustedProcessRunner.Cancellation.NEVER,null));assertEquals(MigrationVerificationResult.Status.BLOCKED,result.status());assertTrue(result.issues().stream().anyMatch(x->x.startsWith("POST_APPLY_SOURCE_CHANGED")));
    }

    @Test void trustedNativeCompileBindingRunsWithoutShellThenStopsAtMissingNextBinding() throws Exception {
        Fixture f=fixture("verify-native","");MigrationApplyPlan apply=f.prepare().plan();var applied=f.service().apply(apply,f.repo(),f.store(),applyAuth(apply),null);MigrationCheckpoint post=checkpoint(f.store(),applied.postApplyCheckpointRef());List<MigrationVerificationPlan.Requirement>requirements=MigrationVerificationPlan.requirements(List.of(f.proposal()));var compile=requirements.stream().filter(x->x.stageType()==MigrationVerificationPlan.StageType.COMPILE).findFirst().orElseThrow();Path javaExe=Path.of(System.getProperty("java.home"),"bin",System.getProperty("os.name").toLowerCase().contains("win")?"java.exe":"java");MigrationRunPlan run=MigrationRunPlan.create("trusted-java",List.of("-version"),apply.worktreeBindingRef(),java.util.Map.of(),List.of(),java.time.Duration.ofSeconds(10),new MigrationRunPlan.OutputBounds(1024*1024,1024*1024),List.of(),MigrationRunPlan.ExecutionMode.CUSTOM_TRUSTED,MigrationRunPlan.Scope.CUSTOM_TRUSTED,MigrationRunPlan.SideEffectClassification.UNKNOWN,post.checkpointId(),MigrationRunPlan.BaselineRole.VARIANT);var binding=MigrationVerificationPlan.TrustedRunBinding.create(compile.stage().stageRef(),run.planId(),post.checkpointId(),null,MigrationVerificationPlan.NetworkPolicy.NETWORK_NOT_REQUIRED);MigrationVerificationPlan plan=MigrationVerificationPlan.create(apply,applied.transactionId(),post.checkpointId(),post.sourceState().sourceStateDigest(),requirements,List.of(),List.of(binding),List.of(),List.of(),List.of(),List.of(),null);var local=new MigrationTrustedProcessRunner.LocalExecution("trusted-java",javaExe,f.repo(),apply.worktreeBindingRef(),null);var auth=MigrationAuthorization.of("host",MigrationAuthorization.Capability.EXECUTE_PROJECT_TEST_COMMAND,MigrationAuthorization.Capability.RUN_UNKNOWN_SIDE_EFFECT_TEST);var result=new MigrationVerificationService().verify(new MigrationVerificationService.Input(plan,apply,post,f.repo(),f.store(),java.util.Map.of(run.planId(),run),java.util.Map.of(run.planId(),local),java.util.Map.of(),auth,MigrationTrustedProcessRunner.Cancellation.NEVER,null));assertTrue(result.stageResults().stream().anyMatch(x->x.stageType()==MigrationVerificationPlan.StageType.COMPILE&&x.status()==MigrationVerificationResult.StageStatus.PASSED));assertEquals(MigrationVerificationResult.Status.BLOCKED,result.status());
    }

    private Fixture fixture(String name,String prefix)throws Exception{
        String java=prefix+"package p;\nimport org.openqa.selenium.By;\nclass Page { static final By SAVE = By.id(\"old\"); }\n";return fixtureBytes(name,java.getBytes(StandardCharsets.UTF_8));
    }
    private Fixture fixtureBytes(String name,byte[]sourceBytes)throws Exception{
        Path repo=temporary.resolve(name);Files.createDirectory(repo);init(repo);Files.writeString(repo.resolve(".gitignore"),"target/\n",StandardCharsets.UTF_8);Path source=repo.resolve("src/p/Page.java");Files.createDirectories(source.getParent());Files.write(source,sourceBytes);git(repo,"add","--all");git(repo,"commit","-m","base");
        MigrationGitService gitService=new MigrationGitService();var inspected=gitService.inspect(repo);Path state=temporary.resolve(name+"-state");MigrationStateStore store=MigrationStateStore.resolve(gitService,inspected,state);
        var config=MigrationCheckpoint.Configuration.create(MigrationIsolationPlan.Choice.USE_CURRENT_WORKTREE,MigrationCheckpoint.IntendedMode.MIGRATION_REVIEW,store.storage());MigrationCheckpoint checkpoint=MigrationCheckpoint.create(inspected.preflight(),inspected.sourceState(),config,List.of(),null,null);store.write(checkpoint);
        var declaration=TrustedSelectorSourceProjection.scan(repo,List.of(Path.of("src")),List.of()).declarations().get(0);
        var candidate=new TrustedCandidateMaterial(declaration.declarationRef(),"candidate","css selector","[data-testid=\"save\"]","trusted-analysis",TrustedCandidateMaterial.ValidationState.VERIFIED_IN_SCOPE,TrustedCandidateMaterial.TargetComparison.SAME_TARGET,TrustedCandidateMaterial.Intent.FIND_ONE,1,true,false,false,List.of(),"sha256:"+"a".repeat(64));
        var input=new MigrationProposalEngine.Input(repo,declaration,null,candidate,List.of(),inspected.preflight().repositoryBindingRef(),inspected.preflight().worktreeBindingRef(),checkpoint.checkpointId());MigrationProposal proposal=new MigrationProposalEngine().planLocator(input);
        assertEquals(MigrationProposal.Eligibility.READY_FOR_REVIEW,proposal.eligibility());MigrationProposalSet set=MigrationProposalSet.create(checkpoint.checkpointId(),null,List.of(proposal),List.of(),MigrationProposalSet.Completeness.COMPLETE,List.of());
        var decision=MigrationDecisionLedger.decide(proposal,checkpoint.checkpointId(),MigrationDecisionLedger.Decision.APPROVE_FOR_S11,"trusted-host",null,null);MigrationDecisionLedger ledger=MigrationDecisionLedger.create(List.of(decision));
        return new Fixture(repo,source,store,checkpoint,proposal,set,ledger,new MigrationApplyService());
    }
    private Batch batch(String name,int count)throws Exception{Path repo=temporary.resolve(name);Files.createDirectory(repo);init(repo);List<Path>sources=new java.util.ArrayList<>();for(int i=0;i<count;i++){Path p=repo.resolve("src/p/Page"+i+".java");Files.createDirectories(p.getParent());Files.writeString(p,"package p; import org.openqa.selenium.By; class Page"+i+" { static final By SAVE = By.id(\"old-"+i+"\"); }\n",StandardCharsets.UTF_8);sources.add(p);}git(repo,"add","--all");git(repo,"commit","-m","base");MigrationGitService gs=new MigrationGitService();var inspected=gs.inspect(repo);MigrationStateStore store=MigrationStateStore.resolve(gs,inspected,temporary.resolve(name+"-state"));var config=MigrationCheckpoint.Configuration.create(MigrationIsolationPlan.Choice.USE_CURRENT_WORKTREE,MigrationCheckpoint.IntendedMode.MIGRATION_REVIEW,store.storage());var checkpoint=MigrationCheckpoint.create(inspected.preflight(),inspected.sourceState(),config,List.of(),null,null);store.write(checkpoint);List<MigrationProposal>proposals=new java.util.ArrayList<>();for(var d:TrustedSelectorSourceProjection.scan(repo,List.of(Path.of("src")),List.of()).declarations()){var candidate=new TrustedCandidateMaterial(d.declarationRef(),"candidate","css selector","[data-i=\""+proposals.size()+"\"]","trusted-analysis",TrustedCandidateMaterial.ValidationState.VERIFIED_IN_SCOPE,TrustedCandidateMaterial.TargetComparison.SAME_TARGET,TrustedCandidateMaterial.Intent.FIND_ONE,1,true,false,false,List.of(),"sha256:"+"a".repeat(64));proposals.add(new MigrationProposalEngine().planLocator(new MigrationProposalEngine.Input(repo,d,null,candidate,List.of(),inspected.preflight().repositoryBindingRef(),inspected.preflight().worktreeBindingRef(),checkpoint.checkpointId())));}var set=MigrationProposalSet.create(checkpoint.checkpointId(),null,proposals,List.of(),MigrationProposalSet.Completeness.COMPLETE,List.of());var ledger=MigrationDecisionLedger.create(proposals.stream().map(p->MigrationDecisionLedger.decide(p,checkpoint.checkpointId(),MigrationDecisionLedger.Decision.APPROVE_FOR_S11,"host",null,null)).toList());return new Batch(repo,sources,store,checkpoint,proposals,set,ledger,new MigrationApplyService());}
    private static void init(Path repo)throws Exception{git(repo,"init");git(repo,"config","user.email","test@example.invalid");git(repo,"config","user.name","Test");}
    private static MigrationTargetedAuthorization applyAuth(MigrationApplyPlan p){return MigrationTargetedAuthorization.of(p.applyPlanId(),p.repositoryBindingRef(),p.worktreeBindingRef(),"test-host",MigrationTargetedAuthorization.Capability.APPLY_APPROVED_SOURCE_CHANGES);}
    private static MigrationTargetedAuthorization rollbackAuth(MigrationApplyPlan p,String tx){return MigrationTargetedAuthorization.of(tx,p.repositoryBindingRef(),p.worktreeBindingRef(),"test-host",MigrationTargetedAuthorization.Capability.ROLLBACK_TOOL_CHANGES);}
    private static MigrationCheckpoint checkpoint(MigrationStateStore store,String id)throws Exception{return store.read(store.root().resolve("checkpoints/"+id.replace(':','-')+".json"));}
    private static void git(Path repo,String...args)throws Exception{var command=new java.util.ArrayList<String>();command.add("git");command.addAll(List.of(args));Process p=new ProcessBuilder(command).directory(repo.toFile()).redirectErrorStream(true).start();String out=new String(p.getInputStream().readAllBytes(),StandardCharsets.UTF_8);assertEquals(0,p.waitFor(),out);}
    private record Fixture(Path repo,Path source,MigrationStateStore store,MigrationCheckpoint checkpoint,MigrationProposal proposal,MigrationProposalSet set,MigrationDecisionLedger ledger,MigrationApplyService service){
        MigrationApplyService.PreparationResult prepare()throws IOException{return service.prepare(new MigrationApplyService.PreparationInput(repo,set,ledger,List.of(proposal.proposalId()),List.of(),checkpoint,proposal.evidenceRefs(),store));}
    }
    private record Batch(Path repo,List<Path>sources,MigrationStateStore store,MigrationCheckpoint checkpoint,List<MigrationProposal>proposals,MigrationProposalSet set,MigrationDecisionLedger ledger,MigrationApplyService service){}
}
