# 13 - אינדקס מלא של כל הקבצים

זהו אינדקס מהיר של כל הקבצים שנמצאו בפרויקט. ההסברים המפורטים נמצאים במסמכים האחרים בתיקייה.

## קבצי שורש

| קובץ | תפקיד | סטטוס |
|---|---|---|
| `README.md` | מבוא והוראות | שימושי |
| `PROJECT_KNOWLEDGE.md` | ידע עזר ל-AI | שימושי |
| `PROJECT_FILE_MAP_HE.md` | מסמך מפה גדול אם קיים | שימושי |
| `index.html` | HTML שאינו חלק ברור מ-Android | לבדיקה |
| `build.gradle.kts` | plugins כלליים | חובה לבנייה |
| `settings.gradle.kts` | מודולים ו-repositories | חובה |
| `gradle.properties` | הגדרות Gradle | חובה לבנייה |
| `gradlew` | Gradle עבור Linux/macOS | חובה לבנייה |
| `gradlew.bat` | Gradle עבור Windows | חובה לבנייה ב-Windows |
| `release.ps1` | בניית release ו-GitHub Release | נחוץ לפרסום |
| `local.properties` | נתיבים מקומיים וסביבה | מקומי, לא לשתף |
| `.gitignore` | קבצים שלא נכנסים ל-Git | חובה לניהול תקין |
| `docs/` | מסמכי הלימוד בעברית | שימושי |

## Gradle

| קובץ | תפקיד | סטטוס |
|---|---|---|
| `gradle/libs.versions.toml` | גרסאות dependencies | חובה |
| `gradle/gradle-daemon-jvm.properties` | הגדרות JVM של Gradle | לא ללמוד כרגע |
| `gradle/wrapper/gradle-wrapper.jar` | Gradle Wrapper | לא לערוך |
| `gradle/wrapper/gradle-wrapper.properties` | גרסת Wrapper | לא לערוך בלי סיבה |

## Android module

| קובץ | תפקיד | סטטוס |
|---|---|---|
| `app/build.gradle.kts` | הגדרות Android ו-dependencies | חובה |
| `app/proguard-rules.pro` | כללי shrink/obfuscation | נחוץ ל-release |
| `app/.gitignore` | התעלמות מקבצים בתוך app | שימושי |
| `app/src/main/AndroidManifest.xml` | Activity והרשאות | חובה |

## Android entry and models

| קובץ | תפקיד | סטטוס |
|---|---|---|
| `MainActivity.kt` | כניסה, ניווט, state ואינטגרציה | חובה |
| `model/Question.kt` | מודל שאלה | נחוץ ל-quiz |
| `model/SubtitleClip.kt` | קטע כתובית | נחוץ לנגן/quiz |
| `model/SubtitleItem.kt` | פריט כתובית ו-parser קשור | נחוץ/לבדוק כפילות |
| `model/SubtitleSegment.kt` | קטע כתובית נוסף | לבדיקה |
| `model/UserProfile.kt` | משתמש מקומי | נחוץ/לבדוק סנכרון |
| `model/VideoMetadata.kt` | metadata לסרטון | נחוץ ל-Room |
| `model/VideoProject.kt` | פרויקט סרטון | נחוץ/לבדיקה |

## Android data

| קובץ | תפקיד | סטטוס |
|---|---|---|
| `data/AppDatabase.kt` | Room database | נחוץ אם Room בשימוש |
| `data/VideoMetadataDao.kt` | פעולות Room | נחוץ אם Room בשימוש |
| `data/VideoRepository.kt` | שכבת גישה לנתוני וידאו | שימושי מאוד |
| `data/UserStatsManager.kt` | XP/streak מקומיים | נחוץ כרגע |
| `data/ServerConnectionManager.kt` | כתובת backend שמורה | נחוץ לעדכונים/שרת ביתי |
| `data/SubtitlesService.kt` | שירות כתוביות Android | חלקי/לבדיקה |

## Android remote/network

