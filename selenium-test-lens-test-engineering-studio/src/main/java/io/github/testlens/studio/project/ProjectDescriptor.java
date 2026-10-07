package io.github.testlens.studio.project;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** Immutable, validated description of a local project opened by Test Engineering Studio. @since 0.5.0 */
public record ProjectDescriptor(
        int schemaVersion,
        String projectId,
        String applicationName,
        Path projectRoot,
        BuildSystem buildSystem,
        Status status,
        ConfigurationSource configurationSource,
        List<Path> mainSourceRoots,
        List<Path> testSourceRoots,
        List<Path> classpathEntries,
        Path workspaceDirectory,
        URI startUrl,
        BrowserFlags browser,
        List<String> evidence,
        List<String> limitations) {

    public static final int SCHEMA_VERSION = 1;

    public ProjectDescriptor {
        if (schemaVersion != SCHEMA_VERSION) throw new IllegalArgumentException("Unsupported project schema " + schemaVersion);
        if (projectId == null || projectId.isBlank()) throw new IllegalArgumentException("projectId is required");
        if (applicationName == null || applicationName.isBlank()) throw new IllegalArgumentException("applicationName is required");
        projectRoot = Objects.requireNonNull(projectRoot, "projectRoot");
        buildSystem = Objects.requireNonNull(buildSystem, "buildSystem");
        status = Objects.requireNonNull(status, "status");
        configurationSource = Objects.requireNonNull(configurationSource, "configurationSource");
        mainSourceRoots = List.copyOf(mainSourceRoots == null ? List.of() : mainSourceRoots);
        testSourceRoots = List.copyOf(testSourceRoots == null ? List.of() : testSourceRoots);
        classpathEntries = List.copyOf(classpathEntries == null ? List.of() : classpathEntries);
        workspaceDirectory = Objects.requireNonNull(workspaceDirectory, "workspaceDirectory");
        browser = browser == null ? BrowserFlags.defaults() : browser;
        evidence = List.copyOf(evidence == null ? List.of() : evidence);
        limitations = List.copyOf(limitations == null ? List.of() : limitations);
        Path normalizedRoot=projectRoot.toAbsolutePath().normalize();
        validateContained(normalizedRoot,workspaceDirectory,"workspaceDirectory");
        mainSourceRoots.forEach(path->validateContained(normalizedRoot,path,"mainSourceRoot"));
        testSourceRoots.forEach(path->validateContained(normalizedRoot,path,"testSourceRoot"));
    }

    public enum BuildSystem { MAVEN, GRADLE, UNKNOWN }
    public List<Path> sourceRoots(){List<Path> value=new java.util.ArrayList<>(mainSourceRoots);value.addAll(testSourceRoots);return List.copyOf(value);}
    public enum Status { READY, NEEDS_CONFIGURATION, INVALID_CONFIGURATION, UNSUPPORTED_PROJECT }
    public enum ConfigurationSource { CLI_OVERRIDE, PROJECT_CONFIG, AUTO_DETECTED }

    /** Browser preferences only; discovery never starts a browser. */
    public record BrowserFlags(Browser browser, boolean headless) {
        public BrowserFlags { browser = Objects.requireNonNull(browser, "browser"); }
        public static BrowserFlags defaults() { return new BrowserFlags(Browser.CHROME, true); }
    }

    public enum Browser { CHROME, FIREFOX }

    private static void validateContained(Path root,Path value,String label){
        Path normalized=Objects.requireNonNull(value,label).toAbsolutePath().normalize();
        if(!normalized.startsWith(root))throw new IllegalArgumentException(label+" escapes project root");
        for(Path cursor=normalized;cursor!=null&&cursor.startsWith(root);cursor=cursor.getParent()){
            if(java.nio.file.Files.exists(cursor,java.nio.file.LinkOption.NOFOLLOW_LINKS)&&java.nio.file.Files.isSymbolicLink(cursor))throw new IllegalArgumentException(label+" contains a symbolic link");
            if(cursor.equals(root))break;
        }
    }
}
