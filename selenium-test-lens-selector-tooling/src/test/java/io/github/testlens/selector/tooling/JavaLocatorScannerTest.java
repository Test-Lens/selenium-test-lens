package io.github.testlens.selector.tooling;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static io.github.testlens.selector.tooling.SelectorIndexModel.*;
import static org.junit.jupiter.api.Assertions.*;

class JavaLocatorScannerTest {
    @TempDir Path temporary;

    @Test
    void discoversAllStandardByStrategiesAndKeepsInitializerOwnership() throws Exception {
        Fixture fixture = fixture();
        fixture.source("sample/AllLocators.java", """
                package sample;
                import org.openqa.selenium.By;
                class AllLocators {
                    static final By SAVE = By.id("save");
                    By instance = By.cssSelector("button.save");
                    void use() {
                        By local = By.xpath("//button");
                        use(By.name("email"));
                        use(By.className("primary"));
                        use(By.tagName("button"));
                        use(By.linkText("Dalej"));
                        use(By.partialLinkText("Dal"));
                        use(SAVE);
                    }
                    void use(By by) {}
                }
                """);

        SelectorIndex index = fixture.scan();
        List<DeclarationRecord> declarations = declarations(index);
        assertEquals(8, declarations.size());
        assertEquals(1, declarations.stream().filter(value -> value.declarationKind() == DeclarationKind.BY_FIELD
                && value.declaringSymbol().memberSignature().equals("SAVE")).count());
        assertEquals(Set.of("id", "css selector", "xpath", "name", "class name", "tag name", "link text",
                        "partial link text"),
                declarations.stream().map(DeclarationRecord::resolvedLocator).filter(java.util.Objects::nonNull)
                        .map(ResolvedLocator::strategy).collect(Collectors.toSet()));
        assertEquals(8, index.coverage().resolved());
    }

    @Test
    void resolvesLiteralsTextBlocksConstantsStaticImportsAndConstantConcatenation() throws Exception {
        Fixture fixture = fixture();
        fixture.source("sample/Constants.java", """
                package sample;
                final class Constants {
                    static final String PREFIX = "save-";
                    static final String SUFFIX = "zmiany ✅";
                    static final String QUOTED = "a\\\"b\\\\c";
                }
                """);
        fixture.source("sample/ConstantLocators.java", """
                package sample;
                import static org.openqa.selenium.By.id;
                import org.openqa.selenium.By;
                import static sample.Constants.*;
                class ConstantLocators {
                    static final By A = id(PREFIX + SUFFIX);
                    static final By B = By.name(QUOTED);
                    static final By C = By.xpath(\"""
                            //button[@title='Zapisz']
                            \""");
                }
                """);

        List<DeclarationRecord> declarations = declarations(fixture.scan());
        assertEquals(3, declarations.size());
        assertTrue(declarations.stream().allMatch(value -> value.resolutionStatus() == ResolutionStatus.RESOLVED),
                declarations.toString());
        assertTrue(declarations.stream().anyMatch(value -> "save-zmiany ✅".equals(value.resolvedLocator().value())));
        assertTrue(declarations.stream().anyMatch(value -> "a\"b\\c".equals(value.resolvedLocator().value())));
        assertTrue(declarations.stream().anyMatch(value -> value.resolvedLocator().value().contains("//button")));
    }

    @Test
    void modelsDynamicConcatenationAndHelperTemplateWithoutExecutingCode() throws Exception {
        Fixture fixture = fixture();
        fixture.source("sample/Dynamic.java", """
                package sample;
                import org.openqa.selenium.By;
                class Dynamic {
                    By row(String rowId) { return By.id("row-" + rowId); }
                    void use(String accountId) { use(By.xpath("//div[@id='account-" + accountId + "']")); }
                    void use(By by) {}
                }
                """);

        List<DeclarationRecord> declarations = declarations(fixture.scan());
        assertEquals(2, declarations.size());
        DeclarationRecord helper = declarations.stream()
                .filter(value -> value.declarationKind() == DeclarationKind.HELPER_METHOD).findFirst().orElseThrow();
        assertEquals(ExpressionKind.TEMPLATE, helper.locatorExpression().kind());
        assertEquals(ResolutionStatus.DYNAMIC, helper.resolutionStatus());
        assertTrue(helper.locatorExpression().parameterDependencies().contains("rowId"));
        assertNull(helper.resolvedLocator());
        assertTrue(declarations.stream().anyMatch(value -> value.resolutionStatus() == ResolutionStatus.DYNAMIC
                && value.locatorExpression().parameterDependencies().contains("accountId")));
    }

