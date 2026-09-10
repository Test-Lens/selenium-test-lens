# Locators

**Selenium Test Lens 0.1.0 — stable**

The main facade accepts ordinary Selenium locators and exposes the original convenience factories:

```java
UiLocator byId = lens.locator(By.id("save"), "Save button");
UiLocator byTestId = lens.getByTestId("save");
UiLocator byText = lens.getByText("Saved");
UiLocator byRole = lens.getByRole("button", "Save");
```

`JsOverlayDebug` additionally exposes `getByPlaceholder(...)`, `getByTextContaining(...)`, and `getByLabel(...)`, including labeled overloads.

## Historical matching rules

These 0.1.0 helpers generate CSS or XPath selectors. In particular, `getByRole(role, name)` matches explicit or selected implicit roles, then compares the requested name with `aria-label` or normalized element text. It does **not** use the browser's complete accessible-name computation. `getByLabel(...)` recognizes selected form controls through `aria-label`, `label[for]`, an ancestor `label`, or `aria-labelledby` patterns implemented by its XPath.

The 0.1.0 API has no locator composition, descendant-query stages, collection filters, or shadow-root traversal. `nth()`, `first()`, and `last()` are indexed views of one global `By` query:

```java
UiLocator rows = lens.locator(By.cssSelector("table tbody tr"), "Order rows");
int count = rows.count();
rows.first().click();
rows.nth(2).waitUntilVisible();
rows.last().expect().toContainText("Complete");
```

Locators resolve lazily for actions and indexed views; they do not cache a `WebElement`. `resolveAll()` returns the current Selenium result list.
