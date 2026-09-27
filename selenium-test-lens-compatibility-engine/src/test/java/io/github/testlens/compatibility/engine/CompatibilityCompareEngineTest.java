package io.github.testlens.compatibility.engine;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.github.testlens.compatibility.engine.CompatibilityComparisonReport.*;
import static io.github.testlens.compatibility.engine.CompatibilityRunManifest.*;
import static org.junit.jupiter.api.Assertions.*;

class CompatibilityCompareEngineTest {
    private final CompatibilityCompareEngine engine=new CompatibilityCompareEngine();

    @Test void cleanHeadlessPassComparisonIsComparableAndDeterministic(){
        var b=manifest("a",EffectiveHeadlessState.HEADED,ObservabilityMode.DEFAULT,ResultStatus.PASSED,1,"dataset",1280,EvidenceRetention.FULL_TRACE,null);
        var v=manifest("a",EffectiveHeadlessState.HEADLESS,ObservabilityMode.DEFAULT,ResultStatus.PASSED,1,"dataset",1280,EvidenceRetention.FULL_TRACE,null);
        var one=compare(b,v,ComparisonIntent.Axis.HEADLESS_MODE);var two=compare(b,v,ComparisonIntent.Axis.HEADLESS_MODE);
        assertEquals(Comparability.COMPARABLE_WITH_LIMITATIONS,one.testComparisons().get(0).comparability().status());
        assertEquals(Outcome.NO_MEANINGFUL_DIFFERENCE,one.testComparisons().get(0).outcome());
        assertEquals(one,two);assertEquals(CodeChangeRequired.NO,one.testComparisons().get(0).codeChangeRequired());
    }

    @Test void viewportMismatchBlocksHeadlessAttributionAndConfigurationComesFirst(){
        var b=manifest("a",EffectiveHeadlessState.HEADED,ObservabilityMode.DEFAULT,ResultStatus.PASSED,1,"dataset",1280,EvidenceRetention.FULL_TRACE,null);
        var v=manifest("a",EffectiveHeadlessState.HEADLESS,ObservabilityMode.DEFAULT,ResultStatus.FAILED,1,"dataset",800,EvidenceRetention.FULL_TRACE,"ZERO_MATCH");
        var report=compare(b,v,ComparisonIntent.Axis.HEADLESS_MODE);var c=report.testComparisons().get(0);
        assertEquals(Comparability.NOT_COMPARABLE,c.comparability().status());assertEquals(Outcome.NOT_COMPARABLE,c.outcome());
        assertTrue(c.recommendationCodes().contains(RecommendationCode.ALIGN_VIEWPORT_AND_RERUN));assertEquals(CodeChangeRequired.NO,c.codeChangeRequired());
        assertTrue(report.findings().stream().anyMatch(f->f.category()==FindingCategory.VIEWPORT_RESPONSIVE));
    }

    @Test void unknownCriticalFactsAreNotKnownEqual(){
        var b=manifest("a",EffectiveHeadlessState.HEADED,ObservabilityMode.DEFAULT,ResultStatus.PASSED,1,null,1280,EvidenceRetention.FULL_TRACE,null);
        var v=manifest("a",EffectiveHeadlessState.HEADLESS,ObservabilityMode.DEFAULT,ResultStatus.PASSED,1,null,1280,EvidenceRetention.FULL_TRACE,null);
        var c=compare(b,v,ComparisonIntent.Axis.HEADLESS_MODE).testComparisons().get(0);
        assertEquals(Comparability.UNKNOWN,c.comparability().status());assertEquals(Outcome.INCONCLUSIVE,c.outcome());
        assertTrue(c.comparability().dimensions().stream().anyMatch(d->d.dimension().equals("dataset")&&d.state()==DimensionState.UNKNOWN_BOTH));
    }

    @Test void passAfterRetryIsOneBehavioralDeltaNotAttemptPairing(){
        var b=manifest("a",EffectiveHeadlessState.HEADED,ObservabilityMode.DEFAULT,ResultStatus.PASSED,1,"dataset",1280,EvidenceRetention.FULL_TRACE,null);
        var v=manifest("a",EffectiveHeadlessState.HEADLESS,ObservabilityMode.DEFAULT,ResultStatus.PASSED,2,"dataset",1280,EvidenceRetention.FULL_TRACE,"TRANSIENT");
        var c=compare(b,v,ComparisonIntent.Axis.HEADLESS_MODE).testComparisons().get(0);
        assertEquals(Outcome.PASS_WITH_BEHAVIORAL_DELTA,c.outcome());assertEquals(2,c.variant().attemptCount());assertTrue(c.variant().passedAfterRetry());
    }

