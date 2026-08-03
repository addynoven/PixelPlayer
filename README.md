# NeonPlayer 🎵

<p align="center">
  <img src="assets/icon.png" alt="NeonPlayer Icon" width="140"/>
</p>

<p align="center">
  <strong>A modern, high-performance Online & Offline Music Player for Android</strong><br>
  Originally forked from PixelPlayer, now evolved into a powerful online music player with native YouTube integration.
</p>

<p align="center">
  <a href="https://github.com/addynoven/PixelPlayer/releases/tag/v1.0.0-online">
    <img src="https://img.shields.io/github/v/release/addynoven/PixelPlayer?include_prereleases&logo=github&style=for-the-badge&label=Latest%20Release&color=blue" alt="Latest Release">
  </a>
  <a href="https://github.com/addynoven/PixelPlayer/releases">
    <img src="https://img.shields.io/github/downloads/addynoven/PixelPlayer/total?style=for-the-badge&logo=github&color=brightgreen" alt="Total Downloads">
  </a>
  <img src="https://img.shields.io/badge/Android-11%2B-green?style=for-the-badge&logo=android" alt="Android 11+">
  <img src="https://img.shields.io/badge/Kotlin-100%25-purple?style=for-the-badge&logo=kotlin" alt="Kotlin">
</p>

---

## 📖 The Origin Story & Project Truth

**NeonPlayer** originated as a fork of [PixelPlayer](https://github.com/PixelPlayerHQ/PixelPlayer) (originally created as a local offline music player). 

We transformed it from a purely offline player into a full-featured **Online & Offline Music Ecosystem**. We added direct **YouTube & YouTube Music** streaming engines, smart connectivity handling, multi-source resolution, and custom UI/branding updates.

NeonPlayer is actively maintained and evolved by **neon** ([@addynoven](https://github.com/addynoven)).

---

## ✨ Features

### 🎧 YouTube & Cloud Streaming
- **Native YouTube Music Engine** - Direct audio stream resolution via optimized InnerTube payloads (`ANDROID_SDKLESS`, `IOS`, `TVHTML5`).
- **High-Quality Audio** - Streams high-bitrate WebM/Opus audio with pre-flight HTTP verification checks.
- **Online & Offline Hybrid** - Seamlessly switch between local storage and cloud streaming sources.
- **Telegram & Cloud Drive Integration** - Stream audio directly from connected cloud services.

### 🎨 Modern UI/UX
- **Material You Design** - Dynamic colors that adapt to system theme and album artwork.
- **Smooth Jetpack Compose UI** - Fluid animations, micro-interactions, and custom player controls.
- **Padded Adaptive Icons & Splash Screen** - Clean, rounded, non-cropped branding UI.

### 🎵 Audio & Playback Features
- **Media3 ExoPlayer** - Industry-leading audio engine with FFmpeg integration.
- **Synchronized Lyrics** - Embedded and online LRC lyrics via LRCLIB.
- **Daily Mix & AI Playlists** - Automated recommendations based on listening history.
- **Custom Equalizer & Audio Effects** - Built-in multi-band audio controls.

---

## ⬇️ Downloads & Releases

Get the official **v1.0.0-online** APK binaries directly from GitHub Releases:

<p align="center">
  <a href="https://github.com/addynoven/PixelPlayer/releases/tag/v1.0.0-online">
    <img src="https://raw.githubusercontent.com/Kunzisoft/Github-badge/main/get-it-on-github.png" alt="Get it on GitHub" height="60">
  </a>
</p>

### Download Options:
- [**`app-arm64-v8a-debug.apk`**](https://github.com/addynoven/PixelPlayer/releases/download/v1.0.0-online/app-arm64-v8a-debug.apk) — For modern 64-bit Android devices (recommended)
- [**`app-armeabi-v7a-debug.apk`**](https://github.com/addynoven/PixelPlayer/releases/download/v1.0.0-online/app-armeabi-v7a-debug.apk) — For legacy 32-bit ARM Android devices

---

## 🛠️ Tech Stack

| Component | Technology |
|---|---|
| **Language** | [Kotlin](https://kotlinlang.org/) (100%) |
| **UI Framework** | [Jetpack Compose](https://developer.android.com/jetpack/compose) & Material Design 3 |
| **Audio Engine** | [Media3 ExoPlayer](https://developer.android.com/guide/topics/media/media3) |
| **Online Source** | YouTube InnerTube Client, OkHttp, Retrofit |
| **Architecture** | Feature-driven Modular MVVM with Hilt |
| **Local Storage** | Room Database & DataStore Preferences |

---

## 🤝 Maintainer

**NeonPlayer** is maintained and developed by **neon** ([@addynoven](https://github.com/addynoven)).

---

## 📄 License & Attribution

- **NeonPlayer Modifications**: Maintained by **neon**.
- **Original Base Code**: Forked from PixelPlayer. Portions created prior to May 2026 remain available under the MIT License; see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for full details.

---

<p align="center">
  Made with ❤️ by <a href="https://github.com/addynoven">neon</a>
</p>
