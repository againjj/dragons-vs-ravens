package com.dragonsvsravens.game

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GameBotsTest {

    @Test
    fun `simple bot chooses an immediate win before heuristic scoring`() {
        val strategy = SimpleGameBotStrategy()
        val snapshot = GameSnapshot(
            board = linkedMapOf(
                "a2" to Piece.gold,
                "d5" to Piece.dragon,
                "g7" to Piece.raven
            ),
            boardSize = 7,
            specialSquare = "d4",
            phase = Phase.move,
            activeSide = Side.dragons,
            pendingMove = null,
            turns = emptyList(),
            ruleConfigurationId = "sherwood-rules",
            positionKeys = listOf("initial")
        )

        val move = strategy.chooseMove(snapshot, GameRules.getLegalMoves(snapshot))

        assertEquals(LegalMove("a2", "a1"), move)
    }

    @Test
    fun `simple bot prefers moving the gold closer to a corner in a quiet position`() {
        val strategy = SimpleGameBotStrategy()
        val snapshot = GameSnapshot(
            board = linkedMapOf(
                "d4" to Piece.gold,
                "g7" to Piece.raven
            ),
            boardSize = 7,
            specialSquare = "d4",
            phase = Phase.move,
            activeSide = Side.dragons,
            pendingMove = null,
            turns = emptyList(),
            ruleConfigurationId = "original-game",
            positionKeys = listOf("initial")
        )

        val move = strategy.chooseMove(snapshot, GameRules.getLegalMoves(snapshot))

        assertEquals(LegalMove("d4", "a4"), move)
    }

    @Test
    fun `simple bot stays deterministic under stable legal move ordering`() {
        val strategy = SimpleGameBotStrategy()
        val snapshot = GameSnapshot(
            board = linkedMapOf(
                "d4" to Piece.gold,
                "g7" to Piece.raven
            ),
            boardSize = 7,
            specialSquare = "d4",
            phase = Phase.move,
            activeSide = Side.dragons,
            pendingMove = null,
            turns = emptyList(),
            ruleConfigurationId = "original-game",
            positionKeys = listOf("initial")
        )
        val legalMoves = GameRules.getLegalMoves(snapshot)

        val firstChoice = strategy.chooseMove(snapshot, legalMoves)
        val secondChoice = strategy.chooseMove(snapshot, legalMoves)

        assertEquals(firstChoice, secondChoice)
    }

    @Test
    fun `minimax bot chooses the immediate winning move when available`() {
        val strategy = MinimaxGameBotStrategy()
        val snapshot = GameSnapshot(
            board = linkedMapOf(
                "a2" to Piece.gold,
                "d5" to Piece.dragon,
                "g7" to Piece.raven
            ),
            boardSize = 7,
            specialSquare = "d4",
            phase = Phase.move,
            activeSide = Side.dragons,
            pendingMove = null,
            turns = emptyList(),
            ruleConfigurationId = "sherwood-rules",
            positionKeys = listOf("initial")
        )

        val move = strategy.chooseMove(snapshot, GameRules.getLegalMoves(snapshot))

        assertEquals(LegalMove("a2", "a1"), move)
    }

    @Test
    fun `minimax bot stays deterministic under stable legal move ordering`() {
        val strategy = MinimaxGameBotStrategy()
        val snapshot = GameSnapshot(
            board = linkedMapOf(
                "d4" to Piece.gold,
                "g7" to Piece.raven
            ),
            boardSize = 7,
            specialSquare = "d4",
            phase = Phase.move,
            activeSide = Side.dragons,
            pendingMove = null,
            turns = emptyList(),
            ruleConfigurationId = "original-game",
            positionKeys = listOf("initial")
        )
        val legalMoves = GameRules.getLegalMoves(snapshot)

        val firstChoice = strategy.chooseMove(snapshot, legalMoves)
        val secondChoice = strategy.chooseMove(snapshot, legalMoves)

        assertEquals(firstChoice, secondChoice)
    }

    @Test
    fun `minimax bot search uses hypothetical snapshots without mutating the input snapshot`() {
        val strategy = MinimaxGameBotStrategy()
        val snapshot = GameSnapshot(
            board = linkedMapOf(
                "d4" to Piece.gold,
                "g7" to Piece.raven
            ),
            boardSize = 7,
            specialSquare = "d4",
            phase = Phase.move,
            activeSide = Side.dragons,
            pendingMove = null,
            turns = emptyList(),
            ruleConfigurationId = "original-game",
            positionKeys = listOf("initial")
        )
        val originalSnapshot = snapshot.copy(
            board = LinkedHashMap(snapshot.board),
            turns = snapshot.turns.toList(),
            positionKeys = snapshot.positionKeys.toList()
        )

        strategy.chooseMove(snapshot, GameRules.getLegalMoves(snapshot))

        assertEquals(originalSnapshot, snapshot)
    }

    @Test
    fun `random bot still selects only legal actions`() {
        val strategy = RandomGameBotStrategy(object : RandomIndexSource {
            override fun nextInt(bound: Int): Int = bound - 1
        })
        val snapshot = GameRules.startGame("sherwood-rules")
        val legalMoves = GameRules.getLegalMoves(snapshot)

        val move = strategy.chooseMove(snapshot, legalMoves)

        assertTrue(move in legalMoves)
    }

    @Test
    fun `bot registry exposes both release four bots on supported rulesets and none on free play`() {
        val registry = BotRegistry(object : RandomIndexSource {
            override fun nextInt(bound: Int): Int = 0
        })

        assertEquals(
            listOf(BotRegistry.randomBotId, BotRegistry.simpleBotId, BotRegistry.minimaxBotId),
            registry.availableBotsFor("original-game").map(BotSummary::id)
        )
        assertTrue(registry.availableBotsFor(GameRules.freePlayRuleConfigurationId).isEmpty())
    }
}
