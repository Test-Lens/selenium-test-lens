package io.github.testlens.compatibility.engine;

import org.junit.jupiter.api.Test;

import java.util.List;

import static io.github.testlens.compatibility.engine.CompatibilityRunManifest.*;
import static org.junit.jupiter.api.Assertions.*;

class CompatibilityManifestModelTest {
    @Test void structuredIdentityDigestsInvocationAndNeverStoresArguments() {
        CompatibilityCaptureDescriptor d=CompatibilityCaptureDescriptor.builder()
                .testKey(Framework.TESTNG,"example.LoginTest","login","login",true,"row-7:secret","suite-with-params")
                .datasetKey("customer-secret-row").environmentKey("staging")
                .build();
        TestKey key=d.testIdentity().value();
        assertEquals(Framework.TESTNG,key.framework());
        assertTrue(key.invocationDiscriminator().value().startsWith("compatibility-invocation-v1:sha256:"));
        assertFalse(key.toString().contains("row-7:secret"));
        assertFalse(d.context().datasetDigest().value().contains("customer-secret-row"));
        assertFalse(d.context().environmentDigest().value().contains("staging"));
    }

    @Test void manualIdentityWithoutTrustedLogicalKeyIsUnknown() {
        CompatibilityCaptureDescriptor d=CompatibilityCaptureDescriptor.builder()
                .testKey(Framework.MANUAL,"","",null,false,null,null).build();
        assertEquals(Knowledge.UNKNOWN,d.testIdentity().knowledge());
        assertEquals("MANUAL_LOGICAL_KEY_MISSING",d.issues().get(0).code());
    }

    @Test void unknownIsDistinctFromKnownEmpty() {
        Fact<String> unknown=Fact.unknown();
        Fact<String> empty=Fact.known("",Provenance.CALLER_SUPPLIED);
        assertNotEquals(unknown,empty);
        assertEquals(Knowledge.UNKNOWN,unknown.knowledge());
        assertEquals(Knowledge.KNOWN,empty.knowledge());
    }

    @Test void attemptsRetainFailedRetryAndConservativeFailureDigest() {
        FailureSignature failure=CompatibilityAttempts.failure("org.example.TimeoutException","TIMEOUT","click",null,
                List.of("WAIT_TIMEOUT"),"token=secret id 123456 at 0xCAFE",FailurePhase.TEST);
        Attempt first=CompatibilityAttempts.attempt(1,ResultStatus.FAILED,failure,null,null,null);
        Attempt second=CompatibilityAttempts.attempt(2,ResultStatus.PASSED,null,null,null,null);
        assertNotEquals(first.attemptRef(),second.attemptRef());
        assertTrue(failure.normalizedMessageDigest().value().startsWith("compatibility-failure-message-v1:sha256:"));
        assertFalse(failure.toString().contains("token=secret"));
        assertEquals("id <number> at <hex>",CompatibilityAttempts.normalizeMessage("id 123456 at 0xCAFE"));
    }

    @Test void descriptorKeepsRequestedAndEffectiveHeadlessSeparateFromFast() {
        CompatibilityCaptureDescriptor d=CompatibilityCaptureDescriptor.builder()
                .requestedHeadless(RequestedHeadlessMode.HEADLESS,RequestedHeadlessProvenance.EXPLICIT_JAVA)
                .attestEffectiveHeadless(EffectiveHeadlessState.HEADLESS,EffectiveHeadlessProvenance.MANAGED_FACTORY_ATTESTED)
                .observability(ObservabilityMode.FAST,HudPreset.DEBUG,false,false,false,"SUMMARY_ONLY","OFF")
                .build();
        assertEquals(RequestedHeadlessMode.HEADLESS,d.execution().requestedHeadless().value());
        assertEquals(EffectiveHeadlessState.HEADLESS,d.execution().effectiveHeadless().value());
        assertEquals(ObservabilityMode.FAST,d.execution().observabilityMode().value());
        assertEquals(HudPreset.DEBUG,d.execution().hudPreset().value());
    }
}
