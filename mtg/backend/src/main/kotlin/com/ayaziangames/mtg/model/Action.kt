package com.ayaziangames.mtg.model

data class PlayerAction(
    val playerIndex: Int,
    val command: ActionCommand
) {
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

sealed interface ActionCommand

data class DrawCommand(val count: Int) : ActionCommand {
    fun update(
        count: Int = this.count
    ): DrawCommand =
        if (count == this.count) {
            this
        } else {
            DrawCommand(count)
        }
}
