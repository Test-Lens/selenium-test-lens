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

## S11B1 guarded source Apply

S11B1 adds source mutation only inside the non-published migration-tooling module. It does not add a runtime API,
compile or run tests, perform headed/headless/FAST verification, invoke a shell, mutate source through Git, or commit.
Its successful terminal state is `VERIFICATION_PENDING`, never `VERIFIED`.

An `APPROVE_FOR_S11` decision makes a proposal eligible for consideration; it is not source-write authority. The host
selects exact proposal IDs and separately supplies `APPLY_APPROVED_SOURCE_CHANGES` bound to the resulting apply-plan
ID and repository/worktree bindings. Decisions are active only through explicit decision-ref supersession. Array
order and timestamps have no precedence. Missing superseded decisions, cross-proposal links, cycles, or multiple
non-superseded decisions invalidate the ledger. A superseding reject or defer prevents Apply.

Proposal dependencies use typed, content-addressed `MigrationDependencyResolution` artifacts. New evidence,
configuration alignment, prerequisite proposals, and trusted manual decisions are distinct resolution types bound to
an exact proposal, dependency semantic ref, checkpoint, and evidence digests. Approval never bypasses an unresolved
or stale dependency.

Preparation reruns read-only Git preflight and source fingerprinting, writes a fresh checkpoint, and validates every
selected proposal before any source write. Only `READY_FOR_REVIEW` proposals with exact source edits are accepted.
All files are contained below the trusted worktree, regular and non-symlink, outside `.git`, strictly UTF-8 or UTF-8
BOM, and LF or CRLF. The current full-file digest, AST node, declaration, range, old construct, and locator semantics
must still match. There is no nearby search, fuzzy relocation, or automatic patch rebase.

Executable proposal edits are rebuilt in memory rather than executed directly. The immutable
`MigrationApplyPlan` binds the selected proposals, effective decisions, dependency resolutions, evidence,
repository/worktree/checkpoints, approved UTF-16 ranges, exact original and proposed file SHA-256 values,
content-addressed byte artifacts, merged verification-requirement digest, and exact owned-diff digest. The strict
Jackson Core codec rejects duplicates, unknown fields, unsupported versions, invalid enums, semantic-ID mismatch,
and bounds violations. The persisted plan is reloaded and must equal the plan whose ID the host authorized.

Hard V1 bounds are 32 proposals, 64 files, 512 edits, 64 MiB per source artifact, 512 MiB aggregate originals,
512 MiB aggregate targets, 64 MiB exact final diff, 16 MiB per plan/journal document, and 1,024 issues. Existing-file
modification is the only supported source operation; file creation and deletion remain unsupported.

### Lock, journal, and replacement

A repository-binding-specific `FileChannel.tryLock()` is acquired before last-moment validation and held through
replacement, immediate compensation, and transaction finalization. It prevents concurrent Test Lens Apply calls but
does not lock an IDE or arbitrary process, so every target SHA is checked again before the first write and immediately
before its own replacement. Any pre-write mismatch aborts the entire batch with zero writes. A mid-transaction
external edit stops remaining files and produces a partial transaction without overwriting that edit.

Before mutation, all original and target artifacts must exist and match their exact digest and size; an ownership
registry and `PREPARED` journal event must be durable. Source backups are the bytes observed before migration, so a
pre-existing user edit is preserved in both the backup and the rollback target rather than replaced with Git `HEAD`.
Backups, targets, plans, journals, and diffs are `LOCAL_SENSITIVE_ARTIFACT` data below the already-safe B1 state root.

The journal is an append-only sequence of individually atomic event files. Each event binds its sequence,
transaction, semantic payload, previous-event digest, and own digest. Missing, reordered, corrupted, or cross-
transaction events fail integrity validation. Operational timestamps are provenance and do not affect event identity.
The transaction ID is derived from the exact plan and preparation checkpoint plus a fixed V1 attempt identity; it is
not a random UUID.

Each file uses a transaction-owned sibling temp created with `CREATE_NEW`, exact bytes written through `FileChannel`,
`force(true)`, digest verification, and `ATOMIC_MOVE` with replacement when supported. The explicit fallback is a
same-filesystem replacement move and is not described as atomic. POSIX permissions are retained when available;
DOS read-only files are blocked rather than made writable. Windows ACL ownership and arbitrary extended attributes
are not promised. The resulting target digest is checked immediately. Multi-file Apply is deliberately not called
atomic: replacements occur one at a time and a failure at file N leaves an exact `PARTIAL_APPLY` record and never
attempts N+1.

