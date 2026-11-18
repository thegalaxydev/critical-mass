import os
import sys

import numpy as np
import soundfile as sf

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "criticalmass", "sounds")
RATE = 44100
STEADY_FROM = 6.2
STEADY_TO = 9.0
JOIN_SECONDS = 0.3
PEAK = 0.7


def looped(x):
    n = int(RATE * JOIN_SECONDS)
    body = x[:-n].copy()
    turn = np.linspace(0, np.pi / 2, n)
    body[:n] = body[:n] * np.sin(turn) + x[-n:] * np.cos(turn)
    return body


def octave_down(x):
    return np.interp(np.arange(2 * len(x)) / 2.0, np.arange(len(x)), x, period=len(x))


def main():
    paths = [a for a in sys.argv[1:] if a != "fresh"]
    if "fresh" not in sys.argv or len(paths) != 1:
        print("left alone; pass the siren recording and fresh to cut the siren sounds from it again")
        return
    recording, rate = sf.read(paths[0], always_2d=True)
    assert rate == RATE, "the recording should be at %d Hz, not %d" % (RATE, rate)
    steady = recording.mean(axis=1)[int(RATE * STEADY_FROM):int(RATE * STEADY_TO)]
    high = looped(steady)
    high = PEAK * high / np.max(np.abs(high))
    for name, signal in (("siren", high), ("siren_low", octave_down(high))):
        sf.write(os.path.join(OUT, name + ".ogg"), signal.astype(np.float32), RATE, format="OGG", subtype="VORBIS")
        print(name, round(len(signal) / RATE, 2), "seconds")


if __name__ == "__main__":
    main()
