package com.ayaziangames.game.bot.strategy

import com.ayaziangames.game.bot.*
import com.ayaziangames.game.model.*
import com.ayaziangames.game.rules.*


class RandomGameBotStrategy(
    private val randomIndexSource: RandomIndexSource
) : GameBotStrategy {
    override fun chooseMove(snapshot: GameSnapshot, legalMoves: List<LegalMove>): LegalMove {
        require(legalMoves.isNotEmpty()) { "Random bot requires at least one legal move." }
        return legalMoves[randomIndexSource.nextInt(legalMoves.size)]
    }
}
