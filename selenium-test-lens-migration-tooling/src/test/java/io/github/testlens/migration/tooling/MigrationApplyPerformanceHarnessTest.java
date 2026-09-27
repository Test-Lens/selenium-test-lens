package io.github.testlens.migration.tooling;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

class MigrationApplyPerformanceHarnessTest {
    @TempDir Path temporary;

    @Test void boundedPlanningAndExactReplacementDiagnostics() throws Exception {
        for(int edits:new int[]{10,100,1_000}){
            long memory=used(),start=System.nanoTime();List<String>canonical=new ArrayList<>(edits);
            for(int i=0;i<edits;i++)canonical.add(i+":"+(i+10)+":"+MigrationSourceTransactionWriter.sha(("replacement-"+i).getBytes(StandardCharsets.UTF_8)));
            String digest="migration-apply-planning-harness-v1:sha256:"+MigrationDigests.digest("migration-apply-planning-harness-v1",canonical.toArray(String[]::new));
            System.out.printf("MIGRATION_S11B1_PLAN edits=%d planMs=%.2f memoryDelta=%d digest=%s%n",edits,(System.nanoTime()-start)/1e6,Math.max(0,used()-memory),digest);
        }
        String tx="migration-apply-transaction-v1:sha256:"+"a".repeat(64);var writer=new MigrationSourceTransactionWriter(MigrationSourceTransactionWriter.Faults.NONE);
        for(int files:new int[]{1,10,64}){
            Path root=temporary.resolve("files-"+files);Files.createDirectory(root);List<Path>paths=new ArrayList<>();byte[]original="old\n".getBytes(StandardCharsets.UTF_8),target="new\n".getBytes(StandardCharsets.UTF_8);
            for(int i=0;i<files;i++){Path p=root.resolve("F"+i+".java");Files.write(p,original);paths.add(p);}long start=System.nanoTime();
            for(int i=0;i<paths.size();i++)writer.replace(paths.get(i),target,tx,MigrationSourceTransactionWriter.sha(original),i,new MigrationApplyPlan.Metadata(false,List.of()));long apply=System.nanoTime()-start;start=System.nanoTime();
            for(int i=0;i<paths.size();i++)writer.replace(paths.get(i),original,tx,MigrationSourceTransactionWriter.sha(target),i,new MigrationApplyPlan.Metadata(false,List.of()));long rollback=System.nanoTime()-start;
            System.out.printf("MIGRATION_S11B1_IO files=%d applyMs=%.2f rollbackMs=%.2f bytes=%d%n",files,apply/1e6,rollback/1e6,(long)files*(original.length+target.length));
        }
    }
    private static long used(){Runtime r=Runtime.getRuntime();return r.totalMemory()-r.freeMemory();}
}
