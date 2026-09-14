package io.github.testlens.selenium.auth;

import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Separate-JVM helper for the filesystem lock contract test. */
public final class AuthStateFileLockHolderMain {
    private AuthStateFileLockHolderMain() {}

    public static void main(String[] args) throws Exception {
        try (FileChannel channel = FileChannel.open(Path.of(args[0]),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock ignored = channel.lock()) {
            System.out.println("READY");
            System.out.flush();
            System.in.read();
        }
    }
}
