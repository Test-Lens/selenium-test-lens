package io.github.testlens.selector.tooling;

import io.github.testlens.selector.engine.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SelectorPolicyDraftApplierTest {
    @TempDir Path root;

    @Test void browserPreparationChangesNoBytesAndTrustedApplyIsTheOnlyWrite() throws Exception {
        SelectorPolicyWorkspace loader=new SelectorPolicyWorkspace();PolicyWorkspaceSnapshot before=loader.load(root,"project");
        PolicyDraft draft=draft(before,"alpha",PolicyWorkspaceSnapshot.Origin.TRACKED);
        assertFalse(Files.exists(root.resolve(SelectorPolicyWorkspace.TRACKED)));
        PolicyDraft.PendingPolicyChange pending=PolicyDraft.PendingPolicyChange.transfer(draft,"prepared, not saved");
        assertFalse(Files.exists(root.resolve(SelectorPolicyWorkspace.TRACKED)));
        SelectorPolicyDraftApplier.ApplyResult result=new SelectorPolicyDraftApplier().apply(new SelectorPolicyDraftApplier.TrustedPolicyWorkspace(root,"project"),pending,Set.of());
        assertEquals(SelectorPolicyDraftApplier.Status.APPLIED,result.status());assertTrue(Files.exists(root.resolve(SelectorPolicyWorkspace.TRACKED)));
        assertNotNull(loader.load(root,"project").find(draft.proposedRule().ruleId()));
    }

    @Test void changingNonTargetLocalFileInvalidatesTrackedDraft() throws Exception {
        SelectorPolicyWorkspace loader=new SelectorPolicyWorkspace();PolicyWorkspaceSnapshot before=loader.load(root,"project");
        PolicyDraft draft=draft(before,"tracked",PolicyWorkspaceSnapshot.Origin.TRACKED);
        SelectorPolicy.Rule local=rule("local");SelectorPolicyJson.writeAtomic(new SelectorPolicy.Document(1,1,List.of(local)),root,SelectorPolicyWorkspace.LOCAL);
        SelectorPolicyDraftApplier.ApplyResult result=new SelectorPolicyDraftApplier().apply(new SelectorPolicyDraftApplier.TrustedPolicyWorkspace(root,"project"),PolicyDraft.PendingPolicyChange.transfer(draft,"pending"),Set.of());
        assertEquals(SelectorPolicyDraftApplier.Status.STALE_DRAFT,result.status());assertFalse(Files.exists(root.resolve(SelectorPolicyWorkspace.TRACKED)));
    }

    @Test void typedHostAcknowledgementIsRequiredAndBrowserCannotSupplyIt() throws Exception {
        PolicyWorkspaceSnapshot before=new SelectorPolicyWorkspace().load(root,"project");PolicyDraft base=draft(before,"ack",PolicyWorkspaceSnapshot.Origin.LOCAL);
        PolicyDraft guarded=PolicyDraft.create(base.action(),base.destination(),base.proposedRule(),base.oldRuleId(),base.source(),base.preview(),
                List.of(PolicyDraft.Warning.PREVIEW_INCOMPLETE),List.of(),Set.of(PolicyDraft.HostAcknowledgement.PREVIEW_INCOMPLETE),base.securityClassification(),base.workspacePreconditions());
        PolicyDraft.PendingPolicyChange pending=PolicyDraft.PendingPolicyChange.transfer(guarded,"pending");SelectorPolicyDraftApplier applier=new SelectorPolicyDraftApplier();
        assertEquals(SelectorPolicyDraftApplier.Status.ACKNOWLEDGEMENT_REQUIRED,applier.apply(new SelectorPolicyDraftApplier.TrustedPolicyWorkspace(root,"project"),pending,Set.of()).status());
        assertFalse(Files.exists(root.resolve(SelectorPolicyWorkspace.LOCAL)));
        assertEquals(SelectorPolicyDraftApplier.Status.APPLIED,applier.apply(new SelectorPolicyDraftApplier.TrustedPolicyWorkspace(root,"project"),pending,Set.of(PolicyDraft.HostAcknowledgement.PREVIEW_INCOMPLETE)).status());
    }

    private static PolicyDraft draft(PolicyWorkspaceSnapshot workspace,String value,PolicyWorkspaceSnapshot.Origin destination){SelectorPolicy.Rule rule=rule(value);PolicyDraft.Preview preview=new PolicyDraft.Preview(PolicyDraft.ScopeChoice.PROJECT,List.of(),List.of(),List.of(),1,0,1,0,true,List.of());return PolicyDraft.create(PolicyDraft.Action.ADD,destination,rule,null,new PolicyDraft.Source(null,"candidate",null,null),preview,List.of(),List.of(),Set.of(),PolicyDraft.SecurityClassification.DIGEST_ONLY,PolicyDraft.WorkspacePreconditions.from(workspace,Set.of(),Set.of(rule.ruleId())));}
    private static SelectorPolicy.Rule rule(String value){return SelectorPolicy.Rule.create(SelectorPolicy.Decision.STABLE,0,SelectorPolicy.Scope.project(),SelectorPolicy.ExactMatcher.from(SelectorSubject.trusted("id",value)),new SelectorPolicy.Reason("USER_MARKED_EXACT_STABLE",null));}
}
