package io.github.testlens.studio;

import io.github.testlens.application.model.ApplicationModel;
import io.github.testlens.application.tooling.ai.AgentContextPack;
import io.github.testlens.application.tooling.ai.ContractHeader;
import io.github.testlens.application.tooling.ai.RepairProposal;
import io.github.testlens.application.tooling.ai.TestPlan;
import io.github.testlens.application.tooling.ai.workflow.TestEngineeringRun;
import io.github.testlens.application.tooling.ai.workflow.WorkflowReport;
import io.github.testlens.application.tooling.ai.workflow.TrustedRepairApplier;
import io.github.testlens.application.tooling.json.ApplicationModelJson;
import io.github.testlens.application.tooling.json.StrictJson;
import io.github.testlens.application.tooling.source.PageObjectCorrelator;
import io.github.testlens.selector.tooling.ExistingProjectIndex;
import io.github.testlens.selector.tooling.ExistingProjectIndexer;
import io.github.testlens.studio.workspace.StudioWorkspaceStore;
import io.github.testlens.studio.browser.Ownership;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class StudioRepairProjectionRefreshTest {
    @TempDir Path root;

    @Test void appliedRepairRefreshesCorrelationBeforeNextPlanningAndKeepsRerun() throws Exception {
        Path sourceRoot = root.resolve("src/test/java");
        Files.createDirectories(sourceRoot);
        Path source = sourceRoot.resolve("LoginPage.java");
        Files.writeString(source, "package example; import org.openqa.selenium.By; public final class LoginPage { "
                + "private final By loginButton = By.id(\"old-login\"); public LoginPage(){} public boolean login(){ return true; } }");
        ExistingProjectIndex initial = index(sourceRoot);
        StudioWorkspaceStore store = new StudioWorkspaceStore(root);
        store.write(StudioWorkspaceStore.ArtifactKind.APPLICATION_MODEL,
                StrictJson.readObject(new ApplicationModelJson().write(model())), initial.projectFingerprint());
        CapturingGateway gateway = new CapturingGateway();

        try (TestEngineeringStudioService service = new TestEngineeringStudioService(
                configuration(sourceRoot), gateway, (java.util.function.Supplier<org.openqa.selenium.WebDriver>) null)) {
            assertTrue(service.correlations(0, 100).total() > 0, "fixture must begin correlated");
            String firstRun = service.createRequirement("Invalid password should show an error");
            RepairProposal proposal = proposal(initial);
            gateway.repair = proposal;
            service.registerRepairProposal(firstRun, proposal);

            var applied = service.approveRepair(firstRun, proposal.proposalId());

            assertEquals("APPROVED", applied.decision());
            assertEquals("APPLIED", applied.applyStatus());
            ExistingProjectIndex repaired = index(sourceRoot);
            assertNotEquals(initial.projectFingerprint(), repaired.projectFingerprint());
            assertEquals(StudioWorkspaceStore.Freshness.FRESH,
                    store.freshness(StudioWorkspaceStore.ArtifactKind.CORRELATIONS,
                            repaired.projectFingerprint()),
                    "persisted correlations must describe the repaired source index");

            service.refreshProject();
            String secondRun = service.createRequirement("Use the repaired login button");
            service.generatePlan(secondRun);

            assertNotNull(gateway.preparedContext);
            String repairedFingerprint = ExistingProjectIndexer.selectorValueFingerprint(
                    "[data-testid='login-submit']");
            assertTrue(gateway.preparedContext.source().declarations().stream()
                    .anyMatch(value -> repairedFingerprint.equals(value.selectorValueFingerprint())),
                    "new planning must use declarations correlated to the repaired selector");
            assertTrue(service.workflowHistory().stream()
                    .anyMatch(value -> value.runId().equals(firstRun)));

            var rerun = service.rerun(firstRun);
            assertEquals("SUCCESS", rerun.finalState());
            assertEquals(1, gateway.reruns);
        }
    }

    @Test void appliedWriteRemainsRecordedWhenProjectionRefreshFailsAndOldCorrelationIsBlocked() throws Exception {
        Path sourceRoot=root.resolve("src/test/java");Files.createDirectories(sourceRoot);
        Path source=sourceRoot.resolve("LoginPage.java");
        Files.writeString(source,"import org.openqa.selenium.By; final class LoginPage { private final By loginButton = By.id(\"old-login\"); }");
        ExistingProjectIndex initial=index(sourceRoot);StudioWorkspaceStore store=new StudioWorkspaceStore(root);
        store.write(StudioWorkspaceStore.ArtifactKind.APPLICATION_MODEL,
                StrictJson.readObject(new ApplicationModelJson().write(model())),initial.projectFingerprint());
        ExistingProjectIndexer delegate=new ExistingProjectIndexer();AtomicInteger indexes=new AtomicInteger();
        TestEngineeringStudioService.ProjectIndexer failing=request->{
            if(indexes.incrementAndGet()==2)throw new IllegalStateException("controlled reindex failure");
            return delegate.index(request);
        };
        CapturingGateway gateway=new CapturingGateway();
        try(TestEngineeringStudioService service=new TestEngineeringStudioService(configuration(sourceRoot),gateway,null,
                Ownership.STUDIO_OWNED,failing,new PageObjectCorrelator(),new TrustedRepairApplier(),store)){
            String runId=service.createRequirement("repair selector");RepairProposal proposal=proposal(initial);gateway.repair=proposal;
            service.registerRepairProposal(runId,proposal);

            var result=service.approveRepair(runId,proposal.proposalId());

            assertEquals("APPROVED",result.decision());assertEquals("APPLIED",result.applyStatus());
            assertTrue(Files.readString(source).contains("login-submit"),"source write already succeeded");
            assertEquals("STALE",service.workflow(runId).freshness());
            assertTrue(service.workflow(runId).limitations().stream().anyMatch(value->value.startsWith("PROJECT_PROJECTION_REFRESH_FAILED:")));
            assertEquals(0,service.correlations(0,100).total(),"old correlation must be unavailable");
            assertThrows(IllegalStateException.class,service::correlate,
                    "a failed reindex must not permit correlation against the pre-repair index");
            assertTrue(service.projectOverview().stage().limitations().stream().anyMatch(value->value.code().equals("PROJECT_PROJECTION_REFRESH_FAILED")));
            assertTrue(service.repairHistory().stream().anyMatch(value->"APPLIED".equals(value.applyStatus())));
            String next=service.createRequirement("new plan after failed refresh");
            IllegalArgumentException blocked=assertThrows(IllegalArgumentException.class,()->service.generatePlan(next));
            assertTrue(blocked.getMessage().contains("could not be related"),
                    "the refreshed index must fail closed before workflow preparation");
            assertNull(gateway.preparedContext,"stale correlation must not reach workflow preparation");
        }
    }

    @Test void appliedWriteIsReturnedWhenWorkflowSnapshotPersistenceFails() throws Exception {
        Path sourceRoot=root.resolve("src/test/java");Files.createDirectories(sourceRoot);
        Path source=sourceRoot.resolve("LoginPage.java");
        Files.writeString(source,"import org.openqa.selenium.By; final class LoginPage { private final By loginButton = By.id(\"old-login\"); }");
        ExistingProjectIndex initial=index(sourceRoot);StudioWorkspaceStore store=new StudioWorkspaceStore(root);
        store.write(StudioWorkspaceStore.ArtifactKind.APPLICATION_MODEL,
                StrictJson.readObject(new ApplicationModelJson().write(model())),initial.projectFingerprint());
        CapturingGateway gateway=new CapturingGateway();
        try(TestEngineeringStudioService service=new TestEngineeringStudioService(
                configuration(sourceRoot),gateway,(java.util.function.Supplier<org.openqa.selenium.WebDriver>)null)){
            String runId=service.createRequirement("repair selector");RepairProposal proposal=proposal(initial);gateway.repair=proposal;
            service.registerRepairProposal(runId,proposal);
            Path snapshot=store.workspaceRoot().resolve("ai/sessions").resolve(runId+".json");
            Files.delete(snapshot);Files.createDirectory(snapshot);

            var result=assertDoesNotThrow(()->service.approveRepair(runId,proposal.proposalId()));

            assertEquals("APPROVED",result.decision());assertEquals("APPLIED",result.applyStatus());
            assertTrue(Files.readString(source).contains("login-submit"),"source write already succeeded");
            assertEquals("STALE",service.workflow(runId).freshness(),"an unpersisted applied state must fail closed");
            assertTrue(service.workflow(runId).limitations().stream()
                    .anyMatch(value->value.startsWith("PROJECT_STATE_PERSIST_FAILED:")));
            assertTrue(service.projectOverview().stage().limitations().stream()
                    .anyMatch(value->value.detail().contains("PROJECT_STATE_PERSIST_FAILED:")));
            assertTrue(service.repairHistory().stream().anyMatch(value->"APPLIED".equals(value.applyStatus())));
            assertThrows(IllegalStateException.class,()->service.rerun(runId),
                    "verification must wait until the applied state can be persisted safely");
        }
    }

    private TestEngineeringStudioService.Configuration configuration(Path sourceRoot) {
        return new TestEngineeringStudioService.Configuration(root, List.of(sourceRoot), List.of(),
                List.of("src/test/java"), "fixture");
    }

    private ExistingProjectIndex index(Path sourceRoot) {
        return new ExistingProjectIndexer().index(new ExistingProjectIndexer.Request(
                root, List.of(sourceRoot), List.of()));
    }

    private static RepairProposal proposal(ExistingProjectIndex index) {
        var element = index.elements().get(0);
        var owner = index.classes().stream().filter(value -> value.id().equals(element.ownerClassId()))
                .findFirst().orElseThrow();
        var file = index.sourceFiles().stream().filter(value -> value.logicalPath().equals(owner.logicalPath()))
                .findFirst().orElseThrow();
        var range = element.range();
        var target = new RepairProposal.SourceTarget(element.id(), element.declarationRef(), owner.logicalPath(),
                new RepairProposal.SourceRange(range.startLine(), range.startColumn(), range.endLine(),
                        range.endColumn(), range.startOffset(), range.endOffsetExclusive()),
                file.contentFingerprint(), element.declarationFingerprint(), "STRONG", "id", "old-login");
        var replacement = new RepairProposal.SelectorEvidence("cssSelector", "[data-testid='login-submit']",
                "candidate-new", "VERIFIED_IN_SCOPE", "SAME_TARGET", true, List.of("STABLE"),
                "LIVE_CANDIDATE_ANALYSIS");
        return new RepairProposal(ready(), "repair-1", RepairProposal.ApplicationPolicy.PROPOSE_ONLY,
                "selector", "old selector absent", "id:old-login",
                "cssSelector:[data-testid='login-submit']", element.declarationRef(), "candidate-old",
                "candidate-new", "classification", "drift", List.of("SAME_TARGET"), List.of("STABLE"),
                List.of("InvalidLoginTest"), List.of(), List.of("rerun"), target, replacement,
                List.of("login"));
    }

    private static ApplicationModel model() {
        var selector = new ApplicationModel.SelectorProjection("css selector", "[data-testid='login-submit']",
                "candidate-new", "VERIFIED_IN_SCOPE", "SAME_TARGET", true, List.of("STABLE"), 1,
                List.of(), List.of(), ApplicationModel.EvidenceSource.LIVE_CANDIDATE_ANALYSIS);
        var element = new ApplicationModel.ElementModel("login-button", "LoginButton",
                ApplicationModel.ElementType.BUTTON, "button", "Sign in", "Sign in",
                List.of(ApplicationModel.Action.CLICK), selector, List.of(),
                ApplicationModel.SelectorQuality.VERIFIED, null, "element-fp", provenance(), List.of());
        var page = new ApplicationModel.PageModel("login-page", "Login",
                new ApplicationModel.PageIdentity("/login", null, "Login", List.of("login-button"),
                        "page-fp", ApplicationModel.EvidenceSource.OBSERVED),
                List.of("Login"), List.of("page-fp"), List.of(), List.of(), List.of(element),
                List.of(), List.of(), provenance(), List.of());
        return new ApplicationModel(ApplicationModel.SCHEMA_VERSION, "fixture-app", "fixture", "test",
                Instant.EPOCH, List.of(page), List.of(), List.of(),
                new ApplicationModel.Coverage(ApplicationModel.Completeness.COMPLETE_FOR_OBSERVED_CONTEXT,
                        1, 1, 0, 1, 1, 0, 1, 0, false), List.of(), provenance());
    }

    private static ApplicationModel.Provenance provenance() {
        return ApplicationModel.Provenance.observed("fixture", "repair-refresh-test", List.of("fixture"));
    }

    private static ContractHeader ready() {
        return new ContractHeader(ContractHeader.SCHEMA_VERSION, ContractHeader.Status.READY,
                List.of("fixture"), List.of(), ContractHeader.Confidence.OBSERVED);
    }

    private static final class CapturingGateway implements StudioWorkflowGateway {
        private AgentContextPack preparedContext;
        private RepairProposal repair;
        private int reruns;

        @Override public void prepare(String runId,
                io.github.testlens.application.tooling.ai.workflow.TestEngineeringRequest request,
                AgentContextPack context) {
            preparedContext = context;
        }

        @Override public TestPlan generatePlan(String runId, String requirement) {
            return new TestPlan(ready(), List.of());
        }

        @Override public RepairProposal repair(String runId) {
            return repair;
        }

        @Override public WorkflowReport run(String runId) {
            throw new AssertionError("initial run is outside this regression");
        }

        @Override public WorkflowReport rerun(String runId, RepairProposal proposal) {
            reruns++;
            return new WorkflowReport(WorkflowReport.SCHEMA_VERSION, runId, "rerun",
                    TestEngineeringRun.State.SUCCESS, List.of(),
                    new WorkflowReport.Metrics(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0), List.of());
        }
    }
}
