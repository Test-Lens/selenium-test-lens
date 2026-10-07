package io.github.testlens.studio;

import io.github.testlens.application.tooling.ai.workflow.*;
import io.github.testlens.application.tooling.ai.workflow.runner.provider.AgentExecutorProvider;
import io.github.testlens.application.tooling.ai.workflow.runner.provider.CodexCliAgentExecutorProvider;
import io.github.testlens.studio.browser.*;
import io.github.testlens.studio.launcher.StudioLauncherService;
import io.github.testlens.studio.project.ProjectDescriptor;
import io.github.testlens.studio.project.ProjectDiscovery;
import io.github.testlens.studio.project.StudioConfigurationLoader;
import io.github.testlens.studio.projection.StudioProjections.Capability;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.concurrent.CountDownLatch;

/**
 * Consumer bootstrap for the local Test Engineering Studio distribution.
 * It composes the existing deterministic services and starts no browser or agent until a user action requests one.
 * @since 0.5.0
 */
public final class TestEngineeringStudio {
    private TestEngineeringStudio() { }

    public static LaunchHandle launch(LaunchRequest request) throws IOException {
        Objects.requireNonNull(request,"request");
        StudioConfigurationLoader.EffectiveConfiguration machine=new StudioConfigurationLoader().load(request.projectRoot());
        LoadedProvider<BrowserSessionProvider> loadedBrowsers=loadConfiguredProvider(
                machine.browserProvider(),"LOCAL",BrowserSessionProvider.class,request.testClasspath());
        BrowserSessionProvider browsers=loadedBrowsers==null?new DefaultLocalBrowserSessionProvider():loadedBrowsers.provider();
        Path staging=request.projectRoot().toAbsolutePath().normalize().resolve(".test-lens/ai/runner");
        LoadedProvider<AgentExecutorProvider> loadedAgents=loadConfiguredProvider(
                machine.agentProvider(),"CODEX",AgentExecutorProvider.class,request.testClasspath());
        AgentExecutorProvider agents=loadedAgents==null
                ?(machine.codexExecutable()==null?new CodexCliAgentExecutorProvider(staging)
                :new CodexCliAgentExecutorProvider(machine.codexExecutable(),staging))
                :loadedAgents.provider();
        try { return launch(request,browsers,agents).withResources(loadedBrowsers,loadedAgents); }
        catch (RuntimeException|IOException failure) { closeQuietly(loadedAgents);closeQuietly(loadedBrowsers);throw failure; }
    }

    private static <T>LoadedProvider<T> loadConfiguredProvider(String configured,String builtIn,Class<T> type,List<Path> classpath)throws IOException{
        String selection=configured==null?builtIn:configured.trim().toUpperCase(java.util.Locale.ROOT);
        if(selection.equals(builtIn))return null;
        if(!selection.equals("SERVICE")&&!selection.equals("CUSTOM"))throw new IllegalArgumentException("Unsupported "+type.getSimpleName()+" provider: "+configured);
        if(classpath==null||classpath.isEmpty())throw new IllegalArgumentException("No project classpath is available for "+type.getSimpleName()+" provider discovery");
        List<URL> urls=new ArrayList<>();for(Path path:classpath)urls.add(path.toUri().toURL());
        URLClassLoader loader=new URLClassLoader(urls.toArray(URL[]::new),TestEngineeringStudio.class.getClassLoader());
        List<T> providers=ServiceLoader.load(type,loader).stream().map(ServiceLoader.Provider::get).limit(2).toList();
        if(providers.size()!=1){loader.close();throw new IllegalArgumentException("Expected exactly one "+type.getSimpleName()+" service provider, found "+providers.size());}
        return new LoadedProvider<>(providers.get(0),loader);
    }

    private record LoadedProvider<T>(T provider,URLClassLoader loader) implements AutoCloseable { @Override public void close()throws IOException{loader.close();} }
    private static void closeQuietly(AutoCloseable value){if(value!=null)try{value.close();}catch(Exception ignored){}}

