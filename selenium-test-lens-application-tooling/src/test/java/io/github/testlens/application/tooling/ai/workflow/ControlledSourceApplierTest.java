package io.github.testlens.application.tooling.ai.workflow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ControlledSourceApplierTest {
    @TempDir Path workspace;

    @Test void finalFingerprintCheckPreservesConcurrentUserEdit() throws Exception {
        Path target = workspace.resolve("src/test/java/example/Test.java");
        Files.createDirectories(target.getParent());
        Files.writeString(target, "original");
        String expected = ArtifactEnvelope.digest("original");
        ControlledSourceApplier applier = new ControlledSourceApplier(path -> Files.writeString(path, "user edit"));

        ControlledSourceApplier.ApplyResult result = applier.apply(workspace,
                new ControlledSourceApplier.ApplyRequest(Path.of("src/test/java/example/Test.java"), expected,
                        "replacement", List.of("src/test/java"), true));

        assertEquals(ControlledSourceApplier.Status.SOURCE_PRECONDITION_FAILED, result.status());
        assertEquals("user edit", Files.readString(target));
    }
}
