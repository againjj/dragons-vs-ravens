import { useEffect, useMemo, useState } from "react";

import {
    createResponseError,
    isServerUnavailableError,
    isUnauthorizedError,
    notifyAuthSessionExpired,
    notifyServerUnavailable,
    serverUnavailableMessage,
    sessionExpiredMessage
} from "@ayaziangames/platform-frontend/api-client";
import { buildGameCreatePath, type GameEntry, type GameStartOptions } from "@ayaziangames/platform-frontend/game-entry";

interface MtgGameState {
    id: string;
    gameSlug: "mtg";
    version: number;
    lifecycle: "active" | "finished";
}

interface CreateGameResponse {
    game: MtgGameState;
}

const playRoutePattern = /^\/g\/([^/]+)$/;
const emptyLifecycle = () => undefined;

const readGameIdFromLocation = (): string | null => {
    const routeGameId = window.location.pathname.match(playRoutePattern)?.[1] ?? null;
    return routeGameId ? decodeURIComponent(routeGameId) : null;
};

const fetchMtgGame = async (gameId: string): Promise<MtgGameState> => {
    const response = await fetch(`/api/games/${encodeURIComponent(gameId)}`);
    if (!response.ok) {
        throw await createResponseError(response, `Unable to load game "${gameId}".`);
    }
    const game = await response.json() as MtgGameState;
    if (game.gameSlug !== "mtg") {
        throw new Error(`Game "${gameId}" is not a Magic: the Gathering game.`);
    }
    return game;
};

const createMtgGame = async (): Promise<MtgGameState> => {
    const response = await fetch("/api/games/mtg", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({ publiclyListed: true })
    });
    if (!response.ok) {
        throw await createResponseError(response, "Unable to start Magic: the Gathering right now.");
    }
    const payload = await response.json() as CreateGameResponse;
    return payload.game;
};

const endMtgGame = async (game: MtgGameState): Promise<MtgGameState> => {
    const response = await fetch(`/api/games/${encodeURIComponent(game.id)}/commands`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            type: "endGame",
            expectedVersion: game.version
        })
    });
    if (!response.ok) {
        throw await createResponseError(response, "Unable to end this game right now.");
    }
    return await response.json() as MtgGameState;
};

const CreateMtgScreen = ({ onStartGame }: { gameName: string; onStartGame: (options?: GameStartOptions | boolean) => void }) => (
    <section className="panel">
        <button
            id="start-mtg-button"
            type="button"
            onClick={() => {
                onStartGame();
            }}
        >
            Start
        </button>
    </section>
);

const MtgPlayScreen = () => {
    const gameId = useMemo(readGameIdFromLocation, []);
    const [game, setGame] = useState<MtgGameState | null>(null);
    const [message, setMessage] = useState<string | null>(null);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const isFinished = game?.lifecycle === "finished";

    useEffect(() => {
        if (!gameId) {
            setMessage("Game ID is missing.");
            return;
        }

        let isActive = true;
        void fetchMtgGame(gameId)
            .then((loadedGame) => {
                if (isActive) {
                    setGame(loadedGame);
                    setMessage(null);
                }
            })
            .catch((error: unknown) => {
                if (isActive) {
                    if (isUnauthorizedError(error)) {
                        notifyAuthSessionExpired();
                        setMessage(sessionExpiredMessage);
                    } else if (isServerUnavailableError(error)) {
                        notifyServerUnavailable();
                        setMessage(serverUnavailableMessage);
                    } else {
                        setMessage(error instanceof Error ? error.message : "Unable to load Magic: the Gathering.");
                    }
                }
            });

        const stream = new EventSource(`/api/games/${encodeURIComponent(gameId)}/stream`);
        stream.addEventListener("game", (event) => {
            const nextGame = JSON.parse((event as MessageEvent).data) as MtgGameState;
            if (nextGame.gameSlug === "mtg") {
                setGame(nextGame);
                setMessage(null);
            }
        });
        stream.onerror = () => {
            notifyServerUnavailable();
            stream.close();
        };

        return () => {
            isActive = false;
            stream.close();
        };
    }, [gameId]);

    const handleEndGame = () => {
        if (!game || isSubmitting || isFinished) {
            return;
        }

        setIsSubmitting(true);
        setMessage(null);
        void endMtgGame(game)
            .then(setGame)
            .catch((error: unknown) => {
                if (isUnauthorizedError(error)) {
                    notifyAuthSessionExpired();
                    setMessage(sessionExpiredMessage);
                } else if (isServerUnavailableError(error)) {
                    notifyServerUnavailable();
                    setMessage(serverUnavailableMessage);
                } else {
                    setMessage(error instanceof Error ? error.message : "Unable to end this game right now.");
                }
            })
            .finally(() => {
                setIsSubmitting(false);
            });
    };

    return (
        <section className="game-page">
            <h1 className="content-title">Magic: the Gathering</h1>

            <section className="panel page-header-panel">
                <div className="page-header-copy">
                    <h2>{gameId ? `Game ${gameId}` : "Current Game"}</h2>
                    <p aria-live="polite">{isFinished ? "Game ended" : "Game active"}</p>
                    <p aria-live="polite">{message ?? " "}</p>
                </div>
            </section>

            <section className="panel">
                <button
                    id="end-mtg-game-button"
                    type="button"
                    disabled={!game || isSubmitting || isFinished}
                    onClick={handleEndGame}
                >
                    End Game
                </button>
            </section>
        </section>
    );
};

export const mtgGameEntry: GameEntry = {
    identity: {
        slug: "mtg",
        displayName: "Magic: the Gathering"
    },
    routes: {
        createPath: buildGameCreatePath("mtg"),
        buildPlayPath: (gameId) => `/g/${encodeURIComponent(gameId.trim())}`,
        matchPlayPath: (pathname) => pathname.match(playRoutePattern)?.[1] ?? null
    },
    components: {
        CreateScreen: CreateMtgScreen,
        PlayScreen: MtgPlayScreen
    },
    lifecycle: {
        useSession: emptyLifecycle,
        startGame: async () => {
            const game = await createMtgGame();
            return game.id;
        },
        openGame: emptyLifecycle,
        returnToLobby: emptyLifecycle,
        enterCreateMode: emptyLifecycle,
        clearCreateMode: emptyLifecycle
    }
};
