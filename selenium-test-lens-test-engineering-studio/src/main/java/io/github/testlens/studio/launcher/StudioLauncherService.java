package io.github.testlens.studio.launcher;

import io.github.testlens.studio.StudioWorkflowGateway;
import io.github.testlens.studio.TestEngineeringStudioService;
import io.github.testlens.studio.project.ProjectDescriptor;
import io.github.testlens.studio.transport.TestEngineeringStudioServer;
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
    private final BrowserOpener browserOpener;

    public StudioLauncherService(StudioWorkflowGateway workflowGateway, Supplier<WebDriver> driverProvider) {
        this(workflowGateway, driverProvider, StudioLauncherService::browseDesktop);
    }

    StudioLauncherService(StudioWorkflowGateway workflowGateway, Supplier<WebDriver> driverProvider,
                          BrowserOpener browserOpener) {
        this.workflowGateway = workflowGateway;
        this.driverProvider = driverProvider;
        this.browserOpener = Objects.requireNonNull(browserOpener, "browserOpener");
    }

    public LaunchHandle launch(ProjectDescriptor descriptor) throws IOException {
        return launch(descriptor, LaunchOptions.defaults());
    }

    public LaunchHandle launch(ProjectDescriptor descriptor, LaunchOptions options) throws IOException {
        Objects.requireNonNull(descriptor, "descriptor");
        if(descriptor.status()!=ProjectDescriptor.Status.READY)throw new IllegalArgumentException("Project is not ready: "+descriptor.status());
        options = options == null ? LaunchOptions.defaults() : options;
        List<String> repairPrefixes = descriptor.sourceRoots().stream()
                .map(path -> relativePrefix(descriptor.projectRoot(), path)).distinct().toList();
        TestEngineeringStudioService.Configuration configuration = new TestEngineeringStudioService.Configuration(
                descriptor.projectRoot(), descriptor.sourceRoots(), descriptor.classpathEntries(),
                repairPrefixes, descriptor.applicationName());
        TestEngineeringStudioService service = new TestEngineeringStudioService(
                configuration, workflowGateway, driverProvider, descriptor.workspaceDirectory());
        service.attachProjectDescriptor(descriptor);
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
        @Override public void close() { if (closed.compareAndSet(false, true)) server.close(); }
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