    @Test void baselineRedSameDifferentUnknownAndImprovementArePreserved(){
        var same=compare(failed("a","TIMEOUT",true),failedVariant("a","TIMEOUT",true),ComparisonIntent.Axis.HEADLESS_MODE).testComparisons().get(0);assertEquals(Outcome.BASELINE_ALREADY_FAILED_SAME_FAILURE,same.outcome());
        var different=compare(failed("a","TIMEOUT",true),failedVariant("a","ASSERTION",true),ComparisonIntent.Axis.HEADLESS_MODE).testComparisons().get(0);assertEquals(Outcome.BASELINE_ALREADY_FAILED_DIFFERENT_FAILURE,different.outcome());
        var unknown=compare(failed("a","TIMEOUT",false),failedVariant("a","TIMEOUT",false),ComparisonIntent.Axis.HEADLESS_MODE).testComparisons().get(0);assertEquals(Outcome.INCONCLUSIVE,unknown.outcome());assertTrue(unknown.baselineAlreadyFailed());
        var improved=compare(failed("a","TIMEOUT",true),manifest("a",EffectiveHeadlessState.HEADLESS,ObservabilityMode.DEFAULT,ResultStatus.PASSED,1,"dataset",1280,EvidenceRetention.FULL_TRACE,null),ComparisonIntent.Axis.HEADLESS_MODE).testComparisons().get(0);assertEquals(Outcome.VARIANT_IMPROVEMENT,improved.outcome());
    }

    @Test void fastSummaryOnlyIsExpectedDifferenceNotZeroBehavior(){
        var b=manifest("a",EffectiveHeadlessState.HEADLESS,ObservabilityMode.DEFAULT,ResultStatus.PASSED,1,"dataset",1280,EvidenceRetention.FULL_TRACE,null);
        var v=manifest("a",EffectiveHeadlessState.HEADLESS,ObservabilityMode.FAST,ResultStatus.PASSED,1,"dataset",1280,EvidenceRetention.SUMMARY_ONLY,null);
        var c=compare(b,v,ComparisonIntent.Axis.OBSERVABILITY_MODE).testComparisons().get(0);
        assertEquals(Outcome.EXPECTED_OBSERVABILITY_DIFFERENCE,c.outcome());
        assertTrue(c.behaviorDiff().groups().stream().anyMatch(g->g.group().equals("locators")&&g.state()==BehaviorState.PARTIAL));
        assertTrue(c.behaviorDiff().groups().stream().anyMatch(g->g.group().equals("evidenceRetention")&&g.state()==BehaviorState.EXPECTED_DIFFERENCE));
    }

    @Test void duplicateIdsCoalesceButDistinctSameIdentityIsAmbiguous(){
        var b=manifest("a",EffectiveHeadlessState.HEADED,ObservabilityMode.DEFAULT,ResultStatus.PASSED,1,"dataset",1280,EvidenceRetention.FULL_TRACE,null);
        var v=manifest("a",EffectiveHeadlessState.HEADLESS,ObservabilityMode.DEFAULT,ResultStatus.PASSED,1,"dataset",1280,EvidenceRetention.FULL_TRACE,null);
        var duplicate=engine.compare(List.of(b,b),List.of(v),intent(ComparisonIntent.Axis.HEADLESS_MODE));assertEquals(1,duplicate.coverage().duplicateIdsCoalesced());assertEquals(1,duplicate.coverage().matched());
        var other=manifest("a",EffectiveHeadlessState.HEADLESS,ObservabilityMode.DEFAULT,ResultStatus.FAILED,1,"dataset",1280,EvidenceRetention.FULL_TRACE,"FAIL");
        var ambiguous=engine.compare(List.of(b),List.of(v,other),intent(ComparisonIntent.Axis.HEADLESS_MODE));assertEquals(1,ambiguous.coverage().ambiguous());assertEquals(0,ambiguous.coverage().matched());
    }

    @Test void sameHeadlessStateDoesNotEstablishIntendedAxis(){var m=manifest("a",EffectiveHeadlessState.HEADED,ObservabilityMode.DEFAULT,ResultStatus.PASSED,1,"dataset",1280,EvidenceRetention.FULL_TRACE,null);var c=compare(m,m,ComparisonIntent.Axis.HEADLESS_MODE).testComparisons().get(0);assertEquals(Comparability.NOT_COMPARABLE,c.comparability().status());assertTrue(c.comparability().reasonCodes().contains("INTENDED_AXIS_NOT_ESTABLISHED"));}