    /** Programmatic extension point used by custom browser/agent providers and deterministic certification. */
    public static LaunchHandle launch(LaunchRequest request, BrowserSessionProvider browsers,
                                      AgentExecutorProvider agentProvider) throws IOException {
        Objects.requireNonNull(request,"request");Objects.requireNonNull(browsers,"browsers");Objects.requireNonNull(agentProvider,"agentProvider");
        Path root=request.projectRoot().toAbsolutePath().normalize();
        List<Path> roots=new ArrayList<>(request.mainSourceRoots());roots.addAll(request.testSourceRoots());
        ProjectDescriptor descriptor;
        if(roots.isEmpty()&&request.testClasspath().isEmpty())descriptor=new ProjectDiscovery().discover(root);
        else {
            ProjectDiscovery.Overrides overrides=new ProjectDiscovery.Overrides(null,null,
                    roots.isEmpty()?null:roots,request.testClasspath().isEmpty()?null:request.testClasspath(),null,null,null,null);
            descriptor=new ProjectDiscovery().discover(new ProjectDiscovery.Request(root,overrides));
        }
        StudioConfigurationLoader.EffectiveConfiguration config=new StudioConfigurationLoader().load(root);
        Browser browser="FIREFOX".equalsIgnoreCase(config.browser())?Browser.FIREFOX:Browser.CHROME;
        BrowserRequest executionRequest=new BrowserRequest(Purpose.TEST_EXECUTION,browser,Ownership.STUDIO_OWNED,config.headless(),config.browserProfile());
        AgentExecutor routed=roleRouting(agentProvider,config.agentProfiles());
        List<Path> effectiveClasspath=request.testClasspath().isEmpty()?descriptor.classpathEntries():request.testClasspath();
        String classpath=effectiveClasspath.stream().map(Path::toString).collect(java.util.stream.Collectors.joining(File.pathSeparator));
        ReviewableCoordinatorWorkflowGateway gateway=new ReviewableCoordinatorWorkflowGateway(routed,
                (executor,input)->coordinator(executor,input,browsers,executionRequest,descriptor,effectiveClasspath,classpath),
                new GeneratedTestPolicyValidator(GeneratedTestPolicyValidator.Policy.defaults()),
                TestEngineeringStudio::generatedSourcePath,
                new TargetedRepairSourceCompiler(root,17,classpath,descriptor.sourceRoots(),effectiveClasspath));
        StudioLauncherService launcher=StudioLauncherService.withBrowserProvider(gateway,browsers);
        StudioLauncherService.LaunchHandle delegate=launcher.launch(descriptor,new StudioLauncherService.LaunchOptions(request.openBrowser()));
        delegate.service().attachBrowserProfile(config.browserProfile());
        BrowserAvailability mappingAvailability=browsers.preflight(new BrowserRequest(Purpose.MAPPING,browser,Ownership.STUDIO_OWNED,config.headless(),config.browserProfile()));
        BrowserAvailability executionAvailability=browsers.preflight(executionRequest);
        BrowserAvailability browserAvailability=mappingAvailability==BrowserAvailability.AVAILABLE?executionAvailability:mappingAvailability;
        AgentExecutorProvider.ProviderAvailability agentAvailability=agentProvider.preflight();
        if(agentAvailability.available())for(AgentExecutor.Role role:AgentExecutor.Role.values()){
            String profile=config.agentProfiles().getOrDefault(role.name(),defaultProfile(role));
            AgentExecutorProvider.ProviderAvailability roleAvailability=agentProvider.preflight(role,profile);
            if(!roleAvailability.available()){agentAvailability=roleAvailability;break;}
        }
        delegate.service().attachCapabilities(
                new Capability(browserAvailability.name(),browserAvailability==BrowserAvailability.AVAILABLE?"Local browser preflight passed":"Local browser preflight did not pass"),
                new Capability(agentAvailability.status().name(),agentAvailability.reason()),
                new Capability(javax.tools.ToolProvider.getSystemJavaCompiler()==null?"NOT_AVAILABLE":"AVAILABLE",javax.tools.ToolProvider.getSystemJavaCompiler()==null?"A JDK compiler is required":"JDK compiler available"));
        return new LaunchHandle(delegate);
    }

