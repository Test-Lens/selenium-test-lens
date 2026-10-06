package io.github.testlens.application.tooling.source;

import io.github.testlens.selector.tooling.ExistingProjectIndex;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Traverses the bounded ExistingProjectIndex usage graph from correlated declaration to affected tests. @since 0.5.0 */
public final class SourceImpactAnalyzer {
    public SourceImpact analyze(String applicationElementId,PageObjectCorrelation correlation,ExistingProjectIndex index,int maxEdges){
        if(maxEdges<1)throw new IllegalArgumentException("maxEdges must be positive");
        Set<String>declarations=new HashSet<>();correlation.elements().stream().filter(value->applicationElementId.equals(value.applicationElementId())&&value.sourceDeclarationRef()!=null&&value.state()!=PageObjectCorrelation.State.AMBIGUOUS&&value.state()!=PageObjectCorrelation.State.CONFLICT).forEach(value->declarations.add(value.sourceDeclarationRef()));
        Set<String>methods=new HashSet<>();index.methods().stream().filter(value->value.declarationRefs().stream().anyMatch(declarations::contains)).forEach(value->methods.add(value.id()));
        index.edges().stream().filter(edge->edge.type()==ExistingProjectIndex.EdgeType.METHOD_TO_DECLARATION&&declarations.contains(edge.toId())).forEach(edge->methods.add(edge.fromId()));
        ArrayDeque<String>queue=new ArrayDeque<>(methods);int traversed=0;boolean truncated=false;
        while(!queue.isEmpty()) {String target=queue.removeFirst();for(var edge:index.edges())if(edge.type()==ExistingProjectIndex.EdgeType.METHOD_TO_METHOD&&target.equals(edge.toId())){if(++traversed>maxEdges){truncated=true;queue.clear();break;}if(methods.add(edge.fromId()))queue.addLast(edge.fromId());}}
        Set<String>tests=new HashSet<>();for(var edge:index.edges())if(edge.type()==ExistingProjectIndex.EdgeType.TEST_TO_METHOD&&methods.contains(edge.toId())){if(++traversed>maxEdges){truncated=true;break;}tests.add(edge.fromId());}
        return new SourceImpact(applicationElementId,declarations.stream().toList(),methods.stream().toList(),tests.stream().toList(),truncated?List.of("IMPACT_EDGE_LIMIT_REACHED"):List.of());
    }
}
