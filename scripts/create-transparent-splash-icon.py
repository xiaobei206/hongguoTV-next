#!/usr/bin/env python3
"""Create a same-canvas transparent splash icon from the original launcher WebP.

No resizing, cropping, drawing, sharpening, or logo reconstruction is performed.
Only near-black, neutral pixels connected to the image boundary are keyed out.
"""
from __future__ import annotations

from collections import deque
from pathlib import Path
import sys

from PIL import Image


def main() -> int:
    if len(sys.argv) != 3:
        print(
            "Usage: create-transparent-splash-icon.py INPUT.webp OUTPUT.png",
            file=sys.stderr,
        )
        return 2

    source = Path(sys.argv[1])
    output = Path(sys.argv[2])
    if not source.is_file():
        print(f"Source image not found: {source}", file=sys.stderr)
        return 2

    with Image.open(source) as opened:
        image = opened.convert("RGBA")

    width, height = image.size
    if width < 1 or height < 1:
        print("Source image has invalid dimensions.", file=sys.stderr)
        return 2

    pixels = image.load()
    visited = bytearray(width * height)
    queue: deque[tuple[int, int]] = deque()

    def is_background(x: int, y: int) -> bool:
        red, green, blue, _ = pixels[x, y]
        low = min(red, green, blue)
        high = max(red, green, blue)
        # Black canvas compression noise is nearly neutral. The chroma guard helps
        # retain dark-red/orange antialiasing around the original glossy logo.
        return high <= 24 and (high - low) <= 14

    def enqueue(x: int, y: int) -> None:
        index = y * width + x
        if not visited[index] and is_background(x, y):
            visited[index] = 1
            queue.append((x, y))

    # Seed every edge so all external background regions are discovered.
    for x in range(width):
        enqueue(x, 0)
        enqueue(x, height - 1)
    for y in range(height):
        enqueue(0, y)
        enqueue(width - 1, y)

    # Flood-fill only edge-connected background; enclosed black details of the
    # play symbol remain untouched.
    while queue:
        x, y = queue.popleft()
        if x > 0:
            enqueue(x - 1, y)
        if x + 1 < width:
            enqueue(x + 1, y)
        if y > 0:
            enqueue(x, y - 1)
        if y + 1 < height:
            enqueue(x, y + 1)

    changed = 0
    for y in range(height):
        for x in range(width):
            if visited[y * width + x]:
                red, green, blue, alpha = pixels[x, y]
                if alpha:
                    pixels[x, y] = (red, green, blue, 0)
                    changed += 1

    if changed == 0:
        print("No border-connected near-black background pixels were found.", file=sys.stderr)
        return 1
    if image.size != Image.open(source).size:
        print("Internal error: output canvas dimensions changed.", file=sys.stderr)
        return 1

    output.parent.mkdir(parents=True, exist_ok=True)
    image.save(output, format="PNG", optimize=True)

    # Validate that the output has alpha and retains the exact source canvas.
    with Image.open(output) as result:
        if result.size != (width, height) or result.mode != "RGBA":
            print("Generated splash icon failed size/alpha validation.", file=sys.stderr)
            return 1
        alpha_min, alpha_max = result.getchannel("A").getextrema()
        if alpha_min != 0 or alpha_max != 255:
            print("Generated splash icon does not contain transparent and opaque pixels.", file=sys.stderr)
            return 1

    print(
        f"Generated {output} ({width}x{height}); keyed out {changed} "
        "edge-connected background pixels; no crop or resize."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
