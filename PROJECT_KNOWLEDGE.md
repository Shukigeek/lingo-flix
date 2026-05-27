# 🎬 LingoFlix Project Knowledge Base

This document serves as a persistent guide for AI assistants to understand the architecture, UI/UX flow, and critical logic of the LingoFlix project.

## 🏛️ Project Architecture
- **Tech Stack**: Kotlin, Jetpack Compose, Media3 (ExoPlayer), Room (planned/partially integrated), Coroutines.
- **Navigation**: Managed via a `currentScreen` state string in `MainActivity.kt`. No complex navigation libraries to keep it lightweight.

## 📱 Screen Flows
1. **Dashboard (`DashboardScreen.kt`)**: 
   - Unified UI with a semi-transparent white background over the `friends.jpg` app background.
   - Shows Top 3 **actual** recent videos (sorted by `lastModified`).
   - Quick access to "My Library", "Random Sentences", and "Favorites".
2. **Video Library (`VideoListScreen.kt`)**: 
   - Directory-based file system (videos stored in internal storage under `videos/`).
   - Long-press or Gear icon opens options: Rename, Move, Search Subtitles (Web), Import SRT (Local), AI Subtitles.
   - Swipe (Slide) to delete or quick practice.
3. **Difficulty Selection (`DifficultyScreen.kt`)**:
   - Intermediate screen between video selection and the player.
   - Offers "Regular View" (just watching) vs "Quiz Mode" (Easy, Medium, Hard).
4. **Video Player (`VideoPlayerScreen.kt`)**:
   - Multi-mode: Single video, Random Pool (across all shared videos), or Favorites only.
   - **Quiz Logic**: Splits text by words/punctuation and masks words based on difficulty.
   - **Progress**: Saves sentence index per video filename to resume later.
   - **Offline STT**: Mic button uses `RecognizerIntent` with `EXTRA_PREFER_OFFLINE`.

## 🛠️ Critical Logic & Data
- **Subtitle Parsing (`SrtParser.kt`)**: 
   - Handles multi-encoding (UTF-8, Windows-1255 for Hebrew, Windows-1252 for Spanish).
   - `detectSubtitleLanguage` is used to set the audio language and voice input language.
- **Favorites Mechanism**: 
   - Clips are identified by `filename|startTimeMs`. 
   - In mixed modes (Random/Favs), the `clipId` must be derived from the specific clip's source URI, not the global `videoUri`.
- **Background Persistence**: 
   - `favorite_clips` and `linked_videos` (for random pool) are stored in `SharedPreferences`.

## 🎨 Design Principles (User Preferences)
- **Background**: Full-screen semi-transparent `friends.jpg`.
- **Colors**: Vibrant Duolingo-style colors (DuoBlue, DuoGreen).
- **Layout**: Cohesive blocks rather than separate boxes. Minimize vertical space to avoid scrolling on the Dashboard.
- **Feedback**: Temporary overlays (like "No Subtitles" for 5s) instead of persistent text.
- **Buttons**: Custom `DuoButton` with shadow/depth effect.

## 📂 Key Paths
- **Videos/SRTs**: `context.filesDir/videos/`
- **Background Image**: `res/drawable/friends.jpg`
- **Utility Components**: `ui/components/LingoComponents.kt`
