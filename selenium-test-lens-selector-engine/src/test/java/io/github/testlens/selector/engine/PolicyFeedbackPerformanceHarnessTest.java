package io.github.testlens.selector.engine;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicyFeedbackPerformanceHarnessTest {
    @Test
    @EnabledIfSystemProperty(named="testlens.selector.policy.performance",matches="true")
    void measuresDraftAndPatternPreviewScalingWithoutCiThresholds(){
        System.out.printf(Locale.ROOT,"POLICY_PREVIEW_ENV java=%s os=%s processors=%d maxHeapMiB=%d warmups=1 repetitions=3 measured=pattern-preview%n",System.getProperty("java.version"),System.getProperty("os.name"),Runtime.getRuntime().availableProcessors(),Runtime.getRuntime().maxMemory()/1048576);
        measure(1_000,1);measure(10_000,10);measure(25_000,100);measure(100_000,100);
    }

    private static void measure(int subjectCount,int ruleCount){
        SelectorSubject source=subject("550e8400-e29b-41d4-a716-446655440000","source");
        PatternProposal.Result proposal=new PatternProposal().propose(source);
        List<SimilaritySubject> catalog=new ArrayList<>(subjectCount);
        ComponentIdentity component=ComponentIdentity.of("TARGET","primary");
        SimilaritySubject.Correlation correlation=new SimilaritySubject.Correlation(SimilaritySubject.Confidence.EXACT,List.of("PERF"),List.of(),List.of(),1);
        for(int i=0;i<subjectCount;i++){
            SelectorSubject value=subject(String.format("550e8400-e29b-41d4-a716-%012x",i),"decl-"+i);
            catalog.add(new SimilaritySubject(value,"subject-"+i,component,null,null,List.of(),null,List.of(),List.of(),null,null,List.of(),correlation,SimilaritySubject.SourceState.CURRENT));
        }
        List<SelectorPolicy.Rule> policyRules=new ArrayList<>(ruleCount);
        for(int i=0;i<ruleCount;i++)policyRules.add(SelectorPolicy.Rule.create(SelectorPolicy.Decision.STABLE,0,SelectorPolicy.Scope.project(),SelectorPolicy.ExactMatcher.from(subject("rule-"+i,"rule-"+i)),new SelectorPolicy.Reason("PERF",null)));
        PolicyWorkspaceSnapshot workspace=workspace(policyRules);
        CandidateAnalysis.Candidate candidate=new CandidateAnalysis.Candidate("candidate",new CandidateAnalysis.Locator("id",source.canonicalValue()),false,List.of(CandidateAnalysis.Origin.ID),List.of(),new CandidateAnalysis.Validation(CandidateAnalysis.ValidationState.VERIFIED_IN_SCOPE,1,CandidateAnalysis.TargetComparison.SAME_TARGET,List.of()),new CandidateAnalysis.Complexity(CandidateAnalysis.ScopeFragility.DIRECT,CandidateAnalysis.SemanticPreference.ID,0,0,36,1),List.of(),List.of(),true);
        long exactStart=System.nanoTime();
        PolicyDraftService.Preparation exact=new PolicyDraftService().prepareExact(workspace,candidate,source,SelectorPolicy.Decision.STABLE,PolicyDraft.ScopeChoice.PROJECT,PolicyWorkspaceSnapshot.Origin.LOCAL,ObservationEvidence.unavailable());
        long exactNanos=System.nanoTime()-exactStart;
        FederatedPatternPreview previewEngine=new FederatedPatternPreview();previewEngine.preview(proposal,catalog,workspace.compiled(),Map.of(),false);
        long[]times=new long[3];FederatedPatternPreview.Result preview=null;long memoryBefore=used();for(int repetition=0;repetition<times.length;repetition++){long previewStart=System.nanoTime();preview=previewEngine.preview(proposal,catalog,workspace.compiled(),Map.of(),false);times[repetition]=System.nanoTime()-previewStart;}java.util.Arrays.sort(times);long previewNanos=times[1];
        assertTrue(exact.prepared());assertTrue(preview.matchedStaticSubjects().size()>0);
        System.out.printf(Locale.ROOT,"POLICY_PREVIEW subjects=%d rules=%d exactDraftMs=%.1f previewMedianMs=%.1f previewRangeMs=%.1f..%.1f matches=%d usages=%d heapDeltaMiB=%.1f%n",subjectCount,ruleCount,exactNanos/1_000_000d,previewNanos/1_000_000d,times[0]/1_000_000d,times[2]/1_000_000d,preview.matchedStaticSubjects().size(),preview.usageCount(),Math.max(0,used()-memoryBefore)/1048576d);
    }

    private static SelectorSubject subject(String value,String declaration){return new SelectorSubject(SelectorSubject.SubjectKind.STATIC_DECLARATION,"id",SelectorSubject.ValueState.KNOWN,value,declaration,"module","src/Page.java","Page#field",null,null,null,SelectorSubject.InputTrust.SOURCE_CANONICAL,null);}
    private static PolicyWorkspaceSnapshot workspace(List<SelectorPolicy.Rule> rules){if(rules.isEmpty())return PolicyWorkspaceSnapshot.create(PolicyWorkspaceSnapshot.OriginDocument.absent(PolicyWorkspaceSnapshot.Origin.TRACKED),PolicyWorkspaceSnapshot.OriginDocument.absent(PolicyWorkspaceSnapshot.Origin.LOCAL),null);byte[] bytes=rules.stream().map(SelectorPolicy.Rule::ruleId).sorted().reduce("",String::concat).getBytes(StandardCharsets.UTF_8);PolicyWorkspaceSnapshot.OriginDocument tracked=new PolicyWorkspaceSnapshot.OriginDocument(PolicyWorkspaceSnapshot.Origin.TRACKED,PolicyWorkspaceSnapshot.FileState.EXPECTED_PRESENT,PolicyWorkspaceSnapshot.rawFileDigest(bytes),PolicyWorkspaceSnapshot.semanticDigest(rules),rules);return PolicyWorkspaceSnapshot.create(tracked,PolicyWorkspaceSnapshot.OriginDocument.absent(PolicyWorkspaceSnapshot.Origin.LOCAL),null);}
    private static long used(){Runtime runtime=Runtime.getRuntime();return runtime.totalMemory()-runtime.freeMemory();}
}
