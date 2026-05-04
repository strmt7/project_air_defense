# UE5 Playability Risk Review - 2026-05-05

Scope: architecture and edge-case review across Project Air Defense UE5 playability, UI, graphics, simulation, QA, packaging, and mobile proofing. This note is intentionally docs-only and prioritizes risks by likely player impact and likelihood.

## Top risks

1. Android UE5 runtime remains unproven on device. Packaging docs record missing Android optional component / Java selection blockers, and existing proof scripts produce Win64 packaged screenshots rather than install-run-device evidence.
2. City data delivery is inconsistent for Android. Runtime code resolves `ProjectDir/ExternalData/helsinki_kalasatama_3dtiles/tileset.json`, packaging config stages `../ExternalData`, while Android packaging notes still describe HTTPS streaming with an empty `RemoteTilesetUrl`.
3. Mobile graphics settings are aggressive and not platform-gated. Android config enables Vulkan SM5/mobile virtual textures/movable shadows/AO/TAA, while player settings expose TSR/SMAA/AO/ray-tracing/reflection toggles that may be unsupported, expensive, or placebo on phones.
4. Android touch cancellation remains device-unproven. The 2026-05-05 pass added widget-level mouse/touch release, mouse-leave, destruct, and drawer-close cleanup for held camera controls, but Android-specific cancellation/system interruption still needs emulator or device proof.
5. "Mobile UI proof" coverage is currently desktop landscape proof. It validates Win64 screenshots and safe-zone-like layout but not actual Android DPI, notches, touch hit targets, focus behavior, logcat, or thermal/frame pacing.
6. Live battle runtime uses a fixed simulation seed. This is valuable for regression tests but makes player battles repeatable and can hide distribution edge cases unless release/runtime seed selection is explicit.
7. Credits/economy have limited gameplay meaning. The simulation awards credits, but no interceptor spend cost or resource trade-off was found, so the player-facing credit counter can inflate without driving decisions.
8. Damage visualization is abstracted from the real city mesh. The 3x3 district model and damage scars are clear for verification, but impacts may not align with individual Helsinki buildings, reducing player trust in city damage feedback.
9. Mobile performance risk is still high. Cesium 3D tiles plus per-frame instance refresh for missile trails, blasts, launch plumes, and radar/HUD overlays are bounded but not device-profiled.
10. Stale Canvas HUD code can confuse future integration. `ProjectAirDefenseBattleHud` duplicates battle UI concepts but the game mode uses plain `AHUD`; accidental reactivation could bypass the safer UMG/safe-zone path.

## Recommended next implementation order

1. Prove Android packaging and data loading first: produce an APK/AAB install on a representative phone, verify the Helsinki tileset loads from the intended source, and capture logcat plus first-frame/player-start evidence.
2. Add device-focused mobile QA proof: tap through menu, start battle, open/close systems drawer, hold/release camera controls, and record frame time, memory, thermals, and screenshots.
3. Gate mobile graphics/settings by platform capability: hide or disable unsupported ray-tracing/TSR/SMAA/AO options on Android, and define a known-good low/default/high mobile profile.
4. Prove touch cancellation on Android: hold camera controls, drag/release off target, close the drawer during hold, background/restore, and confirm no repeat timer remains active.
5. Separate deterministic QA seed from release play seed, then add a reproducibility hook for bug reports.
6. Decide whether credits should become a spendable resource; if yes, add cost/balance tests before changing live tuning.
7. Improve damage-to-city clarity with either better district labeling/impact feedback or explicit UI language that damage is district-level.
8. Profile and budget VFX/Cesium rendering on device before raising visual density or adding more HUD/radar effects.
9. Retire or clearly label inactive Canvas HUD code once the UMG path is stable.
