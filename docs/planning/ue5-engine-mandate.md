# UE5 Engine Mandate

## Decision

Unreal Engine 5 is the only permitted engine for all future gameplay, rendering, tooling, editor, prototype, and shipping work in this repo.

## Consequences

- Do not add new libGDX runtime features.
- Treat the current Android/libGDX codebase as migration input only.
- Do not use static 2D images as gameplay backdrops for sea, horizon, skyline, or sky.
- City scenes must be real geometry, streamed geospatial content, or authored 3D assets with documented provenance.
- New city-data work must target a UE5-native content pipeline.

## Required UE5 World Stack

- `World Partition` for city-scale streaming and runtime cell management: [Level Streaming in Unreal Engine](https://dev.epicgames.com/documentation/unreal-engine/level-streaming-in-unreal-engine)
- `Nanite` for dense city meshes and high-poly imported districts: [Nanite Virtualized Geometry in Unreal Engine](https://dev.epicgames.com/documentation/en-us/unreal-engine/nanite-virtualized-geometry-in-unreal-engine)
- `Large World Coordinates` when preserving real-world scale and geospatial alignment: [Large World Coordinates Rendering in Unreal Engine 5](https://dev.epicgames.com/documentation/en-us/unreal-engine/large-world-coordinates-rendering-in-unreal-engine-5)
- `PCG + World Partition` only as controlled infill on top of authoritative city data, not as a substitute for it: [Using PCG with World Partition in Unreal Engine](https://dev.epicgames.com/documentation/en-us/unreal-engine/using-pcg-with-world-partition-in-unreal-engine)
- `Lumen` and modern UE5 lighting for dynamic night scenes and reflections: [Lumen Technical Details in Unreal Engine](https://dev.epicgames.com/documentation/unreal-engine/lumen-technical-details-in-unreal-engine)

## Explicitly Rejected

- Google Earth or Google Maps bulk extraction for shipped geometry or derived city datasets.
- Static skyline cards or background photos in gameplay view.
- New runtime work that keeps the current libGDX renderer alive as a long-term target.

## Planning Boundary

This document changes the target architecture and content policy only. It does not claim that the repo is already migrated.
