# Read-only Selector Lab foundation

Selector Lab is an internal, non-published developer-tooling surface. It opens only through an explicit internal
call; while closed it installs no DOM, listeners, command waits, candidate work, or WebDriver commands. FAST keeps
its normal zero-presentation behavior until that explicit call and is not changed to DEFAULT.

The caller thread exclusively owns WebDriver for the entire blocking Lab session. Browser-to-Java communication
uses one bounded `executeAsyncScript` wait at a time. The session preserves the caller's Selenium script timeout,
temporarily widening it only when required for the bounded human wait and restoring it in `finally`. No background
thread or global driver is used.

## Picker contract

PICKING installs a Test Lens-owned viewport plane. Hit testing temporarily disables only that plane, recursively
enters at most eight nested open shadow roots, and returns the selected element and shadow hosts through standard
WebDriver element serialization. The target receives no style, class, attribute, focus, or marker mutation.

The precise guarantee is: **No picker event is dispatched to the selected application element and no default action
of that element is executed.** A pre-existing capture listener on `window` or `document` may observe an event whose
target is Test Lens instrumentation before the later-installed picker listener can suppress it. The Lab does not
claim otherwise and has no CDP/provider-specific workaround. Closed shadow roots are unsupported.

Lab operates only in the caller's current frame and current window. It never switches frames or windows. To inspect
an iframe descendant, the caller first switches WebDriver into that frame. Navigation or document replacement
invalidates the target, analysis IDs, retained elements, queued commands, and Lab highlights; V1 closes the active
exploration rather than carrying state into the new document.

## Read-only command boundary

Commands are versioned, session/document scoped, monotonic, limited to 16 queued entries and 16 KiB payloads, and
refer only to Java-known `analysisId` and `candidateId` values. The allowlist is START_PICK, REPICK,
HIGHLIGHT_CANDIDATE, FIND_SIMILAR, PREVIEW_PATTERN, and CLOSE. Browser locator text and JavaScript are never accepted
for execution. `sessionRef` detects stale sessions; it is not an authentication secret because page JavaScript can
inspect same-world tooling and the open shadow root.

Candidate generation, Selenium validation, same-target comparison, policy evaluation, and ranking remain in
`LiveCandidateAnalysisService`. Java sends a redacted presentation in exact engine order. The browser never reranks
or becomes the executable source of truth. Up to 20 matches per candidate and 200 total element references are
retained ephemerally for highlighting; highlighting never re-resolves, clicks, focuses, or scrolls. Lab highlights
use their own nodes and cleanup does not clear ordinary action/assertion highlights.

Find Similar and pattern preview operate only on a caller-supplied in-memory catalog. Audit context arrives through
a sanitized JDK-only projection. Lab does not parse the project, load history, write JSON, save policy, mutate
source, invoke Git, or perform automatic repair.

## Presentation and security

Redaction occurs before values enter browser DOM. Password/input values, raw policy notes, source expressions,
remote element IDs, window handles, and test arguments are not rendered. Dynamic content is inserted with
`textContent`; the Lab uses no `eval`, `Function`, local storage, or session storage. Copy is offered only for an
approved non-redacted standard Selenium locator. Opaque or redacted candidates cannot be copied.

The module is excluded from Central publishing in 0.4.0. It is not a public Selector Lab API, does not add
`TestLens.openLab()`, and does not enable policy editing, Apply/Fix, or source rewriting.
