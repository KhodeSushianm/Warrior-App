# گزارش فاز ۴ — Authentication

> تاریخ: ۲۰۲۶-۰۹-۲۶ | وضعیت: ✅ کامل‌شده و push‌شده روی `main` (کامیت `fd35c31`)
> تست‌های جدید: **۲۲، همه سبز** | ktlint: سبز | APK: بازسازی‌شده

---

## ۱. خروجی‌ها

### :core:security — `PasswordHasher`
- PBKDF2WithHmacSHA256، **۱۲۰,۰۰۰ تکرار** (کف OWASP 2023)، salt تصادفی **۱۶ بایت** با SecureRandom، کلید ۳۲ بایت
- `verify` با `MessageDigest.isEqual` (مقایسهٔ constant-time)
- salt و hash به‌صورت hex جدا ذخیره می‌شوند (`users.passwordSalt` / `users.passwordHash`)
- تست برداری مستقل: سه بردار PBKDF2-HMAC-SHA256 که با **python hashlib** تولید و تأیید شده بودند (c=1/2/4096) — تطابق کامل

### :domain:auth
- `LocalAccount`, `LocalSession` (اینترفیس), `AuthRepository` (اینترفیس), خطاهای نوع‌دار `DuplicateUsernameException` / `InvalidCredentialsException`
- Use Caseها: `CreateLocalAccount` (نرمال‌سازی username به lowercase + validation + شروع session)، `Login`، `Logout` (فقط پاک‌کردن session)، `ObserveSession`، `GetAccount`

### :data:local
- `LocalSessionManager`: DataStore preferences با دو کلید `currentUserId` / `sessionCreatedAt` (مطابق §۱۴.۲؛ Logout فقط همین دو کلید را پاک می‌کند)
- `RoomAuthRepository`: register با بررسی تکراری‌بودن (case-insensitive)، login با verify؛ پسورد plain هرگز ذخیره نمی‌شود
- `AuthModule`: بایندینگ‌های Hilt

### UI
- `feature:auth`: `AuthViewModel` (UiState کامل: mode/login/register، filds، isSubmitting، errors) + صفحهٔ واقعی Login/Register با نمایش خطاهای دامنه‌ای
- `feature:profile`: `ProfileViewModel` (نمایش اکانت از Session + Logout)
- `app`: **ریشهٔ session-محور** — `AppViewModel` جریان `currentUserId` را observe می‌کند؛ `null` → درخت Auth، غیر‌null → درخت Main با bottom nav. Login/Logout خودبه‌خود درخت را عوض می‌کنند (بدون ناوبری دستی)

### وابستگی‌های ساختاری مهم
- `:app` حالا به `:data:local` وابسته است — بدون آن Hilt ماژول‌های DI دیتابیس/اث را aggregate نمی‌کرد (و در runtime هم bindingها missing می‌شدند)
- Use Caseها `@Inject constructor` گرفتند (`javax.inject` در ماژول‌های domain)

---

## ۲. تست‌ها

| سوئیت | تعداد | نتیجه |
|---|---|---|
| `PasswordHasherTest` (بردارهای مستقل + verify + salt) | 5 | ✅ |
| `AuthValidationTest` | 6 | ✅ |
| `AuthUseCasesTest` (Fake repo/session: نرمال‌سازی، duplicate، logout، observe) | 7 | ✅ |
| `AuthRepositoryTest` (Robolectric: round-trip، hash≠plain، duplicate case-insensitive، wrong password) | 4 | ✅ |
| اجرای مجدد سوئیت فاز ۲/۳ در data:local | 13 | ✅ |

**DoD فاز ۴:** جریان Register → Login → (ری‌استارت = بقای session در DataStore) → Logout با تست‌های سطح repository/use-case پوشش داده شد؛ بررسی دستگاه/امولاتور مانند قبل به milestone دستی واگذار است (بدون KVM).

---

## ۳. یادداشت‌ها
1. هش در تست `register_storesHashNotPlainText` بررسی می‌شود: طول hash = ۶۴ hex (۳۲ بایت) و salt = ۳۲ hex (۱۶ بایت) و عدم حضور متن پسورد.
2. ثبت‌نام بلافاصله session را شروع می‌کند (رفتار محصولی رایج)؛ تست‌ها این contracts را صریح کرده‌اند.
3. بیلد در sandbox یک‌گیگابایتی همچنان با زنجیرهٔ فراخوان‌های تفکیک‌شده انجام می‌شود؛ `build-debug.sh` و `test-lowram.sh` بدون تغییر کار کردند.

---

## ۴. گام بعدی
فاز ۵ — Workout Logging: جریان سه‌مرحله‌ای ساخت Session (اطلاعات سربرگ → Activityها → Review)، ویرایش/حذف با Confirmation، enum pickerها با لیبل انگلیسی، ViewModelها با UiState/Events و ذخیرهٔ تراکنشی از طریق Repository. **منتظر تایید مالک.**
