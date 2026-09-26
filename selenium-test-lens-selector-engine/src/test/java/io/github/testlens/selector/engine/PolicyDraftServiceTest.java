package io.github.testlens.selector.engine;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PolicyDraftServiceTest {
    @Test void workspaceDigestCoversBothOriginsAndRawFileDigestIsSeparate(){
        SelectorPolicy.Rule rule=rule("one");
        PolicyWorkspaceSnapshot empty=workspace(List.of(),List.of());
        PolicyWorkspaceSnapshot local=workspace(List.of(),List.of(rule));
        assertNotEquals(empty.effectiveWorkspaceDigest(),local.effectiveWorkspaceDigest());
        assertNotEquals(PolicyWorkspaceSnapshot.rawFileDigest("x".getBytes()),PolicyWorkspaceSnapshot.semanticDigest(List.of()));
    }

    @Test void exactDraftContainsDigestOnlyAndDuplicateAcrossOriginBlocksTransfer(){
        SelectorSubject subject=SelectorSubject.trusted("id","secret-low-entropy");
        CandidateAnalysis.Candidate candidate=candidate(CandidateAnalysis.ValidationState.VERIFIED_IN_SCOPE);
        PolicyDraftService service=new PolicyDraftService();
        PolicyDraftService.Preparation prepared=service.prepareExact(workspace(List.of(),List.of()),candidate,subject,
                SelectorPolicy.Decision.STABLE,PolicyDraft.ScopeChoice.PROJECT,PolicyWorkspaceSnapshot.Origin.TRACKED,ObservationEvidence.unavailable());
        assertTrue(prepared.prepared());assertTrue(prepared.draft().transferable());
        assertFalse(prepared.draft().toString().contains(subject.canonicalValue()));
        PolicyWorkspaceSnapshot duplicate=workspace(List.of(),List.of(prepared.draft().proposedRule()));
        PolicyDraftService.Preparation second=service.prepareExact(duplicate,candidate,subject,SelectorPolicy.Decision.STABLE,
                PolicyDraft.ScopeChoice.PROJECT,PolicyWorkspaceSnapshot.Origin.TRACKED,ObservationEvidence.unavailable());
        assertTrue(second.draft().blockingIssues().contains(PolicyDraft.BlockingIssue.ALREADY_EXISTS));
        assertThrows(IllegalArgumentException.class,()->PolicyDraft.PendingPolicyChange.transfer(second.draft(),"blocked"));
    }

    @Test void stableDraftRejectsWrongTargetAndAmbiguousRequiresHostAcknowledgement(){
        PolicyDraftService service=new PolicyDraftService();SelectorSubject subject=SelectorSubject.trusted("id","x");
        assertFalse(service.prepareExact(workspace(List.of(),List.of()),candidate(CandidateAnalysis.ValidationState.WRONG_TARGET),subject,
                SelectorPolicy.Decision.STABLE,PolicyDraft.ScopeChoice.PROJECT,PolicyWorkspaceSnapshot.Origin.LOCAL,ObservationEvidence.unavailable()).prepared());
        PolicyDraft ambiguous=service.prepareExact(workspace(List.of(),List.of()),candidate(CandidateAnalysis.ValidationState.VALID_BUT_AMBIGUOUS),subject,
                SelectorPolicy.Decision.STABLE,PolicyDraft.ScopeChoice.PROJECT,PolicyWorkspaceSnapshot.Origin.LOCAL,ObservationEvidence.unavailable()).draft();
        assertTrue(ambiguous.requiredHostAcknowledgements().contains(PolicyDraft.HostAcknowledgement.AMBIGUOUS_LIVE_VALIDATION));
    }

    private static PolicyWorkspaceSnapshot workspace(List<SelectorPolicy.Rule> tracked,List<SelectorPolicy.Rule> local){
        return PolicyWorkspaceSnapshot.create(origin(PolicyWorkspaceSnapshot.Origin.TRACKED,tracked),origin(PolicyWorkspaceSnapshot.Origin.LOCAL,local),"project-v1");
    }
    private static PolicyWorkspaceSnapshot.OriginDocument origin(PolicyWorkspaceSnapshot.Origin origin,List<SelectorPolicy.Rule> rules){
        if(rules.isEmpty())return PolicyWorkspaceSnapshot.OriginDocument.absent(origin);
        return new PolicyWorkspaceSnapshot.OriginDocument(origin,PolicyWorkspaceSnapshot.FileState.EXPECTED_PRESENT,
                PolicyWorkspaceSnapshot.rawFileDigest(origin.name().getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                PolicyWorkspaceSnapshot.semanticDigest(rules),rules);
    }
    private static SelectorPolicy.Rule rule(String value){return SelectorPolicy.Rule.create(SelectorPolicy.Decision.STABLE,0,SelectorPolicy.Scope.project(),SelectorPolicy.ExactMatcher.from(SelectorSubject.trusted("id",value)),new SelectorPolicy.Reason("USER_MARKED_EXACT_STABLE",null));}
    private static CandidateAnalysis.Candidate candidate(CandidateAnalysis.ValidationState state){return new CandidateAnalysis.Candidate("candidate",new CandidateAnalysis.Locator("id","secret-low-entropy"),false,List.of(CandidateAnalysis.Origin.ID),List.of(),new CandidateAnalysis.Validation(state,1,CandidateAnalysis.TargetComparison.SAME_TARGET,List.of()),new CandidateAnalysis.Complexity(CandidateAnalysis.ScopeFragility.DIRECT,CandidateAnalysis.SemanticPreference.ID,0,0,5,1),List.of(),List.of(),true);}
}
