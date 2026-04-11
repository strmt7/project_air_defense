# AI Agent Upstream Sources

Compact provenance map for the repo's AI workflow and asset-sourcing surfaces.

| Source | Used for | Local surface |
| --- | --- | --- |
| `ZMB-UZH/omero-docker-extended` | badge generation, adapter layout, routing docs, `caveman` / `context-budget` pattern | `AGENTS.md`, `CLAUDE.md`, `GEMINI.md`, `.github/copilot-instructions.md`, `tools/update_readme_badges.py`, `.agents/skills/` |
| `JuliusBrussee/caveman` `v1.5.0` | terse-response overlay concept, configurable default mode, `off` mode, `/caveman-help`, and the 2026-04-11 release notes for those behaviors | `.agents/skills/caveman/SKILL.md`, `.agents/skills/caveman-help/SKILL.md` |
| `travisvn/awesome-claude-skills` | cross-agent repo-skill organization ideas | `docs/reference/ai-agent-skills.md` |
| `amitshekhariitbhu/awesome-android-complete-reference` | Android reference categories for docs and skills | `skills/android-3d-air-defense/SKILL.md`, `docs/popular-3d-android-game-workflows.md` |
| `OloOcki/awesome-citygml` | city-dataset and tool discovery for future levels | `docs/level-asset-source-map.md`, `.agents/skills/level-asset-curation/SKILL.md` |
| `pinterest/ktlint` and `org.jlleitschuh.gradle.ktlint` | Kotlin format/lint gate | root `build.gradle.kts`, `.editorconfig`, `.github/workflows/ktlint.yml` |

## Local rule
- This repo copies workflow ideas, not blind text. When upstream guidance conflicts with measured game behavior, repo-local verification wins.
