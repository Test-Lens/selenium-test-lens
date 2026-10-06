# Selenium Test Lens — Codex workflow

Treat repository code, build files and existing documentation as authoritative. Discover modules, Java/Selenium versions and commands before proposing changes. Do not assume historical versions or undocumented APIs.

## Coordination
The main agent is the orchestrator and owns public contracts, integration and final verification. For small changes work directly. For separable multi-module changes use named agents below; allow at most three active children. Delegate only when supported by the current client. Otherwise perform the same roles sequentially and disclose that fallback.

Use lens_explorer to map unknown code, lens_implementer for production changes, lens_test_engineer for behavior tests, lens_reviewer for an independent read-only review and lens_docs_engineer for examples/documentation. Children must not spawn more agents.

Before delegation provide: goal, relevant paths/symbols, proposed signatures and behavior, exclusive writable paths, acceptance criteria, relevant instructions/skills, validation commands and dependencies. Pass concise task-local context rather than a full transcript when supported. Children must read applicable AGENTS.md files themselves. Do not weaken user instructions to save tokens.

Assign one writer per file. Freeze shared contracts before concurrent implementation and tests. Tests may be designed concurrently but must validate the integrated code. A contract change pauses dependent work until all workers receive it. Children never stage/commit/push. Preserve unrelated changes; never reset or stash them automatically.

## Verification
Use $lens-feature for implementation, $lens-testing for tests, $lens-review for review, $lens-docs for documentation and $lens-auth-bridge for browser-to-HTTP authentication work. Load only applicable skills.

Discover validation commands from build files and CI; run focused checks first, then required project checks. Distinguish baseline failures, introduced failures and checks not run. Do not mask failures by weakening assertions, deleting tests or adding retries/sleeps. Review the integrated diff after meaningful implementation changes, fix concrete findings, and rerun affected checks. No release, deployment or publishing without task authorization.

## Reporting and context
Return compact handoffs: changed paths, contract changes, findings, exact commands/results, remaining risks. Avoid broad repository dumps and repeated full scans. Send the user concise updates and explain outcomes in Polish unless requested otherwise. Keep source/documentation language consistent with the project. Never put cookies, tokens or authentication state in logs, reports, prompts or committed fixtures.
