# UE5 Visual Acceptance

## Core Rule

The gameplay city view must read as real 3D space from multiple camera angles, not as a flat backdrop.

## Mandatory Visual Criteria

- no static 2D background images in gameplay
- no healthy synthetic city towers, district cubes, or debug anchors over the real city mesh
- visible sea, shoreline, skyline, and terrain are all real geometry or streamed geospatial content
- buildings must hold up under orbit, pan, zoom, and yaw inspection
- skyline silhouette must remain legible from far view and near view
- water, roads, facades, and roofs must use distinct material logic
- lighting must be handled by UE5 scene lighting, not baked fake overlays in the gameplay camera
- missile, launch, intercept, and impact VFX must follow `docs/reference/missile-vfx-reference.md`

## Mandatory Camera Criteria

- orbit / rotate
- pan in both axes
- zoom in and out
- reset to default framing
- at least one elevated inspection view and one gameplay view

## Failure Cases

Reject the build if any of these are true:

- the skyline only looks correct from one camera angle
- structures read like flat extrusions or billboard silhouettes
- the horizon is a static image
- full-health gameplay overlays look like replacement buildings
- the sea or shoreline is only a painted texture with no real scene depth
- camera controls are too limited to inspect the scene
- projectile traces read as opaque rods, bead strings, giant rings, or full-screen smoke overlays
