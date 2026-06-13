package utils.jsExecHelper.api;

import utils.jsExecHelper.JsOverlayDebug;

public final class ApiOverlayContext {
    private static final ThreadLocal<JsOverlayDebug> TL = new ThreadLocal<>();

    private ApiOverlayContext() {}

    public static void set(JsOverlayDebug overlay) { TL.set(overlay); }
    public static JsOverlayDebug get() { return TL.get(); }
    public static void clear() { TL.remove(); }
}
