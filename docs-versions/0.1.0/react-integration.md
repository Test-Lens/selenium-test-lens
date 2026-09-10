# React integration

**Selenium Test Lens 0.1.0 — stable**

Add the optional artifact:

```xml
<dependency>
    <groupId>io.github.test-lens</groupId>
    <artifactId>selenium-test-lens-react</artifactId>
    <version>0.1.0</version>
</dependency>
```

Create the low-level overlay facade, then obtain React helpers:

```java
JsOverlayDebug overlay = new JsOverlayDebug(driver);
ReactSafeExecutor react = ReactSupport.reactSafe(overlay);

react.click(By.cssSelector("[data-testid='save']"), "Save");
react.clearAndType(By.cssSelector("input[name='email']"),
    "person@example.test", "Email");
```

`ReactSupport.actionability(...)` adds checks for signals such as `aria-busy`, selected loading attributes, progress/spinner/skeleton selectors, dialogs, and custom blockers. `ReactSelectHelper` supports the DOM conventions of selected React-style controls.

These are Selenium/DOM heuristics, not React DevTools integration. They do not inspect component state, hooks, the virtual DOM, or framework internals. `ReactSupport.smartClick(...)` is a historical low-level React helper; it is not the same contract as the current development-line `UiLocator.click()`.
