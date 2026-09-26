package io.github.testlens.selector.tooling;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelectorToolingModuleBoundaryTest {
    @Test
    void parserDependencyExistsOnlyInNonPublishedToolingModule() throws Exception {
        Path root = Path.of(System.getProperty("maven.multiModuleProjectDirectory", ".."))
                .toAbsolutePath().normalize();
        String tooling = Files.readString(root.resolve("selenium-test-lens-selector-tooling/pom.xml"));
        assertTrue(tooling.contains("<groupId>io.github.testlens</groupId>"));
        assertTrue(tooling.contains("javaparser-symbol-solver-core"));
        assertTrue(tooling.contains("<version>3.28.2</version>"));
        assertTrue(tooling.contains("selenium-test-lens-selector-engine"));
        assertTrue(tooling.contains("tools.jackson.core"));
        assertTrue(tooling.contains("jackson-core"));
        assertFalse(tooling.contains("jackson-databind"));
        assertFalse(tooling.contains("jackson-annotations"));
        assertTrue(tooling.contains("<maven.deploy.skip>true</maven.deploy.skip>"));

        String engine = Files.readString(root.resolve("selenium-test-lens-selector-engine/pom.xml"));
        assertFalse(engine.contains("jackson"));
        assertFalse(engine.contains("javaparser"));
        assertFalse(engine.contains("selenium-java"));

        for (String module : List.of("selenium-test-lens-core", "selenium-test-lens-selenium",
                "selenium-test-lens-testng", "selenium-test-lens-junit5", "selenium-test-lens-overlay",
                "selenium-test-lens-react", "selenium-test-lens-allure")) {
            String pom = Files.readString(root.resolve(module).resolve("pom.xml"));
            assertFalse(pom.contains("javaparser"), module + " must not depend on JavaParser");
            assertFalse(pom.contains("selenium-test-lens-selector-tooling"),
                    module + " must not depend on selector tooling");
            assertFalse(pom.contains("selenium-test-lens-selector-engine"),
                    module + " must not depend on selector engine");
            assertFalse(pom.contains("tools.jackson.core"), module + " must not depend on Jackson 3");
        }
    }
}
