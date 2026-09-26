package io.github.testlens.selector.engine;

import org.junit.jupiter.api.Test;

import java.util.List;

import static io.github.testlens.selector.engine.StabilityAssessment.EffectiveDisposition.*;
import static org.junit.jupiter.api.Assertions.*;

class SelectorPolicyEngineTest {
    private final SelectorStabilityEngine engine=new SelectorStabilityEngine();

    @Test void uuidPolicyIsExactAndDoesNotEraseAppearance(){
        SelectorSubject first=subject("550e8400-e29b-41d4-a716-446655440000",null,null);
        SelectorSubject second=subject("123e4567-e89b-42d3-a456-426614174000",null,null);
        assertEquals(REVIEW_GENERATED_LOOKING,analyze(first,List.of()).effectiveDisposition());
        SelectorPolicy.Rule stable=rule(SelectorPolicy.Decision.STABLE,SelectorPolicy.Scope.project(),SelectorPolicy.ExactMatcher.from(first),0);
        StabilityAssessment accepted=analyze(first,List.of(stable));
        assertEquals(DECLARED_STABLE,accepted.effectiveDisposition());
        assertFalse(accepted.appearanceSignals().isEmpty());
        assertEquals(REVIEW_GENERATED_LOOKING,analyze(second,List.of(stable)).effectiveDisposition());
        assertEquals(DECLARED_UNSTABLE,analyze(first,List.of(rule(SelectorPolicy.Decision.UNSTABLE,SelectorPolicy.Scope.project(),SelectorPolicy.ExactMatcher.from(first),0))).effectiveDisposition());
    }

    @Test void canonicalDigestIsAmbiguitySafeAndStrategySensitive(){
        assertEquals(CanonicalDigests.exactValueDigest(SelectorSubject.trusted("id","abc")),CanonicalDigests.exactValueDigest(SelectorSubject.trusted("id","abc")));
        assertNotEquals(CanonicalDigests.exactValueDigest(SelectorSubject.trusted("id","abc")),CanonicalDigests.exactValueDigest(SelectorSubject.trusted("name","abc")));
        assertNotEquals(CanonicalDigests.sha256(CanonicalDigests.lengthPrefixed("a","bc")),CanonicalDigests.sha256(CanonicalDigests.lengthPrefixed("ab","c")));
    }

    @Test void structuralPatternsAreAnchoredAndBounded(){
        SelectorPolicy.StructuralPattern uuid=new SelectorPolicy.StructuralPattern("id",List.of(SelectorPolicy.Segment.literal("user-"),SelectorPolicy.Segment.run(SelectorPolicy.SegmentKind.UUID_LIKE,36,36)));
        assertTrue(uuid.matches(SelectorSubject.trusted("id","user-550e8400-e29b-41d4-a716-446655440000")));
        assertFalse(uuid.matches(SelectorSubject.trusted("id","xuser-550e8400-e29b-41d4-a716-446655440000")));
        SelectorPolicy.StructuralPattern decimal=new SelectorPolicy.StructuralPattern("id",List.of(SelectorPolicy.Segment.literal("item-"),SelectorPolicy.Segment.run(SelectorPolicy.SegmentKind.DECIMAL_RUN,1,8)));
        assertTrue(decimal.matches(SelectorSubject.trusted("id","item-123")));assertFalse(decimal.matches(SelectorSubject.trusted("id","item-123456789")));
        SelectorPolicy.StructuralPattern opaque=new SelectorPolicy.StructuralPattern("id",List.of(SelectorPolicy.Segment.opaque("abc123",3,6)));
        assertTrue(opaque.matches(SelectorSubject.trusted("id","a1b2")));assertFalse(opaque.matches(SelectorSubject.trusted("id","a1Z2")));
        assertThrows(IllegalArgumentException.class,()->SelectorPolicy.Segment.opaque("abc",1,SelectorPolicy.MAX_RUN_LENGTH+1));
    }

    @Test void scopePriorityExactAndConstraintPrecedenceAreDeterministic(){
        SelectorSubject scoped=subject("item-123","decl-X","a/Login.java");
        SelectorPolicy.StructuralPattern broad=new SelectorPolicy.StructuralPattern("id",List.of(SelectorPolicy.Segment.literal("item-"),SelectorPolicy.Segment.run(SelectorPolicy.SegmentKind.DECIMAL_RUN,1,8)));
        SelectorPolicy.Rule projectStable=rule(SelectorPolicy.Decision.STABLE,SelectorPolicy.Scope.project(),broad,999);
        SelectorPolicy.Rule declarationUnstable=rule(SelectorPolicy.Decision.UNSTABLE,new SelectorPolicy.Scope(null,null,"decl-X",null,null,null,null),SelectorPolicy.ExactMatcher.from(scoped),-5);
        assertEquals(DECLARED_UNSTABLE,analyze(scoped,List.of(projectStable,declarationUnstable)).effectiveDisposition());
        SelectorPolicy.Rule pathStable=rule(SelectorPolicy.Decision.STABLE,new SelectorPolicy.Scope(null,"a/Login.java",null,null,null,null,null),broad,1);
        SelectorPolicy.Rule pathUnstable=rule(SelectorPolicy.Decision.UNSTABLE,new SelectorPolicy.Scope(null,"a/Login.java",null,null,null,null,null),broad,2);
        assertEquals(DECLARED_UNSTABLE,analyze(scoped,List.of(pathStable,pathUnstable)).effectiveDisposition());
    }

