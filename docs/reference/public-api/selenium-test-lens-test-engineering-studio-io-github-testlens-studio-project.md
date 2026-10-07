---
search:
  exclude: true
---

# selenium-test-lens-test-engineering-studio: `io.github.testlens.studio.project`

Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.

## `io.github.testlens.studio.project.ProjectDescriptor$Browser` {#io-github-testlens-studio-project-projectdescriptor-browser}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.project`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.studio.project.ProjectDescriptor$Browser CHROME
public static final io.github.testlens.studio.project.ProjectDescriptor$Browser FIREFOX
public static io.github.testlens.studio.project.ProjectDescriptor$Browser[] values()
public static io.github.testlens.studio.project.ProjectDescriptor$Browser valueOf(java.lang.String)
```

## `io.github.testlens.studio.project.ProjectDescriptor$BrowserFlags` {#io-github-testlens-studio-project-projectdescriptor-browserflags}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.project`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.studio.project.ProjectDescriptor$BrowserFlags(io.github.testlens.studio.project.ProjectDescriptor$Browser, boolean)
public static io.github.testlens.studio.project.ProjectDescriptor$BrowserFlags defaults()
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.studio.project.ProjectDescriptor$Browser browser()
public boolean headless()
```

## `io.github.testlens.studio.project.ProjectDescriptor$BuildSystem` {#io-github-testlens-studio-project-projectdescriptor-buildsystem}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.project`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.studio.project.ProjectDescriptor$BuildSystem MAVEN
public static final io.github.testlens.studio.project.ProjectDescriptor$BuildSystem GRADLE
public static final io.github.testlens.studio.project.ProjectDescriptor$BuildSystem UNKNOWN
public static io.github.testlens.studio.project.ProjectDescriptor$BuildSystem[] values()
public static io.github.testlens.studio.project.ProjectDescriptor$BuildSystem valueOf(java.lang.String)
```

## `io.github.testlens.studio.project.ProjectDescriptor$ConfigurationSource` {#io-github-testlens-studio-project-projectdescriptor-configurationsource}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.project`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.studio.project.ProjectDescriptor$ConfigurationSource CLI_OVERRIDE
public static final io.github.testlens.studio.project.ProjectDescriptor$ConfigurationSource PROJECT_CONFIG
public static final io.github.testlens.studio.project.ProjectDescriptor$ConfigurationSource AUTO_DETECTED
public static io.github.testlens.studio.project.ProjectDescriptor$ConfigurationSource[] values()
public static io.github.testlens.studio.project.ProjectDescriptor$ConfigurationSource valueOf(java.lang.String)
```

## `io.github.testlens.studio.project.ProjectDescriptor$Status` {#io-github-testlens-studio-project-projectdescriptor-status}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.project`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.studio.project.ProjectDescriptor$Status READY
public static final io.github.testlens.studio.project.ProjectDescriptor$Status NEEDS_CONFIGURATION
public static final io.github.testlens.studio.project.ProjectDescriptor$Status INVALID_CONFIGURATION
public static final io.github.testlens.studio.project.ProjectDescriptor$Status UNSUPPORTED_PROJECT
public static io.github.testlens.studio.project.ProjectDescriptor$Status[] values()
public static io.github.testlens.studio.project.ProjectDescriptor$Status valueOf(java.lang.String)
```

## `io.github.testlens.studio.project.ProjectDescriptor` {#io-github-testlens-studio-project-projectdescriptor}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.project`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public static final int SCHEMA_VERSION
public io.github.testlens.studio.project.ProjectDescriptor(int, java.lang.String, java.lang.String, java.nio.file.Path, io.github.testlens.studio.project.ProjectDescriptor$BuildSystem, io.github.testlens.studio.project.ProjectDescriptor$Status, io.github.testlens.studio.project.ProjectDescriptor$ConfigurationSource, java.util.List<java.nio.file.Path>, java.util.List<java.nio.file.Path>, java.util.List<java.nio.file.Path>, java.nio.file.Path, java.net.URI, io.github.testlens.studio.project.ProjectDescriptor$BrowserFlags, java.util.List<java.lang.String>, java.util.List<java.lang.String>)
public java.util.List<java.nio.file.Path> sourceRoots()
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public int schemaVersion()
public java.lang.String projectId()
public java.lang.String applicationName()
public java.nio.file.Path projectRoot()
public io.github.testlens.studio.project.ProjectDescriptor$BuildSystem buildSystem()
public io.github.testlens.studio.project.ProjectDescriptor$Status status()
public io.github.testlens.studio.project.ProjectDescriptor$ConfigurationSource configurationSource()
public java.util.List<java.nio.file.Path> mainSourceRoots()
public java.util.List<java.nio.file.Path> testSourceRoots()
public java.util.List<java.nio.file.Path> classpathEntries()
public java.nio.file.Path workspaceDirectory()
public java.net.URI startUrl()
public io.github.testlens.studio.project.ProjectDescriptor$BrowserFlags browser()
public java.util.List<java.lang.String> evidence()
public java.util.List<java.lang.String> limitations()
```

## `io.github.testlens.studio.project.ProjectDiscovery$Overrides` {#io-github-testlens-studio-project-projectdiscovery-overrides}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.project`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.studio.project.ProjectDiscovery$Overrides(java.lang.String, java.lang.String, java.util.List<java.nio.file.Path>, java.util.List<java.nio.file.Path>, java.nio.file.Path, java.lang.String, java.lang.String, java.lang.Boolean)
public static io.github.testlens.studio.project.ProjectDiscovery$Overrides none()
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String projectId()
public java.lang.String applicationName()
public java.util.List<java.nio.file.Path> sourceRoots()
public java.util.List<java.nio.file.Path> classpathEntries()
public java.nio.file.Path workspaceDirectory()
public java.lang.String startUrl()
public java.lang.String browser()
public java.lang.Boolean headless()
```

## `io.github.testlens.studio.project.ProjectDiscovery$Request` {#io-github-testlens-studio-project-projectdiscovery-request}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.project`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.studio.project.ProjectDiscovery$Request(java.nio.file.Path, io.github.testlens.studio.project.ProjectDiscovery$Overrides)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.nio.file.Path projectRoot()
public io.github.testlens.studio.project.ProjectDiscovery$Overrides overrides()
```

## `io.github.testlens.studio.project.ProjectDiscovery` {#io-github-testlens-studio-project-projectdiscovery}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.project`
- Classification: `INTERNAL`
- Type kind: `class`

