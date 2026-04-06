import {
    beginGame,
    capturePiece,
    commitTurn,
    createInitialState,
    cycleSetupPiece,
    getCapturableSquares,
    getSquareName,
    getTargetableSquares,
    movePiece,
    moveToNotation,
    rowLetters,
    sideOwnsPiece,
    type Piece,
    type GameState
} from "./game.js";

const boardElement = document.querySelector<HTMLDivElement>("#board");
const boardShellElement = document.querySelector<HTMLDivElement>(".board-shell");
const pageElement = document.querySelector<HTMLElement>(".page");
const statusElement = document.querySelector<HTMLParagraphElement>("#status");
const moveListElement = document.querySelector<HTMLOListElement>("#move-list");
const startButton = document.querySelector<HTMLButtonElement>("#start-button");
const fullscreenButton = document.querySelector<HTMLButtonElement>("#fullscreen-button");
const resetButton = document.querySelector<HTMLButtonElement>("#reset-button");
const captureSkipButton = document.querySelector<HTMLButtonElement>("#capture-skip-button");
const columnLabelsBottom = document.querySelector<HTMLDivElement>("#column-labels-bottom");
const rowLabelsLeft = document.querySelector<HTMLDivElement>("#row-labels-left");

if (
    !boardElement ||
    !boardShellElement ||
    !pageElement ||
    !statusElement ||
    !moveListElement ||
    !startButton ||
    !fullscreenButton ||
    !resetButton ||
    !captureSkipButton ||
    !columnLabelsBottom ||
    !rowLabelsLeft
) {
    throw new Error("Required DOM elements are missing.");
}

let state: GameState = createInitialState();

const pieceGlyph: Record<Piece, string> = {
    dragon: "D",
    raven: "R",
    gold: "G"
};

const handleSquareClick = (square: string): void => {
    if (state.phase === "setup") {
        state = cycleSetupPiece(state, square);
        render();
        return;
    }

    if (state.phase === "capture") {
        state = capturePiece(state, square);
        render();
        return;
    }

    const currentPiece = state.board.get(square);
    if (!state.selectedSquare) {
        if (currentPiece && sideOwnsPiece(state.activeSide, currentPiece)) {
            state.selectedSquare = square;
        }
        render();
        return;
    }

    if (state.selectedSquare === square) {
        state.selectedSquare = null;
        render();
        return;
    }

    if (currentPiece && sideOwnsPiece(state.activeSide, currentPiece)) {
        state.selectedSquare = square;
        render();
        return;
    }

    state = movePiece(state, state.selectedSquare, square);
    render();
};

const updateBoardSize = (): void => {
    const shellStyles = window.getComputedStyle(boardShellElement);
    const labelColumnWidth = Number.parseFloat(shellStyles.getPropertyValue("--label-col-width")) || 30;
    const labelRowHeight = Number.parseFloat(shellStyles.getPropertyValue("--label-row-height")) || 30;
    const boardLabelGap = Number.parseFloat(shellStyles.getPropertyValue("--board-label-gap")) || 8;
    const narrowLayout = window.matchMedia("(max-width: 900px), (max-aspect-ratio: 4 / 5)").matches;

    const availableWidth = boardShellElement.clientWidth - labelColumnWidth - boardLabelGap;
    const availableHeight = narrowLayout
        ? availableWidth
        : boardShellElement.clientHeight - labelRowHeight - boardLabelGap;
    const nextBoardSize = Math.max(180, Math.floor(Math.min(availableWidth, availableHeight)));
    const nextBoardSizeValue = `${nextBoardSize}px`;

    if (boardShellElement.style.getPropertyValue("--board-size") !== nextBoardSizeValue) {
        boardShellElement.style.setProperty("--board-size", nextBoardSizeValue);
    }
};

