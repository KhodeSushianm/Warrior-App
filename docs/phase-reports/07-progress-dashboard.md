# گزارش فاز ۷ — Progress Engine + Home Dashboard

> تاریخ: ۲۰۲۶-۰۹-۲۷ | وضعیت: ✅ کامل‌شده و push‌شده روی `main`
> تست‌های جدید: **۳۲، همه سبز** | رگرسیون: ۲۰ تست (domain:training + core:common) سبز | ktlint: سبز | APK: ساخته‌شده از سورس نهایی (≈۱۱MB)

---

## ۱. خروجی‌ها

### قانون مرکزی هفته (§13.2) — `WeekBoundaryProvider`
- تنها محل تعریف مرز هفته در کل اپ: `WEEK_START_DAY = Calendar.SATURDAY` (شنبه→جمعه)؛
  تغییر به `MONDAY` برای انتشار بین‌المللی یک تغییر تک‌خطی است.
- پیاده‌سازی **Calendar-based و بدون desugaring** (سازگار با minSdk 24 و تصمیم فاز ۶):
  همهٔ محاسبات نیمه‌شبِ *wall-clock* محلی را حفظ می‌کنند، بنابراین هفته همیشه دقیقاً
  ۷ روز محلی است — حتی روی DST (طول epoch هفته می‌تواند ۱۶۷ یا ۱۶۹ ساعت شود، که درست است).
- API: `startOfWeek / startOfNextWeek / startOfPreviousWeek / endOfWeekExclusive`.

### موتور پیشرفت خالص (§13.1/§13.3/§13.4) — بدون اندروید، بدون ذخیره‌سازی
- `ProgressEngine` (توابع خالص): متریک‌های هفتگی (Sessions، Rounds، Training Time،
  Average Intensity با گردکردن half-up، Training Days، Average Session Duration،
  توزیع Workout/Focus بر حسب **دقیقه** با ترتیب پایدار enum)، رکوردهای شخصی و Recent.
- `ProgressCalculator`: یک پاس محاسباتی روی یک لیست خام → snapshot سازگار
  (`thisWeek` + `lastWeek` برای deltaها + Streak + PRها + ۳ Session آخر).
- `ObserveHomeProgress`: جریان زنده از `GetTrainingHistory` scoped به کاربر جاری —
  با هر تغییر Room کل snapshot **مجدداً مشتق** می‌شود؛ هیچ متریکی ذخیره نمی‌شود (§13.4).
- **Streak**: هفته‌های پی‌درپو (شنبه→جمعه) با حداقل یک Session؛ هفتهٔ جاریِ هنوز خالی
  streak را نمی‌شکند (شمارش از هفتهٔ قبل ادامه می‌یابد).
- **PRها**: طولانی‌ترین Session، بیشترین Round در یک Session، کل زمان/تعداد/راندها؛
  تساوی‌ها به **جدیدترین** Session می‌رسند (date desc, createdAt desc).

### صفحهٔ Home واقعی (placeholder فاز ۱ حذف شد)
- `HomeViewModel` با الگوی فاز ۶ (`ObserveSession` + `flatMapLatest`) و snapshot زنده.
- کارت **THIS WEEK · «Sep 19 – Sep 25»**: زمان تمرین بزرگ + delta دقیقه‌ای نسبت به هفتهٔ
  قبل، و سه MiniStat (SESSIONS / ROUNDS / AVG INTENSITY) هرکدام با `DeltaText`.
- **RECENT SESSIONS**: سه ردیف آخر (نقطهٔ رنگی نوع، `rowTitle` مشترک، تاریخ کوتاه · مدت · int) —
  **کلیک‌پذیر به جزئیات History** (ورودی جدید `onOpenSession` در ناوبری).
- **PERSONAL RECORDS**: طولانی‌ترین Session، بیشترین Round، «Current streak — N weeks» مطابق پروتوتایپ.
- Empty State (بدون Session)، حالت Loading، و دو CTA: دکمهٔ «+» در TopBar
  (آیکون جدید `WarriorIconPlus`) و دکمهٔ «+ Log Workout».
- `DateFormats.weekRange`: لیبل «Sep 19 – Sep 25» با فرمت `endExclusive − 1ms`
  تا آخرین روز محلی حتی هفته‌های DST درست بماند.

