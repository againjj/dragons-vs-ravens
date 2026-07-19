# Magic: the Gathering Agent Instructions

This file contains instructions for work inside the `mtg/` game module.

## Required Context

- Read the repository root `AGENTS.md` and `code-summary.md` first.
- Read this file and `mtg/code-summary.md` before changing this game module.
- If Magic: the Gathering work affects assembled app wiring or shared service boundaries, also read `app/AGENTS.md`, `app/code-summary.md`, `platform/AGENTS.md`, and `platform/code-summary.md`.

## Ownership

- `mtg/` owns Magic: the Gathering game-specific backend payloads, frontend UI, and tests.
- Keep Magic: the Gathering isolated from other game modules.
- Magic: the Gathering may depend on `platform/` for shared backend runtime contracts and on `@ayaziangames/platform-frontend` for shared frontend game-entry types.

## Gameplay Rules

- Creating a game starts an active placeholder game.
- The only supported command is `endGame`, which immediately finishes the game.
- Do not move Magic: the Gathering-specific behavior into `platform/` or `app/`.
