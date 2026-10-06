---
name: lens-auth-bridge
description: Design, implement or test Lens browser authentication transfer to REST Assured or another HTTP client, including Playwright storageState import. Exclude unrelated authentication features.
---

1. Inspect existing Lens state export/import, Selenium cookies/storage access and the intended HTTP-client integration before adding adapters. Do not assume an existing Playwright-compatible format.
2. Define an explicit target URI and authentication strategy: cookie session, configured bearer-token extraction or configured CSRF handling. Do not heuristically forward every storage key or identity-provider cookie.
3. Match cookies by host/domain, path, secure transport and expiry; preserve same-name cookies on different paths. Parse host-only/domain-cookie metadata correctly; identify ambiguity in source formats and define a tested policy. Reject unrelated scopes and expired state.
4. Extract bearer tokens only through configured origin/key/JSON-path rules. Treat storageState as a snapshot: localStorage/IndexedDB/sessionStorage are not HTTP headers and refresh is not automatic. Unsupported storage types must be explicit errors or documented limitations.
5. Apply credentials only to allowed request destinations; handle cross-origin redirects without leaking secrets. Keep snapshots out of logs/reports/prompts/Git; redact HTTP logging. Keep runtime state per test/user/environment, with clear expiry and refresh ownership.
6. Keep the HTTP adapter optional if required by module architecture. Verify APIs against dependency versions. Test domain/path/secure/expiry filtering, duplicate names, CSRF, token extraction, secret redaction, isolation and redirect behavior where applicable; prove authenticated access with a controlled integration endpoint.
7. Distinguish session reuse from performing a new login. Do not claim support for device-bound tokens, passkeys or SSO flows unless tested.
