package io.github.testlens.studio.launcher;

import io.github.testlens.studio.TestEngineeringStudio;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

/** Minimal local Studio entry point. All project and workflow actions remain explicit in the UI. @since 0.5.0 */
public final class Main {
    private Main() { }

    public static void main(String[] args) throws Exception {
        Arguments arguments = Arguments.parse(args);
        TestEngineeringStudio.LaunchHandle handle = TestEngineeringStudio.launch(
                new TestEngineeringStudio.LaunchRequest(arguments.project(),java.util.List.of(),java.util.List.of(),java.util.List.of(),arguments.openBrowser()));
        Runtime.getRuntime().addShutdownHook(new Thread(handle::close, "test-lens-studio-shutdown"));
        System.out.println("Test Engineering Studio: " + handle.uri());
        new CountDownLatch(1).await();
    }

    record Arguments(Path project, boolean openBrowser) {
        static Arguments parse(String[] args) {
            Path project = Path.of(".");
            boolean open = true;
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--project" -> {
                        if (++i >= args.length || args[i].isBlank()) throw usage("--project requires a directory");
                        project = Path.of(args[i]);
                    }
                    case "--no-open" -> open = false;
                    default -> throw usage("Unknown option: " + args[i]);
                }
            }
            return new Arguments(project, open);
        }

        private static IllegalArgumentException usage(String detail) {
            return new IllegalArgumentException(detail + ". Usage: studio [--project <directory>] [--no-open]");
        }
    }
}
