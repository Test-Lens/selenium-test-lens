package io.github.testlens.selector.tooling;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.json.JsonFactory;

import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static io.github.testlens.selector.tooling.SelectorIndexModel.*;

final class SelectorIndexJson {
    static final int MAX_DOCUMENT_BYTES = 64 * 1024 * 1024;
    private static final int MAX_FILES = 20_000, MAX_DECLARATIONS = 100_000, MAX_CHILDREN = 64;
    private static final JsonFactory FACTORY = JsonFactory.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .streamReadConstraints(StreamReadConstraints.builder().maxDocumentLength(MAX_DOCUMENT_BYTES)
                    .maxNestingDepth(32).maxStringLength(1_048_576).maxNameLength(128)
                    .maxNumberLength(20).maxTokenCount(8_000_000).build()).build();
    private SelectorIndexJson() {
    }

    static String serialize(SelectorIndex index) {
        Json out = new Json();
        out.object(() -> {
            out.number("schemaVersion", SCHEMA_VERSION);
            out.name("generator").object(() -> {
                out.string("name", GENERATOR_NAME);
                out.string("version", GENERATOR_VERSION);
            });
            out.name("project").object(() -> project(out, index.project()));
            out.name("issues").array(index.issues(), issue -> issue(out, issue));
            out.name("files").array(index.files(), file -> file(out, file));
            out.name("coverage").object(() -> coverage(out, index.coverage()));
        });
        return out.value();
    }

    static SelectorIndex read(Path path) throws IOException {
        if (Files.size(path) > MAX_DOCUMENT_BYTES) throw new IndexFormatException("Selector index exceeds hard document limit");
        try (InputStream in = Files.newInputStream(path); JsonParser parser = FACTORY.createParser(ObjectReadContext.empty(), in)) {
            return parse(parser);
        } catch (IndexFormatException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw new IndexFormatException("Invalid selector index: " + failure.getMessage(), failure);
        }
    }

