# نقشهٔ اجرای WARRIOR — اجرای فازبندی‌شده با دروازهٔ تایید

> نسخهٔ ۱.۰ — مبتنی بر `warrior-architecture.md` v2.1 و `Database Design — WARRIOR v1.md` v4
> اصل حاکم: **هیچ فازی بدون تایید صریح مالک پروژه شروع نمی‌شود و هیچ فازی بدون گزارش و تست سبز تمام نمی‌شود.**

---

## قواعد اجرا (Approval Gates)

1. شروع هر فاز فقط پس از **تایید صریح** مالک پروژه انجام می‌شود.
2. پایان هر فاز = commit و push روی `main` + **گزارش فاز** (کارهای انجام‌شده، تست‌ها، انحراف‌ها از برنامه) + بررسی مالک.
3. ورود پنهانی اسکوپ فاز بعد به فاز جاری ممنوع است؛ تغییر اسکوپ = به‌روزرسانی همین سند با تایید مجدد.
4. هر فاز با **معیار پذیرش (DoD)** تمام می‌شود، نه با «کد نوشته شد».
5. اسکیمای دیتابیس در پایان فاز ۲ **Freeze** می‌شود؛ بعد از آن هر تغییر = Migration مطابق بخش ۱۶ سند معماری.

## استراتژی تست و بررسی در این محیط

- **تست‌های JVM Unit** (دامنه، Use Caseها، Progress Engine) و **تست‌های Robolectric** (Room/DAO/تراکنش‌ها) در همین محیط اجرا می‌شوند و باید سبز باشند.
- **تست‌های Compose UI / Instrumented**: در هر فاز نوشته می‌شوند؛ اجرای واقعی روی دستگاه/امولاتور نیازمند دستگاه شماست — در پایان فازهای مربوطه، در صورت نیاز build تستی تحویل می‌شود.
- اگر محدودیت شبکه/دیسک محیط، دانلود Android SDK را غیرممکن کند: خروجی فاز = سورس کامل + اسکریپت بuild برای اجرای محلی شما، و همان ابتدا اطلاع داده می‌شود.

---

## جدول خلاصهٔ فازها

| فاز | نام | خروجی کلیدی | حجم |
|---|---|---|---|
| ۱ | Foundation | پروژهٔ ماژولار، ابزارها، دیزاین‌سیستم تیره/RTL | M |
| ۲ | Database | اسکیمای Room + DAOها + تراکنش‌ها + **Schema Freeze** | M‑L |
| ۳ | Domain & Repository | مدل‌های دامنه، Mapperها، Use Caseها، Validation | M |
| ۴ | Authentication | هش PBKDF2، Session در DataStore، صفحه‌های Register/Login | M |
| ۵ | Workout Logging | ساخت/ویرایش/حذف Session + Activity + Round | L |
| ۶ | History | لیست گروه‌بندی‌شده، جزئیات، Edit/Delete | M |
| ۷ | Progress Engine + Dashboard | موتور محاسبات هفتگی + صفحهٔ خانه | M‑L |
| ۸ | Charts + Progress Screen | نمودارهای سفارشی Compose + heatmap نقطه‌ای | M |
| ۹ | Profile + UI Polish | پروفایل، RTL/فارسی کامل، Empty/Loading/Errorها | M |
| ۱۰ | Hardening + Release RC | تست کامل، build ریلیز بدون destructive fallback، APK نهایی | M |

---

## فاز ۱ — Foundation: ابزارها، ماژول‌ها، دیزاین‌سیستم

**هدف:** اسکلتی که همهٔ فازهای بعد روی آن سوار می‌شوند؛ بدون هیچ فیچر محصولی.

**خروجی‌ها:**
- ست‌آپ محیط: JDK 17، Gradle Wrapper، Android SDK (سطح compile)، `libs.versions.toml`
- ماژول‌ها مطابق بخش ۱۸ معماری: `:app`، `:feature:auth|home|workout|history|progress|profile`، `:domain:auth|training|progress`، `:data:local`، `:core:common|designsystem|security`
- وابستگی‌ها: Compose BOM، Hilt، Room، DataStore، Navigation Compose (type-safe)
- دیزاین‌سیستم v0 مطابق مرجع بصری و بخش ۷.۱: تم **تیره‌اول**، رنگ‌ها، تایپوگرافی، کامپوننت‌های Card/Button/TextField/TopBar — همگی **RTL-aware**
- اسکلت اپ با گراف ناوبری + وایرینگ Hilt؛ یک صفحهٔ خالی per feature
- تنظیم lint/ktlint با baseline تمیز

