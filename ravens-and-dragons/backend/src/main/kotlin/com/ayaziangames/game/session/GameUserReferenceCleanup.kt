package com.ayaziangames.game.session

import com.ayaziangames.auth.UserReferenceCleanup
import com.ayaziangames.platform.game.runtime.GameSessionService
import org.springframework.stereotype.Component

@Component
class GameUserReferenceCleanup(
    private val gameSessionService: GameSessionService
) : UserReferenceCleanup {
    override fun clearUserReferences(userId: String) {
        gameSessionService.clearUserReferences(userId)
    }
}
