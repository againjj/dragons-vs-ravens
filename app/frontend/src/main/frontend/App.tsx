import { useCallback, useEffect, useMemo, useRef, useState } from "react";

import { useAppDispatch, useAppSelector } from "./app/hooks.js";
import ayazianGamesLogoUrl from "./assets/AyazianGamesLogo.png";
import { AuthPanel } from "./components/AuthPanel.js";
import { LobbyScreen, type PublicGameListing } from "./components/LobbyScreen.js";
import { ProfileScreen } from "./components/ProfileScreen.js";
import { StatusBanner } from "./components/StatusBanner.js";
import {
    gameActions,
    selectFeedbackMessage,
    selectIsLoadingGame
} from "ravens-and-dragons-frontend/app-integration";
import { continueAsGuest, loadAuthSession, login, logout, signup, signedOutSession } from "./features/auth/authThunks.js";
import type { GameEntry, GameStartOptions } from "@ayaziangames/platform-frontend/game-entry";
import { useFullscreen } from "@ayaziangames/platform-frontend/hooks/useFullscreen";
import type { AppDispatch } from "./app/store.js";
import { useGameRoute } from "./hooks/useGameRoute.js";
import { selectCurrentUser, selectIsAuthenticated, selectOAuthProviders } from "./features/auth/authSelectors.js";
import { authActions } from "./features/auth/authSlice.js";
import { fetchPlayerGames, openPlayerGamesStream, type PlayerGameListing } from "./features/playerGames/playerGamesClient.js";
import { ravensAndDragonsGameEntry } from "ravens-and-dragons-frontend";
import { ticTacToeGameEntry } from "tic-tac-toe-frontend";
import { ginRummyGameEntry } from "gin-rummy-frontend";
import { lunarBaseGameEntry } from "lunar-base-frontend";
import { mtgGameEntry } from "mtg-frontend";
import {
    authSessionExpiredEventType,
    createResponseError,
    isServerUnavailableError,
    isUnauthorizedError,
    notifyAuthSessionExpired,
    notifyServerUnavailable,
    serverUnavailableEventType,
    serverUnavailableMessage,
    sessionExpiredMessage
} from "@ayaziangames/platform-frontend/api-client";

interface AppProps {
    gameEntries?: GameEntry<AppDispatch>[];
}

interface RegisteredGameModule {
    slug: string;
    displayName: string;
}

const localOnlyGameSlugs = new Set(["mtg"]);
const registeredGameEntries: GameEntry<AppDispatch>[] = [
    ravensAndDragonsGameEntry,
    ticTacToeGameEntry,
    ginRummyGameEntry,
    lunarBaseGameEntry,
    mtgGameEntry
];
const defaultVisibleGameEntries = registeredGameEntries.filter((entry) => !localOnlyGameSlugs.has(entry.identity.slug));
const appTitle = "Ayazian Games";

const HeaderLogo = () => (
    <span className="header-logo-clip">
        <img className="header-logo" src={ayazianGamesLogoUrl} alt={appTitle} />
    </span>
);

const fetchPublicGames = async (): Promise<PublicGameListing[]> => {
    const response = await fetch("/api/games/public");
    if (!response.ok) {
        throw await createResponseError(response, "Unable to load public games.");
    }
    const payload = await response.json() as unknown;
    return Array.isArray(payload) ? payload as PublicGameListing[] : [];
};

const fetchRegisteredGameModules = async (): Promise<RegisteredGameModule[] | null> => {
    const response = await fetch("/api/games/modules");
    if (!response.ok) {
        throw await createResponseError(response, "Unable to load available games.");
    }
    const payload = await response.json() as unknown;
    return Array.isArray(payload) ? payload as RegisteredGameModule[] : null;
};

const useGameSessionLifecycles = (gameEntries: GameEntry<AppDispatch>[]) => {
    gameEntries.forEach((entry) => {
        entry.lifecycle.useSession();
    });
};

