package com.warrior.data.local.repository

import androidx.room.withTransaction
import com.warrior.data.local.database.WarriorDatabase
import com.warrior.data.local.entity.BodyMetricEntity
import com.warrior.data.local.entity.RoundEntity
import com.warrior.data.local.entity.SessionTagEntity
import com.warrior.data.local.entity.TrainingSessionEntity
import com.warrior.data.local.entity.UserEntity
import com.warrior.data.local.entity.WorkoutActivityEntity
import com.warrior.domain.training.BackupFormatException
import com.warrior.domain.training.BackupRepository
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** Versioned whole-database backup document (Season 2 / Phase 12). */
@Serializable
data class WarriorBackupFile(
    val app: String,
    val formatVersion: Int,
    val exportedAt: Long,
    val users: List<UserEntity> = emptyList(),
    val trainingSessions: List<TrainingSessionEntity> = emptyList(),
    val workoutActivities: List<WorkoutActivityEntity> = emptyList(),
    val rounds: List<RoundEntity> = emptyList(),
    val bodyMetrics: List<BodyMetricEntity> = emptyList(),
    val sessionTags: List<SessionTagEntity> = emptyList(),
)

/**
 * JSON export/import of the whole local database.
 * Import parses + validates BEFORE touching storage and then replaces
 * everything in ONE transaction: a failure mid-way rolls back and leaves the
 * existing data untouched (same guarantee as session writes, Phase 5).
 */
@Singleton
class JsonBackupRepository @Inject constructor(
    private val db: WarriorDatabase,
) : BackupRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun exportAll(): String {
        val file = WarriorBackupFile(
            app = BackupRepository.APP_ID,
            formatVersion = BackupRepository.FORMAT_VERSION,
            exportedAt = System.currentTimeMillis(),
            users = db.userDao().getAllForExport(),
            trainingSessions = db.trainingSessionDao().getAllForExport(),
            workoutActivities = db.workoutActivityDao().getAllForExport(),
            rounds = db.roundDao().getAllForExport(),
            bodyMetrics = db.bodyMetricDao().getAllForExport(),
            sessionTags = db.sessionTagDao().getAllForExport(),
        )
        return json.encodeToString(WarriorBackupFile.serializer(), file)
    }

    override suspend fun importAll(payload: String): Int {
        // 1) Parse + validate with zero side effects.
        val file = try {
            json.decodeFromString(WarriorBackupFile.serializer(), payload)
        } catch (e: Exception) {
            throw BackupFormatException("not a valid WARRIOR backup file")
        }
        if (file.app != BackupRepository.APP_ID) {
            throw BackupFormatException("file is not a WARRIOR backup (app='${file.app}')")
        }
        if (file.formatVersion !in 1..BackupRepository.FORMAT_VERSION) {
            throw BackupFormatException("unsupported backup format version ${file.formatVersion}")
        }

        // 2) Atomic replace. Deleting users cascades to every child table
        //    (FK enforcement is on), then parents-first re-insert.
        db.withTransaction {
            db.userDao().deleteAll()
            file.users.forEach { db.userDao().insert(it) }
            file.trainingSessions.forEach { db.trainingSessionDao().insertForImport(it) }
            file.workoutActivities.forEach { db.workoutActivityDao().insertForImport(it) }
            file.rounds.forEach { db.roundDao().insertForImport(it) }
            file.bodyMetrics.forEach { db.bodyMetricDao().insert(it) }
            file.sessionTags.forEach { db.sessionTagDao().insert(it) }
        }
        return file.trainingSessions.size
    }
}
