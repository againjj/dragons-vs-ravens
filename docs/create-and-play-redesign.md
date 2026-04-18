# Create And Play Redesign Plan

## Goal

Redesign the flow so game creation happens in a dedicated client-only draft experience at `/create`, while active play remains on persisted game routes at `/g/{gameId}`.

This plan assumes:

- `/create` is local-only state.
- No game id exists while the user is on `/create`.
- Nothing from `/create` is saved server-side until `Start Game` is clicked.
- Reloading or leaving `/create` loses the draft.

## Desired UX

### Creation flow

1. From the lobby, clicking `Create Game` navigates to `/create`.
2. The `/create` screen shows three panels:
   - board on the left
   - configuration controls in the middle
   - rules panel on the right
3. Rules configuration can be changed before a game exists.
4. If `Free Play` is selected, the board can be edited immediately.
5. If the selection changes away from `Free Play`, the free-play board setup is discarded.
6. Clicking `Start Game` creates the persisted game, assigns a game id, and navigates to `/g/{gameId}`.

### Live game flow

1. The game page keeps the board on the left.
2. The center panel becomes the move list.
3. The right panel becomes the rules panel.
4. Seat ownership moves into the header as a single line below the existing header information.
5. The buttons currently in the rules panel move to the top of the move-list panel.
6. Those buttons stay outside the scrollable move-list content.
7. A user can claim both sides by pressing claim twice when both seats are open.
8. Even after claiming one side, the same user can still claim the other side later.
9. If one user owns both sides, undo should always be available.
10. When the game is over, the header status line should explain who won or why the game was drawn.

## Current State Summary

### Routing

- The app currently routes between login, lobby, profile, and `/g/{gameId}`.
- There is no `/create` route today.
- Clicking `Create Game` currently calls the server immediately and then opens `/g/{gameId}`.

### State ownership

- Game setup for a new game is currently server-backed from the beginning.
- The Redux game slice assumes the active non-lobby experience is a persisted game.
- Rule selection, board size, and starting side are stored on the server-backed session.

### Layout

- The active game screen currently places controls and seats in the middle/right area and the move list in a lower panel.
- Seats are rendered as their own section with a `Seats` heading.
- Action buttons are mixed into the sidebar instead of being attached to the move-list header.

### Seat and undo rules

- The backend currently forbids one user from claiming both sides.
- Frontend claim-button selectors also assume a user with one side cannot claim the other.
- Undo currently depends on the side that made the last move.

## Proposed Architecture

### Frontend state split

Keep persisted game state and draft creation state separate.

- Persisted game state stays in `features/game` and continues to mirror `/api/games/{id}`.
- Add a draft-create state owned by the frontend only.
- The draft should track:
  - selected rule configuration id
  - selected starting side
  - selected board size
  - local draft snapshot for board rendering
  - whether the draft board is editable
- The draft snapshot should be rebuilt locally when configuration changes.

This preserves the current rule that canonical game logic belongs in the backend for persisted games, while allowing the pre-game draft to be ephemeral and local.

### Route model

Add a dedicated create route to the frontend and backend route forwarding.

- Add `/create` to Spring’s forwarded app routes.
- Extend the route hook to recognize `/create`.
- Treat `/create` as a separate app page rather than pretending it is an active game.
- Leaving `/create` should clear the draft state.

### Screen split

Separate the creation screen from the live game screen.

- Keep `App.tsx` focused on top-level page selection.
- Add a dedicated create screen component for `/create`.
- Keep `GameScreen.tsx` focused on the live persisted game route.
- Reuse presentational pieces where practical:
  - board rendering
  - rules rendering
  - shared button groups where the behavior is the same

### Server boundary

Do not persist draft-create state before `Start Game`.

- Avoid introducing a server-side draft lifecycle.
- Prefer a single create request at `Start Game` that includes enough information to seed the initial game state.
- Keep `/api/games/{id}/commands` for persisted games only.

## Detailed Work Plan

### Phase 1: Routing and page model

Add the route and page scaffolding first.

- Update the route controller to forward `/create` to the frontend app.
- Update the route hook page model to include `create`.
- Update route synchronization so direct navigation to `/create` works.
- Change the lobby `Create Game` button to navigate to `/create` instead of calling `POST /api/games`.
- Ensure auth redirect behavior preserves `/create` as a valid post-login destination.

Expected files:

