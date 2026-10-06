package io.github.testlens.selector.tooling;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static io.github.testlens.selector.tooling.ExistingProjectIndex.*;
import static org.junit.jupiter.api.Assertions.*;

class ExistingProjectIndexerTest {
    @TempDir Path temporary;

    @Test
    void exposesTheExactDeterministicSelectorValueFingerprintWithoutAcceptingNull() {
        String first = ExistingProjectIndexer.selectorValueFingerprint("private-value");
        assertEquals(first, ExistingProjectIndexer.selectorValueFingerprint("private-value"));
        assertNotEquals(first, ExistingProjectIndexer.selectorValueFingerprint("other-value"));
        assertTrue(first.matches("sha256:[0-9a-f]{64}"));
        assertThrows(NullPointerException.class, () -> ExistingProjectIndexer.selectorValueFingerprint(null));
    }

    @Test
    void indexesPageObjectsComponentsLocatorsTestsAndUsageGraphInOneScannerPass() throws Exception {
        Fixture fixture = fixture();
        fixture.source("sample/BasePage.java", """
                package sample;
                class BasePage { void open() { navigate(); } void navigate() {} }
                """);
        fixture.source("sample/MenuComponent.java", """
                package sample;
                import org.openqa.selenium.By;
                class MenuComponent { By item = By.cssSelector(".menu-item"); void choose() { use(item); } void use(By by) {} }
                """);
        fixture.source("sample/LoginPage.java", """
                package sample;
                import org.openqa.selenium.By;
                import io.github.testlens.selenium.locator.UiLocator;
                class LoginPage extends BasePage {
                    By email = By.id("email");
                    UiLocator save = lens.locator(By.id("save"));
                    Lens lens;
                    void login() { fill(); submit(); }
                    void fill() { use(email); }
                    void submit() { save.click(); }
                    void use(By by) {}
                }
                class Lens { UiLocator locator(By by) { return null; } }
                """);
        fixture.source("sample/LoginTests.java", """
                package sample;
                import org.junit.jupiter.api.Test;
                import org.junit.jupiter.api.Tag;
                class LoginTests {
                    LoginPage page;
                    @Test @Tag("smoke") void signsIn() { page.login(); }
                }
                """);
        fixture.source("sample/LegacyTests.java", """
                package sample;
                import org.testng.annotations.Test;
                class LegacyTests { LoginPage page; @Test(groups={"regression"}) void legacy() { page.login(); } }
                """);

        ExistingProjectIndex index = fixture.index();

        assertEquals(Completeness.COMPLETE, index.completeness(), index.limitations().toString());
        assertEquals(ClassClassification.BASE_PAGE, classification(index, "BasePage"));
        assertEquals(ClassClassification.COMPONENT, classification(index, "MenuComponent"));
        assertEquals(ClassClassification.PAGE_OBJECT, classification(index, "LoginPage"));
        assertTrue(index.elements().stream().anyMatch(value -> value.name().equals("email")
                && value.strategy().equals("id") && value.valueProjection().fingerprint().startsWith("sha256:")));
        assertTrue(index.elements().stream().anyMatch(value -> value.name().equals("save")
                && value.declarationRef().startsWith("java-decl-v1:")));
        assertEquals(2, index.tests().size());
        assertTrue(index.tests().stream().anyMatch(value -> value.framework() == TestFramework.JUNIT5
                && value.tags().equals(List.of("smoke"))));
        assertTrue(index.tests().stream().anyMatch(value -> value.framework() == TestFramework.TESTNG
                && value.groups().equals(List.of("regression"))));
        assertTrue(index.edges().stream().anyMatch(value -> value.type() == EdgeType.TEST_TO_METHOD));
        assertTrue(index.edges().stream().anyMatch(value -> value.type() == EdgeType.METHOD_TO_METHOD));
        assertTrue(index.edges().stream().anyMatch(value -> value.type() == EdgeType.METHOD_TO_DECLARATION));
        assertEquals(index.metrics().files(), index.metrics().parsedFiles());
    }

