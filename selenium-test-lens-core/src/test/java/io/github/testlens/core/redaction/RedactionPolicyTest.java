package io.github.testlens.core.redaction;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class RedactionPolicyTest {
    private static final String SECRET = "canary-secret-7f18d2";

    @Test
    void defaultsRedactEverySupportedKeyWithoutSubstringFalsePositives() {
        RedactionPolicy policy = RedactionPolicy.defaults();
        List<String> keys = List.of("authorization", "proxy-authorization", "cookie", "set-cookie",
                "x-api-key", "api-key", "apikey", "password", "passwd", "pwd", "secret",
                "client-secret", "client_secret", "token", "access-token", "access_token",
                "refresh-token", "refresh_token", "id-token", "id_token", "session", "sessionid",
                "jsessionid", "csrf", "xsrf");
        for (String key : keys) {
            assertEquals("[REDACTED]", policy.redact(key.toUpperCase(), SECRET), key);
            assertEquals("[REDACTED]", policy.redact("metadata." + key, SECRET), key);
        }
        assertEquals(SECRET, policy.redact("tokenizer", SECRET));
        assertEquals(SECRET, policy.redact("passwordPolicy", SECRET));
        assertEquals(SECRET, policy.redact("sessionName", SECRET));
    }

    @Test
    void structuredTextCredentialsJwtAndLiteralsAreRedactedIdempotently() {
        RedactionPolicy policy = RedactionPolicy.builder().secret(SECRET).build();
        String input = "Authorization: Bearer abc.def\npassword=p4ss&ok=yes\n"
                + "Cookie: session=inside-cookie; preference=dark\n"
                + "{\"access_token\":\"json-secret\"} Basic Zm9vOmJhcg== "
                + "aaaaaaaa.bbbbbbbb.cccccccc " + SECRET;
        String safe = policy.redact(input);
        assertFalse(safe.contains("abc.def"));
        assertFalse(safe.contains("p4ss"));
        assertFalse(safe.contains("json-secret"));
        assertFalse(safe.contains("inside-cookie"));
        assertFalse(safe.contains("Zm9vOmJhcg"));
        assertFalse(safe.contains(SECRET));
        assertEquals(safe, policy.redact(safe));

        RedactionPolicy overlapping = RedactionPolicy.builder()
                .secret("short-secret")
                .secret("short-secret-with-suffix")
                .build();
        assertEquals("[REDACTED]", overlapping.redact("short-secret-with-suffix"));
    }

    @Test
    void urlsHandleAbsoluteRelativeEncodedKeysUserInfoFragmentsAndMalformedInput() {
        RedactionPolicy policy = RedactionPolicy.defaults();
        String absolute = policy.redactUrl("https://user:pass@example.test/a?ok=1&access%5Ftoken=" + SECRET + "#frag");
        assertFalse(absolute.contains("user"));
        assertFalse(absolute.contains("pass"));
        assertFalse(absolute.contains(SECRET));
        assertFalse(absolute.contains("frag"));
        assertTrue(absolute.contains("ok=1"));
        assertTrue(absolute.contains("[REDACTED]") || absolute.contains("%5BREDACTED%5D"));

        String relative = policy.redactUrl("/orders?token=" + SECRET + "&page=2#x");
        assertFalse(relative.contains(SECRET));
        assertTrue(relative.contains("page=2"));
        assertEquals("url[length=11]", policy.redactUrl("http://[bad"));
    }

    @Test
    void builderSupportsCustomKeyReplacementDisabledAndHidesSecretConfiguration() {
        RedactionPolicy policy = RedactionPolicy.builder().replacement("***")
                .sensitiveKey("tenant-session").secret(SECRET).build();
        assertEquals("***", policy.redact("tenant-session", "value"));
        assertEquals("x=***", policy.redact("x=" + SECRET));
        assertEquals(1, policy.additionalSensitiveKeyCount());
        assertEquals(1, policy.literalSecretCount());
        assertFalse(policy.toString().contains(SECRET));
        assertEquals("token=" + SECRET, RedactionPolicy.disabled().redact("token=" + SECRET));
        assertThrows(IllegalArgumentException.class, () -> RedactionPolicy.builder().replacement(""));
    }

    @Test
    void oneImmutableInstanceIsSafeForConcurrentUse() throws Exception {
        RedactionPolicy policy = RedactionPolicy.builder().secret(SECRET).build();
        var executor = Executors.newFixedThreadPool(8);
        try {
            List<Callable<String>> calls = new ArrayList<>();
            for (int i = 0; i < 100; i++) calls.add(() -> policy.redact("token=" + SECRET));
            for (var result : executor.invokeAll(calls)) assertEquals("token=[REDACTED]", result.get());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void fiveMiBInputCompletesWithoutPathologicalRegexBacktracking() {
        RedactionPolicy policy = RedactionPolicy.defaults();
        String input = "ordinary diagnostics ".repeat((5 * 1024 * 1024) / 21);
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> assertEquals(input, policy.redact(input)));
    }
}
