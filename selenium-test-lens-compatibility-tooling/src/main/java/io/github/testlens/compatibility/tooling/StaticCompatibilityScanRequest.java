package io.github.testlens.compatibility.tooling;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Explicit trusted input for a bounded Java 17 source scan. */
public record StaticCompatibilityScanRequest(Path projectRoot, List<Path> sourceRoots,
        List<Path> classpathEntries, Charset encoding, Map<String,String> sourceTestRefs) {
    public static final int MAX_SOURCE_ROOTS=32, MAX_CLASSPATH_ENTRIES=256, MAX_FILES=25_000;
    public static final long MAX_SOURCE_BYTES=2L*1024*1024;
    public StaticCompatibilityScanRequest {
        if(projectRoot==null)throw new IllegalArgumentException("projectRoot required");
        sourceRoots=sourceRoots==null?List.of():List.copyOf(sourceRoots);
        classpathEntries=classpathEntries==null?List.of():List.copyOf(classpathEntries);
        encoding=encoding==null?StandardCharsets.UTF_8:encoding;
        sourceTestRefs=sourceTestRefs==null?Map.of():Map.copyOf(sourceTestRefs);
        if(sourceRoots.isEmpty()||sourceRoots.size()>MAX_SOURCE_ROOTS)throw new IllegalArgumentException("1..32 source roots required");
        if(classpathEntries.size()>MAX_CLASSPATH_ENTRIES)throw new IllegalArgumentException("too many classpath entries");
        if(sourceTestRefs.size()>MAX_FILES)throw new IllegalArgumentException("too many source correlations");
    }
}
