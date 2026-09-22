from datetime import date, datetime, timedelta

from fastapi import APIRouter, HTTPException, status
from sqlalchemy import func, select

from ..deps import DB, CurrentUser
from ..models import Achievement, Favorite, QuizAttempt, SentenceProgress, User, UserAchievement, VideoSource, VocabularyItem
from ..schemas import (
    AchievementOut,
    DayXP,
    FavoriteIn,
    FavoriteOut,
    LeaderboardEntry,
    ProgressUpdate,
    StatsOut,
    VideoSourceIn,
    VideoSourceOut,
    VocabularyOut,
)
from ..services import achievements as ach
from .common import level_of, refresh_streak

router = APIRouter(tags=["progress"])


@router.get("/stats", response_model=StatsOut)
async def stats(user: CurrentUser, db: DB, days: int = 14):
    refresh_streak(user)
    today = date.today()
    start = datetime.combine(today - timedelta(days=days - 1), datetime.min.time())
    rows = (
        await db.execute(
            select(func.date(QuizAttempt.created_at), func.sum(QuizAttempt.xp_awarded), func.count(QuizAttempt.id))
            .where(QuizAttempt.user_id == user.id, QuizAttempt.created_at >= start)
            .group_by(func.date(QuizAttempt.created_at))
        )
    ).all()
    by_day = {str(r[0]): (int(r[1] or 0), int(r[2] or 0)) for r in rows}
    series = []
    for i in range(days):
        d = today - timedelta(days=days - 1 - i)
        xp, n = by_day.get(d.isoformat(), (0, 0))
        series.append(DayXP(day=d, xp=xp, attempts=n))
    today_xp = by_day.get(today.isoformat(), (0, 0))[0]

    total_attempts = await db.scalar(select(func.count(QuizAttempt.id)).where(QuizAttempt.user_id == user.id)) or 0
    correct = await db.scalar(select(func.count(QuizAttempt.id)).where(QuizAttempt.user_id == user.id, QuizAttempt.is_correct.is_(True))) or 0
    seen = await db.scalar(select(func.count(SentenceProgress.id)).where(SentenceProgress.user_id == user.id)) or 0
    mastered = await db.scalar(select(func.count(SentenceProgress.id)).where(SentenceProgress.user_id == user.id, SentenceProgress.mastery >= 0.9)) or 0
    due = await db.scalar(select(func.count(SentenceProgress.id)).where(SentenceProgress.user_id == user.id, SentenceProgress.due_at <= datetime.utcnow())) or 0
    vocab = await db.scalar(select(func.count(VocabularyItem.id)).where(VocabularyItem.user_id == user.id)) or 0
    best_combo = await db.scalar(select(func.max(QuizAttempt.combo)).where(QuizAttempt.user_id == user.id)) or 0

    acc: dict[str, float] = {}
    for diff in ("easy", "medium", "hard"):
        n = await db.scalar(select(func.count(QuizAttempt.id)).where(QuizAttempt.user_id == user.id, QuizAttempt.difficulty == diff)) or 0
        c = await db.scalar(select(func.count(QuizAttempt.id)).where(QuizAttempt.user_id == user.id, QuizAttempt.difficulty == diff, QuizAttempt.is_correct.is_(True))) or 0
        acc[diff] = round(c / n, 3) if n else 0.0
    await db.commit()
    return StatsOut(
        total_xp=user.total_xp,
        level=level_of(user.total_xp),
        xp_in_level=user.total_xp % 1000,
        streak_count=user.streak_count,
        longest_streak=user.longest_streak,
        today_xp=today_xp,
        daily_goal_xp=user.daily_goal_xp,
        daily_goal_reached=today_xp >= user.daily_goal_xp,
        total_attempts=total_attempts,
        correct_attempts=correct,
        accuracy=round(correct / total_attempts, 3) if total_attempts else 0.0,
        sentences_seen=seen,
        sentences_mastered=mastered,
        due_reviews=due,
        vocabulary_size=vocab,
        best_combo=best_combo,
        xp_by_day=series,
        accuracy_by_difficulty=acc,
    )


