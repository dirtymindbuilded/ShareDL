# ShareDL

A small Android share-target downloader inspired by Seal's workflow and built on the same `youtubedl-android` / yt-dlp stack.

## Features

- Appears in Android's Share sheet for `text/plain` links.
- Accepts YouTube, Instagram, TikTok, and other URLs supported by yt-dlp.
- Quality picker: Best, 1080p, 720p, 480p, 360p, MP3 audio.
- Downloads in a foreground service with progress notification.
- Saves files under `Download/ShareDL`.
- Also supports manually pasting a URL.

## Build

1. Open this folder in Android Studio.
2. Let Gradle sync and download dependencies.
3. Build > Build APK(s).
4. Install the APK on Android 8+.
5. In YouTube / Instagram / TikTok, tap Share and choose **ShareDL**.

The project intentionally does **not** use Seal's app name, logo, package ID, or pretend to be an official Seal build. Seal's source is GPLv3, while its README separately restricts using the Seal name for derivatives.

Use this only for media you have permission to download and in accordance with the relevant platform rules and applicable law.

## GitHub Actions APK build

Push the project to GitHub. The included `.github/workflows/android.yml` runs on pushes to `main`/`master` or manually from **Actions > Build Android APK > Run workflow**. Download the resulting `ShareDL-debug-apk` artifact.
