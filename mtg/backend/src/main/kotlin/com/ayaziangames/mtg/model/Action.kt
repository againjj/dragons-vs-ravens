package com.ayaziangames.mtg.model

data class PlayerAction(
    val playerIndex: Int,
    val command: ActionCommand
) {
    fun serialize(): String =
        "actionByPlayer($playerIndex) { ${command.serialize()} }"

    fun update(
        playerIndex: Int = this.playerIndex,
        command: ActionCommand = this.command
    ): PlayerAction =
        if (
            playerIndex == this.playerIndex &&
            command === this.command
        ) {
            this
        } else {
            PlayerAction(
                playerIndex = playerIndex,
                command = command
            )
        }
}

sealed interface ActionCommand {
    fun serialize(): String
}

data class DrawCommand(val count: Int) : ActionCommand {
    override fun serialize(): String =
        "draw($count)"

    fun update(
        count: Int = this.count
    ): DrawCommand =
        if (count == this.count) {
            this
        } else {
            DrawCommand(count)
        }
}

data class UntapCommand(val permanentIndexes: List<Int>) : ActionCommand {
    override fun serialize(): String =
        "untap(${permanentIndexes.joinToString(prefix = "listOf(", postfix = ")")})"

    fun update(
        permanentIndexes: List<Int> = this.permanentIndexes
    ): UntapCommand =
        if (permanentIndexes === this.permanentIndexes) {
            this
        } else {
            UntapCommand(permanentIndexes)
        }
}

data object PassPriorityCommand : ActionCommand {
    override fun serialize(): String =
        "passPriority()"
}

data class DiscardCardsCommand(val cardIndexes: List<Int>) : ActionCommand {
    override fun serialize(): String =
        "discardCards(${cardIndexes.joinToString(prefix = "listOf(", postfix = ")")})"

    fun update(
        cardIndexes: List<Int> = this.cardIndexes
    ): DiscardCardsCommand =
        if (cardIndexes === this.cardIndexes) {
            this
        } else {
            DiscardCardsCommand(cardIndexes)
        }
}
