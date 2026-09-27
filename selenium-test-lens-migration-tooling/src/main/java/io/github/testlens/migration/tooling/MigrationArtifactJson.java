package io.github.testlens.migration.tooling;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.json.JsonFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Deterministic LF JSON for local-sensitive proposal, decision, and dry-run artifacts. */
public final class MigrationArtifactJson {
    public static final long MAX_BYTES=64L*1024*1024;
    private static final JsonFactory FACTORY=JsonFactory.builder().build();
    public byte[] write(Object artifact){Objects.requireNonNull(artifact);try{Bounded out=new Bounded(MAX_BYTES);try(JsonGenerator g=FACTORY.createGenerator(ObjectWriteContext.empty(),out)){value(g,artifact);g.writeRaw('\n');}return out.toByteArray();}catch(IOException e){throw new IllegalArgumentException("artifact serialization failed",e);}}
    /** Strictly validates syntax, duplicate keys, bounds, schema, and content-addressed identifier shape. */
    public ValidatedDocument read(byte[]bytes,Kind kind){Map<String,Object>root=MigrationStrictJson.object(bytes,MAX_BYTES);String schema=kind==Kind.PROPOSAL_SET?"proposalSetId":kind==Kind.DECISION_LEDGER?"ledgerSemanticDigest":"dryRunId";Object id=root.get(schema);if(!(id instanceof String s)||!s.matches(kind.pattern))throw new IllegalArgumentException("invalid "+schema);if(kind!=Kind.DRY_RUN&&(number(root.get("schemaVersion"))!=1||number(root.get("algorithmVersion"))!=1))throw new IllegalArgumentException("unsupported artifact version");return new ValidatedDocument(kind,s,java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(root)));}
    private static long number(Object value){if(!(value instanceof Number n))throw new IllegalArgumentException("numeric version required");return n.longValue();}
    private static void value(JsonGenerator g,Object v)throws IOException{if(v==null){g.writeNull();return;}if(v instanceof String s){g.writeString(s);return;}if(v instanceof Integer n){g.writeNumber(n);return;}if(v instanceof Long n){g.writeNumber(n);return;}if(v instanceof Boolean b){g.writeBoolean(b);return;}if(v instanceof Enum<?>e){g.writeString(e.name());return;}if(v instanceof Map<?,?>m){g.writeStartObject();for(var e:m.entrySet().stream().sorted(Comparator.comparing(x->String.valueOf(x.getKey()))).toList()){g.writeName(String.valueOf(e.getKey()));value(g,e.getValue());}g.writeEndObject();return;}if(v instanceof Collection<?>c){g.writeStartArray();for(Object x:c)value(g,x);g.writeEndArray();return;}if(v.getClass().isRecord()){g.writeStartObject();for(var c:v.getClass().getRecordComponents())try{g.writeName(c.getName());value(g,c.getAccessor().invoke(v));}catch(IllegalAccessException|InvocationTargetException e){throw new IOException(e);}g.writeEndObject();return;}throw new IOException("unsupported artifact value "+v.getClass());}
    public enum Kind{PROPOSAL_SET("migration-proposal-set-v1:sha256:[0-9a-f]{64}"),DECISION_LEDGER("migration-decision-ledger-v1:sha256:[0-9a-f]{64}"),DRY_RUN("migration-dry-run-v1:sha256:[0-9a-f]{64}");final String pattern;Kind(String p){pattern=p;}}
    public record ValidatedDocument(Kind kind,String semanticId,Map<String,Object>fields){ }
    private static final class Bounded extends ByteArrayOutputStream{final long max;Bounded(long max){this.max=max;}void check(int n){if(count+(long)n>max)throw new IllegalArgumentException("OUTPUT_LIMIT_EXCEEDED");}@Override public synchronized void write(int b){check(1);super.write(b);}@Override public synchronized void write(byte[]b,int o,int l){check(l);super.write(b,o,l);}}
}
