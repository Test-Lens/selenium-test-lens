package io.github.testlens.selector.tooling;

import io.github.testlens.selector.engine.CanonicalDigests;
import io.github.testlens.selector.engine.SelectorPolicy;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.json.JsonFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Strict streaming policy codec. Deliberately no databind or reflection mapping. */
final class SelectorPolicyJson {
    static final int MAX_DOCUMENT_BYTES = 1_048_576;
    static final int MAX_STRING_LENGTH = 16_384;
    private static final JsonFactory FACTORY = JsonFactory.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .streamReadConstraints(StreamReadConstraints.builder()
                    .maxDocumentLength(MAX_DOCUMENT_BYTES)
                    .maxNestingDepth(20)
                    .maxStringLength(MAX_STRING_LENGTH)
                    .maxNameLength(128)
                    .maxNumberLength(12)
                    .maxTokenCount(250_000)
                    .build())
            .build();

    private SelectorPolicyJson() { }

    static SelectorPolicy.Document read(Path path) throws IOException {
        long size=Files.size(path);
        if(size>MAX_DOCUMENT_BYTES)throw new PolicyFormatException("Policy document exceeds "+MAX_DOCUMENT_BYTES+" bytes");
        try(var input=Files.newInputStream(path); JsonParser parser=FACTORY.createParser(ObjectReadContext.empty(),input)){
            return parse(parser);
        } catch (RuntimeException error) {
            if(error instanceof PolicyFormatException format)throw format;
            throw new PolicyFormatException("Invalid selector policy JSON: "+error.getMessage(),error);
        }
    }

    static SelectorPolicy.Document parse(byte[] bytes) {
        if(bytes.length>MAX_DOCUMENT_BYTES)throw new PolicyFormatException("Policy document exceeds "+MAX_DOCUMENT_BYTES+" bytes");
        try(var parser=FACTORY.createParser(ObjectReadContext.empty(),new ByteArrayInputStream(bytes))){return parse(parser);}
        catch(RuntimeException error){if(error instanceof PolicyFormatException format)throw format;throw new PolicyFormatException("Invalid selector policy JSON: "+error.getMessage(),error);}
    }

    static byte[] serialize(SelectorPolicy.Document document) {
        try {
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            try(JsonGenerator g=FACTORY.createGenerator(ObjectWriteContext.empty(),out)){
                g.writeStartObject();
                g.writeNumberProperty("schemaVersion",document.schemaVersion());
                g.writeNumberProperty("canonicalizationVersion",document.canonicalizationVersion());
                g.writeArrayPropertyStart("rules");
                for(SelectorPolicy.Rule rule:document.canonical().rules())writeRule(g,rule);
                g.writeEndArray();g.writeEndObject();g.flush();
            }
            out.write('\n');
            return out.toByteArray();
        } catch(RuntimeException error){throw new PolicyFormatException("Could not serialize selector policies",error);}
    }

    static void writeAtomic(SelectorPolicy.Document document,Path projectRoot,Path destination)throws IOException{
        writeAtomic(document,projectRoot,destination,SelectorPolicyJson::replace);
    }

    static void writeAtomic(SelectorPolicy.Document document,Path projectRoot,Path destination,MoveOperation move)throws IOException{
        Path root=projectRoot.toAbsolutePath().normalize().toRealPath();
        Path target=destination.isAbsolute()?destination.toAbsolutePath().normalize():root.resolve(destination).normalize();
        if(!target.startsWith(root))throw new IllegalArgumentException("Policy destination must remain under projectRoot: "+destination);
        Path parent=target.getParent();if(parent==null)throw new IllegalArgumentException("Policy destination requires a parent directory");
        Files.createDirectories(parent);Path realParent=parent.toRealPath();
        if(!realParent.startsWith(root))throw new IllegalArgumentException("Policy destination escapes projectRoot through a link: "+destination);
        byte[] bytes=serialize(document);Path temporary=Files.createTempFile(realParent,"selector-policies-",".tmp");
        try{
            Files.write(temporary,bytes);
            move.replace(temporary,target);
        }finally{Files.deleteIfExists(temporary);}
    }

