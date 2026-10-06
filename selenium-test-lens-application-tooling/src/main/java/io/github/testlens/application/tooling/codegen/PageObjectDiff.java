package io.github.testlens.application.tooling.codegen;

import io.github.testlens.application.tooling.drift.ApplicationDrift;

import java.util.Comparator;
import java.util.List;

/** Generated source before/after paired with semantic model changes explaining the regeneration. @since 0.5.0 */
public record PageObjectDiff(String pageId,String beforeSource,String afterSource,
                             List<ApplicationDrift.Change>semanticChanges) {
    public PageObjectDiff { semanticChanges=(semanticChanges==null?List.<ApplicationDrift.Change>of():semanticChanges).stream().sorted(Comparator.comparing(ApplicationDrift.Change::type).thenComparing(ApplicationDrift.Change::entityId)).toList(); }
}
