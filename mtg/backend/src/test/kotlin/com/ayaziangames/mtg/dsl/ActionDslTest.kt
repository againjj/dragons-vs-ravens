package com.ayaziangames.mtg.dsl

import com.ayaziangames.mtg.model.DrawCommand
import com.ayaziangames.mtg.model.DiscardCardsCommand
import com.ayaziangames.mtg.model.PassPriorityCommand
import com.ayaziangames.mtg.model.PlayerAction
import com.ayaziangames.mtg.model.UntapCommand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ActionDslTest {
    @Test
    fun buildsPlayerActions() {
        val action = actionByPlayer(1) {
            draw(2)
        }

        assertEquals(PlayerAction(playerIndex = 1, command = DrawCommand(2)), action)

        assertEquals(
            PlayerAction(playerIndex = 0, command = UntapCommand(listOf(0, 2))),
            actionByPlayer(0) { untap(listOf(0, 2)) }
        )
        assertEquals(
            PlayerAction(playerIndex = 1, command = PassPriorityCommand),
            actionByPlayer(1) { passPriority() }
        )
        assertEquals(
            PlayerAction(playerIndex = 0, command = DiscardCardsCommand(listOf(1, 3))),
            actionByPlayer(0) { discardCards(listOf(1, 3)) }
        )
    }

    @Test
    fun rejectsPlayerActionsWithoutExactlyOneCommand() {
        val missingCommand = assertFailsWith<IllegalArgumentException> {
            actionByPlayer(0) {
            }
        }
        assertEquals("Player action requires exactly one command.", missingCommand.message)

        val extraCommand = assertFailsWith<IllegalArgumentException> {
            actionByPlayer(0) {
                draw(1)
                passPriority()
            }
        }
        assertEquals("Player action can only contain one command.", extraCommand.message)
    }
}
