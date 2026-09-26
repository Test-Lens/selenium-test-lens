package io.github.testlens.selector.engine;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.github.testlens.selector.engine.CandidateAnalysis.*;
import static org.junit.jupiter.api.Assertions.*;

class CandidateEngineTest {
    private final CandidateGenerator generator=new CandidateGenerator();

    @Test void generatesAllV1SourcesDeduplicatesOriginalAndHonorsBounds(){
        TargetSnapshot snapshot=new TargetSnapshot("a","save","submit",List.of("save","css-1a2b3c"),Map.of("data-testid","save-test","data-qa","save-qa"),"Zapisz 'teraz' \"OK\"","Zapisz","button",
                List.of(new TargetSnapshot.AncestorHint(1,"form","checkout",Map.of(),List.of("form"))),0,false);
        CandidateGenerator.Generated result=generator.generate(snapshot,request(new Locator("id","save"),List.of("data-testid","data-qa"),CompiledPolicySet.empty(),ObservationEvidence.unavailable()));
        assertTrue(result.generatedBeforeDedup()>result.candidates().size());
        assertTrue(result.candidates().size()<=50);
        Candidate id=find(result.candidates(),"id","save");assertEquals(List.of(Origin.ORIGINAL,Origin.ID),id.origins());
        assertNotNull(find(result.candidates(),"css selector","[data-testid=\"save-test\"]"));
        assertNotNull(find(result.candidates(),"name","submit"));assertNotNull(find(result.candidates(),"class name","save"));
        assertNotNull(find(result.candidates(),"css selector","a.save"));assertNotNull(find(result.candidates(),"tag name","a"));
        assertNotNull(find(result.candidates(),"link text","Zapisz 'teraz' \"OK\""));
        assertTrue(result.candidates().stream().anyMatch(c->c.origins().contains(Origin.TEXT_XPATH)&&c.locator().value().contains("concat(")));
        assertTrue(result.candidates().stream().anyMatch(c->c.origins().contains(Origin.STABLE_ANCESTOR)&&c.complexity().ancestorDepth()==1&&c.stabilityComponents().size()==2));
    }

    @Test void cssAndXpathEscapingAreLayerSpecificAndUnicodeSafe(){
        assertEquals("\\-",CandidateGenerator.cssIdentifier("-"));
        assertEquals("\\fffd ",CandidateGenerator.cssIdentifier("\0"));
        assertTrue(CandidateGenerator.cssIdentifier("1 a\\✅").startsWith("\\31 "));
        assertEquals("a\\\"b\\\\c\\a ",CandidateGenerator.cssString("a\"b\\c\n"));
        assertEquals("'simple'",CandidateGenerator.xpathLiteral("simple"));
        assertEquals("\"it's\"",CandidateGenerator.xpathLiteral("it's"));
        assertTrue(CandidateGenerator.xpathLiteral("a'\"b").startsWith("concat("));
    }

    @Test void exactStableUuidKeepsAppearanceAndCanRankAboveText(){
        String uuid="550e8400-e29b-41d4-a716-446655440000";SelectorSubject subject=SelectorSubject.trusted("id",uuid);
        SelectorPolicy.Rule rule=SelectorPolicy.Rule.create(SelectorPolicy.Decision.STABLE,0,SelectorPolicy.Scope.project(),SelectorPolicy.ExactMatcher.from(subject),new SelectorPolicy.Reason("TEST",null));
        CompiledPolicySet policies=CompiledPolicySet.compile(new SelectorPolicy.Document(1,1,List.of(rule)));
        TargetSnapshot snapshot=new TargetSnapshot("button",uuid,null,List.of(),Map.of(),"Save","Save","button",List.of(),0,false);
        Candidate id=find(generator.generate(snapshot,request(null,List.of("data-testid"),policies,ObservationEvidence.unavailable())).candidates(),"id",uuid);
        assertEquals(StabilityAssessment.EffectiveDisposition.DECLARED_STABLE,id.stabilityComponents().get(0).assessment().effectiveDisposition());
        assertFalse(id.stabilityComponents().get(0).assessment().appearanceSignals().isEmpty());
        ObservationEvidence changed=ObservationEvidence.of(List.of(new ObservationEvidence.Comparison("run-2",ObservationEvidence.Domain.NEW_RUN,true,true)));
        Candidate conflicted=find(generator.generate(snapshot,request(null,List.of("data-testid"),policies,changed)).candidates(),"id",uuid);
        assertEquals(StabilityAssessment.EffectiveDisposition.POLICY_EVIDENCE_CONFLICT,conflicted.stabilityComponents().get(0).assessment().effectiveDisposition());
        assertEquals(Recommendation.REVIEW_REQUIRED,new CandidateRanker().recommend(new CandidateRanker().rank(List.of(conflicted.withValidation(new Validation(ValidationState.VERIFIED_IN_SCOPE,1,TargetComparison.SAME_TARGET,List.of())))),null,true));
    }

