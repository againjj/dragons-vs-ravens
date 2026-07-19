package com.ayaziangames.mtg.engine

import com.ayaziangames.mtg.model.DrawCommand
import com.ayaziangames.mtg.model.Game
import com.ayaziangames.mtg.model.GamePhase
import com.ayaziangames.mtg.model.GameStep
import com.ayaziangames.mtg.model.PlayerAction

class MtgEngine {
    fun performAction(game: Game, action: PlayerAction): Game {
        val afterCommand = when (val command = action.command) {
            is DrawCommand -> game.drawCards(action.playerIndex, command.count)
        }
        return afterCommand.update(log = afterCommand.log + action)
    }

    private fun Game.drawCards(playerIndex: Int, count: Int): Game {
        require(playerIndex == gameState.activePlayer) {
            "Only the active player can draw during the draw step."
        }
        require(gameState.phase == GamePhase.BEGINNING && gameState.step == GameStep.DRAW_STEP) {
            "Draw actions are only supported during the draw step."
        }

        val drawingPlayer = gameState.player.getOrNull(playerIndex)
            ?: error("Unknown player index: $playerIndex.")
        require(drawingPlayer.library.cards.size >= count) {
            "Player $playerIndex cannot draw $count card(s) from a library of ${drawingPlayer.library.cards.size}."
        }

        val drawnCards = drawingPlayer.library.cards.take(count)
        val updatedPlayer = drawingPlayer.update(
            library = drawingPlayer.library.update(cards = drawingPlayer.library.cards.drop(count)),
            hand = drawingPlayer.hand.update(cards = drawingPlayer.hand.cards + drawnCards)
        )
        val updatedPlayers = gameState.player.replaceAt(playerIndex, updatedPlayer)

        return update(
            gameState = gameState.update(
                player = updatedPlayers,
                phase = GamePhase.MAIN_PHASE_1,
                step = null
            )
        )
    }

    private fun <T> List<T>.replaceAt(index: Int, value: T): List<T> =
        mapIndexed { currentIndex, currentValue -> if (currentIndex == index) value else currentValue }
}

val engine = MtgEngine()
