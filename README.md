# Island

A Dynamic Island for Android: a pill that sits over the camera cutout and morphs to show what's happening.

## What it shows

- **Music**: title/artist of the active media session (play state aware)
- **Sound recording**: red pulsing dot whenever any app is recording audio
- **Charging wattage**: live watts while charging, with an animated decimal counter (e.g. `18.42 W`)

Tap the pill to expand/collapse.

## Permissions (set up in the app)

1. **Draw over other apps** (the pill itself)
2. **Notification access** (lets Island read the active media session)
3. **Notifications** (Android 13+, for the foreground service)

## Build

GitHub Actions builds a debug APK on every push to `main` (artifact: `island-debug-apk`).

## Notes

- Charging wattage = battery current x voltage. Some devices report current in different units or sign; the monitor normalizes both.
- Stack: Kotlin, Jetpack Compose, WindowManager overlay (`TYPE_APPLICATION_OVERLAY`).
