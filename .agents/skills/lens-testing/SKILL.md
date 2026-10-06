---
name: lens-testing
description: Create or repair behavior tests for Selenium Test Lens features and regressions. Use for unit, integration or browser test work.
---

1. Read requirements and public contract; inspect existing test frameworks, fixtures, build/CI commands and supported environments.
2. Build a small behavior matrix: success, meaningful failure, boundary cases and lifecycle. Include OFF behavior, concurrent sessions, retries or unsupported capabilities only when the change touches them.
3. Choose the lowest test level that proves the requirement. Use browser tests where DOM, browser storage, BiDi or browser lifecycle behavior is essential. Mock external boundaries, not the behavior under test.
4. For a regression show the test fails for the defect and passes with the fix when feasible; disclose when this comparison was not performed.
5. Avoid sleeps, automatic retries, private-method assertions and tests that merely mirror implementation. Preserve existing assertions and dependencies.
6. Run tests on integrated code, not only a stub contract. Report commands, counts/failures, environment assumptions and checks not run. Do not alter production files outside the assigned ownership.
