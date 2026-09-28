# Releasing Test Lens 0.4.x

This is the maintainer runbook for the 0.4.x line. Every step that changes Git, Maven Central, GitHub, or published documentation requires a separate human authorization. None of the validation scripts commits, tags, pushes, publishes, or deploys documentation.

## Release boundary

The 0.4.0 feature scope is frozen. The 0.4.x line accepts correctness and security fixes, Selenium/browser compatibility fixes, performance-regression fixes, documentation fixes, and release-tooling fixes. It does not accept new execution or scheduling contracts.

Adaptive Parallelism / CPU-aware worker sizing is **DEFERRED — FUTURE_FEATURE_RELEASE** and is excluded from 0.4.x. A future design must use an early TestNG configurator rather than the current `@Listeners` boundary, distinguish an explicit count from TestNG's default of five, account for DataProvider and auxiliary pools and PER_CLASS safety, prefer JUnit's native dynamic strategy, and start with integer/percentage plus a maximum cap rather than `auto`.

## A. Snapshot hardening

1. Confirm the intended branch, clean worktree, and exact HEAD.
2. Run both reactor checks:

   ```powershell
   ./scripts/check-reactor-versions.ps1 -ExpectedVersion 0.4.0-SNAPSHOT
   ./scripts/check-reactor-versions.ps1 -ExpectedVersion 0.4.0-SNAPSHOT -IncludeBrowserIt
   ```

3. Run the publication policy check with `./scripts/validate-release-packaging.ps1 -MatrixOnly`.
4. Complete the bounded unit, reactor, browser, consumer, documentation, and performance-regression gates. Record every skip and NOT_RUN item.

## B. Release preparation

Create the release branch according to the repository's current maintainer flow. Prepare the exact version only after snapshot hardening passes. Version changes must be limited to tracked reactor POMs and release-owned documentation; generated and fixture POMs are not version sources.

For a patch release, `prepare-patch-release.ps1` performs only bounded local file preparation. For 0.4.0, make the equivalent reviewed version and documentation changes explicitly. Review the complete diff before committing.

## C. Release-candidate evidence

Retain this evidence with the release record:

- release commit SHA and exact version;
- normal and `browser-it` reactor inventories;
- public API counts (283 types and 2183 callables) and the `v0.3.1`/`@since 0.4.0` result;
- full Maven reactor result;
- browser matrix with browser, browser-driver, Java, OS, headed/headless, and DEFAULT/FAST versions or modes;
- Maven and Gradle clean-room consumer results on Java 17 and 21;
- the 8-published/9-nonpublished packaging matrix;
- Central profile verification, source JARs, Javadoc JARs, licenses, and signatures;
- strict docs, links, signatures, version boundary, Javadoc language, and Mike simulation;
- dependency versions and all NOT_RUN, skipped, platform, provider, and scale limitations.

Run the release-form checks before deployment:

```powershell
./scripts/check-reactor-versions.ps1 -ExpectedVersion 0.4.0
./scripts/check-reactor-versions.ps1 -ExpectedVersion 0.4.0 -IncludeBrowserIt
mvn test -Dheaded=false
mvn -Pcentral-release "-Dgpg.skip=true" "-DskipTests" verify
./scripts/validate-release-packaging.ps1 -Version 0.4.0
./scripts/validate-clean-room-consumer.ps1
./scripts/check-public-api.ps1
./scripts/check-public-api-since.ps1 -BaselineTag v0.3.1 -ExpectedSince 0.4.0
```

Also run all documentation gates described in [Documentation versioning](documentation-versioning.md). These commands validate; they do not authorize publication.

## D. Central, tag, GitHub, and documentation ordering

1. Validate the exact release commit and preserve its SHA.
2. With explicit publication authorization, deploy its signed artifacts using the `central-release` profile. The Central plugin waits for validation and has `autoPublish=false`.
3. Inspect the validated Central deployment and publish it manually.
4. After artifact identity and availability are confirmed, create the immutable `v0.4.0` tag from that exact commit and push it with explicit authorization.
5. Create or retry the GitHub Release from the immutable tag.
6. The tag-driven documentation workflow publishes the immutable Mike version and updates `latest`; verify both the versioned URL and redirects.
7. Verify Central coordinates, source/Javadoc artifacts, GitHub Release, docs, and clean-room consumers externally.
8. Prepare and separately commit the post-release `0.4.1-SNAPSHOT` bump.

The docs workflow accepts a release tag; it does not require the tag to precede Central staging. Keeping the tag after successful manual Central publication avoids publishing an immutable release marker for artifacts that were never made available.

## Failure and resume rules

- If Central staging fails before the tag, fix and retry or discard that failed staging deployment. Do not create a release tag.
- If a tag exists and publication later fails, never move or rewrite it. Resume only when identical artifact identity can be preserved; otherwise document the incident and issue a patch release.
- If GitHub Release creation fails, retry against the existing immutable tag.
- If docs publishing fails, rerun the serialized immutable docs workflow. Follow its explicit repair procedure; do not force-push or delete version history.
- Never recover a release through history rewriting, tag movement, blanket repository cleanup, or an unreviewed republish.

## Publication policy

The parent POM and seven runtime/library JAR modules are published. Examples, browser tests, selector modules, compatibility modules, and migration tooling are nonpublished. `validate-release-packaging.ps1` derives reactor membership from tracked POMs, requires every module to be classified, verifies effective `maven.deploy.skip`, verifies the Central exclusion set, and stages only publishable artifacts.

## External state changes

The runbook deliberately separates validation from authorization. Committing, merging, pushing, Central deployment/publication, tag creation, GitHub Release creation, Mike deployment, and the post-release bump are individually reviewed maintainer actions.
