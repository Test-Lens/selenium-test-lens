package io.github.testlens.studio.transport;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.testlens.application.mapper.ApplicationMapperOptions;
import io.github.testlens.application.tooling.json.StrictJson;
import io.github.testlens.application.tooling.ai.workflow.AgentExecutor;
import io.github.testlens.core.redaction.RedactionPolicy;
import io.github.testlens.studio.TestEngineeringStudioService;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/** Loopback-only, token-authenticated, allowlisted local transport for Studio. */
public final class TestEngineeringStudioServer implements AutoCloseable {
    public static final String TOKEN_HEADER="X-Test-Lens-Session";
    public static final int MAX_REQUEST_BYTES=64*1024;
    private static final String WEB_ROOT="/io/github/testlens/studio/web/";
    private static final Set<String> ACTIONS=Set.of("SCAN_PROJECT","MAP_APPLICATION","CORRELATE","REFRESH_PROJECT",
            "CREATE_REQUIREMENT","GENERATE_PLAN","REGENERATE_PLAN","GENERATE_IMPLEMENTATION","RUN","DIAGNOSE","PREPARE_REPAIR","RERUN","APPROVE_REPAIR","REJECT_REPAIR");
    private final TestEngineeringStudioService service;
    private final HttpServer server;
    private final String token;
    private final String expectedHost;
    private final String expectedOrigin;
    private final ExecutorService executor;
    private final AtomicBoolean started=new AtomicBoolean();

    public TestEngineeringStudioServer(TestEngineeringStudioService service)throws IOException{
        this.service=Objects.requireNonNull(service);this.token=randomToken();
        this.server=HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"),0),16);
        String address="127.0.0.1";
        this.expectedHost=address+":"+server.getAddress().getPort();this.expectedOrigin="http://"+expectedHost;
        server.createContext("/api/",this::api);server.createContext("/",this::staticResource);
        this.executor=Executors.newFixedThreadPool(4,r->{Thread thread=new Thread(r,"test-lens-studio-http");thread.setDaemon(true);return thread;});
        server.setExecutor(executor);
    }
    public void start(){if(!started.compareAndSet(false,true))throw new IllegalStateException("Studio server already started");server.start();}
    public URI uri(){return URI.create(expectedOrigin+"/");}
    public String sessionToken(){return token;}
    @Override public void close(){if(started.compareAndSet(true,false))server.stop(0);executor.shutdownNow();}