Immediate compensation is available only in the same uninterrupted Apply call after an internal write failure and
only when the host supplied a separate `ROLLBACK_TOOL_CHANGES` authorization in advance. Each target must still equal
the exact tool-written digest. Apply authority alone does not imply rollback authority, and later verification failure
will never trigger automatic rollback.

### Rollback and recovery

Explicit rollback is transaction-scoped and requires `ROLLBACK_TOOL_CHANGES` bound to the exact transaction and local
bindings. A file is restored only when its current digest equals the transaction's proposed digest. An original digest
is idempotently `ALREADY_ORIGINAL`; any other digest is `USER_MODIFIED` and is never overwritten; missing files remain
missing. Rollback uses the same guarded exact-byte replacement boundary and never invokes `git reset`, `checkout`, or
`restore`. It affects no unrelated file and retains the append-only history.

Crash recovery reads the typed plan, ownership registry, journal chain, backups, and current target digests. Each file
is classified as `ORIGINAL`, `TOOL_APPLIED`, `UNKNOWN_MODIFIED`, or `MISSING`. A prepared transaction with only original
bytes is safe to abandon. A mixture of original and proposed bytes offers guarded rollback but never continues pending
writes automatically. All proposed bytes with a missing final event require reconciliation, not a second Apply. An
unknown modification, missing backup, or corrupt journal requires manual recovery. Only an exact transaction-owned
temp name and target digest may be cleaned; arbitrary `.tmp` files are outside tool ownership.

The final diff is generated from transaction-owned original and proposed artifacts, with logical `a/` and `b/` paths,
not from the whole worktree. Pre-existing unrelated dirt is excluded and pre-existing edits in a touched file remain
part of the original side. The sanitized apply-plan summary exposes only paths, counts, digests, budgets, dependency
state, required capabilities, rollback availability, and verification-requirement digest; exact bytes and diff remain
separate local-sensitive artifacts.

After all target digests pass, S11B1 records a fresh source checkpoint and appends `VERIFICATION_PENDING`. Failure to
record post-apply state is `RECOVERY_REQUIRED` and does not cause an unsafe automatic rollback. S11B2 must execute at
least the verification requirements whose digest is bound into the ApplyPlan; it may add stricter checks but cannot
silently remove reviewed requirements. Compile, affected tests, headed/headless/FAST runs, post-change compatibility
comparison, and the final verification verdict belong exclusively to S11B2.

## S11B2 post-apply verification

Verification is a separate, non-mutating phase over an accepted Apply transaction in `VERIFICATION_PENDING`. The
content-derived verification plan must reproduce the exact requirement digest bound into the ApplyPlan; additional
requirements may make a later plan stricter, but no required reviewed obligation can disappear. Requirements are
deduplicated by typed stage, target/test identity, execution mode, expectation, side-effect class, and source scope,
then run in the fixed order source structure, compile, direct tests, known use-site tests, headed, headless, FAST,
manifest validation, and compatibility comparison. Only required stages determine `VERIFIED`; optional stages remain
visible when not run.

Before verification and before and after every stage, tooling checks the repository/worktree binding, the exact
tool-written digest of each touched file, and the post-Apply source-state digest. Ignored `target/` or `build/`
outputs do not change that source state, while a tracked or other relevant source change blocks the remaining plan
as `POST_APPLY_SOURCE_CHANGED`. The post-Apply checkpoint/source digest, ApplyPlan ID, and transaction ID are the
revision identity for uncommitted source; unchanged Git `HEAD` is never presented as the patched revision.

A verification requirement is not a command. A trusted host binds an exact stage ref to an existing shell-free
`MigrationRunPlan`, working-tree/checkpoint binding, expected artifacts, network policy, and optional wrapper
identity. The existing S10 authorization still requires `EXECUTE_PROJECT_TEST_COMMAND` plus the exact read-only,
unknown, or mutating business-side-effect capability. Compile also executes project/build code. Missing approval or
a missing exact binding blocks the stage instead of choosing a nearby test or broadening its scope.

