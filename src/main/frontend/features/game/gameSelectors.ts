import { createSelector } from "@reduxjs/toolkit";

import { getCapturableSquares, getTargetableSquares, normalizeSelectedSquare } from "../../game.js";
import type { RootState } from "../../app/store.js";

export const selectGameState = (state: RootState) => state.game;
export const selectGameView = (state: RootState) => state.game.view;
export const selectCurrentGameId = (state: RootState) => state.game.currentGameId;
export const selectSnapshot = (state: RootState) => state.game.session?.snapshot ?? null;
export const selectLifecycle = (state: RootState) => state.game.session?.lifecycle ?? null;
export const selectCanUndo = (state: RootState) => state.game.session?.canUndo ?? false;
export const selectSelectedSquare = (state: RootState) => state.ui.selectedSquare;
export const selectIsSubmitting = (state: RootState) => state.game.isSubmitting;
export const selectIsLoadingGame = (state: RootState) => state.game.loadState === "loading";
export const selectFeedbackMessage = (state: RootState) => state.game.feedbackMessage;
export const selectAvailableRuleConfigurations = (state: RootState) =>
    state.game.session?.availableRuleConfigurations ?? [];
export const selectSelectedRuleConfigurationId = (state: RootState) =>
    state.game.session?.selectedRuleConfigurationId ?? null;
export const selectSelectedStartingSide = (state: RootState) =>
    state.game.session?.selectedStartingSide ?? "dragons";
export const selectSelectedBoardSize = (state: RootState) =>
    state.game.session?.selectedBoardSize ?? 7;
export const selectCurrentRuleConfiguration = createSelector(
    selectAvailableRuleConfigurations,
    selectSelectedRuleConfigurationId,
    (ruleConfigurations, selectedRuleConfigurationId) =>
        ruleConfigurations.find((ruleConfiguration) => ruleConfiguration.id === selectedRuleConfigurationId) ?? null
);

export const selectCapturableSquares = createSelector(selectSnapshot, (snapshot) =>
    snapshot && snapshot.phase === "capture" ? getCapturableSquares(snapshot) : []
);

export const selectIsFinishedGame = createSelector(
    selectSnapshot,
    selectLifecycle,
    (snapshot, lifecycle) => snapshot?.phase === "none" && lifecycle === "finished"
);

export const selectShowPreGameControls = createSelector(
    selectSnapshot,
    selectLifecycle,
    (snapshot, lifecycle) => snapshot?.phase === "none" && lifecycle === "new"
);

export const selectTargetableSquares = createSelector(
    selectSnapshot,
    selectSelectedSquare,
    (snapshot, selectedSquare) => {
        if (!snapshot) {
            return [];
        }

        return getTargetableSquares(snapshot, normalizeSelectedSquare(snapshot, selectedSquare));
    }
);

export const selectStatusText = createSelector(selectGameState, selectSnapshot, selectIsFinishedGame, (gameState, snapshot, isFinishedGame) => {
    if (gameState.feedbackMessage) {
        return gameState.feedbackMessage;
    }

    if (!snapshot) {
        if (gameState.loadState === "error") {
            return "Unable to load game.";
        }

        return gameState.connectionState === "reconnecting"
            ? "Connection lost. Trying to reconnect..."
            : "Loading game...";
    }

    if (snapshot.phase === "setup") {
        return "Setup phase: place the pieces on the board.";
    }

    if (snapshot.phase === "none") {
        return isFinishedGame
            ? "This game is finished. Go back to the lobby to create a new game."
            : "No game in progress. Select a play style and start a game.";
    }

    if (snapshot.phase === "capture") {
        return `${snapshot.activeSide === "dragons" ? "Dragons" : "Ravens"} moved. Capture a piece, or skip the capture.`;
    }

    const moverLabel = snapshot.activeSide === "dragons" ? "Dragons" : "Ravens";
    return `${moverLabel} to move.`;
});
