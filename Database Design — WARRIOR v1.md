## 1. Tables

### `users`

|Column|Type|Constraints|
|---|---|---|
|`id`|`Long`|PK, AutoGenerate|
|`username`|`String`|NOT NULL, UNIQUE|
|`displayName`|`String`|NOT NULL|
|`passwordHash`|`String`|NOT NULL|
|`passwordSalt`|`String`|NOT NULL|
|`createdAt`|`Long`|NOT NULL — UTC epoch millis|
|`updatedAt`|`Long`|NOT NULL — UTC epoch millis|

**Indexes:**

```
UNIQUE INDEX (username)
```

---

## 2. `training_sessions`

|Column|Type|Constraints|
|---|---|---|
|`id`|`Long`|PK, AutoGenerate|
|`userId`|`Long`|FK → users.id, CASCADE|
|`date`|`Long`|NOT NULL — local-day midnight represented as UTC epoch millis|
|`startedAt`|`Long?`|NULL — UTC epoch millis|
|`endedAt`|`Long?`|NULL — UTC epoch millis|
|`overallIntensity`|`Int`|NOT NULL, CHECK 1–10|
|`overallFeeling`|`Feeling`|NOT NULL, stored as String|
|`notes`|`String?`|NULL|
|`createdAt`|`Long`|NOT NULL — UTC epoch millis|
|`updatedAt`|`Long`|NOT NULL — UTC epoch millis|

**Indexes:**

```
INDEX (userId)
INDEX (userId, date)
```

### Important

`totalDuration` در این جدول وجود ندارد.

مدت Session از مجموع Activityها محاسبه می‌شود:

```
SUM(workout_activities.duration)
WHERE sessionId = ?
```

بنابراین:

```
Room Raw Data
      ↓
Activity durations
      ↓
Derived Session Duration
```

---

# 3. `workout_activities`

|Column|Type|Constraints|
|---|---|---|
|`id`|`Long`|PK, AutoGenerate|
|`sessionId`|`Long`|FK → training_sessions.id, CASCADE|
|`type`|`WorkoutType`|NOT NULL, stored as String|
|`duration`|`Long`|NOT NULL, CHECK > 0|
|`intensity`|`Int`|NOT NULL, CHECK 1–10|
|`focusArea`|`FocusArea`|NOT NULL, stored as String|
|`notes`|`String?`|NULL|
|`createdAt`|`Long`|NOT NULL — UTC epoch millis|
|`updatedAt`|`Long`|NOT NULL — UTC epoch millis|

**Index:**

```
INDEX (sessionId)
```

---

# 4. `rounds`

|Column|Type|Constraints|
|---|---|---|
|`id`|`Long`|PK, AutoGenerate|
|`activityId`|`Long`|FK → workout_activities.id, CASCADE|
|`roundNumber`|`Int`|NOT NULL, CHECK > 0|
|`duration`|`Long`|NOT NULL, CHECK > 0|
|`restDuration`|`Long`|NOT NULL, CHECK >= 0|
|`intensity`|`Int`|NOT NULL, CHECK 1–10|
|`notes`|`String?`|NULL|
|`createdAt`|`Long`|NOT NULL — UTC epoch millis|
|`updatedAt`|`Long`|NOT NULL — UTC epoch millis|

**Indexes:**

```
INDEX (activityId)

UNIQUE INDEX (
    activityId,
    roundNumber
)
```

---

# 5. Relationships

```
User
 │
 └── 1:N ── TrainingSession
                │
                └── 1:N ── WorkoutActivity
                               │
                               └── 1:N ── Round
```

Foreign Keys:

```
users.id
    ↓
training_sessions.userId

training_sessions.id
    ↓
workout_activities.sessionId

workout_activities.id
    ↓
rounds.activityId
```

تمام Foreign Keyها:

```
ON DELETE CASCADE
```

---

# 6. Check Constraints

### Training Session

```
overallIntensity BETWEEN 1 AND 10
```

