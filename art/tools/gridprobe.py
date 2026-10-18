import sys
from PIL import Image
import numpy as np


def edges_period(a, axis):
    d = np.abs(np.diff(a.astype(int), axis=axis)).sum(axis=2)
    prof = d.sum(axis=1 - axis) if axis == 0 else d.sum(axis=0)
    prof = prof - prof.mean()
    best, bp = 0, 0
    for p in range(8, 96):
        s = 0
        for off in range(p):
            s = max(s, prof[off::p].sum())
        if s / (len(prof) / p) > best:
            best, bp = s / (len(prof) / p), p
    return bp


def native_period(a):
    d = np.abs(np.diff(a.astype(int), axis=1)).sum(axis=2).sum(axis=0)
    idx = np.where(d > d.mean() * 2)[0]
    gaps = np.diff(idx)
    gaps = gaps[gaps > 3]
    return np.median(gaps) if len(gaps) else None


im = np.array(Image.open(sys.argv[1]).convert("RGB"))
d = np.abs(np.diff(im.astype(int), axis=1)).sum(axis=2).sum(axis=0)
idx = np.where(d > d.mean() * 1.5)[0]
gaps = np.diff(idx)
gaps = gaps[gaps > 2]
vals, counts = np.unique(gaps, return_counts=True)
top = sorted(zip(counts, vals), reverse=True)[:8]
print("edge gap histogram (count, px):", top)
