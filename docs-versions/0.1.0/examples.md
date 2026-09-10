# Examples

**Selenium Test Lens 0.1.0 — stable**

## Form interaction and assertion

```java
TestLens lens = TestLens.attach(driver);
lens.startSession("profile");

lens.getByTestId("display-name").fill("Ada");
lens.getByRole("button", "Save").click();
lens.getByTextContaining("Saved").expect().toBeVisible();
```

The last line uses normalized XPath text matching from 0.1.0, not browser-computed accessible text.

## Indexed result

```java
UiLocator products = lens.locator(By.cssSelector("[data-testid='product']"));
products.nth(1).expect().toContainText("Keyboard");
```

## Context switch

```java
lens.switchToFrame(By.id("payment-frame"), "Payment");
lens.getByTestId("card-number").fill("4111111111111111");
lens.switchToDefaultContent();
```

## Manual report export

```java
UiTestLensSession session = UiTestLensSession.start("diagnostic snapshot");
session.finishSkipped("Environment unavailable");
session.exportJson(Path.of("target", "snapshot.json"));
session.exportHtml(Path.of("target", "snapshot.html"));
```

The repository's 0.1.0 examples also demonstrate overlay policy, retryable assertions, actionability reports, React helpers, browser contexts, alerts, and manual network events. They were source examples, not a published artifact.
