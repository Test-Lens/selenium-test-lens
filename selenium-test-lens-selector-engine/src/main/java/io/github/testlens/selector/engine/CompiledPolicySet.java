package io.github.testlens.selector.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable indexed policy set. */
public final class CompiledPolicySet {
    private final Map<String,List<SelectorPolicy.Rule>> exact;
    private final Map<String,List<SelectorPolicy.Rule>> patterns;
    private final List<SelectorPolicy.Rule> rules;

    private CompiledPolicySet(Map<String,List<SelectorPolicy.Rule>> exact,Map<String,List<SelectorPolicy.Rule>> patterns,List<SelectorPolicy.Rule> rules){
        this.exact=freeze(exact);this.patterns=freeze(patterns);this.rules=List.copyOf(rules);
    }
    public static CompiledPolicySet compile(SelectorPolicy.Document document){
        Map<String,SelectorPolicy.Rule> ids=new LinkedHashMap<>();
        for(SelectorPolicy.Rule r:document.rules()){
            SelectorPolicy.Rule previous=ids.putIfAbsent(r.ruleId(),r);
            if(previous!=null&&!previous.equals(r))throw new IllegalArgumentException("Conflicting duplicate ruleId: "+r.ruleId());
        }
        Map<String,List<SelectorPolicy.Rule>> exact=new HashMap<>(),patterns=new HashMap<>();
        for(SelectorPolicy.Rule r:ids.values()){
            if(r.matcher() instanceof SelectorPolicy.ExactMatcher e) exact.computeIfAbsent(key(e.strategy(),e.valueDigest()),ignored->new ArrayList<>()).add(r);
            else patterns.computeIfAbsent(r.matcher().strategy(),ignored->new ArrayList<>()).add(r);
        }
        return new CompiledPolicySet(exact,patterns,new ArrayList<>(ids.values()));
    }
    public static CompiledPolicySet empty(){return compile(SelectorPolicy.Document.empty());}
    List<SelectorPolicy.Rule> candidates(SelectorSubject subject){
        List<SelectorPolicy.Rule> out=new ArrayList<>();
        if(subject.hasTrustedExactValue())out.addAll(exact.getOrDefault(key(subject.strategy(),CanonicalDigests.exactValueDigest(subject)),List.of()));
        out.addAll(patterns.getOrDefault(subject.strategy(),List.of()));
        return out;
    }
    public List<SelectorPolicy.Rule> rules(){return rules;}
    private static String key(String strategy,String digest){return strategy+'\0'+digest;}
    private static Map<String,List<SelectorPolicy.Rule>> freeze(Map<String,List<SelectorPolicy.Rule>> source){
        Map<String,List<SelectorPolicy.Rule>> out=new HashMap<>();source.forEach((k,v)->out.put(k,List.copyOf(v)));return Map.copyOf(out);
    }
}
