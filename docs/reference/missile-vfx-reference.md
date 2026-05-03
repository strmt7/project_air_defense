# Missile VFX Reference

Purpose: keep projectile, launch, intercept, and impact visuals tied to inspected reference footage instead of invented graphic shapes.

## 2026-05-03 Video Pass

Sampled 20 relevant Patriot / PAC-3 / air-defense videos with `yt-dlp` frame extraction and contact sheets. Temporary downloaded videos were deleted after sampling; contact-sheet proof was kept outside the repo under `%TEMP%\project_air_defense_patriot_video_review`.

| # | Source |
| --- | --- |
| 1 | [Patriot PAC-3 missile test - direct impact](https://www.youtube.com/watch?v=KMrugIQlzOk) |
| 2 | [MIM-104F Patriot PAC-3 ERINT Live Fire](https://www.youtube.com/watch?v=r_chBn5HKeM) |
| 3 | [Patriot PAC-3 Missile Segment Enhancement MSE Test](https://www.youtube.com/watch?v=kmb5FPS9TYE) |
| 4 | [U.S. F-35, PAC-3 Team Up to Defeat Target](https://www.youtube.com/watch?v=zjQxDsivbyE) |
| 5 | [Patriot Missile Intercept](https://www.youtube.com/watch?v=ahlifSWIzjY) |
| 6 | [Patriot intercepting Syrian UAV](https://www.youtube.com/watch?v=ZbT9O1yIi7Q) |
| 7 | [US DEU Patriot Firing Exercise Intercept](https://www.youtube.com/watch?v=OIMJh1CVkzY) |
| 8 | [Patriot Missile Live Fire](https://www.youtube.com/watch?v=WuMlDVxLtRs) |
| 9 | [Soldiers firing MIM-104 Patriot during Talisman Sabre](https://www.youtube.com/watch?v=Chrpf65mDuA) |
| 10 | [Air Defense Missile launched, Patriot missile](https://www.youtube.com/watch?v=_K76SGo5sJc) |
| 11 | [Patriot Missile Launch](https://www.youtube.com/watch?v=K04JaCHIURM) |
| 12 | [U.S. Military Patriot Missile Live-fire Footage HD](https://www.youtube.com/watch?v=Au1i9el2_9s) |
| 13 | [Patriot Missile Live Fire at NATO Missile Firing Installation](https://www.youtube.com/watch?v=2HevaNzsMys) |
| 14 | [Patriot Missile shoots down Hera target](https://www.youtube.com/watch?v=oPDmxacBpBU) |
| 15 | [MIM-104 Patriot PAC 2 interception test](https://www.youtube.com/watch?v=WWXeQFoCZyQ) |
| 16 | [MIM-104A Patriot missile hits](https://www.youtube.com/watch?v=2k-pBXw9kR0) |
| 17 | [PAC-3 Missile Test](https://www.youtube.com/watch?v=DT6DzaG_658) |
| 18 | [The PAC-3 Missile System Test P6-4](https://www.youtube.com/watch?v=23a1mCKPpUY) |
| 19 | [The PAC-3 Missile System Test P6L-3](https://www.youtube.com/watch?v=Mtz2BQ2Au0w) |
| 20 | [Iron Dome and Patriot MIM-104 Intercept Exercise](https://www.youtube.com/watch?v=xvMmqvWO40c) |

Additional non-YouTube references:

- [DVIDS PAC-3 launch video](https://www.dvidshub.net/video/782607/pac-3-launch)
- [Missile Defense Agency: PATRIOT Advanced Capability-3](https://www.mda.mil/system/pac_3.html)
- [CSIS Missile Threat: Patriot](https://missilethreat.csis.org/system/patriot/)
- [The War Zone: PAC-3 hit-to-kill interceptors and lethality enhancer](https://www.twz.com/patriot-pac-3-hit-to-kill-interceptors-also-pack-a-little-known-explosive-warhead)

## Extracted Visual Rules

- Missile bodies are usually tiny at gameplay distance; the motor/exhaust and smoke trail are the readable elements.
- Smoke trails are pale gray-white, thin, directional, and often slightly broken by wind/camera motion; they should not appear as brown rods or bead strings.
- Launches create a bright motor flare and a dense ground plume, but launcher plumes must be camera-safe and must never occlude the whole view.
- Midair intercepts read as a compact white/yellow flash followed by small gray smoke/fragments; they are not giant blue rings, opaque disks, or huge solid smoke balls.
- Ground-coupled hostile impacts may be larger and dirtier than midair intercepts, with wider smoke/debris and stronger environment coupling.
- Night intercepts read primarily as glowing points and flash blooms, not detailed missile meshes.

## Current Implementation Mapping

| Rule | Runtime implementation |
| --- | --- |
| Tiny bodies, readable exhaust | `ProjectAirDefenseBattleManager` uses small cone bodies and separate short exhaust instances. |
| Thin pale smoke | Trail cylinders are velocity-aligned, shortened, and color-separated for hostile/interceptor paths. |
| Camera-safe launch plume | Launch plume smoke uses compact vertical columns and culls near the active camera. |
| Compact midair intercept | Intercept blast core/smoke caps are separate from hostile ground-impact caps. |
| Larger ground damage | Hostile impacts keep larger smoke/debris and district-damage floor scarring. |

## Visual Proof Artifacts

- `benchmark-results/ue5-vfx-reference-camera-safe-intercept.png`
- `benchmark-results/ue5-vfx-reference-camera-safe-late.png`
- `benchmark-results/ue5-vfx-reference-camera-safe-early.png`

The 2-second early capture can still look pale while streamed city tiles settle. The 4-second and 6-second captures are the stable visual proof checkpoints for active battle readability.
