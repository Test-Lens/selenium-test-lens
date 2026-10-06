package io.github.testlens.studio.projection;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Uniform, bounded stage metadata consumed by every Studio view. */
public record StageProjection(StageStatus status, List<Item> evidence, List<Item> limitations,
                              List<Artifact> artifacts, List<Action> actions) {
    public StageProjection {
        Objects.requireNonNull(status, "status");
        evidence = items(evidence); limitations = items(limitations);
        artifacts = (artifacts == null ? List.<Artifact>of() : artifacts).stream()
                .filter(Objects::nonNull).distinct().sorted(Comparator.comparing(Artifact::id)).limit(100).toList();
        actions = (actions == null ? List.<Action>of() : actions).stream()
                .filter(Objects::nonNull).distinct().sorted(Comparator.comparing(Action::id)).limit(32).toList();
    }
    public static StageProjection empty(String actionId, String label) {
        return new StageProjection(StageStatus.NOT_STARTED, List.of(), List.of(), List.of(),
                List.of(new Action(actionId, label, true)));
    }
    private static List<Item> items(List<Item> values) {
        return (values == null ? List.<Item>of() : values).stream().filter(Objects::nonNull).distinct()
                .sorted(Comparator.comparing(Item::code).thenComparing(Item::detail)).limit(500).toList();
    }
    public record Item(String code, String detail) {
        public Item { Objects.requireNonNull(code); detail = detail == null ? "" : detail; }
    }
    public record Artifact(String id, String kind, String freshness) {
        public Artifact { Objects.requireNonNull(id); Objects.requireNonNull(kind); freshness = freshness == null ? "UNKNOWN" : freshness; }
    }
    public record Action(String id, String label, boolean enabled) {
        public Action { Objects.requireNonNull(id); Objects.requireNonNull(label); }
    }
}
