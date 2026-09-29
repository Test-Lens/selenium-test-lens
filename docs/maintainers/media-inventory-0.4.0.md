# 0.4.0 public media inventory

This inventory covers media tracked by the 0.4.0 documentation tree and references reachable from current public Markdown, HTML, CSS, and MkDocs configuration. Versioned 0.3.x documentation is immutable and was not rewritten.

## Current

| Asset | References | Subject | Classification |
| --- | --- | --- | --- |
| `assets/brand/test-lens-logo-horizontal.png` | README, landing | Test Lens wordmark | CURRENT; keep |
| `assets/brand/test-lens-icon.png` | MkDocs navigation | Test Lens icon | CURRENT; keep |
| `assets/brand/test-lens-badge.png` | brand guidance | Test Lens badge | CURRENT; keep |
| `assets/brand/test-lens-wordmark.png` | brand guidance | Test Lens wordmark | CURRENT; keep |
| `assets/images/favicon.png` | MkDocs theme | site favicon | CURRENT; keep |
| `assets/media/hud-action-assertion-lifecycle.webp` | README, What's New, HUD guide | correlated semantic HUD action/assertion lifecycle | CURRENT 0.4.0 |
| `assets/media/hud-highlight-lifecycle.webp` | HUD guide | ACTION/WAITING/RETRY/SUCCESS/FAILURE | CURRENT 0.4.0 |
| `assets/media/hud-default-vs-fast.webp` | configuration | DEFAULT live presentation versus FAST | CURRENT 0.4.0 |
| `assets/media/native-selenium-observation.webp` | Getting Started | observed ordinary Selenium operations | CURRENT 0.4.0 |
| `assets/media/smart-click-fallback.webp` | element actions | one operation with bounded fallback detail | CURRENT 0.4.0 |

## Stale and replaced

No 0.3.x screenshot, GIF, WebP, MP4, or WebM file was tracked in the current tree or in the inspected media history. There was therefore no binary asset to delete or retain. The stale visual was the code-native `docs/demo/hud/` presentation itself: it identified `0.3.0-demo`, emitted generic text-derived rows, and used one yellow outline. It was replaced in place by the 0.4.0 semantic, operation-correlated, state-aware scenarios while preserving its version-relative URL and runtime-asset copy hook.

| Previous subject | Reason stale | Replacement |
| --- | --- | --- |
| `docs/demo/hud/` tagged `0.3.0-demo` | wrong version and generic rows | current interactive 0.4.0 demo plus `hud-action-assertion-lifecycle.webp` |
| single yellow highlight in demo | did not show the state lifecycle | `hud-highlight-lifecycle.webp` |
| no DEFAULT/FAST visual | 0.4.0 behavior absent | `hud-default-vs-fast.webp` |
| no native-observation visual | 0.4.0 behavior absent | `native-selenium-observation.webp` |
| no Smart Click cascade visual | 0.4.0 behavior absent | `smart-click-fallback.webp` |

## Missing, duplicate, and remove decisions

- MISSING: none for the selected 0.4.0 public stories.
- DUPLICATE: no duplicate binary media was found.
- REMOVE: none. Brand assets remain referenced; historical media was not present in this branch.
- REPLACE_FOR_0_4_0: the stale code-native demo was updated; the five generated WebP files are the stable replacements/additions.

## External video references

The audited README/docs tree contains no embedded YouTube link and no referenced MP4 or WebM. Animated WebP matches the static-site hosting boundary without a video player, codec negotiation, or external service. The clips remain readable at documentation width and are substantially smaller than equivalent high-frame-rate GIFs.

## Static screenshot decision

No tracked HUD or HUD Studio screenshot existed to refresh. The landing page and HUD Studio both render the versioned runtime directly, so a static duplicate would become stale sooner and add no information. DEFAULT/FAST and semantic HUD behavior are clearer as short animations; TestNG `PER_CLASS` remains clearer as lifecycle text. A future static screenshot is warranted only for a materially changed Studio layout or a non-animated social/README preview, and should be produced from the same local fixture rather than cropped from an unrelated application.
