---
title: Generate your first Selenium test with AI
description: Follow the local Test Engineering Studio workflow from a Maven Selenium requirement to a reviewed JUnit 5 test and Selenium evidence.
---

# Generate your first Selenium test with AI

This tutorial uses the local, opt-in **Test Engineering Studio**. It does not describe the separate AI Integration Prompt Builder, which creates prompt text for an external coding agent.

Example requirement:

> Verify that logging in with an invalid password displays the expected error and does not authenticate the user.

## Before you start

- A Maven Selenium project that reaches `test-compile`, running on JDK 17 or newer with `javac`.
- The application available at a non-secret local or test URL. Prepare authentication through the application's normal test setup; do not put credentials in Studio configuration.
- To run the browser workflow, an available Chrome or Firefox setup; Chrome is the currently verified local end-to-end path.
- To use the real AI stages, [install and sign in to Codex CLI](https://developers.openai.com/codex/cli) using its supported setup and ensure the required `codex exec` capabilities are available, or configure a project-provided `AgentExecutorProvider`. Starting Studio itself does not require an AI call.

## 1. Launch Studio

From the root of the Maven Selenium project, run the published 0.5.1 plugin goal:

```powershell
mvn io.github.test-lens:test-lens-test-engineering-maven-plugin:0.5.1:studio
```

Maven compiles the project's tests, starts the loopback-only Studio host and opens the local interface. Stop the Maven process to stop Studio. For project setup, Codex discovery and provider configuration, see [Studio Quick Start](test-engineering-studio-getting-started.md).

## 2. Prepare the project context

1. Select **Scan project** to index the existing source, tests and Page Objects.
2. Start the application and its normal test-session/authentication bootstrap.
3. Select **Map application** and map the current page or use guided mapping.
4. Select **Correlate Page Objects**. Review any partial, ambiguous or conflicting results before proceeding.

These steps are deliberate user actions. Studio does not silently scan source, open a browser, or contact the application merely because the runtime dependency is present.

## 3. Generate and run the test

1. Enter the requirement above in the **Requirement** field.
2. Select **Generate plan** to ask the AI Architect for a structured test plan.
3. Review the plan, then select **Generate test** to ask the AI Implementer for a JUnit 5 proposal.
4. Review the proposed source and deterministic policy results. Studio applies its security, Page Object API and AST checks; generated code is not trusted merely because it came from an agent.
5. Select **Run** only after reviewing the proposal. Studio performs targeted compilation and Selenium execution.
6. Inspect the run evidence and the separate AI Reviewer decision. A Selenium PASS alone is not an approval, and a reviewer decision does not replace the execution evidence.

The generated-test workflow in this release targets JUnit 5. Test Lens also publishes a TestNG runtime adapter, but full TestNG test generation and targeted execution are not part of Studio 0.5.1.

## If the run fails

Use **Diagnose** to inspect the failure classification and available evidence. A product behavior failure is not automatically a selector problem. If Studio prepares a selector repair, inspect its candidate, same-target/stability evidence and diff. The source remains unchanged until you explicitly choose **Approve and apply**; reject proposals that are ambiguous, stale, or change the test's expected behavior. After an approved repair, use **Run verification** and review the new execution evidence.

[Read the detailed Studio workflow](test-engineering-studio.md) · [Review the Selector Self-Healing boundary](test-engineering-studio.md#review-a-repair)
