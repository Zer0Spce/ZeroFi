# ZeroFi

Turn a spare Android phone into a dedicated pocket Wi-Fi dashboard.

ZeroFi is a landscape-first Android app inspired by dedicated mobile Wi-Fi devices such as Samsung's Galaxy 5G Mobile Wi-Fi. The goal is to make an unused Android phone feel like a purpose-built pocket router: simple status at a glance, hotspot shortcuts/control where Android permits it, data-usage monitoring, signal information, connected-device visibility where available, and an optional dedicated-device/kiosk experience.

## Design direction

- Dark, touch-friendly router dashboard
- Accent themes: Sunset Orange, Green, Blue, Red, Purple, and White / Black
- Subtle glow, border, press-scale, and state animations
- Large circular gauges for mobile network and data usage
- Landscape-first layout designed to stay readable on small phones
- No fake telemetry: unsupported data is shown as unavailable rather than invented

## Planned features

- 4G / 5G network status and signal information
- Mobile data usage with configurable billing-cycle reset and data cap
- Hotspot status and system tethering integration where Android/OEM permissions allow
- Hotspot SSID/password/QR-code screen
- Connected-client information where exposed by the device
- Upload/download activity and uptime
- 2.4 GHz / 5 GHz status where available
- Boot launch and keep-awake options
- Fullscreen router mode
- Optional Android Lock Task / Device Owner kiosk mode
- Hidden PIN-protected exit from dedicated router mode

## Android support

Initial target: Android 10+ (API 29+).

Android deliberately restricts silent Wi-Fi tethering control and Soft AP client information for ordinary third-party apps. ZeroFi will use capability detection and provide graceful fallbacks to the system tethering/settings UI rather than pretending unsupported controls work.

## Tech

- Kotlin
- Jetpack Compose + Material 3
- Android Gradle Plugin 9.4
- compileSdk / targetSdk 37

## Status

Early development / UI foundation.
