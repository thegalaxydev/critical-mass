# Visual language and production pipeline

## Rules

- 16 texels per block for every block face and model element; items are 16x16. Multi-element models pack their faces into 16/32/64 px atlases at that same density.
- Materials: riveted dark steel (pressure parts), iron (frames, end plates), copper (pipework, heat transfer), brass (mechanisms, instrument fittings), pale ceramic (fuel), lead-grey (absorbers, shielding), off-white rubberised canvas (protective equipment).
- Every functional face reads at a distance:
  - Shaft sockets: brass ring around the shaft, only on faces where Create connects (`hasShaftTowards`).
  - Primary loop: red collar = outlet / hot, blue collar = inlet / cold.
  - Steam: red-collared inlet, pale-collared exhaust.
  - Flow direction: brass double chevrons on the pump housing, pointing to the outlet.
- Warning stripes only on hazard-bearing objects (hatch, cask, waste canister, suit trim). Lamps are drawn unlit unless a real blockstate lights them (a block state such as `powered=true`).
- No baked directional scene lighting, no text, low noise, discrete pixels.

## Pipeline (reproducible; scripts in `art/tools/`)

1. `modelgen.py` + `specs*.py`: each block model is defined as elements; face sizes give region sizes; regions are packed into an atlas and the model JSON gets exact UVs.
2. `paint*.py`, `items_paint.py`, `armor_paint.py`: draw a blockout at game resolution per region (functional pixels such as holes, outlines and transparency are "locked").
3. `template_fit`: the blockout is centred in a 1024 px canvas at about 30 px per texel. This matches the generator's native pixel size, so the generator's pixels line up with ours.
4. the generator (image tool `generate`, model `generator`, via `image_tool.py --edit`) repaints the template, with the Create/Aeronautics style board attached.
5. `finalize`: crop the template area, take the majority colour of a 5x5 sample per texel, quantize to the texture's own material ramps, copy locked pixels and alpha from the blockout, remove single-pixel speckles.
6. `iso.py` renders the real model JSON with the final textures for review; the `inspect` run renders them in game.

Raw generated outputs (downscaled) are kept in `art/generated/`, and every prompt is logged in `art/SOURCES.md`.
