"""Seed content: a starter catalog and sentence packs so the app is useful on first run.

The packs are original, sitcom-flavoured practice sentences (plus a handful of very
short, widely quoted catchphrases). Users add full episodes by importing their own
SRT files through ``POST /api/v1/sentences/import-srt``.
"""
from __future__ import annotations

from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from .models import Episode, Sentence, Series
from .services import quiz_engine as qe
from .services.srt import normalize

CATALOG: list[dict] = [
    {
        "slug": "friends",
        "title": "Friends",
        "description": "Six friends in New York. Fast, idiomatic everyday English - the classic show for learners. Import your episode SRT files here.",
        "language": "en",
        "genre": "sitcom",
        "difficulty_cefr": "B1",
        "seasons": 10,
        "episodes": [
            {"season": 1, "number": 1, "title": "The One Where Monica Gets a Roommate"},
            {"season": 1, "number": 2, "title": "The One with the Sonogram at the End"},
            {"season": 1, "number": 3, "title": "The One with the Thumb"},
            {"season": 1, "number": 4, "title": "The One with George Stephanopoulos"},
            {"season": 1, "number": 5, "title": "The One with the East German Laundry Detergent"},
            {"season": 2, "number": 1, "title": "The One with Ross's New Girlfriend"},
            {"season": 3, "number": 2, "title": "The One Where No One's Ready"},
            {"season": 5, "number": 16, "title": "The One with the Cop"},
            {"season": 6, "number": 9, "title": "The One Where Ross Got High"},
            {"season": 10, "number": 18, "title": "The Last One"},
        ],
        "catchphrases": [
            "How you doin'?",
            "We were on a break!",
            "Pivot! Pivot! Pivot!",
            "Could I be wearing any more clothes?",
            "Joey doesn't share food!",
            "Oh. My. God.",
            "It's a moo point.",
            "Smelly cat, smelly cat, what are they feeding you?",
        ],
    },
    {
        "slug": "the-office-us",
        "title": "The Office (US)",
        "description": "Workplace comedy with clear, slower conversational English.",
        "language": "en",
        "genre": "mockumentary",
        "difficulty_cefr": "B1",
        "seasons": 9,
        "episodes": [{"season": 1, "number": 1, "title": "Pilot"}, {"season": 2, "number": 1, "title": "The Dundies"}],
        "catchphrases": ["That's what she said.", "I declare bankruptcy!", "Bears. Beets. Battlestar Galactica."],
    },
    {
        "slug": "how-i-met-your-mother",
        "title": "How I Met Your Mother",
        "description": "Fast New York banter, lots of slang and running jokes.",
        "language": "en",
        "genre": "sitcom",
        "difficulty_cefr": "B2",
        "seasons": 9,
        "episodes": [{"season": 1, "number": 1, "title": "Pilot"}],
        "catchphrases": ["Suit up!", "It's gonna be legen... wait for it... dary!", "Challenge accepted."],
    },
    {
        "slug": "brooklyn-nine-nine",
        "title": "Brooklyn Nine-Nine",
        "description": "Police-precinct comedy; quick dialogue, modern vocabulary.",
        "language": "en",
        "genre": "sitcom",
        "difficulty_cefr": "B2",
        "seasons": 8,
        "episodes": [{"season": 1, "number": 1, "title": "Pilot"}],
        "catchphrases": ["Cool cool cool cool cool.", "Title of your sex tape.", "Nine-Nine!"],
    },
    {
        "slug": "la-casa-de-papel",
        "title": "La Casa de Papel",
        "description": "Spanish heist thriller - great for Castilian Spanish listening.",
        "language": "es",
        "genre": "thriller",
        "difficulty_cefr": "B2",
        "seasons": 5,
        "episodes": [{"season": 1, "number": 1, "title": "Efectuar lo acordado"}],
        "catchphrases": [],
    },
    {
        "slug": "dark",
        "title": "Dark",
        "description": "German sci-fi mystery. Slow, deliberate speech - good for German learners.",
        "language": "de",
        "genre": "sci-fi",
        "difficulty_cefr": "B2",
        "seasons": 3,
        "episodes": [{"season": 1, "number": 1, "title": "Geheimnisse"}],
        "catchphrases": [],
    },
    {
        "slug": "lingoflix-coffee-shop",
        "title": "LingoFlix Originals: The Coffee Shop",
        "description": "Original sitcom-style practice lines written for learners. No video needed - use text-to-speech.",
        "language": "en",
        "genre": "practice-pack",
        "difficulty_cefr": "A2",
        "seasons": 1,
        "episodes": [
            {"season": 1, "number": 1, "title": "The One with the Broken Espresso Machine"},
            {"season": 1, "number": 2, "title": "The One Where Everybody Moves Apartments"},
            {"season": 1, "number": 3, "title": "The One with the Terrible Date"},
        ],
        "catchphrases": [],
    },
]

