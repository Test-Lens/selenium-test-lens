package io.github.testlens.application.mapper;

import io.github.testlens.core.redaction.RedactionPolicy;
import io.github.testlens.selector.engine.CompiledPolicySet;
import io.github.testlens.selector.engine.ObservationEvidence;

import java.time.Clock;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/** Explicit options for bounded mapping. Creating options performs no browser work. @since 0.5.0 */
public final class ApplicationMapperOptions {
    public enum Mode { CURRENT_PAGE, GUIDED, SAFE_EXPLORE }
    public enum ActionDecision { ALLOW, DENY, REQUIRE_EXPLICIT_APPROVAL }
    @FunctionalInterface public interface ActionPolicy { ActionDecision evaluate(SafeAction action); }
    public record SafeAction(String pageId,String elementId,String semanticName,String elementType,String targetUrl){ }

    private final String applicationName;private final Mode mode;private final int maxDiscoveredNodes,maxActionableElements,
            maxCandidateAnalyses,maxRegions,maxShadowDepth,maxPages,maxPageStates,maxTransitions,maxCrawlDepth,maxExploreActions;
    private final List<String> preferredTestAttributes;private final RedactionPolicy redactionPolicy;
    private final CompiledPolicySet selectorPolicies;private final ObservationEvidence selectorEvidence;
    private final ApplicationOverrides overrides;private final ActionPolicy actionPolicy;private final Clock clock;

    private ApplicationMapperOptions(Builder b){applicationName=b.applicationName;mode=b.mode;maxDiscoveredNodes=b.maxDiscoveredNodes;
        maxActionableElements=b.maxActionableElements;maxCandidateAnalyses=b.maxCandidateAnalyses;maxRegions=b.maxRegions;
        maxShadowDepth=b.maxShadowDepth;maxPages=b.maxPages;maxPageStates=b.maxPageStates;maxTransitions=b.maxTransitions;
        maxCrawlDepth=b.maxCrawlDepth;maxExploreActions=b.maxExploreActions;preferredTestAttributes=List.copyOf(b.preferredTestAttributes);
        redactionPolicy=b.redactionPolicy;selectorPolicies=b.selectorPolicies;selectorEvidence=b.selectorEvidence;overrides=b.overrides;
        actionPolicy=b.actionPolicy;clock=b.clock;}
    public static Builder builder(String applicationName){return new Builder(applicationName);}
    public String applicationName(){return applicationName;}public Mode mode(){return mode;}public int maxDiscoveredNodes(){return maxDiscoveredNodes;}
    public int maxActionableElements(){return maxActionableElements;}public int maxCandidateAnalyses(){return maxCandidateAnalyses;}
    public int maxRegions(){return maxRegions;}public int maxShadowDepth(){return maxShadowDepth;}public int maxPages(){return maxPages;}
    public int maxPageStates(){return maxPageStates;}public int maxTransitions(){return maxTransitions;}public int maxCrawlDepth(){return maxCrawlDepth;}
    public int maxExploreActions(){return maxExploreActions;}public List<String> preferredTestAttributes(){return preferredTestAttributes;}
    public RedactionPolicy redactionPolicy(){return redactionPolicy;}public CompiledPolicySet selectorPolicies(){return selectorPolicies;}
    public ObservationEvidence selectorEvidence(){return selectorEvidence;}public ApplicationOverrides overrides(){return overrides;}
    public ActionPolicy actionPolicy(){return actionPolicy;}public Clock clock(){return clock;}

    public static final class Builder{
        private final String applicationName;private Mode mode=Mode.CURRENT_PAGE;private int maxDiscoveredNodes=500,maxActionableElements=100,
                maxCandidateAnalyses=100,maxRegions=32,maxShadowDepth=3,maxPages=50,maxPageStates=20,maxTransitions=200,
                maxCrawlDepth=3,maxExploreActions=10;private final LinkedHashSet<String>preferredTestAttributes=new LinkedHashSet<>(List.of("data-testid","data-test","data-qa"));
        private RedactionPolicy redactionPolicy=RedactionPolicy.defaults();private CompiledPolicySet selectorPolicies=CompiledPolicySet.empty();
        private ObservationEvidence selectorEvidence=ObservationEvidence.unavailable();private ApplicationOverrides overrides=ApplicationOverrides.empty();
        private ActionPolicy actionPolicy=ignored->ActionDecision.DENY;private Clock clock=Clock.systemUTC();
        private Builder(String name){if(name==null||name.isBlank())throw new IllegalArgumentException("applicationName is required");applicationName=name.trim();}
        public Builder mode(Mode v){mode=Objects.requireNonNull(v);return this;}public Builder maxDiscoveredNodes(int v){maxDiscoveredNodes=bound(v,1,5000,"maxDiscoveredNodes");return this;}
        public Builder maxActionableElements(int v){maxActionableElements=bound(v,1,500,"maxActionableElements");return this;}
        public Builder maxCandidateAnalyses(int v){maxCandidateAnalyses=bound(v,1,500,"maxCandidateAnalyses");return this;}
        public Builder maxRegions(int v){maxRegions=bound(v,1,128,"maxRegions");return this;}public Builder maxShadowDepth(int v){maxShadowDepth=bound(v,0,8,"maxShadowDepth");return this;}
        public Builder maxPages(int v){maxPages=bound(v,1,500,"maxPages");return this;}public Builder maxPageStates(int v){maxPageStates=bound(v,1,100,"maxPageStates");return this;}
        public Builder maxTransitions(int v){maxTransitions=bound(v,0,2000,"maxTransitions");return this;}public Builder maxCrawlDepth(int v){maxCrawlDepth=bound(v,0,10,"maxCrawlDepth");return this;}
        public Builder maxExploreActions(int v){maxExploreActions=bound(v,0,100,"maxExploreActions");return this;}
        public Builder preferredTestAttributes(List<String> values){preferredTestAttributes.clear();for(String value:values){if(value==null||!value.matches("[A-Za-z_][A-Za-z0-9_.:-]{0,63}"))throw new IllegalArgumentException("Unsafe test attribute: "+value);preferredTestAttributes.add(value);}if(preferredTestAttributes.size()>8)throw new IllegalArgumentException("At most 8 test attributes");return this;}
        public Builder redactionPolicy(RedactionPolicy v){redactionPolicy=Objects.requireNonNull(v);return this;}public Builder selectorPolicies(CompiledPolicySet v){selectorPolicies=Objects.requireNonNull(v);return this;}
        public Builder selectorEvidence(ObservationEvidence v){selectorEvidence=Objects.requireNonNull(v);return this;}public Builder overrides(ApplicationOverrides v){overrides=Objects.requireNonNull(v);return this;}
        public Builder actionPolicy(ActionPolicy v){actionPolicy=Objects.requireNonNull(v);return this;}public Builder clock(Clock v){clock=Objects.requireNonNull(v);return this;}
        public ApplicationMapperOptions build(){if(maxCandidateAnalyses>maxActionableElements)maxCandidateAnalyses=maxActionableElements;return new ApplicationMapperOptions(this);}
        private static int bound(int v,int min,int max,String name){if(v<min||v>max)throw new IllegalArgumentException(name+" must be "+min+".."+max);return v;}
    }
}
