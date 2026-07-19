package com.ayaziangames.mtg.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class GameUpdateTest {
    @Test
    fun updateReturnsSameGameWhenFieldsAreUnchanged() {
        val game = gameModel()

        assertSame(game, game.update())
    }

    @Test
    fun updateReturnsNewGameWhenFieldsChange() {
        val game = gameModel()
        val action = PlayerAction(0, DrawCommand(1))
        val log = listOf(action)

        val updated = game.update(log = log)

        assertNotSame(game, updated)
        assertSame(game.availableCards, updated.availableCards)
        assertSame(game.gameState, updated.gameState)
        assertEquals(log, updated.log)
    }

    private fun gameModel(): Game {
        val definition = CardDefinition(name = "Forest")
        val card = Card(definition)
        val playerState = PlayerState(
            deck = listOf(card),
            library = Library(listOf(card)),
            battlefield = Battlefield(emptyList()),
            hand = Hand(emptyList()),
            life = 20
        )
        return Game(
            availableCards = CardDatabase(mapOf(definition.name to definition)),
            gameState = GameState(
                player = listOf(playerState),
                activePlayer = 0,
                phase = GamePhase.BEGINNING,
                step = GameStep.DRAW_STEP
            )
        )
    }
}
