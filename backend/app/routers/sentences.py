import json
from datetime import date

from fastapi import APIRouter, HTTPException, Query, status
from sqlalchemy import func, or_, select

from ..deps import DB, CurrentUser, OptionalUser
from ..models import Episode, Sentence, SentenceProgress, Series, DailyChallenge
from ..schemas import (
    DailyChallengeOut,
    DailyChallengeSubmit,
    EpisodeCreate,
    EpisodeOut,
    SentenceCreate,
    SentenceOut,
    SeriesCreate,
    SeriesOut,
    SrtImportRequest,
    SrtImportResult,
)
from ..services import quiz_engine as qe
from ..services.srt import detect_language, merge_fragments, normalize, parse_srt
from .common import touch_streak

router = APIRouter(tags=["sentences"])


def _visible(user_id: int | None):
    if user_id is None:
        return Sentence.is_public.is_(True)
    return or_(Sentence.is_public.is_(True), Sentence.owner_id == user_id)


# ---------- Series ----------
@router.get("/series", response_model=list[SeriesOut])
async def list_series(db: DB, user: OptionalUser, language: str | None = None):
    q = select(Series)
    if user is None:
        q = q.where(Series.is_public.is_(True))
    else:
        q = q.where(or_(Series.is_public.is_(True), Series.owner_id == user.id))
    if language:
        q = q.where(Series.language == language)
    rows = (await db.scalars(q.order_by(Series.title))).all()
    out = []
    for s in rows:
        ep_count = await db.scalar(select(func.count(Episode.id)).where(Episode.series_id == s.id)) or 0
        sent_count = (
            await db.scalar(select(func.count(Sentence.id)).join(Episode, Sentence.episode_id == Episode.id).where(Episode.series_id == s.id))
            or 0
        )
        o = SeriesOut.model_validate(s)
        o.episode_count = ep_count
        o.sentence_count = sent_count
        out.append(o)
    return out


@router.post("/series", response_model=SeriesOut, status_code=status.HTTP_201_CREATED)
async def create_series(body: SeriesCreate, user: CurrentUser, db: DB):
    if await db.scalar(select(Series).where(Series.slug == body.slug)):
        raise HTTPException(status.HTTP_409_CONFLICT, "slug already exists")
    s = Series(**body.model_dump(), owner_id=user.id)
    db.add(s)
    await db.commit()
    await db.refresh(s)
    return SeriesOut.model_validate(s)


@router.get("/series/{slug}/episodes", response_model=list[EpisodeOut])
async def list_episodes(slug: str, db: DB, user: OptionalUser):
    s = await db.scalar(select(Series).where(Series.slug == slug))
    if not s:
        raise HTTPException(404, "series not found")
    eps = (await db.scalars(select(Episode).where(Episode.series_id == s.id).order_by(Episode.season, Episode.number))).all()
    out = []
    for e in eps:
        o = EpisodeOut.model_validate(e)
        o.sentence_count = await db.scalar(select(func.count(Sentence.id)).where(Sentence.episode_id == e.id)) or 0
        out.append(o)
    return out


@router.post("/series/{slug}/episodes", response_model=EpisodeOut, status_code=status.HTTP_201_CREATED)
async def create_episode(slug: str, body: EpisodeCreate, user: CurrentUser, db: DB):
    s = await db.scalar(select(Series).where(Series.slug == slug))
    if not s:
        raise HTTPException(404, "series not found")
    if s.owner_id not in (None, user.id) and not s.is_public:
        raise HTTPException(403, "not your series")
    e = await db.scalar(select(Episode).where(Episode.series_id == s.id, Episode.season == body.season, Episode.number == body.number))
    if e:
        raise HTTPException(status.HTTP_409_CONFLICT, "episode exists")
    e = Episode(series_id=s.id, **body.model_dump())
    db.add(e)
    await db.commit()
    await db.refresh(e)
    return EpisodeOut.model_validate(e)


