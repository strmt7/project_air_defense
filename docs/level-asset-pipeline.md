# Level Asset Pipeline

Use this workflow before importing new city, structure, terrain, or texture assets.

## Source order
1. `docs/level-asset-source-map.md`
2. `android/assets/ATTRIBUTION.md`
3. `docs/visual-benchmark-tel-aviv-night.md`

## Pipeline
1. Pick a source with explicit ship-safe license for runtime assets, or mark it reference-only.
2. Inspect scale, axes, and format before import.
3. Convert to a mobile-safe runtime format such as OBJ or glTF.
4. Downscale textures to the smallest size that still survives phone distance.
5. Normalize scale against launcher pads, roads, and tower heights.
6. Add or update attribution.
7. Rebuild, install, screenshot, and benchmark.

## Mobile constraints
- Prefer a few high-value landmarks over hundreds of unique meshes.
- Prefer atlased or reusable materials over many small textures.
- Keep horizon, coast, and skyline silhouette readable before adding more geometry.
- Never import an asset without a recorded license boundary.
