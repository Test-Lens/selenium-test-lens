package io.github.testlens.application.tooling.source;

import io.github.testlens.application.model.ApplicationModel;
import io.github.testlens.selector.tooling.ExistingProjectIndex;
import io.github.testlens.selector.tooling.ExistingProjectIndexer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Correlates the existing source index with ApplicationModel using selector identity and page context, never names alone. @since 0.5.0 */
public final class PageObjectCorrelator {
    public PageObjectCorrelation correlate(ApplicationModel application, ExistingProjectIndex source,
                                           CorrelationOverrides overrides) {
        Objects.requireNonNull(application,"application"); Objects.requireNonNull(source,"source");
        CorrelationOverrides effectiveOverrides = overrides == null ? CorrelationOverrides.none() : overrides;
        Map<String,ApplicationModel.PageModel> pages = new HashMap<>();
        Map<String,ApplicationModel.ElementModel> appElements = new HashMap<>();
        for (var page: application.pages()) { pages.put(page.pageId(),page); for(var element:page.elements())appElements.put(element.elementId(),element); }
        Map<String,ExistingProjectIndex.ClassEntry> classes = new HashMap<>(); source.classes().forEach(value->classes.put(value.id(),value));
        Map<String,ExistingProjectIndex.ElementEntry> sourceElements = new HashMap<>(); source.elements().forEach(value->sourceElements.put(value.id(),value));
        List<PageObjectCorrelation.ElementCorrelation> results = new ArrayList<>();
        Map<String,Map<String,Integer>> pageClassMatches = new LinkedHashMap<>();
        for (var page:application.pages()) for (var element:page.elements()) {
            List<Match> matches = new ArrayList<>();
            for (var candidate:source.elements()) {
                List<PageObjectCorrelation.Evidence> evidence = selectorEvidence(element,candidate);
                if(!evidence.isEmpty()) {
                    var owner=classes.get(candidate.ownerClassId());
                    if(owner!=null && samePageName(page,owner)) evidence.add(PageObjectCorrelation.Evidence.SAME_PAGE_IDENTITY);
                    matches.add(new Match(candidate,List.copyOf(evidence)));
                }
            }
            List<String> overriddenIds = overridesForApplicationElement(effectiveOverrides.sourceElementToApplicationElement(),element.elementId());
            if(overriddenIds.size()>1){results.add(result(page,element,null,PageObjectCorrelation.State.CONFLICT,List.of(PageObjectCorrelation.Evidence.USER_OVERRIDE),overriddenIds));continue;}
            String overridden = overriddenIds.isEmpty()?null:overriddenIds.get(0);
            if(overridden!=null) {
                var declaration=sourceElements.get(overridden);
                if(declaration==null) results.add(result(page,element,null,PageObjectCorrelation.State.CONFLICT,List.of(PageObjectCorrelation.Evidence.USER_OVERRIDE),List.of("Override references unknown source element: "+overridden)));
                else { results.add(result(page,element,declaration,PageObjectCorrelation.State.EXACT,List.of(PageObjectCorrelation.Evidence.USER_OVERRIDE),List.of())); count(pageClassMatches,page.pageId(),declaration.ownerClassId()); }
                continue;
            }
            if(matches.isEmpty()) { results.add(result(page,element,null,PageObjectCorrelation.State.NO_MATCH,List.of(),List.of())); continue; }
            List<Match> contextual=matches.stream().filter(match->match.evidence().contains(PageObjectCorrelation.Evidence.SAME_PAGE_IDENTITY)
                    || pageOverrideMatches(effectiveOverrides,match.element().ownerClassId(),page.pageId())).toList();
            List<Match> winners=contextual.isEmpty()?matches:contextual;
            if(winners.size()>1) { results.add(result(page,element,null,PageObjectCorrelation.State.AMBIGUOUS,commonEvidence(winners),winners.stream().map(match->match.element().declarationRef()).toList())); continue; }
            Match winner=winners.get(0); List<PageObjectCorrelation.Evidence> evidence=new ArrayList<>(winner.evidence());
            if(pageOverrideMatches(effectiveOverrides,winner.element().ownerClassId(),page.pageId()))evidence.add(PageObjectCorrelation.Evidence.USER_OVERRIDE);
            PageObjectCorrelation.State state=evidence.contains(PageObjectCorrelation.Evidence.USER_OVERRIDE)?PageObjectCorrelation.State.EXACT:
                    evidence.contains(PageObjectCorrelation.Evidence.SAME_PAGE_IDENTITY)||(evidence.contains(PageObjectCorrelation.Evidence.NORMALIZED_SELECTOR_MATCH)&&evidence.contains(PageObjectCorrelation.Evidence.LIVE_SAME_TARGET))?PageObjectCorrelation.State.STRONG:PageObjectCorrelation.State.PROBABLE;
            results.add(result(page,element,winner.element(),state,evidence,List.of())); count(pageClassMatches,page.pageId(),winner.element().ownerClassId());
        }
        List<PageObjectCorrelation.ClassCorrelation> classResults=new ArrayList<>();
        for(var page:application.pages()) for(var entry:pageClassMatches.getOrDefault(page.pageId(),Map.of()).entrySet()) {
            List<PageObjectCorrelation.Evidence> evidence=new ArrayList<>(); var owner=classes.get(entry.getKey());
            if(owner!=null&&samePageName(page,owner))evidence.add(PageObjectCorrelation.Evidence.SAME_PAGE_IDENTITY);
            List<PageObjectCorrelation.ElementCorrelation> strongElements=results.stream().filter(value->value.pageId().equals(page.pageId())&&value.sourceElementId()!=null).filter(value->{var sourceElement=sourceElements.get(value.sourceElementId());return sourceElement!=null&&sourceElement.ownerClassId().equals(entry.getKey())&&(value.state()==PageObjectCorrelation.State.STRONG||value.state()==PageObjectCorrelation.State.EXACT);}).toList();
            boolean hasStrongElement=!strongElements.isEmpty();
            strongElements.stream().flatMap(value->value.evidence().stream()).distinct().forEach(evidence::add);
            if(pageOverrideMatches(effectiveOverrides,entry.getKey(),page.pageId()))evidence.add(PageObjectCorrelation.Evidence.USER_OVERRIDE);
            PageObjectCorrelation.State state=evidence.contains(PageObjectCorrelation.Evidence.USER_OVERRIDE)?PageObjectCorrelation.State.EXACT:
                    evidence.contains(PageObjectCorrelation.Evidence.SAME_PAGE_IDENTITY)||hasStrongElement?PageObjectCorrelation.State.STRONG:PageObjectCorrelation.State.PROBABLE;
            classResults.add(new PageObjectCorrelation.ClassCorrelation(page.pageId(),entry.getKey(),state,evidence,List.of(),List.of()));
        }
        for(var override:effectiveOverrides.classToPage().entrySet()) if(!classes.containsKey(override.getKey())||!pages.containsKey(override.getValue()))
            classResults.add(new PageObjectCorrelation.ClassCorrelation(override.getValue(),override.getKey(),PageObjectCorrelation.State.CONFLICT,
                    List.of(PageObjectCorrelation.Evidence.USER_OVERRIDE),List.of("Override references unknown stable ID"),List.of()));
        for(var override:effectiveOverrides.classToPage().entrySet()) if(classes.containsKey(override.getKey())&&pages.containsKey(override.getValue())&&classResults.stream().noneMatch(value->value.classId().equals(override.getKey())&&value.pageId().equals(override.getValue())))
            classResults.add(new PageObjectCorrelation.ClassCorrelation(override.getValue(),override.getKey(),PageObjectCorrelation.State.EXACT,
                    List.of(PageObjectCorrelation.Evidence.USER_OVERRIDE),List.of(),List.of()));
        int exact=count(results,PageObjectCorrelation.State.EXACT),strong=count(results,PageObjectCorrelation.State.STRONG),probable=count(results,PageObjectCorrelation.State.PROBABLE),ambiguous=count(results,PageObjectCorrelation.State.AMBIGUOUS),noMatch=count(results,PageObjectCorrelation.State.NO_MATCH),conflicts=count(results,PageObjectCorrelation.State.CONFLICT);
        boolean partial=source.completeness()==ExistingProjectIndex.Completeness.PARTIAL||ambiguous+noMatch+conflicts>0;
        return new PageObjectCorrelation(PageObjectCorrelation.SCHEMA_VERSION,classResults,results,
                partial?PageObjectCorrelation.Completeness.PARTIAL:PageObjectCorrelation.Completeness.COMPLETE,
                partial?List.of("Not every observed element has one evidence-backed source declaration"):List.of(),
                new PageObjectCorrelation.Metrics(application.pages().size(),appElements.size(),source.elements().size(),exact,strong,probable,ambiguous,noMatch,conflicts));
    }
    private static List<PageObjectCorrelation.Evidence> selectorEvidence(ApplicationModel.ElementModel app,ExistingProjectIndex.ElementEntry source){
        List<PageObjectCorrelation.Evidence> evidence=new ArrayList<>();
        List<ApplicationModel.SelectorProjection> selectors=new ArrayList<>(); if(app.preferredSelector()!=null)selectors.add(app.preferredSelector());selectors.addAll(app.alternativeSelectors());
        for(var selector:selectors){
            boolean sameCandidate=selector.candidateId().equals(source.declarationRef())||selector.candidateId().equals(source.id());
            boolean normalizedMatch=normalize(selector.strategy()).equals(normalize(source.strategy())) && source.valueProjection()!=null && source.valueProjection().fingerprint()!=null
                    && source.valueProjection().fingerprint().equals(ExistingProjectIndexer.selectorValueFingerprint(selector.value()));
            if(sameCandidate)evidence.add(PageObjectCorrelation.Evidence.SAME_CANDIDATE_ID);
            if(normalizedMatch)evidence.add(PageObjectCorrelation.Evidence.NORMALIZED_SELECTOR_MATCH);
            if((sameCandidate||normalizedMatch) && selector.sameTarget()!=null && ("true".equalsIgnoreCase(selector.sameTarget())||selector.sameTarget().toUpperCase(Locale.ROOT).contains("SAME")))evidence.add(PageObjectCorrelation.Evidence.LIVE_SAME_TARGET);
        }
        return new ArrayList<>(evidence.stream().distinct().toList());
    }
    private static String normalize(String value){if(value==null)return"";return value.toLowerCase(Locale.ROOT).replace("by.","").replace("selector","").replace("_","").replace("-","").trim();}
    private static boolean samePageName(ApplicationModel.PageModel page,ExistingProjectIndex.ClassEntry owner){return normalizeName(owner.simpleName()).equals(normalizeName(page.canonicalName()));}
    private static String normalizeName(String value){return value==null?"":value.toLowerCase(Locale.ROOT).replaceAll("(page|component|screen|view)$","").replaceAll("[^a-z0-9]","");}
    private static boolean pageOverrideMatches(CorrelationOverrides overrides,String classId,String pageId){return pageId.equals(overrides.classToPage().get(classId));}
    private static List<String> overridesForApplicationElement(Map<String,String> values,String appId){return values.entrySet().stream().filter(entry->appId.equals(entry.getValue())).map(Map.Entry::getKey).sorted().toList();}
    private static void count(Map<String,Map<String,Integer>> values,String page,String owner){values.computeIfAbsent(page,key->new HashMap<>()).merge(owner,1,Integer::sum);}
    private static int count(List<PageObjectCorrelation.ElementCorrelation> values,PageObjectCorrelation.State state){return(int)values.stream().filter(value->value.state()==state).count();}
    private static PageObjectCorrelation.ElementCorrelation result(ApplicationModel.PageModel page,ApplicationModel.ElementModel app,ExistingProjectIndex.ElementEntry source,PageObjectCorrelation.State state,List<PageObjectCorrelation.Evidence> evidence,List<String> conflicts){return new PageObjectCorrelation.ElementCorrelation(page.pageId(),app.elementId(),source==null?null:source.id(),source==null?null:source.declarationRef(),state,evidence,conflicts,List.of());}
    private static List<PageObjectCorrelation.Evidence> commonEvidence(List<Match> matches){return matches.stream().flatMap(match->match.evidence().stream()).distinct().toList();}
    private record Match(ExistingProjectIndex.ElementEntry element,List<PageObjectCorrelation.Evidence> evidence){}
}
