# UE5 City Pipeline

Use this skill when a real district or city subset must become a UE5-ready pilot with lawful sourcing, deterministic manifests, and either a direct 3D Tiles runtime proof path or a Blender-to-UE5 ingest path.

## Load first

1. `AGENTS.md`
2. `docs/planning/ue5-engine-mandate.md`
3. `docs/planning/ue5-city-model-strategies.md`
4. `docs/planning/ue5-city-pilot-helsinki-kalasatama.md`
5. `docs/planning/ue5-city-pilot-3dbag-rotterdam.md` only when evaluating the remote candidate path
6. `docs/reference/ue5-city-model-source-map.md`
7. `tools/ue5_city_pipeline/catalog.json`

## Rules

- Do not select Google Earth or Google Maps extraction.
- Prefer official textured district meshes over semantic-only data.
- Use the active local Helsinki Kalasatama 3D Tiles dataset first when the repo needs a truthful runtime proof.
- Do not download additional large city datasets unless a task explicitly needs fallback evaluation or bake preparation.
- Keep one pilot district active at a time.
- Update the manifest with the generator, not by hand.
- Keep downloaded raw data out of git under `data/external/`.

## Commands

- Validate the pipeline:
  `python tools/ue5_city_pipeline/test_ue5_city_pipeline.py`
- Active Helsinki: generate the checked-in pilot manifest:
  `python tools/ue5_city_pipeline/generate_import_manifest.py --source helsinki_kalasatama_3dtiles --output data/ue5_city_pilot/helsinki_kalasatama/pilot_manifest.json`
- Active Helsinki: dry-run the source download:
  `python tools/ue5_city_pipeline/download_city_source.py --source helsinki_kalasatama_3dtiles --dest data/external --dry-run`
- Active Helsinki: build the master runtime `tileset.json` after extraction:
  `python tools/ue5_city_pipeline/build_master_tileset.py --root data/external/helsinki_kalasatama_3dtiles --output data/external/helsinki_kalasatama_3dtiles/tileset.json`
