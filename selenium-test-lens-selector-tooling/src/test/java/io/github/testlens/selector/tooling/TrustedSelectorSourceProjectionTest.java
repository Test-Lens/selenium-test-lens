package io.github.testlens.selector.tooling;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TrustedSelectorSourceProjectionTest {
 @TempDir Path root;
 @Test void bridgeExposesOnlyBoundedPatchFactsAndExactCallRange()throws Exception{Path file=source("p/Page.java","package p; import org.openqa.selenium.By; class Page { static final By SAVE = By.id(\"old\"); }");var snapshot=TrustedSelectorSourceProjection.scan(root,List.of(Path.of("src")),List.of());var declaration=snapshot.declarations().get(0);String text=Files.readString(file);assertEquals("By.id(\"old\")",text.substring(declaration.range().startOffset(),declaration.range().endOffsetExclusive()));assertEquals(TrustedSelectorSourceProjection.ExpressionShape.DIRECT_LITERAL,declaration.expressionShape());assertEquals("id",declaration.locatorStrategy());assertFalse(declaration.toString().contains("package p"));}
 @Test void useGraphRetainsSharedDeclarationConsumersWithoutSourceSnippets()throws Exception{source("p/Page.java","package p; import org.openqa.selenium.By; class Page { static final By SAVE = By.id(\"old\"); By one(){return SAVE;} By two(){return SAVE;} }");var snapshot=TrustedSelectorSourceProjection.scan(root,List.of(Path.of("src")),List.of());var graph=TrustedSelectorUseGraph.analyze(root,List.of(Path.of("src")),List.of(),snapshot);assertTrue(graph.coverage().totalKnownUseCount()>=2||graph.coverage().unresolvedSymbolIssues()>0);assertTrue(graph.useSites().size()<=TrustedSelectorUseGraph.MAX_RETAINED_USE_SITES);}
 @Test void exactFileLimitIsCompleteButActualTruncationPropagates()throws Exception{
  source("p/A.java","package p; class A {}");
  source("p/B.java","package p; class B {}");
  var exact=TrustedSelectorSourceProjection.scan(root,List.of(Path.of("src")),List.of(),2);assertTrue(exact.coverage().complete());assertFalse(exact.limitations().contains("FILES_LIMIT"));
  source("p/C.java","package p; class C {}");
  var limited=TrustedSelectorSourceProjection.scan(root,List.of(Path.of("src")),List.of(),2);assertFalse(limited.coverage().complete());assertTrue(limited.limitations().contains("FILES_LIMIT"));
  var graph=TrustedSelectorUseGraph.analyze(root,List.of(Path.of("src")),List.of(),limited);assertFalse(graph.coverage().complete());assertTrue(graph.issues().contains("FILES_LIMIT"));
  var repeated=TrustedSelectorSourceProjection.scan(root,List.of(Path.of("src")),List.of(),2);assertEquals(limited.coverage(),repeated.coverage());assertEquals(limited.limitations(),repeated.limitations());
 }
 private Path source(String logical,String value)throws Exception{Path file=root.resolve("src").resolve(logical);Files.createDirectories(file.getParent());Files.writeString(file,value,StandardCharsets.UTF_8);return file;}
}
