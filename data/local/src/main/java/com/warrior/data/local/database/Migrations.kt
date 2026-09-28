package com.warrior.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Schema v1 (frozen at Phase 2) -> v2 (Season 2 / Phase 12):
 * two purely additive tables. SQL must match the Room-generated schema in
 * `schemas/com.warrior.data.local.database.WarriorDatabase/2.json` byte for
 * byte in structure — MigrationTest validates exactly that via
 * MigrationTestHelper.runMigrationsAndValidate (Architecture Rule 11).
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `body_metrics` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `userId` INTEGER NOT NULL,
                `date` INTEGER NOT NULL,
                `weightKg` REAL NOT NULL,
                `heightCm` REAL,
                `reachCm` REAL,
                `bodyFatPercent` REAL,
                `restingHeartRate` INTEGER,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                FOREIGN KEY(`userId`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_body_metrics_userId` ON `body_metrics` (`userId`)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_body_metrics_userId_date` ON `body_metrics` (`userId`, `date`)",
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `session_tags` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `sessionId` INTEGER NOT NULL,
                `userId` INTEGER NOT NULL,
                `tag` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                FOREIGN KEY(`sessionId`) REFERENCES `training_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`userId`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_session_tags_sessionId` ON `session_tags` (`sessionId`)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_session_tags_userId_tag` ON `session_tags` (`userId`, `tag`)",
        )
    }
}
