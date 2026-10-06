package io.github.testlens.studio.projection;

import io.github.testlens.application.tooling.source.PageObjectCorrelation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StudioProjectionFactoryTest {
    @Test void correlationProjectionIsBoundedAndPreservesDomainStates() {
        var entries=java.util.stream.IntStream.range(0,600).mapToObj(i->new PageObjectCorrelation.ElementCorrelation(
                "page","app-"+i,i%2==0?"source-"+i:null,i%2==0?"LoginPage.java#field"+i:null,
                i%2==0?PageObjectCorrelation.State.EXACT:PageObjectCorrelation.State.NO_MATCH,
                i%2==0?List.of(PageObjectCorrelation.Evidence.NORMALIZED_SELECTOR_MATCH):List.of(),List.of(),List.of())).toList();
        var domain=new PageObjectCorrelation(PageObjectCorrelation.SCHEMA_VERSION,List.of(),entries,
                PageObjectCorrelation.Completeness.PARTIAL,List.of("unmatched"),new PageObjectCorrelation.Metrics(1,600,300,300,0,0,0,300,0));
        var projection=new StudioProjectionFactory().correlations(domain,10,1_000);
        assertEquals(500,projection.items().size());
        assertEquals(600,projection.total());
        assertEquals(StageStatus.NEEDS_REVIEW,projection.stage().status());
        // Domain correlation canonicalizes by applicationElementId before pagination.
        assertEquals("app-107",projection.items().get(0).applicationElementId());
        assertEquals("NO_MATCH",projection.items().get(0).state());
        assertEquals("app-108",projection.items().get(1).applicationElementId());
        assertEquals("EXACT",projection.items().get(1).state());
    }

    @Test void commonStageCanonicalizesAndBoundsPresentationData() {
        var stage=new StageProjection(StageStatus.PARTIAL,
                java.util.stream.IntStream.range(0,700).mapToObj(i->new StageProjection.Item("E"+i,"detail")).toList(),
                List.of(new StageProjection.Item("L","limit")),List.of(),List.of());
        assertEquals(500,stage.evidence().size());
        assertEquals("limit",stage.limitations().get(0).detail());
    }
}
