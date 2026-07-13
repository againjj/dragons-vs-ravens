package com.ayaziangames.game.bot

import com.ayaziangames.game.persistence.*
import com.ayaziangames.game.session.*
import com.ayaziangames.game.bot.machine.*
import com.ayaziangames.game.bot.strategy.*
import com.ayaziangames.game.model.*
import com.ayaziangames.game.rules.*


data class LegalMove(
    val origin: String,
    val destination: String
)

data class BotDefinition(
    val id: String,
    val displayName: String,
    val supportedRuleConfigurationIds: Set<String>,
    val strategy: GameBotStrategy
) {
    fun toSummary(): BotSummary = BotSummary(id = id, displayName = displayName)
}

interface GameBotStrategy {
    fun chooseMove(snapshot: GameSnapshot, legalMoves: List<LegalMove>): LegalMove
}

interface RandomIndexSource {
    fun nextInt(bound: Int): Int
}
