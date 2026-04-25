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


RED_DARK = (124, 30, 38, 255)
RED_MID = (190, 43, 55, 255)
RED_BRIGHT = (255, 82, 96, 255)
RED_GLOW = (255, 42, 64, 255)


def base_top():
    p = canvas((38, 40, 45, 255))
    rect(p, 0, 0, 16, 1, (18, 20, 24, 255))
    rect(p, 0, 15, 16, 16, (18, 20, 24, 255))
    rect(p, 0, 0, 1, 16, (18, 20, 24, 255))
    rect(p, 15, 0, 16, 16, (18, 20, 24, 255))
    rect(p, 3, 3, 13, 13, (52, 54, 62, 255))
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        p[y][x] = (96, 100, 110, 255)
    line(p, [(4, 7), (5, 7), (6, 7), (9, 7), (10, 7), (11, 7)], RED_MID)
    return p


def base_side():
    p = canvas((32, 34, 40, 255))
    rect(p, 0, 0, 16, 2, (62, 66, 76, 255))
    rect(p, 0, 14, 16, 16, (18, 20, 24, 255))
    rect(p, 1, 4, 15, 5, (22, 24, 28, 255))
    line(p, [(2, 9), (3, 9), (4, 9), (10, 9), (11, 9), (12, 9), (13, 9)], RED_DARK)
    return p


def core_top():
    p = canvas((46, 48, 56, 255))
    rect(p, 1, 1, 15, 15, (58, 62, 72, 255))
    rect(p, 4, 4, 12, 12, (31, 33, 39, 255))
    line(p, [(7, 3), (8, 3), (7, 12), (8, 12), (3, 7), (3, 8), (12, 7), (12, 8)], RED_MID)
    return p


def core_side():
    p = canvas((42, 44, 52, 255))
    rect(p, 0, 0, 16, 1, (74, 80, 92, 255))
    rect(p, 0, 15, 16, 16, (20, 22, 27, 255))
    rect(p, 2, 3, 14, 12, (53, 56, 66, 255))
    line(p, [(3, 5), (4, 5), (5, 5), (5, 6), (10, 8), (11, 8), (12, 8), (12, 9)], RED_DARK)
    return p


def panel(on):
    bg = (56, 18, 24, 255) if not on else (100, 18, 27, 255)
    glow = RED_GLOW if not on else RED_BRIGHT
    p = canvas(bg)
    rect(p, 0, 0, 16, 1, (22, 8, 10, 255))
    rect(p, 0, 15, 16, 16, (22, 8, 10, 255))
    rect(p, 0, 0, 1, 16, (22, 8, 10, 255))
    rect(p, 15, 0, 16, 16, (22, 8, 10, 255))
    rect(p, 3, 3, 13, 13, (84, 22, 32, 255) if not on else (150, 28, 42, 255))
    line(p, [(4, 5), (5, 5), (6, 5), (6, 6), (9, 10), (10, 10), (11, 10), (11, 11)], glow)
    line(p, [(4, 10), (5, 10), (10, 5), (11, 5)], glow)
    return p


def antenna():
    p = canvas((35, 38, 45, 255))
    rect(p, 2, 2, 14, 14, (54, 57, 65, 255))
    rect(p, 5, 5, 11, 11, (20, 23, 29, 255))
    line(p, [(7, 1), (8, 1), (7, 14), (8, 14), (1, 7), (1, 8), (14, 7), (14, 8)], RED_BRIGHT)
    return p


TEXTURES = {
    "signal_receiver_base_top.png": base_top(),
    "signal_receiver_base_side.png": base_side(),
    "signal_receiver_core_top.png": core_top(),
    "signal_receiver_core_side.png": core_side(),
    "signal_receiver_panel_off.png": panel(False),
    "signal_receiver_panel_on.png": panel(True),
    "signal_receiver_antenna.png": antenna(),
    "signal_receiver.png": panel(False),
    "signal_receiver_powered.png": panel(True),
}


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    for name, pixels in TEXTURES.items():
        write_png(os.path.join(OUT_DIR, name), pixels)


if __name__ == "__main__":
    main()
