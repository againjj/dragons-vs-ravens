# Magic: the Gathering Code Summary

## Overview

`mtg/` is the Magic: the Gathering game module. It currently owns a minimal placeholder application integration that can create a game and finish it through a single command, plus an early local rules engine and type-safe builder DSL for describing cards and focused engine tests.

The parent project has two child projects:

- `mtg/backend`
- `mtg/frontend`

## Backend Project

- `mtg/backend/build.gradle.kts`
  - Kotlin/JVM backend module with Java 21.
  - Depends on `:platform:backend` only.
- `src/main/kotlin/com/ayaziangames/mtg/MtgGameModuleDefinition.kt`
  - Magic: the Gathering implementation of the platform game module contract.
  - Declares the `mtg` slug and `/mtg/create` browser route.
- `src/main/kotlin/com/ayaziangames/mtg/MtgGameHandler.kt`
  - Implements the platform `GameHandler` port for Magic: the Gathering.
  - Creates an active placeholder game, validates `endGame` commands by version, finishes the game, and preserves platform-owned listing flags.
  - Supplies Magic: the Gathering public-listing display data and reports no player seats for the shared player-game menu because this placeholder has no seat ownership.
- `src/main/kotlin/com/ayaziangames/mtg/dsl/`
  - Owns the type-safe builder DSL for card databases, games, game states, and actions. DSL files are paired with the model files they build: `ActionDsl.kt`, `CardDsl.kt`, `CardDatabaseDsl.kt`, `GameDsl.kt`, and `GameStateDsl.kt`; `GenericDsl.kt` and `SingleAssignment.kt` hold shared DSL plumbing. Public use flows through the top-level DSL entrypoints and receiver-scope operations, while builder constructors and build helpers remain internal. Builder variables and state-setting DSL blocks are single-assignment, including the required card database `name`, and builder methods that only add items return no value. The DSL rejects duplicate card definitions, requires power/toughness to be specified together, builds identity-distinct deck card and permanent instances, resolves zone cards by index to existing deck instances, validates legal phase/step combinations, and enforces player-state constraints around required decks, life totals, active-player range, zone membership, card uniqueness across zones, and full deck coverage.
- `src/main/kotlin/com/ayaziangames/mtg/model/`
  - Owns immutable Magic: the Gathering model data for card definitions/databases, mana symbols, physical cards, games, game states, player states, zones, permanents, and player actions. Concrete model classes expose data-class-like `update` methods that return `this` when every supplied field is unchanged, and `serialize` methods that emit formatted Kotlin DSL code capable of rebuilding the model. Game serialization references the named card database through `availableCards = <cardDatabase.name>` rather than inlining definitions. Model classes are regular immutable classes except `PlayerAction` and `ActionCommand` subtypes, which remain data classes. The model stores library, battlefield, and hand as separate typed player fields whose concrete types inherit from `Zone`. A player action contains exactly one action command.
- `src/main/kotlin/com/ayaziangames/mtg/engine/`
  - Owns early Magic: the Gathering engine behavior. The current engine supports drawing cards during the active player's draw step and advances the game to first main phase.
- `src/test/kotlin/com/ayaziangames/mtg/`
  - Keeps backend tests aligned with the production source pairs. DSL tests are split by `ActionDsl`, `CardDsl`, `CardDatabaseDsl`, `GameDsl`, and `GameStateDsl`, with shared DSL setup in `DslTestSupport.kt`. Model update tests are split by the corresponding model areas instead of using a single aggregate model test.

## Frontend Project

- `mtg/frontend/build.gradle.kts`
  - Applies the shared frontend Gradle convention.
  - Typechecks and tests the Magic: the Gathering frontend package with Gradle-managed Node/npm.
- `src/main/frontend/mtg-entry.tsx`
  - Exports `mtgGameEntry` through the package entrypoint for the app-owned frontend shell.
  - Owns a no-options create screen and a play screen whose only game action is ending the game.
  - Uses shared frontend API failure classification so expired sessions and server-down states surface consistently, and closes the Magic: the Gathering SSE stream on errors.

## Boundaries

Magic: the Gathering does not depend on other game modules. Platform owns only generic game runtime behavior; Magic: the Gathering owns its command semantics and UI.
