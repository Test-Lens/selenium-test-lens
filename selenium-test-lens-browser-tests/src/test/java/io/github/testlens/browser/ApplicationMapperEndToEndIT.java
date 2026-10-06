package io.github.testlens.browser;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.testlens.TestLens;
import io.github.testlens.application.mapper.ApplicationMapper;
import io.github.testlens.application.mapper.ApplicationMapperOptions;
import io.github.testlens.application.mapper.ApplicationOverrides;
import io.github.testlens.application.mapper.MappingObservation;
import io.github.testlens.application.model.ApplicationModel;
import io.github.testlens.application.tooling.ai.AgentContextPack;
import io.github.testlens.application.tooling.ai.AgentTask;
import io.github.testlens.application.tooling.ai.ContextSlicer;
import io.github.testlens.application.tooling.ai.ContractHeader;
import io.github.testlens.application.tooling.ai.RepairProposal;
import io.github.testlens.application.tooling.codegen.GeneratedPageObject;
import io.github.testlens.application.tooling.codegen.PageObjectGenerationOptions;
import io.github.testlens.application.tooling.codegen.PageObjectGenerator;
import io.github.testlens.application.tooling.codegen.PageObjectSourceStore;
import io.github.testlens.application.tooling.drift.ApplicationDrift;
import io.github.testlens.application.tooling.drift.ApplicationModelDiffer;
import io.github.testlens.application.tooling.json.ApplicationModelJson;
import io.github.testlens.core.redaction.RedactionPolicy;
import io.github.testlens.core.trace.UiTestLensSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Real-browser vertical contract for mapping, generation, bounded AI context, execution, and drift. */
class ApplicationMapperEndToEndIT {
    @TempDir Path temporary;

