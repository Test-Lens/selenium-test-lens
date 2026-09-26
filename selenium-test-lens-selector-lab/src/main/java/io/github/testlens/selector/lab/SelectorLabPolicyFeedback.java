package io.github.testlens.selector.lab;

import io.github.testlens.selector.engine.*;

import java.util.Objects;

/** Browser-safe preparation state. This class has no filesystem capability. */
public final class SelectorLabPolicyFeedback {
    private final PolicyWorkspaceSnapshot workspace;
    private final PolicyDraftService drafts = new PolicyDraftService();
    private String analysisId;
    private SessionCandidateFeedback useOnce;
    private PolicyDraft activeDraft;

    public SelectorLabPolicyFeedback(PolicyWorkspaceSnapshot workspace) { this.workspace=Objects.requireNonNull(workspace); }

    public void beginAnalysis(String analysisId) { this.analysisId=Objects.requireNonNull(analysisId);useOnce=null;activeDraft=null; }
    public SessionCandidateFeedback useOnce(String requestedAnalysisId, CandidateAnalysis.Candidate candidate) {
        requireAnalysis(requestedAnalysisId);
        CandidateAnalysis.ValidationState state=candidate.validation().state();
        if(state!=CandidateAnalysis.ValidationState.VERIFIED_IN_SCOPE&&state!=CandidateAnalysis.ValidationState.VALID_FOR_INTENT&&state!=CandidateAnalysis.ValidationState.VALID_BUT_AMBIGUOUS)
            throw new IllegalArgumentException("USE_ONCE_REJECTED_FOR_"+state);
        useOnce=new SessionCandidateFeedback(analysisId,candidate.candidateId(),SessionCandidateFeedback.State.USER_ACCEPTED_ONCE,state==CandidateAnalysis.ValidationState.VALID_BUT_AMBIGUOUS);
        return useOnce;
    }
    public PolicyDraftService.Preparation prepareExact(String requestedAnalysisId,CandidateAnalysis.Candidate candidate,
                                                       SelectorSubject subject,SelectorPolicy.Decision decision,
                                                       PolicyDraft.ScopeChoice scope,PolicyWorkspaceSnapshot.Origin destination,
                                                       ObservationEvidence evidence){
        requireAnalysis(requestedAnalysisId);PolicyDraftService.Preparation value=drafts.prepareExact(workspace,candidate,subject,decision,scope,destination,evidence);
        activeDraft=value.prepared()?value.draft():null;return value;
    }
    public PolicyDraftService.Preparation preparePattern(String requestedAnalysisId,CandidateAnalysis.Candidate candidate,
                                                         SelectorSubject subject,SelectorPolicy.Decision decision,
                                                         PolicyDraft.ScopeChoice scope,PolicyWorkspaceSnapshot.Origin destination,
                                                         PatternProposal.Result proposal,FederatedPatternPreview.Result preview,
                                                         ObservationEvidence evidence,String proposalId){
        requireAnalysis(requestedAnalysisId);PolicyDraftService.Preparation value=drafts.preparePattern(workspace,proposal,subject,decision,scope,destination,preview,evidence,candidate.candidateId(),proposalId);
        activeDraft=value.prepared()?value.draft():null;return value;
    }
    public PolicyDraft.PendingPolicyChange transfer(String draftId){
        if(activeDraft==null||!activeDraft.draftId().equals(draftId))throw new IllegalArgumentException("STALE_DRAFT_ID");
        PolicyDraft.PendingPolicyChange pending=PolicyDraft.PendingPolicyChange.transfer(activeDraft,"Policy change prepared; trusted host approval required");activeDraft=null;return pending;
    }
    public PolicyDraftService.Preparation prepareRemove(String ruleId){
        PolicyDraftService.Preparation value=drafts.prepareRemove(workspace,ruleId);activeDraft=value.prepared()?value.draft():null;return value;
    }
    public PolicyDraftService.Preparation prepareReplace(String ruleId,SelectorPolicy.Decision decision,SelectorPolicy.Scope replacementScope){
        PolicyDraftService.Preparation value=drafts.prepareReplace(workspace,ruleId,decision,replacementScope);activeDraft=value.prepared()?value.draft():null;return value;
    }
    public void cancelDraft(){activeDraft=null;}
    public void clear(){analysisId=null;useOnce=null;activeDraft=null;}
    public SessionCandidateFeedback useOnce(){return useOnce;} public PolicyDraft activeDraft(){return activeDraft;}
    private void requireAnalysis(String requested){if(analysisId==null||!analysisId.equals(requested))throw new IllegalArgumentException("STALE_ANALYSIS_ID");}
}
