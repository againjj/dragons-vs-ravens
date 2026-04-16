# Refactor Plan

## Goal

Improve code organization without changing gameplay, auth behavior, transport behavior, or the existing UI flow. The refactor should make rule changes, frontend extensions, and future multiplayer/auth work easier to implement with less cross-file churn.

## Guardrails

- Preserve all current gameplay rules and UX behavior unless a separate change explicitly asks for behavior changes.
- Keep canonical gameplay logic in the Kotlin game module.
- Keep browser-local state in frontend UI state and selectors.
- Avoid broad rewrites. Prefer staged, reviewable refactors with passing tests after each stage.
- Update tests alongside structural changes only when imports, boundaries, or public behavior require it.

## Target Refactors

1. Split backend rule metadata from backend rule execution.
2. Split frontend `game.ts` into focused modules.
3. Deduplicate game-view and auth-refresh flow in frontend thunks.
4. Extract a dedicated game screen container from `App.tsx`.
5. Separate command application concerns from session/SSE infrastructure in `GameSessionService`.

## Recommended Order

1. Frontend `game.ts` split.
2. Backend `GameRules.kt` split.
3. Frontend thunk deduplication.
4. Frontend `App.tsx` extraction.
5. Backend `GameSessionService` split.

This order lowers risk because it starts with file-organization improvements that have smaller runtime consequences, then moves into heavier backend restructuring once the boundaries are clearer.

## Phase 1: Split Frontend `game.ts`

### Current Problem

`src/main/frontend/game.ts` currently mixes:

- wire types and DTOs
- board geometry helpers
- original-style move legality and capture simulation
- selection normalization
- move-history formatting

This gives the file too many reasons to change and makes import boundaries hard to reason about.

### Proposed File Layout

- `src/main/frontend/game-types.ts`
  - shared wire types and DTOs
  - enums/unions like `Piece`, `Side`, `Phase`, `ViewerRole`
  - request/response interfaces
- `src/main/frontend/board-geometry.ts`
  - column and row helpers
  - square-name helpers
  - board dimension helpers
  - center/corner/highlight helpers
- `src/main/frontend/game-rules-client.ts`
  - `sideOwnsPiece`
  - `canCapturePiece`
  - `getCapturableSquares`
  - original-style targeting and legality helpers
  - `normalizeSelectedSquare`
- `src/main/frontend/move-history.ts`
  - turn notation
  - grouped move-history row helpers
- `src/main/frontend/game.ts`
  - optional compatibility barrel during transition

### Execution Steps

1. Create `game-types.ts` and move type-only exports first.
2. Move generic board helpers into `board-geometry.ts`.
3. Move rules-oriented helper logic into `game-rules-client.ts`.
4. Move history formatting helpers into `move-history.ts`.
5. Decide whether `game.ts` should remain as a temporary re-export barrel.
6. Update imports incrementally across selectors, components, thunks, and tests.
7. Remove the compatibility barrel once all imports point at focused modules.

### Verification

- Run frontend tests that cover selectors, board behavior, move list formatting, and game-client types.
- Run full `./gradlew test` when the phase is complete.

### Risks

- Circular imports if the module boundaries are chosen carelessly.
- Over-centralizing exports again through an oversized barrel file.

### Mitigation

- Keep `game-types.ts` dependency-light.
- Make `board-geometry.ts` independent of Redux and browser code.
- Allow `game-rules-client.ts` to depend on `game-types.ts` and `board-geometry.ts`, but not vice versa.

## Phase 2: Split Backend `GameRules.kt`

### Current Problem

`src/main/kotlin/com/dragonsvsravens/game/GameRules.kt` currently owns:

- rule configuration catalog data
- UI-facing rule description content
- snapshot creation
- turn commit helpers
- free-play logic
- trivial logic
- original-style logic
- repetition and no-legal-move handling

That concentration makes every new rule or rule tweak touch a single oversized file.

### Proposed File Layout

