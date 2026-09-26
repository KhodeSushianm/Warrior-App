package com.warrior.core.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHasherTest {

    // Independent vectors (python hashlib.pbkdf2_hmac sha256, dkLen=32).
    @Test
    fun matchesKnownPbkdf2Vectors() {
        val salt = "salt".toByteArray(Charsets.UTF_8)
        assertEquals(
            "120fb6cffcf8b32c43e7225256c4f837a86548c92ccc35480805987cb70be17b",
            PasswordHasher.hashToHex("password", salt, iterations = 1),
        )
        assertEquals(
            "ae4d0c95af6b46d32d0adff928f06dd02a303f8ef3c251dfd6e2d85a95474c43",
            PasswordHasher.hashToHex("password", salt, iterations = 2),
        )
        assertEquals(
            "c5e478d59288c841aa530db6845c4c8d962893a001ce4e11a4963873aa98134a",
            PasswordHasher.hashToHex("password", salt, iterations = 4096),
        )
    }

    @Test
    fun saltIs16BytesAndRandom() {
        val a = PasswordHasher.newSalt()
        val b = PasswordHasher.newSalt()
        assertEquals(PasswordHasher.SALT_BYTES, a.size)
        assertFalse(a.contentEquals(b))
    }

    @Test
    fun outputIs32Bytes() {
        assertEquals(32, PasswordHasher.hash("x", PasswordHasher.newSalt()).size)
    }

    @Test
    fun verify_acceptsCorrectAndRejectsWrong() {
        val salt = PasswordHasher.newSalt()
        val hex = PasswordHasher.hashToHex("correct horse battery", salt)
        assertTrue(PasswordHasher.verify("correct horse battery", salt, hex))
        assertFalse(PasswordHasher.verify("correct horse batter", salt, hex))
        assertFalse(PasswordHasher.verify("correct horse battery", PasswordHasher.newSalt(), hex))
        assertFalse(PasswordHasher.verify("correct horse battery", salt, "not-hex"))
    }

    @Test
    fun hashIsNotPlainTextAndSaltRoundTrip() {
        val salt = PasswordHasher.newSalt()
        val hex = PasswordHasher.hashToHex("warrior-pass-1", salt)
        assertFalse(hex.contains("warrior"))
        assertArrayEquals(salt, PasswordHasher.saltFromHex(PasswordHasher.saltToHex(salt)))
    }
}
