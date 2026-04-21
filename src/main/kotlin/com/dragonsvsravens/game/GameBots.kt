package com.dragonsvsravens.game

import org.springframework.stereotype.Component
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.abs

data class LegalMove(
    val origin: String,
    val destination: String
)

data class BotDefinition(
    val id: String,
    val displayName: String,
    val supportedRuleConfigurationIds: Set<String>,
    val strategy: GameBotStrategy
) {
    fun toSummary(): BotSummary = BotSummary(id = id, displayName = displayName)
}

interface GameBotStrategy {
    fun chooseMove(snapshot: GameSnapshot, legalMoves: List<LegalMove>): LegalMove
}

interface RandomIndexSource {
    fun nextInt(bound: Int): Int
}

@Component
class ThreadLocalRandomIndexSource : RandomIndexSource {
    override fun nextInt(bound: Int): Int = ThreadLocalRandom.current().nextInt(bound)
}

class RandomGameBotStrategy(
    private val randomIndexSource: RandomIndexSource
) : GameBotStrategy {
    override fun chooseMove(snapshot: GameSnapshot, legalMoves: List<LegalMove>): LegalMove {
        require(legalMoves.isNotEmpty()) { "Random bot requires at least one legal move." }
        return legalMoves[randomIndexSource.nextInt(legalMoves.size)]
    }
}

class SimpleGameBotStrategy : GameBotStrategy {
    override fun chooseMove(snapshot: GameSnapshot, legalMoves: List<LegalMove>): LegalMove {
        require(legalMoves.isNotEmpty()) { "Simple bot requires at least one legal move." }

        val winningMove = legalMoves.firstOrNull { move ->
            moverWins(snapshot, applyMove(snapshot, move))
        }
        if (winningMove != null) {
            return winningMove
        }

        var bestMove = legalMoves.first()
        var bestScore = scoreMove(snapshot, bestMove)

        legalMoves.drop(1).forEach { move ->
            val score = scoreMove(snapshot, move)
            if (score > bestScore) {
                bestMove = move
                bestScore = score
            }
        }

        return bestMove
    }

    private fun scoreMove(snapshot: GameSnapshot, move: LegalMove): Int {
        val nextSnapshot = applyMove(snapshot, move)
        val mover = snapshot.activeSide
        val opponent = oppositeSide(mover)

        var score = 0
        score += capturedOpponentCount(snapshot, nextSnapshot, mover) * 100
        score += moverSpecificScore(snapshot, nextSnapshot, mover)
        score += (mobilityScore(nextSnapshot, mover) - mobilityScore(snapshot, mover)) * 2
        if (opponentHasImmediateWin(nextSnapshot, opponent)) {
            score -= 150
        }
        return score
    }

    private fun moverSpecificScore(previousSnapshot: GameSnapshot, nextSnapshot: GameSnapshot, mover: Side): Int =
        when (mover) {
            Side.dragons -> (goldCornerDistance(previousSnapshot) - goldCornerDistance(nextSnapshot)) * 20
            Side.ravens -> (ravenPressure(nextSnapshot) - ravenPressure(previousSnapshot)) * 15
        }

    private fun mobilityScore(snapshot: GameSnapshot, side: Side): Int =
        GameRules.getLegalMoves(snapshotForSide(snapshot, side)).size

    private fun opponentHasImmediateWin(snapshot: GameSnapshot, opponent: Side): Boolean =
        GameRules.getLegalMoves(snapshotForSide(snapshot, opponent))
            .any { move -> moverWins(snapshotForSide(snapshot, opponent), applyMove(snapshotForSide(snapshot, opponent), move)) }

    private fun capturedOpponentCount(previousSnapshot: GameSnapshot, nextSnapshot: GameSnapshot, mover: Side): Int =
        countPiecesForSide(previousSnapshot, oppositeSide(mover)) - countPiecesForSide(nextSnapshot, oppositeSide(mover))

    private fun countPiecesForSide(snapshot: GameSnapshot, side: Side): Int =
        snapshot.board.values.count { GameRules.sideOwnsPiece(side, it) }

    private fun goldCornerDistance(snapshot: GameSnapshot): Int {
        val goldSquare = goldSquare(snapshot) ?: return 0
        return cornerSquares(snapshot.boardSize).minOf { corner -> manhattanDistance(goldSquare, corner) }
    }

