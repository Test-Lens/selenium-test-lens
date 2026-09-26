package io.github.testlens.selector.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Immutable two-origin policy workspace used for preparation and optimistic apply. */
public record PolicyWorkspaceSnapshot(int schemaVersion, int canonicalizationVersion,
                                      OriginDocument tracked, OriginDocument local,
                                      String effectiveWorkspaceDigest, String projectFingerprint) {
    public static final int SCHEMA_VERSION = 1;

    public PolicyWorkspaceSnapshot {
        if (schemaVersion != SCHEMA_VERSION) throw new IllegalArgumentException("Unsupported workspace schemaVersion");
        if (canonicalizationVersion != CanonicalDigests.CANONICALIZATION_VERSION) throw new IllegalArgumentException("Unsupported canonicalizationVersion");
        Objects.requireNonNull(tracked, "tracked");
        Objects.requireNonNull(local, "local");
        if (tracked.origin() != Origin.TRACKED || local.origin() != Origin.LOCAL) throw new IllegalArgumentException("Workspace origins are fixed");
        String expected = workspaceDigest(schemaVersion, canonicalizationVersion, tracked, local, projectFingerprint);
        if (!expected.equals(effectiveWorkspaceDigest)) throw new IllegalArgumentException("effectiveWorkspaceDigest does not match workspace content");
    }

    public static PolicyWorkspaceSnapshot create(OriginDocument tracked, OriginDocument local, String projectFingerprint) {
        return new PolicyWorkspaceSnapshot(SCHEMA_VERSION, CanonicalDigests.CANONICALIZATION_VERSION, tracked, local,
                workspaceDigest(SCHEMA_VERSION, CanonicalDigests.CANONICALIZATION_VERSION, tracked, local, projectFingerprint), projectFingerprint);
    }

    public List<OriginRule> originRules() {
        List<OriginRule> result = new ArrayList<>();
        tracked.rules().forEach(rule -> result.add(new OriginRule(Origin.TRACKED, rule)));
        local.rules().forEach(rule -> result.add(new OriginRule(Origin.LOCAL, rule)));
        return result.stream().sorted(Comparator.comparing((OriginRule value) -> value.rule().ruleId()).thenComparing(value -> value.origin().name())).toList();
    }

    public CompiledPolicySet compiled() {
        java.util.Map<String, SelectorPolicy.Rule> byId = new java.util.TreeMap<>();
        for (OriginRule value : originRules()) byId.putIfAbsent(value.rule().ruleId(), value.rule());
        return CompiledPolicySet.compile(new SelectorPolicy.Document(SelectorPolicy.SCHEMA_VERSION,
                CanonicalDigests.CANONICALIZATION_VERSION, new ArrayList<>(byId.values())));
    }

    public OriginRule find(String ruleId) {
        return originRules().stream().filter(value -> value.rule().ruleId().equals(ruleId)).findFirst().orElse(null);
    }

    public enum Origin { TRACKED, LOCAL }
    public enum FileState { EXPECTED_ABSENT, EXPECTED_PRESENT, INVALID, UNAVAILABLE }

    public record OriginDocument(Origin origin, FileState fileState, String fileDigest,
                                 String semanticRuleSetDigest, List<SelectorPolicy.Rule> rules) {
        public OriginDocument {
            Objects.requireNonNull(origin, "origin");
            Objects.requireNonNull(fileState, "fileState");
            rules = List.copyOf(Objects.requireNonNull(rules, "rules"));
            String expected = semanticDigest(rules);
            if (!expected.equals(semanticRuleSetDigest)) throw new IllegalArgumentException("semanticRuleSetDigest does not match rules");
            if (fileState == FileState.EXPECTED_PRESENT && !isHex(fileDigest)) throw new IllegalArgumentException("Present policy file requires fileDigest");
            if (fileState == FileState.EXPECTED_ABSENT && fileDigest != null) throw new IllegalArgumentException("Absent policy file cannot have fileDigest");
            if (fileState == FileState.EXPECTED_ABSENT && !rules.isEmpty()) throw new IllegalArgumentException("Absent policy file cannot contain rules");
        }

        public static OriginDocument absent(Origin origin) {
            return new OriginDocument(origin, FileState.EXPECTED_ABSENT, null, semanticDigest(List.of()), List.of());
        }
    }

    public record OriginRule(Origin origin, SelectorPolicy.Rule rule) {
        public OriginRule { Objects.requireNonNull(origin); Objects.requireNonNull(rule); }
    }

    public static String rawFileDigest(byte[] bytes) {
        return CanonicalDigests.digestBytes("selector-policy-file-v1", Objects.requireNonNull(bytes));
    }

    public static String semanticDigest(List<SelectorPolicy.Rule> rules) {
        List<String> fields = new ArrayList<>();
        rules.stream().map(SelectorPolicy.Rule::ruleId).sorted().forEach(fields::add);
        return CanonicalDigests.digest("selector-policy-rule-set-v1", fields.toArray(String[]::new));
    }

    private static String workspaceDigest(int schema, int canonicalization, OriginDocument tracked,
                                          OriginDocument local, String projectFingerprint) {
        return "selector-policy-workspace-v1:sha256:" + CanonicalDigests.digest("selector-policy-workspace-v1",
                Integer.toString(schema), Integer.toString(canonicalization),
                tracked.fileState().name(), tracked.semanticRuleSetDigest(),
                local.fileState().name(), local.semanticRuleSetDigest(), projectFingerprint == null ? "" : projectFingerprint);
    }

    private static boolean isHex(String value) { return value != null && value.matches("[0-9a-f]{64}"); }
}
