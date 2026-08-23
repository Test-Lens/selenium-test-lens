---
search:
  exclude: true
---

# selenium-test-lens-selenium: `io.github.testlens.selenium.auth`

Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.

## `io.github.testlens.selenium.auth.AuthCookie` {#io-github-testlens-selenium-auth-authcookie}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.auth`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.selenium.auth.AuthCookie(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.time.Instant, boolean, boolean, java.lang.String)
public static io.github.testlens.selenium.auth.AuthCookie fromSeleniumCookie(org.openqa.selenium.Cookie)
public org.openqa.selenium.Cookie toSeleniumCookie()
public java.lang.String name()
public java.lang.String value()
public java.lang.String domain()
public java.lang.String path()
public java.time.Instant expiry()
public boolean secure()
public boolean httpOnly()
public java.lang.String sameSite()
```

## `io.github.testlens.selenium.auth.AuthRestoreOptions$Builder` {#io-github-testlens-selenium-auth-authrestoreoptions-builder}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.auth`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.selenium.auth.AuthRestoreOptions$Builder navigateToOrigin(boolean)
public io.github.testlens.selenium.auth.AuthRestoreOptions$Builder clearExistingCookies(boolean)
public io.github.testlens.selenium.auth.AuthRestoreOptions$Builder clearExistingStorage(boolean)
public io.github.testlens.selenium.auth.AuthRestoreOptions$Builder restoreCookies(boolean)
public io.github.testlens.selenium.auth.AuthRestoreOptions$Builder restoreLocalStorage(boolean)
public io.github.testlens.selenium.auth.AuthRestoreOptions$Builder restoreSessionStorage(boolean)
public io.github.testlens.selenium.auth.AuthRestoreOptions$Builder validateOrigin(boolean)
public io.github.testlens.selenium.auth.AuthRestoreOptions$Builder failIfExpired(boolean)
public io.github.testlens.selenium.auth.AuthRestoreOptions build()
```

## `io.github.testlens.selenium.auth.AuthRestoreOptions` {#io-github-testlens-selenium-auth-authrestoreoptions}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.auth`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public static io.github.testlens.selenium.auth.AuthRestoreOptions defaults()
public static io.github.testlens.selenium.auth.AuthRestoreOptions$Builder builder()
public boolean navigateToOrigin()
public boolean clearExistingCookies()
public boolean clearExistingStorage()
public boolean restoreCookies()
public boolean restoreLocalStorage()
public boolean restoreSessionStorage()
public boolean validateOrigin()
public boolean failIfExpired()
```

## `io.github.testlens.selenium.auth.AuthRestoreResult` {#io-github-testlens-selenium-auth-authrestoreresult}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.auth`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public static io.github.testlens.selenium.auth.AuthRestoreResult restored(java.lang.String, int, int, int, java.time.Duration)
public static io.github.testlens.selenium.auth.AuthRestoreResult failed(java.lang.String, java.lang.Throwable, java.time.Duration)
public static io.github.testlens.selenium.auth.AuthRestoreResult expired(java.lang.String, java.time.Duration)
public static io.github.testlens.selenium.auth.AuthRestoreResult originMismatch(java.lang.String, java.time.Duration)
public static io.github.testlens.selenium.auth.AuthRestoreResult skipped(java.lang.String, java.time.Duration)
public io.github.testlens.selenium.auth.AuthRestoreStatus status()
public java.lang.String message()
public int cookiesRestored()
public int localStorageEntriesRestored()
public int sessionStorageEntriesRestored()
public java.lang.Throwable exception()
public java.time.Duration elapsed()
```

## `io.github.testlens.selenium.auth.AuthRestoreStatus` {#io-github-testlens-selenium-auth-authrestorestatus}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.auth`
- Classification: `ADVANCED_API`
- Type kind: `enum`

