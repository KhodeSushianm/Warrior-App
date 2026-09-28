# گزارش فاز ۱۳ — فارسی + RTL + تقویم شمسی (فصل ۲)

> تاریخ: ۲۰۲۶-۰۹-۲۸ | وضعیت: ✅ کامل و push‌شده روی `main`
> تست‌ها: **۱۴۳ سبز** (۸۸ JVM + ۳۲ feature + ۲۳ Robolectric) | ktlint: سبز
> APK: release امضاشده ۲.۰۲MB (R8 + gate) · debug ۱۱.۳MB — هر دو از سورس نهایی
> SHA-256 release: `25943b5aa60a28d5b302fde4bbcc52a562583db44230551e16a7c6f613da6a9f`

---

## ۱. خروجی‌ها

### موتور تقویم جلالی (`core:common/time/JalaliCalendar.kt`) — خالص، بدون کتابخانه
- الگوریتم **jalaali «breaks»** (نه قاعدهٔ تقریبی ۳۳‌ساله): با الگوریتم مرجع
  jalaali-js روی Node **تک‌به‌تک راستی‌آزمایی شد** — لنگرهای اعتدالین (نوروز
  ۱۴۰۳/۱۴۰۴/۱۴۰۵)، سال کبیسهٔ ۱۳۹۹ (اسفند ۳۰) و round-trip ده سال کامل روزها.
  دو باگ حین توسعه با همان مرجع شکار شد (فرمول leapG و یک لنگر دستی غلط).
- نام ماه‌ها، نام/مخفف یک‌حرفی روزهای هفته و **تبدیل ارقام فارسی** (۰-۹ → ۰-۹).

### `DateFormats` دو‌تقویمی/دو‌زبانه (فقط نمایش — ذخیره‌سازی UTC بدون تغییر)
- `dayHeader` / `short` / `weekRange` / `monthLabel` همه پارامترهای
  `calendar: DisplayCalendar` و `locale` گرفتند؛ مسیر انگلیسی/میلادی **بایت‌به‌بایت
  بدون تغییر** (تست رگرسیون صریح). نمونهٔ شمسی-فارسی:
  «یکشنبه ۲۹ شهریور ۱۴۰۵» · «پ ۲ مهر» · «۴ مهر – ۱۰ مهر».

### Heatmap شمسی (domain)
- `ProgressEngine.monthsHeatmap(..., jalali = true)`: bucketing بر اساس ماه‌های
  جلالی با همان **ستون‌های شنبه→جمعه** (قانون هفته مستقل از تقویم است)؛ offset
  روز اول از تبدیل معکوس جلالی→میلادی محاسبه می‌شود. تست با fixture دستی
  راستی‌آزمایی‌شده با مرجع (مرداد/شهریور/مهر ۱۴۰۵ + اسفند کبیسه/غیرکبیسه + عبور از سال).
- زنجیرهٔ تزریق: `ObserveHomeProgress(userId, jalali)` ← VM ← تنظیمات.

### تنظیمات نمایش (DataStore، معماری §14.2)
- `AppPreferences` (مرز دامنه در domain:auth): زبان ∈ {system, en, fa} و
  تقویم ∈ {gregorian, jalali} با Flow زنده؛ پیاده‌سازی `DataStoreAppPreferences`
  (فایل settings جدا از session).
- **سوییچ زبان بدون appcompat**: `AppLocaleHolder` (کش همزمان برای
  `attachBaseContext`) + `Configuration.setLocale/setLayoutDirection` — کل درخت
  Compose برای fa خودکار **RTL** می‌شود؛ تغییر زبان → `Activity.recreate()`.
- `WarriorApplication` کش را از DataStore شارژ می‌کند.

### UI
- Profile: ردیف‌های **زبان** و **تقویم** با دیالوگ انتخاب (badge = مقدار فعلی).
- **heatmap در RTL آینه می‌شود**: ترتیب ماه‌ها راست→چپ و ستون شنبه سمت راست
  (LayoutDirection در `TrainingHeatmapGrid`).
- همهٔ برچسب‌های تاریخ (هدر روز History، جزئیات، Recentها، THIS WEEK،
  مقایسهٔ Progress، ماه‌های heatmap) از تقویم/زبان انتخابی پیروی می‌کنند؛
  `weekRangeLabel` از VMها به UI منتقل شد (locale-aware).
- **`values-fa` کامل برای هر ۷ ماژول** (~۱۵۰ رشته + plurals فارسی) — هیچ رشتهٔ
  hardcode جدیدی وارد نشد؛ تصمیم: لیبل enumهای دامنه (Cardio/…) در v1.1 هم
  انگلیسی می‌مانند (زبان مشترک domain — ثبت در گزارش فاز ۹)، بقیهٔ UI کاملاً ترجمه شد.

---

## ۲. تست‌ها

| سوئیت | تعداد | نکته |
|---|---|---|
| `JalaliCalendarTest` (جدید) | 5 | لنگرهای اعتدالین، کبیسه‌ها، round-trip ۱۰ سال (>۳۶۰۰ روز)، ارقام فارسی |
| `DateFormatsTest` (+1) | 6 | dayHeader/weekRange/short/monthLabel جلالی-فارسی + رگرسیون مسیر انگلیسی |
| `JalaliHeatmapTest` (جدید) | 2 | offset/trainedDays/todayDay ماه‌های فارسی، اسفند ۲۹/۳۰، عبور از سال |
| VMهای home/progress/history/profile | 26 | با `FakeAppPreferences`؛ برچسب‌های weekRange به UI منتقل شدند |
| بقیهٔ سوئیت‌ها | 110 | بدون تغییر سبز (workout ۶، auth ۱۸، training ۱۷، progress ۴۰، Robolectric ۲۳، …) |

معیار پذیرش: RTL یکدست (LayoutDirection خودکار + آینه‌سازی heatmap) ✅ ·
ساختار i18n کامل (افزودن زبان سوم = یک پوشهٔ values) ✅ · ذخیره‌سازی بدون تغییر ✅.

---

## ۳. یادداشت‌ها / انحراف‌ها
1. کش زبان در `AppLocaleHolder` همزمان است و در cold-start پیش از رسیدن Flow
   DataStore مقدار «system» دارد — در عمل اولین collect پیش از اولین
   `attachBaseContext` فعالیت انجام می‌شود؛ لبهٔ نادر یک‌بار recreate خودکار است.
2. اعداد آمار (sessions/rounds/…) عمداً با ارقام لاتین نمایش داده می‌شوند
   (خوانایی در نمودارها)؛ فقط تاریخ‌ها/برچسب‌ها فارسی‌سازی رقم دارند.
3. `Locale("fa")` با SimpleDateFormat برای مسیر «فارسی + میلادی» نام ماه‌های
   میلادی را فارسی می‌دهد (سپتامبر) — بدون کد اضافه.

## ۴. گام بعدی
فاز ۱۴ — تایمر زندهٔ راند (Workout Mode): تایمر کار/استراحت، بوق و ویبره،
پیشروی خودکار و ثبت خودکار Session از تایمرها.
