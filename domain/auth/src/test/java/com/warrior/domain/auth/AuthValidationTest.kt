package com.warrior.domain.auth

import com.warrior.domain.auth.validation.AuthValidation
import com.warrior.domain.auth.validation.AuthValidationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidationTest {

    @Test
    fun validRegister_passes() {
        assertTrue(AuthValidation.validateRegister("warrior_1", "The Warrior", "password123").isEmpty())
    }

    @Test
    fun usernameRules_enforced() {
        assertTrue(AuthValidation.validateRegister("ab", "Name", "password123").isNotEmpty())
        assertTrue(AuthValidation.validateRegister("UPPER", "Name", "password123").isNotEmpty())
        assertTrue(AuthValidation.validateRegister("a".repeat(31), "Name", "password123").isNotEmpty())
        assertTrue(AuthValidation.validateRegister("ok_1", "Name", "password123").isEmpty())
    }

    @Test
    fun displayNameRules_enforced() {
        assertTrue(AuthValidation.validateRegister("warrior", " ", "password123").isNotEmpty())
        assertTrue(AuthValidation.validateRegister("warrior", "x".repeat(51), "password123").isNotEmpty())
    }

    @Test
    fun passwordMinLength_enforced() {
        assertTrue(AuthValidation.validateRegister("warrior", "Name", "1234567").isNotEmpty())
        assertTrue(AuthValidation.validateRegister("warrior", "Name", "12345678").isEmpty())
    }

    @Test
    fun requireRegister_throwsTypedError() {
        val error = runCatching { AuthValidation.requireRegister("x", " ", "z") }.exceptionOrNull()
        assertTrue(error is AuthValidationException)
        assertEquals(3, (error as AuthValidationException).messages.size)
    }

    @Test
    fun loginRequiresNonBlank() {
        assertTrue(AuthValidation.validateLogin(" ", "x").isNotEmpty())
        assertTrue(AuthValidation.validateLogin("u", "").isNotEmpty())
        assertTrue(AuthValidation.validateLogin("u", "p").isEmpty())
    }
}
