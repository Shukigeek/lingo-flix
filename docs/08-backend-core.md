# 08 - Backend: בסיס והפעלת השרת

## `backend/app/__init__.py`

מסמן את `app` כחבילת Python. אין בו לוגיקה מרכזית.

## `backend/app/main.py`

### תפקיד
נקודת הכניסה של FastAPI:

- יוצר אפליקציה.
- מגדיר lifespan.
- מאתחל database.
- מכניס seed.
- מוודא achievements.
- מוסיף CORS.
- רושם routers.
- מספק `/health`.

### האם נחוץ?
חובה.

### איך לחשוב עליו
הוא לא אמור להכיל את כל הלוגיקה. הוא בעיקר מחבר רכיבים.

## `backend/app/config.py`

### תפקיד
קורא configuration מ-environment או `.env`.

כולל:

- database URL.
- secret key.
- access token lifetime.
- CORS.
- AI keys.
- OpenSubtitles.
- media cache.
- XP.
- app version/update settings.

### נחוץ?
חובה.

### אבטחה
ה-secret האמיתי חייב להיות ארוך ואקראי ולא להישאר ברירת המחדל.

## `backend/app/database.py`

### תפקיד
חיבור SQLAlchemy async:

- Base.
- engine.
- session factory.
- `get_db`.
- `init_db`.
- reset עבור tests.

### נחוץ?
חובה עבור נתונים שנשמרים בשרת.

## `backend/app/deps.py`

### תפקיד
Dependencies משותפים ל-FastAPI:

- `DB`.
- `CurrentUser`.
- `OptionalUser`.

### נחוץ?
כן. הוא מונע שכפול של בדיקת token וחיבור database בכל endpoint.

## `backend/app/models.py`

### תפקיד
טבלאות database:

- User.
- Series.
- Episode.
- Sentence.
- SentenceProgress.
- QuizAttempt.
- Achievement.
- UserAchievement.
- Favorite.
- VideoSource.
- VocabularyItem.
- LlmCache.
- DailyChallenge.
- SelfTestReport.

### נחוץ?
חובה.

### דרך לימוד
קודם `User`, אחר כך `Sentence`, `SentenceProgress`, `QuizAttempt`, ואז הישגים ומדיה.

## `backend/app/schemas.py`

### תפקיד
צורת JSON של ה-API באמצעות Pydantic.

ההבדל:

- `models.py` = איך מידע נשמר ב-database.
- `schemas.py` = איך מידע נכנס ויוצא בבקשות HTTP.

### נחוץ?
חובה עבור API מסודר ובדיקת קלט.

## `backend/app/security.py`

### תפקיד

- hashing passwords.
- verify password.
- create JWT.
- decode JWT.

### נחוץ?
חובה עבור authentication.

## `backend/app/seed.py`

### תפקיד
מכניס נתוני התחלה, כמו achievements או content.

### נחוץ?
נחוץ אם רוצים שרת עם נתוני בסיס.

### מה לבדוק
הרצה חוזרת לא אמורה ליצור כפילויות.

## `backend/app/routers/common.py`

### תפקיד
פונקציות משותפות:

- חישוב level.
- המרת User ל-UserOut.
- עדכון streak.
- איפוס streak.

### נחוץ?
שימושי ונחוץ כדי למנוע קוד כפול.
