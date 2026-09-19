package io.github.testlens.hud;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Immutable opt-in configuration for local HUD source navigation. @since 0.3.1 */
public final class SourceNavigationOptions {
    private final boolean enabled;
    private final SourceNavigationModifier activationModifier;
    private final SourceIde ide;
    private final List<Path> sourceRoots;
    private final String customUriTemplate;

    private SourceNavigationOptions(Builder builder) {
        enabled = builder.enabled;
        activationModifier = builder.activationModifier;
        ide = builder.ide;
        sourceRoots = List.copyOf(builder.sourceRoots);
        customUriTemplate = builder.customUriTemplate;
    }

    /** Returns disabled defaults. @since 0.3.1 */
    public static SourceNavigationOptions defaults() { return builder().build(); }
    /** Returns a source-navigation builder. @since 0.3.1 */
    public static Builder builder() { return new Builder(); }
    /** Reports whether call-site capture and HUD reveal are enabled. @since 0.3.1 */
    public boolean enabled() { return enabled; }
    /** Returns the reveal chord. @since 0.3.1 */
    public SourceNavigationModifier activationModifier() { return activationModifier; }
    /** Returns the IDE protocol provider. @since 0.3.1 */
    public SourceIde ide() { return ide; }
    /** Returns additional local source roots. @since 0.3.1 */
    public List<Path> sourceRoots() { return sourceRoots; }
    /** Returns the CUSTOM URI template, or an empty string. @since 0.3.1 */
    public String customUriTemplate() { return customUriTemplate; }

    /** Builds immutable source-navigation options. @since 0.3.1 */
    public static final class Builder {
        private boolean enabled;
        private SourceNavigationModifier activationModifier = SourceNavigationModifier.CTRL_ALT;
        private SourceIde ide = SourceIde.INTELLIJ;
        private final List<Path> sourceRoots = new ArrayList<>();
        private String customUriTemplate = "";
        private Builder() {}
        /** Enables or disables source navigation. @since 0.3.1 */
        public Builder enabled(boolean value) { enabled = value; return this; }
        /** Selects the activation chord. @since 0.3.1 */
        public Builder activationModifier(SourceNavigationModifier value) { activationModifier = Objects.requireNonNull(value); return this; }
        /** Selects the IDE provider. @since 0.3.1 */
        public Builder ide(SourceIde value) { ide = Objects.requireNonNull(value); return this; }
        /** Replaces additional source roots. @since 0.3.1 */
        public Builder sourceRoots(Path... values) {
            sourceRoots.clear();
            if (values != null) Arrays.stream(values).filter(Objects::nonNull).forEach(sourceRoots::add);
            return this;
        }
        /** URI template for CUSTOM; supports {file}, {line}, and {column}. @since 0.3.1 */
        public Builder customUriTemplate(String value) { customUriTemplate = value == null ? "" : value; return this; }
        /** Builds immutable options. @since 0.3.1 */
        public SourceNavigationOptions build() { return new SourceNavigationOptions(this); }
    }
}
