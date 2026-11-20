package io.github.testlens.selenium.network;

import org.openqa.selenium.bidi.BiDi;
import org.openqa.selenium.bidi.Event;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Cached adapter using public APIs across Selenium BiDi binary signature changes. */
final class SeleniumBiDiCompatibility {
    private static final ClassValue<Map<String, Method>> METHODS = new ClassValue<>() {
        @Override protected Map<String, Method> computeValue(Class<?> type) { return new ConcurrentHashMap<>(); }
    };

    private SeleniumBiDiCompatibility() {}

    /** Older Selenium decoders unbox nullable response sizes; request header size is required by the protocol. */
    static Map<String, Object> normalizeSizes(Map<String, Object> payload) {
        if (payload.get("request") instanceof Map<?, ?> request && request.get("headersSize") == null) {
            throw new BiDiCaptureException(BiDiFailureCategory.PROTOCOL_ERROR,
                    "BiDi request headersSize must be a non-null unsigned size",
                    Map.of("initializationStage", "DECODE_EVENT"),
                    new IllegalArgumentException("Missing or null required BiDi request headersSize"));
        }
        Map<String, Object> normalized = new LinkedHashMap<>(payload);
        normalizeSizes(normalized, "response", "headersSize", "bodySize");
        return normalized;
    }

    private static void normalizeSizes(Map<String, Object> payload, String field, String... sizes) {
        if (!(payload.get(field) instanceof Map<?, ?> original)) return;
        Map<Object, Object> copied = null;
        for (String size : sizes) {
            if (original.get(size) != null) continue;
            if (copied == null) copied = new LinkedHashMap<>(original);
            copied.put(size, -1L);
        }
        if (copied != null) payload.put(field, copied);
    }

    static Object size(Object value, String getter) {
        return invoke(value, value.getClass(), getter, new Class<?>[0]);
    }

    static <T> Object addListener(BiDi bidi, Event<T> event, Consumer<T> consumer) {
        Object id = invoke(bidi, BiDi.class, "addListener", new Class<?>[]{Event.class, Consumer.class}, event, consumer);
        if (!(id instanceof Long) && !(id instanceof String)) throw incompatible("Unsupported BiDi listener identifier", null);
        return id;
    }

    static void removeListener(BiDi bidi, Object id) {
        invoke(bidi, BiDi.class, "removeListener", new Class<?>[]{id instanceof String ? String.class : long.class}, id);
    }

    private static Object invoke(Object target, Class<?> owner, String name, Class<?>[] parameters, Object... arguments) {
        try {
            Method method = METHODS.get(owner).computeIfAbsent(name, ignored -> {
                try {
                    Method found = owner.getMethod(name, parameters);
                    if (parameters.length == 0 && found.getReturnType() != long.class
                            && found.getReturnType() != Long.class) {
                        throw incompatible("Unsupported Selenium BiDi size accessor: " + name, null);
                    }
                    if (name.equals("addListener") && found.getReturnType() != long.class
                            && found.getReturnType() != Long.class && found.getReturnType() != String.class) {
                        throw incompatible("Unsupported Selenium BiDi listener registration signature", null);
                    }
                    return found;
                }
                catch (NoSuchMethodException failure) { throw incompatible("Selenium BiDi public method unavailable: " + name, failure); }
            });
            return method.invoke(target, arguments);
        } catch (IllegalAccessException failure) {
            throw incompatible("Selenium BiDi public method inaccessible", failure);
        } catch (InvocationTargetException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof LinkageError linkage) throw incompatible("Selenium BiDi binary incompatibility", linkage);
            if (cause instanceof RuntimeException runtime) throw runtime;
            if (cause instanceof Error error) throw error;
            throw incompatible("Selenium BiDi invocation failed", cause);
        } catch (LinkageError failure) {
            throw incompatible("Selenium BiDi binary incompatibility", failure);
        }
    }

    static BiDiCaptureException incompatible(String message, Throwable cause) {
        return new BiDiCaptureException(BiDiFailureCategory.BINARY_INCOMPATIBILITY, message,
                Map.of("initializationStage", "SELENIUM_COMPATIBILITY"), cause);
    }
}
