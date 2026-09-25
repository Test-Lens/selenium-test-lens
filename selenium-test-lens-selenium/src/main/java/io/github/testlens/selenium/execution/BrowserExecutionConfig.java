package io.github.testlens.selenium.execution;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

/**
 * Immutable pre-session browser execution configuration.
 *
 * @since 0.4.0
 */
public final class BrowserExecutionConfig {
    static final String HEADLESS_PROPERTY = "testLens.headless";
    static final String HEADLESS_ENVIRONMENT = "TEST_LENS_HEADLESS";
    private final HeadlessMode headless;

    private BrowserExecutionConfig(HeadlessMode headless) {
        this.headless = headless;
    }

    /**
     * @return immutable configuration resolved from process sources
     * @since 0.4.0
     */
    public static BrowserExecutionConfig resolve() {
        return resolve(HeadlessMode.UNSET);
    }

    /**
     * @param explicitHeadless explicit intent, or UNSET to consult process sources
     * @return immutable resolved configuration
     * @since 0.4.0
     */
    public static BrowserExecutionConfig resolve(HeadlessMode explicitHeadless) {
        return resolve(explicitHeadless, System::getProperty, System::getenv);
    }

    static BrowserExecutionConfig resolve(HeadlessMode explicitHeadless,
                                          Function<String, String> properties,
                                          Function<String, String> environment) {
        HeadlessMode explicit = Objects.requireNonNull(explicitHeadless, "explicitHeadless");
        Objects.requireNonNull(properties, "properties");
        Objects.requireNonNull(environment, "environment");
        if (explicit != HeadlessMode.UNSET) return new BrowserExecutionConfig(explicit);
        String property = properties.apply(HEADLESS_PROPERTY);
        if (property != null) return new BrowserExecutionConfig(parse(property,
                "system property " + HEADLESS_PROPERTY));
        String env = environment.apply(HEADLESS_ENVIRONMENT);
        if (env != null) return new BrowserExecutionConfig(parse(env,
                "environment variable " + HEADLESS_ENVIRONMENT));
        return new BrowserExecutionConfig(HeadlessMode.UNSET);
    }

    private static HeadlessMode parse(String raw, String source) {
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "true" -> HeadlessMode.TRUE;
            case "false" -> HeadlessMode.FALSE;
            default -> throw new IllegalArgumentException("Invalid " + source + " value '" + raw
                    + "'; allowed values are true and false");
        };
    }

    /**
     * @return resolved headless intent
     * @since 0.4.0
     */
    public HeadlessMode headless() {
        return headless;
    }
}