    private static SelectorIndex parse(JsonParser parser) throws IOException {
        expect(parser.nextToken(), JsonToken.START_OBJECT, "selector-index object");
        Integer schema = null; ProjectMetadata project = null; List<SourceFileIndex> files = null;
        List<Issue> issues = null; Coverage coverage = null; boolean generator = false;
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String name = property(parser, "selector-index"); JsonToken value = parser.nextToken();
            switch (name) {
                case "schemaVersion" -> schema = integer(parser, value, name);
                case "generator" -> { parseGenerator(parser, value); generator = true; }
                case "project" -> project = parseProject(parser, value);
                case "issues" -> issues = parseIssues(parser, value);
                case "files" -> files = parseFiles(parser, value);
                case "coverage" -> coverage = parseCoverage(parser, value);
                default -> extension(parser, name, value, "selector-index");
            }
        }
        if (schema == null || schema != SCHEMA_VERSION) throw error("Unsupported selector-index schemaVersion: " + schema);
        if (!generator || project == null || files == null || issues == null || coverage == null) throw error("Selector-index required fields missing");
        int declarations = files.stream().mapToInt(file -> file.declarations().size()).sum();
        if (coverage.declarationsFound() != declarations) throw error("Coverage declaration count does not match files");
        return new SelectorIndex(project, files, coverage, issues);
    }

    private static void parseGenerator(JsonParser p, JsonToken token) throws IOException {
        expect(token, JsonToken.START_OBJECT, "generator object"); String name = null, version = null;
        while (p.nextToken() != JsonToken.END_OBJECT) { String field = property(p, "generator"); JsonToken value = p.nextToken();
            if (field.equals("name")) name = string(p, value, field); else if (field.equals("version")) version = string(p, value, field); else extension(p, field, value, "generator"); }
        if (name == null || version == null) throw error("Generator required fields missing");
    }

    private static ProjectMetadata parseProject(JsonParser p, JsonToken token) throws IOException {
        expect(token, JsonToken.START_OBJECT, "project object"); Map<String,String> strings = new HashMap<>(); List<String> roots = null;
        while (p.nextToken() != JsonToken.END_OBJECT) { String name = property(p, "project"); JsonToken value = p.nextToken();
            if (name.equals("sourceRoots")) roots = strings(p, value, name);
            else if (Set.of("sourceRevision","parserVersion","languageLevel","encoding","toolConfigFingerprint","classpathFingerprint").contains(name)) strings.put(name, string(p, value, name));
            else extension(p, name, value, "project"); }
        if (roots == null || missing(strings,"parserVersion","languageLevel","encoding","toolConfigFingerprint","classpathFingerprint")) throw error("Project required fields missing");
        roots.forEach(SelectorIndexJson::logicalPath); return new ProjectMetadata(strings.get("sourceRevision"), roots, strings.get("parserVersion"), strings.get("languageLevel"), strings.get("encoding"), strings.get("toolConfigFingerprint"), strings.get("classpathFingerprint"));
    }

    private static List<SourceFileIndex> parseFiles(JsonParser p, JsonToken token) throws IOException {
        expect(token, JsonToken.START_ARRAY, "files array"); List<SourceFileIndex> values = new ArrayList<>();
        while (p.nextToken() != JsonToken.END_ARRAY) { if (values.size() >= MAX_FILES) throw error("Too many indexed files"); values.add(parseFile(p)); }
        return List.copyOf(values);
    }

    private static SourceFileIndex parseFile(JsonParser p) throws IOException {
        expect(p.currentToken(), JsonToken.START_OBJECT, "file object"); Map<String,String> strings = new HashMap<>(); Boolean generated = null, readOnly = null; ParseStatus status = null; List<Issue> issues = null; List<DeclarationRecord> declarations = null;
        while (p.nextToken() != JsonToken.END_OBJECT) { String name = property(p, "file"); JsonToken value = p.nextToken(); switch (name) {
            case "logicalPath","sourceLanguage","encoding","contentHash" -> strings.put(name,string(p,value,name));
            case "generated" -> generated=bool(p,value,name); case "readOnly" -> readOnly=bool(p,value,name);
            case "parseStatus" -> status=enumValue(ParseStatus.class,string(p,value,name),name);
            case "issues" -> issues=parseIssues(p,value); case "declarations" -> declarations=parseDeclarations(p,value);
            default -> extension(p,name,value,"file"); }}
        if (missing(strings,"logicalPath","sourceLanguage","encoding") || generated==null || readOnly==null || status==null || issues==null || declarations==null) throw error("File required fields missing");
        logicalPath(strings.get("logicalPath")); hash(strings.get("contentHash"),"contentHash",true);
        return new SourceFileIndex(strings.get("logicalPath"),strings.get("sourceLanguage"),strings.get("encoding"),strings.get("contentHash"),generated,readOnly,status,issues,declarations);
    }

    private static List<DeclarationRecord> parseDeclarations(JsonParser p, JsonToken token) throws IOException {
        expect(token,JsonToken.START_ARRAY,"declarations array"); List<DeclarationRecord> values=new ArrayList<>();
        while(p.nextToken()!=JsonToken.END_ARRAY){if(values.size()>=MAX_DECLARATIONS)throw error("Too many declarations");values.add(parseDeclaration(p));}return List.copyOf(values);
    }

    private static DeclarationRecord parseDeclaration(JsonParser p) throws IOException {
        expect(p.currentToken(),JsonToken.START_OBJECT,"declaration object");Map<String,String>s=new HashMap<>();Integer schema=null;Boolean generated=null,readOnly=null;SourceRange range=null;DeclaringSymbol symbol=null;DeclarationKind kind=null;LocatorExpression expression=null;ResolvedLocator locator=null;ResolutionStatus status=null;
        while(p.nextToken()!=JsonToken.END_OBJECT){String name=property(p,"declaration");JsonToken value=p.nextToken();switch(name){
            case"schemaVersion"->schema=integer(p,value,name);case"declarationRef","sourceLanguage","logicalPath","contentFingerprint"->s.put(name,string(p,value,name));
            case"sourceRange"->range=parseRange(p,value);case"declaringSymbol"->symbol=parseSymbol(p,value);case"declarationKind"->kind=enumValue(DeclarationKind.class,string(p,value,name),name);
            case"locatorExpression"->expression=parseExpression(p,value,0);case"resolvedLocator"->locator=parseLocator(p,value);case"resolutionStatus"->status=enumValue(ResolutionStatus.class,string(p,value,name),name);
            case"generated"->generated=bool(p,value,name);case"readOnly"->readOnly=bool(p,value,name);default->extension(p,name,value,"declaration");}}
        if(schema==null||schema!=SCHEMA_VERSION)throw error("Unsupported declaration schemaVersion: "+schema);
        if(missing(s,"declarationRef","sourceLanguage","logicalPath","contentFingerprint")||range==null||symbol==null||kind==null||expression==null||status==null||generated==null||readOnly==null)throw error("Declaration required fields missing");
        if(!s.get("declarationRef").matches("java-decl-v1:sha256:[0-9a-f]{64}"))throw error("Malformed declarationRef");logicalPath(s.get("logicalPath"));hash(s.get("contentFingerprint"),"contentFingerprint",false);
        if(status==ResolutionStatus.RESOLVED&&locator==null&&expression.kind()==ExpressionKind.SINGLE)throw error("Resolved single declaration requires resolvedLocator");
        return new DeclarationRecord(schema,s.get("declarationRef"),s.get("sourceLanguage"),s.get("logicalPath"),range,symbol,kind,expression,locator,status,generated,readOnly,s.get("contentFingerprint"));
    }

    private static SourceRange parseRange(JsonParser p,JsonToken token)throws IOException{expect(token,JsonToken.START_OBJECT,"sourceRange object");Map<String,Integer>n=new HashMap<>();String unit=null;while(p.nextToken()!=JsonToken.END_OBJECT){String name=property(p,"sourceRange");JsonToken value=p.nextToken();if(name.equals("offsetUnit"))unit=string(p,value,name);else if(Set.of("startLine","startColumn","endLine","endColumn","startOffset","endOffsetExclusive").contains(name))n.put(name,integer(p,value,name));else extension(p,name,value,"sourceRange");}if(n.size()!=6||!"UTF16_CODE_UNIT".equals(unit))throw error("Invalid sourceRange");SourceRange r=new SourceRange(n.get("startLine"),n.get("startColumn"),n.get("endLine"),n.get("endColumn"),n.get("startOffset"),n.get("endOffsetExclusive"));if(r.startLine()<1||r.startColumn()<1||r.endLine()<r.startLine()||r.endColumn()<1||r.startOffset()<0||r.endOffsetExclusive()<r.startOffset())throw error("Invalid sourceRange bounds");return r;}
    private static DeclaringSymbol parseSymbol(JsonParser p,JsonToken token)throws IOException{expect(token,JsonToken.START_OBJECT,"declaringSymbol object");Map<String,String>s=new HashMap<>();while(p.nextToken()!=JsonToken.END_OBJECT){String n=property(p,"declaringSymbol");JsonToken v=p.nextToken();if(Set.of("kind","qualifiedTypeName","memberSignature","localScopeFingerprint").contains(n))s.put(n,string(p,v,n));else extension(p,n,v,"declaringSymbol");}if(s.get("kind")==null||s.get("kind").isBlank())throw error("Declaring symbol kind required");return new DeclaringSymbol(s.get("kind"),s.get("qualifiedTypeName"),s.get("memberSignature"),s.get("localScopeFingerprint"));}
    private static LocatorExpression parseExpression(JsonParser p,JsonToken token,int depth)throws IOException{if(depth>16)throw error("Expression nesting exceeds limit");expect(token,JsonToken.START_OBJECT,"locatorExpression object");ExpressionKind kind=null;String normalized=null,factory=null;List<String>dependencies=null;List<LocatorExpressionChild>children=null;while(p.nextToken()!=JsonToken.END_OBJECT){String n=property(p,"locatorExpression");JsonToken v=p.nextToken();switch(n){case"kind"->kind=enumValue(ExpressionKind.class,string(p,v,n),n);case"normalizedExpression"->normalized=string(p,v,n);case"factorySymbol"->factory=string(p,v,n);case"parameterDependencies"->dependencies=strings(p,v,n);case"children"->children=parseChildren(p,v,depth+1);default->extension(p,n,v,"locatorExpression");}}if(kind==null||normalized==null||dependencies==null||children==null)throw error("Locator expression required fields missing");return new LocatorExpression(kind,normalized,factory,dependencies,children);}
    private static List<LocatorExpressionChild> parseChildren(JsonParser p,JsonToken token,int depth)throws IOException{expect(token,JsonToken.START_ARRAY,"expression children");List<LocatorExpressionChild>out=new ArrayList<>();while(p.nextToken()!=JsonToken.END_ARRAY){if(out.size()>=MAX_CHILDREN)throw error("Too many expression children");expect(p.currentToken(),JsonToken.START_OBJECT,"expression child");ExpressionKind kind=null;String normalized=null,factory=null;List<String>deps=null;List<LocatorExpressionChild>children=null;ResolvedLocator locator=null;ResolutionStatus status=null;while(p.nextToken()!=JsonToken.END_OBJECT){String n=property(p,"expression child");JsonToken v=p.nextToken();switch(n){case"kind"->kind=enumValue(ExpressionKind.class,string(p,v,n),n);case"normalizedExpression"->normalized=string(p,v,n);case"factorySymbol"->factory=string(p,v,n);case"parameterDependencies"->deps=strings(p,v,n);case"children"->children=parseChildren(p,v,depth+1);case"resolvedLocator"->locator=parseLocator(p,v);case"resolutionStatus"->status=enumValue(ResolutionStatus.class,string(p,v,n),n);default->extension(p,n,v,"expression child");}}if(kind==null||normalized==null||deps==null||children==null||status==null)throw error("Expression child required fields missing");out.add(new LocatorExpressionChild(new LocatorExpression(kind,normalized,factory,deps,children),locator,status));}return List.copyOf(out);}
    private static ResolvedLocator parseLocator(JsonParser p,JsonToken token)throws IOException{expect(token,JsonToken.START_OBJECT,"resolvedLocator object");String strategy=null,value=null;while(p.nextToken()!=JsonToken.END_OBJECT){String n=property(p,"resolvedLocator");JsonToken v=p.nextToken();if(n.equals("strategy"))strategy=string(p,v,n);else if(n.equals("value"))value=string(p,v,n);else extension(p,n,v,"resolvedLocator");}if(strategy==null||strategy.isBlank()||value==null)throw error("Resolved locator required fields missing");return new ResolvedLocator(strategy,value);}
    private static List<Issue> parseIssues(JsonParser p,JsonToken token)throws IOException{expect(token,JsonToken.START_ARRAY,"issues array");List<Issue>out=new ArrayList<>();while(p.nextToken()!=JsonToken.END_ARRAY){expect(p.currentToken(),JsonToken.START_OBJECT,"issue object");String code=null,message=null,path=null;while(p.nextToken()!=JsonToken.END_OBJECT){String n=property(p,"issue");JsonToken v=p.nextToken();if(n.equals("code"))code=string(p,v,n);else if(n.equals("message"))message=string(p,v,n);else if(n.equals("logicalPath"))path=string(p,v,n);else extension(p,n,v,"issue");}if(code==null||message==null)throw error("Issue required fields missing");if(path!=null)logicalPath(path);out.add(new Issue(code,message,path));}return List.copyOf(out);}
    private static Coverage parseCoverage(JsonParser p,JsonToken token)throws IOException{expect(token,JsonToken.START_OBJECT,"coverage object");Map<String,Integer>n=new HashMap<>();Boolean incomplete=null;Set<String>ints=Set.of("sourceRootsRequested","sourceRootsFound","filesDiscovered","filesParsed","filesFailed","filesExcluded","generatedFilesExcluded","unsupportedLanguageFiles","declarationsFound","resolved","partiallyResolved","dynamic","custom","unsupported","errors","symbolResolutionIssues");while(p.nextToken()!=JsonToken.END_OBJECT){String name=property(p,"coverage");JsonToken value=p.nextToken();if(ints.contains(name)){int x=integer(p,value,name);if(x<0)throw error("Negative coverage value: "+name);n.put(name,x);}else if(name.equals("incompleteClasspath"))incomplete=bool(p,value,name);else extension(p,name,value,"coverage");}if(n.size()!=ints.size()||incomplete==null)throw error("Coverage required fields missing");return new Coverage(n.get("sourceRootsRequested"),n.get("sourceRootsFound"),n.get("filesDiscovered"),n.get("filesParsed"),n.get("filesFailed"),n.get("filesExcluded"),n.get("generatedFilesExcluded"),n.get("unsupportedLanguageFiles"),n.get("declarationsFound"),n.get("resolved"),n.get("partiallyResolved"),n.get("dynamic"),n.get("custom"),n.get("unsupported"),n.get("errors"),incomplete,n.get("symbolResolutionIssues"));}
    private static List<String> strings(JsonParser p,JsonToken token,String name)throws IOException{expect(token,JsonToken.START_ARRAY,name+" array");List<String>out=new ArrayList<>();while(p.nextToken()!=JsonToken.END_ARRAY)out.add(string(p,p.currentToken(),name));return List.copyOf(out);}
    private static String property(JsonParser p,String where){expect(p.currentToken(),JsonToken.PROPERTY_NAME,where+" field");return p.currentName();}
    private static String string(JsonParser p,JsonToken token,String name){expect(token,JsonToken.VALUE_STRING,name+" string");return p.getString();}
    private static int integer(JsonParser p,JsonToken token,String name){expect(token,JsonToken.VALUE_NUMBER_INT,name+" integer");return p.getIntValue();}
    private static boolean bool(JsonParser p,JsonToken token,String name){if(token==JsonToken.VALUE_TRUE)return true;if(token==JsonToken.VALUE_FALSE)return false;throw error("Expected "+name+" boolean");}
    private static <E extends Enum<E>>E enumValue(Class<E>type,String value,String name){try{return Enum.valueOf(type,value);}catch(IllegalArgumentException e){throw error("Unsupported "+name+": "+value);}}
    private static void extension(JsonParser p,String name,JsonToken token,String where)throws IOException{if(!name.startsWith("x-"))throw error("Unknown "+where+" field: "+name);if(token==JsonToken.START_ARRAY||token==JsonToken.START_OBJECT)p.skipChildren();}
    private static void logicalPath(String value){if(value==null||value.isBlank()||value.contains("\\")||value.startsWith("/")||value.matches("^[A-Za-z]:.*")||List.of(value.split("/")).contains(".."))throw error("Unsafe logical path: "+value);}
    private static void hash(String value,String name,boolean optional){if(value==null&&optional)return;if(value==null||!value.matches("sha256:[0-9a-f]{64}"))throw error("Malformed "+name);}
    private static boolean missing(Map<String,String>map,String...names){for(String name:names)if(map.get(name)==null)return true;return false;}
    private static void expect(JsonToken actual,JsonToken expected,String what){if(actual!=expected)throw error("Expected "+what+" but found "+actual);}
    private static IndexFormatException error(String message){return new IndexFormatException(message);}
    static final class IndexFormatException extends IllegalArgumentException{IndexFormatException(String message){super(message);}IndexFormatException(String message,Throwable cause){super(message,cause);}}

    static void writeLocal(SelectorIndex index, Path projectRoot, Path output) throws IOException {
        Path root = projectRoot.toAbsolutePath().normalize().toRealPath();
        Path target = root.resolve("target").normalize();
        Path destination = output.isAbsolute() ? output.normalize() : root.resolve(output).normalize();
        if (!destination.startsWith(target)) {
            throw new IllegalArgumentException("Selector index output must be under project target/: " + output);
        }
        Files.createDirectories(destination.getParent());
        Path temporary = Files.createTempFile(destination.getParent(), "selector-index-", ".tmp");
        try {
            Files.writeString(temporary, serialize(index), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void project(Json out, ProjectMetadata project) {
        if (project.sourceRevision() != null) out.string("sourceRevision", project.sourceRevision());
        out.name("sourceRoots").array(project.sourceRoots(), out::string);
        out.string("parserVersion", project.parserVersion());
        out.string("languageLevel", project.languageLevel());
        out.string("encoding", project.encoding());
        out.string("toolConfigFingerprint", project.toolConfigFingerprint());
        out.string("classpathFingerprint", project.classpathFingerprint());
    }

    private static void file(Json out, SourceFileIndex file) {
        out.object(() -> {
            out.string("logicalPath", file.logicalPath());
            out.string("sourceLanguage", file.sourceLanguage());
            out.string("encoding", file.encoding());
            if (file.contentHash() != null) out.string("contentHash", file.contentHash());
            out.bool("generated", file.generated());
            out.bool("readOnly", file.readOnly());
            out.string("parseStatus", file.parseStatus().name());
            out.name("issues").array(file.issues(), issue -> issue(out, issue));
            out.name("declarations").array(file.declarations(), declaration -> declaration(out, declaration));
        });
    }

    private static void declaration(Json out, DeclarationRecord declaration) {
        out.object(() -> {
            out.number("schemaVersion", declaration.schemaVersion());
            out.string("declarationRef", declaration.declarationRef());
            out.string("sourceLanguage", declaration.sourceLanguage());
            out.string("logicalPath", declaration.logicalPath());
            out.name("sourceRange").object(() -> range(out, declaration.sourceRange()));
            out.name("declaringSymbol").object(() -> symbol(out, declaration.declaringSymbol()));
            out.string("declarationKind", declaration.declarationKind().name());
            out.name("locatorExpression").object(() -> expression(out, declaration.locatorExpression()));
            if (declaration.resolvedLocator() != null) {
                out.name("resolvedLocator").object(() -> {
                    out.string("strategy", declaration.resolvedLocator().strategy());
                    out.string("value", declaration.resolvedLocator().value());
                });
            }
            out.string("resolutionStatus", declaration.resolutionStatus().name());
            out.bool("generated", declaration.generated());
            out.bool("readOnly", declaration.readOnly());
            out.string("contentFingerprint", declaration.contentFingerprint());
        });
    }

    private static void range(Json out, SourceRange range) {
        out.number("startLine", range.startLine());
        out.number("startColumn", range.startColumn());
        out.number("endLine", range.endLine());
        out.number("endColumn", range.endColumn());
        out.number("startOffset", range.startOffset());
        out.number("endOffsetExclusive", range.endOffsetExclusive());
        out.string("offsetUnit", "UTF16_CODE_UNIT");
    }

    private static void symbol(Json out, DeclaringSymbol symbol) {
        out.string("kind", symbol.kind());
        if (symbol.qualifiedTypeName() != null) out.string("qualifiedTypeName", symbol.qualifiedTypeName());
        if (symbol.memberSignature() != null) out.string("memberSignature", symbol.memberSignature());
        if (symbol.localScopeFingerprint() != null) out.string("localScopeFingerprint", symbol.localScopeFingerprint());
    }

    private static void expression(Json out, LocatorExpression expression) {
        out.string("kind", expression.kind().name());
        out.string("normalizedExpression", expression.normalizedExpression());
        if (expression.factorySymbol() != null) out.string("factorySymbol", expression.factorySymbol());
        out.name("parameterDependencies").array(expression.parameterDependencies(), out::string);
        out.name("children").array(expression.children(), child -> out.object(() -> {
            expression(out, child.expression());
            if (child.resolvedLocator() != null) {
                out.name("resolvedLocator").object(() -> {
                    out.string("strategy", child.resolvedLocator().strategy());
                    out.string("value", child.resolvedLocator().value());
                });
            }
            out.string("resolutionStatus", child.resolutionStatus().name());
        }));
    }

    private static void issue(Json out, Issue issue) {
        out.object(() -> {
            out.string("code", issue.code());
            out.string("message", issue.message());
            if (issue.logicalPath() != null) out.string("logicalPath", issue.logicalPath());
        });
    }

    private static void coverage(Json out, Coverage coverage) {
        out.number("sourceRootsRequested", coverage.sourceRootsRequested());
        out.number("sourceRootsFound", coverage.sourceRootsFound());
        out.number("filesDiscovered", coverage.filesDiscovered());
        out.number("filesParsed", coverage.filesParsed());
        out.number("filesFailed", coverage.filesFailed());
        out.number("filesExcluded", coverage.filesExcluded());
        out.number("generatedFilesExcluded", coverage.generatedFilesExcluded());
        out.number("unsupportedLanguageFiles", coverage.unsupportedLanguageFiles());
        out.number("declarationsFound", coverage.declarationsFound());
        out.number("resolved", coverage.resolved());
        out.number("partiallyResolved", coverage.partiallyResolved());
        out.number("dynamic", coverage.dynamic());
        out.number("custom", coverage.custom());
        out.number("unsupported", coverage.unsupported());
        out.number("errors", coverage.errors());
        out.bool("incompleteClasspath", coverage.incompleteClasspath());
        out.number("symbolResolutionIssues", coverage.symbolResolutionIssues());
    }

    private static final class Json {
        private final StringBuilder out = new StringBuilder();
        private boolean first = true;

        String value() {
            return out.toString();
        }

        Json name(String name) {
            separator();
            quote(name);
            out.append(':');
            return this;
        }

        void object(Runnable body) {
            out.append('{');
            boolean outer = first;
            first = true;
            body.run();
            out.append('}');
            first = outer;
        }

        <T> void array(List<T> values, java.util.function.Consumer<T> writer) {
            out.append('[');
            boolean outer = first;
            first = true;
            for (T value : values) {
                separator();
                writer.accept(value);
            }
            out.append(']');
            first = outer;
        }

        void string(String name, String value) {
            name(name);
            quote(value);
        }

        void string(String value) {
            quote(value);
        }

        void number(String name, long value) {
            name(name);
            out.append(value);
        }

        void bool(String name, boolean value) {
            name(name);
            out.append(value);
        }

        private void separator() {
            if (!first) out.append(',');
            first = false;
        }

        private void quote(String value) {
            out.append('"');
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                switch (c) {
                    case '"' -> out.append("\\\"");
                    case '\\' -> out.append("\\\\");
                    case '\b' -> out.append("\\b");
                    case '\f' -> out.append("\\f");
                    case '\n' -> out.append("\\n");
                    case '\r' -> out.append("\\r");
                    case '\t' -> out.append("\\t");
                    default -> {
                        if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                        else out.append(c);
                    }
                }
            }
            out.append('"');
        }
    }
}
