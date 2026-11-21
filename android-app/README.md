# 🚗 CarController — اپ اندروید (مرحلهٔ ۴)

اپلیکیشن اندروید کنترل خودرو — جایگزین کامل صفحات وب پنل اکسس‌پوینت.

## ساخت APK بدون Android Studio (با GitHub Actions)

راهنمای کامل فارسی: [`../docs/ANDROID-APP.md`](../docs/ANDROID-APP.md) — خلاصه:

1. کل پوشهٔ `CarController-VPS` را در یک مخزن **Private** گیتهاب آپلود کنید.
2. تب **Actions** → ورک‌فلو **Build Android APK** خودکار اجرا می‌شود (یا دکمهٔ Run workflow).
3. از اجرای سبز، بخش **Artifacts** → `CarController-APK` را دانلود کنید.
4. `app-release.apk` (امضاشده) را روی گوشی نصب کنید (اجازهٔ «نصب از منابع ناشناس»).

## دو حالت اتصال

| حالت | مسیر | ویژگی |
|---|---|---|
| 🖥️ سرور (VPS) | اینترنت → HTTPS سرور → GPRS دستگاه | از هر جا؛ رخداد/پیامک/مسیر/صف فرمان |
| 📡 اکسس‌پوینت | وای‌فای ESP32-S3 → `http://192.168.4.1` | بدون اینترنت؛ نزدیک خودرو |

هر دو حالت صفحهٔ اصلی مشترک دارند: **دو دکمهٔ عملیات تگ RFID** (مرحله ۱ / مرحله ۲)، کنترل خروجی‌ها، قفل/آژیر و تمام صفحات تنظیمات.

## ساختار

```
android-app/
├── .github/workflows/build-android.yml   ← در ریشهٔ مخزن (یک پله بالاتر)
├── app/src/main/java/com/carcontroller/app/
│   ├── MainActivity.kt      ← ناوبری
│   ├── AppModel.kt          ← منطق دو حالت (فرمان‌ها + ACK)
│   ├── VpsClient.kt         ← REST/WebSocket سرور
│   ├── ApClient.kt          ← اندپوینت‌های مستقیم /api/* فریمور
│   ├── DeviceState.kt       ← مدل یکسان وضعیت/تنظیمات
│   └── ui/screens/…         ← صفحه‌ها (خانه/تنظیمات/رخدادها/…)
└── keystore.jks + keystore.properties   ← امضای release
```

## نکته‌های فنی

- Kotlin 2.0 + Jetpack Compose (Material 3) — بدون XML.
- گواهی سرور self-signed است؛ OkHttp با trust-all + TLS برقرار می‌شود (مثل «ادامه بده» مرورگر).
- در حالت سرور، فرمان‌ها از مسیرهای تایپ‌شدهٔ REST می‌روند و ACK دستگاه پیگیری می‌شود.
- در حالت اکسس‌پوینت، اپ مستقیم `GET/POST /api/*` فریمور (مرحلهٔ ۴) را صدا می‌زند.
