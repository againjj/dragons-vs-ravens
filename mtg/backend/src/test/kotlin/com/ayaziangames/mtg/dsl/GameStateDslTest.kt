package com.ayaziangames.mtg.dsl

import com.ayaziangames.mtg.model.BEGINNING
import com.ayaziangames.mtg.model.BEGINNING_OF_COMBAT
import com.ayaziangames.mtg.model.Battlefield
import com.ayaziangames.mtg.model.CLEANUP
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
import com.ayaziangames.mtg.model.Hand
import com.ayaziangames.mtg.model.Library
import com.ayaziangames.mtg.model.MAIN_PHASE_1
import com.ayaziangames.mtg.model.MAIN_PHASE_2
import com.ayaziangames.mtg.model.ManaSymbol
import com.ayaziangames.mtg.model.UNTAP_STEP
import com.ayaziangames.mtg.model.UPKEEP
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertSame

class GameStateDslTest {
    @Test
    fun buildsGameStateFromNestedPlayerZones() {
        val cards = cardDefinitions {
            name = "cards"
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
                manaCost = listOf(ManaSymbol.M1, ManaSymbol.G)
                power = 2
                toughness = 2
            }
        }

        val builtGame = game {
            availableCards = cards
            gameState {
                player {
                    deck {
                        card("Forest")
                        card("Forest")
                        card("Grizzly Bears")
                        card("Forest")
                    }
                    library {
                        deckCard(3)
                    }
                    hand {
                        deckCard(0)
                    }
                    battlefield {
                        permanent {
                            deckCard(1)
                            tapped = true
                        }
                        permanent {
                            deckCard(2)
                            tapped = true
                        }
                    }
                    life = 17
                }
                activePlayer = 0
                phase = BEGINNING
                step = DRAW_STEP
            }
        }

        val player = builtGame.gameState.player.single()
        val firstForest = player.deck[0]
        val secondForest = player.deck[1]
        val bears = player.deck[2]
        val thirdForest = player.deck[3]

        assertNotEquals(firstForest, secondForest)
        assertSame(cards.cardNamed("Forest"), firstForest.definition)
        assertSame(cards.cardNamed("Forest"), secondForest.definition)
        assertSame(cards.cardNamed("Grizzly Bears"), bears.definition)
        assertSame(cards.cardNamed("Forest"), thirdForest.definition)
        assertSame(thirdForest, player.library.cards.single())
        assertSame(firstForest, player.hand.cards.single())

        assertEquals(2, player.battlefield.permanents.size)
        val firstPermanent = player.battlefield.permanents[0]
        val secondPermanent = player.battlefield.permanents[1]
        assertNotEquals(firstPermanent, secondPermanent)
        assertSame(secondForest, firstPermanent.card)
        assertSame(bears, secondPermanent.card)
        assertEquals(true, firstPermanent.tapped)
        assertEquals(true, secondPermanent.tapped)

