# گزارش فاز ۱ — Foundation

> تاریخ: ۲۰۲۶-۰۹-۲۶ | وضعیت: ✅ کامل‌شده و push‌شده روی `main`
> معیار پذیرش مرجع: `EXECUTION-PLAN.md` فاز ۱

---

## ۱. چه چیزی ساخته شد

### ماژول‌ها (مطابق بخش ۱۸ سند معماری)
```text
:app                         ← MainActivity، NavHost type-safe، BottomBar، Hilt root
:feature:auth|home|workout|history|progress|profile
:domain:auth|training|progress   (Kotlin JVM خالص)
:data:local                  (Room + DataStore + Hilt — کد در فاز ۲)
:core:common | :core:designsystem | :core:security
```

### پشتهٔ فنی (single-source در `gradle/libs.versions.toml`)
AGP 8.7.3 · Kotlin 2.0.21 · KSP 2.0.21-1.0.28 · Compose BOM 2024.12.01 · Hilt 2.53.1 ·
Navigation Compose 2.8.5 (type-safe routes با `@Serializable`) · Room 2.6.1 · DataStore 1.1.1 ·
minSdk 24 / target & compile 35 · Java 17

### دیزاین‌سیستم v0 (`:core:designsystem`) — مطابق `ui-preview.html`
- `WarriorTheme`: تم تیرهٔ تک‌اسکیم (dark-first)، پالت دقیقاً هم‌رنگ پروتوتایپ
  (Background #0E0E13، Surface #16161D، Accent #FF4D4D، رنگ‌های نوع تمرین)
- `WarriorTypography`: اعداد درشت ExtraBold برای متریک‌ها
- کامپوننت‌ها: `WarriorCard`, `WarriorButton` (PRIMARY/GHOST/DANGER), `WarriorTextField`,
  `WarriorTopBar`, `WarriorChip`, `WarriorBadge`, `DeltaText`
- آیکون‌های سفارشی BottomBar: `WarriorIconHome/History/Progress/Profile` (بدون وابستگی extended-icons)

### اسکلت اپ
- `WarriorApplication` (@HiltAndroidApp) + `MainActivity` (@AndroidEntryPoint)
- ناوبری type-safe: Auth → Home/History/Progress/Profile (با bottom bar) + WorkoutRoute
  (از CTA خانه؛ bottom bar در Auth/Workout مخفی)
- DI نمونه: `AppModule.provideClock()` — پایهٔ تست‌پذیری Progress Engine فاز ۷
- قانون ۱۱ معماری از حالا در build typeها: `ALLOW_DESTRUCTIVE_MIGRATION` فقط در debug=true

### کیفیت
- ktlint 12.1.1 روی **هر ۱۴ ماژول** apply و **سبز** (با `.editorconfig` و استثنای نام‌گذاری `@Composable`)
- `scripts/setup-toolchain.sh`: بوت‌استرپ idempotent ابزارها (JDK/Gradle/SDK)
- `scripts/build-debug.sh`: بیلد قابل‌تکرار debug (شامل workaround مستند dex — بخش ۳)

---

## ۲. نتیجه معیار پذیرش (DoD)

| معیار | نتیجه |
|---|---|
| `assembleDebug` موفق | ✅ `app-debug.apk` ≈ 10MB ساخته شد (BUILD SUCCESSFUL) |
| اجرای اپ با تم تیره | ⚠️ در این sandbox امکان ندارد (بدون KVM/امولاتور) — APK برای نصب روی دستگاه شما آماده است |
| تطابق گراف ماژول‌ها با بخش ۱۸ | ✅ دقیقاً همان ۱۴ ماژول |
| minSdk 24 / target 35 | ✅ |
| lint/ktlint baseline تمیز | ✅ ktlintCheck سبز روی همهٔ ماژولها (بدون نیاز به baseline) |

---

## ۳. محدودیت سخت محیط و راه‌حل مهندسی (مهم برای فازهای بعد)

سندباکس فقط **۱GiB RAM** (cgroup) و بدون swap دارد. بیلد کامل AGP در این سقف ممکن نبود:
merge کردن dex کتابخانه‌های خارجی (`mergeExtDexDebug`) با D8 داخل daemon همیشه OOM می‌شد.

**راه‌حل (در `scripts/build-debug.sh` مستند و خودکار شده):**
1. لیست ورودی‌های `mergeExtDexDebug` با یک init script از خود Gradle dump می‌شود؛
2. merge با یک فرآیند **مستقل D8** (کل RAM آزاد، بدون daemon) انجام می‌شود؛
3. Gradle با `-x :app:mergeExtDexDebug` ادامه می‌دهد و بسته‌بندی می‌کند.
خروجی نهایی APK **کاملاً معادل** بیلد عادی است (همان ورودی‌ها، همان d8).

سایر تنظیمات پایداری: heap کوچک Gradle + `kotlin.compiler.execution.strategy=in-process` +
بیلد مرحله‌به‌مرحلهٔ ماژول‌ها در محیط‌های کم‌حافظه.

---

## ۴. انحراف‌ها از برنامهٔ فاز (شفاف)

1. **تست اجرا روی امولاتور**: ممکن نبود (بدون KVM). تست‌های Robolectric فاز ۲ همین خلأ را برای لایهٔ داده پر می‌کنند؛ تست دستگاه برای milestoneهای بعد با build تستی تحویل شما می‌شود.
2. **رشته‌های UI placeholderها hardcode انگلیسی‌اند**؛ مهاجرت به resources در فاز ۹ (طبق DoD همان فاز).
3. ktlint به‌جای apply تکی در هر ماژول، از طریق `subprojects {}` در root اعمال شد (ساده‌تر و یکسان).

---

## ۵. بازسازی محیط (برای هر ماشین/فاز بعد)

```bash
scripts/setup-toolchain.sh   # JDK17 + Gradle 8.11.1 + SDK 35 (idempotent)
scripts/build-debug.sh       # APK debug
```

## ۶. گام بعدی
فاز ۲ — Database: Entityها، DAOها، `insertFullSession` تراکنشی، `exportSchema=true` و تست‌های Robolectric → نقطهٔ Schema Freeze. **منتظر تایید مالک.**
