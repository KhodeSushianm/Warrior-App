# گزارش فاز ۱۰ — Hardening + Release RC (گزارش پایانی پروژه)

> تاریخ: ۲۰۲۶-۰۹-۲۷ | وضعیت: ✅ کامل‌شده و push‌شده روی `main` + تگ `v1.0.0`
> سوئیت کامل: **۸۴ تست سبز** (۶۶ unit در JVM مستقل + ۱۸ Robolectric) | ktlint: سبز
> خروجی: **`app-release.apk` امضاشده — ۱.۹۰MB** (R8: shrink + obfuscate کامل، از ۱۱MB دیباگ)
> GitHub Release: تگ `v1.0.0` + فایل APK برای دانلود مستقیم

---

## ۱. خروجی‌ها

### قانون ۱۱ — بدون migration مخرب در Release (سیم‌کشی نهایی)
- flag `ALLOW_DESTRUCTIVE_MIGRATION` در **BuildConfig خود `:data:local`** تعریف شد
  (debug=true / release=false) و `DatabaseModule` فقط در debug آن را wire می‌کند؛
  release روی migration نیافتاده **با صدای بلند fail می‌شود** نه پاک‌کردن داده.
- field مردهٔ BuildConfig در `:app` (بازماندهٔ فاز ۲) حذف شد.

### زیرساخت MigrationTestHelper (آماده برای migrationهای آینده)
- `room-testing` + schema JSONهای frozen به‌عنوان **test assets** (`sourceSets.test.assets`).
- `MigrationTest`: دیتابیس را دقیقاً از **اسکیمای v1 frozen** می‌سازد (۴ جدول + seed خام)،
  سپس با Room production باز می‌کند و سلامت داده را می‌سنجد — الگوی هر migration آینده.
- رفع نقص زنجیرهٔ تست sandbox در `test-lowram.sh`: تسک‌های
  `generateDebugUnitTestConfig`/`processDebugUnitTestJavaRes` + **پچ
  `test_config.properties`** (AGP مسیر `android_merged_assets` را به assets واریانت اصلی
  اشاره می‌دهد؛ نسخهٔ پچ‌شده به `mergeDebugUnitTestAssets` اشاره می‌کند — بدون آن،
  MigrationTestHelper فایل schema را پیدا نمی‌کند).

### بیلد Release — R8 واقعی در sandbox یک‌گیگابایتی (`scripts/build-release.sh`)
خودِ `minifyReleaseWithR8` داخل daemon ممکن نبود (R8 به ~۶۰۰-۷۰۰MB heap نیاز دارد؛
در ۵۱۲m هیپ OOM و در ۶۴۰m+ کل daemon توسط کرنل kill می‌شود — با dmesg تایید شد).
راه‌حل مهندسی (مستند و اسکریپت‌شده، هم‌خانوادهٔ الگوی D8 فاز ۱):
1. Gradle همهٔ ورودی‌های R8 را می‌سازد (بدون R8/lint/package)؛
2. classpath زمان اجرا با ArtifactView (`android-classes-jar`) دامپ می‌شود (۱۰۷ jar)؛
3. کلاس‌های app جار می‌شوند و **قوانین consumer همهٔ AARها** از transforms cache
   جمع می‌شود (۵۹ فایل) — نکتهٔ حیاتی: بدون آن‌ها R8 کلاس‌های reflection-made
   مثل `WarriorDatabase_Impl` را بی‌صدا حذف می‌کند (در همین بیلد کشف و اصلاح شد)؛
4. R8 مستقل (`builder-8.7.3.jar` داخلی AGP، هیپ ۷۰۰MB، بدون daemon زنده)؛
5. اسکریپت **gate صحت** دارد: mapping باید `WarriorDatabase_Impl` را بدون تغییر نام
   نگه داشته باشد، وگرنه بیلد fail می‌شود؛
6. Gradle بسته‌بندی + امضا می‌کند.
- `proguard-rules.pro` واقعی (attributes تشخیص کرش) + قوانین پیش‌فرض optimize +
  aapt + generated. `mapping.txt` (۲۸.۶MB) برای دی‌ابفاسکیشن کرش‌ها نگهداری می‌شود.
- انحراف شفاف: `shrinkResources=false` (sیم‌کشی داخلی `-printresources` AGP در جریان
  R8 مستقل قابل بازتولید نیست)؛ **کد** shrink + obfuscate کامل است. `lintVitalRelease`
  هم در همین زنجیره exclude شد ولی **جداگانه اجرا و سبز شد** (پیش از R8).

### امضا و نسخه
- keystore خودامضای RC تولید و (عمداً) commit شد تا بیلد **تکرارپذیر** باشد —
  با note امنیتی صریح در `keystore.properties`: این کلید RC/sideload است،
  **نه** کلید انتشار فروشگاه؛ پیش از انتشار عمومی جایگزین و امن نگهداری شود.
- `versionName = 1.0.0` (versionCode 1)؛ صفحهٔ About همان را نمایش می‌دهد.

