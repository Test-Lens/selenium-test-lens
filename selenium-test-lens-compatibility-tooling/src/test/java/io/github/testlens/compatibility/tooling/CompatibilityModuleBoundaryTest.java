package io.github.testlens.compatibility.tooling;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompatibilityModuleBoundaryTest {
    @Test void productionDependenciesAreIsolatedFromRuntimeAndSelectorModules()throws Exception {
        Path root=Path.of(System.getProperty("maven.multiModuleProjectDirectory","..")).toAbsolutePath().normalize();
        String engine=Files.readString(root.resolve("selenium-test-lens-compatibility-engine/pom.xml"));
        assertFalse(engine.contains("selenium-api"));assertFalse(engine.contains("jackson"));assertFalse(engine.contains("javaparser"));assertFalse(engine.contains("selector-"));
        assertTrue(engine.contains("maven.deploy.skip"));
        String tooling=Files.readString(root.resolve("selenium-test-lens-compatibility-tooling/pom.xml"));
        assertTrue(tooling.contains("selenium-test-lens-compatibility-engine"));assertTrue(tooling.contains("jackson-core"));assertTrue(tooling.contains("selenium-api"));
        assertFalse(tooling.contains("jackson-databind"));assertFalse(tooling.contains("javaparser"));assertFalse(tooling.contains("selector-"));assertTrue(tooling.contains("maven.deploy.skip"));
        for(String module:List.of("selenium-test-lens-core","selenium-test-lens-overlay","selenium-test-lens-selenium","selenium-test-lens-junit5","selenium-test-lens-testng","selenium-test-lens-allure","selenium-test-lens-react","selenium-test-lens-selector-engine","selenium-test-lens-selector-live","selenium-test-lens-selector-lab","selenium-test-lens-selector-tooling")){
            String pom=Files.readString(root.resolve(module).resolve("pom.xml"));
            assertFalse(pom.contains("selenium-test-lens-compatibility-"),module+" must not acquire compatibility capture");
        }
    }

    @Test void ordinaryRuntimeSourcesContainNoCaptureHook()throws Exception {
        Path root=Path.of(System.getProperty("maven.multiModuleProjectDirectory","..")).toAbsolutePath().normalize();
        for(String module:List.of("selenium-test-lens-core","selenium-test-lens-selenium","selenium-test-lens-junit5","selenium-test-lens-testng")){
            Path sources=root.resolve(module).resolve("src/main");
            try(var files=Files.walk(sources)){
                for(Path file:files.filter(Files::isRegularFile).toList()){
                    String content=Files.readString(file);
                    assertFalse(content.contains("SeleniumCompatibilityCapture"),file.toString());
                    assertFalse(content.contains("CompatibilityManifestBuilder"),file.toString());
                }
            }
        }
    }
}
