package io.github.testlens.selector.lab;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelectorLabModuleBoundaryTest {
    @Test
    void moduleHasNoToolingJacksonOrJavaParserDependency() throws Exception {
        String pom = Files.readString(Path.of("pom.xml"));
        assertFalse(pom.contains("selector-tooling"));
        assertFalse(pom.contains("jackson"));
        assertFalse(pom.contains("javaparser"));
        assertTrue(pom.contains("maven.deploy.skip"));
    }

    @Test
    void browserDispatcherCannotReachPolicyWriter() throws Exception {
        Path source = Path.of("src/main/java/io/github/testlens/selector/lab/SelectorLabSession.java");
        String java = Files.readString(source);
        assertFalse(java.contains("SelectorPolicyJson"));
        assertFalse(java.contains("SelectorPolicyDraftApplier"));
        assertFalse(java.contains("java.nio.file"));
        assertFalse(java.contains("WRITE_POLICY"));
        assertFalse(java.contains("APPLY_DRAFT"));
        String resource = Files.readString(Path.of("src/main/resources/uitestlens/runtime/selector-lab.js"));
        assertFalse(resource.contains("WRITE_POLICY"));
        assertFalse(resource.contains("APPLY_DRAFT"));
        assertFalse(resource.contains("SET_PROJECT_ROOT"));
        assertTrue(resource.contains("PREPARED · NOT SAVED · PENDING HOST APPROVAL"));
    }
}
