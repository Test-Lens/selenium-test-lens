# Test Lens 0.4.x patch-release lane

After v0.4.0, `main` carries the latest stable 0.4.x work and is bumped to `0.4.1-SNAPSHOT`. Use `fix/0.4.1-<topic>` for a 0.4.1 fix, release 0.4.1 from an exact reviewed commit, tag it `v0.4.1`, and then bump `main` to `0.4.2-SNAPSHOT`.

Do not keep a permanent maintenance branch while no 0.5 feature line exists. If 0.5 development begins, introduce `maintenance/0.4.x` deliberately at that time.

## Allowed scope

The patch lane accepts bug and security/correctness fixes, Selenium or browser compatibility fixes, performance-regression fixes, documentation fixes, and release-tooling fixes. It excludes new feature contracts, Adaptive Parallelism, new scheduler behavior, and large public API additions.

## Preparing a patch release

Start from a clean worktree whose root version is the exact target snapshot, for example `0.4.1-SNAPSHOT`. Then run:

```powershell
./scripts/prepare-patch-release.ps1 -TargetVersion 0.4.1 -WhatIf
./scripts/prepare-patch-release.ps1 -TargetVersion 0.4.1 -Confirm
```

The helper validates the tracked reactor and clean Git state, rejects an existing local release tag, and updates only tracked reactor POM versions plus bounded MkDocs release fields. It does not update release prose automatically. Maintainers must add and review the changelog section and any release notes.

The helper does not commit, tag, push, publish to Central, create a GitHub Release, or deploy docs.

## Checking a prepared patch

After compiling the release-form tree, run:

```powershell
mvn -DskipTests compile
./scripts/check-patch-release.ps1 -ExpectedVersion 0.4.1
```

The read-only checker validates both reactor shapes, publication policy, documentation version, changelog release section, local tag absence, public API inventory, and `@since` baseline. Continue with the complete evidence and publication sequence in [Releasing Test Lens 0.4.x](release-0.4.x.md).

Always inspect the complete diff and stage intended files explicitly. Never use a patch helper as authorization to commit, tag, push, or publish.
