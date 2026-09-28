# WARRIOR — Season 2 Execution Plan (Post-MVP)

> شروع: ۲۰۲۶-۰۹-۲۸ · وضعیت با ✅/🔄/⏸ به‌روز می‌شود · الگوی هر فاز مثل MVP:
> پیاده‌سازی + تست ← بیلد ← commit & push ← گزارش فاز

| فاز | عنوان | خروجی‌های کلیدی | وضعیت |
|---|---|---|---|
| ۱۲ | Database v2 + Body Metrics + Backup | مهاجرت v1→v2 (جداول `body_metrics` و `session_tags`)، تست Migration، متریک‌های بدن (دامنه+دیتا+UI در Profile)، Export/Import کامل JSON از طریق SAF | ✅ — [گزارش](phase-reports/12-database-v2-body-backup.md) |
| ۱۳ | Persian + RTL + Jalali | `values-fa` همهٔ ماژول‌ها، سوییچ زبان درون‌اپی (بدون appcompat)، تقویم شمسی نمایشی (ذخیره‌سازی UTC بدون تغییر)، RTL یکدست | ✅ — [گزارش](phase-reports/13-persian-rtl-jalali.md) |
| ۱۴ | Live Round Timer | حالت تمرین زنده: تایمر کار/استراحت، بوق+ویبره، پیشروی خودکار راند، ثبت خودکار Session از تایمرها، تنظیمات پیش‌فرض در DataStore | 🔄 |
| ۱۵ | Templates + Goals | الگوهای تمرین (DataStore JSON، scoped به کاربر)، Quick-Add از Home، اهداف هفتگی (جلسه/دقیقه) + رینگ پیشرفت، Streak Freeze | ⏸ |
| ۱۶ | Analytics + Smart PRs | نمودار روند شدت/حجم، رادار Focus، توزیع روزهای هفته، کارت‌های Insight متنی، PR تفکیک‌شده بر اساس نوع فعالیت + جشن رکوردشکنی هنگام Save | ⏸ |
| ۱۷ | Achievements + Search + Widget | مدال‌ها/بج‌ها (مشتق از داده + اطلاعیه)، جستجوی full-text در Notes، فیلتر تگ‌ها (از جدول `session_tags` فاز ۱۲)، ویجت Glance خانه | ⏸ |
| ۱۸ | 3D Glass Boxer + v1.1.0 | انسان شیشه‌ای سه‌بعدی با لباس بوکس (three.js بسته‌بندی‌شده در assets، کاملاً آفلاین، WebView)، لیبل‌های دادهٔ مداری ۳بعدی، fallback دوبعدی، انتشار Release v1.1.0 | ⏸ |

## قوانین فصل ۲ (میراث MVP)
- Source of Truth فقط Room؛ همهٔ آمار derived و بدون ذخیره (§13.4).
- هفته شنبه→جمعه فقط از `WeekBoundaryProvider`.
- Schema: تغییر فقط با Migration تست‌شده (MigrationTestHelper فاز ۱۰)؛ release بدون fallback مخرب (قانون ۱۱).
- هیچ رشتهٔ hardcode در UI؛ رشته‌های جدید هم‌زمان en (+fa از فاز ۱۳).
- بدون وابستگی شبکه‌ای؛ کتابخانهٔ جدید فقط وقتی ضرورت دارد (three.js استثنای asset-محور فاز ۱۸).
- بیلد sandbox با زنجیرهٔ تفکیک‌شده (`scripts/build-debug.sh` / `build-release.sh` / `test-lowram.sh`).
