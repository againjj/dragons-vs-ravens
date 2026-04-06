import assert from "node:assert/strict";
import test from "node:test";

import {
    beginGame,
    capturePiece,
    commitTurn,
    createInitialState,
    cycleSetupPiece,
    getCapturableSquares,
    getTargetableSquares,
    movePiece,
    moveToNotation
} from "../../../build/generated/frontend/game.js";

test("initial state starts in setup with gold at e5", () => {
    const state = createInitialState();

    assert.equal(state.phase, "setup");
    assert.equal(state.activeSide, "dragons");
    assert.equal(state.board.get("e5"), "gold");
    assert.deepEqual(state.turns, []);
});

test("setup clicks cycle empty to dragon to raven to empty", () => {
    const first = cycleSetupPiece(createInitialState(), "a1");
    const second = cycleSetupPiece(first, "a1");
    const third = cycleSetupPiece(second, "a1");

    assert.equal(first.board.get("a1"), "dragon");
    assert.equal(second.board.get("a1"), "raven");
    assert.equal(third.board.has("a1"), false);
});

test("setup cannot modify the gold square", () => {
    const state = cycleSetupPiece(createInitialState(), "e5");

    assert.equal(state.board.get("e5"), "gold");
    assert.equal(state.board.size, 1);
});

test("begin game resets selection and pending move", () => {
    const started = beginGame({
        ...createInitialState(),
        selectedSquare: "a1",
        pendingMove: { from: "a1", to: "a2" }
    });

    assert.equal(started.phase, "move");
    assert.equal(started.activeSide, "dragons");
    assert.equal(started.selectedSquare, null);
    assert.equal(started.pendingMove, null);
});

test("movePiece enters capture phase when an opposing piece exists", () => {
    const started = beginGame(cycleSetupPiece(cycleSetupPiece(createInitialState(), "a1"), "b2"));
    const moved = movePiece(
        {
            ...started,
            board: new Map([
                ["e5", "gold"],
                ["a1", "dragon"],
                ["b2", "raven"]
            ])
        },
        "a1",
        "a2"
    );

    assert.equal(moved.phase, "capture");
    assert.equal(moved.activeSide, "dragons");
    assert.deepEqual(getCapturableSquares(moved), ["b2"]);
    assert.deepEqual(moved.pendingMove, { from: "a1", to: "a2" });
});

test("movePiece commits the turn immediately when nothing is capturable", () => {
    const moved = movePiece(
        {
            ...beginGame(createInitialState()),
            board: new Map([
                ["e5", "gold"],
                ["a1", "dragon"]
            ])
        },
        "a1",
        "a2"
    );

    assert.equal(moved.phase, "move");
    assert.equal(moved.activeSide, "ravens");
    assert.equal(moved.pendingMove, null);
    assert.equal(moved.selectedSquare, null);
    assert.equal(moved.turns.length, 1);
    assert.deepEqual(moved.turns[0], { from: "a1", to: "a2" });
});

test("capturePiece removes the piece and commits the move", () => {
    const captured = capturePiece(
        {
            board: new Map([
                ["e5", "gold"],
                ["a2", "dragon"],
                ["b2", "raven"]
            ]),
            phase: "capture",
            activeSide: "dragons",
            selectedSquare: null,
            pendingMove: { from: "a1", to: "a2" },
            turns: []
        },
        "b2"
    );

    assert.equal(captured.board.has("b2"), false);
    assert.equal(captured.phase, "move");
    assert.equal(captured.activeSide, "ravens");
    assert.deepEqual(captured.turns, [{ from: "a1", to: "a2", captured: "b2" }]);
});

test("commitTurn can be used to skip capture", () => {
    const committed = commitTurn({
        board: new Map([
            ["e5", "gold"],
            ["a2", "dragon"],
            ["b2", "raven"]
        ]),
        phase: "capture",
        activeSide: "dragons",
        selectedSquare: null,
        pendingMove: { from: "a1", to: "a2" },
        turns: []
    });

    assert.equal(committed.phase, "move");
    assert.equal(committed.activeSide, "ravens");
    assert.deepEqual(committed.turns, [{ from: "a1", to: "a2" }]);
});

test("targetable squares are all empty squares except the selected one during move phase", () => {
    const targetableSquares = getTargetableSquares({
        ...beginGame(createInitialState()),
        board: new Map([
            ["e5", "gold"],
            ["a1", "dragon"],
            ["b2", "raven"]
        ]),
        selectedSquare: "a1"
    });

    assert.equal(targetableSquares.includes("a1"), false);
    assert.equal(targetableSquares.includes("b2"), false);
    assert.equal(targetableSquares.includes("c3"), true);
    assert.equal(targetableSquares.length, 78);
});

test("move notation includes captures only when present", () => {
    assert.equal(moveToNotation({ from: "a1", to: "a2" }), "a1-a2");
    assert.equal(moveToNotation({ from: "a1", to: "a2", captured: "b2" }), "a1-a2xb2");
});
