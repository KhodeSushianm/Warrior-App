# گزارش فاز ۱۲ — Database v2 + Body Metrics + Backup (فصل ۲)

> تاریخ: ۲۰۲۶-۰۹-۲۸ | وضعیت: ✅ کامل و push‌شده روی `main`
> تست‌ها: **۴۱ تست جدید/به‌روز، همه سبز** (کل سوئیت: ۱۴۱) | ktlint: سبز
> APKها از سورس نهایی: release امضاشده ۱.۹۸MB (R8 + gate صحت) · debug ۱۱.۳MB

---

## ۱. خروجی‌ها

### Schema v2 — اولین مهاجرت واقعی پروژه (قانون ۱۱)
- دو جدول **کاملاً additive**: `body_metrics` (FK به users با CASCADE، ایندکس‌های
  userId و userId+date) و `session_tags` (FK به sessions و users؛ جدول از حالا ساخته شد
  تا اسکیما **یک بار** نسخه بخورد — مصرف UI آن در فاز ۱۷).
- `MIGRATION_1_2` با SQL دقیقاً مطابق schema JSON صادرشده؛ `2.json` در VCS commit شد.
- `DatabaseModule`: `addMigrations(MIGRATION_1_2)` — release بدون fallback مخرب؛
  debug پشت همان flag فاز ۱۰.
- `MigrationTest` گسترش یافت: **`runMigrationsAndValidate`** (تطبیق بایت‌به‌بایت با
  `2.json`) + حفظ دادهٔ v1 + نوشتن در جداول جدید؛ و تست فاز ۱۰ حالا مسیر migration
  واقعی production را طی می‌کند (v1 → بازکردن با Room v2).

### متریک‌های بدن (دامنهٔ progress — بدون ماژول جدید)
- `BodyMetric` + `BodyErrorCode` + `BodyRepository` (مرز دامنه) + `RoomBodyRepository`.
- `BodyValidation` با بازه‌های ایمن (وزن ۲۰–۴۰۰kg، قد/ریچ ۱۰۰–۲۵۰cm، چربی ۳–۷۰٪،
  HR ۳۰–۲۲۰، تاریخ futuro رد می‌شود) — همان الگوی کد-محور i18n-ready فاز ۹.
- Use Caseها: Observe/Add/Delete/GetLatest (Add با timestamp از `TimeProvider` تزریقی).

### صفحهٔ Athlete Body (در feature:profile)
- هیرو: وزن فعلی با **delta نسبت به اندازهٔ قبلی** (رنگ‌بندی مثبت/منفی) و هدر روز.
- **نمودار روند وزن** (`WeightTrendChart` — Canvas سفارشی جدید در designsystem:
  پلی‌لاین + فیل گرادیانی + نقطهٔ آخرین اندازه برجسته) روی ۲۰ نقطهٔ آخر،
  نرمال‌شده در VM (تست‌پذیر).
- کارت اندازه‌گیری‌ها (قد/ریچ/چربی/HR — آخرین مقدار غیرnull در تاریخچه)، لیست
  History با حذف تأییدشده، دیالوگ افزودن با کیبورد عددی و خطاهای کد‌محور.
- مسیر جدید `BodyRoute`؛ bottom bar در آن مخفی (مثل جزئیات History).

### پشتیبان‌گیری کامل — Export/Import (ردیف Post-MVP پروفایل فعال شد)
- `BackupRepository` (مرز دامنه در domain:training) + `JsonBackupRepository`:
  سند JSON نسخه‌دار `{app, formatVersion, exportedAt, users, sessions, activities,
  rounds, bodyMetrics, sessionTags}` — **کل دستگاه** شامل hash/salt رمزها.
- Import: parse+validate **بدون هیچ اثر جانبی** (app/formatVersion چک می‌شود) و سپس جایگزینی
  اتمیک در یک تراکنش (delete users → cascade همه‌چیز → درج parents-first).
  خطا = دادهٔ موجود دست‌نخورده (تست Robolectric).
- UI با **SAF** (بدون مجوز حافظه): `CreateDocument` برای export و `OpenDocument`
  برای import؛ فایل‌خوانی/نوشتن در لایهٔ Screen (VM بدون Context و کاملاً تست‌پذیر).
- تأیید صریح «Replace & import» + دیالوگ نتیجه؛ پس از restore موفق، **logout خودکار**
  (session قدیمی ممکن است به کاربر حذف‌شده اشاره کند) با پیام راهنمای login.

---

## ۲. تست‌ها (۴۱ جدید/به‌روز — کل ۱۴۱ سبز)

| سوئیت | تعداد | نکته |
|---|---|---|
| `BodyUseCasesTest` (domain) | 3 | بازه‌ها/کدها، scoping کاربر، timestamp، delete ownership، latest |
| `MigrationTest` (Robolectric) | 2 | v1→v2 با `runMigrationsAndValidate` روی `2.json` + مسیر production |
| `BackupRepositoryTest` (Robolectric) | 4 | سند نسخه‌دار، restore روی «دستگاه جدید» (login با hash بازیابی‌شده!)، wipe-and-restore هم‌دستگاه، رد payload بیگانه/خراب/نسخهٔ آینده بدون تغییر داده |
| `ProfileViewModelTest` (+4) | 10 | export payload/result، import موفق (restore→sign-out پس از dismiss)، invalid، blank |
| `BodyViewModelTest` (جدید) | 5 | افزودن زنده + latest/previous/trend نرمال‌شده [1.0,0.0]، ممیز کاما، کدهای خطا، حذف، signed-out |
| رگرسیون | 117 | domain:progress ۳۵ قبلی، auth ۱۸، training ۱۷، common ۷، Robolectric قبلی‌ها، VMهای home/history/workout/progress |

---

## ۳. یادداشت‌ها / انحراف‌ها
1. `@Serializable` مستقیم روی entityهای Room — DTO جدا نمک‌خواباندیم؛ `formatVersion`
   در سند، مسیر ارتقای فرمت را باز می‌گذارد (import نسخه‌های ناشناخته reject می‌شود).
2. Import **کل دستگاه** را جایگزین می‌کند (语义 پشتیبان‌گیری کامل) — merge انتخابی
   می‌تواند پس از MVP اضافه شود؛ UI صریح هشدار می‌دهد.
3. کیس وردشی `session_tags` ساخته شد ولی UI ندارد (فاز ۱۷) — برای جلوگیری از v3.
4. بیلد sandbox: همان زنجیرهٔ تفکیک‌شده؛ `build-release.sh` بدون تغییر کار کرد
   (gate نگاشت mapping هم `WarriorDatabase_Impl` را تأیید کرد).

## ۴. گام بعدی
فاز ۱۳ — فارسی + RTL + تقویم شمسی: `values-fa` همهٔ ماژول‌ها، سوییچ زبان درون‌اپی،
تقویم جلالی نمایشی (ذخیره‌سازی UTC بدون تغییر) و RTL یکدست.