# ---------- Sentences ----------
@router.get("/sentences", response_model=list[SentenceOut])
async def list_sentences(
    db: DB,
    user: OptionalUser,
    q: str | None = None,
    language: str | None = None,
    cefr: str | None = None,
    series_slug: str | None = None,
    episode_id: int | None = None,
    min_words: int = 2,
    limit: int = Query(50, ge=1, le=500),
    offset: int = Query(0, ge=0),
    random_order: bool = False,
):
    stmt = select(Sentence).where(_visible(user.id if user else None), Sentence.word_count >= min_words)
    if q:
        stmt = stmt.where(Sentence.normalized.contains(normalize(q)))
    if language:
        stmt = stmt.where(Sentence.language == language)
    if cefr:
        stmt = stmt.where(Sentence.cefr == cefr)
    if episode_id:
        stmt = stmt.where(Sentence.episode_id == episode_id)
    if series_slug:
        stmt = stmt.join(Episode, Sentence.episode_id == Episode.id).join(Series, Episode.series_id == Series.id).where(Series.slug == series_slug)
    stmt = stmt.order_by(func.random()) if random_order else stmt.order_by(Sentence.episode_id, Sentence.order_index)
    rows = (await db.scalars(stmt.offset(offset).limit(limit))).all()
    return [SentenceOut.model_validate(r) for r in rows]


@router.get("/sentences/{sentence_id}", response_model=SentenceOut)
async def get_sentence(sentence_id: int, db: DB, user: OptionalUser):
    s = await db.scalar(select(Sentence).where(Sentence.id == sentence_id, _visible(user.id if user else None)))
    if not s:
        raise HTTPException(404, "sentence not found")
    return SentenceOut.model_validate(s)


@router.post("/sentences", response_model=SentenceOut, status_code=status.HTTP_201_CREATED)
async def create_sentence(body: SentenceCreate, user: CurrentUser, db: DB):
    cefr, score = qe.estimate_cefr(body.text, body.language)
    s = Sentence(
        owner_id=user.id,
        text=body.text.strip(),
        normalized=normalize(body.text),
        translation=body.translation,
        language=body.language,
        start_ms=body.start_ms,
        end_ms=body.end_ms,
        speaker=body.speaker,
        word_count=len(body.text.split()),
        difficulty_score=score,
        cefr=cefr,
        tags=",".join(body.tags) or None,
        is_public=body.is_public,
    )
    db.add(s)
    await db.commit()
    await db.refresh(s)
    return SentenceOut.model_validate(s)


@router.delete("/sentences/{sentence_id}", status_code=204)
async def delete_sentence(sentence_id: int, user: CurrentUser, db: DB):
    s = await db.scalar(select(Sentence).where(Sentence.id == sentence_id, Sentence.owner_id == user.id))
    if not s:
        raise HTTPException(404, "sentence not found or not yours")
    await db.delete(s)
    await db.commit()


@router.post("/sentences/import-srt", response_model=SrtImportResult)
async def import_srt(body: SrtImportRequest, user: CurrentUser, db: DB):
    """Parse an SRT file, clean it, and store every line as a sentence in the user's bank.

    Optionally attaches the lines to a series/episode so they show up in the catalog.
    """
    lines = parse_srt(body.srt_content)
    if not lines:
        raise HTTPException(400, "No subtitle lines found")
    lines = merge_fragments(lines)
    lang = body.language or detect_language(" ".join(l.text for l in lines[:200]))

    episode_id = None
    if body.series_slug:
        series = await db.scalar(select(Series).where(Series.slug == body.series_slug))
        if series is None:
            series = Series(
                slug=body.series_slug,
                title=body.series_slug.replace("-", " ").title(),
                language=lang,
                owner_id=user.id,
                is_public=body.is_public,
            )
            db.add(series)
            await db.flush()
        season = body.season or 1
        number = body.episode or 1
        ep = await db.scalar(select(Episode).where(Episode.series_id == series.id, Episode.season == season, Episode.number == number))
        if ep is None:
            ep = Episode(series_id=series.id, season=season, number=number, title=body.episode_title or body.source_name or f"S{season:02d}E{number:02d}")
            db.add(ep)
            await db.flush()
        episode_id = ep.id
        # Replace previous import of the same episode by this user
        old = (await db.scalars(select(Sentence).where(Sentence.episode_id == ep.id, Sentence.owner_id == user.id))).all()
        for o in old:
            await db.delete(o)

    imported, skipped = 0, 0
    created: list[Sentence] = []
    seen: set[str] = set()
    for i, ln in enumerate(lines):
        norm = normalize(ln.text)
        wc = len(ln.text.split())
        if wc < body.min_words or norm in seen:
            skipped += 1
            continue
        seen.add(norm)
        cefr, score = qe.estimate_cefr(ln.text, lang)
        s = Sentence(
            episode_id=episode_id,
            owner_id=user.id,
            text=ln.text,
            normalized=norm,
            language=lang,
            start_ms=ln.start_ms,
            end_ms=ln.end_ms,
            order_index=i,
            speaker=ln.speaker,
            word_count=wc,
            difficulty_score=score,
            cefr=cefr,
            tags=body.source_name,
            is_public=body.is_public,
        )
        db.add(s)
        created.append(s)
        imported += 1
    await db.commit()
    for s in created:
        await db.refresh(s)
    return SrtImportResult(imported=imported, skipped=skipped, episode_id=episode_id, language=lang, sentences=[SentenceOut.model_validate(s) for s in created])


