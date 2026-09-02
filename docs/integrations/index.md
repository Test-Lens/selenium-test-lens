# Integrations

The main Lens facade is runner-agnostic and attaches to an already-created driver. See [existing WebDriver integration](../framework-integration.md) for TestNG, Page Objects, Allure coexistence, and manual lifecycle patterns.

JUnit Jupiter users can choose the published [JUnit 5 lifecycle extension](junit5.md), which owns a new driver and Lens session for every invocation and injects both as parameters.

TestNG users can choose the published [TestNG lifecycle listener](testng.md), which attaches state to the physical `ITestResult` and exposes it through `TestLensTestNgContext`.

The [Optional React API](react.md) remains isolated in its own published module.
