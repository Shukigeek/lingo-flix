# 10 - שירותי Backend

## `services/__init__.py`

מסמן את התיקייה כחבילת Python. אין בו לוגיקה.

## `services/srt.py`

### תפקיד
מנוע SRT טהור:

- parsing של timestamps.
- ניקוי tags ו-sound cues.
- parsing של blocks.
- merge fragments.
- language detection.
- normalization.
- words.
- shift.
- VTT to SRT.

### נחוץ?
נחוץ מאוד.

### למה הוא קובץ לימוד טוב
אין בו HTTP, database או UI. כל פונקציה מקבלת נתונים ומחזירה נתונים.

## `services/quiz_engine.py`

### תפקיד

- tokenization.
- קביעת מילים להחסרה.
- choices.
- hints.
- cloze.
- similarity.
- grading.
- XP.
- combo multiplier.

### נחוץ?
נחוץ מאוד עבור מנגנון החידון.

## `services/srs.py`

### תפקיד
Spaced Repetition: קובע מתי להציג משפט שוב לפי איכות תשובה.

### נחוץ?
נחוץ ללמידה ארוכת טווח.

## `services/achievements.py`

### תפקיד
מחשב metrics ופותח achievements.

### נחוץ?
נחוץ ל-gamification, לא להפעלה הבסיסית.

## `services/llm.py`

### תפקיד
שכבת AI עם Anthropic, Gemini ו-fallback היוריסטי.

מבצעת:

- smart cloze.
- explanations.
- translation.
- rating.
- solving.
- quality judgment.
- self-test.
- cache.

### נחוץ?
אופציונלי למוצר בסיסי, חשוב לגרסת AI.

### יתרון
אפשר לבדוק עם heuristic או fake provider בלי API key אמיתי.

## `services/media.py`

### תפקיד
עבודה עם YouTube/yt-dlp ומדיה חיצונית:

- זיהוי URL.
- בחירת format.
- info.
- כתוביות.

### נחוץ?
נחוץ אם המוצר מקבל מדיה מהאינטרנט.

## `services/opensubtitles.py`

### תפקיד
חיפוש והורדת כתוביות מ-OpenSubtitles.

### נחוץ?
אופציונלי. תלוי ב-API key ובשירות החיצוני.

## קשרים חשובים

```text
srt.py -> sentences.py / media.py
quiz_engine.py -> quiz.py
srs.py -> quiz.py / progress.py
llm.py -> ai.py / quiz_engine.py
achievements.py -> main.py / progress.py
media.py -> media.py router
opensubtitles.py -> media.py router
```
