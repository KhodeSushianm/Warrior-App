# WARRIOR — System Architecture

> نسخه: 2.0 (Revised)
> وضعیت: Architecture / Product Definition
> پلتفرم: Android
> مدل اجرا: Completely Offline / Local-First
> تکنولوژی اصلی: Kotlin + Jetpack Compose

---

## تغییرات نسبت به نسخه ۱.۰

این نسخه روی معماری قبلی ساخته شده و مسائل زیر رو که در نسخه اول مبهم بودن، صریح کرده:

1. **Password Hashing** — الگوریتم و پارامترهای دقیق مشخص شده (بخش ۲۱).
2. **Transaction Strategy** — نحوه‌ی دقیق ذخیره‌ی Session + Activities + Rounds به‌صورت atomic (بخش ۱۹).
3. **Migration Strategy** — رویکرد مشخص برای Room migrations به‌جای destructive fallback (بخش ۲۰).
4. **Multi-Account روی یک دستگاه** — صراحتاً مشخص شده که چند اکانت محلی روی یک گوشی پشتیبانی می‌شه یا نه (بخش ۲۲).
5. **فشرده‌سازی** — بخش‌های تکراری (نمودارهای مفهومی مشابه) در نسخه اول ادغام شدن تا سند به‌عنوان یک مرجع کدنویسی، مستقیم‌تر و قابل‌استفاده‌تر باشه.

بقیه‌ی ساختار (لایه‌بندی، مدل داده، Progress Engine، Development Order) بدون تغییر معنایی باقی مونده چون در نسخه اول درست بودن — فقط دقیق‌تر شدن.

---

# 1. هدف این سند

این سند معماری فنی و محصولی اپلیکیشن **WARRIOR** را تعریف می‌کند تا قبل از شروع کدنویسی دقیقاً بدانیم:

- چه چیزی می‌سازیم و چرا Backend در MVP وجود ندارد.
- داده‌ها چگونه ذخیره، Migrate و محافظت می‌شوند.
- اجزای سیستم چگونه با یکدیگر ارتباط دارند و مرز هر لایه کجاست.
- عملیات چندمرحله‌ای (مثل ذخیره‌ی یک Session) چگونه Atomic باقی می‌مانند.
- Progress و Analytics چگونه محاسبه می‌شوند.
- چه تصمیم‌هایی برای آینده از الان در معماری لحاظ می‌شود.

این سند مرجع اصلی توسعه‌ی MVP است.

---

# 2. Product Definition

WARRIOR یک اپلیکیشن اندرویدی برای بوکسورهاست که به آن‌ها اجازه می‌دهد:

1. تمرینات خود را ثبت کنند.
2. جلسات تمرینی قبلی را مشاهده کنند.
3. جزئیات هر Workout و Round را نگهداری کنند.
4. حجم و الگوی تمرین را تحلیل کنند.
5. Progress خود را در بازه‌های زمانی مختلف مشاهده کنند.
6. Personal Recordها و رکوردهای تمرینی را دنبال کنند.

تمرکز محصول روی **ثبت و تحلیل عملکرد تمرینی** است، نه آموزش تکنیک بوکس.

---

# 3. Core Product Loop

```text
LOG → TRACK → ANALYZE → IMPROVE
```

| مرحله | توضیح |
|---|---|
| Log | کاربر یک Training Session ثبت می‌کند |
| Track | تاریخچه، مدت تمرین، Roundها، شدت و Focusها ذخیره می‌شوند |
| Analyze | اپ از داده خام، آمار هفتگی و Progress Chart تولید می‌کند |
| Improve | کاربر با دیدن روند خودش تصمیم بهتر می‌گیرد |

> اپلیکیشن نباید صرفاً از روی افزایش یک عدد ادعا کند که «بوکسور بهتر شده است».
> سیستم داده و روند را نشان می‌دهد؛ تفسیر تا حد امکان به کاربر واگذار می‌شود.

---

# 4. MVP Scope

## 4.1 Features

