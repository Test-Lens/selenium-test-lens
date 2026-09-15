# Allure

Allure remains the report UI and result ecosystem; Test Lens supplies finalized browser observability and evidence. The optional adapter consumes `TestLensFinalizationResult`. It does not use `ReportUploader`, generate an Allure report, access WebDriver, or capture a second raw screenshot.

## Install

Add the optional artifact alongside your own Allure runner adapter. Allure is not pulled into the Test Lens core, Selenium, JUnit 5, TestNG, or umbrella artifacts.

```xml
<dependency>
    <groupId>io.github.test-lens</groupId>
    <artifactId>selenium-test-lens-allure</artifactId>
    <version>0.3.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-jupiter</artifactId>
    <version>2.35.5</version>
    <scope>test</scope>
</dependency>
```

The runtime module depends only on `allure-java-commons`. Version 2.35.5 is tested. JUnit 5 uses the current `allure-jupiter` artifact name; TestNG projects use `allure-testng`.

## Explicit lifecycle

Attach after Test Lens finalization while Allure still has the current test or step. Explicit placement is runner-neutral and avoids relying on unspecified ordering of independent extensions or listeners.

```java
@Test
void checkout() {
    TestLens lens = createStartedLens();
    try {
        runCheckout(lens);
        AllureAttachResult publication = AllureTestLens.attach(lens.finishPassed());
        recordIntegrationDiagnostic(publication);
    } catch (RuntimeException | Error failure) {
        TestLensFinalizationResult finalized = lens.finishFailed(failure);
        AllureAttachResult publication = AllureTestLens.attach(finalized);
        recordIntegrationDiagnostic(publication);
        throw failure;
    }
}
```

Use the same placement in TestNG or an application-owned callback whose ordering is explicit. V1 intentionally does not auto-register another JUnit 5 extension or TestNG listener: neither runner promises a portable ordering between independent Test Lens and Allure callbacks. Calling after Allure stops the executable returns `SKIPPED_NO_ACTIVE_CONTEXT` and never attaches to a guessed test.

## Attachment policy

Failed sessions default to fixed-name diagnostic and clean PNGs (`image/png`), the report (`text/html`), and completed bundle (`application/zip`). Trace JSON (`application/json`) is opt-in. Passed and skipped sessions are also opt-in:

```java
AllureTestLensOptions options = AllureTestLensOptions.builder()
        .attachNonFailedSessions(true)
        .attachTrace(true)
        .build();
AllureAttachResult publication = AllureTestLens.attach(lens.finishPassed(), options);
```

Missing/unreadable artifacts and Allure write failures are returned in `AllureAttachResult`; they never replace the original failure and local evidence remains. A repeat for the same result and context returns `SKIPPED_ALREADY_ATTACHED`. Tracking is synchronized and weakly retains finalized results; Allure current-context semantics keep parallel tests separated.

## Security and limitations

Only central-pipeline artifacts are streamed. Visual redaction remains present in diagnostic and clean screenshots; `RedactionPolicy` protects textual report/trace content. The adapter never reads the DOM or captures an alternate raw image. BLUR remains visual obfuscation; SOLID is security-oriented masking.

`ReportUploader` is a separate HTTP transport. This adapter uses Allure's public runtime lifecycle and does not map trace events to steps, alter runner-owned status, model retries, or invoke the Allure CLI/generator.