### راستی‌آزمایی APK ریلیز (استاتیک — هر چه در sandbox ممکن است)
| بررسی | نتیجه |
|---|---|
| `apksigner verify` | ✅ CN=WARRIOR RC (SHA-256 `cad958…`) |
| `aapt2 badging` | ✅ `com.warrior.app` · v1.0.0 (code 1) · label «WARRIOR» · launchable=MainActivity · minSdk 24 |
| dex | ✅ ۳,۳۱۰ کلاس؛ `MainActivity`/`WarriorApplication`/`WarriorDatabase_Impl` با نام اصلی (manifest/Room reflection)؛ بقیه obfuscate شده |
| mapping | ✅ Hilt componentها، Compose runtime، Room و ViewModelها همه حاضر |

### اجرای کامل سوئیت تست‌ها (۸۴ تست، همه سبز — پس از تغییرات نهایی)
| لایه | سوئیت‌ها | تعداد |
|---|---|---|
| domain:auth | Validation (+codes) + UseCases (+UpdateAccount) | 18 |
| domain:training | Validation (+codes) + SessionUseCases | 17 |
| domain:progress | WeekBoundary(9) + Engine(13) + Calculator(2) + Flowها(2) + Charts(8) + **Performance**(1: یک سال داده، ۵,۰۰۰ activity، home=8-20ms/screen=7-62ms) | 35 |
| core:common | DateFormats(5) + TimeUtils(2) | 7 |
| data:local (Robolectric) | Database(7) + Mapper + Auth(8) + **Migration(1)** | 18 |
| feature:workout / history / home / progress / profile | VMها | 6/3/4/4/6 |

### DoD بخش ۲۰ معماری — وضعیت هر بند
| بند DoD | پوشش خودکار | smoke دستگاهی (چک‌لیست مالک) |
|---|---|---|
| اجرای بدون اینترنت | معماری local-first (هیچ شبکه‌ای در کد نیست؛ grep=۰ شبکه) | ☐ حالت هواپیما: ساخت account و لاگین |
| Account محلی + Login | AuthUseCasesTest + AuthRepositoryTest (PBKDF2/۱۲۰k) | ☐ ثبت + ورود + خروج |
| Session با چند Activity/Round | SessionUseCasesTest + WorkoutLoggingViewModelTest + WarriorDatabaseTest (تراکنش/rollback) | ☐ ساخت session با ۲ activity و round |
| Intensity/Focus/Notes | تست‌های validation + VM | ☐ ثبت و مشاهده در جزئیات |
| Save/Edit/Delete | تست‌های VM + cascade در DB تست | ☐ ویرایش و حذف از History |
| آمار هفته/Progress/Chartها/PRها | ۳۵ تست domain:progress + VM تست‌ها + fixture دستی | ☐ مقایسهٔ اعداد Home/Progress با انتظار |
| Logout | ProfileViewModelTest + AuthUseCasesTest | ☐ Logout → صفحهٔ Auth؛ داده با login مجدد برگردد |

> **چک‌لیست smoke مالک (RC روی دستگاه تمیز):** نصب `app-release.apk` → حالت هواپیما →
> Register → ساخت Session (Heavy Bag ۳ راند + Cardio) → مشاهدهٔ Home (هفته/heatmap/PR) →
> History (جزئیات/Edit/Delete) → Progress (مقایسه/نمودار/توزیع‌ها) → Profile (ویرایش نام،
> About، Logout → Login مجدد با نام کاربری ویرایش‌شده) → بستن/بازکردن اپ (داده باقی بماند).

---

## ۲. انحراف‌ها / یادداشت‌ها
1. **بدون امولاتور**: smoke دستگاهی مانند فازهای قبل ممکن نبود؛ جایگزین = راستی‌آزمایی
   استاتیک APK + ۸۴ تست خودکار + چک‌لیست بالا برای مالک.
2. `shrinkResources=false` (فقط کد shrink می‌شود) — دلیل فنی در بالا؛ با CI پرحافظه
   در آینده یک‌خطی قابل فعال‌سازی است.
3. keystore RC در ریپو است (تصمیم آگاهانه برای تکرارپذیری sideload) — پیش از Play
   حتماً کلید جدا بسازید.
4. `mapping.txt` در artifacts بیلد نگه داشته می‌شود (crash reporting آینده).

---

## ۳. جمع‌بندی پروژه (فازهای ۱–۱۰)
MVP کامل شد: Foundation ماژولار (۱۴ ماژول) ← Schema Freeze (Room v1) ← Domain/UseCase ←
Auth محلی (PBKDF2) ← Workout Logging سه‌مرحله‌ای ← History زنده ← Progress Engine خالص
(هفتهٔ شنبه→جمعه، Streak، PR) ← نمودارهای Canvas سفارشی ← Profile + i18n-ready کامل ←
Hardening + Release RC امضاشده. حلقهٔ محصول `LOG → TRACK → ANALYZE → IMPROVE` به‌صورت
کاملاً آفلاین برقرار است. مسیر آینده طبق معماری: `Export → Import → Cloud Sync`.

**Release v1.0.0 (RC1) در بخش Releases ریپو با APK آمادهٔ نصب منتشر شد.**
