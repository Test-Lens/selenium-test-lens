package io.github.testlens.compatibility.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static io.github.testlens.compatibility.engine.CompatibilityRunManifest.*;

/** Explicit allowlisted metadata supplied by a trusted compatibility harness. */
public final class CompatibilityCaptureDescriptor {
    private final Fact<TestKey> testIdentity;
    private final Execution execution;
    private final Context context;
    private final Configuration configuration;
    private final List<Attempt> attempts;
    private final List<Issue> issues;

    private CompatibilityCaptureDescriptor(Builder b) {
        testIdentity=b.testIdentity; execution=b.execution(); context=b.context(); configuration=b.configuration();
        attempts=List.copyOf(b.attempts); issues=List.copyOf(b.issues);
    }
    public static Builder builder() { return new Builder(); }
    public Fact<TestKey> testIdentity(){return testIdentity;}
    public Execution execution(){return execution;}
    public Context context(){return context;}
    public Configuration configuration(){return configuration;}
    public List<Attempt> attempts(){return attempts;}
    public List<Issue> issues(){return issues;}

    public static final class Builder {
        private Fact<TestKey> testIdentity=Fact.unknown();
        private Fact<RequestedHeadlessMode> requested=Fact.unknown();
        private Fact<RequestedHeadlessProvenance> requestedProvenance=Fact.unknown();
        private Fact<EffectiveHeadlessState> effective=Fact.unknown();
        private Fact<EffectiveHeadlessProvenance> effectiveProvenance=Fact.unknown();
        private Fact<ObservabilityMode> observability=Fact.unknown();
        private Fact<HudPreset> hudPreset=Fact.unknown();
        private Fact<DriverScope> driverScope=Fact.unknown();
        private Fact<Lifecycle> lifecycle=Fact.unknown();
        private Fact<String> dataset=Fact.unknown(),environment=Fact.unknown(),testRevision=Fact.unknown(),sutRevision=Fact.unknown(),locale=Fact.unknown(),timezone=Fact.unknown();
        private Fact<Boolean> liveHud=Fact.unknown(),automaticFeedback=Fact.unknown(),sourceNavigation=Fact.unknown();
        private Fact<String> retention=Fact.unknown(),network=Fact.unknown();
        private final List<Attempt> attempts=new ArrayList<>();
        private final List<Issue> issues=new ArrayList<>();

        public Builder testKey(Framework framework,String testClass,String testMethod,String logicalKey,
                               boolean parameterizedOrDynamic,String invocationKey,String suiteContextKey) {
            if(framework==Framework.MANUAL&&(logicalKey==null||logicalKey.isBlank())) { testIdentity=Fact.unknown(); issue("MANUAL_LOGICAL_KEY_MISSING",IssueSeverity.REVIEW); return this; }
            Fact<String> invocation=opaque("compatibility-invocation-v1",invocationKey);
            Fact<String> suite=opaque("compatibility-suite-context-v1",suiteContextKey);
            String ref="compatibility-test-v1:sha256:"+CompatibilityDigests.digest("compatibility-test-v1",framework.name(),safe(testClass),safe(testMethod),safe(logicalKey),invocation.knowledge()==Knowledge.KNOWN?invocation.value():"");
            testIdentity=Fact.known(new TestKey(framework,testClass,testMethod,logicalKey,parameterizedOrDynamic,invocation,suite,ref),Provenance.TRUSTED_DESCRIPTOR);
            return this;
        }
        public Builder requestedHeadless(RequestedHeadlessMode value,RequestedHeadlessProvenance provenance){requested=Fact.known(Objects.requireNonNull(value),Provenance.TRUSTED_DESCRIPTOR);requestedProvenance=Fact.known(Objects.requireNonNull(provenance),Provenance.TRUSTED_DESCRIPTOR);return this;}
        public Builder attestEffectiveHeadless(EffectiveHeadlessState value,EffectiveHeadlessProvenance provenance){effective=Fact.known(Objects.requireNonNull(value),Provenance.TRUSTED_DESCRIPTOR);effectiveProvenance=Fact.known(Objects.requireNonNull(provenance),Provenance.TRUSTED_DESCRIPTOR);return this;}
        public Builder observability(ObservabilityMode mode,HudPreset preset,Boolean live,Boolean automatic,Boolean source,String passedRetention,String networkState){observability=fact(mode);hudPreset=fact(preset);liveHud=fact(live);automaticFeedback=fact(automatic);sourceNavigation=fact(source);retention=text(passedRetention);network=text(networkState);return this;}
        public Builder lifecycle(DriverScope scope,Lifecycle value){driverScope=fact(scope);lifecycle=fact(value);return this;}
        public Builder datasetKey(String value){dataset=digestFact("compatibility-dataset-v1",value);return this;}
        public Builder environmentKey(String value){environment=digestFact("compatibility-environment-v1",value);return this;}
        public Builder testSourceRevision(String value){testRevision=digestFact("compatibility-test-revision-v1",value);return this;}
        public Builder systemUnderTestRevision(String value){sutRevision=digestFact("compatibility-sut-revision-v1",value);return this;}
        public Builder locale(String value){locale=text(value);return this;}
        public Builder timezone(String value){timezone=text(value);return this;}
        public Builder attempt(Attempt value){attempts.add(Objects.requireNonNull(value));return this;}
        public Builder issue(String code,IssueSeverity severity){issues.add(new Issue(code,severity,List.of()));return this;}
        public CompatibilityCaptureDescriptor build(){return new CompatibilityCaptureDescriptor(this);}

        private Execution execution(){return new Execution(requested,requestedProvenance,effective,effectiveProvenance,observability,hudPreset,driverScope,lifecycle);}
        private Context context(){return new Context(dataset,environment,testRevision,sutRevision,locale,timezone);}
        private Configuration configuration(){String digest="compatibility-config-v1:sha256:"+CompatibilityDigests.digest("compatibility-config-v1",show(observability),show(hudPreset),show(liveHud),show(automaticFeedback),show(sourceNavigation),show(retention),show(network));return new Configuration(liveHud,automaticFeedback,sourceNavigation,retention,network,digest);}
        private static <T> Fact<T> fact(T value){return value==null?Fact.unknown():Fact.known(value,Provenance.TRUSTED_DESCRIPTOR);}
        private static Fact<String> text(String value){if(value==null||value.isBlank())return Fact.unknown();String cleaned=value.trim();if(cleaned.codePointCount(0,cleaned.length())>256||cleaned.chars().anyMatch(Character::isISOControl))throw new IllegalArgumentException("descriptor text must be a bounded safe label");return Fact.known(cleaned,Provenance.TRUSTED_DESCRIPTOR);}
        private static Fact<String> digestFact(String domain,String value){return value==null||value.isBlank()?Fact.unknown():Fact.known(domain+":sha256:"+CompatibilityDigests.digest(domain,value),Provenance.TRUSTED_DESCRIPTOR);}
        private static Fact<String> opaque(String domain,String value){return value==null||value.isBlank()?Fact.unknown():Fact.known(domain+":sha256:"+CompatibilityDigests.digest(domain,value),Provenance.TRUSTED_DESCRIPTOR);}
        private static String show(Fact<?> fact){return fact.knowledge()+":"+(fact.value()==null?"":fact.value());}
        private static String safe(String value){return value==null?"":value;}
    }
}