    private static void replace(Path source,Path target)throws IOException{
        try{Files.move(source,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
        catch(AtomicMoveNotSupportedException ignored){Files.move(source,target,StandardCopyOption.REPLACE_EXISTING);}
    }

    static SelectorPolicy.Document merge(SelectorPolicy.Document tracked,SelectorPolicy.Document local){
        List<SelectorPolicy.Rule> rules=new ArrayList<>(tracked.rules());rules.addAll(local.rules());
        // compilation validates duplicate IDs; return canonical coalesced model
        java.util.Map<String,SelectorPolicy.Rule> byId=new java.util.TreeMap<>();
        for(SelectorPolicy.Rule rule:rules){SelectorPolicy.Rule old=byId.putIfAbsent(rule.ruleId(),rule);if(old!=null&&!old.equals(rule))throw new PolicyFormatException("Conflicting duplicate ruleId: "+rule.ruleId());}
        return new SelectorPolicy.Document(SelectorPolicy.SCHEMA_VERSION,CanonicalDigests.CANONICALIZATION_VERSION,new ArrayList<>(byId.values()));
    }

    private static SelectorPolicy.Document parse(JsonParser p){
        expect(p.nextToken(),JsonToken.START_OBJECT,"root object");Integer schema=null,canonical=null;List<SelectorPolicy.Rule> rules=null;
        while(p.nextToken()!=JsonToken.END_OBJECT){expect(p.currentToken(),JsonToken.PROPERTY_NAME,"root field");String name=p.currentName();JsonToken value=p.nextToken();
            switch(name){case "schemaVersion"->schema=integer(p,value,name);case "canonicalizationVersion"->canonical=integer(p,value,name);case "rules"->rules=rules(p,value);default->skipExtension(p,name,value,"root");}}
        if(p.nextToken()!=null)throw error("Trailing JSON content");
        if(schema==null||canonical==null||rules==null)throw error("schemaVersion, canonicalizationVersion and rules are required");
        return new SelectorPolicy.Document(schema,canonical,rules);
    }

    private static List<SelectorPolicy.Rule> rules(JsonParser p,JsonToken token){
        expect(token,JsonToken.START_ARRAY,"rules array");List<SelectorPolicy.Rule> out=new ArrayList<>();Set<String> ids=new HashSet<>();
        while(p.nextToken()!=JsonToken.END_ARRAY){if(out.size()>=SelectorPolicy.MAX_RULES)throw error("Too many rules");SelectorPolicy.Rule r=rule(p,p.currentToken());if(!ids.add(r.ruleId()))throw error("Duplicate ruleId: "+r.ruleId());out.add(r);}return out;
    }
    private static SelectorPolicy.Rule rule(JsonParser p,JsonToken token){
        expect(token,JsonToken.START_OBJECT,"rule object");String id=null;SelectorPolicy.Decision decision=null;Integer priority=null;SelectorPolicy.Scope scope=null;SelectorPolicy.Matcher matcher=null;SelectorPolicy.Reason reason=null;
        while(p.nextToken()!=JsonToken.END_OBJECT){String name=property(p,"rule");JsonToken value=p.nextToken();switch(name){
            case "ruleId"->id=string(p,value,name);case "decision"->decision=enumValue(SelectorPolicy.Decision.class,string(p,value,name),name);
            case "priority"->priority=integer(p,value,name);case "scope"->scope=scope(p,value);case "matcher"->matcher=matcher(p,value);case "reason"->reason=reason(p,value);
            default->skipExtension(p,name,value,"rule");}}
        if(id==null||decision==null||priority==null||scope==null||matcher==null)throw error("ruleId, decision, priority, scope and matcher are required");
        return new SelectorPolicy.Rule(id,decision,priority,scope,matcher,reason);
    }
    private static SelectorPolicy.Scope scope(JsonParser p,JsonToken token){
        expect(token,JsonToken.START_OBJECT,"scope object");String module=null,path=null,ref=null,symbol=null,usageClass=null,usageMethod=null,context=null;
        while(p.nextToken()!=JsonToken.END_OBJECT){String n=property(p,"scope");JsonToken v=p.nextToken();switch(n){case "modulePath"->module=string(p,v,n);case "logicalPath"->path=string(p,v,n);case "declarationRef"->ref=string(p,v,n);case "declaringSymbol"->symbol=string(p,v,n);case "usageClass"->usageClass=string(p,v,n);case "usageMethod"->usageMethod=string(p,v,n);case "contextFingerprint"->context=string(p,v,n);default->skipExtension(p,n,v,"scope");}}
        return new SelectorPolicy.Scope(module,path,ref,symbol,usageClass,usageMethod,context);
    }
    private static SelectorPolicy.Matcher matcher(JsonParser p,JsonToken token){
        expect(token,JsonToken.START_OBJECT,"matcher object");SelectorPolicy.MatcherKind kind=null;String strategy=null,digest=null,hint=null;List<SelectorPolicy.Segment> segments=null;
        while(p.nextToken()!=JsonToken.END_OBJECT){String n=property(p,"matcher");JsonToken v=p.nextToken();switch(n){case "kind"->kind=enumValue(SelectorPolicy.MatcherKind.class,string(p,v,n),n);case "strategy"->strategy=string(p,v,n);case "valueDigest"->digest=string(p,v,n);case "displayHint"->hint=string(p,v,n);case "segments"->segments=segments(p,v);default->skipExtension(p,n,v,"matcher");}}
        if(kind==null||strategy==null)throw error("matcher kind and strategy are required");
        return switch(kind){case EXACT_VALUE_DIGEST->{if(digest==null||segments!=null)throw error("EXACT_VALUE_DIGEST requires valueDigest and forbids segments");yield new SelectorPolicy.ExactMatcher(strategy,digest,hint);}case STRUCTURAL_PATTERN->{if(segments==null||digest!=null||hint!=null)throw error("STRUCTURAL_PATTERN requires segments and forbids exact fields");yield new SelectorPolicy.StructuralPattern(strategy,segments);}};
    }
    private static List<SelectorPolicy.Segment> segments(JsonParser p,JsonToken token){
        expect(token,JsonToken.START_ARRAY,"segments array");List<SelectorPolicy.Segment> out=new ArrayList<>();while(p.nextToken()!=JsonToken.END_ARRAY){if(out.size()>=SelectorPolicy.MAX_SEGMENTS)throw error("Too many pattern segments");out.add(segment(p,p.currentToken()));}return out;
    }
    private static SelectorPolicy.Segment segment(JsonParser p,JsonToken token){
        expect(token,JsonToken.START_OBJECT,"segment object");SelectorPolicy.SegmentKind kind=null;String literal=null,alphabet=null;Integer min=null,max=null;
        while(p.nextToken()!=JsonToken.END_OBJECT){String n=property(p,"segment");JsonToken v=p.nextToken();switch(n){case "kind"->kind=enumValue(SelectorPolicy.SegmentKind.class,string(p,v,n),n);case "literal"->literal=string(p,v,n);case "minLength"->min=integer(p,v,n);case "maxLength"->max=integer(p,v,n);case "alphabet"->alphabet=string(p,v,n);default->skipExtension(p,n,v,"segment");}}
        if(kind==null)throw error("segment kind is required");if(kind==SelectorPolicy.SegmentKind.LITERAL){if(literal==null||min!=null||max!=null||alphabet!=null)throw error("LITERAL requires only literal");return SelectorPolicy.Segment.literal(literal);}if(min==null||max==null||literal!=null)throw error("Run segment requires minLength/maxLength and forbids literal");return kind==SelectorPolicy.SegmentKind.OPAQUE_RUN?SelectorPolicy.Segment.opaque(alphabet,min,max):SelectorPolicy.Segment.run(kind,min,max);
    }
    private static SelectorPolicy.Reason reason(JsonParser p,JsonToken token){
        expect(token,JsonToken.START_OBJECT,"reason object");String code=null,note=null;while(p.nextToken()!=JsonToken.END_OBJECT){String n=property(p,"reason");JsonToken v=p.nextToken();switch(n){case "code"->code=string(p,v,n);case "note"->note=string(p,v,n);default->skipExtension(p,n,v,"reason");}}if(code==null)throw error("reason code is required");return new SelectorPolicy.Reason(code,note);
    }

    private static void writeRule(JsonGenerator g,SelectorPolicy.Rule r){g.writeStartObject();g.writeStringProperty("ruleId",r.ruleId());g.writeStringProperty("decision",r.decision().name());g.writeNumberProperty("priority",r.priority());g.writeObjectPropertyStart("scope");field(g,"modulePath",r.scope().modulePath());field(g,"logicalPath",r.scope().logicalPath());field(g,"declarationRef",r.scope().declarationRef());field(g,"declaringSymbol",r.scope().declaringSymbol());field(g,"usageClass",r.scope().usageClass());field(g,"usageMethod",r.scope().usageMethod());field(g,"contextFingerprint",r.scope().contextFingerprint());g.writeEndObject();g.writeObjectPropertyStart("matcher");g.writeStringProperty("kind",r.matcher().kind().name());g.writeStringProperty("strategy",r.matcher().strategy());if(r.matcher() instanceof SelectorPolicy.ExactMatcher e){g.writeStringProperty("valueDigest",e.valueDigest());field(g,"displayHint",e.displayHint());}else if(r.matcher() instanceof SelectorPolicy.StructuralPattern pattern){g.writeArrayPropertyStart("segments");for(SelectorPolicy.Segment s:pattern.segments()){g.writeStartObject();g.writeStringProperty("kind",s.kind().name());if(s.kind()==SelectorPolicy.SegmentKind.LITERAL)g.writeStringProperty("literal",s.literal());else{g.writeNumberProperty("minLength",s.minLength());g.writeNumberProperty("maxLength",s.maxLength());field(g,"alphabet",s.alphabet());}g.writeEndObject();}g.writeEndArray();}g.writeEndObject();g.writeObjectPropertyStart("reason");g.writeStringProperty("code",r.reason().code());field(g,"note",r.reason().note());g.writeEndObject();g.writeEndObject();}
    private static void field(JsonGenerator g,String name,String value){if(value!=null)g.writeStringProperty(name,value);}
    private static String property(JsonParser p,String where){expect(p.currentToken(),JsonToken.PROPERTY_NAME,where+" field");return p.currentName();}
    private static String string(JsonParser p,JsonToken t,String name){expect(t,JsonToken.VALUE_STRING,name+" string");return p.getString();}
    private static int integer(JsonParser p,JsonToken t,String name){expect(t,JsonToken.VALUE_NUMBER_INT,name+" integer");return p.getIntValue();}
    private static <E extends Enum<E>>E enumValue(Class<E> type,String value,String name){try{return Enum.valueOf(type,value);}catch(IllegalArgumentException e){throw error("Unsupported "+name+": "+value);}}
    private static void skipExtension(JsonParser p,String name,JsonToken token,String where){
        if(!name.startsWith("x-"))throw error("Unknown "+where+" field: "+name);
        if(token==JsonToken.START_ARRAY||token==JsonToken.START_OBJECT)p.skipChildren(); else p.finishToken();
    }
    private static void expect(JsonToken actual,JsonToken expected,String what){if(actual!=expected)throw error("Expected "+what+" but found "+actual);}
    private static PolicyFormatException error(String message){return new PolicyFormatException(message);}

    static final class PolicyFormatException extends IllegalArgumentException {
        PolicyFormatException(String message){super(message);}PolicyFormatException(String message,Throwable cause){super(message,cause);}
    }
    @FunctionalInterface interface MoveOperation { void replace(Path source,Path target)throws IOException; }
}
