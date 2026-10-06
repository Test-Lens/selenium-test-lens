---
name: lens-review
description: Review Selenium Test Lens diffs for correctness, lifecycle, compatibility and missing regression tests. Use for independent code review.
---

1. Read the task, applicable instructions and integrated diff against the specified base. Follow affected callers and relevant tests.
2. Inspect behavioral contracts, defaults, optional dependencies and declared version compatibility.
3. Check touched lifecycle paths for cross-test contamination, races, leaked listeners/resources, failure cleanup and retry handling. Inspect browser feature fallbacks and disabled behavior where relevant.
4. Check secret handling and authentication scope where credentials are involved. Verify tests prove requirements rather than merely executing methods.
5. Return only supported findings with severity, path/symbol, concrete trigger, impact and suggested verification. Separate unverified concerns from confirmed defects. Do not demand unrelated redesign or cosmetic rewrites.
6. Remain read-only. Report no actionable findings when supported, together with meaningful validation gaps.
