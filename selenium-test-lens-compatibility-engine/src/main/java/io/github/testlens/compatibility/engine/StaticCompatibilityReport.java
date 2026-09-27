package io.github.testlens.compatibility.engine;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.github.testlens.compatibility.engine.CompatibilityComparisonReport.*;

/** Sanitized source-only compatibility evidence. It never represents a runtime verdict. */
public record StaticCompatibilityReport(
        int schemaVersion, int algorithmVersion, AnalysisMode analysisMode, Coverage coverage,
        List<Finding> findings, List<DetectorDecision> detectorDecisions,
        List<String> issues, List<String> limitations, Metrics metrics) {
    public static final int SCHEMA_VERSION = 1;
    public static final int ALGORITHM_VERSION = 1;
    public StaticCompatibilityReport {
        if (schemaVersion != 1 || algorithmVersion != 1 || analysisMode != AnalysisMode.STATIC)
            throw new IllegalArgumentException("unsupported static report version/mode");
        coverage = Objects.requireNonNull(coverage);
        findings = bounded(findings, 100_000, "findings");
        if (findings.stream().anyMatch(f -> f.causalState() != CausalState.HYPOTHESIS
                || f.codeChangeRequired() != CodeChangeRequired.UNKNOWN))
            throw new IllegalArgumentException("STATIC evidence cannot confirm a cause or require a code change");
        detectorDecisions = bounded(detectorDecisions, 64, "detector decisions");
        issues = strings(issues, 4096, 512); limitations = strings(limitations, 4096, 512);
        metrics = metrics == null ? new Metrics(0,0,0) : metrics;
    }
    public enum AnalysisMode { STATIC }
    public enum DetectorStatus { IMPLEMENTED, DEFERRED, REJECTED_AS_TOO_NOISY }
    public record Coverage(int sourceRootsRequested, int sourceRootsFound, int filesDiscovered,
                           int javaFiles, int filesParsed, int filesFailed, int filesExcluded,
                           int generatedFilesExcluded, int unsupportedLanguages,
                           int symbolResolutionIssues, boolean classpathIncomplete,
                           Map<String,Integer> findingsByCode) {
        public Coverage { findingsByCode = findingsByCode == null ? Map.of() : Map.copyOf(findingsByCode); }
    }
    public record SourceRange(int beginLine,int beginColumn,int endLine,int endColumn) {
        public SourceRange { if(beginLine<1||beginColumn<1||endLine<beginLine||endColumn<1)throw new IllegalArgumentException("invalid source range"); }
    }
    public record Evidence(StatementKind kind,String code,List<String> reasonCodes) {
        public Evidence { kind=Objects.requireNonNull(kind);code=safe(code,128);reasonCodes=strings(reasonCodes,16,128); }
    }
    public record Finding(String findingRef,String logicalPath,SourceRange sourceRange,String declaringSymbol,
                          FindingCategory category,String code,Severity severity,CausalState causalState,
                          Confidence confidence,Evidence evidence,RecommendationCode recommendation,
                          CodeChangeRequired codeChangeRequired,String testIdentityRef,String descriptor,
                          List<String> limitations) {
        public Finding { findingRef=safe(findingRef,128);logicalPath=safe(logicalPath,512);Objects.requireNonNull(sourceRange);
            declaringSymbol=safe(declaringSymbol,256);category=Objects.requireNonNull(category);code=safe(code,128);
            severity=Objects.requireNonNull(severity);causalState=Objects.requireNonNull(causalState);
            confidence=Objects.requireNonNull(confidence);evidence=Objects.requireNonNull(evidence);
            recommendation=Objects.requireNonNull(recommendation);codeChangeRequired=Objects.requireNonNull(codeChangeRequired);
            testIdentityRef=safe(testIdentityRef,128);descriptor=safe(descriptor,512);limitations=strings(limitations,16,256); }
    }
    public record DetectorDecision(String code,DetectorStatus status,String reason) {
        public DetectorDecision { code=safe(code,128);status=Objects.requireNonNull(status);reason=safe(reason,256); }
    }
    public record Metrics(long parseNanos,long symbolResolutionNanos,long detectionNanos) { }
    private static <T> List<T> bounded(List<T> x,int n,String f){x=x==null?List.of():List.copyOf(x);if(x.size()>n)throw new IllegalArgumentException(f+" exceeds "+n);return x;}
    private static List<String> strings(List<String>x,int n,int l){x=x==null?List.of():x.stream().map(v->safe(v,l)).distinct().sorted().toList();if(x.size()>n)throw new IllegalArgumentException("too many strings");return x;}
    private static String safe(String x,int n){x=x==null?"":x;if(x.codePointCount(0,x.length())>n||x.chars().anyMatch(Character::isISOControl))throw new IllegalArgumentException("unsafe report text");return x;}
}
