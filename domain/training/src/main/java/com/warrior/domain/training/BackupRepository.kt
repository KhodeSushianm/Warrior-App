package com.warrior.domain.training

/**
 * Whole-device backup boundary (Season 2 / Phase 12). The app is local-first,
 * so a portable export is the user's only safety net — this restores full
 * fidelity: accounts (with password hashes), sessions, activities, rounds,
 * body metrics and tags.
 *
 * Format is versioned JSON ([formatVersion]); import REPLACES all data
 * (confirmed in the UI) inside a single transaction — a failed import leaves
 * the database untouched.
 */
/** Thrown when an import payload is not a valid WARRIOR backup. */
class BackupFormatException(message: String) : IllegalArgumentException(message)

interface BackupRepository {

    /** Serializes the whole database to a versioned JSON document. */
    suspend fun exportAll(): String

    /**
     * Validates then atomically replaces all data from a JSON document.
     * Returns the number of restored training sessions.
     * Throws on malformed/foreign/unsupported payloads.
     */
    suspend fun importAll(json: String): Int

    companion object {
        const val APP_ID = "WARRIOR"
        const val FORMAT_VERSION = 1
    }
}