- **Authentication / Local Account**: ثبت‌نام، Login، Session محلی، Logout
- **Home Dashboard**: آمار هفته جاری، Recent Session، Personal Records، CTA
- **Workout Logging**: ایجاد Session، چند Activity در هر Session، Duration/Intensity/Focus/Notes، Roundهای Activityهای Round-based
- **History**: لیست، گروه‌بندی بر اساس تاریخ، جزئیات، Edit، Delete
- **Progress**: Weekly Statistics، Training Volume، Distribution، Trend Charts
- **Profile**: Display Name، Username، Settings، Logout

---

# 5. Offline-First Architecture

```text
Android App
    ├── UI (Compose)
    ├── ViewModel
    ├── Domain / Use Cases
    ├── Repository
    └── Local Database (Room / SQLite)
```

هیچ Request اجباری به اینترنت وجود ندارد. تمام موارد زیر روی دستگاه ذخیره می‌شوند: User Account، Password Hash، Training Sessions، Workout Activities، Rounds، Notes، Profile Data، Progress Data، Personal Records، App Preferences.

## 5.1 چرا Backend فعلاً نداریم؟

Backend وقتی ضروری می‌شود که بخواهیم Sync بین چند دستگاه، Cloud Backup، Social Features، Coach Accounts، Online Authentication، Remote Analytics یا Billing اضافه کنیم. برای محصول فعلی، Local Database تمام نیازهای اصلی را پوشش می‌دهد.

## 5.2 محدودیت‌های Offline

- **Device Migration**: با تعویض گوشی، داده به‌صورت خودکار منتقل نمی‌شود.
- **Device Loss**: با Reset یا گم‌شدن گوشی، داده از بین می‌رود مگر Backup داشته باشیم.
- **Local Account**: Username/Password یک Identity واقعی در Cloud نیست؛ روی گوشی دیگر همان کاربر داده‌ای ندارد.

مسیر آینده: `Export Backup → Import Backup → Cloud Sync`.

---

# 6. Architectural Principle

> UI نباید مستقیماً با Database صحبت کند.

```text
Composable → ViewModel → Use Case → Repository → Room DAO → SQLite
```

لایه‌های اصلی سیستم: `Presentation → Domain → Data → Infrastructure/Android`

---

# 7. Presentation Layer

شامل Jetpack Compose Screens، Components، ViewModel، UI State، UI Events. UI نباید Business Logic پیچیده داشته باشد.

```kotlin
data class HomeUiState(
    val isLoading: Boolean = true,
    val trainingTime: Duration = Duration.ZERO,
    val sessionCount: Int = 0,
    val roundCount: Int = 0,
    val recentSessions: List<SessionSummary> = emptyList(),
    val error: String? = null
)
```

UI فقط State را Render می‌کند (Unidirectional Data Flow):

```text
User Action → UI Event → ViewModel → Use Case → Repository → Database
                                                                  ↓
Compose UI ← UI State ← ViewModel ← Flow ─────────────────────────┘
```

---

# 8. Domain Layer

قلب منطقی سیستم؛ شامل Domain Models، Use Cases، Progress Calculation، Validation Rules، Business Rules. Domain نباید به Android UI وابسته باشد.

## Use Caseهای اصلی

```text
CreateLocalAccount, Login, Logout
CreateTrainingSession, UpdateTrainingSession, DeleteTrainingSession
GetTrainingSession, GetTrainingHistory
AddWorkoutActivity, UpdateWorkoutActivity, DeleteWorkoutActivity
AddRound, UpdateRound, DeleteRound
GetDashboardStats, GetWeeklyProgress
GetWorkoutDistribution, GetFocusDistribution, GetPersonalRecords
```

---

# 9. Repository Layer

Repository واسط بین Domain و Data است. ViewModel نباید DAO را مستقیماً صدا بزند.

```kotlin
interface TrainingRepository {
    fun observeSessions(userId: Long): Flow<List<TrainingSession>>
    suspend fun getSession(sessionId: Long): TrainingSession?
    suspend fun saveSession(session: TrainingSession)
    suspend fun deleteSession(sessionId: Long)
}
```

**مزیت**: در آینده می‌توان `Room + API + Sync Manager` را بدون بازطراحی UI اضافه کرد.

MVP: `Repository → Room` | آینده: `Repository → { Room, API, Sync }`

---

# 10. Data Layer

