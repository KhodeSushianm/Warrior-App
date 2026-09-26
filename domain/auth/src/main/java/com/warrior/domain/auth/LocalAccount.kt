package com.warrior.domain.auth

/** Local account identity (no credentials — those never leave the security layer). */
data class LocalAccount(
    val id: Long,
    val username: String,
    val displayName: String,
)
