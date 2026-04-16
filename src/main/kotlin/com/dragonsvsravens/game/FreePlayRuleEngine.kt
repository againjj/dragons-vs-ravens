package com.dragonsvsravens.game

internal object FreePlayRuleEngine : RuleSet {
    override fun startPhase(): Phase = Phase.setup

    override fun validateMove(snapshot: GameSnapshot, origin: String, destination: String, piece: Piece) = Unit

    override fun getCapturableSquares(snapshot: GameSnapshot): List<String> =
        snapshot.board.entries
            .filter { (_, piece) -> RuleEngineSupport.canCapturePiece(snapshot.activeSide, piece) }
            .map { (square, _) -> square }

    override fun applyMove(snapshot: GameSnapshot, origin: String, destination: String, piece: Piece): GameSnapshot {
        val movedSnapshot = RuleEngineSupport.createMovedSnapshot(snapshot, origin, destination, piece)
        return if (getCapturableSquares(movedSnapshot).isNotEmpty()) {
            movedSnapshot.copy(phase = Phase.capture)
        } else {
            commitPendingTurn(movedSnapshot)
        }
    }

    override fun capturePiece(snapshot: GameSnapshot, square: String): GameSnapshot {
        val piece = snapshot.board[square] ?: throw IllegalArgumentException("No piece exists at $square.")
        require(RuleEngineSupport.canCapturePiece(snapshot.activeSide, piece)) {
            "The active side cannot capture the piece at $square."
        }

        val board = LinkedHashMap(snapshot.board)
        board.remove(square)
        return commitPendingTurn(snapshot.copy(board = board, pendingMove = snapshot.pendingMove), listOf(square))
    }

    override fun commitPendingTurn(snapshot: GameSnapshot): GameSnapshot {
        val pendingMove = snapshot.pendingMove ?: return snapshot
        return snapshot.copy(
            phase = Phase.move,
            activeSide = RuleEngineSupport.oppositeSide(snapshot.activeSide),
            pendingMove = null,
            turns = snapshot.turns + pendingMove
        )
    }

    private fun commitPendingTurn(snapshot: GameSnapshot, capturedSquares: List<String>): GameSnapshot {
        val pendingMove = snapshot.pendingMove ?: return snapshot
        val completedMove = pendingMove.copy(capturedSquares = capturedSquares)
        return snapshot.copy(
            phase = Phase.move,
            activeSide = RuleEngineSupport.oppositeSide(snapshot.activeSide),
            pendingMove = null,
            turns = snapshot.turns + completedMove
        )
    }
}
