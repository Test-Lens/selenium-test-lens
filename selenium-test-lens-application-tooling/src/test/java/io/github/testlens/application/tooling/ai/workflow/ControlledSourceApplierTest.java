package io.github.testlens.application.tooling.ai.workflow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ControlledSourceApplierTest {
    @TempDir Path workspace;

    @Test
    void requiresTrustedApplyAllowedPathAndMatchingFingerprint() throws Exception {
        Path source = workspace.resolve("src/test/java/LoginTest.java");
        Files.createDirectories(source.getParent());
        Files.writeString(source, "class LoginTest { }\n");
        String fingerprint = ArtifactEnvelope.digest(Files.readString(source));
        ControlledSourceApplier applier = new ControlledSourceApplier();

        assertEquals(ControlledSourceApplier.Status.TRUST_REQUIRED,
                applier.apply(workspace, request("src/test/java/LoginTest.java", fingerprint, false,
                        List.of("src/test/java"))).status());
        assertEquals(ControlledSourceApplier.Status.PATH_BLOCKED,
                applier.apply(workspace, request("src/test/java/LoginTest.java", fingerprint, true,
                        List.of("src/main/java"))).status());
        assertEquals(ControlledSourceApplier.Status.SOURCE_PRECONDITION_FAILED,
                applier.apply(workspace, request("src/test/java/LoginTest.java", "sha256:stale", true,
                        List.of("src/test/java"))).status());

        var applied = applier.apply(workspace, request("src/test/java/LoginTest.java", fingerprint, true,
                List.of("src/test/java")));
        assertEquals(ControlledSourceApplier.Status.APPLIED, applied.status());
        assertEquals("class LoginTest { void invalidPassword() {} }\n", Files.readString(source));
    }

    private static ControlledSourceApplier.ApplyRequest request(String path, String fingerprint, boolean trusted,
                                                                 List<String> allowedPrefixes) {
        return new ControlledSourceApplier.ApplyRequest(Path.of(path), fingerprint,
                "class LoginTest { void invalidPassword() {} }\n", allowedPrefixes, trusted);
    }
}
