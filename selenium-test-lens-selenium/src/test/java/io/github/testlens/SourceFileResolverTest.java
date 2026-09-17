package io.github.testlens;

import io.github.testlens.core.logging.SourceLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SourceFileResolverTest {
    @TempDir Path temp;

    @Test void resolvesMavenSourceRootAndCachesResult() throws Exception {
        Path file = temp.resolve("src/test/java/example/pages/LoginPage.java");
        Files.createDirectories(file.getParent()); Files.writeString(file, "class LoginPage {}");
        SourceFileResolver resolver = new SourceFileResolver(temp, List.of());
        SourceLocation source = new SourceLocation("example.pages.LoginPage", "login", "LoginPage.java", 53);
        assertEquals(file.toAbsolutePath(), resolver.resolve(source).orElseThrow());
        assertEquals(file.toAbsolutePath(), resolver.resolve(source).orElseThrow());
        assertEquals(1, resolver.cacheSize());
    }

    @Test void resolvesMultiModuleAndCustomRootsAndReturnsEmptyWhenMissing() throws Exception {
        Path moduleFile = temp.resolve("module-a/src/main/kotlin/example/DashboardPage.kt");
        Path customFile = temp.resolve("acceptance/source/example/CheckoutPage.java");
        Files.createDirectories(moduleFile.getParent()); Files.writeString(moduleFile, "class DashboardPage");
        Files.createDirectories(customFile.getParent()); Files.writeString(customFile, "class CheckoutPage {}");
        SourceFileResolver resolver = new SourceFileResolver(temp, List.of(Path.of("acceptance/source")));
        assertEquals(moduleFile.toAbsolutePath(), resolver.resolve(
                new SourceLocation("example.DashboardPage", "open", "DashboardPage.kt", 41)).orElseThrow());
        assertEquals(customFile.toAbsolutePath(), resolver.resolve(
                new SourceLocation("example.CheckoutPage", "pay", "CheckoutPage.java", 12)).orElseThrow());
        assertTrue(resolver.resolve(new SourceLocation("example.Missing", "x", "Missing.java", 1)).isEmpty());
    }
}
