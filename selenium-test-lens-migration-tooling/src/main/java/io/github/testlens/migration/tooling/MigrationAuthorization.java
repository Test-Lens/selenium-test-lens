package io.github.testlens.migration.tooling;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/** Trusted-host capabilities for one explicit migration action. Imported artifacts cannot create this value. */
public record MigrationAuthorization(Set<Capability> capabilities,String provenance){
    public MigrationAuthorization{capabilities=capabilities==null?Set.of():Set.copyOf(capabilities);provenance=safe(provenance);}
    public static MigrationAuthorization of(String provenance,Capability...capabilities){EnumSet<Capability>set=EnumSet.noneOf(Capability.class);if(capabilities!=null)java.util.Collections.addAll(set,capabilities);return new MigrationAuthorization(set,provenance);}
    public boolean allows(Capability capability){return capabilities.contains(Objects.requireNonNull(capability));}
    public enum Capability{EXECUTE_PROJECT_TEST_COMMAND,RUN_READ_ONLY_TEST,RUN_UNKNOWN_SIDE_EFFECT_TEST,RUN_MUTATING_TEST,CREATE_BRANCH,CREATE_WORKTREE,CREATE_STASH,RESTORE_STASH,ALLOW_REPOSITORY_GIT_HELPERS}
    private static String safe(String value){if(value==null||value.isBlank()||value.length()>256||value.indexOf('\0')>=0)throw new IllegalArgumentException("trusted authorization provenance required");return value;}
}
