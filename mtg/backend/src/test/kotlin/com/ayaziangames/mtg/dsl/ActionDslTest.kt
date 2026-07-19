package com.ayaziangames.mtg.dsl

import com.ayaziangames.mtg.model.DrawCommand
import com.ayaziangames.mtg.model.PlayerAction
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
                draw(1)
            }
        }
        assertEquals("Player action can only contain one command.", extraCommand.message)
    }
}
