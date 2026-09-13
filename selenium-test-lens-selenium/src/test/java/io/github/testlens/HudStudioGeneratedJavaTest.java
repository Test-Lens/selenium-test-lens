package io.github.testlens;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudStudioGeneratedJavaTest {
    @TempDir Path temp;

    @Test
    void actualStudioOutputCompilesForRepresentativeConfigurations() throws Exception {
        Path script = locateScript();
        Process process;
        try {
            process = new ProcessBuilder("node", script.toString(), "--emit-generated", temp.toString()).start();
        } catch (IOException unavailable) {
            throw new AssertionError("Node.js is required for HUD Studio generator validation", unavailable);
        }
        assertTrue(process.waitFor(15, TimeUnit.SECONDS), "HUD Studio generator timed out");
        String errors = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, process.exitValue(), errors);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertTrue(compiler != null, "A JDK compiler is required");
        for (String name : List.of("compact", "compact-color-opacity", "debug-pipeline-override",
                "position-resize", "company-logo", "minimal", "standard", "typography-overrides", "header-inline",
                "scrollbar-custom")) {
            String fragment = Files.readString(temp.resolve(name + ".javafrag"), StandardCharsets.UTF_8);
            StringBuilder imports = new StringBuilder();
            StringBuilder body = new StringBuilder();
            for (String line : fragment.lines().toList()) {
                if (line.startsWith("import ")) imports.append(line).append('\n');
                else body.append(line).append('\n');
            }
            String typeName = "Generated" + name.replace("-", "");
            Path source = temp.resolve(typeName + ".java");
            Files.writeString(source, imports + "import org.openqa.selenium.WebDriver;\nclass " + typeName
                    + " { void configure(WebDriver driver) {\n" + body + "}\n}\n", StandardCharsets.UTF_8);
            int result = compiler.run(null, null, null, "-proc:none", "-classpath", System.getProperty("java.class.path"),
                    "-d", temp.toString(), source.toString());
            assertEquals(0, result, "Generated Studio configuration did not compile: " + name);
        }
    }

    private static Path locateScript() {
        Path direct = Path.of("scripts", "test-hud-studio.mjs").toAbsolutePath().normalize();
        if (Files.isRegularFile(direct)) return direct;
        return Path.of("..", "scripts", "test-hud-studio.mjs").toAbsolutePath().normalize();
    }
}