    @Test
    void mapsGeneratesCompilesExecutesAndProposesSelectorRepair() throws Exception {
        try (FixtureApplication application = FixtureApplication.start()) {
            BrowserTestHarness.ExecutorCommandMetrics wire = new BrowserTestHarness.ExecutorCommandMetrics();
            WebDriver driver = BrowserTestHarness.createDriver(wire);
            try {
                driver.get(application.url("/login"));
                wire.reset();

                ApplicationMapperOptions options = ApplicationMapperOptions.builder("Mapper browser fixture")
                        .mode(ApplicationMapperOptions.Mode.GUIDED)
                        .maxDiscoveredNodes(160)
                        .maxActionableElements(60)
                        .maxCandidateAnalyses(60)
                        .maxPages(12)
                        .maxPageStates(12)
                        .maxTransitions(24)
                        .build();
                int commandsBeforeConstruction = wire.total();
                ApplicationMapper mapper = ApplicationMapper.start(driver, options);
                assertEquals(commandsBeforeConstruction, wire.total(),
                        "creating an unused mapper must not issue a browser command");

                MappingObservation loginObservation = mapper.observe();
                ApplicationModel.ElementModel loginButton = element(mapper.model(), loginObservation.pageId(), "Log in");
                assertSelectorIntelligenceEvidence(loginButton);

                mapper.beginTransition(loginButton.elementId(), ApplicationModel.Action.CLICK);
                driver.findElement(By.cssSelector("[data-testid='login-submit']")).click();
                awaitPath(driver, "/dashboard");
                MappingObservation dashboardObservation = mapper.observe();
                ApplicationMapper contextMapper = ApplicationMapper.start(driver,
                        ApplicationMapperOptions.builder("Current context fixture").build());
                MappingObservation topContext = contextMapper.observe();
                assertFalse(topContext.elementIds().stream()
                        .map(id -> elementById(contextMapper.model(), id))
                        .filter(Objects::nonNull)
                        .anyMatch(element -> "Frame only".equals(element.accessibleName())),
                        "the top-level observation must not silently traverse an iframe");

                driver.switchTo().frame(driver.findElement(By.id("support-frame")));
                MappingObservation frameObservation = contextMapper.observe();
                assertTrue(frameObservation.elementIds().stream()
                        .map(id -> elementById(contextMapper.model(), id))
                        .filter(Objects::nonNull)
                        .anyMatch(element -> "Frame only".equals(element.accessibleName())),
                        "explicit current browsing-context selection must be observable");
                driver.switchTo().defaultContent();

                ApplicationModel.ElementModel customersLink = element(mapper.model(), dashboardObservation.pageId(), "Customers");
                mapper.beginTransition(customersLink.elementId(), ApplicationModel.Action.OPEN);
                driver.findElement(By.cssSelector("[data-testid='customers-link']")).click();
                awaitPath(driver, "/customers");
                MappingObservation customersObservation = mapper.observe();

                ApplicationModel.ElementModel firstCustomer = element(mapper.model(), customersObservation.pageId(), "Open Ada");
                mapper.beginTransition(firstCustomer.elementId(), ApplicationModel.Action.OPEN);
                driver.findElement(By.cssSelector("[data-testid='open-customer-101']")).click();
                awaitPath(driver, "/customer/101");
                MappingObservation firstDetails = mapper.observe();

                ApplicationModel.ElementModel modalButton = element(mapper.model(), firstDetails.pageId(), "Edit customer");
                mapper.beginTransition(modalButton.elementId(), ApplicationModel.Action.CLICK);
                driver.findElement(By.cssSelector("[data-testid='edit-customer']")).click();
                MappingObservation modalState = mapper.observe();
                assertNotEquals(firstDetails.stateId(), modalState.stateId(), "SPA modal must create a distinct page state");

                driver.get(application.url("/customers"));
                MappingObservation customersRevisit = mapper.observe();
                assertEquals(customersObservation.pageId(), customersRevisit.pageId());
                ApplicationModel.ElementModel secondCustomer = element(mapper.model(), customersRevisit.pageId(), "Open Grace");
                mapper.beginTransition(secondCustomer.elementId(), ApplicationModel.Action.OPEN);
                driver.findElement(By.cssSelector("[data-testid='open-customer-202']")).click();
                awaitPath(driver, "/customer/202");
                MappingObservation secondDetails = mapper.observe();
                assertEquals(firstDetails.pageId(), secondDetails.pageId(),
                        "dynamic route identifiers must resolve to one PageIdentity");

                ApplicationModel model = mapper.model();
                assertEquals(model.pages().size(), model.pages().stream().map(ApplicationModel.PageModel::pageId).distinct().count());
                assertEquals(4, model.pages().size(), "login, dashboard, customers, and customer details are distinct pages: "
                        + model.pages().stream().map(page -> page.canonicalName() + "="
                                + page.identity().normalizedUrlPattern()).toList());
                assertTrue(model.transitions().size() >= 4);
                assertTrue(model.pages().stream().flatMap(page -> page.elements().stream())
                        .anyMatch(element -> "Shadow action".equals(element.accessibleName())));
                assertTrue(model.pages().stream().flatMap(page -> page.elements().stream())
                        .anyMatch(element -> element.preferredSelector() != null
                                && element.preferredSelector().value().contains("data-testid")));

                ApplicationModelJson json = new ApplicationModelJson();
                Path modelFile = temporary.resolve("application-model.json");
                byte[] persistedModel = json.write(model);
                String persistedText = new String(persistedModel, StandardCharsets.UTF_8);
                assertFalse(persistedText.contains("must-not-be-captured"));
                assertFalse(persistedText.contains("private note"), () -> jsonContext(persistedText, "private note"));
                Files.write(modelFile, persistedModel);
                ApplicationModel restored = json.read(Files.readAllBytes(modelFile));
                assertEquals(model, restored);

                PageObjectGenerationOptions generation = PageObjectGenerationOptions.verifiedOnly(
                        "fixture.generated", temporary.resolve("generated-sources"));
                PageObjectGenerator generator = new PageObjectGenerator();
                List<GeneratedPageObject> generated = generator.generate(restored, generation);
                PageObjectSourceStore.WriteResult sources = new PageObjectSourceStore().write(generation, generated);
                Path compiled = compile(sources);

                GeneratedPageObject generatedLogin = generated.stream()
                        .filter(page -> page.pageId().equals(loginObservation.pageId()))
                        .findFirst().orElseThrow();
                GeneratedPageObject generatedDetails = generated.stream()
                        .filter(page -> page.pageId().equals(firstDetails.pageId()))
                        .findFirst().orElseThrow();
                AgentContextPack context = context(model, generatedLogin, generatedDetails);
                assertEquals(2, context.pages().size());
                assertTrue(context.excluded().stream().anyMatch(decision -> decision.kind().equals("PAGE")));
                assertFalse(context.pages().stream().anyMatch(page -> page.pageId().equals(dashboardObservation.pageId())));

                driver.get(application.url("/login"));
                executeGeneratedLogin(driver, compiled, "fixture.generated", generatedLogin, loginButton.elementId());
                awaitPath(driver, "/dashboard");

                driver.get(application.url("/login?variant=2"));
                ApplicationMapper preliminaryChangedMapper = ApplicationMapper.start(driver, options);
                MappingObservation preliminaryChanged = preliminaryChangedMapper.observe();
                String oldFingerprint = page(model, loginObservation.pageId()).provenance().attributes()
                        .get("pageObservationFingerprint");
                String changedFingerprint = page(preliminaryChangedMapper.model(), preliminaryChanged.pageId())
                        .provenance().attributes().get("pageObservationFingerprint");
                ApplicationOverrides identityOverride = new ApplicationOverrides(ApplicationOverrides.SCHEMA_VERSION,
                        Map.of(), Map.of(), Set.of(), Set.of(),
                        Map.of(oldFingerprint, "LOGIN", changedFingerprint, "LOGIN"), Map.of("LOGIN", "LoginPage"));
                ApplicationMapperOptions driftOptions = ApplicationMapperOptions.builder("Mapper browser fixture")
                        .mode(ApplicationMapperOptions.Mode.GUIDED).overrides(identityOverride).build();

                driver.get(application.url("/login"));
                ApplicationMapper oldLoginMapper = ApplicationMapper.start(driver, driftOptions);
                MappingObservation oldLoginObservation = oldLoginMapper.observe();
                ApplicationModel oldLoginModel = oldLoginMapper.model();
                ApplicationModel.ElementModel oldMappedButton = element(oldLoginModel, oldLoginObservation.pageId(), "Log in");

                driver.get(application.url("/login?variant=2"));
                ApplicationMapper changedMapper = ApplicationMapper.start(driver, driftOptions);
                MappingObservation changedLoginObservation = changedMapper.observe();
                ApplicationModel changed = changedMapper.model();
                ApplicationModel.ElementModel changedButton = element(changed, changedLoginObservation.pageId(), "Log in");
                ApplicationDrift drift = new ApplicationModelDiffer().compare(oldLoginModel, changed);
                ApplicationDrift.Change selectorChange = drift.changes().stream()
                        .filter(change -> change.type() == ApplicationDrift.ChangeType.SELECTOR_CHANGED)
                        .findFirst().orElseThrow(() -> new AssertionError("selector drift was not detected: before="
                                + selector(oldMappedButton) + ", after=" + selector(changedButton)
                                + ", changes=" + drift.changes()));
                assertEquals(oldMappedButton.elementId(), selectorChange.entityId());

                RepairProposal proposal = repair(selectorChange, oldMappedButton, changedButton);
                assertEquals(RepairProposal.ApplicationPolicy.PROPOSE_ONLY, proposal.applicationPolicy());
                assertTrue(proposal.sameTargetEvidence().contains("LIVE_VALIDATED"));

                PageObjectGenerationOptions changedGeneration = PageObjectGenerationOptions.verifiedOnly(
                        "fixture.changed", temporary.resolve("changed-sources"));
                List<GeneratedPageObject> regenerated = generator.generate(changed, changedGeneration);
                PageObjectSourceStore.WriteResult changedSources = new PageObjectSourceStore().write(changedGeneration, regenerated);
                Path changedClasses = compile(changedSources);
                GeneratedPageObject changedLogin = regenerated.stream().findFirst().orElseThrow();
                executeGeneratedLogin(driver, changedClasses, "fixture.changed", changedLogin, changedButton.elementId());
                awaitPath(driver, "/dashboard");
            } finally {
                driver.quit();
            }
        }
    }

