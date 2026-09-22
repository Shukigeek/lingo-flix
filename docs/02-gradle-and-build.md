# 02 - Gradle ובניית Android

## `settings.gradle.kts`

### תפקיד
מגדיר את שם הפרויקט ואת המודולים שבו.

```kotlin
rootProject.name = "lingoFlix"
include(":app")
```

### האם נחוץ?
כן, חובה לבנייה.

### מה להבין
`app` הוא מודול Android אחד בתוך פרויקט Gradle. כאשר מריצים `./gradlew :app:compileDebugKotlin`, מבקשים מ-Gradle לבנות את המודול הזה.

## `build.gradle.kts` בשורש

### תפקיד
מגדיר plugins כלליים עבור תתי-הפרויקטים.

### האם נחוץ?
כן, חובה לבנייה, אבל בדרך כלל לא משנים אותו הרבה.

## `app/build.gradle.kts`

### תפקיד
זה קובץ הבנייה החשוב של האפליקציה. הוא מגדיר:

- `applicationId` - הזהות של האפליקציה.
- `minSdk` - גרסת Android מינימלית.
- `targetSdk` - גרסת Android שאליה מכוונים.
- `versionCode` - מספר פנימי לעדכון.
- `versionName` - המספר שהמשתמש רואה.
- Compose.
- Media3/ExoPlayer.
- Room/KSP.
- Retrofit/OkHttp/Gson.
- ML Kit.
- חתימת release.

### האם נחוץ?
חובה.

### קשר לעדכון האפליקציה
השרת מחזיר `latest_version_code`. האפליקציה משווה אותו ל-`BuildConfig.VERSION_CODE`. אם המספר של השרת גבוה יותר, יש עדכון.

## `gradle/libs.versions.toml`

### תפקיד
מרכז את שמות וגרסאות הספריות.

### האם נחוץ?
חובה, כי `app/build.gradle.kts` משתמש בו.

### מה ללמוד
לא צריך לזכור גרסאות. צריך להבין שכל dependency נותן יכולת לקוד:

- Compose - UI.
- Media3 - וידאו.
- Retrofit - HTTP.
- Room - database מקומי.
- ML Kit - יכולות שפה מקומיות.

## `gradlew` ו-`gradlew.bat`

### תפקיד
מריצים Gradle בצורה זהה במחשבים שונים.

### פקודות חשובות

```bash
./gradlew :app:compileDebugKotlin
./gradlew assembleDebug
```

## `gradle/wrapper/`

מכיל את Gradle Wrapper. לא לערוך ידנית בלי סיבה.

## `gradle.properties`

הגדרות כלליות של Gradle. לא חלק מהלוגיקה העסקית.

## `local.properties`

הגדרות מקומיות של המחשב, בדרך כלל Android SDK. לא משתפים ולא מעלים ל-Git.

## `release.ps1`

סקריפט release ב-PowerShell:

1. קורא גרסה.
2. מעלה `versionCode`.
3. בונה APK חתום.
4. יוצר GitHub Release.

נחוץ רק לפרסום גרסאות. הוא שימושי מאוד למערכת העדכון, אך מיועד בעיקר ל-Windows.

## מה אסור לשכוח

`release.keystore` הוא סוד. לא משתפים אותו ולא מעלים אותו למאגר ציבורי.
