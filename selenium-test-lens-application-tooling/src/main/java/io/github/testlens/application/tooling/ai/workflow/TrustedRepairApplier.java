package io.github.testlens.application.tooling.ai.workflow;

import io.github.testlens.application.tooling.ai.RepairProposal;
import io.github.testlens.application.tooling.codegen.SelectorJavaExpression;
import io.github.testlens.selector.tooling.ExistingProjectIndex;
import io.github.testlens.selector.tooling.ExistingProjectIndexer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Explicit trusted-host boundary for a minimal, evidence-backed selector declaration repair. @since 0.5.0 */
public final class TrustedRepairApplier {
    private final ControlledSourceApplier sourceApplier = new ControlledSourceApplier();

    public ApplyResult apply(Path workspace, ApplyRequest request) throws IOException {
        if (workspace == null || request == null) throw new IllegalArgumentException("workspace and request are required");
        if (!request.trustedApply()) return result(Status.TRUST_REQUIRED, null, null, null);
        RepairProposal proposal = request.proposal();
        RepairProposal.SourceTarget target = proposal.sourceTarget();
        RepairProposal.SelectorEvidence replacement = proposal.replacementEvidence();
        if (target == null || replacement == null || !validProposal(proposal, target, replacement)) {
            return result(Status.PROPOSAL_INVALID, null, null, null);
        }
        if (!"EXACT".equals(target.correlationState()) && !"STRONG".equals(target.correlationState())) {
            return result(Status.CORRELATION_BLOCKED, target.logicalPath(), null, null);
        }

        ExistingProjectIndex.ElementEntry declaration = request.sourceIndex().elements().stream()
                .filter(value -> value.id().equals(target.sourceElementId()))
                .filter(value -> value.declarationRef().equals(target.declarationRef()))
                .findFirst().orElse(null);
        if (declaration == null || declaration.range() == null
                || !Objects.equals(declaration.declarationFingerprint(), target.declarationFingerprint())
                || !sameRange(declaration.range(), target.range())) {
            return result(Status.DECLARATION_PRECONDITION_FAILED, target.logicalPath(), null, null);
        }
        ExistingProjectIndex.ClassEntry owner = request.sourceIndex().classes().stream()
                .filter(value -> value.id().equals(declaration.ownerClassId())).findFirst().orElse(null);
        ExistingProjectIndex.SourceFile indexedFile = request.sourceIndex().sourceFiles().stream()
                .filter(value -> value.logicalPath().equals(target.logicalPath())).findFirst().orElse(null);
        if (owner == null || !owner.logicalPath().equals(target.logicalPath()) || indexedFile == null
                || !indexedFile.contentFingerprint().equals(target.sourceFileFingerprint())) {
            return result(Status.DECLARATION_PRECONDITION_FAILED, target.logicalPath(), null, null);
        }
        if (!normalize(declaration.strategy()).equals(normalize(target.oldStrategy()))
                || declaration.valueProjection() == null
                || !Objects.equals(declaration.valueProjection().fingerprint(),
                ExistingProjectIndexer.selectorValueFingerprint(target.oldValue()))) {
            return result(Status.OLD_SELECTOR_MISMATCH, target.logicalPath(), null, null);
        }

        Path root = workspace.toAbsolutePath().normalize();
        Files.createDirectories(root);
        root = root.toRealPath();
        Path relative;
        try { relative = Path.of(target.logicalPath()).normalize(); }
        catch (RuntimeException invalid) { return result(Status.PATH_BLOCKED, target.logicalPath(), null, null); }
        if (relative.isAbsolute() || relative.startsWith("..") || !allowed(relative, request.allowedPathPrefixes())) {
            return result(Status.PATH_BLOCKED, target.logicalPath(), null, null);
        }
        Path source = root.resolve(relative).normalize();
        if (!source.startsWith(root) || containsSymbolicLink(root, source)
                || !Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS)) {
            return result(Status.PATH_BLOCKED, target.logicalPath(), null, null);
        }
        byte[] bytes = Files.readAllBytes(source);
        String fileFingerprint = "sha256:" + ArtifactEnvelope.digest(bytes);
        if (!fileFingerprint.equals(target.sourceFileFingerprint())) {
            return result(Status.SOURCE_PRECONDITION_FAILED, target.logicalPath(), fileFingerprint, null);
        }
        String current;
        try {
            current = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
        } catch (java.nio.charset.CharacterCodingException invalidEncoding) {
            return result(Status.UNSUPPORTED_SOURCE_ENCODING, target.logicalPath(), fileFingerprint, null);
        }
        RepairProposal.SourceRange range = target.range();
        if (range.endOffsetExclusive() > current.length()) {
            return result(Status.DECLARATION_PRECONDITION_FAILED, target.logicalPath(), fileFingerprint, null);
        }
        String declarationText = current.substring(range.startOffset(), range.endOffsetExclusive());
        String expectedOldExpression = SelectorJavaExpression.byExpression(target.oldStrategy(), target.oldValue());
        if (expectedOldExpression == null) {
            return result(Status.UNSUPPORTED_DECLARATION, target.logicalPath(), fileFingerprint, null);
        }
        int expressionStart = declarationText.indexOf(expectedOldExpression);
        if (expressionStart < 0 || declarationText.indexOf(expectedOldExpression,
                expressionStart + expectedOldExpression.length()) >= 0) {
            return result(Status.UNSUPPORTED_DECLARATION, target.logicalPath(), fileFingerprint, null);
        }
        int absoluteExpressionStart = range.startOffset() + expressionStart;
        int absoluteExpressionEnd = absoluteExpressionStart + expectedOldExpression.length();
        String replacementExpression = SelectorJavaExpression.byExpression(replacement.strategy(), replacement.value());
        if (replacementExpression == null) {
            return result(Status.UNSUPPORTED_SELECTOR, target.logicalPath(), fileFingerprint, null);
        }
        String patched = current.substring(0, absoluteExpressionStart) + replacementExpression
                + current.substring(absoluteExpressionEnd);
        ControlledSourceApplier.ApplyResult applied = sourceApplier.apply(root,
                new ControlledSourceApplier.ApplyRequest(relative, ArtifactEnvelope.digest(current), patched,
                        request.allowedPathPrefixes(), true));
        Status status = switch (applied.status()) {
            case APPLIED -> Status.APPLIED;
            case TRUST_REQUIRED -> Status.TRUST_REQUIRED;
            case PATH_BLOCKED -> Status.PATH_BLOCKED;
            case SOURCE_PRECONDITION_FAILED -> Status.SOURCE_PRECONDITION_FAILED;
        };
        return result(status, target.logicalPath(), applied.previousFingerprint(), applied.appliedFingerprint());
    }

    private static boolean validProposal(RepairProposal proposal, RepairProposal.SourceTarget target,
                                         RepairProposal.SelectorEvidence replacement) {
        if (proposal.applicationPolicy() != RepairProposal.ApplicationPolicy.PROPOSE_ONLY
                || !Objects.equals(proposal.sourceDeclarationRef(), target.declarationRef())
                || !Objects.equals(proposal.newCandidateId(), replacement.candidateId())
                || !Objects.equals(proposal.oldSelector(), target.oldStrategy() + ":" + target.oldValue())
                || !Objects.equals(proposal.newSelector(), replacement.strategy() + ":" + replacement.value())) return false;
        if (!"LIVE_CANDIDATE_ANALYSIS".equals(replacement.source())
                || !"VERIFIED_IN_SCOPE".equals(replacement.validation())
                || !"SAME_TARGET".equals(replacement.sameTarget()) || !replacement.unique()) return false;
        return !replacement.stability().isEmpty()
                && replacement.stability().stream().map(value -> value.toUpperCase(Locale.ROOT)).allMatch(value ->
                value.equals("NO_APPEARANCE_SIGNAL") || value.equals("DECLARED_STABLE") || value.equals("STABLE"));
    }

    private static boolean sameRange(ExistingProjectIndex.SourceRange indexed, RepairProposal.SourceRange expected) {
        return indexed.startLine() == expected.startLine() && indexed.startColumn() == expected.startColumn()
                && indexed.endLine() == expected.endLine() && indexed.endColumn() == expected.endColumn()
                && indexed.startOffset() == expected.startOffset()
                && indexed.endOffsetExclusive() == expected.endOffsetExclusive();
    }

    private static boolean allowed(Path relative, List<String> prefixes) {
        String logical = relative.toString().replace('\\', '/');
        return prefixes.stream().filter(Objects::nonNull).map(value -> value.replace('\\', '/'))
                .anyMatch(prefix -> logical.equals(prefix) || logical.startsWith(prefix.endsWith("/") ? prefix : prefix + "/"));
    }

    private static boolean containsSymbolicLink(Path root, Path target) {
        Path cursor = root;
        for (Path segment : root.relativize(target)) {
            cursor = cursor.resolve(segment);
            if (Files.exists(cursor, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(cursor)) return true;
        }
        return false;
    }

    private static String normalize(String value) {
        return Objects.toString(value, "").toLowerCase(Locale.ROOT).replace("by.", "")
                .replace("selector", "").replace("_", "").replace("-", "").trim();
    }

    private static ApplyResult result(Status status, String path, String previous, String applied) {
        return new ApplyResult(status, path, previous, applied);
    }

    public record ApplyRequest(RepairProposal proposal, ExistingProjectIndex sourceIndex,
                               List<String> allowedPathPrefixes, boolean trustedApply) {
        public ApplyRequest {
            Objects.requireNonNull(proposal, "proposal");
            Objects.requireNonNull(sourceIndex, "sourceIndex");
            allowedPathPrefixes = List.copyOf(allowedPathPrefixes == null ? List.of() : allowedPathPrefixes);
        }
    }

    public record ApplyResult(Status status, String logicalPath, String previousFingerprint,
                              String appliedFingerprint) { }

    public enum Status {
        APPLIED, TRUST_REQUIRED, PROPOSAL_INVALID, CORRELATION_BLOCKED, PATH_BLOCKED,
        SOURCE_PRECONDITION_FAILED, DECLARATION_PRECONDITION_FAILED, OLD_SELECTOR_MISMATCH,
        UNSUPPORTED_DECLARATION, UNSUPPORTED_SELECTOR, UNSUPPORTED_SOURCE_ENCODING
    }
}
