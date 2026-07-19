package com.ayaziangames.mtg

import com.ayaziangames.platform.game.runtime.GameRecord
import com.ayaziangames.platform.game.runtime.InvalidCommandException
import com.ayaziangames.platform.game.runtime.VersionConflictException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals

class MtgGameHandlerTest {
    private val objectMapper = jacksonObjectMapper().findAndRegisterModules()
    private val handler = MtgGameHandler(
        objectMapper = objectMapper,
        clock = Clock.fixed(Instant.parse("2026-07-18T00:00:00Z"), ZoneOffset.UTC)
    )

    @Test
    fun createsActiveGame() {
        val game = handler.createGame("MTG1234", objectMapper.createObjectNode(), "user-1")
        val state = game.readState()

        assertEquals("MTG1234", state.id)
        assertEquals("mtg", state.gameSlug)
        assertEquals(1, state.version)
        assertEquals("active", state.lifecycle)
        assertEquals("user-1", state.createdByUserId)
    }

    @Test
    fun endGameFinishesTheGame() {
        val game = handler.createGame("MTG1234", objectMapper.createObjectNode(), null)
        val updated = handler.applyCommand(game, endGameCommand(expectedVersion = 1), "user-1")
        val state = updated.readState()

        assertEquals(2, state.version)
        assertEquals("finished", state.lifecycle)
    }

    @Test
    fun rejectsEndGameAfterGameIsOver() {
        val game = handler.createGame("MTG1234", objectMapper.createObjectNode(), null)
        val finished = handler.applyCommand(game, endGameCommand(expectedVersion = 1), "user-1")

        val exception = assertThrows<InvalidCommandException> {
            handler.applyCommand(finished, endGameCommand(expectedVersion = 2), "user-1")
        }

        assertEquals("This Magic: the Gathering game is already over.", exception.message)
    }

    @Test
    fun rejectsUnsupportedCommands() {
        val game = handler.createGame("MTG1234", objectMapper.createObjectNode(), null)

        val exception = assertThrows<InvalidCommandException> {
            handler.applyCommand(
                game,
                objectMapper.createObjectNode()
                    .put("type", "drawCard")
                    .put("expectedVersion", 1),
                "user-1"
            )
        }

        assertEquals("Unsupported Magic: the Gathering command: drawCard.", exception.message)
    }

    @Test
    fun rejectsStaleCommands() {
        val game = handler.createGame("MTG1234", objectMapper.createObjectNode(), null)

        val exception = assertThrows<VersionConflictException> {
            handler.applyCommand(game, endGameCommand(expectedVersion = 0), "user-1")
        }

        assertEquals(1, objectMapper.treeToValue(exception.latestState, MtgGameState::class.java).version)
    }

    private fun endGameCommand(expectedVersion: Long) =
        objectMapper.createObjectNode()
            .put("type", "endGame")
            .put("expectedVersion", expectedVersion)

    private fun GameRecord.readState(): MtgGameState =
        objectMapper.treeToValue(publicState, MtgGameState::class.java)
}
