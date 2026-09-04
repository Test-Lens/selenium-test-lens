# Select controls

These methods instantiate Selenium `Select`; the target must be a native HTML `<select>`. Custom/React dropdowns require normal component DOM interactions or the optional React helpers.

<!-- API SIGNATURES: io.github.testlens.selenium.locator.UiLocator -->
```java
UiLocator selectByVisibleText(String text)
UiLocator selectByValue(String value)
UiLocator selectByIndex(int index)
String selectedText()
String selectedValue()
```

The three selection methods check actionability, perform the corresponding Selenium `Select` call, emit operation feedback, and return the same locator. Text/value must be non-null. Selenium determines missing-option and invalid-element failures. `selectedText()` returns the first selected option's text; `selectedValue()` returns its `value` attribute and may be null.

```java
UiLocator country = lens.getByTestId("country");
country.selectByValue("PL");
assertEquals("Poland", country.selectedText());
```

Use `optionLocator.expect().toBeSelected()` to poll a native option's selected state. `toBeSelected()` is distinct from `toBeChecked()`: checkbox and radio state belongs to the checked assertion family, while selected state belongs to native options and supported `aria-selected` roles.
