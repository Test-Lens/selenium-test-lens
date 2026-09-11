# Consumer compatibility and release validation

Selenium Test Lens supports consumption through Maven and Gradle. Java 17 is
the minimum source, bytecode, and runtime level; Java 11 is not supported. CI
verifies consumers on JDK 17 and JDK 21 while keeping production bytecode
targeted to Java 17.

## Clean-room release boundary

The source reactor is currently `0.3.0-SNAPSHOT`. Compatibility validation
copies tracked source to a temporary directory, transforms all nine reactor
POMs to the corresponding release `0.3.0` version, builds release artifacts, and stages
the seven publishable Maven coordinates in an isolated local repository. It
never edits the source POMs.

The Maven and Gradle smoke consumers both resolve Test Lens from that staging
repository. The Gradle build uses `exclusiveContent` for
`io.github.test-lens`, so Maven Central can supply third-party dependencies
but cannot supply Test Lens. It rejects snapshots, source-tree repositories,
project dependencies, composite substitution, dynamic versions, unresolved
components, inconsistent Selenium versions, and artifacts whose files are
outside staging.

The Gradle test loads the main runtime, React integration, JUnit 5 extension,
TestNG listener and their transitive core/overlay artifacts. It exercises
public logging, redaction and trace APIs without starting a `WebDriver`.
Browser behavior remains covered by the independent Chrome/Firefox gate.
The existing Maven release smoke still exercises its historical headless
Chrome contract; the Gradle consumer itself is deliberately browser-free.

## Run locally

Run clean-room preparation and the Gradle consumer in PowerShell 7:

```powershell
./scripts/validate-gradle-consumer.ps1
```

The script derives release `0.3.0` from the root `0.3.0-SNAPSHOT` POM, creates fresh
Maven and Gradle repositories below the operating system's temporary
directory, runs the checked-in Wrapper, validates the graph and class files,
and removes its work directory in `finally`. Use
`-KeepWorkDirectoryOnFailure` only when diagnostics are required.

To run the checked-in consumer against an already prepared isolated
repository:

```powershell
./consumer-tests/gradle/gradlew test verifyResolvedGraph `
  "-PtestLensVersion=0.3.0" `
  "-PtestLensRepository=/absolute/path/to/isolated-repository" `
  --no-daemon --stacktrace
```

The Wrapper is pinned to Gradle 8.10.2 with a distribution checksum and is
validated in CI. Its fixture, cache, reports and wrapper files are outside all
published Test Lens JARs.

## Published-user coordinates

The latest version available from Maven Central is `0.2.0`:

```kotlin
dependencies {
    testImplementation("io.github.test-lens:selenium-test-lens:0.2.0")
    testImplementation("io.github.test-lens:selenium-test-lens-react:0.2.0")
}
```

```groovy
dependencies {
    testImplementation 'io.github.test-lens:selenium-test-lens:0.2.0'
    testImplementation 'io.github.test-lens:selenium-test-lens-react:0.2.0'
}
```

Dedicated runner adapters are also tested by the isolated clean-room gate and
published as optional `0.2.0` coordinates.
