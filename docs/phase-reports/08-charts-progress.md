# گزارش فاز ۸ — Charts + Progress Screen

> تاریخ: ۲۰۲۶-۰۹-۲۷ | وضعیت: ✅ کامل‌شده و push‌شده روی `main`
> تست‌های جدید: **۱۴، همه سبز** | رگرسیون: ۵۲ تست سبز | ktlint: سبز | APK: ≈۱۱.۱MB از سورس نهایی
> کارایی: مشتق‌شدن snapshot کامل (Home و Progress) با **۱,۰۰۰ Session / ۵,۰۰۰ Activity**: **۸ms و ۷ms** روی JVM ضعیف sandbox

---

## ۱. خروجی‌ها

### نمودارهای سفارشی Compose Canvas (`core:designsystem/components/Charts.kt`) — بدون کتابخانهٔ خارجی
- `VolumeBarChart`: میله‌های گرد (شعاع ۸dp بالا / ۴dp پایین، Path-based دقیقاً مثل مرجع)،
  هفتهٔ جاری Accent و بقیه SurfaceVariant؛ لیبل‌های زیر نمودار با همان هندسهٔ میله‌ها
  (weight + gap یکسان) همیشه تراز می‌مانند.
- `TrainingHeatmapGrid`: heatmap نقطه‌ای — N ماه کنار هم، هر ماه یک گرید ۷ ستونه که
  ستون‌هایش **شنبه→جمعه** است (همان قانون مرکزی §13.2)؛ نقطه‌ها: Outline (بدون تمرین)،
  TextPrimary (تمرین‌شده)، Accent (امروز). لیبل ماه با `TextMeasurer` داخل همان Canvas.
- `DistributionBarRow`: ردیف توزیع مرجع (نام ۹6dp + ترک قرصی SurfaceVariant + فیل
  متناسب + مقدار راست‌چین ExtraBold).
- همه domain-agnostic هستند (دادهٔ presentation می‌گیرند)؛ نگاشت مدل‌ها در feature انجام می‌شود.

### موتور پیشرفت (ادامهٔ فاز ۷، همچنان خالص و بدون ذخیره‌سازی §13.4)
- `ProgressEngine.volumeSeries`: ۸ هفته منتهی به هفتهٔ جاری (oldest-first) — bucketing با
  خودِ `WeekBoundaryProvider.startOfWeek` (بدون هیچ ریاضیِ مرز در جای دیگر).
- `ProgressEngine.workoutDistribution` (کل تاریخچه، ترتیب enum، صفرها حذف) و
  `topFocusAreas` (نزولی، tiebreak با نام enum، cap=۴).
- `ProgressEngine.monthsHeatmap`: ۳ ماه تقویمی منتهی به ماه جاری — offset ستونِ روز اول،
  تعداد روزها (**Leap-year aware**)، روزهای تمرین‌شده (bucket per month در یک پاس)،
  todayDay فقط برای ماه جاری؛ timezone-آگاه (تست تهران/UTC).
- `ProgressSnapshot` + `ProgressCalculator.progressScreen` + `ObserveProgress`
  (جریان زندهٔ scoped به کاربر، مثل فاز ۷).
- `HomeProgress` با `heatmap` گسترش یافت (پیش‌فرض خالی — سازگار با تست‌های قبلی).

### صفحهٔ Progress (placeholder حذف شد) — مطابق مرجع تاییدشده
- `ProgressViewModel`: نرمال‌سازی کسری حجم‌ها نسبت به max هفته (fractions در VM محاسبه و
  تست می‌شود، نه در Composable).
- کارت مقایسهٔ هفته: «SEP 19 – SEP 25 vs PREVIOUS WEEK» با ۵ ردیف
  (Sessions / Training time / Rounds / Avg intensity / **Training days** — پوشش متریک
  «فراکانس» در plan) هرکدام با `DeltaText`.
