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

## S10B3 explicit orchestration

S10B3 completes the S10 preparation/review boundary. A `MigrationRunPlan` is a content-addressed, shell-free plan
containing a trusted executable reference, an argument array, a previously validated worktree binding, a small typed
environment overlay, secret-reference names, a finite timeout, bounded expected artifacts, execution mode and scope,
business side-effect classification, checkpoint, and baseline role. The local host separately binds the executable
reference to an absolute executable and the worktree reference to a directory. Imported JSON, HTML, source, selectors,
and browser content cannot supply either binding.

Execution always uses `ProcessBuilder(List<String>)`. Arguments are never reparsed from the human-readable dry-run
display. V1 on Windows supports native executables such as `java.exe`; `.cmd`, `.bat`, and `.ps1` are rejected with a
safe stop because supporting them would introduce a command-shell boundary. Project wrappers are suggestions until a
host deliberately supplies a supported executable. There is no general shell capability.

Authorization has two independent dimensions. `EXECUTE_PROJECT_TEST_COMMAND` acknowledges that Maven, Gradle, Java,
or a test runner can execute arbitrary host/project code. A second capability acknowledges application/data effects:
`RUN_READ_ONLY_TEST`, `RUN_UNKNOWN_SIDE_EFFECT_TEST`, or `RUN_MUTATING_TEST`. Arbitrary tests default to `UNKNOWN`;
method names and annotations do not prove read-only behavior. Missing capabilities produce `SAFE_STOP` without a TTY
prompt, including in CI.

The child inherits the host environment according to normal Java `ProcessBuilder` behavior; B3's additional overlay
contains only allowlisted non-secret keys. Plans serialize secret-reference names, not values. A host resolver injects
a value transiently into the child environment; values are excluded from plan IDs,
dry-run output, checkpoints, and durable results. Standard output and error are independently bounded to at most
8 MiB, drained concurrently, and represented durably by byte counts, digests, truncation state, and an empty-by-default
diagnostic summary. Exact logs are local-sensitive and remain in memory unless the host explicitly writes them through
the existing safe state store. Every process has a timeout and cancellation control. Timeout, cancellation, or thread
interruption terminates descendants and the parent with a bounded graceful/forced sequence and records if any process
remains.

### Run artifacts and baselines

Expected artifacts are trusted plan paths below either a conservative project `target/` or `build/` output root, or
the B1 state root. Absolute
paths, traversal, `.git`, symlink escape, and oversized files are rejected. Before execution the orchestrator records
existence and exact SHA-256; afterward it enforces `MUST_BE_CREATED`, `MUST_CHANGE`, `MAY_REUSE`, or `OPTIONAL`.
Modification time is not evidence of freshness. A compatibility manifest projection strictly rejects duplicate JSON
fields, unknown top-level fields, unsupported schema/algorithm versions, malformed IDs, test-identity mismatch, and
effective headed/headless or DEFAULT/FAST mismatch. A stale, malformed, or mode-mismatched manifest cannot become new
evidence.

Migration Assistant cannot make an arbitrary Maven, TestNG, or JUnit suite emit compatibility manifests. A supported
run either invokes an already configured trusted command, consumes a trace/bundle through a separately supplied trusted
host adapter, or receives manifests externally. Migration tooling deliberately does not depend on Selenium-heavy
`compatibility-tooling` and installs no runtime listener.

`ORIGINAL_BASELINE` binds evidence and run records to the pre-instrumentation source/configuration checkpoint.
`INSTRUMENTED_BASELINE` requires an original parent plus an explicit instrumentation-delta digest and a new checkpoint;
it never replaces or masquerades as the original. B3 only records instrumentation already established externally or
manually. It does not apply an instrumentation patch. Selected headed STANDARD, headless STANDARD, and headless FAST
runs remain separate plans, and a single-test scope is never silently expanded to a suite.

