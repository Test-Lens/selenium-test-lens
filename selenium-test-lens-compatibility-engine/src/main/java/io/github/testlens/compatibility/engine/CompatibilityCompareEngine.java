package io.github.testlens.compatibility.engine;

import java.util.*;

import static io.github.testlens.compatibility.engine.CompatibilityComparisonReport.*;
import static io.github.testlens.compatibility.engine.CompatibilityRunManifest.*;

/** Pure JDK deterministic matcher and comparison engine for manifest schema V1. */
public final class CompatibilityCompareEngine {
    public static final int MAX_MANIFESTS_PER_SIDE = 25_000;

    public CompatibilityComparisonReport compare(List<CompatibilityRunManifest> baselineInput,
                                                 List<CompatibilityRunManifest> variantInput,
                                                 ComparisonIntent intent) {
        Objects.requireNonNull(intent, "intent");
        Input baseline=deduplicate(baselineInput,"baseline"), variant=deduplicate(variantInput,"variant");
        Map<String,List<CompatibilityRunManifest>> left=index(baseline.accepted), right=index(variant.accepted);
        SortedSet<String> keys=new TreeSet<>(); keys.addAll(left.keySet());keys.addAll(right.keySet());
        List<TestComparison> comparisons=new ArrayList<>();List<Unmatched> unmatchedLeft=new ArrayList<>(),unmatchedRight=new ArrayList<>();List<AmbiguousMatch> ambiguous=new ArrayList<>();List<Finding> findings=new ArrayList<>();List<Recommendation> recommendations=new ArrayList<>();
        for(String key:keys){
            List<CompatibilityRunManifest> l=left.getOrDefault(key,List.of()),r=right.getOrDefault(key,List.of());
            if(l.isEmpty()){r.forEach(m->unmatchedRight.add(unmatched(m,"UNMATCHED_VARIANT")));continue;}
            if(r.isEmpty()){l.forEach(m->unmatchedLeft.add(unmatched(m,"UNMATCHED_BASELINE")));continue;}
            if(l.size()!=1||r.size()!=1){ambiguous.add(new AmbiguousMatch(key,ids(l),ids(r)));continue;}
            Compared c=compareOne(l.get(0),r.get(0),intent);comparisons.add(c.comparison);findings.addAll(c.findings);recommendations.addAll(c.recommendations);
        }
        comparisons.sort(Comparator.comparing(TestComparison::testIdentityRef).thenComparing(TestComparison::invocationRef));
        findings.sort(Comparator.comparing(Finding::findingId));
        recommendations=recommendations.stream().distinct().sorted(Comparator.comparing((Recommendation x)->x.testIdentityRef()).thenComparing(x->x.code().name())).toList();
        Coverage coverage=coverage(baselineInput.size(),variantInput.size(),baseline,variant,comparisons,unmatchedLeft,unmatchedRight,ambiguous);
        List<String> issues=new ArrayList<>();if(baseline.rejected+variant.rejected>0)issues.add("MANIFESTS_REJECTED");if(!ambiguous.isEmpty())issues.add("AMBIGUOUS_MATCHES_PRESENT");
        return new CompatibilityComparisonReport(1,1,AnalysisMode.COMPARE,intent,coverage,comparisons,unmatchedLeft,unmatchedRight,ambiguous,findings,recommendations,issues,List.of("REPORT_COVERS_SUPPLIED_MATCHED_INVOCATIONS_ONLY"));
    }

