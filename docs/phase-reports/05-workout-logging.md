# گزارش فاز ۵ — Workout Logging

> تاریخ: ۲۰۶-۰۹-۲۶ | وضعیت: ✅ کامل‌شده و push‌شده روی `main` (کامیت `ad57568`)
> تست‌های جدید: **۸، همه سبز** | ktlint: سبز | APK: بازسازی‌شده

---

## ۱. خروجی‌ها

### جریان لاگینگ (مطابق پروتوتایپ تاییدشده)
- **مرحله ۱ — Session Info**: تاریخ (امروز = نیمه‌شب محلی UTC millis)، اسلایدر شدت کلی ۱–۱۰، چیپ‌های Feeling، یادداشت
- **مرحله ۲ — Activities**: کارت‌های Activity با چیپ‌های نوع (Cardio/Heavy Bag/Mitt Work/Sparring)، استپر Duration (±۵ دقیقه، کف ۵)، اسلایدر شدت، چیپ‌های Focus (۱۰ مورد)، و برای انواع round-based ویرایشگر Roundها (استپر work/rest، افزودن/حذف با **شماره‌گذاری خودکار مجدد**)؛ سوئیچ به Cardio راندها را حذف و برگشت به Sparring راند پیش‌فرض می‌سازد
- **مرحله ۳ — Review & Save**: خلاصهٔ مجموع duration/راندها/شدت/Feeling + Save تراکنشی
- **حذف با Confirmation Dialog** (AlertDialog): متن صریح «Activityها و Roundها هم حذف می‌شوند»
- **حالت Edit**: `WorkoutRoute(sessionId)` type-safe؛ ViewModel موجودی را از Repository بارگذاری می‌کند و با `UpdateTrainingSession` (تراکنش `updateFullSession`) ذخیره می‌کند

### معماری
-_draft_ کامل Aggregate در UiState؛ **یک نوشتن تراکنشی** هنگام Save (قانون ۷) — هیچ نوشتن میانی وجود ندارد
- ViewModel فقط Use Caseها را می‌بیند؛ userId از `ObserveSession` گرفته می‌شود (هرگز از UI)
- لیبل‌های انگلیسی enumها در `domain/training/.../Labels.kt` (نه hardcode در UI)
- `TimeUtils` در `:core:common` با `java.util.Calendar` (بدون core-library desugaring برای minSdk 24)

---

## ۲. تست‌ها

| سوئیت | تعداد | نتیجه |
|---|---|---|
| `WorkoutLoggingViewModelTest` | 6 | ✅ |
| `TimeUtilsTest` | 2 | ✅ |

پوشش VM: راند پیش‌فرض برای انواع round-based و حذف هنگام Cardio، شماره‌گذاری مجدد پس از حذف راند، Save ساختِ تراکنشی (با بررسی aggregate ذخیره‌شده)، **خطای validation هنگام Save بدون Activity و عدم ایجاد هیچ رکوردی**، حالت Edit (بارگذاری + update در جا با حفظ id)، حذف پس از تایید.
سناریوی «crash وسط تراکنش» همان تست rollback فاز ۲ است (هیچ Session ناقصی باقی نمی‌ماند) و بدون تغییر سبز ماند.

---

## ۳. یادداشت‌ها / انحراف‌ها
1. **تست Compose UI** در این sandbox اجرا نشد (بدون امولاتور)؛ تست‌های VM معادل منطق جریان را پوشش می‌دهند. تست UI دستگاه در فاز ۱۰ روی build تستی انجام می‌شود.
2. ورودی‌های Edit/Delete از صفحهٔ History در **فاز ۶** سیم‌کشی می‌شوند (روترها و جریان‌ها همینک آماده‌اند).
3. بیلد sandbox همچنان با زنجیرهٔ فراخوان‌های تفکیک‌شده (الگوی مستند فاز ۱) انجام شد؛ APK نهایی از سورس commit‌شده ساخته شده است.

---

## ۴. گام بعدی
فاز ۶ — History: لیست گروه‌بندی‌شده بر اساس روز (با مرز هفتهٔ شنبه–جمعه برای هدرها)، صفحهٔ جزئیات (Session → Activities → Rounds)، ورودی‌های Edit/Delete، Empty State، و تصمیم تقویم نمایش (پیشنهاد: میلادی). **منتظر تایید مالک.**
