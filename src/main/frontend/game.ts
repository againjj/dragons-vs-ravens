export type Piece = "dragon" | "raven" | "gold";
export type Side = "dragons" | "ravens";
export type Phase = "setup" | "move" | "capture";

export interface MoveRecord {
    from: string;
    to: string;
    captured?: string;
}

export interface GameState {
    board: Map<string, Piece>;
    phase: Phase;
    activeSide: Side;
    selectedSquare: string | null;
    pendingMove: MoveRecord | null;
    turns: MoveRecord[];
}

export const rowLetters = ["i", "h", "g", "f", "e", "d", "c", "b", "a"];
export const bottomToTopLetters = [...rowLetters].reverse();

export const getSquareName = (rowIndex: number, colIndex: number): string => `${rowLetters[rowIndex]}${colIndex + 1}`;

export const createInitialBoard = (): Map<string, Piece> => {
    const board = new Map<string, Piece>();
    board.set("e5", "gold");
    return board;
};

export const createInitialState = (): GameState => ({
    board: createInitialBoard(),
    phase: "setup",
    activeSide: "dragons",
    selectedSquare: null,
    pendingMove: null,
    turns: []
});

export const oppositeSide = (side: Side): Side => (side === "dragons" ? "ravens" : "dragons");

export const sideOwnsPiece = (side: Side, piece: Piece): boolean => {
    if (piece === "gold") {
        return side === "dragons";
    }
    return side === "dragons" ? piece === "dragon" : piece === "raven";
};

export const canCapturePiece = (side: Side, piece: Piece): boolean => {
    return side === "dragons" ? piece === "raven" : piece === "dragon" || piece === "gold";
};

const clearTransientState = (state: GameState): GameState => ({
    ...state,
    selectedSquare: null,
    pendingMove: null
});

export const cycleSetupPiece = (state: GameState, square: string): GameState => {
    if (square === "e5") {
        return state;
    }

    const board = new Map(state.board);
    const currentPiece = board.get(square);
    if (!currentPiece) {
        board.set(square, "dragon");
        return { ...state, board };
    }

    if (currentPiece === "dragon") {
        board.set(square, "raven");
        return { ...state, board };
    }

    board.delete(square);
    return { ...state, board };
};

export const beginGame = (state: GameState): GameState => ({
    ...clearTransientState(state),
    phase: "move",
    activeSide: "dragons"
});

export const resetGame = (): GameState => createInitialState();

export const commitTurn = (state: GameState, capturedSquare?: string): GameState => {
    if (!state.pendingMove) {
        return state;
    }

    const completedMove: MoveRecord = {
        ...state.pendingMove,
        ...(capturedSquare ? { captured: capturedSquare } : {})
    };

    return {
        ...clearTransientState(state),
        phase: "move",
        activeSide: oppositeSide(state.activeSide),
        turns: [...state.turns, completedMove]
    };
};

export const getCapturableSquares = (state: GameState): string[] =>
    [...state.board.entries()]
        .filter(([, piece]) => canCapturePiece(state.activeSide, piece))
        .map(([square]) => square);

export const getTargetableSquares = (state: GameState): string[] => {
    if (state.phase !== "move" || !state.selectedSquare) {
        return [];
    }

    const targetableSquares: string[] = [];
    for (let rowIndex = 0; rowIndex < 9; rowIndex += 1) {
        for (let colIndex = 0; colIndex < 9; colIndex += 1) {
            const square = getSquareName(rowIndex, colIndex);
            if (!state.board.has(square) && square !== state.selectedSquare) {
                targetableSquares.push(square);
            }
        }
    }
    return targetableSquares;
};

export const movePiece = (state: GameState, origin: string, destination: string): GameState => {
    if (origin === destination) {
        return state;
    }

    const piece = state.board.get(origin);
    if (!piece || state.board.has(destination)) {
        return state;
    }

    const board = new Map(state.board);
    board.delete(origin);
    board.set(destination, piece);

    const movedState: GameState = {
        ...state,
        board,
        selectedSquare: null,
        pendingMove: { from: origin, to: destination }
    };

    if (getCapturableSquares(movedState).length > 0) {
        return {
            ...movedState,
            phase: "capture"
        };
    }

    return commitTurn(movedState);
};

export const capturePiece = (state: GameState, square: string): GameState => {
    const piece = state.board.get(square);
    if (!piece || !canCapturePiece(state.activeSide, piece)) {
        return state;
    }

    const board = new Map(state.board);
    board.delete(square);
    return commitTurn({ ...state, board }, square);
};

export const moveToNotation = (move: MoveRecord): string =>
    `${move.from}-${move.to}${move.captured ? `x${move.captured}` : ""}`;