# Original practice lines: (season, episode, speaker, text, translation_he, tags)
COFFEE_SHOP_LINES: list[tuple[int, int, str, str, str, str]] = [
    (1, 1, "Maya", "Could I get a large coffee with no sugar, please?", "אפשר לקבל קפה גדול בלי סוכר, בבקשה?", "ordering,polite"),
    (1, 1, "Dan", "The espresso machine broke again, so everything is going to take a while.", "מכונת האספרסו התקלקלה שוב, אז הכול ייקח זמן.", "apology,present-continuous"),
    (1, 1, "Maya", "Seriously? This is the third time this week!", "ברצינות? זו הפעם השלישית השבוע!", "complaint,emphasis"),
    (1, 1, "Dan", "I know, I know. I already called the repair guy.", "אני יודע, אני יודע. כבר התקשרתי לטכנאי.", "past-simple"),
    (1, 1, "Noa", "Don't worry about it, we can just grab tea instead.", "אל תדאגי, נוכל פשוט לקחת תה במקום.", "suggestion,phrasal-verb"),
    (1, 1, "Maya", "I don't want tea, I want my coffee and I want it now.", "אני לא רוצה תה, אני רוצה את הקפה שלי ואני רוצה אותו עכשיו.", "want,emphasis"),
    (1, 1, "Dan", "Okay, how about a free muffin while you wait?", "אוקיי, מה עם מאפין חינם בזמן שאתם מחכים?", "offer,how-about"),
    (1, 1, "Noa", "Now you're talking! Blueberry, if you have it.", "עכשיו אתה מדבר! אוכמניות, אם יש לך.", "idiom,conditional"),
    (1, 1, "Maya", "Fine, but this doesn't mean I forgive you.", "בסדר, אבל זה לא אומר שאני סולחת לך.", "but,present-simple"),
    (1, 1, "Dan", "I'll take what I can get.", "אני אקח מה שאני יכול לקבל.", "idiom,future"),
    (1, 2, "Eli", "Why did you pack the plates under the books?", "למה ארזת את הצלחות מתחת לספרים?", "past-simple,question"),
    (1, 2, "Noa", "Because the box was already open and I was in a hurry.", "כי הקופסה כבר הייתה פתוחה והייתי בלחץ.", "because,past"),
    (1, 2, "Eli", "Half of them are broken. We'll have to eat off napkins.", "חצי מהן שבורות. נצטרך לאכול על מפיות.", "have-to,future"),
    (1, 2, "Maya", "Could everyone please stop shouting? The neighbors are staring.", "אפשר שכולם יפסיקו לצעוק? השכנים בוהים.", "polite-request,present-continuous"),
    (1, 2, "Dan", "This couch is not going to fit through that door.", "הספה הזאת לא תיכנס דרך הדלת הזאת.", "future,negative"),
    (1, 2, "Eli", "Turn it sideways. No, the other sideways!", "תסובב אותה הצידה. לא, הצד השני!", "imperative"),
    (1, 2, "Noa", "If we lift together on three, it might work.", "אם נרים ביחד בשלוש, זה עלול לעבוד.", "conditional,might"),
    (1, 2, "Dan", "One, two... wait, are we lifting on three or after three?", "אחת, שתיים... רגע, אנחנו מרימים בשלוש או אחרי שלוש?", "question,present-continuous"),
    (1, 2, "Maya", "I honestly can't believe we're friends with these people.", "אני באמת לא מאמינה שאנחנו חברים של האנשים האלה.", "can't-believe,adverb"),
    (1, 2, "Eli", "You say that every single weekend.", "את אומרת את זה כל סוף שבוע.", "present-simple,frequency"),
    (1, 3, "Noa", "So, how was the date with the dentist?", "אז, איך היה הדייט עם רופא השיניים?", "question,past"),
    (1, 3, "Maya", "He spent forty minutes talking about flossing.", "הוא בילה ארבעים דקות בלדבר על חוט דנטלי.", "spend-time,gerund"),
    (1, 3, "Noa", "That's awful. Did he at least pay for dinner?", "זה נורא. לפחות הוא שילם על הארוחה?", "at-least,past"),
    (1, 3, "Maya", "He suggested we split it, and then he checked my teeth.", "הוא הציע שנתחלק, ואז הוא בדק לי את השיניים.", "suggest,past"),
    (1, 3, "Eli", "In his defense, your teeth do look great.", "להגנתו, השיניים שלך באמת נראות מעולה.", "idiom,emphasis-do"),
    (1, 3, "Maya", "I'm never letting you set me up again.", "אני לעולם לא אתן לך לשדך אותי שוב.", "phrasal-verb,future"),
    (1, 3, "Dan", "Hey, at least you got a free cleaning out of it.", "היי, לפחות יצא לך ניקוי שיניים בחינם מזה.", "idiom,get-out-of"),
    (1, 3, "Noa", "Next time, I'm picking someone who can't see inside your mouth.", "בפעם הבאה אני בוחרת מישהו שלא יכול לראות בתוך הפה שלך.", "future-continuous,relative-clause"),
    (1, 3, "Maya", "Next time, I'm staying home with a pizza.", "בפעם הבאה אני נשארת בבית עם פיצה.", "future-plan"),
    (1, 3, "Eli", "Can I come? I'll bring the terrible movie.", "אפשר לבוא? אני אביא את הסרט הנוראי.", "request,future"),
    (1, 1, "Noa", "Whatever happens, we'll figure it out together.", "מה שלא יקרה, נמצא פתרון ביחד.", "phrasal-verb,idiom"),
    (1, 2, "Dan", "I guess we're ordering takeout again tonight.", "אני מניח שאנחנו מזמינים אוכל הביתה שוב הלילה.", "guess,present-continuous"),
    (1, 3, "Eli", "You have to admit, that was kind of hilarious.", "את חייבת להודות, זה היה די מצחיק.", "have-to,admit"),
    (1, 1, "Maya", "I was going to say something nice, but never mind.", "התכוונתי להגיד משהו נחמד, אבל לא חשוב.", "was-going-to,never-mind"),
    (1, 2, "Noa", "Remind me why we didn't hire movers?", "תזכיר לי למה לא שכרנו מובילים?", "remind,past-negative"),
    (1, 3, "Dan", "Apparently he also collects toothbrushes.", "מסתבר שהוא גם אוסף מברשות שיניים.", "apparently,present-simple"),
    (1, 1, "Eli", "It's not a big deal, we can always come back tomorrow.", "זה לא סיפור גדול, תמיד נוכל לחזור מחר.", "idiom,can"),
    (1, 2, "Maya", "Be careful with that lamp, it was my grandmother's.", "תיזהר עם המנורה הזאת, היא הייתה של סבתא שלי.", "imperative,possessive"),
    (1, 3, "Noa", "Trust me, the next one will be better.", "תאמיני לי, הבא יהיה יותר טוב.", "future,comparative"),
    (1, 1, "Dan", "Anyway, the coffee's on the house today.", "בכל מקרה, הקפה היום על חשבון הבית.", "idiom,anyway"),
]