- `src/main/kotlin/com/dragonsvsravens/game/RuleCatalog.kt`
  - rule configuration registry
  - summary/description content
  - preset board definitions
- `src/main/kotlin/com/dragonsvsravens/game/RuleEngine.kt`
  - shared interface currently represented by `RuleSet`
- `src/main/kotlin/com/dragonsvsravens/game/GameSnapshotFactory.kt`
  - idle/start/base snapshot creation
  - starting-side resolution
  - setup-cycle helpers
- `src/main/kotlin/com/dragonsvsravens/game/FreePlayRuleEngine.kt`
- `src/main/kotlin/com/dragonsvsravens/game/TrivialRuleEngine.kt`
- `src/main/kotlin/com/dragonsvsravens/game/OriginalStyleRuleEngine.kt`
- `src/main/kotlin/com/dragonsvsravens/game/GameRules.kt`
  - thin facade if preserving current call sites is useful during transition

### Execution Steps

1. Extract the `RuleSet` contract into a dedicated interface file.
2. Extract the static rule configuration records and summaries into `RuleCatalog`.
3. Extract snapshot-building functions into `GameSnapshotFactory`.
4. Move each ruleset implementation into its own file.
5. Leave `GameRules.kt` as a facade that delegates to the catalog/factory/engines.
6. Once stable, decide whether to keep the facade or update all callers to use the new boundaries directly.

### Verification

- Run `GameRulesTest`.
- Run controller and service tests that exercise command flow.
- Run full `./gradlew test`.

### Risks

- Breaking persistence-facing assumptions if snapshot creation or rule IDs drift.
- Accidentally changing behavior while moving shared helper functions.

### Mitigation

- Preserve current public APIs until tests are green.
- Move code in small commits or sub-phases with behavior tests unchanged.
- Keep rule IDs, summaries, preset boards, and starting-side defaults byte-for-byte consistent.

## Phase 3: Deduplicate Frontend Game View/Auth Sync in `gameThunks.ts`

### Current Problem

`src/main/frontend/features/game/gameThunks.ts` repeats:

- fetch game view
- update game slice
- patch auth session from the game view
- normalize selected square
- refresh after `401` and `403` responses

That duplication makes it easier for game-open, command, and seat-claim flows to drift apart.

### Proposed Refactor

Add dedicated internal thunk helpers such as:

- `applyFetchedGameView(view)`
- `loadAndApplyGameView(gameId)`
- `handleCommandAuthFailure(status)`

### Execution Steps

1. Extract a helper that applies `GameViewResponse` to both the game slice and auth slice.
2. Replace the repeated body in `loadGameView` and `refreshCurrentGameView`.
3. Extract common unauthorized/forbidden recovery logic used by `sendCommand` and `claimSide`.
4. Re-check whether `applyServerSession` should also optionally trigger a metadata refresh helper rather than duplicating orchestration in call sites.

### Verification

- Run `src/test/frontend/game-thunks.test.ts`.
- Run selector and stream tests if imports or flow sequencing change.
- Run full `./gradlew test`.

### Risks

- Slight sequencing changes between game state updates and auth state updates.
- Accidentally clearing UI selection at the wrong time.

### Mitigation

- Keep helper extraction behavior-preserving.
- Preserve the current order of dispatches unless a test proves a safer order is needed.

## Phase 4: Extract Game Screen Container from `App.tsx`

### Current Problem

`src/main/frontend/App.tsx` currently handles:

- auth bootstrap
- top-level shell chrome
- route switching
- fullscreen wiring
- board sizing wiring
- create-game navigation
- complete game-screen composition

This is still manageable, but it is now beyond the “top-level wiring” role described in the repo guidance.

### Proposed File Layout

- `src/main/frontend/components/AppChrome.tsx`
  - hero/header/actions if the shared top shell should be isolated
- `src/main/frontend/components/GameScreen.tsx`
  - game layout
  - board shell
  - controls panel
  - seat panel
  - rules legend
  - move list

At minimum, extract `GameScreen.tsx`. `AppChrome.tsx` is optional if the shell still feels too busy afterward.

