# Implementation Plan

## Recommended Order

Implement these features in this order:

1. Multi-game support
2. Database persistence
3. User login and side assignment

This order fits the current codebase because the app is still modeled as a single shared in-memory game. The main architectural shift is turning that singleton into an addressable game resource. Persistence should be added after that so the database reflects the correct multi-game shape. Authentication and player ownership should come last because those rules depend on persisted game and seat data.

## Ticket Checklist

### Epic 1: Multi-Game Support

1. [x] Introduce a game store abstraction on the backend.
   - Goal: decouple game logic from singleton storage.
   - Change `GameSessionService.kt` so it depends on a `GameStore`-style interface instead of owning one `storedGame`.
   - Add a first in-memory implementation that stores multiple games by id.
   - Keep behavior identical for one game while refactoring internals.

2. [x] Make `GameSession` truly resource-oriented.
   - Goal: stop hardcoding `"default"`.
   - Update `GameModels.kt` so `id` is assigned per game creation path.
   - Add any small request/response models needed for game creation, such as `CreateGameRequest` or `CreateGameResponse`.

3. [x] Refactor service methods to be per-game.
   - Goal: every operation should accept `gameId`.
   - Change service methods from `getGame()`, `applyCommand(...)`, `createEmitter()` to `getGame(gameId)`, `applyCommand(gameId, ...)`, `createEmitter(gameId)`.
   - Keep version-conflict handling per game.
   - Scope SSE emitter lists per game instead of one global emitter list.

4. [x] Add per-game REST and SSE endpoints.
   - Goal: expose multi-game API shape.
   - Update `GameController.kt`.
   - Add:
     - `POST /api/games`
     - `GET /api/games/{gameId}`
     - `POST /api/games/{gameId}/commands`
     - `GET /api/games/{gameId}/stream`
   - Status: the new endpoints are live, and the old `/api/game` endpoints are intentionally still present as compatibility aliases for the default game so the current frontend can keep working during Milestone B.

5. [x] Add backend tests for game isolation.
   - Goal: prove two games do not interfere.
   - Extend `GameControllerTest.kt` and `GameSessionServiceTest.kt`.
   - Cover:
     - creating two games
     - mutating one game without affecting the other
     - version conflicts only within a single game
     - SSE snapshot delivery scoped to one game

6. [x] Thread `gameId` through the frontend transport layer.
   - Goal: client API calls target the selected game.
   - Update `game-client.ts` so load, command, and stream functions all take `gameId`.
   - Replace hardcoded `/api/game...` URLs with `/api/games/${gameId}...`.

7. [x] Add current-game selection to client state.
   - Goal: the app knows which game is open.
   - Update `gameSlice.ts` or add a small companion slice for `currentGameId`.
   - Update `gameThunks.ts` and `useGameSession.ts` to load and stream the active game only.

8. [x] Add a minimal create/open game UI.
   - Goal: make multi-game usable before building a full lobby.
   - Update `App.tsx` and add a dedicated lobby component instead of expanding `ControlsPanel.tsx`.
   - Implemented shape:
     - lobby at `/`
     - create game button
     - text input for a game ID or pasted link
     - open existing game by ID
     - active game route at `/g/{gameId}`
     - back-to-lobby path from an open game

9. [x] Add frontend tests for per-game routing.
   - Goal: keep client migration safe.
   - Extend `game-client.test.js` and related thunk tests.
   - Cover:
     - fetch URL includes game ID
     - command URL includes game ID
     - stream URL includes game ID
     - switching games reconnects correctly

10. [x] Update docs for the new game resource model.
   - Goal: avoid future confusion.
   - Update `README.md` and `docs/code-summary.md`.

### Epic 2: Database Persistence

11. Add database and migration dependencies.
    - Goal: introduce durable storage cleanly.
    - Update `build.gradle.kts`.
    - Recommended first pass:
      - Spring Data JDBC or JPA
      - Flyway
      - H2 for local and test
      - PostgreSQL driver for deploy

12. Add database configuration.
    - Goal: app can run locally and in deploy with persistent storage.
    - Update `application.properties`.
    - Add datasource settings with environment-variable overrides.

