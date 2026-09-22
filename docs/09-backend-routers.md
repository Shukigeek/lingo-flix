# 09 - Backend API ו-Routers

## מהו Router?

Router הוא קבוצת כתובות HTTP הקשורות לתחום אחד.

```text
POST /api/v1/auth/login
GET  /api/v1/progress/stats
POST /api/v1/quiz/check
```

## `routers/__init__.py`

חבילת Python. אין בו לוגיקה.

## `routers/auth.py`

### תפקיד

- הרשמה.
- login.
- guest.
- `/me`.
- עדכון משתמש.
- upgrade מ-guest לחשבון.

### תלוי ב
`security.py`, `models.py`, `schemas.py`, `deps.py`.

### נחוץ?
כן אם יש משתמשים.

## `routers/sentences.py`

### תפקיד

- סדרות.
- פרקים.
- משפטים.
- import SRT.
- daily challenge.
- השלמת daily challenge.

### נחוץ?
כן עבור בנק משפטים ותרגול.

## `routers/quiz.py`

### תפקיד

- יצירת quiz.
- שאלות batch.
- בדיקת תשובות.
- hearts refill.
- שאלות due לחזרה.
- ספירת review.

### נחוץ?
כן עבור quiz בצד השרת.

## `routers/progress.py`

### תפקיד

- stats.
- achievements.
- leaderboard.
- favorites.
- vocabulary.
- videos.
- progress של video.

### נחוץ?
כן עבור שמירת התקדמות וסנכרון.

## `routers/media.py`

### תפקיד

- resolve media.
- YouTube info.
- YouTube subtitles.
- OpenSubtitles search/download.
- SRT shift.
- SRT validate.

### נחוץ?
רק לפיצ'רי מדיה וכתוביות חיצוניות, אך חשוב למוצר המלא.

## `routers/ai.py`

### תפקיד

- AI status.
- הסבר.
- smart SRT.
- self-test.
- היסטוריית self-test.

### נחוץ?
אופציונלי לפעולה הבסיסית, נחוץ לפיצ'רי AI.

## `routers/app_info.py`

### תפקיד

```text
GET /api/v1/app/version
```

מחזיר גרסה, קישור הורדה, הערות ועדכון כפוי.

### נחוץ?
כן עבור מערכת העדכון מול Raspberry Pi.

## `routers/common.py`

פונקציות עזר לכל ה-routers. לא router שמקבל בקשות בעצמו.

## איך לקרוא Router

לכל endpoint שאל:

1. מה ה-URL?
2. האם הוא GET/POST/PATCH/DELETE?
3. איזה schema נכנס?
4. האם צריך משתמש?
5. איזה service או model הוא מפעיל?
6. מה מוחזר ללקוח?

## זרימת דוגמה: בדיקת תשובה

```text
Android שולח POST /quiz/check
 -> quiz.py מקבל AnswerCheckRequest
 -> deps מאמת משתמש
 -> quiz_engine.grade בודק תשובה
 -> srs מעדכן חזרה עתידית
 -> progress/achievements מתעדכנים
 -> AnswerCheckResult חוזר ל-Android
```
