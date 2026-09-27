# Migration Git preflight and checkpoints

S10B1 is internal, non-published, read-only migration tooling. It records Git and source state for later review; it
does not propose or apply edits, create branches or worktrees, stash changes, run tests, or contact Git remotes.
The interrupted S10B1 implementation was resumed from its existing worktree and completed without discarding or
rewriting the valid checkpoint, preflight, and fixture work that was already present.

## Git boundary

Git is invoked as a trusted executable with `ProcessBuilder(List<String>)`, never through a shell. The closed B1
allowlist contains read-only semantic operations only. Every invocation sets `GIT_TERMINAL_PROMPT=0`,
`GIT_OPTIONAL_LOCKS=0`, `GIT_PAGER=cat`, and `PAGER=cat`, and passes `-c core.fsmonitor=false`,
`-c color.ui=false`, and `--no-pager`. This prevents repository-configured fsmonitor and pager helpers from running.
No network operation is available. A `safe.directory` failure is reported without weakening global Git security.

Status uses `--porcelain=v2 -z`. Staged, unstaged, untracked, conflicted, rename, and copy facts remain distinct,
including staged-then-modified paths and names containing whitespace or newlines. Named, detached, and unborn HEAD,
worktrees, sparse checkout, and merge/rebase/cherry-pick/revert/bisect markers are represented. Submodule presence is
detected locally from `.gitmodules` and gitlinks; detailed state is intentionally partial and no update is run.

## Identity and source state

Opaque domain-separated digests bind a checkpoint to the same local Git common directory and worktree. Absolute
repository, worktree, `.git`, and user-home paths stay in an ephemeral local context and are not serialized. Another
clone requires explicit future rebind plus a complete fresh validation.

Clean tracked state uses the HEAD tree. Only dirty paths are hashed: exact index blob SHA-256 for staged content,
exact file SHA-256 for working/untracked regular files, link-target text digest without following symlinks, and an
explicit deleted state. Rename and copy origins are retained in the dirty fingerprint. Metadata is checked before
and after reads, and regular files are opened with no-follow semantics. A conflicted index is recorded by preflight
but deliberately makes the source checkpoint incomplete until manual resolution. Limits are 10,000 paths, 64 MiB per file,
512 MiB aggregate dirty content, and 16 MiB checkpoint JSON. Overflow or a concurrent edit yields
`INCOMPLETE_SOURCE_STATE` and blocks later mutation eligibility.

## State root and checkpoint

An explicit trusted state root is allowed, but an in-worktree root must be under `target`, already ignored, and not
inside `.git`. Without one, `target/test-lens/migration` is used only when `git check-ignore` proves it ignored;
otherwise `${user.home}/.test-lens/migration/<repository-binding>/` is used. Ignore files are never modified, and
checkpoint creation must not change the semantic worktree status.

Checkpoint IDs are `migration-checkpoint-v1:sha256:<hex>` and filenames use a Windows-safe hyphenated form. Jackson
Core streaming provides fixed ordering, strict duplicate/schema/enum checks, bounds, a stable trailing LF, and
sibling-temp atomic replacement. Checkpoints are local-sensitive because logical Git filenames are retained; they
contain no credentials and are never uploaded automatically.

Resume returns `VALID`, `VALID_WITH_WARNINGS`, `REQUIRES_REFRESH`, or `BLOCKED`, with independent reasons:
`HEAD_CHANGED`, `BRANCH_CHANGED`, `WORKTREE_CHANGED`, `INDEX_CHANGED`, `WORKING_FILE_CHANGED`,
`UNTRACKED_CHANGED`, `GIT_OPERATION_CHANGED`, `INPUT_REPORT_CHANGED`, `SOURCE_PRECONDITION_CHANGED`,
`MISSING_ARTIFACT`, `REPOSITORY_BINDING_CHANGED`, and `CHECKPOINT_INCOMPLETE`. B1 reports changed paths but has no
proposal-target relevance knowledge yet.

## Plans are not actions

