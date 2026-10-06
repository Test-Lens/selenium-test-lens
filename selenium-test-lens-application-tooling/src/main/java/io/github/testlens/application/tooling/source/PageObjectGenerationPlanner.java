package io.github.testlens.application.tooling.source;

import io.github.testlens.selector.tooling.ExistingProjectIndex;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Prevents duplicate generation when an evidence-backed existing Page Object already represents a page. @since 0.5.0 */
public final class PageObjectGenerationPlanner {
    public Decision decide(String pageId,PageObjectCorrelation correlation,ExistingProjectIndex index){
        Objects.requireNonNull(pageId,"pageId");Objects.requireNonNull(correlation,"correlation");Objects.requireNonNull(index,"index");
        Map<String,ExistingProjectIndex.ClassEntry>classes=index.classes().stream().collect(Collectors.toMap(ExistingProjectIndex.ClassEntry::id,value->value));
        List<ExistingProjectIndex.ClassEntry>matches=correlation.classes().stream().filter(value->pageId.equals(value.pageId())&&(value.state()==PageObjectCorrelation.State.EXACT||value.state()==PageObjectCorrelation.State.STRONG)).map(value->classes.get(value.classId())).filter(Objects::nonNull).filter(value->value.classification()==ExistingProjectIndex.ClassClassification.PAGE_OBJECT).distinct().sorted(java.util.Comparator.comparing(ExistingProjectIndex.ClassEntry::qualifiedName)).toList();
        if(matches.isEmpty())return new Decision(Action.GENERATE,null,null,List.of("NO_EVIDENCE_BACKED_EXISTING_PAGE_OBJECT"));
        if(matches.size()>1)return new Decision(Action.CONFLICT,null,null,matches.stream().map(ExistingProjectIndex.ClassEntry::qualifiedName).toList());
        var match=matches.get(0);return new Decision(Action.REUSE_EXISTING,match.id(),match.qualifiedName(),List.of("CORRELATION_"+correlation.classes().stream().filter(value->value.classId().equals(match.id())&&value.pageId().equals(pageId)).findFirst().orElseThrow().state(),"ORIGIN_"+match.origin()));
    }
    public record Decision(Action action,String classId,String qualifiedName,List<String>evidence){public Decision{evidence=(evidence==null?List.<String>of():evidence).stream().filter(Objects::nonNull).distinct().sorted().toList();}}
    public enum Action{REUSE_EXISTING,GENERATE,CONFLICT}
}