# ---------- Daily challenge ----------
@router.get("/daily", response_model=DailyChallengeOut)
async def daily_challenge(user: CurrentUser, db: DB, size: int = Query(7, ge=3, le=20)):
    today = date.today()
    ch = await db.scalar(select(DailyChallenge).where(DailyChallenge.user_id == user.id, DailyChallenge.day == today))
    if ch is None:
        # Prefer due reviews, then fill with unseen sentences at the user's level.
        due = (
            await db.scalars(
                select(SentenceProgress.sentence_id)
                .where(SentenceProgress.user_id == user.id, SentenceProgress.due_at <= func.now())
                .order_by(SentenceProgress.due_at)
                .limit(size // 2)
            )
        ).all()
        ids = list(due)
        seen_sub = select(SentenceProgress.sentence_id).where(SentenceProgress.user_id == user.id)
        fill = (
            await db.scalars(
                select(Sentence.id)
                .where(_visible(user.id), Sentence.language == user.target_language, Sentence.word_count >= 3, Sentence.id.not_in(seen_sub))
                .order_by(func.random())
                .limit(size - len(ids))
            )
        ).all()
        ids.extend(fill)
        if len(ids) < size:
            more = (await db.scalars(select(Sentence.id).where(_visible(user.id), Sentence.word_count >= 3).order_by(func.random()).limit(size - len(ids)))).all()
            ids.extend(x for x in more if x not in ids)
        ch = DailyChallenge(user_id=user.id, day=today, sentence_ids=json.dumps(ids))
        db.add(ch)
        await db.commit()
        await db.refresh(ch)
    ids = json.loads(ch.sentence_ids)
    rows = (await db.scalars(select(Sentence).where(Sentence.id.in_(ids)))).all() if ids else []
    by_id = {r.id: r for r in rows}
    sentences = [SentenceOut.model_validate(by_id[i]) for i in ids if i in by_id]
    return DailyChallengeOut(day=today, sentences=sentences, completed=ch.completed, score=ch.score, xp_earned=ch.xp_earned)


@router.post("/daily/complete", response_model=DailyChallengeOut)
async def complete_daily(body: DailyChallengeSubmit, user: CurrentUser, db: DB):
    today = date.today()
    ch = await db.scalar(select(DailyChallenge).where(DailyChallenge.user_id == user.id, DailyChallenge.day == today))
    if ch is None:
        raise HTTPException(404, "no challenge for today; call GET /daily first")
    if not ch.completed:
        ratio = min(1.0, body.score / body.total)
        bonus = int(50 + 100 * ratio) + (50 if ratio >= 1.0 else 0)
        ch.completed = True
        ch.score = body.score
        ch.xp_earned = bonus
        user.total_xp += bonus
        touch_streak(user)
        await db.commit()
    ids = json.loads(ch.sentence_ids)
    rows = (await db.scalars(select(Sentence).where(Sentence.id.in_(ids)))).all() if ids else []
    by_id = {r.id: r for r in rows}
    return DailyChallengeOut(
        day=today,
        sentences=[SentenceOut.model_validate(by_id[i]) for i in ids if i in by_id],
        completed=ch.completed,
        score=ch.score,
        xp_earned=ch.xp_earned,
    )
