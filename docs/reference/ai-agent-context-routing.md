# AI Agent Context Routing

UE5-only rule: use Unreal Engine 5 planning docs for all new engine/runtime work. Existing Android/libGDX files are legacy migration-reference only.

Single-session rule: never spawn, delegate to, coordinate, or recommend background agents, subagents, parallel agents, or separate agent sessions.

## Numeric caps
- Open at most 4 task-specific files in the first pass.
- Run at most 2 refine loops before naming the edit target.
- Add at most 3 files per escalation round.
- If you have opened 8 task-specific files, stop and summarize.

## Default route
1. `AGENTS.md`, this file, `docs/reference/ai-agent-skills.md`
2. `docs/index.md` only when the first three files are not enough
3. one domain doc, one code file, one nearest test

## Task map
| Task | Load first |
| --- | --- |
| UE5 engine migration, city sourcing, geospatial pipeline | `docs/planning/ue5-engine-mandate.md`, `docs/planning/ue5-city-model-strategies.md`, `docs/reference/ue5-city-model-source-map.md` |
| UE5 photoreal city pilot or camera inspection surface | `docs/planning/ue5-city-pilot-3dbag-rotterdam.md`, `docs/planning/ue5-city-pilot-helsinki-kalasatama.md`, `docs/planning/ue5-visual-acceptance.md`, `ue5/ProjectAirDefenseUE5/README.md` |
| UE5 mobile gameplay flow, touch state, radar, battle start, or deterministic verification | `docs/reference/ue5-mobile-agent-skill-sources.md`, `.agents/skills/ue5-mobile-gameplay/SKILL.md`, `docs/planning/ue5-gameplay-migration-matrix.md` |
| UE5 mobile HUD, menus, settings, or touch ergonomics | `docs/reference/ue5-mobile-agent-skill-sources.md`, `.agents/skills/ue5-mobile-ui/SKILL.md`, nearest UE5 UI code file |
| UE5 front-end menu stack, options flow, or CommonUI-routing decision | `docs/reference/ue5-mobile-agent-skill-sources.md`, `.agents/skills/ue5-commonui-menu-systems/SKILL.md`, nearest UE5 UI code file |
| UE5 mobile rendering, graphics settings, AA, AO, reflections, or device-tier policy | `docs/reference/ue5-mobile-agent-skill-sources.md`, `.agents/skills/ue5-mobile-rendering/SKILL.md`, current runtime settings or user-settings code |
| Gameplay, balance, radar, interception | `docs/architecture.md`, `core/src/main/kotlin/com/airdefense/game/BattleSimulation.kt`, nearest `core/src/test` module |
| 3D graphics, lighting, VFX, imported models | `docs/visual-benchmark-tel-aviv-night.md`, `docs/level-asset-pipeline.md`, `BattleScreen.kt`, `android/assets/ATTRIBUTION.md` |
| UI, controls, touch ergonomics | `StartScreen.kt`, `BattleScreen.kt`, `docs/popular-3d-android-game-workflows.md` |
| Android launch/build/compatibility | `android/build.gradle.kts`, `AndroidLauncher.kt`, `docs/release-and-install.md` |
| Benchmarks and standards | `scripts/run-benchmark-suite.ps1`, `docs/benchmark-suite.md`, `docs/benchmark-sources.md` |
| Agent docs, badges, workflows, skills | `docs/index.md`, `docs/reference/ai-agent-skills.md`, `docs/reference/ai-agent-integrations.md`, `docs/reference/ai-agent-upstream-sources.md`, `.agents/skills/` |

- Stop when you know the exact file to edit, the exact verification command or artifact, and any required asset provenance boundary.
