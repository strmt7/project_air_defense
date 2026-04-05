---
name: android-3d-air-defense
description: Use this skill when building or iterating on the Air Defense Warrior Android 3D missile-defense prototype, including realistic projectile tuning, libGDX scene setup, and balancing interceptor guidance.
---

# Android 3D Air Defense Skill

Use this workflow whenever you modify gameplay, visuals, or physics in this repository.

## Objectives
- Keep the game fully 3D (models, camera, and world-space motion).
- Preserve physically plausible ballistic motion (`g = -9.81 m/s^2`).
- Keep touch controls responsive in landscape mode.
- Prioritize small, testable changes with clear difficulty progression.

## Workflow
1. **Tune physics first**
   - Update missile spawn altitude, speed, and gravity interactions.
   - Keep interceptor velocity high enough for feasible interceptions.
2. **Validate game feel**
   - Ensure cooldown, wave progression, and spawn timing create pressure without impossible states.
3. **Preserve rendering performance**
   - Reuse procedural models where possible.
   - Avoid per-frame allocations in hot loops.
4. **Check Android compatibility**
   - Keep `minSdk >= 28` and ensure required ABIs are bundled.

## Balancing defaults
- Incoming missile speed: `38..52 + wave * 1.6`.
- Interceptor speed: `145`.
- Interceptor blast radius: `9`.
- Cooldown: `1.2s`.

## Definition of done for gameplay updates
- Missiles visibly follow ballistic arcs.
- Player can reliably neutralize threats with timing skill.
- HUD reflects wave, readiness, and active threats.