**معیار پذیرش:** `./gradlew assembleDebug` موفق؛ اپ روی امولاتور/دستگاه بالا می‌آید و تم تیره دیده می‌شود؛ گراف وابستگی ماژول‌ها با بخش ۱۸ مطابقت دارد؛ پیشنهاد `minSdk 24 / target 35` اعمال شده (قابل تغییر با تایید شما).

**ریسک/یادداشت:** سنگین‌ترین بخش این فاز دانلود SDK است؛ اگر محیط محدودیت داشت، بلافاصله اعلام می‌شود.

---

## فاز ۲ — Database: اسکما، DAOها، Schema Freeze

**خروجی‌ها:**
- چهار Entity با FK/CASCADE/Indexes/CHECKها دقیقاً مطابق سند v4 + TypeConverters (`Instant↔Long`، enumها `↔String`)
- `WarriorDatabase` v1 با `exportSchema = true` و commit شدن JSON اسکیما در VCS
- DAOها: `UserDao`، `TrainingSessionDao` (شامل `insertFullSession` تراکنشی با `ActivityWithRounds`)، `WorkoutActivityDao` و `RoundDao` با امضاهای Ownership-dar
- تست‌ها (Robolectric): rollback تراکنش در صورت خطا، CASCADE delete، عدم دسترسی کاربر A به دادهٔ کاربر B، یکتایی `(activityId, roundNumber)`، نقض CHECKها

**معیار پذیرش:** همهٔ تست‌ها سبز؛ فایل اسکیما v1 در ریپو commit شده = **نقطهٔ Freeze**.

---

## فاز ۳ — Domain & Repository: منطق محصول بدون اندروید

**خروجی‌ها:**
- مدل‌های دامنه (`TrainingSession`, `WorkoutActivity`, `Round` با `kotlin.time.Duration`) + Mapperهای entity↔domain
- اینترفیس‌های Repository در `:domain` و پیاده‌سازی Room در `:data:local`
- Use Caseها: CRUD کامل Session/Activity/Round + History + کوئری‌های Ownership-dar
- قوانین Validation در دامنه: intensity 1..10، duration>0، restDuration>=0، roundNumber>0، قوانین username/displayName
- تست‌های Unit سادهٔ JVM برای Use Caseها، Mapperها و Validation

**معیار پذیرش:** تست‌های دامنه بدون هیچ وابستگی اندرویدی سبز می‌شوند (قانون ۸ معماری)؛ هیچ فراخوانی‌ای خارج از `:data` به DAO دسترسی ندارد.

---

## فاز ۴ — Authentication: هویت محلی

**خروجی‌ها:**
- `:core:security`: PBKDF2WithHmacSHA256 با ≥۱۲۰,۰۰۰ تکرار، salt تصادفی ۱۶ بایتی (SecureRandom)، مقایسهٔ constant-time
- Session manager روی DataStore (`currentUserId`, `sessionCreatedAt`)
- Use Caseهای `CreateLocalAccount / Login / Logout`
- صفحه‌های Compose:Register و Login با UI State کامل (loading/error/validation) + ناوبری ریشه که بر اساس Session نقطهٔ ورود را انتخاب می‌کند
- تست‌ها: بردارهای شناخته‌شدهٔ هش، username تکراری، پسورد غلط، بقای Session پس از ری‌استارت

**معیار پذیرش:** جریان Register → Login → بستن اپ → ورود خودکار → Logout کار می‌کند؛ هیچ‌جا پسورد plain ذخیره نمی‌شود (تست + بازبینی).

---

## فاز ۵ — Workout Logging: قلب محصول

**خروجی‌ها:**
- جریان ساخت Session: اطلاعات سربرگ (تاریخ، شدت کلی، Feeling، Notes) → لیست Activityها (type/duration/intensity/focus/notes) → Roundها برای انواع round-based (roundNumber/duration/rest/intensity)
- ViewModelها با UiState/Events مطابق بخش ۷؛ ذخیرهٔ تراکنشی فقط از طریق entry point واحد Repository
- ویرایش و حذف Session با Confirmation Dialog
- پیکرهای UI برای enumها با لیبل فارسی (WorkoutType/FocusArea/Feeling)
- تست‌ها: Unit برای ViewModelها + سناریوی crash-وسط-تراکنش (هیچ Session ناقصی نباید بماند) + تست Compose مسیر خوش‌حال (تحویل build تستی در صورت نیاز)

**معیار پذیرش:** ساخت/ویرایش/حذف کامل یک Session با N فعالیت و M راند به‌صورت آفلاین؛ شکست شبیه‌سازی‌شدهٔ وسط تراکنش = دیتابیس بدون اثر.

---

## فاز ۶ — History: تاریخچه

