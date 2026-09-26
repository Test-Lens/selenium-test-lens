package io.github.testlens.selector.tooling;

import io.github.testlens.selector.engine.CompiledPolicySet;
import io.github.testlens.selector.engine.SelectorPolicy;
import io.github.testlens.selector.engine.SelectorSubject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SelectorPolicyJsonTest {
    @TempDir Path temp;

    @Test void emptyRulesRoundTripIsDeterministic(){
        byte[] first=SelectorPolicyJson.serialize(SelectorPolicy.Document.empty());
        assertArrayEquals(first,SelectorPolicyJson.serialize(SelectorPolicyJson.parse(first)));
        assertEquals("{\"schemaVersion\":1,\"canonicalizationVersion\":1,\"rules\":[]}\n",new String(first,StandardCharsets.UTF_8));
    }

    @Test void exactStableAndUnstableRoundTripWithoutPlaintextOrDisplayHint(){
        SelectorSubject secret=SelectorSubject.trusted("id","account-550e8400-e29b-41d4-a716-446655440000");
        SelectorPolicy.Rule stable=SelectorPolicy.Rule.create(SelectorPolicy.Decision.STABLE,0,SelectorPolicy.Scope.project(),SelectorPolicy.ExactMatcher.from(secret),new SelectorPolicy.Reason("USER",null));
        SelectorPolicy.Rule unstable=SelectorPolicy.Rule.create(SelectorPolicy.Decision.UNSTABLE,1,SelectorPolicy.Scope.project(),SelectorPolicy.ExactMatcher.from(SelectorSubject.trusted("name","volatile")),new SelectorPolicy.Reason("USER","Zażółć ✅"));
        SelectorPolicy.Document document=doc(unstable,stable);byte[] bytes=SelectorPolicyJson.serialize(document);String json=new String(bytes,StandardCharsets.UTF_8);
        assertFalse(json.contains(secret.canonicalValue()));assertFalse(json.contains("displayHint"));
        assertEquals(document.canonical(),SelectorPolicyJson.parse(bytes).canonical());
        assertArrayEquals(bytes,SelectorPolicyJson.serialize(SelectorPolicyJson.parse(bytes)));
    }

    @Test void structuralPatternAndAllScopeFieldsRoundTrip(){
        String context="context-v1:sha256:"+"a".repeat(64);
        SelectorPolicy.Scope scope=new SelectorPolicy.Scope("module","src/test/A.java","decl","Type#method","Use","test",context);
        SelectorPolicy.StructuralPattern pattern=new SelectorPolicy.StructuralPattern("id",List.of(SelectorPolicy.Segment.literal("item-"),SelectorPolicy.Segment.run(SelectorPolicy.SegmentKind.DECIMAL_RUN,1,8),SelectorPolicy.Segment.opaque("abc123",2,4)));
        SelectorPolicy.Rule rule=SelectorPolicy.Rule.create(SelectorPolicy.Decision.STABLE,42,scope,pattern,new SelectorPolicy.Reason("REVIEWED","Uwagi ✅"));
        assertEquals(doc(rule),SelectorPolicyJson.parse(SelectorPolicyJson.serialize(doc(rule))));
    }

    @Test void explicitlySuppliedDisplayHintRoundTripsButDoesNotAffectRuleIdentity(){
        SelectorSubject subject=SelectorSubject.trusted("id","secret-value");String digest=io.github.testlens.selector.engine.CanonicalDigests.exactValueDigest(subject);
        SelectorPolicy.Rule without=SelectorPolicy.Rule.create(SelectorPolicy.Decision.STABLE,0,SelectorPolicy.Scope.project(),new SelectorPolicy.ExactMatcher("id",digest,null),new SelectorPolicy.Reason("USER",null));
        SelectorPolicy.Rule with=SelectorPolicy.Rule.create(SelectorPolicy.Decision.STABLE,0,SelectorPolicy.Scope.project(),new SelectorPolicy.ExactMatcher("id",digest,"reviewed hint"),new SelectorPolicy.Reason("USER",null));
        assertEquals(without.ruleId(),with.ruleId());String json=new String(SelectorPolicyJson.serialize(doc(with)),StandardCharsets.UTF_8);
        assertTrue(json.contains("reviewed hint"));assertFalse(json.contains("secret-value"));assertEquals(doc(with),SelectorPolicyJson.parse(json.getBytes(StandardCharsets.UTF_8)));
    }

    @Test void forwardExtensionIsAllowedOnlyWhenExplicitlyNamespaced(){
        String base=new String(SelectorPolicyJson.serialize(SelectorPolicy.Document.empty()),StandardCharsets.UTF_8).trim();
        String extension=base.substring(0,base.length()-1)+",\"x-future\":{\"nested\":[1,true]}}";
        assertEquals(SelectorPolicy.Document.empty(),SelectorPolicyJson.parse(extension.getBytes(StandardCharsets.UTF_8)));
        assertThrows(SelectorPolicyJson.PolicyFormatException.class,()->SelectorPolicyJson.parse(base.replace("\"rules\"","\"future\"").getBytes(StandardCharsets.UTF_8)));
    }

    @Test void strictSchemaTypesEnumsAndRequiredFieldsAreRejected(){
        assertInvalid("{\"schemaVersion\":2,\"canonicalizationVersion\":1,\"rules\":[]}");
        assertInvalid("{\"schemaVersion\":\"1\",\"canonicalizationVersion\":1,\"rules\":[]}");
        assertInvalid("{\"schemaVersion\":1,\"canonicalizationVersion\":1}");
        SelectorPolicy.Rule rule=exactRule();String json=new String(SelectorPolicyJson.serialize(doc(rule)),StandardCharsets.UTF_8);
        assertInvalid(json.replace("\"STABLE\"","\"MAYBE\""));
        assertInvalid(json.replace("\"EXACT_VALUE_DIGEST\"","\"REGEX\""));
        assertInvalid(json.replace(rule.matcher() instanceof SelectorPolicy.ExactMatcher e?e.valueDigest():"never","bad"));
        assertInvalid(json.replace("\"valueDigest\":","\"x-valueDigest\":"));
    }

    @Test void duplicateFieldsAndRuleIdsAreRejected(){
        assertInvalid("{\"schemaVersion\":1,\"schemaVersion\":1,\"canonicalizationVersion\":1,\"rules\":[]}");
        SelectorPolicy.Rule rule=exactRule();String object=ruleObject(rule);
        assertInvalid("{\"schemaVersion\":1,\"canonicalizationVersion\":1,\"rules\":["+object+","+object+"]}");
    }

    @Test void resourceConstraintsRejectOversizeNestingStringsNumbersAndControls(){
        assertThrows(SelectorPolicyJson.PolicyFormatException.class,()->SelectorPolicyJson.parse(new byte[SelectorPolicyJson.MAX_DOCUMENT_BYTES+1]));
        String nested="{\"schemaVersion\":1,\"canonicalizationVersion\":1,\"rules\":[],\"x-deep\":"+"[".repeat(30)+"0"+"]".repeat(30)+"}";assertInvalid(nested);
        String huge="{\"schemaVersion\":1,\"canonicalizationVersion\":1,\"rules\":[],\"x-text\":\""+"a".repeat(SelectorPolicyJson.MAX_STRING_LENGTH*2)+"\"}";assertInvalid(huge);
        assertInvalid("{\"schemaVersion\":12345678901234567890,\"canonicalizationVersion\":1,\"rules\":[]}");
        assertInvalid("{\"schemaVersion\":1,\"canonicalizationVersion\":1,\"rules\":[],\"x-text\":\"bad\u0001text\"}");
    }

    @Test void trackedAndLocalMergeCoalescesIdenticalRule(){
        SelectorPolicy.Rule same=exactRule();SelectorPolicy.Rule local=SelectorPolicy.Rule.create(SelectorPolicy.Decision.UNSTABLE,0,SelectorPolicy.Scope.project(),SelectorPolicy.ExactMatcher.from(SelectorSubject.trusted("id","other")),new SelectorPolicy.Reason("LOCAL",null));
        SelectorPolicy.Document merged=SelectorPolicyJson.merge(doc(same),doc(same,local));
        assertEquals(2,merged.rules().size());assertDoesNotThrow(()->CompiledPolicySet.compile(merged));
    }

    @Test void atomicWriteAndMoveFailurePreserveDestination()throws Exception{
        Path root=Files.createDirectory(temp.resolve("project"));Path destination=root.resolve(".test-lens/selector-policies.json");
        SelectorPolicyJson.writeAtomic(doc(exactRule()),root,destination);byte[] original=Files.readAllBytes(destination);
        IOException failure=assertThrows(IOException.class,()->SelectorPolicyJson.writeAtomic(SelectorPolicy.Document.empty(),root,destination,(source,target)->{throw new IOException("synthetic move failure");}));
        assertEquals("synthetic move failure",failure.getMessage());assertArrayEquals(original,Files.readAllBytes(destination));
        assertThrows(IllegalArgumentException.class,()->SelectorPolicyJson.writeAtomic(SelectorPolicy.Document.empty(),root,root.resolve("../outside.json")));
    }

    private static SelectorPolicy.Rule exactRule(){SelectorSubject s=SelectorSubject.trusted("id","save");return SelectorPolicy.Rule.create(SelectorPolicy.Decision.STABLE,0,SelectorPolicy.Scope.project(),SelectorPolicy.ExactMatcher.from(s),new SelectorPolicy.Reason("USER",null));}
    private static SelectorPolicy.Document doc(SelectorPolicy.Rule... rules){return new SelectorPolicy.Document(1,1,List.of(rules));}
    private static void assertInvalid(String json){assertThrows(RuntimeException.class,()->SelectorPolicyJson.parse(json.getBytes(StandardCharsets.UTF_8)));}
    private static String ruleObject(SelectorPolicy.Rule rule){String json=new String(SelectorPolicyJson.serialize(doc(rule)),StandardCharsets.UTF_8).trim();return json.substring(json.indexOf('[')+1,json.lastIndexOf(']'));}
}
