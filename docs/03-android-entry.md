# 03 - Android ונקודת הכניסה

## `app/src/main/AndroidManifest.xml`

### תפקיד
זהו קובץ הזהות של האפליקציה. הוא קובע:

- איזו Activity נפתחת.
- שם האפליקציה.
- האייקון.
- ה-theme.
- הרשאות.

### הרשאות בפרויקט

- `INTERNET` - חיבור לשרת.
- `READ_MEDIA_VIDEO` - קריאת סרטונים.
- `RECORD_AUDIO` - מיקרופון, אם משתמשים בהקלטה.

### האם נחוץ?
חובה.

## `MainActivity.kt`

### תפקיד
נקודת הכניסה של האפליקציה ומנהל הזרימה הראשי.

### אחריות קיימת

- יצירת Activity.
- הפעלת Compose.
- הפעלת `LingoFlixTheme`.
- Immersive mode.
- ניווט דרך `navigationStack`.
- ניהול הסרטון הנבחר.
- ניהול XP ו-streak.
- ניהול favorites ו-random pool.
- בחירת קבצים.
- פתיחת מסכי האפליקציה.

### האם נחוץ?
חובה.

### למה הוא קשה להבנה
הוא מכיל הרבה אחריות. זה עובד, אבל מקשה על תחזוקה.

### איך לקרוא אותו
לא לקרוא הכל בבת אחת. לחפש לפי סדר:

1. `onCreate` - איך האפליקציה מתחילה.
2. `setContent` - איפה מתחיל Compose.
3. `navigationStack` - איך עוברים מסכים.
4. `pickVideoLauncher` - איך בוחרים קובץ.
5. `when (currentScreen)` - איזה מסך מוצג.
6. callbacks כמו `onVideoSelected` - איך מסך מדבר עם Activity.

### שיפור עתידי
להוציא החוצה:

- Navigation state.
- Import manager.
- Quiz state.
- User statistics.

לכל תחום יכול להיות ViewModel משלו.

## `index.html`

קובץ HTML בשורש שאינו נראה חלק מזרימת Android או FastAPI. צריך לבדוק אם כלי חיצוני משתמש בו. אם לא, הוא מועמד לארכיון או מחיקה לאחר בדיקה.