    @Test
    void discoversFindByAndPreservesFindBysAndFindAllCompositionOrder() throws Exception {
        Fixture fixture = fixture();
        fixture.source("sample/Page.java", """
                package sample;
                import org.openqa.selenium.WebElement;
                import org.openqa.selenium.support.FindBy;
                import org.openqa.selenium.support.FindBys;
                import org.openqa.selenium.support.FindAll;
                import org.openqa.selenium.support.How;
                class Page {
                    @FindBy(id = "save") WebElement save;
                    @FindBy(name = "email") WebElement email;
                    @FindBy(className = "primary") WebElement primary;
                    @FindBy(css = "button.next") WebElement css;
                    @FindBy(tagName = "button") WebElement tag;
                    @FindBy(linkText = "Dalej") WebElement link;
                    @FindBy(partialLinkText = "Dal") WebElement partial;
                    @FindBy(xpath = "//button") WebElement xpath;
                    @FindBy(how = How.ID, using = "long-form") WebElement longForm;
                    @FindBys({@FindBy(id = "form"), @FindBy(css = "button.save")}) WebElement chained;
                    @FindAll({@FindBy(id = "primary"), @FindBy(name = "fallback")}) WebElement alternatives;
                    @FindBy(id = "a", css = ".b") WebElement invalid;
                }
                """);

        SelectorIndex index = fixture.scan();
        List<DeclarationRecord> declarations = declarations(index);
        assertEquals(12, declarations.size());
        assertEquals(Set.of("id", "name", "class name", "css selector", "tag name", "link text",
                        "partial link text", "xpath"),
                declarations.stream().map(DeclarationRecord::resolvedLocator).filter(java.util.Objects::nonNull)
                        .map(ResolvedLocator::strategy).collect(Collectors.toSet()));
        DeclarationRecord chain = byKind(declarations, DeclarationKind.FIND_BYS);
        assertEquals(ExpressionKind.ORDERED_CHAIN, chain.locatorExpression().kind());
        assertEquals(List.of("id", "css selector"), childStrategies(chain));
        assertNull(chain.resolvedLocator());
        DeclarationRecord alternatives = byKind(declarations, DeclarationKind.FIND_ALL);
        assertEquals(ExpressionKind.ALTERNATIVES, alternatives.locatorExpression().kind());
        assertEquals(List.of("id", "name"), childStrategies(alternatives));
        assertEquals(1, index.coverage().errors());
        assertTrue(index.files().get(0).issues().stream()
                .anyMatch(value -> value.code().equals("FINDBY_MULTIPLE_STRATEGIES")));
    }

    @Test
    void recognizesCustomHelpersWithoutExecutingThem() throws Exception {
        Fixture fixture = fixture();
        fixture.source("sample/Custom.java", """
                package sample;
                import org.openqa.selenium.By;
                class SemanticBy { static By role(String value) { throw new AssertionError("must not run"); } }
                class CompositeBy { static By descendants(By value) { throw new AssertionError("must not run"); } }
                class Custom {
                    By role = SemanticBy.role("button");
                    By nested = CompositeBy.descendants(By.id("root"));
                }
                """);

        List<DeclarationRecord> declarations = declarations(fixture.scan());
        assertEquals(2, declarations.size());
        assertEquals(2, declarations.stream().filter(value -> value.resolutionStatus() == ResolutionStatus.CUSTOM).count());
        assertTrue(declarations.stream().anyMatch(value -> value.locatorExpression().normalizedExpression()
                .contains("SemanticBy.role")));
        assertTrue(declarations.stream().map(value -> value.locatorExpression().factorySymbol())
                .filter(java.util.Objects::nonNull)
                .anyMatch(value -> value.contains("sample.SemanticBy.role")));
    }

    @Test
    void isolatesMalformedFilesAndReportsKotlinAndIncompleteClasspath() throws Exception {
        Fixture fixture = fixture();
        fixture.source("sample/Good.java", "package sample; import org.openqa.selenium.By; class Good { By x=By.id(\"x\"); }");
        fixture.source("sample/Broken.java", "package sample; class Broken {");
        fixture.source("sample/Kotlin.kt", "package sample\nclass Kotlin");
        ScanRequest request = new ScanRequest(fixture.root, List.of(fixture.sources),
                List.of(fixture.types, fixture.root.resolve("missing.jar")));

        SelectorIndex index = new JavaLocatorScanner().scan(request);
        assertEquals(1, index.coverage().filesFailed());
        assertEquals(1, index.coverage().unsupportedLanguageFiles());
        assertTrue(index.coverage().incompleteClasspath());
        assertTrue(index.coverage().symbolResolutionIssues() > 0);
        assertEquals(1, index.coverage().resolved());
    }

