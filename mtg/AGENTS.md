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

## Development Guardrails

- Treat the current app integration as a placeholder, not the rules engine. The platform-facing game still only creates an active game and supports `endGame`; richer Magic: the Gathering model, DSL, and engine work should evolve behind that boundary until it is deliberately connected to gameplay.
- Model state should be immutable and identity-aware. Concrete model classes should expose `update(...)` methods that behave like data-class `copy(...)`, but return `this` when unchanged. Reference fields should use object identity checks; scalar fields may use value equality.
- Preserve physical card identity. Zone movement must operate on actual deck card instances, not just card names or definitions. Serialized zones should continue using `deckCard(index)` so duplicate card names still refer to distinct physical cards.
- Enforce invariants at DSL/model construction boundaries. The DSL should reject illegal or incomplete states early, including duplicate definitions, missing required fields, invalid phase/step pairs, active player out of range, zone cards from the wrong deck, duplicate zone membership, and missing deck cards.
- Keep the DSL type-safe and scoped. Follow the Kotlin type-safe builder pattern already used in `mtg/backend/src/main/kotlin/com/ayaziangames/mtg/dsl`; builders should have internal constructors/build methods, use `@MtgDsl`, and avoid exposing mutation paths outside the intended receiver scope.
- New DSL items need to be scoped as tightly as possible. Add new entrypoints, receiver functions, builder methods, and visible types only to the smallest DSL context where they are valid.
- Use single-assignment for declarative DSL fields. Required or declarative properties like card database `name`, player `deck`, `life`, phase, and step should stay single-assignment so fixtures read like canonical game descriptions rather than mutable scripts.
- Every possible state that the engine produces must be generateable by the DSL. If a change would make the engine produce a state that cannot be described by the DSL, stop and ask how that state should be represented.
- Keep model, state, and engine responsibilities separate. Model classes own immutable data shape and model-local helpers; state/DSL builders own construction and validation of representable states; the engine owns legal action validation and state transitions.
- Pair production files with focused tests. As the model grows, tests should continue mirroring production areas, such as `CardDslTest`, `GameStateDslTest`, and `GameUpdateTest`, instead of returning to broad catch-all Magic: the Gathering tests.
- Pin serialization as a public contract. `serialize()` emits Kotlin DSL meant to reproduce the game, so formatting and ordering matter. Keep exact-string tests for representative fixtures, especially the engine test game.
- Engine actions should be narrow, validated, and logged. Each supported command should validate player, phase, step, resource, and state preconditions before changing state. Successful actions should append the original `PlayerAction` to the game log.
- Additional actions should be assumed invalid except in contexts where they are explicitly stated to be valid. If the valid context for an action is not mentioned in the request or existing rules, ask for clarification before implementing it.
- Grow rules by adding explicit command/model concepts, not ad hoc branches. Future Magic: the Gathering behavior should add small typed commands and model fields as needed, with tests describing the rule transition.
- Do not persist or expose derived Magic: the Gathering state as canonical state. Store canonical rule/action state, and derive presentation labels, availability, prompts, and summaries at the read/view/client layers.
- Update `mtg/code-summary.md` when Magic: the Gathering conventions change, especially around DSL shape, identity, serialization, model/state/engine boundaries, and test layout.