    private static void assertSelectorIntelligenceEvidence(ApplicationModel.ElementModel element) {
        assertNotNull(element.preferredSelector());
        assertEquals(ApplicationModel.EvidenceSource.LIVE_CANDIDATE_ANALYSIS, element.preferredSelector().source());
        assertEquals(ApplicationModel.SelectorQuality.VERIFIED, element.selectorQuality());
        assertTrue(element.preferredSelector().validation().contains("VERIFIED"));
        assertTrue(element.preferredSelector().sameTarget().contains("SAME_TARGET"));
        assertTrue(element.preferredSelector().unique());
    }

    private AgentContextPack context(ApplicationModel model, GeneratedPageObject login, GeneratedPageObject details) {
        ContractHeader header = new ContractHeader(ContractHeader.SCHEMA_VERSION, ContractHeader.Status.READY,
                List.of("browser mapping"), List.of(), ContractHeader.Confidence.OBSERVED);
        AgentTask task = new AgentTask(header, AgentTask.TaskType.CREATE_TEST,
                "Verify login and opening a customer detail",
                List.of(login.pageId(), details.pageId()), List.of(), List.of(),
                List.of("Page Objects only", "Java 17"));
        Map<String,List<String>> apis = new LinkedHashMap<>();
        apis.put(login.pageId(), login.bindings().stream().map(binding -> binding.fieldName() + "()" ).toList());
        apis.put(details.pageId(), details.bindings().stream().map(binding -> binding.fieldName() + "()" ).toList());
        model.pages().stream().filter(page -> !apis.containsKey(page.pageId()))
                .forEach(page -> apis.put(page.pageId(), List.of("unrelated()")));
        return new ContextSlicer().slice(model, task, apis, List.of("JUnit 5", "No raw selectors"),
                RedactionPolicy.defaults(), new ContextSlicer.Limits(2, 100, 20, 0));
    }

