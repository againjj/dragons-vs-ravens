package com.ayaziangames.mtg.dsl

import com.ayaziangames.mtg.model.BEGINNING_OF_COMBAT
import com.ayaziangames.mtg.model.Battlefield
import com.ayaziangames.mtg.model.CLEANUP
import com.ayaziangames.mtg.model.COMBAT_DAMAGE
import com.ayaziangames.mtg.model.Card
import com.ayaziangames.mtg.model.CardDatabase
import com.ayaziangames.mtg.model.DECLARE_ATTACKERS
import com.ayaziangames.mtg.model.DECLARE_BLOCKERS
import com.ayaziangames.mtg.model.DRAW_STEP
import com.ayaziangames.mtg.model.END_OF_COMBAT
import com.ayaziangames.mtg.model.END_STEP
import com.ayaziangames.mtg.model.GamePhase
import com.ayaziangames.mtg.model.GamePhase.BEGINNING
import com.ayaziangames.mtg.model.GamePhase.COMBAT
import com.ayaziangames.mtg.model.GamePhase.ENDING
import com.ayaziangames.mtg.model.GamePhase.MAIN_PHASE_1
import com.ayaziangames.mtg.model.GamePhase.MAIN_PHASE_2
import com.ayaziangames.mtg.model.GameState
import com.ayaziangames.mtg.model.GameStep
import com.ayaziangames.mtg.model.Hand
import com.ayaziangames.mtg.model.Library
import com.ayaziangames.mtg.model.Permanent
import com.ayaziangames.mtg.model.PlayerState
import com.ayaziangames.mtg.model.UNTAP_STEP
import com.ayaziangames.mtg.model.UPKEEP
import java.util.IdentityHashMap

@MtgDsl
class GameStateBuilder internal constructor(
    private val availableCards: CardDatabase
) {
    private val players = mutableListOf<PlayerState>()
    private val assignedActivePlayer = SingleAssignment<Int?>("activePlayer", null)
    private val assignedPhase = SingleAssignment<GamePhase?>("phase", null)
    private val assignedStep = SingleAssignment<GameStep?>("step", null)

    var activePlayer: Int?
        get() = assignedActivePlayer.value
        set(value) = assignedActivePlayer.set(value)
    var phase: GamePhase?
        get() = assignedPhase.value
        set(value) = assignedPhase.set(value)
    var step: GameStep?
        get() = assignedStep.value
        set(value) = assignedStep.set(value)

    fun player(init: @MtgDsl PlayerStateBuilder.() -> Unit) {
        players += PlayerStateBuilder(availableCards).apply(init).build()
    }

    internal fun build(): GameState {
        val builtPhase = requireNotNull(phase) { "Game state requires phase." }
        validatePhaseStep(builtPhase, step)
        val builtActivePlayer = requireNotNull(activePlayer) { "Game state requires activePlayer." }
        require(builtActivePlayer in players.indices) {
            "Active player $builtActivePlayer must be in range 0..${players.lastIndex}."
        }
        return GameState(
            player = players.toList(),
            activePlayer = builtActivePlayer,
            phase = builtPhase,
            step = step
        )
    }
}

