package com.ayaziangames.mtg.dsl

import com.ayaziangames.mtg.model.BEGINNING
import com.ayaziangames.mtg.model.DRAW_STEP
import com.ayaziangames.mtg.model.GamePhase
import com.ayaziangames.mtg.model.GameStep

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