Windows wrapper support uses trusted `java.exe` and an exact, digest-bound wrapper JAR/properties pair. The only
allowed main classes are `org.gradle.wrapper.GradleWrapperMain` for
`gradle/wrapper/gradle-wrapper.jar` plus `.properties`, and
`org.apache.maven.wrapper.MavenWrapperMain` for `.mvn/wrapper/maven-wrapper.jar` plus `.properties`. Maven layouts
without the compatible JAR stop as unsupported. Wrapper paths must remain regular, non-symlink files below the
worktree; changed JAR or properties bytes make the run plan stale. `.cmd`, `.bat`, and `.ps1` remain unsupported and
there is no generic `cmd.exe` or shell fallback.

Build network authorization is explicit. `NETWORK_AUTHORIZED` requires `ALLOW_BUILD_TOOL_NETWORK`; `OFFLINE`
requires the wrapper's typed offline argument, and a wrapper cannot claim that network is irrelevant. This is an
authorization and disclosure boundary, not an operating-system firewall. A wrapper bootstrap may still fail when
its distribution is unavailable locally. Credentials embedded in a distribution URL are never shown; exact
properties bytes are represented only by their SHA-256 binding.

Fresh compatibility manifests are checked with the S10 pre/post digest rules and a strict bounded migration-side
reader. Required post-change evidence must bind the transaction, ApplyPlan, checkpoint, source-state digest,
execution mode, and structured test identity. Process success, test outcome, artifact validity, and compatibility
acceptance remain separate facts. A stale/missing manifest or insufficient source relation cannot certify the
transaction.

Comparison axes are question-specific: pre/post headed and pre/post headless use `BUILD`; post-change headed versus
headless uses `HEADLESS_MODE`; and post-change headless DEFAULT versus FAST uses `OBSERVABILITY_MODE`. The last two
must share the new source revision, while BUILD must compare different source revisions. Plan-bound acceptable
outcomes and comparability rules decide the stage. A headed regression, unresolved targeted headless finding, or
FAST execution regression fails; expected FAST evidence reduction can be accepted. Historical baseline-red evidence
is retained rather than rewritten as a patch regression.

Verification results distinguish `VERIFIED`, `FAILED`, `INCONCLUSIVE`, `BLOCKED`, and `INTERRUPTED`, with the same
coverage per stage and proposal. Plans and results use strict duplicate-rejecting, bounded deterministic JSON below
the B1 safe state root. Reruns create new result history and an explicit supersession relation without applying source
again. A short transaction-operation lock prevents rollback and verification state transitions from racing; no
repository Apply lock is held during external commands. Compile, test, timeout, or comparison failure never triggers
automatic rollback. B1's separately authorized ownership-checked rollback remains the only rollback path. S11B3
owns final report and recovery presentation, and automated commit remains absent.

## S11B3 final reporting and recovery presentation

The final `MigrationApplyReport` is a deterministic, content-addressed local record of one Apply transaction. It keeps
the historical transaction outcome, the accepted verification result, and the freshly inspected current worktree
relation as three independent facts. A transaction can therefore remain historically `APPLIED_VERIFIED` while its
current relation says `USER_MODIFIED_AFTER_APPLY`; later user work never erases old evidence and is never described as
verified. Report generation performs a new read-only Git preflight, source fingerprint, touched-file digest check,
journal-chain validation, backup check, and owned-diff artifact check. It executes no command, verification, recovery,
rollback, source write, or Git mutation.

The canonical migration-owned diff is still the S11B1 original-artifact-to-target-artifact diff. Whole-worktree Git
diffs and unrelated dirty files are not migration ownership evidence. The sanitized JSON and standalone HTML expose
logical paths, edit counts, semantic digests, Apply outcomes, and the local-sensitive diff reference/digest/byte count,
but never embed exact hunks or source bytes. Absolute paths, logs, secret values, environment values, and credentialed
wrapper URLs are excluded. Exact diff viewing is an explicit local-sensitive operation outside the shareable report.
HTML is static, escaped, self-contained, and has no Apply, rollback, test, Git, or commit controls.

Verification coverage lists every required and optional stage as passed, failed, blocked, inconclusive, interrupted,
or not run. Compile, direct tests, known-use tests, headed, headless, FAST, manifest validation, and compatibility
comparison remain separate rows. Per-proposal coverage maps the exact required stages back to each proposal, and the
compatibility projection retains comparison intent, outcome, comparability, and targeted finding state. A successful
process or one resolved proposal cannot hide an unrun or failed required obligation. Verification history is displayed
with an explicitly selected accepted result ref; timestamps never select a latest result.

