import { cleanup, render, screen, waitFor } from "@testing-library/react";
import "@testing-library/jest-dom/vitest";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, test, vi } from "vitest";

import { mtgGameEntry } from "../../main/frontend/mtg-entry.js";

class TestEventSource {
    onerror: (() => void) | null = null;

    constructor(readonly url: string) {
    }

    addEventListener() {
    }

    close() {
    }
}

describe("mtgGameEntry", () => {
    afterEach(() => {
        cleanup();
        vi.restoreAllMocks();
    });

    test("declares Magic: the Gathering identity and create route", () => {
        expect(mtgGameEntry.identity).toEqual({
            slug: "mtg",
            displayName: "Magic: the Gathering"
        });
        expect(mtgGameEntry.routes.createPath).toBe("/mtg/create");
    });

    test("create screen starts the game without options", async () => {
        const user = userEvent.setup();
        const onStartGame = vi.fn();
        const CreateScreen = mtgGameEntry.components.CreateScreen;

        render(<CreateScreen gameName="Magic: the Gathering" onStartGame={onStartGame} />);
        await user.click(screen.getByRole("button", { name: "Start" }));

        expect(onStartGame).toHaveBeenCalledWith();
    });

    test("play screen loads and ends the game", async () => {
        const user = userEvent.setup();
        const fetchMock = vi.fn()
            .mockResolvedValueOnce({
                ok: true,
                json: async () => ({
                    id: "MTG1234",
                    gameSlug: "mtg",
                    version: 1,
                    lifecycle: "active"
                })
            })
            .mockResolvedValueOnce({
                ok: true,
                json: async () => ({
                    id: "MTG1234",
                    gameSlug: "mtg",
                    version: 2,
                    lifecycle: "finished"
                })
            });
        vi.stubGlobal("fetch", fetchMock);
        vi.stubGlobal("EventSource", TestEventSource);
        window.history.pushState({}, "", "/g/MTG1234");
        const PlayScreen = mtgGameEntry.components.PlayScreen;

        render(<PlayScreen />);

        expect(await screen.findByText("Game active")).toBeInTheDocument();
        await user.click(screen.getByRole("button", { name: "End Game" }));
        expect(await screen.findByText("Game ended")).toBeInTheDocument();
        expect(fetchMock).toHaveBeenLastCalledWith(
            "/api/games/MTG1234/commands",
            expect.objectContaining({
                body: JSON.stringify({
                    type: "endGame",
                    expectedVersion: 1
                })
            })
        );
    });

    test("play screen disables end game button after the game ends", async () => {
        const fetchMock = vi.fn()
            .mockResolvedValueOnce({
                ok: true,
                json: async () => ({
                    id: "MTG1234",
                    gameSlug: "mtg",
                    version: 2,
                    lifecycle: "finished"
                })
            });
        vi.stubGlobal("fetch", fetchMock);
        vi.stubGlobal("EventSource", TestEventSource);
        window.history.pushState({}, "", "/g/MTG1234");
        const PlayScreen = mtgGameEntry.components.PlayScreen;

        render(<PlayScreen />);

        expect(await screen.findByText("Game ended")).toBeInTheDocument();
        await waitFor(() => {
            expect(screen.getByRole("button", { name: "End Game" })).toBeDisabled();
        });
    });
});
