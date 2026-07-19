package com.ayaziangames.mtg.dsl

import com.ayaziangames.mtg.model.ActionCommand
import com.ayaziangames.mtg.model.DrawCommand
import com.ayaziangames.mtg.model.PlayerAction

@MtgDsl
class PlayerActionBuilder internal constructor() {
    private var command: ActionCommand? = null

    fun draw(count: Int) {
        require(count > 0) { "Draw count must be positive." }
        setCommand(DrawCommand(count))
    }

    internal fun build(playerIndex: Int): PlayerAction =
        PlayerAction(
            playerIndex = playerIndex,
            command = requireNotNull(command) { "Player action requires exactly one command." }
        )

    private fun setCommand(command: ActionCommand) {
        require(this.command == null) { "Player action can only contain one command." }
        this.command = command
    }
}

fun actionByPlayer(playerIndex: Int, init: @MtgDsl PlayerActionBuilder.() -> Unit): PlayerAction =
    PlayerActionBuilder().apply(init).build(playerIndex)
