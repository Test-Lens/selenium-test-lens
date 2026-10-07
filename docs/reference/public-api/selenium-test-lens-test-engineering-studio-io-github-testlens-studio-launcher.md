---
search:
  exclude: true
---

# selenium-test-lens-test-engineering-studio: `io.github.testlens.studio.launcher`

Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.

## `io.github.testlens.studio.launcher.Main` {#io-github-testlens-studio-launcher-main}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.launcher`
- Classification: `INTERNAL`
- Type kind: `class`

```java
public static void main(java.lang.String[]) throws java.lang.Exception
```

## `io.github.testlens.studio.launcher.StudioLauncherService$LaunchHandle` {#io-github-testlens-studio-launcher-studiolauncherservice-launchhandle}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.launcher`
- Classification: `INTERNAL`
- Type kind: `class`

```java
public io.github.testlens.studio.project.ProjectDescriptor descriptor()
public java.net.URI uri()
public io.github.testlens.studio.TestEngineeringStudioService service()
public boolean browserOpened()
public void close()
```

## `io.github.testlens.studio.launcher.StudioLauncherService$LaunchOptions` {#io-github-testlens-studio-launcher-studiolauncherservice-launchoptions}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.launcher`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.studio.launcher.StudioLauncherService$LaunchOptions(boolean)
public static io.github.testlens.studio.launcher.StudioLauncherService$LaunchOptions defaults()
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public boolean openBrowser()
```

## `io.github.testlens.studio.launcher.StudioLauncherService` {#io-github-testlens-studio-launcher-studiolauncherservice}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.launcher`
- Classification: `INTERNAL`
- Type kind: `class`

```java
public io.github.testlens.studio.launcher.StudioLauncherService(io.github.testlens.studio.StudioWorkflowGateway, java.util.function.Supplier<org.openqa.selenium.WebDriver>)
public static io.github.testlens.studio.launcher.StudioLauncherService withBrowserProvider(io.github.testlens.studio.StudioWorkflowGateway, io.github.testlens.studio.browser.BrowserSessionProvider)
public io.github.testlens.studio.launcher.StudioLauncherService$LaunchHandle launch(io.github.testlens.studio.project.ProjectDescriptor) throws java.io.IOException
public io.github.testlens.studio.launcher.StudioLauncherService$LaunchHandle launch(io.github.testlens.studio.project.ProjectDescriptor, io.github.testlens.studio.launcher.StudioLauncherService$LaunchOptions) throws java.io.IOException
```