    private Compared compareOne(CompatibilityRunManifest b,CompatibilityRunManifest v,ComparisonIntent intent){
        List<DimensionAssessment>d=new ArrayList<>();List<String>reasons=new ArrayList<>();
        d.add(new DimensionAssessment("testIdentity",DimensionState.EQUAL_KNOWN,true,"MATCHED_STRUCTURED_IDENTITY"));
        add(d,"dataset",b.context().datasetDigest(),v.context().datasetDigest(),true,"DATASET_MISMATCH");
        if(intent.axis()!=ComparisonIntent.Axis.BUILD){
            add(d,"testSourceRevision",b.context().testSourceRevision(),v.context().testSourceRevision(),true,"TEST_REVISION_MISMATCH");
            add(d,"systemUnderTestRevision",b.context().systemUnderTestRevision(),v.context().systemUnderTestRevision(),true,"SUT_REVISION_MISMATCH");
        }
        add(d,"environment",b.context().environmentDigest(),v.context().environmentDigest(),true,"ENVIRONMENT_MISMATCH");
        add(d,"browserName",b.browser().name(),v.browser().name(),true,"BROWSER_MISMATCH");
        add(d,"browserVersion",b.browser().version(),v.browser().version(),true,"BROWSER_VERSION_MISMATCH");
        add(d,"platform",b.browser().platform(),v.browser().platform(),true,"PLATFORM_MISMATCH");
        add(d,"driverVersion",b.browser().driverVersion(),v.browser().driverVersion(),false,"DRIVER_VERSION_MISMATCH");
        add(d,"viewportWidth",b.display().viewportWidth(),v.display().viewportWidth(),true,"VIEWPORT_MISMATCH");
        add(d,"viewportHeight",b.display().viewportHeight(),v.display().viewportHeight(),true,"VIEWPORT_MISMATCH");
        add(d,"devicePixelRatio",b.display().devicePixelRatio(),v.display().devicePixelRatio(),false,"DEVICE_SCALE_MISMATCH");
        add(d,"locale",b.context().locale(),v.context().locale(),false,"LOCALE_MISMATCH");
        add(d,"timezone",b.context().timezone(),v.context().timezone(),false,"TIMEZONE_MISMATCH");
        add(d,"lifecycle",b.execution().lifecycle(),v.execution().lifecycle(),true,"LIFECYCLE_MISMATCH");
        add(d,"driverScope",b.execution().driverScope(),v.execution().driverScope(),true,"DRIVER_SCOPE_MISMATCH");
        if(intent.axis()==ComparisonIntent.Axis.HEADLESS_MODE){
            intendedHeadless(d,b,v);add(d,"observabilityMode",b.execution().observabilityMode(),v.execution().observabilityMode(),true,"OBSERVABILITY_NOT_HELD_CONSTANT");
            addKnown(d,"semanticConfiguration",b.configuration().semanticConfigurationDigest(),v.configuration().semanticConfigurationDigest(),true,"SEMANTIC_CONFIGURATION_MISMATCH");
        } else if(intent.axis()==ComparisonIntent.Axis.OBSERVABILITY_MODE){
            intendedObservability(d,b,v);add(d,"effectiveHeadless",b.execution().effectiveHeadless(),v.execution().effectiveHeadless(),true,"HEADLESS_NOT_HELD_CONSTANT");
            d.add(new DimensionAssessment("observabilityDerivedOverrides",DimensionState.UNKNOWN_BOTH,false,"EXPLICIT_OVERRIDE_PROVENANCE_UNAVAILABLE"));
        } else if(intent.axis()==ComparisonIntent.Axis.BUILD){
            intendedBuild(d,b,v);
            add(d,"effectiveHeadless",b.execution().effectiveHeadless(),v.execution().effectiveHeadless(),true,"HEADLESS_NOT_HELD_CONSTANT");
            add(d,"observabilityMode",b.execution().observabilityMode(),v.execution().observabilityMode(),true,"OBSERVABILITY_NOT_HELD_CONSTANT");
            addKnown(d,"semanticConfiguration",b.configuration().semanticConfigurationDigest(),v.configuration().semanticConfigurationDigest(),true,"SEMANTIC_CONFIGURATION_MISMATCH");
        } else {
            d.add(new DimensionAssessment("intendedAxis",DimensionState.NOT_APPLICABLE,true,"AXIS_COMPARE_NOT_SPECIALIZED_V1"));
        }
        d.forEach(x->{if(!x.reasonCode().equals("EQUAL_KNOWN")&&!x.reasonCode().equals("MATCHED_STRUCTURED_IDENTITY")&&x.state()!=DimensionState.INTENDED_DIFFERENCE&&x.state()!=DimensionState.NOT_APPLICABLE)reasons.add(x.reasonCode());});
        Comparability status=comparability(d,intent);
        ComparabilityResult comparability=new ComparabilityResult(status,reasons,d);
        BehaviorDiff behavior=behavior(b,v,intent);
        FailureRelationship failure=failureRelationship(b,v);
        Outcome outcome=outcome(b,v,status,behavior,failure,intent);
        String testRef=b.testIdentity().value().testIdentityRef(), invocation=invocation(b);
        Generated generated=findings(testRef,invocation,b,v,intent,comparability,behavior,outcome,failure);
        CodeChangeRequired change=codeChange(outcome,comparability,generated.recommendations);
        List<String>limitations=new ArrayList<>(reasons);if(behavior.evidenceLimited())limitations.add("BEHAVIOR_EVIDENCE_LIMITED");
        List<String>verify=generated.recommendations.stream().map(x->verification(x.code())).distinct().toList();
        TestComparison comparison=new TestComparison(testRef,invocation,side(b),side(v),comparability,behavior,failure,outcome,b.terminalResult().status()==ResultStatus.FAILED,generated.findings.stream().map(Finding::findingId).toList(),generated.recommendations.stream().map(Recommendation::code).toList(),change,verify,limitations);
        return new Compared(comparison,generated.findings,generated.recommendations);
    }