```java
public io.github.testlens.studio.project.ProjectDiscovery()
public io.github.testlens.studio.project.ProjectDescriptor discover(java.nio.file.Path)
public io.github.testlens.studio.project.ProjectDescriptor discover(io.github.testlens.studio.project.ProjectDiscovery$Request)
```

## `io.github.testlens.studio.project.StudioConfigurationLoader$CliOverrides` {#io-github-testlens-studio-project-studioconfigurationloader-clioverrides}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.project`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.studio.project.StudioConfigurationLoader$CliOverrides(java.lang.String, java.lang.Boolean, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
public static io.github.testlens.studio.project.StudioConfigurationLoader$CliOverrides none()
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String browser()
public java.lang.Boolean headless()
public java.lang.String browserProvider()
public java.lang.String browserProfile()
public java.lang.String agentProvider()
public java.lang.String codexExecutable()
```

## `io.github.testlens.studio.project.StudioConfigurationLoader$EffectiveConfiguration` {#io-github-testlens-studio-project-studioconfigurationloader-effectiveconfiguration}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.project`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.studio.project.StudioConfigurationLoader$EffectiveConfiguration(int, java.lang.String, boolean, java.lang.String, java.lang.String, java.lang.String, java.util.Map<java.lang.String, java.lang.String>, java.nio.file.Path, java.util.List<java.lang.String>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public int schemaVersion()
public java.lang.String browser()
public boolean headless()
public java.lang.String browserProvider()
public java.lang.String browserProfile()
public java.lang.String agentProvider()
public java.util.Map<java.lang.String, java.lang.String> agentProfiles()
public java.nio.file.Path codexExecutable()
public java.util.List<java.lang.String> evidence()
```

## `io.github.testlens.studio.project.StudioConfigurationLoader` {#io-github-testlens-studio-project-studioconfigurationloader}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.project`
- Classification: `INTERNAL`
- Type kind: `class`

```java
public static final int SCHEMA_VERSION
public io.github.testlens.studio.project.StudioConfigurationLoader()
public io.github.testlens.studio.project.StudioConfigurationLoader$EffectiveConfiguration load(java.nio.file.Path)
public io.github.testlens.studio.project.StudioConfigurationLoader$EffectiveConfiguration load(java.nio.file.Path, io.github.testlens.studio.project.StudioConfigurationLoader$CliOverrides)
```
