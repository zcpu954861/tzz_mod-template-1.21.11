#!/usr/bin/env python3
import os
import struct
import zlib

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT_DIR = os.path.join(ROOT, "src", "main", "resources", "assets", "tzz_mod", "textures", "block")


def write_png(path, pixels):
    height = len(pixels)
    width = len(pixels[0])
    raw = bytearray()
    for row in pixels:
        raw.append(0)
        for r, g, b, a in row:
            raw.extend((r, g, b, a))

    def chunk(kind, data):
        body = kind + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)

    png = (
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
        + chunk(b"IDAT", zlib.compress(bytes(raw), 9))
        + chunk(b"IEND", b"")
    )
    with open(path, "wb") as file:
        file.write(png)


def canvas(color):
    return [[color for _ in range(16)] for _ in range(16)]


def rect(pixels, x0, y0, x1, y1, color):
    for y in range(y0, y1):
        for x in range(x0, x1):
            pixels[y][x] = color


def line(pixels, points, color):
    for x, y in points:
        if 0 <= x < 16 and 0 <= y < 16:
            pixels[y][x] = color


def base_top():
    p = canvas((38, 42, 48, 255))
    rect(p, 0, 0, 16, 1, (18, 21, 25, 255))
    rect(p, 0, 15, 16, 16, (18, 21, 25, 255))
    rect(p, 0, 0, 1, 16, (18, 21, 25, 255))
    rect(p, 15, 0, 16, 16, (18, 21, 25, 255))
    rect(p, 3, 3, 13, 13, (49, 55, 64, 255))
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        p[y][x] = (95, 102, 112, 255)
    line(p, [(4, 7), (5, 7), (6, 7), (9, 7), (10, 7), (11, 7)], (40, 202, 216, 255))
    return p


def base_side():
    p = canvas((32, 36, 42, 255))
    rect(p, 0, 0, 16, 2, (62, 68, 78, 255))
    rect(p, 0, 14, 16, 16, (18, 21, 25, 255))
    rect(p, 1, 4, 15, 5, (22, 25, 29, 255))
    line(p, [(2, 9), (3, 9), (4, 9), (10, 9), (11, 9), (12, 9), (13, 9)], (32, 150, 168, 255))
    return p


def core_top():
    p = canvas((45, 50, 58, 255))
    rect(p, 1, 1, 15, 15, (57, 64, 74, 255))
    rect(p, 4, 4, 12, 12, (30, 34, 40, 255))
    line(p, [(7, 3), (8, 3), (7, 12), (8, 12), (3, 7), (3, 8), (12, 7), (12, 8)], (40, 202, 216, 255))
    return p


def core_side():
    p = canvas((42, 46, 54, 255))
    rect(p, 0, 0, 16, 1, (74, 82, 94, 255))
    rect(p, 0, 15, 16, 16, (20, 23, 28, 255))
    rect(p, 2, 3, 14, 12, (52, 58, 68, 255))
    line(p, [(3, 5), (4, 5), (5, 5), (5, 6), (10, 8), (11, 8), (12, 8), (12, 9)], (33, 164, 186, 255))
    return p


def panel(on):
    bg = (20, 46, 54, 255) if not on else (23, 92, 104, 255)
    glow = (40, 202, 216, 255) if not on else (95, 248, 255, 255)
    p = canvas(bg)
    rect(p, 0, 0, 16, 1, (9, 18, 22, 255))
    rect(p, 0, 15, 16, 16, (9, 18, 22, 255))
    rect(p, 0, 0, 1, 16, (9, 18, 22, 255))
    rect(p, 15, 0, 16, 16, (9, 18, 22, 255))
    rect(p, 3, 3, 13, 13, (14, 70, 82, 255) if not on else (22, 128, 144, 255))
    line(p, [(4, 5), (5, 5), (6, 5), (6, 6), (9, 10), (10, 10), (11, 10), (11, 11)], glow)
    line(p, [(4, 10), (5, 10), (10, 5), (11, 5)], glow)
    return p


def antenna():
    p = canvas((35, 39, 46, 255))
    rect(p, 2, 2, 14, 14, (52, 58, 66, 255))
    rect(p, 5, 5, 11, 11, (20, 24, 30, 255))
    line(p, [(7, 1), (8, 1), (7, 14), (8, 14), (1, 7), (1, 8), (14, 7), (14, 8)], (60, 224, 236, 255))
    return p


TEXTURES = {
    "signal_emitter_base_top.png": base_top(),
    "signal_emitter_base_side.png": base_side(),
    "signal_emitter_core_top.png": core_top(),
    "signal_emitter_core_side.png": core_side(),
    "signal_emitter_panel_off.png": panel(False),
    "signal_emitter_panel_on.png": panel(True),
    "signal_emitter_antenna.png": antenna(),
}


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    for name, pixels in TEXTURES.items():
        write_png(os.path.join(OUT_DIR, name), pixels)


if __name__ == "__main__":
    main()
