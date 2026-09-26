package io.github.testlens.selector.tooling;

import io.github.testlens.selector.engine.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.github.testlens.selector.tooling.SelectorHistoryModel.*;
import static io.github.testlens.selector.tooling.SelectorIndexModel.*;

/** Opt-in offline measurements. There are deliberately no CI wall-time assertions. */
class SelectorAuditPerformanceHarnessTest {
    @Test
    @EnabledIfSystemProperty(named="selector.audit.perf",matches="true")
    void measuresAuditScaling() throws Exception {
        Path output=Path.of(System.getProperty("selector.audit.perf.output","target/selector-audit-performance/metrics.csv")).toAbsolutePath().normalize();
        Files.createDirectories(output.getParent());List<String>rows=new ArrayList<>();
        rows.add("kind,declarations,historyBuckets,policies,candidatePercent,analysisNanos,jsonNanos,htmlNanos,jsonBytes,htmlBytes,heapDeltaBytes");
        for(int declarations:List.of(100,1_000,10_000,25_000))measure("declarations",declarations,0,0,0,rows);
        for(int buckets:List.of(10_000,100_000))measure("history",Math.min(25_000,buckets/4),buckets,0,0,rows);
        for(int policies:List.of(100,1_000))measure("policies",10_000,0,policies,0,rows);
        for(int coverage:List.of(10,100))measure("candidates",10_000,0,0,coverage,rows);
        Files.write(output,rows,StandardCharsets.UTF_8);
    }

    private static void measure(String kind,int declarationCount,int bucketCount,int policyCount,int candidatePercent,List<String>rows){
        SelectorIndex index=index(declarationCount);CompiledPolicySet policies=policies(index,policyCount);History history=bucketCount==0?null:history(index,bucketCount);List<SelectorAuditOrchestrator.CandidateInput>candidates=candidates(index,candidatePercent);
        SelectorAuditOrchestrator.Request request=new SelectorAuditOrchestrator.Request(index,policies,history,candidates,"selector-project-v1:sha256:"+"1".repeat(64),Map.of(),Map.of(),null,null,null,List.of());
        long memory=used(),start=System.nanoTime();SelectorAuditModel.AuditReport report=new SelectorAuditOrchestrator().audit(request);long analysis=System.nanoTime()-start;
        start=System.nanoTime();byte[]json=SelectorAuditJson.serialize(report);long jsonNanos=System.nanoTime()-start;
        start=System.nanoTime();byte[]html=SelectorAuditHtml.render(report);long htmlNanos=System.nanoTime()-start;
        rows.add(String.join(",",kind,Integer.toString(declarationCount),Integer.toString(bucketCount),Integer.toString(policyCount),Integer.toString(candidatePercent),Long.toString(analysis),Long.toString(jsonNanos),Long.toString(htmlNanos),Integer.toString(json.length),Integer.toString(html.length),Long.toString(Math.max(0,used()-memory))));
    }

