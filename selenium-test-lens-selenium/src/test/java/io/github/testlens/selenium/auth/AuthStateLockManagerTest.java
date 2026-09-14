package io.github.testlens.selenium.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthStateLockManagerTest {
    @TempDir Path temp;

    @Test
    void canonicalAliasesHaveTheSameIdentity() throws Exception {
        AuthStateLockManager locks = new AuthStateLockManager();
        Path direct = temp.resolve("state.json");
        Path alias = temp.resolve("child").resolve("..").resolve("state.json");
        assertEquals(locks.canonicalize(direct), locks.canonicalize(alias));
    }

    @Test
    void secondOwnerWaitsAndReleaseAfterExceptionAllowsEntry() throws Exception {
        AuthStateLockManager first = new AuthStateLockManager();
        AuthStateLockManager second = new AuthStateLockManager();
        Path path = first.canonicalize(temp.resolve("state.json"));
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicBoolean secondEntered = new AtomicBoolean();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            var owner = executor.submit(() -> first.withLock(path, () -> {
                entered.countDown();
                release.await();
                throw new ManagedAuthStateException(ManagedAuthStateFailureReason.PERSIST_FAILED, "expected");
            }));
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            var waiter = executor.submit(() -> second.withLock(path, () -> { secondEntered.set(true); return null; }));
            assertFalse(secondEntered.get());
            release.countDown();
            assertThrows(java.util.concurrent.ExecutionException.class, () -> owner.get(5, TimeUnit.SECONDS));
            waiter.get(5, TimeUnit.SECONDS);
            assertTrue(secondEntered.get());
        } finally {
            executor.shutdownNow();
        }
        assertTrue(Files.exists(path.resolveSibling("state.json.lock")));
        assertEquals("ok", second.withLock(path, () -> "ok"));
    }

    @Test
    void separateProcessFileLockBlocksUntilItsOwnerReleases() throws Exception {
        AuthStateLockManager locks = new AuthStateLockManager();
        Path target = locks.canonicalize(temp.resolve("state.json"));
        Path sidecar = target.resolveSibling("state.json.lock");
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        Process owner = new ProcessBuilder(java, "-cp", System.getProperty("java.class.path"),
                AuthStateFileLockHolderMain.class.getName(), sidecar.toString())
                .redirectErrorStream(true).start();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try (BufferedReader output = new BufferedReader(new InputStreamReader(owner.getInputStream()))) {
            assertEquals("READY", output.readLine());
            var waiter = executor.submit(() -> locks.withLock(target, () -> "entered"));
            assertThrows(TimeoutException.class, () -> waiter.get(200, TimeUnit.MILLISECONDS));
            owner.getOutputStream().write('\n');
            owner.getOutputStream().flush();
            assertEquals("entered", waiter.get(5, TimeUnit.SECONDS));
            assertTrue(owner.waitFor(5, TimeUnit.SECONDS));
            assertEquals(0, owner.exitValue());
        } finally {
            executor.shutdownNow();
            owner.destroyForcibly();
        }
    }
}
