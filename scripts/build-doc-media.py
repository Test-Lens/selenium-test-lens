"""Build loop-friendly animated WebP files and contact sheets from captured PNG frames."""

from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
FRAME_ROOT = ROOT / "selenium-test-lens-browser-tests" / "target" / "docs-media-frames"
MEDIA_ROOT = ROOT / "docs" / "assets" / "media"
CONTACT_ROOT = ROOT / "target" / "docs-media-contact-sheets"
SCENARIOS = (
    "hud-action-assertion-lifecycle",
    "hud-highlight-lifecycle",
    "hud-default-vs-fast",
    "native-selenium-observation",
    "smart-click-fallback",
)


def build_scenario(directory: Path) -> None:
    frame_paths = sorted(directory.glob("*.png"))
    if not frame_paths:
        raise RuntimeError(f"No frames found in {directory}")
    frames = [Image.open(path).convert("RGB") for path in frame_paths]
    dimensions = {frame.size for frame in frames}
    if len(dimensions) != 1:
        raise RuntimeError(f"Mixed frame dimensions in {directory}: {dimensions}")
    width, height = frames[0].size

    MEDIA_ROOT.mkdir(parents=True, exist_ok=True)
    destination = MEDIA_ROOT / f"{directory.name}.webp"
    frames[0].save(destination, save_all=True, append_images=frames[1:], duration=400,
                   loop=0, format="WEBP", quality=82, method=6)

    sample_indexes = sorted({0, len(frames) // 4, len(frames) // 2, 3 * len(frames) // 4, len(frames) - 1})
    sample_width = 384
    sample_height = round(frames[0].height * sample_width / frames[0].width)
    sheet = Image.new("RGB", (sample_width * len(sample_indexes), sample_height + 28), "white")
    draw = ImageDraw.Draw(sheet)
    for column, index in enumerate(sample_indexes):
        sample = frames[index].resize((sample_width, sample_height), Image.Resampling.LANCZOS)
        sheet.paste(sample, (column * sample_width, 28))
        draw.text((column * sample_width + 6, 7), f"frame {index:02d}", fill="black")
    CONTACT_ROOT.mkdir(parents=True, exist_ok=True)
    sheet.save(CONTACT_ROOT / f"{directory.name}.png", optimize=True)

    for frame in frames:
        frame.close()
    print(f"{destination.relative_to(ROOT)}: {len(frame_paths)} frames, {width}x{height}, {len(frame_paths) * 0.4:.1f}s")


def main() -> None:
    found = {path.name for path in FRAME_ROOT.iterdir() if path.is_dir()}
    expected = set(SCENARIOS)
    if found != expected:
        raise RuntimeError(f"Expected exactly {sorted(expected)} under {FRAME_ROOT}; found {sorted(found)}")
    for name in SCENARIOS:
        build_scenario(FRAME_ROOT / name)


if __name__ == "__main__":
    main()
