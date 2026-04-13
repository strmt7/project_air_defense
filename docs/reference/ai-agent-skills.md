# AI Agent Skills

These repo-local skills are mandatory guidance surfaces for external agents.

## Precedence
1. `AGENTS.md`
2. `docs/reference/ai-agent-context-routing.md`
3. this file
4. matching skill in `.agents/skills/`
5. adapter files such as `CLAUDE.md`

Engine rule: Unreal Engine 5 is the only permitted engine for new runtime/editor/tooling work. Existing Android/libGDX skills remain only for migration inventory, behavioral comparison, and asset extraction until removed.

## Skills
| Skill | Use when | Outcome |
| --- | --- | --- |
| `context-budget` | the task is broad or repetitive | context stays small and fast |
| `caveman` | any normal progress update or summary | token use stays low without losing proof |
| `caveman-help` | an agent or user needs the current caveman mode map | one-shot help without changing active mode |
| `verification-loop` | after non-trivial changes | verification is explicit and bounded |
| `web-discovery` | current public-web research spans multiple sites or source types | discovery stays bounded, source-first, and date-aware |
| `site-extract` | a known public page needs structured extraction | evidence stays compact, citation-ready, and reproducible |
| `browser-fallback` | a page is JS-heavy or stateful and simple fetch is not enough | browser work stays deterministic and limited |
| `source-audit` | a web-backed claim or recommendation needs trust weighting | confirmed facts, inference, and gaps stay separate |
| `compliance-and-rate-limit` | repeated requests or larger-scope collection could create policy or load risk | collection stays cache-aware, paced, and non-evasive |
| `ue5-city-pipeline` | a real district needs a UE5-native source, manifest, and ingest plan | source choice, preprocessing, and import staging stay deterministic |
| `ue5-photoreal-city-scene` | the city scene, lighting, materials, or camera policy needs UE5-level visual direction | no flat-card regressions and no fake skyline shortcuts |
| `android-visual-qa` | emulator navigation, screenshots, OCR, or template-driven tapping on libGDX surfaces | screen verification stops depending on guessed taps |
| `level-asset-curation` | importing city, structure, terrain, or texture assets | asset sourcing stays legal, documented, and mobile-safe |
| `android-3d-air-defense` | modifying gameplay, graphics, UI, Android behavior, or benchmarks | repo-specific game workflow is reused instead of re-derived |

## Rules
- Use UE5 planning docs before any new engine decision or city-data ingestion decision.
- Load `ue5-city-pipeline` before changing the pilot source, ingest manifest, or UE5 source catalog.
- Load `ue5-photoreal-city-scene` before changing camera policy, skyline composition, lighting direction, or photoreal quality targets.
- `caveman` is mandatory here by default, not opt-in.
- Repo-local default maps to `lite` behavior from upstream `v1.5.0`, even though upstream falls back to `full` when `CAVEMAN_DEFAULT_MODE` and config are unset.
- Upstream `off` mode exists, but this repo does not read env/config state or auto-write mode state.
- Upstream `wenyan-*` variants exist, but this repo keeps the mandatory default workflow in concise English.
- Caveman-style compression is limited to live AI prompting or conversational output; repo docs, README prose, and function descriptions stay in standard technical English.
- Compression changes phrasing only; it never reduces safety, verification, or uncertainty handling.
- Every external runtime asset must be reflected in `android/assets/ATTRIBUTION.md`.
- Every workflow or source-map change must update the relevant doc in `docs/`.
- Do not use background agents or subagents unless the user explicitly asks for them.
- Do not leak PATs, passwords, or internal-only URLs into external research tools.
