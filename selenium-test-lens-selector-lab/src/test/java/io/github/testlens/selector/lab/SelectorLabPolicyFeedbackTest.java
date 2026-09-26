package io.github.testlens.selector.lab;

import io.github.testlens.selector.engine.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SelectorLabPolicyFeedbackTest {
    @Test void useOnceIsAnalysisLocalAndNeverChangesCandidate(){
        SelectorLabPolicyFeedback feedback=new SelectorLabPolicyFeedback(workspace());feedback.beginAnalysis("a");
        CandidateAnalysis.Candidate candidate=candidate(CandidateAnalysis.ValidationState.VERIFIED_IN_SCOPE);
        SessionCandidateFeedback accepted=feedback.useOnce("a",candidate);
        assertEquals(SessionCandidateFeedback.State.USER_ACCEPTED_ONCE,accepted.state());
        assertEquals(CandidateAnalysis.ValidationState.VERIFIED_IN_SCOPE,candidate.validation().state());
        feedback.beginAnalysis("b");assertNull(feedback.useOnce());
        assertThrows(IllegalArgumentException.class,()->feedback.useOnce("b",candidate(CandidateAnalysis.ValidationState.WRONG_TARGET)));
    }
    @Test void transferProducesCapabilityFreePendingAndClearsActiveDraft(){
        SelectorLabPolicyFeedback feedback=new SelectorLabPolicyFeedback(workspace());feedback.beginAnalysis("a");
        PolicyDraftService.Preparation prepared=feedback.prepareExact("a",candidate(CandidateAnalysis.ValidationState.VERIFIED_IN_SCOPE),SelectorSubject.trusted("id","secret"),SelectorPolicy.Decision.STABLE,PolicyDraft.ScopeChoice.PROJECT,PolicyWorkspaceSnapshot.Origin.LOCAL,ObservationEvidence.unavailable());
        PolicyDraft.PendingPolicyChange pending=feedback.transfer(prepared.draft().draftId());
        assertTrue(pending.hostApprovalRequired());assertNull(feedback.activeDraft());assertFalse(pending.toString().contains("secret"));
    }
    private static PolicyWorkspaceSnapshot workspace(){return PolicyWorkspaceSnapshot.create(PolicyWorkspaceSnapshot.OriginDocument.absent(PolicyWorkspaceSnapshot.Origin.TRACKED),PolicyWorkspaceSnapshot.OriginDocument.absent(PolicyWorkspaceSnapshot.Origin.LOCAL),null);}
    private static CandidateAnalysis.Candidate candidate(CandidateAnalysis.ValidationState state){return new CandidateAnalysis.Candidate("c",new CandidateAnalysis.Locator("id","secret"),false,List.of(CandidateAnalysis.Origin.ID),List.of(),new CandidateAnalysis.Validation(state,1,CandidateAnalysis.TargetComparison.SAME_TARGET,List.of()),new CandidateAnalysis.Complexity(CandidateAnalysis.ScopeFragility.DIRECT,CandidateAnalysis.SemanticPreference.ID,0,0,6,1),List.of(),List.of(),true);}
}
