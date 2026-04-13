# AI Agent Context Routing

Load the smallest correct slice first.

UE5-only rule: use Unreal Engine 5 planning docs for all new engine/runtime work. Existing Android/libGDX files are legacy migration-reference only.

## Numeric caps
- Open at most 4 task-specific files in the first pass.
- Run at most 2 refine loops before naming the edit target.
- Add at most 3 files per escalation round.
- If you have opened 8 task-specific files, stop and summarize.

## Default route
1. `AGENTS.md`
2. this file
3. `docs/reference/ai-agent-skills.md`
4. `docs/index.md` when the first three files are not enough
5. one domain doc
6. one code file
7. one nearest test

## Task map
| Task | Load first |
| --- | --- |
| UE5 engine migration, city sourcing, geospatial pipeline | `docs/planning/ue5-engine-mandate.md`, `docs/planning/ue5-city-model-strategies.md`, `docs/reference/ue5-city-model-source-map.md` |
| Gameplay, balance, radar, interception | `docs/architecture.md`, `core/src/main/kotlin/com/airdefense/game/BattleSimulation.kt`, nearest `core/src/test` module |
| 3D graphics, lighting, VFX, imported models | `docs/visual-benchmark-tel-aviv-night.md`, `docs/level-asset-pipeline.md`, `BattleScreen.kt`, `android/assets/ATTRIBUTION.md` |
| UI, controls, touch ergonomics | `StartScreen.kt`, `BattleScreen.kt`, `docs/popular-3d-android-game-workflows.md` |
| Android launch/build/compatibility | `android/build.gradle.kts`, `AndroidLauncher.kt`, `docs/release-and-install.md` |
| Benchmarks and standards | `scripts/run-benchmark-suite.ps1`, `docs/benchmark-suite.md`, `docs/benchmark-sources.md` |
| Agent docs, badges, workflows, skills | `docs/index.md`, `docs/reference/ai-agent-skills.md`, `docs/reference/ai-agent-integrations.md`, `docs/reference/ai-agent-upstream-sources.md`, `.agents/skills/` |

## Stop conditions
- You know the exact file to edit.
- You know the exact command or artifact that will verify the change.
- You can explain the license or provenance boundary for any new asset.
