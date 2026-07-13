package com.ayaziangames.auth

import com.ayaziangames.platform.game.runtime.PlayerAccountMissingException
import com.ayaziangames.platform.game.runtime.PlayerAccountValidator
import org.springframework.stereotype.Component

@Component
class UserPlayerAccountValidator(
    private val userRepository: UserRepository
) : PlayerAccountValidator {
    override fun requirePlayerAccountsExist(userIds: Set<String>) {
        if (userIds.isEmpty()) {
            return
        }
        val existingUserIds = userRepository.lockExistingIds(userIds)
        if (existingUserIds.size != userIds.size) {
            throw PlayerAccountMissingException()
        }
    }
}
