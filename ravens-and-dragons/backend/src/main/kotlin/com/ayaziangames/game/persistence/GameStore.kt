package com.ayaziangames.game.persistence

import com.ayaziangames.game.model.*
import java.time.Instant

data class StoredGame(
    val session: GameSession,
    val undoEntries: List<UndoEntry>,
    val lastAccessedAt: Instant
)
