package io.github.testlens.selenium.network;

import org.junit.jupiter.api.Test;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SeleniumBiDiCompatibilityTest {
    @Test
    void nullableSizeNormalizationDoesNotMutateProtocolPayloadOrAlterOtherFields() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("headersSize", 0L);
        request.put("url", "/api");
        Map<String, Object> response = new LinkedHashMap<>();
        long large = (long) Integer.MAX_VALUE + 9876;
        response.put("bytesReceived", large);
        response.put("headersSize", large);
        response.put("bodySize", null);
        Map<String, Object> input = new LinkedHashMap<>(Map.of("request", request, "response", response));

        Map<String, Object> normalized = SeleniumBiDiCompatibility.normalizeSizes(input);
        assertEquals(0L, request.get("headersSize"));
        assertNull(response.get("bodySize"));
        assertEquals(large, ((Map<?, ?>) normalized.get("response")).get("bytesReceived"));
        assertEquals(large, ((Map<?, ?>) normalized.get("response")).get("headersSize"));
        assertEquals("/api", ((Map<?, ?>) normalized.get("request")).get("url"));
        assertEquals(java.util.Set.of("request", "response"), normalized.keySet());
    }

    @Test
    void acceptsPrimitiveAndBoxedSizesWithoutTruncatingAndPreservesNull() {
        long large = (long) Integer.MAX_VALUE + 9876;
        assertEquals(large, SeleniumBiDiCompatibility.size(new PrimitiveSize(large), "getSize"));
        assertEquals(large, SeleniumBiDiCompatibility.size(new BoxedSize(large), "getSize"));
        assertNull(SeleniumBiDiCompatibility.size(new BoxedSize(null), "getSize"));
    }

    @Test
    void unsupportedAccessorShapeFailsInsteadOfSilentlyDroppingMetadata() {
        BiDiCaptureException failure = assertThrows(BiDiCaptureException.class,
                () -> SeleniumBiDiCompatibility.size(new UnsupportedSize(), "getSize"));
        assertEquals(BiDiFailureCategory.BINARY_INCOMPATIBILITY, failure.category());
    }

    @Test
    void missingPublicAccessorAndLinkageFailureHaveBinaryIncompatibilityClassification() {
        BiDiCaptureException missing = assertThrows(BiDiCaptureException.class,
                () -> SeleniumBiDiCompatibility.size(new Object(), "getSize"));
        assertEquals(BiDiFailureCategory.BINARY_INCOMPATIBILITY, missing.category());
        BiDiCaptureException linkage = assertThrows(BiDiCaptureException.class,
                () -> SeleniumBiDiCompatibility.size(new BrokenSize(), "getSize"));
        assertEquals(BiDiFailureCategory.BINARY_INCOMPATIBILITY, linkage.category());
        assertInstanceOf(NoSuchMethodError.class, linkage.getCause());
    }

    public record PrimitiveSize(long value) { public long getSize() { return value; } }
    public record BoxedSize(Long value) { public Long getSize() { return value; } }
    public static final class UnsupportedSize { public String getSize() { return "42"; } }
    public static final class BrokenSize {
        public long getSize() { throw new NoSuchMethodError("incompatible accessor"); }
    }
}