```text
local/
    dao/
    entity/
    database/
repository/
mapper/
```

---

# 11. Core Data Model

```text
User
 └── TrainingSession
       ├── WorkoutActivity
       │      └── Round
       └── Notes
```

## 11.1 User

```text
User
----------------------
id
username
displayName
passwordHash
passwordSalt
createdAt
updatedAt
```

## 11.2 TrainingSession

```text
TrainingSession
----------------------
id
userId
date
startedAt
endedAt
totalDuration
overallIntensity
overallFeeling
notes
createdAt
updatedAt
```

## 11.3 WorkoutActivity

```text
WorkoutActivity
----------------------
id
sessionId
type
duration
intensity
focusArea
notes
createdAt
updatedAt
```

## 11.4 Round

```text
Round
----------------------
id
activityId
roundNumber
duration
restDuration
intensity
notes
```

## 11.5 Workout Types (قابل توسعه)

نسخه اول: `CARDIO, HEAVY_BAG, MITT_WORK, SPARRING`
آینده: `SHADOW_BOXING, TECHNICAL_DRILL, STRENGTH, CONDITIONING, JUMP_ROPE, OTHER`

Typeها به‌شکل enum/domain value تعریف می‌شوند؛ UI مستقیماً به Stringهای پراکنده وابسته نیست.

## 11.6 Focus Areas

`JAB, FOOTWORK, DEFENSE, HEAD_MOVEMENT, COMBINATIONS, POWER, SPEED, CONDITIONING, TIMING, DISTANCE`

در MVP هر Activity یک Focus دارد؛ طراحی Domain امکان چند-Focus در آینده را حفظ می‌کند.

## 11.7 Relationships & Foreign Keys

```text
User 1 ─── N TrainingSession
TrainingSession 1 ─── N WorkoutActivity
WorkoutActivity 1 ─── N Round
```

```text
TrainingSession.userId    → User.id              (ON DELETE CASCADE)
WorkoutActivity.sessionId → TrainingSession.id    (ON DELETE CASCADE)
Round.activityId          → WorkoutActivity.id    (ON DELETE CASCADE)
```

حذف Session باید Activityها و Roundهای متعلق به آن را نیز حذف کند (Cascade، نه Manual Cleanup).

---

# 12. Raw Data vs Derived Data

## Raw Data (ورودی کاربر)
`Session, Activity, Round, Intensity, Focus, Notes, Date, Duration`

## Derived Data (محاسبه‌شده)
`Weekly Training Time, Session Count, Round Count, Average Intensity, Workout Distribution, Focus Distribution, Streak, Longest Session, Most Rounds`

> اصل: Derived Data هرگز به‌صورت دستی توسط کاربر ثبت نمی‌شود.

---

# 13. Progress Engine

```text
Room → Training Data → Progress Engine → Metrics → Charts / Dashboard / Reports
```

## 13.1 Weekly Progress

هر هفته: `Sessions, Rounds, Training Time, Average Intensity, Training Days, Average Session Duration, Workout Distribution, Focus Distribution` — به‌همراه مقایسه با هفته قبل (مثلاً `Sessions: +1`).

## 13.2 Weekly Boundary

هفته با یک Rule ثابت محاسبه می‌شود: **Monday → Sunday**. این Rule در یک محل مرکزی (مثلاً `WeekBoundaryProvider`) تعریف و در کل اپ یکسان استفاده می‌شود؛ هیچ Feature نباید boundary خودش را حساب کند.

## 13.3 Source of Truth

```text
Room Raw Data = Source of Truth
```

```text
درست:    Room → Calculate → Dashboard
غلط:    Dashboard → Save statistic → Assume it is correct
```

## 13.4 Derived Metrics Storage Decision

در MVP، Metrics هفتگی به‌صورت دائمی ذخیره **نمی‌شوند** مگر نیاز عملکردی مشخصی ایجاد شود:

```text
Raw Training Data → Progress Calculator → WeeklyProgress (in-memory / cached)
```

مزیت: داده تکراری کمتر، ناسازگاری کمتر، همیشه قابل محاسبه‌ی مجدد، تغییر Algorithm آسان‌تر.

