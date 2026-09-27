package io.github.testlens.compatibility.tooling;

import com.github.javaparser.*;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.*;
import io.github.testlens.compatibility.engine.CompatibilityDigests;
import io.github.testlens.compatibility.engine.StaticCompatibilityReport;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import io.github.testlens.compatibility.engine.CompatibilityComparisonReport.CausalState;
import io.github.testlens.compatibility.engine.CompatibilityComparisonReport.CodeChangeRequired;
import io.github.testlens.compatibility.engine.CompatibilityComparisonReport.Confidence;
import io.github.testlens.compatibility.engine.CompatibilityComparisonReport.FindingCategory;
import io.github.testlens.compatibility.engine.CompatibilityComparisonReport.RecommendationCode;
import io.github.testlens.compatibility.engine.CompatibilityComparisonReport.Severity;
import io.github.testlens.compatibility.engine.CompatibilityComparisonReport.StatementKind;
import static io.github.testlens.compatibility.engine.StaticCompatibilityReport.*;

/** Explicit, offline, symbol-aware Java source analyzer. It never loads or executes project classes. */
public final class StaticCompatibilityAnalyzer {
    private static final Set<String> ROBOT_INPUT=Set.of("mouseMove","mousePress","mouseRelease","keyPress","keyRelease");
    private static final Set<String> DESKTOP=Set.of("open","browse","mail","edit","print");
    private static final Set<String> HEADLESS_PROPERTIES=Set.of("java.awt.headless","headed","testLens.headless","TEST_LENS_HEADLESS");
    private static final int MAX_FINDINGS_FILE=64, MAX_CONST_DEPTH=16, MAX_CONST_VISITS=64, MAX_JS_CHARS=8192;

