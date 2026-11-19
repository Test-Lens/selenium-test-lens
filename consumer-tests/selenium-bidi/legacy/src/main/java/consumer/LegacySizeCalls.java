package consumer;

import org.openqa.selenium.bidi.network.RequestData;
import org.openqa.selenium.bidi.network.ResponseData;

/** Frozen 4.39 bytecode reproducing the calls made by Lens 0.5.1. */
public final class LegacySizeCalls {
    private LegacySizeCalls() {}
    public static Long requestHeaders(RequestData request) { return request.getHeadersSize(); }
    public static long responseHeaders(ResponseData response) { return response.getHeadersSize(); }
    public static long responseBody(ResponseData response) { return response.getBodySize(); }
}