        val libraryZone: com.ayaziangames.mtg.model.Zone = player.library
        val battlefieldZone: com.ayaziangames.mtg.model.Zone = player.battlefield
        val handZone: com.ayaziangames.mtg.model.Zone = player.hand
        assertSame(player.library, libraryZone)
        assertSame(player.battlefield, battlefieldZone)
        assertSame(player.hand, handZone)
        assertEquals(17, player.life)
    }

    @Test
    fun rejectsGameStateAndZoneFieldsAssignedMoreThanOnce() {
        assertDuplicateGameStateField("activePlayer") {
            minimalPlayer()
            activePlayer = 0
            activePlayer = 1
            phase = BEGINNING
        }
        assertDuplicateGameStateField("phase") {
            minimalPlayer()
            activePlayer = 0
            phase = BEGINNING
            phase = COMBAT
        }
        assertDuplicateGameStateField("step") {
            minimalPlayer()
            activePlayer = 0
            phase = BEGINNING
            step = UNTAP_STEP
            step = DRAW_STEP
        }

        assertDuplicatePlayerField("life") {
            deck {
                card("Forest")
            }
            library {
                deckCard(0)
            }
            life = 20
            life = 19
        }
        assertDuplicatePlayerField("deck") {
            deck {
                card("Forest")
            }
            deck {
                card("Forest")
            }
            library {
                deckCard(0)
            }
            life = 20
        }
        assertDuplicatePlayerField("library") {
            deck {
                card("Forest")
            }
            library {
                deckCard(0)
            }
            library {
                deckCard(0)
            }
            life = 20
        }
        assertDuplicatePlayerField("hand") {
            deck {
                card("Forest")
            }
            hand {
                deckCard(0)
            }
            hand {
                deckCard(0)
            }
            life = 20
        }
        assertDuplicatePlayerField("battlefield") {
            deck {
                card("Forest")
            }
            battlefield {
                permanent {
                    deckCard(0)
                }
            }
            battlefield {
            }
            life = 20
        }
    }

    @Test
    fun rejectsPermanentFieldsAssignedMoreThanOnce() {
        assertDuplicatePlayerField("card") {
            deck {
                card("Forest")
                card("Forest")
            }
            library {
                deckCard(1)
            }
            battlefield {
                permanent {
                    deckCard(0)
                    deckCard(1)
                }
            }
            life = 20
        }

        val directCardThenDeckCard = assertFailsWith<IllegalArgumentException> {
            val externalCard = com.ayaziangames.mtg.model.Card(forestCardDatabase().cardNamed("Forest"))
            gameWithSinglePlayer {
                deck {
                    card("Forest")
                }
                battlefield {
                    permanent {
                        card = externalCard
                        deckCard(0)
                    }
                }
                life = 20
            }
        }
        assertEquals("Property card can only be assigned once.", directCardThenDeckCard.message)

        val deckCardThenDirectCard = assertFailsWith<IllegalArgumentException> {
            val externalCard = com.ayaziangames.mtg.model.Card(forestCardDatabase().cardNamed("Forest"))
            gameWithSinglePlayer {
                deck {
                    card("Forest")
                }
                battlefield {
                    permanent {
                        deckCard(0)
                        card = externalCard
                    }
                }
                life = 20
            }
        }
        assertEquals("Property card can only be assigned once.", deckCardThenDirectCard.message)

        assertDuplicatePlayerField("tapped") {
            deck {
                card("Forest")
            }
            battlefield {
                permanent {
                    deckCard(0)
                    tapped = false
                    tapped = true
                }
            }
            life = 20
        }
    }

    @Test
    fun enforcesZoneCardConstraints() {
        val duplicateZoneCard = assertFailsWith<IllegalArgumentException> {
            gameWithSinglePlayer {
                deck {
                    card("Forest")
                }
                library {
                    deckCard(0)
                }
                hand {
                    deckCard(0)
                }
                life = 20
            }
        }
        assertEquals("A deck card can only be used once among all zones: Forest.", duplicateZoneCard.message)

        val duplicatePermanentCard = assertFailsWith<IllegalArgumentException> {
            gameWithSinglePlayer {
                deck {
                    card("Forest")
                }
                battlefield {
                    permanent {
                        deckCard(0)
                    }
                    permanent {
                        deckCard(0)
                    }
                }
                life = 20
            }
        }
        assertEquals("A deck card can only be used once among all zones: Forest.", duplicatePermanentCard.message)

        val missingZoneCard = assertFailsWith<IllegalArgumentException> {
            gameWithSinglePlayer {
                deck {
                    card("Forest")
                }
                life = 20
            }
        }
        assertEquals("All deck cards must occur in some zone: Forest.", missingZoneCard.message)
    }

    @Test
    fun rejectsZoneCardsFromAnotherDeck() {
        val otherDeckCard = game {
            availableCards = forestCardDatabase()
            gameState {
                minimalPlayer()
                activePlayer = 0
                phase = BEGINNING
                step = DRAW_STEP
            }
        }.gameState.player.single().deck.single()

        val libraryCard = assertFailsWith<IllegalArgumentException> {
            gameWithSinglePlayer {
                deck {
                    card("Forest")
                }
                library {
                    card(otherDeckCard)
                }
                life = 20
            }
        }
        assertEquals("Zones can only contain cards from the same player's deck: Forest.", libraryCard.message)

        val handCard = assertFailsWith<IllegalArgumentException> {
            gameWithSinglePlayer {
                deck {
                    card("Forest")
                }
                hand {
                    card(otherDeckCard)
                }
                life = 20
            }
        }
        assertEquals("Zones can only contain cards from the same player's deck: Forest.", handCard.message)

        val battlefieldCard = assertFailsWith<IllegalArgumentException> {
            gameWithSinglePlayer {
                deck {
                    card("Forest")
                }
                battlefield {
                    permanent {
                        card = otherDeckCard
                    }
                }
                life = 20
            }
        }
        assertEquals("Zones can only contain cards from the same player's deck: Forest.", battlefieldCard.message)
    }

    @Test
    fun validatesRequiredPlayerAndGameStateFields() {
        val missingDeck = assertFailsWith<IllegalArgumentException> {
            gameWithSinglePlayer {
                life = 20
            }
        }
        assertEquals("Player state requires deck.", missingDeck.message)

        val missingPhase = assertFailsWith<IllegalArgumentException> {
            game {
                availableCards = forestCardDatabase()
                gameState {
                    minimalPlayer()
                    activePlayer = 0
                }
            }
        }
        assertEquals("Game state requires phase.", missingPhase.message)
    }

    @Test
    fun validatesActivePlayerAndLifeRanges() {
        val activePlayer = assertFailsWith<IllegalArgumentException> {
            gameWithSinglePlayer(activePlayer = 1) {
                deck {
                    card("Forest")
                }
                library {
                    deckCard(0)
                }
                life = 20
            }
        }
        assertEquals("Active player 1 must be in range 0..0.", activePlayer.message)

        val life = assertFailsWith<IllegalArgumentException> {
            gameWithSinglePlayer {
                deck {
                    card("Forest")
                }
                library {
                    deckCard(0)
                }
                life = -1
            }
        }
        assertEquals("Player life must be >= 0.", life.message)
    }

    @Test
    fun acceptsOnlyLegalPhaseAndStepCombinations() {
        val legalCombinations = mapOf(
            BEGINNING to setOf(UNTAP_STEP, UPKEEP, DRAW_STEP),
            MAIN_PHASE_1 to setOf(null),
            COMBAT to setOf(BEGINNING_OF_COMBAT, DECLARE_ATTACKERS, DECLARE_BLOCKERS, COMBAT_DAMAGE, END_OF_COMBAT),
            MAIN_PHASE_2 to setOf(null),
            ENDING to setOf(END_STEP, CLEANUP)
        )

        for (phase in GamePhase.entries) {
            for (step in listOf<GameStep?>(null) + GameStep.entries) {
                if (step in legalCombinations.getValue(phase)) {
                    buildMinimalGame(phase, step)
                } else {
                    val exception = assertFailsWith<IllegalArgumentException> {
                        buildMinimalGame(phase, step)
                    }
                    assertEquals(
                        "Step ${step?.name ?: "null"} is not valid for phase ${phase.name}.",
                        exception.message
                    )
                }
            }
        }
    }

    private fun assertDuplicateGameStateField(
        field: String,
        init: @MtgDsl GameStateBuilder.() -> Unit
    ) {
        val exception = assertFailsWith<IllegalArgumentException> {
            game {
                availableCards = forestCardDatabase()
                gameState(init)
            }
        }
        assertEquals("Property $field can only be assigned once.", exception.message)
    }

    private fun assertDuplicatePlayerField(
        field: String,
        init: @MtgDsl PlayerStateBuilder.() -> Unit
    ) {
        val exception = assertFailsWith<IllegalArgumentException> {
            gameWithSinglePlayer(init = init)
        }
        assertEquals("Property $field can only be assigned once.", exception.message)
    }
}
