package io.github.testlens.studio;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class StudioFrontendContractTest {
    @Test void frontendConsumesCanonicalStudioProjections() throws Exception {
        Path repository=Path.of("..").toAbsolutePath().normalize();
        Process process=new ProcessBuilder("node",repository.resolve("scripts/test-test-engineering-studio.mjs").toString())
                .directory(repository.toFile()).redirectErrorStream(true).start();
        assertTrue(process.waitFor(Duration.ofSeconds(20).toMillis(),java.util.concurrent.TimeUnit.MILLISECONDS),"frontend contract timed out");
        String output=new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0,process.exitValue(),output);
        assertTrue(output.contains("Test Engineering Studio contract: PASS"),output);
    }
}
