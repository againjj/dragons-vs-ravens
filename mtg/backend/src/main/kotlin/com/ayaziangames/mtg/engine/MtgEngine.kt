package com.ayaziangames.mtg.engine

import com.ayaziangames.mtg.model.DiscardCardsCommand
import com.ayaziangames.mtg.model.DrawCommand
import com.ayaziangames.mtg.model.Game
import com.ayaziangames.mtg.model.GamePhase
import com.ayaziangames.mtg.model.GameState
import com.ayaziangames.mtg.model.GameStep
import com.ayaziangames.mtg.model.PassPriorityCommand
import com.ayaziangames.mtg.model.PlayerAction
import com.ayaziangames.mtg.model.UntapCommand
import com.ayaziangames.mtg.model.WinLossState

class MtgEngine {
    fun performAction(game: Game, action: PlayerAction): Game {
        require(!game.gameState.isGameOver()) {
            "The game is over; no actions are valid."
        }
        val afterCommand = when (val command = action.command) {
            is DrawCommand -> game.drawCards(action.playerIndex, command.count)
            is UntapCommand -> game.untap(action.playerIndex, command.permanentIndexes)
            PassPriorityCommand -> game.passPriority(action.playerIndex)
            is DiscardCardsCommand -> game.discardCards(action.playerIndex, command.cardIndexes)
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
        require(count == 1) {
            "Draw step draw actions must draw exactly 1 card."
        }

        val drawingPlayer = gameState.player.getOrNull(playerIndex)
            ?: error("Unknown player index: $playerIndex.")
        if (drawingPlayer.library.cards.isEmpty()) {
            return update(gameState = gameState.withPlayerLost(playerIndex)).advanceTurnState()
        }

        val drawnCards = drawingPlayer.library.cards.take(count)
        val updatedPlayer = drawingPlayer.update(
            library = drawingPlayer.library.update(cards = drawingPlayer.library.cards.drop(count)),
            hand = drawingPlayer.hand.update(cards = drawingPlayer.hand.cards + drawnCards)
        )
        val updatedPlayers = gameState.player.replaceAt(playerIndex, updatedPlayer)

        return update(gameState = gameState.update(player = updatedPlayers)).advanceTurnState()
    }

    private fun Game.untap(playerIndex: Int, permanentIndexes: List<Int>): Game {
        require(playerIndex == gameState.activePlayer) {
            "Only the active player can untap during the untap step."
        }
        require(gameState.phase == GamePhase.BEGINNING && gameState.step == GameStep.UNTAP_STEP) {
            "Untap actions are only supported during the untap step."
        }

        val untappingPlayer = gameState.player.getOrNull(playerIndex)
            ?: error("Unknown player index: $playerIndex.")
        val tappedPermanentIndexes = untappingPlayer.battlefield.permanents
            .mapIndexedNotNull { index, permanent -> if (permanent.tapped) index else null }
        require(permanentIndexes == tappedPermanentIndexes) {
            "Untap must specify all tapped permanents in battlefield order: $tappedPermanentIndexes."
        }

        val afterUntap = if (permanentIndexes.isEmpty()) {
            this
        } else {
            val updatedPermanents = untappingPlayer.battlefield.permanents.mapIndexed { index, permanent ->
                if (index in permanentIndexes) permanent.update(tapped = false) else permanent
            }
            val updatedPlayer = untappingPlayer.update(
                battlefield = untappingPlayer.battlefield.update(permanents = updatedPermanents)
            )
            update(
                gameState = gameState.update(player = gameState.player.replaceAt(playerIndex, updatedPlayer))
            )
        }
        return afterUntap.advanceTurnState()
    }

    private fun Game.passPriority(playerIndex: Int): Game {
        require(gameState.currentTurnState() in priorityTurnStates) {
            "Pass priority actions are not supported during ${gameState.phase.name}${gameState.step?.let { ", ${it.name}" } ?: ""}."
        }
        val expectedPlayer = requireNotNull(gameState.priorityPlayer) {
            "Priority player is required while priority actions are valid."
        }
        require(playerIndex == expectedPlayer) {
            "Player $expectedPlayer must pass priority next."
        }

        val isLastPriorityPass = expectedPlayer == gameState.playerBeforeActive()
        return if (isLastPriorityPass) {
            advanceTurnState()
        } else {
            update(gameState = gameState.update(priorityPlayer = gameState.nextPlayerAfter(expectedPlayer)))
        }
    }

    private fun Game.discardCards(playerIndex: Int, cardIndexes: List<Int>): Game {
        require(playerIndex == gameState.activePlayer) {
            "Only the active player can discard during cleanup."
        }
        require(gameState.phase == GamePhase.ENDING && gameState.step == GameStep.CLEANUP) {
            "Discard actions are only supported during cleanup."
        }

        val discardingPlayer = gameState.player.getOrNull(playerIndex)
            ?: error("Unknown player index: $playerIndex.")
        val discardCount = discardingPlayer.hand.cards.size - 7
        require(discardCount > 0) {
            "Discard is only valid when the active player has more than 7 cards in hand."
        }
        require(cardIndexes.size == discardCount) {
            "Cleanup discard must specify exactly $discardCount card(s)."
        }
        require(cardIndexes.toSet().size == cardIndexes.size) {
            "Cleanup discard cannot specify the same card more than once."
        }
        require(cardIndexes.all { it in discardingPlayer.hand.cards.indices }) {
            "Cleanup discard indexes must be in range 0..${discardingPlayer.hand.cards.lastIndex}."
        }

        val discardIndexSet = cardIndexes.toSet()
        val discardedCards = cardIndexes.map { discardingPlayer.hand.cards[it] }
        val updatedPlayer = discardingPlayer.update(
            hand = discardingPlayer.hand.update(
                cards = discardingPlayer.hand.cards.filterIndexed { index, _ -> index !in discardIndexSet }
            ),
            graveyard = discardingPlayer.graveyard.update(cards = discardingPlayer.graveyard.cards + discardedCards)
        )
        return update(
            gameState = gameState.update(player = gameState.player.replaceAt(playerIndex, updatedPlayer))
        ).advanceTurnState()
    }

    private fun Game.advanceTurnState(): Game {
        val nextTurnState = gameState.currentTurnState().next()
        val nextGameState = if (nextTurnState == null) {
            gameState.startNextTurn()
        } else {
            gameState.update(
                phase = nextTurnState.phase,
                step = nextTurnState.step,
                priorityPlayer = priorityPlayerFor(nextTurnState, gameState.activePlayer)
            )
        }
        return update(gameState = nextGameState.skipUnneededCleanup())
    }

    private fun GameState.skipUnneededCleanup(): GameState =
        if (phase == GamePhase.ENDING && step == GameStep.CLEANUP && player[activePlayer].hand.cards.size <= 7) {
            startNextTurn()
        } else {
            this
        }

    private fun GameState.startNextTurn(): GameState =
        update(
            activePlayer = (activePlayer + 1) % player.size,
            phase = GamePhase.BEGINNING,
            step = GameStep.UNTAP_STEP,
            priorityPlayer = null
        )

    private fun GameState.nextPlayerAfter(playerIndex: Int): Int =
        (playerIndex + 1) % player.size

    private fun GameState.playerBeforeActive(): Int =
        (activePlayer + player.size - 1) % player.size

    private fun GameState.isGameOver(): Boolean =
        player.all { it.winLossState != null }

    private fun GameState.withPlayerLost(playerIndex: Int): GameState {
        val losingPlayer = player.getOrNull(playerIndex)
            ?: error("Unknown player index: $playerIndex.")
        val playersWithLoser = player.replaceAt(
            playerIndex,
            losingPlayer.update(winLossState = WinLossState.PLAYER_LOST)
        )
        val remainingPlayerIndexes = playersWithLoser
            .mapIndexedNotNull { index, playerState ->
                if (playerState.winLossState != WinLossState.PLAYER_LOST) index else null
            }
        val updatedPlayers = if (remainingPlayerIndexes.size == 1) {
            val winnerIndex = remainingPlayerIndexes.single()
            playersWithLoser.replaceAt(
                winnerIndex,
                playersWithLoser[winnerIndex].update(winLossState = WinLossState.PLAYER_WON)
            )
        } else {
            playersWithLoser
        }
        return update(player = updatedPlayers)
    }

    private fun <T> List<T>.replaceAt(index: Int, value: T): List<T> =
        mapIndexed { currentIndex, currentValue -> if (currentIndex == index) value else currentValue }

    private fun GameState.currentTurnState(): TurnState =
        TurnState(phase, step)

    private fun TurnState.next(): TurnState? {
        val currentIndex = turnStates.indexOf(this)
        require(currentIndex >= 0) {
            "Unknown turn state: ${phase.name}${step?.let { ", ${it.name}" } ?: ""}."
        }
        return turnStates.getOrNull(currentIndex + 1)
    }

    private fun priorityPlayerFor(turnState: TurnState, activePlayer: Int): Int? =
        if (turnState in priorityTurnStates) activePlayer else null

    private data class TurnState(
        val phase: GamePhase,
        val step: GameStep?
    )

    private companion object {
        val turnStates = listOf(
            TurnState(GamePhase.BEGINNING, GameStep.UNTAP_STEP),
            TurnState(GamePhase.BEGINNING, GameStep.UPKEEP),
            TurnState(GamePhase.BEGINNING, GameStep.DRAW_STEP),
            TurnState(GamePhase.MAIN_PHASE_1, null),
            TurnState(GamePhase.COMBAT, GameStep.BEGINNING_OF_COMBAT),
            TurnState(GamePhase.COMBAT, GameStep.DECLARE_ATTACKERS),
            TurnState(GamePhase.COMBAT, GameStep.DECLARE_BLOCKERS),
            TurnState(GamePhase.COMBAT, GameStep.COMBAT_DAMAGE),
            TurnState(GamePhase.COMBAT, GameStep.END_OF_COMBAT),
            TurnState(GamePhase.MAIN_PHASE_2, null),
            TurnState(GamePhase.ENDING, GameStep.END_STEP),
            TurnState(GamePhase.ENDING, GameStep.CLEANUP)
        )

        val priorityTurnStates = setOf(
            TurnState(GamePhase.BEGINNING, GameStep.UPKEEP),
            TurnState(GamePhase.MAIN_PHASE_1, null),
            TurnState(GamePhase.COMBAT, GameStep.BEGINNING_OF_COMBAT),
            TurnState(GamePhase.COMBAT, GameStep.DECLARE_ATTACKERS),
            TurnState(GamePhase.COMBAT, GameStep.DECLARE_BLOCKERS),
            TurnState(GamePhase.COMBAT, GameStep.COMBAT_DAMAGE),
            TurnState(GamePhase.COMBAT, GameStep.END_OF_COMBAT),
            TurnState(GamePhase.MAIN_PHASE_2, null),
            TurnState(GamePhase.ENDING, GameStep.END_STEP)
        )
    }
}

val engine = MtgEngine()