const updateStatus = (): void => {
    if (state.phase === "setup") {
        statusElement.textContent = "Setup phase: click a square to place dragon, raven, or empty. Gold stays at e5.";
        return;
    }

    if (state.phase === "capture") {
        const opposingLabel = state.activeSide === "dragons" ? "raven" : "dragon or gold";
        statusElement.textContent = `${state.activeSide === "dragons" ? "Dragons" : "Ravens"} moved. Capture one ${opposingLabel}, or skip the capture.`;
        return;
    }

    const moverLabel = state.activeSide === "dragons" ? "Dragons" : "Ravens";
    const extra = state.activeSide === "dragons" ? " Dragons may also move the gold." : "";
    statusElement.textContent = `${moverLabel} to move.${extra}`;
};

const initializeLabels = (): void => {
    const columnsMarkup = Array.from({ length: 9 }, (_, index) => `<span>${index + 1}</span>`).join("");
    columnLabelsBottom.innerHTML = columnsMarkup;
    rowLabelsLeft.innerHTML = rowLetters.map((letter) => `<span>${letter}</span>`).join("");
};

const renderBoard = (): void => {
    const validCaptureSquares = new Set(state.phase === "capture" ? getCapturableSquares(state) : []);
    const targetableSquares = new Set(getTargetableSquares(state));

    boardElement.innerHTML = "";

    for (let rowIndex = 0; rowIndex < 9; rowIndex += 1) {
        for (let colIndex = 0; colIndex < 9; colIndex += 1) {
            const squareName = getSquareName(rowIndex, colIndex);
            const piece = state.board.get(squareName);
            const squareButton = document.createElement("button");
            squareButton.type = "button";
            squareButton.className = "square";
            squareButton.dataset.square = squareName;
            squareButton.setAttribute("aria-label", `Square ${squareName}`);

            if (state.selectedSquare === squareName) {
                squareButton.classList.add("selected");
            }

            if (targetableSquares.has(squareName)) {
                squareButton.classList.add("targetable");
            }

            if (validCaptureSquares.has(squareName)) {
                squareButton.classList.add("capture-target");
            }

            const label = document.createElement("span");
            label.className = "square-label";
            label.textContent = squareName;
            squareButton.append(label);

            if (piece) {
                const pieceElement = document.createElement("div");
                pieceElement.className = `piece ${piece}`;
                pieceElement.textContent = pieceGlyph[piece];
                squareButton.append(pieceElement);
            }

            boardElement.append(squareButton);
        }
    }
};

const renderMoveList = (): void => {
    moveListElement.innerHTML = state.turns
        .map((move) => `<li>${moveToNotation(move)}</li>`)
        .join("");
};

const renderControls = (): void => {
    startButton.disabled = state.phase !== "setup";
    captureSkipButton.disabled = state.phase !== "capture";
};

const render = (): void => {
    updateBoardSize();
    renderBoard();
    renderMoveList();
    renderControls();
    updateStatus();
};

boardElement.addEventListener("click", (event) => {
    const target = event.target;
    if (!(target instanceof Element)) {
        return;
    }

    const squareButton = target.closest<HTMLButtonElement>(".square[data-square]");
    const squareName = squareButton?.dataset.square;
    if (!squareName) {
        return;
    }

    handleSquareClick(squareName);
});

startButton.addEventListener("click", () => {
    state = beginGame(state);
    render();
});

fullscreenButton.addEventListener("click", async () => {
    if (!document.fullscreenEnabled) {
        statusElement.textContent = "Fullscreen is not available in this browser.";
        return;
    }

    if (document.fullscreenElement) {
        await document.exitFullscreen();
        return;
    }

    await pageElement.requestFullscreen();
});

resetButton.addEventListener("click", () => {
    state = createInitialState();
    render();
});

captureSkipButton.addEventListener("click", () => {
    state = commitTurn(state);
    render();
});

const resizeObserver = new ResizeObserver(() => {
    updateBoardSize();
});

resizeObserver.observe(boardShellElement);
window.addEventListener("resize", updateBoardSize);

initializeLabels();
render();
