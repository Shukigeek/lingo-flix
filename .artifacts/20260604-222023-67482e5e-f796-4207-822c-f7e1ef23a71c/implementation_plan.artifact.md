# Implementation Plan - LingoFlix Gamification and UX Enhancements

This plan outlines the steps to make LingoFlix more engaging and fun by adding gamification features, improving quiz mechanics, and redesigning the dashboard.

## Phase 1: Core Fun

### [Animated Answer Feedback (Feature 3.2)]

Improve feedback for correct and wrong answers.

#### [VideoPlayerScreen.kt](file:///C:/Users/shuki/AndroidStudioProjects/lingoFlix/app/src/main/java/com/example/lingoFlix/ui/VideoPlayerScreen.kt)

- Implement shake animation for wrong answers.
- Implement flash (green/red) for sentence card.
- Add floating XP text animation.
- Integrate `SoundManager` for "ding" and "buzzer" sounds.

#### [NEW] [SoundManager.kt](file:///C:/Users/shuki/AndroidStudioProjects/lingoFlix/app/src/main/java/com/example/lingoFlix/util/SoundManager.kt)

- Create a utility for playing sounds using `SoundPool`.

---

### [Combo Multiplier (Feature 1.2)]

Add a combo streak counter to multiply XP.

#### [VideoPlayerScreen.kt](file:///C:/Users/shuki/AndroidStudioProjects/lingoFlix/app/src/main/java/com/example/lingoFlix/ui/VideoPlayerScreen.kt)

- Add `comboCount` state.
- Update `comboCount` on correct/wrong answers.
- Display "🔥 Nx COMBO!" badge with pulse animation.
- Update `onCorrectAnswer` signature to accept `multiplier`.

#### [MainActivity.kt](file:///C:/Users/shuki/AndroidStudioProjects/lingoFlix/app/src/main/java/com/example/lingoFlix/MainActivity.kt)

- Update `onCorrectAnswer` lambda to apply the multiplier to XP.

---

### [Multiple Choice Mode (Feature 2.1)]

Add a new quiz mode using buttons instead of typing.

#### [DifficultySelectionDialog.kt](file:///C:/Users/shuki/AndroidStudioProjects/lingoFlix/app/src/main/java/com/example/lingoFlix/ui/components/CommonDialogs.kt)

- Add "אופן תרגול: הקלדה / בחירה" toggle.

#### [VideoPlayerScreen.kt](file:///C:/Users/shuki/AndroidStudioProjects/lingoFlix/app/src/main/java/com/example/lingoFlix/ui/VideoPlayerScreen.kt)

- Implement logic for "multiple_choice" mode.
- Generate wrong options from other clips.
- Display 4 buttons for answers.

---

### [Lives / Hearts System (Feature 1.1)]

Add a hearts system during quiz sessions.

#### [VideoPlayerScreen.kt](file:///C:/Users/shuki/AndroidStudioProjects/lingoFlix/app/src/main/java/com/example/lingoFlix/ui/VideoPlayerScreen.kt)

- Add `heartsLeft` state (starts at 3).
- Decrement hearts on wrong answers.
- Display heart icons at the top with scale/alpha animations.
- Show "Game Over" dialog when hearts reach 0.

#### [UserStatsManager.kt](file:///C:/Users/shuki/AndroidStudioProjects/lingoFlix/app/src/main/java/com/example/lingoFlix/data/UserStatsManager.kt)

- Add `max_hearts_survived` to SharedPreferences.

---

### [Auto-Advance Option (Feature 5.5)]

Automatically move to the next clip after an answer.

#### [VideoPlayerScreen.kt](file:///C:/Users/shuki/AndroidStudioProjects/lingoFlix/app/src/main/java/com/example/lingoFlix/ui/VideoPlayerScreen.kt)

- Add `isAutoAdvance` toggle in the HUD.
- Use `LaunchedEffect(isChecked)` to delay and advance.

## Verification Plan

### Manual Verification
- Run the app and test each feature in the quiz mode.
- Verify XP calculation with combo multiplier.
- Verify hearts system and game over dialog.
- Test multiple choice mode.
- Check animations and sounds (if resources are added).

### Automated Tests
- Run `./gradlew :app:compileDebugKotlin` to ensure no syntax errors.
