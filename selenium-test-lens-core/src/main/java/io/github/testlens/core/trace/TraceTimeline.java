package io.github.testlens.core.trace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TraceTimeline {
    private final List<TraceEvent> events = new ArrayList<>();

    public synchronized TraceEvent add(TraceEvent event) {
        if (event == null) {
            return null;
        }
        events.add(event);
        return event;
    }

    public synchronized List<TraceEvent> events() {
        return Collections.unmodifiableList(new ArrayList<>(events));
    }
}

