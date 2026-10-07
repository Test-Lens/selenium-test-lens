package io.github.testlens.studio.launcher;

import io.github.testlens.studio.StudioWorkflowGateway;
import io.github.testlens.studio.TestEngineeringStudioService;
import io.github.testlens.studio.project.ProjectDescriptor;
import io.github.testlens.studio.projection.StudioProjections.Capability;
import io.github.testlens.studio.transport.TestEngineeringStudioServer;
import io.github.testlens.studio.browser.BrowserSessionProvider;
import org.openqa.selenium.WebDriver;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/** Explicit launcher boundary for the local Studio server. It performs no scan, map, workflow or repair action. @since 0.5.0 */
public final class StudioLauncherService {
    private final StudioWorkflowGateway workflowGateway;
    private final Supplier<WebDriver> driverProvider;
    private final BrowserSessionProvider browserSessionProvider;
    private final BrowserOpener browserOpener;

    public StudioLauncherService(StudioWorkflowGateway workflowGateway, Supplier<WebDriver> driverProvider) {
        this(workflowGateway, driverProvider, StudioLauncherService::browseDesktop);
    }

    /** Creates a launcher with the explicit S15 browser lifecycle boundary. @since 0.5.0 */
    public static StudioLauncherService withBrowserProvider(StudioWorkflowGateway workflowGateway,
                                                             BrowserSessionProvider browserProvider) {
        return new StudioLauncherService(workflowGateway, browserProvider, StudioLauncherService::browseDesktop);
    }

    StudioLauncherService(StudioWorkflowGateway workflowGateway, Supplier<WebDriver> driverProvider,
                          BrowserOpener browserOpener) {
        this.workflowGateway = workflowGateway;
        this.driverProvider = driverProvider;
        this.browserSessionProvider = null;
        this.browserOpener = Objects.requireNonNull(browserOpener, "browserOpener");
    }

    private StudioLauncherService(StudioWorkflowGateway workflowGateway, BrowserSessionProvider browserProvider,
                                  BrowserOpener browserOpener) {
        this.workflowGateway=workflowGateway;this.driverProvider=null;this.browserSessionProvider=browserProvider;
        this.browserOpener=Objects.requireNonNull(browserOpener,"browserOpener");
    }

    public LaunchHandle launch(ProjectDescriptor descriptor) throws IOException {
        return launch(descriptor, LaunchOptions.defaults());
    }

    public LaunchHandle launch(ProjectDescriptor descriptor, LaunchOptions options) throws IOException {
        Objects.requireNonNull(descriptor, "descriptor");
        if(descriptor.status()!=ProjectDescriptor.Status.READY)throw new IllegalArgumentException("Project is not ready: "+descriptor.status());
        options = options == null ? LaunchOptions.defaults() : options;
        List<Path> sourceOrder = new java.util.ArrayList<>(descriptor.testSourceRoots());
        sourceOrder.addAll(descriptor.mainSourceRoots());
        List<String> repairPrefixes = sourceOrder.stream()
                .map(path -> relativePrefix(descriptor.projectRoot(), path)).distinct().toList();
        TestEngineeringStudioService.Configuration configuration = new TestEngineeringStudioService.Configuration(
                descriptor.projectRoot(), descriptor.sourceRoots(), descriptor.classpathEntries(),
                repairPrefixes, descriptor.applicationName());
        TestEngineeringStudioService service = browserSessionProvider == null
                ? new TestEngineeringStudioService(configuration, workflowGateway, driverProvider, descriptor.workspaceDirectory())
                : new TestEngineeringStudioService(configuration, workflowGateway, browserSessionProvider, descriptor.workspaceDirectory());
        service.attachProjectDescriptor(descriptor);
        // Preserve the programmatic S13/S14 launcher contract. The S15 consumer facade
        // replaces these optimistic legacy capabilities with provider preflight results.
        if (browserSessionProvider == null) {
            service.attachCapabilities(
                    new Capability(driverProvider == null ? "NOT_AVAILABLE" : "AVAILABLE",
                            driverProvider == null ? "Browser provider is not configured" : "Programmatic browser provider configured"),
                    new Capability(workflowGateway == null ? "NOT_AVAILABLE" : "AVAILABLE",
                            workflowGateway == null ? "Agent workflow is not configured" : "Programmatic workflow gateway configured"),
                    new Capability(
                            javax.tools.ToolProvider.getSystemJavaCompiler() == null ? "NOT_AVAILABLE" : "AVAILABLE",
                            javax.tools.ToolProvider.getSystemJavaCompiler() == null ? "A JDK compiler is required" : "JDK compiler available"));
        }
        TestEngineeringStudioServer server = new TestEngineeringStudioServer(service);
        try {
            server.start();
            URI uri = server.uri();
            boolean opened = false;
            if (options.openBrowser()) {
                try { opened = browserOpener.open(uri); }
                catch (RuntimeException ignored) { opened = false; }
            }
            return new LaunchHandle(descriptor, uri, service, server, opened);
        } catch (RuntimeException failure) {
            server.close();
            throw failure;
        }
    }

    public record LaunchOptions(boolean openBrowser) {
        public static LaunchOptions defaults() { return new LaunchOptions(true); }
    }

    /** Running local Studio handle. Closing it only stops Studio; caller-owned WebDriver remains caller-owned. */
    public static final class LaunchHandle implements AutoCloseable {
        private final ProjectDescriptor descriptor;
        private final URI uri;
        private final TestEngineeringStudioService service;
        private final TestEngineeringStudioServer server;
        private final boolean browserOpened;
        private final AtomicBoolean closed = new AtomicBoolean();

        private LaunchHandle(ProjectDescriptor descriptor, URI uri, TestEngineeringStudioService service,
                             TestEngineeringStudioServer server, boolean browserOpened) {
            this.descriptor = descriptor;
            this.uri = uri;
            this.service = service;
            this.server = server;
            this.browserOpened = browserOpened;
        }

        public ProjectDescriptor descriptor() { return descriptor; }
        public URI uri() { return uri; }
        public TestEngineeringStudioService service() { return service; }
        public boolean browserOpened() { return browserOpened; }
        @Override public void close() { if (closed.compareAndSet(false, true)) { server.close(); service.close(); } }
    }

    @FunctionalInterface
    interface BrowserOpener { boolean open(URI uri); }

    private static boolean browseDesktop(URI uri) {
        if (!Desktop.isDesktopSupported()) return false;
        Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(Desktop.Action.BROWSE)) return false;
        try { desktop.browse(uri); return true; }
        catch (IOException | UnsupportedOperationException | SecurityException ignored) { return false; }
    }

    private static String relativePrefix(Path root, Path source) {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path normalizedSource = source.toAbsolutePath().normalize();
        if (!normalizedSource.startsWith(normalizedRoot)) {
            throw new IllegalArgumentException("Source root escapes the project root");
        }
        String value = normalizedRoot.relativize(normalizedSource).toString().replace('\\', '/');
        return value.isBlank() ? "." : value;
    }
}