### Workout Activity

```
duration > 0
intensity BETWEEN 1 AND 10
```

### Round

```
roundNumber > 0
duration > 0
restDuration >= 0
intensity BETWEEN 1 AND 10
```

DB Constraint خط دفاع آخر است.

همین قوانین باید در **Domain Use Case** نیز enforce شوند.

> **یادداشت پیاده‌سازی (فاز ۲):** Room نمی‌تواند `CHECK` clause را در DDL صادر کند؛ بنابراین «خط دفاع آخر» در لایهٔ داده با **گاردهای `init` روی Entityها** پیاده‌سازی شده است: هر ساخت نمونه (هم در مسیر write و هم read) مقادیر نامعتبر را با `IllegalArgumentException` رد می‌کند. معنای قوانین بدون تغییر است و در `WarriorDatabaseTest` تست دارد.

```
UI Validation
      ↓
Domain Validation
      ↓
Database Constraint
```

---

# 7. Enums

## `WorkoutType`

```
enum class WorkoutType {
    CARDIO,
    HEAVY_BAG,
    MITT_WORK,
    SPARRING
}
```

## `FocusArea`

```
enum class FocusArea {
    JAB,
    FOOTWORK,
    DEFENSE,
    HEAD_MOVEMENT,
    COMBINATIONS,
    POWER,
    SPEED,
    CONDITIONING,
    TIMING,
    DISTANCE
}
```

## `Feeling`

```
enum class Feeling {
    EXCELLENT,
    GOOD,
    OKAY,
    TIRED,
    EXHAUSTED
}
```

---

# 8. Enum Stability Policy

مقادیر Enum که وارد Release شده‌اند:

- Rename نمی‌شوند.
- حذف نمی‌شوند.
- مقدار جدید می‌تواند اضافه شود.
- مقدار قدیمی می‌تواند از UI مخفی شود.
- Label نمایشی می‌تواند آزادانه تغییر کند.

اگر Rename واقعاً ضروری باشد:

```
Old Value
    ↓
Database Migration
    ↓
New Value
```

مثلاً:

```
UPDATE workout_activities
SET type = 'NEW_VALUE'
WHERE type = 'OLD_VALUE';
```

---

# 9. Time & Date Policy

تمام timestampها:

```
UTC epoch millis
```

شامل:

```
createdAt
updatedAt
startedAt
endedAt
```

### `date`

`date` نشان‌دهنده‌ی **روز محلی تمرین** است.

```
Local Date
    ↓
Local Midnight
    ↓
UTC Epoch Millis
    ↓
Database
```

مثال:

```
User Timezone:
Asia/Tehran

Training Date:
2026-09-25

Stored:
UTC epoch millis corresponding to
2026-09-25 00:00 Asia/Tehran
```

Weekly calculation با Timezone فعلی دستگاه و مرز هفتهٔ مرکزی انجام می‌شود: ثابت `WeekStartDay` (پیش‌فرض `SATURDAY` → هفتهٔ شنبه تا جمعه؛ سند معماری بخش ۱۳.۲).

```
Stored UTC timestamp
        ↓
Device Local Timezone
        ↓
Saturday → Friday
        ↓
Weekly Progress
```

---

# 10. Converters

```
Instant      ↔ Long
WorkoutType  ↔ String
FocusArea    ↔ String
Feeling      ↔ String
```

Durationها مستقیماً به شکل `Long` میلی‌ثانیه در Entity ذخیره می‌شوند و در Domain به `Duration` تبدیل می‌شوند.

```
Room Entity
    Long
     ↓
Mapper
     ↓
Domain
Duration
```

---

# 11. Database

```
Database: WarriorDatabase
Version: 1
exportSchema: true
```

Tables:

```
users
training_sessions
workout_activities
rounds
```

---

# 12. Source of Truth

```
Room
 │
 ├── Users
 ├── Sessions
 ├── Activities
 └── Rounds
        │
        ↓
   Raw Training Data
        │
        ↓
 Progress Engine
        │
        ↓
 Derived Metrics
```