13. Create the initial games schema.
    - Goal: persist game state without over-modeling too early.
    - Add migration files under the standard resources migration path.
    - Recommended first schema:
      - `games` table
      - columns for `id`, `version`, `created_at`, `updated_at`
      - JSON or text columns for snapshot and undo snapshots
      - selected rule config and selected starting side columns

14. Implement a DB-backed game repository or store.
    - Goal: make database the source of truth for games.
    - Add repository and entity classes in the backend game module.
    - Replace the in-memory store with a DB-backed store behind the same interface.
    - Keep SSE emitters in memory. Only game state needs persistence.

15. Handle concurrency and versioning in persisted updates.
    - Goal: preserve optimistic locking semantics.
    - Enforce version checks in the database update path.
    - Make sure stale writes still surface as the existing `409` response behavior.

16. Add persistence integration tests.
    - Goal: prove save, load, and update behavior survives service restart boundaries.
    - Add repository tests and controller or service integration tests.
    - Cover:
      - create then reload game
      - command updates persist
      - undo snapshots persist
      - optimistic locking and version conflicts still work

17. Add a startup and reconnect smoke-test path.
    - Goal: ensure a user can reopen a persisted game after restart.
    - This can be an integration test or a manual checklist documented in the repo.

18. Document deployment data requirements.
    - Goal: make production persistence intentional.
    - Update `README.md` with DB environment variables and local run instructions.

### Epic 3: Login And Side Assignment

19. Add a user domain model.
    - Goal: establish identity before authorization rules.
    - Add user entity, repository, and basic service.
    - Keep fields minimal at first: id, username or email, password hash, createdAt.

20. Add Spring Security.
    - Goal: support authenticated sessions.
    - Update `build.gradle.kts` with security dependencies.
    - Add backend security configuration.
    - Recommended first pass: session-cookie auth, not JWT.

21. Add signup, login, and logout endpoints.
    - Goal: make auth usable end to end.
    - Add controller classes for auth flows.
    - Decide whether to support self-signup or seeded test users first.

22. Persist player-seat assignments on games.
    - Goal: connect users to sides.
    - Add schema changes for:
      - `dragons_user_id`
      - `ravens_user_id`
      - optional `created_by_user_id`
    - Update game models, repository, and store accordingly.

23. Add seat-claiming or invitation flow.
    - Goal: let a user join a side.
    - Recommended first pass:
      - creator makes a game
      - either side can be claimed if empty
      - unclaimed users are spectators
    - Add endpoints such as:
      - `POST /api/games/{gameId}/join`
      - or `POST /api/games/{gameId}/claim-side`

24. Enforce authorization on commands.
    - Goal: only the correct logged-in player can act.
    - In command handling, verify:
      - authenticated user is assigned to the active side
      - spectators cannot mutate
      - wrong-side users cannot mutate
    - Reads and SSE can remain public or authenticated depending on product choice.

25. Expose viewer identity and game seat info to the client.
    - Goal: frontend can render correct affordances.
    - Extend the game payload or add a companion endpoint with:
      - current user
      - dragons player
      - ravens player
      - viewer role for this game

26. Add login UI and auth state handling.
    - Goal: users can actually sign in and know who they are.
    - Update frontend state and add auth components.
    - Suggested location: a new auth feature folder under `src/main/frontend/features`.

27. Gate controls by user role and active side.
    - Goal: game UI reflects authorization rules.
    - Update selectors and components so only the assigned user on the active side can move.
    - Spectators should still see the board and history.

28. Add auth and authorization tests.
    - Goal: keep security behavior explicit.
    - Backend:
      - unauthenticated user cannot claim side if protected
      - wrong user cannot submit commands
      - correct user can act
    - Frontend:
      - controls disabled for spectator or wrong side
      - seat info rendered correctly

29. Document user and seat behavior.
    - Goal: make the multiplayer model clear.
    - Update `README.md` and `docs/code-summary.md`.

## Milestone Cuts

- Milestone A: tickets 1-5
  - Status: complete.
  - Backend multi-game API works, even before the UI catches up.
  - The legacy `/api/game` routes remain in place as default-game compatibility aliases until the frontend is migrated.
