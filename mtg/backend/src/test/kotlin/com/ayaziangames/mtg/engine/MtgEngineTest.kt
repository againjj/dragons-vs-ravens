package com.ayaziangames.mtg.engine

import com.ayaziangames.mtg.dsl.cardDefinitions
import com.ayaziangames.mtg.dsl.game
import com.ayaziangames.mtg.dsl.actionByPlayer
import com.ayaziangames.mtg.model.ManaSymbol.G
import com.ayaziangames.mtg.model.ManaSymbol.M1
import com.ayaziangames.mtg.model.ManaSymbol.R
import com.ayaziangames.mtg.model.BEGINNING
import com.ayaziangames.mtg.model.DRAW_STEP
import com.ayaziangames.mtg.model.MAIN_PHASE_1
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MtgEngineTest {
    @Test
    fun drawsACardForTheActivePlayerAndAdvancesToFirstMainPhase() {
        val cardDatabase = cardDefinitions {
            cardDefinition {
                name = "Forest"
                superTypes = listOf("Basic")
                types = listOf("Land")
                subTypes = listOf("Forest")
            }
            cardDefinition {
                name = "Grizzly Bears"
                types = listOf("Creature")
                subTypes = listOf("Bear")
                manaCost = listOf(M1, G)
                power = 2
                toughness = 2
            }
            cardDefinition {
                name = "Mountain"
                superTypes = listOf("Basic")
                types = listOf("Land")
                subTypes = listOf("Mountain")
            }
            cardDefinition {
                name = "Goblin Assailant"
                types = listOf("Creature")
                subTypes = listOf("Goblin", "Warrior")
                manaCost = listOf(M1, R)
                power = 2
                toughness = 2
            }
        }
        val testGame = game {
            availableCards = cardDatabase
            gameState {
                player {
                    deck {
                        for (i in 0 until 36) {
                            card("Grizzly Bears")
                        }
                        for (i in 0 until 24) {
                            card("Forest")
                        }
                    }
                    library {
                        for (i in 10 until 60) {
                            deckCard(i * 13 % 60)
                        }
                    }
                    hand {
                        for (i in 3 until 10) {
                            deckCard(i * 13 % 60)
                        }
                    }
                    battlefield {
                        for (i in 0 until 3) {
                            permanent {
                                deckCard(i * 13 % 60)
                                tapped = false
                            }
                        }
                    }
                    life = 20
                }
                player {
                    deck {
                        for (i in 0 until 24) {
                            card("Mountain")
                        }
                        for (i in 0 until 36) {
                            card("Goblin Assailant")
                        }
                    }
                    library {
                        for (i in 10 until 60) {
                            deckCard(i * 13 % 60)
                        }
                    }
                    hand {
                        for (i in 3 until 10) {
                            deckCard(i * 13 % 60)
                        }
                    }
                    battlefield {
                        for (i in 0 until 3) {
                            permanent {
                                deckCard(i * 13 % 60)
                                tapped = false
                            }
                        }
                    }
                    life = 20
                }
                activePlayer = 0
                phase = BEGINNING
                step = DRAW_STEP
            }
            log = emptyList()
        }

        val actual = engine.performAction(testGame, actionByPlayer(0) { draw(1) })

        assertEquals(testGame.availableCards, actual.availableCards)
        assertEquals(testGame.gameState.player[0].deck, actual.gameState.player[0].deck)
        assertEquals(testGame.gameState.player[0].library.cards.drop(1), actual.gameState.player[0].library.cards)
        assertEquals(
            testGame.gameState.player[0].hand.cards + testGame.gameState.player[0].library.cards[0],
            actual.gameState.player[0].hand.cards
        )
        assertEquals(
            testGame.gameState.player[0].battlefield.permanents,
            actual.gameState.player[0].battlefield.permanents
        )
        assertEquals(testGame.gameState.player[0].life, actual.gameState.player[0].life)
        assertEquals(testGame.gameState.player[1], actual.gameState.player[1])
        assertEquals(testGame.gameState.activePlayer, actual.gameState.activePlayer)
        assertEquals(MAIN_PHASE_1, actual.gameState.phase)
        assertNull(actual.gameState.step)
        assertEquals(testGame.log + actionByPlayer(0) { draw(1) }, actual.log)
    }
}
