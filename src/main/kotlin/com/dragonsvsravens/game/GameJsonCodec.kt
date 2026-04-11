package com.dragonsvsravens.game

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component

@Component
class GameJsonCodec(
    private val objectMapper: ObjectMapper
) {
    fun writeSnapshot(snapshot: GameSnapshot): String =
        objectMapper.writeValueAsString(snapshot)

    fun readSnapshot(json: String): GameSnapshot =
        objectMapper.readValue(json, GameSnapshot::class.java)

    fun writeUndoSnapshots(undoSnapshots: List<GameSnapshot>): String =
        objectMapper.writeValueAsString(undoSnapshots)

    fun readUndoSnapshots(json: String): List<GameSnapshot> =
        objectMapper.readValue(json, object : TypeReference<List<GameSnapshot>>() {})
}
