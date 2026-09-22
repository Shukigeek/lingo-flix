# 12 - תוכנית לימוד אישית

## מטרה
להגיע למצב שבו אפשר להסביר את הפרויקט בבית הספר, לשנות אותו בביטחון, ולדעת איפה לחפש כשיש תקלה.

## שלב 1 - הבנת מפת הפרויקט

למד:

- `settings.gradle.kts`.
- `app/build.gradle.kts`.
- `AndroidManifest.xml`.
- `backend/app/main.py`.

שאלת בדיקה: מה רץ בטלפון ומה רץ בשרת?

## שלב 2 - Compose

למד:

- `DashboardScreen.kt`.
- `LingoComponents.kt`.
- `DifficultyScreen.kt`.

מושגים:

- `@Composable`.
- `Modifier`.
- `remember`.
- state.
- callback.

## שלב 3 - קבצים וכתוביות

למד:

- `FileUtils.kt`.
- `SrtParser.kt`.
- `backend/app/services/srt.py`.

תרגיל: לקחת SRT קטן ולהסביר איך הוא נהפך לרשימת משפטים.

## שלב 4 - הזרימה המרכזית

למד:

- `VideoListScreen.kt`.
- `VideoPlayerScreen.kt`.
- `SubtitleClip.kt`.
- `GameLogic.kt`.
- `SmartCloze.kt`.

תרגיל: לעקוב אחרי לחיצה על סרטון עד הופעת משפט בחידון.

## שלב 5 - שמירת נתונים

למד:

- `UserStatsManager.kt`.
- `AppDatabase.kt`.
- `VideoMetadataDao.kt`.
- `VideoRepository.kt`.

שאלת בדיקה: מה נשמר בטלפון ומה נשמר בשרת?

## שלב 6 - Backend בסיסי

למד:

- `config.py`.
- `database.py`.
- `models.py`.
- `schemas.py`.
- `security.py`.

מושגים:

- FastAPI.
- endpoint.
- request/response.
- Pydantic schema.
- SQLAlchemy model.
- JWT.

## שלב 7 - חידון ו-SRS

למד:

- `quiz_engine.py`.
- `srs.py`.
- `routers/quiz.py`.
- `routers/progress.py`.

תרגיל: להסביר מה קורה אחרי תשובה נכונה ולאחר תשובה שגויה.

## שלב 8 - Raspberry Pi ועדכון

למד:

- `docker-compose.yml`.
- `Dockerfile`.
- `run.sh`.
- `ServerConnectionManager.kt`.
- `data/network/`.
- `app_info.py`.

תרגיל: להפעיל backend במחשב, לחבר את הטלפון באותה רשת, ולבדוק `/health` ו-`/app/version`.

## איך לקרוא פונקציה

לכל פונקציה שאל:

1. מה נכנס אליה?
2. מה היא משנה?
3. מה היא מחזירה?
4. מי קורא לה?
5. מה קורה אם יש שגיאה?
6. האם היא עושה דבר אחד או יותר מדי דברים?

## איך לבדוק שינוי

1. לשנות דבר קטן.
2. להריץ בדיקה ממוקדת.
3. להריץ build או pytest.
4. לבדוק את הזרימה ידנית.
5. רק אז לבצע שינוי נוסף.

## היעד הסופי

להיות מסוגל להסביר את שלושת המסלולים הבאים:

```text
ייבוא סרטון -> SRT -> נגן
```

```text
משפט -> quiz -> בדיקת תשובה -> XP/progress
```

```text
אפליקציה -> כתובת שרת -> בדיקת גרסה -> הורדת APK
```