    @Test
    void reportsAmbiguousCallsAndBoundsAsPartialWithoutPersistingSourceOrAbsolutePaths() throws Exception {
        Fixture fixture = fixture();
        fixture.source("sample/A.java", """
                package sample; import org.openqa.selenium.By;
                class APage {
                    By secret = By.id("token=do-not-store");
                    void same() {} void consume(By by) {} void use() { consume(secret); }
                }
                class BPage { void same() {} }
                class Caller { Object helper; void call() { helper.same(); } }
                """);
        ExistingProjectIndexer.Bounds bounds = new ExistingProjectIndexer.Bounds(4, 10, 10, 10, 10, 10, 1);

        ExistingProjectIndex index = fixture.index(bounds);
        String rendered = index.toString();

        assertEquals(Completeness.PARTIAL, index.completeness());
        assertTrue(index.limitations().stream().anyMatch(value -> value.code().equals("AMBIGUOUS_CALL")));
        assertTrue(index.limitations().stream().anyMatch(value -> value.code().equals("EDGES_LIMIT")));
        assertFalse(rendered.contains("token=do-not-store"));
        assertFalse(rendered.contains(fixture.root.toAbsolutePath().toString()));
    }

    @Test
    void identityAndCanonicalOrderingAreDeterministicAndIgnoreRanges() throws Exception {
        Fixture fixture = fixture();
        Path page = fixture.source("z/ZPage.java", """
                package z; import org.openqa.selenium.By;
                class ZPage { By save = By.id("save"); void clickSave() { use(save); } void use(By by) {} }
                """);
        fixture.source("a/APage.java", "package a; class APage {}");
        ExistingProjectIndex first = fixture.index();
        Files.writeString(page, Files.readString(page).replace("class ZPage", "\n\nclass ZPage"));
        ExistingProjectIndex second = fixture.index();

        assertEquals(first.classes().stream().map(ClassEntry::id).toList(),
                second.classes().stream().map(ClassEntry::id).toList());
        assertEquals(first.elements().stream().map(ElementEntry::id).toList(),
                second.elements().stream().map(ElementEntry::id).toList());
        assertNotEquals(first.elements().get(0).range().startLine(), second.elements().get(0).range().startLine());
        assertEquals(first.classes().stream().map(ClassEntry::id).sorted().toList(),
                first.classes().stream().map(ClassEntry::id).toList());
    }

    @Test
    void keepsEachVariableAsTheOwnerInMultiVariableFields() throws Exception {
        Fixture fixture = fixture();
        fixture.source("sample/MultiPage.java", """
                package sample; import org.openqa.selenium.By;
                class MultiPage { By first = By.id("first"), second = By.id("second"); }
                """);

        assertEquals(List.of("first", "second"), fixture.index().elements().stream()
                .map(ElementEntry::name).sorted().toList());
    }

    private ClassClassification classification(ExistingProjectIndex index, String simpleName) {
        return index.classes().stream().filter(value -> value.simpleName().equals(simpleName)).findFirst()
                .orElseThrow().classification();
    }

    private Fixture fixture() throws IOException {
        Path root = Files.createDirectory(temporary.resolve("project-" + Files.list(temporary).count()));
        Path sources = Files.createDirectories(root.resolve("module/src/test/java"));
        Path types = Files.createDirectories(root.resolve("types"));
        write(types, "org/openqa/selenium/By.java", """
                package org.openqa.selenium; public class By {
                  public static By id(String value) { return null; }
                  public static By cssSelector(String value) { return null; }
                }
                """);
        write(types, "io/github/testlens/selenium/locator/UiLocator.java", """
                package io.github.testlens.selenium.locator; public class UiLocator { public void click() {} }
                """);
        write(types, "org/junit/jupiter/api/Test.java",
                "package org.junit.jupiter.api; public @interface Test {}");
        write(types, "org/junit/jupiter/api/Tag.java",
                "package org.junit.jupiter.api; public @interface Tag { String value(); }");
        write(types, "org/testng/annotations/Test.java",
                "package org.testng.annotations; public @interface Test { String[] groups() default {}; }");
        return new Fixture(root, sources, types);
    }

    private Path write(Path root, String relative, String content) throws IOException {
        Path file = root.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        return file;
    }

    private final class Fixture {
        private final Path root;
        private final Path sources;
        private final Path types;

        private Fixture(Path root, Path sources, Path types) {
            this.root = root;
            this.sources = sources;
            this.types = types;
        }

        private Path source(String relative, String content) throws IOException { return write(sources, relative, content); }
        private ExistingProjectIndex index() { return index(ExistingProjectIndexer.Bounds.defaults()); }
        private ExistingProjectIndex index(ExistingProjectIndexer.Bounds bounds) {
            return new ExistingProjectIndexer().index(new ExistingProjectIndexer.Request(root,
                    List.of(sources), List.of(types), java.nio.charset.StandardCharsets.UTF_8, bounds));
        }
    }
}