| קובץ | תפקיד | סטטוס |
|---|---|---|
| `data/remote/ApiClient.kt` | Retrofit API כללי | נחוץ אם API כללי בשימוש |
| `data/remote/ApiModels.kt` | DTOs ל-API | נחוץ עם LingoApi |
| `data/remote/LingoApi.kt` | endpoints כלליים | נחוץ אם מחובר |
| `data/remote/SessionManager.kt` | token/session | נחוץ ל-auth |
| `data/network/ApiClient.kt` | Retrofit לפי כתובת שרת | נחוץ לעדכונים |
| `data/network/AppApi.kt` | endpoint גרסה | נחוץ לעדכון |
| `data/network/AppVersionResponse.kt` | תשובת גרסה | נחוץ לעדכון |

## Android UI

| קובץ | תפקיד | סטטוס |
|---|---|---|
| `ui/DashboardScreen.kt` | מסך בית | חובה למוצר |
| `ui/DifficultyScreen.kt` | בחירת קושי | נחוץ לזרימה |
| `ui/ExerciseScreen.kt` | תרגול | נחוץ אם מחובר |
| `ui/ExerciseViewModel.kt` | state של תרגול | נחוץ אם המסך משתמש בו |
| `ui/ServerUpdateCard.kt` | שרת ועדכונים | נחוץ לפיצ'ר השרת |
| `ui/SettingsScreen.kt` | הגדרות | נחוץ אם מחובר |
| `ui/VideoListScreen.kt` | ספריית סרטונים | חובה לייבוא/ניהול |
| `ui/VideoPlayerScreen.kt` | נגן וחידון | חובה לתכונה המרכזית |
| `ui/components/CommonDialogs.kt` | dialogs | נחוץ למסכים שמשתמשים בהם |
| `ui/components/LingoComponents.kt` | רכיבי UI חוזרים | שימושי ונחוץ לעיצוב |
| `ui/theme/Color.kt` | צבעים | נחוץ ל-theme |
| `ui/theme/Theme.kt` | theme ראשי | חובה |
| `ui/theme/Type.kt` | typography | נחוץ ל-theme |

## Android utilities

| קובץ | תפקיד | סטטוס |
|---|---|---|
| `util/GameLogic.kt` | לוגיקת משחק | נחוץ אם מחובר |
| `util/SmartCloze.kt` | הסתרת מילים | נחוץ ל-cloze |
| `util/SoundManager.kt` | צלילים | אופציונלי |
| `utils/FileUtils.kt` | קבצים ו-URI | חובה לייבוא |
| `utils/OfflineTranslator.kt` | תרגום offline | אופציונלי |
| `utils/OpenSubtitlesHasher.kt` | hash ל-OpenSubtitles | אופציונלי |
| `utils/SecurityUtils.kt` | אבטחה/הצפנה | נחוץ אם נתונים רגישים |
| `utils/SrtParser.kt` | parsing SRT מקומי | נחוץ אם SRT מקומי |
| `utils/SubtitleGenerator.kt` | יצירת כתוביות | חלקי/לבדיקה |

## Android tests

| קובץ | תפקיד | סטטוס |
|---|---|---|
| `app/src/test/.../ExampleUnitTest.kt` | בדיקת unit בסיסית | תבנית/לבדיקה |
| `app/src/androidTest/.../ExampleInstrumentedTest.kt` | בדיקת מכשיר בסיסית | תבנית/לבדיקה |

## Android resources

| נתיב | תפקיד | סטטוס |
|---|---|---|
| `res/drawable/friends.jpg` | רקע | נחוץ לעיצוב |
| `res/drawable/ic_launcher_background.xml` | רקע אייקון | נחוץ לאייקון |
| `res/drawable/ic_launcher_foreground.xml` | חזית אייקון | נחוץ לאייקון |
| `res/layout/activity_main.xml` | layout XML | לבדוק אם בשימוש |
| `res/raw/correct_sound.wav` | צליל נכון | אופציונלי |
| `res/raw/wrong_sound.wav` | צליל שגוי | אופציונלי |
| `res/values/colors.xml` | צבעי XML | נחוץ אם מפנים אליו |
| `res/values/strings.xml` | טקסטים ו-app name | נחוץ |
| `res/values/themes.xml` | Android theme | נחוץ |
| `res/xml/backup_rules.xml` | גיבוי | נחוץ אם Manifest מפנה |
| `res/xml/data_extraction_rules.xml` | חילוץ נתונים | נחוץ אם Manifest מפנה |
| `res/mipmap-*/*` | אייקונים לגדלי מסך | נחוץ לאייקון |

