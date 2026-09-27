package io.github.testlens.compatibility.engine;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Domain-separated, length-prefixed compatibility identifiers. */
public final class CompatibilityDigests {
    private CompatibilityDigests() {}

    public static String digest(String domain, String... values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            add(digest, domain);
            if (values != null) for (String value : values) add(digest, value == null ? "" : value);
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is required by Java 17", impossible);
        }
    }

    private static void add(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(new byte[]{(byte)(bytes.length >>> 24), (byte)(bytes.length >>> 16),
                (byte)(bytes.length >>> 8), (byte)bytes.length});
        digest.update(bytes);
    }
}