@router.get("/achievements", response_model=list[AchievementOut])
async def achievements(user: CurrentUser, db: DB):
    await ach.ensure_definitions(db)
    metrics = await ach.compute_metrics(db, user)
    defs = (await db.scalars(select(Achievement).order_by(Achievement.threshold))).all()
    unlocked = {ua.achievement_id: ua.unlocked_at for ua in (await db.scalars(select(UserAchievement).where(UserAchievement.user_id == user.id))).all()}
    out = []
    for a in defs:
        o = AchievementOut.model_validate(a)
        o.unlocked_at = unlocked.get(a.id)
        o.progress = min(metrics.get(a.metric, 0), a.threshold)
        o.threshold = a.threshold
        out.append(o)
    return out


@router.get("/leaderboard", response_model=list[LeaderboardEntry])
async def leaderboard(user: CurrentUser, db: DB, limit: int = 50, weekly: bool = False):
    if weekly:
        since = datetime.utcnow() - timedelta(days=7)
        rows = (
            await db.execute(
                select(User, func.coalesce(func.sum(QuizAttempt.xp_awarded), 0).label("xp"))
                .join(QuizAttempt, QuizAttempt.user_id == User.id, isouter=True)
                .where(User.is_active.is_(True), (QuizAttempt.created_at >= since) | (QuizAttempt.id.is_(None)))
                .group_by(User.id)
                .order_by(func.coalesce(func.sum(QuizAttempt.xp_awarded), 0).desc())
                .limit(limit)
            )
        ).all()
        entries = [(u, int(xp)) for u, xp in rows]
    else:
        users = (await db.scalars(select(User).where(User.is_active.is_(True)).order_by(User.total_xp.desc()).limit(limit))).all()
        entries = [(u, u.total_xp) for u in users]
    out = []
    for i, (u, xp) in enumerate(entries, start=1):
        out.append(
            LeaderboardEntry(
                rank=i, user_id=u.id, username=u.username, display_name=u.display_name, total_xp=xp, streak_count=u.streak_count, level=level_of(u.total_xp), is_me=u.id == user.id
            )
        )
    if not any(e.is_me for e in out):
        rank = (await db.scalar(select(func.count(User.id)).where(User.total_xp > user.total_xp)) or 0) + 1
        out.append(LeaderboardEntry(rank=rank, user_id=user.id, username=user.username, display_name=user.display_name, total_xp=user.total_xp, streak_count=user.streak_count, level=level_of(user.total_xp), is_me=True))
    return out


# ---------- Favorites ----------
@router.get("/favorites", response_model=list[FavoriteOut])
async def list_favorites(user: CurrentUser, db: DB):
    rows = (await db.scalars(select(Favorite).where(Favorite.user_id == user.id).order_by(Favorite.created_at.desc()))).all()
    return [FavoriteOut.model_validate(r) for r in rows]


@router.put("/favorites", response_model=FavoriteOut)
async def add_favorite(body: FavoriteIn, user: CurrentUser, db: DB):
    f = await db.scalar(select(Favorite).where(Favorite.user_id == user.id, Favorite.clip_id == body.clip_id))
    if f is None:
        f = Favorite(user_id=user.id, **body.model_dump())
        db.add(f)
    else:
        f.text = body.text
        f.translation = body.translation
    await db.commit()
    await db.refresh(f)
    return FavoriteOut.model_validate(f)


@router.delete("/favorites/{clip_id:path}", status_code=204)
async def remove_favorite(clip_id: str, user: CurrentUser, db: DB):
    f = await db.scalar(select(Favorite).where(Favorite.user_id == user.id, Favorite.clip_id == clip_id))
    if f:
        await db.delete(f)
        await db.commit()


