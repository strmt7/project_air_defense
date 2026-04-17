# UE5 Regression Root Cause: 2026-04-17

## Scope

This note records the verified causes behind the repeated dark, flat, fake-looking UE5 city loop. It is not a style brief and must not be used as permission to revert to older broken states.

## Hard Facts

- Recent UE5 runtime commits introduced the affected systems: city bootstrap, camera framing, mobile UI, HUD, graphics controls, and battle simulation.
- The active runtime config now points at local Helsinki Kalasatama 3D Tiles, not the earlier documented 3DBAG remote candidate.
- Fresh editor-game captures on 2026-04-17 prove the game no longer crashes after the main menu and no longer collapses immediately after 90 seconds of unattended battle.
- The current city is still not production-quality. Persistent parent-tile slabs and photogrammetry artifacts remained after a 180-second load wait, so more wait time is not the fix.

## Loop Diagnosis

- The loop started because multiple high-risk surfaces changed together: city source, Cesium bootstrap, camera framing, lighting, UI, and battle scale.
- Earlier passes treated "process is alive" and "a screenshot exists" as too close to success. The required proof is a readable, post-input, active-battle frame.
- Documentation and runtime config diverged: 3DBAG was described as active while the game loaded local Helsinki. That made later reasoning target the wrong city source.
- Visual debugging primitives became player-facing geometry. The healthy district floor instances were not a minor styling issue; they falsified the city scene by drawing synthetic towers over real 3D tiles.
- Some fixes tuned symptoms after the scene was already contaminated. The correct order is source contract, georeference, camera/scale, lighting, then overlays and effects.

## Root Causes Fixed

- `ACesiumSunSky` was spawned without being anchored to the active `ACesiumGeoreference`. That broke sky/lighting alignment and contributed to black or wrong-looking captures.
- Cesium streaming was pushed into unstable visual territory by disabling useful culling and LOD behavior. Frustum culling, culled screen-space error, and LOD transitions are restored.
- Post-process exposure was over-controlled by project code. The current default path leaves exposure on the native UE5/Cesium lighting lane; `PostExposureBias` only becomes an override when it is explicitly configured to a non-zero value.
- Launcher placement was too far for the active district scale. The battle could show `CITY 0%` quickly because gameplay scale exceeded the real tileset bounds and engagement envelope.
- Healthy district damage floors were rendered as cyan instanced cube towers over the real city, including the main menu. Full-health synthetic city geometry is now forbidden; only actual damaged floors may render damage evidence.
- The documentation still described 3DBAG as active while the runtime used Helsinki. That mismatch caused stale-loop reasoning and is now corrected.

## Current Verification

- UE5 editor target build: passed.
- Menu visual proof: `benchmark-results/ue5-editor-rootcause-menu.png`.
- Battle visual proof: `benchmark-results/ue5-editor-rootcause-battle.png`, auto-started through `-ProjectAirDefenseAutoStartBattle` and showing `CITY 88%` after 90 seconds.
- Cyan-like pixel proxy on menu captures fell from 281 to 38 after removing healthy district floor instances.
- UE automation: five `ProjectAirDefense.BattleSimulation` tests passed.
- UE Monte Carlo: 300 runs passed with average intercept rate `0.840` and average city integrity `77.14`.
- Pipeline tests: `tools/ue5_city_pipeline/test_ue5_city_pipeline.py` passed.
- Badge check, ktlint, `:core:test`, and `git diff --check`: passed.

## Remaining Issues

- Helsinki 3D Tiles are a truthful local source but still show distant slabs and imperfect photogrammetry from the default gameplay view.
- The next real visual upgrade is not more fake geometry. It is either a better bounded official city source or a controlled Helsinki OBJ/Nanite bake with proper material cleanup.
- Damage visualization is still a gameplay overlay, not true per-building deformation of the source mesh. It must remain subdued until a proper building-level damage binding exists.

## Recovery Plan Amendment

### Priority 0: Keep The Scene Truthful

- Keep the runtime pointed at the active local Helsinki 3D Tiles path until a replacement source is selected and verified end-to-end.
- Do not patch visual weakness with static images, flat sky cards, copied block towers, or healthy synthetic district geometry.
- Treat any new geometry overlay as suspect until a screenshot proves it is not contaminating the real city mesh.
- If the current Helsinki tileset remains too artifact-heavy, choose one explicit replacement strategy: a bounded official mesh/Nanite bake, a cleaned Helsinki OBJ district, or another documented open city source. Do not mix strategies in one commit.

### Priority 1: Stabilize Lighting Before Adding Effects

- The lighting stack must stay georeferenced: `ACesiumSunSky`, `ACesiumGeoreference`, tileset origin, and camera focus all need to agree.
- Day/night work must be evaluated from at least three solar times: day, dusk, and night.
- Do not force global exposure unless `PostExposureBias` is intentionally non-zero and the before/after capture proves it helps.
- Buildings that look black are a lighting/material pipeline problem first, not a reason to add glow overlays.

### Priority 2: Make Gameplay Proof Repeatable

- The authoritative balance lane is the UE5 C++ simulation plus `scripts/run-ue5-battle-monte-carlo.ps1`.
- The active-battle visual proof must use `-ProjectAirDefenseAutoStartBattle` or a recorded tap/click proof, never an assumed menu transition.
- A gameplay claim requires both a headless metric and an active-battle screenshot. Process survival alone is not proof.
- Launcher distance, district radius, and threat envelopes must stay tied to measured tileset bounds.

### Priority 3: Improve UI Without Blocking The Sky

- Keep the top HUD split into small left and right cards; do not restore a full-width top bar.
- Use switches, sliders, and hold controls for mobile settings and camera operations; avoid repetitive button lists where a mobile control is clearer.
- Preserve the exact main menu title `PROJECT AIR DEFENSE`.
- Minimum text size and safe-zone spacing must be preserved after every menu or HUD layout change.

### Priority 4: Ship Only Verified Deltas

- Each commit must explain whether it changes source data, lighting, camera/scale, gameplay simulation, UI, or docs.
- Any visual commit needs a fresh menu capture and an active-battle capture.
- Any simulation commit needs UE automation plus the 300-run Monte Carlo lane.
- Generated logs, screenshots, Cesium caches, intermediate build output, and staged packaged scratch output must stay out of git.
- If GitHub Actions fail after push, fix the failing workflow on `main` with the smallest root-cause commit and poll again.

## Rules Going Forward

- Do not reintroduce static gameplay backgrounds.
- Do not render healthy synthetic buildings, towers, or debug anchors over the real city.
- Do not claim a visual fix without a fresh screenshot.
- Do not claim gameplay balance without UE automation or Monte Carlo proof.
- Do not change the main menu title from `PROJECT AIR DEFENSE`.
- Keep all future work in a single AI session; do not spawn subagents.
