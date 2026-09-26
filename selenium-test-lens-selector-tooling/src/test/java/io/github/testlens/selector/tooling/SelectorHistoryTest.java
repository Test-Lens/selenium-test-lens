package io.github.testlens.selector.tooling;

import io.github.testlens.selector.engine.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

import static io.github.testlens.selector.tooling.SelectorHistoryModel.*;
import static org.junit.jupiter.api.Assertions.*;

class SelectorHistoryTest {
    @TempDir Path temp;
    @Test void trustedHistoryComputesDigestFamilyAndChangedComparableEvidence(){SelectorHistoryAggregator a=new SelectorHistoryAggregator();a.add(trusted("one",run("r1",0),ComponentIdentity.of("TARGET","primary")));a.add(trusted("two",run("r2",0),ComponentIdentity.of("TARGET","primary")));History h=a.snapshot("project");assertEquals(1,h.subjects().size());Subject s=h.subjects().get(0);assertEquals(2,s.valueDigests().size());assertTrue(s.domainSummaries().stream().anyMatch(DomainSummary::changed));assertFalse(new String(SelectorHistoryJson.serialize(h,64*1024*1024),StandardCharsets.UTF_8).contains("user-card-one"));}
    @Test void componentPathSeparatesSubjectsAndRepeatedComponentIsStable(){SelectorHistoryAggregator a=new SelectorHistoryAggregator();ComponentIdentity first=ComponentIdentity.of("COMPOSITE_CHILD","children","0"),second=ComponentIdentity.of("COMPOSITE_CHILD","children","1");a.add(trusted("one",run("r1",0),first));a.add(trusted("one",run("r1",0),second));a.add(trusted("one",run("r1",0),first));assertEquals(2,a.snapshot(null).subjects().size());assertEquals(2,a.snapshot(null).subjects().stream().filter(s->s.componentRef().equals(first.componentRef())).findFirst().orElseThrow().samples().get(0).occurrenceCount());}
    @Test void dynamicValuesDoNotBecomeChanged(){SelectorHistoryAggregator a=new SelectorHistoryAggregator();a.add(trusted("123456789",run("r1",0),ComponentIdentity.of("TARGET","primary"),Comparability.ComparisonMode.EXPECTED_DYNAMIC_VALUE));a.add(trusted("987654321",run("r2",0),ComponentIdentity.of("TARGET","primary"),Comparability.ComparisonMode.EXPECTED_DYNAMIC_VALUE));assertTrue(a.snapshot(null).subjects().get(0).domainSummaries().stream().noneMatch(DomainSummary::changed));}
    @Test void reportValuesAreNeverDigestedClassifiedOrPersisted(){String json="{\"schemaVersion\":\"1.0\",\"retention\":{\"passedSessionRetention\":\"RETAIN_TRACE\"},\"events\":[{\"id\":\"e1\",\"locatorObservation\":{\"locator\":{\"strategy\":\"id\",\"value\":\"secret-550e8400-e29b-41d4-a716-446655440000\",\"valueState\":\"KNOWN\"},\"outcome\":\"RESOLVED\",\"matchCount\":{\"value\":1},\"usageSource\":{\"className\":\"P\",\"methodName\":\"m\",\"fileName\":\"P.java\",\"line\":2}}}]}";SelectorHistoryAggregator a=new SelectorHistoryAggregator();RuntimeReportHistoryImporter.ImportResult result=new RuntimeReportHistoryImporter().importReport(json.getBytes(StandardCharsets.UTF_8),run("report",0),a);assertEquals(1,result.observations());Subject s=a.snapshot(null).subjects().get(0);assertTrue(s.valueDigests().isEmpty());assertTrue(s.appearanceFamilies().isEmpty());assertTrue(s.structuralFamilies().isEmpty());String persisted=new String(SelectorHistoryJson.serialize(a.snapshot(null),64*1024*1024),StandardCharsets.UTF_8);assertFalse(persisted.contains("secret-"));assertTrue(persisted.contains("VALUE_UNAVAILABLE"));}
    @Test void reportImportIsIdempotentAndSummaryOnlyIsIncomplete(){byte[] report="{\"retention\":{\"passedSessionRetention\":\"SUMMARY_ONLY\"},\"events\":[]}".getBytes(StandardCharsets.UTF_8);SelectorHistoryAggregator a=new SelectorHistoryAggregator();RuntimeReportHistoryImporter importer=new RuntimeReportHistoryImporter();assertFalse(importer.importReport(report,run("r",0),a).duplicate());assertTrue(importer.importReport(report,run("r",0),a).duplicate());History h=a.snapshot(null);assertEquals(1,h.coverage().reportsImported());assertEquals(1,h.coverage().reportsSkipped());assertEquals(1,h.coverage().summaryOnlyRuns());assertTrue(h.coverage().incomplete());}
    @Test void deterministicJsonRoundTripsAndRejectsDuplicateFields()throws Exception{SelectorHistoryAggregator a=new SelectorHistoryAggregator();a.add(trusted("one",run("r1",0),ComponentIdentity.of("TARGET","primary")));History h=a.snapshot("project");byte[] one=SelectorHistoryJson.serialize(h,64*1024*1024),two=SelectorHistoryJson.serialize(h,64*1024*1024);assertArrayEquals(one,two);Path file=temp.resolve("history.json");Files.write(file,one);assertEquals(h,SelectorHistoryJson.read(file));Path bad=temp.resolve("bad.json");Files.writeString(bad,"{\"schemaVersion\":1,\"schemaVersion\":1}");assertThrows(Exception.class,()->SelectorHistoryJson.read(bad));}
    @Test void deterministicAggregationDoesNotUseImportOrder(){TrustedObservation first=trusted("one",run("r1",0),ComponentIdentity.of("TARGET","primary")),second=trusted("two",run("r2",0),ComponentIdentity.of("TARGET","primary"));SelectorHistoryAggregator a=new SelectorHistoryAggregator(),b=new SelectorHistoryAggregator();a.add(first);a.add(second);b.add(second);b.add(first);assertArrayEquals(SelectorHistoryJson.serialize(a.snapshot(null),64*1024*1024),SelectorHistoryJson.serialize(b.snapshot(null),64*1024*1024));}
    @Test void unknownChronologyUsesStableFallbackAndReportsIt(){SelectorHistoryAggregator a=new SelectorHistoryAggregator(new Limits(1,10,10,1024*1024,10,10,10));a.add(trusted("one",run("z",0),ComponentIdentity.of("TARGET","primary")));a.add(trusted("two",run("a",0),ComponentIdentity.of("TARGET","primary")));History h=a.snapshot(null);assertTrue(h.retention().pruned());assertTrue(h.retention().chronologyIncomplete());}
    @Test void trustedChronologyPrunesOldestRunWithoutUsingImportOrder(){
        RunDescriptor oldRun=new RunDescriptor("junit5","PTest","test","PTest#test","old",0,"dataset","tests-r1","app-r1","local",null,"sequence",1L);
        RunDescriptor newRun=new RunDescriptor("junit5","PTest","test","PTest#test","new",0,"dataset","tests-r1","app-r1","local",null,"sequence",2L);
        SelectorHistoryAggregator a=new SelectorHistoryAggregator(new Limits(1,10,10,1024*1024,10,10,10));
        a.add(trusted("new",newRun,ComponentIdentity.of("TARGET","primary")));
        a.add(trusted("old",oldRun,ComponentIdentity.of("TARGET","primary")));
        History history=a.snapshot(null);
        assertEquals(List.of(newRun.runRef()),history.runs().stream().map(Run::runRef).toList());
        assertFalse(history.retention().chronologyIncomplete());
    }
    @Test void ambiguousCorrelationsRemainSeparateBuckets(){
        SimilaritySubject.Correlation ambiguous=new SimilaritySubject.Correlation(SimilaritySubject.Confidence.AMBIGUOUS,List.of("USAGE_SOURCE"),List.of("MULTIPLE_DECLARATIONS"),List.of(),1);
        TrustedObservation first=withCorrelation(trusted("one",run("r1",0),ComponentIdentity.of("TARGET","primary")),"ambiguous-one",ambiguous);
        TrustedObservation second=withCorrelation(trusted("two",run("r1",0),ComponentIdentity.of("TARGET","primary")),"ambiguous-two",ambiguous);
        SelectorHistoryAggregator aggregate=new SelectorHistoryAggregator();aggregate.add(first);aggregate.add(second);
        assertEquals(2,aggregate.snapshot(null).subjects().size());
        assertEquals(2,aggregate.snapshot(null).coverage().ambiguousCorrelations());
    }
    @Test void localWriterEnforcesTargetBoundaryAndWritesAtomically()throws Exception{Path project=temp.resolve("project");Files.createDirectories(project.resolve("target/test-lens/selector-history"));SelectorHistoryAggregator a=new SelectorHistoryAggregator();a.add(trusted("one",run("r",0),ComponentIdentity.of("TARGET","primary")));Path output=Path.of("target/test-lens/selector-history/history-v1.json");SelectorHistoryJson.writeLocal(a,"project",project,output);assertTrue(Files.exists(project.resolve(output)));assertThrows(IllegalArgumentException.class,()->SelectorHistoryJson.writeLocal(a,"project",project,Path.of("../outside.json")));}
    @Test void serializationFailurePreservesExistingDestination()throws Exception{
        Path project=temp.resolve("atomic-project");Path destination=project.resolve("target/test-lens/selector-history/history-v1.json");Files.createDirectories(destination.getParent());Files.writeString(destination,"old-history");
        SelectorHistoryAggregator aggregate=new SelectorHistoryAggregator(new Limits(10,10,10,1024,10,10,10));
        RunDescriptor oversized=new RunDescriptor("x".repeat(4_000),"P","m","P#m","run",0,"dataset","tests","app","local",null,null,null);
        aggregate.add(trusted("one",oversized,ComponentIdentity.of("TARGET","primary")));
        assertThrows(SelectorHistoryJson.HistorySizeException.class,()->SelectorHistoryJson.writeLocal(aggregate,"project",project,project.relativize(destination)));
        assertEquals("old-history",Files.readString(destination));
    }
    @Test void sameTextAcrossDigestDomainsDiffers(){assertNotEquals(CanonicalDigests.digest("selector-project-v1","same"),CanonicalDigests.digest("selector-run-v1","same"));}
    @Test void absoluteMachinePathCannotBecomeProjectIdentity(){SelectorHistoryAggregator aggregate=new SelectorHistoryAggregator();assertThrows(IllegalArgumentException.class,()->aggregate.snapshot("D:\\secret\\workspace"));}
    @Test void historyReaderRejectsUnsupportedSchemaAndExcessiveStrings()throws Exception{
        Path unsupported=temp.resolve("unsupported.json");Files.writeString(unsupported,"{\"schemaVersion\":2}");assertThrows(Exception.class,()->SelectorHistoryJson.read(unsupported));
        Path excessive=temp.resolve("excessive.json");Files.writeString(excessive,"{\"x-value\":\""+"a".repeat(SelectorHistoryJson.MAX_STRING+1)+"\"}");assertThrows(Exception.class,()->SelectorHistoryJson.read(excessive));
    }
    private static TrustedObservation trusted(String suffix,RunDescriptor run,ComponentIdentity component){return trusted(suffix,run,component,Comparability.ComparisonMode.EXACT_VALUE);}private static TrustedObservation trusted(String suffix,RunDescriptor run,ComponentIdentity component,Comparability.ComparisonMode mode){SelectorSubject subject=new SelectorSubject(SelectorSubject.SubjectKind.STATIC_DECLARATION,"id",SelectorSubject.ValueState.KNOWN,"user-card-"+suffix,"java-decl-v1:sha256:"+"a".repeat(64),"module","src/P.java","P#m",null,null,null,SelectorSubject.InputTrust.SOURCE_CANONICAL,"By.id(template)");SimilaritySubject.Correlation correlation=new SimilaritySubject.Correlation(SimilaritySubject.Confidence.EXACT,List.of("DECLARATION_REF"),List.of(),List.of(),1);return new TrustedObservation(subject,component,run,ObservationEvidence.Domain.NEW_RUN,mode,"By.id(template)","obs-"+run.invocationDiscriminator()+suffix,correlation,SimilaritySubject.SourceState.CURRENT,new SimilaritySubject.UsageReference("usage-"+run.invocationDiscriminator(),"src/P.java","P","m",10),false);}private static RunDescriptor run(String invocation,int attempt){return new RunDescriptor("junit5","PTest","test","PTest#test",invocation,attempt,"dataset","tests-r1","app-r1","local",null,null,null);}
    private static TrustedObservation withCorrelation(TrustedObservation observation,String observationRef,SimilaritySubject.Correlation correlation){return new TrustedObservation(observation.subject(),observation.component(),observation.run(),observation.domain(),observation.comparisonMode(),observation.normalizedTemplate(),observationRef,correlation,observation.sourceState(),observation.usage(),observation.policyOrEvidenceConflict());}
}
