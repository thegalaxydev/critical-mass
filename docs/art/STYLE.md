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
- Metal is shaded the way Create's own sheets are, never as a flat fill with a one-pixel edge. `art/tools/create_style.py` holds the patterns, measured from Create's `brass_block`, `copper_underside`, `industrial_iron_block` and `whistle`:
  - Plate: a dark outer frame with the darkest pixels at its corners, a bright one-pixel inner rim, then the field.
  - Brass and copper field: diagonal sheen bands about 8 pixels apart across five or six tones, darker toward the top-left and bottom-right of the field.
  - Iron and steel field: a dark inset line under the rim, then horizontal streaks, alternate rows lighter and each row brightest in the middle.
  - Tube or thin bar: banded across its width with the highlight about 40% of the way over, and a darker row under any rim or hoop.
  - Cavity: dark maroon, not black.
  - Keep that shading structural. Scattered rivets, glints and dots are still noise.

## How the generated textures were made

The generated textures in `art/SOURCES.md` came from a pipeline whose scripts are no longer kept in this project:

1. Each block model was defined as elements. Face sizes gave region sizes, the regions were packed into an atlas and the model JSON got exact UVs.
2. A blockout was drawn at game resolution for each region, with functional pixels such as holes, outlines and transparency locked.
3. The blockout was centred in a 1024 px canvas at about 30 px per texel, which matches the image generator's pixel size.
4. An image generator repainted that template, with the Create and Aeronautics style board attached.
5. The result was cropped to the template, reduced to the majority colour of a 5x5 sample per texel and quantized to the texture's own material ramps. Locked pixels and alpha were copied from the blockout and single-pixel speckles removed.

Raw generated outputs (downscaled) are kept in `art/generated/`.

## Current tools

Blocks made since have a Blockbench file in `art/blockbench/` and scripts in `art/tools/` that paint the texture sheet and export the models. `iso.py` renders a model JSON with its textures for review, and the `inspect` run renders it in game. `art/SOURCES.md` records how each texture was made, and `manifest.py` rebuilds `ASSET_MANIFEST.md`.
