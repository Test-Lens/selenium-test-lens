package io.github.testlens.studio.maven;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class StudioMojoMetadataTest {

    @Test
    void generatedPluginDescriptorDeclaresStudioGoalAndTestClasspathResolution() throws Exception {
        try (InputStream descriptor = StudioMojo.class.getResourceAsStream("/META-INF/maven/plugin.xml")) {
            assertNotNull(descriptor, "Maven plugin descriptor should be generated before tests");
            String xml = new String(descriptor.readAllBytes(), StandardCharsets.UTF_8);

            assertTrue(xml.contains("<goalPrefix>test-lens</goalPrefix>"));
            assertTrue(xml.contains("<goal>studio</goal>"));
            assertTrue(xml.contains("<requiresDependencyResolution>test</requiresDependencyResolution>"));
            assertTrue(xml.contains("<threadSafe>false</threadSafe>"));
            assertTrue(xml.contains("<name>project</name>"));
            assertTrue(xml.contains("<name>openBrowser</name>"));
        }
    }
}