## Backend core

| קובץ | תפקיד | סטטוס |
|---|---|---|
| `backend/app/__init__.py` | Python package | חובה/שימושי |
| `backend/app/main.py` | יצירת FastAPI ורישום routers | חובה |
| `backend/app/config.py` | configuration | חובה |
| `backend/app/database.py` | SQLAlchemy engine/session | חובה לנתונים |
| `backend/app/deps.py` | dependencies של API | נחוץ |
| `backend/app/models.py` | טבלאות | חובה לנתונים |
| `backend/app/schemas.py` | JSON contracts | חובה ל-API |
| `backend/app/security.py` | password/JWT | חובה ל-auth |
| `backend/app/seed.py` | נתוני התחלה | נחוץ אם seed מופעל |

## Backend routers

| קובץ | תפקיד | סטטוס |
|---|---|---|
| `routers/__init__.py` | Python package | שימושי |
| `routers/auth.py` | auth | נחוץ למשתמשים |
| `routers/ai.py` | AI endpoints | אופציונלי לפי פיצ'רים |
| `routers/app_info.py` | app version | נחוץ לעדכון |
| `routers/common.py` | helpers ל-routers | נחוץ/שימושי |
| `routers/media.py` | מדיה וכתוביות | נחוץ לפיצ'רי מדיה |
| `routers/progress.py` | stats/favorites/progress | נחוץ לסנכרון |
| `routers/quiz.py` | quiz/hearts/review | נחוץ לחידון |
| `routers/sentences.py` | sentences/series/import | נחוץ לבנק משפטים |

## Backend services

| קובץ | תפקיד | סטטוס |
|---|---|---|
| `services/__init__.py` | Python package | שימושי |
| `services/achievements.py` | achievements | אופציונלי-gamification |
| `services/llm.py` | AI providers/cache | אופציונלי/חשוב ל-AI |
| `services/media.py` | yt-dlp/media | נחוץ למדיה חיצונית |
| `services/opensubtitles.py` | OpenSubtitles | אופציונלי |
| `services/quiz_engine.py` | יצירת ובדיקת quiz | נחוץ מאוד |
| `services/srs.py` | spaced repetition | נחוץ ללמידה ארוכת טווח |
| `services/srt.py` | SRT parsing/cleaning | נחוץ מאוד |

## Backend הפעלה ובדיקות

| קובץ | תפקיד | סטטוס |
|---|---|---|
| `backend/requirements.txt` | חבילות Python | חובה להתקנה |
| `backend/.env.example` | תבנית settings | שימושי מאוד |
| `backend/run.sh` | הפעלה מקומית | שימושי מאוד |
| `backend/Dockerfile` | Docker image | נחוץ ל-Docker |
| `backend/docker-compose.yml` | שירות קבוע ו-volume | מומלץ ל-Raspberry Pi |
| `backend/tests/conftest.py` | setup pytest | נחוץ לבדיקות |
| `backend/tests/test_api.py` | בדיקות API | חשוב |
| `backend/tests/test_llm_with_fake_provider.py` | בדיקות AI ללא ספק אמיתי | חשוב |
| `backend/tests/test_srt_and_quiz.py` | בדיקות SRT/quiz | חשוב |

## קבצים סודיים או אוטומטיים

| נתיב | תפקיד | פעולה |
|---|---|---|
| `app/release.keystore` | חתימת APK | לא לשתף |
| `app/src/release.keystore` | חתימת APK נוספת | לא לשתף; לבדוק כפילות |
| `.idea/` | הגדרות Android Studio | לא ללמוד כרגע |
| `.kotlin/errors/` | לוגי compiler | לא קוד מקור |
| `app/build/` | תוצרי build | לא לערוך |
| `.gradle/` | cache/build state | לא לערוך |
| `kspCaches/` | cache של KSP | לא לערוך |
| `.artifacts/` | תוכניות ותוצרי כלי פיתוח | לא נצרך בהרצה |
