# 04 - מסכי Android ו-Compose

## מהו Compose?

במקום XML נפרד לכל מסך, כותבים פונקציות Kotlin שמייצרות UI:

```kotlin
@Composable
fun MyScreen() { ... }
```

כאשר state משתנה, Compose מצייר מחדש את החלק הרלוונטי.

## `DashboardScreen.kt`

### תפקיד
מסך הבית:

- משתמש.
- XP ורמה.
- סרטונים אחרונים.
- קיצורי דרך.
- סטטיסטיקות.
- `ServerUpdateCard`.

### נחוץ?
כן, מסך פתיחה.

### מה ללמוד
`@Composable`, `remember`, `LazyColumn`, callbacks ו-`Modifier`.

## `VideoListScreen.kt`

### תפקיד
ספריית סרטונים:

- הצגת סרטונים.
- ייבוא וידאו.
- ייבוא SRT.
- מחיקה.
- שינוי metadata.
- חיפוש כתוביות.
- פתיחת נגן.

### נחוץ?
כן, חלק מרכזי בזרימת המשתמש.

### שיפור
המסך גדול ומכיל UI ולוגיקה. כדאי להעביר פעולות קבצים ל-Repository/ViewModel.

## `DifficultyScreen.kt`

### תפקיד
בחירת צפייה רגילה, quiz, קושי וסוג תרגול.

### נחוץ?
כן, אם זה המסך שמגיע לפני הנגן.

## `VideoPlayerScreen.kt`

### תפקיד
המסך המרכזי:

- ניגון Media3.
- סנכרון כתוביות לפי position.
- מצב צפייה.
- מצב quiz.
- בדיקת תשובות.
- מועדפים.
- XP, combo, hearts ופידבק.

### נחוץ?
כן, זה הלב של האפליקציה.

### איך ללמוד
לחלק לחמישה מסלולים:

1. יצירת player.
2. קבלת הזמן הנוכחי.
3. מציאת subtitle מתאים.
4. בניית השאלה.
5. טיפול בתשובה.

## `ExerciseScreen.kt`

מסך תרגול נפרד. צריך לבדוק אם `MainActivity` באמת מציג אותו. אם לא, ייתכן שהוא מסך ישן או פיצ'ר חלקי.

## `ExerciseViewModel.kt`

ViewModel שמחזיק state ולוגיקה עבור `ExerciseScreen`. נחוץ אם המסך משתמש בו.

## `SettingsScreen.kt`

מסך הגדרות. נחוץ אם מחובר לניווט. מקום מתאים בעתיד להגדרות שרת, שפה, צלילים והעדפות.

## `ServerUpdateCard.kt`

### תפקיד
מאפשר:

1. הזנת כתובת שרת.
2. שמירת הכתובת.
3. בדיקת גרסה.
4. פתיחת APK חדש בדפדפן.

### נחוץ?
כן עבור Raspberry Pi ועדכונים.

### מגבלה
הוא לא מתקין APK אוטומטית. הוא פותח קישור ומעביר את האישור ל-Android.

## `ui/components/LingoComponents.kt`

רכיבי UI חוזרים:

- `DuoButton`.
- progress bar.
- feedback banner.
- confetti.
- word tile.

נחוץ כדי למנוע העתקת עיצוב בין מסכים.

## `ui/components/CommonDialogs.kt`

דיאלוגים משותפים, כולל בחירת קושי וסוג תרגול ו-X-Ray.

## `ui/theme/Color.kt`

צבעי Compose.

## `ui/theme/Type.kt`

Typography.

## `ui/theme/Theme.kt`

`LingoFlixTheme`, שמקיף את כל האפליקציה ומגדיר צבעים, typography ו-theme.

כל שלושת קבצי ה-theme נחוצים כל עוד `MainActivity` משתמש בהם.