`USE_CURRENT_WORKTREE`, `CREATE_DEDICATED_BRANCH`, `CREATE_DEDICATED_WORKTREE`, `STASH_EXPLICITLY`, and
`MANUAL_RESOLUTION_REQUIRED` are immutable plans. B1 never executes them. Exact-OID stash handling is reserved for
S10B3. Source apply, verify, rollback, and commit remain S11 responsibilities.

## S10B2 proposal and review boundary

S10B2 adds offline proposal planning, not migration execution. Compatibility comparison, static compatibility, and
Selector Audit JSON enter as untrusted bytes. Bounded duplicate-rejecting readers validate V1 schemas and project
only structured codes into `SANITIZED_EVIDENCE`. Exact file-byte SHA-256 is retained. Human explanations, HTML,
safe display text, candidate labels, and `By.toString()` are never promoted to patch material. In particular,
Selector Audit's `ALTERNATIVE_AVAILABLE` can request review but cannot supply a locator value.

Exact locator proposals require a current `TRUSTED_LOCAL_PATCH_MATERIAL` projection and an explicitly supplied,
live-validated trusted candidate. The narrow selector-tooling bridge exposes declaration and component references,
project-relative path, exact file digest, UTF-16 source range, declaration and expression shape, resolved standard
strategy/scalar when available, construct digest, declaring symbol, freshness, and limitations. It exposes neither
source text nor an AST. A bounded symbol-resolved use graph retains at most 100,000 uses, samples at most 256 per
proposal, and carries unresolved-symbol and source-root coverage rather than claiming unknown uses are complete.

The V1 Java transformer supports an exact standard Selenium `By.<factory>(compile-time scalar)` expression. It
re-reads the current file, verifies its full digest, strictly decodes UTF-8 or UTF-8 BOM, rejects mixed newlines,
reparses Java 17, and verifies the exact AST node and old locator semantics before computing an in-memory edit.
LF and CRLF are preserved. Java literal escaping does not reinterpret CSS or XPath. Parameterized/concatenated
locators, helper-generated/custom locators, PageFactory annotations, and a strategy-changing static-import call that
would need an import edit remain manual-only. A runtime observation such as `row-123` can never replace
`"row-" + rowId`.

Patch previews replace only approved UTF-16 ranges and use deterministic three-line unified-diff context with
`a/<logical-path>` and `b/<logical-path>` labels, no timestamps or absolute paths. Full-file SHA, node kind, construct
and semantic digests, encoding, newline convention, bindings, and already-applied semantics form the S11
precondition. Any later edit to a touched file stales the V1 proposal; there is no fuzzy relocation or silent rebase.
Exact diffs are `LOCAL_SENSITIVE_ARTIFACT` data and are in-memory by default. Explicit persistence can only use the
existing B1 state store under `patches/`, `proposals/`, `decisions/`, or `reports/`.

Proposal eligibility is `READY_FOR_REVIEW`, `REVIEW_REQUIRED`, `MANUAL_ONLY`, `BLOCKED_INSUFFICIENT_EVIDENCE`,
`BLOCKED_STALE_INPUT`, or `NO_CHANGE_RECOMMENDED`; there is no auto-apply state. Static findings remain hypotheses.
Non-comparable viewport, dataset, or browser evidence produces configuration/rerun guidance without a locator patch.
Policy stability, rank, Audit recommendation, and Use once are evidence or preferences, never source authorization.
Blast radius reports exact declaration targets, bounded known uses and correlated test refs, plus `COMPLETE`, `PARTIAL`,
or `UNKNOWN` coverage. Same-node/different-semantics and overlapping-range conflicts have no automatic winner;
configuration alignment and new-evidence dependencies are explicit.

The decision ledger records approve-for-S11, reject, defer, and manual-only acknowledgement against the proposal ID,
semantic digest, checkpoint, source preconditions, and evidence digests. Notes are bounded inert local text. Any
proposal, checkpoint, source, or evidence change invalidates approval rather than inheriting a decision by display
wording. The deterministic dry-run JSON summarizes evidence, eligibility/category counts, targets, blast radius,
conflicts, dependencies, verification steps, and decisions but omits exact diffs. S10B2 has no Apply button, source
writer, Git action, test command runner, or verification executor. S10B3 may add explicitly authorized orchestration;
S11 remains the sole owner of source apply, post-patch verification, and guarded rollback.
