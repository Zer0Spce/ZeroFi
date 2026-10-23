# ZeroFi

Turn a spare Android phone into a dedicated pocket Wi-Fi router dashboard.

ZeroFi is a landscape-first Android app inspired by dedicated mobile Wi-Fi devices such as Samsung's Galaxy 5G Mobile Wi-Fi. It is designed to make an unused Android phone feel like a purpose-built pocket router: status at a glance, real mobile-data telemetry, Android hotspot integration, a connection QR card, battery runtime estimates, and optional kiosk/dedicated-device behavior.

## Current features

- Android 6.0+ support (API 23 minimum)
- Landscape-first fullscreen pocket-router dashboard
- 4G / 5G / 3G / 2G network type where Android exposes it
- Carrier, cellular connectivity, signal level and dBm on supported Android versions
- Battery percentage, charging state, temperature, uptime, and estimated runtime remaining
- Mobile-data usage for the current billing cycle and last 3 days via Usage Access
- Configurable monthly data allowance and billing-cycle reset day
- Real Android tethering/hotspot settings integration with graceful OEM fallbacks
- Android 16+ tethering status/band visibility where permitted
- Saved ZeroFi connection card with SSID/password/security/band and Wi-Fi QR code
- Connected-device screen that never fabricates unavailable client information
- Theme accents: Sunset Orange, Green, Blue, Red, Purple, and White / Black
- Dark and light appearance
- OLED Saver with true-black AMOLED surfaces, reduced glow, brightness control, and anti-burn-in dashboard drift
- Keep-screen-awake and immersive fullscreen controls
- Auto-launch-after-boot option
- Router Lock using Android Lock Task / screen pinning
- Full dedicated kiosk mode when ZeroFi is configured as Device Owner
- Dedicated Battery Saver on Android 7+ Device Owner devices: suspends ordinary user-installed apps while leaving Android system apps untouched

## Android / OEM limitations

Android deliberately protects Wi-Fi tethering configuration and Soft AP client details from ordinary third-party apps. ZeroFi uses public Android APIs and capability detection rather than hidden/reflection hacks.

That means:

- On many phones ZeroFi opens the manufacturer's real hotspot page instead of silently toggling tethering.
- Hotspot client names/IP/MAC/count may be unavailable depending on Android/OEM permissions.
- Android 16+ exposes additional tethering status that older versions do not.
- Device Owner is required for the strongest kiosk and app-suspension features.

Unsupported information is displayed as unavailable rather than invented.

## Dedicated Device Owner setup

Full Router Lock and Dedicated Battery Saver are intended for a spare/dedicated device. Device Owner generally must be configured before accounts are added or after a factory reset.

```bash
adb shell dpm set-device-owner com.zer0spce.zerofi/.admin.ZeroFiDeviceAdminReceiver
```

Without Device Owner, ZeroFi can still request normal Android screen pinning and all non-admin dashboard features continue to work.

## OLED Saver

OLED Saver is intended for phones that will leave ZeroFi visible for long periods:

- Uses true `#000000` background pixels
- Reduces decorative card glow
- Supports low app-specific brightness
- Moves the dashboard by a few pixels every minute to reduce static burn-in exposure

OLED Saver can also be used on LCD phones, but the true-black pixel power benefit is specific to emissive displays such as OLED/AMOLED.

## Tech

- Kotlin
- Jetpack Compose + Material 3
- Android Gradle Plugin 9.4
- compileSdk / targetSdk 37
- minSdk 23 (Android 6.0)
- ZXing QR generation
- GitHub Actions debug APK builds

## Status

Active alpha development. GitHub Actions is used as the build gate before APK device testing.
