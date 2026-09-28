# گزارش تسک پس از MVP — رفع باگ‌ها + پولیش بصری + لوگوی مینیمال (v1.0.1)

> تاریخ: ۲۰۲۶-۰۹-۲۸ | وضعیت: ✅ کامل و push‌شده روی `main` + تگ/Release `v1.0.1`
> تست‌ها: **۱۱۸/۱۱۸ سبز** (۷۷ domain/core + ۲۳ feature VM + ۱۸ Robolectric) | ktlint: سبز
> APK ریلیز: **۱.۹۲MB** امضاشده (R8 + gate صحت mapping) · دیباگ: ۱۱.۲MB — هر دو از سورس نهایی

---

## ۱. باگ‌های رفع‌شده (گزارش مالک: «متن‌ها جا کم آورده و حروف زیر هم رفته»)

| # | باگ | ریشه | رفع |
|---|---|---|---|
| 1 | **چیپ‌های Feeling/Type/Focus له‌شده** — ۵ و ۴ و ۱۰ چیپ در یک `Row` بدون wrap؛ در صفحات باریک عرض چیپ به صفر می‌رسید و حروف عمودی/زیرهم می‌افتادند | چیدمان فاز ۵ | `FlowRow` با فاصلهٔ افقی+عمودی ۶dp — چیپ‌ها به خط بعد می‌پیچند |
| 2 | **ردیف راندها از صفحه بیرون می‌زد** — دو استپر ۶۴dp + دکمه‌های TextButton (هدف لمسی ۴۸dp) ≈ ۳۷۰dp > عرض موجود | استپرهای بزرگ | `Stepper` فشرده: دکمه‌های دایره‌ای ۳۰dp، مقدار ۵۲dp، ✕ دایره‌ای؛ + `horizontalScroll` به‌عنوان fallback صفحات خیلی کوچک |
| 3 | **«AVG INTENSITY» در کارت آمار کوچک می‌شکست** (سه کارت ~۱۰۰dp با padding ۱۶dp) | padding بزرگ + فونت ۱۰sp | `contentPadding=12dp` (پارامتر جدید `WarriorCard`) + فونت ۹sp + `maxLines=1` + Ellipsis |
| 4 | زیرنویس ردیف‌های History/Home/جزئیات در دادهٔ طولانی می‌پیچید | — | `maxLines=1` + Ellipsis روی عنوان و زیرنویس |
| 5 | «Head Movement» در ستون ۹6dp توزیع Focus بریده می‌شد | عرض ثابت | ستون نام ۱۰۰dp |
| 6 | **لوگو روی لانچرهای مدرن با پس‌زمینهٔ سفید/جعبه‌ای رندر می‌شد** — آیکون legacy بود (بدون adaptive) | manifest → drawable ساده | آیکون **adaptive** کامل: background برند `#0E0E13` + foreground + **monochrome** (پشتیبانی Themed Icons اندروید ۱۳) + fallback برداری برای API 24/25 |

## ۲. ارتقای رابط کاربری (تمیزتر و چشم‌نوازتر)

- **کارت قهرمان THIS WEEK**: گلو/گرادیان ظریف قرمز روی سطح کارت (پارامتر `brush` به
  `WarriorCard` اضافه شد — سایر کارت‌ها بدون تغییر).
- **دکمهٔ اصلی گرادیانی**: `WarriorButton` PRIMARY حالا Accent→قرمز تیره با ripple
  مقید به shape (GHOST/DANGER بدون تغییر).
- **Wordmark دورنگ**: «WAR» سفید + «RIOR» قرمز در Home و صفحهٔ Auth (مطابق پروتوتایپ).
- **DeltaText با جهت**: `▲ 35m` / `▼ 5m` / `±0` — خوانایی مقایسه‌های هفتگی بهتر.
- **هدر بخش‌ها**: تیک قرمز ۳dp + letter-spacing بازتر (`WarriorSectionHeader` مشترک).
- **چیپ انتخاب‌شده**: متن Accent (کنترست واضح‌تر حالت انتخاب).
- **استپرهای دایره‌ای مینیمال** (workout) به‌جای TextButtonهای سنگین.
- `windowBackground` = رنگ برند `#0E0E13` — پرش رنگی هنگام launch حذف شد.

## ۳. لوگوی جدید (مینیمال)

نشانهٔ **«W» دو‌رنگ** (نیمهٔ چپ سفید، نیمهٔ راست قرمز Accent) با stroke ضخیم و
گوشه‌های گرد روی زمینهٔ تیرهٔ برند — هندسی، مقیاس‌پذیر (vector)، بدون جزئیات اضافه.
لایهٔ monochrome هم برای آیکون‌های tematیک اندروید ۱۳+ ساخته شد.

## ۴. نسخه و انتشار
- `versionName 1.0.1` · `versionCode 2` · هر دو APK از سورس نهایی و پس از ktlint ساخته شدند.
- راستی‌آزمایی ریلیز: `apksigner` (CN=WARRIOR RC) · `badging` (v1.0.1/code 2, minSdk 24,
  آیکون برداری) · dex: ۳,۳۵۰ کلاس با `MainActivity`/`WarriorDatabase_Impl` به نام اصلی.
- SHA-256: `70a26da55d33cfbc5c87647473386b598a6a751245a7d776688b671e68fa1bd9`
- GitHub Release `v1.0.1` با فایل `WARRIOR-v1.0.1-release.apk`.
- ⚠️ ارتقا از v1.0.0 روی دستگاه به‌دلیل یکسان بودن کلید امضا بدون حذف نسخهٔ قبل ممکن است؛
  در صورت نصب نسخهٔ debug قدیمی، اول آن را حذف کنید (کلید متفاوت).

## ۵. سخت‌سازی `scripts/build-release.sh` (ناشی از ریست شدن sandbox بین پیام‌ها)
محیط بین پیام‌ها کامل ریست شد (JDK/Gradle/SDK/کش‌ها) و زنجیرهٔ ریلیز سه نقص پنهان داشت
که رفع و **دائمی** شد: مسیر کش Gradle با fallback (`/root/.gradle` در برابر `$HOME`)،
درخواست صریح تسک‌هایی که با exclude شدن R8 از گراف حذف می‌شدند
(`transformReleaseClassesWithAsm`, `extractProguardFiles`, `mergeReleaseGeneratedProguardFiles`)،
و استخراج قوانین consumer مستقیماً از فایل‌های AAR (به‌همراه gate: کمتر از ۵ فایل = fail).

## ۶. تست‌ها (بدون تغییر در منطق — همه سبز پس از پولیش)
domain:auth ۱۸ · domain:training ۱۷ · domain:progress ۳۵ (perf: home=9ms/screen=6ms) ·
core:common ۷ · featureها ۶/۳/۴/۴/۶ · Robolectric ۱۸ (شامل MigrationTest).
تغییرات این تسک فقط لایهٔ presentation بود؛ هیچ VM/UseCase/DAO دست نخورد
(یک استثنا: هیچ — حتی strings).