    private void intendedHeadless(List<DimensionAssessment>d,CompatibilityRunManifest b,CompatibilityRunManifest v){
        Fact<EffectiveHeadlessState> l=b.execution().effectiveHeadless(),r=v.execution().effectiveHeadless();
        DimensionState s=state(l,r);String reason="HEADLESS_STATE_UNKNOWN";
        if(s==DimensionState.DIFFERENT&&l.knowledge()==Knowledge.KNOWN&&r.knowledge()==Knowledge.KNOWN)s=DimensionState.INTENDED_DIFFERENCE;
        else if(s==DimensionState.EQUAL_KNOWN)reason="INTENDED_AXIS_NOT_ESTABLISHED";
        else if(s==DimensionState.CONFLICTED)reason="HEADLESS_STATE_CONFLICTED";
        d.add(new DimensionAssessment("effectiveHeadless",s,true,reason));
    }
    private void intendedObservability(List<DimensionAssessment>d,CompatibilityRunManifest b,CompatibilityRunManifest v){
        Fact<ObservabilityMode> l=b.execution().observabilityMode(),r=v.execution().observabilityMode();DimensionState s=state(l,r);String reason="OBSERVABILITY_STATE_UNKNOWN";
        if(s==DimensionState.DIFFERENT&&l.knowledge()==Knowledge.KNOWN&&r.knowledge()==Knowledge.KNOWN)s=DimensionState.INTENDED_DIFFERENCE;
        else if(s==DimensionState.EQUAL_KNOWN)reason="INTENDED_AXIS_NOT_ESTABLISHED";
        else if(s==DimensionState.CONFLICTED)reason="OBSERVABILITY_STATE_CONFLICTED";
        d.add(new DimensionAssessment("observabilityMode",s,true,reason));
    }
    private void intendedBuild(List<DimensionAssessment>d,CompatibilityRunManifest b,CompatibilityRunManifest v){
        DimensionState test=buildRevision(d,"testSourceRevision",b.context().testSourceRevision(),v.context().testSourceRevision(),"TEST_REVISION_MISMATCH");
        DimensionState sut=buildRevision(d,"systemUnderTestRevision",b.context().systemUnderTestRevision(),v.context().systemUnderTestRevision(),"SUT_REVISION_MISMATCH");
        boolean established=test==DimensionState.INTENDED_DIFFERENCE||sut==DimensionState.INTENDED_DIFFERENCE;
        boolean unknown=List.of(test,sut).stream().anyMatch(CompatibilityCompareEngine::unknown);
        DimensionState state=established?DimensionState.INTENDED_DIFFERENCE:unknown?DimensionState.UNKNOWN_BOTH:DimensionState.EQUAL_KNOWN;
        d.add(new DimensionAssessment("buildRevision",state,true,established?"BUILD_REVISION_CHANGED":unknown?"BUILD_REVISION_UNKNOWN":"INTENDED_AXIS_NOT_ESTABLISHED"));
    }
    private static DimensionState buildRevision(List<DimensionAssessment>d,String name,Fact<String>l,Fact<String>r,String mismatch){
        DimensionState state=state(l,r);
        if(state==DimensionState.DIFFERENT&&l.knowledge()==Knowledge.KNOWN&&r.knowledge()==Knowledge.KNOWN)state=DimensionState.INTENDED_DIFFERENCE;
        d.add(new DimensionAssessment(name,state,true,state==DimensionState.INTENDED_DIFFERENCE?mismatch:knowledgeReason(name,state)));
        return state;
    }
    private static boolean unknown(DimensionState state){return state==DimensionState.UNKNOWN_LEFT||state==DimensionState.UNKNOWN_RIGHT||state==DimensionState.UNKNOWN_BOTH||state==DimensionState.CONFLICTED;}
    private static <T>void add(List<DimensionAssessment>d,String name,Fact<T>l,Fact<T>r,boolean critical,String mismatch){DimensionState s=state(l,r);d.add(new DimensionAssessment(name,s,critical,s==DimensionState.DIFFERENT?mismatch:knowledgeReason(name,s)));}
    private static void addKnown(List<DimensionAssessment>d,String name,String l,String r,boolean critical,String mismatch){DimensionState s=Objects.equals(l,r)?DimensionState.EQUAL_KNOWN:DimensionState.DIFFERENT;d.add(new DimensionAssessment(name,s,critical,s==DimensionState.DIFFERENT?mismatch:"EQUAL_KNOWN"));}
    private static String knowledgeReason(String name,DimensionState state){return switch(state){case EQUAL_KNOWN->"EQUAL_KNOWN";case UNKNOWN_LEFT->name.toUpperCase(Locale.ROOT)+"_UNKNOWN_LEFT";case UNKNOWN_RIGHT->name.toUpperCase(Locale.ROOT)+"_UNKNOWN_RIGHT";case UNKNOWN_BOTH->name.toUpperCase(Locale.ROOT)+"_UNKNOWN_BOTH";case CONFLICTED->name.toUpperCase(Locale.ROOT)+"_CONFLICTED";default->name.toUpperCase(Locale.ROOT)+"_DIFFERENT";};}
    private static <T>DimensionState state(Fact<T>l,Fact<T>r){if(l.knowledge()==Knowledge.CONFLICTED||r.knowledge()==Knowledge.CONFLICTED)return DimensionState.CONFLICTED;if(l.knowledge()!=Knowledge.KNOWN&&r.knowledge()!=Knowledge.KNOWN)return DimensionState.UNKNOWN_BOTH;if(l.knowledge()!=Knowledge.KNOWN)return DimensionState.UNKNOWN_LEFT;if(r.knowledge()!=Knowledge.KNOWN)return DimensionState.UNKNOWN_RIGHT;return Objects.equals(l.value(),r.value())?DimensionState.EQUAL_KNOWN:DimensionState.DIFFERENT;}
    private static Comparability comparability(List<DimensionAssessment>d,ComparisonIntent intent){
        String axisDimension=switch(intent.axis()){case HEADLESS_MODE->"effectiveHeadless";case OBSERVABILITY_MODE->"observabilityMode";case BUILD->"buildRevision";default->"intendedAxis";};
        DimensionAssessment axis=d.stream().filter(x->x.dimension().equals(axisDimension)).findFirst().orElse(null);
        if(axis==null||axis.state()!=DimensionState.INTENDED_DIFFERENCE)return axis!=null&&(axis.state()==DimensionState.UNKNOWN_BOTH||axis.state()==DimensionState.UNKNOWN_LEFT||axis.state()==DimensionState.UNKNOWN_RIGHT||axis.state()==DimensionState.CONFLICTED)?Comparability.UNKNOWN:Comparability.NOT_COMPARABLE;
        if(d.stream().anyMatch(x->x.critical()&&x.state()==DimensionState.DIFFERENT))return Comparability.NOT_COMPARABLE;
        if(d.stream().anyMatch(x->x.critical()&&(x.state()==DimensionState.UNKNOWN_BOTH||x.state()==DimensionState.UNKNOWN_LEFT||x.state()==DimensionState.UNKNOWN_RIGHT||x.state()==DimensionState.CONFLICTED)))return Comparability.UNKNOWN;
        return d.stream().anyMatch(x->!x.critical()&&x.state()!=DimensionState.EQUAL_KNOWN&&x.state()!=DimensionState.NOT_APPLICABLE)?Comparability.COMPARABLE_WITH_LIMITATIONS:Comparability.COMPARABLE;
    }

