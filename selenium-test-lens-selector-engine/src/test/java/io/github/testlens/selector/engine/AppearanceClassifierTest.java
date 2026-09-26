package io.github.testlens.selector.engine;

import org.junit.jupiter.api.Test;

import java.util.List;

import static io.github.testlens.selector.engine.AppearanceSignal.Family.*;
import static org.junit.jupiter.api.Assertions.*;

class AppearanceClassifierTest {
    private final AppearanceClassifier classifier=new AppearanceClassifier();

    @Test void calibratedFamiliesAndFalsePositives(){
        assertFamily("id","550e8400-e29b-41d4-a716-446655440000",UUID_LIKE);
        assertFamily("id","user-550e8400-e29b-41d4-a716-446655440000",STABLE_PREFIX_SUSPICIOUS_SUFFIX);
        assertFamily("id","1712345678901",EPOCH_TIMESTAMP_LIKE);
        assertNo("id","product-2024");
        assertNo("id","business-202409");
        assertNo("id","customer-123456");
        assertFamily("id","counter-123456789",LONG_NUMERIC_SEQUENCE_LIKE);
        assertFamily("id","asset-9f86d081884c7d659a2feaa0c55ad015",HEX_HASH_FRAGMENT_LIKE);
        assertNo("id","color-ff00aa");
        assertFamily("id","row-x7q9m2k4p8z1",HIGH_ENTROPY_SUFFIX_LIKE);
        assertNo("id","human-readable-identifier");
        assertFamily("class name","css-1a2b3c",CSS_IN_JS_TOKEN_LIKE);
        assertNo("class name","stable-button");
        assertNo("class name","MuiButton-root");
        assertFamily("class name","Button_root__1a2b3c",CSS_MODULE_TOKEN_LIKE);
        assertFamily("id",":r1a:",REACT_USEID_LIKE);
        assertFamily("id","ember12345",FRAMEWORK_COUNTER_ID_LIKE);
    }

    @Test void onlySimpleCssTokensAreInspected(){
        assertFamily("css selector",".css-1a2b3c",CSS_IN_JS_TOKEN_LIKE);
        assertTrue(classifier.classify(subject("css selector","button.css-1a2b3c:nth-child(2)")).isEmpty());
        assertTrue(classifier.classify(subject("xpath","//*[@id='550e8400-e29b-41d4-a716-446655440000']")).isEmpty());
    }

    @Test void rangesAreUnicodeCodePointOffsets(){
        List<AppearanceSignal> signals=classifier.classify(subject("id","✅-550e8400-e29b-41d4-a716-446655440000"));
        AppearanceSignal uuid=signals.stream().filter(s->s.family()==UUID_LIKE).findFirst().orElseThrow();
        assertEquals(2,uuid.startCodePoint());
    }

    @Test void redactedInputIsInsufficientForAppearanceClassification(){
        SelectorSubject redacted=new SelectorSubject(SelectorSubject.SubjectKind.RUNTIME_OBSERVATION,"id",SelectorSubject.ValueState.KNOWN,"[REDACTED]",null,null,null,null,null,null,null,SelectorSubject.InputTrust.RUNTIME_REDACTED,null);
        assertTrue(classifier.classify(redacted).isEmpty());
        assertThrows(IllegalArgumentException.class,()->CanonicalDigests.exactValueDigest(redacted));
    }

    private void assertFamily(String strategy,String value,AppearanceSignal.Family family){assertTrue(classifier.classify(subject(strategy,value)).stream().anyMatch(s->s.family()==family),value);}
    private void assertNo(String strategy,String value){assertTrue(classifier.classify(subject(strategy,value)).isEmpty(),value);}
    private static SelectorSubject subject(String strategy,String value){return SelectorSubject.trusted(strategy,value);}
}
