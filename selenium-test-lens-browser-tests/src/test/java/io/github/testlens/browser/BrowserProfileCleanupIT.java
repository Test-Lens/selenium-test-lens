package io.github.testlens.browser;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class BrowserProfileCleanupIT {
    @Test
    void exactUserDataDirectoryMatchingDoesNotConfuseTenProfiles() {
        Path root = BrowserTestHarness.runRoot();
        List<Path> profiles = new ArrayList<>();
        for (int number = 1; number <= 10; number++) profiles.add(root.resolve("chrome-" + number));

        for (int expected = 0; expected < profiles.size(); expected++) {
            String[] arguments = {"--headless=new", "--user-data-dir=" + profiles.get(expected)};
            for (int candidate = 0; candidate < profiles.size(); candidate++) {
                assertEquals(expected == candidate,
                        BrowserTestHarness.profileArgumentMatches(arguments, profiles.get(candidate)),
                        arguments[1] + " must match only " + profiles.get(expected));
            }
        }
    }

    @Test
    void closingOneConcurrentOwnerLeavesTheOtherSessionAlive() {
        assumeTrue(BrowserTestHarness.browserName().equals("chrome"));
        WebDriver first = BrowserTestHarness.createDriver();
        WebDriver second = BrowserTestHarness.createDriver();
        try {
            first.get("data:text/html,<title>owner-a</title>");
            second.get("data:text/html,<title>owner-b</title>");
            Path firstProfile = BrowserTestHarness.ownedProfile(first);
            Path secondProfile = BrowserTestHarness.ownedProfile(second);
            List<Long> secondDriverPids = BrowserTestHarness.ownedChromeDriverProcessIds(second);
            assertFalse(secondDriverPids.isEmpty(), "owner B must record its ChromeDriver process");

            first.quit();

            assertEquals("owner-b", second.getTitle());
            assertTrue(second.getCurrentUrl().startsWith("data:text/html"));
            assertTrue(secondDriverPids.stream().allMatch(BrowserProfileCleanupIT::isAlive),
                    "closing owner A must not terminate owner B's ChromeDriver");
            assertFalse(Files.exists(firstProfile), "owner A profile must be deleted");
            assertTrue(Files.isDirectory(secondProfile), "owner B profile must remain until owner B closes");
        } finally {
            first.quit();
            second.quit();
        }

        assertEquals(List.of(), BrowserTestHarness.aliveOwnedProcessIds());
        assertEquals(List.of(), BrowserTestHarness.remainingOwnedProfiles());
    }

    @Test
    void concurrentStartupAssignsIndependentOwners() throws Exception {
        assumeTrue(BrowserTestHarness.browserName().equals("chrome"));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<WebDriver> firstFuture = executor.submit(() -> createAfterSignal(ready, start));
            Future<WebDriver> secondFuture = executor.submit(() -> createAfterSignal(ready, start));
            assertTrue(ready.await(30, TimeUnit.SECONDS), "both startup tasks must reach the start barrier");
            start.countDown();
            WebDriver first = firstFuture.get();
            WebDriver second = secondFuture.get();
            try {
                assertFalse(BrowserTestHarness.ownedProfile(first).equals(BrowserTestHarness.ownedProfile(second)));
                assertFalse(BrowserTestHarness.ownedChromeDriverProcessIds(first).isEmpty());
                assertFalse(BrowserTestHarness.ownedChromeDriverProcessIds(second).isEmpty());
                first.get("data:text/html,<title>concurrent-a</title>");
                second.get("data:text/html,<title>concurrent-b</title>");
                assertEquals("concurrent-a", first.getTitle());
                assertEquals("concurrent-b", second.getTitle());
            } finally {
                first.quit();
                second.quit();
            }
        } finally {
            start.countDown();
            executor.shutdownNow();
        }

        assertEquals(List.of(), BrowserTestHarness.aliveOwnedProcessIds());
        assertEquals(List.of(), BrowserTestHarness.remainingOwnedProfiles());
    }

    @Test
    void sequentialChromeSessionsLeaveNoOwnedProcessesOrProfiles() throws Exception {
        assumeTrue(BrowserTestHarness.browserName().equals("chrome"));
        Set<Path> scopedBefore = globalScopedDirectories();
        int diagnosticsBefore = BrowserTestHarness.cleanupDiagnostics().size();

        for (int session = 0; session < 3; session++) {
            WebDriver driver = null;
            try {
                driver = BrowserTestHarness.createDriver();
                driver.get("data:text/html,<title>owned-profile-" + session + "</title>");
                assertFalse(driver.getTitle().isBlank());
                assertTrue(BrowserTestHarness.runRoot().startsWith(
                        Path.of("target", "browser-profiles").toAbsolutePath().normalize()));
            } finally {
                if (driver != null) driver.quit();
            }
        }

        assertEquals(Set.of(), globalScopedDirectories().stream()
                .filter(path -> !scopedBefore.contains(path)).collect(Collectors.toSet()),
                "explicit user-data-dir must not leak new ChromeDriver scoped_dir profiles");
        assertEquals(java.util.List.of(), BrowserTestHarness.aliveOwnedProcessIds());
        assertEquals(java.util.List.of(), BrowserTestHarness.remainingOwnedProfiles());
        assertEquals(diagnosticsBefore, BrowserTestHarness.cleanupDiagnostics().size(),
                BrowserTestHarness.cleanupDiagnostics().toString());
    }

    @Test
    void timeoutFailureRemainsPrimaryWhileFinallyCleansTheOwnedSession() {
        assumeTrue(BrowserTestHarness.browserName().equals("chrome"));
        TimeoutException failure = assertThrows(TimeoutException.class, () -> {
            WebDriver driver = BrowserTestHarness.createDriver();
            try {
                throw new TimeoutException("primary browser-test timeout");
            } finally {
                driver.quit();
            }
        });

        assertTrue(failure.getMessage().startsWith("primary browser-test timeout"));
        assertEquals(java.util.List.of(), BrowserTestHarness.aliveOwnedProcessIds());
        assertEquals(java.util.List.of(), BrowserTestHarness.remainingOwnedProfiles());
    }

    @Test
    void abortedChromeSetupRemovesItsPreallocatedProfile() throws Exception {
        assumeTrue(BrowserTestHarness.browserName().equals("chrome"));
        Set<Path> scopedBefore = globalScopedDirectories();
        String previousBinary = System.getProperty("test.chrome.binary");
        try {
            System.setProperty("test.chrome.binary",
                    BrowserTestHarness.runRoot().resolve("missing-chrome.exe").toString());
            assertThrows(RuntimeException.class, BrowserTestHarness::createDriver);
        } finally {
            if (previousBinary == null) System.clearProperty("test.chrome.binary");
            else System.setProperty("test.chrome.binary", previousBinary);
        }

        assertEquals(Set.of(), globalScopedDirectories().stream()
                .filter(path -> !scopedBefore.contains(path)).collect(Collectors.toSet()));
        assertEquals(java.util.List.of(), BrowserTestHarness.aliveOwnedProcessIds());
        assertEquals(java.util.List.of(), BrowserTestHarness.remainingOwnedProfiles());
    }

    private static Set<Path> globalScopedDirectories() throws IOException {
        Path temp = Path.of(System.getProperty("java.io.tmpdir"));
        if (!Files.isDirectory(temp)) return Set.of();
        try (var paths = Files.list(temp)) {
            return paths.filter(Files::isDirectory)
                    .filter(path -> path.getFileName().toString().startsWith("scoped_dir"))
                    .map(path -> path.toAbsolutePath().normalize())
                    .collect(Collectors.toSet());
        }
    }

    private static WebDriver createAfterSignal(CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        start.await();
        return BrowserTestHarness.createDriver();
    }

    private static boolean isAlive(long pid) {
        return ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
    }
}
