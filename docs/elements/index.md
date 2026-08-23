# Elements

`UiLocator` is a lazy description of one element or an indexed member of a matching collection. It stores a Selenium `By`, a human-readable label, retry options, and observability collaborators; it resolves against the current DOM when an operation runs.

```java
UiLocator save = lens.getByTestId("save");
save.waitUntilClickable().click();
save.expect().toBeEnabled();
```

- [Locators](locators.md): construct `UiLocator` values.
- [Actions](actions.md): click, type, clear, keys, pointer actions.
- [Waiting](waiting.md): explicit waits and retry rules.
- [Assertions](assertions.md): retryable expectations and results.
- [Element information](information.md): resolve and read state.
- [Collections](collections.md): count and select matching elements.
- [Select controls](select-controls.md): native HTML `<select>` operations.

Raw `WebElement`/Selenium remains available through `resolve()`, `resolveAll()`, and `TestLens.driver()` for operations Lens does not wrap.