    private static BehaviorDiff behavior(CompatibilityRunManifest b,CompatibilityRunManifest v,ComparisonIntent intent){
        List<BehaviorGroupDiff>g=new ArrayList<>();
        addGroup(g,"attempts",true,statuses(b).equals(statuses(v))&&b.terminalResult().passedAfterRetry()==v.terminalResult().passedAfterRetry(),BehaviorChannel.EXECUTION_BEHAVIOR,"ATTEMPT_SEQUENCE_DIFFERS",statuses(b).toString(),statuses(v).toString());
        Attempt ba=last(b),va=last(v);boolean complete=ba.evidence().behavior()==CompletenessState.COMPLETE&&va.evidence().behavior()==CompletenessState.COMPLETE;
        group(g,"recoveryRetries",complete&&ba.evidence().detailedRetries()&&va.evidence().detailedRetries(),ba.behavior().operationRetries(),va.behavior().operationRetries(),"RETRY_EVIDENCE_UNAVAILABLE");
        group(g,"locators",complete&&ba.evidence().locatorObservations()&&va.evidence().locatorObservations(),ba.behavior().locators(),va.behavior().locators(),"LOCATOR_EVIDENCE_UNAVAILABLE");
        group(g,"waits",complete,ba.behavior().waits(),va.behavior().waits(),"WAIT_EVIDENCE_UNAVAILABLE");
        boolean smart=complete&&ba.evidence().smartClickDetail()==SmartClickDetail.COMPLETE&&va.evidence().smartClickDetail()==SmartClickDetail.COMPLETE;
        group(g,"interactionFallback",smart,ba.behavior().interactions(),va.behavior().interactions(),"SMART_CLICK_EVIDENCE_UNAVAILABLE");
        group(g,"browserContext",complete,ba.behavior().browserContext(),va.behavior().browserContext(),"CONTEXT_EVIDENCE_UNAVAILABLE");
        group(g,"auth",complete,ba.behavior().authOutcomes(),va.behavior().authOutcomes(),"AUTH_EVIDENCE_UNAVAILABLE");
        boolean expected=intent.axis()==ComparisonIntent.Axis.OBSERVABILITY_MODE&&ba.evidence().retention()!=va.evidence().retention();
        g.add(new BehaviorGroupDiff("evidenceRetention",expected?BehaviorState.EXPECTED_DIFFERENCE:(ba.evidence().retention()==va.evidence().retention()?BehaviorState.SAME_KNOWN:BehaviorState.DIFFERENT),expected?BehaviorChannel.EXPECTED_PRESET_DIFFERENCE:BehaviorChannel.OBSERVABILITY_BEHAVIOR,expected?"EXPECTED_FAST_EVIDENCE_REDUCTION":"EVIDENCE_RETENTION_DIFFERS",ba.evidence().retention().name(),va.evidence().retention().name()));
        boolean execution=g.stream().anyMatch(x->x.channel()==BehaviorChannel.EXECUTION_BEHAVIOR&&x.state()==BehaviorState.DIFFERENT);
        return new BehaviorDiff(g,execution,expected,g.stream().anyMatch(x->x.state()==BehaviorState.UNKNOWN||x.state()==BehaviorState.PARTIAL));
    }
    private static void group(List<BehaviorGroupDiff>g,String name,boolean sufficient,Object l,Object r,String missing){if(!sufficient){g.add(new BehaviorGroupDiff(name,BehaviorState.PARTIAL,BehaviorChannel.EXECUTION_BEHAVIOR,missing,"evidence unavailable","evidence unavailable"));return;}addGroup(g,name,true,Objects.equals(l,r),BehaviorChannel.EXECUTION_BEHAVIOR,name.toUpperCase(Locale.ROOT)+"_DIFFERS",String.valueOf(l),String.valueOf(r));}
    private static void addGroup(List<BehaviorGroupDiff>g,String name,boolean sufficient,boolean same,BehaviorChannel channel,String reason,String l,String r){g.add(new BehaviorGroupDiff(name,!sufficient?BehaviorState.PARTIAL:(same?BehaviorState.SAME_KNOWN:BehaviorState.DIFFERENT),channel,!sufficient?"EVIDENCE_UNAVAILABLE":(same?"SAME_KNOWN":reason),same?"":l,same?"":r));}
    private static FailureRelationship failureRelationship(CompatibilityRunManifest b,CompatibilityRunManifest v){if(b.terminalResult().status()!=ResultStatus.FAILED||v.terminalResult().status()!=ResultStatus.FAILED)return FailureRelationship.NOT_APPLICABLE;FailureSignature l=last(b).failure(),r=last(v).failure();boolean complete=b.completeness().failure()==CompletenessState.COMPLETE&&v.completeness().failure()==CompletenessState.COMPLETE;List<Fact<String>>lf=List.of(l.exceptionClass(),l.category(),l.operationCategory(),l.safeSubjectRef()),rf=List.of(r.exceptionClass(),r.category(),r.operationCategory(),r.safeSubjectRef());boolean any=lf.stream().anyMatch(x->x.knowledge()==Knowledge.KNOWN);if(!complete)return any?FailureRelationship.PARTIAL:FailureRelationship.UNKNOWN;for(int i=0;i<lf.size();i++){if(lf.get(i).knowledge()!=Knowledge.KNOWN||rf.get(i).knowledge()!=Knowledge.KNOWN)return FailureRelationship.PARTIAL;if(!Objects.equals(lf.get(i).value(),rf.get(i).value()))return FailureRelationship.DIFFERENT_KNOWN;}if(l.phase()!=r.phase()||!l.reasonCodes().equals(r.reasonCodes()))return FailureRelationship.DIFFERENT_KNOWN;if(l.normalizedMessageDigest().knowledge()==Knowledge.KNOWN&&r.normalizedMessageDigest().knowledge()==Knowledge.KNOWN&&!Objects.equals(l.normalizedMessageDigest().value(),r.normalizedMessageDigest().value()))return FailureRelationship.PARTIAL;return FailureRelationship.SAME_KNOWN;}
    private static Outcome outcome(CompatibilityRunManifest b,CompatibilityRunManifest v,Comparability c,BehaviorDiff behavior,FailureRelationship failure,ComparisonIntent intent){if(c==Comparability.NOT_COMPARABLE)return Outcome.NOT_COMPARABLE;if(c==Comparability.UNKNOWN)return Outcome.INCONCLUSIVE;ResultStatus l=b.terminalResult().status(),r=v.terminalResult().status();if(l==ResultStatus.PASSED&&r==ResultStatus.FAILED)return Outcome.VARIANT_REGRESSION;if(l==ResultStatus.FAILED&&r==ResultStatus.PASSED)return Outcome.VARIANT_IMPROVEMENT;if(l==ResultStatus.FAILED&&r==ResultStatus.FAILED)return switch(failure){case SAME_KNOWN->Outcome.BASELINE_ALREADY_FAILED_SAME_FAILURE;case DIFFERENT_KNOWN->Outcome.BASELINE_ALREADY_FAILED_DIFFERENT_FAILURE;default->Outcome.INCONCLUSIVE;};if(l==ResultStatus.PASSED&&r==ResultStatus.PASSED){if(behavior.executionDelta())return behavior.groups().stream().anyMatch(g->g.group().equals("attempts")&&g.state()==BehaviorState.DIFFERENT)?Outcome.PASS_WITH_BEHAVIORAL_DELTA:Outcome.BOTH_PASS_DIFFERENT_BEHAVIOR;if(behavior.expectedPresetDifference())return Outcome.EXPECTED_OBSERVABILITY_DIFFERENCE;return behavior.evidenceLimited()?Outcome.INCONCLUSIVE:Outcome.NO_MEANINGFUL_DIFFERENCE;}return Outcome.INCONCLUSIVE;}

