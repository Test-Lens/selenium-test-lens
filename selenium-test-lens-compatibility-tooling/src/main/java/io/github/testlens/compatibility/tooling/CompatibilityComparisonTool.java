package io.github.testlens.compatibility.tooling;

import io.github.testlens.compatibility.engine.ComparisonIntent;
import io.github.testlens.compatibility.engine.CompatibilityCompareEngine;
import io.github.testlens.compatibility.engine.CompatibilityComparisonReport;
import io.github.testlens.compatibility.engine.CompatibilityRunManifest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Offline orchestrator over explicitly supplied manifest files or non-recursive directories. */
public final class CompatibilityComparisonTool {
    public static final long MAX_AGGREGATE_INPUT_BYTES_PER_SIDE=128L*1024*1024;
    public static final long MAX_ESTIMATED_RETAINED_BYTES_PER_SIDE=256L*1024*1024;
    private final CompatibilityManifestJson manifests=new CompatibilityManifestJson();
    private final CompatibilityCompareEngine engine=new CompatibilityCompareEngine();
    private final long maximumAggregateBytes,maximumEstimatedRetainedBytes;

    public CompatibilityComparisonTool(){this(MAX_AGGREGATE_INPUT_BYTES_PER_SIDE,MAX_ESTIMATED_RETAINED_BYTES_PER_SIDE);}
    CompatibilityComparisonTool(long maximumAggregateBytes,long maximumEstimatedRetainedBytes){if(maximumAggregateBytes<1||maximumEstimatedRetainedBytes<1)throw new IllegalArgumentException("invalid manifest aggregate bound");this.maximumAggregateBytes=maximumAggregateBytes;this.maximumEstimatedRetainedBytes=maximumEstimatedRetainedBytes;}

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
        List<SizedInput> inputs=new ArrayList<>(files.size());long aggregate=0,estimated=0;
        for(Path path:files){long size=Files.size(path);if(size>CompatibilityManifestJson.MAX_DOCUMENT_BYTES)throw new ManifestInputLimitException("MANIFEST_INPUT_LIMIT_EXCEEDED",path.getFileName().toString());aggregate=addBounded(aggregate,size,maximumAggregateBytes,"AGGREGATE_MANIFEST_INPUT_LIMIT_EXCEEDED");estimated=addBounded(estimated,Math.max(8192L,Math.multiplyExact(size,4L)),maximumEstimatedRetainedBytes,"ESTIMATED_RETAINED_INPUT_LIMIT_EXCEEDED");inputs.add(new SizedInput(path,size));}
        List<CompatibilityRunManifest> result=new ArrayList<>(inputs.size());for(SizedInput input:inputs)result.add(manifests.read(readExact(input)));return List.copyOf(result);
    }

    private static long addBounded(long total,long value,long maximum,String code){long next;try{next=Math.addExact(total,value);}catch(ArithmeticException failure){throw new ManifestInputLimitException(code,"overflow");}if(next>maximum)throw new ManifestInputLimitException(code,Long.toString(next));return next;}
    private static byte[]readExact(SizedInput input)throws IOException{if(input.size>Integer.MAX_VALUE)throw new ManifestInputLimitException("MANIFEST_INPUT_LIMIT_EXCEEDED",input.path.getFileName().toString());byte[]bytes=new byte[(int)input.size];try(InputStream stream=Files.newInputStream(input.path)){int offset=0;while(offset<bytes.length){int read=stream.read(bytes,offset,bytes.length-offset);if(read<0)throw new IOException("MANIFEST_INPUT_CHANGED_DURING_READ: "+input.path.getFileName());offset+=read;}if(stream.read()!=-1)throw new IOException("MANIFEST_INPUT_CHANGED_DURING_READ: "+input.path.getFileName());}return bytes;}
    private record SizedInput(Path path,long size){}
    public static final class ManifestInputLimitException extends IllegalArgumentException{private final String code;ManifestInputLimitException(String code,String detail){super(code+": "+detail);this.code=code;}public String code(){return code;}}
}
