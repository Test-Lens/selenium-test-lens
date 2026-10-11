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
        assertEquals(ExistingProjectIndexer.selectorValueFingerprint("[data-testid='login-submit']"),
                ExistingProjectIndexer.selectorValueFingerprint("[data-testid=\"login-submit\"]"));
        assertTrue(first.matches("sha256:[0-9a-f]{64}"));
        assertThrows(NullPointerException.class, () -> ExistingProjectIndexer.selectorValueFingerprint(null));
    }

    @Test
    void indexesPageObjectsComponentsLocatorsTestsAndUsageGraphInOneScannerPass() throws Exception {
        Fixture fixture = fixture();
        fixture.source("sample/BasePage.java", """
                package sample;
                import org.openqa.selenium.By;
                class BasePage { By root = By.id("root"); void open() { navigate(); } void navigate() {} }
                """);
        fixture.source("sample/MenuComponent.java", """
                package sample;
                import org.openqa.selenium.By;
                class MenuComponent { By item = By.cssSelector(".menu-item"); void choose() { use(item); } void use(By by) {} }
                """);
        fixture.source("sample/TopNavigation.java", """
                package sample;
                class TopNavigation { void openCustomers() {} }
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
                    void focusRoot() { use(root); }
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
        assertEquals(ClassClassification.COMPONENT, classification(index, "TopNavigation"));
        assertEquals(ClassClassification.PAGE_OBJECT, classification(index, "LoginPage"));
        assertTrue(index.elements().stream().anyMatch(value -> value.name().equals("save")
                && value.strategy().equals("id")), "UiLocator wrapping By must reuse the same declaration scan");
        assertTrue(index.elements().stream().anyMatch(value -> value.name().equals("email")
                && value.strategy().equals("id") && value.valueProjection().fingerprint().startsWith("sha256:")));
        String rootDeclaration=index.elements().stream().filter(value->value.name().equals("root")).findFirst().orElseThrow().declarationRef();
        String focusRoot=index.methods().stream().filter(value->value.name().equals("focusRoot")).findFirst().orElseThrow().id();
        assertTrue(index.edges().stream().anyMatch(value->value.type()==EdgeType.METHOD_TO_DECLARATION&&value.fromId().equals(focusRoot)&&value.toId().equals(rootDeclaration)),"Inherited locator use must remain connected to its base declaration");
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
    void reportsUnresolvedScopedCallsAndBoundsAsPartialWithoutPersistingSourceOrAbsolutePaths() throws Exception {
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
        assertTrue(index.limitations().stream().anyMatch(value -> value.code().equals("UNRESOLVED_DYNAMIC_CALL")));
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

    @Test
    void indexesConstructionAndCallableSurfaceWithoutCollapsingSimilarMethods() throws Exception {
        Fixture fixture = fixture();
        fixture.source("sample/LoginPage.java", """
                package sample;
                import org.openqa.selenium.WebDriver;
                public class LoginPage implements LoginActions {
                    public LoginPage(WebDriver driver) {}
                    protected LoginPage(String profile) {}
                    LoginPage(int port) {}
                    private LoginPage() {}

                    public LoginPage loginAs(String username, String password) { return this; }
                    public boolean loginErrorVisible() { return true; }
                    public boolean loginErrorHidden() { return false; }
                    public static LoginPage open(WebDriver driver) { return new LoginPage(driver); }
                    protected void resetForSubclass() {}
                    void packageHelper() {}
                    private void internalSecret() {}
                }
                """);
        fixture.source("sample/LoginActions.java", """
                package sample;
                public interface LoginActions {
                    default boolean loginErrorFromContract() { return true; }
                }
                """);
        fixture.source("sample/DashboardPage.java", """
                package sample;
                public class DashboardPage { public boolean loaded() { return true; } }
                """);
        fixture.source("sample/AbstractPage.java", """
                package sample;
                public abstract class AbstractPage { public boolean loaded() { return true; } }
                """);
        fixture.source("sample/Outer.java", """
                package sample;
                public class Outer {
                    public class InnerPage { public boolean loaded() { return true; } }
                    public static class StaticPage { public boolean loaded() { return true; } }
                }
                """);

        ExistingProjectIndex index = fixture.index();
        ClassEntry login = index.classes().stream()
                .filter(value -> value.qualifiedName().equals("sample.LoginPage")).findFirst().orElseThrow();

        assertEquals(Accessibility.PUBLIC, login.accessibility());
        assertEquals(List.of("LoginActions"),login.extendsTypes());
        assertEquals(List.of("LoginPage()", "LoginPage(String)", "LoginPage(WebDriver)", "LoginPage(int)"),
                login.constructors().stream().map(ConstructorEntry::signature).toList());
        assertEquals(Accessibility.PUBLIC, constructor(login, "LoginPage(WebDriver)").accessibility());
        assertEquals(List.of(new Parameter("driver", "org.openqa.selenium.WebDriver")),
                constructor(login, "LoginPage(WebDriver)").parameters());
        assertEquals(Accessibility.PROTECTED, constructor(login, "LoginPage(String)").accessibility());
        assertEquals(Accessibility.PACKAGE_PRIVATE, constructor(login, "LoginPage(int)").accessibility());
        assertEquals(Accessibility.PRIVATE, constructor(login, "LoginPage()").accessibility());
        assertTrue(login.constructors().stream().noneMatch(ConstructorEntry::implicit));

        ClassEntry dashboard = index.classes().stream()
                .filter(value -> value.qualifiedName().equals("sample.DashboardPage")).findFirst().orElseThrow();
        assertEquals(List.of(new ConstructorEntry("DashboardPage()", List.of(), Accessibility.PUBLIC, true)),
                dashboard.constructors(), "a public implicit constructor is part of the callable surface");
        assertTrue(dashboard.directlyInstantiable());
        assertFalse(index.classes().stream().filter(value -> value.qualifiedName().equals("sample.AbstractPage"))
                .findFirst().orElseThrow().directlyInstantiable());
        assertFalse(index.classes().stream().filter(value -> value.qualifiedName().equals("sample.Outer.InnerPage"))
                .findFirst().orElseThrow().directlyInstantiable());
        assertTrue(index.classes().stream().filter(value -> value.qualifiedName().equals("sample.Outer.StaticPage"))
                .findFirst().orElseThrow().directlyInstantiable());

        MethodEntry loginAs = method(index, login.id(), "loginAs");
        assertEquals(Accessibility.PUBLIC, loginAs.accessibility());
        assertEquals(List.of(new Parameter("username", "String"), new Parameter("password", "String")),
                loginAs.parameters());
        assertEquals("LoginPage", loginAs.returnType());
        assertFalse(loginAs.staticMethod());

        MethodEntry open = method(index, login.id(), "open");
        assertEquals(Accessibility.PUBLIC, open.accessibility());
        assertTrue(open.staticMethod());
        assertEquals("LoginPage", open.returnType());

        assertEquals(List.of("loginErrorHidden", "loginErrorVisible"), index.methods().stream()
                .filter(value -> value.ownerClassId().equals(login.id()) && value.name().startsWith("loginError"))
                .map(MethodEntry::name).sorted().toList(),
                "similar names with opposite semantics must remain separate capabilities");
        assertEquals(Accessibility.PROTECTED, method(index, login.id(), "resetForSubclass").accessibility());
        assertEquals(Accessibility.PACKAGE_PRIVATE, method(index, login.id(), "packageHelper").accessibility());
        assertEquals(Accessibility.PRIVATE, method(index, login.id(), "internalSecret").accessibility());
        ClassEntry actions=index.classes().stream().filter(value->value.qualifiedName().equals("sample.LoginActions"))
                .findFirst().orElseThrow();
        assertEquals(Accessibility.PUBLIC,method(index,actions.id(),"loginErrorFromContract").accessibility(),
                "interface methods are public even without an explicit modifier");
        assertEquals(index.methods().stream().map(MethodEntry::id).sorted().toList(),
                index.methods().stream().map(MethodEntry::id).toList());
    }

    @Test
    void explicitImportResolvesInheritedParentWhenSimpleNamesCollide() throws Exception {
        Fixture fixture=fixture();
        fixture.source("trusted/BasePage.java", """
                package trusted;
                public class BasePage { public boolean inheritedVisible() { return true; } }
                """);
        fixture.source("other/BasePage.java", """
                package other;
                public class BasePage { public boolean unrelated() { return true; } }
                """);
        fixture.source("child/LoginPage.java", """
                package child;
                import trusted.BasePage;
                public class LoginPage extends BasePage { public boolean local() { return true; } }
                """);

        ExistingProjectIndex index=fixture.index();
        ClassEntry login=index.classes().stream()
                .filter(value->value.qualifiedName().equals("child.LoginPage")).findFirst().orElseThrow();

        assertEquals(List.of("trusted.BasePage"),login.extendsTypes());
    }

    @Test
    void preservesVarargsInIndexedMethodParameterTypes() throws Exception {
        Fixture fixture = fixture();
        fixture.source("sample/LoginPage.java", """
                package sample;
                public class LoginPage {
                    public boolean invalidLogin(String username, String... passwords) { return true; }
                }
                """);

        ExistingProjectIndex index = fixture.index();
        ClassEntry owner = index.classes().stream()
                .filter(value -> value.qualifiedName().equals("sample.LoginPage")).findFirst().orElseThrow();
        MethodEntry method = method(index, owner.id(), "invalidLogin");

        assertEquals(List.of("String", "String..."),
                method.parameters().stream().map(Parameter::type).toList());
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
        write(types, "org/openqa/selenium/WebDriver.java",
                "package org.openqa.selenium; public interface WebDriver {}");
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

    private static ConstructorEntry constructor(ClassEntry owner, String signature) {
        return owner.constructors().stream().filter(value -> value.signature().equals(signature))
                .findFirst().orElseThrow();
    }

    private static MethodEntry method(ExistingProjectIndex index, String ownerClassId, String name) {
        return index.methods().stream().filter(value -> value.ownerClassId().equals(ownerClassId)
                && value.name().equals(name)).findFirst().orElseThrow();
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
