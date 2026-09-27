package io.github.testlens.migration.tooling;

import java.nio.file.Path;

/** Ephemeral absolute-path context. It is deliberately excluded from checkpoint serialization. */
public record MigrationLocalContext(Path repositoryRoot,Path worktreeRoot,Path gitCommonDir){ }