    private static RepairProposal repair(ApplicationDrift.Change change, ApplicationModel.ElementModel before,
                                          ApplicationModel.ElementModel after) {
        ContractHeader header = new ContractHeader(ContractHeader.SCHEMA_VERSION, ContractHeader.Status.READY,
                change.evidence(), List.of(), ContractHeader.Confidence.LIVE_VALIDATED);
        return new RepairProposal(header, "repair-" + change.entityId(), RepairProposal.ApplicationPolicy.PROPOSE_ONLY,
                "Regenerate the mapped Page Object selector", change.reason(),
                selector(before), selector(after), List.of("LIVE_VALIDATED", "SAME_TARGET"),
                after.preferredSelector().stability(), List.of("generated login fixture"));
    }

    private static String selector(ApplicationModel.ElementModel element) {
        return element.preferredSelector().strategy() + ":" + element.preferredSelector().value();
    }

    private static void executeGeneratedLogin(WebDriver driver, Path classes, String packageName,
                                              GeneratedPageObject generated,
                                              String buttonElementId) throws Exception {
        GeneratedPageObject.ElementBinding button = generated.bindings().stream()
                .filter(binding -> binding.elementId().equals(buttonElementId)).findFirst().orElseThrow();
        GeneratedPageObject.ElementBinding username = generated.bindings().stream()
                .filter(binding -> binding.fieldName().toLowerCase().contains("username")).findFirst().orElseThrow();
        TestLens lens = TestLens.attach(driver);
        UiTestLensSession session = lens.startSession("generated page object execution");
        try (URLClassLoader loader = new URLClassLoader(new URL[]{classes.toUri().toURL()},
                ApplicationMapperEndToEndIT.class.getClassLoader())) {
            Class<?> pageType = Class.forName(packageName + "." + generated.extensionClassName(), true, loader);
            Object page = pageType.getConstructor(TestLens.class).newInstance(lens);
            Method fill = pageType.getMethod("fill" + upperFirst(username.fieldName()), String.class);
            fill.invoke(page, "mapped-user");
            Method click = pageType.getMethod("click" + upperFirst(button.fieldName()));
            click.invoke(page);
            lens.finishPassed();
            assertTrue(session.events().size() > 3, "generated Lens-native methods must leave runtime trace evidence");
        } catch (Exception failure) {
            lens.finishFailed(failure);
            throw failure;
        }
    }