@router.post("/favorites/sync", response_model=list[FavoriteOut])
async def sync_favorites(items: list[FavoriteIn], user: CurrentUser, db: DB):
    """Merge a device's favorite list with the server (union)."""
    existing = {f.clip_id: f for f in (await db.scalars(select(Favorite).where(Favorite.user_id == user.id))).all()}
    for it in items:
        if it.clip_id not in existing:
            f = Favorite(user_id=user.id, **it.model_dump())
            db.add(f)
            existing[it.clip_id] = f
    await db.commit()
    rows = (await db.scalars(select(Favorite).where(Favorite.user_id == user.id).order_by(Favorite.created_at.desc()))).all()
    return [FavoriteOut.model_validate(r) for r in rows]


# ---------- Vocabulary ----------
@router.get("/vocabulary", response_model=list[VocabularyOut])
async def vocabulary(user: CurrentUser, db: DB, weak_only: bool = False, limit: int = 200):
    q = select(VocabularyItem).where(VocabularyItem.user_id == user.id)
    if weak_only:
        q = q.where(VocabularyItem.mastery < 0.6)
    rows = (await db.scalars(q.order_by(VocabularyItem.times_missed.desc(), VocabularyItem.updated_at.desc()).limit(limit))).all()
    return [VocabularyOut.model_validate(r) for r in rows]


@router.delete("/vocabulary/{item_id}", status_code=204)
async def delete_vocab(item_id: int, user: CurrentUser, db: DB):
    row = await db.scalar(select(VocabularyItem).where(VocabularyItem.id == item_id, VocabularyItem.user_id == user.id))
    if row:
        await db.delete(row)
        await db.commit()


# ---------- Video sources (library sync) ----------
@router.get("/videos", response_model=list[VideoSourceOut])
async def list_videos(user: CurrentUser, db: DB):
    rows = (await db.scalars(select(VideoSource).where(VideoSource.user_id == user.id).order_by(VideoSource.last_played_at.desc().nullslast(), VideoSource.created_at.desc()))).all()
    return [VideoSourceOut.model_validate(r) for r in rows]


@router.post("/videos", response_model=VideoSourceOut, status_code=status.HTTP_201_CREATED)
async def add_video(body: VideoSourceIn, user: CurrentUser, db: DB):
    from ..services.srt import parse_srt

    existing = None
    if body.youtube_id:
        existing = await db.scalar(select(VideoSource).where(VideoSource.user_id == user.id, VideoSource.youtube_id == body.youtube_id))
    elif body.url:
        existing = await db.scalar(select(VideoSource).where(VideoSource.user_id == user.id, VideoSource.url == body.url))
    v = existing or VideoSource(user_id=user.id)
    for k, val in body.model_dump().items():
        setattr(v, k, val)
    v.sentence_count = len(parse_srt(body.subtitle_srt)) if body.subtitle_srt else v.sentence_count
    if existing is None:
        db.add(v)
    await db.commit()
    await db.refresh(v)
    return VideoSourceOut.model_validate(v)


@router.patch("/videos/{video_id}/progress", response_model=VideoSourceOut)
async def update_progress(video_id: int, body: ProgressUpdate, user: CurrentUser, db: DB):
    v = await db.scalar(select(VideoSource).where(VideoSource.id == video_id, VideoSource.user_id == user.id))
    if not v:
        raise HTTPException(404, "video not found")
    v.progress_index = body.progress_index
    v.last_played_at = datetime.utcnow()
    await db.commit()
    await db.refresh(v)
    return VideoSourceOut.model_validate(v)


@router.delete("/videos/{video_id}", status_code=204)
async def delete_video(video_id: int, user: CurrentUser, db: DB):
    v = await db.scalar(select(VideoSource).where(VideoSource.id == video_id, VideoSource.user_id == user.id))
    if v:
        await db.delete(v)
        await db.commit()