### تصمیم/انحراف مهم: حذف `java.time.Clock` از DI
`AppModule` از فاز ۴ یک provider برای `java.time.Clock` داشت؛ `java.time` روی API 24/25
بدون desugaring crash می‌کند (risk نهفته). با `TimeProvider` خالص (fun interface در
`domain:progress`) جایگزین شد — هم‌راستا با سیاست «بدون desugaring» فازهای ۵ و ۶.
`WeekBoundaryProvider` و `TimeProvider` در `AppModule` provide می‌شوند؛ `:app` اکنون
به `:domain:progress` وابسته است (composition root).

---

## ۲. تست‌ها

| سوئیت | تعداد | نتیجه |
|---|---|---|
| `WeekBoundaryProviderTest` — لبه‌های جمعه/شنبه (آخرین ms و نیمه‌شب تیز)، تهران (+3:30 بدون DST در برابر UTC)، **DST بهاره/پاییزه Europe/Berlin** (هفتهٔ ۱۶۷ و ۱۶۹ ساعته = ۷ روز محلی) | 9 | ✅ |
| `ProgressEngineTest` — fixture دستی کامل (§13.1: ۳ session/۱۱۵m/۹ rounds/avg 7.67→8/۲ روز/avg session 38m + هر دو توزیع)، گردکردن half-up، مرز start-inclusive/end-exclusive، PRها و تساوی‌ها، recent (ترتیب و cap)، ۵ سناریوی Streak | 13 | ✅ |
| `ProgressCalculatorTest` — snapshot سازگار (frame هفته‌ها، deltaها، streak=2، رکوردهای کل تاریخچه، recent با date desc) + تاریخچهٔ خالی | 2 | ✅ |
| `ObserveHomeProgressTest` — مشتق‌شدن مجدد با هر تغییر repository + scoping کاربر | 2 | ✅ |
| `DateFormatsTest.weekRange` — هفتهٔ UTC، تهران و هفتهٔ DST برلین (جدید) | 2 | ✅ |
| `HomeViewModelTest` — snapshot و لیبل‌ها (TZ=UTC پین‌شده)، جریان زندهٔ session جدید، Empty State، signed-out | 4 | ✅ |

**معیار پذیرش فاز:** همهٔ متریک‌ها با fixtureهای دستی محاسبه‌شده برابرند ✅ |
هیچ متریکی در دیتابیس ذخیره نمی‌شود (§13.4) ✅ | مرز هفته فقط در یک نقطهٔ مرکزی ✅.

رگرسیون: `domain:training` (۱۶) و بقیهٔ `core:common` (۴) سبز — `Labels.kt` فقط
extension جدید `TrainingSession.rowTitle` گرفت (additive).

---

## ۳. یادداشت‌ها
1. **Heatmap «Training Days · Last 3 Months»** در پروتوتایپ Home متعلق به فاز ۸ است
   (خروجی صریح Charts) — عمداً در این فاز اضافه نشد.
2. رندر ردیف session در Home عمداً با History یکی نیست (sub متفاوت: `int X` در برابر
   `felt X` + badge)؛ `rowTitle` مشترک شد و یکدستی کامل رندر در پولیش فاز ۹.
3. بیلد sandbox با همان الگوی زنجیرهٔ تفکیک‌شدهٔ فاز ۱: ماژول‌های Android جداگانه
   compile شدند (metaspace daemon ≤ ۱GiB)، merge dex خارجی با D8 مستقل، و APK نهایی
   **پس از ktlintFormat** از سورس commit‌شده ساخته شد. تست‌ها نیز پس از فرمت نهایی
   در JVM مستقل دوباره اجرا شدند.

---

## ۴. گام بعدی
فاز ۸ — Charts + Progress Screen: نمودارهای سفارشی Compose Canvas (حجم هفتگی،
فراکانس، توزیع Workout/Focus و heatmap نقطه‌ای روزهای تمرین برای Home)، صفحهٔ Progress
با مقایسهٔ هفته‌ها، و تست عملکرد با ≈۵,۰۰۰ Activity مصنوعی. **منتظر تایید مالک.**
