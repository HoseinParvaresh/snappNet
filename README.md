# Snapp Net (com.hosein.snappnet)

Reads the fare shown in the Snapp Box driver app (AccessibilityService), computes
**net = fare × 78 %** (integer math, rate configurable, default 78) and shows it in a draggable floating card.
It never taps, accepts or rejects anything.

## Build the APK
1. Install Android Studio (current stable) and open this folder. Let Gradle sync
   (it downloads Gradle 8.9, AGP 8.7.3, Kotlin 2.0.21, Compose BOM 2024.12.01, compile/target SDK 35).
   If Studio offers newer versions or a newer SDK, accepting them is fine.
2. Run unit tests: `./gradlew testDebugUnitTest` (or right-click `app/src/test` → Run).
3. Build: **Build → Build APK(s)** → `app/build/outputs/apk/debug/app-debug.apk`
   (CLI: `./gradlew assembleDebug`; if there is no `gradlew`, run `gradle wrapper` once, or use Studio).

## Install and enable on a phone
1. Copy the APK to the phone (or `adb install -r app-debug.apk`) and allow "install unknown apps".
2. Open **Snapp Net** → press **باز کردن تنظیمات دسترسی‌پذیری** → find **Snapp Net** → turn it on.
   - Android 13+: if the switch is greyed out ("Restricted setting"): App info → ⋮ → *Allow restricted settings*, then retry.
   - Xiaomi/Huawei/Samsung etc.: also allow autostart / disable battery optimisation for Snapp Net so the system doesn't kill it.
3. Keep "تشخیص مبلغ" and "نمایش کارت شناور" on. Open Snapp Box; the card appears when an offer with a fare is on screen.
   Drag it where you like; the position is remembered.
4. The overlay-permission row is optional (the card is drawn via the accessibility service).

## IMPORTANT: not verified against the real Snapp Box
I had no access to Snapp Box. Two assumptions need a real-device check:
* **Package id.** `core/SnappPackages.kt` matches `*snappbox*` / `cab.snapp*box*` loosely. Check yours:
  `adb shell dumpsys window | grep mCurrentFocus` with Snapp Box open, then edit that file if needed.
* **Fare text.** The extractor accepts a number only if it is 5,000–3,000,000 and is tied to a currency word
  (تومان / Toman) in the same text or the neighbouring text; labels like «کرایه / مبلغ» raise confidence.
  If Snapp Box draws the fare without exposing text to accessibility (custom canvas/Compose without semantics),
  nothing will be detected, and there is no OCR fallback in this version. Inspect the real tree with
  Android Studio's *Layout Inspector* or `uiautomator dump` and adjust `FareExtractor` (`CURRENCY`, `LABELS`, range).
If the screen is ambiguous (two different plausible fares) nothing is shown, by design.

## Architecture
```
core/      FareCalculator, NumberNormalizer, FareExtractor, FareTracker, TreeNode+NodeFlattener, SnappPackages  (pure Kotlin, unit-tested)
service/   SnappAccessibilityService (event filter → scan on its own thread → publish), AndroidTreeNode
overlay/   OverlayController (one window, created once, shown/hidden/re-texted, draggable, minimisable)
data/      SettingsRepository (DataStore), AppState (live status/stats)
permission/PermissionHelper (status checks + deep links only)
ui/        MainActivity, MainScreen (Compose, RTL)
```
Latency/battery design: events are filtered by package before any work; content changes scan only the changed
subtree (+3 parents); at most 2 scans in flight (a "dirty" flag triggers one catch-up scan); no polling, no debounce
on the show path; fares are deduplicated; the only timers are 150 ms re-checks that confirm a fare really vanished
(hidden after 2 consecutive empty full scans, or immediately when you leave Snapp Box).

## ساخت APK روی GitHub
مخزن را بسازید، همهٔ فایل‌ها (از جمله پوشهٔ مخفی `.github`) را push کنید، به تب **Actions** بروید،
workflow به نام **Build APK** را اجرا کنید و بعد از سبز شدن، فایل `SnappNet-debug-apk` را از پایین صفحهٔ اجرا دانلود کنید.

## رابط کاربری (نسخهٔ جدید)
نوار پایین مثل اپ راننده: **خانه | دسترسی‌ها | [دکمهٔ روشن/خاموش] | پاپ‌آپ | تنظیمات**
* خانه: وضعیت، انتخاب واحد (تومان/ریال)، ماشین‌حساب دستی.
* دسترسی‌ها: دسترس‌پذیری، بهینه‌سازی باتری، نمایش روی برنامه‌ها (اختیاری).
* پاپ‌آپ: پیش‌نمایش زنده، رنگ‌ها، اندازه‌ها، شفافیت، عرض، محتوا، هشدار درآمد کم.
* تنظیمات: روشن/خاموش بودن کارت، ابزار عیب‌یابی، بازنشانی.
فونت: شبنم (`res/font/shabnam_fd.ttf`) در برنامه و کارت شناور. درصد دریافتی ثابت است (`FareCalculator.DEFAULT_RATE_PERCENT`).