    private fun ravenPressure(snapshot: GameSnapshot): Int {
        val goldSquare = goldSquare(snapshot) ?: return 100
        val ravens = snapshot.board.entries
            .filter { (_, piece) -> piece == Piece.raven }
            .map { (square) -> square }
        if (ravens.isEmpty()) {
            return 0
        }

        val nearestDistance = ravens.minOf { square -> manhattanDistance(square, goldSquare) }
        val adjacentRavens = ravens.count { square ->
            BoardCoordinates.isOrthogonallyAdjacent(square, goldSquare, snapshot.boardSize)
        }
        return (adjacentRavens * 4) - nearestDistance
    }

    private fun moverWins(previousSnapshot: GameSnapshot, nextSnapshot: GameSnapshot): Boolean {
        val terminalTurn = nextSnapshot.turns.lastOrNull() ?: return false
        if (terminalTurn.type != TurnType.gameOver) {
            return false
        }

        return when (previousSnapshot.activeSide) {
            Side.dragons -> terminalTurn.outcome == "Dragons win"
            Side.ravens -> terminalTurn.outcome == "Ravens win"
        }
    }

    private fun applyMove(snapshot: GameSnapshot, move: LegalMove): GameSnapshot =
        GameRules.movePiece(snapshot, move.origin, move.destination)

    private fun goldSquare(snapshot: GameSnapshot): String? =
        snapshot.board.entries.firstOrNull { (_, piece) -> piece == Piece.gold }?.key

    private fun snapshotForSide(snapshot: GameSnapshot, side: Side): GameSnapshot =
        snapshot.copy(phase = Phase.move, activeSide = side, pendingMove = null)

    private fun oppositeSide(side: Side): Side =
        when (side) {
            Side.dragons -> Side.ravens
            Side.ravens -> Side.dragons
        }

    private fun cornerSquares(boardSize: Int): List<String> = listOf(
        "a1",
        "a$boardSize",
        "${'a' + (boardSize - 1)}1",
        "${'a' + (boardSize - 1)}$boardSize"
    )

    private fun manhattanDistance(first: String, second: String): Int {
        val firstColumn = first[0] - 'a'
        val secondColumn = second[0] - 'a'
        val firstRow = first.drop(1).toInt() - 1
        val secondRow = second.drop(1).toInt() - 1
        return abs(firstColumn - secondColumn) + abs(firstRow - secondRow)
    }
}

@Component
class BotRegistry(
    randomIndexSource: RandomIndexSource
) {
    companion object {
        const val randomBotId = "random"
        const val simpleBotId = "simple"
        val releaseTwoSupportedRuleConfigurationIds = setOf(
            "original-game",
            "sherwood-rules",
            "square-one",
            "sherwood-x-9",
            "square-one-x-9"
        )
    }

    private val definitions = linkedMapOf(
        randomBotId to BotDefinition(
            id = randomBotId,
            displayName = "Random",
            supportedRuleConfigurationIds = releaseTwoSupportedRuleConfigurationIds,
            strategy = RandomGameBotStrategy(randomIndexSource)
        ),
        simpleBotId to BotDefinition(
            id = simpleBotId,
            displayName = "Simple",
            supportedRuleConfigurationIds = releaseTwoSupportedRuleConfigurationIds,
            strategy = SimpleGameBotStrategy()
        )
    )

    fun availableBotsFor(ruleConfigurationId: String): List<BotSummary> =
        definitions.values
            .filter { ruleConfigurationId in it.supportedRuleConfigurationIds }
            .map(BotDefinition::toSummary)

    fun summaryFor(botId: String?): BotSummary? =
        botId?.let { requireDefinition(it).toSummary() }

    fun requireSupportedDefinition(botId: String, ruleConfigurationId: String): BotDefinition {
        val definition = requireDefinition(botId)
        if (ruleConfigurationId !in definition.supportedRuleConfigurationIds) {
            throw InvalidCommandException("${definition.displayName} is not available for this rule configuration.")
        }
        return definition
    }

    private fun requireDefinition(botId: String): BotDefinition =
        definitions[botId] ?: throw InvalidCommandException("Unknown bot: $botId")
}
