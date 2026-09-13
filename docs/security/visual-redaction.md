# Visual redaction / sensitive element masking

Visual redaction covers pixels rendered by sensitive page elements while Test Lens captures screenshot evidence. It uses temporary browser-side overlay nodes: element values, attributes, application state, focus, and event handlers are not changed. Every capture removes its masks in `finally`, including when WebDriver screenshot capture fails.

This mechanism complements, but does not replace, [`RedactionPolicy`](redaction.md):

| Protection | Configuration | Boundary |
| --- | --- | --- |
| Text redaction | `RedactionPolicy` | logs, trace, network diagnostics, reports, and text bundle components |
| Visual redaction | `VisualRedactionOptions` | pixels in Test Lens viewport and stitched full-page screenshots |

```java
VisualRedactionOptions visual = VisualRedactionOptions.builder()
        .maskPasswordInputs(true)
        .mask(By.id("username"), VisualMaskMode.BLUR)
        .mask(By.id("password"), VisualMaskMode.SOLID)
        .failurePolicy(VisualRedactionFailurePolicy.STRICT)
        .build();

TestLens lens = TestLens.attach(driver, TestLensOptions.builder()
        .redactionPolicy(RedactionPolicy.defaults())
        .visualRedaction(visual)
        .build());
```

## Modes and safe defaults

`SOLID` covers the complete bounding rectangle with an opaque surface. It is the recommended security-oriented mode for passwords, credentials, tokens, customer identifiers, and other confidential data. The default color is neutral `#2B2F36`; `solidColor(...)` accepts only `#RRGGBB`.

`BLUR` uses CSS `backdrop-filter` with a bounded radius of 2–32 CSS pixels (8 by default). **BLUR is visual privacy/obfuscation, not cryptographic, irreversible, or equivalent to SOLID redaction.** Fine shapes can remain inferable. If the browser does not confirm blur support, Test Lens safely falls back from BLUR to SOLID and reports that fallback in the capture result; it never falls back to an unmasked BLUR target.

`paddingPx(...)` optionally expands every rectangle by 0–32 pixels. `maskLabel(...)` adds plain text to SOLID masks; it is assigned through `textContent`, never HTML. No arbitrary CSS, HTML, JavaScript, filter string, or class injection is exposed.

`input[type=password]` is automatically masked with SOLID by `VisualRedactionOptions.defaults()` and therefore by `TestLensOptions.defaults()`. This selector is evaluated in the current Selenium frame immediately before capture and before every full-page tile. It does not mask ordinary text inputs. Disable it explicitly with `maskPasswordInputs(false)` only in controlled evidence.

## Failure policy

`STRICT` is the default and fail-closed choice. A missing/stale target, locator failure, overlay failure, geometry mismatch, or missing browser confirmation returns a failed screenshot result and does not publish the PNG. Automatic password discovery may legitimately find zero fields. In failure collection, the screenshot component is marked failed and its diagnostic is aggregated with the bundle/finalization diagnostics; the original test failure remains the session outcome.

`BEST_EFFORT` is an explicit opt-in. Test Lens masks every target it can resolve, captures evidence, and places a structured summary in `ScreenshotCaptureResult.message()`: requested, installed, verified, failed, and non-secret reason categories. An absent automatic password selector is normal; an absent explicit locator is a warning.

## Capture coverage and lifecycle

The same `ScreenshotCapture` seam protects manual `TestLens` and `JsOverlayDebug` screenshots, failed-step screenshots, automatic legacy failure screenshots, failure-bundle diagnostic and clean screenshots, and both viewport and portable full-page captures.

The lifecycle is sequential on one WebDriver: resolve all current elements, install the complete batch in one browser-side command, wait for two animation frames, verify every mask against its target's **current** geometry, capture pixels, then remove the batch in `finally`. Test Lens does not use parallel streams, virtual threads, or concurrent script calls against one driver.

Verification checks that the original target reference is still connected, both rectangles remain valid, the mask belongs to the current batch, and the mask rectangle still covers and matches the target rectangle within an internal 1.5 CSS-pixel rounding tolerance. If React or another SPA replaces, moves, or resizes a target during the paint barrier, Test Lens removes the entire batch, resolves every rule again, and performs one bounded retry. `STRICT` captures only after all requested masks are verified. `BEST_EFFORT` reports the incomplete batch explicitly after retry exhaustion.

The batch container is a top-level sibling of application roots under `body` (or the document element fallback), so React normally does not own its nodes. Mask DOM survival is not treated as proof that target geometry remains valid. Masks use `pointer-events: none`, do not receive focus, and do not change DOM values. No continuous `MutationObserver` or `ResizeObserver` tracking is installed.

“Clean” means that Test Lens HUD, highlights, arrows, and diagnostic decoration are temporarily hidden. It never means unredacted: the sensitive masks are installed independently for both the diagnostic and clean capture.

For `FULL_PAGE`, every tile performs scroll/settle, complete re-resolution, batch installation, paint barrier, geometry verification, tile capture, and batch replacement/cleanup. Geometry from a preceding tile is never the source of truth, including for sticky or fixed elements. Existing full-page geometry/scale limits and restoration rules still apply.

## Supported contexts and limits

- The current Selenium browsing context is supported for viewport capture. A rule aimed at an element in the current frame can be masked. Test Lens does not traverse child iframes automatically; switch into a frame to resolve its document. It does not attempt cross-origin frame penetration.
- Full-page capture remains top-level-context only. It does not expand iframe documents. Pixels rendered inside an unselected iframe are not covered by rules from its parent document.
- Standard Selenium locators can target light DOM. Open shadow roots require the caller to expose/use an appropriate Selenium search context; the current `By` rule registry does not recursively traverse them. Closed shadow roots cannot be targeted.
- A mask can cover the rectangular pixels of a selected `canvas` or `video` element, but Test Lens cannot identify sensitive subregions inside their bitmap/frame. Native browser chrome, permission dialogs, URL bars, OS dialogs, and other UI outside the page DOM cannot be masked.
- Top-layer browser constructs and unusual compositing are browser-controlled. Use STRICT and verify evidence in the browsers you support for high-risk workflows.
- Page source still contains the original values. Visual masking does not sanitize DOM, page-source, console, trace, network, or attached video artifacts.

## Screenshots versus live/video masking

Masks exist only for the duration of a Test Lens screenshot. Existing videos are caller-supplied attachments and are not modified. The renderer can later be extended to persistent masks for live/recording use, but that requires lifecycle ownership across navigation, frame changes, DOM mutation, and recorder start/stop; it is deliberately outside this feature.

## Small visual-demo fixture

The real-browser contract fixture demonstrates the intended setup: a normal customer input is explicitly SOLID-masked, the customer email is BLUR-obfuscated, and a password input receives automatic SOLID masking. The test analyzes the resulting pixel regions and also places a synthetic secret below the initial viewport to exercise scroll-and-stitch. No real credentials are stored in fixtures.
