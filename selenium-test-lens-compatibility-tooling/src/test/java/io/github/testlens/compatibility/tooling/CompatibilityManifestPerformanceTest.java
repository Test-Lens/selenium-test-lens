package io.github.testlens.compatibility.tooling;

import io.github.testlens.compatibility.engine.CompatibilityAttempts;
import io.github.testlens.compatibility.engine.CompatibilityCaptureDescriptor;
import io.github.testlens.compatibility.engine.CompatibilityRunManifest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.github.testlens.compatibility.engine.CompatibilityRunManifest.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class CompatibilityManifestPerformanceTest {
    @Test void offlineBuildSerializeReadMatrix(){
        assumeTrue(Boolean.getBoolean("compatibility.performance"),"opt-in diagnostic harness");
        for(int count:new int[]{100,1_000,10_000})run(count);
    }

    private static void run(int count){
        Runtime runtime=Runtime.getRuntime();runtime.gc();long beforeMemory=runtime.totalMemory()-runtime.freeMemory();
        long start=System.nanoTime();List<CompatibilityRunManifest> manifests=new ArrayList<>(count);
        for(int i=0;i<count;i++)manifests.add(manifest(i));long built=System.nanoTime();
        CompatibilityManifestJson codec=new CompatibilityManifestJson();List<byte[]> documents=new ArrayList<>(count);long bytes=0;
        for(CompatibilityRunManifest manifest:manifests){byte[] document=codec.write(manifest);documents.add(document);bytes+=document.length;}long serialized=System.nanoTime();
        for(byte[] document:documents)codec.read(document);long parsed=System.nanoTime();
        long afterMemory=runtime.totalMemory()-runtime.freeMemory();
        System.out.printf("COMPAT_MANIFEST_PERF count=%d buildMs=%.3f serializeMs=%.3f parseMs=%.3f bytes=%d bytesPerManifest=%.1f memoryDeltaBytes=%d%n",
                count,millis(start,built),millis(built,serialized),millis(serialized,parsed),bytes,bytes/(double)count,Math.max(0,afterMemory-beforeMemory));
    }

    private static CompatibilityRunManifest manifest(int i){
        CompatibilityCaptureDescriptor descriptor=CompatibilityCaptureDescriptor.builder()
                .testKey(Framework.JUNIT5,"example.Compatibility"+(i%20),"case"+(i%100),"logical",true,"row-"+i,"suite")
                .requestedHeadless(i%2==0?RequestedHeadlessMode.HEADLESS:RequestedHeadlessMode.HEADED,RequestedHeadlessProvenance.CALLER_SUPPLIED)
                .attestEffectiveHeadless(i%2==0?EffectiveHeadlessState.HEADLESS:EffectiveHeadlessState.HEADED,EffectiveHeadlessProvenance.MANAGED_FACTORY_ATTESTED)
                .observability(i%3==0?ObservabilityMode.FAST:ObservabilityMode.DEFAULT,HudPreset.STANDARD,i%3!=0,true,true,i%3==0?"SUMMARY_ONLY":"RETAIN_TRACE","OFF")
                .datasetKey("dataset-"+(i%5)).environmentKey("env").testSourceRevision("revision-a").systemUnderTestRevision("build-b")
                .attempt(CompatibilityAttempts.attempt(1,ResultStatus.PASSED,null,null,null,null)).build();
        return new CompatibilityManifestBuilder().build(descriptor,null,null,null);
    }
    private static double millis(long a,long b){return(b-a)/1_000_000d;}
}