    @Test void cleanPassFailIsRegressionObservationNotAutomaticallyConfirmedCause(){var b=manifest("a",EffectiveHeadlessState.HEADED,ObservabilityMode.DEFAULT,ResultStatus.PASSED,1,"dataset",1280,EvidenceRetention.FULL_TRACE,null);var v=manifest("a",EffectiveHeadlessState.HEADLESS,ObservabilityMode.DEFAULT,ResultStatus.FAILED,1,"dataset",1280,EvidenceRetention.FULL_TRACE,"FAIL");var report=compare(b,v,ComparisonIntent.Axis.HEADLESS_MODE);assertEquals(Outcome.VARIANT_REGRESSION,report.testComparisons().get(0).outcome());assertTrue(report.findings().stream().anyMatch(f->f.code().equals("VARIANT_RESULT_REGRESSION")&&f.causalState()==CausalState.HYPOTHESIS));assertFalse(report.findings().stream().anyMatch(f->f.causalState()==CausalState.CONFIRMED_CAUSE));}

    @Test void datasetMismatchAndHeadlessMismatchForObservabilityPreventFalseAttribution(){var b=manifest("a",EffectiveHeadlessState.HEADLESS,ObservabilityMode.DEFAULT,ResultStatus.PASSED,1,"A",1280,EvidenceRetention.FULL_TRACE,null);var v=manifest("a",EffectiveHeadlessState.HEADED,ObservabilityMode.FAST,ResultStatus.FAILED,1,"B",1280,EvidenceRetention.SUMMARY_ONLY,"FAIL");var c=compare(b,v,ComparisonIntent.Axis.OBSERVABILITY_MODE).testComparisons().get(0);assertEquals(Comparability.NOT_COMPARABLE,c.comparability().status());assertTrue(c.comparability().reasonCodes().contains("DATASET_MISMATCH"));assertTrue(c.comparability().reasonCodes().contains("HEADLESS_NOT_HELD_CONSTANT"));}

    private CompatibilityComparisonReport compare(CompatibilityRunManifest b,CompatibilityRunManifest v,ComparisonIntent.Axis axis){return engine.compare(List.of(b),List.of(v),intent(axis));}
    private ComparisonIntent intent(ComparisonIntent.Axis axis){return new ComparisonIntent(axis,"baseline","variant");}
    private CompatibilityRunManifest failed(String key,String category,boolean complete){return manifest(key,EffectiveHeadlessState.HEADED,ObservabilityMode.DEFAULT,ResultStatus.FAILED,1,"dataset",1280,EvidenceRetention.FULL_TRACE,complete?category:null);}
    private CompatibilityRunManifest failedVariant(String key,String category,boolean complete){return manifest(key,EffectiveHeadlessState.HEADLESS,ObservabilityMode.DEFAULT,ResultStatus.FAILED,1,"dataset",1280,EvidenceRetention.FULL_TRACE,complete?category:null);}

