package com.ayaziangames.mtg.model

class GameState(
    val player: List<PlayerState>,
    val activePlayer: Int,
    val phase: GamePhase,
    val step: GameStep? = null,
    val priorityPlayer: Int? = null
) {
    fun serialize(): String =
        buildString {
            appendLine("gameState {")
            for (playerState in player) {
                appendLine(playerState.serialize().indentBy(4))
            }
            appendLine("    activePlayer = $activePlayer")
            appendLine("    phase = ${phase.name}")
            if (step != null) {
                appendLine("    step = ${step.name}")
            }
            if (priorityPlayer != null) {
                appendLine("    priorityPlayer = $priorityPlayer")
            }
            append("}")
        }

    fun update(
        player: List<PlayerState> = this.player,
        activePlayer: Int = this.activePlayer,
        phase: GamePhase = this.phase,
        step: GameStep? = this.step,
        priorityPlayer: Int? = this.priorityPlayer
    ): GameState =
        if (
            player === this.player &&
            activePlayer == this.activePlayer &&
            phase === this.phase &&
            step === this.step &&
            priorityPlayer == this.priorityPlayer
        ) {
            this
        } else {
            GameState(
                player = player,
                activePlayer = activePlayer,
                phase = phase,
                step = step,
                priorityPlayer = priorityPlayer
            )
        }
}

class PlayerState(
    val deck: List<Card>,
    val library: Library,
    val battlefield: Battlefield,
    val hand: Hand,
    val life: Int,
    val graveyard: Graveyard = Graveyard(emptyList()),
    val winLossState: WinLossState? = null
) {
    fun serialize(): String =
        buildString {
            appendLine("player {")
            appendLine("    deck {")
            append(deck.joinToString("\n") { it.serialize().indentBy(8) })
            appendLine()
            appendLine("    }")
            appendLine(library.serialize(deck).indentBy(4))
            appendLine(hand.serialize(deck).indentBy(4))
            appendLine(battlefield.serialize(deck).indentBy(4))
            appendLine(graveyard.serialize(deck).indentBy(4))
            appendLine("    life = $life")
            if (winLossState != null) {
                appendLine("    winLossState = ${winLossState.name}")
            }
            append("}")
        }

    fun update(
        deck: List<Card> = this.deck,
        library: Library = this.library,
        battlefield: Battlefield = this.battlefield,
        hand: Hand = this.hand,
        graveyard: Graveyard = this.graveyard,
        life: Int = this.life,
        winLossState: WinLossState? = this.winLossState
    ): PlayerState =
        if (
            deck === this.deck &&
            library === this.library &&
            battlefield === this.battlefield &&
            hand === this.hand &&
            graveyard === this.graveyard &&
            life == this.life &&
            winLossState === this.winLossState
        ) {
            this
        } else {
            PlayerState(
                deck = deck,
                library = library,
                battlefield = battlefield,
                hand = hand,
                life = life,
                graveyard = graveyard,
                winLossState = winLossState
            )
        }
}

sealed interface Zone {
    val cards: List<Card>
}

class Library(
    override val cards: List<Card>
) : Zone {
    fun serialize(deck: List<Card>): String =
        buildString {
            appendLine("library {")
            append(cards.joinToString("\n") { "    deckCard(${deck.indexOfCardByIdentity(it)})" })
            if (cards.isNotEmpty()) {
                appendLine()
            }
            append("}")
        }

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
    fun serialize(deck: List<Card>): String =
        buildString {
            appendLine("hand {")
            append(cards.joinToString("\n") { "    deckCard(${deck.indexOfCardByIdentity(it)})" })
            if (cards.isNotEmpty()) {
                appendLine()
            }
            append("}")
        }

    fun update(
        cards: List<Card> = this.cards
    ): Hand =
        if (cards === this.cards) {
            this
        } else {
            Hand(cards)
        }
}

class Graveyard(
    override val cards: List<Card>
) : Zone {
    fun serialize(deck: List<Card>): String =
        buildString {
            appendLine("graveyard {")
            append(cards.joinToString("\n") { "    deckCard(${deck.indexOfCardByIdentity(it)})" })
            if (cards.isNotEmpty()) {
                appendLine()
            }
            append("}")
        }

    fun update(
        cards: List<Card> = this.cards
    ): Graveyard =
        if (cards === this.cards) {
            this
        } else {
            Graveyard(cards)
        }
}

class Battlefield(
    val permanents: List<Permanent>
) : Zone {
    override val cards: List<Card> = permanents.map { it.card }

    fun serialize(deck: List<Card>): String =
        buildString {
            appendLine("battlefield {")
            append(permanents.joinToString("\n") { it.serialize(deck).indentBy(4) })
            if (permanents.isNotEmpty()) {
                appendLine()
            }
            append("}")
        }

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
    fun serialize(deck: List<Card>): String =
        buildString {
            appendLine("permanent {")
            appendLine("    deckCard(${deck.indexOfCardByIdentity(card)})")
            appendLine("    tapped = $tapped")
            append("}")
        }

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

enum class WinLossState {
    PLAYER_LOST,
    PLAYER_WON
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

val PLAYER_LOST = WinLossState.PLAYER_LOST
val PLAYER_WON = WinLossState.PLAYER_WON

private fun List<Card>.indexOfCardByIdentity(card: Card): Int {
    val index = indexOfFirst { it === card }
    require(index >= 0) { "Zone card ${card.definition.name} is not in the player's deck." }
    return index
}
