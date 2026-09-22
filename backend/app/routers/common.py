from datetime import date, timedelta

from ..models import User
from ..schemas import UserOut

XP_PER_LEVEL = 1000


def level_of(xp: int) -> int:
    return xp // XP_PER_LEVEL + 1


def user_out(user: User) -> UserOut:
    return UserOut(
        id=user.id,
        email=user.email,
        username=user.username,
        display_name=user.display_name,
        native_language=user.native_language,
        target_language=user.target_language,
        level_cefr=user.level_cefr,
        total_xp=user.total_xp,
        level=level_of(user.total_xp),
        xp_in_level=user.total_xp % XP_PER_LEVEL,
        streak_count=user.streak_count,
        longest_streak=user.longest_streak,
        daily_goal_xp=user.daily_goal_xp,
        hearts=user.hearts,
        created_at=user.created_at,
    )


def touch_streak(user: User, today: date | None = None) -> None:
    """Update the streak for activity today. Idempotent within a day."""
    today = today or date.today()
    last = user.last_activity_date
    if last == today:
        return
    if last == today - timedelta(days=1):
        user.streak_count += 1
    else:
        user.streak_count = 1
    user.longest_streak = max(user.longest_streak, user.streak_count)
    user.last_activity_date = today


def refresh_streak(user: User, today: date | None = None) -> None:
    """Zero the streak if the user skipped a day."""
    today = today or date.today()
    if user.last_activity_date and user.last_activity_date < today - timedelta(days=1):
        user.streak_count = 0