    @Test
    void declarationRefsRangesHashesAndJsonAreDeterministicWithoutExactSourceOrAbsolutePaths() throws Exception {
        Fixture fixture = fixture();
        Path file = fixture.source("sample/Stable.java", """
                package sample;
                import org.openqa.selenium.By;
                class Stable {
                    By first = By.id("save");
                    By second = By.id("save");
                }
                """);
        byte[] before = Files.readAllBytes(file);
        SelectorIndex first = fixture.scan();
        String json1 = SelectorIndexJson.serialize(first);
        SelectorIndex second = fixture.scan();
        String json2 = SelectorIndexJson.serialize(second);

        assertEquals(json1, json2);
        assertArrayEquals(before, Files.readAllBytes(file));
        assertFalse(json1.contains("exactSource"));
        assertFalse(json1.contains(fixture.root.toAbsolutePath().toString()));
        assertTrue(json1.contains("\"offsetUnit\":\"UTF16_CODE_UNIT\""));
        assertEquals(2, declarations(first).stream().map(DeclarationRecord::declarationRef).distinct().count());
        assertTrue(declarations(first).stream().allMatch(value ->
                value.declarationRef().matches("java-decl-v1:sha256:[0-9a-f]{64}")
                        && value.sourceRange().endOffsetExclusive() > value.sourceRange().startOffset()));
        String source = Files.readString(file);
        assertTrue(declarations(first).stream().allMatch(value -> source.substring(
                value.sourceRange().startOffset(), value.sourceRange().endOffsetExclusive()).contains("By.id")));
        assertTrue(first.files().get(0).contentHash().matches("sha256:[0-9a-f]{64}"));

        Path output = fixture.root.resolve("target/selector-tooling/selector-index.json");
        SelectorIndexJson.writeLocal(first, fixture.root, output);
        assertEquals(json1, Files.readString(output));
        assertThrows(IllegalArgumentException.class,
                () -> SelectorIndexJson.writeLocal(first, fixture.root, fixture.root.resolve("selector-index.json")));
    }

    @Test
    void declarationRefIgnoresWhitespaceAndPrecedingCommentsButChangesWithLocatorExpression() throws Exception {
        Fixture fixture = fixture();
        Path file = fixture.source("sample/Stable.java", """
                package sample; import org.openqa.selenium.By;
                class Stable { By save = By.id("save"); }
                """);
        String initial = declarations(fixture.scan()).get(0).declarationRef();
        Files.writeString(file, """
                package sample;
                import org.openqa.selenium.By;
                // preceding comment moved
                class Stable {

                    By save = By.id("save");
                }
                """);
        assertEquals(initial, declarations(fixture.scan()).get(0).declarationRef());
        Files.writeString(file, Files.readString(file).replace("\"save\"", "\"changed\""));
        assertNotEquals(initial, declarations(fixture.scan()).get(0).declarationRef());
    }

    @Test
    void sortsFilesIndependentlyOfFilesystemEnumerationOrder() throws Exception {
        Fixture fixture = fixture();
        fixture.source("z/Z.java", "package z; import org.openqa.selenium.By; class Z { By z=By.id(\"z\"); }");
        fixture.source("a/A.java", "package a; import org.openqa.selenium.By; class A { By a=By.id(\"a\"); }");

        SelectorIndex index = fixture.scan();

        assertEquals(List.of("module/src/main/java/a/A.java", "module/src/main/java/z/Z.java"),
                index.files().stream().map(SourceFileIndex::logicalPath).toList());
        assertEquals(SelectorIndexJson.serialize(index), SelectorIndexJson.serialize(fixture.scan()));
    }

    @Test
    void enforcesRootFileCountFileSizeAndGeneratedSourceSafety() throws Exception {
        Fixture fixture = fixture();
        fixture.source("sample/A.java", "package sample; class A {}");
        fixture.source("sample/B.java", "package sample; class B {}");
        Path generated = fixture.root.resolve("target/generated-sources/generated");
        Files.createDirectories(generated);
        Files.writeString(generated.resolve("Generated.java"), "class Generated {}");
        Limits limits = new Limits(32, 1, 4, 64, 8, 8, 32);
        SelectorIndex limited = new JavaLocatorScanner().scan(new ScanRequest(fixture.root,
                List.of(fixture.sources, generated), List.of(fixture.types), "JAVA_17", StandardCharsets.UTF_8,
                null, limits));
        assertTrue(limited.issues().stream().anyMatch(value -> value.code().equals("FILES_LIMIT")));
        assertTrue(limited.issues().stream().anyMatch(value -> value.code().equals("GENERATED_ROOT_EXCLUDED")));
        assertTrue(limited.coverage().generatedFilesExcluded() > 0);
        assertTrue(limited.coverage().filesExcluded() > 0 || limited.issues().stream()
                .anyMatch(value -> value.code().equals("FILES_LIMIT")));

        Path outside = Files.createDirectory(temporary.resolve("outside"));
        assertThrows(IllegalArgumentException.class,
                () -> new JavaLocatorScanner().scan(new ScanRequest(fixture.root, List.of(outside), List.of())));
        assertThrows(IllegalArgumentException.class,
                () -> new JavaLocatorScanner().scan(new ScanRequest(fixture.root, List.of(Path.of("../outside")), List.of())));
    }

