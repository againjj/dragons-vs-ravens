package com.ayaziangames.training

import com.ayaziangames.game.bot.*
import com.ayaziangames.game.bot.machine.*
import com.ayaziangames.game.bot.strategy.*
import com.ayaziangames.game.model.*
import com.ayaziangames.game.rules.*


import com.fasterxml.jackson.databind.ObjectMapper
import java.nio.file.Files
import java.nio.file.Path

class TrainingExampleCodec(
    private val objectMapper: ObjectMapper
) {
    fun write(path: Path, dataset: MachineTrainedDataset) {
        path.parent?.let(Files::createDirectories)
        Files.newBufferedWriter(path).use { writer ->
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(writer, dataset)
        }
    }

    fun read(path: Path): MachineTrainedDataset =
        Files.newBufferedReader(path).use { reader ->
            objectMapper.readValue(reader, MachineTrainedDataset::class.java)
        }
}