- Milestone B: tickets 6-10
  - Status: complete.
  - The browser now has a lobby at `/` and game URLs at `/g/{gameId}`.
  - The browser loads, commands, and SSE updates all target per-game endpoints.
  - Newly created games use 7-character IDs from the PLUS-code alphabet.
- Milestone C: tickets 11-18
  - Multi-game plus persistence.
- Milestone D: tickets 19-25
  - Auth and backend player ownership.
- Milestone E: tickets 26-29
  - Auth-complete user experience.

## Milestone B Implementation Summary

Milestone B migrated the browser from the default shared game to an explicit per-game flow while preserving the current React/Redux split:

- keep REST and SSE URL construction in `src/main/frontend/game-client.ts`
- keep shared session and current-game state in `src/main/frontend/features/game`
- keep `src/main/frontend/App.tsx` focused on shell composition
- keep `src/main/frontend/components` focused on rendering and user input

Delivered phases:

### Phase 1: Thread `gameId` through transport and stream helpers

Files to update:

- `src/main/frontend/game-client.ts`
- `src/main/frontend/features/game/gameStream.ts`
- `src/main/frontend/features/game/gameThunks.ts`
- `src/main/frontend/features/game/useGameSession.ts`

Changes:

- Update `fetchGameSession`, `sendGameCommandRequest`, and `openGameStream` to accept a `gameId`.
- Point those helpers at `/api/games/{gameId}`, `/api/games/{gameId}/commands`, and `/api/games/{gameId}/stream`.
- Add a small create-game client helper for `POST /api/games`.
- Keep the transport layer responsible for raw request and response handling, including `409` conflict behavior and SSE event parsing.

Notes:

- This is the narrowest first step because the backend contract already exists.
- Avoid leaving any hardcoded `/api/game` URLs in the active frontend path after this phase.
- It is fine to keep the compatibility backend routes in place for now, but the browser should stop depending on them.

### Phase 2: Add current-game state and lifecycle wiring

Files to update:

- `src/main/frontend/features/game/gameSlice.ts`
- `src/main/frontend/features/game/gameSelectors.ts`
- `src/main/frontend/features/game/gameThunks.ts`
- `src/main/frontend/features/game/useGameSession.ts`
- `src/main/frontend/app/store.ts`

Changes:

- Extend the game feature state with a `currentGameId` field and a small set of reducers for:
  - selecting a game id to open
  - marking that a new game is being created or loaded
  - clearing stale feedback when the active game changes
- Update thunks so load, create, and command submission all resolve against `currentGameId`.
- Update `useGameSession` so the load-and-subscribe effect depends on `currentGameId` and reconnects when that id changes.
- Reset or normalize browser-local selection when switching games so a square selected in one game never leaks into another.

Notes:

- Keep `currentGameId` in the existing game slice unless the state shape becomes awkward; Milestone B does not need a separate routing feature.
- The reconnect path should close the previous SSE stream before opening the next one.
- A missing or invalid game id should surface as a normal load error rather than leaving the app half-connected.

### Phase 3: Add a minimal lobby screen, game URLs, and game-screen return path

Files to update:

- `src/main/frontend/App.tsx`
- a new small component under `src/main/frontend/components` for the lobby screen
- `src/main/frontend/hooks/useGameRoute.ts`
- `src/main/resources/static/styles.css` if the existing panel styles need a small extension
- a small backend route-forward so direct loads of `/g/{gameId}` serve the frontend app shell

Changes:

- Add a separate lobby screen for session entry with:
  - create game button
  - text input for game id
  - open game button
- Add browser URL handling so:
  - `/` shows the lobby
  - `/g/{gameId}` shows the requested game
  - direct loads of `/g/{gameId}` work
- Add a game-screen affordance to leave the active session and return to the lobby.
- After creating a game, immediately switch the client to the returned game id and load that session.
- Preserve the existing board, controls, move list, and rules display on the game screen once a game is open.
- Show the active game id on the game screen so copying or re-opening a game is straightforward.
- Treat lobby and active-game views as explicit client state so `App.tsx` can render one screen or the other cleanly.

