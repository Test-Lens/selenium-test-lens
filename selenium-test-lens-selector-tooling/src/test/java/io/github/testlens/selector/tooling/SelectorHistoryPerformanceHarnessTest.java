package io.github.testlens.selector.tooling;

import io.github.testlens.selector.engine.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.github.testlens.selector.tooling.SelectorHistoryModel.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Opt-in, offline measurements only. There are deliberately no CI timing assertions. */
class SelectorHistoryPerformanceHarnessTest {
    @TempDir Path temporary;

    @Test
    @EnabledIfSystemProperty(named="selector.history.perf",matches="true")
    void measuresSimilarityHistoryAndPolicyPreviewScaling() throws Exception {
        Path output=Path.of(System.getProperty("selector.history.perf.output",
                "target/selector-history-performance/metrics.csv")).toAbsolutePath().normalize();
        Files.createDirectories(output.getParent());
        List<String> rows=new ArrayList<>();
        rows.add("kind,size,secondarySize,buildNanos,queryOrImportNanos,serializeOrPreviewNanos,bytes,heapDeltaBytes");
        for(int size:List.of(100,1_000,10_000,100_000))measureSimilarity(size,rows);
        for(int buckets:List.of(1_000,10_000,100_000))measureHistory(buckets,rows);
        for(int rules:List.of(10,100,1_000))measurePolicies(rules,rows);
        Files.write(output,rows,StandardCharsets.UTF_8);
    }

    private static void measureSimilarity(int count,List<String> rows){
        long memoryBefore=usedMemory(),buildStart=System.nanoTime();List<SimilaritySubject>subjects=new ArrayList<>(count);
        for(int i=0;i<count;i++)subjects.add(similaritySubject(i));
        SimilarityIndex index=new SimilarityIndex(subjects);long build=System.nanoTime()-buildStart,queryStart=System.nanoTime();
        SimilarityResult result=index.find(subjects.get(count/2),SimilarityResult.QueryScope.PROJECT,CompiledPolicySet.empty(),Map.of());
        long query=System.nanoTime()-queryStart;assertFalse(result.relatedSubjects().isEmpty());
        rows.add(row("similarity",count,0,build,query,0,0,Math.max(0,usedMemory()-memoryBefore)));
    }

    private void measureHistory(int requestedBuckets,List<String> rows)throws Exception{
        int observationsPerSubject=4,subjectCount=requestedBuckets/observationsPerSubject;
        Limits limits=new Limits(100,25_000,100_000,64L*1024*1024,32,16,8);
        SelectorHistoryAggregator aggregate=new SelectorHistoryAggregator(limits);List<RunDescriptor>runs=List.of(run("a"),run("b"),run("c"),run("d"));
        long memoryBefore=usedMemory(),importStart=System.nanoTime();
        for(int subject=0;subject<subjectCount;subject++)for(int r=0;r<observationsPerSubject;r++)aggregate.add(observation(subject,runs.get(r)));
        long imported=System.nanoTime()-importStart,snapshotStart=System.nanoTime();History snapshot=aggregate.snapshot("performance-project");long snapshotNanos=System.nanoTime()-snapshotStart;
        Path project=Files.createDirectory(temporary.resolve("history-"+requestedBuckets));Files.createDirectories(project.resolve("target/test-lens/selector-history"));
        long serializeStart=System.nanoTime();SelectorHistoryJson.writeLocal(aggregate,"performance-project",project,Path.of("target/test-lens/selector-history/history-v1.json"));long serialize=System.nanoTime()-serializeStart;
        long bytes=Files.size(project.resolve("target/test-lens/selector-history/history-v1.json"));
        rows.add(row("history",requestedBuckets,snapshot.subjects().size(),snapshotNanos,imported,serialize,bytes,Math.max(0,usedMemory()-memoryBefore)));
    }

    private static void measurePolicies(int ruleCount,List<String> rows){
        List<SelectorPolicy.Rule>rules=new ArrayList<>(ruleCount);for(int i=0;i<ruleCount;i++){SelectorSubject subject=SelectorSubject.trusted("id","policy-"+i);rules.add(SelectorPolicy.Rule.create(i%2==0?SelectorPolicy.Decision.STABLE:SelectorPolicy.Decision.UNSTABLE,0,SelectorPolicy.Scope.project(),SelectorPolicy.ExactMatcher.from(subject),new SelectorPolicy.Reason("PERFORMANCE",null)));}
        long memoryBefore=usedMemory(),compileStart=System.nanoTime();CompiledPolicySet compiled=CompiledPolicySet.compile(new SelectorPolicy.Document(1,1,rules));long compile=System.nanoTime()-compileStart;
        List<SelectorSubject>subjects=new ArrayList<>();for(int i=0;i<10_000;i++)subjects.add(SelectorSubject.trusted("id","policy-"+(i%ruleCount)));
        long previewStart=System.nanoTime();new SelectorStabilityEngine().preview(subjects,ObservationEvidence.unavailable(),compiled);long preview=System.nanoTime()-previewStart;
        rows.add(row("policy-preview",10_000,ruleCount,compile,0,preview,0,Math.max(0,usedMemory()-memoryBefore)));
    }

    private static SimilaritySubject similaritySubject(int index){
        String declaration="java-decl-v1:sha256:"+CanonicalDigests.digest("performance-declaration-v1",Integer.toString(index));
        SelectorSubject subject=new SelectorSubject(SelectorSubject.SubjectKind.STATIC_DECLARATION,"id",SelectorSubject.ValueState.KNOWN,"item-"+index,declaration,"module","src/Page"+(index%100)+".java","Page#field"+index,null,null,null,SelectorSubject.InputTrust.SOURCE_CANONICAL,"By.id(item)");
        return SimilaritySubject.create(subject,ComponentIdentity.of("TARGET","primary"),"By.id(item)",List.of(),List.of(),null,null,List.of(),correlation(),SimilaritySubject.SourceState.CURRENT);
    }

    private static TrustedObservation observation(int subjectIndex,RunDescriptor run){
        SimilaritySubject indexed=similaritySubject(subjectIndex);SimilaritySubject.UsageReference usage=new SimilaritySubject.UsageReference("usage-"+subjectIndex,"src/Page.java","Page","test",subjectIndex+1);
        return new TrustedObservation(indexed.subject(),indexed.component(),run,ObservationEvidence.Domain.NEW_RUN,Comparability.ComparisonMode.EXACT_VALUE,indexed.subject().templateInformation(),"observation-"+subjectIndex+'-'+run.invocationDiscriminator(),correlation(),SimilaritySubject.SourceState.CURRENT,usage,false);
    }

    private static SimilaritySubject.Correlation correlation(){return new SimilaritySubject.Correlation(SimilaritySubject.Confidence.EXACT,List.of("DECLARATION_REF"),List.of(),List.of(),1);}
    private static RunDescriptor run(String invocation){return new RunDescriptor("junit5","PerformanceTest","case","PerformanceTest#case",invocation,0,"dataset","tests-r1","app-r1","local",null,null,null);}
    private static String row(String kind,int size,int secondary,long build,long query,long serialize,long bytes,long heap){return String.join(",",kind,Integer.toString(size),Integer.toString(secondary),Long.toString(build),Long.toString(query),Long.toString(serialize),Long.toString(bytes),Long.toString(heap));}
    private static long usedMemory(){Runtime runtime=Runtime.getRuntime();return runtime.totalMemory()-runtime.freeMemory();}
}
