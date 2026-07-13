package com.ayaziangames.game.bot.machine

import com.ayaziangames.game.bot.*
import com.ayaziangames.game.bot.strategy.*
import com.ayaziangames.game.model.*
import com.ayaziangames.game.rules.*


class MachineTrainedBotStrategy(
    private val modelsByRuleConfigurationId: Map<String, MachineTrainedModel>
) : GameBotStrategy {
    override fun chooseMove(snapshot: GameSnapshot, legalMoves: List<LegalMove>): LegalMove {
        require(legalMoves.isNotEmpty()) { "Machine-trained bot requires at least one legal move." }
        val model = requireNotNull(modelsByRuleConfigurationId[snapshot.ruleConfigurationId]) {
            "Machine-trained bot does not support ${snapshot.ruleConfigurationId}."
        }

        BotStrategySupport.findImmediateWinningMove(snapshot, legalMoves)?.let { return it }

        val scoringContext = MachineTrainedFeatureEncoder.createScoringContext(snapshot, model)
        var bestMove = legalMoves.first()
        var bestScore = scoreMove(snapshot, bestMove, scoringContext)

        for (index in 1 until legalMoves.size) {
            val move = legalMoves[index]
            val score = scoreMove(snapshot, move, scoringContext)
            if (score > bestScore) {
                bestMove = move
                bestScore = score
            }
        }

        return bestMove
    }

    private fun scoreMove(
        snapshot: GameSnapshot,
        move: LegalMove,
        scoringContext: MachineTrainedFeatureEncoder.ScoringContext
    ): Float {
        val nextSnapshot = BotStrategySupport.applyMove(snapshot, move)
        val features = MachineTrainedFeatureEncoder.encode(snapshot, move, nextSnapshot, scoringContext)
        return MachineTrainedMoveScorer.score(scoringContext, features)
    }
}
