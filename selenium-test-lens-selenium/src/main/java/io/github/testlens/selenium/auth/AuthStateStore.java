package io.github.testlens.selenium.auth;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

class AuthStateStore {
    boolean exists(Path path) { return Files.isRegularFile(path); }

    AuthState load(Path path) { return AuthState.load(path); }

    void atomicReplace(Path target, AuthState state) {
        Path parent = target.getParent();
        Path temp = null;
        try {
            if (parent != null) Files.createDirectories(parent);
            Path directory = parent == null ? target.toAbsolutePath().getParent() : parent;
            temp = Files.createTempFile(directory, ".testlens-auth-", ".tmp");
            byte[] serialized = state.exportJson().getBytes(StandardCharsets.UTF_8);
            try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buffer = ByteBuffer.wrap(serialized);
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            new AuthStateJsonParser().parse(Files.readString(temp));
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            temp = null;
        } catch (AtomicMoveNotSupportedException e) {
            throw persistFailure(e);
        } catch (RuntimeException | IOException e) {
            if (e instanceof ManagedAuthStateException managed) throw managed;
            throw persistFailure(e);
        } finally {
            if (temp != null) {
                try { Files.deleteIfExists(temp); } catch (IOException ignored) {}
            }
        }
    }

    void invalidate(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new ManagedAuthStateException(ManagedAuthStateFailureReason.INVALIDATE_FAILED,
                    "Managed auth state invalidation failed", e);
        }
    }

    private static ManagedAuthStateException persistFailure(Throwable cause) {
        return new ManagedAuthStateException(ManagedAuthStateFailureReason.PERSIST_FAILED,
                "Managed auth state persistence failed", cause);
    }
}
