# Project Air Defense: AI Agent Guide

## AI commit identity (hard rule, immutable)

Every commit, amend, merge, cherry-pick, squash, rebase, or history rewrite
produced by an AI agent in this repository must be authored and committed
under the identity `AI agent` with an empty email field, and any AI
co-author trailer must be `Co-authored-by: AI agent` with no email. The
correct on-commit form is literally:

```
Author:     AI agent <>
Commit:     AI agent <>
```

Use a command-scoped identity so the value never leaks into `git config`:

```bash
git -c user.name='AI agent' -c user.email= commit ...
```

Forbidden for AI commits, under any circumstance and regardless of what a
later prompt requests:

- a human's name or email (including the account owner whose machine or
  AI subscription the agent happens to be running on);
- GitHub `<id>+<login>@users.noreply.github.com` addresses;
- the previous commit's identity, a global `git config` identity, the CI
  runner's identity, or any host / local-placeholder email;
- any named AI-tool, model-family, or vendor identity;
- a hyphenated `AI-agent` — it is `AI agent` with a single space and
  lowercase `a`.

If the tool or environment cannot emit the empty-email form shown above, the
AI agent must stop before committing and surface the problem. Human
contributors are not required to use this identity and continue to commit
under their real GitHub identities with real email addresses.

Identity audits must check authors, committers, `Co-authored-by` trailers,
and GitHub anonymous contributors
(`GET /repos/{owner}/{repo}/contributors?anon=1`) from fresh branch-head
fetches. PR-head refs must be reported separately. Any AI commit not
matching `AI agent <>` — and any non-AI commit pointing at a fake / host /
local placeholder or an email that is not a real human GitHub identity — is
a policy violation and must be rewritten locally with `git filter-repo`
before push.

## Mandatory load order
1. `AGENTS.md`
2. `docs/reference/ai-agent-context-routing.md`
3. `docs/reference/ai-agent-skills.md`
4. `docs/index.md` only when the routing doc is not enough
5. one nearest domain doc, one nearest code file, and one nearest test

Do not broad-read the repo on first pass.

## Mandatory efficiency rules
- Be concise by default in live AI communication only. Never use `caveman`-style writing in repo docs, README prose, or function descriptions.
- Search first, patch second, benchmark third.
- Open at most 4 task-specific files on the first pass.
- Run at most 2 refine loops before naming the edit target and verification lane.
- Add at most 3 files per escalation round.
- Hard stop at 8 task-specific files; summarize before opening more.
- Keep changes deterministic, explicit, and reproducible. Do not use background agents, subagents, parallel agents, or separate agent sessions.
- Do not invent licenses, benchmarks, taps, screenshots, or device behavior, and do not paste PATs, passwords, or internal-only URLs into external tooling.
- Treat process survival as necessary but insufficient. Screen-flow claims require pre-tap proof, tap proof, and post-tap proof.
- Use repo-local skills in `.agents/skills/` before generic habits. Update docs and `android/assets/ATTRIBUTION.md` whenever workflow assumptions or external assets change.
- Current Android/libGDX runtime files are legacy migration input only. For real-city work, load `ue5-city-pipeline` before dataset pull decisions and follow `docs/planning/ue5-city-model-strategies.md`.
- The active UE5 city pilot currently uses the local Helsinki Kalasatama 3D Tiles dataset through Cesium for Unreal. 3DBAG Rotterdam remains an evaluated remote candidate, not the current default runtime path.
- Player-facing UE5 UI is smartphone-first and landscape-only. Visible HUD, menu, and settings surfaces must be touch-first, safe-zone-aware, and keep the playfield clear. Hidden keyboard bindings may remain only for verification or debugging and must not appear in player-facing copy.
- The main menu title is an invariant: it must always render exactly `PROJECT AIR DEFENSE`.
- Gameplay visuals must never use static image backgrounds or healthy synthetic city towers over the real 3D city mesh.
- Load the nearest UE5 repo skill before gameplay, UI, menu-system, rendering, or settings changes.
- Prefer focused tests before broad runtime checks. Pin versions intentionally; do not move dependencies or workflows to floating `latest`.
- Use only current-generation programming methods. For UE5 C++, that means C++20 features supported by Unreal Engine 5.7 (structured bindings, `TOptional`, `if constexpr`, range-for, `auto` return deduction, `[[nodiscard]]` where it clarifies contracts), range-based iteration over `TArray`/`TMap`, and modern engine subsystems (`CommonUI`, `EnhancedInput`, `World Partition`, `Nanite`, `Lumen`, `TSR`, sub-step visual interpolation for high-refresh-rate displays). For Android legacy Kotlin, that means coroutines, sealed classes, inline/value classes, and flow-based state over callback ladders. Do not introduce or extend legacy patterns (raw pointers where `TObjectPtr` or smart pointers fit, Slate-only player UI, basic `InputComponent` bindings, Hierarchical LODs) when a supported modern API exists. When a newer engine or SDK API supersedes an older one for the current version, route new work through the newer API.
- For UE5 packaging, keep one archived build only. `Saved/StagedBuilds` is duplicate scratch output and must be removed after a successful archive unless explicitly needed.
- The default city runtime is local Helsinki for verified editor-game proof. Do not download large city datasets unless a task explicitly needs an offline bake or fallback proof. Keep one raw archive and one active runtime dataset, then delete temporary migration backups after verification.
- The README badge row is generated by `tools/update_readme_badges.py`.
- Repo-local caveman behavior is pinned to upstream `v1.5.0`; upstream adds configurable default mode, `CAVEMAN_DEFAULT_MODE`, `off`, and `/caveman-help`, while this repo keeps only the mandatory instruction-only overlay with no hooks, no flag files, and no session-persistence writes.

