import os
import sys

import numpy as np
import soundfile as sf

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "criticalmass", "sounds", "alarm.ogg")
RATE = 44100
BLAST_SECONDS = 0.19
GAP_SECONDS = 0.06
BLASTS = 3
GROUP_SECONDS = 1.0
GROUPS = 2
ATTACK_SECONDS = 0.004
RELEASE_SECONDS = 0.018
VOICES = ((466.16, 1.0), (493.88, 0.85), (233.08, 0.5))
TOP_HZ = 9000.0
EVEN_LEVEL = 0.55
BITE_HZ = 3000.0
BITE_WIDTH = 900.0
BITE_LEVEL = 1.2
BARK = 0.035
BARK_RATE = 18.0
ROUGH_HZ = 62.0
ROUGH_DEPTH = 0.35
DRIVE = 2.2
GAIN = 0.7


def voice(frequency, t):
    pitch = frequency * (1.0 + BARK * np.exp(-t * BARK_RATE))
    phase = 2 * np.pi * np.cumsum(pitch) / RATE
    wave = np.zeros(len(t))
    for n in range(1, int(TOP_HZ / frequency) + 1):
        level = (1.0 if n % 2 else EVEN_LEVEL) / n
        level *= 1.0 + BITE_LEVEL * np.exp(-((n * frequency - BITE_HZ) / BITE_WIDTH) ** 2)
        wave += level * np.sin(n * phase)
    return wave


def blast():
    t = np.arange(int(RATE * BLAST_SECONDS)) / RATE
    wave = sum(level * voice(frequency, t) for frequency, level in VOICES)
    wave *= 1.0 - ROUGH_DEPTH * (0.5 + 0.5 * np.sin(2 * np.pi * ROUGH_HZ * t))
    wave = np.tanh(DRIVE * wave / max(abs(wave)))
    attack = int(RATE * ATTACK_SECONDS)
    release = int(RATE * RELEASE_SECONDS)
    envelope = np.ones(len(t))
    envelope[:attack] = np.linspace(0.0, 1.0, attack)
    envelope[-release:] = np.linspace(1.0, 0.0, release) ** 2
    return wave * envelope * (0.9 + 0.1 * np.exp(-t * 12.0))


def group():
    out = np.zeros(int(RATE * GROUP_SECONDS))
    one = blast()
    step = int(RATE * (BLAST_SECONDS + GAP_SECONDS))
    for i in range(BLASTS):
        out[i * step:i * step + len(one)] += one
    return out


def main():
    if "fresh" not in sys.argv:
        print("left alone; pass fresh to write the alarm sound again")
        return
    signal = np.concatenate([group() for _ in range(GROUPS)])
    signal = GAIN * signal / max(abs(signal))
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    sf.write(OUT, signal.astype(np.float32), RATE, format="OGG", subtype="VORBIS")
    print("alarm.ogg", round(len(signal) / RATE, 2), "seconds")


if __name__ == "__main__":
    main()
