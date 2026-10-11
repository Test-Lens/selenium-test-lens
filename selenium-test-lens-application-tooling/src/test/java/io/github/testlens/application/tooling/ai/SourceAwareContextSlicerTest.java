package io.github.testlens.application.tooling.ai;

import io.github.testlens.application.tooling.ToolingFixtures;
import io.github.testlens.application.tooling.source.PageObjectCorrelation;
import io.github.testlens.core.redaction.RedactionPolicy;
import io.github.testlens.selector.tooling.ExistingProjectIndex;
import io.github.testlens.selector.tooling.ExistingProjectIndexer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceAwareContextSlicerTest {
    private static final ContractHeader HEADER = new ContractHeader(1, ContractHeader.Status.READY,
            List.of("test"), List.of(), ContractHeader.Confidence.OBSERVED);

    @Test
    void includesOnlyCorrelatedLoginSourceAndNeverProjectsRawSelectorValue() {
        AgentTask task = new AgentTask(HEADER, AgentTask.TaskType.CREATE_TEST, "Test invalid login",
                List.of("login-page"), List.of(), List.of("login"), List.of());

        AgentContextPack pack = new ContextSlicer().slice(ToolingFixtures.model(), task, Map.of(), List.of(),
                RedactionPolicy.defaults(), new ContextSlicer.Limits(1, 20, 10, 0), index(), correlation(),
                ContextSlicer.SourceLimits.defaults());

        assertEquals(List.of("class-login"), pack.source().classes().stream()
                .map(AgentContextPack.PageObjectClassContext::classId).toList());
        assertEquals(List.of("decl-login"), pack.source().declarations().stream()
                .map(AgentContextPack.SourceDeclarationContext::declarationRef).toList());
        assertEquals(List.of("test-login"), pack.source().tests().stream()
                .map(AgentContextPack.ExistingTestContext::testId).toList());
        assertTrue(pack.source().methods().stream().allMatch(method -> method.ownerClassId().equals("class-login")));
        assertFalse(pack.source().classes().stream().anyMatch(value -> value.qualifiedName().contains("Customers")));

        String prompt = new PromptPackRenderer().render(PromptPackRenderer.Role.TEST_IMPLEMENTER, pack, 100_000);
        assertFalse(prompt.contains("[data-super-secret='raw-login-selector']"));
        assertTrue(prompt.contains(ExistingProjectIndexer.selectorValueFingerprint(
                "[data-super-secret='raw-login-selector']")));
    }

    @Test
    void reportsPartialContextWhenSourceMethodBudgetIsReached() {
        AgentTask task = new AgentTask(HEADER, AgentTask.TaskType.CREATE_TEST, "Test invalid login",
                List.of("login-page"), List.of(), List.of("login"), List.of());

        AgentContextPack pack = new ContextSlicer().slice(ToolingFixtures.model(), task, Map.of(), List.of(),
                RedactionPolicy.defaults(), new ContextSlicer.Limits(1, 20, 10, 0), index(), correlation(),
                new ContextSlicer.SourceLimits(2, 1, 2, 2, 100_000));

        assertEquals(1, pack.source().methods().size());
        assertEquals(AgentContextPack.Completeness.PARTIAL, pack.completeness());
        assertEquals(ContractHeader.Status.PARTIAL, pack.header().status());
        assertTrue(pack.excluded().stream().anyMatch(value -> value.kind().equals("PAGE_OBJECT_METHOD")
                && value.reason().equals("maxMethods reached")));
        assertEquals(AgentContextPack.ManifestCompleteness.PARTIAL,
                pack.pageObjectCapabilities().completeness());
        assertTrue(pack.pageObjectCapabilities().limitations().contains("maxMethods reached"));
    }

    @Test
    void exposesACompleteDeterministicPublicCapabilityManifestWithoutRawSource() {
        AgentContextPack pack = slice(index(), correlation(), ContextSlicer.SourceLimits.defaults());
        AgentContextPack.PageObjectCapabilityManifest manifest = pack.pageObjectCapabilities();

        assertEquals("project-fingerprint", manifest.sourceFingerprint());
        assertEquals(AgentContextPack.ManifestCompleteness.COMPLETE, manifest.completeness());
        assertTrue(manifest.limitations().isEmpty());
        assertEquals(List.of("example.LoginPage"), manifest.classes().stream()
                .map(AgentContextPack.PageObjectCapability::qualifiedName).toList());

        AgentContextPack.PageObjectCapability login = manifest.classes().get(0);
        assertEquals("src/test/java/LoginPage.java", login.logicalPath());
        assertEquals("PUBLIC", login.accessibility());
        assertEquals(List.of("LoginPage(String)", "LoginPage(WebDriver)"), login.constructors().stream()
                .map(AgentContextPack.ConstructorCapability::signature).toList());
        AgentContextPack.ConstructorCapability direct = login.constructors().stream()
                .filter(value -> value.signature().equals("LoginPage(WebDriver)")).findFirst().orElseThrow();
        assertEquals("DIRECT_CONSTRUCTION", direct.acquisition());
        assertEquals(List.of(new AgentContextPack.CapabilityParameter("driver", "org.openqa.selenium.WebDriver")), direct.parameters());
        AgentContextPack.ConstructorCapability protectedConstructor = login.constructors().stream()
                .filter(value -> value.signature().equals("LoginPage(String)")).findFirst().orElseThrow();
        assertEquals("PROTECTED", protectedConstructor.accessibility());
        assertNull(protectedConstructor.acquisition(), "hidden construction must not be advertised as callable");

        assertEquals(List.of("healthStatus()", "login(String, String)", "loginErrorHidden()",
                        "loginErrorVisible()", "open(WebDriver)"),
                login.methods().stream().map(AgentContextPack.MethodCapability::signature).toList(),
                "all public methods, including locator-independent workflows, must be present and ordered");
        assertFalse(login.methods().stream().anyMatch(value -> value.name().equals("internalSecret")));

        AgentContextPack.MethodCapability loginMethod = login.methods().stream()
                .filter(value -> value.name().equals("login")).findFirst().orElseThrow();
        assertEquals(List.of(new AgentContextPack.CapabilityParameter("username", "String"),
                new AgentContextPack.CapabilityParameter("password", "String")), loginMethod.parameters());
        assertEquals("LoginPage", loginMethod.returnType());
        assertFalse(loginMethod.staticMethod());
        assertEquals(List.of("login"), loginMethod.applicationElementIds());
        assertTrue(login.methods().stream().filter(value -> value.name().startsWith("loginError"))
                .map(AgentContextPack.MethodCapability::name).toList()
                .containsAll(List.of("loginErrorVisible", "loginErrorHidden")),
                "similar names with different semantics must not be merged");
        assertTrue(login.methods().stream().filter(value -> value.name().equals("open"))
                .findFirst().orElseThrow().staticMethod());
        assertTrue(login.methods().stream().anyMatch(value -> value.name().equals("healthStatus")
                && value.applicationElementIds().isEmpty()),
                "public methods do not need to reference a locator to be callable");

        String rendered = new PromptPackRenderer().render(PromptPackRenderer.Role.TEST_IMPLEMENTER, pack, 100_000);
        assertFalse(rendered.contains("[data-super-secret='raw-login-selector']"));
        assertFalse(rendered.contains("return true"), "method bodies must not enter the capability manifest");
    }

    @Test
    void reportsPartialManifestForCharacterAndClassBounds() {
        ExistingProjectIndex longIndex = withLongPublicMethod(index());
        AgentContextPack characters = slice(longIndex, correlation(), new ContextSlicer.SourceLimits(2, 20, 2, 2, 512));
        assertEquals(AgentContextPack.ManifestCompleteness.PARTIAL,
                characters.pageObjectCapabilities().completeness());
        assertTrue(characters.pageObjectCapabilities().limitations().contains("maxSerializedCharacters reached"));
        assertEquals(AgentContextPack.Completeness.PARTIAL, characters.completeness());

        AgentContextPack classes = slice(index(), twoTrustedLoginClasses(),
                new ContextSlicer.SourceLimits(1, 20, 2, 2, 100_000));
        assertEquals(1, classes.pageObjectCapabilities().classes().size());
        assertEquals(AgentContextPack.ManifestCompleteness.PARTIAL,
                classes.pageObjectCapabilities().completeness());
        assertTrue(classes.excluded().stream().anyMatch(value -> value.kind().equals("PAGE_OBJECT_CLASS")
                && value.reason().equals("maxClasses reached")));
    }

    @Test
    void reportsPartialManifestWhenNoTrustedCorrelatedClassExists() {
        AgentContextPack pack = slice(index(), untrustedCorrelation(), ContextSlicer.SourceLimits.defaults());

        assertTrue(pack.pageObjectCapabilities().classes().isEmpty());
        assertEquals(AgentContextPack.ManifestCompleteness.PARTIAL,
                pack.pageObjectCapabilities().completeness());
        assertTrue(pack.pageObjectCapabilities().limitations().contains(
                "No trusted correlated Page Object class is available for the requested scope"));
    }

    @Test
    void unrelatedGlobalCorrelationGapsDoNotDowngradeCompleteSelectedCapabilities() {
        AgentContextPack pack = slice(index(), partialCorrelation(), ContextSlicer.SourceLimits.defaults());

        assertEquals(AgentContextPack.ManifestCompleteness.COMPLETE,
                pack.pageObjectCapabilities().completeness());
        assertTrue(pack.pageObjectCapabilities().limitations().isEmpty());
        assertTrue(pack.pageObjectCapabilities().classes().stream()
                .flatMap(value -> value.methods().stream())
                .anyMatch(value -> value.signature().equals("login(String, String)")));
    }

    @Test
    void unrelatedNonStructuralIndexLimitationDoesNotDowngradeCompleteSelectedCapabilities() {
        ExistingProjectIndex original = index();
        ExistingProjectIndex partial = new ExistingProjectIndex(original.schemaVersion(),
                original.projectFingerprint(), original.sourceFiles(), original.classes(), original.elements(),
                original.methods(), original.tests(), original.edges(), ExistingProjectIndex.Completeness.PARTIAL,
                List.of(new ExistingProjectIndex.Limitation("UNRESOLVED_DYNAMIC_CALL",
                        "src/test/java/UnrelatedHelper.java", "unrelated method call")), original.metrics());

        AgentContextPack pack = slice(partial, correlation(), ContextSlicer.SourceLimits.defaults());

        assertEquals(AgentContextPack.Completeness.PARTIAL, pack.completeness());
        assertEquals(AgentContextPack.ManifestCompleteness.COMPLETE,
                pack.pageObjectCapabilities().completeness());
        assertEquals("PARTIAL", pack.pageObjectCapabilities().provenance().sourceIndexCompleteness());
        assertTrue(pack.pageObjectCapabilities().provenance().partialReasons().isEmpty());
    }

    @Test
    void unrelatedApplicationContextGapsDoNotDowngradeCompleteSelectedCapabilities() {
        AgentTask task = new AgentTask(HEADER, AgentTask.TaskType.CREATE_TEST, "Test invalid login",
                List.of("login-page"), List.of(), List.of("login", "unobserved-element"), List.of());

        AgentContextPack pack = new ContextSlicer().slice(ToolingFixtures.model(), task, Map.of(), List.of(),
                RedactionPolicy.defaults(), ContextSlicer.Limits.defaults(), index(), correlation(),
                ContextSlicer.SourceLimits.defaults());

        assertEquals(AgentContextPack.Completeness.PARTIAL, pack.completeness());
        assertEquals(AgentContextPack.ManifestCompleteness.COMPLETE,
                pack.pageObjectCapabilities().completeness());
        assertTrue(pack.pageObjectCapabilities().limitations().isEmpty());
        assertTrue(pack.pageObjectCapabilities().classes().stream()
                .flatMap(value -> value.methods().stream())
                .anyMatch(value -> value.signature().equals("login(String, String)")));
    }

    private static AgentContextPack slice(ExistingProjectIndex index, PageObjectCorrelation correlation,
                                          ContextSlicer.SourceLimits sourceLimits) {
        AgentTask task = new AgentTask(HEADER, AgentTask.TaskType.CREATE_TEST, "Test invalid login",
                List.of("login-page"), List.of(), List.of("login"), List.of());
        return new ContextSlicer().slice(ToolingFixtures.model(), task, Map.of(), List.of(),
                RedactionPolicy.defaults(), new ContextSlicer.Limits(1, 20, 10, 0), index, correlation,
                sourceLimits);
    }

    @Test
    void exposesInheritedPublicCapabilitiesAndKeepsLegacyIndexEntriesCallable() {
        ExistingProjectIndex original=index();
        var base=new ExistingProjectIndex.ClassEntry("class-base","src/test/java/BasePage.java","example.BasePage",
                "BasePage",ExistingProjectIndex.ClassClassification.BASE_PAGE,
                ExistingProjectIndex.Origin.HAND_WRITTEN,List.of());
        var child=new ExistingProjectIndex.ClassEntry("class-login","src/test/java/LoginPage.java","example.LoginPage",
                "LoginPage",ExistingProjectIndex.ClassClassification.PAGE_OBJECT,
                ExistingProjectIndex.Origin.HAND_WRITTEN,List.of("BasePage"),ExistingProjectIndex.Accessibility.PUBLIC,
                original.classes().stream().filter(value->value.id().equals("class-login")).findFirst().orElseThrow().constructors());
        var inherited=method("method-inherited","class-base","authenticated()",List.of(),"boolean",false,
                ExistingProjectIndex.Accessibility.PUBLIC,List.of());
        var classes=new java.util.ArrayList<>(original.classes().stream().filter(value->!value.id().equals("class-login")).toList());
        classes.add(base);classes.add(child);
        var methods=new java.util.ArrayList<>(original.methods());methods.add(inherited);
        var hierarchical=new ExistingProjectIndex(original.schemaVersion(),original.projectFingerprint(),original.sourceFiles(),
                classes,original.elements(),methods,original.tests(),original.edges(),original.completeness(),
                original.limitations(),original.metrics());

        AgentContextPack pack=slice(hierarchical,correlation(),ContextSlicer.SourceLimits.defaults());
        var login=pack.pageObjectCapabilities().classes().stream().filter(value->value.classId().equals("class-login"))
                .findFirst().orElseThrow();
        assertTrue(login.methods().stream().anyMatch(value->value.name().equals("authenticated")));
        assertEquals(ExistingProjectIndex.Accessibility.PUBLIC,base.accessibility(),
                "the legacy ClassEntry constructor preserves the pre-manifest callable surface");
        assertEquals(ExistingProjectIndex.Accessibility.PUBLIC,
                method("legacy","class-base","legacy()",List.of()).accessibility());
    }

    private static ExistingProjectIndex index() {
        var loginClass = publicClazz("class-login", "example.LoginPage", "LoginPage", List.of(
                constructor("LoginPage(WebDriver)", "driver", "org.openqa.selenium.WebDriver", ExistingProjectIndex.Accessibility.PUBLIC),
                constructor("LoginPage(String)", "profile", "String", ExistingProjectIndex.Accessibility.PROTECTED)));
        var customersClass = publicClazz("class-customers", "example.CustomersPage", "CustomersPage", List.of(
                new ExistingProjectIndex.ConstructorEntry("CustomersPage()", List.of(),
                        ExistingProjectIndex.Accessibility.PUBLIC, true)));
        var loginElement = element("source-login", "class-login", "loginButton", "decl-login",
                "[data-super-secret='raw-login-selector']");
        var customersElement = element("source-customers", "class-customers", "search", "decl-customers",
                "[data-testid='customer-search']");
        var loginMethod = method("method-login", "class-login", "login(String, String)",
                List.of(new ExistingProjectIndex.Parameter("username", "String"),
                        new ExistingProjectIndex.Parameter("password", "String")), "LoginPage", false,
                ExistingProjectIndex.Accessibility.PUBLIC, List.of("decl-login"));
        var visibleMethod = method("method-visible", "class-login", "loginErrorVisible()", List.of(),
                "boolean", false, ExistingProjectIndex.Accessibility.PUBLIC, List.of("decl-login"));
        var hiddenMethod = method("method-hidden", "class-login", "loginErrorHidden()", List.of(),
                "boolean", false, ExistingProjectIndex.Accessibility.PUBLIC, List.of("decl-login"));
        var healthMethod = method("method-health", "class-login", "healthStatus()", List.of(),
                "boolean", false, ExistingProjectIndex.Accessibility.PUBLIC, List.of());
        var openMethod = method("method-open", "class-login", "open(WebDriver)",
                List.of(new ExistingProjectIndex.Parameter("driver", "WebDriver")), "LoginPage", true,
                ExistingProjectIndex.Accessibility.PUBLIC, List.of());
        var secretMethod = method("method-secret", "class-login", "internalSecret()", List.of(),
                "void", false, ExistingProjectIndex.Accessibility.PRIVATE, List.of());
        var customersMethod = method("method-search", "class-customers", "search(String)",
                List.of(new ExistingProjectIndex.Parameter("query", "String")), "void", false,
                ExistingProjectIndex.Accessibility.PUBLIC, List.of("decl-customers"));
        var loginTest = new ExistingProjectIndex.TestEntry("test-login", "class-login-test", "test-method-login",
                ExistingProjectIndex.TestFramework.JUNIT5, List.of("login"), List.of(), List.of("login"));
        var customerTest = new ExistingProjectIndex.TestEntry("test-customers", "class-customer-test", "test-method-customers",
                ExistingProjectIndex.TestFramework.JUNIT5, List.of("customers"), List.of(), List.of("search"));
        return new ExistingProjectIndex(1, "project-fingerprint", List.of(loginClass, customersClass),
                List.of(loginElement, customersElement), List.of(loginMethod, visibleMethod, hiddenMethod,
                        healthMethod, openMethod, secretMethod, customersMethod),
                List.of(loginTest, customerTest), List.of(
                new ExistingProjectIndex.Edge("edge-login-test", ExistingProjectIndex.EdgeType.TEST_TO_METHOD,
                        "test-login", "method-login"),
                new ExistingProjectIndex.Edge("edge-customer-test", ExistingProjectIndex.EdgeType.TEST_TO_METHOD,
                        "test-customers", "method-search")
        ), ExistingProjectIndex.Completeness.COMPLETE, List.of(),
                new ExistingProjectIndex.Metrics(1, 4, 2, 2, 2, 7, 2, 2, 4));
    }

    private static PageObjectCorrelation correlation() {
        return new PageObjectCorrelation(1, List.of(
                new PageObjectCorrelation.ClassCorrelation("login-page", "class-login",
                        PageObjectCorrelation.State.STRONG, List.of(PageObjectCorrelation.Evidence.SAME_PAGE_IDENTITY),
                        List.of(), List.of()),
                new PageObjectCorrelation.ClassCorrelation("dashboard-page", "class-customers",
                        PageObjectCorrelation.State.STRONG, List.of(PageObjectCorrelation.Evidence.SAME_PAGE_IDENTITY),
                        List.of(), List.of())
        ), List.of(
                new PageObjectCorrelation.ElementCorrelation("login-page", "login", "source-login", "decl-login",
                        PageObjectCorrelation.State.STRONG,
                        List.of(PageObjectCorrelation.Evidence.NORMALIZED_SELECTOR_MATCH), List.of(), List.of()),
                new PageObjectCorrelation.ElementCorrelation("dashboard-page", "menu", "source-customers", "decl-customers",
                        PageObjectCorrelation.State.STRONG,
                        List.of(PageObjectCorrelation.Evidence.NORMALIZED_SELECTOR_MATCH), List.of(), List.of())
        ), PageObjectCorrelation.Completeness.COMPLETE, List.of(),
                new PageObjectCorrelation.Metrics(2, 2, 2, 0, 2, 0, 0, 0, 0));
    }

    private static ExistingProjectIndex.ClassEntry clazz(String id, String qualifiedName, String simpleName) {
        return new ExistingProjectIndex.ClassEntry(id, "src/test/java/" + simpleName + ".java", qualifiedName,
                simpleName, ExistingProjectIndex.ClassClassification.PAGE_OBJECT,
                ExistingProjectIndex.Origin.HAND_WRITTEN, List.of());
    }

    private static ExistingProjectIndex.ClassEntry publicClazz(String id, String qualifiedName, String simpleName,
                                                                List<ExistingProjectIndex.ConstructorEntry> constructors) {
        return new ExistingProjectIndex.ClassEntry(id, "src/test/java/" + simpleName + ".java", qualifiedName,
                simpleName, ExistingProjectIndex.ClassClassification.PAGE_OBJECT,
                ExistingProjectIndex.Origin.HAND_WRITTEN, List.of(), ExistingProjectIndex.Accessibility.PUBLIC,
                constructors);
    }

    private static ExistingProjectIndex.ConstructorEntry constructor(String signature, String parameterName,
                                                                      String parameterType,
                                                                      ExistingProjectIndex.Accessibility accessibility) {
        return new ExistingProjectIndex.ConstructorEntry(signature,
                List.of(new ExistingProjectIndex.Parameter(parameterName, parameterType)), accessibility, false);
    }

    private static ExistingProjectIndex.ElementEntry element(String id, String owner, String name,
                                                               String declaration, String rawValue) {
        return new ExistingProjectIndex.ElementEntry(id, owner, name, declaration, "css",
                new ExistingProjectIndex.ValueProjection("RESOLVED",
                        ExistingProjectIndexer.selectorValueFingerprint(rawValue)),
                new ExistingProjectIndex.SourceRange(1, 1, 1, 20, 0, 19));
    }

    private static ExistingProjectIndex.MethodEntry method(String id, String owner, String signature,
                                                            List<String> declarations) {
        return new ExistingProjectIndex.MethodEntry(id, owner, signature.substring(0, signature.indexOf('(')),
                signature, List.of(), "void", List.of("CLICK"), declarations, List.of(),
                ExistingProjectIndex.MethodClassification.ACTION);
    }

    private static ExistingProjectIndex.MethodEntry method(String id, String owner, String signature,
                                                            List<ExistingProjectIndex.Parameter> parameters,
                                                            String returnType, boolean staticMethod,
                                                            ExistingProjectIndex.Accessibility accessibility,
                                                            List<String> declarations) {
        return new ExistingProjectIndex.MethodEntry(id, owner, signature.substring(0, signature.indexOf('(')),
                signature, parameters, returnType, List.of(), declarations, List.of(),
                ExistingProjectIndex.MethodClassification.ACTION, accessibility, staticMethod);
    }

    private static ExistingProjectIndex withLongPublicMethod(ExistingProjectIndex original) {
        String longType = "VeryLongCapabilityType".repeat(24);
        var methods = new java.util.ArrayList<>(original.methods());
        methods.add(method("method-long", "class-login", "describe(" + longType + ")",
                List.of(new ExistingProjectIndex.Parameter("value", longType)), longType, false,
                ExistingProjectIndex.Accessibility.PUBLIC, List.of()));
        return new ExistingProjectIndex(original.schemaVersion(), original.projectFingerprint(), original.sourceFiles(),
                original.classes(), original.elements(), methods, original.tests(), original.edges(),
                original.completeness(), original.limitations(), original.metrics());
    }

    private static PageObjectCorrelation twoTrustedLoginClasses() {
        PageObjectCorrelation base = correlation();
        return new PageObjectCorrelation(base.schemaVersion(), List.of(
                new PageObjectCorrelation.ClassCorrelation("login-page", "class-login",
                        PageObjectCorrelation.State.STRONG, List.of(PageObjectCorrelation.Evidence.SAME_PAGE_IDENTITY),
                        List.of(), List.of()),
                new PageObjectCorrelation.ClassCorrelation("login-page", "class-customers",
                        PageObjectCorrelation.State.STRONG, List.of(PageObjectCorrelation.Evidence.SAME_PAGE_IDENTITY),
                        List.of(), List.of())), base.elements(), base.completeness(), base.limitations(), base.metrics());
    }

    private static PageObjectCorrelation untrustedCorrelation() {
        return new PageObjectCorrelation(1, List.of(
                new PageObjectCorrelation.ClassCorrelation("login-page", "class-login",
                        PageObjectCorrelation.State.PROBABLE, List.of(), List.of(), List.of())), List.of(),
                PageObjectCorrelation.Completeness.COMPLETE, List.of(),
                new PageObjectCorrelation.Metrics(1, 1, 0, 1, 0, 0, 0, 0, 0));
    }

    private static PageObjectCorrelation partialCorrelation() {
        PageObjectCorrelation base = correlation();
        return new PageObjectCorrelation(base.schemaVersion(), base.classes(), base.elements(),
                PageObjectCorrelation.Completeness.PARTIAL, List.of("Unrelated page remains unmatched"),
                base.metrics());
    }
}