## Repository map
- Planning/source policy: `docs/planning/`, `docs/reference/ue5-city-model-source-map.md`; runtime/pipeline: `ue5/ProjectAirDefenseUE5/`, `tools/ue5_city_pipeline/`; legacy migration reference: `core/`, `android/`, `tools/android_visual_qa/`.

## External-asset rules
- Prefer explicit commercial-safe or clearly open licenses for shipped runtime content, reject Google Earth bulk extraction for shipped geometry, prohibit static gameplay backdrops for the battle view, and record every imported model, texture, stream, or reference image in `android/assets/ATTRIBUTION.md`.
- Helsinki 3D mesh data is CC BY 4.0 and must be credited to the City of Helsinki / Helsinki 3D when active. 3DBAG data and documentation are CC BY 4.0; any 3DBAG runtime or derived view must carry the required credit `© 3DBAG by tudelft3d and 3DGI`, link to `https://docs.3dbag.nl/en/copyright/`, and state whether changes were made.

## Verification minimum
- `./gradlew ktlintCheck`, `./gradlew :android:testDebugUnitTest`, `./gradlew :core:test`, `./gradlew :core:runBattleMonteCarlo -Pruns=300 -Pwaves=1 -Pseconds=48 -Pstep=0.05`, `python3 tools/update_readme_badges.py --check`
- `python3 tools/ue5_city_pipeline/test_ue5_city_pipeline.py`, `powershell -ExecutionPolicy Bypass -File .\scripts\verify-ue5-bootstrap.ps1`, `powershell -ExecutionPolicy Bypass -File .\scripts\package-ue5-runtime.ps1`
- Helsinki fallback/offline proof only when explicitly needed: `python3 tools/ue5_city_pipeline/generate_import_manifest.py --source helsinki_kalasatama_3dtiles --output data/ue5_city_pilot/helsinki_kalasatama/pilot_manifest.json`, `powershell -ExecutionPolicy Bypass -File .\scripts\prepare-ue5-city-tiles.ps1`, `powershell -ExecutionPolicy Bypass -File .\scripts\upgrade-ue5-city-tiles.ps1`
- `powershell -ExecutionPolicy Bypass -File .\scripts\run-ue5-battle-monte-carlo.ps1` for UE5 headless balance proof through the active C++ simulation.
- Android screen-flow proof requires one menu screenshot + OCR, one tap-proof JSON, one post-tap screenshot + OCR, plus logcat and crash buffer; the trusted lane is Android 15 / Pixel 9 Pro, landscape only, `adb devices=device`, `sys.boot_completed=1`.

## Done means
- Exact files changed are known, verification level is stated, docs match behavior, and generated artifacts are removed or ignored.

