package io.github.testlens;

import io.github.testlens.hud.SourceIde;
import io.github.testlens.hud.SourceNavigationOptions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class IdeNavigationUriProviderTest {
    @Test void createsEscapedIntellijTargetForWindowsPathWithSpacesAndSpecialCharacters() {
        String target = IdeNavigationUriProvider.target(options(SourceIde.INTELLIJ),
                Path.of("D:\\Java Projects\\client #1\\Login Page.java"), 53, null).orElseThrow();
        assertTrue(target.startsWith("idea://open?file="));
        assertTrue(target.contains("Java%20Projects"));
        assertTrue(target.contains("%23"));
        assertTrue(target.endsWith("&line=53"));
    }

    @Test void createsVsCodeTargetWithEscapedFileUriPath() {
        String target = IdeNavigationUriProvider.target(options(SourceIde.VSCODE),
                Path.of("D:\\Java Projects\\Login Page.java"), 41, 7).orElseThrow();
        assertTrue(target.startsWith("vscode://file/"));
        assertTrue(target.contains("Java%20Projects"));
        assertTrue(target.endsWith(":41:7"));
    }

    @Test void customProviderRequiresTemplateAndExpandsPlaceholders() {
        assertTrue(IdeNavigationUriProvider.target(options(SourceIde.CUSTOM), Path.of("Login.java"), 3, null).isEmpty());
        SourceNavigationOptions custom = SourceNavigationOptions.builder().enabled(true).ide(SourceIde.CUSTOM)
                .customUriTemplate("myide://open?file={file}&line={line}&column={column}").build();
        String target = IdeNavigationUriProvider.target(custom, Path.of("Login.java"), 3, 2).orElseThrow();
        assertTrue(target.startsWith("myide://open?file="));
        assertTrue(target.endsWith("&line=3&column=2"));
    }

    private static SourceNavigationOptions options(SourceIde ide) {
        return SourceNavigationOptions.builder().enabled(true).ide(ide).build();
    }
}