**خروجی‌ها:**
- لیست Sessionها گروه‌بندی‌شده بر اساس تاریخ (نزولی) با هدرهای روز
- صفحهٔ جزئیات: Session → Activityها → Roundها + همهٔ Notes/Intensityها
- ورودی‌های Edit (به فاز ۵ delegate می‌شود) و Delete با تایید
- Empty State استاندارد
- **تصمیم محصولی این فاز:** تقویم نمایش تاریخ‌ها (پیشنهاد: جلالی برای نمایش، ذخیره‌سازی مطابق سند بدون تغییر)

**معیار پذیرش:** ترتیب و گروه‌بندی لیست با کوئری مطابقت دارد؛ حذف از تاریخچه = حذف آبشاری کامل؛ جزئیات کامل و صحیح.

---

## فاز ۷ — Progress Engine + Home Dashboard

**خروجی‌ها:**
- `WeekBoundaryProvider` با ثابت `WeekStartDay = SATURDAY` (شنبه→جمعه) در یک نقطهٔ مرکزی
- Progress Engine خالص (بدون اندروید): متریک‌های هفتگی، مقایسه با هفتهٔ قبل، توزیع Workout/Focus، Streak، رکوردهای شخصی (طولانی‌ترین Session، بیشترین Round، کل زمان)
- صفحهٔ Home مطابق سبک مرجع: کارت‌های آمار هفته، Recent Sessions، PRها، CTA به ثبت تمرین
- تست‌ها با Clock ثابت: لبه‌های مرز هفته (جمعه/شنبه)، تغییر timezone/DST

**معیار پذیرش:** همهٔ متریک‌ها با fixtureهای دستی محاسبه‌شده برابرند؛ هیچ متریکی در دیتابیس ذخیره نمی‌شود (بخش ۱۳.۴).

---

## فاز ۸ — Charts + Progress Screen

**خروجی‌ها:**
- نمودارهای **سفارشی Compose Canvas** (بدون کتابخانهٔ خارجی، مطابق offline-first): حجم تمرین هفتگی، فراکانس، توزیع Workout/Focus، و **heatmap نقطه‌ای روزهای تمرین** مطابق مرجع بصری
- صفحهٔ Progress با مقایسهٔ هفته‌ها
- تست عملکرد با دادهٔ مصنوعی یک سال (≈۵,۰۰۰ Activity)

**معیار پذیرش:** نمودارها از Flowهای Derived رندر می‌شوند و با دادهٔ حجیم روان می‌مانند.

---

## فاز ۹ — Profile + UI Polish

**خروجی‌ها:**
- صفحهٔ Profile: نمایش/ویرایش displayName و username، Logout، دربارهٔ اپ
- رشته‌های کامل فارسی (resources `fa`) و بازبینی RTL در همهٔ صفحه‌ها
- پولیش: انیمیشن‌ها، Empty/Loading/Error Stateها در همهٔ فیچرها، یکنواختی تایپوگرافی و تم تیره، ترنزیشن‌های ناوبری
- **تصمیم محصولی این فاز:** زبان v1 فقط فارسی یا fa+en (پیشنهاد: فقط فارسی)

**معیار پذیرش:** چک‌لیست UX مقابل مرجع بصری سبز؛ هیچ رشتهٔ hardcode نشده؛ چیدمان RTL بدون المان واژگون.

---

## فاز ۱۰ — Hardening + Release RC

**خروجی‌ها:**
- اجرای کامل سوئیت تست‌ها + زیرساخت MigrationTestHelper (آماده برای Migrationهای آینده، با یک تست نمونه)
- build ریلیز: R8/ProGuard rules، **بدون `fallbackToDestructiveMigration()`** (فقط Debug پشت BuildConfig flag، قانون ۱۱)
- سناریوی smoke: نصب تمیز + اجرای کاملاً آفلاین (حالت هواپیما) + بررسی تک‌تک موارد Definition of Done (بخش ۲۰ معماری)
- APK ریلیز کاندیدیت + گزارش پایانی پروژه

**معیار پذیرش:** RC روی دستگاه/امولاتور تمیز، بدون اینترنت، همهٔ موارد DoD سند معماری را پاس می‌کند.

---

## نقاط تصمیم‌سازی که در طول اجرا به تایید شما می‌رسد

| تصمیم | پیشنهاد من | فاز اعمال |
|---|---|---|
| تقویم نمایش تاریخ | جلالی (ذخیره‌سازی مطابق سند بدون تغییر) | ۶ |
| زبان v1 | فقط فارسی (+ ساختار آماده برای i18n) | ۹ |
| کتابخانهٔ نمودار | Compose Canvas سفارشی (بدون وابستگی خارجی) | ۸ |
| minSdk / targetSdk | ۲۴ / ۳۵ | ۱ |

## گردش کار هر فاز

```text
تایید شما ← پیاده‌سازی + تست‌ها ← commit & push روی main ← گزارش فاز ← بررسی شما ← تایید فاز بعد
```
