"""Achievement definitions and unlock logic."""
from __future__ import annotations

from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from ..models import Achievement, QuizAttempt, SentenceProgress, User, UserAchievement

DEFINITIONS: list[dict] = [
    {"code": "first_steps", "title": "צעדים ראשונים", "description": "ענית על השאלה הראשונה שלך", "icon": "👶", "xp_reward": 20, "metric": "attempts", "threshold": 1},
    {"code": "ten_correct", "title": "מתחמם", "description": "10 תשובות נכונות", "icon": "🔥", "xp_reward": 50, "metric": "correct", "threshold": 10},
    {"code": "hundred_correct", "title": "מאה!", "description": "100 תשובות נכונות", "icon": "💯", "xp_reward": 200, "metric": "correct", "threshold": 100},
    {"code": "five_hundred_correct", "title": "מכונה", "description": "500 תשובות נכונות", "icon": "🤖", "xp_reward": 500, "metric": "correct", "threshold": 500},
    {"code": "streak_3", "title": "רצף של 3", "description": "תרגלת 3 ימים ברצף", "icon": "📅", "xp_reward": 60, "metric": "streak", "threshold": 3},
    {"code": "streak_7", "title": "שבוע שלם", "description": "תרגלת 7 ימים ברצף", "icon": "🗓️", "xp_reward": 150, "metric": "streak", "threshold": 7},
    {"code": "streak_30", "title": "חודש של אש", "description": "30 ימים ברצף", "icon": "🌋", "xp_reward": 600, "metric": "streak", "threshold": 30},
    {"code": "xp_1000", "title": "רמה 2", "description": "צברת 1000 XP", "icon": "⭐", "xp_reward": 100, "metric": "xp", "threshold": 1000},
    {"code": "xp_10000", "title": "אלוף", "description": "צברת 10,000 XP", "icon": "🏆", "xp_reward": 500, "metric": "xp", "threshold": 10000},
    {"code": "combo_5", "title": "קומבו x5", "description": "5 תשובות נכונות ברצף", "icon": "⚡", "xp_reward": 40, "metric": "combo", "threshold": 5},
    {"code": "combo_10", "title": "קומבו x10", "description": "10 תשובות נכונות ברצף", "icon": "🌟", "xp_reward": 120, "metric": "combo", "threshold": 10},
    {"code": "sentences_50", "title": "אספן משפטים", "description": "תרגלת 50 משפטים שונים", "icon": "📚", "xp_reward": 100, "metric": "sentences", "threshold": 50},
    {"code": "mastered_25", "title": "שולט בחומר", "description": "25 משפטים במאסטרי מלא", "icon": "🎓", "xp_reward": 250, "metric": "mastered", "threshold": 25},
    {"code": "hard_mode_10", "title": "קשוח", "description": "10 תשובות נכונות ברמה קשה", "icon": "💪", "xp_reward": 120, "metric": "hard_correct", "threshold": 10},
    {"code": "night_owl", "title": "ינשוף לילה", "description": "תרגלת אחרי חצות", "icon": "🦉", "xp_reward": 30, "metric": "night", "threshold": 1},
]


async def ensure_definitions(db: AsyncSession) -> None:
    existing = {a.code for a in (await db.scalars(select(Achievement))).all()}
    added = False
    for d in DEFINITIONS:
        if d["code"] not in existing:
            db.add(Achievement(**d))
            added = True
    if added:
        await db.commit()


async def compute_metrics(db: AsyncSession, user: User, last_combo: int = 0, last_hour: int | None = None) -> dict[str, int]:
    attempts = await db.scalar(select(func.count(QuizAttempt.id)).where(QuizAttempt.user_id == user.id)) or 0
    correct = await db.scalar(select(func.count(QuizAttempt.id)).where(QuizAttempt.user_id == user.id, QuizAttempt.is_correct.is_(True))) or 0
    hard_correct = (
        await db.scalar(
            select(func.count(QuizAttempt.id)).where(
                QuizAttempt.user_id == user.id, QuizAttempt.is_correct.is_(True), QuizAttempt.difficulty == "hard"
            )
        )
        or 0
    )
    sentences = await db.scalar(select(func.count(SentenceProgress.id)).where(SentenceProgress.user_id == user.id)) or 0
    mastered = await db.scalar(select(func.count(SentenceProgress.id)).where(SentenceProgress.user_id == user.id, SentenceProgress.mastery >= 0.9)) or 0
    best_combo = await db.scalar(select(func.max(QuizAttempt.combo)).where(QuizAttempt.user_id == user.id)) or 0
    return {
        "attempts": attempts,
        "correct": correct,
        "hard_correct": hard_correct,
        "streak": user.streak_count,
        "xp": user.total_xp,
        "combo": max(best_combo, last_combo),
        "sentences": sentences,
        "mastered": mastered,
        "night": 1 if last_hour is not None and (last_hour >= 0 and last_hour < 4) else 0,
    }


async def check_and_unlock(db: AsyncSession, user: User, metrics: dict[str, int]) -> list[Achievement]:
    await ensure_definitions(db)
    all_defs = (await db.scalars(select(Achievement))).all()
    unlocked_ids = {ua.achievement_id for ua in (await db.scalars(select(UserAchievement).where(UserAchievement.user_id == user.id))).all()}
    new: list[Achievement] = []
    for a in all_defs:
        if a.id in unlocked_ids:
            continue
        value = metrics.get(a.metric, 0)
        if value >= a.threshold:
            db.add(UserAchievement(user_id=user.id, achievement_id=a.id))
            user.total_xp += a.xp_reward
            new.append(a)
    if new:
        await db.commit()
    return new
