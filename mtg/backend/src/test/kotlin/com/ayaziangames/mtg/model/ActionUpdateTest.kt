package com.ayaziangames.mtg.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class ActionUpdateTest {
    @Test
    fun updateReturnsSameDrawCommandWhenCountIsUnchanged() {
        val command = DrawCommand(1)

        assertSame(command, command.update())
    }

    @Test
    fun updateReturnsNewDrawCommandWhenCountChanges() {
        val command = DrawCommand(1)

        val updated = command.update(count = 2)

        assertNotSame(command, updated)
        assertEquals(2, updated.count)
    }

    @Test
    fun updateReturnsSameUntapCommandWhenIndexesAreUnchanged() {
        val indexes = listOf(0, 2)
        val command = UntapCommand(indexes)

        assertSame(command, command.update())
    }

    @Test
    fun updateReturnsNewUntapCommandWhenIndexesChange() {
        val command = UntapCommand(listOf(0))
        val updatedIndexes = listOf(1)

        val updated = command.update(permanentIndexes = updatedIndexes)

        assertNotSame(command, updated)
        assertSame(updatedIndexes, updated.permanentIndexes)
    }

    @Test
    fun updateReturnsSameDiscardCardsCommandWhenIndexesAreUnchanged() {
        val indexes = listOf(0, 2)
        val command = DiscardCardsCommand(indexes)

        assertSame(command, command.update())
    }

    @Test
    fun updateReturnsNewDiscardCardsCommandWhenIndexesChange() {
        val command = DiscardCardsCommand(listOf(0))
        val updatedIndexes = listOf(1)

        val updated = command.update(cardIndexes = updatedIndexes)

        assertNotSame(command, updated)
        assertSame(updatedIndexes, updated.cardIndexes)
    }

    @Test
    fun updateReturnsSamePlayerActionWhenFieldsAreUnchanged() {
        val command = DrawCommand(1)
        val action = PlayerAction(0, command)

        assertSame(action, action.update())
    }

    @Test
    fun updateReturnsNewPlayerActionWhenFieldsChange() {
        val action = PlayerAction(0, DrawCommand(1))
        val updatedCommand = DrawCommand(2)

        val updated = action.update(playerIndex = 1, command = updatedCommand)

        assertNotSame(action, updated)
        assertEquals(1, updated.playerIndex)
        assertSame(updatedCommand, updated.command)
    }
}
