package io.github.testlens.selector.engine;

import java.util.Objects;

/** Explainable appearance finding; it is not a stability verdict. */
public record AppearanceSignal(Family family, Confidence confidence, String detector, int detectorVersion,
                               String subjectPart, int startCodePoint, int endCodePoint, String fragmentDigest,
                               String reasonCode, String explanation) {
    public AppearanceSignal {
        Objects.requireNonNull(family); Objects.requireNonNull(confidence); Objects.requireNonNull(detector);
        Objects.requireNonNull(subjectPart); Objects.requireNonNull(fragmentDigest); Objects.requireNonNull(reasonCode);
        Objects.requireNonNull(explanation);
        if (detectorVersion <= 0 || startCodePoint < 0 || endCodePoint < startCodePoint) throw new IllegalArgumentException("Invalid detector metadata");
    }
    public enum Family { UUID_LIKE, EPOCH_TIMESTAMP_LIKE, LONG_NUMERIC_SEQUENCE_LIKE, HEX_HASH_FRAGMENT_LIKE,
        HIGH_ENTROPY_SUFFIX_LIKE, CSS_IN_JS_TOKEN_LIKE, CSS_MODULE_TOKEN_LIKE, FRAMEWORK_COUNTER_ID_LIKE,
        REACT_USEID_LIKE, STABLE_PREFIX_SUSPICIOUS_SUFFIX }
    public enum Confidence { LOW, MEDIUM, HIGH }
}
