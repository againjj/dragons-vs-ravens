import { describe, expect, test, vi, beforeEach } from "vitest";

import { createAppStore } from "../../main/frontend/app/store.js";
import { createSession } from "./fixtures.js";

const {
    createGameSessionMock,
    fetchGameSessionMock,
    sendGameCommandRequestMock
} = vi.hoisted(() => ({
    createGameSessionMock: vi.fn(),
    fetchGameSessionMock: vi.fn(),
    sendGameCommandRequestMock: vi.fn()
}));

vi.mock("../../main/frontend/game-client.js", () => ({
    createGameSession: createGameSessionMock,
    fetchGameSession: fetchGameSessionMock,
    sendGameCommandRequest: sendGameCommandRequestMock
}));

import { createGame, openGame, returnToLobby } from "../../main/frontend/features/game/gameThunks.js";

describe("gameThunks", () => {
    beforeEach(() => {
        createGameSessionMock.mockReset();
        fetchGameSessionMock.mockReset();
        sendGameCommandRequestMock.mockReset();
    });

    test("createGame enters the game view with the created session", async () => {
        const session = createSession({ id: "game-101" });
        createGameSessionMock.mockResolvedValue(session);
        const store = createAppStore();

        const loaded = await store.dispatch(createGame());

        expect(loaded).toBe(true);
        expect(store.getState().game.view).toBe("game");
        expect(store.getState().game.currentGameId).toBe("game-101");
        expect(store.getState().game.session?.id).toBe("game-101");
    });

    test("openGame enters the game view for a valid game id", async () => {
        const session = createSession({ id: "game-202" });
        fetchGameSessionMock.mockResolvedValue(session);
        const store = createAppStore();

        const loaded = await store.dispatch(openGame("game-202"));

        expect(loaded).toBe(true);
        expect(fetchGameSessionMock).toHaveBeenCalledWith("game-202");
        expect(store.getState().game.view).toBe("game");
        expect(store.getState().game.currentGameId).toBe("game-202");
        expect(store.getState().game.session?.id).toBe("game-202");
    });

    test("openGame keeps the requested game route active and shows feedback for an invalid game id", async () => {
        fetchGameSessionMock.mockRejectedValue(new Error("missing"));
        const store = createAppStore();

        const loaded = await store.dispatch(openGame("missing-game"));

        expect(loaded).toBe(false);
        expect(store.getState().game.view).toBe("game");
        expect(store.getState().game.currentGameId).toBe("missing-game");
        expect(store.getState().game.feedbackMessage).toBe('Unable to open game "missing-game".');
    });

    test("returnToLobby clears the active game session and local selection", async () => {
        const store = createAppStore({
            game: {
                view: "game",
                currentGameId: "game-303",
                session: createSession({ id: "game-303" })
            },
            ui: {
                selectedSquare: "a1"
            }
        });

        store.dispatch(returnToLobby());

        expect(store.getState().game.view).toBe("lobby");
        expect(store.getState().game.currentGameId).toBeNull();
        expect(store.getState().game.session).toBeNull();
        expect(store.getState().ui.selectedSquare).toBeNull();
    });
});