در MVP جدول جداگانه‌ای برای موارد زیر وجود ندارد:

```
WeeklyProgress
Statistics
Streak
PersonalRecords
WorkoutDistribution
FocusDistribution
Charts
SessionTotalDuration
```

همه از Raw Data محاسبه می‌شوند.

---

# 13. DAO Contract

## `UserDao`

```
@Dao
interface UserDao {

    @Insert
    suspend fun insert(user: UserEntity): Long

    @Query("""
        SELECT * FROM users
        WHERE id = :id
    """)
    suspend fun getById(id: Long): UserEntity?

    @Query("""
        SELECT * FROM users
        WHERE username = :username
    """)
    suspend fun getByUsername(username: String): UserEntity?

    @Query("""
        SELECT * FROM users
    """)
    fun observeAll(): Flow<List<UserEntity>>

    @Delete
    suspend fun delete(user: UserEntity)
}
```

---

# 14. Training Session DAO

```
data class ActivityWithRounds(
    val activity: WorkoutActivityEntity,
    val rounds: List<RoundEntity> = emptyList(),
)

@Dao
abstract class TrainingSessionDao {

    @Query("""
        SELECT * FROM training_sessions
        WHERE userId = :userId
        ORDER BY date DESC
    """)
    abstract fun observeSessions(
        userId: Long
    ): Flow<List<TrainingSessionEntity>>

    @Query("""
        SELECT * FROM training_sessions
        WHERE userId = :userId
        AND id = :sessionId
    """)
    abstract suspend fun getSession(
        userId: Long,
        sessionId: Long
    ): TrainingSessionEntity?

    @Query("""
        SELECT COALESCE(
            SUM(wa.duration), 0
        )
        FROM workout_activities wa
        INNER JOIN training_sessions ts
            ON wa.sessionId = ts.id
        WHERE wa.sessionId = :sessionId
        AND ts.userId = :userId
    """)
    abstract suspend fun getSessionTotalDuration(
        userId: Long,
        sessionId: Long
    ): Long

    @Insert
    protected abstract suspend fun insertSession(
        session: TrainingSessionEntity
    ): Long

    @Insert
    protected abstract suspend fun insertActivities(
        activities: List<WorkoutActivityEntity>
    ): List<Long>

    @Insert
    protected abstract suspend fun insertRounds(rounds: List<RoundEntity>)

    @Update
    abstract suspend fun updateSession(
        session: TrainingSessionEntity
    )

    @Query("""
        DELETE FROM training_sessions
        WHERE userId = :userId
        AND id = :sessionId
    """)
    abstract suspend fun deleteSession(
        userId: Long,
        sessionId: Long
    )

    @Transaction
    open suspend fun insertFullSession(
        session: TrainingSessionEntity,
        activitiesWithRounds: List<ActivityWithRounds>,
    ): Long {

        val sessionId = insertSession(session)

        val activityIds = insertActivities(
            activitiesWithRounds.map {
                it.activity.copy(sessionId = sessionId)
            }
        )

        // Room ترتیب IDهای برگشتی insertAll را مطابق ترتیب ورودی تضمین می‌کند،
        // پس این جفت‌سازی داخلی و امن است.
        val allRounds = activitiesWithRounds.flatMapIndexed { index, awr ->
            awr.rounds.map { it.copy(activityId = activityIds[index]) }
        }

        if (allRounds.isNotEmpty()) {
            insertRounds(allRounds)
        }

        return sessionId
    }
}
```

### نکته معماری

Transaction boundary همین یک متد در `TrainingSessionDao` است (یک DAO در Room می‌تواند روی چند Entity کار کند). `WorkoutActivityDao` و `RoundDao` فقط برای عملیات تکی روی Activity/Round یک Session **موجود** استفاده می‌شوند و Repository برای ایجاد Session فقط همین یک entry point را صدا می‌زند؛ هدف این است که هیچ Use Caseای نتواند Session را بدون Activity/Round مربوطه به‌صورت ناقص commit کند. این امضا دقیقاً با بخش ۱۵.۲ سند معماری یکسان است.

