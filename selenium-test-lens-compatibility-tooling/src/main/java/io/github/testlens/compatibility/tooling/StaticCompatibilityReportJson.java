package io.github.testlens.compatibility.tooling;

import io.github.testlens.compatibility.engine.StaticCompatibilityReport;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.json.JsonFactory;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.*;

/** Canonical bounded JSON output for STATIC compatibility evidence. */
public final class StaticCompatibilityReportJson {
    public static final long MAX_OUTPUT_BYTES=64L*1024*1024;
    private static final JsonFactory FACTORY=JsonFactory.builder().build();
    public byte[] write(StaticCompatibilityReport report){Objects.requireNonNull(report);try{Bounded out=new Bounded(MAX_OUTPUT_BYTES);try(JsonGenerator g=FACTORY.createGenerator(ObjectWriteContext.empty(),out)){value(g,report);g.writeRaw('\n');}return out.toByteArray();}catch(IOException e){throw new CompatibilityReportJson.ReportFormatException("Static report cannot be serialized",e);}}
    public Path writeDefault(StaticCompatibilityReport r,Path root)throws IOException{Path p=CompatibilityReportJson.safeDestination(root,Path.of("target","test-lens","compatibility","reports","compatibility-static-v1.json"),"compatibility-static-v1.json");CompatibilityReportJson.atomic(p,write(r));return p;}
    private static void value(JsonGenerator g,Object v)throws IOException{if(v==null){g.writeNull();return;}if(v instanceof String s){g.writeString(s);return;}if(v instanceof Integer n){g.writeNumber(n);return;}if(v instanceof Long n){g.writeNumber(n);return;}if(v instanceof Boolean b){g.writeBoolean(b);return;}if(v instanceof Enum<?>e){g.writeString(e.name());return;}if(v instanceof Map<?,?>m){g.writeStartObject();for(var e:m.entrySet().stream().sorted(Comparator.comparing(x->String.valueOf(x.getKey()))).toList()){g.writeName(String.valueOf(e.getKey()));value(g,e.getValue());}g.writeEndObject();return;}if(v instanceof Collection<?>c){g.writeStartArray();for(Object x:c)value(g,x);g.writeEndArray();return;}if(v.getClass().isRecord()){g.writeStartObject();for(var c:v.getClass().getRecordComponents())try{g.writeName(c.getName());value(g,c.getAccessor().invoke(v));}catch(IllegalAccessException|InvocationTargetException e){throw new IOException(e);}g.writeEndObject();return;}throw new IOException("unsupported static report value");}
    private static final class Bounded extends ByteArrayOutputStream{final long max;Bounded(long max){this.max=max;}void check(int n){if(count+(long)n>max)throw new CompatibilityReportJson.ReportFormatException("OUTPUT_LIMIT_EXCEEDED");}@Override public synchronized void write(int b){check(1);super.write(b);}@Override public synchronized void write(byte[]b,int o,int l){check(l);super.write(b,o,l);}}
}
