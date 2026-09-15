"""Copies the canonical browser-side visual renderer into the static HUD demo."""

from pathlib import Path
import shutil


ASSET_MANIFEST = Path(__file__).with_name("hud-demo-runtime-assets.txt")


def runtime_files():
    files = tuple(
        line.strip()
        for line in ASSET_MANIFEST.read_text(encoding="utf-8").splitlines()
        if line.strip() and not line.lstrip().startswith("#")
    )
    if not files:
        raise RuntimeError("HUD demo runtime asset manifest is empty")
    if len(files) != len(set(files)):
        raise RuntimeError("HUD demo runtime asset manifest contains duplicates")
    for name in files:
        path = Path(name)
        if path.is_absolute() or ".." in path.parts:
            raise RuntimeError(f"Invalid HUD demo runtime asset path: {name}")
    return files


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
        for name in runtime_files():
            target = destination / name
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(source / name, target)
