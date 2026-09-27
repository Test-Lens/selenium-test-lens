package io.github.testlens.migration.tooling;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

final class MigrationDigests {
    private MigrationDigests() { }
    static String digest(String domain, String... fields) {
        MessageDigest md=sha256(); put(md,domain);
        for(String field:fields)put(md,field==null?"":field);
        return HexFormat.of().formatHex(md.digest());
    }
    static String bytes(String domain, byte[] value) {
        MessageDigest md=sha256();put(md,domain);md.update(ByteBuffer.allocate(8).putLong(value.length).array());md.update(value);
        return HexFormat.of().formatHex(md.digest());
    }
    static MessageDigest sha256(){try{return MessageDigest.getInstance("SHA-256");}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    private static void put(MessageDigest md,String value){byte[]b=value.getBytes(StandardCharsets.UTF_8);md.update(ByteBuffer.allocate(8).putLong(b.length).array());md.update(b);}
}
