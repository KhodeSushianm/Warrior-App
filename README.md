# WARRIOR 🥊

اپلیکیشن اندرویدی **کاملاً آفلاین (Local-First)** برای بوکسورها: ثبت تمرین، ردیابی تاریخچه و تحلیل پیشرفت.

حلقهٔ اصلی محصول: `LOG → TRACK → ANALYZE → IMPROVE`

## وضعیت پروژه

| مرحله | وضعیت |
|---|---|
| معماری سیستم (نسخهٔ ۲.۱) | ✅ انجام‌شده |
| طراحی دیتابیس (نسخهٔ ۴) | ✅ آمادهٔ Schema Freeze |
| مرجع بصری UI/UX | ✅ موجود |
| کدنویسی MVP | ⏳ شروع‌نشده — فاز ۱: Foundation |

## اسکوپ MVP

- **احراز هویت محلی**: Register / Login / Logout — هش PBKDF2WithHmacSHA256 (≥۱۲۰,۰۰۰ تکرار، salt جدا) و Session در DataStore
- **Home Dashboard**: آمار هفته، آخرین Session، رکوردهای شخصی (PR)
- **Workout Logging**: Session ← Activity ← Round + Duration / Intensity / Focus / Notes
- **History**: لیست گروه‌بندی‌شده بر اساس تاریخ، جزئیات، Edit، Delete
- **Progress**: آمار هفتگی، توزیع تمرین/Focus، نمودارهای روند
- **Profile**: نام نمایشی، نام کاربری، تنظیمات، Logout

در MVP هیچ Backend، Sync یا قابلیت اجتماعی وجود ندارد (مسیر آینده: `Export → Import → Cloud Sync`).

## تکنولوژی و معماری

Kotlin · Jetpack Compose · Room (SQLite) · DataStore · Hilt

معماری لایه‌ای با جریان دادهٔ یک‌طرفه:

```text
Composable → ViewModel → Use Case → Repository → Room DAO → SQLite
```

تمام آمارها (Derived Data) در زمان اجرا از دادهٔ خام محاسبه می‌شوند؛ Source of Truth فقط Room است.

## مستندات

| سند | محتوا |
|---|---|
| [warrior-architecture.md](warrior-architecture.md) | معماری سیستم و تعریف محصول — نسخهٔ ۲.۱ |
| [Database Design — WARRIOR v1.md](Database%20Design%20%E2%80%94%20WARRIOR%20v1.md) | اسکیمای Room، DAOها، تراکنش‌ها، Migration — نسخهٔ ۴ |
| [EXECUTION-PLAN.md](EXECUTION-PLAN.md) | نقشهٔ اجرای فازبندی‌شده با دروازهٔ تایید — نسخهٔ ۱.۰ |
| [APP UI-UX DESIGN REFFERENCE.jpg](APP%20UI-UX%20DESIGN%20REFFERENCE.jpg) | مرجع سبک بصری (تم تیره) — نه spec صفحه‌به‌صفحه |

## نقشهٔ راه توسعهٔ MVP

```text
Foundation ← Database ← Authentication ← Workout Logging ← History
        ← Dashboard ← Progress Engine ← Charts ← UI Polish ← Testing
```

## اجرای پروژه

فعلاً کدی وجود ندارد؛ با شروع فاز ۱ (ساخت پروژهٔ Android با Compose/Hilt/Room) این بخش تکمیل می‌شود.

## مجوز

فعلاً بدون LICENSE — تمام حقوق برای مالک ریپو محفوظ است.
