package com.ayaziangames.training

import com.ayaziangames.game.bot.*
import com.ayaziangames.game.bot.machine.*
import com.ayaziangames.game.bot.strategy.*
import com.ayaziangames.game.model.*
import com.ayaziangames.game.rules.*


import com.fasterxml.jackson.databind.ObjectMapper
import java.nio.file.Files
import java.nio.file.Path

class MachineTrainedArtifactReader(
    private val objectMapper: ObjectMapper
) {
    fun read(path: Path): MachineTrainedModel =
        Files.newBufferedReader(path).use { reader ->
            val payload = objectMapper.readValue(reader, MachineTrainedArtifactPayload::class.java)
            MachineTrainedArtifactSupport.toModel(payload)
        }
}
