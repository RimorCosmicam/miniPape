# miniPape

miniPape creates cover-screen wallpapers for the Samsung Galaxy Z Flip 7.

- **miniPape for Mac** — a native macOS 27 Liquid Glass cropper, timeline editor, live phone preview controller, and sender.
- **miniPape for Android** — a native Material 3 Expressive receiver, wallpaper gallery, cover-screen preview, and on-device editor.

Both editors share miniMate's 16-effect filter catalog. Filters are stored as an ordered, non-destructive stack in each wallpaper recipe and rendered during Mac and phone previews.

The canonical output canvas is **1048 × 948 pixels**. Mac-to-phone communication stays on the local network and uses a short pairing code. No cloud account is required.

## Repository layout

- `macOS/` — SwiftUI and AVFoundation application
- `android/` — Kotlin and Jetpack Compose application
- `shared/` — protocol and device-canvas specifications
- `.github/workflows/` — CI-only builds for the Mac app and APK

## Platform boundaries

Samsung publicly supports Flex Window widgets and launching an activity on cover display ID 1. Android's public `WallpaperManager` exposes system and lock wallpaper destinations, but no Samsung-specific cover-screen destination. miniPape therefore previews in its cover activity, saves finished wallpapers to its gallery, and hands installation to the public Samsung/system wallpaper flow.

## Build policy

Application binaries are built by GitHub Actions. Do not compile on the development Mac.
