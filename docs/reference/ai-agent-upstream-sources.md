# AI Agent Upstream Sources

Compact provenance map for the repo's AI workflow and asset-sourcing surfaces.

| Source | Used for | Local surface |
| --- | --- | --- |
| `ZMB-UZH/omero-docker-extended` | badge generation, adapter layout, docs index pattern, routing docs, `caveman` / `context-budget` pattern | `AGENTS.md`, `CLAUDE.md`, `GEMINI.md`, `.github/copilot-instructions.md`, `docs/index.md`, `docs/reference/ai-agent-integrations.md`, `tools/update_readme_badges.py`, `.agents/skills/` |
| `JuliusBrussee/caveman` `v1.5.0` | terse-response overlay concept, configurable default mode, `off` mode, `/caveman-help`, and the 2026-04-11 release notes for those behaviors | `.agents/skills/caveman/SKILL.md`, `.agents/skills/caveman-help/SKILL.md` |
| `travisvn/awesome-claude-skills` | cross-agent repo-skill organization ideas | `docs/reference/ai-agent-skills.md` |
| `amitshekhariitbhu/awesome-android-complete-reference` | Android reference categories for docs and skills | `skills/android-3d-air-defense/SKILL.md`, `docs/popular-3d-android-game-workflows.md` |
| `OloOcki/awesome-citygml` | city-dataset and tool discovery for future levels | `docs/level-asset-source-map.md`, `.agents/skills/level-asset-curation/SKILL.md` |
| Epic Unreal Engine docs, Cesium for Unreal docs, ArcGIS Maps SDK for Unreal docs, Helsinki 3D, Berlin 3D portal, 3DBAG docs, CityJSON software index, BlenderGIS, and `Project-PLATEAU/PLATEAU-SDK-for-Unreal` | UE5 city ingestion, geospatial source selection, and photoreal scene planning | `docs/planning/ue5-*.md`, `docs/reference/ue5-city-model-source-map.md`, `.agents/skills/ue5-*/SKILL.md`, `tools/ue5_city_pipeline/` |
| `Genymobile/scrcpy`, `appium/appium`, `mobile-dev-inc/Maestro`, `AirtestProject/Airtest`, `tesseract-ocr/tesseract`, `opencv/opencv`, `adbutils`, and `pytesseract` | repo-local Android screen-verification toolchain and evaluated alternatives | `docs/android-visual-qa.md`, `docs/reference/android-visual-qa-sources.md`, `.agents/skills/android-visual-qa/SKILL.md`, `tools/android_visual_qa/` |
| official docs and repos for `Firecrawl`, `Browser Use`, `Playwright`, `Chrome DevTools MCP`, `Jina Reader`, `Crawl4AI`, and `Crawlee/Apify` | safe public-web research, extraction, browser fallback, source-audit, and rate-limit workflow design | `docs/reference/ai-agent-web-research-stack.md`, `.agents/skills/web-discovery/SKILL.md`, `.agents/skills/site-extract/SKILL.md`, `.agents/skills/browser-fallback/SKILL.md`, `.agents/skills/source-audit/SKILL.md`, `.agents/skills/compliance-and-rate-limit/SKILL.md` |
| `pinterest/ktlint` and `org.jlleitschuh.gradle.ktlint` | Kotlin format/lint gate | root `build.gradle.kts`, `.editorconfig`, `.github/workflows/ktlint.yml` |

## Local rule
- This repo copies workflow ideas, not blind text. When upstream guidance conflicts with measured game behavior, repo-local verification wins.