- `src/main/kotlin/com/dragonsvsravens/AppRoutesController.kt`
- `src/main/frontend/hooks/useGameRoute.ts`
- `src/main/frontend/App.tsx`
- `src/main/frontend/components/LobbyScreen.tsx`
- `src/test/frontend/app-routing.test.tsx`
- `src/test/frontend/lobby-screen.test.tsx`

### Phase 2: Draft-create state

Add local-only creation state in Redux or a tightly scoped frontend slice.

- Introduce actions/selectors for:
  - entering create mode with default draft values
  - updating selected ruleset
  - updating starting side
  - updating board size
  - cycling draft setup squares in free play
  - clearing the draft when leaving `/create`
- Build local draft snapshots from the same frontend helper types used for rendering.
- Restrict board editing to the `free-play` draft ruleset.
- On ruleset change away from `free-play`, discard the draft setup and rebuild the snapshot from the chosen ruleset defaults.
- Keep browser-local selection state separate from the draft snapshot itself.

Expected files:

- `src/main/frontend/features/game/gameSlice.ts` or a new dedicated draft slice
- `src/main/frontend/features/game/gameSelectors.ts`
- `src/main/frontend/game-types.ts`
- `src/main/frontend/game-rules-client.ts`
- `src/main/frontend/board-geometry.ts` if needed
- frontend tests covering draft behavior

### Phase 3: Create screen UI

Build the new `/create` screen around the draft state.

- Add a new component, likely `CreateGameScreen.tsx`.
- Layout the page into three panels:
  - board
  - configuration controls
  - rules
- Reuse the existing board component if possible, but feed it draft selectors/actions instead of server commands.
- Extract or adapt the pre-game section of `ControlsPanel` so it can drive local draft state instead of persisted commands.
- Show the same rules-description content on the right based on the selected ruleset.
- Place the `Start Game` button in the middle configuration panel.

Expected files:

- `src/main/frontend/components/CreateGameScreen.tsx`
- `src/main/frontend/components/Board.tsx`
- `src/main/frontend/components/ControlsPanel.tsx` or an extracted create-controls component
- related CSS in `src/main/resources/static/styles.css`
- new frontend component tests

### Phase 4: Create request handoff

Create the real game only when `Start Game` is clicked.

- Extend `CreateGameRequest` and the backend create flow so the request can carry:
  - selected rule configuration id
  - selected starting side
  - selected board size
  - optional free-play board state or equivalent setup payload
- On `Start Game`, send the draft configuration to `POST /api/games`.
- Have the backend create the initial persisted session from that payload.
- After success:
  - store the returned session
  - navigate to `/g/{gameId}`
  - load the auth-aware game view if needed
- On failure, keep the user on `/create` and surface the error without losing the draft.

Backend notes:

- The backend should stay the source of truth for the stored game snapshot.
- Free-play creation should seed the initial persisted snapshot to match the draft board.
- Preset rulesets should create their normal initial game state without any extra draft board payload.

Expected files:

- `src/main/frontend/game-client.ts`
- `src/main/frontend/features/game/gameThunks.ts`
- `src/main/kotlin/com/dragonsvsravens/game/GameController.kt`
- `src/main/kotlin/com/dragonsvsravens/game/GameSessionService.kt`
- `src/main/kotlin/com/dragonsvsravens/game/GameSessionFactory.kt`
- `src/main/kotlin/com/dragonsvsravens/game/GameModels.kt`
- controller/service tests for create payload handling

### Phase 5: Live game layout redesign

Reshape the active game screen after create flow is in place.

- Update `GameScreen.tsx` to use:
  - left: board
  - center: move list
  - right: rules
- Move live-game action buttons out of the rules panel and into a fixed header area at the top of the move-list panel.
- Keep the move history list as the only scrollable area in the center panel.
- Move seat ownership into the header as a single inline line beneath the title/status.
- Remove the dedicated `Seats` section heading from the live page.

Potential component refactor:

- Keep `MoveList.tsx` focused on history rows and scrolling.
- Add a move-list shell/header component if needed so buttons are structurally outside the scroll container.
- Replace `SeatPanel.tsx` on the live page with a compact header row component, or refactor `SeatPanel` into reusable subparts.

Expected files:

- `src/main/frontend/components/GameScreen.tsx`
- `src/main/frontend/components/MoveList.tsx`
- `src/main/frontend/components/SeatPanel.tsx` or a replacement header component
- `src/main/resources/static/styles.css`
- layout-focused frontend tests

