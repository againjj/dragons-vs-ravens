package com.ayaziangames.mtg.model

class Game(
    val availableCards: CardDatabase,
    val gameState: GameState,
    val log: List<PlayerAction> = emptyList()
) {
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
