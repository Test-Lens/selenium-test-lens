package io.github.mmaciekk111.uitestlens.selenium.overlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class OverlayPolicy {
    private static final OverlayPolicy NONE = new OverlayPolicy(List.of());

    private final List<OverlayHandler> handlers;

    private OverlayPolicy(List<OverlayHandler> handlers) {
        this.handlers = List.copyOf(handlers);
    }

    public static OverlayPolicy none() {
        return NONE;
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<OverlayHandler> handlers() {
        return handlers;
    }

    public boolean isEmpty() {
        return handlers.isEmpty();
    }

    public static final class Builder {
        private final List<OverlayHandler> handlers = new ArrayList<>();

        public Builder handler(OverlayHandler handler) {
            handlers.add(Objects.requireNonNull(handler, "handler must not be null"));
            return this;
        }

        public OverlayPolicy build() {
            return handlers.isEmpty() ? none() : new OverlayPolicy(handlers);
        }
    }
}
