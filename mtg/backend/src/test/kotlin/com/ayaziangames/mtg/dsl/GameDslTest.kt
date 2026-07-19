package com.ayaziangames.mtg.dsl

import com.ayaziangames.mtg.model.BEGINNING
import com.ayaziangames.mtg.model.DRAW_STEP
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class GameDslTest {
    @Test
    fun buildsGamesWithAvailableCardsStateAndLog() {
        val cards = forestCardDatabase()
        val action = actionByPlayer(0) { draw(1) }

        val builtGame = game {
            availableCards = cards
            gameState {
                minimalPlayer()
                activePlayer = 0
                phase = BEGINNING
                step = DRAW_STEP
            }
            log = listOf(action)
        }

        assertSame(cards, builtGame.availableCards)
        assertEquals(0, builtGame.gameState.activePlayer)
        assertEquals(BEGINNING, builtGame.gameState.phase)
        assertEquals(DRAW_STEP, builtGame.gameState.step)
        assertEquals(listOf(action), builtGame.log)
    }

    @Test
    fun rejectsGameFieldsAssignedMoreThanOnce() {
        val cards = forestCardDatabase()

        val duplicateAvailableCards = assertFailsWith<IllegalArgumentException> {
            game {
                this.availableCards = cards
                this.availableCards = cards
            }
        }
        assertEquals("Property availableCards can only be assigned once.", duplicateAvailableCards.message)

        val log = assertFailsWith<IllegalArgumentException> {
            val action = actionByPlayer(0) { draw(1) }
            game {
                availableCards = cards
                this.log = listOf(action)
                this.log = emptyList()
            }
        }
        assertEquals("Property log can only be assigned once.", log.message)

        val gameState = assertFailsWith<IllegalArgumentException> {
            game {
                availableCards = cards
                gameState {
                    minimalPlayer()
                    activePlayer = 0
                    phase = BEGINNING
                    step = DRAW_STEP
                }
                gameState {
                    minimalPlayer()
                    activePlayer = 0
                    phase = BEGINNING
                    step = DRAW_STEP
                }
            }
        }
        assertEquals("Property gameState can only be assigned once.", gameState.message)
    }
}
