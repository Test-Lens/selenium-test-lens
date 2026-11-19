package io.github.testlens.selenium.network;

import consumer.LegacySizeCalls;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.BuildInfo;
import org.openqa.selenium.bidi.network.RequestData;
import org.openqa.selenium.bidi.network.ResponseData;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

class BinaryArtifactTest {
    @Test void usesFrozenLensAndOneExactSeleniumVersion() throws Exception {
        String expected = System.getProperty("expected.selenium");
        assertEquals(expected, new BuildInfo().getReleaseLabel());
        Path lens = Path.of(NetworkDiagnostics.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        assertEquals(Path.of(System.getProperty("frozen.dir"), "lens.jar").toRealPath(), lens.toRealPath());
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(lens)));
        assertEquals(System.getProperty("expected.lens.sha256").toLowerCase(), hash);
        int checked = 0;
        for (String entry : System.getProperty("java.class.path").split(java.io.File.pathSeparator)) {
            Path jar = Path.of(entry);
            if (!Files.isRegularFile(jar) || !entry.endsWith(".jar")) continue;
            // Selenium's Bazel-built JARs omit Maven pom.properties and versioned manifests.
            // Verify actual resolved Maven paths in the runtime classpath, not an unused POM property.
            if (jar.getFileName().toString().startsWith("selenium-")) {
                assertTrue(jar.getFileName().toString().endsWith("-" + expected + ".jar"), "Mixed Selenium runtime JAR: " + jar);
                assertEquals(expected, jar.toRealPath().getParent().getFileName().toString(), "Resolved Maven version: " + jar);
                checked++;
            }
        }
        assertTrue(checked >= 3, "Must verify resolved Selenium modules, not only BuildInfo");
        for (Class<?> owner : new Class<?>[]{org.openqa.selenium.WebDriver.class,
                org.openqa.selenium.bidi.BiDi.class, org.openqa.selenium.chrome.ChromeDriver.class}) {
            Path source = Path.of(owner.getProtectionDomain().getCodeSource().getLocation().toURI());
            assertTrue(source.getFileName().toString().endsWith("-" + expected + ".jar"), "Loaded Selenium class: " + owner);
        }
        System.out.println("BINARY PROVENANCE Selenium=" + expected + " Lens SHA256=" + hash + " checkedModules=" + checked);
    }

    @Test void reproducesOriginalBinaryBreakWhenDescriptorChanges() throws Exception {
        // Null receiver avoids version-specific model constructors. JVM method resolution precedes dereference.
        if (RequestData.class.getMethod("getHeadersSize").getReturnType() != Long.class) {
            assertThrows(NoSuchMethodError.class, () -> LegacySizeCalls.requestHeaders(null));
        } else assertThrows(NullPointerException.class, () -> LegacySizeCalls.requestHeaders(null));
        if (ResponseData.class.getMethod("getHeadersSize").getReturnType() != long.class) {
            assertThrows(NoSuchMethodError.class, () -> LegacySizeCalls.responseHeaders(null));
        } else assertThrows(NullPointerException.class, () -> LegacySizeCalls.responseHeaders(null));
        if (ResponseData.class.getMethod("getBodySize").getReturnType() != long.class) {
            assertThrows(NoSuchMethodError.class, () -> LegacySizeCalls.responseBody(null));
        } else assertThrows(NullPointerException.class, () -> LegacySizeCalls.responseBody(null));
    }
}
