# Test Lens Brand Assets

This folder contains source PNG assets for Selenium Test Lens documentation and repository presentation.

## Files

| File | Intended use |
|---|---|
| `test-lens-logo-horizontal.png` | Primary horizontal logo for the README hero, landing pages, and large documentation headers. |
| `test-lens-wordmark.png` | Standalone wordmark for narrow spaces or text-heavy docs. |
| `test-lens-icon.png` | Standalone icon for the GitHub organization avatar, favicon, HUD badge, or report icon. |
| `test-lens-badge.png` | Compact badge for docs, social previews, and smaller landing sections. |

## Usage Notes

- Keep source PNGs in this folder.
- Do not use these files as runtime dependencies from Selenium Test Lens JavaScript or HUD code.
- HUD runtime branding should use CSS, SVG, or inline assets rather than documentation image files.
- Keep image paths relative and GitHub-renderable when referencing these assets from docs.
- Verify transparent backgrounds before public release.

## TODO

The current PNGs appear to include a checkerboard-style background in preview. Before public release, replace them with true transparent PNG or SVG assets if the checkerboard is baked into the image rather than real alpha transparency.
