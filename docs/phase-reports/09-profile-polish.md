# گزارش فاز ۹ — Profile + UI Polish

> تاریخ: ۲۰۲۶-۰۹-۲۷ | وضعیت: ✅ کامل‌شده و push‌شده روی `main`
> تست‌های جدید: **۱۶، همه سبز** | رگرسیون: همهٔ سوئیت‌های فازهای قبل سبز | ktlint: سبز | APK: ≈۱۱.۱MB از سورس نهایی

---

## ۰. تصمیم محصولی فاز (گیت تایید)
**زبان v1 = انگلیسی فقط — تایید مجدد توسط مالک در همین فاز.**
جدول تصمیم‌های EXECUTION-PLAN (بند «نقاط تصمیم‌سازی») از قبل انگلیسی را ثبت کرده بود و
فازهای ۶–۸ بر همان مبنا shipped شدند؛ بند «پیشنهاد: فقط فارسی» در شرح فاز ۹ با آن در
تضاد بود که با تایید مالک به نفع انگلیسی حل شد. ساختار منابع کاملاً **i18n-ready** است:
افزودن فارسی در آینده = یک پوشهٔ `values-fa/strings.xml` در هر ماژول (بدون تغییر کد).

---

## ۱. خروجی‌ها

### صفحهٔ Profile (مطابق پروتوتایپ تاییدشده)
- کارت هویت: آواتار (حرف اول، دایرهٔ AccentSoft)، displayName و «@username · local account».
- ردیف‌ها: **Edit profile** (دیالوگ ویرایش displayName/username با Save/Cancel)،
  **Export backup** (نمایان ولی غیرفعال با badge «Post-MVP» — مسیر آیندهٔ Export→Import→Sync)،
  **About · v1.0-mvp** (دیالوگ دربارهٔ اپ + نسخه، badge «offline»).
- کارت حریم خصوصی (داده فقط روی دستگاه، PBKDF2 + salt) و دکمهٔ danger **Log Out**.
- زنجیرهٔ ویرایش: `UpdateAccount` (use case جدید، validation مثل ثبت‌نام + نرمال‌سازی
  lowercase) ← `AuthRepository.updateAccount` ← `UserDao.updateProfile`
  (کوئری UPDATE **additive** — Schema v1 Freeze بدون تغییر/بدون Migration).
- یکتایی username روی دستگاه حفظ می‌شود (`DuplicateUsernameException`)؛ password هرگز
  در این جریان لمس نمی‌شود (تست Robolectric: login با password قبلی و username جدید).
- `versionName` از `BuildConfig` اپ از طریق پارامتر به ProfileScreen تزریق می‌شود.

### مهاجرت کامل رشته‌ها به resources (بدون رشتهٔ hardcode در UI)
- `strings.xml` (+`plurals`) در **اپ و هر ۶ فیچر** — ≈۹۰ رشته؛ نام‌گذاری با پیشوند ماژول.
- **کدهای خطای پایدار** به‌جای متن خام domain: `AuthErrorCode` (۸ مقدار) و
  `TrainingErrorCode` (۸ قاعدهٔ validation + ۴ کد سطح جریان: SESSION_NOT_FOUND /
  NOT_SIGNED_IN / SAVE_FAILED / UNEXPECTED). VMها فقط کد منتشر می‌کنند و UI با
  `stringResource` نگاشت می‌کند → ترجمهٔ آینده بدون تغییر domain.
- `DateFormats.dayHeader` پارامتری شد (todayLabel/yesterdayLabel از منابع) + **باگ
  timezone اصلاح شد** (شاخهٔ تاریخ کامل حالا zone را رعایت می‌کند).
- سیاست نمادها: `— ← ✕ − + ·` نماد ریاضی/بصری‌اند نه copy — inline باقی ماندند (مستند).
- انحراف شفاف: لیبل enumها و `rowTitle` همچنان در `Labels.kt` domain است (زبان مشترک
  لایهٔ domain، v1=EN). مسیر مهاجرت fa: جایگزینی با resolver سمت UI — بقیهٔ اپ تمام‌منبعی است.

