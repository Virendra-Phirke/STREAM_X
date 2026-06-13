# StreamX Player

## Overview
StreamX Player is a modern, fully-featured media player for Android built with Kotlin and Jetpack Compose. It allows users to scan, manage, and play both video and audio files directly from their device.

## Key Features
- **Video & Audio Playback**: Supports a wide range of media formats utilizing the robust `Media3 ExoPlayer`.
- **Media Scanning**: Automatically scans the device's local storage for video and audio files using the `MediaStore` API.
- **Background Playback**: Includes a foreground service (`PlaybackService`) allowing audio to continue playing even when the app is minimized or the screen is locked.
- **Picture-in-Picture (PiP)**: Supports PiP mode for seamless multitasking while watching videos.
- **Modern UI/UX**: Built entirely with Jetpack Compose, featuring Material Design 3 guidelines.
- **Mini Player**: A persistent mini-player appears above the bottom navigation bar when navigating the app while media is playing.
- **Playlists**: Create and manage custom playlists for your favorite audio and video files.
- **Dynamic Theming**: Fluid, animated theme color changes powered by a custom `ThemeManager`.
- **Keyboard Shortcuts**: Built-in keyboard shortcuts (Spacebar to play/pause, arrows to skip) for hardware keyboards.

## Tech Stack & Architecture
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Media Player**: AndroidX Media3 (ExoPlayer & MediaSession)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Navigation**: Jetpack Compose Navigation (`NavHost`)
- **Image Loading**: Coil (with VideoFrameDecoder and custom AudioCoverFetcher)
- **Local Database**: Room Database (for playlists and media metadata)
- **Dependency Injection**: Manual DI via `AppContainer`
- **Concurrency**: Kotlin Coroutines & Flow

## Setup & Run Locally

**Prerequisites:**
- [Android Studio](https://developer.android.com/studio) (Latest stable version recommended)
- Android SDK (API Level 24 to 36)

**Steps:**
1. Clone or download the repository to your local machine.
2. Open **Android Studio**.
3. Select **Open** and choose the directory containing this project (`streamx-player`).
4. Allow Android Studio to sync the Gradle project and resolve dependencies.
5. Create a file named `.env` in the root project directory and set your API keys if needed (e.g., `GEMINI_API_KEY`). See `.env.example` for reference.
6. (Optional) If you face signing keystore errors, remove or comment out this line from the `app/build.gradle.kts` file: `signingConfig = signingConfigs.getByName("debugConfig")`.
7. Build and run the app on an Android Emulator or a physical device (Android 7.0+ / API 24+).

## Permissions Required
The app requests the following permissions to function correctly:
- `READ_MEDIA_AUDIO` / `READ_MEDIA_VIDEO` (Android 13+): To scan and access media files.
- `READ_EXTERNAL_STORAGE` (Android 12 and below): To access local storage files.
- `POST_NOTIFICATIONS` (Android 13+): To show media controls in the notification drawer for the foreground service.
- `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_MEDIA_PLAYBACK`: Required for background media playback capabilities.