```java
public static final io.github.testlens.selenium.auth.AuthRestoreStatus RESTORED
public static final io.github.testlens.selenium.auth.AuthRestoreStatus FAILED
public static final io.github.testlens.selenium.auth.AuthRestoreStatus SKIPPED
public static final io.github.testlens.selenium.auth.AuthRestoreStatus EXPIRED
public static final io.github.testlens.selenium.auth.AuthRestoreStatus ORIGIN_MISMATCH
public static io.github.testlens.selenium.auth.AuthRestoreStatus[] values()
public static io.github.testlens.selenium.auth.AuthRestoreStatus valueOf(java.lang.String)
```

## `io.github.testlens.selenium.auth.AuthState` {#io-github-testlens-selenium-auth-authstate}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.auth`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.selenium.auth.AuthState(io.github.testlens.selenium.auth.AuthStateMetadata, java.util.List<io.github.testlens.selenium.auth.AuthCookie>, java.util.List<io.github.testlens.selenium.auth.AuthStorageEntry>, java.util.List<io.github.testlens.selenium.auth.AuthStorageEntry>)
public io.github.testlens.selenium.auth.AuthStateMetadata metadata()
public java.util.List<io.github.testlens.selenium.auth.AuthCookie> cookies()
public java.util.List<io.github.testlens.selenium.auth.AuthStorageEntry> localStorage()
public java.util.List<io.github.testlens.selenium.auth.AuthStorageEntry> sessionStorage()
public java.lang.String exportJson()
public java.nio.file.Path save(java.nio.file.Path)
public static io.github.testlens.selenium.auth.AuthState load(java.nio.file.Path)
```

## `io.github.testlens.selenium.auth.AuthStateException` {#io-github-testlens-selenium-auth-authstateexception}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.auth`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.selenium.auth.AuthStateException(java.lang.String)
public io.github.testlens.selenium.auth.AuthStateException(java.lang.String, java.lang.Throwable)
```

## `io.github.testlens.selenium.auth.AuthStateJsonExporter` {#io-github-testlens-selenium-auth-authstatejsonexporter}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.auth`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.selenium.auth.AuthStateJsonExporter()
public java.lang.String export(io.github.testlens.selenium.auth.AuthState)
```

## `io.github.testlens.selenium.auth.AuthStateJsonParser` {#io-github-testlens-selenium-auth-authstatejsonparser}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.auth`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.selenium.auth.AuthStateJsonParser()
public io.github.testlens.selenium.auth.AuthState parse(java.lang.String)
```

## `io.github.testlens.selenium.auth.AuthStateManager` {#io-github-testlens-selenium-auth-authstatemanager}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.auth`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.selenium.auth.AuthStateManager(org.openqa.selenium.WebDriver)
public io.github.testlens.selenium.auth.AuthStateManager(org.openqa.selenium.WebDriver, io.github.testlens.core.OverlayLogger)
public io.github.testlens.selenium.auth.AuthState captureState(io.github.testlens.selenium.auth.AuthStateOptions)
public io.github.testlens.selenium.auth.AuthRestoreResult restoreState(io.github.testlens.selenium.auth.AuthState, io.github.testlens.selenium.auth.AuthRestoreOptions)
public io.github.testlens.selenium.auth.AuthState load(java.nio.file.Path)
public io.github.testlens.selenium.auth.AuthRestoreResult restoreState(java.nio.file.Path, io.github.testlens.selenium.auth.AuthRestoreOptions)
```

## `io.github.testlens.selenium.auth.AuthStateMetadata$Builder` {#io-github-testlens-selenium-auth-authstatemetadata-builder}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.auth`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.selenium.auth.AuthStateMetadata$Builder id(java.lang.String)
public io.github.testlens.selenium.auth.AuthStateMetadata$Builder label(java.lang.String)
public io.github.testlens.selenium.auth.AuthStateMetadata$Builder role(java.lang.String)
public io.github.testlens.selenium.auth.AuthStateMetadata$Builder origin(java.lang.String)
public io.github.testlens.selenium.auth.AuthStateMetadata$Builder domain(java.lang.String)
public io.github.testlens.selenium.auth.AuthStateMetadata$Builder createdAt(java.time.Instant)
public io.github.testlens.selenium.auth.AuthStateMetadata$Builder expiresAt(java.time.Instant)
public io.github.testlens.selenium.auth.AuthStateMetadata$Builder createdBy(java.lang.String)
public io.github.testlens.selenium.auth.AuthStateMetadata$Builder labelEntry(java.lang.String, java.lang.String)
public io.github.testlens.selenium.auth.AuthStateMetadata$Builder note(java.lang.String, java.lang.String)
public io.github.testlens.selenium.auth.AuthStateMetadata$Builder labels(java.util.Map<java.lang.String, java.lang.String>)
public io.github.testlens.selenium.auth.AuthStateMetadata$Builder notes(java.util.Map<java.lang.String, java.lang.String>)
public io.github.testlens.selenium.auth.AuthStateMetadata build()
```

## `io.github.testlens.selenium.auth.AuthStateMetadata` {#io-github-testlens-selenium-auth-authstatemetadata}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.auth`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public static io.github.testlens.selenium.auth.AuthStateMetadata$Builder builder()
public io.github.testlens.selenium.auth.AuthStateMetadata$Builder toBuilder()
public java.lang.String id()
public java.lang.String label()
public java.lang.String role()
public java.lang.String origin()
public java.lang.String domain()
public java.time.Instant createdAt()
public java.time.Instant expiresAt()
public java.lang.String createdBy()
public java.util.Map<java.lang.String, java.lang.String> labels()
public java.util.Map<java.lang.String, java.lang.String> notes()
```

## `io.github.testlens.selenium.auth.AuthStateOptions$Builder` {#io-github-testlens-selenium-auth-authstateoptions-builder}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.auth`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.selenium.auth.AuthStateOptions$Builder label(java.lang.String)
public io.github.testlens.selenium.auth.AuthStateOptions$Builder role(java.lang.String)
public io.github.testlens.selenium.auth.AuthStateOptions$Builder origin(java.lang.String)
public io.github.testlens.selenium.auth.AuthStateOptions$Builder includeCookies(boolean)
public io.github.testlens.selenium.auth.AuthStateOptions$Builder includeLocalStorage(boolean)
public io.github.testlens.selenium.auth.AuthStateOptions$Builder includeSessionStorage(boolean)
public io.github.testlens.selenium.auth.AuthStateOptions$Builder expiresAt(java.time.Instant)
public io.github.testlens.selenium.auth.AuthStateOptions$Builder labelEntry(java.lang.String, java.lang.String)
public io.github.testlens.selenium.auth.AuthStateOptions$Builder note(java.lang.String, java.lang.String)
public io.github.testlens.selenium.auth.AuthStateOptions build()
```

## `io.github.testlens.selenium.auth.AuthStateOptions` {#io-github-testlens-selenium-auth-authstateoptions}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.auth`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public static io.github.testlens.selenium.auth.AuthStateOptions defaults()
public static io.github.testlens.selenium.auth.AuthStateOptions$Builder builder()
public java.lang.String label()
public java.lang.String role()
public java.lang.String origin()
public boolean includeCookies()
public boolean includeLocalStorage()
public boolean includeSessionStorage()
public java.time.Instant expiresAt()
public java.util.Map<java.lang.String, java.lang.String> labels()
public java.util.Map<java.lang.String, java.lang.String> notes()
```

## `io.github.testlens.selenium.auth.AuthStorageEntry` {#io-github-testlens-selenium-auth-authstorageentry}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.auth`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.selenium.auth.AuthStorageEntry(java.lang.String, java.lang.String, java.lang.String, io.github.testlens.selenium.auth.AuthStorageType)
public java.lang.String origin()
public java.lang.String key()
public java.lang.String value()
public io.github.testlens.selenium.auth.AuthStorageType type()
```

## `io.github.testlens.selenium.auth.AuthStorageType` {#io-github-testlens-selenium-auth-authstoragetype}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.auth`
- Classification: `ADVANCED_API`
- Type kind: `enum`

```java
public static final io.github.testlens.selenium.auth.AuthStorageType LOCAL_STORAGE
public static final io.github.testlens.selenium.auth.AuthStorageType SESSION_STORAGE
public static io.github.testlens.selenium.auth.AuthStorageType[] values()
public static io.github.testlens.selenium.auth.AuthStorageType valueOf(java.lang.String)
```
