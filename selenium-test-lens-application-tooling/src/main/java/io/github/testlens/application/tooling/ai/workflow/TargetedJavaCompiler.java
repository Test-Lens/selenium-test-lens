package io.github.testlens.application.tooling.ai.workflow;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/** In-process, source-non-mutating compiler boundary with optimistic source preconditions. @since 0.5.0 */
public final class TargetedJavaCompiler {
    private final JavaCompiler compiler;

    public TargetedJavaCompiler() { this(ToolProvider.getSystemJavaCompiler()); }
    public TargetedJavaCompiler(JavaCompiler compiler) { this.compiler = compiler; }

    public CompilationResult compile(CompilationRequest request, Map<Path, String> currentSources) {
        Objects.requireNonNull(request, "request");
        currentSources = Map.copyOf(currentSources == null ? Map.of() : currentSources);
        List<CompilationDiagnostic> preconditions = verifyPreconditions(request, currentSources);
        if (!preconditions.isEmpty()) return new CompilationResult(false, preconditions, CompiledOutput.empty());
        if (compiler == null) return new CompilationResult(false,
                List.of(new CompilationDiagnostic("NO_SYSTEM_COMPILER", null, -1, -1, "A JDK compiler is required")), CompiledOutput.empty());

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager standard = compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8);
             MemoryFileManager files = new MemoryFileManager(standard)) {
            List<JavaFileObject> units = request.sources().stream().map(SourceUnit::asJavaFileObject).toList();
            List<String> options = List.of("-proc:none", "--release", Integer.toString(request.release()),
                    "-classpath", request.classpath());
            boolean successful = Boolean.TRUE.equals(compiler.getTask(null, files, diagnostics, options, null, units).call());
            List<CompilationDiagnostic> bounded = diagnostics.getDiagnostics().stream().limit(request.maxDiagnostics())
                    .map(TargetedJavaCompiler::diagnostic).toList();
            return new CompilationResult(successful, bounded, files.output());
        } catch (IOException failure) {
            return new CompilationResult(false,
                    List.of(new CompilationDiagnostic("COMPILER_IO", null, -1, -1, bounded(failure.getMessage(), 1_000))), CompiledOutput.empty());
        }
    }

    private static List<CompilationDiagnostic> verifyPreconditions(CompilationRequest request, Map<Path, String> currentSources) {
        List<CompilationDiagnostic> result = new ArrayList<>();
        for (SourceUnit source : request.sources()) {
            Path path = source.path().normalize();
            String current = currentSources.get(path);
            String actual = current == null ? ArtifactEnvelope.digest(new byte[0]) : ArtifactEnvelope.digest(current);
            if (!actual.equals(source.expectedCurrentFingerprint())) {
                result.add(new CompilationDiagnostic("SOURCE_PRECONDITION_FAILED", path, -1, -1,
                        "expected=" + source.expectedCurrentFingerprint() + ", actual=" + actual));
            }
        }
        return result;
    }

    private static CompilationDiagnostic diagnostic(Diagnostic<? extends JavaFileObject> value) {
        Path path = null;
        if (value.getSource() != null) {
            try { path = Path.of(value.getSource().toUri().getPath()).normalize(); } catch (RuntimeException ignored) { }
        }
        return new CompilationDiagnostic(value.getKind().name(), path, value.getLineNumber(), value.getColumnNumber(),
                bounded(value.getMessage(Locale.ROOT), 2_000));
    }

    private static String bounded(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max);
    }

    public record CompilationRequest(List<SourceUnit> sources, int release, String classpath, int maxDiagnostics) {
        public CompilationRequest {
            sources = List.copyOf(sources == null ? List.of() : sources);
            if (sources.isEmpty()) throw new IllegalArgumentException("at least one source is required");
            if (release < 8 || release > Runtime.version().feature()) throw new IllegalArgumentException("unsupported Java release");
            classpath = classpath == null ? "" : classpath;
            if (maxDiagnostics < 1 || maxDiagnostics > 1_000) throw new IllegalArgumentException("invalid diagnostic bound");
        }
    }

    public record SourceUnit(Path path, String binaryName, String content, String expectedCurrentFingerprint) {
        public SourceUnit {
            if (path == null || path.isAbsolute() || binaryName == null || binaryName.isBlank() || content == null
                    || expectedCurrentFingerprint == null || expectedCurrentFingerprint.isBlank()) {
                throw new IllegalArgumentException("complete relative source precondition is required");
            }
            path = path.normalize();
            if (path.startsWith("..")) throw new IllegalArgumentException("source path escapes target root");
        }
        JavaFileObject asJavaFileObject() { return new SourceFile(binaryName, content); }
    }

    public record CompilationResult(boolean successful, List<CompilationDiagnostic> diagnostics, CompiledOutput output) {
        public CompilationResult { diagnostics = List.copyOf(diagnostics); Objects.requireNonNull(output, "output"); }
        public int generatedClasses() { return output.classCount(); }
    }
    public record CompilationDiagnostic(String code, Path path, long line, long column, String message) { }

    /** Immutable compiled bytecode boundary used only for controlled targeted execution. */
    public static final class CompiledOutput {
        private final Map<String, byte[]> classes;
        private CompiledOutput(Map<String, byte[]> classes) {
            this.classes = classes.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                    Map.Entry::getKey, value -> value.getValue().clone()));
        }
        private static CompiledOutput empty() { return new CompiledOutput(Map.of()); }
        public int classCount() { return classes.size(); }
        public Class<?> loadClass(String binaryName, ClassLoader parent) throws ClassNotFoundException {
            if (!classes.containsKey(binaryName)) throw new ClassNotFoundException(binaryName);
            return new MemoryClassLoader(parent, classes).loadClass(binaryName);
        }
        /** Writes verified compiled classes to a caller-approved output root using atomic per-file replacement. */
        public void writeTo(Path outputRoot) throws IOException {
            Path root=Objects.requireNonNull(outputRoot,"outputRoot").toAbsolutePath().normalize();
            Files.createDirectories(root);
            Path realRoot=root.toRealPath();
            for(Map.Entry<String,byte[]> entry:classes.entrySet()){
                Path target=realRoot.resolve(entry.getKey().replace('.',java.io.File.separatorChar)+".class").normalize();
                if(!target.startsWith(realRoot))throw new IOException("Compiled class path escapes output root");
                Files.createDirectories(target.getParent());
                Path temporary=Files.createTempFile(target.getParent(),target.getFileName().toString(),".tmp");
                try{
                    Files.write(temporary,entry.getValue());
                    try{Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
                    catch(java.nio.file.AtomicMoveNotSupportedException unsupported){Files.move(temporary,target,StandardCopyOption.REPLACE_EXISTING);}
                }finally{Files.deleteIfExists(temporary);}
            }
        }
        @Override public String toString() {
            return classes.entrySet().stream().sorted(Map.Entry.comparingByKey())
                    .map(value -> value.getKey() + "=" + ArtifactEnvelope.digest(value.getValue()))
                    .collect(java.util.stream.Collectors.joining(",", "CompiledOutput[", "]"));
        }
    }

    private static final class SourceFile extends SimpleJavaFileObject {
        private final String content;
        private SourceFile(String binaryName, String content) {
            super(URI.create("string:///" + binaryName.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.content = content;
        }
        @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) { return content; }
    }

    private static final class ClassFile extends SimpleJavaFileObject {
        private final String className;
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private ClassFile(String className, Kind kind) {
            super(URI.create("memory:///" + className.replace('.', '/') + kind.extension), kind);
            this.className = className;
        }
        @Override public ByteArrayOutputStream openOutputStream() { return bytes; }
    }

    private static final class MemoryFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {
        private final List<ClassFile> classes = new ArrayList<>();
        private MemoryFileManager(StandardJavaFileManager delegate) { super(delegate); }
        @Override public JavaFileObject getJavaFileForOutput(JavaFileManager.Location location, String className,
                JavaFileObject.Kind kind, FileObject sibling) {
            ClassFile output = new ClassFile(className, kind);
            classes.add(output);
            return output;
        }
        private CompiledOutput output() {
            return new CompiledOutput(classes.stream().collect(java.util.stream.Collectors.toMap(
                    value -> value.className, value -> value.bytes.toByteArray())));
        }
    }

    private static final class MemoryClassLoader extends ClassLoader {
        private final Map<String, byte[]> classes;
        private MemoryClassLoader(ClassLoader parent, Map<String, byte[]> classes) {
            super(parent);
            this.classes = classes;
        }
        @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
                Class<?> loaded=findLoadedClass(name);
                if(loaded==null)loaded=classes.containsKey(name)?findClass(name):super.loadClass(name,false);
                if(resolve)resolveClass(loaded);
                return loaded;
            }
        }
        @Override protected Class<?> findClass(String name) throws ClassNotFoundException {
            byte[] bytes = classes.get(name);
            if (bytes == null) return super.findClass(name);
            return defineClass(name, bytes, 0, bytes.length);
        }
    }
}
