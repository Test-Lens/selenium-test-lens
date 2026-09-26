package io.github.testlens.selector.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

import static io.github.testlens.selector.engine.CandidateAnalysis.*;

/** Deterministic bounded generator over an already captured neutral target snapshot. */
public final class CandidateGenerator {
    public static final int MAX_CANDIDATES=50;
    private final SelectorStabilityEngine stabilityEngine=new SelectorStabilityEngine();

    public Generated generate(TargetSnapshot target,Request request){
        Objects.requireNonNull(target);Objects.requireNonNull(request);
        LinkedHashMap<String,Draft> drafts=new LinkedHashMap<>();int[] before={0};
        if(request.original()!=null)add(drafts,before,request.original(),Origin.ORIGINAL,ScopeFragility.DIRECT,semantic(request.original()),0,null,true,List.of(request.original()));
        scalar(drafts,before,"id",target.id(),Origin.ID,ScopeFragility.DIRECT,SemanticPreference.ID,0,null);
        request.preferredTestAttributes().stream().sorted().forEach(a->scalar(drafts,before,"css selector",attribute(a,target.testAttributes().get(a)),Origin.TEST_ATTRIBUTE,ScopeFragility.SIMPLE_COMPOUND,SemanticPreference.PREFERRED_TEST_ATTRIBUTE,0,null));
        scalar(drafts,before,"name",target.name(),Origin.NAME,ScopeFragility.DIRECT,SemanticPreference.NAME,0,null);
        target.classTokens().stream().sorted().limit(TargetSnapshot.MAX_CLASS_TOKENS).forEach(c->{
            scalar(drafts,before,"class name",c,Origin.CLASS_TOKEN,ScopeFragility.DIRECT,SemanticPreference.CLASS_OR_TAG_CLASS,0,null);
            if(validTag(target.tagName()))scalar(drafts,before,"css selector",target.tagName()+"."+cssIdentifier(c),Origin.TAG_CLASS,ScopeFragility.SIMPLE_COMPOUND,SemanticPreference.CLASS_OR_TAG_CLASS,0,null);
        });
        if(validTag(target.tagName()))scalar(drafts,before,"tag name",target.tagName(),Origin.TAG,ScopeFragility.DIRECT,SemanticPreference.GENERIC_TEXT_OR_TAG,0,null);
        if("a".equalsIgnoreCase(target.tagName())&&!blank(target.visibleText()))scalar(drafts,before,"link text",target.visibleText(),Origin.LINK_TEXT,ScopeFragility.DIRECT,SemanticPreference.LINK_TEXT,0,"TEXT_LOCALE_SENSITIVE");
        if(!blank(target.visibleText()))scalar(drafts,before,"xpath",".//*[normalize-space(.)="+xpathLiteral(target.visibleText())+"]",Origin.TEXT_XPATH,ScopeFragility.TEXT_XPATH,SemanticPreference.GENERIC_TEXT_OR_TAG,0,"TEXT_LOCALE_SENSITIVE");
        Locator targetComponent=targetComponent(target,request.preferredTestAttributes());String targetCss=targetPart(target,request.preferredTestAttributes());
        if(targetCss!=null)for(TargetSnapshot.AncestorHint ancestor:target.ancestors().stream().sorted(Comparator.comparingInt(TargetSnapshot.AncestorHint::depth)).toList()){
            Locator ancestorComponent=ancestorComponent(ancestor,request.preferredTestAttributes());String ancestorCss=ancestorPart(ancestor,request.preferredTestAttributes());
            if(ancestorCss!=null){Locator compound=new Locator("css selector",ancestorCss+" "+targetCss);add(drafts,before,compound,Origin.STABLE_ANCESTOR,ScopeFragility.ANCESTOR_COMPOUND,semantic(targetComponent),ancestor.depth(),"ANCESTOR_DEPTH_"+ancestor.depth(),false,List.of(targetComponent,ancestorComponent));}
        }
        boolean capped=drafts.size()>MAX_CANDIDATES;List<Candidate> candidates=drafts.values().stream().limit(MAX_CANDIDATES).map(d->candidate(d,request)).toList();
        return new Generated(candidates,before[0],capped);
    }

