# AI Agent Skills

These repo-local skills are mandatory guidance surfaces for external agents.

## Precedence
1. `AGENTS.md`
2. `docs/reference/ai-agent-context-routing.md`
3. this file
4. matching skill in `.agents/skills/`
5. adapter files such as `CLAUDE.md`

## Skills
| Skill | Use when | Outcome |
| --- | --- | --- |
| `context-budget` | the task is broad or repetitive | context stays small and fast |
| `caveman` | any normal progress update or summary | token use stays low without losing proof |
| `caveman-help` | an agent or user needs the current caveman mode map | one-shot help without changing active mode |
| `verification-loop` | after non-trivial changes | verification is explicit and bounded |
| `level-asset-curation` | importing city, structure, terrain, or texture assets | asset sourcing stays legal, documented, and mobile-safe |
| `android-3d-air-defense` | modifying gameplay, graphics, UI, Android behavior, or benchmarks | repo-specific game workflow is reused instead of re-derived |

## Rules
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