    private static SelectorIndex index(int count){List<SourceFileIndex>files=new ArrayList<>(count);for(int i=0;i<count;i++){String path="module/src/test/java/perf/Page"+i+".java",ref=ref(i),value=i%7==0?"item-550e8400-e29b-41d4-a716-446655440000":"item-"+i;LocatorExpression expression=new LocatorExpression(ExpressionKind.SINGLE,"By.id(value-"+i+")",List.of(),List.of());DeclarationRecord d=new DeclarationRecord(1,ref,"java",path,new SourceRange(1,1,1,20,0,19),new DeclaringSymbol("FIELD","perf.Page"+i,"field",null),DeclarationKind.BY_FIELD,expression,new ResolvedLocator("id",value),ResolutionStatus.RESOLVED,false,false,"sha256:"+CanonicalDigests.digest("audit-perf-content-v1",Integer.toString(i)));files.add(new SourceFileIndex(path,"java","UTF-8","sha256:"+CanonicalDigests.digest("audit-perf-file-v1",Integer.toString(i)),false,false,ParseStatus.PARSED,List.of(),List.of(d)));}SelectorIndexModel.Coverage coverage=new SelectorIndexModel.Coverage(1,1,count,count,0,0,0,0,count,count,0,0,0,0,0,false,0);return new SelectorIndex(new ProjectMetadata("selector-project-v1:sha256:"+"1".repeat(64),List.of("module/src/test/java"),"3.28.2","JAVA_17","UTF-8","sha256:"+"2".repeat(64),"sha256:"+"3".repeat(64)),files,coverage,List.of());}
    private static CompiledPolicySet policies(SelectorIndex index,int count){if(count==0)return CompiledPolicySet.empty();List<SelectorPolicy.Rule>rules=new ArrayList<>();for(int i=0;i<count;i++){DeclarationRecord d=index.files().get(i%index.files().size()).declarations().get(0);SelectorSubject subject=SelectorEngineAdapter.subjects(d).get(0);rules.add(SelectorPolicy.Rule.create(SelectorPolicy.Decision.STABLE,0,SelectorPolicy.Scope.project(),SelectorPolicy.ExactMatcher.from(subject),new SelectorPolicy.Reason("PERF",null)));}return CompiledPolicySet.compile(new SelectorPolicy.Document(1,1,rules));}
    private static History history(SelectorIndex index,int buckets){int subjects=Math.min(index.files().size(),Math.max(1,buckets/4));List<Subject>items=new ArrayList<>(subjects);for(int i=0;i<subjects;i++){DeclarationRecord d=index.files().get(i).declarations().get(0);String component=ComponentIdentity.of("TARGET","primary").componentRef();SimilaritySubject.Correlation correlation=new SimilaritySubject.Correlation(SimilaritySubject.Confidence.EXACT,List.of("DECLARATION_REF"),List.of(),List.of(),1);DomainSummary domain=new DomainSummary(ObservationEvidence.Domain.NEW_RUN,Comparability.State.COMPARABLE,4,1,false,List.of());items.add(new Subject("selector-history-subject-v1:sha256:"+CanonicalDigests.digest("audit-perf-history-v1",Integer.toString(i)),"id","TARGET",List.of("primary"),component,d.declarationRef(),null,null,List.of(),List.of(),List.of(new ValueSummary(CanonicalDigests.digest("selector-exact-value-v1",Integer.toString(i)),4,4)),List.of(domain),new SelectorHistoryModel.UsageSummary(4,List.of()),List.of(),Comparability.ComparisonMode.EXACT_VALUE,correlation,SimilaritySubject.SourceState.CURRENT));}SelectorHistoryModel.Coverage coverage=new SelectorHistoryModel.Coverage(0,0,buckets,buckets,0,0,0,false,List.of());Retention retention=new Retention(1,subjects,buckets,0,false,true,List.of());return new History(1,1,1,1,1,new Generator("audit-performance","0.4.0"),new SelectorHistoryModel.Project("selector-project-v1:sha256:"+"1".repeat(64)),List.of(),items,coverage,retention);}
    private static List<SelectorAuditOrchestrator.CandidateInput>candidates(SelectorIndex index,int percent){int count=index.files().size()*percent/100;List<SelectorAuditOrchestrator.CandidateInput>out=new ArrayList<>(count);for(int i=0;i<count;i++){DeclarationRecord d=index.files().get(i).declarations().get(0);String id="candidate-"+i;CandidateAnalysis.Candidate original=new CandidateAnalysis.Candidate(id,new CandidateAnalysis.Locator("id",id),false,List.of(CandidateAnalysis.Origin.ORIGINAL),List.of(),new CandidateAnalysis.Validation(CandidateAnalysis.ValidationState.NOT_LIVE_VALIDATED,0,CandidateAnalysis.TargetComparison.UNKNOWN,List.of()),new CandidateAnalysis.Complexity(CandidateAnalysis.ScopeFragility.DIRECT,CandidateAnalysis.SemanticPreference.ID,0,0,id.length(),1),List.of(),List.of(),true);CandidateAnalysis analysis=new CandidateAnalysis(1,CandidateAnalysis.UsageIntent.FIND_ONE,null,d.declarationRef(),d.logicalPath(),id,List.of(original),CandidateAnalysis.Recommendation.REVIEW_REQUIRED,new CandidateAnalysis.Completeness(true,false,false,0,1,1,0),List.of());out.add(new SelectorAuditOrchestrator.CandidateInput(analysis));}return out;}
    private static String ref(int i){return "java-decl-v1:sha256:"+CanonicalDigests.digest("audit-perf-declaration-v1",Integer.toString(i));}
    private static long used(){Runtime r=Runtime.getRuntime();return r.totalMemory()-r.freeMemory();}
}
