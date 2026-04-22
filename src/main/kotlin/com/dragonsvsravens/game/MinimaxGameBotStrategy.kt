package com.dragonsvsravens.game

fun interface MinimaxSearchObserver {
    fun onNodeEvaluated()
}

private object NoOpMinimaxSearchObserver : MinimaxSearchObserver {
    override fun onNodeEvaluated() = Unit
}

class MinimaxGameBotStrategy(
    private val searchDepth: Int = 2,
    private val searchObserver: MinimaxSearchObserver = NoOpMinimaxSearchObserver
) : GameBotStrategy {
    init {
        require(searchDepth >= 1) { "Minimax bot requires a search depth of at least 1." }
    }

    override fun chooseMove(snapshot: GameSnapshot, legalMoves: List<LegalMove>): LegalMove {
        require(legalMoves.isNotEmpty()) { "Minimax bot requires at least one legal move." }

        val maximizingSide = snapshot.activeSide
        var bestMove = legalMoves.first()
        var bestScore = minimax(
            snapshot = BotStrategySupport.applyMove(snapshot, bestMove),
            depthRemaining = searchDepth - 1,
            maximizingSide = maximizingSide
        )

        legalMoves.drop(1).forEach { move ->
            val score = minimax(
                snapshot = BotStrategySupport.applyMove(snapshot, move),
                depthRemaining = searchDepth - 1,
                maximizingSide = maximizingSide
            )
            if (score > bestScore) {
                bestMove = move
                bestScore = score
            }
        }

        return bestMove
    }

    private fun minimax(
        snapshot: GameSnapshot,
        depthRemaining: Int,
        maximizingSide: Side
    ): Int {
        searchObserver.onNodeEvaluated()

        BotStrategySupport.terminalScore(snapshot, maximizingSide)?.let { return it }

        if (depthRemaining == 0 || snapshot.phase != Phase.move) {
            return BotStrategySupport.evaluateForSide(snapshot, maximizingSide)
        }

        val legalMoves = GameRules.getLegalMoves(snapshot)
        if (legalMoves.isEmpty()) {
            return BotStrategySupport.evaluateForSide(snapshot, maximizingSide)
        }

        return if (snapshot.activeSide == maximizingSide) {
            var bestScore = Int.MIN_VALUE
            legalMoves.forEach { move ->
                val score = minimax(
                    snapshot = BotStrategySupport.applyMove(snapshot, move),
                    depthRemaining = depthRemaining - 1,
                    maximizingSide = maximizingSide
                )
                if (score > bestScore) {
                    bestScore = score
                }
            }
            bestScore
        } else {
            var bestScore = Int.MAX_VALUE
            legalMoves.forEach { move ->
                val score = minimax(
                    snapshot = BotStrategySupport.applyMove(snapshot, move),
                    depthRemaining = depthRemaining - 1,
                    maximizingSide = maximizingSide
                )
                if (score < bestScore) {
                    bestScore = score
                }
            }
            bestScore
        }
    }
}
