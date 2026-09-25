package io.github.testlens;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

final class ObservabilityModeResolver {
    static final String PROPERTY = "testLens.observability";
    static final String ENVIRONMENT = "TEST_LENS_OBSERVABILITY";

    private ObservabilityModeResolver() {}

    static ObservabilityMode resolve(ObservabilityMode explicit) {
        return resolve(explicit, System::getProperty, System::getenv);
    }

    static ObservabilityMode resolve(ObservabilityMode explicit,
                                     Function<String, String> properties,
                                     Function<String, String> environment) {
        Objects.requireNonNull(properties, "properties");
        Objects.requireNonNull(environment, "environment");
        if (explicit != null) return explicit;
        String property = properties.apply(PROPERTY);
        if (property != null) return parse(property, "system property " + PROPERTY);
        String env = environment.apply(ENVIRONMENT);
        if (env != null) return parse(env, "environment variable " + ENVIRONMENT);
        return ObservabilityMode.DEFAULT;
    }

    private static ObservabilityMode parse(String raw, String source) {
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "default" -> ObservabilityMode.DEFAULT;
            case "fast" -> ObservabilityMode.FAST;
            default -> throw new IllegalArgumentException("Invalid " + source + " value '" + raw
                    + "'; allowed values are default and fast");
        };
    }
}
