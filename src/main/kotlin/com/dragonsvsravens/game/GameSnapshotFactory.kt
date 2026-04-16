package com.dragonsvsravens.game

internal object GameSnapshotFactory {
    private val setupCycle = listOf(Piece.dragon, Piece.raven, Piece.gold)

    fun createInitialSnapshot(
        ruleConfigurationId: String = GameRules.freePlayRuleConfigurationId,
        selectedStartingSide: Side = Side.dragons,
        selectedBoardSize: Int = GameRules.defaultBoardSize
    ): GameSnapshot = createBaseSnapshot(ruleConfigurationId, Phase.none, selectedStartingSide, selectedBoardSize)

    fun createIdleSnapshot(
        ruleConfigurationId: String,
        selectedStartingSide: Side = Side.dragons,
        selectedBoardSize: Int = GameRules.defaultBoardSize
    ): GameSnapshot =
        createBaseSnapshot(ruleConfigurationId, Phase.none, selectedStartingSide, selectedBoardSize)

    fun startGame(
        ruleConfigurationId: String = GameRules.freePlayRuleConfigurationId,
        selectedStartingSide: Side = Side.dragons,
        selectedBoardSize: Int = GameRules.defaultBoardSize
    ): GameSnapshot {
        val configuration = RuleCatalog.getRuleConfiguration(ruleConfigurationId)
        val initialSnapshot = createBaseSnapshot(
            configuration.summary.id,
            configuration.ruleSet.startPhase(),
            selectedStartingSide,
            selectedBoardSize
        )
        return initializePositionHistory(initialSnapshot)
    }

    fun cycleSetupPiece(snapshot: GameSnapshot, square: String): GameSnapshot {
        val board = LinkedHashMap(snapshot.board)
        val nextPiece = nextSetupPiece(board[square])

        if (nextPiece == null) {
            board.remove(square)
        } else {
            board[square] = nextPiece
        }

        return snapshot.copy(board = board)
    }

    fun endSetup(snapshot: GameSnapshot, selectedStartingSide: Side = Side.dragons): GameSnapshot =
        initializePositionHistory(
            snapshot.copy(
                phase = Phase.move,
                activeSide = resolveStartingSide(snapshot.ruleConfigurationId, selectedStartingSide),
                pendingMove = null
            )
        )

    fun initializePositionHistory(snapshot: GameSnapshot): GameSnapshot {
        val configuration = RuleCatalog.getRuleConfiguration(snapshot.ruleConfigurationId)
        val positionKey = configuration.ruleSet.positionKey(snapshot)
        return if (positionKey == null) {
            snapshot.copy(positionKeys = emptyList())
        } else {
            snapshot.copy(positionKeys = listOf(positionKey))
        }
    }

    fun resolveStartingSide(ruleConfigurationId: String, selectedStartingSide: Side): Side {
        val configuration = RuleCatalog.getRuleConfiguration(ruleConfigurationId)
        return if (ruleConfigurationId == GameRules.freePlayRuleConfigurationId) {
            selectedStartingSide
        } else {
            configuration.startingSide
        }
    }

    private fun createBaseSnapshot(
        ruleConfigurationId: String,
        phase: Phase,
        selectedStartingSide: Side,
        selectedBoardSize: Int
    ): GameSnapshot {
        val configuration = RuleCatalog.getRuleConfiguration(ruleConfigurationId)
        GameRules.validateBoardSize(selectedBoardSize)
        val boardSize = if (ruleConfigurationId == GameRules.freePlayRuleConfigurationId) selectedBoardSize else configuration.boardSize
        val specialSquare = if (ruleConfigurationId == GameRules.freePlayRuleConfigurationId) {
            BoardCoordinates.centerSquare(boardSize)
        } else {
            configuration.specialSquare
        }
        return GameSnapshot(
            board = LinkedHashMap(configuration.presetBoard),
            boardSize = boardSize,
            specialSquare = specialSquare,
            phase = phase,
            activeSide = resolveStartingSide(ruleConfigurationId, selectedStartingSide),
            pendingMove = null,
            turns = emptyList(),
            ruleConfigurationId = configuration.summary.id
        )
    }

    private fun nextSetupPiece(piece: Piece?): Piece? {
        if (piece == null) {
            return setupCycle.first()
        }

        val currentIndex = setupCycle.indexOf(piece)
        return if (currentIndex == setupCycle.lastIndex) null else setupCycle[currentIndex + 1]
    }
}
