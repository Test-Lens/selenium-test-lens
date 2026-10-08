# Search Console recovery for versioned documentation

The project documentation uses one crawlable gateway and one moving canonical release tree:

- `/selenium-test-lens/` is a real HTML gateway with a self-canonical URL. It does not perform a client-side redirect.
- `/selenium-test-lens/latest/` contains the current recommended release and is self-canonical page by page.
- numbered releases remain available, while their canonical URLs point to the equivalent page under `latest`;
- `/selenium-test-lens/dev/` is `noindex,follow`, canonicalizes to `latest`, and is excluded from the production sitemap;
- `/selenium-test-lens/sitemap.xml` is a sitemap index containing only `/latest/sitemap.xml`.

The root gateway links directly to `latest`; it is not a substitute for the complete current documentation.
Mike's `latest` alias uses `copy`, so the current documentation is served directly rather than through another
redirect page. Compatibility redirects are allowed only when registered, `noindex`, one hop, and targeted at an
existing page in the same documentation version.

Run the complete local contract before publishing:

```powershell
./scripts/validate-versioned-docs.ps1
```

The simulation produces the same root, `latest`, numbered, historical, and `dev` filesystem shape uploaded to
GitHub Pages. `check-doc-seo.ps1` then starts a local HTTP server and verifies status codes, root content,
canonicals, robots metadata, sitemap URLs, and the redirect graph.

## After deployment

Search Console recovery is asynchronous. A successful deploy does not imply immediate recrawling or removal of
an existing report. After the complete Pages artifact has been deployed:

1. Fetch `/selenium-test-lens/` and confirm HTTP 200 with real content, a self-canonical, and no meta/JavaScript redirect.
2. Fetch `/selenium-test-lens/latest/` and a representative current page; confirm HTTP 200 and self-canonicals.
3. Fetch `/selenium-test-lens/sitemap.xml` and `/selenium-test-lens/latest/sitemap.xml`; confirm every listed page returns 200 directly.
4. Confirm `/selenium-test-lens/0.5.0/` and `/selenium-test-lens/0.4.0/` remain available.
5. Submit or refresh `https://test-lens.github.io/selenium-test-lens/sitemap.xml` in Search Console.
6. Use URL Inspection on the root gateway, `/latest/`, Getting Started, and Test Engineering Studio pages.
7. Request indexing for the current overview and key current pages.
8. Start validation of the reported redirect issue and monitor indexing/coverage over subsequent crawls.

Do not request indexing for `dev`, compatibility redirect pages, or numbered duplicates whose declared canonical
is `latest`.
