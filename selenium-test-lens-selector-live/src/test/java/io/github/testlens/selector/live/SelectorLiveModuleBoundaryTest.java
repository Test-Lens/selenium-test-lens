package io.github.testlens.selector.live;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SelectorLiveModuleBoundaryTest {
    @Test void liveIsNonPublishedAndHasNoOfflineParserOrJsonDependencies() throws Exception {
        Path root=Path.of(System.getProperty("maven.multiModuleProjectDirectory","..")).toAbsolutePath().normalize();
        String pom=Files.readString(root.resolve("selenium-test-lens-selector-live/pom.xml"));
        assertTrue(pom.contains("<maven.deploy.skip>true</maven.deploy.skip>"));
        assertTrue(pom.contains("selenium-test-lens-selector-engine"));assertTrue(pom.contains("selenium-test-lens</artifactId>"));
        assertFalse(pom.contains("javaparser"));assertFalse(pom.contains("jackson"));
        String runtime=Files.readString(root.resolve("selenium-test-lens-selenium/pom.xml"));
        assertFalse(runtime.contains("selenium-test-lens-selector-engine"));assertFalse(runtime.contains("selenium-test-lens-selector-live"));
    }
}
