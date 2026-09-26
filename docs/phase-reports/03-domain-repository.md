# گزارش فاز ۳ — Domain & Repository

> تاریخ: ۲۰۲۶-۰۹-۲۶ | وضعیت: ✅ کامل‌شده و push‌شده روی `main` (کامیت `a4034d2`)
> تست‌ها: **۲۴ تست جدید، همه سبز** (۱۶ دامنهٔ تمرین + ۶ احراز هویت + ۲ مپر) | ktlint: سبز

---

## ۱. خروجی‌ها

### مدل‌های دامنه (`:domain:training`) — بدون هیچ وابستگی اندرویدی
- `TrainingSession` (Aggregate root با لیست Activityها)، `WorkoutActivity` (با لیست Roundها)، `Round`
- مدت‌ها با `kotlin.time.Duration`؛ تایم‌استمپ‌ها `Long` UTC millis (بدون نیاز به core-library desugaring)
- `totalDuration` و `totalRounds` همیشه **مشتق‌شده** (قانون Raw/Derived)

### Validation دامنه‌ای
- `TrainingValidation`: شدت ۱..۱۰ (Session/Activity/Round)، duration>0، restDuration>=0، roundNumber>0، حداقل یک Activity؛ خطای نوع‌دار `ValidationException`
- `AuthValidation` در `:domain:auth`: username با `^[a-z0-9_]{3,30}$`، displayName تا ۵۰ کاراکتر، password ≥ ۸ (برای فاز ۴)

### Repository (مرز Domain/Data، قانون ۴ و ۹ معماری)
- اینترفیس `TrainingRepository` در `:domain:training` (observeSessions/getSession/create/update/delete — همه Ownership-dar)
- `RoomTrainingRepository` (+ `RepositoryModule` با `@Binds`) در `:data:local`؛ تنها مصرف‌کنندهٔ DAOها
- نوشتن‌های مرکب فقط از طریق `insertFullSession` / **`updateFullSession`** (متد `@Transaction` جدید در DAO: update سربرگ ← حذف Activityهای قدیم با cascade ← درج درخت جدید)

### Use Caseها (بخش ۸ معماری)
`CreateTrainingSession`, `UpdateTrainingSession`, `DeleteTrainingSession`, `GetTrainingSession`, `GetTrainingHistory`, `AddWorkoutActivity`, `UpdateWorkoutActivity`, `DeleteWorkoutActivity`, `AddRound` (با شمارهٔ راند خودکار بعدی), `UpdateRound`, `DeleteRound` — همه با `Result<T>` و validation دامنه‌ای قبل از نوشتن.

### Mapperها
`SessionMapper`: entity↔domain با تبدیل `Long millis ↔ Duration`؛ ساخت `ActivityWithRounds` برای نوشتن تراکنشی.

---

## ۲. تست‌ها

| سوئیت | تعداد | نتیجه |
|---|---|---|
| `SessionUseCasesTest` (Fake repository، runTest) | 10 | ✅ |
| `TrainingValidationTest` | 6 | ✅ |
| `AuthValidationTest` | 6 | ✅ |
| `SessionMapperTest` (JVM خالص در data:local) | 2 | ✅ |
| `WarriorDatabaseTest` (فاز ۲، اجرای مجدد) | 7 | ✅ |

نکات پوشش: scoping کاربر در create/update/delete، مرتب‌سازی تاریخچه، شکست validation بدون اثر جانبی، شماره‌گذاری خودکار راند، خطای activity ناموجود.

**DoD فاز ۳:** تست‌های دامنه بدون وابستگی اندروید سبز (قانون ۸) ✅ | هیچ فراخوانی DAO خارج از `:data` ✅ (تنها مصرف‌کننده: RoomTrainingRepository).

---

## ۳. یادداشت‌ها

1. **اسکیما بدون تغییر** — `updateFullSession` فقط رفتار DAO است؛ Freeze فاز ۲ دست‌نخورده (diff روی `schemas/1.json` خالی).
2. **هزینهٔ N+1 در observeSessions**: هر emission، aggregateها را با کوئری‌های جدا بارگذاری می‌کند؛ برای حجم MVP قابل قبول است و در فاز ۱۰ (پرفورمنس) بازبینی می‌شود.
3. **شناسهٔ Activity/Round پس از update تغییر می‌کند** (حذف+درج مجدد)؛ جریان‌های UI همیشه از Repository رفرش می‌کنند (فاز ۵ این قرارداد را رعایت می‌کند).
4. محیط sandbox ریست شده بود (JDK/SDK/.git ناپایدارند): با `scripts/setup-toolchain.sh` بازسازی شد و تاریخچهٔ git با fetch از ریموت بازیابی شد؛ فایل‌های فاز ۳ بدون loss باقی ماندند.

---

## ۴. گام بعدی
فاز ۴ — Authentication: PBKDF2 در `:core:security`، Session manager روی DataStore، Use Caseهای Register/Login/Logout، صفحه‌های Compose Auth + ناوبری ریشه. **منتظر تایید مالک.**
