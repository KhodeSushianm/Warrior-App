package com.warrior.domain.auth.validation

/**
 * Stable machine-readable identifiers for every auth error the UI can show
 * (Phase 9, i18n-ready): feature layers map codes to string resources, so no
 * domain message text is ever rendered directly. [messages] stay on the
 * exception for logs/debugging only.
 */
enum class AuthErrorCode {
    USERNAME_FORMAT,
    DISPLAY_NAME_INVALID,
    PASSWORD_TOO_SHORT,
    USERNAME_BLANK,
    PASSWORD_EMPTY,
    DUPLICATE_USERNAME,
    INVALID_CREDENTIALS,
    UNEXPECTED,
}

/** Thrown when local-account input violates domain rules (Phase 4 uses these at registration). */
class AuthValidationException(
    val messages: List<String>,
    val codes: List<AuthErrorCode> = emptyList(),
) : IllegalArgumentException(messages.joinToString(separator = "; "))

/**
 * Local account validation rules (Architecture v2.1 §14, DB v4 §1).
 * Username is normalized to lowercase before storage in Phase 4.
 */
object AuthValidation {

    private val USERNAME_REGEX = Regex("^[a-z0-9_]{3,30}$")
    private const val DISPLAY_NAME_MAX = 50
    private const val PASSWORD_MIN = 8

    fun validateRegister(username: String, displayName: String, password: String): List<Pair<String, AuthErrorCode>> {
        val errors = mutableListOf<Pair<String, AuthErrorCode>>()
        errors += usernameErrors(username)
        errors += displayNameErrors(displayName)
        if (password.length < PASSWORD_MIN) {
            errors += "password must be at least $PASSWORD_MIN chars" to AuthErrorCode.PASSWORD_TOO_SHORT
        }
        return errors
    }

    fun validateLogin(username: String, password: String): List<Pair<String, AuthErrorCode>> {
        val errors = mutableListOf<Pair<String, AuthErrorCode>>()
        if (username.isBlank()) errors += "username must not be blank" to AuthErrorCode.USERNAME_BLANK
        if (password.isEmpty()) errors += "password must not be empty" to AuthErrorCode.PASSWORD_EMPTY
        return errors
    }

    /** Profile edit (Phase 9): identity fields only — password never changes here. */
    fun validateProfile(username: String, displayName: String): List<Pair<String, AuthErrorCode>> =
        usernameErrors(username) + displayNameErrors(displayName)

    fun requireRegister(username: String, displayName: String, password: String) =
        requireValid(validateRegister(username, displayName, password))

    fun requireLogin(username: String, password: String) =
        requireValid(validateLogin(username, password))

    fun requireProfile(username: String, displayName: String) =
        requireValid(validateProfile(username, displayName))

    private fun requireValid(errors: List<Pair<String, AuthErrorCode>>) {
        if (errors.isNotEmpty()) {
            throw AuthValidationException(
                messages = errors.map { it.first },
                codes = errors.map { it.second },
            )
        }
    }

    private fun usernameErrors(username: String): List<Pair<String, AuthErrorCode>> =
        if (!USERNAME_REGEX.matches(username)) {
            listOf("username must be 3-30 chars of a-z, 0-9 or underscore" to AuthErrorCode.USERNAME_FORMAT)
        } else {
            emptyList()
        }

    private fun displayNameErrors(displayName: String): List<Pair<String, AuthErrorCode>> =
        if (displayName.isBlank() || displayName.length > DISPLAY_NAME_MAX) {
            listOf("displayName must be 1-$DISPLAY_NAME_MAX non-blank chars" to AuthErrorCode.DISPLAY_NAME_INVALID)
        } else {
            emptyList()
        }
}
