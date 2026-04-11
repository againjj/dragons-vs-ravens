import { useRef, type CSSProperties } from "react";

import { useAppDispatch, useAppSelector } from "./app/hooks.js";
import { Board } from "./components/Board.js";
import { ControlsPanel } from "./components/ControlsPanel.js";
import { LobbyScreen } from "./components/LobbyScreen.js";
import { MoveList } from "./components/MoveList.js";
import { StatusBanner } from "./components/StatusBanner.js";
import {
    selectCurrentGameId,
    selectCurrentRuleConfiguration,
    selectFeedbackMessage,
    selectGameView,
    selectIsLoadingGame,
    selectSnapshot,
    selectStatusText
} from "./features/game/gameSelectors.js";
import { gameActions } from "./features/game/gameSlice.js";
import {
    createGame,
    endGame,
    endSetup,
    openGame,
    returnToLobby,
    selectBoardSize,
    selectRuleConfiguration,
    selectStartingSide,
    skipCapture,
    startGame,
    undoMove
} from "./features/game/gameThunks.js";
import { getBoardDimension, getColumnLetters } from "./game.js";
import { useGameSession } from "./features/game/useGameSession.js";
import { useBoardSizing } from "./hooks/useBoardSizing.js";
import { useFullscreen } from "./hooks/useFullscreen.js";
import { useGameRoute } from "./hooks/useGameRoute.js";

export const App = () => {
    const dispatch = useAppDispatch();
    const statusText = useAppSelector(selectStatusText);
    const currentRuleConfiguration = useAppSelector(selectCurrentRuleConfiguration);
    const currentGameId = useAppSelector(selectCurrentGameId);
    const feedbackMessage = useAppSelector(selectFeedbackMessage);
    const isLoadingGame = useAppSelector(selectIsLoadingGame);
    const snapshot = useAppSelector(selectSnapshot);
    const view = useAppSelector(selectGameView);
    const boardDimension = getBoardDimension(snapshot);
    const columnLetters = getColumnLetters(boardDimension);
    const boardStyle = { "--board-dimension": String(boardDimension) } as CSSProperties;
    const pageRef = useRef<HTMLElement | null>(null);
    const boardShellRef = useRef<HTMLDivElement | null>(null);
    const { toggleFullscreen } = useFullscreen(pageRef);

    useGameRoute();
    useGameSession();
    useBoardSizing(boardShellRef);

    const handleFullscreen = (): void => {
        void toggleFullscreen().then(({ message }) => {
            if (message) {
                dispatch(gameActions.feedbackMessageSet(message));
            }
        });
    };

    return (
        <main className="page" ref={pageRef}>
            <section className="hero">
                <div className="hero-header">
                    <div className="hero-copy">
                        <h1>Dragons vs Ravens</h1>
                        {view === "game" && currentGameId ? (
                            <p className="game-id-banner">Game ID: {currentGameId}</p>
                        ) : null}
                    </div>
                    <div className="hero-actions">
                        {view === "game" ? (
                            <button
                                id="back-to-lobby-button"
                                className="secondary-button"
                                type="button"
                                onClick={() => {
                                    void dispatch(returnToLobby());
                                }}
                            >
                                Back to Lobby
                            </button>
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
            </section>

            {view === "lobby" ? (
                <LobbyScreen
                    feedbackMessage={feedbackMessage}
                    isLoading={isLoadingGame}
                    onCreateGame={() => {
                        void dispatch(createGame());
                    }}
                    onOpenGame={(gameId) => {
                        void dispatch(openGame(gameId));
                    }}
                />
            ) : (
                <section className="game-layout">
                    <section className="panel board-panel">
                        <StatusBanner text={statusText} />
                        <div className="board-shell" ref={boardShellRef}>
                            <Board />
                            <div className="board-footer">
                                <div className="board-footer-spacer" aria-hidden="true"></div>
                                <div className="column-labels bottom" id="column-labels-bottom" style={boardStyle}>
                                    {columnLetters.map((letter) => (
                                        <span key={letter}>{letter}</span>
                                    ))}
                                </div>
                            </div>
                        </div>
                    </section>

                    <section className="panel side-panel top-panel">
                        <section className="controls-panel">
                            <ControlsPanel
                                onStartGame={() => {
                                    void dispatch(startGame());
                                }}
                                onSelectRuleConfiguration={(ruleConfigurationId) => {
                                    void dispatch(selectRuleConfiguration(ruleConfigurationId));
                                }}
                                onSelectStartingSide={(side) => {
                                    void dispatch(selectStartingSide(side));
                                }}
                                onSelectBoardSize={(boardSize) => {
                                    void dispatch(selectBoardSize(boardSize));
                                }}
                                onEndSetup={() => {
                                    void dispatch(endSetup());
                                }}
                                onEndGame={() => {
                                    void dispatch(endGame());
                                }}
                                onUndo={() => {
                                    void dispatch(undoMove());
                                }}
                                onSkipCapture={() => {
                                    void dispatch(skipCapture());
                                }}
                            />
                        </section>

                        <section className="legend">
                            <h2>Rules</h2>
                            {(currentRuleConfiguration?.descriptionSections ?? []).map((section, index) => (
                                <div key={`${section.heading ?? "section"}-${index}`} className="legend-section">
                                    {section.heading ? <h3>{section.heading}</h3> : null}
                                    {section.paragraphs.map((paragraph) => (
                                        <p key={paragraph}>{paragraph}</p>
                                    ))}
                                </div>
                            ))}
                        </section>
                    </section>

                    <section className="panel side-panel bottom-panel">
                        <MoveList />
                    </section>
                </section>
            )}
        </main>
    );
};