    private static AgentWorkflowCoordinator coordinator(AgentExecutor executor, CoordinatorWorkflowGateway.Input input,
            BrowserSessionProvider browsers, BrowserRequest browserRequest, ProjectDescriptor descriptor,
            List<Path> classpathEntries, String classpath) {
        TestEngineeringRequest request=input.request();Path source=generatedSourcePath(request);
        AgentWorkflowCoordinator.Configuration configuration=new AgentWorkflowCoordinator.Configuration(source,
                request.target().testClass(),"",17,classpath,50,Duration.ofMinutes(3),List.of(),2);
        BrowserRequest mappingRequest=new BrowserRequest(Purpose.MAPPING,browserRequest.browser(),
                Ownership.STUDIO_OWNED,browserRequest.headless(),browserRequest.profileId());
        ExternalRepairPipeline repairs=new ExternalRepairPipeline(descriptor.projectRoot(),descriptor.sourceRoots(),
                classpathEntries,browsers,mappingRequest,descriptor.startUrl());
        return new AgentWorkflowCoordinator(new TestEngineeringWorkflow(WorkflowPolicy.defaults()),executor,
                new TargetedJavaCompiler(),new JUnitTargetedTestExecutor(browsers,browserRequest,classpathEntries,descriptor.startUrl()),
                new GeneratedTestPolicyValidator(GeneratedTestPolicyValidator.Policy.defaults()),repairs.classifier(),
                repairs.stabilizer(),configuration);
    }

    private static AgentExecutor roleRouting(AgentExecutorProvider provider,Map<String,String> profiles){
        Map<AgentExecutor.Role,String> roleProfiles=new EnumMap<>(AgentExecutor.Role.class);
        for(AgentExecutor.Role role:AgentExecutor.Role.values())roleProfiles.put(role,
                profiles.getOrDefault(role.name(),defaultProfile(role)));
        return command->provider.executorFor(command.role(),roleProfiles.get(command.role())).execute(command);
    }

    private static String defaultProfile(AgentExecutor.Role role){return switch(role){
        case TEST_ARCHITECT,STABILIZER->"high";
        default->"standard";
    };}

    private static Path generatedSourcePath(TestEngineeringRequest request){String root=request.allowedPaths().isEmpty()?"src/test/java":request.allowedPaths().get(0);return Path.of(root).resolve(request.target().testClass().replace('.','/')+".java");}

    /** Maven/project-derived launch request. Paths are canonicalized by project discovery. @since 0.5.0 */
    public record LaunchRequest(Path projectRoot,List<Path> mainSourceRoots,List<Path> testSourceRoots,
                                List<Path> testClasspath,boolean openBrowser){
        public LaunchRequest{Objects.requireNonNull(projectRoot,"projectRoot");mainSourceRoots=List.copyOf(mainSourceRoots==null?List.of():mainSourceRoots);testSourceRoots=List.copyOf(testSourceRoots==null?List.of():testSourceRoots);testClasspath=List.copyOf(testClasspath==null?List.of():testClasspath);}
    }

    /** Running Studio host. Closing it never repeats workflows or repairs. @since 0.5.0 */
    public static final class LaunchHandle implements AutoCloseable {
        private final StudioLauncherService.LaunchHandle delegate;private final CountDownLatch closed=new CountDownLatch(1);private final List<AutoCloseable> resources;
        private LaunchHandle(StudioLauncherService.LaunchHandle delegate){this(delegate,List.of());}
        private LaunchHandle(StudioLauncherService.LaunchHandle delegate,List<AutoCloseable> resources){this.delegate=delegate;this.resources=List.copyOf(resources);}
        private LaunchHandle withResources(AutoCloseable... values){List<AutoCloseable> kept=new ArrayList<>();for(AutoCloseable value:values)if(value!=null)kept.add(value);return new LaunchHandle(delegate,kept);}
        public URI uri(){return delegate.uri();}
        public String projectId(){return delegate.descriptor().projectId();}
        public void await()throws InterruptedException{closed.await();}
        @Override public void close(){try{delegate.close();}finally{resources.forEach(TestEngineeringStudio::closeQuietly);closed.countDown();}}
    }
}
