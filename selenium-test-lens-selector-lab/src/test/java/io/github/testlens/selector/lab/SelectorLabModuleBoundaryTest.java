package io.github.testlens.selector.lab;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class SelectorLabModuleBoundaryTest {
    @Test void moduleHasNoToolingJacksonOrJavaParserDependency()throws Exception{
        String pom=Files.readString(Path.of("pom.xml"));
        assertFalse(pom.contains("selector-tooling"));assertFalse(pom.contains("jackson"));assertFalse(pom.contains("javaparser"));
        assertTrue(pom.contains("maven.deploy.skip"));
    }
}