### پولیش UI
- کامپوننت‌های مشترک جدید در designsystem: `WarriorEmptyState` (title/body/CTA)،
  `WarriorLoadingBox`، `WarriorSectionHeader` — هر سه در History/Home/Progress یکدست شدند.
- **انیمیشن نمودارها**: میله‌های حجم و فیل‌های توزیع با `animateFloatAsState` (۴۵۰ms)
  نرم به مقدار جدید می‌روند.
- **ترنزیشن ناوبری**: fade + slide ملایم و یکسان برای همهٔ مقصدها (enter/exit/pop).
- یکدستی: `rowTitle` تاریخچه به extension مشترک domain مهاجرت کرد (کپی خصوصی حذف)؛
  «Total duration» در Review از `DateFormats.durationLabel` مشترک استفاده می‌کند.
- `typeColor` در ۳ فیچر باقی ماند: گراف §۱۸ اجازهٔ وابستگی core→domain نمی‌دهد؛
  یکدستی کامل نیازمند ماژول shared-ui است که تغییر گراف ماژول‌هاست (نگاهداشت برای پس از MVP).

---

## ۲. تست‌ها

| سوئیت | تعداد | نتیجه |
|---|---|---|
| `domain:auth` — `AuthValidationTest.profileRules` + کدهای requireRegister؛ ۴ تست `UpdateAccount` (ویرایش+نرمال‌سازی+login با نام جدید، تضاد username، case یکسان با خود = تضاد نیست، ورودی نامعتبر/کاربر ناموجود) | 18 (+5) | ✅ |
| `domain:training` — requireValid با کدهای متناظر ۱:۱ با messages | 17 (+1) | ✅ |
| `data:local` (Robolectric) — ۴ تست `updateAccount`: persist + password دست‌نخورده، تضاد، نگه‌داشت username خودش، کاربر ناموجود | 17 (+4) | ✅ |
| `feature:profile` — بارگذاری هویت، prefill+save+refresh، تضاد username (کد + دیالوگ باز)، کدهای validation، logout، about toggle | 6 (جدید) | ✅ |
| `feature:workout` — assertion خطا به کد `NO_ACTIVITIES` مهاجرت کرد | 6 | ✅ |
| رگرسیون: history ۳ (DayGroup بدون label)، home ۴، progress ۴، core:common ۷، domain:progress ۳۵ (perf: home=20ms/screen=62ms) | 53 | ✅ |

**معیارهای پذیرش فاز:** چک‌لیست UX مقابل `ui-preview.html` (بخش ۳) سبز ✅ |
هیچ رشتهٔ hardcode در UI (ممیزی grep: فقط نمادها) ✅ | چیدمان LTR یکدست ✅ (v1 انگلیسی، بدون منطق RTL).

---

## ۳. چک‌لیست UX مقابل پروتوتایپ

| صفحهٔ پروتوتایپ | وضعیت |
|---|---|
| Auth: wordmark، tagline، فیلدها، Create Account / Log In، سوئیچ ghost، note «local-only» | ✅ (فاز ۴ + منابع فاز ۹) |
| Home: THIS WEEK + deltaها، heatmap ۳ ماه، Recent Sessions، Personal Records (streak)، CTA | ✅ (فاز ۷/۸) |
| Log Flow: ۳ مرحله + step indicator، استپرها، چیپ‌ها، راندها، Review، Save/Delete | ✅ (فاز ۵) |
| History: گروه‌بندی روزانه + جزئیات (Activities/Rounds/Notes) + Edit/Delete | ✅ (فاز ۶) |
| Progress: compare card، volume 8 هفته، توزیع workout/focus | ✅ (فاز ۸) |
| Profile: identity card، ردیف‌ها با badge، کارت حریم خصوصی، Log Out danger | ✅ (این فاز) |

---

## ۴. گام بعدی
فاز ۱۰ — Hardening + Release RC: سوئیت کامل تست‌ها + MigrationTestHelper، build ریلیز
با R8 و **بدون** fallbackToDestructiveMigration (فقط Debug پشت flag)، سناریوی smoke
کاملاً آفلاین، و APK ریلیز کاندیدیت + گزارش پایانی. **منتظر تایید مالک.**
