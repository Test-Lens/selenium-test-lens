package io.github.testlens.selector.tooling;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static io.github.testlens.selector.tooling.SelectorIndexModel.ScanRequest;

class SelectorToolingPerformanceHarnessTest {
    @TempDir Path temporary;

    @Test
    void recordsOfflineSmallMediumAndLargeScanMetrics() throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("selector.tooling.perf"),
                "opt-in offline selector-tooling performance harness");
        Path output = Path.of(System.getProperty("selector.tooling.perf.output",
                "target/selector-tooling-performance/scan.csv")).toAbsolutePath().normalize();
        Files.createDirectories(output.getParent());
        List<String> rows = new ArrayList<>();
        rows.add("profile,files,declarations,totalNanos,indexBytes,heapDeltaBytes");
        run("small", 20, rows);
        run("medium", 250, rows);
        run("large", 1_000, rows);
        Files.write(output, rows, StandardCharsets.UTF_8);
    }

    private void run(String profile, int count, List<String> rows) throws Exception {
        Path root = Files.createDirectory(temporary.resolve(profile));
        Path sourceRoot = Files.createDirectories(root.resolve("module/src/main/java"));
        for (int i = 0; i < count; i++) {
            Path file = sourceRoot.resolve("fixture/Page" + i + ".java");
            Files.createDirectories(file.getParent());
            Files.writeString(file, """
                    package fixture;
                    import org.openqa.selenium.By;
                    class Page%d {
                        static final By SAVE = By.id("save-%d");
                        By row(String id) { return By.cssSelector("#row-" + id); }
                    }
                    """.formatted(i, i));
        }
        Runtime runtime = Runtime.getRuntime();
        long beforeHeap = runtime.totalMemory() - runtime.freeMemory();
        long started = System.nanoTime();
        var index = new JavaLocatorScanner().scan(new ScanRequest(root, List.of(sourceRoot), List.of()));
        long elapsed = System.nanoTime() - started;
        long afterHeap = runtime.totalMemory() - runtime.freeMemory();
        int bytes = SelectorIndexJson.serialize(index).getBytes(StandardCharsets.UTF_8).length;
        rows.add(String.join(",", profile, Integer.toString(index.coverage().filesParsed()),
                Integer.toString(index.coverage().declarationsFound()), Long.toString(elapsed),
                Integer.toString(bytes), Long.toString(Math.max(0, afterHeap - beforeHeap))));
    }
}
