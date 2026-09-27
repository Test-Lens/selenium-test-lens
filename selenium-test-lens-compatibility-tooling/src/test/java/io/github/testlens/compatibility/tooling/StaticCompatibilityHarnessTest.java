package io.github.testlens.compatibility.tooling;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;

class StaticCompatibilityHarnessTest {
    @TempDir Path temp;
    @Test @EnabledIfSystemProperty(named="compat.static.performance",matches="true") void offlineScaleHarness()throws Exception{
        for(int count:new int[]{100,1_000,10_000}){Path root=Files.createDirectories(temp.resolve("p"+count)),src=Files.createDirectories(root.resolve("src"));for(int i=0;i<count;i++)Files.writeString(src.resolve("C"+i+".java"),i%20==0?"import java.awt.Robot; class C"+i+"{void x()throws Exception{new Robot().mousePress(1);}}":"class C"+i+"{int x="+i+";}");long before=used(),start=System.nanoTime();var report=new StaticCompatibilityAnalyzer().analyze(new StaticCompatibilityScanRequest(root,List.of(Path.of("src")),List.of(),null,Map.of()));long scan=System.nanoTime()-start;start=System.nanoTime();byte[]json=new StaticCompatibilityReportJson().write(report);long jsonNs=System.nanoTime()-start;start=System.nanoTime();byte[]html=new StaticCompatibilityReportHtml().write(report);long htmlNs=System.nanoTime()-start;System.out.printf(Locale.ROOT,"STATIC_PERF files=%d parsed=%d findings=%d parseMs=%.1f symbolMs=%.1f detectMs=%.1f totalScanMs=%.1f jsonMs=%.1f htmlMs=%.1f memoryDeltaMiB=%.1f jsonBytes=%d htmlBytes=%d%n",count,report.coverage().filesParsed(),report.findings().size(),report.metrics().parseNanos()/1e6,report.metrics().symbolResolutionNanos()/1e6,report.metrics().detectionNanos()/1e6,scan/1e6,jsonNs/1e6,htmlNs/1e6,(used()-before)/1048576.0,json.length,html.length);}
    }
    @Test @EnabledIfSystemProperty(named="compat.static.selfAudit",matches="true") void selfAuditRepository()throws Exception{
        Path root=Path.of(System.getProperty("maven.multiModuleProjectDirectory","..")).toAbsolutePath().normalize();List<Path>roots=new ArrayList<>();try(var modules=Files.list(root)){for(Path module:modules.filter(Files::isDirectory).sorted().toList())for(String rel:List.of("src/main/java","src/test/java"))if(Files.isDirectory(module.resolve(rel)))roots.add(root.relativize(module.resolve(rel)));}var report=new StaticCompatibilityAnalyzer().analyze(new StaticCompatibilityScanRequest(root,roots,List.of(),null,Map.of()));System.out.printf("STATIC_SELF_AUDIT roots=%d files=%d parsed=%d failed=%d symbolIssues=%d findings=%s review=%d info=%d%n",roots.size(),report.coverage().javaFiles(),report.coverage().filesParsed(),report.coverage().filesFailed(),report.coverage().symbolResolutionIssues(),report.coverage().findingsByCode(),report.findings().stream().filter(f->f.severity().name().equals("REVIEW")).count(),report.findings().stream().filter(f->f.severity().name().equals("INFO")).count());
    }
    private static long used(){Runtime r=Runtime.getRuntime();return r.totalMemory()-r.freeMemory();}
}
