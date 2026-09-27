package io.github.testlens.compatibility.tooling;

import io.github.testlens.compatibility.engine.ComparisonIntent;
import io.github.testlens.compatibility.engine.CompatibilityCompareEngine;
import io.github.testlens.compatibility.engine.CompatibilityComparisonReport;
import io.github.testlens.compatibility.engine.CompatibilityRunManifest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Offline orchestrator over explicitly supplied manifest files or non-recursive directories. */
public final class CompatibilityComparisonTool {
    private final CompatibilityManifestJson manifests=new CompatibilityManifestJson();
    private final CompatibilityCompareEngine engine=new CompatibilityCompareEngine();

    public CompatibilityComparisonReport compare(List<Path> baseline, List<Path> variant, ComparisonIntent intent) throws IOException {
        return engine.compare(load(baseline),load(variant),intent);
    }

    public List<CompatibilityRunManifest> load(List<Path> supplied) throws IOException {
        if(supplied==null||supplied.size()>CompatibilityCompareEngine.MAX_MANIFESTS_PER_SIDE)throw new IllegalArgumentException("explicit manifest input exceeds bound");
        List<Path> files=new ArrayList<>();
        for(Path path:supplied){
            Path normalized=path.toAbsolutePath().normalize();
            if(Files.isDirectory(normalized))try(var stream=Files.list(normalized)){stream.filter(p->Files.isRegularFile(p)&&p.getFileName().toString().endsWith(".json")).forEach(files::add);}
            else if(Files.isRegularFile(normalized))files.add(normalized);else throw new IOException("Manifest input does not exist: "+normalized.getFileName());
        }
        files=files.stream().distinct().sorted(Comparator.comparing(Path::toString)).toList();
        if(files.size()>CompatibilityCompareEngine.MAX_MANIFESTS_PER_SIDE)throw new IllegalArgumentException("expanded manifest input exceeds bound");
        List<CompatibilityRunManifest> result=new ArrayList<>(files.size());for(Path path:files)result.add(manifests.read(Files.readAllBytes(path)));return List.copyOf(result);
    }
}
