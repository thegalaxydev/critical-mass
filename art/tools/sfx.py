import os
import sys

import numpy as np
import soundfile as sf

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "criticalmass", "sounds")
RATE = 22050


def clock(seconds):
    return np.arange(int(RATE * seconds)) / RATE


def turns(frequency, t):
    f = np.full(len(t), float(frequency)) if np.isscalar(frequency) else frequency
    return np.cumsum(f) / RATE


def sine(frequency, t):
    return np.sin(2 * np.pi * turns(frequency, t))


def square(frequency, t, duty=0.5):
    return np.where(turns(frequency, t) % 1.0 < duty, 1.0, -1.0)


def triangle(frequency, t):
    return 4 * np.abs(turns(frequency, t) % 1.0 - 0.5) - 1


def saw(frequency, t):
    return 2 * (turns(frequency, t) % 1.0) - 1


def noise(t, seed):
    return np.random.default_rng(seed).uniform(-1, 1, len(t))


def lowpass(x, cutoff):
    a = np.exp(-2 * np.pi * cutoff / RATE)
    out = np.empty(len(x))
    held = 0.0
    for i, v in enumerate(x):
        held = (1 - a) * v + a * held
        out[i] = held
    return out


def highpass(x, cutoff):
    return x - lowpass(x, cutoff)


def crush(x, bits):
    steps = 2 ** (bits - 1)
    return np.round(x * steps) / steps


def hold(x, factor):
    return np.repeat(x[::factor], factor)[:len(x)]


def decay(t, seconds):
    return np.exp(-t / seconds)


def edges(x, attack=0.004, release=0.01):
    n = len(x)
    a = max(1, int(RATE * attack))
    r = max(1, int(RATE * release))
    shape = np.ones(n)
    shape[:a] = np.linspace(0, 1, a)
    shape[-r:] *= np.linspace(1, 0, r)
    return x * shape


def seamless(x, seconds=0.08):
    n = int(RATE * seconds)
    body = x[:-n].copy()
    fade = np.linspace(0, 1, n)
    body[:n] = body[:n] * fade + x[-n:] * (1 - fade)
    return body


def level(x, peak):
    return peak * x / max(1e-9, np.max(np.abs(x)))


def pad(x, seconds):
    return np.concatenate([x, np.zeros(max(0, int(RATE * seconds) - len(x)))])


def mix(*parts):
    n = max(len(p) for p in parts)
    out = np.zeros(n)
    for p in parts:
        out[:len(p)] += p
    return out


def crackle(t, seed, rate, sharpness=0.0015):
    rng = np.random.default_rng(seed)
    out = np.zeros(len(t))
    count = int(rate * len(t) / RATE)
    for at in rng.integers(0, len(t), count):
        span = min(len(t) - at, int(RATE * sharpness * (1 + 3 * rng.random())))
        out[at:at + span] += rng.uniform(0.4, 1.0) * rng.choice((-1, 1)) * np.linspace(1, 0, span)
    return out


def hum():
    t = clock(2.0)
    wave = 0.25 * sine(50, t) + 1.0 * sine(100, t) + 0.35 * sine(150, t) + 0.3 * sine(200, t) + 0.18 * sine(300, t) + 0.08 * square(100, t)
    wave *= 0.9 + 0.1 * sine(0.5, t)
    return level(crush(lowpass(wave, 900), 7), 0.5)


def generator_run():
    t = clock(2.0)
    wave = 0.7 * saw(64, t) + 0.5 * sine(128, t) + 0.22 * sine(800 + 6 * sine(1.0, t), t) + 0.18 * lowpass(noise(t, 11), 400)
    wave *= 0.92 + 0.08 * sine(4.0, t)
    return level(crush(lowpass(wave, 1600), 7), 0.5)


def motor_run():
    t = clock(1.0)
    wave = 0.8 * triangle(120, t) + 0.3 * sine(480 + 4 * sine(2.0, t), t) + 0.25 * highpass(lowpass(noise(t, 12), 3000), 900)
    wave *= 0.9 + 0.1 * sine(8.0, t)
    return level(crush(wave, 7), 0.45)


def steam_hiss():
    t = clock(1.6)
    wave = highpass(lowpass(noise(t, 13), 5200), 1400) * (0.85 + 0.15 * sine(3.0, t))
    return level(crush(seamless(wave), 7), 0.45)


def reactor_thrum():
    t = clock(4.0)
    wave = 1.0 * sine(40, t) + 0.5 * sine(80, t) * (0.6 + 0.4 * sine(0.5, t)) + 0.12 * sine(1200, t) * (0.5 + 0.5 * sine(0.25, t)) + 0.1 * triangle(120, t)
    return level(crush(lowpass(wave, 1500), 8), 0.5)


def thump(t, start, end, seconds):
    return sine(end + (start - end) * decay(t, seconds * 0.35), t) * decay(t, seconds)


