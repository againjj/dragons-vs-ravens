package com.ayaziangames.mtg.engine

import com.ayaziangames.mtg.dsl.cardDefinitions
import com.ayaziangames.mtg.dsl.game
import com.ayaziangames.mtg.dsl.actionByPlayer
import com.ayaziangames.mtg.model.ManaSymbol.G
import com.ayaziangames.mtg.model.ManaSymbol.M1
import com.ayaziangames.mtg.model.ManaSymbol.R
import com.ayaziangames.mtg.model.BEGINNING
import com.ayaziangames.mtg.model.BEGINNING_OF_COMBAT
import com.ayaziangames.mtg.model.CLEANUP
import com.ayaziangames.mtg.model.COMBAT
import com.ayaziangames.mtg.model.DECLARE_ATTACKERS
import com.ayaziangames.mtg.model.DECLARE_BLOCKERS
import com.ayaziangames.mtg.model.DRAW_STEP
import com.ayaziangames.mtg.model.COMBAT_DAMAGE
import com.ayaziangames.mtg.model.END_OF_COMBAT
import com.ayaziangames.mtg.model.END_STEP
import com.ayaziangames.mtg.model.ENDING
import com.ayaziangames.mtg.model.Game
import com.ayaziangames.mtg.model.GamePhase
import com.ayaziangames.mtg.model.GameStep
import com.ayaziangames.mtg.model.MAIN_PHASE_1
import com.ayaziangames.mtg.model.MAIN_PHASE_2
import com.ayaziangames.mtg.model.PLAYER_LOST
import com.ayaziangames.mtg.model.PLAYER_WON
import com.ayaziangames.mtg.model.PlayerState
import com.ayaziangames.mtg.model.UNTAP_STEP
import com.ayaziangames.mtg.model.UPKEEP
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame

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
        assertEquals(0, actual.gameState.priorityPlayer)
        assertEquals(testGame.log + actionByPlayer(0) { draw(1) }, actual.log)
    }

    @Test
    fun untapsAllTappedPermanentsAndAdvancesToUpkeep() {
        val baseGame = testGame()
        val testGame = baseGame.withTurnState(BEGINNING, UNTAP_STEP).withPlayer(
            0,
            baseGame.gameState.player[0].let { player ->
                player.update(
                    battlefield = player.battlefield.update(
                        permanents = player.battlefield.permanents.mapIndexed { index, permanent ->
                            permanent.update(tapped = index != 1)
                        }
                    )
                )
            }
        )

        val actual = engine.performAction(testGame, actionByPlayer(0) { untap(listOf(0, 2)) })

        assertEquals(listOf(false, false, false), actual.gameState.player[0].battlefield.permanents.map { it.tapped })
        assertEquals(BEGINNING, actual.gameState.phase)
        assertEquals(UPKEEP, actual.gameState.step)
        assertEquals(0, actual.gameState.priorityPlayer)
    }

    @Test
    fun untapWithNoTappedPermanentsDoesNotReplaceUnchangedStateObjects() {
        val game = testGame().withTurnState(BEGINNING, UNTAP_STEP)
        val activePlayerBeforeUntap = game.gameState.player[0]

        val actual = engine.performAction(game, actionByPlayer(0) { untap(emptyList()) })

        assertSame(activePlayerBeforeUntap, actual.gameState.player[0])
        assertSame(activePlayerBeforeUntap.battlefield, actual.gameState.player[0].battlefield)
        assertSame(activePlayerBeforeUntap.battlefield.permanents, actual.gameState.player[0].battlefield.permanents)
        assertEquals(BEGINNING, actual.gameState.phase)
        assertEquals(UPKEEP, actual.gameState.step)
        assertEquals(0, actual.gameState.priorityPlayer)
    }

    @Test
    fun priorityPassesMoveThroughTheTurnInPlayerOrder() {
        val baseGame = testGame()
        var game = baseGame.withTurnState(BEGINNING, UPKEEP).withPlayer(
            0,
            baseGame.gameState.player[0].let { player ->
                player.update(
                    library = player.library.update(cards = player.library.cards + player.hand.cards.last()),
                    hand = player.hand.update(cards = player.hand.cards.dropLast(1))
                )
            }
        )

        game = engine.performAction(game, actionByPlayer(0) { passPriority() })
        assertEquals(BEGINNING, game.gameState.phase)
        assertEquals(UPKEEP, game.gameState.step)
        assertEquals(1, game.gameState.priorityPlayer)

        game = engine.performAction(game, actionByPlayer(1) { passPriority() })
        assertEquals(BEGINNING, game.gameState.phase)
        assertEquals(DRAW_STEP, game.gameState.step)
        assertNull(game.gameState.priorityPlayer)

        game = engine.performAction(game, actionByPlayer(0) { draw(1) })
        assertEquals(MAIN_PHASE_1, game.gameState.phase)
        assertNull(game.gameState.step)
        assertEquals(0, game.gameState.priorityPlayer)

        val expectedPriorityStates = listOf(
            COMBAT to BEGINNING_OF_COMBAT,
            COMBAT to DECLARE_ATTACKERS,
            COMBAT to DECLARE_BLOCKERS,
            COMBAT to COMBAT_DAMAGE,
            COMBAT to END_OF_COMBAT,
            MAIN_PHASE_2 to null,
            ENDING to END_STEP,
            BEGINNING to UNTAP_STEP
        )
        for ((expectedPhase, expectedStep) in expectedPriorityStates) {
            game = engine.performAction(game, actionByPlayer(0) { passPriority() })
            assertEquals(1, game.gameState.priorityPlayer)
            game = engine.performAction(game, actionByPlayer(1) { passPriority() })
            assertEquals(expectedPhase, game.gameState.phase)
            assertEquals(expectedStep, game.gameState.step)
            if (expectedPhase == BEGINNING && expectedStep == UNTAP_STEP) {
                assertNull(game.gameState.priorityPlayer)
            } else {
                assertEquals(0, game.gameState.priorityPlayer)
            }
        }
        assertEquals(1, game.gameState.activePlayer)
    }

    @Test
    fun priorityPassRejectsOutOfOrderPlayers() {
        val game = testGame().withTurnState(BEGINNING, UPKEEP)

        val exception = assertFailsWith<IllegalArgumentException> {
            engine.performAction(game, actionByPlayer(1) { passPriority() })
        }

        assertEquals("Player 0 must pass priority next.", exception.message)
    }

    @Test
    fun priorityPassRejectsPriorityStatesWithoutAPriorityPlayer() {
        val game = testGame().update(
            gameState = testGame().gameState.update(
                phase = BEGINNING,
                step = UPKEEP,
                priorityPlayer = null
            )
        )

        val exception = assertFailsWith<IllegalArgumentException> {
            engine.performAction(game, actionByPlayer(0) { passPriority() })
        }

        assertEquals("Priority player is required while priority actions are valid.", exception.message)
    }

    @Test
    fun cleanupDiscardsDownToSevenCardsAndMovesDiscardedCardsToGraveyard() {
        val baseGame = testGame()
        val testGame = baseGame.withTurnState(ENDING, END_STEP).withPlayer(
            0,
            baseGame.gameState.player[0].let { player ->
                player.update(
                    library = player.library.update(cards = player.library.cards.drop(2)),
                    hand = player.hand.update(cards = player.hand.cards + player.library.cards.take(2))
                )
            }
        )
        var game = engine.performAction(testGame, actionByPlayer(0) { passPriority() })

        game = engine.performAction(game, actionByPlayer(1) { passPriority() })

        assertEquals(ENDING, game.gameState.phase)
        assertEquals(CLEANUP, game.gameState.step)
        assertNull(game.gameState.priorityPlayer)

        val firstDiscard = game.gameState.player[0].hand.cards[1]
        val secondDiscard = game.gameState.player[0].hand.cards[7]

        val actual = engine.performAction(game, actionByPlayer(0) { discardCards(listOf(1, 7)) })

        assertEquals(7, actual.gameState.player[0].hand.cards.size)
        assertEquals(listOf(firstDiscard, secondDiscard), actual.gameState.player[0].graveyard.cards)
        assertEquals(1, actual.gameState.activePlayer)
        assertEquals(BEGINNING, actual.gameState.phase)
        assertEquals(UNTAP_STEP, actual.gameState.step)
        assertNull(actual.gameState.priorityPlayer)
    }

    @Test
    fun emptyLibraryDrawMakesPlayerLoseAndRemainingPlayerWin() {
        val baseGame = testGame()
        val emptyLibraryPlayer = baseGame.gameState.player[0].let { player ->
            player.update(
                library = player.library.update(cards = emptyList()),
                hand = player.hand.update(cards = player.hand.cards + player.library.cards)
            )
        }
        val game = baseGame.withPlayer(0, emptyLibraryPlayer)

        val actual = engine.performAction(game, actionByPlayer(0) { draw(1) })

        assertEquals(PLAYER_LOST, actual.gameState.player[0].winLossState)
        assertEquals(PLAYER_WON, actual.gameState.player[1].winLossState)

        val gameOverException = assertFailsWith<IllegalArgumentException> {
            engine.performAction(actual, actionByPlayer(0) { passPriority() })
        }
        assertEquals("The game is over; no actions are valid.", gameOverException.message)
    }

    @Test
    fun drawRejectsInvalidActorStateAndCount() {
        assertActionFails(
            "Only the active player can draw during the draw step.",
            testGame(),
            actionByPlayer(1) { draw(1) }
        )
        assertActionFails(
            "Draw actions are only supported during the draw step.",
            testGame().withTurnState(MAIN_PHASE_1, null),
            actionByPlayer(0) { draw(1) }
        )
        assertActionFails(
            "Draw step draw actions must draw exactly 1 card.",
            testGame(),
            actionByPlayer(0) { draw(2) }
        )
    }

    @Test
    fun untapRejectsInvalidActorStateAndPermanentIndexes() {
        val baseGame = testGame()
        val untapGame = baseGame.withTurnState(BEGINNING, UNTAP_STEP).withPlayer(
            0,
            baseGame.gameState.player[0].update(
                battlefield = baseGame.gameState.player[0].battlefield.update(
                    permanents = baseGame.gameState.player[0].battlefield.permanents.mapIndexed { index, permanent ->
                        permanent.update(tapped = index == 0)
                    }
                )
            )
        )

        assertActionFails(
            "Only the active player can untap during the untap step.",
            untapGame,
            actionByPlayer(1) { untap(listOf(0)) }
        )
        assertActionFails(
            "Untap actions are only supported during the untap step.",
            testGame(),
            actionByPlayer(0) { untap(emptyList()) }
        )
        assertActionFails(
            "Untap must specify all tapped permanents in battlefield order: [0].",
            untapGame,
            actionByPlayer(0) { untap(emptyList()) }
        )
    }

    @Test
    fun priorityPassRejectsInvalidTurnState() {
        assertActionFails(
            "Pass priority actions are not supported during BEGINNING, DRAW_STEP.",
            testGame(),
            actionByPlayer(0) { passPriority() }
        )
    }

    @Test
    fun cleanupDiscardRejectsInvalidActorStateAndCardIndexes() {
        val cleanupGame = testGame().withTwoExtraCardsInActiveHand().withTurnState(ENDING, CLEANUP)

        assertActionFails(
            "Only the active player can discard during cleanup.",
            cleanupGame,
            actionByPlayer(1) { discardCards(listOf(0, 1)) }
        )
        assertActionFails(
            "Discard actions are only supported during cleanup.",
            testGame().withTwoExtraCardsInActiveHand(),
            actionByPlayer(0) { discardCards(listOf(0, 1)) }
        )
        assertActionFails(
            "Discard is only valid when the active player has more than 7 cards in hand.",
            testGame().withTurnState(ENDING, CLEANUP),
            actionByPlayer(0) { discardCards(emptyList()) }
        )
        assertActionFails(
            "Cleanup discard must specify exactly 2 card(s).",
            cleanupGame,
            actionByPlayer(0) { discardCards(listOf(0)) }
        )
        assertActionFails(
            "Cleanup discard cannot specify the same card more than once.",
            cleanupGame,
            actionByPlayer(0) { discardCards(listOf(0, 0)) }
        )
        assertActionFails(
            "Cleanup discard indexes must be in range 0..8.",
            cleanupGame,
            actionByPlayer(0) { discardCards(listOf(0, 9)) }
        )
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
                        graveyard {
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
                        graveyard {
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

    private fun Game.withTurnState(
        phase: GamePhase,
        step: GameStep?
    ): Game =
        update(gameState = gameState.update(phase = phase, step = step, priorityPlayer = priorityPlayerFor(phase, step)))

    private fun Game.withPlayer(playerIndex: Int, playerState: PlayerState): Game =
        update(
            gameState = gameState.update(
                player = gameState.player.mapIndexed { index, currentPlayer ->
                    if (index == playerIndex) playerState else currentPlayer
                }
            )
        )

    private fun Game.withTwoExtraCardsInActiveHand(): Game {
        val activePlayerState = gameState.player[gameState.activePlayer]
        return withPlayer(
            gameState.activePlayer,
            activePlayerState.update(
                library = activePlayerState.library.update(cards = activePlayerState.library.cards.drop(2)),
                hand = activePlayerState.hand.update(cards = activePlayerState.hand.cards + activePlayerState.library.cards.take(2))
            )
        )
    }

    private fun assertActionFails(
        expectedMessage: String,
        game: Game,
        action: com.ayaziangames.mtg.model.PlayerAction
    ) {
        val exception = assertFailsWith<IllegalArgumentException> {
            engine.performAction(game, action)
        }
        assertEquals(expectedMessage, exception.message)
    }

    private fun Game.priorityPlayerFor(phase: GamePhase, step: GameStep?): Int? =
        if (isPriorityState(phase, step)) gameState.activePlayer else null

    private fun isPriorityState(phase: GamePhase, step: GameStep?): Boolean =
        when (phase) {
            GamePhase.BEGINNING -> step == UPKEEP
            GamePhase.MAIN_PHASE_1 -> step == null
            GamePhase.COMBAT -> step in setOf(BEGINNING_OF_COMBAT, DECLARE_ATTACKERS, DECLARE_BLOCKERS, COMBAT_DAMAGE, END_OF_COMBAT)
            GamePhase.MAIN_PHASE_2 -> step == null
            GamePhase.ENDING -> step == END_STEP
        }
}