### Phase 6: Dual-seat claiming and undo rules

Update seat ownership rules for live games.

- Remove the backend restriction that prevents one user from claiming both sides.
- Keep claim validation so a side still cannot be taken if another user already owns it.
- Update viewer-role and claim-availability selectors so a user who owns one side can still claim the other if it is open.
- Update the live-game seat UI so the second claim action is still visible when appropriate.
- Update undo availability:
  - when two different users own the sides, keep the current last-mover restriction
  - when the same user owns both sides, allow undo whenever undo is otherwise available

Expected files:

- `src/main/kotlin/com/dragonsvsravens/game/GameCommandService.kt`
- `src/main/frontend/features/game/gameSelectors.ts`
- `src/main/frontend/components/SeatPanel.tsx` or replacement header actions
- backend and frontend tests for dual-seat claim and undo behavior

### Phase 7: Finished-game status messaging

Improve game-over messaging in the header.

- Extend or reuse move-history/game-over metadata so selectors can derive the exact game-over reason.
- Update the header status selector to prefer:
  - winner messaging when one side wins
  - explicit draw messaging when the game ended in a draw
  - manual-end wording if that remains a supported outcome
- Keep the game-over row in the move list aligned with the same source of truth.

Expected files:

- `src/main/frontend/features/game/gameSelectors.ts`
- `src/main/frontend/move-history.ts`
- potentially backend turn/game-over payload helpers if extra structure is needed
- tests covering winner and draw status text

## Testing Plan

### Routing tests

- Direct navigation to `/create` loads the create screen.
- Navigating from lobby to `/create` does not create a game immediately.
- Login redirect preserves `/create` and returns there after auth.
- Leaving `/create` for `/lobby` or `/profile` clears the draft.

### Create flow tests

- Default create draft loads with the expected initial ruleset.
- Selecting `Free Play` enables immediate board editing.
- Editing the board in `Free Play` updates only local draft state.
- Switching from `Free Play` to another ruleset discards the custom setup.
- Clicking `Start Game` sends one create request and then navigates to `/g/{id}`.
- Failed create requests keep the user on `/create` with the draft intact.

### Live-game UI tests

- Move-list action buttons render above the scrollable move history.
- Seats render inline in the header without a `Seats` heading.
- Rules remain visible in the right panel.
- Game-over status text shows the winner or draw reason.

### Seat and undo tests

- A user may claim dragons and later claim ravens if ravens remain open.
- A user may claim ravens and later claim dragons if dragons remain open.
- Another user still cannot steal an already-claimed side.
- Undo is available for same-user dual ownership whenever undo snapshots exist.
- Undo remains restricted for split ownership.

### Backend tests

- `POST /api/games` accepts the new create payload.
- Free-play creation seeds the requested board correctly.
- Preset ruleset creation still builds the expected canonical starting state.
- Claim-side behavior allows same-user dual ownership.

## Risks And Decisions

### Risk: draft logic drifting from server rules

Because `/create` is local-only, draft rendering can drift from server-created state if the frontend invents too much setup logic.

Mitigation:

- Keep draft behavior as thin as possible.
- Reuse existing frontend helper logic only for rendering and free-play square cycling.
- Push the final persisted-state construction back through the backend create API.

### Risk: overloading the current game slice

The existing game slice assumes a persisted game route.

Mitigation:

- Prefer a dedicated draft-create slice or a clearly separated draft branch of state.
- Avoid mixing draft ids, draft sessions, and persisted sessions in the same fields.

### Risk: layout refactor affecting responsiveness

Moving controls and seats around can break the current board sizing and mobile behavior.

Mitigation:

- Keep the board panel structure compatible with `useBoardSizing`.
- Add responsive layout tests where possible.
- Verify the move-list header stays fixed while the history scrolls.

## Suggested Delivery Order

1. Add `/create` routing and page selection.
2. Add local draft-create state and selectors.
3. Build the `/create` screen.
4. Update the create API payload and `Start Game` handoff.
5. Redesign the live game layout.
6. Enable dual-seat claiming and adjust undo behavior.
7. Update finished-game status messaging.
8. Run full frontend and backend test coverage for the redesign.

## Out Of Scope

The following are not part of this redesign unless requested separately:

- persisting unfinished `/create` drafts
- collaborative shared pre-game setup before a game id exists
- gameplay rule changes beyond the explicitly requested seat/undo UX behavior
- broader visual redesign beyond the requested panel/header/button movement
