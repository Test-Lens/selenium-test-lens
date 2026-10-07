package io.github.testlens.application.tooling.ai.workflow.runner.provider;

import io.github.testlens.application.tooling.ai.workflow.AgentExecutor;

import java.util.Objects;

/** Provider-neutral bootstrap boundary for selecting a configured agent executor. @since 0.5.0 */
public interface AgentExecutorProvider {
    ProviderAvailability availability();

    ProviderAvailability preflight();

    default ProviderAvailability preflight(AgentExecutor.Role role, String logicalProfileId) {
        if (role == null || !validProfileId(logicalProfileId)) {
            return new ProviderAvailability(Status.PROFILE_INVALID,
                    "A role and safe logical profile id are required", null);
        }
        return preflight();
    }

    AgentExecutor executorFor(AgentExecutor.Role role, String logicalProfileId);

    /** Provider preflight disposition. @since 0.5.0 */
    enum Status { AVAILABLE, NOT_AVAILABLE, VERSION_UNSUPPORTED, PROFILE_INVALID }

    /** Bounded provider preflight result. @since 0.5.0 */
    record ProviderAvailability(Status status, String reason, String providerVersion) {
        private static final int MAX_REASON_LENGTH = 512;
        private static final int MAX_VERSION_LENGTH = 128;

        public ProviderAvailability {
            Objects.requireNonNull(status, "status");
            reason = bounded(reason == null ? "" : reason, MAX_REASON_LENGTH);
            providerVersion = providerVersion == null ? null : bounded(providerVersion, MAX_VERSION_LENGTH);
        }

        public boolean available() { return status == Status.AVAILABLE; }

        private static String bounded(String value, int limit) {
            String sanitized = value.replaceAll("[\\p{Cntrl}&&[^\\t]]", " ").trim();
            return sanitized.length() <= limit ? sanitized : sanitized.substring(0, limit);
        }
    }

    /** Failure raised when a requested provider/profile is unavailable. @since 0.5.0 */
    final class ProviderUnavailableException extends IllegalStateException {
        private final ProviderAvailability availability;

        public ProviderUnavailableException(ProviderAvailability availability) {
            super(Objects.requireNonNull(availability, "availability").reason());
            this.availability = availability;
        }

        public ProviderAvailability availability() { return availability; }
    }

    private static boolean validProfileId(String value) {
        return value != null && value.matches("[A-Za-z0-9._-]{1,96}");
    }
}
