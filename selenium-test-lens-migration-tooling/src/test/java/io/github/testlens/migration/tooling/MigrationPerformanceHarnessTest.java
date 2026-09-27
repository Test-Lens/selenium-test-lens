package io.github.testlens.migration.tooling;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class MigrationPerformanceHarnessTest {
    @TempDir Path temp;

    @Test
    void measuresHundredThousandAndTenThousandDirtyEntries() throws Exception {
        Path repository = temp.resolve("repo");
        Files.createDirectory(repository);
        git(repository, "init");
        git(repository, "config", "user.email", "perf@example.invalid");
        git(repository, "config", "user.name", "Perf");
        Files.writeString(repository.resolve("base"), "x");
        git(repository, "add", "--", "base");
        git(repository, "commit", "-m", "base");
        MigrationGitService service = new MigrationGitService();
        SafeGit safeGit = new SafeGit("git");
        int previous = 0;
        for (int count : new int[]{100, 1_000, 10_000}) {
            for (int i = previous; i < count; i++) {
                Files.writeString(repository.resolve("f" + String.format("%05d", i) + ".txt"),
                        "value-" + i, StandardCharsets.UTF_8);
            }
            previous = count;
            long memoryBefore = used();

            var inspected = service.inspect(repository);
            var status = safeGit.execute(SafeGit.Operation.STATUS, repository, null);
            long parseStart = System.nanoTime();
            new GitPorcelainV2Parser().parse(status.stdout());
            long parseEnd = System.nanoTime();
            new SourceStateFingerprinter(safeGit).fingerprint(inspected.preflight(), inspected.localContext());
            long hashEnd = System.nanoTime();
            var checkpoint = MigrationCheckpoint.create(inspected.preflight(), inspected.sourceState(),
                    MigrationCheckpoint.Configuration.create(
                            MigrationIsolationPlan.Choice.CREATE_DEDICATED_WORKTREE,
                            MigrationCheckpoint.IntendedMode.PREFLIGHT_ONLY,
                            MigrationCheckpoint.StateStorage.EXTERNAL_TOOL_ROOT),
                    List.of(), null, null);
            long checkpointEnd = System.nanoTime();
            var codec = new MigrationCheckpointJson();
            byte[] json = codec.write(checkpoint);
            long writeEnd = System.nanoTime();
            var decoded = codec.read(json);
            long readEnd = System.nanoTime();
            new MigrationResumeValidator().validate(decoded, inspected.preflight(), inspected.sourceState(), List.of());
            long resumeEnd = System.nanoTime();

            System.out.printf("MIGRATION_PERF entries=%d gitStatusMs=%.1f parseMs=%.1f sourceHashMs=%.1f "
                            + "checkpointMs=%.1f jsonWriteMs=%.1f jsonReadMs=%.1f resumeMs=%.1f "
                            + "memoryDelta=%d checkpointBytes=%d%n",
                    count, status.duration().toNanos() / 1e6, (parseEnd - parseStart) / 1e6,
                    (hashEnd - parseEnd) / 1e6, (checkpointEnd - hashEnd) / 1e6,
                    (writeEnd - checkpointEnd) / 1e6, (readEnd - writeEnd) / 1e6,
                    (resumeEnd - readEnd) / 1e6, Math.max(0, used() - memoryBefore), json.length);
        }
    }

    private static long used() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private static void git(Path directory, String... arguments) throws Exception {
        var command = new java.util.ArrayList<String>();
        command.add("git");
        command.addAll(List.of(arguments));
        Process process = new ProcessBuilder(command).directory(directory.toFile()).redirectErrorStream(true).start();
        process.getInputStream().transferTo(java.io.OutputStream.nullOutputStream());
        if (process.waitFor() != 0) throw new AssertionError("git failed");
    }
}