اگر در آینده Dataset بزرگ شود، محاسبات سنگین شوند یا Offline Analytics گسترده شود، می‌توان یک Aggregation Table اضافه کرد — اما در MVP ضرورتی ندارد.

---

# 14. Authentication & Security (تکمیل‌شده)

## 14.1 Password Hashing — الگوریتم مشخص

Password هرگز Plain Text ذخیره نمی‌شود. الگوریتم انتخابی: **Argon2id** (از طریق کتابخانه `bouncycastle` یا `argon2-jvm`)، با پارامترهای:

```text
Algorithm:    Argon2id
Memory:       19 MiB (19456 KB)
Iterations:   2
Parallelism:  1
Salt:         16 bytes, per-user, SecureRandom
Output hash:  32 bytes
```

اگر Argon2 روی Android به دلیل حجم کتابخانه توجیه نداشت، جایگزین قابل‌قبول: **PBKDF2WithHmacSHA256** با حداقل ۱۲۰,۰۰۰ iteration (توصیه‌ی OWASP 2023) — این گزینه در AndroidX به‌صورت native در دسترس است و نیازی به کتابخانه‌ی خارجی ندارد، پس برای MVP ترجیح داده می‌شود.

```text
password + salt
      ↓
PBKDF2WithHmacSHA256 (120,000 iterations)
      ↓
passwordHash (stored) + salt (stored separately)
```

`salt` به‌صورت یک ستون جدا (`passwordSalt`) در `User` entity ذخیره می‌شود، نه ترکیب‌شده داخل hash.

## 14.2 Local Session

Session محلی با `DataStore` (نه SharedPreferences) نگه‌داری می‌شود:

```text
DataStore
----------------------
currentUserId: Long?
sessionCreatedAt: Long?
```

Logout یعنی پاک‌شدن این مقادیر، نه حذف داده‌ی کاربر.

## 14.3 Multi-Account روی یک دستگاه

**تصمیم صریح**: در MVP، سیستم از **چند اکانت محلی روی یک دستگاه** پشتیبانی می‌کند (نه صرفاً تک‌کاربره). دلیل: جدول `User` از ابتدا با `id` مستقل طراحی شده و تمام Queryها بر اساس `userId` فیلتر می‌شوند (نگاه کنید به بخش ۱۴.۴)، بنابراین افزودن اکانت دوم هزینه‌ی معماری اضافه ندارد؛ فقط باید در UI یک ورودی «Switch Account / Add Account» در Profile اضافه شود (خارج از اسکوپ MVP اولیه، اما بدون تغییر در Data Layer قابل افزودن است).

اگر تیم ترجیح می‌دهد MVP را تک‌کاربره نگه دارد، تنها تغییر لازم این است که `DataStore.currentUserId` هرگز عوض نشود؛ خود مدل داده نیازی به تغییر ندارد.

## 14.4 Ownership Rule

هر Query که داده‌ی کاربر را برمی‌گرداند، باید `userId` را در `WHERE` یا از طریق JOIN تا `TrainingSession.userId` رعایت کند. این قانون در DAO سطح امضای متد اجباری می‌شود (هر متد History/Progress پارامتر `userId` می‌گیرد)، نه به‌صورت توافق ضمنی.

---

# 15. Transaction Strategy (تکمیل‌شده)

## 15.1 مسئله

ذخیره‌ی یک Session شامل چند نوشتن هم‌زمان است: خود Session، N تا Activity، و برای هرکدام M تا Round. اگر این عملیات atomic نباشد و وسط راه قطع شود (مثلاً crash یا خطای دیسک)، داده‌ی ناقص (Session بدون Activity، یا Activity بدون Round) در دیتابیس باقی می‌ماند.

## 15.2 راه‌حل: Room `@Transaction`

تمام نوشتن‌های مرکب از طریق یک متد `@Transaction` در DAO یا Repository انجام می‌شوند، نه چند `insert` جدا از هم که از ViewModel صدا زده شوند:

