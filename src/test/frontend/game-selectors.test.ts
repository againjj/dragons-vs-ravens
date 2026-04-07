import { describe, expect, test } from "vitest";

import { createAppStore } from "../../main/frontend/app/store.js";
import { gameActions } from "../../main/frontend/features/game/gameSlice.js";
import { selectStatusText, selectTargetableSquares } from "../../main/frontend/features/game/gameSelectors.js";
import { uiActions } from "../../main/frontend/features/ui/uiSlice.js";
import { createSession } from "./fixtures.js";

describe("game selectors", () => {
    test("status text prefers feedback messages over snapshot-derived text", () => {
        const store = createAppStore({
            game: {
                session: createSession(),
                isSubmitting: false,
                loadState: "ready",
                connectionState: "open",
                feedbackMessage: null
            },
            ui: {
                selectedSquare: null
            }
        });

        store.dispatch(gameActions.feedbackMessageSet("Fullscreen is not available in this browser."));

        expect(selectStatusText(store.getState())).toBe("Fullscreen is not available in this browser.");
    });

    test("targetable squares come from the selected square and current snapshot", () => {
        const store = createAppStore({
            game: {
                session: createSession({}, {
                    phase: "move",
                    board: {
                        e5: "gold",
                        a1: "dragon",
                        b2: "raven"
                    }
                }),
                isSubmitting: false,
                loadState: "ready",
                connectionState: "open",
                feedbackMessage: null
            },
            ui: {
                selectedSquare: "a1"
            }
        });

        const targetableSquares = selectTargetableSquares(store.getState());

        expect(targetableSquares).toContain("c3");
        expect(targetableSquares).not.toContain("a1");
        expect(targetableSquares).not.toContain("b2");
        expect(targetableSquares).toHaveLength(78);
    });

    test("status text uses the updated setup copy", () => {
        const store = createAppStore({
            game: {
                session: createSession({}, {
                    phase: "setup"
                }),
                isSubmitting: false,
                loadState: "ready",
                connectionState: "open",
                feedbackMessage: null
            },
            ui: {
                selectedSquare: null
            }
        });

        expect(selectStatusText(store.getState())).toBe("Setup phase: place the pieces. Then start the game.");
    });

    test("status text omits the extra gold reminder during move phase", () => {
        const store = createAppStore({
            game: {
                session: createSession({}, {
                    phase: "move",
                    activeSide: "dragons"
                }),
                isSubmitting: false,
                loadState: "ready",
                connectionState: "open",
                feedbackMessage: null
            },
            ui: {
                selectedSquare: null
            }
        });

        expect(selectStatusText(store.getState())).toBe("Dragons to move.");
    });

    test("status text uses the generic capture copy during capture phase", () => {
        const store = createAppStore({
            game: {
                session: createSession({}, {
                    phase: "capture",
                    activeSide: "ravens"
                }),
                isSubmitting: false,
                loadState: "ready",
                connectionState: "open",
                feedbackMessage: null
            },
            ui: {
                selectedSquare: null
            }
        });

        expect(selectStatusText(store.getState())).toBe("Ravens moved. Capture a piece, or skip the capture.");
    });

    test("local selection can be updated independently of the shared session", () => {
        const store = createAppStore({
            game: {
                session: createSession({}, {
                    phase: "move",
                    board: {
                        a1: "dragon",
                        e5: "gold"
                    }
                }),
                isSubmitting: false,
                loadState: "ready",
                connectionState: "open",
                feedbackMessage: null
            },
            ui: {
                selectedSquare: null
            }
        });

        store.dispatch(uiActions.selectedSquareSet("a1"));

        expect(store.getState().ui.selectedSquare).toBe("a1");
    });
});
