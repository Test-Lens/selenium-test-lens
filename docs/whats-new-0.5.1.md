---
title: What's new in 0.5.1
description: Test Engineering Studio workflow, Page Object verification, recovery, reviewer evidence, and controlled selector-healing fixes in Test Lens 0.5.1.
---

# What's new in 0.5.1

!!! warning "Experimental feature — temporarily unavailable"

    Test Engineering Studio is undergoing further stabilization and is not currently recommended for production use. Public documentation will be restored after end-to-end certification.

Studio remains experimental and has been temporarily withdrawn from publicly recommended features. The changes below describe the shipped patch; they do not establish production readiness.

Test Lens 0.5.1 is a patch release focused on making the opt-in Test Engineering Studio workflow
deterministic, reviewable, and recoverable. It preserves the existing runtime contracts and Maven coordinates from
0.5.0; it does not add background agents, autonomous source editing, TestNG execution to Studio, or a broader
self-healing loop.

## Studio workflow stabilization

The reviewed workflow now keeps the Architect, Implementer, and Reviewer boundaries explicit:

- the Architect receives a bounded application and Page Object context;
- the Implementer is checked against the canonical, indexed Page Object API before compilation;
- the Reviewer receives a structured Reviewer Evidence Pack rather than relying on a single pass/fail label;
- agent protocol failures retain bounded, value-free diagnostics without exposing raw provider output;
- restored runs expose only backend-approved next actions.

The evidence pack distinguishes compilation status, required API assertions, swallowed-failure checks, control-path
status, and quality findings. A generated test that catches an assertion failure and completes normally cannot become
a false green result.

## Page Object source verification

Page Object capability manifests now carry readiness information, and references are resolved through a canonical API
model. Source API Verification checks the referenced class and method against the indexed source before the generated
test reaches compilation. Missing, ambiguous, stale, or incompatible capabilities are reported as capability or
reference failures; Studio does not invent a locator or bypass the Page Object with direct `findElement` calls.

## Recovery without invented success

Persisted Studio sessions retain reviewed artifacts and source fingerprints. After a host interruption, work that was
running or verifying is reported as interrupted, not passed. A recovered `RUN` can proceed only through an action that
the backend exposes for the restored state, and stale source evidence must be refreshed before execution or apply.

## Controlled selector self-healing

Selector repair remains proposal-first. Studio can propose a repair only after failure classification identifies
selector instability and Selector Intelligence supplies live, same-target, unique replacement evidence. Applying it
still requires explicit approval and fresh checks of the allowed path, source and declaration fingerprints, old
selector, and replacement evidence. A viewed proposal, an agent recommendation, or a passing rerun is not approval.

## Security hardening

Agent and generated-source boundaries now use structured, value-free redaction findings. Credential-shaped output,
unsafe artifact references, mutable provenance payloads, and oversized or invalid diagnostics fail closed. Fingerprints
bind reviewed artifacts to the source that produced them, and the trusted apply boundary rechecks those fingerprints
immediately before mutation.

The new classified `USER_API` types are:

- `RedactionPolicy.SensitiveContentRule`;
- `RedactionPolicy.SensitiveContentFinding`;
- `RedactionPolicy.StructuredRedactionFinding`.

`RedactionPolicy` also exposes the corresponding generated-source and structured-agent-output inspection methods.
The new Page Object, reviewer-evidence, validation, recovery, and Studio runtime-contract types are documented in the
generated API catalog as internal tooling surfaces, not as a new stable runtime integration API.

## Compatibility and scope

- Existing `selenium-test-lens`, JUnit 5, TestNG, React, and Allure runtime contracts remain supported.
- Full generated-test execution in Test Engineering Studio uses JUnit 5.
- The published TestNG runtime adapter remains supported for ordinary Test Lens lifecycle integration, but TestNG is
  outside the current end-to-end Studio workflow.
- Studio remains opt-in local tooling. Updating the runtime dependency alone does not start indexing, a browser, an
  agent process, or the loopback Studio host.

See the [exact public API catalog](reference/public-api-catalog.md).
