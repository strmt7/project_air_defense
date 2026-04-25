# AI Agent Integrations

`AGENTS.md` is the universal baseline for this repository. Its pinned
Karpathy agent baseline is centralized there so each adapter can inherit the
same four-principle behavior without duplicating prompt text. It is adapted
from `forrestchang/andrej-karpathy-skills` at
`2c606141936f1eeef17fa3043a72095b4765b9c2`.

## Adapter Map

- Claude Code: `CLAUDE.md`
- Gemini CLI: `GEMINI.md`
- GitHub Copilot: `.github/copilot-instructions.md`
- Repo-local reusable workflows: `.agents/skills/`

## Rule

Adapter files may add harness-specific reminders, but they must not contradict `AGENTS.md`, the measured benchmark workflow, or the repo's asset-provenance and verification rules.
