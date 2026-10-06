---
name: lens-feature
description: Implement or change Selenium Test Lens features and public APIs. Use for production-code changes; exclude documentation-only edits.
---

1. Read applicable instructions; locate module ownership, public entry points, similar implementations, build constraints and tests.
2. Define observable behavior, public signatures, defaults, error handling and compatibility before editing. Reuse established abstractions; do not add speculative frameworks or dependencies.
3. Check applicable lifecycle risks: attach/start/finish, retries, disabled mode, session isolation, thread safety, listener cleanup and browser capabilities. Verify actual semantics in code rather than assuming OFF means every API is disabled.
4. Keep integrations optional where the architecture requires it. Respect each module's declared Java and Selenium baseline.
5. Implement the assigned scope, add meaningful coverage with the test engineer, run required checks and request independent review of the integrated diff.
6. Report behavior, changed paths, commands/results and limitations. Never claim a published artifact or runtime capability from source existence alone.