async def seed_all(db: AsyncSession) -> None:
    count = await db.scalar(select(func.count(Series.id))) or 0
    if count > 0:
        return
    for item in CATALOG:
        s = Series(
            slug=item["slug"],
            title=item["title"],
            description=item["description"],
            language=item["language"],
            genre=item["genre"],
            difficulty_cefr=item["difficulty_cefr"],
            seasons=item["seasons"],
            is_public=True,
        )
        db.add(s)
        await db.flush()
        ep_map: dict[tuple[int, int], Episode] = {}
        for ep in item["episodes"]:
            e = Episode(series_id=s.id, season=ep["season"], number=ep["number"], title=ep["title"])
            db.add(e)
            await db.flush()
            ep_map[(ep["season"], ep["number"])] = e
        # Catchphrases go into a special "quotes" episode (season 0)
        if item["catchphrases"]:
            quotes = Episode(series_id=s.id, season=0, number=0, title="Famous quotes")
            db.add(quotes)
            await db.flush()
            for i, q in enumerate(item["catchphrases"]):
                cefr, score = qe.estimate_cefr(q, item["language"])
                db.add(
                    Sentence(
                        episode_id=quotes.id,
                        text=q,
                        normalized=normalize(q),
                        language=item["language"],
                        order_index=i,
                        word_count=len(q.split()),
                        difficulty_score=score,
                        cefr=cefr,
                        tags="quote",
                        is_public=True,
                    )
                )
        if item["slug"] == "lingoflix-coffee-shop":
            for i, (season, number, speaker, text, he, tags) in enumerate(COFFEE_SHOP_LINES):
                e = ep_map[(season, number)]
                cefr, score = qe.estimate_cefr(text, "en")
                db.add(
                    Sentence(
                        episode_id=e.id,
                        text=text,
                        normalized=normalize(text),
                        translation=he,
                        language="en",
                        start_ms=i * 4000,
                        end_ms=i * 4000 + 3500,
                        order_index=i,
                        speaker=speaker,
                        word_count=len(text.split()),
                        difficulty_score=score,
                        cefr=cefr,
                        tags=tags,
                        is_public=True,
                    )
                )
    await db.commit()