    static CompatibilityRunManifest manifest(String logical,EffectiveHeadlessState headless,ObservabilityMode obs,ResultStatus terminal,int attempts,String dataset,Integer viewport,EvidenceRetention retention,String failureCategory){
        String testRef="compatibility-test-v1:sha256:"+CompatibilityDigests.digest("compatibility-test-v1","TESTNG","example.Test",logical);
        Fact<String> invocation=Fact.known("compatibility-invocation-v1:sha256:"+CompatibilityDigests.digest("compatibility-invocation-v1",logical),Provenance.TRUSTED_DESCRIPTOR);
        Fact<TestKey>key=Fact.known(new TestKey(Framework.TESTNG,"example.Test",logical,logical,false,invocation,Fact.known("suite",Provenance.TRUSTED_DESCRIPTOR),testRef),Provenance.TRUSTED_DESCRIPTOR);
        Execution execution=new Execution(Fact.known(headless==EffectiveHeadlessState.HEADLESS?RequestedHeadlessMode.HEADLESS:RequestedHeadlessMode.HEADED,Provenance.TRUSTED_DESCRIPTOR),Fact.known(RequestedHeadlessProvenance.CALLER_SUPPLIED,Provenance.TRUSTED_DESCRIPTOR),Fact.known(headless,Provenance.TRUSTED_CAPTURE_DESCRIPTOR),Fact.known(EffectiveHeadlessProvenance.TRUSTED_CAPTURE_DESCRIPTOR,Provenance.TRUSTED_DESCRIPTOR),Fact.known(obs,Provenance.TRUSTED_DESCRIPTOR),Fact.known(HudPreset.STANDARD,Provenance.TRUSTED_DESCRIPTOR),Fact.known(DriverScope.PER_METHOD,Provenance.TRUSTED_DESCRIPTOR),Fact.known(Lifecycle.MANAGED_INVOCATION,Provenance.TRUSTED_DESCRIPTOR));
        Browser browser=new Browser(Fact.known("chrome",Provenance.CAPABILITY_REPORTED),Fact.known("152",Provenance.CAPABILITY_REPORTED),Fact.unknown(),Fact.unknown(),Fact.known("WINDOWS",Provenance.CAPABILITY_REPORTED),Map.of(),"compatibility-capabilities-v1:sha256:"+CompatibilityDigests.digest("compatibility-capabilities-v1","chrome","152","WINDOWS"));
        Fact<Integer>vp=viewport==null?Fact.unknown():Fact.known(viewport,Provenance.EXPLICIT_WEBDRIVER_CAPTURE);Display display=new Display(vp,vp,vp,vp,Fact.known(1.0,Provenance.EXPLICIT_WEBDRIVER_CAPTURE));
        Fact<String>datasetFact=dataset==null?Fact.unknown():Fact.known("compatibility-dataset-v1:sha256:"+CompatibilityDigests.digest("compatibility-dataset-v1",dataset),Provenance.TRUSTED_DESCRIPTOR);
        Context context=new Context(datasetFact,known("env"),known("test-rev"),known("sut-rev"),Fact.known("en-US",Provenance.TRUSTED_DESCRIPTOR),Fact.known("UTC",Provenance.TRUSTED_DESCRIPTOR));
        String config="compatibility-config-v1:sha256:"+CompatibilityDigests.digest("compatibility-config-v1",obs.name());Configuration configuration=new Configuration(Fact.known(obs==ObservabilityMode.DEFAULT,Provenance.TRUSTED_DESCRIPTOR),Fact.known(obs==ObservabilityMode.DEFAULT,Provenance.TRUSTED_DESCRIPTOR),Fact.known(false,Provenance.TRUSTED_DESCRIPTOR),Fact.known(retention.name(),Provenance.TRUSTED_DESCRIPTOR),Fact.known("OFF",Provenance.TRUSTED_DESCRIPTOR),config);
        List<Attempt> attemptList=new java.util.ArrayList<>();for(int i=1;i<=attempts;i++){ResultStatus status=i==attempts?terminal:ResultStatus.FAILED;FailureSignature failure=status==ResultStatus.FAILED&&failureCategory!=null?CompatibilityAttempts.failure("example."+failureCategory,failureCategory,"TEST","subject",List.of(failureCategory),failureCategory,FailurePhase.TEST):noFailure();SmartClickDetail detail=retention==EvidenceRetention.FULL_TRACE?SmartClickDetail.COMPLETE:SmartClickDetail.UNKNOWN;EvidenceCompleteness evidence=new EvidenceCompleteness(retention,retention==EvidenceRetention.SUMMARY_ONLY?CompletenessState.PARTIAL:CompletenessState.COMPLETE,retention==EvidenceRetention.FULL_TRACE,retention==EvidenceRetention.FULL_TRACE,false,false,true,detail);BehaviorSummary behavior=new BehaviorSummary(0,0,List.of(),new LocatorSummary(0,0,0,0,0,0,List.of()),new WaitSummary(0,0,0,0,0),new InteractionSummary(0,0,0,detail,List.of()),new ContextSummary(0,0,0,0,0),List.of(),0,List.of(),0);attemptList.add(CompatibilityAttempts.attempt(i,status,failure,behavior,new TimingSummary(1,10,Fact.known(10L,Provenance.DERIVED)),evidence));}
        TerminalResult tr=new TerminalResult(terminal,attempts,terminal==ResultStatus.PASSED&&attempts>1,terminal==ResultStatus.FAILED&&attempts>1);Completeness completeness=new Completeness(CompletenessState.COMPLETE,CompletenessState.COMPLETE,CompletenessState.PARTIAL,viewport==null?CompletenessState.PARTIAL:CompletenessState.COMPLETE,dataset==null?CompletenessState.PARTIAL:CompletenessState.COMPLETE,retention==EvidenceRetention.SUMMARY_ONLY?CompletenessState.PARTIAL:CompletenessState.COMPLETE,failureCategory==null&&terminal==ResultStatus.FAILED?CompletenessState.PARTIAL:CompletenessState.COMPLETE);
        String placeholder="compatibility-manifest-v1:sha256:"+"0".repeat(64);CompatibilityRunManifest temp=new CompatibilityRunManifest(1,1,placeholder,key,execution,browser,display,context,configuration,attemptList,tr,completeness,List.of(),List.of());String id=CompatibilityManifestIdentity.id(temp);return new CompatibilityRunManifest(1,1,id,key,execution,browser,display,context,configuration,attemptList,tr,completeness,List.of(),List.of());
    }
    private static Fact<String>known(String value){return Fact.known(value,Provenance.TRUSTED_DESCRIPTOR);}
}
