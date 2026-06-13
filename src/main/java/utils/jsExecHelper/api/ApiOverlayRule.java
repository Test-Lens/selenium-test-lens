package utils.jsExecHelper.api;

import java.util.regex.Pattern;

public final class ApiOverlayRule {
    private static final ThreadLocal<Pattern> URL_PATTERN =
            ThreadLocal.withInitial(() -> Pattern.compile(".*")); // default: wszystko

    private ApiOverlayRule() {}

    public static void setUrlPattern(String regex) {
        URL_PATTERN.set(Pattern.compile(regex));
    }

    public static boolean matches(String url) {
        return URL_PATTERN.get().matcher(url).matches();
    }
}
