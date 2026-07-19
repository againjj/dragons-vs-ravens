package com.ayaziangames.mtg.dsl

import com.ayaziangames.mtg.model.CardDatabase
import com.ayaziangames.mtg.model.Game
import com.ayaziangames.mtg.model.GameState
import com.ayaziangames.mtg.model.PlayerAction

@MtgDsl
class GameBuilder internal constructor() {
    private val assignedAvailableCards = SingleAssignment<CardDatabase?>("availableCards", null)
    private val assignedGameState = SingleAssignment<GameState?>("gameState", null)
    private val assignedLog = SingleAssignment("log", emptyList<PlayerAction>())

    var availableCards: CardDatabase?
        get() = assignedAvailableCards.value
        set(value) = assignedAvailableCards.set(value)
    private val gameState: GameState?
        get() = assignedGameState.value
    var log: List<PlayerAction>
        get() = assignedLog.value
        set(value) = assignedLog.set(value)

    fun gameState(init: @MtgDsl GameStateBuilder.() -> Unit) {
        assignedGameState.set(GameStateBuilder(
            availableCards = requireNotNull(availableCards) { "Game requires availableCards before gameState." }
        ).apply(init).build())
    }

    internal fun build(): Game =
        Game(
            availableCards = requireNotNull(availableCards) { "Game requires availableCards." },
            gameState = requireNotNull(gameState) { "Game requires gameState." },
            log = log.toList()
        )
}

fun game(init: @MtgDsl GameBuilder.() -> Unit): Game =
    GameBuilder().apply(init).build()