export const App = ({ gameEntries }: AppProps) => {
    const dispatch = useAppDispatch();
    const usesDefaultGameEntries = gameEntries === undefined;
    const allGameEntries = gameEntries ?? registeredGameEntries;
    const [enabledGameSlugs, setEnabledGameSlugs] = useState<Set<string>>(
        () => new Set((gameEntries ?? defaultVisibleGameEntries).map((entry) => entry.identity.slug))
    );
    const visibleGameEntries = useMemo(
        () => usesDefaultGameEntries
            ? allGameEntries.filter((entry) => enabledGameSlugs.has(entry.identity.slug))
            : allGameEntries,
        [allGameEntries, enabledGameSlugs, usesDefaultGameEntries]
    );
    const isAuthenticated = useAppSelector(selectIsAuthenticated);
    const currentUser = useAppSelector(selectCurrentUser);
    const oauthProviders = useAppSelector(selectOAuthProviders);
    const feedbackMessage = useAppSelector(selectFeedbackMessage);
    const isLoadingGame = useAppSelector(selectIsLoadingGame);
    const pageRef = useRef<HTMLElement | null>(null);
    const userMenuRef = useRef<HTMLDivElement | null>(null);
    const { toggleFullscreen } = useFullscreen(pageRef);
    const [activeGameSlug, setActiveGameSlug] = useState<string | null>(null);
    const [selectedLobbyGameSlug, setSelectedLobbyGameSlug] = useState(visibleGameEntries[0]?.identity.slug ?? "");
    const [publicGames, setPublicGames] = useState<PublicGameListing[]>([]);
    const [playerGames, setPlayerGames] = useState<PlayerGameListing[]>([]);
    const [isPlayerGamesStreamPaused, setIsPlayerGamesStreamPaused] = useState(false);
    const [isUserMenuOpen, setIsUserMenuOpen] = useState(false);
    const [lobbyOpenErrorMessage, setLobbyOpenErrorMessage] = useState<string | null>(null);
    const [createGameErrorMessage, setCreateGameErrorMessage] = useState<string | null>(null);
    const [serverErrorMessage, setServerErrorMessage] = useState<string | null>(null);
    const gameEntriesBySlug = useMemo(
        () => new Map(visibleGameEntries.map((entry) => [entry.identity.slug, entry])),
        [visibleGameEntries]
    );
    const activeGameEntry = activeGameSlug ? gameEntriesBySlug.get(activeGameSlug) ?? null : null;

    const PlayScreen = activeGameEntry?.components.PlayScreen ?? null;
    const handleAuthExpired = useCallback(() => {
        setIsUserMenuOpen(false);
        setPlayerGames([]);
        setLobbyOpenErrorMessage(null);
        setServerErrorMessage(null);
        setIsPlayerGamesStreamPaused(false);
        dispatch(authActions.authSessionSet(signedOutSession(oauthProviders)));
        dispatch(authActions.authFeedbackMessageSet(sessionExpiredMessage));
    }, [dispatch, oauthProviders]);
    const handleServerUnavailable = useCallback(() => {
        setServerErrorMessage(serverUnavailableMessage);
    }, []);
    const { page, navigateToCreate, navigateToGame, navigateToLobby, navigateToProfile, openGameFromLobby, createGameSlug, currentGameId } = useGameRoute(
        visibleGameEntries,
        activeGameEntry,
        setActiveGameSlug
    );
    const showProfileLink = isAuthenticated && (currentUser?.authType === "local" || currentUser?.authType === "oauth");
    const currentUserId = currentUser?.id ?? null;
    const userTurnCount = playerGames.filter((game) => game.isCurrentUserTurn).length;
    useGameSessionLifecycles(visibleGameEntries);

    const currentCreateGameEntry = createGameSlug ? gameEntriesBySlug.get(createGameSlug) ?? null : null;
    const CurrentCreateScreen = currentCreateGameEntry?.components.CreateScreen ?? null;
    const selectedLobbyGameEntry = gameEntriesBySlug.get(selectedLobbyGameSlug) ?? visibleGameEntries[0];

    useEffect(() => {
        if (!usesDefaultGameEntries || !isAuthenticated) {
            return;
        }

        let isActive = true;
        void fetchRegisteredGameModules()
            .then((modules) => {
                if (!isActive || modules === null) {
                    return;
                }
                setEnabledGameSlugs(new Set(modules.map((module) => module.slug)));
            })
            .catch((error: unknown) => {
                if (!isActive) {
                    return;
                }
                if (isUnauthorizedError(error)) {
                    notifyAuthSessionExpired();
                } else if (isServerUnavailableError(error)) {
                    notifyServerUnavailable();
                }
            });

        return () => {
            isActive = false;
        };
    }, [isAuthenticated, usesDefaultGameEntries]);

    useEffect(() => {
        if (!visibleGameEntries.some((entry) => entry.identity.slug === selectedLobbyGameSlug)) {
            setSelectedLobbyGameSlug(visibleGameEntries[0]?.identity.slug ?? "");
        }
    }, [selectedLobbyGameSlug, visibleGameEntries]);

    const pageTitle = useMemo(() => {
        if (page === "login") {
            return `${appTitle}: Login`;
        }
        if (page === "profile") {
            return `${appTitle}: Profile`;
        }
        if (page === "create" && currentCreateGameEntry) {
            return `${appTitle}: Create ${currentCreateGameEntry.identity.displayName}`;
        }
        if (page === "game" && currentGameId && activeGameEntry) {
            return `${appTitle}: ${activeGameEntry.identity.displayName} (${currentGameId})`;
        }
        return appTitle;
    }, [activeGameEntry, currentCreateGameEntry, currentGameId, page]);

    useEffect(() => {
        void dispatch(loadAuthSession());
    }, [dispatch]);

    useEffect(() => {
        document.title = pageTitle;
    }, [pageTitle]);

    useEffect(() => {
        const onAuthExpired = () => {
            handleAuthExpired();
        };
        const onServerUnavailable = () => {
            handleServerUnavailable();
        };
        window.addEventListener(authSessionExpiredEventType, onAuthExpired);
        window.addEventListener(serverUnavailableEventType, onServerUnavailable);
        return () => {
            window.removeEventListener(authSessionExpiredEventType, onAuthExpired);
            window.removeEventListener(serverUnavailableEventType, onServerUnavailable);
        };
    }, [handleAuthExpired, handleServerUnavailable]);

    useEffect(() => {
        if (!isAuthenticated || !currentUserId) {
            setPlayerGames([]);
            setIsUserMenuOpen(false);
            setIsPlayerGamesStreamPaused(false);
            return;
        }
    }, [currentUserId, isAuthenticated]);

    useEffect(() => {
        if (!isAuthenticated || !currentUserId || isPlayerGamesStreamPaused) {
            return;
        }

        let isMounted = true;
        let closeStream = () => {};
        void fetchPlayerGames()
            .then((games) => {
                if (isMounted) {
                    setPlayerGames(games);
                    closeStream = openPlayerGamesStream((updatedGames) => {
                        if (isMounted) {
                            setPlayerGames(updatedGames);
                        }
                    }, () => {
                        if (isMounted) {
                            setIsPlayerGamesStreamPaused(true);
                            notifyServerUnavailable();
                        }
                    });
                }
            })
            .catch((error) => {
                if (isMounted) {
                    if (isUnauthorizedError(error)) {
                        notifyAuthSessionExpired();
                    } else if (isServerUnavailableError(error)) {
                        setIsPlayerGamesStreamPaused(true);
                        notifyServerUnavailable();
                    } else {
                        setLobbyOpenErrorMessage(error instanceof Error ? error.message : "Unable to load your games.");
                    }
                }
            });
        return () => {
            isMounted = false;
            closeStream();
        };
    }, [currentUserId, isAuthenticated, isPlayerGamesStreamPaused]);

    useEffect(() => {
        if (currentCreateGameEntry) {
            setSelectedLobbyGameSlug(currentCreateGameEntry.identity.slug);
        }
    }, [currentCreateGameEntry]);

    useEffect(() => {
        if (!isUserMenuOpen) {
            return;
        }

        const closeOnOutsidePointerDown = (event: PointerEvent) => {
            const target = event.target;
            if (target instanceof Node && userMenuRef.current?.contains(target)) {
                return;
            }
            setIsUserMenuOpen(false);
        };

        document.addEventListener("pointerdown", closeOnOutsidePointerDown);
        return () => {
            document.removeEventListener("pointerdown", closeOnOutsidePointerDown);
        };
    }, [isUserMenuOpen]);

    useEffect(() => {
        if (page === "create" && createGameSlug && !currentCreateGameEntry) {
            navigateToLobby("replace");
        }
    }, [createGameSlug, currentCreateGameEntry, navigateToLobby, page]);

    const loadPublicGames = useCallback(() => {
        void fetchPublicGames()
            .then(setPublicGames)
            .catch((error) => {
                if (isUnauthorizedError(error)) {
                    notifyAuthSessionExpired();
                    return;
                }
                if (isServerUnavailableError(error)) {
                    notifyServerUnavailable();
                    return;
                }
                setLobbyOpenErrorMessage(error instanceof Error ? error.message : "Unable to load public games.");
            });
    }, []);

    useEffect(() => {
        if (page === "lobby") {
            loadPublicGames();
        }
    }, [loadPublicGames, page]);

    const handleFullscreen = (): void => {
        void toggleFullscreen().then(({ message }) => {
            if (message) {
                dispatch(gameActions.feedbackMessageSet(message));
            }
        });
    };

    const handleLogout = () => {
        setIsUserMenuOpen(false);
        void dispatch(logout());
    };

    const normalizeStartOptions = (options?: GameStartOptions | boolean): GameStartOptions => {
        if (typeof options === "boolean") {
            return { publiclyListed: options };
        }
        return { publiclyListed: true, ...options };
    };

    const handleStartGameFromCreate = (gameSlug: string, options?: GameStartOptions | boolean) => {
        setCreateGameErrorMessage(null);
        void (async () => {
            try {
                const entry = gameEntriesBySlug.get(gameSlug);
                if (!entry) {
                    setCreateGameErrorMessage("Unable to start that game right now.");
                    return;
                }
                const gameId = await entry.lifecycle.startGame(dispatch, gameSlug, normalizeStartOptions(options));
                if (gameId) {
                    navigateToGame(gameId, { gameSlug });
                }
            } catch (error) {
                if (isUnauthorizedError(error)) {
                    notifyAuthSessionExpired();
                    return;
                }
                if (isServerUnavailableError(error)) {
                    notifyServerUnavailable();
                    return;
                }
                setCreateGameErrorMessage(error instanceof Error ? error.message : "Unable to start a game right now.");
            }
        })();
    };

    return (
        <main className="page" ref={pageRef}>
            <header className="hero app-header">
                <div className="hero-header">
                    <div className="hero-copy">
                        <div className="brand-row">
                            {isAuthenticated && currentUser ? (
                                <div className="user-menu-shell" ref={userMenuRef}>
                                    <button
                                        type="button"
                                        className="icon-button user-menu-trigger"
                                        aria-label="Menu"
                                        aria-haspopup="menu"
                                        aria-expanded={isUserMenuOpen}
                                        onClick={() => {
                                            if (isPlayerGamesStreamPaused) {
                                                setIsPlayerGamesStreamPaused(false);
                                            }
                                            setIsUserMenuOpen((open) => !open);
                                        }}
                                    >
                                        <svg viewBox="0 0 24 24" aria-hidden="true">
                                            <path d="M4 6h16M4 12h16M4 18h16" />
                                        </svg>
                                        {userTurnCount > 0 ? (
                                            <span className="turn-count-badge" aria-hidden="true">{userTurnCount}</span>
                                        ) : null}
                                    </button>
                                    {isUserMenuOpen ? (
                                        <div className="user-menu-panel" role="menu">
                                            {showProfileLink ? (
                                                page === "profile" ? (
                                                    <span className="is-current-page" role="menuitem" aria-current="page">Profile</span>
                                                ) : (
                                                    <a
                                                        href="/profile"
                                                        role="menuitem"
                                                        onClick={(event) => {
                                                            event.preventDefault();
                                                            setIsUserMenuOpen(false);
                                                            navigateToProfile();
                                                        }}
                                                    >
                                                        Profile
                                                    </a>
                                                )
                                            ) : null}
                                            {page === "lobby" ? (
                                                <span className="is-current-page" role="menuitem" aria-current="page">Lobby</span>
                                            ) : (
                                                <a
                                                    href="/lobby"
                                                    role="menuitem"
                                                    onClick={(event) => {
                                                        event.preventDefault();
                                                        setIsUserMenuOpen(false);
                                                        navigateToLobby();
                                                    }}
                                                >
                                                    Lobby
                                                </a>
                                            )}
                                            <div className="user-menu-separator" role="separator" />
                                            {playerGames.map((game) => {
                                                const isCurrentGame = page === "game" && currentGameId === game.gameId;
                                                const menuItemContent = (
                                                    <>
                                                        {game.isCurrentUserTurn ? (
                                                            <span className="your-turn-badge">
                                                                <span>Your</span>
                                                                <span>Turn</span>
                                                            </span>
                                                        ) : null}
                                                        <span>{game.gameName}: {game.gameId}</span>
                                                    </>
                                                );
                                                return isCurrentGame ? (
                                                    <span
                                                        key={game.gameId}
                                                        className="is-current-page"
                                                        role="menuitem"
                                                        aria-current="page"
                                                    >
                                                        {menuItemContent}
                                                    </span>
                                                ) : (
                                                    <a
                                                        key={game.gameId}
                                                        href={`/g/${encodeURIComponent(game.gameId)}`}
                                                        role="menuitem"
                                                        onClick={(event) => {
                                                            event.preventDefault();
                                                            setIsUserMenuOpen(false);
                                                            void openGameFromLobby(game.gameId);
                                                        }}
                                                    >
                                                        {menuItemContent}
                                                    </a>
                                                );
                                            })}
                                            {playerGames.length > 0 ? <div className="user-menu-separator" role="separator" /> : null}
                                            <button
                                                type="button"
                                                role="menuitem"
                                                className="user-menu-action"
                                                onClick={handleLogout}
                                            >
                                                Log Out
                                            </button>
                                        </div>
                                    ) : null}
                                </div>
                            ) : null}
                            <h1>
                                {page === "login" ? (
                                    <span className="header-home-link">
                                        <HeaderLogo />
                                    </span>
                                ) : (
                                    <a
                                        className="header-home-link"
                                        href="/lobby"
                                        onClick={(event) => {
                                            event.preventDefault();
                                            navigateToLobby();
                                        }}
                                    >
                                        <HeaderLogo />
                                    </a>
                                )}
                            </h1>
                        </div>
                    </div>
                    <div className="hero-actions">
                        {isAuthenticated && currentUser ? (
                            <span className="user-display-name">{currentUser.displayName}</span>
                        ) : null}
                        <button
                            id="fullscreen-button"
                            className="icon-button"
                            type="button"
                            title="Full screen"
                            aria-label="Full screen"
                            onClick={handleFullscreen}
                        >
                            <svg viewBox="0 0 24 24" aria-hidden="true">
                                <path d="M4 9V4h5M20 9V4h-5M4 15v5h5M20 15v5h-5" />
                            </svg>
                        </button>
                    </div>
                </div>
            </header>

            <section className="page-content">
                {page === "loading" ? (
                    <section className="panel">
                        <StatusBanner text="Loading..." />
                    </section>
                ) : page === "login" ? (
                    <AuthPanel
                        onContinueAsGuest={() => {
                            void dispatch(continueAsGuest());
                        }}
                        onLogin={(request) => {
                            void dispatch(login(request));
                        }}
                        onSignup={(request) => {
                            void dispatch(signup(request));
                        }}
                        onLogout={handleLogout}
                    />
                ) : page === "lobby" ? (
                    <LobbyScreen
                        games={visibleGameEntries.map((entry) => entry.identity)}
                        publicGames={publicGames}
                        selectedGameSlug={selectedLobbyGameEntry?.identity.slug ?? ""}
                        feedbackMessage={feedbackMessage}
                        openErrorMessage={lobbyOpenErrorMessage}
                        isLoading={isLoadingGame}
                        onCreateGame={(gameSlug) => {
                            setLobbyOpenErrorMessage(null);
                            setCreateGameErrorMessage(null);
                            setSelectedLobbyGameSlug(gameSlug);
                            navigateToCreate(gameSlug);
                        }}
                        onDismissOpenError={() => {
                            setLobbyOpenErrorMessage(null);
                        }}
                        onOpenGame={(gameId) => {
                            void openGameFromLobby(gameId).then((result) => {
                                setLobbyOpenErrorMessage(result.errorMessage ?? null);
                            });
                        }}
                        onSelectGame={(gameSlug) => {
                            setSelectedLobbyGameSlug(gameSlug);
                        }}
                    />
                ) : page === "create" ? (
                    currentCreateGameEntry && CurrentCreateScreen ? (
                        <>
                            <CurrentCreateScreen
                                gameName={currentCreateGameEntry.identity.displayName}
                                onStartGame={(options: GameStartOptions | boolean | undefined) => {
                                    handleStartGameFromCreate(currentCreateGameEntry.identity.slug, options);
                                }}
                            />
                            {createGameErrorMessage ? (
                                <div
                                    className="modal-backdrop"
                                    role="presentation"
                                    onClick={() => setCreateGameErrorMessage(null)}
                                >
                                    <section
                                        className="panel modal-dialog"
                                        role="dialog"
                                        aria-modal="true"
                                        aria-labelledby="create-game-error-title"
                                        onClick={(event) => event.stopPropagation()}
                                    >
                                        <h2 id="create-game-error-title">Start Game Error</h2>
                                        <p>{createGameErrorMessage}</p>
                                        <button type="button" onClick={() => setCreateGameErrorMessage(null)}>OK</button>
                                    </section>
                                </div>
                            ) : null}
                        </>
                    ) : (
                        <section className="panel">
                            <StatusBanner text="Loading..." />
                        </section>
                    )
                ) : page === "profile" ? (
                    <section className="auth-layout">
                        <ProfileScreen />
                    </section>
                ) : PlayScreen ? (
                    <PlayScreen key={currentGameId ?? activeGameSlug ?? "active-game"} />
                ) : (
                    <section className="panel">
                        <StatusBanner text="Loading..." />
                    </section>
                )}
            </section>

            <footer className="app-footer">
                <small>&copy; 2026 Johnathon Ayazian</small>
            </footer>
            {serverErrorMessage ? (
                <div
                    className="modal-backdrop"
                    role="presentation"
                    onClick={() => {
                        setServerErrorMessage(null);
                    }}
                >
                    <section
                        className="panel modal-dialog"
                        role="dialog"
                        aria-modal="true"
                        aria-labelledby="server-error-title"
                        onClick={(event) => {
                            event.stopPropagation();
                        }}
                    >
                        <h2 id="server-error-title">Server Unavailable</h2>
                        <p>{serverErrorMessage}</p>
                        <button
                            type="button"
                            onClick={() => {
                                setServerErrorMessage(null);
                            }}
                        >
                            OK
                        </button>
                    </section>
                </div>
            ) : null}
        </main>
    );
};