```kotlin
@Dao
abstract class TrainingSessionDao {

    @Insert
    abstract suspend fun insertSession(session: TrainingSessionEntity): Long

    @Insert
    abstract suspend fun insertActivities(activities: List<WorkoutActivityEntity>): List<Long>

    @Insert
    abstract suspend fun insertRounds(rounds: List<RoundEntity>)

    @Transaction
    open suspend fun insertFullSession(
        session: TrainingSessionEntity,
        activities: List<WorkoutActivityEntity>,
        roundsByActivityIndex: Map<Int, List<RoundEntity>>
    ): Long {
        val sessionId = insertSession(session)
        val activityIds = insertActivities(
            activities.map { it.copy(sessionId = sessionId) }
        )
        val allRounds = roundsByActivityIndex.flatMap { (index, rounds) ->
            rounds.map { it.copy(activityId = activityIds[index]) }
        }
        if (allRounds.isNotEmpty()) insertRounds(allRounds)
        return sessionId
    }
}
```

## 15.3 قانون کلی

> هر Use Case که بیش از یک جدول را تغییر می‌دهد، باید از طریق یک متد `@Transaction` واحد در DAO عبور کند. Repository اجازه ندارد چند `suspend fun` مجزای DAO را پشت‌سرهم صدا بزند و امیدوار باشد هیچ‌کدام fail نشود.

همین قانون برای `Edit` و `Delete` روی Session (که Activity/Round وابسته دارند) هم اعمال می‌شود؛ `Delete` با Cascade FK خودکار انجام می‌شود ولی باید داخل یک Transaction باشد تا با عملیات هم‌زمان (مثلاً یک Flow observer) تداخل نکند.

## 15.4 خطا و Rollback

اگر هر مرحله‌ی داخل `@Transaction` Exception پرتاب کند، Room کل تراکنش را rollback می‌کند — این رفتار پیش‌فرض Room/SQLite است و نیازی به کد دستی ندارد. Use Case لایه‌ی بالاتر باید این Exception را catch کند و آن را به یک `Result<Unit>` یا خطای Domain-level تبدیل کند تا UI بتواند پیام مناسب نشان دهد.

---

# 16. Migration Strategy (تکمیل‌شده)

## 16.1 مسئله در نسخه‌ی قبلی

فقط عنوان «Migration Strategy» ذکر شده بود بدون رویکرد مشخص. برای اپی که تمام داده‌ی کاربر روی خود دستگاه است، **destructive migration** (`fallbackToDestructiveMigration()`) قابل‌قبول نیست چون یعنی از دست رفتن کامل تاریخچه‌ی تمرین کاربر با هر آپدیت اسکیما.

## 16.2 رویکرد انتخابی

```text
Database Version 1 (MVP Launch)
      ↓
هر تغییر اسکیما = یک Migration صریح (Migration(from, to))
      ↓
Room exportSchema = true
      ↓
Schema JSON files در version control (برای تست Migration)
```

قوانین:

1. **`exportSchema = true`** از همان نسخه‌ی اول، تا فایل‌های JSON اسکیمای هر نسخه در پروژه ذخیره شود و بشود Migrationهای بعدی را در تست خودکار (`MigrationTestHelper`) بررسی کرد.
2. **هیچ Migration ای داده حذف نمی‌کند** مگر ستون واقعاً از دامنه حذف شده باشد؛ در آن صورت هم قبلش یک بررسی محصولی لازم است.
3. اگر تغییر اسکیما پیچیده شود (مثلاً تغییر نوع یک ستون)، از الگوی «ساخت جدول جدید → کپی داده → drop جدول قدیم → rename» استفاده می‌شود، نه ALTER مستقیم در SQLite (که محدودیت دارد).
4. هر Migration باید یک تست مجزا در `androidTest` داشته باشد که یک دیتابیس نمونه با نسخه‌ی قبلی می‌سازد، Migration را اجرا می‌کند، و صحت داده را بعد از Migration بررسی می‌کند.

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE WorkoutActivity ADD COLUMN focusAreaSecondary TEXT DEFAULT NULL"
        )
    }
}
```

## 16.3 Rule اضافه‌شده

> **Rule 11**: هیچ Migration نباید بدون تست خودکار merge شود؛ `fallbackToDestructiveMigration()` فقط در Debug build و پشت یک BuildConfig flag مجاز است، هرگز در Release.

---

# 17. Architecture Rules (به‌روزشده)

| # | قانون |
|---|---|
| 1 | UI مستقیم به Room دسترسی ندارد |
| 2 | Business Logic داخل Composable نوشته نمی‌شود |
| 3 | Raw Data و Derived Data جدا هستند |
| 4 | Repository مرز Data و Domain است |
| 5 | Password هرگز Plain Text ذخیره نمی‌شود (PBKDF2WithHmacSHA256، ≥۱۲۰,۰۰۰ iteration، salt جدا) |
| 6 | هر Query داده‌ی User باید Ownership (`userId`) را در امضای متد رعایت کند |
| 7 | هر نوشتن مرکب (چند جدول) باید در یک `@Transaction` واحد در DAO انجام شود |
| 8 | Progress calculations باید Testable و بدون وابستگی به Android باشند |
| 9 | Featureها باید تا حد امکان مستقل باشند |
| 10 | Backend در MVP وجود ندارد |
| 11 | هیچ Migration بدون تست خودکار merge نمی‌شود؛ destructive fallback فقط در Debug |

---

# 18. Modular System Boundary

```text
:app
├── :feature:auth
├── :feature:home
├── :feature:workout
├── :feature:history
├── :feature:progress
└── :feature:profile
        ↓
    :domain:*
        ↓
     :data:*
        ↓
     :core:*
