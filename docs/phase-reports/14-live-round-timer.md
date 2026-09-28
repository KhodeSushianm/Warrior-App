# گزارش فاز ۱۴ — تایمر زندهٔ راند (Workout Mode)

> تاریخ: ۲۰۲۶-۰۹-۲۸ | وضعیت: ✅ کامل و push‌شده روی `main`
> تست‌های جدید: **۶ (LiveTimerViewModelTest)** — کل سوئیت ۱۴۹ سبز | ktlint: سبز
> APK: release امضاشده ۲.۰۴MB · SHA-256 `9672d681f94902d77d2f9881159b25cac0e18e8e4bfd89772f3ec9c3f65046e7`

---

## ۱. خروجی‌ها

### ماشین حالت تایمر (`LiveTimerViewModel`) — بدون drift، کاملاً تست‌پذیر
- **زمان‌سنجی لنگری (anchor-based)**: `remaining = anchorRemaining − (now − anchor)` با
  `TimeProvider` تزریقی — شمارش معکوس هرگز drift نمی‌کند و **overshoot بین فازها
  carry می‌شود** (۱ ثانیه تأخیر در پایان کار، از استراحت کم می‌شود؛ تست صریح).
- صفحه هر ~۲۰۰ms فقط `tick()` صدا می‌زند — تمام منطق در VM (تست با ساعت جعلی،
  بدون delay واقعی).
- انتقال خودکار WORK→REST→راند بعد؛ **پایان راند آخر = پایان تمرین بدون استراحت
  اضافی** (رفتار تایمرهای بوکس). Skip فاز، Pause/Resume، و «پایان زودهنگام» با
  حفظ راندهای **شروع‌شده**.
- رویدادها (`WorkStarted/RestStarted/Finished`) با `SharedFlow` → بوق و ویبره در UI.

### ثبت خودکار Session از تایمر
- پایان تمرین → خلاصه (راند/زمان کل/نوع/تمرکز) → **Save Session** از همان مسیر
  تراکنشی `CreateTrainingSession` (aggregate کامل: Activity با N راند کار/استراحت،
  مدت بلوک = `N×work + (N−1)×rest`، شدت پیش‌فرض ۷، Feeling=GOOD، تاریخ امروز)؛
  Discard هم دارد. خطای ذخیره → پیام منبع‌محور.
- تنظیمات اجرا (work/rest/rounds) به‌عنوان **پیش‌فرض بعدی** در DataStore ذخیره
  می‌شوند (`TimerPreferences` — همان فایل settings فاز ۱۳، یک store مشترک).

### UI (تیره، بزرگ، قابل‌دید در باشگاه)
- رینگ پیشرفت Canvas ۲۴۰dp (کار=قرمز / استراحت=سبز)، شمارش `m:ss` با ۵۶sp،
  «ROUND 3/6»، نقاط راند (پر/جاری/باقی)، دکمه‌های بزرگ Skip / Pause-Resume / End.
- **بوق + ویبره** متمایز برای شروع کار، استراحت و پایان (ToneGenerator بدون
  مجوز + `VIBRATE` در manifest؛ VibratorManager در API 31+ و fallback).
- صفحه حین اجرا روشن می‌ماند (`FLAG_KEEP_SCREEN_ON` — بدون نیاز به سرویس
  foreground؛ تایمر پس‌زمینه = ارتقای آینده، مستند در یادداشت‌ها).
- ورودی: دکمهٔ ghost «تایمر راند» زیر CTA ثبت تمرین در Home (`LiveTimerRoute`).
- رشته‌ها en+fa کامل (بدون hardcode).

---

## ۲. تست‌ها

| سناریو | پوشش |
|---|---|
| پیش‌فرض‌ها از DataStore + clamp استپرها (work≥5s، rounds≥1) | ✅ |
| شمارش معکوس + carry شدن overshoot به فاز بعد (۱۱s از ۱۰s → REST با ۴s) | ✅ |
| Pause/Resume: گذر زمان دیواری حین pause مصرف نمی‌شود | ✅ |
| اجرای کامل ۲ راندی → FINISHED → aggregate دقیق (SPARRING، ۲۵s، راندهای ۱۰/۵، شماره‌ها) | ✅ |
| پایان زودهنگام حین REST → فقط راند شروع‌شده ذخیره می‌شود | ✅ |
| start → persist شدن config به‌عنوان پیش‌فرض بعدی | ✅ |

رگرسیون: workout logging ۶، Robolectric ۲۳، home ۴ و بقیه سبز (مجموع ۱۴۹).

---

## ۳. یادداشت‌ها / انحراف‌ها
1. تایمر فقط با صفحهٔ باز کار می‌کند (KEEP_SCREEN_ON). سرویس foreground با
   نوتیفیکیشن (اجرا با صفحهٔ خاموش) ارتقای طبیعی بعدی است — نیازمند مجوز
   POST_NOTIFICATIONS روی API 33+؛ عمداً بیرون از اسکوپ این فاز.
2. شدت/Feeling پیش‌فرض (۷/GOOD) است — کاربر پس از Save می‌تواند از History
   ویرایش کند (جریان موجود).
3. `TimerPreferences` به `AppPreferences` موجود اضافه شد (یک DataStore، دو مرز
   دامنه) — طبق §14.2 بدون SharedPreferences.

## ۴. گام بعدی
فاز ۱۵ — الگوهای تمرین (Templates) + اهداف هفتگی + Streak Freeze.
