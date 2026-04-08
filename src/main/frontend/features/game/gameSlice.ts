import { createSlice, type PayloadAction } from "@reduxjs/toolkit";

import type { ServerGameSession } from "../../game.js";

export type GameView = "lobby" | "game";

export interface GameState {
    currentGameId: string | null;
    view: GameView;
    session: ServerGameSession | null;
    isSubmitting: boolean;
    loadState: "idle" | "loading" | "ready" | "error";
    connectionState: "idle" | "connecting" | "open" | "reconnecting";
    feedbackMessage: string | null;
}

export const initialGameState: GameState = {
    currentGameId: null,
    view: "lobby",
    session: null,
    isSubmitting: false,
    loadState: "idle",
    connectionState: "idle",
    feedbackMessage: null
};

const gameSlice = createSlice({
    name: "game",
    initialState: initialGameState,
    reducers: {
        gameLoadRequested(state, action: PayloadAction<string>) {
            state.currentGameId = action.payload;
            state.view = "game";
            state.session = null;
            state.isSubmitting = false;
            state.loadState = "loading";
            state.connectionState = "connecting";
            state.feedbackMessage = null;
        },
        gameOpened(state, action: PayloadAction<string>) {
            state.currentGameId = action.payload;
            state.view = "game";
            state.feedbackMessage = null;
        },
        returnedToLobby(state) {
            state.currentGameId = null;
            state.view = "lobby";
            state.session = null;
            state.isSubmitting = false;
            state.loadState = "idle";
            state.connectionState = "idle";
            state.feedbackMessage = null;
        },
        loadStarted(state) {
            state.loadState = "loading";
            state.connectionState = state.view === "game" ? "connecting" : "idle";
            state.feedbackMessage = null;
        },
        loadFailed(state) {
            state.loadState = "error";
            state.session = null;
        },
        commandStarted(state) {
            state.isSubmitting = true;
            state.feedbackMessage = null;
        },
        commandFinished(state) {
            state.isSubmitting = false;
        },
        sessionUpdated(state, action: PayloadAction<ServerGameSession>) {
            state.currentGameId = action.payload.id;
            state.session = action.payload;
            state.loadState = "ready";
            state.feedbackMessage = null;
        },
        feedbackMessageSet(state, action: PayloadAction<string>) {
            state.feedbackMessage = action.payload;
        },
        streamConnected(state) {
            state.connectionState = "open";
        },
        streamDisconnected(state) {
            state.connectionState = "reconnecting";
        }
    }
});

export const gameReducer = gameSlice.reducer;
export const gameActions = gameSlice.actions;
