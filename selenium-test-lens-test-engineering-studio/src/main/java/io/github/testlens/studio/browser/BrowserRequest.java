package io.github.testlens.studio.browser;

import java.util.Objects;

/** Explicit browser bootstrap request. Creating a request performs no browser work. @since 0.5.0 */
public record BrowserRequest(Purpose purpose, Browser browser, Ownership ownership, boolean headless,String profileId) {
    public BrowserRequest {
        Objects.requireNonNull(purpose, "purpose");
        Objects.requireNonNull(browser, "browser");
        Objects.requireNonNull(ownership, "ownership");
        if(profileId==null||!profileId.matches("[A-Za-z0-9._-]{1,96}"))throw new IllegalArgumentException("A safe logical browser profile id is required");
    }

    public BrowserRequest(Purpose purpose,Browser browser,Ownership ownership,boolean headless){this(purpose,browser,ownership,headless,"default");}

    public static BrowserRequest studioOwned(Purpose purpose, Browser browser) {
        return new BrowserRequest(purpose, browser, Ownership.STUDIO_OWNED, true,"default");
    }

    public static BrowserRequest callerOwned(Purpose purpose, Browser browser) {
        return new BrowserRequest(purpose, browser, Ownership.CALLER_OWNED, true,"default");
    }
}