    @Test
    void handlesJava17SurroundingSyntaxUnicodeAndEscapedJsonAsData() throws Exception {
        Fixture fixture = fixture();
        fixture.source("sample/Modern.java", """
                package sample;
                import org.openqa.selenium.By;
                record Modern<T>(T value) {
                    By locator() {
                        var lambda = (java.util.function.Supplier<String>) () -> switch (value.toString()) {
                            case "x" -> "<script>alert('x')</script>";
                            default -> "zażółć ✅ token=sekret";
                        };
                        return By.id("<script>" + lambda.get());
                    }
                    static class Nested { By value = By.name("e-mail@example.test"); }
                }
                """);
        SelectorIndex index = fixture.scan();
        assertEquals(0, index.coverage().filesFailed());
        String json = SelectorIndexJson.serialize(index);
        assertTrue(json.contains("<script>") || json.contains("e-mail@example.test"));
        assertTrue(json.contains("zażółć") || json.contains("e-mail@example.test"));
    }

    @Test
    void evaluatorStopsAtCyclesAndDepthLimits() throws Exception {
        Fixture fixture = fixture();
        fixture.source("sample/Cycle.java", """
                package sample;
                import org.openqa.selenium.By;
                class Cycle {
                    static final String A = B;
                    static final String B = A;
                    By value = By.id(A);
                }
                """);
        Limits limits = new Limits(1_000_000, 100, 8, 100, 3, 3, 20);
        SelectorIndex index = new JavaLocatorScanner().scan(new ScanRequest(fixture.root, List.of(fixture.sources),
                List.of(fixture.types), "JAVA_17", StandardCharsets.UTF_8, null, limits));
        assertTrue(index.files().get(0).issues().stream().anyMatch(value ->
                value.code().equals("CONSTANT_CYCLE") || value.code().equals("EVALUATION_DEPTH_LIMIT")
                        || value.code().equals("SYMBOL_UNRESOLVED")));
        assertNull(declarations(index).get(0).resolvedLocator());
    }

    @Test
    void localScopeFingerprintsSeparateSameNamesInDifferentMethodsAndOneFieldHasManyUses() throws Exception {
        Fixture fixture = fixture();
        fixture.source("sample/Scopes.java", """
                package sample;
                import org.openqa.selenium.By;
                class Scopes {
                    static final By SHARED = By.id("shared");
                    void a() { By item = By.id("a"); use(SHARED); }
                    void b() { By item = By.id("b"); use(SHARED); }
                    void use(By by) {}
                }
                """);
        List<DeclarationRecord> declarations = declarations(fixture.scan());
        assertEquals(3, declarations.size());
        assertEquals(1, declarations.stream().filter(value -> value.declarationKind() == DeclarationKind.BY_FIELD).count());
        assertEquals(2, declarations.stream().filter(value -> value.declarationKind() == DeclarationKind.BY_LOCAL)
                .map(value -> value.declaringSymbol().localScopeFingerprint()).distinct().count());
    }

    @Test
    void rejectsInvalidEncodingAndLimitsSourceRoots() throws Exception {
        Fixture fixture = fixture();
        Path invalid = fixture.sources.resolve("sample/Invalid.java");
        Files.createDirectories(invalid.getParent());
        Files.write(invalid, new byte[]{(byte) 0xc3, 0x28});
        SelectorIndex index = fixture.scan();
        assertEquals(ParseStatus.FAILED, index.files().get(0).parseStatus());
        assertTrue(index.files().get(0).issues().stream().anyMatch(value -> value.code().equals("INVALID_ENCODING")));

        Limits oneRoot = new Limits(1_000, 10, 1, 20, 10, 10, 20);
        assertThrows(IllegalArgumentException.class, () -> new JavaLocatorScanner().scan(new ScanRequest(
                fixture.root, List.of(fixture.sources, fixture.types), List.of(), "JAVA_17",
                StandardCharsets.UTF_8, null, oneRoot)));
    }

