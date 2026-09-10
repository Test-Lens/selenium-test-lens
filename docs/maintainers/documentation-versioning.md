# Publishing versioned documentation

Documentation is built by GitHub Actions and stored on the durable `gh-pages` branch through `mike`. GitHub Pages remains configured for **GitHub Actions**: after `mike` updates one version, the workflow uploads the complete branch as one Pages artifact. Do not use `mkdocs gh-deploy`, force-push, delete all versions, clear `gh-pages`, or switch Pages to branch deployment.

## Published Javadocs

Published API Javadocs are written in English. They document observable contracts rather than repeating
method names. A public behavior change must update both its Javadoc and a contract test in the same change.
Run `./scripts/check-public-javadoc-language.ps1` locally; CI checks the published modules for Polish
Javadoc text, including a maintained set of Polish terms written without diacritics. Do not guess `@since`
versions: verify the symbol against the corresponding release tag before adding or changing that tag.

## Version lifecycle

- `/dev/` is rebuilt from `main`; its display name is read from the root snapshot POM. It never moves `latest`.
- A `vMAJOR.MINOR.PATCH` tag is accepted only when every reactor POM has that exact non-snapshot version. The immutable version must not already exist. The workflow publishes from the tagged commit, assigns `latest`, and sets the root default to `latest`.
- `/0.1.0/` uses the archived sources in `docs-versions/0.1.0`, because tag `v0.1.0` predates the complete MkDocs site. Its edit link is disabled.
- The root URL and `/latest/` resolve to the latest stable release, never to `dev`.

After preparing a real release, remove its `Coming in X.Y.Z` markers in the release-preparation commit. After tagging, advance the root POM and `extra.version.current` to the next snapshot together. New unreleased features must receive a marker; normal dependency examples continue to use the latest Maven Central release. Never update an already published stable version from `main`.

## One-time 0.1.0 migration

After this commit is pushed:

1. Confirm repository Pages source is **GitHub Actions**.
2. Run the Documentation workflow with `bootstrap-0.1.0` from the Actions UI.
3. Enter the exact confirmation `publish-immutable-0.1.0`.
4. Inspect `/0.1.0/`, `/dev/`, `/latest/`, and the root URL.
5. Verify the version selector in both stable and development pages.
6. Confirm the root opens stable 0.1.0.

The workflow declares `contents: write` only on its publication job. If organization policy disables write-capable `GITHUB_TOKEN`s despite job permissions, enable repository **Read and write permissions** for Actions; do not replace the token with a personal secret.

## Verification and recovery

Run `./scripts/validate-versioned-docs.ps1` locally in PowerShell 7. It creates a disposable Git repository, bootstraps stable and dev documentation, hashes 0.1.0 across a dev redeploy, rejects a duplicate stable deployment, simulates the next stable release, and removes the temporary repository. `mike list --branch gh-pages` in a read-only clone shows deployed metadata.

To recover a Pages deployment without changing documentation, choose `redeploy-pages`; it uploads the existing complete `gh-pages` branch. If a `mike` push conflicts, let the serialized workflow fail, inspect the remote branch, and rerun the same operation. Never force-push or rebuild old versions from `main`.
