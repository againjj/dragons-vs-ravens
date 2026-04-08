import { useState } from "react";

interface LobbyScreenProps {
    feedbackMessage: string | null;
    isLoading: boolean;
    onCreateGame: () => void;
    onOpenGame: (gameId: string) => void;
}

export const LobbyScreen = ({
    feedbackMessage,
    isLoading,
    onCreateGame,
    onOpenGame
}: LobbyScreenProps) => {
    const [gameId, setGameId] = useState("");

    return (
        <section className="lobby-screen panel">
            <div className="lobby-copy">
                <h2>Game Lobby</h2>
                <p>Create a fresh game or open an existing one by ID.</p>
            </div>

            <div className="lobby-actions">
                <button
                    id="create-game-button"
                    type="button"
                    disabled={isLoading}
                    onClick={onCreateGame}
                >
                    Create Game
                </button>

                <div className="control-row">
                    <label className="control-label" htmlFor="game-id-input">
                        Game ID
                    </label>
                    <input
                        id="game-id-input"
                        className="text-input"
                        type="text"
                        value={gameId}
                        disabled={isLoading}
                        placeholder="Paste a game ID"
                        onChange={(event) => {
                            setGameId(event.target.value);
                        }}
                    />
                </div>

                <button
                    id="open-game-button"
                    type="button"
                    disabled={isLoading}
                    onClick={() => {
                        onOpenGame(gameId);
                    }}
                >
                    Open Game
                </button>
            </div>

            <p className="lobby-feedback" aria-live="polite">
                {feedbackMessage ?? " "}
            </p>
        </section>
    );
};