    @Test
    void blocksSymlinkEscapeWhenPlatformAllowsCreatingLinks() throws Exception {
        Fixture fixture = fixture();
        Path outside = Files.createDirectories(temporary.resolve("external-source"));
        Files.writeString(outside.resolve("Leaked.java"),
                "import org.openqa.selenium.By; class Leaked { By x=By.id(\"secret\"); }");
        Path link = fixture.sources.resolve("linked-outside");
        try {
            Files.createSymbolicLink(link, outside);
        } catch (UnsupportedOperationException | IOException | SecurityException unavailable) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false,
                    "symbolic links are unavailable in this environment: " + unavailable.getClass().getSimpleName());
        }
        SelectorIndex index = fixture.scan();
        assertTrue(index.issues().stream().anyMatch(value -> value.code().equals("PATH_ESCAPE_BLOCKED")));
        assertTrue(index.files().stream().noneMatch(value -> value.logicalPath().contains("Leaked.java")));
    }

    @Test
    void ignoresLookalikeFindByAndNonSeleniumByFactories() throws Exception {
        Fixture fixture = fixture();
        fixture.source("sample/Fakes.java", """
                package sample;
                @interface FindBy { String id(); }
                class By { static Object id(String value) { return null; } }
                class Fakes {
                    @FindBy(id="not-selenium") Object field;
                    Object value = By.id("not-selenium");
                }
                """);
        SelectorIndex index = fixture.scan();
        assertEquals(0, index.coverage().declarationsFound());
    }

    private DeclarationRecord byKind(List<DeclarationRecord> declarations, DeclarationKind kind) {
        return declarations.stream().filter(value -> value.declarationKind() == kind).findFirst().orElseThrow();
    }

    private List<String> childStrategies(DeclarationRecord declaration) {
        return declaration.locatorExpression().children().stream()
                .map(LocatorExpressionChild::resolvedLocator)
                .map(ResolvedLocator::strategy)
                .toList();
    }

    private List<DeclarationRecord> declarations(SelectorIndex index) {
        return index.files().stream().flatMap(value -> value.declarations().stream()).toList();
    }

    private Fixture fixture() throws IOException {
        Path root = Files.createDirectory(temporary.resolve("project-" + Files.list(temporary).count()));
        Path sources = Files.createDirectories(root.resolve("module/src/main/java"));
        Path types = Files.createDirectories(root.resolve("types"));
        writeTypeStubs(types);
        return new Fixture(root, sources, types);
    }

    private void writeTypeStubs(Path types) throws IOException {
        write(types, "org/openqa/selenium/By.java", """
                package org.openqa.selenium;
                public abstract class By {
                    public static By id(String value) { return null; }
                    public static By cssSelector(String value) { return null; }
                    public static By xpath(String value) { return null; }
                    public static By name(String value) { return null; }
                    public static By className(String value) { return null; }
                    public static By tagName(String value) { return null; }
                    public static By linkText(String value) { return null; }
                    public static By partialLinkText(String value) { return null; }
                }
                """);
        write(types, "org/openqa/selenium/WebElement.java",
                "package org.openqa.selenium; public interface WebElement {}");
        write(types, "org/openqa/selenium/support/How.java", """
                package org.openqa.selenium.support;
                public enum How { CLASS_NAME, CSS, ID, ID_OR_NAME, LINK_TEXT, NAME, PARTIAL_LINK_TEXT, TAG_NAME, XPATH, UNSET }
                """);
        write(types, "org/openqa/selenium/support/FindBy.java", """
                package org.openqa.selenium.support;
                public @interface FindBy {
                    How how() default How.UNSET; String using() default ""; String id() default "";
                    String name() default ""; String className() default ""; String css() default "";
                    String tagName() default ""; String linkText() default ""; String partialLinkText() default "";
                    String xpath() default "";
                }
                """);
        write(types, "org/openqa/selenium/support/FindBys.java", """
                package org.openqa.selenium.support; public @interface FindBys { FindBy[] value(); }
                """);
        write(types, "org/openqa/selenium/support/FindAll.java", """
                package org.openqa.selenium.support; public @interface FindAll { FindBy[] value(); }
                """);
    }

    private Path write(Path root, String relative, String content) throws IOException {
        Path file = root.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
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

        private Path source(String relative, String content) throws IOException {
            return write(sources, relative, content);
        }

        private SelectorIndex scan() {
            return new JavaLocatorScanner().scan(new ScanRequest(root, List.of(sources), List.of(types)));
        }
    }
}
