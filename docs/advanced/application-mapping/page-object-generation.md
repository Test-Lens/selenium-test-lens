# Page Object generation

The Page Object generator consumes an `ApplicationModel`; it does not inspect the DOM and it does not ask an LLM to invent selectors. Output is deterministic for an equivalent canonical model and generation options.

!!! warning "Development availability"
    `selenium-test-lens-application-tooling` is source-only development work for 0.5.0 and is not published with Maven 0.4.0.

## Generate Lens-native source

```java
Path output = Path.of(".test-lens", "generated", "src", "main", "java");
PageObjectGenerationOptions options = PageObjectGenerationOptions.verifiedOnly(
        "com.example.test.generated",
        output
);

PageObjectGenerator generator = new PageObjectGenerator();
List<GeneratedPageObject> pages = generator.generate(model, options);
PageObjectSourceStore.WriteResult result =
        new PageObjectSourceStore().write(options, pages);
```

Generated bases use the public Test Lens interaction layer:

```java
protected UiLocator loginButton() {
    return lens.locator(By.cssSelector("[data-testid='login-submit']"), "loginButton");
}

public GeneratedLoginPage clickLoginButton() {
    loginButton().click();
    return this;
}
```

The actual class and method names come from canonical page and element semantics. Methods remain mechanical (`fillUsername`, `clickLoginButton`) unless an observed transition justifies a destination Page Object return type. The generator does not invent domain workflows such as `approveMortgageApplication()`.

## Selector quality

`PageObjectGenerationOptions.verifiedOnly(...)` uses `ReviewSelectorPolicy.SKIP`. Elements with unavailable selectors or `REVIEW_REQUIRED` quality are omitted and reported in `GeneratedPageObject.warnings()`. This is the safe default.

`GENERATE_WITH_WARNING` can emit a review-required selector with a source warning, but it does not upgrade the selector's evidence. Unsupported selector strategies are skipped. Selection order and quality come from Selector Intelligence, not from generator scoring.

## Regeneration without overwriting user code

For each page, the generator owns a `Generated*Page` base class and creates a user extension only when it is absent:

```text
GeneratedLoginPage.java   <- regenerated
LoginPage.java            <- created once, then preserved
```

`PageObjectSourceStore` atomically replaces generated bases. Existing extension classes are returned in `preservedExtensions()` and are never overwritten. Keep custom workflows, assertions, and application-specific helpers in the extension class.

Generated fields and methods include model element and selector candidate IDs in source metadata, which preserves the relation between model element, generated member, and later runtime or drift evidence.

Treat `.test-lens/generated` as generated source, not as the location for hand-written changes. The store rejects path traversal and symbolic-link targets beneath the configured output root.

## Compile before use

The generator itself has no Selenium compile dependency because it emits source text offline. The generated source imports `TestLens`, `UiLocator`, and Selenium `By`, so compile it against the same Test Lens and Selenium classpath as the consuming tests. The repository browser contract uses the JDK compiler and then executes a generated Page Object against the fixture application; a string snapshot alone is not the generation contract.

For semantic changes, use `PageObjectGenerator.diff(before, after, options)`. It returns before/after source plus the associated `ApplicationDrift.Change` records, rather than presenting an unexplained textual diff.