    private Generated findings(String test,String invocation,CompatibilityRunManifest b,CompatibilityRunManifest v,ComparisonIntent intent,ComparabilityResult comp,BehaviorDiff behavior,Outcome outcome,FailureRelationship failure){List<Finding>f=new ArrayList<>();List<Recommendation>r=new ArrayList<>();DimensionAssessment mismatch=comp.dimensions().stream().filter(x->x.state()==DimensionState.DIFFERENT&&x.critical()).findFirst().orElse(null);if(mismatch!=null){RecommendationCode rc=recommendationFor(mismatch.reasonCode());FindingCategory category=mismatch.dimension().startsWith("viewport")?FindingCategory.VIEWPORT_RESPONSIVE:FindingCategory.CONFIGURATION;f.add(finding(test,invocation,intent,category,mismatch.reasonCode(),Severity.WARNING,CausalState.HYPOTHESIS,Confidence.HIGH,StatementKind.CONFIGURATION_DIFFERENCE,List.of(b.manifestId(),v.manifestId())));r.add(new Recommendation(rc,test,mismatch.reasonCode(),CodeChangeRequired.NO));}
        else if(comp.status()==Comparability.UNKNOWN){f.add(finding(test,invocation,intent,FindingCategory.CONFIGURATION,"CRITICAL_METADATA_MISSING",Severity.REVIEW,CausalState.OBSERVATION,Confidence.LOW,StatementKind.MISSING_DATA,List.of(b.manifestId(),v.manifestId())));r.add(new Recommendation(RecommendationCode.COLLECT_MISSING_METADATA,test,"CRITICAL_METADATA_MISSING",CodeChangeRequired.UNKNOWN));}
        if(outcome==Outcome.VARIANT_REGRESSION){FindingCategory cat=intent.axis()==ComparisonIntent.Axis.OBSERVABILITY_MODE?FindingCategory.INFRASTRUCTURE:FindingCategory.APPLICATION_BEHAVIOR;f.add(finding(test,invocation,intent,cat,"VARIANT_RESULT_REGRESSION",Severity.ERROR,CausalState.HYPOTHESIS,comp.status()==Comparability.COMPARABLE?Confidence.HIGH:Confidence.MEDIUM,StatementKind.OBSERVATION,List.of(b.manifestId(),v.manifestId())));r.add(new Recommendation(intent.axis()==ComparisonIntent.Axis.OBSERVABILITY_MODE?RecommendationCode.REVIEW_INFRASTRUCTURE:RecommendationCode.INVESTIGATE_APPLICATION_DIFFERENCE,test,"VARIANT_RESULT_REGRESSION",CodeChangeRequired.UNKNOWN));}
        if(outcome==Outcome.PASS_WITH_BEHAVIORAL_DELTA||outcome==Outcome.BOTH_PASS_DIFFERENT_BEHAVIOR){f.add(finding(test,invocation,intent,FindingCategory.INTERACTION,"PASS_WITH_EXECUTION_DELTA",Severity.REVIEW,CausalState.OBSERVATION,Confidence.MEDIUM,StatementKind.OBSERVATION,List.of(b.manifestId(),v.manifestId())));r.add(new Recommendation(RecommendationCode.COLLECT_MORE_EVIDENCE,test,"PASS_WITH_EXECUTION_DELTA",CodeChangeRequired.UNKNOWN));}
        if(outcome==Outcome.EXPECTED_OBSERVABILITY_DIFFERENCE){f.add(finding(test,invocation,intent,FindingCategory.INFRASTRUCTURE,"EXPECTED_FAST_EVIDENCE_REDUCTION",Severity.INFO,CausalState.OBSERVATION,Confidence.HIGH,StatementKind.FACT,List.of(b.manifestId(),v.manifestId())));r.add(new Recommendation(RecommendationCode.KEEP_CURRENT_TEST,test,"EXPECTED_FAST_EVIDENCE_REDUCTION",CodeChangeRequired.NO));}
        if(behavior.evidenceLimited()&&outcome!=Outcome.EXPECTED_OBSERVABILITY_DIFFERENCE&&f.stream().noneMatch(x->x.code().equals("CRITICAL_METADATA_MISSING"))){f.add(finding(test,invocation,intent,FindingCategory.UNKNOWN,"BEHAVIOR_EVIDENCE_LIMITED",Severity.INFO,CausalState.OBSERVATION,Confidence.LOW,StatementKind.MISSING_DATA,List.of(b.manifestId(),v.manifestId())));}
        if(outcome==Outcome.NO_MEANINGFUL_DIFFERENCE)r.add(new Recommendation(RecommendationCode.KEEP_CURRENT_TEST,test,"NO_MEANINGFUL_DIFFERENCE",CodeChangeRequired.NO));if(outcome==Outcome.INCONCLUSIVE&&r.isEmpty())r.add(new Recommendation(RecommendationCode.INCONCLUSIVE,test,"INCONCLUSIVE",CodeChangeRequired.UNKNOWN));return new Generated(f,r);}
    private static Finding finding(String test,String invocation,ComparisonIntent intent,FindingCategory category,String code,Severity severity,CausalState causal,Confidence confidence,StatementKind kind,List<String>refs){String id="compatibility-finding-v1:sha256:"+CompatibilityDigests.digest("compatibility-finding-v1","1","1",test,invocation,intent.axis().name(),category.name(),code,String.join("|",new TreeSet<>(refs)));return new Finding(id,test,invocation,category,code,severity,causal,confidence,List.of(new EvidenceStatement(kind,code,refs)),refs,List.of());}
    private static RecommendationCode recommendationFor(String reason){if(reason.contains("VIEWPORT"))return RecommendationCode.ALIGN_VIEWPORT_AND_RERUN;if(reason.contains("DATASET"))return RecommendationCode.ALIGN_DATASET_AND_RERUN;if(reason.contains("BROWSER"))return RecommendationCode.ALIGN_BROWSER_VERSION_AND_RERUN;if(reason.contains("TEST_REVISION"))return RecommendationCode.ALIGN_TEST_REVISION;if(reason.contains("SUT_REVISION"))return RecommendationCode.ALIGN_SUT_REVISION;return RecommendationCode.COLLECT_MISSING_METADATA;}
    private static CodeChangeRequired codeChange(Outcome o,ComparabilityResult c,List<Recommendation>r){if(o==Outcome.NO_MEANINGFUL_DIFFERENCE||o==Outcome.EXPECTED_OBSERVABILITY_DIFFERENCE||r.stream().anyMatch(x->x.code()==RecommendationCode.ALIGN_VIEWPORT_AND_RERUN))return CodeChangeRequired.NO;return CodeChangeRequired.UNKNOWN;}
    private static String verification(RecommendationCode c){return switch(c){case ALIGN_VIEWPORT_AND_RERUN->"Rerun the same invocation with aligned viewport";case ALIGN_DATASET_AND_RERUN->"Rerun with the same dataset";case ALIGN_BROWSER_VERSION_AND_RERUN->"Rerun with aligned browser version";case ALIGN_TEST_REVISION->"Compare the same test source revision";case ALIGN_SUT_REVISION->"Compare the same system-under-test revision";case COLLECT_MISSING_METADATA->"Collect missing controlled-dimension metadata";case COLLECT_MORE_EVIDENCE->"Repeat controlled capture with complete behavior evidence";case KEEP_CURRENT_TEST->"Keep current test and preserve comparison coverage";default->"Review supplied manifests and repeat a controlled comparison";};}

