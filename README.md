# 🎬 LingoFlix - AI Developer Guide

Welcome to the **LingoFlix** project. This document is optimized for AI agents to understand the project structure, features, and technical implementation quickly.

## 🚀 Project Overview
LingoFlix is a language-learning Android app that uses movie subtitles (SRT) to create interactive learning experiences. Users can upload videos, import subtitles, and practice listening/typing through automated quizzes.

---

## 📂 Project Structure (Core Files)

### 📱 UI Components
- **[`MainActivity.kt`](file:///C:/Users/shuki/AndroidStudioProjects/lingoFlix/app/src/main/java/com/example/lingoFlix/MainActivity.kt)**: The brain of the app. Handles navigation, `ExoPlayer` integration, SRT parsing, and the Quiz logic.
- **[`DashboardScreen.kt`](file:///C:/Users/shuki/AndroidStudioProjects/lingoFlix/app/src/main/java/com/example/lingoFlix/ui/DashboardScreen.kt)**: Home screen displaying XP points, Daily Streaks, and main navigation buttons (My Videos, Random Pool, Favorites).
- **[`Theme.kt`](file:///C:/Users/shuki/AndroidStudioProjects/lingoFlix/app/src/main/java/com/example/lingoFlix/ui/theme/Theme.kt)**: Custom "Warm" theme with Orange/Cream color palette and Doodle background logic.

### ⚙️ Data & Logic
- **[`UserStatsManager.kt`](file:///C:/Users/shuki/AndroidStudioProjects/lingoFlix/app/src/main/java/com/example/lingoFlix/data/UserStatsManager.kt)**: Manages persistence of XP points and Daily Streak counts using `SharedPreferences`.
- **[`SubtitleManager.kt`](file:///C:/Users/shuki/AndroidStudioProjects/lingoFlix/app/src/main/java/com/example/lingoFlix/api/SubtitleManager.kt)**: (Internal use) Logic for handling subtitle file paths.

---

## 🛠️ Key Features for Developers

### 1. 🧠 Quiz Logic (`VideoPlayerScreen`)
- **Difficulty Levels**: 
    - `קל`: 1 word hidden.
    - `בינוני`: 40% words hidden.
    - `קשה`: 70% words hidden.
- **XP Rewards**: 20 XP (Easy), 30 XP (Medium), 40 XP (Hard) per correct answer.

### 2. 📝 SRT Parsing (`parseSrtFile`)
- Supports multiple encodings: `UTF-8`, `Windows-1255` (Hebrew), and `ISO-8859-1`.
- Automatically detects Hebrew to set `LocalLayoutDirection` to `Rtl`.

### 3. 💾 State Persistence
- **Video Metadata**: Saved in `lingo_prefs`.
- **User Stats**: Saved in `user_stats`.
- **Folders**: Videos and SRTs are stored in `context.filesDir/videos`.

### 4. 🔄 Navigation & Back Handling
- Uses `BackHandler` to prevent app closing when in sub-screens (Player/List).
- `enableEdgeToEdge` with `safeDrawingPadding` for UI accessibility.

---

## 🧪 Verification Commands
```bash
# Compile and check for errors
./gradlew :app:compileDebugKotlin

# Clean build
./gradlew clean assembleDebug
```

## 📝 Design Principles
- **Warm Aesthetics**: High contrast, orange primary colors, rounded corners (8.dp/16.dp).
- **Interactive Background**: Doodle icons at 4% alpha for texture.
- **AI-First**: All critical logic is kept in high-level composables for easy modification.
