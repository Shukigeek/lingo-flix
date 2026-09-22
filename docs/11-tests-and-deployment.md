# 11 - בדיקות, Docker ופריסה

## `backend/tests/conftest.py`

מכין סביבת pytest, database בדיקה ו-client לבקשות API.

### נחוץ?
נחוץ לבדיקות, לא להרצת המשתמשים.

## `backend/tests/test_api.py`

בודק:

- register/login.
- guest/upgrade.
- import SRT.
- practice flow.
- daily challenge.
- favorites.
- videos.
- smart SRT ו-self-test.

### נחוץ?
כן, זה כיסוי ההתנהגות המרכזית של השרת.

## `backend/tests/test_llm_with_fake_provider.py`

בודק AI בלי ספק אמיתי ובלי עלויות.

### נחוץ?
חשוב מאוד לפיתוח יציב.

## `backend/tests/test_srt_and_quiz.py`

בודק parsing של SRT ומנוע quiz.

### נחוץ?
כן, אלה יחידות לוגיקה שקל לבדוק מהר.

## `backend/requirements.txt`

רשימת חבילות Python. חובה להתקנת backend.

## `backend/run.sh`

מריץ backend מקומית:

1. יוצר virtualenv אם אין.
2. מתקין dependencies.
3. מריץ Uvicorn על `0.0.0.0:8000`.

### נחוץ?
שימושי מאוד למחשב בדיקה.

## `backend/Dockerfile`

מתאר image של השרת.

### נחוץ?
כן אם מריצים ב-Docker/Raspberry Pi.

## `backend/docker-compose.yml`

מפעיל את API עם:

- port 8000.
- volume למסד נתונים.
- environment variables.
- `restart: unless-stopped`.

### נחוץ?
מומלץ מאוד לשרת ביתי שתמיד רץ.

## `backend/.env.example`

תבנית למשתני סביבה. לא מכניסים לתוכה סודות אמיתיים.

משתנים חשובים לעדכון:

```text
LINGO_APP_LATEST_VERSION_CODE=10
LINGO_APP_LATEST_VERSION_NAME=2.0
LINGO_APP_DOWNLOAD_URL=...
LINGO_APP_UPDATE_NOTES=...
LINGO_APP_FORCE_UPDATE=false
```

## `app/release.keystore` ו-`app/src/release.keystore`

מפתחות חתימת Android. נחוצים לפרסום עדכונים, אך אסור לשתף או להעלות ל-repository ציבורי.

## `.idea/`

הגדרות Android Studio. שימושיות ל-IDE, לא חלק מלוגיקת המוצר.

## `.artifacts/`

תוכניות ותוצרים של סביבת הפיתוח. לא נצרכים בהרצת האפליקציה.

## `app/build/`, `.gradle/`, `kspCaches/`, `intermediates/`

קבצים שנוצרים אוטומטית. לא לומדים אותם ולא עורכים אותם.

## פקודות אימות

```bash
# Android
JAVA_HOME=/home/sar1/jdk/jdk-17.0.20.1+1 ./gradlew :app:compileDebugKotlin

# Backend
cd backend
.venv/bin/python -m pytest -q

# בדיקת גרסה
curl http://localhost:8000/api/v1/app/version
```
