package io.github.testlens.browser;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.CommandPayload;
import org.openqa.selenium.remote.Response;
import org.openqa.selenium.json.Json;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/** Owns real-browser processes and Chrome profiles created by this test JVM. */
final class BrowserTestHarness {
    private static final Duration GRACEFUL_PROCESS_WAIT = Duration.ofSeconds(4);
    private static final Duration TERMINATE_PROCESS_WAIT = Duration.ofSeconds(2);
    private static final Duration FORCE_PROCESS_WAIT = Duration.ofSeconds(2);
    private static final int DELETE_ATTEMPTS = isWindows() ? 12 : 3;
    private static final Path RUN_ROOT = Path.of(System.getProperty("test.browser.profileRoot",
                    Path.of("target", "browser-profiles").toString()))
            .toAbsolutePath().normalize()
            .resolve("run-" + ProcessHandle.current().pid() + "-" + UUID.randomUUID().toString().substring(0, 8));
    private static final AtomicInteger SESSION_SEQUENCE = new AtomicInteger();
    private static final ThreadLocal<ExecutorCommandMetrics> CONSTRUCTING_METRICS = new ThreadLocal<>();
    private static final Object CHROME_STARTUP_LOCK = new Object();
    private static final Set<OwnedChrome> ACTIVE_CHROME = ConcurrentHashMap.newKeySet();
    private static final Set<ProcessIdentity> OBSERVED_OWNED_PROCESSES = ConcurrentHashMap.newKeySet();
    private static final CopyOnWriteArrayList<String> CLEANUP_DIAGNOSTICS = new CopyOnWriteArrayList<>();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(BrowserTestHarness::closeRemainingChrome,
                "test-lens-browser-cleanup"));
    }

    private BrowserTestHarness() {}

    static WebDriver createDriver() {
        return createDriver(PageLoadStrategy.NORMAL);
    }

    static WebDriver createDriver(ExecutorCommandMetrics metrics) {
        return createDriver(PageLoadStrategy.NORMAL, false, metrics);
    }

    static WebDriver createDriver(PageLoadStrategy pageLoadStrategy) {
        return createDriver(pageLoadStrategy, false);
    }

    static WebDriver createBiDiDriver() {
        return createDriver(PageLoadStrategy.NORMAL, true);
    }

    private static WebDriver createDriver(PageLoadStrategy pageLoadStrategy, boolean bidi) {
        return createDriver(pageLoadStrategy, bidi, null);
    }

    private static WebDriver createDriver(PageLoadStrategy pageLoadStrategy, boolean bidi,
                                          ExecutorCommandMetrics metrics) {
        boolean headed = Boolean.parseBoolean(System.getProperty("headed", "false"));
        CONSTRUCTING_METRICS.set(metrics);
        try {
        return switch (browserName()) {
            case "chrome" -> createChrome(pageLoadStrategy, bidi, headed, metrics);
            case "firefox" -> createFirefox(pageLoadStrategy, bidi, headed, metrics);
            default -> throw new IllegalArgumentException(
                    "Unsupported -Dbrowser=" + browserName() + "; expected chrome or firefox");
        };
        } finally {
            CONSTRUCTING_METRICS.remove();
        }
    }

    private static WebDriver createChrome(PageLoadStrategy pageLoadStrategy, boolean bidi, boolean headed,
                                          ExecutorCommandMetrics metrics) {
        synchronized (CHROME_STARTUP_LOCK) {
            Set<ProcessIdentity> startupBaseline = currentDescendantIdentities();
            Path profile = createOwnedProfile();
            OwnedChrome owner = new OwnedChrome(profile);
            ACTIVE_CHROME.add(owner);
            try {
                ChromeOptions options = bidi ? new ChromeOptions().enableBiDi() : new ChromeOptions();
                options.setPageLoadStrategy(pageLoadStrategy);
                String binary = System.getProperty("test.chrome.binary", "").trim();
                if (!binary.isEmpty()) options.setBinary(binary);
                options.addArguments("--user-data-dir=" + profile,
                        "--window-size=1280,900", "--disable-dev-shm-usage", "--no-sandbox");
                if (!headed) options.addArguments("--headless=new");
                WebDriver driver = metrics == null ? new ChromeDriver(options) : new MeasuredChromeDriver(options, metrics);
                owner.discoverOwnedProcesses();
                owner.rememberChromeDriversStartedAfter(startupBaseline);
                return owner.attach(driver);
            } catch (RuntimeException | Error startupFailure) {
                owner.cleanupAfterFailedStart(startupBaseline);
                throw startupFailure;
            }
        }
    }

    private static WebDriver createFirefox(PageLoadStrategy pageLoadStrategy, boolean bidi, boolean headed,
                                           ExecutorCommandMetrics metrics) {
        FirefoxOptions options = bidi ? new FirefoxOptions().enableBiDi() : new FirefoxOptions();
        options.setPageLoadStrategy(pageLoadStrategy);
        String binary = System.getProperty("test.firefox.binary", "").trim();
        if (!binary.isEmpty()) options.setBinary(binary);
        if (!headed) options.addArguments("-headless");
        WebDriver firefox = null;
        try {
            firefox = metrics == null ? new FirefoxDriver(options) : new MeasuredFirefoxDriver(options, metrics);
            firefox.manage().window().setSize(new org.openqa.selenium.Dimension(1280, 900));
            return firefox;
        } catch (RuntimeException | Error startupFailure) {
            if (firefox != null) {
                try {
                    firefox.quit();
                } catch (RuntimeException cleanupFailure) {
                    startupFailure.addSuppressed(cleanupFailure);
                }
            }
            throw startupFailure;
        }
    }

    static String browserName() {
        return System.getProperty("browser", "chrome").trim().toLowerCase(Locale.ROOT);
    }

    static final class ExecutorCommandMetrics {
        private final ConcurrentHashMap<String, CommandMetric> commands = new ConcurrentHashMap<>();
        private final LongAdder hudBatches = new LongAdder();
        private final LongAdder hudEvents = new LongAdder();
        private final LongAdder hudPayloadBytes = new LongAdder();

        void record(CommandPayload payload, long elapsedNanos) {
            String command = payload.getName();
            CommandMetric metric = commands.computeIfAbsent(command, ignored -> new CommandMetric());
            metric.count.increment();
            metric.elapsedNanos.add(Math.max(0L, elapsedNanos));
            recordHudBatch(payload);
        }

        void reset() {
            commands.clear();
            hudBatches.reset();
            hudEvents.reset();
            hudPayloadBytes.reset();
        }

        int total() { return commands.values().stream().mapToInt(value -> value.count.intValue()).sum(); }

        int count(String command) {
            CommandMetric value = commands.get(command);
            return value == null ? 0 : value.count.intValue();
        }

        java.util.Map<String, CommandMeasurement> snapshot() {
            java.util.Map<String, CommandMeasurement> result = new java.util.TreeMap<>();
            commands.forEach((name, value) -> result.put(name,
                    new CommandMeasurement(value.count.intValue(), value.elapsedNanos.sum())));
            return result;
        }

        HudTransportMeasurement hudTransportSnapshot() {
            return new HudTransportMeasurement(hudBatches.intValue(), hudEvents.intValue(), hudPayloadBytes.sum());
        }

        private void recordHudBatch(CommandPayload payload) {
            if (!"executeScript".equals(payload.getName())) return;
            Object script = payload.getParameters().get("script");
            if (!(script instanceof String value) || !value.contains("test-lens:hud-semantic-batch")) return;
            Object arguments = payload.getParameters().get("args");
            Object batch = arguments instanceof List<?> values && !values.isEmpty() ? values.get(0) : List.of();
            int events = batch instanceof List<?> values ? values.size() : 0;
            hudBatches.increment();
            hudEvents.add(events);
            hudPayloadBytes.add(new Json().toJson(batch).getBytes(StandardCharsets.UTF_8).length);
        }

        record CommandMeasurement(int count, long elapsedNanos) {
            CommandMeasurement minus(CommandMeasurement before) {
                CommandMeasurement baseline = before == null ? new CommandMeasurement(0, 0L) : before;
                return new CommandMeasurement(Math.max(0, count - baseline.count),
                        Math.max(0L, elapsedNanos - baseline.elapsedNanos));
            }
        }

        record HudTransportMeasurement(int batches, int events, long payloadBytes) {
            HudTransportMeasurement minus(HudTransportMeasurement before) {
                HudTransportMeasurement baseline = before == null ? new HudTransportMeasurement(0, 0, 0L) : before;
                return new HudTransportMeasurement(Math.max(0, batches - baseline.batches),
                        Math.max(0, events - baseline.events), Math.max(0L, payloadBytes - baseline.payloadBytes));
            }
        }

        private static final class CommandMetric {
            private final LongAdder count = new LongAdder();
            private final LongAdder elapsedNanos = new LongAdder();
        }
    }

    private static final class MeasuredChromeDriver extends ChromeDriver {
        private ExecutorCommandMetrics metrics;

        private MeasuredChromeDriver(ChromeOptions options, ExecutorCommandMetrics metrics) {
            super(options);
            this.metrics = metrics;
        }

        @Override protected Response execute(CommandPayload payload) {
            ExecutorCommandMetrics target = metrics == null ? CONSTRUCTING_METRICS.get() : metrics;
            long started = System.nanoTime();
            try {
                return super.execute(payload);
            } finally {
                if (target != null) target.record(payload, System.nanoTime() - started);
            }
        }
    }

    private static final class MeasuredFirefoxDriver extends FirefoxDriver {
        private ExecutorCommandMetrics metrics;

        private MeasuredFirefoxDriver(FirefoxOptions options, ExecutorCommandMetrics metrics) {
            super(options);
            this.metrics = metrics;
        }

        @Override protected Response execute(CommandPayload payload) {
            ExecutorCommandMetrics target = metrics == null ? CONSTRUCTING_METRICS.get() : metrics;
            long started = System.nanoTime();
            try {
                return super.execute(payload);
            } finally {
                if (target != null) target.record(payload, System.nanoTime() - started);
            }
        }
    }

    static Path runRoot() {
        return RUN_ROOT;
    }

    static List<Path> remainingOwnedProfiles() {
        if (!Files.isDirectory(RUN_ROOT)) return List.of();
        try (var children = Files.list(RUN_ROOT)) {
            return children.filter(Files::isDirectory).toList();
        } catch (IOException e) {
            recordDiagnostic("Could not inspect owned browser profiles", e);
            return List.of(RUN_ROOT);
        }
    }

    static List<Long> aliveOwnedProcessIds() {
        return OBSERVED_OWNED_PROCESSES.stream().map(ProcessIdentity::resolve)
                .flatMap(java.util.Optional::stream).filter(ProcessHandle::isAlive)
                .map(ProcessHandle::pid).sorted().toList();
    }

    static List<String> cleanupDiagnostics() {
        return List.copyOf(CLEANUP_DIAGNOSTICS);
    }

    private static Path createOwnedProfile() {
        try {
            Files.createDirectories(RUN_ROOT);
            Path profile = RUN_ROOT.resolve("chrome-" + SESSION_SEQUENCE.incrementAndGet());
            Files.createDirectory(profile);
            return profile;
        } catch (IOException e) {
            throw new IllegalStateException("Could not create a repo-owned Chrome profile", e);
        }
    }

    private static Set<ProcessIdentity> currentDescendantIdentities() {
        Set<ProcessIdentity> result = new LinkedHashSet<>();
        ProcessHandle.current().descendants().forEach(process -> result.add(ProcessIdentity.of(process)));
        return result;
    }

    private static Set<ProcessHandle> processesForProfile(Path profile) {
        Set<ProcessHandle> all = new LinkedHashSet<>();
        ProcessHandle.allProcesses().filter(process -> usesProfile(process, profile)).forEach(process -> {
            all.add(process);
            process.descendants().forEach(all::add);
            addChromeDriverAncestorOwnedByCurrentJvm(process, all);
        });
        return all;
    }

    static boolean profileArgumentMatches(String[] arguments, Path profile) {
        String expected = normalizedPath(profile);
        for (int index = 0; index < arguments.length; index++) {
            String argument = arguments[index];
            String value = null;
            if (argument.regionMatches(true, 0, "--user-data-dir=", 0, "--user-data-dir=".length())) {
                value = argument.substring("--user-data-dir=".length());
            } else if (argument.equalsIgnoreCase("--user-data-dir") && index + 1 < arguments.length) {
                value = arguments[++index];
            }
            if (value != null && normalizedPath(Path.of(stripQuotes(value))).equals(expected)) return true;
        }
        return false;
    }

    private static boolean usesProfile(ProcessHandle process, Path profile) {
        return process.info().arguments().map(arguments -> profileArgumentMatches(arguments, profile)).orElse(false);
    }

    private static String normalizedPath(Path path) {
        Path normalized;
        try {
            normalized = path.toRealPath();
        } catch (IOException ignored) {
            normalized = path.toAbsolutePath().normalize();
        }
        String value = normalized.toString().replace('\\', '/');
        return isWindows() ? value.toLowerCase(Locale.ROOT) : value;
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static void addChromeDriverAncestorOwnedByCurrentJvm(ProcessHandle process, Set<ProcessHandle> result) {
        List<ProcessHandle> chain = new ArrayList<>();
        ProcessHandle cursor = process;
        while (cursor.parent().isPresent()) {
            cursor = cursor.parent().orElseThrow();
            if (cursor.pid() == ProcessHandle.current().pid()) {
                chain.stream().filter(BrowserTestHarness::isChromeDriverProcess).forEach(result::add);
                return;
            }
            chain.add(cursor);
        }
    }

    private static boolean isChromeDriverProcess(ProcessHandle process) {
        String command = process.info().command().orElse("").replace('\\', '/').toLowerCase(Locale.ROOT);
        return command.endsWith("/chromedriver.exe") || command.endsWith("/chromedriver");
    }

    private static void stopOwnedProcesses(Set<ProcessHandle> processes) {
        waitForExit(processes, GRACEFUL_PROCESS_WAIT);
        List<ProcessHandle> alive = processes.stream().filter(ProcessHandle::isAlive).toList();
        alive.forEach(ProcessHandle::destroy);
        waitForExit(new LinkedHashSet<>(alive), TERMINATE_PROCESS_WAIT);
        alive = alive.stream().filter(ProcessHandle::isAlive).toList();
        alive.forEach(ProcessHandle::destroyForcibly);
        waitForExit(new LinkedHashSet<>(alive), FORCE_PROCESS_WAIT);
        List<Long> survivors = alive.stream().filter(ProcessHandle::isAlive).map(ProcessHandle::pid).toList();
        if (!survivors.isEmpty()) {
            recordDiagnostic("Owned Chrome processes remained after bounded shutdown: " + survivors, null);
        }
    }

    private static void waitForExit(Set<ProcessHandle> processes, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (processes.stream().anyMatch(ProcessHandle::isAlive) && System.nanoTime() < deadline) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                recordDiagnostic("Interrupted while waiting for owned Chrome processes", interrupted);
                return;
            }
        }
    }

    private static void deleteOwnedProfile(Path profile) {
        Path normalized = profile.toAbsolutePath().normalize();
        if (!normalized.startsWith(RUN_ROOT)) {
            recordDiagnostic("Refused to delete a browser profile outside the current run root", null);
            return;
        }
        IOException lastFailure = null;
        for (int attempt = 1; attempt <= DELETE_ATTEMPTS; attempt++) {
            try {
                deleteTree(normalized);
                deleteRunRootIfEmpty();
                return;
            } catch (IOException failure) {
                lastFailure = failure;
                if (attempt < DELETE_ATTEMPTS) {
                    try {
                        Thread.sleep(Math.min(250L, 25L * attempt));
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        recordDiagnostic("Interrupted while deleting an owned Chrome profile", interrupted);
                        return;
                    }
                }
            }
        }
        recordDiagnostic("Could not delete owned Chrome profile after " + DELETE_ATTEMPTS + " attempts", lastFailure);
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }

    private static void deleteRunRootIfEmpty() throws IOException {
        try {
            Files.deleteIfExists(RUN_ROOT);
        } catch (DirectoryNotEmptyException ignored) {
            // Other browser sessions owned by this test JVM are still active.
        }
    }

    private static void closeRemainingChrome() {
        for (OwnedChrome owner : List.copyOf(ACTIVE_CHROME)) owner.quitAndCleanup();
    }

    private static void recordDiagnostic(String message, Throwable failure) {
        String detail = failure == null ? message
                : message + ": " + failure.getClass().getSimpleName() + ": " + failure.getMessage();
        CLEANUP_DIAGNOSTICS.add(detail);
        System.err.println("[browser-harness-cleanup] " + detail);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static final class OwnedChrome implements InvocationHandler {
        private final Path profile;
        private final Set<ProcessIdentity> ownedProcesses = ConcurrentHashMap.newKeySet();
        private final Set<ProcessIdentity> chromeDriverProcesses = ConcurrentHashMap.newKeySet();
        private final AtomicBoolean closed = new AtomicBoolean();
        private volatile WebDriver delegate;

        private OwnedChrome(Path profile) {
            this.profile = profile;
        }

        private WebDriver attach(WebDriver driver) {
            delegate = driver;
            Set<Class<?>> interfaces = new LinkedHashSet<>();
            collectPublicInterfaces(driver.getClass(), interfaces);
            interfaces.add(WebDriver.class);
            interfaces.add(JavascriptExecutor.class);
            return (WebDriver) Proxy.newProxyInstance(driver.getClass().getClassLoader(),
                    interfaces.toArray(Class<?>[]::new), this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] arguments) throws Throwable {
            if (method.getName().equals("quit") && method.getParameterCount() == 0) {
                quitAndCleanup();
                return null;
            }
            try {
                return method.invoke(delegate, arguments);
            } catch (InvocationTargetException invocationFailure) {
                throw invocationFailure.getCause();
            }
        }

        private void cleanupAfterFailedStart(Set<ProcessIdentity> startupBaseline) {
            remember(processesForProfile(profile));
            rememberChromeDriversStartedAfter(startupBaseline);
            quitAndCleanup();
        }

        private void rememberChromeDriversStartedAfter(Set<ProcessIdentity> startupBaseline) {
            ProcessHandle.current().descendants()
                    .filter(BrowserTestHarness::isChromeDriverProcess)
                    .filter(process -> !startupBaseline.contains(ProcessIdentity.of(process)))
                    .forEach(process -> remember(Set.of(process)));
            rememberOwnedDescendants();
        }

        private void quitAndCleanup() {
            if (!closed.compareAndSet(false, true)) return;
            rememberOwnedDescendants();
            Set<ProcessHandle> processes = resolveOwnedProcesses();
            Throwable quitFailure = null;
            try {
                if (delegate != null) delegate.quit();
            } catch (Throwable failure) {
                quitFailure = failure;
            } finally {
                stopOwnedProcesses(processes);
                deleteOwnedProfile(profile);
                ACTIVE_CHROME.remove(this);
            }
            if (quitFailure != null) {
                recordDiagnostic("WebDriver.quit() failed for an owned Chrome session", quitFailure);
            }
        }

        private void discoverOwnedProcesses() {
            remember(processesForProfile(profile));
            rememberOwnedDescendants();
        }

        private void rememberOwnedDescendants() {
            Set<ProcessHandle> roots = resolveOwnedProcesses();
            Set<ProcessHandle> descendants = new LinkedHashSet<>();
            roots.forEach(root -> root.descendants().forEach(descendants::add));
            remember(descendants);
        }

        private void remember(Set<ProcessHandle> processes) {
            for (ProcessHandle process : processes) {
                ProcessIdentity identity = ProcessIdentity.of(process);
                ownedProcesses.add(identity);
                OBSERVED_OWNED_PROCESSES.add(identity);
                if (isChromeDriverProcess(process)) chromeDriverProcesses.add(identity);
            }
        }

        private Set<ProcessHandle> resolveOwnedProcesses() {
            Set<ProcessHandle> result = new LinkedHashSet<>();
            ownedProcesses.stream().map(ProcessIdentity::resolve)
                    .flatMap(java.util.Optional::stream).forEach(result::add);
            return result;
        }

        private static void collectPublicInterfaces(Class<?> type, Set<Class<?>> result) {
            if (type == null) return;
            for (Class<?> contract : type.getInterfaces()) collectInterface(contract, result);
            collectPublicInterfaces(type.getSuperclass(), result);
        }

        private static void collectInterface(Class<?> contract, Set<Class<?>> result) {
            if (Modifier.isPublic(contract.getModifiers())) result.add(contract);
            for (Class<?> parent : contract.getInterfaces()) collectInterface(parent, result);
        }
    }

    static Path ownedProfile(WebDriver driver) {
        return ownerOf(driver).profile;
    }

    static List<Long> ownedChromeDriverProcessIds(WebDriver driver) {
        return ownerOf(driver).chromeDriverProcesses.stream().map(ProcessIdentity::resolve)
                .flatMap(java.util.Optional::stream).filter(ProcessHandle::isAlive)
                .map(ProcessHandle::pid).sorted().toList();
    }

    private static OwnedChrome ownerOf(WebDriver driver) {
        if (!Proxy.isProxyClass(driver.getClass()) || !(Proxy.getInvocationHandler(driver) instanceof OwnedChrome owner)) {
            throw new IllegalArgumentException("WebDriver is not owned by this browser test harness");
        }
        return owner;
    }

    private record ProcessIdentity(long pid, Instant startInstant) {
        private static ProcessIdentity of(ProcessHandle process) {
            return new ProcessIdentity(process.pid(), process.info().startInstant().orElse(null));
        }

        private java.util.Optional<ProcessHandle> resolve() {
            return ProcessHandle.of(pid).filter(process -> startInstant == null
                    || process.info().startInstant().map(startInstant::equals).orElse(false));
        }
    }
}
