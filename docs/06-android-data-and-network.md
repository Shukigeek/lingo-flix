# 06 - אחסון מקומי ותקשורת עם השרת

## `data/remote/ApiClient.kt`

יוצר Retrofit עבור ה-API הכללי. נחוץ אם האפליקציה שולחת login, quiz, progress או AI לשרת.

## `data/remote/LingoApi.kt`

ממשק Retrofit. כל פונקציה מייצגת endpoint HTTP.

לדוגמה רעיונית:

```kotlin
@POST("auth/login")
suspend fun login(...)
```

## `data/remote/ApiModels.kt`

Data classes שמייצגות JSON של השרת. חייבות להתאים ל-schemas של Python.

## `data/remote/SessionManager.kt`

שומר token/session. צריך לבדוק האם השמירה מוצפנת, איך מתבצע logout ומה קורה כשה-token פג.

## `data/network/AppApi.kt`

ממשק קטן עבור מערכת העדכונים:

```text
GET /api/v1/app/version
```

## `data/network/AppVersionResponse.kt`

מתאר תשובת גרסה:

- `latestVersionCode`.
- `latestVersionName`.
- `downloadUrl`.
- `updateNotes`.
- `forceUpdate`.

## `data/network/ApiClient.kt`

בונה Retrofit לפי כתובת שרת שהמשתמש הזין. הוא נפרד כרגע מה-client הכללי.

### שיפור עתידי
לאחד את שני API clients או ליצור factory אחד שמספק את כל השירותים.

## `data/ServerConnectionManager.kt`

שומר ומנרמל כתובת שרת:

```text
192.168.1.50:8000
 -> http://192.168.1.50:8000/
```

### נחוץ?
כן עבור מחשב בדיקה ו-Raspberry Pi.

### רעיונות לשיפור

- QR code.
- גילוי שרת מקומי.
- כפתור Test connection.
- שמירת כמה שרתים.
- בדיקת `/health` בעת ההתחברות.

## `data/SubtitlesService.kt`

שירות כתוביות בצד Android, אך כרגע נראה חלקי ומכיל TODOs.

### סטטוס
חלקי / לבדיקה. לא למחוק בלי חיפוש שימושים.

## קשרי הנתונים החשובים

```text
Screen
  -> ViewModel / Repository
      -> local files or Room

Screen
  -> Retrofit API
      -> FastAPI Router
          -> Backend Service
              -> Database
```

## בעיה שצריך לפתור בעתיד
הפרויקט מחזיק גם מידע מקומי וגם מידע בשרת. צריך להגדיר לכל נתון:

- איפה הוא נשמר.
- מתי הוא מסתנכרן.
- מה קורה offline.
- מה קורה בהתנגשות בין שני ערכים.
