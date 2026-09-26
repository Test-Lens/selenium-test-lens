# Selector candidate live analysis

Test Lens 0.4.0 contains an internal, non-published foundation for explicit developer-tool candidate analysis. It
is not a Selector Lab UI, automatic repair facility, or source patcher. Normal Selenium execution and FAST do not
load policies, capture target metadata, generate candidates, or issue validation commands.

The JDK-only selector engine owns neutral snapshots, bounded candidate generation, stability-policy assessment,
score-free ranking, structured explanations, and advisory recommendations. The optional `selector-live` module
owns Selenium integration. It runs synchronously on the caller-owned driver thread, captures at most one bounded
read-only JavaScript metadata snapshot, and may call `getText()`, `getAccessibleName()`, and `getAriaRole()` at most
once each. It never reads current input, password, textarea, or contenteditable values.

Every unique executable candidate is resolved with exactly one `SearchContext.findElements(By)` call. The returned
list supplies both cardinality and Selenium element-equality comparison with the caller-provided target; there is no
second `findElement`, click, key input, focus, scroll, navigation, JavaScript click, or DOM mutation. Validation is
only **verified in the current session, context, and DOM state**. It is not a cross-run stability guarantee.

V1 candidate origins are the original locator, ID, configured test attributes (`data-testid` by default), name,
individual class tokens, tag/class, tag, exact link text, exact-text XPath, and one bounded descendant candidate per
ancestor depth up to three. Generation is capped at 50 candidates, eight class tokens, eight preferred attributes,
256 text code points, 2,048 locator code points, and a 32 KiB metadata snapshot. Accessible name and ARIA role are
rationale only; no fictional `By.role` or `By.accessibleName` API is generated.

Ranking is lexicographic: live correctness, stability/policy/evidence, scope fragility, project semantic preference,
complexity, churn avoidance, and a deterministic locator tie-break. It is not a probability or 0–100 quality score.
A `STABLE` policy can override a generated-looking appearance warning but can never override a wrong target, invalid
selector, or no match. UUID-like selectors remain visible; policy/evidence conflicts require review. The original
locator can legitimately remain the best result (`KEEP_CURRENT`). Recommendations never authorize source changes.

Raw values and executable custom `By` handles are request-local. Opaque locators are not reconstructed from
`toString()`, and reports must not persist Selenium objects, remote IDs, window handles, current form values, or an
ephemeral matching-element index. Any future durable codec belongs at the trusted tooling boundary and must redact
presentation data without claiming that a redacted locator is executable.
