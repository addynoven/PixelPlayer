# AGENTS.md — PixelPlayer Agent Guidelines

## 1. Overview & Architecture

**PixelPlayer** is an open-source, feature-rich Android music player built using modern Android standards:
- **Language**: Kotlin (100%)
- **UI Framework**: Jetpack Compose with Material Design 3 (Material You dynamic theming)
- **Audio Engine**: Media3 ExoPlayer + FFmpeg extensions
- **State & DI**: Kotlin StateFlow / SharedFlow, Hilt Dependency Injection
- **Database**: Room Database
- **Integrations**: LRCLIB (Synchronized Lyrics), Deezer API (Artist Artwork), AI Playlists (Gemini, DeepSeek, OpenAI), Remote Providers (GDrive, Jellyfin, Navidrome, Netease, QQ Music, Telegram).

### Core Architectural Philosophy
- **Feature-Driven Modular Architecture**: Group components by feature boundaries (e.g., `gdrive`, `jellyfin`, `navidrome`, `telegram`, `ai`, `lyrics`) rather than monolithic layer-first buckets.
- **Clarity over Cleverness**: Keep implementations straightforward and readable. Avoid premature abstractions and over-engineered patterns.
- **Integration-First Testing**: Test module behavior and workflows end-to-end where possible.

---

## 2. Directory & Module Structure

```
PixelPlayer/
├── app/                      # Main Android Application module
│   └── src/main/java/com/theveloper/pixelplay/
│       ├── data/             # Data sources, Room DB, network clients, background services
│       ├── di/               # Hilt dependency injection modules
│       ├── presentation/     # Compose UI screens, components, ViewModels by feature
│       ├── ui/               # Theme, widgets (Glance), styling tokens
│       └── utils/            # Helper utilities and extensions
├── shared/                   # Shared module for cross-cutting models and data structures
├── wear/                     # Wear OS application module
├── baselineprofile/          # Baseline Profile & Benchmark configuration
├── build.gradle.kts          # Root build configuration
└── settings.gradle.kts       # Gradle module definitions
```

---

## 3. Command Reference

### Build Commands
```bash
# Build Debug APK
./gradlew assembleDebug

# Build Release APK
./gradlew assembleRelease

# Build all modules
./gradlew build
```

### Testing Commands
```bash
# Run unit tests across all modules
./gradlew test

# Run app unit tests specifically
./gradlew :app:testDebugUnitTest

# Run connected Android instrumentation tests
./gradlew connectedAndroidTest
```

### Code Quality & Formatting
```bash
# Run Android Lint
./gradlew lint

# Run Detekt / Ktlint (if configured)
./gradlew check
```

---

## 4. Coding Standards & Best Practices

1. **Feature Scoping**: When adding new features or integrations, co-locate ViewModels, UI screens, repository interfaces, and models under the specific feature directory (e.g., `presentation/<feature_name>/`).
2. **Type Safety & Fail-Fast Validation**: Enforce strict non-null Kotlin types. Validate inputs at module boundaries before invoking underlying business logic.
3. **Structured Error Handling**:
   - Catch specific exceptions rather than generic `Throwable` where possible.
   - Log technical context using structured logging.
   - Return clean, user-friendly error state via `StateFlow` to the UI.
4. **Asynchronous Code & State**: Use Kotlin Coroutines (`Flow`, `StateFlow`) for reactive state management and UI state exposure.
5. **No Premature Abstraction**: Prefer simple, clear code over multi-layered wrapper interfaces unless multiple implementations exist.

---

## 5. Verification Checklist

Before completing any task or bug fix:
- [ ] Code compiles without Gradle errors (`./gradlew assembleDebug`).
- [ ] Relevant unit/integration tests pass (`./gradlew test`).
- [ ] No regression in underlying media player state handling or Room DB schema without proper migrations.