    @Test void equalOppositePoliciesConflictAndFileOrderCannotResolveIt(){
        SelectorSubject s=subject("item-123",null,null);SelectorPolicy.ExactMatcher exact=SelectorPolicy.ExactMatcher.from(s);
        SelectorPolicy.Rule a=rule(SelectorPolicy.Decision.STABLE,SelectorPolicy.Scope.project(),exact,0),b=rule(SelectorPolicy.Decision.UNSTABLE,SelectorPolicy.Scope.project(),exact,0);
        assertEquals(POLICY_POLICY_CONFLICT,analyze(s,List.of(a,b)).effectiveDisposition());
        assertEquals(POLICY_POLICY_CONFLICT,analyze(s,List.of(b,a)).effectiveDisposition());
    }

    @Test void exactBeatsPatternAtEqualScopeAndPriorityAndAssessmentIsDeterministic(){
        SelectorSubject s=subject("item-123",null,null);
        SelectorPolicy.StructuralPattern pattern=new SelectorPolicy.StructuralPattern("id",List.of(SelectorPolicy.Segment.literal("item-"),SelectorPolicy.Segment.run(SelectorPolicy.SegmentKind.DECIMAL_RUN,1,8)));
        SelectorPolicy.Rule broad=rule(SelectorPolicy.Decision.UNSTABLE,SelectorPolicy.Scope.project(),pattern,0);
        SelectorPolicy.Rule exact=rule(SelectorPolicy.Decision.STABLE,SelectorPolicy.Scope.project(),SelectorPolicy.ExactMatcher.from(s),0);
        StabilityAssessment first=analyze(s,List.of(broad,exact)),second=analyze(s,List.of(exact,broad));
        assertEquals(DECLARED_STABLE,first.effectiveDisposition());assertEquals(first,second);
    }

    @Test void changedComparableEvidenceConflictsWithStablePolicy(){
        SelectorSubject s=subject("fixed-id","decl",null);SelectorPolicy.Rule stable=rule(SelectorPolicy.Decision.STABLE,SelectorPolicy.Scope.project(),SelectorPolicy.ExactMatcher.from(s),0);
        ObservationEvidence changed=ObservationEvidence.of(List.of(new ObservationEvidence.Comparison("run-2",ObservationEvidence.Domain.NEW_RUN,true,true)));
        assertEquals(POLICY_EVIDENCE_CONFLICT,engine.analyze(s,changed,compiled(stable)).effectiveDisposition());
        ObservationEvidence dataset=ObservationEvidence.of(List.of(new ObservationEvidence.Comparison("dataset-b",ObservationEvidence.Domain.DIFFERENT_DATASET,false,true)));
        assertEquals(DECLARED_STABLE,engine.analyze(s,dataset,compiled(stable)).effectiveDisposition());
        assertEquals(OBSERVED_VARIABLE,engine.analyze(s,changed,CompiledPolicySet.empty()).effectiveDisposition());
    }

