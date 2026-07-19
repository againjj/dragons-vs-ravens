package com.ayaziangames.mtg.model

class Game(
    val availableCards: CardDatabase,
    val gameState: GameState,
    val log: List<PlayerAction> = emptyList()
) {
    fun serialize(): String =
        buildString {
            appendLine("game {")
            appendLine("    availableCards = ${availableCards.name}")
            append(gameState.serialize().indentBy(4))
            if (log.isNotEmpty()) {
                appendLine()
                appendLine("    log = listOf(")
                append(log.joinToString(",\n") { it.serialize().indentBy(8) })
                appendLine()
                appendLine("    )")
            } else {
                appendLine()
                appendLine("    log = emptyList()")
            }
            append("}")
        }

    fun update(
        availableCards: CardDatabase = this.availableCards,
        gameState: GameState = this.gameState,
        log: List<PlayerAction> = this.log
    ): Game =
        if (
            availableCards === this.availableCards &&
            gameState === this.gameState &&
            log === this.log
        ) {
            this
        } else {
            Game(
                availableCards = availableCards,
                gameState = gameState,
                log = log
            )
        }
}