    private static Map<String,List<CompatibilityRunManifest>>index(List<CompatibilityRunManifest>values){Map<String,List<CompatibilityRunManifest>>out=new TreeMap<>();for(CompatibilityRunManifest m:values){String k=matchKey(m);if(k!=null)out.computeIfAbsent(k,x->new ArrayList<>()).add(m);}out.values().forEach(x->x.sort(Comparator.comparing(CompatibilityRunManifest::manifestId)));return out;}
    private static String matchKey(CompatibilityRunManifest m){if(m.testIdentity().knowledge()!=Knowledge.KNOWN)return null;TestKey k=m.testIdentity().value();String invocation=k.invocationDiscriminator().knowledge()==Knowledge.KNOWN?k.invocationDiscriminator().value():"UNKNOWN";if(k.parameterizedOrDynamic()&&k.invocationDiscriminator().knowledge()!=Knowledge.KNOWN)return null;return k.testIdentityRef()+"|"+invocation;}
    private static Input deduplicate(List<CompatibilityRunManifest>input,String side){Objects.requireNonNull(input,side);if(input.size()>MAX_MANIFESTS_PER_SIDE)throw new IllegalArgumentException(side+" exceeds "+MAX_MANIFESTS_PER_SIDE);Map<String,CompatibilityRunManifest>unique=new TreeMap<>();int rejected=0,duplicates=0;for(CompatibilityRunManifest m:input){if(m==null||matchKey(m)==null){rejected++;continue;}if(unique.putIfAbsent(m.manifestId(),m)!=null)duplicates++;}return new Input(List.copyOf(unique.values()),rejected,duplicates);}
    private static Coverage coverage(int bs,int vs,Input b,Input v,List<TestComparison>c,List<Unmatched>ub,List<Unmatched>uv,List<AmbiguousMatch>a){return new Coverage(bs,vs,b.accepted.size(),v.accepted.size(),b.duplicates+v.duplicates,c.size(),ub.size(),uv.size(),a.size(),count(c,Comparability.COMPARABLE),count(c,Comparability.COMPARABLE_WITH_LIMITATIONS),count(c,Comparability.UNKNOWN),count(c,Comparability.NOT_COMPARABLE),(int)c.stream().filter(x->x.outcome()==Outcome.NO_MEANINGFUL_DIFFERENCE).count(),(int)c.stream().filter(x->x.outcome()==Outcome.PASS_WITH_BEHAVIORAL_DELTA||x.outcome()==Outcome.BOTH_PASS_DIFFERENT_BEHAVIOR).count(),(int)c.stream().filter(x->x.outcome()==Outcome.VARIANT_REGRESSION).count(),(int)c.stream().filter(TestComparison::baselineAlreadyFailed).count(),(int)c.stream().filter(x->x.behaviorDiff().evidenceLimited()).count());}
    private static int count(List<TestComparison>c,Comparability s){return(int)c.stream().filter(x->x.comparability().status()==s).count();}
    private static Unmatched unmatched(CompatibilityRunManifest m,String reason){return new Unmatched(m.testIdentity().knowledge()==Knowledge.KNOWN?m.testIdentity().value().testIdentityRef():"UNKNOWN",m.manifestId(),reason);}
    private static List<String>ids(List<CompatibilityRunManifest>m){return m.stream().map(CompatibilityRunManifest::manifestId).sorted().toList();}
    private static ManifestSide side(CompatibilityRunManifest m){return new ManifestSide(m.manifestId(),m.terminalResult().status(),m.terminalResult().attemptCount(),m.terminalResult().passedAfterRetry(),statuses(m));}
    private static List<ResultStatus>statuses(CompatibilityRunManifest m){return m.attempts().stream().map(Attempt::result).toList();}
    private static Attempt last(CompatibilityRunManifest m){return m.attempts().get(m.attempts().size()-1);}
    private static String invocation(CompatibilityRunManifest m){Fact<String>f=m.testIdentity().value().invocationDiscriminator();return f.knowledge()==Knowledge.KNOWN?f.value():"UNKNOWN";}
    private record Input(List<CompatibilityRunManifest>accepted,int rejected,int duplicates){}
    private record Compared(TestComparison comparison,List<Finding>findings,List<Recommendation>recommendations){}
    private record Generated(List<Finding>findings,List<Recommendation>recommendations){}
}
