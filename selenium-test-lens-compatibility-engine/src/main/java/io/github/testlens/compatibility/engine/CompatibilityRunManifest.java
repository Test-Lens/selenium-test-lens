package io.github.testlens.compatibility.engine;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Sanitized facts for one logical test invocation and all of its physical attempts. */
public record CompatibilityRunManifest(
        int schemaVersion,
        int manifestAlgorithmVersion,
        String manifestId,
        Fact<TestKey> testIdentity,
        Execution execution,
        Browser browser,
        Display display,
        Context context,
        Configuration configuration,
        List<Attempt> attempts,
        TerminalResult terminalResult,
        Completeness completeness,
        List<Issue> issues,
        List<String> evidenceDigests) {

    public static final int SCHEMA_VERSION = 1;
    public static final int ALGORITHM_VERSION = 1;

    public CompatibilityRunManifest {
        if (schemaVersion != SCHEMA_VERSION) throw new IllegalArgumentException("schemaVersion must be 1");
        if (manifestAlgorithmVersion != ALGORITHM_VERSION) throw new IllegalArgumentException("manifestAlgorithmVersion must be 1");
        requireDigest(manifestId, "compatibility-manifest-v1", "manifestId");
        testIdentity = Objects.requireNonNull(testIdentity, "testIdentity");
        execution = Objects.requireNonNull(execution, "execution");
        browser = Objects.requireNonNull(browser, "browser");
        display = Objects.requireNonNull(display, "display");
        context = Objects.requireNonNull(context, "context");
        configuration = Objects.requireNonNull(configuration, "configuration");
        attempts = List.copyOf(Objects.requireNonNull(attempts, "attempts"));
        if (attempts.isEmpty()) throw new IllegalArgumentException("attempts must not be empty");
        if (attempts.size() > 64) throw new IllegalArgumentException("attempts exceeds 64");
        for (int i = 0; i < attempts.size(); i++) {
            if (attempts.get(i).ordinal() != i + 1) throw new IllegalArgumentException("attempt ordinals must be contiguous from 1");
        }
        terminalResult = Objects.requireNonNull(terminalResult, "terminalResult");
        completeness = Objects.requireNonNull(completeness, "completeness");
        issues = List.copyOf(Objects.requireNonNull(issues, "issues"));
        evidenceDigests = List.copyOf(Objects.requireNonNull(evidenceDigests, "evidenceDigests"));
        if (issues.size() > 128 || evidenceDigests.size() > 64) throw new IllegalArgumentException("manifest collection bound exceeded");
    }

    public enum Knowledge { KNOWN, UNKNOWN, NOT_APPLICABLE, UNSUPPORTED, REDACTED, CONFLICTED }
    public enum Provenance {
        TRUSTED_DESCRIPTOR, EXPLICIT_JAVA, SYSTEM_PROPERTY, ENVIRONMENT, FACTORY_DEFAULT, CALLER_SUPPLIED,
        MANAGED_FACTORY_ATTESTED, CAPABILITY_REPORTED, TRUSTED_CAPTURE_DESCRIPTOR,
        PRECREATED_DRIVER_UNKNOWN, UNSUPPORTED_PROVIDER, EXPLICIT_WEBDRIVER_CAPTURE,
        TRACE_REPORT_IMPORT, FAILURE_BUNDLE_IMPORT, DERIVED, UNKNOWN
    }
    public enum Framework { JUNIT5, TESTNG, MANUAL, OTHER }
    public enum RequestedHeadlessMode { HEADLESS, HEADED, UNSET }
    public enum RequestedHeadlessProvenance { EXPLICIT_JAVA, SYSTEM_PROPERTY, ENVIRONMENT, FACTORY_DEFAULT, CALLER_SUPPLIED, UNKNOWN }
    public enum EffectiveHeadlessState { HEADLESS, HEADED }
    public enum EffectiveHeadlessProvenance { MANAGED_FACTORY_ATTESTED, CAPABILITY_REPORTED, TRUSTED_CAPTURE_DESCRIPTOR, PRECREATED_DRIVER_UNKNOWN, UNSUPPORTED_PROVIDER, UNKNOWN }
    public enum ObservabilityMode { DEFAULT, FAST }
    public enum HudPreset { MINIMAL, COMPACT, STANDARD, DEBUG }
    public enum DriverScope { PER_METHOD, PER_CLASS, MANUAL, UNKNOWN }
    public enum Lifecycle { MANAGED_INVOCATION, MANAGED_CLASS, MANUAL, UNKNOWN }
    public enum ResultStatus { PASSED, FAILED, SKIPPED, STARTED, UNKNOWN }
    public enum FailurePhase { SETUP, TEST, TEARDOWN, UNKNOWN }
    public enum EvidenceRetention { FULL_TRACE, SUMMARY_ONLY, FAILURE_BUNDLE_ONLY, NONE, UNKNOWN }
    public enum CompletenessState { COMPLETE, PARTIAL, NOT_AVAILABLE, UNKNOWN }
    public enum SmartClickDetail { COMPLETE, PARTIAL, UNKNOWN, NOT_APPLICABLE }
    public enum IssueSeverity { INFO, REVIEW, WARNING, ERROR }

    /** A value plus explicit knowledge and provenance; unknown never equals a known empty value. */
    public record Fact<T>(Knowledge knowledge, T value, Provenance provenance) {
        public Fact {
            knowledge = Objects.requireNonNull(knowledge, "knowledge");
            provenance = provenance == null ? Provenance.UNKNOWN : provenance;
            if (knowledge == Knowledge.KNOWN && value == null) throw new IllegalArgumentException("KNOWN fact requires value");
            if (knowledge != Knowledge.KNOWN && value != null) throw new IllegalArgumentException("non-KNOWN fact cannot carry value");
        }
        public static <T> Fact<T> known(T value, Provenance provenance) { return new Fact<>(Knowledge.KNOWN, Objects.requireNonNull(value), provenance); }
        public static <T> Fact<T> unknown() { return new Fact<>(Knowledge.UNKNOWN, null, Provenance.UNKNOWN); }
        public static <T> Fact<T> conflicted() { return new Fact<>(Knowledge.CONFLICTED, null, Provenance.UNKNOWN); }
    }

    public record TestKey(Framework framework, String testClass, String testMethod, String logicalTestKey,
                          boolean parameterizedOrDynamic, Fact<String> invocationDiscriminator,
                          Fact<String> suiteContextDigest, String testIdentityRef) {
        public TestKey {
            framework = Objects.requireNonNull(framework, "framework");
            testClass = bounded(testClass, 512, "testClass");
            testMethod = bounded(testMethod, 512, "testMethod");
            logicalTestKey = bounded(logicalTestKey, 512, "logicalTestKey");
            invocationDiscriminator = Objects.requireNonNull(invocationDiscriminator, "invocationDiscriminator");
            suiteContextDigest = Objects.requireNonNull(suiteContextDigest, "suiteContextDigest");
            requireDigest(testIdentityRef, "compatibility-test-v1", "testIdentityRef");
        }
    }

    public record Execution(Fact<RequestedHeadlessMode> requestedHeadless,
                            Fact<RequestedHeadlessProvenance> requestedHeadlessProvenance,
                            Fact<EffectiveHeadlessState> effectiveHeadless,
                            Fact<EffectiveHeadlessProvenance> effectiveHeadlessProvenance,
                            Fact<ObservabilityMode> observabilityMode,
                            Fact<HudPreset> hudPreset,
                            Fact<DriverScope> driverScope,
                            Fact<Lifecycle> lifecycle) {
        public Execution { Objects.requireNonNull(requestedHeadless); Objects.requireNonNull(requestedHeadlessProvenance); Objects.requireNonNull(effectiveHeadless); Objects.requireNonNull(effectiveHeadlessProvenance); Objects.requireNonNull(observabilityMode); Objects.requireNonNull(hudPreset); Objects.requireNonNull(driverScope); Objects.requireNonNull(lifecycle); }
    }

    public record Browser(Fact<String> name, Fact<String> version, Fact<String> driverName,
                          Fact<String> driverVersion, Fact<String> platform,
                          Map<String,String> allowlistedCapabilityFacts, String capabilityDigest) {
        public Browser {
            Objects.requireNonNull(name); Objects.requireNonNull(version); Objects.requireNonNull(driverName); Objects.requireNonNull(driverVersion); Objects.requireNonNull(platform);
            boundedFact(name,256,"browser name"); boundedFact(version,256,"browser version"); boundedFact(driverName,256,"driver name"); boundedFact(driverVersion,256,"driver version"); boundedFact(platform,256,"platform");
            allowlistedCapabilityFacts = java.util.Collections.unmodifiableMap(new TreeMap<>(allowlistedCapabilityFacts == null ? Map.of() : allowlistedCapabilityFacts));
            if (allowlistedCapabilityFacts.size() > 16) throw new IllegalArgumentException("too many capability facts");
            allowlistedCapabilityFacts.forEach((key,value)->{bounded(key,64,"capability key");bounded(value,256,"capability value");});
            requireDigest(capabilityDigest, "compatibility-capabilities-v1", "capabilityDigest");
        }
    }

    public record Display(Fact<Integer> windowWidth, Fact<Integer> windowHeight,
                          Fact<Integer> viewportWidth, Fact<Integer> viewportHeight,
                          Fact<Double> devicePixelRatio) {
        public Display { Objects.requireNonNull(windowWidth); Objects.requireNonNull(windowHeight); Objects.requireNonNull(viewportWidth); Objects.requireNonNull(viewportHeight); Objects.requireNonNull(devicePixelRatio); positive(windowWidth); positive(windowHeight); positive(viewportWidth); positive(viewportHeight); if (devicePixelRatio.knowledge()==Knowledge.KNOWN && (!Double.isFinite(devicePixelRatio.value()) || devicePixelRatio.value()<=0 || devicePixelRatio.value()>100)) throw new IllegalArgumentException("invalid devicePixelRatio"); }
        private static void positive(Fact<Integer> fact) { if (fact.knowledge()==Knowledge.KNOWN && (fact.value()<=0 || fact.value()>100_000)) throw new IllegalArgumentException("invalid display dimension"); }
    }

    public record Context(Fact<String> datasetDigest, Fact<String> environmentDigest,
                          Fact<String> testSourceRevision, Fact<String> systemUnderTestRevision,
                          Fact<String> locale, Fact<String> timezone) {
        public Context { Objects.requireNonNull(datasetDigest); Objects.requireNonNull(environmentDigest); Objects.requireNonNull(testSourceRevision); Objects.requireNonNull(systemUnderTestRevision); Objects.requireNonNull(locale); Objects.requireNonNull(timezone); boundedFact(datasetDigest,256,"dataset");boundedFact(environmentDigest,256,"environment");boundedFact(testSourceRevision,256,"test revision");boundedFact(systemUnderTestRevision,256,"SUT revision");boundedFact(locale,128,"locale");boundedFact(timezone,128,"timezone"); }
    }

    public record Configuration(Fact<Boolean> liveHud, Fact<Boolean> automaticFeedback,
                                Fact<Boolean> sourceNavigation, Fact<String> passedTraceRetention,
                                Fact<String> networkCaptureState, String semanticConfigurationDigest) {
        public Configuration { Objects.requireNonNull(liveHud); Objects.requireNonNull(automaticFeedback); Objects.requireNonNull(sourceNavigation); Objects.requireNonNull(passedTraceRetention); Objects.requireNonNull(networkCaptureState); boundedFact(passedTraceRetention,128,"retention");boundedFact(networkCaptureState,128,"network state");requireDigest(semanticConfigurationDigest, "compatibility-config-v1", "semanticConfigurationDigest"); }
    }

    public record Attempt(int ordinal, String attemptRef, Fact<String> attemptIdentity,
                          ResultStatus result, FailureSignature failure,
                          BehaviorSummary behavior, TimingSummary timing,
                          EvidenceCompleteness evidence) {
        public Attempt { if (ordinal < 1 || ordinal > 64) throw new IllegalArgumentException("invalid attempt ordinal"); requireDigest(attemptRef, "compatibility-attempt-v1", "attemptRef"); Objects.requireNonNull(attemptIdentity); Objects.requireNonNull(result); Objects.requireNonNull(behavior); Objects.requireNonNull(timing); Objects.requireNonNull(evidence); }
    }

    public record FailureSignature(Fact<String> exceptionClass, Fact<String> category,
                                   Fact<String> operationCategory, Fact<String> safeSubjectRef,
                                   List<String> reasonCodes, Fact<String> normalizedMessageDigest,
                                   FailurePhase phase) {
        public FailureSignature { Objects.requireNonNull(exceptionClass); Objects.requireNonNull(category); Objects.requireNonNull(operationCategory); Objects.requireNonNull(safeSubjectRef); boundedFact(exceptionClass,512,"exception class");boundedFact(category,128,"failure category");boundedFact(operationCategory,128,"operation category");boundedFact(safeSubjectRef,512,"safe subject ref");reasonCodes = sortedBounded(reasonCodes, 32, 128, "reasonCodes"); Objects.requireNonNull(normalizedMessageDigest); boundedFact(normalizedMessageDigest,256,"message digest");phase = Objects.requireNonNull(phase); }
    }

    public record BehaviorSummary(long operationRetries, long retryTimeMillis,
                                  List<GroupCount> retryReasons, LocatorSummary locators,
                                  WaitSummary waits, InteractionSummary interactions,
                                  ContextSummary browserContext, List<GroupCount> authOutcomes,
                                  long uploadOperations, List<GroupCount> warnings,
                                  long artifactCount) {
        public BehaviorSummary { nonnegative(operationRetries, retryTimeMillis, uploadOperations, artifactCount); retryReasons=boundedGroups(retryReasons); Objects.requireNonNull(locators); Objects.requireNonNull(waits); Objects.requireNonNull(interactions); Objects.requireNonNull(browserContext); authOutcomes=boundedGroups(authOutcomes); warnings=boundedGroups(warnings); }
    }
    public record GroupCount(String code, long count) { public GroupCount { code=bounded(code,128,"code"); if(code.isBlank()||count<0)throw new IllegalArgumentException("invalid group count"); } }
    public record LocatorSummary(long observations,long resolved,long notFound,long errors,long knownCardinalityAnomalies,long durationNanos,List<GroupCount> outcomes) { public LocatorSummary { nonnegative(observations,resolved,notFound,errors,knownCardinalityAnomalies,durationNanos); outcomes=boundedGroups(outcomes); } }
    public record WaitSummary(long observations,long attempts,long timeouts,long failures,long elapsedMillis) { public WaitSummary { nonnegative(observations,attempts,timeouts,failures,elapsedMillis); } }
    public record InteractionSummary(long observations,long recoveries,long fallbacks,SmartClickDetail smartClickDetail,List<GroupCount> fallbackTypes) { public InteractionSummary { nonnegative(observations,recoveries,fallbacks); Objects.requireNonNull(smartClickDetail); fallbackTypes=boundedGroups(fallbackTypes); } }
    public record ContextSummary(long frameOperations,long shadowContexts,long newWindowOperations,long windowSwitches,long contextFailures) { public ContextSummary { nonnegative(frameOperations,shadowContexts,newWindowOperations,windowSwitches,contextFailures); } }
    public record TimingSummary(long sampleCount,long totalDurationMillis,Fact<Long> logicalDurationMillis) { public TimingSummary { nonnegative(sampleCount,totalDurationMillis); Objects.requireNonNull(logicalDurationMillis); if(logicalDurationMillis.knowledge()==Knowledge.KNOWN && logicalDurationMillis.value()<0)throw new IllegalArgumentException("negative duration"); } }
    public record EvidenceCompleteness(EvidenceRetention retention, CompletenessState behavior,
                                       boolean locatorObservations, boolean detailedRetries,
                                       boolean screenshots, boolean network, boolean viewport,
                                       SmartClickDetail smartClickDetail) { public EvidenceCompleteness { Objects.requireNonNull(retention); Objects.requireNonNull(behavior); Objects.requireNonNull(smartClickDetail); } }
    public record TerminalResult(ResultStatus status,int attemptCount,boolean passedAfterRetry,boolean failedAfterRetry) { public TerminalResult { Objects.requireNonNull(status); if(attemptCount<1||attemptCount>64)throw new IllegalArgumentException("invalid attemptCount"); if(passedAfterRetry&&status!=ResultStatus.PASSED)throw new IllegalArgumentException("passedAfterRetry requires PASSED"); if(failedAfterRetry&&status!=ResultStatus.FAILED)throw new IllegalArgumentException("failedAfterRetry requires FAILED"); } }
    public record Completeness(CompletenessState testIdentity,CompletenessState execution,CompletenessState browser,
                               CompletenessState display,CompletenessState context,CompletenessState behavior,
                               CompletenessState failure) { public Completeness { Objects.requireNonNull(testIdentity); Objects.requireNonNull(execution); Objects.requireNonNull(browser); Objects.requireNonNull(display); Objects.requireNonNull(context); Objects.requireNonNull(behavior); Objects.requireNonNull(failure); } }
    public record Issue(String code,IssueSeverity severity,List<String> references) { public Issue { code=bounded(code,128,"issue code"); if(code.isBlank())throw new IllegalArgumentException("issue code blank"); Objects.requireNonNull(severity); references=sortedBounded(references,16,512,"issue references"); } }

    public static BehaviorSummary emptyBehavior() { return new BehaviorSummary(0,0,List.of(),new LocatorSummary(0,0,0,0,0,0,List.of()),new WaitSummary(0,0,0,0,0),new InteractionSummary(0,0,0,SmartClickDetail.UNKNOWN,List.of()),new ContextSummary(0,0,0,0,0),List.of(),0,List.of(),0); }
    public static EvidenceCompleteness noEvidence() { return new EvidenceCompleteness(EvidenceRetention.NONE,CompletenessState.NOT_AVAILABLE,false,false,false,false,false,SmartClickDetail.UNKNOWN); }
    public static FailureSignature noFailure() { return new FailureSignature(Fact.unknown(),Fact.unknown(),Fact.unknown(),Fact.unknown(),List.of(),Fact.unknown(),FailurePhase.UNKNOWN); }

    private static List<GroupCount> boundedGroups(List<GroupCount> values) { values=values==null?List.of():values; if(values.size()>128)throw new IllegalArgumentException("behavior group bound exceeded"); return values.stream().sorted(java.util.Comparator.comparing(GroupCount::code)).toList(); }
    private static List<String> sortedBounded(List<String> values,int max,int maxLength,String field) { values=values==null?List.of():values; if(values.size()>max)throw new IllegalArgumentException(field+" bound exceeded"); return values.stream().map(v->bounded(v,maxLength,field)).distinct().sorted().toList(); }
    private static String bounded(String value,int max,String field) { value=value==null?"":value; if(value.codePointCount(0,value.length())>max)throw new IllegalArgumentException(field+" exceeds "+max); return value; }
    private static void boundedFact(Fact<String> fact,int max,String field){if(fact.knowledge()==Knowledge.KNOWN){String value=bounded(fact.value(),max,field);if(value.chars().anyMatch(Character::isISOControl))throw new IllegalArgumentException(field+" contains control characters");}}
    private static void nonnegative(long... values) { for(long value:values)if(value<0)throw new IllegalArgumentException("negative summary count"); }
    private static void requireDigest(String value,String domain,String field) { if(value==null||!value.matches(java.util.regex.Pattern.quote(domain)+":sha256:[0-9a-f]{64}"))throw new IllegalArgumentException("invalid "+field); }
}
