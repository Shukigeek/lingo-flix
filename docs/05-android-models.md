# 05 - מודלים ונתונים באפליקציה

## מהו model?

Model הוא תיאור של מידע. הוא בדרך כלל לא מציג מסך ולא שולח בקשה, אלא מגדיר איך נתונים נראים.

## `model/UserProfile.kt`

מתאר משתמש מקומי. נחוץ אם מסכים משתמשים בו. יש לבדוק איך הוא קשור למשתמש שב-backend.

## `model/Question.kt`

מתאר שאלה בחידון. נחוץ למסך או מנוע שמייצרים שאלות מקומית.

## `model/SubtitleClip.kt`

מתאר קטע כתובית עם טקסט וזמני התחלה/סיום. נחוץ לנגן, לתרגול ולמועדפים.

## `model/SubtitleItem.kt`

מתאר פריט כתובית. בקובץ נמצא גם קוד parser, ולכן הוא לא רק model טהור.

### סימן שאלה
קיים גם `utils/SrtParser.kt`. צריך לבדוק אם יש כפילות.

## `model/SubtitleSegment.kt`

מתאר קטע כתובית נוסף. נחוץ רק אם מסכים או תרגילים משתמשים בו.

## `model/VideoMetadata.kt`

מתאר metadata של סרטון. קשור ל-Room ול-`VideoMetadataDao`.

## `model/VideoProject.kt`

מתאר פרויקט וידאו, כנראה סרטון יחד עם כתוביות ומידע תרגול.

## `data/AppDatabase.kt`

### תפקיד
מסד נתונים מקומי Room.

### נחוץ?
כן אם metadata נשמר ב-Room.

### מה ללמוד

- Entity.
- Database.
- DAO.
- Singleton database.

## `data/VideoMetadataDao.kt`

ממשק פעולות על טבלת metadata. בדרך כלל כולל insert, update, delete ו-query.

## `data/VideoRepository.kt`

שכבת תיווך בין UI לבין אחסון. זה המקום שבו מסך אמור לבקש רשימת סרטונים בלי לדעת את כל פרטי הקבצים.

### מצב אידיאלי
```text
Screen -> ViewModel -> Repository -> DAO/File system
```

### מצב שצריך לבדוק
אם מסכים קוראים ישירות ל-`File`, ה-Repository עדיין לא משמש כשכבת תיווך מלאה.

## `data/UserStatsManager.kt`

שומר XP, level, streak ותאריך פעילות ב-SharedPreferences.

### נחוץ?
כן כל עוד ה-XP המקומי מוצג.

### סיכון
קיים גם User ב-backend. צריך לקבוע מי מקור האמת כדי לא ליצור XP שונה בטלפון ובשרת.
