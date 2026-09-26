package com.warrior.domain.auth.validation

/** Thrown when local-account input violates domain rules (Phase 4 uses these at registration). */
class AuthValidationException(val messages: List<String>) :
    IllegalArgumentException(messages.joinToString(separator = "; "))

/**
 * Local account validation rules (Architecture v2.1 §14, DB v4 §1).
 * Username is normalized to lowercase before storage in Phase 4.
 */
object AuthValidation {

    private val USERNAME_REGEX = Regex("^[a-z0-9_]{3,30}$")
    private const val DISPLAY_NAME_MAX = 50
    private const val PASSWORD_MIN = 8

    fun validateRegister(username: String, displayName: String, password: String): List<String> {
        val errors = mutableListOf<String>()
        if (!USERNAME_REGEX.matches(username)) {
            errors += "username must be 3-30 chars of a-z, 0-9 or underscore"
        }
        if (displayName.isBlank() || displayName.length > DISPLAY_NAME_MAX) {
            errors += "displayName must be 1-$DISPLAY_NAME_MAX non-blank chars"
        }
        if (password.length < PASSWORD_MIN) {
            errors += "password must be at least $PASSWORD_MIN chars"
        }
        return errors
    }

    fun validateLogin(username: String, password: String): List<String> {
        val errors = mutableListOf<String>()
        if (username.isBlank()) errors += "username must not be blank"
        if (password.isEmpty()) errors += "password must not be empty"
        return errors
    }

    fun requireRegister(username: String, displayName: String, password: String) {
        val errors = validateRegister(username, displayName, password)
        if (errors.isNotEmpty()) throw AuthValidationException(errors)
    }

    fun requireLogin(username: String, password: String) {
        val errors = validateLogin(username, password)
        if (errors.isNotEmpty()) throw AuthValidationException(errors)
    }
}