- «TRAINING VOLUME · LAST 8 WEEKS» با لیبل‌های W-7…W-1/NOW.
- «WORKOUT DISTRIBUTION» درصدی از کل (رنگ هر نوع) و «FOCUS DISTRIBUTION» top-4 با
  `durationLabel` و مقیاس نسبت به بیشترین — هر دو **کل تاریخچه** (مطابق پروتوتایپ).
- Empty State (بدون داده: فقط کارت مقایسه + پیام راهنما) و حالت Loading.

### صفحهٔ Home — کارت heatmap
«TRAINING DAYS · LAST 3 MONTHS» بین کارت هفته و Recent Sessions (دقیقاً جایگاه مرجع)،
با لیبل ماه از `DateFormats.monthLabel` جدید («Sep»).

---

## ۲. تست‌ها

| سوئیت | تعداد | نتیجه |
|---|---|---|
| `ProgressChartsTest` — پنجرهٔ دقیق ۸ هفته‌ای (خارج از پنجره/هفتهٔ بعد حذف)، ترتیب oldest-first، trainingDays، توزیع کل (ترتیب enum + حذف صفر)، top-focus با tiebreak، heatmap: offsetهای دستی (Jul=4/Aug=0/Sep=3)، **سال کبیسه ۲۰۲۸ و عبور از سال نو**، timezone تهران↔UTC، snapshot سازگار `progressScreen` | 8 | ✅ |
| `ProgressEnginePerformanceTest` — fixture یک سال (۱,۰۰۰ Session × ۵ Activity = ۵,۰۰۰): بودجهٔ <۱,۵۰۰ms (عملی: ۸/۷ms) + ۴ invariant صحت (جمع توزیع = کل دقیقه‌ها، جمع سری = پنجره، frame یکسان Home/Progress، heatmap ۳ ماهه) | 1 | ✅ |
| `DateFormatsTest.monthLabel` | 1 | ✅ |
| `ProgressViewModelTest` — کسرهای دستی (۴۵/۹۰=۰.۵، NOW=۱.۰)، تغییر زندهٔ max و reshape کسرها، تاریخچهٔ خالی (همه صفر)، signed-out | 4 | ✅ |

**معیار پذیرش فاز:** نمودارها فقط از Flowهای Derived رندر می‌شوند (VM تست‌ها جریان زنده را
اثبات می‌کنند) ✅ | با دادهٔ حجیم روان می‌مانند (۸ms << بودجهٔ فریم) ✅.

رگرسیون: domain:progress فاز ۷ (۲۶)، core:common (۶)، domain:training (۱۶)،
feature:home (۴) — همه سبز؛ `HomeProgress.heatmap` با مقدار پیش‌فرض افزودنی است.

---

## ۳. یادداشت‌ها / انحراف‌ها
1. **تست رندر Compose** (روانی بصری Canvas روی دستگاه) در sandbox ممکن نیست (بدون
   امولاتور) — همان سیاست فاز ۵؛ منطق/دادهٔ نمودارها کاملاً تست‌شده و راستی‌آزمایی
   دستگاهی در فاز ۱۰ (smoke RC) انجام می‌شود.
2. `typeColor` در سه feature تکرار شده (History/Home/Progress) — یکدستی در پولیش فاز ۹.
3. توزیع‌ها «کل تاریخچه» هستند (مطابق پروتوتایپ تاییدشده)؛ اگر پنجرهٔ زمانی خواسته
   شوید، موتور آماده است (همان توابع با فیلتر تاریخ).
4. بیلد sandbox با زنجیرهٔ تفکیک‌شده: پس از ktlintFormat همهٔ ماژول‌ها جداگانه recompile
   شدند، APK از سورس فرمت‌شدهٔ نهایی ساخته شد و تست‌ها پس از آن دوباره اجرا شدند.

---

## ۴. گام بعدی
فاز ۹ — Profile + UI Polish: صفحهٔ Profile (ویرایش displayName/username، Logout، دربارهٔ
اپ)، مهاجرت کامل رشته‌ها به resources انگلیسی (i18n-ready)، انیمیشن‌ها و
Empty/Loading/Error یکدست، و تصمیم زبان v1. **منتظر تایید مالک.**
