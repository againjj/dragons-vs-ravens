package com.ayaziangames.mtg.engine

import com.ayaziangames.mtg.dsl.cardDefinitions
import com.ayaziangames.mtg.dsl.game
import com.ayaziangames.mtg.dsl.actionByPlayer
import com.ayaziangames.mtg.model.ManaSymbol.G
import com.ayaziangames.mtg.model.ManaSymbol.M1
import com.ayaziangames.mtg.model.ManaSymbol.R
import com.ayaziangames.mtg.model.BEGINNING
import com.ayaziangames.mtg.model.DRAW_STEP
import com.ayaziangames.mtg.model.Game
import com.ayaziangames.mtg.model.MAIN_PHASE_1
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MtgEngineTest {
    @Test
    fun drawsACardForTheActivePlayerAndAdvancesToFirstMainPhase() {
        val testGame = testGame()

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

    @Test
    fun serializesTheEngineTestGameAsKotlinDsl() {
        val serialized = testGame().serialize()

        assertEquals(
            """
            game {
                availableCards = cardDatabase
                gameState {
                    player {
                        deck {
                            card("Grizzly Bears")
                            card("Grizzly Bears")
                            card("Grizzly Bears")
                            card("Grizzly Bears")
                            card("Grizzly Bears")
                            card("Grizzly Bears")
                            card("Grizzly Bears")
                            card("Grizzly Bears")
                            card("Grizzly Bears")
                            card("Grizzly Bears")
                            card("Grizzly Bears")
                            card("Grizzly Bears")
                            card("Grizzly Bears")
                            card("Grizzly Bears")
                            card("Grizzly Bears")
                            card("Grizzly Bears")
                            card("Grizzly Bears")
                            card("Grizzly Bears")
                            card("Grizzly Bears")
                            card("Grizzly Bears")
                            card("Grizzly Bears")
                            card("Grizzly Bears")
                            card("Grizzly Bears")
                            card("Grizzly Bears")
                            card("Grizzly Bears")
                            card("Grizzly Bears")
                            card("Grizzly Bears")
                            card("Grizzly Bears")
                            card("Grizzly Bears")
                            card("Grizzly Bears")
                            card("Grizzly Bears")
                            card("Grizzly Bears")
                            card("Grizzly Bears")
                            card("Grizzly Bears")
                            card("Grizzly Bears")
                            card("Grizzly Bears")
                            card("Forest")
                            card("Forest")
                            card("Forest")
                            card("Forest")
                            card("Forest")
                            card("Forest")
                            card("Forest")
                            card("Forest")
                            card("Forest")
                            card("Forest")
                            card("Forest")
                            card("Forest")
                            card("Forest")
                            card("Forest")
                            card("Forest")
                            card("Forest")
                            card("Forest")
                            card("Forest")
                            card("Forest")
                            card("Forest")
                            card("Forest")
                            card("Forest")
                            card("Forest")
                            card("Forest")
                        }
                        library {
                            deckCard(10)
                            deckCard(23)
                            deckCard(36)
                            deckCard(49)
                            deckCard(2)
                            deckCard(15)
                            deckCard(28)
                            deckCard(41)
                            deckCard(54)
                            deckCard(7)
                            deckCard(20)
                            deckCard(33)
                            deckCard(46)
                            deckCard(59)
                            deckCard(12)
                            deckCard(25)
                            deckCard(38)
                            deckCard(51)
                            deckCard(4)
                            deckCard(17)
                            deckCard(30)
                            deckCard(43)
                            deckCard(56)
                            deckCard(9)
                            deckCard(22)
                            deckCard(35)
                            deckCard(48)
                            deckCard(1)
                            deckCard(14)
                            deckCard(27)
                            deckCard(40)
                            deckCard(53)
                            deckCard(6)
                            deckCard(19)
                            deckCard(32)
                            deckCard(45)
                            deckCard(58)
                            deckCard(11)
                            deckCard(24)
                            deckCard(37)
                            deckCard(50)
                            deckCard(3)
                            deckCard(16)
                            deckCard(29)
                            deckCard(42)
                            deckCard(55)
                            deckCard(8)
                            deckCard(21)
                            deckCard(34)
                            deckCard(47)
                        }
                        hand {
                            deckCard(39)
                            deckCard(52)
                            deckCard(5)
                            deckCard(18)
                            deckCard(31)
                            deckCard(44)
                            deckCard(57)
                        }
                        battlefield {
                            permanent {
                                deckCard(0)
                                tapped = false
                            }
                            permanent {
                                deckCard(13)
                                tapped = false
                            }
                            permanent {
                                deckCard(26)
                                tapped = false
                            }
                        }
                        life = 20
                    }
                    player {
                        deck {
                            card("Mountain")
                            card("Mountain")
                            card("Mountain")
                            card("Mountain")
                            card("Mountain")
                            card("Mountain")
                            card("Mountain")
                            card("Mountain")
                            card("Mountain")
                            card("Mountain")
                            card("Mountain")
                            card("Mountain")
                            card("Mountain")
                            card("Mountain")
                            card("Mountain")
                            card("Mountain")
                            card("Mountain")
                            card("Mountain")
                            card("Mountain")
                            card("Mountain")
                            card("Mountain")
                            card("Mountain")
                            card("Mountain")
                            card("Mountain")
                            card("Goblin Assailant")
                            card("Goblin Assailant")
                            card("Goblin Assailant")
                            card("Goblin Assailant")
                            card("Goblin Assailant")
                            card("Goblin Assailant")
                            card("Goblin Assailant")
                            card("Goblin Assailant")
                            card("Goblin Assailant")
                            card("Goblin Assailant")
                            card("Goblin Assailant")
                            card("Goblin Assailant")
                            card("Goblin Assailant")
                            card("Goblin Assailant")
                            card("Goblin Assailant")
                            card("Goblin Assailant")
                            card("Goblin Assailant")
                            card("Goblin Assailant")
                            card("Goblin Assailant")
                            card("Goblin Assailant")
                            card("Goblin Assailant")
                            card("Goblin Assailant")
                            card("Goblin Assailant")
                            card("Goblin Assailant")
                            card("Goblin Assailant")
                            card("Goblin Assailant")
                            card("Goblin Assailant")
                            card("Goblin Assailant")
                            card("Goblin Assailant")
                            card("Goblin Assailant")
                            card("Goblin Assailant")
                            card("Goblin Assailant")
                            card("Goblin Assailant")
                            card("Goblin Assailant")
                            card("Goblin Assailant")
                            card("Goblin Assailant")
                        }
                        library {
                            deckCard(10)
                            deckCard(23)
                            deckCard(36)
                            deckCard(49)
                            deckCard(2)
                            deckCard(15)
                            deckCard(28)
                            deckCard(41)
                            deckCard(54)
                            deckCard(7)
                            deckCard(20)
                            deckCard(33)
                            deckCard(46)
                            deckCard(59)
                            deckCard(12)
                            deckCard(25)
                            deckCard(38)
                            deckCard(51)
                            deckCard(4)
                            deckCard(17)
                            deckCard(30)
                            deckCard(43)
                            deckCard(56)
                            deckCard(9)
                            deckCard(22)
                            deckCard(35)
                            deckCard(48)
                            deckCard(1)
                            deckCard(14)
                            deckCard(27)
                            deckCard(40)
                            deckCard(53)
                            deckCard(6)
                            deckCard(19)
                            deckCard(32)
                            deckCard(45)
                            deckCard(58)
                            deckCard(11)
                            deckCard(24)
                            deckCard(37)
                            deckCard(50)
                            deckCard(3)
                            deckCard(16)
                            deckCard(29)
                            deckCard(42)
                            deckCard(55)
                            deckCard(8)
                            deckCard(21)
                            deckCard(34)
                            deckCard(47)
                        }
                        hand {
                            deckCard(39)
                            deckCard(52)
                            deckCard(5)
                            deckCard(18)
                            deckCard(31)
                            deckCard(44)
                            deckCard(57)
                        }
                        battlefield {
                            permanent {
                                deckCard(0)
                                tapped = false
                            }
                            permanent {
                                deckCard(13)
                                tapped = false
                            }
                            permanent {
                                deckCard(26)
                                tapped = false
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
            """.trimIndent(),
            serialized
        )
    }

    private fun testGame(): Game {
        val cardDatabase = cardDefinitions {
            name = "cardDatabase"
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

        return game {
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
    }
}
