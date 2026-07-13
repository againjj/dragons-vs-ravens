package com.ayaziangames.platform.game.runtime

fun interface PlayerAccountValidator {
    fun requirePlayerAccountsExist(userIds: Set<String>)
}

