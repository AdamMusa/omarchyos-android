#!/usr/bin/env python3
"""Generate a deterministic Android boot animation from Omarchy's ASCII mark."""

from __future__ import annotations

import argparse
import math
import shutil
import tempfile
import zipfile
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

WIDTH = 1080
HEIGHT = 2400
FPS = 30
BACKGROUND = (7, 9, 15)
MINT = (105, 230, 197)
VIOLET = (164, 145, 255)


def font(size: int) -> ImageFont.FreeTypeFont:
    candidates = (
        "/System/Library/Fonts/Menlo.ttc",
        "/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf",
        "/usr/share/fonts/dejavu/DejaVuSansMono.ttf",
    )
    for candidate in candidates:
        if Path(candidate).exists():
            return ImageFont.truetype(candidate, size=size)
    raise RuntimeError("A monospaced TrueType font is required")


def render_frame(lines: list[str], reveal: float, phase: float) -> Image.Image:
    canvas = Image.new("RGB", (WIDTH, HEIGHT), BACKGROUND)
    draw = ImageDraw.Draw(canvas)
    logo_font = font(21)
    small_font = font(22)
    line_height = 31
    widest = max(draw.textlength(line, font=logo_font) for line in lines)
    origin_x = round((WIDTH - widest) / 2)
    origin_y = round((HEIGHT - len(lines) * line_height) / 2) - 90
    visible_columns = math.ceil(max(map(len, lines)) * reveal)

    for row, line in enumerate(lines):
        partial = line[:visible_columns]
        y = origin_y + row * line_height
        for column, character in enumerate(partial):
            if character == " ":
                continue
            x = origin_x + draw.textlength(line[:column], font=logo_font)
            sweep = max(0.0, 1.0 - abs((column / max(1, len(line))) - reveal) * 8.0)
            pulse = 0.08 * (math.sin(phase + column * 0.08) + 1.0)
            blend = min(1.0, 0.08 + sweep * 0.65 + pulse)
            color = tuple(round(MINT[index] * (1 - blend) + VIOLET[index] * blend)
                          for index in range(3))
            draw.text((x, y), character, fill=color, font=logo_font)

    status = "MOBILE SHELL  /  VERIFIED ANDROID"
    status_width = draw.textlength(status, font=small_font)
    status_y = origin_y + len(lines) * line_height + 72
    draw.text(((WIDTH - status_width) / 2, status_y), status,
              fill=(116, 130, 151), font=small_font)
    cursor_color = MINT if math.sin(phase * 2.0) >= 0 else BACKGROUND
    draw.rounded_rectangle(
        (WIDTH / 2 - 28, status_y + 66, WIDTH / 2 + 28, status_y + 72),
        radius=3,
        fill=cursor_color,
    )
    return canvas


def generate(output: Path, logo: Path) -> None:
    lines = logo.read_text(encoding="utf-8").splitlines()
    with tempfile.TemporaryDirectory(prefix="omarchy-boot-") as temporary:
        root = Path(temporary)
        part0 = root / "part0"
        part1 = root / "part1"
        part0.mkdir()
        part1.mkdir()
        (root / "desc.txt").write_text(
            f"{WIDTH} {HEIGHT} {FPS}\np 1 0 part0\np 0 0 part1\n", encoding="ascii")

        for index in range(28):
            progress = index / 27
            eased = 1.0 - (1.0 - progress) ** 3
            render_frame(lines, eased, index * 0.24).save(
                part0 / f"{index:05d}.png", optimize=True)
        for index in range(24):
            render_frame(lines, 1.0, index * math.tau / 24).save(
                part1 / f"{index:05d}.png", optimize=True)

        output.parent.mkdir(parents=True, exist_ok=True)
        with zipfile.ZipFile(output, "w", compression=zipfile.ZIP_STORED) as archive:
            archive.write(root / "desc.txt", "desc.txt")
            for directory in (part0, part1):
                for frame in sorted(directory.glob("*.png")):
                    archive.write(frame, f"{directory.name}/{frame.name}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--logo", type=Path, default=Path(__file__).with_name("logo.txt"))
    args = parser.parse_args()
    generate(args.output, args.logo)


if __name__ == "__main__":
    main()
