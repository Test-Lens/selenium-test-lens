package utils.jsExecHelper.api;

public final class ApiCallActions {
    private final ApiOverlayPanel panel;

    public ApiCallActions(ApiOverlayPanel panel) { this.panel = panel; }

    public <T> T callWithModal(String title,
                               String method,
                               String url,
                               String payloadPreview,
                               long timeoutMs,
                               java.util.concurrent.Callable<T> call,
                               java.util.function.Function<T, String> responsePreview) {
        panel.ensureOpen();

        String id = panel.showRequest(title, method, url, payloadPreview);
        if (id == null) {
            throw new IllegalStateException("API modal requestId is null – modal not initialized correctly");
        }
        panel.setPending(id, timeoutMs);

        long start = System.currentTimeMillis();
        try {
            T result = call.call();
            long dur = System.currentTimeMillis() - start;

            int status = 200;
            String body = responsePreview != null ? responsePreview.apply(result) : String.valueOf(result);

            panel.setResponse(id, status, dur, "", safeTrim(body));
            return result;
        } catch (Exception e) {
            long dur = System.currentTimeMillis() - start;
            panel.setError(id, e.getClass().getSimpleName() + " after " + dur + "ms", safeTrim(stackToString(e)));
            throw new RuntimeException(e);
        }
    }


    private static String safeTrim(String s) {
        if (s == null) return "";
        int max = 7000; // żeby nie zabić executeScript
        return s.length() > max ? s.substring(0, max) + "\n...(trimmed)" : s;
    }

    private static String stackToString(Throwable t) {
        var sw = new java.io.StringWriter();
        t.printStackTrace(new java.io.PrintWriter(sw));
        return sw.toString();
    }

    // HTTP-client-specific convenience should live in a future adapter module.
}

