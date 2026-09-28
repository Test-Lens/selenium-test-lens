package io.github.testlens.compatibility.tooling;

import io.github.testlens.compatibility.engine.ComparisonIntent;
import io.github.testlens.compatibility.engine.CompatibilityCompareEngine;
import io.github.testlens.compatibility.engine.CompatibilityRunManifest;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.github.testlens.compatibility.engine.CompatibilityRunManifest.*;

/** Opt-in diagnostics only; no timing assertion belongs in CI. */
class CompatibilityComparisonPerformanceTest {
    @Test void measuresHeadlessAndObservabilityComparisonPipeline(){
        Assumptions.assumeTrue(Boolean.getBoolean("compatibility.compare.performance"));
        int[] counts=java.util.Arrays.stream(System.getProperty("compatibility.compare.counts","1000,5000,10000").split(",")).map(String::trim).mapToInt(Integer::parseInt).toArray();
        String selected=System.getProperty("compatibility.compare.axis","ALL");
        System.out.printf("COMPAT_COMPARE_ENV java=%s os=%s processors=%d maxHeapMiB=%d warmups=0 repetitions=1 methodology=phase-used-heap%n",System.getProperty("java.version"),System.getProperty("os.name"),Runtime.getRuntime().availableProcessors(),Runtime.getRuntime().maxMemory()/1048576);
        for(int count:counts){
            if(selected.equals("ALL")||selected.equals("HEADLESS_MODE"))run(count,ComparisonIntent.Axis.HEADLESS_MODE);
            if(selected.equals("ALL")||selected.equals("OBSERVABILITY_MODE"))run(count,ComparisonIntent.Axis.OBSERVABILITY_MODE);
            System.gc();
        }
    }

    private void run(int count,ComparisonIntent.Axis axis){
        long beforeMemory=used(),start=System.nanoTime();List<CompatibilityRunManifest>b=new ArrayList<>(count),v=new ArrayList<>(count);
        for(int i=0;i<count;i++){String key="perf-"+i;if(axis==ComparisonIntent.Axis.HEADLESS_MODE){b.add(CompatibilityReportToolingTest.manifest(key,EffectiveHeadlessState.HEADED,ObservabilityMode.DEFAULT,ResultStatus.PASSED,1));v.add(CompatibilityReportToolingTest.manifest(key,EffectiveHeadlessState.HEADLESS,ObservabilityMode.DEFAULT,ResultStatus.PASSED,1));}else{b.add(CompatibilityReportToolingTest.manifest(key,EffectiveHeadlessState.HEADLESS,ObservabilityMode.DEFAULT,ResultStatus.PASSED,1));v.add(CompatibilityReportToolingTest.manifest(key,EffectiveHeadlessState.HEADLESS,ObservabilityMode.FAST,ResultStatus.PASSED,1));}}
        long peak=used(),built=System.nanoTime();CompatibilityManifestJson codec=new CompatibilityManifestJson();List<byte[]>encodedB=new ArrayList<>(count),encodedV=new ArrayList<>(count);for(int i=0;i<count;i++){encodedB.add(codec.write(b.get(i)));encodedV.add(codec.write(v.get(i)));}peak=Math.max(peak,used());long inputSerialized=System.nanoTime();b.clear();v.clear();for(int i=0;i<count;i++){b.add(codec.read(encodedB.get(i)));v.add(codec.read(encodedV.get(i)));}encodedB.clear();encodedV.clear();peak=Math.max(peak,used());long loaded=System.nanoTime();var report=new CompatibilityCompareEngine().compare(b,v,new ComparisonIntent(axis,"baseline","variant"));peak=Math.max(peak,used());long compared=System.nanoTime();byte[]json=new CompatibilityReportJson().write(report);peak=Math.max(peak,used());long serialized=System.nanoTime();byte[]html=new CompatibilityReportHtml().write(report);peak=Math.max(peak,used());long rendered=System.nanoTime();
        System.out.printf("COMPAT_COMPARE_PERF axis=%s count=%d inputBuildMs=%.1f inputSerializeMs=%.1f inputParseLoadMs=%.1f compareMs=%.1f jsonMs=%.1f htmlMs=%.1f observedHeapDeltaMiB=%.1f endHeapDeltaMiB=%.1f jsonBytes=%d htmlBytes=%d matched=%d%n",axis,count,ms(start,built),ms(built,inputSerialized),ms(inputSerialized,loaded),ms(loaded,compared),ms(compared,serialized),ms(serialized,rendered),Math.max(0,peak-beforeMemory)/1048576d,Math.max(0,used()-beforeMemory)/1048576d,json.length,html.length,report.coverage().matched());
    }
    private static double ms(long a,long b){return(b-a)/1_000_000d;}private static long used(){Runtime r=Runtime.getRuntime();return r.totalMemory()-r.freeMemory();}
}