def breaker_close():
    t = clock(0.5)
    wave = 1.2 * lowpass(noise(t, 21), 1800) * decay(t, 0.03) + 1.0 * thump(t, 90, 45, 0.16) + 0.35 * square(880, t) * decay(t, 0.05) + 0.2 * square(1320, t) * decay(t, 0.03)
    return level(crush(edges(wave, 0.001, 0.05), 6), 0.75)


def breaker_open():
    t = clock(0.35)
    wave = 1.0 * lowpass(noise(t, 22), 2600) * decay(t, 0.02) + 0.6 * thump(t, 140, 80, 0.08) + 0.4 * square(620, t) * decay(t, 0.06)
    return level(crush(edges(wave, 0.001, 0.05), 6), 0.7)


def breaker_trip():
    t = clock(0.7)
    spring = square(300 - 120 * (1 - decay(t, 0.2)), t, 0.3) * decay(t, 0.22) * (0.6 + 0.4 * sine(38, t))
    wave = 1.4 * lowpass(noise(t, 23), 3200) * decay(t, 0.025) + 0.9 * thump(t, 120, 50, 0.14) + 0.55 * spring
    return level(crush(edges(wave, 0.001, 0.08), 6), 0.8)


def switch_throw():
    t = clock(0.45)
    scrape = highpass(noise(t, 24), 1800) * np.clip(1 - np.abs(t - 0.06) / 0.06, 0, 1)
    clunk = np.concatenate([np.zeros(int(RATE * 0.11)), (lowpass(noise(clock(0.3), 25), 1500) * decay(clock(0.3), 0.03) + thump(clock(0.3), 110, 60, 0.1))])
    return level(crush(edges(mix(0.4 * scrape, clunk), 0.001, 0.05), 6), 0.7)


def relay_click():
    t = clock(0.08)
    wave = noise(t, 26) * decay(t, 0.004) + 0.6 * square(2100, t) * decay(t, 0.012)
    return level(crush(edges(wave, 0.0005, 0.02), 6), 0.6)


def buzz(t, seed):
    carrier = square(100 + 14 * sine(23, t), t, 0.35) + 0.6 * square(200, t, 0.2)
    rough = 0.55 + 0.45 * lowpass(np.abs(noise(t, seed)), 60) / 0.5
    return highpass(carrier * rough, 300) + 0.9 * crackle(t, seed + 1, 70)


def arc_zap():
    t = clock(0.42)
    wave = buzz(t, 31) * np.clip(1.2 - t / 0.42, 0, 1)
    return level(crush(edges(wave, 0.001, 0.06), 5), 0.75)


def arc_blast():
    t = clock(1.6)
    bang = noise(t, 32) * decay(t, 0.05)
    zap = buzz(t, 33) * decay(t, 0.16)
    boom = thump(t, 70, 28, 0.55)
    rumble = lowpass(noise(t, 34), 180) * decay(t, 0.5) * 2.5
    wave = 1.5 * bang + 0.8 * zap + 1.3 * boom + 0.8 * rumble
    return level(crush(np.tanh(1.4 * edges(wave, 0.0005, 0.25)), 6), 0.72)


def wire_sizzle():
    t = clock(0.9)
    wave = (0.5 * highpass(noise(t, 35), 2500) + crackle(t, 36, 120)) * decay(t, 0.4)
    return level(crush(edges(wave, 0.002, 0.2), 6), 0.6)


def steam_vent():
    t = clock(1.4)
    shape = np.clip(t / 0.03, 0, 1) * decay(t, 0.45)
    sweep = 5200 * decay(t, 0.9) + 1200
    body = noise(t, 37)
    out = np.empty(len(t))
    held = 0.0
    for i, v in enumerate(body):
        a = np.exp(-2 * np.pi * sweep[i] / RATE)
        held = (1 - a) * v + a * held
        out[i] = held
    return level(crush(edges(highpass(out, 700) * shape, 0.002, 0.2), 7), 0.7)


def pipe_burst():
    t = clock(1.2)
    wave = 1.4 * noise(t, 38) * decay(t, 0.04) + 0.9 * thump(t, 100, 40, 0.2) + 0.8 * highpass(lowpass(noise(t, 39), 4500), 900) * decay(t, 0.35)
    return level(crush(np.tanh(1.3 * edges(wave, 0.0005, 0.2)), 6), 0.7)


def geiger(tone=1.0, seed=40):
    t = clock(0.024)
    snap = noise(t, seed) * decay(t, 0.0004)
    ring = sine(2300 * tone, t) * decay(t, 0.0016) + 0.6 * sine(4700 * tone, t) * decay(t, 0.0008)
    body = 0.35 * sine(950 * tone, t) * decay(t, 0.003)
    return level(crush(edges(snap + ring + body, 0.0001, 0.006), 6), 0.85)


def blips(notes, each, duty=0.5, bits=5):
    parts = []
    for i, f in enumerate(notes):
        t = clock(each)
        parts.append(edges(square(f, t, duty) * (0.75 + 0.25 * decay(t, each * 0.4)), 0.002, 0.012))
    return level(crush(np.concatenate(parts), bits), 0.32)


