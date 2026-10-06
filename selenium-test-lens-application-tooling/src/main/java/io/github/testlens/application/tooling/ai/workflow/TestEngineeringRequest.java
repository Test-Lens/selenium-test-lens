package io.github.testlens.application.tooling.ai.workflow;

import java.util.List;
import java.util.Objects;
import java.nio.file.Path;

/** Structured, provider-neutral input to a test-engineering workflow. @since 0.5.0 */
public record TestEngineeringRequest(Requirement requirement, Scope scope, Framework framework,
                                     Target target, List<String> allowedPaths,
                                     ExecutionPolicy executionPolicy) {
    public TestEngineeringRequest {
        Objects.requireNonNull(requirement, "requirement");
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(framework, "framework");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(executionPolicy, "executionPolicy");
        allowedPaths = List.copyOf(allowedPaths == null ? List.of() : allowedPaths);
        if (allowedPaths.isEmpty() || allowedPaths.stream().anyMatch(TestEngineeringRequest::blank)) {
            throw new IllegalArgumentException("At least one non-blank allowed path is required");
        }
        if (allowedPaths.stream().map(Path::of).anyMatch(path -> path.isAbsolute() || path.normalize().startsWith(".."))) {
            throw new IllegalArgumentException("allowed paths must stay relative to the target root");
        }
    }

    public record Requirement(String summary, List<String> acceptanceCriteria) {
        public Requirement {
            if (blank(summary)) throw new IllegalArgumentException("requirement summary is required");
            acceptanceCriteria = List.copyOf(acceptanceCriteria == null ? List.of() : acceptanceCriteria);
        }
    }

    public record Scope(List<String> included, List<String> excluded) {
        public Scope {
            included = List.copyOf(included == null ? List.of() : included);
            excluded = List.copyOf(excluded == null ? List.of() : excluded);
        }
    }

    public record Framework(String name, String version, String testEngine) {
        public Framework {
            if (blank(name) || blank(testEngine)) throw new IllegalArgumentException("framework name and test engine are required");
        }
    }

    public record Target(String module, String testClass, String scenarioId) {
        public Target {
            if (blank(module) || blank(testClass) || blank(scenarioId)) throw new IllegalArgumentException("target fields are required");
        }
    }

    public enum ExecutionPolicy { DISABLED, TARGETED_AFTER_COMPILE }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
}
