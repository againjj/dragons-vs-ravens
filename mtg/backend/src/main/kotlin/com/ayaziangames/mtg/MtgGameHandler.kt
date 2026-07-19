package com.ayaziangames.mtg

import com.ayaziangames.platform.game.runtime.GameHandler
import com.ayaziangames.platform.game.runtime.GameRecord
import com.ayaziangames.platform.game.runtime.InvalidCommandException
import com.ayaziangames.platform.game.runtime.PublicGameDetails
import com.ayaziangames.platform.game.runtime.VersionConflictException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant

data class MtgGameState(
    val id: String,
    val gameSlug: String,
    val version: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lifecycle: String,
    val createdByUserId: String? = null
)

@Component
@Conditional(MtgModuleCondition::class)
class MtgGameHandler(
    private val objectMapper: ObjectMapper,
    private val clock: Clock
) : GameHandler {
    override val gameSlug: String = MtgGameModuleDefinition.identity.slug

    override fun createGame(
        gameId: String,
        request: JsonNode,
        createdByUserId: String?
    ): GameRecord {
        val now = Instant.now(clock)
        val state = MtgGameState(
            id = gameId,
            gameSlug = gameSlug,
            version = 1,
            createdAt = now,
            updatedAt = now,
            lifecycle = activeLifecycle,
            createdByUserId = createdByUserId
        )
        return state.toRecord(lastAccessedAt = now)
    }

    override fun applyCommand(current: GameRecord, command: JsonNode, actingUserId: String?): GameRecord {
        val state = current.toMtgState()
        requireExpectedVersion(state, command)

        val commandType = command.get("type")?.asText()
        if (commandType != endGameCommandType) {
            throw InvalidCommandException("Unsupported Magic: the Gathering command: ${commandType ?: "missing type"}.")
        }
        if (state.lifecycle == finishedLifecycle) {
            throw InvalidCommandException("This Magic: the Gathering game is already over.")
        }

        val now = Instant.now(clock)
        return state.copy(
            version = state.version + 1,
            updatedAt = now,
            lifecycle = finishedLifecycle
        ).toRecord(lastAccessedAt = current.lastAccessedAt, publiclyListed = current.publiclyListed)
    }

    override fun gameView(current: GameRecord, currentUserId: String?): JsonNode = current.publicState

    override fun publicGameDetails(current: GameRecord): PublicGameDetails = PublicGameDetails(
        gameName = MtgGameModuleDefinition.identity.displayName,
        openSeats = 0
    )

    override fun playerUserIds(current: GameRecord): Set<String> = emptySet()

    private fun requireExpectedVersion(state: MtgGameState, command: JsonNode) {
        val expectedVersion = command.get("expectedVersion")?.asLong()
            ?: throw InvalidCommandException("End game command requires expectedVersion.")
        if (expectedVersion != state.version) {
            throw VersionConflictException(objectMapper.valueToTree(state))
        }
    }

    private fun GameRecord.toMtgState(): MtgGameState =
        objectMapper.treeToValue(publicState, MtgGameState::class.java)

    private fun MtgGameState.toRecord(lastAccessedAt: Instant, publiclyListed: Boolean = true): GameRecord =
        GameRecord(
            id = id,
            gameSlug = gameSlug,
            version = version,
            createdAt = createdAt,
            updatedAt = updatedAt,
            lifecycle = lifecycle,
            publicState = objectMapper.valueToTree(this),
            privateState = objectMapper.createObjectNode(),
            createdByUserId = createdByUserId,
            lastAccessedAt = lastAccessedAt,
            publiclyListed = publiclyListed
        )

    private companion object {
        const val activeLifecycle = "active"
        const val finishedLifecycle = "finished"
        const val endGameCommandType = "endGame"
    }
}