@MtgDsl
class PlayerStateBuilder internal constructor(
    private val availableCards: CardDatabase
) {
    private val assignedDeck = SingleAssignment<List<Card>?>("deck", null)
    private val assignedLibrary = SingleAssignment("library", Library(emptyList()))
    private val assignedHand = SingleAssignment("hand", Hand(emptyList()))
    private val assignedBattlefield = SingleAssignment("battlefield", Battlefield(emptyList()))
    private val assignedLife = SingleAssignment<Int?>("life", null)

    var life: Int?
        get() = assignedLife.value
        set(value) = assignedLife.set(value)

    fun deck(init: @MtgDsl DeckBuilder.() -> Unit) {
        assignedDeck.set(DeckBuilder(availableCards).apply(init).build())
    }

    fun library(init: @MtgDsl CardZoneBuilder.() -> Unit) {
        assignedLibrary.set(Library(CardZoneBuilder(deck()).apply(init).build()))
    }

    fun hand(init: @MtgDsl CardZoneBuilder.() -> Unit) {
        assignedHand.set(Hand(CardZoneBuilder(deck()).apply(init).build()))
    }

    fun battlefield(init: @MtgDsl BattlefieldBuilder.() -> Unit) {
        assignedBattlefield.set(Battlefield(BattlefieldBuilder(deck()).apply(init).build()))
    }

    internal fun build(): PlayerState {
        val builtDeck = deck()
        val builtLibrary = assignedLibrary.value
        val builtBattlefield = assignedBattlefield.value
        val builtHand = assignedHand.value
        val builtLife = requireNotNull(life) { "Player state requires life." }
        require(builtLife >= 0) { "Player life must be >= 0." }
        validateZonesUseDeckCardsExactlyOnce(builtDeck, builtLibrary, builtHand, builtBattlefield)
        return PlayerState(
            deck = builtDeck.toList(),
            library = Library(builtLibrary.cards.toList()),
            battlefield = Battlefield(builtBattlefield.permanents.toList()),
            hand = Hand(builtHand.cards.toList()),
            life = builtLife
        )
    }

    private fun deck(): List<Card> =
        requireNotNull(assignedDeck.value) { "Player state requires deck." }

    private fun validateZonesUseDeckCardsExactlyOnce(
        deck: List<Card>,
        library: Library,
        hand: Hand,
        battlefield: Battlefield
    ) {
        val deckCards = deck.identitySet()
        val zoneCards = library.cards + hand.cards + battlefield.cards
        val cardsFromOtherDecks = zoneCards.filter { it !in deckCards }
        require(cardsFromOtherDecks.isEmpty()) {
            "Zones can only contain cards from the same player's deck: ${cardsFromOtherDecks.names()}."
        }

        val usedCards = identitySetOf<Card>()
        val duplicateCards = mutableListOf<Card>()
        for (card in zoneCards) {
            if (!usedCards.add(card)) {
                duplicateCards += card
            }
        }
        require(duplicateCards.isEmpty()) {
            "A deck card can only be used once among all zones: ${duplicateCards.names()}."
        }

        val missingCards = deck.filter { it !in usedCards }
        require(missingCards.isEmpty()) {
            "All deck cards must occur in some zone: ${missingCards.names()}."
        }
    }
}

@MtgDsl
class CardZoneBuilder internal constructor(
    private val deck: List<Card>
) {
    private val cards = mutableListOf<Card>()

    fun deckCard(index: Int) {
        cards += deck.cardAt(index)
    }

    internal fun card(card: Card) {
        cards += card
    }

    internal fun build(): List<Card> = cards.toList()
}

@MtgDsl
class BattlefieldBuilder internal constructor(
    private val deck: List<Card>
) {
    private val permanents = mutableListOf<Permanent>()

    fun permanent(init: @MtgDsl PermanentBuilder.() -> Unit) {
        permanents += PermanentBuilder(deck).apply(init).build()
    }

    internal fun build(): List<Permanent> = permanents.toList()
}

@MtgDsl
class PermanentBuilder internal constructor(
    private val deck: List<Card>
) {
    private val assignedCard = SingleAssignment<Card?>("card", null)
    private val assignedTapped = SingleAssignment("tapped", false)

    var card: Card?
        get() = assignedCard.value
        set(value) = assignedCard.set(value)
    var tapped: Boolean
        get() = assignedTapped.value
        set(value) = assignedTapped.set(value)

    fun deckCard(index: Int) {
        card = deck.cardAt(index)
    }

    internal fun build(): Permanent =
        Permanent(
            card = requireNotNull(card) { "Permanent requires a card." },
            tapped = tapped
        )
}

private fun List<Card>.cardAt(index: Int): Card =
    getOrNull(index) ?: error("Deck does not contain a card at index $index.")

private fun validatePhaseStep(phase: GamePhase, step: GameStep?) {
    val validSteps = when (phase) {
        BEGINNING -> setOf(UNTAP_STEP, UPKEEP, DRAW_STEP)
        MAIN_PHASE_1 -> setOf(null)
        COMBAT -> setOf(BEGINNING_OF_COMBAT, DECLARE_ATTACKERS, DECLARE_BLOCKERS, COMBAT_DAMAGE, END_OF_COMBAT)
        MAIN_PHASE_2 -> setOf(null)
        ENDING -> setOf(END_STEP, CLEANUP)
    }
    require(step in validSteps) {
        "Step ${step?.name ?: "null"} is not valid for phase ${phase.name}."
    }
}

private fun <T : Any> identitySetOf(): MutableSet<T> =
    java.util.Collections.newSetFromMap(IdentityHashMap())

private fun <T : Any> Iterable<T>.identitySet(): MutableSet<T> =
    identitySetOf<T>().also { it.addAll(this) }

private fun Iterable<Card>.names(): String =
    joinToString { it.definition.name }
