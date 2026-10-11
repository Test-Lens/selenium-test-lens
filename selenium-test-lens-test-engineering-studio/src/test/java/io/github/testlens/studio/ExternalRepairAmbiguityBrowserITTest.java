package io.github.testlens.studio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalRepairAmbiguityBrowserITTest {
    @TempDir Path temporaryDirectory;

    @Test void resolvesPlatformExecutableUnderJavaHomeContainingSpaces() throws Exception {
        Path javaHome=temporaryDirectory.resolve("jdk with spaces");
        Path bin=Files.createDirectories(javaHome.resolve("bin"));
        Path windows=Files.createFile(bin.resolve("java.exe"));
        Path unix=Files.createFile(bin.resolve("java"));

        assertEquals(windows.toAbsolutePath().normalize(),
                ExternalRepairAmbiguityBrowserIT.javaExecutable(javaHome,"Windows 11"));
        assertEquals(unix.toAbsolutePath().normalize(),
                ExternalRepairAmbiguityBrowserIT.javaExecutable(javaHome,"Linux"));
        assertEquals(unix.toAbsolutePath().normalize(),
                ExternalRepairAmbiguityBrowserIT.javaExecutable(javaHome,"Mac OS X"));
        assertEquals(unix.toAbsolutePath().normalize(),
                ExternalRepairAmbiguityBrowserIT.javaExecutable(javaHome,"Darwin"));
    }

    @Test void rejectsMissingPlatformExecutableWithReadableDiagnostic() {
        IllegalStateException failure=assertThrows(IllegalStateException.class,()->
                ExternalRepairAmbiguityBrowserIT.javaExecutable(temporaryDirectory.resolve("missing jdk"),"Linux"));

        assertTrue(failure.getMessage().startsWith("Java executable does not exist: "));
        assertTrue(failure.getMessage().endsWith(Path.of("bin","java").toString()));
    }
}