    @Test void contextRulesRequireKnownMatchingFingerprintAndSameValueCanDifferByScope(){
        String context=CanonicalDigests.contextFingerprint(CanonicalDigests.ContextKnowledge.KNOWN,List.of(
                new CanonicalDigests.ContextSegment("DRIVER_ROOT",null,null,CanonicalDigests.ContextReferenceKind.NONE,null),
                new CanonicalDigests.ContextSegment("FRAME","id","digest",CanonicalDigests.ContextReferenceKind.NAME_OR_ID,"checkout")));
        SelectorSubject known=new SelectorSubject(SelectorSubject.SubjectKind.STATIC_DECLARATION,"id",SelectorSubject.ValueState.KNOWN,"item-123",null,null,"A.java",null,null,null,context,SelectorSubject.InputTrust.SOURCE_CANONICAL,null);
        SelectorSubject other=new SelectorSubject(SelectorSubject.SubjectKind.STATIC_DECLARATION,"id",SelectorSubject.ValueState.KNOWN,"item-123",null,null,"B.java",null,null,null,null,SelectorSubject.InputTrust.SOURCE_CANONICAL,null);
        SelectorPolicy.Rule stable=rule(SelectorPolicy.Decision.STABLE,new SelectorPolicy.Scope(null,null,null,null,null,null,context),SelectorPolicy.ExactMatcher.from(known),0);
        assertEquals(DECLARED_STABLE,analyze(known,List.of(stable)).effectiveDisposition());assertEquals(NO_APPEARANCE_SIGNAL,analyze(other,List.of(stable)).effectiveDisposition());
        assertEquals(context,CanonicalDigests.contextFingerprint(CanonicalDigests.ContextKnowledge.KNOWN,List.of(
                new CanonicalDigests.ContextSegment("DRIVER_ROOT",null,null,CanonicalDigests.ContextReferenceKind.NONE,null),
                new CanonicalDigests.ContextSegment("FRAME","id","digest",CanonicalDigests.ContextReferenceKind.NAME_OR_ID,"checkout"))));
        assertNotEquals(context,CanonicalDigests.contextFingerprint(CanonicalDigests.ContextKnowledge.KNOWN,List.of(
                new CanonicalDigests.ContextSegment("DRIVER_ROOT",null,null,CanonicalDigests.ContextReferenceKind.NONE,null),
                new CanonicalDigests.ContextSegment("FRAME","id","digest",CanonicalDigests.ContextReferenceKind.INDEX,"2"))));
        assertNull(CanonicalDigests.contextFingerprint(CanonicalDigests.ContextKnowledge.PARTIAL,List.of(new CanonicalDigests.ContextSegment("FRAME",null,null,CanonicalDigests.ContextReferenceKind.NONE,null))));
        assertNull(CanonicalDigests.contextFingerprint(CanonicalDigests.ContextKnowledge.KNOWN,List.of(new CanonicalDigests.ContextSegment("WINDOW",null,null,CanonicalDigests.ContextReferenceKind.SESSION_LOCAL_HANDLE,"handle"))));
    }

    @Test void dynamicCustomAndRedactedSubjectsRemainInsufficient(){
        for(SelectorSubject.SubjectKind kind:List.of(SelectorSubject.SubjectKind.TEMPLATE,SelectorSubject.SubjectKind.CUSTOM)){
            SelectorSubject s=new SelectorSubject(kind,"id",SelectorSubject.ValueState.UNAVAILABLE,null,null,null,null,null,null,null,null,SelectorSubject.InputTrust.SOURCE_CANONICAL,"template");
            assertEquals(INSUFFICIENT_DATA,analyze(s,List.of()).effectiveDisposition());
        }
    }

    @Test void ruleIdIgnoresNoteButChangesWithSemantics(){
        SelectorSubject s=SelectorSubject.trusted("id","save");SelectorPolicy.ExactMatcher m=SelectorPolicy.ExactMatcher.from(s);
        SelectorPolicy.Rule a=rule(SelectorPolicy.Decision.STABLE,SelectorPolicy.Scope.project(),m,0,new SelectorPolicy.Reason("USER","first"));
        SelectorPolicy.Rule b=rule(SelectorPolicy.Decision.STABLE,SelectorPolicy.Scope.project(),m,0,new SelectorPolicy.Reason("USER","second"));
        SelectorPolicy.Rule c=rule(SelectorPolicy.Decision.UNSTABLE,SelectorPolicy.Scope.project(),m,0);
        assertEquals(a.ruleId(),b.ruleId());assertNotEquals(a.ruleId(),c.ruleId());
    }

    private StabilityAssessment analyze(SelectorSubject s,List<SelectorPolicy.Rule> rules){return engine.analyze(s,ObservationEvidence.unavailable(),CompiledPolicySet.compile(new SelectorPolicy.Document(1,1,rules)));}
    private static CompiledPolicySet compiled(SelectorPolicy.Rule... rules){return CompiledPolicySet.compile(new SelectorPolicy.Document(1,1,List.of(rules)));}
    private static SelectorPolicy.Rule rule(SelectorPolicy.Decision d,SelectorPolicy.Scope s,SelectorPolicy.Matcher m,int p){return rule(d,s,m,p,new SelectorPolicy.Reason("USER",null));}
    private static SelectorPolicy.Rule rule(SelectorPolicy.Decision d,SelectorPolicy.Scope s,SelectorPolicy.Matcher m,int p,SelectorPolicy.Reason r){return SelectorPolicy.Rule.create(d,p,s,m,r);}
    private static SelectorSubject subject(String value,String ref,String path){return new SelectorSubject(SelectorSubject.SubjectKind.STATIC_DECLARATION,"id",SelectorSubject.ValueState.KNOWN,value,ref,null,path,null,null,null,null,SelectorSubject.InputTrust.SOURCE_CANONICAL,null);}
}
