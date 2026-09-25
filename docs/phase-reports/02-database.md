# گزارش فاز ۲ — Database (Schema Freeze)

> تاریخ: ۲۰۲۶-۰۹-۲۶ | وضعیت: ✅ کامل‌شده | تست‌ها: **OK (7 tests)** | push: روی `main`
> معیار پذیرش مرجع: `EXECUTION-PLAN.md` فاز ۲

---

## ۱. خروجی‌ها

### Entityها (مطابق Database Design v4)
- `UserEntity` — ایندکس UNIQUE روی `username`
- `TrainingSessionEntity` — FK به users با `ON DELETE CASCADE`، ایندکس‌های `(userId)` و `(userId, date)`؛ **بدون ستون totalDuration** (Derived)
- `WorkoutActivityEntity` — FK CASCADE، ایندکس `(sessionId)`
- `RoundEntity` — FK CASCADE، ایندکس `(activityId)` و **UNIQUE `(activityId, roundNumber)`**
- تایم‌استمپ‌ها: `Long` UTC epoch millis؛ enumها با `Converters` به String

### DAOها (Ownership-enforced مطابق بخش‌های ۱۳–۱۶ سند)
- `UserDao`: insert / getById / getByUsername / observeAll / delete
- `TrainingSessionDao`: observeSessions(userId)، getSession(userId, sessionId)،
  `getSessionTotalDuration(userId, sessionId)` (نسخهٔ Ownership-darِ بخش ۱۸ سند؛ نسخهٔ بدون userId حذف شد)،
  updateSession، deleteSession(userId, sessionId)، و **`insertFullSession` تراکنشی** با `List<ActivityWithRounds>`
- `WorkoutActivityDao` / `RoundDao`: همهٔ read/deleteها با JOIN زنجیره‌ای تا `training_sessions.userId`

### دیتابیس و DI
- `WarriorDatabase` v1 با `exportSchema = true` (+ `room.incremental`)
- **Snapshot اسکیما commit شد**: `data/local/schemas/com.warrior.data.local.database.WarriorDatabase/1.json` = **نقطهٔ Freeze**
- `DatabaseModule` (Hilt): Singleton دیتابیس + چهار DAO

### تست‌ها — `WarriorDatabaseTest` (Robolectric, sdk 34)
1. `insertFullSession_writesEverythingAndDerivesDuration` — نوشتن کامل + duration مشتق‌شده (= SUM activityها) از هر دو کوئری Ownership-dar
2. `insertFullSession_rollsBackEverythingOnFailure` — Round تکراری وسط تراکنش → `SQLiteConstraintException` و **هیچ Session ناقصی باقی نمی‌ماند**
3. `deleteSession_cascadesActivitiesAndRounds`
4. `deleteUser_cascadesSessions`
5. `ownership_crossUserAccessIsImpossible` — خواندن/حذف کاربر B از دادهٔ کاربر A: همه no-op/null
6. `roundNumber_isUniquePerActivity`
7. `entityGuards_rejectInvalidValues` — intensity=11، restDuration=-1، roundNumber=0، duration=0 همه رد می‌شوند

نتیجهٔ اجرا: `OK (7 tests)`.

---

## ۲. نتیجهٔ DoD فاز ۲

| معیار | نتیجه |
|---|---|
| همهٔ تست‌ها سبز | ✅ OK (7 tests) |
| اسکیما v1 در ریپو commit شده (Freeze) | ✅ `data/local/schemas/.../1.json` (۴ جدول، ایندکس‌ها و CASCADEها با python validate شدند) |
| تراکنش atomic | ✅ تست rollback |
| Ownership در امضای متدها | ✅ + تست cross-user |

---

## ۳. تصمیم‌های پیاده‌سازی (ثبت در سند دیتابیس)

1. **CHECK constraints**: Room نمی‌تواند CHECK را در DDL صادر کند؛ معادل آن به‌صورت **گاردهای `init` روی Entityها** پیاده شد (خط دفاع آخر در لایهٔ داده، هم در write و هم read) + تست. سند دیتابیس بخش ۶ با یادداشت به‌روز شد.
2. **حذف `getTotalDuration(sessionId)` بدون Ownership** از سند و کد؛ نسخهٔ canonical همان `getSessionTotalDuration(userId, sessionId)` بخش ۱۸ است (قانون ۶).
3. Entityها دقیقاً با تایپ `Long` مطابق جدول‌های سند نگه داشته شدند؛ تبدیل Instant/Duration به Mapperهای فاز ۳ واگذار شد (یادداشت در Converters).

---

## ۴. نکتهٔ محیط (ادامهٔ الگوی فاز ۱)

اجرای تست Robolectric داخل Gradle در این sandbox یک‌گیگابایتی ممکن نبود (daemon + test worker با هم OOM می‌شدند).
**`scripts/test-lowram.sh`** اضافه شد: classpath تست را از Gradle می‌گیرد (task `dumpTestCp` با dependsOn روی transform ASM) و تست‌ها را در یک JVM مستقل 850m اجرا می‌کند — با cwd ماژول (چون `test_config.properties` مسیرهای نسبی دارد). روی ماشین‌های معمولی همان `./gradlew :data:local:testDebugUnitTest` کافی است.

---

## ۵. گام بعدی
فاز ۳ — Domain & Repository: مدل‌های دامنه با `Duration`، Mapperها، اینترفیس‌های Repository و پیاده‌سازی Room، Use Caseهای CRUD/History، Validation دامنه‌ای + تست‌های Unit خالص JVM. **منتظر تایید مالک.**