    private Path compile(PageObjectSourceStore.WriteResult sources) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "browser E2E requires a JDK, not a JRE");
        Path classes = Files.createTempDirectory(temporary, "compiled-");
        List<String> arguments = new ArrayList<>(List.of("-classpath", System.getProperty("java.class.path"),
                "-d", classes.toString()));
        sources.generatedBases().forEach(path -> arguments.add(path.toString()));
        sources.createdExtensions().forEach(path -> arguments.add(path.toString()));
        assertEquals(0, compiler.run(null, null, null, arguments.toArray(String[]::new)),
                "generated Page Objects must compile against the current consumer classpath");
        return classes;
    }

    private static ApplicationModel.ElementModel element(ApplicationModel model, String pageId, String accessibleName) {
        return page(model, pageId).elements().stream()
                .filter(element -> accessibleName.equals(element.accessibleName()))
                .findFirst().orElseThrow(() -> new AssertionError("Missing element " + accessibleName));
    }

    private static ApplicationModel.ElementModel elementById(ApplicationModel model, String elementId) {
        return model.pages().stream().flatMap(page -> page.elements().stream())
                .filter(element -> element.elementId().equals(elementId)).findFirst().orElse(null);
    }

    private static ApplicationModel.PageModel page(ApplicationModel model, String pageId) {
        return model.pages().stream().filter(page -> page.pageId().equals(pageId)).findFirst().orElseThrow();
    }

    private static void awaitPath(WebDriver driver, String path) {
        new WebDriverWait(driver, Duration.ofSeconds(5)).until(webDriver ->
                new URLValue(webDriver.getCurrentUrl()).path().equals(path));
    }

    private static String upperFirst(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static String jsonContext(String document, String value) {
        int match = document.indexOf(value);
        if (match < 0) return "value absent";
        return document.substring(Math.max(0, match - 160), Math.min(document.length(), match + value.length() + 160));
    }

    private record URLValue(String value) {
        String path() {
            try { return java.net.URI.create(value).getPath(); }
            catch (IllegalArgumentException ignored) { return ""; }
        }
    }

    private record FixtureApplication(HttpServer server, String origin) implements AutoCloseable {
        static FixtureApplication start() throws IOException {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            FixtureApplication fixture = new FixtureApplication(server,
                    "http://127.0.0.1:" + server.getAddress().getPort());
            server.createContext("/", fixture::handle);
            server.start();
            return fixture;
        }

        String url(String path) { return origin + path; }

        private void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String query = exchange.getRequestURI().getRawQuery();
            String body;
            if ("/login".equals(path)) body = login("variant=2".equals(query));
            else if ("/dashboard".equals(path)) body = dashboard();
            else if ("/customers".equals(path)) body = customers();
            else if (path.startsWith("/customer/")) body = customer(path.substring("/customer/".length()));
            else if ("/support-frame".equals(path)) body = page("Support frame", "<button aria-label='Frame only'>Frame only</button>");
            else { exchange.sendResponseHeaders(404, -1); exchange.close(); return; }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream output = exchange.getResponseBody()) { output.write(bytes); }
        }

        private String login(boolean changed) {
            String selector = changed ? "login-submit-v2" : "login-submit";
            String generatedId = changed ? "react-select-18-input" : "react-select-17-input";
            return page("Login", """
                    <main aria-label='Login region'>
                      <form aria-label='Login form' onsubmit="location.href='/dashboard';return false">
                        <label for='username'>Username</label><input id='username' data-testid='login-username'>
                        <label for='password'>Password</label><input id='password' type='password' value='must-not-be-captured'>
                        <button id='%s' data-testid='%s' aria-label='Log in'>Log in</button>
                      </form>
                    </main>
                    """.formatted(generatedId, selector));
        }

        private String dashboard() {
            return page("Dashboard", """
                    <nav aria-label='Primary navigation'><a data-testid='customers-link' aria-label='Customers' href='/customers'>Customers</a></nav>
                    <section aria-label='Dashboard widgets'><button aria-label='Refresh widgets'>Refresh</button></section>
                    <mapper-shadow></mapper-shadow>
                    <iframe id='support-frame' title='Support' src='/support-frame'></iframe>
                    <script>customElements.define('mapper-shadow',class extends HTMLElement{connectedCallback(){
                      this.attachShadow({mode:'open'}).innerHTML=`<button data-testid="shadow-action" aria-label="Shadow action">Shadow</button>`;
                    }});</script>
                    """);
        }

        private String customers() {
            return page("Customers", """
                    <main aria-label='Customers region'>
                      <form role='search' aria-label='Customer search'><label for='search'>Search</label><input id='search'></form>
                      <table aria-label='Customer table'><tbody>
                        <tr><td>Ada</td><td><a data-testid='open-customer-101' aria-label='Open Ada' href='/customer/101'>Open</a></td></tr>
                        <tr><td>Grace</td><td><a data-testid='open-customer-202' aria-label='Open Grace' href='/customer/202'>Open</a></td></tr>
                      </tbody></table>
                    </main>
                    """);
        }

        private String customer(String id) {
            return page("Customer details", """
                    <main aria-label='Customer details region'>
                      <form aria-label='Customer form'><label for='name'>Name</label><input id='name' value='Customer %s'></form>
                      <button data-testid='edit-customer' aria-label='Edit customer' onclick="const d=document.getElementById('editor');d.hidden=false;d.setAttribute('open','')">Edit</button>
                      <dialog id='editor' aria-label='Edit dialog' hidden><label for='note'>Note</label><textarea id='note'>private note</textarea></dialog>
                    </main>
                    """.formatted(id));
        }

        private static String page(String title, String content) {
            return "<!doctype html><html><head><meta charset='utf-8'><title>" + title
                    + "</title></head><body>" + content + "</body></html>";
        }

        @Override public void close() { server.stop(0); }
    }
}
