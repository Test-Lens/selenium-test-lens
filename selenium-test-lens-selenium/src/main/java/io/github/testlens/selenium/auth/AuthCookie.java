package io.github.testlens.selenium.auth;

import org.openqa.selenium.Cookie;

import java.time.Instant;
import java.util.Date;

public final class AuthCookie {
    private final String name;
    private final String value;
    private final String domain;
    private final String path;
    private final Instant expiry;
    private final boolean secure;
    private final boolean httpOnly;
    private final String sameSite;

    public AuthCookie(String name,
                      String value,
                      String domain,
                      String path,
                      Instant expiry,
                      boolean secure,
                      boolean httpOnly,
                      String sameSite) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("cookie name must not be blank");
        }
        this.name = name;
        this.value = value == null ? "" : value;
        this.domain = domain == null ? "" : domain;
        this.path = path == null || path.isBlank() ? "/" : path;
        this.expiry = expiry;
        this.secure = secure;
        this.httpOnly = httpOnly;
        this.sameSite = sameSite == null ? "" : sameSite;
    }

    public static AuthCookie fromSeleniumCookie(Cookie cookie) {
        if (cookie == null) {
            throw new IllegalArgumentException("cookie must not be null");
        }
        Date expiry = cookie.getExpiry();
        return new AuthCookie(
                cookie.getName(),
                cookie.getValue(),
                cookie.getDomain(),
                cookie.getPath(),
                expiry == null ? null : expiry.toInstant(),
                cookie.isSecure(),
                cookie.isHttpOnly(),
                cookie.getSameSite()
        );
    }

    public Cookie toSeleniumCookie() {
        Cookie.Builder builder = new Cookie.Builder(name, value)
                .path(path)
                .isSecure(secure)
                .isHttpOnly(httpOnly);
        if (!domain.isBlank()) {
            builder.domain(domain);
        }
        if (expiry != null) {
            builder.expiresOn(Date.from(expiry));
        }
        if (!sameSite.isBlank()) {
            builder.sameSite(sameSite);
        }
        return builder.build();
    }

    public String name() {
        return name;
    }

    public String value() {
        return value;
    }

    public String domain() {
        return domain;
    }

    public String path() {
        return path;
    }

    public Instant expiry() {
        return expiry;
    }

    public boolean secure() {
        return secure;
    }

    public boolean httpOnly() {
        return httpOnly;
    }

    public String sameSite() {
        return sameSite;
    }
}

