package com.ayaziangames.training

import com.ayaziangames.game.bot.*
import com.ayaziangames.game.bot.machine.*
import com.ayaziangames.game.bot.strategy.*
import com.ayaziangames.game.model.*
import com.ayaziangames.game.rules.*


import java.time.Instant

enum class TrainingExampleSource {
    expertImitation
}

data class TrainingExample(
    val positionKey: String,
    val ruleConfigurationId: String,
    val featureSchemaVersion: Int,
    val boardSize: Int,
    val activeSide: Side,
    val candidateMove: LegalMove,
    val expertMove: LegalMove,
    val features: List<Float>,
    val label: Float,
    val source: TrainingExampleSource
)

data class MachineTrainedDataset(
    val ruleConfigurationId: String,
    val featureSchemaVersion: Int,
    val generatedAt: Instant,
    val expertBotId: String,
    val selfPlayBotIds: List<String>,
    val selfPlayGames: Int,
    val examples: List<TrainingExample>
)
