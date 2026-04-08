import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, test, vi } from "vitest";

import { LobbyScreen } from "../../main/frontend/components/LobbyScreen.js";
import { renderWithStore } from "./test-utils.js";

describe("LobbyScreen", () => {
    test("creates a game and opens a typed game id", async () => {
        const user = userEvent.setup();
        const onCreateGame = vi.fn();
        const onOpenGame = vi.fn();

        renderWithStore(
            <LobbyScreen
                feedbackMessage={null}
                isLoading={false}
                onCreateGame={onCreateGame}
                onOpenGame={onOpenGame}
            />
        );

        await user.click(screen.getByRole("button", { name: "Create Game" }));
        await user.type(screen.getByLabelText("Game ID"), "game-606");
        await user.click(screen.getByRole("button", { name: "Open Game" }));

        expect(onCreateGame).toHaveBeenCalledTimes(1);
        expect(onOpenGame).toHaveBeenCalledWith("game-606");
    });

    test("renders feedback text from the lobby state", () => {
        renderWithStore(
            <LobbyScreen
                feedbackMessage='Unable to open game "missing-game".'
                isLoading={false}
                onCreateGame={vi.fn()}
                onOpenGame={vi.fn()}
            />
        );

        expect(screen.getByText('Unable to open game "missing-game".')).toBeInTheDocument();
    });
});
