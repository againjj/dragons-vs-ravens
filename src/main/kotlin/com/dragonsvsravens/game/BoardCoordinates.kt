package com.dragonsvsravens.game

object BoardCoordinates {
    private val allFiles = ('a'..'z').toList()

    fun isValidBoardSize(boardSize: Int): Boolean =
        boardSize in 3..26

    fun isValidSquare(square: String, boardSize: Int): Boolean {
        val file = square.firstOrNull() ?: return false
        val rank = square.drop(1).toIntOrNull() ?: return false
        return isValidBoardSize(boardSize) &&
            file in files(boardSize) &&
            rank in 1..boardSize
    }

    fun allSquares(boardSize: Int): List<String> =
        ranks(boardSize).flatMap { rank ->
            files(boardSize).map { file -> "$file$rank" }
        }

    fun centerSquare(boardSize: Int): String =
        "${files(boardSize)[boardSize / 2]}${(boardSize / 2) + 1}"

    fun cornerSquares(boardSize: Int): Set<String> {
        val files = files(boardSize)
        return setOf(
            "${files.first()}1",
            "${files.first()}$boardSize",
            "${files.last()}1",
            "${files.last()}$boardSize"
        )
    }

    fun isCenter(square: String, specialSquare: String): Boolean =
        square == specialSquare

    fun isCorner(square: String, boardSize: Int): Boolean =
        square in cornerSquares(boardSize)

    fun neighbors(square: String, boardSize: Int): List<String> {
        val (fileIndex, rankIndex) = indexes(square, boardSize) ?: return emptyList()
        return listOfNotNull(
            squareAt(fileIndex, rankIndex + 1, boardSize),
            squareAt(fileIndex + 1, rankIndex, boardSize),
            squareAt(fileIndex, rankIndex - 1, boardSize),
            squareAt(fileIndex - 1, rankIndex, boardSize)
        )
    }

    fun oppositePairs(square: String, boardSize: Int): List<Pair<String, String>> {
        val (fileIndex, rankIndex) = indexes(square, boardSize) ?: return emptyList()
        return listOfNotNull(
            pairAt(fileIndex, rankIndex + 1, fileIndex, rankIndex - 1, boardSize),
            pairAt(fileIndex - 1, rankIndex, fileIndex + 1, rankIndex, boardSize)
        )
    }

    fun isOrthogonallyAdjacent(first: String, second: String, boardSize: Int): Boolean {
        val firstIndexes = indexes(first, boardSize) ?: return false
        val secondIndexes = indexes(second, boardSize) ?: return false
        val fileDistance = kotlin.math.abs(firstIndexes.first - secondIndexes.first)
        val rankDistance = kotlin.math.abs(firstIndexes.second - secondIndexes.second)
        return fileDistance + rankDistance == 1
    }

    fun pathBetween(origin: String, destination: String, boardSize: Int): List<String> {
        val (originFile, originRank) = indexes(origin, boardSize) ?: return emptyList()
        val (destinationFile, destinationRank) = indexes(destination, boardSize) ?: return emptyList()
        if (originFile != destinationFile && originRank != destinationRank) {
            return emptyList()
        }

        val fileStep = destinationFile.compareTo(originFile)
        val rankStep = destinationRank.compareTo(originRank)
        val path = mutableListOf<String>()
        var nextFile = originFile + fileStep
        var nextRank = originRank + rankStep

        while (nextFile != destinationFile || nextRank != destinationRank) {
            path += squareAt(nextFile, nextRank, boardSize) ?: return emptyList()
            nextFile += fileStep
            nextRank += rankStep
        }

        return path
    }

    private fun files(boardSize: Int): List<Char> =
        allFiles.take(boardSize)

    private fun ranks(boardSize: Int): List<Int> =
        (1..boardSize).toList()

    private fun indexes(square: String, boardSize: Int): Pair<Int, Int>? {
        if (!isValidSquare(square, boardSize)) {
            return null
        }

        return files(boardSize).indexOf(square[0]) to (square.drop(1).toInt() - 1)
    }

    private fun squareAt(fileIndex: Int, rankIndex: Int, boardSize: Int): String? {
        val files = files(boardSize)
        if (fileIndex !in files.indices || rankIndex !in 0 until boardSize) {
            return null
        }

        return "${files[fileIndex]}${rankIndex + 1}"
    }

    private fun pairAt(
        firstFileIndex: Int,
        firstRankIndex: Int,
        secondFileIndex: Int,
        secondRankIndex: Int,
        boardSize: Int
    ): Pair<String, String>? {
        val first = squareAt(firstFileIndex, firstRankIndex, boardSize) ?: return null
        val second = squareAt(secondFileIndex, secondRankIndex, boardSize) ?: return null
        return first to second
    }
}