    public StaticCompatibilityReport analyze(StaticCompatibilityScanRequest request) throws IOException {
        Path root=request.projectRoot().toAbsolutePath().normalize().toRealPath();
        List<Path> roots=new ArrayList<>(); int rootsFound=0;
        for(Path supplied:request.sourceRoots()){
            Path candidate=(supplied.isAbsolute()?supplied:root.resolve(supplied)).normalize();
            if(!candidate.startsWith(root))throw new IllegalArgumentException("source root escapes project root");
            if(!Files.exists(candidate))continue;
            Path real=candidate.toRealPath(); if(!real.startsWith(root))throw new IllegalArgumentException("source root escapes project root through link");
            roots.add(real);rootsFound++;
        }
        CombinedTypeSolver solvers=new CombinedTypeSolver(new ReflectionTypeSolver(false));
        for(Path source:roots)solvers.add(new JavaParserTypeSolver(source));
        for(Path cp:request.classpathEntries())if(Files.exists(cp)){
            Path real=cp.toAbsolutePath().normalize().toRealPath();
            if(Files.isDirectory(real))solvers.add(new JavaParserTypeSolver(real));else if(real.toString().endsWith(".jar"))solvers.add(new JarTypeSolver(real));
        }
        ParserConfiguration config=new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17)
                .setCharacterEncoding(request.encoding()).setSymbolResolver(new JavaSymbolSolver(solvers));
        JavaParser parser=new JavaParser(config); List<Finding> findings=new ArrayList<>();List<String> issues=new ArrayList<>();
        int discovered=0,javaFiles=0,parsed=0,failed=0,excluded=0,generated=0,unsupported=0,symbolIssues=0,dynamicJs=0;
        long parseNanos=0,analysisNanos=0;AtomicLong resolutionNanos=new AtomicLong();
        for(Path sourceRoot:roots){try(Stream<Path> walk=Files.walk(sourceRoot)){
            for(Path file:walk.filter(Files::isRegularFile).sorted().toList()){
                discovered++;String unix=root.relativize(file).toString().replace('\\','/');
                if(generated(unix)){excluded++;generated++;continue;}
                if(file.toString().endsWith(".kt")){unsupported++;continue;} if(!file.toString().endsWith(".java"))continue;
                javaFiles++;if(javaFiles>StaticCompatibilityScanRequest.MAX_FILES)throw new IllegalArgumentException("source file limit exceeded");
                if(Files.size(file)>StaticCompatibilityScanRequest.MAX_SOURCE_BYTES){failed++;issues.add("SOURCE_TOO_LARGE:"+unix);continue;}
                long p=System.nanoTime();ParseResult<CompilationUnit> result=parser.parse(file);parseNanos+=System.nanoTime()-p;
                if(!result.isSuccessful()||result.getResult().isEmpty()){failed++;issues.add("PARSE_FAILED:"+unix);continue;}
                parsed++;long a=System.nanoTime();Detector detector=new Detector(unix,request.sourceTestRefs().getOrDefault(unix,""),resolutionNanos);
                detector.scan(result.getResult().orElseThrow());analysisNanos+=System.nanoTime()-a;
                findings.addAll(detector.findings.stream().limit(MAX_FINDINGS_FILE).toList());symbolIssues+=detector.resolutionIssues;dynamicJs+=detector.dynamicJs;
                if(detector.findings.size()>MAX_FINDINGS_FILE)issues.add("FINDINGS_TRUNCATED:"+unix);
            }
        }}
        findings=findings.stream().sorted(Comparator.comparing(Finding::logicalPath).thenComparing(Finding::declaringSymbol).thenComparing(Finding::code).thenComparing(Finding::findingRef)).toList();
        Map<String,Integer> byCode=new TreeMap<>();findings.forEach(f->byCode.merge(f.code(),1,Integer::sum));
        Coverage coverage=new Coverage(request.sourceRoots().size(),rootsFound,discovered,javaFiles,parsed,failed,excluded,generated,unsupported,symbolIssues,symbolIssues>0,byCode);
        List<String> limitations=new ArrayList<>();if(failed>0)limitations.add("Some Java files could not be parsed");if(symbolIssues>0)limitations.add("Classpath or symbol resolution was incomplete");if(unsupported>0)limitations.add("Non-Java source was not analyzed");if(dynamicJs>0){limitations.add("Dynamic JavaScript was not inspected: "+dynamicJs+" call(s)");issues.add("DYNAMIC_JAVASCRIPT_UNANALYZED:"+dynamicJs);}
        return new StaticCompatibilityReport(1,1,AnalysisMode.STATIC,coverage,findings,decisions(),issues,limitations,
                new Metrics(parseNanos,resolutionNanos.get(),Math.max(0,analysisNanos-resolutionNanos.get())));
    }
    private static boolean generated(String p){String x="/"+p.toLowerCase(Locale.ROOT)+"/";return x.contains("/target/generated-sources/")||x.contains("/target/generated-test-sources/");}

    private static final class Detector {
        final String path,testRef; final AtomicLong resolutionNanos; final List<Finding> findings=new ArrayList<>();int resolutionIssues,dynamicJs;
        Detector(String path,String testRef,AtomicLong timing){this.path=path;this.testRef=testRef;this.resolutionNanos=timing;}
        void scan(CompilationUnit cu){
            for(CallableDeclaration<?> callable:cu.findAll(CallableDeclaration.class))scanCallable(callable,cu);
        }
        void scanCallable(CallableDeclaration<?> callable,CompilationUnit cu){
            String symbol=declaring(callable);Map<String,List<Node>> grouped=new LinkedHashMap<>();
            boolean robotConstructed=false;
            for(ObjectCreationExpr n:callable.findAll(ObjectCreationExpr.class)){
                if(!Set.of("Robot","FileDialog","FirefoxProfile").contains(n.getType().getNameAsString()))continue;
                String type=resolveType(n);if("java.awt.Robot".equals(type))robotConstructed=true;
                if("java.awt.FileDialog".equals(type))add(grouped,"NATIVE_FILE_DIALOG",n);
                if(type.endsWith("FirefoxProfile"))add(grouped,"PROFILE_DEPENDENCY",n);
            }
            for(MethodCallExpr call:callable.findAll(MethodCallExpr.class)){
                String name=call.getNameAsString();
                if(name.equals("get")&&call.getScope().filter(ObjectCreationExpr.class::isInstance).map(ObjectCreationExpr.class::cast).filter(x->x.getArguments().stream().anyMatch(a->a.findAll(MethodCallExpr.class).stream().anyMatch(m->m.getNameAsString().equals("getWindowHandles")))).isPresent())add(grouped,"WINDOW_HANDLE_ORDER_ASSUMPTION",call);
                if(!candidateMethod(name))continue;
                String owner=resolveOwner(call);
                if("java.awt.Robot".equals(owner)&&ROBOT_INPUT.contains(name))add(grouped,"NATIVE_ROBOT_INPUT",call);
                if("java.awt.Robot".equals(owner)&&name.equals("createScreenCapture"))add(grouped,"NATIVE_ROBOT_SCREEN_CAPTURE",call);
                if("java.awt.Desktop".equals(owner)&&DESKTOP.contains(name))add(grouped,"DESKTOP_NATIVE_ACTION",call);
                if("java.awt.Toolkit".equals(owner)&&(name.equals("getScreenSize")||name.equals("getScreenResolution")))add(grouped,"TOOLKIT_SCREEN_ASSUMPTION",call);
                if("java.awt.GraphicsEnvironment".equals(owner)&&name.equals("isHeadless"))add(grouped,inConditional(call)?"EXPLICIT_HEADLESS_BRANCH":"EXPLICIT_HEADLESS_CHECK",call);
                if("java.lang.System".equals(owner)&&(name.equals("getProperty")||name.equals("getBoolean"))&&firstLiteral(call).map(HEADLESS_PROPERTIES::contains).orElse(false))add(grouped,inConditional(call)?"EXPLICIT_HEADLESS_BRANCH":"EXPLICIT_HEADLESS_CHECK",call);
                if(isJavascriptExecutor(owner,name)){Optional<String> js=constant(call.getArgument(0),cu,0,new HashSet<>());if(js.isPresent()){String lower=js.get().toLowerCase(Locale.ROOT);if(screenJs(lower))add(grouped,"JS_SCREEN_ASSUMPTION",call);if(focusJs(lower))add(grouped,"JS_FOCUS_VISIBILITY_ASSUMPTION",call);}else dynamicJs++;}
                if(windowOwner(owner)){if(name.equals("setSize"))add(grouped,"EXPLICIT_WINDOW_SIZE_CONFIGURATION",call);if(name.equals("maximize")||name.equals("fullscreen"))add(grouped,"WINDOW_MAXIMIZE_OR_FULLSCREEN",call);}
                if((owner.equals("org.openqa.selenium.chrome.ChromeOptions")||owner.equals("org.openqa.selenium.firefox.FirefoxOptions"))&&name.equals("addArguments")){for(Expression arg:call.getArguments())constant(arg,cu,0,new HashSet<>()).ifPresent(v->{String x=v.toLowerCase(Locale.ROOT);if(x.equals("--headless")||x.startsWith("--headless=")||x.equals("-headless"))add(grouped,"EXPLICIT_HEADLESS_CONFIGURATION",call);if(x.startsWith("--user-data-dir"))add(grouped,"PROFILE_DEPENDENCY",call);});}
            }
            if(grouped.containsKey("TOOLKIT_SCREEN_ASSUMPTION")&&!grouped.containsKey("NATIVE_ROBOT_INPUT"))grouped.put("TOOLKIT_SCREEN_METRIC_READ",grouped.remove("TOOLKIT_SCREEN_ASSUMPTION"));
            if(robotConstructed&&!grouped.containsKey("NATIVE_ROBOT_INPUT")&&!grouped.containsKey("NATIVE_ROBOT_SCREEN_CAPTURE"))add(grouped,"NATIVE_ROBOT_CONSTRUCTION",callable);
            grouped.forEach((code,nodes)->findings.add(make(code,path,symbol,nodes.get(0),nodes.size(),testRef)));
        }
        String resolveOwner(MethodCallExpr call){long t=System.nanoTime();try{ResolvedMethodDeclaration r=call.resolve();return r.declaringType().getQualifiedName();}catch(RuntimeException e){resolutionIssues++;return "";}finally{resolutionNanos.addAndGet(System.nanoTime()-t);}}
        String resolveType(ObjectCreationExpr n){long t=System.nanoTime();try{return n.calculateResolvedType().describe();}catch(RuntimeException e){resolutionIssues++;return "";}finally{resolutionNanos.addAndGet(System.nanoTime()-t);}}
        static void add(Map<String,List<Node>>m,String k,Node n){m.computeIfAbsent(k,x->new ArrayList<>()).add(n);}
    }
    private static Finding make(String code,String path,String symbol,Node node,int count,String testRef){
        var range=node.getRange().orElse(new com.github.javaparser.Range(new Position(1,1),new Position(1,1)));
        Matrix m=matrix(code);String construct=canonical(node);String ref="compatibility-static-finding-v1:sha256:"+CompatibilityDigests.digest("compatibility-static-finding-v1","1",path,symbol,code,construct);
        return new Finding(ref,path,new SourceRange(range.begin.line,range.begin.column,range.end.line,range.end.column),symbol,m.category,code,m.severity,CausalState.HYPOTHESIS,m.confidence,new Evidence(m.statement,code,List.of(m.reason)),m.recommendation,CodeChangeRequired.UNKNOWN,testRef,m.description+(count>1?" ("+count+" related uses in this method)":""),m.limitations);
    }
    private record Matrix(FindingCategory category,Severity severity,Confidence confidence,StatementKind statement,RecommendationCode recommendation,String reason,String description,List<String>limitations){}
    private static Matrix matrix(String c){return switch(c){
        case "NATIVE_ROBOT_INPUT"->m(FindingCategory.INTERACTION,Severity.REVIEW,Confidence.MEDIUM,StatementKind.FACT,RecommendationCode.REVIEW_INTERACTION_ASSUMPTION,"NATIVE_DESKTOP_INPUT","This method invokes java.awt.Robot desktop input.");
        case "NATIVE_ROBOT_SCREEN_CAPTURE"->m(FindingCategory.TEST_ASSUMPTION,Severity.REVIEW,Confidence.MEDIUM,StatementKind.FACT,RecommendationCode.REMOVE_NATIVE_GUI_DEPENDENCY,"NATIVE_SCREEN_CAPTURE","This method invokes Robot screen capture, not WebDriver screenshot capture.");
        case "NATIVE_ROBOT_CONSTRUCTION"->m(FindingCategory.TEST_ASSUMPTION,Severity.INFO,Confidence.LOW,StatementKind.FACT,RecommendationCode.COLLECT_MORE_EVIDENCE,"ROBOT_CONSTRUCTED","This method constructs java.awt.Robot; a relevant native action was not proven.");
        case "DESKTOP_NATIVE_ACTION"->m(FindingCategory.INFRASTRUCTURE,Severity.REVIEW,Confidence.MEDIUM,StatementKind.FACT,RecommendationCode.REVIEW_INFRASTRUCTURE,"DESKTOP_INTEGRATION","This method invokes a java.awt.Desktop native action.");
        case "NATIVE_FILE_DIALOG"->m(FindingCategory.DOWNLOAD_UPLOAD,Severity.REVIEW,Confidence.MEDIUM,StatementKind.FACT,RecommendationCode.REMOVE_NATIVE_GUI_DEPENDENCY,"NATIVE_DIALOG","This method constructs java.awt.FileDialog.");
        case "TOOLKIT_SCREEN_ASSUMPTION"->m(FindingCategory.VIEWPORT_RESPONSIVE,Severity.REVIEW,Confidence.LOW,StatementKind.HYPOTHESIS,RecommendationCode.ALIGN_VIEWPORT_AND_RERUN,"NATIVE_SCREEN_METRIC","This method reads native screen metrics; verify the assumption with a controlled viewport.");
        case "EXPLICIT_HEADLESS_BRANCH"->m(FindingCategory.CONFIGURATION,Severity.INFO,Confidence.MEDIUM,StatementKind.FACT,RecommendationCode.COLLECT_MORE_EVIDENCE,"EXECUTION_MODE_BRANCH","Source behavior explicitly depends on a headed/headless signal.");
        case "EXPLICIT_HEADLESS_CHECK"->m(FindingCategory.CONFIGURATION,Severity.INFO,Confidence.LOW,StatementKind.FACT,RecommendationCode.COLLECT_MORE_EVIDENCE,"EXECUTION_MODE_CHECK","Source reads a headed/headless signal; a behavior branch was not proven.");
        case "TOOLKIT_SCREEN_METRIC_READ"->m(FindingCategory.CONFIGURATION,Severity.INFO,Confidence.LOW,StatementKind.FACT,RecommendationCode.COLLECT_MORE_EVIDENCE,"NATIVE_SCREEN_METRIC_READ","Source reads native screen metrics; coordinate-dependent behavior was not proven.");
        case "JS_SCREEN_ASSUMPTION"->m(FindingCategory.VIEWPORT_RESPONSIVE,Severity.REVIEW,Confidence.MEDIUM,StatementKind.HYPOTHESIS,RecommendationCode.ALIGN_VIEWPORT_AND_RERUN,"JS_SCREEN_METRIC","Executed JavaScript reads window.screen metrics; verify with a controlled run.");
        case "JS_FOCUS_VISIBILITY_ASSUMPTION"->m(FindingCategory.APPLICATION_BEHAVIOR,Severity.REVIEW,Confidence.MEDIUM,StatementKind.HYPOTHESIS,RecommendationCode.REVIEW_INTERACTION_ASSUMPTION,"JS_FOCUS_VISIBILITY","Executed JavaScript observes or changes focus/visibility state.");
        case "EXPLICIT_WINDOW_SIZE_CONFIGURATION"->m(FindingCategory.CONFIGURATION,Severity.INFO,Confidence.MEDIUM,StatementKind.FACT,RecommendationCode.KEEP_CURRENT_TEST,"DETERMINISTIC_VIEWPORT","Source explicitly configures WebDriver window size.");
        case "WINDOW_MAXIMIZE_OR_FULLSCREEN"->m(FindingCategory.VIEWPORT_RESPONSIVE,Severity.REVIEW,Confidence.LOW,StatementKind.HYPOTHESIS,RecommendationCode.ALIGN_VIEWPORT_AND_RERUN,"PROVIDER_DEPENDENT_WINDOW_MODE","Source uses maximize/fullscreen; resulting geometry may depend on provider/environment.");
        case "WINDOW_HANDLE_ORDER_ASSUMPTION"->m(FindingCategory.WINDOWS_TABS,Severity.REVIEW,Confidence.MEDIUM,StatementKind.HYPOTHESIS,RecommendationCode.COLLECT_MORE_EVIDENCE,"HANDLE_SET_ORDER","Source indexes a list created directly from the unordered window-handle set.");
        case "PROFILE_DEPENDENCY"->m(FindingCategory.AUTH_SESSION,Severity.REVIEW,Confidence.LOW,StatementKind.HYPOTHESIS,RecommendationCode.REVIEW_INFRASTRUCTURE,"PROFILE_CONFIGURATION","Source configures a persistent/browser profile dependency.");
        case "EXPLICIT_HEADLESS_CONFIGURATION"->m(FindingCategory.CONFIGURATION,Severity.INFO,Confidence.MEDIUM,StatementKind.FACT,RecommendationCode.COLLECT_MORE_EVIDENCE,"HEADLESS_CONFIGURED","Source explicitly configures headless browser execution.");
        default->throw new IllegalArgumentException(c);};}
    private static Matrix m(FindingCategory c,Severity s,Confidence f,StatementKind k,RecommendationCode r,String reason,String d){return new Matrix(c,s,f,k,r,reason,d,List.of());}
    private static String declaring(CallableDeclaration<?> c){String type=c.findAncestor(TypeDeclaration.class).map(t->t.getNameAsString()).orElse("<anonymous>");return type+"#"+c.getNameAsString()+"/"+c.getParameters().size();}
    private static String canonical(Node n){if(n instanceof MethodCallExpr m)return "call:"+m.getNameAsString();if(n instanceof ObjectCreationExpr o)return "new:"+o.getType().getNameAsString();return n.getClass().getSimpleName();}
    private static Optional<String>firstLiteral(MethodCallExpr c){if(c.getArguments().isEmpty())return Optional.empty();Expression e=c.getArgument(0);return e.isStringLiteralExpr()?Optional.of(e.asStringLiteralExpr().asString()):Optional.empty();}
    private static boolean isJavascriptExecutor(String owner,String name){return (name.equals("executeScript")||name.equals("executeAsyncScript"))&&owner.equals("org.openqa.selenium.JavascriptExecutor");}
    private static boolean candidateMethod(String n){return ROBOT_INPUT.contains(n)||DESKTOP.contains(n)||Set.of("createScreenCapture","getScreenSize","getScreenResolution","isHeadless","getProperty","getBoolean","executeScript","executeAsyncScript","setSize","maximize","fullscreen","addArguments").contains(n);}
    private static boolean windowOwner(String owner){return owner.equals("org.openqa.selenium.WebDriver.Window")||owner.endsWith("WebDriver$Window");}
    private static boolean screenJs(String s){return s.contains("window.screen")||s.matches("(?s).*\\bscreen\\s*\\.\\s*(width|height|availwidth|availheight).*" );}
    private static boolean focusJs(String s){return s.contains("document.hasfocus")||s.contains("window.focus")||s.contains("document.visibilitystate")||s.contains("document.hidden");}
    private static Optional<String>constant(Expression e,CompilationUnit cu,int depth,Set<String>visiting){if(depth>MAX_CONST_DEPTH)return Optional.empty();if(e.isStringLiteralExpr())return bounded(e.asStringLiteralExpr().asString());if(e.isTextBlockLiteralExpr())return bounded(e.asTextBlockLiteralExpr().getValue());if(e.isBinaryExpr()&&e.asBinaryExpr().getOperator()==BinaryExpr.Operator.PLUS){var l=constant(e.asBinaryExpr().getLeft(),cu,depth+1,visiting);var r=constant(e.asBinaryExpr().getRight(),cu,depth+1,visiting);return l.isPresent()&&r.isPresent()?bounded(l.get()+r.get()):Optional.empty();}if(e.isNameExpr()&&visiting.size()<MAX_CONST_VISITS&&visiting.add(e.asNameExpr().getNameAsString())){for(VariableDeclarator v:cu.findAll(VariableDeclarator.class))if(v.getNameAsString().equals(e.asNameExpr().getNameAsString())&&v.getParentNode().filter(FieldDeclaration.class::isInstance).map(FieldDeclaration.class::cast).filter(f->f.isStatic()&&f.isFinal()).isPresent()&&v.getInitializer().isPresent())return constant(v.getInitializer().get(),cu,depth+1,visiting);}return Optional.empty();}
    private static Optional<String>bounded(String s){return s.length()<=MAX_JS_CHARS?Optional.of(s):Optional.empty();}
    private static boolean inConditional(MethodCallExpr call){return call.findAncestor(IfStmt.class).map(i->i.getCondition().findAll(MethodCallExpr.class).contains(call)).orElse(false);}
    private static List<DetectorDecision>decisions(){return List.of(
            new DetectorDecision("NATIVE_ROBOT_INPUT",DetectorStatus.IMPLEMENTED,"resolved Robot input calls, aggregated by method"),new DetectorDecision("NATIVE_ROBOT_SCREEN_CAPTURE",DetectorStatus.IMPLEMENTED,"resolved Robot capture"),new DetectorDecision("DESKTOP_NATIVE_ACTION",DetectorStatus.IMPLEMENTED,"allowlisted Desktop actions"),new DetectorDecision("NATIVE_FILE_DIALOG",DetectorStatus.IMPLEMENTED,"resolved FileDialog construction"),new DetectorDecision("TOOLKIT_SCREEN_ASSUMPTION",DetectorStatus.IMPLEMENTED,"screen read plus native coordinate input in the same method"),new DetectorDecision("TOOLKIT_SCREEN_METRIC_READ",DetectorStatus.IMPLEMENTED,"screen read without proven coordinate-dependent behavior is INFO only"),new DetectorDecision("EXPLICIT_HEADLESS_BRANCH",DetectorStatus.IMPLEMENTED,"resolved API/property access in an if condition"),new DetectorDecision("EXPLICIT_HEADLESS_CHECK",DetectorStatus.IMPLEMENTED,"resolved signal read without proven branch"),new DetectorDecision("JS_SCREEN_ASSUMPTION",DetectorStatus.IMPLEMENTED,"recognized execution API plus bounded constant JS"),new DetectorDecision("JS_FOCUS_VISIBILITY_ASSUMPTION",DetectorStatus.IMPLEMENTED,"recognized execution API plus bounded constant JS"),new DetectorDecision("WINDOW_HANDLE_ORDER_ASSUMPTION",DetectorStatus.IMPLEMENTED,"direct ArrayList(handle set) indexing only"),new DetectorDecision("PROFILE_DEPENDENCY",DetectorStatus.IMPLEMENTED,"bounded profile configuration facts"),new DetectorDecision("NATIVE_UPLOAD_DIALOG_DEPENDENCY",DetectorStatus.DEFERRED,"Robot and dialog evidence is reported separately; flow proof is not reliable in V1"),new DetectorDecision("AUTH_NATIVE_UI_DEPENDENCY",DetectorStatus.DEFERRED,"requires precise cross-API flow evidence"),new DetectorDecision("DEFAULT_VIEWPORT_ASSUMPTION",DetectorStatus.REJECTED_AS_TOO_NOISY,"absence of setSize is not evidence"),new DetectorDecision("GENERIC_SLEEP_HOVER_CLIPBOARD",DetectorStatus.REJECTED_AS_TOO_NOISY,"valid APIs are not headless risks by themselves"));}
}
