package io.github.mmaciekk111.uitestlens.core.trace;

import java.time.Duration;
import java.time.Instant;

public record TraceStep(
        String id,
        String name,
        TraceStatus status,
        Instant startedAt,
        Instant endedAt,
        Duration duration,
        TraceFailure failure
) {
    public TraceStep {
        id = id == null ? "" : id;
        name = name == null ? "" : name;
        status = status == null ? TraceStatus.INFO : status;
        startedAt = startedAt == null ? Instant.now() : startedAt;
        endedAt = endedAt == null ? startedAt : endedAt;
        duration = duration == null || duration.isNegative() ? Duration.ZERO : duration;
    }
}
