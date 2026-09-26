package io.github.testlens.selector.engine;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SelectorEnginePerformanceHarnessTest {
    @Test
    @EnabledIfSystemProperty(named="testlens.selector.engine.performance",matches="true")
    void measuresOfflineScalingWithoutCiThresholds(){
        for(int subjects:List.of(100,1_000,10_000))for(int rules:List.of(10,100,1_000))measure(subjects,rules);
    }

    private static void measure(int subjectCount,int ruleCount){
        List<SelectorSubject> subjects=new ArrayList<>();
        for(int i=0;i<subjectCount;i++)subjects.add(SelectorSubject.trusted("id","item-"+i));
        List<SelectorPolicy.Rule> rules=new ArrayList<>();
        for(int i=0;i<ruleCount;i++){
            SelectorSubject s=SelectorSubject.trusted("id","item-"+(i%subjectCount));
            rules.add(SelectorPolicy.Rule.create(i%2==0?SelectorPolicy.Decision.STABLE:SelectorPolicy.Decision.UNSTABLE,i%7,
                    SelectorPolicy.Scope.project(),SelectorPolicy.ExactMatcher.from(s),new SelectorPolicy.Reason("PERF",null)));
        }
        long beforeMemory=usedMemory(),loadStart=System.nanoTime();
        CompiledPolicySet compiled=CompiledPolicySet.compile(new SelectorPolicy.Document(1,1,rules));
        long loadNanos=System.nanoTime()-loadStart,evaluateStart=System.nanoTime();SelectorStabilityEngine engine=new SelectorStabilityEngine();
        List<StabilityAssessment> assessments=subjects.stream().map(s->engine.analyze(s,ObservationEvidence.unavailable(),compiled)).toList();
        long evaluateNanos=System.nanoTime()-evaluateStart,previewStart=System.nanoTime();engine.preview(subjects,ObservationEvidence.unavailable(),compiled);long previewNanos=System.nanoTime()-previewStart;
        long memory=Math.max(0,usedMemory()-beforeMemory);assertEquals(subjectCount,assessments.size());
        System.out.printf("selector-engine subjects=%d rules=%d loadMs=%.3f evalMs=%.3f previewMs=%.3f approxBytes=%d throughput=%.1f/s%n",
                subjectCount,ruleCount,loadNanos/1_000_000d,evaluateNanos/1_000_000d,previewNanos/1_000_000d,memory,subjectCount/(evaluateNanos/1_000_000_000d));
    }
    private static long usedMemory(){Runtime r=Runtime.getRuntime();return r.totalMemory()-r.freeMemory();}
}