def order_placed():
    return blips((880, 1320), 0.09)


def order_met():
    return blips((523.25, 659.25, 783.99, 1046.5), 0.075)


def order_missed():
    return blips((440, 330, 220), 0.13, 0.25)


def wire_string():
    t = clock(0.3)
    wave = saw(200 + 160 * (1 - decay(t, 0.03)), t) * decay(t, 0.09) + 0.4 * noise(t, 41) * decay(t, 0.008)
    return level(crush(edges(wave, 0.001, 0.05), 6), 0.6)


def wire_cut():
    t = clock(0.22)
    wave = noise(t, 42) * decay(t, 0.006) + 0.7 * square(1400 * decay(t, 0.08) + 300, t) * decay(t, 0.05)
    return level(crush(edges(wave, 0.0005, 0.04), 6), 0.6)


def chug(t, rate, seed):
    beat = np.exp(-(turns(rate, t) % 1.0) * 5.0)
    body = lowpass(noise(t, seed), 700) * 1.6 + 0.8 * saw(46, t) + 0.5 * sine(92, t)
    return body * beat


def engine_start():
    t = clock(2.9)
    rate = 3.5 + 11.0 * np.clip(t / 2.6, 0, 1) ** 1.6
    whine = 0.25 * sine(300 + 500 * np.clip(t / 2.9, 0, 1), t) * np.clip(1 - t / 2.0, 0, 1)
    wave = chug(t, rate, 51) * (0.6 + 0.4 * np.clip(t / 2.6, 0, 1)) + whine
    return level(crush(edges(wave, 0.01, 0.1), 6), 0.7)


def engine_run():
    t = clock(1.0)
    wave = chug(t, 16, 52) + 0.15 * highpass(noise(t, 53), 2500)
    return level(crush(wave, 6), 0.5)


def service():
    t = clock(0.5)
    wave = np.zeros_like(t)
    for i in range(4):
        since = np.clip(t - 0.11 * i, 0, None)
        on = (t >= 0.11 * i).astype(float)
        wave = wave + on * (noise(t, 60 + i) * decay(since, 0.005) + 0.5 * square(880 + 110 * i, t) * decay(since, 0.02))
    return level(crush(edges(wave, 0.0005, 0.04), 6), 0.55)


def fallout():
    t = clock(8.5)
    gust = 0.55 + 0.3 * sine(0.25, t) + 0.15 * sine(0.5, t + 0.7)
    wind = highpass(lowpass(noise(t, 61), 700), 120) * gust
    whistle = lowpass(noise(t, 62), 2400) * sine(0.125, t) ** 2 * 0.18 * sine(430 + 40 * sine(0.25, t), t)
    drone = 0.5 * sine(55, t) + 0.35 * sine(82.5, t) * (0.6 + 0.4 * sine(0.5, t)) + 0.12 * triangle(110, t)
    ticks = crackle(t, 63, 14, 0.0008) * 0.5
    wave = 1.0 * wind + whistle + 0.5 * drone + ticks
    return level(crush(seamless(wave, 0.5), 7), 0.5)


def water_spray():
    t = clock(2.0)
    hiss = highpass(lowpass(noise(t, 71), 3800), 500) * (0.8 + 0.2 * sine(1.5, t))
    patter = lowpass(noise(t, 72), 900) * (0.6 + 0.4 * sine(4.0, t) ** 2) * 0.5
    return level(crush(seamless(hiss + patter), 7), 0.4)


SOUNDS = {
    "water_spray": water_spray,
    "service": service,
    "fallout": fallout,
    "engine_start": engine_start, "engine_run": engine_run,
    "hum": hum, "generator_run": generator_run, "motor_run": motor_run, "steam_hiss": steam_hiss, "reactor_thrum": reactor_thrum, "breaker_close": breaker_close,
    "breaker_open": breaker_open, "breaker_trip": breaker_trip, "switch_throw": switch_throw, "relay_click": relay_click, "arc_zap": arc_zap, "arc_blast": arc_blast,
    "wire_sizzle": wire_sizzle, "steam_vent": steam_vent, "pipe_burst": pipe_burst, "geiger": geiger, "geiger_2": lambda: geiger(0.88, 41), "geiger_3": lambda: geiger(1.13, 42), "order_placed": order_placed, "order_met": order_met,
    "order_missed": order_missed, "wire_string": wire_string, "wire_cut": wire_cut,
}


def main():
    if "fresh" not in sys.argv:
        print("left alone; pass fresh to synthesise the sound set again")
        return
    os.makedirs(OUT, exist_ok=True)
    wanted = [name for name in sys.argv[1:] if name in SOUNDS]
    for name, make in SOUNDS.items():
        if wanted and name not in wanted:
            continue
        signal = make()
        sf.write(os.path.join(OUT, name + ".ogg"), signal.astype(np.float32), RATE, format="OGG", subtype="VORBIS")
        print(name, round(len(signal) / RATE, 2), "seconds")


if __name__ == "__main__":
    main()
