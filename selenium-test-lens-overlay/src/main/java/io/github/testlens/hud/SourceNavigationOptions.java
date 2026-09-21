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
    private final String intellijProjectName;
    private final Path intellijProjectRoot;

    private SourceNavigationOptions(Builder builder) {
        enabled = builder.enabled;
        activationModifier = builder.activationModifier;
        ide = builder.ide;
        sourceRoots = List.copyOf(builder.sourceRoots);
        customUriTemplate = builder.customUriTemplate;
        intellijProjectName = builder.intellijProjectName;
        intellijProjectRoot = builder.intellijProjectRoot;
    }

    /** Returns disabled defaults. @since 0.3.1 */
    public static SourceNavigationOptions defaults() { return builder().build(); }
    /** Returns a source-navigation builder. @since 0.3.1 */
    public static Builder builder() { return new Builder(); }
    /** Reports whether call-site capture and HUD reveal are enabled. @since 0.3.1 */
    public boolean enabled() { return enabled; }
    /** Returns the activation shortcut. @since 0.3.1 */
    public SourceNavigationModifier activationModifier() { return activationModifier; }
    /** Returns the IDE protocol provider. @since 0.3.1 */
    public SourceIde ide() { return ide; }
    /** Returns additional local source roots. @since 0.3.1 */
    public List<Path> sourceRoots() { return sourceRoots; }
    /** Returns the CUSTOM URI template, or an empty string. @since 0.3.1 */
    public String customUriTemplate() { return customUriTemplate; }
    /** Returns the explicitly configured IntelliJ project name, or an empty string. @since 0.4.0 */
    public String intellijProjectName() { return intellijProjectName; }
    /** Returns the explicitly configured IntelliJ project root, or {@code null}. @since 0.4.0 */
    public Path intellijProjectRoot() { return intellijProjectRoot; }

    /** Builds immutable source-navigation options. @since 0.3.1 */
    public static final class Builder {
        private boolean enabled;
        private SourceNavigationModifier activationModifier = SourceNavigationModifier.F8;
        private SourceIde ide = SourceIde.INTELLIJ;
        private final List<Path> sourceRoots = new ArrayList<>();
        private String customUriTemplate = "";
        private String intellijProjectName = "";
        private Path intellijProjectRoot;
        private Builder() {}
        /** Enables or disables source navigation. @since 0.3.1 */
        public Builder enabled(boolean value) { enabled = value; return this; }
        /** Selects the activation shortcut. @since 0.3.1 */
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
        /**
         * Configures the IntelliJ project identity and root used by the JetBrains Toolbox navigation protocol.
         * @since 0.4.0
         */
        public Builder intellijProject(String projectName, Path projectRoot) {
            if (projectName == null || projectName.isBlank()) {
                throw new IllegalArgumentException("IntelliJ project name must not be blank");
            }
            intellijProjectName = projectName.trim();
            intellijProjectRoot = Objects.requireNonNull(projectRoot, "IntelliJ project root must not be null");
            return this;
        }
        /** Builds immutable options. @since 0.3.1 */
        public SourceNavigationOptions build() { return new SourceNavigationOptions(this); }
    }
}
