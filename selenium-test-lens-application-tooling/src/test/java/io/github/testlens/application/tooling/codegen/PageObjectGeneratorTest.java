package io.github.testlens.application.tooling.codegen;

import io.github.testlens.application.model.ApplicationModel;
import io.github.testlens.application.tooling.ToolingFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.ToolProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PageObjectGeneratorTest {
    @TempDir Path temporary;

    @Test
    void generationIsDeterministicUsesSupportedStrategiesTransitionsAndSkipsReviewSelectors() {
        PageObjectGenerationOptions options = PageObjectGenerationOptions.verifiedOnly("example.pages", temporary);
        PageObjectGenerator generator = new PageObjectGenerator();

        List<GeneratedPageObject> first = generator.generate(ToolingFixtures.model(), options);
        List<GeneratedPageObject> second = generator.generate(ToolingFixtures.model(), options);
        GeneratedPageObject login = first.stream().filter(page -> page.pageId().equals("login-page")).findFirst().orElseThrow();

        assertEquals(first, second);
        assertTrue(login.generatedSource().contains("By.id(\"username\")"));
        assertTrue(login.generatedSource().contains("By.cssSelector(\"[data-testid='login']\")"));
        assertTrue(login.generatedSource().contains("DashboardPage clickLoginButton()"));
    }

    @Test
    void duplicateNamesEscapingAndReviewSkipRemainValidJava() throws Exception {
        ApplicationModel.Provenance provenance = ToolingFixtures.provenance("test", true);
        ApplicationModel.ElementModel keyword = ToolingFixtures.element("class-one", "class", ApplicationModel.ElementType.BUTTON,
                List.of(ApplicationModel.Action.CLICK), "xpath", "//button[@title=\"a\\b\"]",
                ApplicationModel.SelectorQuality.VERIFIED, provenance);
        ApplicationModel.ElementModel duplicate = ToolingFixtures.element("class-two", "class", ApplicationModel.ElementType.BUTTON,
                List.of(ApplicationModel.Action.CLICK), "tag name", "button",
                ApplicationModel.SelectorQuality.VERIFIED, provenance);
        ApplicationModel.ElementModel review = ToolingFixtures.element("review", "review", ApplicationModel.ElementType.BUTTON,
                List.of(ApplicationModel.Action.CLICK), "id", "review",
                ApplicationModel.SelectorQuality.REVIEW_REQUIRED, provenance);
        ApplicationModel.PageModel page = ToolingFixtures.page("odd-page", "Odd */ Page", "/odd", "odd-state",
                List.of(keyword, duplicate, review), List.of(), provenance);
        ApplicationModel base = ToolingFixtures.model();
        ApplicationModel model = new ApplicationModel(base.schemaVersion(), base.applicationId(), base.applicationName(),
                base.generatorVersion(), base.generatedAt(), List.of(page), List.of(), List.of(), base.coverage(), List.of(), provenance);
        PageObjectGenerationOptions options = PageObjectGenerationOptions.verifiedOnly("example.generated", temporary);
        List<GeneratedPageObject> generated = new PageObjectGenerator().generate(model, options);
        GeneratedPageObject output = generated.get(0);

        assertEquals(2, output.bindings().size());
        assertTrue(output.warnings().stream().anyMatch(value -> value.contains("review required; skipped")));
        assertNotEquals(output.bindings().get(0).fieldName(), output.bindings().get(1).fieldName());
        PageObjectSourceStore.WriteResult written = new PageObjectSourceStore().write(options, generated);
        assertCompiles(written.generatedBases(), written.createdExtensions());
    }

    @Test
    void regenerationOverwritesGeneratedBaseButPreservesUserExtension() throws Exception {
        PageObjectGenerationOptions options = PageObjectGenerationOptions.verifiedOnly("example.pages", temporary);
        PageObjectSourceStore store = new PageObjectSourceStore();
        List<GeneratedPageObject> initial = new PageObjectGenerator().generate(ToolingFixtures.model(), options);
        PageObjectSourceStore.WriteResult first = store.write(options, initial);
        Path extension = first.createdExtensions().get(0);
        Files.writeString(extension, "// user-authored extension");

        ApplicationModel changed = ToolingFixtures.model(java.time.Instant.EPOCH, "next", "#new-login",
                ApplicationModel.SelectorQuality.VERIFIED, true);
        PageObjectSourceStore.WriteResult second = store.write(options, new PageObjectGenerator().generate(changed, options));

        assertEquals("// user-authored extension", Files.readString(extension));
        assertTrue(second.preservedExtensions().contains(extension));
        Path generatedLogin = second.generatedBases().stream()
                .filter(path -> path.getFileName().toString().equals("GeneratedLoginPage.java"))
                .findFirst().orElseThrow();
        assertTrue(Files.readString(generatedLogin).contains("#new-login"));
    }

    @Test
    void pageAndMethodNamespacesAvoidGeneratedPrefixAndClickNameCollisions() throws Exception {
        ApplicationModel.Provenance provenance = ToolingFixtures.provenance("collision", true);
        ApplicationModel.ElementModel foo = ToolingFixtures.element("foo", "foo", ApplicationModel.ElementType.BUTTON,
                List.of(ApplicationModel.Action.CLICK), "id", "foo", ApplicationModel.SelectorQuality.VERIFIED, provenance);
        ApplicationModel.ElementModel clickFoo = ToolingFixtures.element("click-foo", "clickFoo", ApplicationModel.ElementType.BUTTON,
                List.of(ApplicationModel.Action.CLICK), "id", "click-foo", ApplicationModel.SelectorQuality.VERIFIED, provenance);
        ApplicationModel.PageModel login = ToolingFixtures.page("login", "Login", "/login", "login-state",
                List.of(foo, clickFoo), List.of(), provenance);
        ApplicationModel.PageModel generatedLogin = ToolingFixtures.page("generated-login", "Generated Login", "/generated-login",
                "Generated Login",
                List.of(), List.of(), provenance);
        ApplicationModel base = ToolingFixtures.model();
        ApplicationModel model = new ApplicationModel(base.schemaVersion(), base.applicationId(), base.applicationName(),
                base.generatorVersion(), base.generatedAt(), List.of(login, generatedLogin), List.of(), List.of(),
                base.coverage(), List.of(), provenance);
        PageObjectGenerationOptions options = PageObjectGenerationOptions.verifiedOnly("example.collisions", temporary);
        List<GeneratedPageObject> pages = new PageObjectGenerator().generate(model, options);

        List<String> allTypes = pages.stream()
                .flatMap(page -> java.util.stream.Stream.of(page.generatedClassName(), page.extensionClassName()))
                .toList();
        assertEquals(allTypes.size(), allTypes.stream().distinct().count(),
                "generated bases and user extension classes share one Java type namespace");
        GeneratedPageObject loginOutput = pages.stream().filter(page -> page.pageId().equals("login")).findFirst().orElseThrow();
        assertTrue(loginOutput.generatedSource().contains("clickFoo()"));
        assertTrue(loginOutput.generatedSource().contains("clickClickFoo()"));
        PageObjectSourceStore.WriteResult written = new PageObjectSourceStore().write(options, pages);
        assertCompiles(written.generatedBases(), written.createdExtensions());
    }

    private static void assertCompiles(List<Path> generated, List<Path> extensions) throws Exception {
        var compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "tests require a JDK, not a JRE");
        Path classes = Files.createDirectories(generated.get(0).getParent().resolve("compiled"));
        String selenium = Path.of(org.openqa.selenium.By.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        String classpath = System.getProperty("java.class.path") + java.io.File.pathSeparator + selenium;
        List<String> arguments = new java.util.ArrayList<>(List.of(
                "-classpath", classpath, "-d", classes.toString()));
        generated.forEach(path -> arguments.add(path.toString()));
        extensions.forEach(path -> arguments.add(path.toString()));
        assertEquals(0, compiler.run(null, null, null, arguments.toArray(String[]::new)));
    }
}
