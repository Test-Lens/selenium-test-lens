# Release verification

Selenium Test Lens uses the Central Publisher Portal through `org.sonatype.central:central-publishing-maven-plugin`. Credentials and GPG signing keys are intentionally not stored in the repository. Automatic publication is disabled for the first release: the deployment must validate successfully and then be reviewed and published manually.

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

`-Dgpg.skip=true` keeps this verification credential-free. For a local bundle dry run, invoke the Central lifecycle with `-DskipPublishing=true`; this creates the bundle without uploading it. The configured `excludeArtifacts` prevents `selenium-test-lens-examples` from entering the bundle.

## Central Portal release

Before publishing a real release:

1. Set the reviewed reactor version to `0.1.0` in a clean release commit.
2. Configure GPG signing locally or in CI.
3. Configure Central Portal credentials in Maven `settings.xml` or CI secrets for server id `central`, or override `central.publishing.serverId`.
4. Run tests and `mvn -Pcentral-release verify` without `gpg.skip`.
5. Inspect the generated parent POM and the POM, main JAR, sources JAR, Javadoc JAR and `.asc` files for core, overlay, the main `selenium-test-lens` runtime, and react.
6. Upload the validated bundle through the configured Central lifecycle.
7. Review the deployment in Central Portal and publish it manually; `autoPublish=false` prevents an unreviewed release.

Do not commit Central Portal tokens, GPG keys or passphrases.