    private Candidate candidate(Draft d,Request r){
        List<StabilityComponent> components=new ArrayList<>();int index=0;
        for(Locator component:d.components){SelectorSubject subject=new SelectorSubject(SelectorSubject.SubjectKind.RUNTIME_OBSERVATION,component.strategy(),SelectorSubject.ValueState.KNOWN,component.value(),r.declarationRef(),r.modulePath(),r.logicalPath(),r.declaringSymbol(),r.usageClass(),r.usageMethod(),r.contextFingerprint(),SelectorSubject.InputTrust.RUNTIME_RAW_LOCAL,null);components.add(new StabilityComponent(index++==0?"TARGET":"SCOPE",component.strategy(),component.value(),stabilityEngine.analyze(subject,r.evidence(),r.policies())));}
        List<Reason> reasons=new ArrayList<>();if(d.origins.contains(Origin.TEST_ATTRIBUTE))reasons.add(new Reason("PREFERRED_TEST_ATTRIBUTE","Uses a configured preferred test attribute"));if(d.limitation!=null)reasons.add(new Reason(d.limitation,d.limitation.replace('_',' ')));
        return new Candidate(CandidateIds.id(d.locator,r.contextFingerprint()),d.locator,false,d.origins.stream().sorted().toList(),components,Validation.notLive(),new Complexity(d.fragility,d.preference,d.depth,d.depth>0?1:0,d.locator.value().codePointCount(0,d.locator.value().length()),components.size()),reasons,d.limitation==null?List.of():List.of(d.limitation),d.original);
    }
    private static void scalar(Map<String,Draft> out,int[] before,String strategy,String value,Origin origin,ScopeFragility fragility,SemanticPreference preference,int depth,String limitation){if(blank(value)||value.codePointCount(0,value.length())>TargetSnapshot.MAX_LOCATOR_CODE_POINTS)return;Locator l=new Locator(strategy,value);add(out,before,l,origin,fragility,preference,depth,limitation,false,List.of(l));}
    private static void add(Map<String,Draft> out,int[] before,Locator locator,Origin origin,ScopeFragility fragility,SemanticPreference preference,int depth,String limitation,boolean original,List<Locator> components){before[0]++;String key=locator.strategy()+'\0'+locator.value();Draft old=out.get(key);if(old==null)out.put(key,new Draft(locator,EnumSet.of(origin),fragility,preference,depth,limitation,original,components));else{old.origins.add(origin);old.original|=original;}}
    private static Locator targetComponent(TargetSnapshot t,List<String> attrs){for(String a:attrs){String v=t.testAttributes().get(a);if(!blank(v))return new Locator("css selector",attribute(a,v));}if(!blank(t.id()))return new Locator("id",t.id());if(!t.classTokens().isEmpty())return new Locator("class name",t.classTokens().stream().sorted().findFirst().orElseThrow());return new Locator("tag name",t.tagName());}
    private static Locator ancestorComponent(TargetSnapshot.AncestorHint a,List<String> attrs){for(String n:attrs){String v=a.testAttributes().get(n);if(!blank(v))return new Locator("css selector",attribute(n,v));}if(!blank(a.id()))return new Locator("id",a.id());return new Locator("class name",a.classTokens().stream().sorted().findFirst().orElse(""));}
    private static String targetPart(TargetSnapshot t,List<String> attrs){for(String a:attrs){String v=t.testAttributes().get(a);if(!blank(v))return attribute(a,v);}if(!blank(t.id()))return "#"+cssIdentifier(t.id());if(!t.classTokens().isEmpty())return (validTag(t.tagName())?t.tagName():"")+"."+cssIdentifier(t.classTokens().stream().sorted().findFirst().orElseThrow());return validTag(t.tagName())?t.tagName():null;}
    private static String ancestorPart(TargetSnapshot.AncestorHint a,List<String> attrs){for(String n:attrs){String v=a.testAttributes().get(n);if(!blank(v))return attribute(n,v);}if(!blank(a.id()))return "#"+cssIdentifier(a.id());if(!a.classTokens().isEmpty())return (validTag(a.tagName())?a.tagName():"")+"."+cssIdentifier(a.classTokens().stream().sorted().findFirst().orElseThrow());return null;}
    private static String attribute(String name,String value){return blank(value)?null:"["+name+"=\""+cssString(value)+"\"]";}
    public static String cssString(String value){StringBuilder b=new StringBuilder();value.codePoints().forEach(cp->{if(cp==0)b.append("\\FFFD ");else if(cp=='"'||cp=='\\')b.append('\\').appendCodePoint(cp);else if(cp<32||cp==127)b.append('\\').append(Integer.toHexString(cp)).append(' ');else b.appendCodePoint(cp);});return b.toString();}
    public static String cssIdentifier(String value){StringBuilder b=new StringBuilder();int[] points=value.codePoints().toArray();int i=0;for(int cp:points){boolean safe=cp>=128||cp=='-'||cp=='_'||Character.isLetterOrDigit(cp);if(cp==0)b.append("\\fffd ");else if(cp<32||cp==127||(i==0&&Character.isDigit(cp))||(i==1&&points[0]=='-'&&Character.isDigit(cp)))b.append('\\').append(Integer.toHexString(cp)).append(' ');else if(!safe||(points.length==1&&cp=='-'))b.append('\\').appendCodePoint(cp);else b.appendCodePoint(cp);i++;}return b.toString();}
    public static String xpathLiteral(String value){if(!value.contains("'"))return "'"+value+"'";if(!value.contains("\""))return "\""+value+"\"";StringJoiner joiner=new StringJoiner(", \"'\", ","concat(",")");for(String part:value.split("'",-1))joiner.add("'"+part+"'");return joiner.toString();}
    private static boolean validTag(String value){return !blank(value)&&value.codePoints().allMatch(cp->cp>=128||Character.isLetterOrDigit(cp)||cp=='-'||cp=='_');}
    private static boolean blank(String value){return value==null||value.isBlank();}
    private static SemanticPreference semantic(Locator locator){return switch(locator.strategy()){case "id"->SemanticPreference.ID;case "name"->SemanticPreference.NAME;case "class name"->SemanticPreference.CLASS_OR_TAG_CLASS;case "link text"->SemanticPreference.LINK_TEXT;default->SemanticPreference.GENERIC_TEXT_OR_TAG;};}
    private static final class Draft{final Locator locator;final EnumSet<Origin> origins;final ScopeFragility fragility;final SemanticPreference preference;final int depth;final String limitation;final List<Locator> components;boolean original;Draft(Locator l,EnumSet<Origin> o,ScopeFragility f,SemanticPreference p,int d,String x,boolean original,List<Locator> c){locator=l;origins=o;fragility=f;preference=p;depth=d;limitation=x;this.original=original;components=c;}}
    public record Request(Locator original,List<String> preferredTestAttributes,String contextFingerprint,String declarationRef,String modulePath,String logicalPath,String declaringSymbol,String usageClass,String usageMethod,CompiledPolicySet policies,ObservationEvidence evidence){public Request{preferredTestAttributes=List.copyOf(preferredTestAttributes);policies=policies==null?CompiledPolicySet.empty():policies;evidence=evidence==null?ObservationEvidence.unavailable():evidence;}}
    public record Generated(List<Candidate> candidates,int generatedBeforeDedup,boolean candidateLimitReached){public Generated{candidates=List.copyOf(candidates);}}
}
