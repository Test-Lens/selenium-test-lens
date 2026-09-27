package io.github.testlens.migration.tooling;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Exact, content-addressed resolution of one proposal dependency. */
public record MigrationDependencyResolution(int schemaVersion, int algorithmVersion, String resolutionId,
        String proposalId, MigrationProposalSet.DependencyCode dependencyCode, String dependencySemanticRef,
        ResolutionType resolutionType, String checkpointRef, List<String> evidenceDigests,
        String relatedProposalId, String hostProvenance, List<String> limitations) {
    public MigrationDependencyResolution {
        if (schemaVersion != 1 || algorithmVersion != 1) throw new IllegalArgumentException("resolution version");
        require(proposalId, "migration-proposal-v1"); Objects.requireNonNull(dependencyCode);
        safe(dependencySemanticRef, 512); Objects.requireNonNull(resolutionType); require(checkpointRef, "migration-checkpoint-v1");
        evidenceDigests = evidenceDigests == null ? List.of() : evidenceDigests.stream().distinct().sorted().toList();
        evidenceDigests.forEach(x -> { if (!x.matches("sha256:[0-9a-f]{64}")) throw new IllegalArgumentException("evidence digest"); });
        if (relatedProposalId != null) require(relatedProposalId, "migration-proposal-v1");
        safe(hostProvenance, 256);
        limitations = limitations == null ? List.of() : limitations.stream().distinct().sorted().toList();
        if (limitations.size() > 256) throw new IllegalArgumentException("limitations bound");
        validateType(dependencyCode, resolutionType, relatedProposalId, evidenceDigests);
        String expected = id(proposalId, dependencyCode, dependencySemanticRef, resolutionType, checkpointRef,
                evidenceDigests, relatedProposalId, limitations);
        if (!expected.equals(resolutionId)) throw new IllegalArgumentException("resolutionId");
    }
    public static MigrationDependencyResolution create(String proposal, MigrationProposalSet.DependencyCode code,
            String semanticRef, ResolutionType type, String checkpoint, List<String> evidence,
            String relatedProposal, String provenance, List<String> limitations) {
        return new MigrationDependencyResolution(1, 1,
                id(proposal, code, semanticRef, type, checkpoint, evidence, relatedProposal, limitations),
                proposal, code, semanticRef, type, checkpoint, evidence, relatedProposal, provenance, limitations);
    }
    public enum ResolutionType { NEW_EVIDENCE_ACCEPTED, CONFIGURATION_ALIGNMENT_PROVEN,
        PREREQUISITE_PROPOSAL_SATISFIED, TRUSTED_MANUAL_DECISION }
    private static void validateType(MigrationProposalSet.DependencyCode code, ResolutionType type, String related,
            List<String> evidence) {
        ResolutionType expected = switch (code) {
            case REQUIRES_NEW_EVIDENCE -> ResolutionType.NEW_EVIDENCE_ACCEPTED;
            case REQUIRES_CONFIGURATION_ALIGNMENT -> ResolutionType.CONFIGURATION_ALIGNMENT_PROVEN;
            case BLOCKED_BY_PROPOSAL -> ResolutionType.PREREQUISITE_PROPOSAL_SATISFIED;
            case REQUIRES_MANUAL_DECISION -> ResolutionType.TRUSTED_MANUAL_DECISION;
        };
        if (type != expected) throw new IllegalArgumentException("resolution type does not satisfy dependency");
        if ((code == MigrationProposalSet.DependencyCode.REQUIRES_NEW_EVIDENCE
                || code == MigrationProposalSet.DependencyCode.REQUIRES_CONFIGURATION_ALIGNMENT) && evidence.isEmpty())
            throw new IllegalArgumentException("resolution evidence required");
        if (code == MigrationProposalSet.DependencyCode.BLOCKED_BY_PROPOSAL && related == null)
            throw new IllegalArgumentException("related proposal required");
    }
    private static String id(String proposal, MigrationProposalSet.DependencyCode code, String semantic,
            ResolutionType type, String checkpoint, List<String> evidence, String related, List<String> limitations) {
        List<String> fields = new java.util.ArrayList<>(List.of(proposal, code.name(), semantic, type.name(), checkpoint,
                related == null ? "" : related));
        (evidence == null ? List.<String>of() : evidence).stream().sorted().forEach(fields::add);
        (limitations == null ? List.<String>of() : limitations).stream().sorted().forEach(fields::add);
        return "migration-dependency-resolution-v1:sha256:" + MigrationDigests.digest("migration-dependency-resolution-v1",
                fields.toArray(String[]::new));
    }
    private static void require(String value, String domain) {
        if (value == null || !value.matches(java.util.regex.Pattern.quote(domain) + ":sha256:[0-9a-f]{64}"))
            throw new IllegalArgumentException(domain);
    }
    private static void safe(String value, int max) {
        if (value == null || value.isBlank() || value.length() > max || value.indexOf('\0') >= 0)
            throw new IllegalArgumentException("unsafe value");
    }
}
