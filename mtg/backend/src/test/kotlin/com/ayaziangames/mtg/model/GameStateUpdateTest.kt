package com.ayaziangames.mtg.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class GameStateUpdateTest {
    @Test
    fun updateReturnsSameInstancesWhenGameStateFieldsAreUnchanged() {
        val fixtures = gameStateFixtures()

        assertSame(fixtures.library, fixtures.library.update())
        assertSame(fixtures.hand, fixtures.hand.update())
        assertSame(fixtures.graveyard, fixtures.graveyard.update())
        assertSame(fixtures.permanent, fixtures.permanent.update())
        assertSame(fixtures.battlefield, fixtures.battlefield.update())
        assertSame(fixtures.playerState, fixtures.playerState.update())
        assertSame(fixtures.gameState, fixtures.gameState.update())
    }

    @Test
    fun updateReturnsNewLibraryWhenCardsChange() {
        val fixtures = gameStateFixtures()

        val updated = fixtures.library.update(cards = emptyList())

        assertNotSame(fixtures.library, updated)
        assertEquals(emptyList(), updated.cards)
    }

    @Test
    fun updateReturnsNewHandWhenCardsChange() {
        val fixtures = gameStateFixtures()

        val updated = fixtures.hand.update(cards = listOf(fixtures.card))

        assertNotSame(fixtures.hand, updated)
        assertEquals(listOf(fixtures.card), updated.cards)
    }

    @Test
    fun updateReturnsNewGraveyardWhenCardsChange() {
        val fixtures = gameStateFixtures()

        val updated = fixtures.graveyard.update(cards = listOf(fixtures.card))

        assertNotSame(fixtures.graveyard, updated)
        assertEquals(listOf(fixtures.card), updated.cards)
    }

    @Test
    fun updateReturnsNewPermanentWhenFieldsChange() {
        val fixtures = gameStateFixtures()
        val otherCard = Card(CardDefinition(name = "Mountain"))

        val updated = fixtures.permanent.update(card = otherCard, tapped = true)

        assertNotSame(fixtures.permanent, updated)
        assertSame(otherCard, updated.card)
        assertEquals(true, updated.tapped)
    }

    @Test
    fun updateReturnsNewBattlefieldWhenPermanentsChange() {
        val fixtures = gameStateFixtures()
        val otherPermanent = Permanent(fixtures.card, tapped = true)

        val updated = fixtures.battlefield.update(permanents = listOf(otherPermanent))

        assertNotSame(fixtures.battlefield, updated)
        assertEquals(listOf(otherPermanent), updated.permanents)
    }

    @Test
    fun updateReturnsNewPlayerStateWhenFieldsChange() {
        val fixtures = gameStateFixtures()
        val updatedLibrary = fixtures.library.update(cards = emptyList())
        val updatedHand = fixtures.hand.update(cards = listOf(fixtures.card))
        val updatedGraveyard = fixtures.graveyard.update(cards = listOf(fixtures.card))

        val updated = fixtures.playerState.update(
            library = updatedLibrary,
            hand = updatedHand,
            graveyard = updatedGraveyard,
            life = 19,
            winLossState = WinLossState.PLAYER_LOST
        )

        assertNotSame(fixtures.playerState, updated)
        assertSame(updatedLibrary, updated.library)
        assertSame(updatedHand, updated.hand)
        assertSame(updatedGraveyard, updated.graveyard)
        assertEquals(19, updated.life)
        assertEquals(WinLossState.PLAYER_LOST, updated.winLossState)
    }

    @Test
    fun updateReturnsNewGameStateWhenFieldsChange() {
        val fixtures = gameStateFixtures()
        val updatedPlayer = fixtures.playerState.update(life = 19)

        val updated = fixtures.gameState.update(
            player = listOf(updatedPlayer),
            phase = GamePhase.MAIN_PHASE_1,
            step = null,
            priorityPlayer = 0
        )

        assertNotSame(fixtures.gameState, updated)
        assertEquals(listOf(updatedPlayer), updated.player)
        assertEquals(GamePhase.MAIN_PHASE_1, updated.phase)
        assertEquals(null, updated.step)
        assertEquals(0, updated.priorityPlayer)
    }

    private fun gameStateFixtures(): GameStateFixtures {
        val definition = CardDefinition(name = "Forest")
        val card = Card(definition)
        val library = Library(listOf(card))
        val hand = Hand(emptyList())
        val graveyard = Graveyard(emptyList())
        val permanent = Permanent(card, tapped = false)
        val battlefield = Battlefield(listOf(permanent))
        val playerState = PlayerState(
            deck = listOf(card),
            library = library,
            battlefield = battlefield,
            hand = hand,
            life = 20,
            graveyard = graveyard
        )
        val gameState = GameState(
            player = listOf(playerState),
            activePlayer = 0,
            phase = GamePhase.BEGINNING,
            step = GameStep.DRAW_STEP
        )
        return GameStateFixtures(
            card = card,
            library = library,
            hand = hand,
            graveyard = graveyard,
            permanent = permanent,
            battlefield = battlefield,
            playerState = playerState,
            gameState = gameState
        )
    }

    private class GameStateFixtures(
        val card: Card,
        val library: Library,
        val hand: Hand,
        val graveyard: Graveyard,
        val permanent: Permanent,
        val battlefield: Battlefield,
        val playerState: PlayerState,
        val gameState: GameState
    )
}
