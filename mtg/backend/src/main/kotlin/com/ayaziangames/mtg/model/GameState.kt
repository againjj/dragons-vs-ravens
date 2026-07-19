package com.ayaziangames.mtg.model

class GameState(
    val player: List<PlayerState>,
    val activePlayer: Int,
    val phase: GamePhase,
    val step: GameStep? = null
) {
    fun update(
        player: List<PlayerState> = this.player,
        activePlayer: Int = this.activePlayer,
        phase: GamePhase = this.phase,
        step: GameStep? = this.step
    ): GameState =
        if (
            player === this.player &&
            activePlayer == this.activePlayer &&
            phase === this.phase &&
            step === this.step
        ) {
            this
        } else {
            GameState(
                player = player,
                activePlayer = activePlayer,
                phase = phase,
                step = step
            )
        }
}

class PlayerState(
    val deck: List<Card>,
    val library: Library,
    val battlefield: Battlefield,
    val hand: Hand,
    val life: Int
) {
    fun update(
        deck: List<Card> = this.deck,
        library: Library = this.library,
        battlefield: Battlefield = this.battlefield,
        hand: Hand = this.hand,
        life: Int = this.life
    ): PlayerState =
        if (
            deck === this.deck &&
            library === this.library &&
            battlefield === this.battlefield &&
            hand === this.hand &&
            life == this.life
        ) {
            this
        } else {
            PlayerState(
                deck = deck,
                library = library,
                battlefield = battlefield,
                hand = hand,
                life = life
            )
        }
}

sealed interface Zone {
    val cards: List<Card>
}

class Library(
    override val cards: List<Card>
) : Zone {
    fun update(
        cards: List<Card> = this.cards
    ): Library =
        if (cards === this.cards) {
            this
        } else {
            Library(cards)
        }
}

class Hand(
    override val cards: List<Card>
) : Zone {
    fun update(
        cards: List<Card> = this.cards
    ): Hand =
        if (cards === this.cards) {
            this
        } else {
            Hand(cards)
        }
}

class Battlefield(
    val permanents: List<Permanent>
) : Zone {
    override val cards: List<Card> = permanents.map { it.card }

    fun update(
        permanents: List<Permanent> = this.permanents
    ): Battlefield =
        if (permanents === this.permanents) {
            this
        } else {
            Battlefield(permanents)
        }
}

class Permanent(
    val card: Card,
    val tapped: Boolean
) {
    fun update(
        card: Card = this.card,
        tapped: Boolean = this.tapped
    ): Permanent =
        if (
            card === this.card &&
            tapped == this.tapped
        ) {
            this
        } else {
            Permanent(
                card = card,
                tapped = tapped
            )
        }
}

enum class GamePhase {
    BEGINNING,
    MAIN_PHASE_1,
    COMBAT,
    MAIN_PHASE_2,
    ENDING
}

enum class GameStep {
    UNTAP_STEP,
    UPKEEP,
    DRAW_STEP,
    BEGINNING_OF_COMBAT,
    DECLARE_ATTACKERS,
    DECLARE_BLOCKERS,
    COMBAT_DAMAGE,
    END_OF_COMBAT,
    END_STEP,
    CLEANUP
}

val BEGINNING = GamePhase.BEGINNING
val MAIN_PHASE_1 = GamePhase.MAIN_PHASE_1
val COMBAT = GamePhase.COMBAT
val MAIN_PHASE_2 = GamePhase.MAIN_PHASE_2
val ENDING = GamePhase.ENDING

val UNTAP_STEP = GameStep.UNTAP_STEP
val UPKEEP = GameStep.UPKEEP
val DRAW_STEP = GameStep.DRAW_STEP
val BEGINNING_OF_COMBAT = GameStep.BEGINNING_OF_COMBAT
val DECLARE_ATTACKERS = GameStep.DECLARE_ATTACKERS
val DECLARE_BLOCKERS = GameStep.DECLARE_BLOCKERS
val COMBAT_DAMAGE = GameStep.COMBAT_DAMAGE
val END_OF_COMBAT = GameStep.END_OF_COMBAT
val END_STEP = GameStep.END_STEP
val CLEANUP = GameStep.CLEANUP
