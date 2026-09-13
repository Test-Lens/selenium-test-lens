"""Copies the canonical browser-side visual renderer into the static HUD demo."""

from pathlib import Path
import shutil


RUNTIME_FILES = ("hud-panel.js", "highlight.js", "scroll-arrow.js")


def on_post_build(config, **kwargs):
    project_root = Path(__file__).resolve().parent.parent
    source = (
        project_root
        / "selenium-test-lens-overlay"
        / "src"
        / "main"
        / "resources"
        / "uitestlens"
        / "runtime"
    )
    for demo in ("hud", "hud-studio"):
        destination = Path(config["site_dir"]) / "demo" / demo / "runtime"
        if not (destination.parent / "index.html").is_file():
            continue
        destination.mkdir(parents=True, exist_ok=True)
        for name in RUNTIME_FILES:
            shutil.copyfile(source / name, destination / name)
