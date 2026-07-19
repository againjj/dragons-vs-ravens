package com.ayaziangames.mtg.dsl

import com.ayaziangames.mtg.model.BEGINNING
import com.ayaziangames.mtg.model.DRAW_STEP
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertSame

class CardDslTest {
    @Test
    fun buildsDeckCardsAsUniqueInstancesOfDatabaseDefinitions() {
        val cards = cardDefinitions {
            name = "cards"
            cardDefinition {
                name = "Forest"
            }
        }

        val builtGame = game {
            availableCards = cards
            gameState {
                player {
                    deck {
                        card("Forest")
                        card("Forest")
                    }
                    library {
                        deckCard(0)
                    }
                    hand {
                        deckCard(1)
                    }
                    life = 20
                }
                activePlayer = 0
                phase = BEGINNING
                step = DRAW_STEP
            }
        }

        val deck = builtGame.gameState.player.single().deck
        assertEquals(2, deck.size)
        assertNotEquals(deck[0], deck[1])
        assertSame(cards.cardNamed("Forest"), deck[0].definition)
        assertSame(cards.cardNamed("Forest"), deck[1].definition)
    }

    @Test
    fun rejectsDeckCardsThatAreMissingFromTheAvailableCardDatabase() {
        val exception = assertFailsWith<IllegalStateException> {
            game {
                availableCards = forestCardDatabase()
                gameState {
                    player {
                        deck {
                            card("Mountain")
                        }
                        library {
                            deckCard(0)
                        }
                        life = 20
                    }
                    activePlayer = 0
                    phase = BEGINNING
                }
            }
        }

        assertEquals("Unknown card definition: Mountain.", exception.message)
    }
}
