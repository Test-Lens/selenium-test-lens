<p align="center">
  <img src="docs/assets/brand/test-lens-logo-horizontal.png" alt="Selenium Test Lens" width="720">
</p>

# Selenium Test Lens

Selenium Test Lens adds observable, retryable interactions, an in-browser diagnostic HUD, trace reports, and evidence capture to the Selenium `WebDriver` your test framework already owns.

- [Documentation](https://test-lens.github.io/selenium-test-lens/)
- [Maven Central](https://central.sonatype.com/artifact/io.github.test-lens/selenium-test-lens/0.1.0)
- [Javadoc](https://javadoc.io/doc/io.github.test-lens/selenium-test-lens/0.1.0/)
- [Changelog](CHANGELOG.md)
- [Issues](https://github.com/Test-Lens/selenium-test-lens/issues)

## Requirements

- Java 17 or newer
- Maven 3.x
- A Selenium `WebDriver` created and managed by the consuming test project

## Install

```xml
<dependency>
    <groupId>io.github.test-lens</groupId>
    <artifactId>selenium-test-lens</artifactId>
    <version>0.1.0</version>
</dependency>
```

Selenium is consumer-owned and must be declared separately at the version managed by your project:

```xml
<dependency>
    <groupId>org.seleniumhq.selenium</groupId>
    <artifactId>selenium-java</artifactId>
    <version>${selenium.version}</version>
</dependency>
```

## First session

```java
import io.github.testlens.TestLens;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

WebDriver driver = createExistingFrameworkDriver();
TestLens lens = TestLens.attach(driver);
try {
    lens.startSession("login");
    driver.get("https://example.test/login");

    lens.locator(By.id("username"), "Username").fill("john");
    lens.locator(By.id("login"), "Login").click();
    lens.locator(By.id("welcome"), "Welcome").expect().toBeVisible();
    lens.finishPassed();
} catch (RuntimeException | Error failure) {
    lens.finishFailed(failure);
    throw failure;
} finally {
    driver.quit();
}
```

Test Lens does not own browser lifecycle or displace JUnit, TestNG, Allure, or another reporter. Existing raw Selenium remains valid for operations the Lens facade does not wrap. React-specific support is available as a separate, optional module.

See the [getting-started guide](https://test-lens.github.io/selenium-test-lens/getting-started/) and [framework integration guide](https://test-lens.github.io/selenium-test-lens/framework-integration/) for lifecycle patterns and the full usage documentation.

## Build

```powershell
mvn clean verify
```

The default build runs the fast unit suite. Real-browser integration tests are isolated in an unpublished consumer module and enabled explicitly:

```powershell
mvn -Pbrowser-it -Dbrowser=chrome -Dheaded=false verify
mvn -Pbrowser-it -Dbrowser=firefox -Dheaded=false verify
```

Use `-Dheaded=true` for local visual debugging. The current CI browser gate covers local Chrome and Firefox drivers; Edge and `RemoteWebDriver` grids are not part of this matrix.

See [Real-browser integration tests](docs/browser-integration-tests.md) for prerequisites, scenarios, and CI behavior.

## License

Licensed under the [Apache License 2.0](LICENSE).