    @Test void correctnessAlwaysBeatsPolicyAndOriginalWinsExactTie(){
        Candidate original=candidate("a",true,ValidationState.VERIFIED_IN_SCOPE,StabilityAssessment.EffectiveDisposition.NO_APPEARANCE_SIGNAL,SemanticPreference.ID);
        Candidate wrongStable=candidate("b",false,ValidationState.WRONG_TARGET,StabilityAssessment.EffectiveDisposition.DECLARED_STABLE,SemanticPreference.PREFERRED_TEST_ATTRIBUTE);
        Candidate same=candidate("c",false,ValidationState.VERIFIED_IN_SCOPE,StabilityAssessment.EffectiveDisposition.NO_APPEARANCE_SIGNAL,SemanticPreference.ID);
        List<Candidate> ranked=new CandidateRanker().rank(List.of(wrongStable,same,original));
        assertEquals("a",ranked.get(0).candidateId());assertEquals("b",ranked.get(2).candidateId());
        assertEquals(Recommendation.KEEP_CURRENT,new CandidateRanker().recommend(ranked,"a",true));
    }

    @Test void ambiguityAndUnavailableComparisonRequireReview(){
        Candidate ambiguous=candidate("a",true,ValidationState.VALID_BUT_AMBIGUOUS,StabilityAssessment.EffectiveDisposition.NO_APPEARANCE_SIGNAL,SemanticPreference.ID);
        Candidate unavailable=candidate("b",false,ValidationState.NOT_LIVE_VALIDATED,StabilityAssessment.EffectiveDisposition.NO_APPEARANCE_SIGNAL,SemanticPreference.ID);
        CandidateRanker ranker=new CandidateRanker();
        assertEquals(Recommendation.REVIEW_REQUIRED,ranker.recommend(ranker.rank(List.of(ambiguous)),"a",true));
        assertEquals(Recommendation.REVIEW_REQUIRED,ranker.recommend(ranker.rank(List.of(unavailable)),"a",true));
    }

    private static Candidate candidate(String id,boolean original,ValidationState state,StabilityAssessment.EffectiveDisposition disposition,SemanticPreference preference){
        SelectorSubject subject=SelectorSubject.trusted("id",id);StabilityAssessment base=new SelectorStabilityEngine().analyze(subject,ObservationEvidence.unavailable(),CompiledPolicySet.empty());
        StabilityAssessment assessment=new StabilityAssessment(base.schemaVersion(),base.engine(),base.subject(),base.appearanceSignals(),base.evidenceSummary(),base.policyEvaluation(),base.validation(),disposition,base.issues(),base.explanations());
        return new Candidate(id,new Locator("id",id),false,List.of(original?Origin.ORIGINAL:Origin.ID),List.of(new StabilityComponent("TARGET","id",id,assessment)),new Validation(state,state==ValidationState.WRONG_TARGET?1:1,state==ValidationState.WRONG_TARGET?TargetComparison.DIFFERENT_TARGET:TargetComparison.SAME_TARGET,List.of()),new Complexity(ScopeFragility.DIRECT,preference,0,0,id.length(),1),List.of(),List.of(),original);
    }
    private static Candidate find(List<Candidate> values,String strategy,String value){return values.stream().filter(c->c.locator()!=null&&strategy.equals(c.locator().strategy())&&value.equals(c.locator().value())).findFirst().orElseThrow();}
    private static CandidateGenerator.Request request(Locator original,List<String> attrs,CompiledPolicySet policies,ObservationEvidence evidence){return new CandidateGenerator.Request(original,attrs,null,null,null,null,null,null,null,policies,evidence);}
}
