package io.github.testlens.migration.tooling;

import io.github.testlens.compatibility.engine.CompatibilityRunManifest;
import java.util.List;
import java.util.Map;

/** Strict bounded migration-side manifest projection; it does not depend on Selenium capture tooling. */
public final class MigrationCompatibilityManifestReader {
    public Projection read(byte[]bytes){Map<String,Object>r=MigrationStrictJson.object(bytes);keys(r,List.of("schemaVersion","manifestAlgorithmVersion","manifestId","testIdentity","execution","browser","display","context","configuration","attempts","terminalResult","completeness","issues","evidenceDigests"),true);if(num(r,"schemaVersion")!=1||num(r,"manifestAlgorithmVersion")!=1)throw new IllegalArgumentException("unsupported manifest version");String id=str(r,"manifestId");if(!id.matches("compatibility-manifest-v1:sha256:[0-9a-f]{64}"))throw new IllegalArgumentException("manifestId");Map<String,Object>test=factObject(obj(r,"testIdentity")),execution=obj(r,"execution"),context=obj(r,"context"),terminal=obj(r,"terminalResult");String testRef=str(test,"testIdentityRef"),source=fact(obj(context,"testSourceRevision")),headless=fact(obj(execution,"effectiveHeadless")),observability=fact(obj(execution,"observabilityMode")),status=str(terminal,"status");return new Projection(id,"sha256:"+MigrationDigests.rawBytes(bytes),testRef,source,headless,observability,CompatibilityRunManifest.ResultStatus.valueOf(status));}
    public record Projection(String manifestId,String exactDigest,String testIdentityRef,String testSourceRevision,String effectiveHeadless,String observabilityMode,CompatibilityRunManifest.ResultStatus terminalStatus){public Projection{MigrationApplyPlan.require(manifestId,"compatibility-manifest-v1");MigrationApplyPlan.require(exactDigest,"sha256");MigrationApplyPlan.require(testIdentityRef,"compatibility-test-v1");}}
    @SuppressWarnings("unchecked")private static Map<String,Object>obj(Map<String,Object>m,String k){Object v=m.get(k);if(!(v instanceof Map<?,?>))throw new IllegalArgumentException("object "+k);return(Map<String,Object>)v;}
    private static Map<String,Object>factObject(Map<String,Object>m){if(!"KNOWN".equals(str(m,"knowledge")))throw new IllegalArgumentException("known fact required");Object v=m.get("value");if(!(v instanceof Map<?,?>x))throw new IllegalArgumentException("fact object");@SuppressWarnings("unchecked")Map<String,Object>out=(Map<String,Object>)x;return out;}
    private static String fact(Map<String,Object>m){if(!"KNOWN".equals(str(m,"knowledge")))return null;return str(m,"value");}
    private static String str(Map<String,Object>m,String k){Object v=m.get(k);if(!(v instanceof String s))throw new IllegalArgumentException("string "+k);return s;}
    private static long num(Map<String,Object>m,String k){Object v=m.get(k);if(!(v instanceof Number n))throw new IllegalArgumentException("number "+k);return n.longValue();}
    private static void keys(Map<String,Object>m,List<String>required,boolean extensions){for(String k:required)if(!m.containsKey(k))throw new IllegalArgumentException("missing "+k);for(String k:m.keySet())if(!required.contains(k)&&!(extensions&&k.startsWith("x-")))throw new IllegalArgumentException("unknown "+k);}
}
