package io.github.testlens.api;

import java.util.ArrayList;
import java.util.List;

public final class ApiOverlayPlan {
    private static final ThreadLocal<Boolean> ENABLED = ThreadLocal.withInitial(() -> true);
    private static final ThreadLocal<List<String>> PATHS = ThreadLocal.withInitial(ArrayList::new);
    private static final ThreadLocal<List<String>> KEYS  = ThreadLocal.withInitial(ArrayList::new);

    private ApiOverlayPlan() {}

    public static void enable(boolean on) { ENABLED.set(on); }
    public static boolean isEnabled() { return Boolean.TRUE.equals(ENABLED.get()); }

    public static void clear() { PATHS.get().clear(); KEYS.get().clear(); }

    public static void addPath(String path) { PATHS.get().add(path); }
    public static void addKey(String key) { KEYS.get().add(key); }

    public static List<String> paths() { return PATHS.get(); }
    public static List<String> keys() { return KEYS.get(); }
}

