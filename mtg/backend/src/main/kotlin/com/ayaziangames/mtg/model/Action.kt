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
