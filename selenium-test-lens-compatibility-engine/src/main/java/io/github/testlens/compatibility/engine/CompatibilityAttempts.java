package io.github.testlens.compatibility.engine;

import java.util.List;
import java.util.Locale;

import static io.github.testlens.compatibility.engine.CompatibilityRunManifest.*;

/** Factories for bounded attempt and failure facts. */
public final class CompatibilityAttempts {
    private CompatibilityAttempts() {}

    public static Attempt attempt(int ordinal, ResultStatus result, FailureSignature failure,
                                  BehaviorSummary behavior, TimingSummary timing, EvidenceCompleteness evidence) {
        FailureSignature safeFailure=failure==null?CompatibilityRunManifest.noFailure():failure;
        BehaviorSummary safeBehavior=behavior==null?CompatibilityRunManifest.emptyBehavior():behavior;
        TimingSummary safeTiming=timing==null?new TimingSummary(0,0,Fact.unknown()):timing;
        EvidenceCompleteness safeEvidence=evidence==null?CompatibilityRunManifest.noEvidence():evidence;
        String ref="compatibility-attempt-v1:sha256:"+CompatibilityDigests.digest("compatibility-attempt-v1",
                Integer.toString(ordinal),result.name(),failureKey(safeFailure),safeBehavior.toString(),safeTiming.toString(),safeEvidence.toString());
        return new Attempt(ordinal,ref,Fact.unknown(),result,safeFailure,safeBehavior,safeTiming,safeEvidence);
    }

    public static FailureSignature failure(String exceptionClass,String category,String operationCategory,
                                           String safeSubjectRef,List<String> reasonCodes,String message,
                                           FailurePhase phase) {
        Fact<String> messageDigest=message==null||message.isBlank()?Fact.unknown():Fact.known(
                "compatibility-failure-message-v1:sha256:"+CompatibilityDigests.digest(
                        "compatibility-failure-message-v1",normalizeMessage(message)),Provenance.DERIVED);
        return new FailureSignature(text(exceptionClass),text(category),text(operationCategory),text(safeSubjectRef),
                reasonCodes,messageDigest,phase==null?FailurePhase.UNKNOWN:phase);
    }

    /** Conservative normalization: whitespace folding, then replacement of UUIDs, hex addresses and decimal runs >= 4. */
    public static String normalizeMessage(String value) {
        String normalized=value.replaceAll("[\\r\\n\\t ]+"," ").trim().toLowerCase(Locale.ROOT);
        normalized=normalized.replaceAll("(?i)\\b[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}\\b","<uuid>");
        normalized=normalized.replaceAll("(?i)0x[0-9a-f]+","<hex>");
        normalized=normalized.replaceAll("\\b\\d{4,}\\b","<number>");
        if(normalized.codePointCount(0,normalized.length())>512) normalized=normalized.substring(0,normalized.offsetByCodePoints(0,512));
        return normalized;
    }

    private static Fact<String> text(String value){return value==null||value.isBlank()?Fact.unknown():Fact.known(value,Provenance.DERIVED);}
    private static String failureKey(FailureSignature value){return value==null?"":value.toString();}
}
