package io.github.testlens.application.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationModelTest {
    @Test void idsAndCanonicalOrderingAreDeterministic() {
        String first=ApplicationIds.id("element-v1","page","button","Save");
        assertEquals(first,ApplicationIds.id("element-v1","page","button","Save"));
        assertEquals("save-order",ApplicationIds.semanticToken("  Save order! "));
        var provenance=ApplicationModel.Provenance.observed("o1","window=main",List.of("b","a"));
        var pageA=page("page-b",provenance);
        var pageB=page("page-a",provenance);
        var model=new ApplicationModel(1,"app","Example","0.5.0",Instant.EPOCH,List.of(pageA,pageB),List.of(),List.of(),
                new ApplicationModel.Coverage(ApplicationModel.Completeness.COMPLETE_FOR_OBSERVED_CONTEXT,1,2,2,0,0,0,0,0,false),List.of(),provenance);
        assertEquals(List.of("page-a","page-b"),model.pages().stream().map(ApplicationModel.PageModel::pageId).toList());
        assertEquals(List.of("a","b"),model.provenance().evidence());
    }

    @Test void rejectsUnknownSchema() {
        var p=ApplicationModel.Provenance.observed("o","c",List.of());
        assertThrows(IllegalArgumentException.class,()->new ApplicationModel(2,"a","a","v",Instant.EPOCH,List.of(),List.of(),List.of(),
                new ApplicationModel.Coverage(ApplicationModel.Completeness.PARTIAL,0,0,0,0,0,0,0,0,false),List.of(),p));
    }

    private static ApplicationModel.PageModel page(String id,ApplicationModel.Provenance p){
        var identity=new ApplicationModel.PageIdentity("/x/{id}","/x/{id}",null,List.of(),"fp",ApplicationModel.EvidenceSource.OBSERVED);
        var state=new ApplicationModel.PageState(ApplicationIds.id("state-v1",id,"default"),"default",List.of(),List.of(),p);
        return new ApplicationModel.PageModel(id,id,identity,List.of(),List.of("fp"),List.of(state),List.of(),List.of(),List.of(),List.of(),p,List.of());
    }
}
