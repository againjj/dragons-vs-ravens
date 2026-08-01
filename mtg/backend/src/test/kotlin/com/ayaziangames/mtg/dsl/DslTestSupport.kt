package com.ayaziangames.mtg.dsl

import com.ayaziangames.mtg.model.BEGINNING
import com.ayaziangames.mtg.model.BEGINNING_OF_COMBAT
import com.ayaziangames.mtg.model.COMBAT
import com.ayaziangames.mtg.model.COMBAT_DAMAGE
import com.ayaziangames.mtg.model.DECLARE_ATTACKERS
import com.ayaziangames.mtg.model.DECLARE_BLOCKERS
import com.ayaziangames.mtg.model.DRAW_STEP
import com.ayaziangames.mtg.model.END_OF_COMBAT
import com.ayaziangames.mtg.model.ENDING
import com.ayaziangames.mtg.model.END_STEP
import com.ayaziangames.mtg.model.GamePhase
import com.ayaziangames.mtg.model.GameStep
import com.ayaziangames.mtg.model.MAIN_PHASE_1
import com.ayaziangames.mtg.model.MAIN_PHASE_2
import com.ayaziangames.mtg.model.UPKEEP

internal fun forestCardDatabase() = cardDefinitions {
    name = "cards"
    cardDefinition {
        name = "Forest"
    }
}

internal fun buildMinimalGame(phase: GamePhase, step: GameStep?) {
    game {
        availableCards = forestCardDatabase()
        gameState {
            minimalPlayer()
            activePlayer = 0
            this.phase = phase
            this.step = step
            if (isPriorityState(phase, step)) {
                priorityPlayer = 0
            }
        }
    }
}

internal fun gameWithSinglePlayer(
    activePlayer: Int = 0,
    init: @MtgDsl PlayerStateBuilder.() -> Unit
) {
    game {
        availableCards = forestCardDatabase()
        gameState {
            player(init)
            this.activePlayer = activePlayer
            phase = BEGINNING
            step = DRAW_STEP
        }
    }
}

internal fun GameStateBuilder.minimalPlayer() {
    player {
        deck {
            card("Forest")
        }
        library {
            deckCard(0)
        }
        life = 20
    }
}

private fun isPriorityState(phase: GamePhase, step: GameStep?): Boolean =
    when (phase) {
        GamePhase.BEGINNING -> step == UPKEEP
        GamePhase.MAIN_PHASE_1 -> step == null
        GamePhase.COMBAT -> step in setOf(BEGINNING_OF_COMBAT, DECLARE_ATTACKERS, DECLARE_BLOCKERS, COMBAT_DAMAGE, END_OF_COMBAT)
        GamePhase.MAIN_PHASE_2 -> step == null
        GamePhase.ENDING -> step == END_STEP
    }
