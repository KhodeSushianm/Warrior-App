# گزارش فاز ۶ — History

> تاریخ: ۲۰۶-۰۹-۲۶ | وضعیت: ✅ کامل‌شده و push‌شده روی `main` (کامیت `3de327a`)
> تست‌های جدید: **۵، همه سبز** | ktlint: سبز | APK: بازسازی‌شده از سورس نهایی

---

## ۱. خروجی‌ها

### تصمیم محصولی فاز (ثبت‌شده)
**تقویم نمایش: میلادی (Gregorian) با لیبل انگلیسی** — هم‌راستا با زبان v1 که مالک تایید کرد.
ذخیره‌سازی بدون تغییر: UTC epoch millisِ نیمه‌شب روز محلی (DB v4 §9). پیاده‌سازی در
`core:common/time/DateFormats.kt` با `SimpleDateFormat(Locale.ENGLISH)` و `Calendar`
(بدون desugaring): هدر روزها «Today / Yesterday / Fri, Sep 25, 2026».

### History list (`feature:history`)
- `HistoryViewModel`: مشاهدهٔ زندهٔ تاریخچه با `GetTrainingHistory` scoped به کاربر جاری
  (`ObserveSession` + `flatMapLatest`)؛ گروه‌بندی بر اساس `date` **نزولی** و داخل هر روز
  بر اساس `createdAt` نزولی؛ Empty State با CTA به ثبت تمرین
- ردیف‌ها: نقطهٔ رنگی نوع تمرین، عنوان (نوع + تعداد راند + «+N more»)، زیرنویس
  (تاریخ کوتاه · مدت · feeling) و badge شدت — مطابق زبان بصری پروتوتایپ
- حذف با Confirmation Dialog از همان لیست (cascade کامل در DB)

### History detail
- `HistoryDetailRoute(sessionId)` type-safe؛ `HistoryDetailViewModel` جزئیات را **زنده** از
  flow تاریخچه مشتق می‌کند (پس از Edit/Save در فاز ۵، برگشت به جزئیات خودبهخود تازه است)
- کارت سربرگ: هدر روز، مدت کل (Derived)، تعداد راند، شدت کلی، feeling، یادداشت
- کارت هر Activity: نوع، Focus، مدت، شدت، یادداشت و لیست Roundها با work/rest
- دکمه‌های **Edit** (به `WorkoutRoute(sessionId)`) و **Delete** (با تایید)

### ناوبری
`WarriorApp`: ردیف History → جزئیات؛ جزئیات → Edit؛ bottom bar در جزئیات مخفی می‌ماند.

---

## ۲. تست‌ها

| سوئیت | تعداد | نتیجه |
|---|---|---|
| `HistoryViewModelTest` (گروه‌بندی/ترتیب/حذف) | 2 | ✅ |
| تست `HistoryDetailViewModel` (بروزرسانی زنده + حذف) | 1 | ✅ |
| `DateFormatsTest` (Today/Yesterday/الگو + durationLabel) | 2 | ✅ |

DoD: ترتیب/گروه‌بندی لیست = کوئری `ORDER BY date DESC` ✅ | حذف از History = cascade کامل ✅ |
جزئیات کامل و صحیح (همهٔ Notes/Intensity/Rounds) ✅.

---

## ۳. یادداشت‌ها
1. گروه‌بندی فعلی بر اساس **روز** است (هدر Today/Yesterday/تاریخ کامل)؛ هدرهای هفتگی
   شنبه–جمعه با ورود Progress Engine (فاز ۷) برای کارت‌های مقایسه‌ای استفاده می‌شوند.
2. بیلد sandbox با الگوی زنجیرهٔ تفکیک‌شده؛ APK نهایی پس از ktlintFormat بازسازی شد تا
   باینری دقیقاً مطابق سورس commit‌شده باشد.

---

## ۴. گام بعدی
فاز ۷ — Progress Engine + Home Dashboard: `WeekBoundaryProvider` با `WeekStartDay = SATURDAY`،
متریک‌های هفتگی + مقایسه با هفتهٔ قبل، توزیع‌ها، Streak و PRها، و صفحهٔ Home واقعی به‌جای placeholder.
**منتظر تایید مالک.**
