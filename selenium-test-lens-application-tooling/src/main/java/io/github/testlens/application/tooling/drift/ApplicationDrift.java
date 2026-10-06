package io.github.testlens.application.tooling.drift;

import java.util.Comparator;
import java.util.List;

/** Semantic differences between two ApplicationModel observations. @since 0.5.0 */
public record ApplicationDrift(int schemaVersion, String applicationId, String beforeFingerprint,
                               String afterFingerprint, List<Change> changes, List<String> limitations) {
    public static final int SCHEMA_VERSION = 1;
    public ApplicationDrift {
        if (schemaVersion != SCHEMA_VERSION) throw new IllegalArgumentException("Unsupported drift schemaVersion");
        changes = (changes == null ? List.<Change>of() : changes).stream()
                .sorted(Comparator.comparing(Change::type).thenComparing(Change::entityId)).toList();
        limitations = (limitations == null ? List.<String>of() : limitations).stream().distinct().sorted().toList();
    }

    public record Change(ChangeType type, EntityType entityType, String entityId, String parentId,
                         String beforeValue, String afterValue, String reason,
                         SourceCorrelation sourceCorrelation, List<String> evidence) {
        public Change { evidence = (evidence == null ? List.<String>of() : evidence).stream().distinct().sorted().toList(); }
    }
    public enum EntityType { PAGE, STATE, ELEMENT, TRANSITION }
    public enum ChangeType {
        PAGE_ADDED, PAGE_REMOVED, STATE_ADDED, STATE_REMOVED, ELEMENT_ADDED, ELEMENT_REMOVED,
        ELEMENT_RENAMED, SELECTOR_CHANGED, SELECTOR_BECAME_UNSTABLE, SELECTOR_IMPROVED,
        TRANSITION_ADDED, TRANSITION_REMOVED, TRANSITION_CHANGED
    }
    public enum SourceCorrelation { CORRELATED, NO_SOURCE_CORRELATION }
}
