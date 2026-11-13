import os

import numpy as np
import soundfile as sf

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "criticalmass", "sounds", "alarm.ogg")
RATE = 44100
TONES = (740.0, 587.0, 740.0, 587.0)
TONE_SECONDS = 0.5
EDGE_SECONDS = 0.012
HARMONICS = ((1, 1.0), (2, 0.28), (3, 0.42), (4, 0.10), (5, 0.20), (7, 0.08))
GAIN = 0.55


def tone(frequency):
    t = np.arange(int(RATE * TONE_SECONDS)) / RATE
    wobble = 1.0 + 0.004 * np.sin(2 * np.pi * 6.0 * t)
    phase = 2 * np.pi * frequency * np.cumsum(wobble) / RATE
    wave = sum(level * np.sin(n * phase) for n, level in HARMONICS)
    wave = np.tanh(1.6 * wave / max(abs(wave)))
    edge = int(RATE * EDGE_SECONDS)
    envelope = np.ones(len(t))
    envelope[:edge] = np.linspace(0.0, 1.0, edge) ** 2
    envelope[-edge:] = np.linspace(1.0, 0.0, edge) ** 2
    body = 0.86 + 0.14 * np.exp(-t * 9.0)
    return wave * envelope * body


def smooth(signal, width):
    kernel = np.hanning(width)
    return np.convolve(signal, kernel / kernel.sum(), mode="same")


def main():
    signal = np.concatenate([tone(f) for f in TONES])
    signal = smooth(signal, 9)
    signal = GAIN * signal / max(abs(signal))
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    sf.write(OUT, signal.astype(np.float32), RATE, format="OGG", subtype="VORBIS")
    print("alarm.ogg", round(len(signal) / RATE, 2), "seconds")


if __name__ == "__main__":
    main()
