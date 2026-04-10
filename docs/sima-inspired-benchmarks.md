# SIMA-Inspired Benchmarks

This game is not using DeepMind's SIMA system directly. The useful idea we are borrowing is the evaluation style:

- define short, atomic skills
- verify them consistently across runs
- keep the control vocabulary simple
- treat "works on one scene" as weaker evidence than "works across repeated short tasks"

The original Google DeepMind SIMA write-up is free to read:

- Blog: https://deepmind.google/blog/sima-generalist-ai-agent-for-3d-virtual-environments/
- Technical report: https://arxiv.org/abs/2403.04259

## Applied To This Game

We use the same high-level framing for battle QA:

1. Enter airspace from the menu.
2. Let battle initialize without freezing.
3. Spawn wave 1 and confirm hostile tracks appear.
4. Confirm interceptors launch against in-range threats.
5. Confirm at least one intercept or impact resolves within the short battle window.
6. Confirm HUD state matches the real wave state.

These are short-horizon skills, similar in spirit to the SIMA idea of evaluating many basic tasks instead of only one large success metric.

## Current Atomic Skills

- `enter airspace`
- `initialize battlespace`
- `spawn hostile track`
- `acquire track`
- `launch interceptor`
- `detonate within fuze radius`
- `apply impact to defended zone`
- `advance wave state`

## Why This Helps

- It keeps emulator QA concrete instead of vague.
- It makes regressions easier to isolate.
- It pushes the game toward readable controls and readable battle feedback.
- It gives us a repeatable benchmark layer while the visuals and rendering continue evolving.
