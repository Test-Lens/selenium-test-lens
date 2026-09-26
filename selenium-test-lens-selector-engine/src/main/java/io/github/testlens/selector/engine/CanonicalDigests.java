package io.github.testlens.selector.engine;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public final class CanonicalDigests {
    public static final int CANONICALIZATION_VERSION = 1;
    private CanonicalDigests() { }

    public static String exactValueDigest(SelectorSubject subject) {
        if (!subject.hasTrustedExactValue()) throw new IllegalArgumentException("Exact-value digest requires a trusted canonical value");
        return digest("selector-exact-value-v1", Integer.toString(CANONICALIZATION_VERSION), subject.strategy(), subject.canonicalValue());
    }

    /** Domain-separated, ambiguity-safe SHA-256 for internal selector artifacts. */
    public static String digest(String domain, String... fields) {
        if (domain == null || domain.isBlank()) throw new IllegalArgumentException("digest domain is required");
        String[] canonical = new String[fields.length + 1];
        canonical[0] = domain;
        System.arraycopy(fields, 0, canonical, 1, fields.length);
        return sha256(lengthPrefixed(canonical));
    }

    /** Raw-byte variant used for exact report provenance without base64/string copies. */
    public static String digestBytes(String domain, byte[] bytes) {
        if (domain == null || domain.isBlank()) throw new IllegalArgumentException("digest domain is required");
        try {
            MessageDigest digest=MessageDigest.getInstance("SHA-256");
            byte[] tag=domain.getBytes(StandardCharsets.UTF_8);
            digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(tag.length).array());digest.update(tag);
            digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());digest.update(bytes);
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException("SHA-256 unavailable", impossible); }
    }

    public static String contextFingerprint(ContextKnowledge knowledge,List<ContextSegment> segments) {
        if(knowledge!=ContextKnowledge.KNOWN||segments==null||segments.isEmpty())return null;
        if(segments.stream().anyMatch(segment->segment.referenceKind()==ContextReferenceKind.SESSION_LOCAL_HANDLE
                ||segment.referenceKind()==ContextReferenceKind.REMOTE_ELEMENT_ID))return null;
        List<String> fields=new ArrayList<>();fields.add("context-v1");
        for(ContextSegment segment:segments){fields.add(segment.kind());fields.add(value(segment.locatorStrategy()));
            fields.add(value(segment.locatorValueDigest()));fields.add(segment.referenceKind().name());fields.add(value(segment.referenceValue()));}
        return "context-v1:sha256:" + digest("selector-context-v1", fields.toArray(String[]::new));
    }

    public enum ContextKnowledge { KNOWN, PARTIAL, UNKNOWN }
    public enum ContextReferenceKind { NONE, LOCATOR_DIGEST, INDEX, NAME_OR_ID, SESSION_LOCAL_HANDLE, REMOTE_ELEMENT_ID }
    public record ContextSegment(String kind,String locatorStrategy,String locatorValueDigest,
                                 ContextReferenceKind referenceKind,String referenceValue) {
        public ContextSegment { if(kind==null||kind.isBlank())throw new IllegalArgumentException("Context segment kind required");
            if(referenceKind==null)referenceKind=ContextReferenceKind.NONE; }
    }
    private static String value(String value){return value==null?"":value;}

    static byte[] lengthPrefixed(String... fields) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (String field : fields) {
            byte[] bytes = (field == null ? "" : field).getBytes(StandardCharsets.UTF_8);
            out.writeBytes(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
            out.writeBytes(bytes);
        }
        return out.toByteArray();
    }

    static String sha256(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder hex = new StringBuilder(64);
            for (byte b : digest) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException("SHA-256 unavailable", impossible); }
    }
}
