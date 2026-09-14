package io.github.testlens.selenium.auth;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

final class AuthStateLockManager {
    private static final ConcurrentHashMap<Path, ReentrantLock> JVM_LOCKS = new ConcurrentHashMap<>();

    Path canonicalize(Path path) {
        Path absolute = path.toAbsolutePath().normalize();
        Path parent = absolute.getParent();
        if (parent != null && Files.exists(parent)) {
            try {
                return parent.toRealPath().resolve(absolute.getFileName()).normalize();
            } catch (IOException ignored) {
                // The absolute normalized identity remains deterministic when real-path resolution is unavailable.
            }
        }
        return absolute;
    }

    <T> T withLock(Path canonicalPath, LockedOperation<T> operation) {
        ReentrantLock processLock = JVM_LOCKS.computeIfAbsent(canonicalPath, ignored -> new ReentrantLock());
        processLock.lock();
        try {
            Path parent = canonicalPath.getParent();
            if (parent != null) Files.createDirectories(parent);
            Path sidecar = canonicalPath.resolveSibling(canonicalPath.getFileName() + ".lock");
            try (FileChannel channel = FileChannel.open(sidecar,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                 FileLock ignored = channel.lock()) {
                return operation.run();
            }
        } catch (ManagedAuthStateException e) {
            throw e;
        } catch (Exception e) {
            throw new ManagedAuthStateException(ManagedAuthStateFailureReason.LOCK_FAILED,
                    "Managed auth state lock failed", e);
        } finally {
            processLock.unlock();
        }
    }

    @FunctionalInterface
    interface LockedOperation<T> { T run() throws Exception; }
}
