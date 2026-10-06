package io.github.testlens.application.tooling.source;

import java.util.Comparator;
import java.util.List;

/** Bounded source/test impact of an ApplicationModel change. @since 0.5.0 */
public record SourceImpact(String applicationElementId,List<String>sourceDeclarationRefs,List<String>methodIds,
                           List<String>testIds,List<String>limitations){
    public SourceImpact{sourceDeclarationRefs=canonical(sourceDeclarationRefs);methodIds=canonical(methodIds);testIds=canonical(testIds);limitations=canonical(limitations);}
    private static List<String>canonical(List<String>values){return(values==null?List.<String>of():values).stream().filter(java.util.Objects::nonNull).distinct().sorted().toList();}
}
