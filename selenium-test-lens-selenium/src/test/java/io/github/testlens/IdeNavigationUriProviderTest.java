package io.github.testlens;

import io.github.testlens.hud.SourceIde;
import io.github.testlens.hud.SourceNavigationOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class IdeNavigationUriProviderTest {
    @TempDir Path temp;

    @Test void createsExactJetBrainsTargetForNestedModuleSpacesUnicodeLineAndColumn() throws Exception {
        Path file = source("module with spaces/src/test/java/example/Żółw.java");
        SourceNavigationOptions options = SourceNavigationOptions.builder().enabled(true).ide(SourceIde.INTELLIJ)
                .intellijProject("Client Project", temp).build();

        String target = IdeNavigationUriProvider.target(options, file, 53, 7, temp).orElseThrow();

        assertEquals("jetbrains://idea/navigate/reference?project=Client%20Project&path="
                + "module%20with%20spaces%2Fsrc%2Ftest%2Fjava%2Fexample%2F%C5%BB%C3%B3%C5%82w.java%3A53%3A7",
                target);
        String drive = temp.toAbsolutePath().getRoot() == null ? "" : temp.toAbsolutePath().getRoot().toString();
        if (drive.contains(":")) {
            assertFalse(target.contains(drive.replace(":", "%3A").replace("\\", "%5C")),
                    "Windows drive path must be project-relative");
        }
    }

    @Test void createsLineOnlyTargetAndInfersProjectFromIdeaName() throws Exception {
        Files.createDirectories(temp.resolve(".idea"));
        Files.writeString(temp.resolve(".idea/.name"), "Inferred Project\n");
        Path file = source("src/main/java/example/Login.java");

        String target = IdeNavigationUriProvider.target(options(SourceIde.INTELLIJ), file, 41, null, temp)
                .orElseThrow();

        assertEquals("jetbrains://idea/navigate/reference?project=Inferred%20Project&path="
                + "src%2Fmain%2Fjava%2Fexample%2FLogin.java%3A41", target);
    }

    @Test void resolvesRelativeExplicitProjectRootAgainstExecutionRoot() throws Exception {
        Path project = Files.createDirectories(temp.resolve("checkout-project"));
        Path file = Files.createDirectories(project.resolve("src/test/java/example"))
                .resolve("Checkout.java");
        Files.writeString(file, "class Checkout {}");
        SourceNavigationOptions options = SourceNavigationOptions.builder().enabled(true).ide(SourceIde.INTELLIJ)
                .intellijProject("Checkout", Path.of("checkout-project")).build();

        assertEquals("jetbrains://idea/navigate/reference?project=Checkout&path="
                        + "src%2Ftest%2Fjava%2Fexample%2FCheckout.java%3A12",
                IdeNavigationUriProvider.target(options, file, 12, null, temp).orElseThrow());
    }

    @Test void rejectsMissingProjectMappingMissingFileInvalidLocationAndOutsideProject() throws Exception {
        Path file = source("src/test/java/example/Login.java");
        assertTrue(IdeNavigationUriProvider.target(options(SourceIde.INTELLIJ), file, 3, null, temp).isEmpty());

        SourceNavigationOptions mapped = SourceNavigationOptions.builder().enabled(true).ide(SourceIde.INTELLIJ)
                .intellijProject("Mapped", temp).build();
        assertTrue(IdeNavigationUriProvider.target(mapped, temp.resolve("Missing.java"), 3, null, temp).isEmpty());
        assertTrue(IdeNavigationUriProvider.target(mapped, file, 0, null, temp).isEmpty());
        assertTrue(IdeNavigationUriProvider.target(mapped, file, 3, 0, temp).isEmpty());

        Path otherRoot = Files.createDirectory(temp.resolve("other"));
        Path outside = Files.writeString(otherRoot.resolve("Outside.java"), "class Outside {}");
        SourceNavigationOptions narrower = SourceNavigationOptions.builder().enabled(true).ide(SourceIde.INTELLIJ)
                .intellijProject("Mapped", temp.resolve("src")).build();
        assertTrue(IdeNavigationUriProvider.target(narrower, outside, 1, null, temp).isEmpty());
    }

    @Test void createsVsCodeTargetWithEscapedFileUriPath() throws Exception {
        String target = IdeNavigationUriProvider.target(options(SourceIde.VSCODE),
                source("folder with spaces/Login Page.java"), 41, 7).orElseThrow();
        assertTrue(target.startsWith("vscode://file/"));
        assertTrue(target.contains("folder%20with%20spaces"));
        assertTrue(target.endsWith(":41:7"));
    }

    @Test void customProviderRequiresTemplateAndExpandsPlaceholders() throws Exception {
        Path file = source("Login.java");
        assertTrue(IdeNavigationUriProvider.target(options(SourceIde.CUSTOM), file, 3, null).isEmpty());
        SourceNavigationOptions custom = SourceNavigationOptions.builder().enabled(true).ide(SourceIde.CUSTOM)
                .customUriTemplate("myide://open?file={file}&line={line}&column={column}").build();
        String target = IdeNavigationUriProvider.target(custom, file, 3, 2).orElseThrow();
        assertTrue(target.startsWith("myide://open?file="));
        assertTrue(target.endsWith("&line=3&column=2"));
    }

    private Path source(String relative) throws Exception {
        Path file = temp.resolve(relative);
        Files.createDirectories(file.getParent());
        return Files.writeString(file, "class Source {}");
    }

    private static SourceNavigationOptions options(SourceIde ide) {
        return SourceNavigationOptions.builder().enabled(true).ide(ide).build();
    }
}
