# Existing Page Object correlation

Application mapping does not assume a greenfield test suite. The source-only 0.5.0 tooling can build a deterministic index of existing Java Page Objects, components, helpers, and JUnit 5 or TestNG tests, then correlate their selector declarations with observed `ApplicationModel` elements.

!!! warning "Development availability"
    `ExistingProjectIndexer`, source correlation, and source-aware context slicing are unpublished development APIs. They are built from the current reactor and are not present in Maven Central 0.4.0. They do not change the runtime behavior of an existing Test Lens test.

## One declaration index

`ExistingProjectIndexer` extends the Selector Audit source scan. A Java source file is parsed once; the same declaration identity, logical path, source range, and resolution status used by Selector Audit feed the existing-project projection, while file content hashes contribute to its project fingerprint. The Application Mapper does not contain a second parser for `By` or `UiLocator` declarations.

The resulting `ExistingProjectIndex` contains canonical, stable-ID projections of:

- classes classified conservatively as `PAGE_OBJECT`, `COMPONENT`, `BASE_PAGE`, `TEST_HELPER`, or `UNKNOWN`;
- origin (`HAND_WRITTEN`, generated base or extension, or unknown);
- selector-bearing element declarations and their Selector Audit `declarationRef`;
- Page Object methods, parameters, return types, observed method-call names, referenced declarations, and conservative classifications;
- JUnit 5 and TestNG test methods, tags or groups, and calls;
- bounded source-usage edges;
- coverage metrics, `COMPLETE` or `PARTIAL`, and explicit limitations.

Raw selector values are not copied into this index. A resolved value is represented by the same deterministic SHA-256 projection used by the indexer, together with strategy and resolution state. The index also does not execute project code or store Selenium remote element IDs.

```java
ExistingProjectIndexer indexer = new ExistingProjectIndexer();
ExistingProjectIndex source = indexer.index(new ExistingProjectIndexer.Request(
        projectRoot,
        List.of(projectRoot.resolve("src/main/java"), projectRoot.resolve("src/test/java")),
        compileClasspathEntries
));

if (source.completeness() == ExistingProjectIndex.Completeness.PARTIAL) {
    source.limitations().forEach(System.err::println);
}
```

Use explicit source roots and the real project compile classpath. Bounds cover roots, files, declarations, types, methods, tests, and graph edges. Reaching a bound produces a limitation instead of silently claiming a complete project index. The project fingerprint is content-derived, which leaves room for changed-file indexing without making a daemon part of this release.

## Correlation is evidence-bearing

`PageObjectCorrelator` relates an observed application element to a source element. It does not accept a matching field name as identity. Current automatic evidence is based on selector candidate or normalized selector identity and page/class context; live same-target evidence strengthens a match when it is present in the observed selector projection. Stable-ID `CorrelationOverrides` can declare a reviewed class-to-page or source-element-to-application-element relation.

Every result has one of these states:

| State | Meaning |
|---|---|
| `EXACT` | A reviewed stable-ID override resolves the relation in the current implementation. |
| `STRONG` | Selector evidence and page identity/context identify one declaration. |
| `PROBABLE` | One selector match exists, but contextual evidence is incomplete. |
| `AMBIGUOUS` | More than one declaration remains possible. |
| `NO_MATCH` | No supported evidence links the element to source. |
| `CONFLICT` | An override or evidence set is internally inconsistent. |

The result keeps its evidence, conflicts, limitations, and metrics. Two `Save` buttons or two occurrences of the same broad selector remain ambiguous unless page, state, region, selector context, or a reviewed override disambiguates them.

```java
PageObjectCorrelation correlation = new PageObjectCorrelator().correlate(
        applicationModel,
        source,
        CorrelationOverrides.none()
);
```

Overrides are configuration, not source mutation. They must reference stable model and index IDs and should be reviewed alongside the model version they target.

## Source usage graph

The bounded usage graph is carried by `ExistingProjectIndex.Edge`; it is not a whole-program Java call graph. V1 records these conservative relations:

```text
TEST_TO_METHOD
METHOD_TO_METHOD
METHOD_TO_DECLARATION
```

That is enough to project a typical chain such as:

```text
InvalidLoginTest.invalidPassword
    -> LoginPage.login
    -> username / password / loginButton declarations
    -> LOGIN_USERNAME / LOGIN_PASSWORD / LOGIN_SUBMIT
```

Reflection, unresolved overloads, dynamic dispatch, generated code outside configured roots, and an incomplete classpath can leave edges unresolved. These cases are limitations, not guessed links. A similarity result from Find Similar can support investigation, but it never means "same element."

`SourceImpactAnalyzer` traverses the bounded graph from an application element through correlated declarations and Page Object methods to affected tests. It reports an edge-limit limitation if traversal is truncated. This supports review statements such as "this declaration is used by two methods and seven tests" without sending all project sources to an agent.

## Existing and generated Page Objects

The source index distinguishes hand-written code, generated bases, and generated extensions. A correlated hand-written `LoginPage` is not a reason to generate `LoginPage2`. Generation and regeneration remain separate from indexing:

- indexing describes code that already exists;
- correlation relates that code to the application model;
- the deterministic generator owns only its generated base files;
- user-authored extensions remain user-owned;
- a missing operation becomes a reviewable `PageObjectExtensionProposal`, not a duplicate class or an invented selector.

Continue with [source-aware AI workflow orchestration](../../ai/workflow-orchestration.md) or follow the [existing-project tutorial](tutorial.md#continue-from-an-existing-selenium-project).