Notes:

- Keep this flow intentionally lightweight; Milestone B still does not need a full multiplayer lobby with discovery or presence.
- Prefer a dedicated lobby component rather than expanding `ControlsPanel.tsx`, which should stay focused on in-game controls.
- Returning to the lobby should close the active SSE stream and clear browser-local selection so cross-game state does not leak.
- Do not redesign the in-game layout beyond adding a small, clear way back to the lobby.

### Phase 4: Add frontend safety tests for per-game behavior

Files to update:

- `src/test/frontend/game-client.test.js`
- thunk or lifecycle tests under `src/test/frontend/*.test.ts(x)`
- component tests such as `src/test/frontend/controls-panel.test.tsx` if the create/open UI lives there

Add coverage for:

- fetch URLs include the active game id
- command URLs include the active game id
- stream URLs include the active game id
- creating a game stores and uses the returned id
- switching game ids reconnects SSE and loads the new session
- returning to the lobby disconnects the active stream and hides the game screen
- direct browser loads of `/g/{gameId}` open the requested game
- local square selection is cleared or normalized on game switch
- opening an invalid game id shows the expected error state

Notes:

- Keep tests behavior-focused. The important assertion is that the active game changes the client flow, not that a particular internal reducer fires.
- If `useGameSession` remains difficult to test directly, cover the same behavior through a component-level render test that mounts the real hook.

### Phase 5: Refresh docs after the frontend cutover

Files to update:

- `README.md`
- `docs/code-summary.md`

Changes:

- Update the runtime-flow section to say the browser now targets the per-game endpoints.
- Document the minimal create/open game flow in the UI.
- Clarify that the compatibility `/api/game` routes remain backend aliases, not the main browser path.

## Milestone B Exit Criteria

Milestone B is done when all of the following are true:

- a browser user can create a game from the UI
- a browser user can open an existing game by id
- a browser user can load a game directly at `/g/{gameId}`
- a browser user can leave an open game and return to the lobby screen
- initial load, commands, and SSE updates all target the selected game id
- switching from one game to another updates the rendered session and reconnects the stream cleanly
- frontend tests cover the per-game transport and switching path
- `README.md` and `docs/code-summary.md` describe the new browser behavior

## Milestone B Risks And Guardrails

- The biggest migration risk is stale cross-game client state. Treat `currentGameId` changes as a session boundary and clear or recompute any local UI state tied to one snapshot.
- The lobby-to-game transition should be driven from state, not ad hoc conditional rendering spread across multiple components.
- Keep command thunks dependent on the latest store state so commands cannot accidentally post to an old game after a fast switch.
- Avoid coupling lobby UI to gameplay controls more than necessary. A dedicated lobby component is safer than letting `ControlsPanel.tsx` absorb unrelated responsibilities.
- Preserve the current no-game and load-error UX wording where possible so the change stays focused on resource routing, not product copy.

## Scope Recommendations

- For persistence, start with storing the full game state as a JSON or text blob instead of normalizing turns, squares, and undo history into many tables.
- For authentication, start with server-side session cookies and open-seat claiming rather than JWT and invitations.
- For the first multi-game UI, favor a minimal create-or-open flow over a full lobby.

## Recommended First Deliverable

Build a thin vertical slice for multi-game support first:

- create a game
- open a game by id
- send commands to that game
- receive SSE updates for that game

That establishes the right resource model and makes the persistence and login work much cleaner afterward.

## Current Status

- Completed:
  - backend in-memory multi-game store
  - per-game service methods and SSE scoping
  - multi-game REST and SSE endpoints
  - backend isolation and compatibility tests
  - frontend per-game transport, state, and stream wiring
  - lobby-first browser flow with `/` and `/g/{gameId}` routes
  - direct-load server support for game URLs
  - 7-character PLUS-code-style game ids
  - frontend routing and lifecycle tests
  - docs updates for the multi-game browser flow
- Remaining before Milestone C is done:
  - add database and migration dependencies
  - add persistent game storage behind the existing store abstraction
  - preserve optimistic locking semantics in the persistence layer
  - document deployment data requirements
