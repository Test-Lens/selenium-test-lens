package io.github.testlens.application.tooling.ai.workflow;

import io.github.testlens.application.tooling.ai.ContractHeader;
import io.github.testlens.application.tooling.ai.RepairProposal;
import io.github.testlens.application.tooling.codegen.SelectorJavaExpression;
import io.github.testlens.selector.tooling.ExistingProjectIndex;
import io.github.testlens.selector.tooling.ExistingProjectIndexer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class TrustedRepairApplierTest {
    private static final String LOGICAL_PATH = "src/main/java/example/LoginPage.java";
    private static final String OLD_VALUE = "old-login-button";
    private static final String NEW_VALUE = "[data-testid='login-submit']";
    private static final String SOURCE = """
            package example;
            import org.openqa.selenium.By;
            final class LoginPage {
                private final By loginButton = By.id("old-login-button");
                private final By untouched = By.id("untouched");
            }
            """;

    @TempDir Path workspace;

    @Test
    void appliesOnlyTheCorrelatedDeclarationAfterExplicitTrust() throws Exception {
        Fixture fixture = fixture(SOURCE);
        TrustedRepairApplier applier = new TrustedRepairApplier();

        assertEquals(TrustedRepairApplier.Status.TRUST_REQUIRED,
                applier.apply(workspace, fixture.request(false)).status());
        TrustedRepairApplier.ApplyResult result = applier.apply(workspace, fixture.request(true));

        assertEquals(TrustedRepairApplier.Status.APPLIED, result.status());
        String replacement = SelectorJavaExpression.byExpression("css", NEW_VALUE);
        String expected = SOURCE.substring(0, fixture.start()) + replacement + SOURCE.substring(fixture.end());
        assertEquals(expected, Files.readString(fixture.source()));
        assertTrue(Files.readString(fixture.source()).contains("By.id(\"untouched\")"));
    }

    @Test
    void appliesInsideTheVariableDeclaratorRangeProducedByTheRealIndexer() throws Exception {
        Path source = workspace.resolve(LOGICAL_PATH);
        Files.createDirectories(source.getParent());
        Files.writeString(source, SOURCE);
        ExistingProjectIndex index = new ExistingProjectIndexer().index(new ExistingProjectIndexer.Request(
                workspace, List.of(workspace.resolve("src/main/java")), List.of()));
        ExistingProjectIndex.ElementEntry declaration = index.elements().stream()
                .filter(value -> value.name().equals("loginButton")).findFirst().orElseThrow();
        ExistingProjectIndex.ClassEntry owner = index.classes().stream()
                .filter(value -> value.id().equals(declaration.ownerClassId())).findFirst().orElseThrow();
        ExistingProjectIndex.SourceFile file = index.sourceFiles().stream()
                .filter(value -> value.logicalPath().equals(owner.logicalPath())).findFirst().orElseThrow();
        ExistingProjectIndex.SourceRange indexedRange = declaration.range();
        RepairProposal.SourceTarget target = new RepairProposal.SourceTarget(declaration.id(),
                declaration.declarationRef(), owner.logicalPath(), new RepairProposal.SourceRange(
                indexedRange.startLine(), indexedRange.startColumn(), indexedRange.endLine(),
                indexedRange.endColumn(), indexedRange.startOffset(), indexedRange.endOffsetExclusive()),
                file.contentFingerprint(), declaration.declarationFingerprint(), "STRONG", "id", OLD_VALUE);
        RepairProposal.SelectorEvidence replacement = new RepairProposal.SelectorEvidence("css", NEW_VALUE,
                "candidate-new", "VERIFIED_IN_SCOPE", "SAME_TARGET", true,
                List.of("NO_APPEARANCE_SIGNAL"), "LIVE_CANDIDATE_ANALYSIS");
        ContractHeader header = new ContractHeader(ContractHeader.SCHEMA_VERSION, ContractHeader.Status.READY,
                List.of("real indexed declaration"), List.of(), ContractHeader.Confidence.LIVE_VALIDATED);
        RepairProposal proposal = new RepairProposal(header, "repair-real-index",
                RepairProposal.ApplicationPolicy.PROPOSE_ONLY, "Replace selector", "Old selector has no match",
                "id:" + OLD_VALUE, "css:" + NEW_VALUE, declaration.declarationRef(), "candidate-old",
                replacement.candidateId(), "classification", "drift", List.of("SAME_TARGET"),
                replacement.stability(), List.of("LoginTest"), List.of(), List.of("compile", "rerun"),
                target, replacement, List.of("LoginPage.login"));

        TrustedRepairApplier.ApplyResult applied = new TrustedRepairApplier().apply(workspace,
                new TrustedRepairApplier.ApplyRequest(proposal, index, List.of("src/main/java"), true));

        assertEquals(TrustedRepairApplier.Status.APPLIED, applied.status());
        assertEquals(SOURCE.replace("By.id(\"old-login-button\")",
                SelectorJavaExpression.byExpression("css", NEW_VALUE)), Files.readString(source));
    }

    @Test
    void rejectsStaleFileDeclarationOldSelectorCorrelationAndPathPreconditions() throws Exception {
        Fixture fixture = fixture(SOURCE);
        TrustedRepairApplier applier = new TrustedRepairApplier();

        assertEquals(TrustedRepairApplier.Status.SOURCE_PRECONDITION_FAILED,
                applier.apply(workspace, fixture.withFileFingerprint("sha256:stale").request(true)).status());
        assertEquals(TrustedRepairApplier.Status.DECLARATION_PRECONDITION_FAILED,
                applier.apply(workspace, fixture.withDeclarationFingerprint("stale-declaration").request(true)).status());
        assertEquals(TrustedRepairApplier.Status.OLD_SELECTOR_MISMATCH,
                applier.apply(workspace, fixture.withOldValue("different-old-selector").request(true)).status());
        assertEquals(TrustedRepairApplier.Status.CORRELATION_BLOCKED,
                applier.apply(workspace, fixture.withCorrelation("PROBABLE").request(true)).status());
        assertEquals(TrustedRepairApplier.Status.PATH_BLOCKED,
                applier.apply(workspace, new TrustedRepairApplier.ApplyRequest(fixture.proposal(), fixture.index(),
                        List.of("src/test/java"), true)).status());
        assertEquals(SOURCE, Files.readString(fixture.source()));
    }

    @Test
    void rejectsDeclarationRangesWithZeroOrMultipleMatchingSelectorExpressions() throws Exception {
        Fixture fixture = fixture(SOURCE);
        int fieldStart = SOURCE.indexOf("loginButton");
        int fieldEnd = SOURCE.indexOf(';', fieldStart);
        assertEquals(TrustedRepairApplier.Status.UNSUPPORTED_DECLARATION,
                new TrustedRepairApplier().apply(workspace,
                        fixture.withRange(fieldStart, fieldStart + "loginButton".length()).request(true)).status());

        String duplicate = SOURCE.replace("By.id(\"old-login-button\")",
                "choose(By.id(\"old-login-button\"), By.id(\"old-login-button\"))");
        Fixture duplicateFixture = fixture(duplicate);
        int duplicateStart = duplicate.indexOf("loginButton");
        int duplicateEnd = duplicate.indexOf(';', duplicateStart);
        assertEquals(TrustedRepairApplier.Status.UNSUPPORTED_DECLARATION,
                new TrustedRepairApplier().apply(workspace,
                        duplicateFixture.withRange(duplicateStart, duplicateEnd).request(true)).status());
    }

    @Test
    void rejectsSymbolicLinkSourceWithoutFollowingIt() throws Exception {
        Path real = workspace.resolve("real/LoginPage.java");
        Files.createDirectories(real.getParent());
        Files.writeString(real, SOURCE);
        Path linked = workspace.resolve(LOGICAL_PATH);
        Files.createDirectories(linked.getParent());
        try { Files.createSymbolicLink(linked, real); }
        catch (IOException | UnsupportedOperationException | SecurityException unavailable) {
            assumeTrue(false, "Symbolic links are unavailable in this environment: " + unavailable.getMessage());
        }
        Fixture fixture = fixtureFor(linked, SOURCE, fileFingerprint(SOURCE), "declaration-fingerprint",
                OLD_VALUE, "STRONG");
        assertEquals(TrustedRepairApplier.Status.PATH_BLOCKED,
                new TrustedRepairApplier().apply(workspace, fixture.request(true)).status());
        assertEquals(SOURCE, Files.readString(real));
    }

    private Fixture fixture(String source) throws IOException {
        Path path = workspace.resolve(LOGICAL_PATH);
        Files.createDirectories(path.getParent());
        Files.writeString(path, source);
        return fixtureFor(path, source, fileFingerprint(source), "declaration-fingerprint", OLD_VALUE, "STRONG");
    }

    private Fixture fixtureFor(Path sourcePath, String source, String fileFingerprint,
                               String declarationFingerprint, String oldValue, String correlation) {
        int start = source.indexOf("By.id(\"old-login-button\")");
        int end = start + "By.id(\"old-login-button\")".length();
        var range = new ExistingProjectIndex.SourceRange(4, 36, 4, 60, start, end);
        var sourceFile = new ExistingProjectIndex.SourceFile(LOGICAL_PATH, fileFingerprint);
        var owner = new ExistingProjectIndex.ClassEntry("class-login", LOGICAL_PATH, "example.LoginPage", "LoginPage",
                ExistingProjectIndex.ClassClassification.PAGE_OBJECT, ExistingProjectIndex.Origin.HAND_WRITTEN, List.of());
        var declaration = new ExistingProjectIndex.ElementEntry("source-login", owner.id(), "loginButton", "decl-login",
                "id", new ExistingProjectIndex.ValueProjection("RESOLVED",
                ExistingProjectIndexer.selectorValueFingerprint(OLD_VALUE)), range, "declaration-fingerprint");
        var metrics = new ExistingProjectIndex.Metrics(1, 1, 1, 1, 1, 0, 0, 0, 1);
        ExistingProjectIndex index = new ExistingProjectIndex(ExistingProjectIndex.SCHEMA_VERSION, "project",
                List.of(sourceFile), List.of(owner), List.of(declaration), List.of(), List.of(), List.of(),
                ExistingProjectIndex.Completeness.COMPLETE, List.of(), metrics);
        RepairProposal.SourceTarget target = new RepairProposal.SourceTarget(declaration.id(), declaration.declarationRef(),
                LOGICAL_PATH, new RepairProposal.SourceRange(range.startLine(), range.startColumn(), range.endLine(),
                range.endColumn(), range.startOffset(), range.endOffsetExclusive()), fileFingerprint,
                declarationFingerprint, correlation, "id", oldValue);
        RepairProposal.SelectorEvidence replacement = new RepairProposal.SelectorEvidence("css", NEW_VALUE,
                "candidate-new", "VERIFIED_IN_SCOPE", "SAME_TARGET", true,
                List.of("NO_APPEARANCE_SIGNAL"), "LIVE_CANDIDATE_ANALYSIS");
        ContractHeader header = new ContractHeader(ContractHeader.SCHEMA_VERSION, ContractHeader.Status.READY,
                List.of("live selector evidence"), List.of(), ContractHeader.Confidence.LIVE_VALIDATED);
        RepairProposal proposal = new RepairProposal(header, "repair-login",
                RepairProposal.ApplicationPolicy.PROPOSE_ONLY, "Replace selector", "Old selector has no match",
                "id:" + oldValue, "css:" + NEW_VALUE, declaration.declarationRef(), "candidate-old",
                replacement.candidateId(), "classification", "drift", List.of("SAME_TARGET"),
                replacement.stability(), List.of("LoginTest"), List.of(), List.of("compile", "rerun"),
                target, replacement, List.of("LoginPage.login"));
        return new Fixture(sourcePath, start, end, index, proposal);
    }

    private static String fileFingerprint(String source) {
        return "sha256:" + ArtifactEnvelope.digest(source.getBytes(StandardCharsets.UTF_8));
    }

    private record Fixture(Path source, int start, int end, ExistingProjectIndex index, RepairProposal proposal) {
        TrustedRepairApplier.ApplyRequest request(boolean trusted) {
            return new TrustedRepairApplier.ApplyRequest(proposal, index, List.of("src/main/java"), trusted);
        }

        Fixture withFileFingerprint(String fingerprint) {
            ExistingProjectIndex.SourceFile sourceFile = new ExistingProjectIndex.SourceFile(LOGICAL_PATH, fingerprint);
            ExistingProjectIndex changed = new ExistingProjectIndex(index.schemaVersion(), index.projectFingerprint(),
                    List.of(sourceFile), index.classes(), index.elements(), index.methods(), index.tests(), index.edges(),
                    index.completeness(), index.limitations(), index.metrics());
            RepairProposal.SourceTarget current = proposal.sourceTarget();
            RepairProposal.SourceTarget target = new RepairProposal.SourceTarget(current.sourceElementId(),
                    current.declarationRef(), current.logicalPath(), current.range(), fingerprint,
                    current.declarationFingerprint(), current.correlationState(), current.oldStrategy(), current.oldValue());
            return new Fixture(source, start, end, changed, replaceTarget(proposal, target));
        }

        Fixture withDeclarationFingerprint(String fingerprint) {
            RepairProposal.SourceTarget current = proposal.sourceTarget();
            RepairProposal.SourceTarget target = new RepairProposal.SourceTarget(current.sourceElementId(),
                    current.declarationRef(), current.logicalPath(), current.range(), current.sourceFileFingerprint(),
                    fingerprint, current.correlationState(), current.oldStrategy(), current.oldValue());
            return new Fixture(source, start, end, index, replaceTarget(proposal, target));
        }

        Fixture withOldValue(String oldValue) {
            RepairProposal.SourceTarget current = proposal.sourceTarget();
            RepairProposal.SourceTarget target = new RepairProposal.SourceTarget(current.sourceElementId(),
                    current.declarationRef(), current.logicalPath(), current.range(), current.sourceFileFingerprint(),
                    current.declarationFingerprint(), current.correlationState(), current.oldStrategy(), oldValue);
            return new Fixture(source, start, end, index, replaceOld(proposal, target));
        }

        Fixture withCorrelation(String state) {
            RepairProposal.SourceTarget current = proposal.sourceTarget();
            RepairProposal.SourceTarget target = new RepairProposal.SourceTarget(current.sourceElementId(),
                    current.declarationRef(), current.logicalPath(), current.range(), current.sourceFileFingerprint(),
                    current.declarationFingerprint(), state, current.oldStrategy(), current.oldValue());
            return new Fixture(source, start, end, index, replaceTarget(proposal, target));
        }


        Fixture withRange(int rangeStart, int rangeEnd) {
            ExistingProjectIndex.ElementEntry currentDeclaration = index.elements().get(0);
            ExistingProjectIndex.SourceRange range = new ExistingProjectIndex.SourceRange(
                    1, 1, 1, Math.max(1, rangeEnd - rangeStart), rangeStart, rangeEnd);
            ExistingProjectIndex.ElementEntry changedDeclaration = new ExistingProjectIndex.ElementEntry(
                    currentDeclaration.id(), currentDeclaration.ownerClassId(), currentDeclaration.name(),
                    currentDeclaration.declarationRef(), currentDeclaration.strategy(),
                    currentDeclaration.valueProjection(), range, currentDeclaration.declarationFingerprint());
            ExistingProjectIndex changed = new ExistingProjectIndex(index.schemaVersion(), index.projectFingerprint(),
                    index.sourceFiles(), index.classes(), List.of(changedDeclaration), index.methods(), index.tests(),
                    index.edges(), index.completeness(), index.limitations(), index.metrics());
            RepairProposal.SourceTarget current = proposal.sourceTarget();
            RepairProposal.SourceTarget target = new RepairProposal.SourceTarget(current.sourceElementId(),
                    current.declarationRef(), current.logicalPath(), new RepairProposal.SourceRange(
                    range.startLine(), range.startColumn(), range.endLine(), range.endColumn(),
                    range.startOffset(), range.endOffsetExclusive()), current.sourceFileFingerprint(),
                    current.declarationFingerprint(), current.correlationState(), current.oldStrategy(), current.oldValue());
            return new Fixture(source, rangeStart, rangeEnd, changed, replaceTarget(proposal, target));
        }
    }

    private static RepairProposal replaceTarget(RepairProposal proposal, RepairProposal.SourceTarget target) {
        return replace(proposal, proposal.oldSelector(), target);
    }

    private static RepairProposal replaceOld(RepairProposal proposal, RepairProposal.SourceTarget target) {
        return replace(proposal, target.oldStrategy() + ":" + target.oldValue(), target);
    }

    private static RepairProposal replace(RepairProposal proposal, String oldSelector,
                                           RepairProposal.SourceTarget target) {
        return new RepairProposal(proposal.header(), proposal.proposalId(), proposal.applicationPolicy(),
                proposal.whatChanged(), proposal.why(), oldSelector, proposal.newSelector(),
                proposal.sourceDeclarationRef(), proposal.oldCandidateId(), proposal.newCandidateId(),
                proposal.classificationRef(), proposal.driftRef(), proposal.sameTargetEvidence(),
                proposal.stabilityEvidence(), proposal.affectedTests(), proposal.risks(), proposal.verificationPlan(),
                target, proposal.replacementEvidence(), proposal.affectedMethods());
    }
}
