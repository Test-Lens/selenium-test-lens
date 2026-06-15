# Release verification

Selenium Test Lens is not published automatically yet. The current repository setup supports CI builds and a manual release verification workflow, but Central Portal credentials and GPG signing keys are not stored in the repository.

## CI build

The standard GitHub Actions CI workflow runs on pull requests and pushes to `main`:

```powershell
mvn -q test
mvn -U -Prelease-artifacts -DskipTests package
```

This verifies tests and builds source/Javadoc artifacts for publishable modules.

## Manual release check

The `Release Check` workflow runs manually through `workflow_dispatch`.

It runs:

```powershell
mvn -q test
mvn -U -Pcentral-release "-Dgpg.skip=true" -DskipTests verify
```

`-Dgpg.skip=true` keeps the workflow non-publishing and credential-free. It verifies the release profile wiring as far as possible without a configured signing key.

## Future Central Portal release

Before publishing a real release:

1. Choose a non-SNAPSHOT release version.
2. Configure GPG signing locally or in CI.
3. Configure Central Portal credentials in Maven `settings.xml` or CI secrets for server id `central`, or override `central.publishing.serverId`.
4. Run `mvn -Pcentral-release -DskipTests verify` in the release environment without `-Dgpg.skip=true`.
5. Deploy only after reviewing the release contents.

Do not commit Central Portal tokens, GPG keys or passphrases.