    private void api(HttpExchange exchange)throws IOException{
        try{
            securityHeaders(exchange);
            if(!expectedHost.equals(exchange.getRequestHeaders().getFirst("Host"))){sendError(exchange,403,"HOST_REJECTED");return;}
            boolean readSession="GET".equals(exchange.getRequestMethod())&&constantTime(token,cookie(exchange,"TestLensStudioSession"));
            if(!readSession&&!constantTime(token,exchange.getRequestHeaders().getFirst(TOKEN_HEADER))){sendError(exchange,401,"SESSION_TOKEN_REQUIRED");return;}
            if("OPTIONS".equals(exchange.getRequestMethod())){sendError(exchange,405,"METHOD_NOT_ALLOWED");return;}
            if("GET".equals(exchange.getRequestMethod())){handleGet(exchange);return;}
            if("POST".equals(exchange.getRequestMethod())&&"/api/actions".equals(exchange.getRequestURI().getPath())){
                if(!expectedOrigin.equals(exchange.getRequestHeaders().getFirst("Origin"))){sendError(exchange,403,"ORIGIN_REJECTED");return;}
                String contentType=Objects.toString(exchange.getRequestHeaders().getFirst("Content-Type"),"");
                if(!contentType.toLowerCase(Locale.ROOT).startsWith("application/json")){sendError(exchange,415,"JSON_REQUIRED");return;}
                handleAction(exchange);return;
            }
            sendError(exchange,405,"METHOD_NOT_ALLOWED");
        }catch(PayloadTooLargeException failure){sendError(exchange,413,"PAYLOAD_TOO_LARGE");}
        catch(AgentExecutor.AgentExecutionException failure){sendError(exchange,502,failure.code().name());}
        catch(IllegalArgumentException failure){sendInvalidRequest(exchange,failure);}
        catch(IllegalStateException failure){String code=safeCode(failure.getMessage());if("BROWSER_SESSION_FAILED".equals(code))sendBrowserSessionFailure(exchange,failure);else sendError(exchange,409,code);}
        catch(Exception failure){sendError(exchange,500,"OPERATION_FAILED");}
        finally{exchange.close();}
    }
    private void handleGet(HttpExchange exchange)throws IOException{
        if(exchange.getRequestBody().read()!=-1){sendError(exchange,400,"GET_BODY_REJECTED");return;}
        String path=exchange.getRequestURI().getPath();Map<String,String> query=query(exchange.getRequestURI().getRawQuery());
        Object response=switch(path){
            case "/api/project"->service.projectOverview();case "/api/config"->service.projectConfiguration();case "/api/application"->service.applicationOverview();
            case "/api/correlations"->service.correlations(integer(query,"offset",0),integer(query,"limit",100));
            case "/api/problems"->service.problems(integer(query,"offset",0),integer(query,"limit",100));
            case "/api/workflow"->service.workflow(query.get("runId"));case "/api/workflows"->service.workflowHistory();case "/api/repairs"->service.repairHistory();
            case "/api/snapshot"->service.snapshot();case "/api/status"->Map.of("operationRunning",service.operationRunning());
            default->null;};
        if(response==null){sendError(exchange,404,"NOT_FOUND");return;}sendJson(exchange,200,response);
    }
    private void handleAction(HttpExchange exchange)throws Exception{
        long declared=parseLength(exchange.getRequestHeaders().getFirst("Content-Length"));
        if(declared>MAX_REQUEST_BYTES){sendError(exchange,413,"PAYLOAD_TOO_LARGE");return;}
        byte[] body=readBounded(exchange.getRequestBody());Map<String,Object> request=StrictJson.readObject(body);
        String action=action(request);if(!ACTIONS.contains(action)){sendError(exchange,400,"ACTION_NOT_ALLOWED");return;}
        Object result=switch(action){
            case "SCAN_PROJECT"->service.scanProject();
            case "MAP_APPLICATION"->service.mapApplication(mode(request));
            case "CORRELATE"->service.correlate();case "REFRESH_PROJECT"->service.refreshProject();
            case "CREATE_REQUIREMENT"->Map.of("runId",service.createRequirement(string(request,"requirement",16_384)));
            case "GENERATE_PLAN"->service.generatePlan(string(request,"runId",256));
            case "REGENERATE_PLAN"->service.generatePlan(string(request,"runId",256));
            case "GENERATE_IMPLEMENTATION"->service.generateImplementation(string(request,"runId",256));
            case "RUN"->service.runWorkflow(string(request,"runId",256));
            case "DIAGNOSE"->service.diagnoseWorkflow(string(request,"runId",256));
            case "PREPARE_REPAIR"->service.prepareRepair(string(request,"runId",256));
            case "RERUN"->service.rerun(string(request,"runId",256));
            case "APPROVE_REPAIR"->service.approveRepair(string(request,"runId",256),string(request,"proposalId",256));
            case "REJECT_REPAIR"->service.rejectRepair(string(request,"runId",256),string(request,"proposalId",256));
            default->throw new IllegalArgumentException("Action not allowed");};
        Map<String,Object> response=new LinkedHashMap<>();response.put("action",action);response.put("status","PASS");response.put("result",result);response.put("project",service.projectOverview());response.put("workflow",service.workflow(Objects.toString(request.get("runId"),null)));sendJson(exchange,200,response);
    }
    private void staticResource(HttpExchange exchange)throws IOException{
        try{
            securityHeaders(exchange);if(!expectedHost.equals(exchange.getRequestHeaders().getFirst("Host"))){sendError(exchange,403,"HOST_REJECTED");return;}if(!"GET".equals(exchange.getRequestMethod())){sendError(exchange,405,"METHOD_NOT_ALLOWED");return;}
            String raw=exchange.getRequestURI().getRawPath();String path="/".equals(raw)?"index.html":URLDecoder.decode(raw.substring(1),StandardCharsets.UTF_8);
            if(path.contains("..")||path.contains("\\")||!path.matches("[A-Za-z0-9_./-]+")){sendError(exchange,404,"NOT_FOUND");return;}
            try(InputStream input=getClass().getResourceAsStream(WEB_ROOT+path)){if(input==null){sendError(exchange,404,"NOT_FOUND");return;}byte[] bytes=input.readNBytes(2*1024*1024+1);if(bytes.length>2*1024*1024){sendError(exchange,413,"RESOURCE_TOO_LARGE");return;}if("index.html".equals(path)){String html=new String(bytes,StandardCharsets.UTF_8).replace("<meta name=\"test-lens-session\" content=\"\">","<meta name=\"test-lens-session\" content=\""+token+"\">").replace("data-session-token=\"\"","data-session-token=\""+token+"\"");bytes=html.getBytes(StandardCharsets.UTF_8);exchange.getResponseHeaders().set("Set-Cookie","TestLensStudioSession="+token+"; HttpOnly; SameSite=Strict; Path=/");}String type=path.endsWith(".css")?"text/css; charset=utf-8":path.endsWith(".js")?"text/javascript; charset=utf-8":"text/html; charset=utf-8";exchange.getResponseHeaders().set("Content-Type",type);exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);}
        }finally{exchange.close();}
    }
    private static void securityHeaders(HttpExchange e){e.getResponseHeaders().set("Cache-Control","no-store");e.getResponseHeaders().set("X-Content-Type-Options","nosniff");e.getResponseHeaders().set("Content-Security-Policy","default-src 'self'; script-src 'self'; style-src 'self'; connect-src 'self'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'");e.getResponseHeaders().set("Referrer-Policy","no-referrer");}
    private static byte[] readBounded(InputStream input)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int read,total=0;while((read=input.read(buffer))!=-1){total+=read;if(total>MAX_REQUEST_BYTES)throw new PayloadTooLargeException();out.write(buffer,0,read);}return out.toByteArray();}
    private static ApplicationMapperOptions.Mode mode(Map<String,Object> values){String value=Objects.toString(values.getOrDefault("mode","CURRENT_PAGE"));return ApplicationMapperOptions.Mode.valueOf(value);}
    private static String action(Map<String,Object> values){Object value=values.containsKey("action")?values.get("action"):values.get("actionId");if(!(value instanceof String text)||text.isBlank()||text.length()>128)throw new IllegalArgumentException("action");return text;}
    private static String cookie(HttpExchange exchange,String name){String header=exchange.getRequestHeaders().getFirst("Cookie");if(header==null)return null;for(String value:header.split(";")){String[] pair=value.trim().split("=",2);if(pair.length==2&&pair[0].equals(name))return pair[1];}return null;}
    private static String string(Map<String,Object> values,String name,int max){Object value=values.get(name);if(!(value instanceof String text)||text.isBlank()||text.length()>max)throw new IllegalArgumentException(name);return text;}
    private static int integer(Map<String,String> values,String name,int fallback){String value=values.get(name);if(value==null)return fallback;int result=Integer.parseInt(value);if(result<0||result>500)throw new IllegalArgumentException(name);return result;}
    private static Map<String,String> query(String query){if(query==null||query.isBlank())return Map.of();Map<String,String> out=new HashMap<>();for(String item:query.split("&",20)){String[] pair=item.split("=",2);String key=URLDecoder.decode(pair[0],StandardCharsets.UTF_8),value=pair.length==1?"":URLDecoder.decode(pair[1],StandardCharsets.UTF_8);if(out.putIfAbsent(key,value)!=null)throw new IllegalArgumentException("duplicate query");}return out;}
    private static long parseLength(String value){if(value==null)return-1;try{return Long.parseLong(value);}catch(NumberFormatException failure){throw new IllegalArgumentException("Content-Length");}}
    private static String randomToken(){byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);}
    private static boolean constantTime(String expected,String actual){if(actual==null)return false;byte[] left=expected.getBytes(StandardCharsets.US_ASCII),right=actual.getBytes(StandardCharsets.US_ASCII);int difference=left.length^right.length;for(int i=0;i<left.length;i++)difference|=left[i]^(i<right.length?right[i]:0);return difference==0;}
    private static String safeCode(String message){
        if(message!=null&&message.contains("already running"))return "OPERATION_IN_PROGRESS";
        if(message!=null&&message.startsWith("Unable to open local "))return "BROWSER_SESSION_FAILED";
        if(message!=null&&(message.startsWith("Local browser is not available")||message.startsWith("No browser session provider")))return "BROWSER_UNAVAILABLE";
        return "STATE_CONFLICT";
    }
    private void sendBrowserSessionFailure(HttpExchange exchange,IllegalStateException failure)throws IOException{
        Map<String,Object> body=new LinkedHashMap<>();body.put("error","BROWSER_SESSION_FAILED");
        var configuration=service.projectConfiguration();
        String browser=configuration==null?"UNKNOWN":bounded(configuration.browser(),32);
        boolean headless=configuration!=null&&configuration.headless();
        String configuredBinary=bounded(System.getenv("SE_BROWSER_PATH"),1024);
        Path binary=safePath(configuredBinary);
        String explicitDriver=bounded(System.getProperty("webdriver.chrome.driver"),1024);
        Path driver=explicitDriver==null?pathCandidate("chromedriver"):safePath(explicitDriver);
        Throwable underlying=rootCause(failure);
        body.put("browser",browser);body.put("configuredBinary",configuredBinary==null?"":configuredBinary);
        body.put("optionsBinary",configuredBinary==null?"DEFAULT_DISCOVERY":configuredBinary);
        body.put("binaryExists",binary!=null&&Files.isRegularFile(binary));
        body.put("binaryExecutable",binary!=null&&Files.isExecutable(binary));body.put("headless",headless);
        body.put("headlessMode",headless&&"CHROME".equals(browser)?"--headless=new":headless?"HEADLESS":"HEADED");
        body.put("sandboxMode","true".equalsIgnoreCase(Objects.toString(System.getenv("SE_BROWSER_NO_SANDBOX"),""))?"DISABLED_BY_HOST_CONFIGURATION":"DEFAULT");
        body.put("driverResolution",explicitDriver==null?"SELENIUM_MANAGER":"SYSTEM_PROPERTY");
        body.put("driverPath",driver==null?"":bounded(driver.toString(),1024));
        body.put("driverVersion",driver==null?"":commandVersion(driver));
        body.put("exceptionType",underlying.getClass().getSimpleName());
        body.put("message",safeMessage(underlying.getMessage()));
        sendJson(exchange,409,body);
    }
    private static Throwable rootCause(Throwable value){Throwable current=value;for(int depth=0;depth<12&&current.getCause()!=null&&current.getCause()!=current;depth++)current=current.getCause();return current;}
    private static String safeMessage(String value){String redacted=RedactionPolicy.defaults().redact(Objects.toString(value,"Browser session creation failed"));return bounded(redacted.replaceAll("[\\r\\n]+"," "),2048);}
    private static String bounded(String value,int limit){if(value==null||value.isBlank())return null;String normalized=value.trim();return normalized.length()<=limit?normalized:normalized.substring(0,limit)+"...";}
    private static Path safePath(String value){try{return value==null?null:Path.of(value).toAbsolutePath().normalize();}catch(RuntimeException ignored){return null;}}
    private static Path pathCandidate(String executable){String path=System.getenv("PATH");if(path==null)return null;for(String root:path.split(java.util.regex.Pattern.quote(File.pathSeparator))){try{Path candidate=Path.of(root).resolve(executable).toAbsolutePath().normalize();if(Files.isRegularFile(candidate)&&Files.isExecutable(candidate))return candidate;}catch(RuntimeException ignored){}}return null;}
    private static String commandVersion(Path executable){Process process=null;try{process=new ProcessBuilder(executable.toString(),"--version").redirectErrorStream(true).start();if(!process.waitFor(2,java.util.concurrent.TimeUnit.SECONDS)){process.destroyForcibly();return "VERSION_TIMEOUT";}return bounded(new String(process.getInputStream().readNBytes(512),StandardCharsets.UTF_8).replaceAll("[\\r\\n]+"," "),512);}catch(Exception ignored){return "VERSION_UNAVAILABLE";}finally{if(process!=null&&process.isAlive())process.destroyForcibly();}}
    private static void sendJson(HttpExchange exchange,int status,Object body)throws IOException{byte[] bytes=StrictJson.write(body);exchange.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");exchange.sendResponseHeaders(status,bytes.length);exchange.getResponseBody().write(bytes);}
    private static void sendInvalidRequest(HttpExchange exchange,IllegalArgumentException failure)throws IOException{
        Map<String,Object> body=new LinkedHashMap<>();body.put("error","INVALID_REQUEST");body.put("reason",safeMessage(failure.getMessage()));sendJson(exchange,400,body);
    }
    private static void sendError(HttpExchange exchange,int status,String code)throws IOException{sendJson(exchange,status,Map.of("error",code));}
    private static final class PayloadTooLargeException extends IOException{}
}