---

# 15. Activity DAO — Ownership Enforced

به‌جای اینکه:

```
delete(activityId)
```

داشته باشیم، Repository ابتدا Session مالک را مشخص می‌کند و DAO Contract برای عملیات حساس، `userId` را نیز دریافت می‌کند.

```
@Dao
interface WorkoutActivityDao {

    @Query("""
        SELECT wa.*
        FROM workout_activities wa
        INNER JOIN training_sessions ts
            ON wa.sessionId = ts.id
        WHERE wa.id = :activityId
        AND ts.userId = :userId
    """)
    suspend fun getById(
        userId: Long,
        activityId: Long
    ): WorkoutActivityEntity?

    @Query("""
        SELECT wa.*
        FROM workout_activities wa
        INNER JOIN training_sessions ts
            ON wa.sessionId = ts.id
        WHERE wa.sessionId = :sessionId
        AND ts.userId = :userId
        ORDER BY wa.id ASC
    """)
    suspend fun getActivities(
        userId: Long,
        sessionId: Long
    ): List<WorkoutActivityEntity>

    @Query("""
        SELECT COALESCE(SUM(wa.duration), 0)
        FROM workout_activities wa
        INNER JOIN training_sessions ts
            ON wa.sessionId = ts.id
        WHERE wa.sessionId = :sessionId
        AND ts.userId = :userId
    """)
    suspend fun sumDurationBySession(
        userId: Long,
        sessionId: Long
    ): Long

    @Insert
    suspend fun insert(
        activity: WorkoutActivityEntity
    ): Long

    @Insert
    suspend fun insertAll(
        activities: List<WorkoutActivityEntity>
    ): List<Long>

    @Update
    suspend fun update(
        activity: WorkoutActivityEntity
    )

    @Query("""
        DELETE FROM workout_activities
        WHERE id = :activityId
        AND sessionId IN (
            SELECT id
            FROM training_sessions
            WHERE userId = :userId
        )
    """)
    suspend fun delete(
        userId: Long,
        activityId: Long
    )
}
```

---

# 16. Round DAO — Ownership Enforced

```
@Dao
interface RoundDao {

    @Query("""
        SELECT r.*
        FROM rounds r
        INNER JOIN workout_activities wa
            ON r.activityId = wa.id
        INNER JOIN training_sessions ts
            ON wa.sessionId = ts.id
        WHERE r.id = :roundId
        AND ts.userId = :userId
    """)
    suspend fun getById(
        userId: Long,
        roundId: Long
    ): RoundEntity?

    @Query("""
        SELECT r.*
        FROM rounds r
        INNER JOIN workout_activities wa
            ON r.activityId = wa.id
        INNER JOIN training_sessions ts
            ON wa.sessionId = ts.id
        WHERE r.activityId = :activityId
        AND ts.userId = :userId
        ORDER BY r.roundNumber ASC
    """)
    suspend fun getRounds(
        userId: Long,
        activityId: Long
    ): List<RoundEntity>

    @Insert
    suspend fun insert(
        round: RoundEntity
    ): Long

    @Insert
    suspend fun insertAll(
        rounds: List<RoundEntity>
    )

    @Update
    suspend fun update(
        round: RoundEntity
    )

    @Query("""
        DELETE FROM rounds
        WHERE id = :roundId
        AND activityId IN (
            SELECT wa.id
            FROM workout_activities wa
            INNER JOIN training_sessions ts
                ON wa.sessionId = ts.id
            WHERE ts.userId = :userId
        )
    """)
    suspend fun delete(
        userId: Long,
        roundId: Long
    )
}
```

---

# 17. Ownership Rule

برای تمام عملیات روی Training Data:

```
User
 ↓
TrainingSession
 ↓
WorkoutActivity
 ↓
Round
```

