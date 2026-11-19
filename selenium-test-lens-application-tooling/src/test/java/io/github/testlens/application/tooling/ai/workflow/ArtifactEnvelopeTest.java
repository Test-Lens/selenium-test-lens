package io.github.testlens.application.tooling.ai.workflow;

import io.github.testlens.application.tooling.ai.ContractHeader;
import io.github.testlens.application.tooling.ai.TestPlan;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArtifactEnvelopeTest {
    @Test void defensivelyCopiesByteArrayAtIngressAndAccessor() {
        byte[] input = { 1, 2, 3 };
        ArtifactEnvelope<byte[]> envelope = ArtifactEnvelope.create("bytes", "run", List.of(), 1, input);
        input[0] = 9;
        assertArrayEquals(new byte[] { 1, 2, 3 }, envelope.payload());

        byte[] returned = envelope.payload();
        returned[1] = 9;
        assertArrayEquals(new byte[] { 1, 2, 3 }, envelope.payload());
        assertEquals(ArtifactEnvelope.digest(new byte[] { 1, 2, 3 }), envelope.payloadDigest());
    }

    @Test void publicConstructorRejectsDigestMismatch() {
        assertThrows(IllegalArgumentException.class,
                () -> new ArtifactEnvelope<>("id", "run", List.of(), 1, ArtifactEnvelope.digest("different"), "payload"));
    }

    @Test void publicConstructorUsesTheSameByteArraySnapshotContract() {
        byte[] input = { 4, 5, 6 };
        ArtifactEnvelope<byte[]> envelope = new ArtifactEnvelope<>("id", "run", List.of("parent"), 2,
                ArtifactEnvelope.digest(input), input);
        input[0] = 0;
        byte[] returned = envelope.payload();
        returned[1] = 0;
        assertArrayEquals(new byte[] { 4, 5, 6 }, envelope.payload());
        assertEquals(ArtifactEnvelope.digest(envelope.payload()), envelope.payloadDigest());
        assertEquals(List.of("parent"), envelope.parents());
        assertEquals(2, envelope.attempt());
    }

    @Test void metadataAndParentValidationRemainActive() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> ArtifactEnvelope.create("", "run", List.of(), 1, "payload")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> ArtifactEnvelope.create("id", "", List.of(), 1, "payload")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> ArtifactEnvelope.create("id", "run", List.of(), 0, "payload")));
        var parents = new ArrayList<>(List.of("parent"));
        var envelope = ArtifactEnvelope.create("id", "run", parents, 1, "payload");
        parents.add("late");
        assertEquals(List.of("parent"), envelope.parents());
        assertThrows(UnsupportedOperationException.class, () -> envelope.parents().add("mutate"));
    }

    @Test void rejectsMutablePayloadShapesInsteadOfClaimingImmutableProvenance() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> ArtifactEnvelope.create("list", "run", List.of(), 1, new ArrayList<>(List.of("value")))),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> ArtifactEnvelope.create("custom", "run", List.of(), 1, new MutablePayload("value"))),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> ArtifactEnvelope.create("enum", "run", List.of(), 1, MutableEnum.VALUE)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> ArtifactEnvelope.create("nested", "run", List.of(), 1,
                                new NestedPayload(List.of(new ArrayList<>(List.of("value")))))));
    }

    @Test void acceptsKnownImmutableWorkflowPayload() {
        ContractHeader header = new ContractHeader(ContractHeader.SCHEMA_VERSION, ContractHeader.Status.READY,
                List.of("fixture"), List.of(), ContractHeader.Confidence.OBSERVED);
        TestPlan plan = new TestPlan(header, List.of());
        ArtifactEnvelope<TestPlan> envelope = ArtifactEnvelope.create("plan", "run", List.of("context"), 1, plan);
        assertSame(plan, envelope.payload());
        assertEquals(ArtifactEnvelope.digest(plan), envelope.payloadDigest());
    }

    private static final class MutablePayload {
        private String value;
        private MutablePayload(String value) { this.value = value; }
        @Override public String toString() { return value; }
    }

    private record NestedPayload(List<ArrayList<String>> values) { }
    private enum MutableEnum {
        VALUE;
        private int state;
        @Override public String toString() { return Integer.toString(state); }
    }
}
