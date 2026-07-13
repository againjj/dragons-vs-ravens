package com.ayaziangames.auth

fun interface UserReferenceCleanup {
    fun clearUserReferences(userId: String)
}