مالکیت باید قابل اثبات باشد.

### Session

```
userId + sessionId
```

### Activity

```
userId + activityId
```

### Round

```
userId + roundId
```

در Queryهای Activity و Round این مالکیت با `JOIN` تا `training_sessions.userId` enforce می‌شود.

---

# 18. Session Total Duration

```
suspend fun getSessionTotalDuration(
    userId: Long,
    sessionId: Long
): Long
```

Query:

```
SELECT COALESCE(SUM(wa.duration), 0)
FROM workout_activities wa
INNER JOIN training_sessions ts
    ON wa.sessionId = ts.id
WHERE wa.sessionId = :sessionId
AND ts.userId = :userId
```

Source of Truth:

```
Activity.duration
```

نه:

```
startedAt
endedAt
```

---

# 19. Transaction Contract

## Create Session

```
BEGIN TRANSACTION

Insert TrainingSession
        ↓
Insert Activities
        ↓
Get Activity IDs
        ↓
Assign Activity IDs to Rounds
        ↓
Insert Rounds

COMMIT
```

اگر هر مرحله Fail شود:

```
ROLLBACK
```

هیچ Session ناقصی نباید باقی بماند.

---

## Delete Session

```
BEGIN TRANSACTION

DELETE TrainingSession
WHERE userId = ?
AND id = ?

        ↓

ON DELETE CASCADE

        ↓

Activities
        ↓
Rounds

COMMIT
```

---

# 20. Soft Delete

در MVP:

```
NO SOFT DELETE
```

حذف واقعی با:

```
ON DELETE CASCADE
```

انجام می‌شود.

قبل از حذف:

```
Confirmation Dialog
```

نمایش داده می‌شود.

---

# 21. Migration

```
Database Version = 1
exportSchema = true
```

قوانین:

```
هر تغییر Schema
        ↓
Migration(from, to)
        ↓
Migration Test
        ↓
Merge
```

ممنوع:

```
fallbackToDestructiveMigration()
```

در Release.

---

# 22. Export Readiness

در MVP:

```
No Export / Import
```

طراحی آینده:

```
Export
   ↓
JSON Snapshot
   ↓
Schema Version
   ↓
Validation
   ↓
Transaction
   ↓
Import
```

اطلاعات حساس Authentication در Export آینده نباید شامل:

```
passwordHash
passwordSalt
```

باشد.

---

# 23. Database Architecture

```
                    ┌─────────────┐
                    │    User     │
                    └──────┬──────┘
                           │
                           │ 1:N
                           ▼
                 ┌──────────────────┐
                 │ TrainingSession  │
                 └────────┬─────────┘
                          │
                          │ 1:N
                          ▼
                ┌───────────────────┐
                │ WorkoutActivity   │
                └─────────┬─────────┘
                          │
                          │ 1:N
                          ▼
                    ┌──────────┐
                    │  Round   │
                    └──────────┘
```

---

# 24. Final Schema

```
WarriorDatabase v1

users
├── id PK
├── username UNIQUE
├── displayName
├── passwordHash
├── passwordSalt
├── createdAt
└── updatedAt

training_sessions
├── id PK
├── userId FK → users.id
├── date
├── startedAt
├── endedAt
├── overallIntensity
├── overallFeeling
├── notes
├── createdAt
└── updatedAt

workout_activities
├── id PK
├── sessionId FK → training_sessions.id
├── type
├── duration
├── intensity
├── focusArea
├── notes
├── createdAt
└── updatedAt

rounds
├── id PK
├── activityId FK → workout_activities.id
├── roundNumber
├── duration
├── restDuration
├── intensity
├── notes
├── createdAt
└── updatedAt
```

**Status: Database Design v4 — SCHEMA FROZEN at Phase 2 (snapshot: `data/local/schemas/com.warrior.data.local.database.WarriorDatabase/1.json`). Any future change = explicit Migration + test (Architecture Rule 11).**