package io.github.testlens.selector.lab;

import io.github.testlens.selector.engine.ObservationEvidence;
import io.github.testlens.selector.engine.PolicyWorkspaceSnapshot;
import io.github.testlens.selector.engine.SelectorPolicy;
import io.github.testlens.selector.engine.SelectorSubject;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelectorLabPolicyViewTest {
    @Test void showsEffectiveOriginAndShadowedRulesWithoutRawValueOrNote(){
        SelectorSubject subject=subject();
        SelectorPolicy.Rule broad=rule(SelectorPolicy.Decision.STABLE,SelectorPolicy.Scope.project(),subject,"TRACKED_NOTE_SECRET");
        SelectorPolicy.Rule narrow=rule(SelectorPolicy.Decision.STABLE,new SelectorPolicy.Scope(null,null,"decl",null,null,null,null),subject,"LOCAL_NOTE_SECRET");
        PolicyWorkspaceSnapshot workspace=PolicyWorkspaceSnapshot.create(document(PolicyWorkspaceSnapshot.Origin.TRACKED,List.of(broad)),document(PolicyWorkspaceSnapshot.Origin.LOCAL,List.of(narrow)),"project");
        Map<String,Object> view=SelectorLabPolicyView.project(workspace,subject,ObservationEvidence.unavailable());
        assertEquals("STABLE",view.get("effective"));assertEquals("LOCAL",view.get("selectedOrigin"));assertEquals(1L,view.get("shadowedCount"));
        assertFalse(view.toString().contains(subject.canonicalValue()));assertFalse(view.toString().contains("NOTE_SECRET"));
    }

    @Test void exposesExistingPolicyConflictAsFact(){
        SelectorSubject subject=subject();
        SelectorPolicy.Rule stable=rule(SelectorPolicy.Decision.STABLE,SelectorPolicy.Scope.project(),subject,null);
        SelectorPolicy.Rule unstable=rule(SelectorPolicy.Decision.UNSTABLE,SelectorPolicy.Scope.project(),subject,null);
        PolicyWorkspaceSnapshot workspace=PolicyWorkspaceSnapshot.create(document(PolicyWorkspaceSnapshot.Origin.TRACKED,List.of(stable)),document(PolicyWorkspaceSnapshot.Origin.LOCAL,List.of(unstable)),null);
        Map<String,Object> view=SelectorLabPolicyView.project(workspace,subject,ObservationEvidence.unavailable());
        assertEquals("CONFLICT",view.get("effective"));assertTrue((Boolean)view.get("policyConflict"));
    }

    private static SelectorSubject subject(){return new SelectorSubject(SelectorSubject.SubjectKind.STATIC_DECLARATION,"id",SelectorSubject.ValueState.KNOWN,"secret-selector-value","decl","module","src/Page.java","Page#save",null,null,null,SelectorSubject.InputTrust.SOURCE_CANONICAL,null);}
    private static SelectorPolicy.Rule rule(SelectorPolicy.Decision decision,SelectorPolicy.Scope scope,SelectorSubject subject,String note){return SelectorPolicy.Rule.create(decision,0,scope,SelectorPolicy.ExactMatcher.from(subject),new SelectorPolicy.Reason("TEST",note));}
    private static PolicyWorkspaceSnapshot.OriginDocument document(PolicyWorkspaceSnapshot.Origin origin,List<SelectorPolicy.Rule> rules){byte[] bytes=(origin.name()+rules.stream().map(SelectorPolicy.Rule::ruleId).toList()).getBytes(StandardCharsets.UTF_8);return new PolicyWorkspaceSnapshot.OriginDocument(origin,PolicyWorkspaceSnapshot.FileState.EXPECTED_PRESENT,PolicyWorkspaceSnapshot.rawFileDigest(bytes),PolicyWorkspaceSnapshot.semanticDigest(rules),rules);}
}