### Execution Steps

1. Create `GameScreen.tsx` and move the large game-only render branch into it.
2. Pass only the data and callbacks that the extracted screen needs.
3. Keep route selection and auth bootstrap in `App.tsx`.
4. Reassess whether the header should also be extracted into a shell component.

### Verification

- Run React tests for controls, board, move list, seat panel, and routing.
- Run full `./gradlew test`.

### Risks

- Prop drilling if the extracted component receives too many callbacks.
- Moving store-aware code into a presentational component unintentionally.

### Mitigation

- Prefer a container component that is allowed to connect to Redux if that keeps interfaces smaller and clearer.
- Keep browser hooks where they naturally belong, rather than forcing everything upward.

## Phase 5: Split Command Handling from `GameSessionService`

### Current Problem

`src/main/kotlin/com/dragonsvsravens/game/GameSessionService.kt` combines:

- game creation/loading
- command routing
- authorization checks
- undo and side-claim logic
- persistence orchestration
- SSE emitter management
- stale-game cleanup

This makes the service a high-churn hub for unrelated changes.

### Proposed File Layout

- `src/main/kotlin/com/dragonsvsravens/game/GameCommandService.kt`
  - apply command
  - validate command requirements
  - side-claim logic
  - authorization helpers
  - undo helpers
- `src/main/kotlin/com/dragonsvsravens/game/GameSessionService.kt`
  - game creation/loading
  - emitter registration and broadcast
  - stale cleanup
  - thin orchestration across store and command service

An alternative is `GameCommandHandler.kt` if a lighter-weight helper object fits better than a service.

### Execution Steps

1. Extract pure/private command-validation helpers first.
2. Move `StoredGame` transition helpers related to command application with them.
3. Introduce a dedicated command service that accepts the current stored game and command, and returns the next stored game.
4. Keep SSE and stale cleanup in `GameSessionService`.
5. Update controller/service tests only where naming or wiring changes require it.

### Verification

- Run service and controller tests covering commands, authorization, and side claiming.
- Run full `./gradlew test`.

### Risks

- Blurring the boundary between persistence coordination and domain command logic.
- Making transaction/version-conflict handling harder to follow if responsibilities split poorly.

### Mitigation

- Keep optimistic concurrency and final `gameStore.put` orchestration in one place.
- Make the new command component return deterministic domain results without owning SSE broadcast or persistence retries.

## Cross-Cutting Test Plan

After each phase:

- run the most relevant focused tests first
- then run `./gradlew test`

Priority test areas:

- `src/test/kotlin/com/dragonsvsravens/game/GameRulesTest.kt`
- `src/test/kotlin/com/dragonsvsravens/game/GameSessionServiceTest.kt`
- `src/test/kotlin/com/dragonsvsravens/game/GameControllerTest.kt`
- `src/test/frontend/game.test.js`
- `src/test/frontend/game-thunks.test.ts`
- `src/test/frontend/game-selectors.test.ts`
- `src/test/frontend/board.test.tsx`
- `src/test/frontend/move-list.test.tsx`
- `src/test/frontend/app-routing.test.tsx`

## Suggested PR Strategy

Keep these as separate PRs or at least separate commits:

1. Frontend `game.ts` module split.
2. Backend rule-module split.
3. Frontend thunk cleanup.
4. `App.tsx` extraction.
5. `GameSessionService` split.

This makes review much easier and reduces the chance of hiding a behavior change inside structural movement.

## Definition Of Done

The refactor is complete when:

- the old oversized modules have been split along the planned boundaries
- imports are updated cleanly with no compatibility clutter left behind unless intentionally retained
- all relevant tests pass
- `docs/code-summary.md` and `README.md` are updated if the final structure changes enough to make the current documentation stale
- no gameplay, auth, routing, or SSE behavior has changed unintentionally

## Recommended First Move

Start with the frontend `game.ts` split. It has the clearest seams, the least backend risk, and it will clarify naming and dependency boundaries before touching larger service and rules refactors.