Before and after every run, Git preflight/source fingerprinting produces checkpoint history. An unexpected tracked or
untracked source-state change is reported as `SOURCE_STATE_CHANGED_BY_RUN` and blocks continuing as if the baseline
were stable. Expected ignored output artifacts do not dirty the source state. Interrupted ledger entries resume
conservatively as `INTERRUPTED`, `UNKNOWN_COMPLETION`, or `ARTIFACTS_PARTIAL`; process disappearance is never treated as
proof that the command did not finish.

### Approved Git isolation actions

B3 has a separate fixed-operation dispatcher; B1's read-only allowlist is unchanged. The only mutation templates are:

```
git branch -- <validated-branch> <exact-commit-oid>
git worktree add -b <validated-branch> -- <trusted-absolute-destination> <exact-commit-oid>
git stash push --message test-lens-migration-v1
git stash push --include-untracked --message test-lens-migration-v1
git stash apply --index <exact-recorded-stash-oid>
```

Branch/worktree candidates pass `check-ref-format`, bases are verified as exact local commit OIDs, repository/worktree
bindings are rechecked, ongoing Git operations block the action, and existing branches or destinations are never
forced, reset, pruned, removed, or reused. Branch creation does not switch the current worktree. Worktree creation
requires both `CREATE_BRANCH` and `CREATE_WORKTREE`; partial failure triggers a fresh observation but no automatic
deletion. Every successful mutation produces a fresh B1 checkpoint while preserving the prior checkpoint.

Mutation commands keep prompting and paging disabled and pass `core.fsmonitor=false`, but unlike B1 reads they do not
set `GIT_OPTIONAL_LOCKS=0`, because real mutations need Git locks. No mutation template contains fetch, pull, push,
commit, reset, clean, restore, checkout, merge, or rebase.

On Git 2.55 for Windows, disposable sentinel fixtures demonstrate that the exact `worktree add` template invokes a
repository `post-checkout` hook and a configured smudge filter, and that the exact `stash push` template invokes a
configured clean filter. These are repository-controlled executable code. The exact branch-ref creation template did
not invoke the `post-checkout` sentinel.
Consequently worktree creation also requires the distinct `ALLOW_REPOSITORY_GIT_HELPERS` capability; without it the
tool returns `SAFE_STOP` before Git runs. Stash creation/restoration conservatively requires the same capability because
Git consults attributes and clean/smudge filters while materializing stash trees; the exact helper set still depends on
repository configuration. Branch creation does not require it because it only creates a ref and performs no checkout.
This retains repository semantics without silently disabling helpers or pretending isolation is code-execution-free.

Stash is an explicit alternative, not the default recommendation for a dirty tree. `TRACKED_ONLY` excludes untracked
and ignored files; `TRACKED_AND_UNTRACKED` adds `--include-untracked`; V1 never uses `--all`. The tool records `refs/stash`
before and after creation and persists the new exact commit OID, scope, bindings, source fingerprints, and before/after
checkpoints. Restore requires `RESTORE_STASH`, the exact recorded OID, and an unchanged post-stash source precondition,
then uses `stash apply --index <oid>`. It never uses a moving `stash@{0}`, `pop`, or `drop`. Conflicts are reported and
left for manual resolution; the stash remains retained.

The orchestration dry-run adds plan IDs, executable references, arguments as separate display values, environment key
names, secret-reference names, expected artifacts, baseline roles, isolation operations, exact commit bases, stash
scope, and every required capability. It executes nothing and never exposes secret values. The B3 state machine ends at
`REVIEWED_FOR_S11`; it includes `PREFLIGHT_READY`, `ISOLATED`, `BASELINE_READY`, `RUNNING`, `EVIDENCE_READY`,
`ANALYSIS_READY`, `PROPOSALS_READY`, `INTERRUPTED`, and `BLOCKED`, but deliberately has no `APPLIED` or `VERIFIED` state.

S10 is therefore complete as preparation, evidence, proposal, review, run orchestration, and workspace isolation.
There is still no source apply, patch apply, post-patch compile/test, automatic rollback, commit, merge, rebase, or Git
network behavior. S11 receives content-addressed checkpoints, exact source preconditions and patch bytes, proposal and
decision ledgers, run/baseline evidence, and isolation-action records; S11 alone may implement guarded source apply,
verification, and precondition-aware rollback.
