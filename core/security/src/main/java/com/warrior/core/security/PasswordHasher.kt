package com.warrior.core.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Local-account password hashing (Architecture v2.1 §14.1).
 *
 * Algorithm: PBKDF2WithHmacSHA256, >= 120,000 iterations (OWASP 2023 minimum),
 * 16-byte per-user SecureRandom salt, 32-byte derived key.
 * Salt is stored separately from the hash (users.passwordSalt column).
 * Verification uses [MessageDigest.isEqual] for constant-time comparison.
 */
object PasswordHasher {

    const val DEFAULT_ITERATIONS = 120_000
    const val SALT_BYTES = 16
    private const val KEY_LENGTH_BITS = 256
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"

    fun newSalt(): ByteArray = ByteArray(SALT_BYTES).apply { SecureRandom().nextBytes(this) }

    fun hash(password: String, salt: ByteArray, iterations: Int = DEFAULT_ITERATIONS): ByteArray {
        require(iterations > 0) { "iterations must be positive" }
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        return factory.generateSecret(spec).encoded
    }

    fun hashToHex(password: String, salt: ByteArray, iterations: Int = DEFAULT_ITERATIONS): String =
        hash(password, salt, iterations).toHex()

    /** Constant-time verification of a password against a stored hex hash. */
    fun verify(password: String, salt: ByteArray, expectedHex: String, iterations: Int = DEFAULT_ITERATIONS): Boolean {
        val expected = runCatching { expectedHex.hexToBytes() }.getOrNull() ?: return false
        return MessageDigest.isEqual(hash(password, salt, iterations), expected)
    }

    fun saltToHex(salt: ByteArray): String = salt.toHex()

    fun saltFromHex(hex: String): ByteArray = hex.hexToBytes()

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.hexToBytes(): ByteArray {
        check(length % 2 == 0) { "hex string must have even length" }
        return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}
