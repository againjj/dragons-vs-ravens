# Code Summary

## Overview

This project is a small Spring Boot 3.3 + Kotlin 2.1 web app that serves a browser-based board game prototype. The backend currently has no game logic or API endpoints; it mainly exists to host the static frontend. Frontend behavior is now split between a pure game-logic module and a DOM/rendering module.

## Current Architecture

- `src/main/kotlin/com/dragonsvsravens/DragonsVsRavensApplication.kt`
  - Minimal Spring Boot entrypoint only.
  - No controllers, services, repositories, or domain model classes yet.
- `src/main/resources/static/index.html`
  - Static shell for the game UI.
  - Loads `/styles.css` and `/app.js`.
- `src/main/resources/static/styles.css`
  - Owns layout, board sizing variables, responsive behavior, and fullscreen styling.
- `src/main/frontend/game.ts`
  - Source of truth for game rules and state transitions.
  - Exports types, state creation helpers, turn/capture logic, and board helper functions.
- `src/main/frontend/app.ts`
  - Browser/UI layer only.
  - Handles DOM lookup, rendering, event listeners, fullscreen interaction, and calls into `game.ts`.
- `src/test/frontend/game.test.js`
  - Frontend behavior tests for the extracted game logic module.
- `src/test/kotlin/com/dragonsvsravens/DragonsVsRavensApplicationTests.kt`
  - Verifies the Spring application context loads.

## Build And Runtime Flow

- Gradle is the primary build entrypoint.
- `build.gradle.kts` uses:
  - Spring Boot for serving the app.
  - Kotlin/JVM with Java 21.
  - `com.github.node-gradle.node` to download Node and npm automatically.
- Frontend build flow:
  - `npm run build` runs `tsc`.
  - `tsconfig.json` compiles `src/main/frontend/**/*.ts` into `build/generated/frontend`.
  - `processResources` depends on the frontend build and copies the generated JS into the app's `static` resources.
- Frontend test flow:
  - `npm run test` runs Node's built-in test runner against `src/test/frontend/**/*.test.js`.
  - Gradle task `testFrontend` runs the frontend tests.
  - `./gradlew test` runs both the frontend tests and the Kotlin/Spring test task.
- Result:
  - Running `./gradlew bootRun` serves the static HTML/CSS and compiled frontend modules through Spring Boot.

## Game Model

The board is represented as `Map<string, Piece>`, where the key is a square name like `e5`.

- `Piece = "dragon" | "raven" | "gold"`
- `Side = "dragons" | "ravens"`
- `Phase = "setup" | "move" | "capture"`

`GameState` currently contains:

- `board`
- `phase`
- `activeSide`
- `selectedSquare`
- `pendingMove`
- `turns`

Important implication: state is entirely in-memory and client-side. Reloading the page resets the game.

## Responsibilities By File

### `game.ts`

This file should remain the home for pure or mostly pure game behavior.

- Creates initial state with the gold at `e5`.
- Owns setup cycling logic.
- Owns turn transitions.
- Owns movement and capture resolution.
- Computes capturable and targetable squares.
- Formats move notation.

Most gameplay changes should start here.

### `app.ts`

This file should remain the browser integration layer.

- Holds the current `state` variable for the page session.
- Maps clicks to state transitions by calling functions from `game.ts`.
- Renders the board, move list, controls, and status text.
- Sizes the board responsively.
- Handles fullscreen support and resize observers.

Most UI-only changes should start here.

## Current Rules Implemented

### Setup phase

- The board starts with only the gold piece at `e5`.
- Clicking a square in setup cycles: empty -> dragon -> raven -> empty.
- `e5` is protected and cannot be changed during setup.

### Turn flow

- Starting the game switches phase from `setup` to `move`.
- Dragons always move first.
- On dragon turns, the player may move either a `dragon` piece or the `gold`.
- On raven turns, the player may move a `raven`.
- Movement currently allows moving a selected owned piece to any empty square on the 9x9 board.
  - There is no pathfinding, adjacency rule, collision rule beyond destination occupancy, or piece-specific movement constraint.

### Capture flow

- After a move, the code checks whether any capturable opposing piece exists anywhere on the board.
- If one exists, phase changes to `capture`.
- During capture:
  - Dragons may capture one `raven`.
  - Ravens may capture one `dragon` or the `gold`.
- Capture is optional because the UI exposes a "Skip Capture" button.
- Completing capture or skipping it commits the turn, appends to move history, and swaps the active side.

## Rendering Strategy

There is no framework. The UI is built with direct DOM manipulation.

- `render()` is the central refresh entrypoint.
- It calls:
  - `updateBoardSize()`
  - `renderBoard()`
  - `renderMoveList()`
  - `renderControls()`
  - `updateStatus()`

Key rendering details:

- Board labels are initialized once during startup.
- `renderBoard()` clears and rebuilds all 81 board buttons each render.
- Board clicks are handled with delegated click handling on the board container.
- Visual highlights are class-based:
  - `selected`
  - `targetable`
  - `capture-target`
- Move history is shown as simple notation like `a1-b2` or `a1-b2xc3`.

Because the board is rerendered from scratch, future UI changes should preserve the pattern of "update state -> call render()".

## Layout And UX Notes

- The board is a 9x9 CSS grid.
- CSS custom properties drive sizing and proportions.
- `updateBoardSize()` computes `--board-size` from the available container space.
- A `ResizeObserver` and `window.resize` listener keep the board responsive.
- The page supports fullscreen via `requestFullscreen()` on the `.page` element.
- Mobile/narrow layouts collapse the three-column layout into stacked sections.

## Testing Status

- Frontend game logic now has dedicated tests in `src/test/frontend/game.test.js`.
- The backend still has only a minimal Spring context test.
- The frontend tests currently cover:
  - initial state
  - setup cycling
  - protected gold square behavior
  - begin-game reset behavior
  - move-to-capture transitions
  - move commits when capture is unavailable
  - capture commits
  - skip-capture commits
  - targetable square calculation
  - move notation

## Extension Points For Future Changes

- To add or change gameplay rules:
  - Start in `src/main/frontend/game.ts`.
  - Update `src/test/frontend/game.test.js`.
- To change UI behavior or display:
  - Start in `src/main/frontend/app.ts`.
  - Update `src/main/resources/static/styles.css` if layout or styling is affected.
- To persist games or support multiplayer:
  - Add backend endpoints and decide whether the backend becomes the source of truth for rules.
- To support undo/redo or replay:
  - Expand the shape of `MoveRecord` or introduce a richer history model in `game.ts`.

## Constraints And Gotchas

- The backend currently does not validate gameplay because there is no API.
- The current implementation allows effectively teleporting a selected piece to any empty square.
- Capture eligibility is global, not positional; if any opposing capturable piece exists anywhere, capture mode begins.
- State remains browser-only and is lost on reload.
- `index.html` assumes the generated entry file remains `/app.js`.
- The frontend tests run against the compiled output in `build/generated/frontend`, so TypeScript build success is part of test success.

## Suggested Priorities Before Larger Feature Work

1. Decide whether future features are still client-only or whether rules/persistence should move partly to the backend.
2. Keep `game.ts` as the main home for new rule logic and avoid drifting behavior back into `app.ts`.
3. Expand frontend tests alongside any new move, capture, or win-condition logic.
4. Update this summary and `docs/codex-rules.md` when the architecture or workflow changes in meaningful ways.
