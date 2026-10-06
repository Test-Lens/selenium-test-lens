package io.github.testlens.application.tooling.store;

import io.github.testlens.application.model.ApplicationModel;
import io.github.testlens.application.tooling.json.ApplicationModelJson;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Atomic model persistence plus bounded, content-addressed history. @since 0.5.0 */
public final class ApplicationModelStore {
    private static final int MAX_HISTORY_SCAN = 10_000;
    private static final java.util.regex.Pattern HISTORY_FILE = java.util.regex.Pattern.compile("model-[0-9a-f]{64}\\.json");
    private final ApplicationModelJson codec;
    public ApplicationModelStore() { this(new ApplicationModelJson()); }
    public ApplicationModelStore(ApplicationModelJson codec) { this.codec = Objects.requireNonNull(codec, "codec"); }

    public void write(Path target, ApplicationModel model) {
        Objects.requireNonNull(target, "target"); Objects.requireNonNull(model, "model");
        Path absolute=target.toAbsolutePath().normalize();
        Path parent=absolute.getParent();if(parent==null)throw new StoreException("Target must have a parent directory");
        write(parent,absolute,model);
    }
    /** Writes beneath an explicit caller-owned boundary. */
    public void write(Path allowedRoot,Path target,ApplicationModel model){
        Objects.requireNonNull(model,"model");Path root=normalizeRoot(allowedRoot);Path absolute=normalizeTarget(root,target);requireSafe(root,absolute);atomicWrite(absolute,codec.write(model));
    }
    public ApplicationModel read(Path source) {
        Objects.requireNonNull(source,"source");Path absolute=source.toAbsolutePath().normalize();Path parent=absolute.getParent();if(parent==null)throw new StoreException("Source must have a parent directory");return read(parent,absolute);
    }
    /** Reads beneath an explicit caller-owned boundary and rejects symbolic-link traversal. */
    public ApplicationModel read(Path allowedRoot,Path source){
        Path root=normalizeRoot(allowedRoot);Path absolute=normalizeTarget(root,source);requireSafe(root,absolute);
        try { if(Files.size(absolute)>io.github.testlens.application.tooling.json.StrictJson.MAX_DOCUMENT_BYTES)throw new StoreException("Application model exceeds document size limit");return codec.read(Files.readAllBytes(absolute)); }
        catch (IOException failure) { throw new StoreException("Cannot read application model", failure); }
    }

    /** Writes at most one history entry per semantic model and retains the newest {@code maxEntries}. */
    public HistoryResult recordHistory(Path directory, ApplicationModel model, int maxEntries) {
        if (maxEntries < 1 || maxEntries > 1_000) throw new IllegalArgumentException("maxEntries must be in [1,1000]");
        String fingerprint = ApplicationModelFingerprint.semantic(model);
        String safeName = "model-"+fingerprint.substring(fingerprint.lastIndexOf(':') + 1) + ".json";
        Path root=normalizeRoot(directory);Path target = root.resolve(safeName).normalize();requireSafe(root,target);
        try {
            Files.createDirectories(root);
            if (Files.isSymbolicLink(root)) throw new StoreException("History directory must not be a symbolic link");
            requireSafe(root,target);
            boolean created = !Files.exists(target, LinkOption.NOFOLLOW_LINKS);
            if (created) atomicWrite(target, codec.write(model));
            List<Path> entries;
            try (var stream = Files.list(root)) {
                entries = stream.filter(path -> HISTORY_FILE.matcher(path.getFileName().toString()).matches())
                        .limit(MAX_HISTORY_SCAN+1L)
                        .sorted(Comparator.comparingLong(ApplicationModelStore::modified).reversed()
                                .thenComparing(path -> path.getFileName().toString())).toList();
            }
            if(entries.size()>MAX_HISTORY_SCAN)throw new StoreException("Application model history exceeds scan limit");
            int removed = 0;
            for (Path entry : entries.stream().skip(maxEntries).toList()) { Files.deleteIfExists(entry); removed++; }
            return new HistoryResult(fingerprint, target, created, removed);
        } catch (IOException failure) { throw new StoreException("Cannot update application model history", failure); }
    }

    private static void atomicWrite(Path target, byte[] document) {
        Path absolute = target.toAbsolutePath().normalize();
        Path parent = absolute.getParent();
        if (parent == null) throw new StoreException("Target must have a parent directory");
        Path temporary = null;
        try {
            Files.createDirectories(parent);
            if (Files.isSymbolicLink(parent) || Files.isSymbolicLink(absolute)) throw new StoreException("Symbolic-link targets are not supported");
            temporary = Files.createTempFile(parent, ".test-lens-model-", ".tmp");
            Files.write(temporary, document);
            try { Files.move(temporary, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException unsupported) { Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException failure) { throw new StoreException("Cannot atomically write " + target, failure); }
        finally { if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException ignored) { } }
    }
    private static Path normalizeRoot(Path allowedRoot){Objects.requireNonNull(allowedRoot,"allowedRoot");return allowedRoot.toAbsolutePath().normalize();}
    private static Path normalizeTarget(Path root,Path target){Objects.requireNonNull(target,"target");Path normalized=target.isAbsolute()?target.toAbsolutePath().normalize():root.resolve(target).normalize();if(!normalized.startsWith(root))throw new StoreException("Model path escapes the allowed root");return normalized;}
    private static void requireSafe(Path root,Path target){if(!target.startsWith(root))throw new StoreException("Model path escapes the allowed root");if(Files.exists(root,LinkOption.NOFOLLOW_LINKS)&&Files.isSymbolicLink(root))throw new StoreException("Allowed model root must not be a symbolic link");Path cursor=root;for(Path segment:root.relativize(target)){cursor=cursor.resolve(segment);if(Files.exists(cursor,LinkOption.NOFOLLOW_LINKS)&&Files.isSymbolicLink(cursor))throw new StoreException("Model path contains a symbolic link");}}
    private static long modified(Path path) { try { return Files.getLastModifiedTime(path).toMillis(); } catch (IOException ignored) { return Long.MIN_VALUE; } }

    public record HistoryResult(String semanticFingerprint, Path path, boolean created, int removedEntries) { }
    public static final class StoreException extends IllegalStateException {
        public StoreException(String message) { super(message); }
        public StoreException(String message, Throwable cause) { super(message, cause); }
    }
}