Recovery presentation consumes S11B1's inspection rather than changing its rules. `MigrationRecoverySummary` explains
whether a prepared transaction is safe to abandon, guarded rollback is wholly or partly available, metadata-only
reconciliation is possible, or manual recovery is required. The immutable `MigrationRecoveryActionPlan` lists only
actions that could be requested. Creating a report never performs them: rollback still requires the targeted
`ROLLBACK_TOOL_CHANGES` capability, reconciliation requires intact transaction evidence, and temp cleanup is limited
to exact registered transaction-owned names. Per-file rollback visibility reports current original/tool/user/missing
state and backup validity instead of presenting a misleading batch boolean.

`MigrationCommitReadiness` is advisory and has only `COMMIT_READY_MANUAL`,
`MANUAL_COMMIT_REQUIRES_REVIEW`, and `COMMIT_BLOCKED`. Readiness requires a complete Apply, a `VERIFIED` accepted
result, current source matching the verified state, and an intact exact owned diff. A touched file that was dirty before
Apply, unrelated staged content, or unrelated worktree changes downgrades readiness because normal staging may include
user work. The report tells the maintainer to inspect the owned diff and index, stage only intended changes, and commit
manually. Migration tooling implements no `git add`, `git commit`, automatic commit, push, fetch, pull, merge, or rebase.

The final JSON reader is strict, bounded, duplicate-rejecting Jackson Core with deterministic ordering and stable LF;
state-store replacement remains atomic. JSON and HTML are limited to 64 MiB, with 32 proposals, 64 files, 512
verification stages, 128 release gaps, and 1,024 issues. Core failure, blocked rollback, required recovery, or current
divergence cannot be silently truncated. Report, exact diff, backups, and journals remain local-sensitive artifacts
under the B1 safe state root.

S11 is complete at this boundary: guarded exact-byte Apply, ownership-checked rollback and recovery, post-Apply
verification, current-state inspection, final owned-diff reporting, and manual commit readiness exist. Automated commit
is deliberately not required for 0.4.0 and no runtime migration API was added.

### S12 release-hardening handoff

`ReleaseHardeningHandoff` and its structured `MigrationReleaseGap` entries make NOT_RUN, skipped, deferred, and known
limitations machine-visible. The current handoff retains these established facts rather than converting them to prose
or claiming success:

- Windows symlink fixtures were skipped where the environment could not create symlinks.
- local Maven wrapper-main execution is covered by a no-network fixture that launches the fixed main class through
  the production `java.exe` process boundary; generic `.cmd`, `.bat`, and `.ps1` execution remains unsupported;
- BrowserStack and Remote/Grid validation remain NOT_RUN;
- the high 10k compatibility case observed about 1.19 GB and observability JSON approached the 64 MiB boundary;
- selector policy preview at 100k was about 44.8 seconds, and the prior selector-history near-64 MiB boundary remains a
  scale-hardening concern;
- the 10k physical-file JavaParser migration benchmark remains NOT_RUN; synthetic migration metrics remain available;
- the reactor-version script's ephemeral recursive target-path behavior must be reproduced and hardened in S12.

S12 owns the final current Chrome/Firefox headed/headless and DEFAULT/FAST matrix, key runtime workflows, feasible
Remote/Grid and available BrowserStack coverage, scale/memory/report-size disposition, platform skips, documentation,
and release gates. It must not present unavailable provider runs as completed.

S12 also owns the `PATCH_RELEASE_LANE` maintainer policy and any supporting docs/scripts. After v0.4.0, `main` remains
the latest stable 0.4.x and moves to `0.4.1-SNAPSHOT`; fixes use `fix/0.4.1-<topic>`, release as 0.4.1/tag `v0.4.1`, then
move to `0.4.2-SNAPSHOT`. No permanent develop/maintenance branch is required while no 0.5 feature work is planned; a
`maintenance/0.4.x` branch can be considered only when 0.5 development begins. This is maintainer policy, not a runtime
restriction. S11B3 records the requirement but does not create patch-release scripts, tags, pushes, or publications.