```

> Featureها مستقل، Domainها capability-oriented، Dataها implementation-oriented، Coreها shared infrastructure هستند.

```text
Jetpack Compose UI
      ↓
   ViewModel (UI State / Events)
      ↓
   Use Cases (Domain Logic)
      ↓
   Repository (Data Abstraction)
      ↓
   Room (Local DB)
      ↓
   SQLite (Device Storage)
```

---

# 19. MVP Development Order

| Phase | محتوا |
|---|---|
| 1 — Foundation | Android Project, Kotlin, Compose, Hilt, Navigation, Room, DataStore, Design System |
| 2 — Database | Entities, DAOها، `@Transaction` writes، `exportSchema=true` |
| 3 — Authentication | Register, PBKDF2 Hashing + Salt, Login, Local Session, Logout, Multi-account support |
| 4 — Workout Logging | Create Session (Transactional), Add Activity, Add Round, Edit, Delete, Validation |
| 5 — History | Session List, Detail, Search/Filter در صورت نیاز, Edit/Delete |
| 6 — Dashboard | Weekly Metrics, Recent Sessions, Personal Records |
| 7 — Progress Engine | Weekly Calculation, Distribution, Trends, Streak |
| 8 — Charts | Training Volume, Frequency, Workout/Focus Distribution |
| 9 — UI Polish | Animations, Empty/Loading/Error States, Typography, Dark Theme |
| 10 — Testing | Unit, Database (شامل Migration tests), UI, Edge Cases |

---

# 20. Definition of Done for MVP

کاربر باید بتواند: بدون اینترنت اجرا کند؛ Account محلی بسازد؛ Login کند؛ Session ایجاد کند؛ چند Workout با Round اضافه کند؛ Intensity/Focus/Notes ثبت کند؛ Session را ذخیره/Edit/Delete کند؛ آمار هفته، Progress، Chartها و Personal Records را ببیند؛ Logout کند.

---

# 21. Non-Goals of MVP

```text
Cloud Sync, Social Network, Coach Dashboard, Online Authentication,
Messaging, Public Profiles, Leaderboard, AI Coaching,
Online Payments, Subscription Backend, Community, Live Training
```

---

# 22. Next Technical Step

قبل از UI Feature development، باید **Database Schema دقیق نسخه‌ی ۱** نهایی شود، شامل:

```text
UserEntity, TrainingSessionEntity, WorkoutActivityEntity, RoundEntity
Primary Keys, Foreign Keys (ON DELETE CASCADE), Indexes
Enums (WorkoutType, FocusArea)
Converters (Duration ↔ Long)
Room DAO Methods (شامل @Transaction insertFullSession)
Database Version = 1, exportSchema = true
```

سپس:

```text
Domain Models → Use Cases → Repository Interfaces → ViewModels → UI State → Screen Design
```
