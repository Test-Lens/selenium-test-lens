package io.github.testlens.migration.tooling;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/** Trusted-host authority bound to one exact apply plan or transaction. */
public record MigrationTargetedAuthorization(Set<Capability> capabilities, String targetRef,
        String repositoryBindingRef, String worktreeBindingRef, String provenance) {
    public MigrationTargetedAuthorization {
        capabilities = capabilities == null ? Set.of() : Set.copyOf(capabilities);
        require(targetRef, "migration-(apply-plan|apply-transaction)-v1");
        require(repositoryBindingRef, "migration-repository-binding-v1");
        require(worktreeBindingRef, "migration-worktree-binding-v1");
        if (provenance == null || provenance.isBlank() || provenance.length() > 256 || provenance.indexOf('\0') >= 0)
            throw new IllegalArgumentException("trusted authorization provenance required");
    }
    public static MigrationTargetedAuthorization of(String target, String repository, String worktree,
            String provenance, Capability... capabilities) {
        EnumSet<Capability> set = EnumSet.noneOf(Capability.class);
        if (capabilities != null) java.util.Collections.addAll(set, capabilities);
        return new MigrationTargetedAuthorization(set, target, repository, worktree, provenance);
    }
    public boolean allows(Capability capability) { return capabilities.contains(Objects.requireNonNull(capability)); }
    public boolean targets(String ref, String repository, String worktree) {
        return targetRef.equals(ref) && repositoryBindingRef.equals(repository) && worktreeBindingRef.equals(worktree);
    }
    public enum Capability { APPLY_APPROVED_SOURCE_CHANGES, ROLLBACK_TOOL_CHANGES }
    private static void require(String value, String domain) {
        if (value == null || !value.matches(domain + ":sha256:[0-9a-f]{64}")) throw new IllegalArgumentException(domain);
    }
}
