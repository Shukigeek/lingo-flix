import json
from datetime import datetime

from fastapi import APIRouter, HTTPException
from sqlalchemy import func, or_, select

from ..config import get_settings
from ..deps import DB, CurrentUser, OptionalUser
from ..models import Episode, QuizAttempt, Sentence, SentenceProgress, Series, VocabularyItem
from ..schemas import (
    AchievementOut,
    AnswerCheckRequest,
    AnswerCheckResult,
    BatchQuizRequest,
    QuizOut,
    QuizRequest,
    ReviewOut,
    SentenceOut,
    SrsState,
    WordResult,
)
from ..services import achievements as ach
from ..services import llm
from ..services import quiz_engine as qe
from ..services.srs import apply_review, quality_from_result
from .common import touch_streak

router = APIRouter(prefix="/quiz", tags=["quiz"])


@router.post("/generate", response_model=QuizOut)
async def generate(body: QuizRequest, db: DB, user: OptionalUser):
    if body.difficulty not in ("easy", "medium", "hard"):
        raise HTTPException(400, "difficulty must be easy|medium|hard")
    native = user.native_language if user else body.native_language
    data = await llm.smart_cloze(body.text, body.difficulty, body.mode, body.language, native, db=db, use_llm=body.use_llm)
    data["sentence_id"] = body.sentence_id
    return QuizOut(**data)


@router.post("/batch", response_model=list[QuizOut])
async def batch(body: BatchQuizRequest, user: CurrentUser, db: DB):
    """Build a practice set from the sentence bank (optionally only due SRS items)."""
    if body.only_due:
        stmt = (
            select(Sentence)
            .join(SentenceProgress, SentenceProgress.sentence_id == Sentence.id)
            .where(SentenceProgress.user_id == user.id, SentenceProgress.due_at <= datetime.utcnow())
            .order_by(SentenceProgress.due_at)
        )
    else:
        stmt = select(Sentence).where(or_(Sentence.is_public.is_(True), Sentence.owner_id == user.id), Sentence.word_count >= 3)
        if body.series_slug:
            stmt = stmt.join(Episode, Sentence.episode_id == Episode.id).join(Series, Episode.series_id == Series.id).where(Series.slug == body.series_slug)
        if body.episode_id:
            stmt = stmt.where(Sentence.episode_id == body.episode_id)
        if body.cefr:
            stmt = stmt.where(Sentence.cefr == body.cefr)
        if body.language:
            stmt = stmt.where(Sentence.language == body.language)
        stmt = stmt.order_by(func.random())
    rows = (await db.scalars(stmt.limit(body.count))).all()
    out = []
    for s in rows:
        data = await llm.smart_cloze(s.text, body.difficulty, body.mode, s.language, user.native_language, db=db, use_llm=body.use_llm)
        data["sentence_id"] = s.id
        data["translation"] = data.get("translation") or s.translation
        s.times_served += 1
        out.append(QuizOut(**data))
    await db.commit()
    return out


@router.post("/check", response_model=AnswerCheckResult)
async def check(body: AnswerCheckRequest, user: CurrentUser, db: DB):
    settings = get_settings()
    is_correct, sim, per_word = qe.grade(body.hidden_words, body.user_answer, lenient=body.lenient, mode=body.mode)
    combo = body.combo + 1 if is_correct else 0
    multiplier = qe.combo_multiplier(combo) if is_correct else 1
    xp = qe.xp_for(body.difficulty, multiplier, {"easy": settings.xp_easy, "medium": settings.xp_medium, "hard": settings.xp_hard}) if is_correct else 0

    user.total_xp += xp
    if is_correct:
        touch_streak(user)
    else:
        user.hearts = max(0, user.hearts - 1)

    attempt = QuizAttempt(
        user_id=user.id,
        sentence_id=body.sentence_id,
        sentence_text=body.text,
        hidden_words=json.dumps(body.hidden_words, ensure_ascii=False),
        user_answer=body.user_answer,
        is_correct=is_correct,
        similarity=sim,
        difficulty=body.difficulty,
        mode=body.mode,
        xp_awarded=xp,
        combo=combo,
        source=body.source,
        duration_ms=body.duration_ms,
    )
    db.add(attempt)

    # SRS update if the sentence is in the bank
    srs_state = None
    if body.sentence_id:
        sentence = await db.scalar(select(Sentence).where(Sentence.id == body.sentence_id))
        if sentence:
            sentence.times_served += 1
            if is_correct:
                sentence.times_correct += 1
            prog = await db.scalar(select(SentenceProgress).where(SentenceProgress.user_id == user.id, SentenceProgress.sentence_id == sentence.id))
            if prog is None:
                prog = SentenceProgress(user_id=user.id, sentence_id=sentence.id, repetitions=0, interval_days=0.0, ease_factor=2.5, correct_count=0, wrong_count=0, mastery=0.0)
                db.add(prog)
            apply_review(prog, quality_from_result(is_correct, sim, body.duration_ms))
            srs_state = SrsState(
                sentence_id=sentence.id,
                repetitions=prog.repetitions,
                interval_days=prog.interval_days,
                ease_factor=prog.ease_factor,
                due_at=prog.due_at,
                mastery=prog.mastery,
            )

    # Personal vocabulary: track missed & correct words
    for w in per_word:
        cw = qe.clean_word(w.word)
        if not cw or len(cw) < 2:
            continue
        item = await db.scalar(select(VocabularyItem).where(VocabularyItem.user_id == user.id, VocabularyItem.word == cw, VocabularyItem.language == body.language))
        if item is None:
            if w.correct:
                continue  # only start tracking words the user struggles with
            item = VocabularyItem(user_id=user.id, word=cw, language=body.language, example=body.text, times_missed=0, times_correct=0, mastery=0.0)
            db.add(item)
        if w.correct:
            item.times_correct += 1
        else:
            item.times_missed += 1
            item.example = body.text
        total = item.times_correct + item.times_missed
        item.mastery = round(item.times_correct / total, 3) if total else 0.0

    await db.commit()

    metrics = await ach.compute_metrics(db, user, last_combo=combo, last_hour=datetime.now().hour)
    new_ach = await ach.check_and_unlock(db, user, metrics)
    await db.refresh(user)

    if is_correct:
        feedback = "מושלם! ✨" if sim >= 0.98 else "נכון (עם שגיאת כתיב קטנה) 👍"
        if multiplier > 1:
            feedback += f"  🔥 x{multiplier}"
    else:
        missed = [w.word for w in per_word if not w.correct]
        feedback = "המילים היו: " + " ".join(missed)

    return AnswerCheckResult(
        is_correct=is_correct,
        similarity=sim,
        per_word=[WordResult(word=w.word, correct=w.correct, user_word=w.user_word, similarity=w.similarity) for w in per_word],
        xp_awarded=xp,
        multiplier=multiplier,
        combo=combo,
        new_total_xp=user.total_xp,
        streak_count=user.streak_count,
        hearts=user.hearts,
        new_achievements=[AchievementOut.model_validate(a) for a in new_ach],
        feedback=feedback,
        srs=srs_state,
    )


@router.post("/hearts/refill")
async def refill_hearts(user: CurrentUser, db: DB):
    user.hearts = 10
    await db.commit()
    return {"hearts": user.hearts}


@router.get("/review/due", response_model=list[ReviewOut])
async def due_reviews(user: CurrentUser, db: DB, limit: int = 20):
    rows = (
        await db.execute(
            select(Sentence, SentenceProgress)
            .join(SentenceProgress, SentenceProgress.sentence_id == Sentence.id)
            .where(SentenceProgress.user_id == user.id, SentenceProgress.due_at <= datetime.utcnow())
            .order_by(SentenceProgress.due_at)
            .limit(limit)
        )
    ).all()
    return [
        ReviewOut(
            sentence=SentenceOut.model_validate(s),
            srs=SrsState(sentence_id=s.id, repetitions=p.repetitions, interval_days=p.interval_days, ease_factor=p.ease_factor, due_at=p.due_at, mastery=p.mastery),
        )
        for s, p in rows
    ]


@router.get("/review/count")
async def due_count(user: CurrentUser, db: DB):
    n = await db.scalar(select(func.count(SentenceProgress.id)).where(SentenceProgress.user_id == user.id, SentenceProgress.due_at <= datetime.utcnow())) or 0
    total = await db.scalar(select(func.count(SentenceProgress.id)).where(SentenceProgress.user_id == user.id)) or 0
    mastered = await db.scalar(select(func.count(SentenceProgress.id)).where(SentenceProgress.user_id == user.id, SentenceProgress.mastery >= 0.9)) or 0
    return {"due": n, "tracked": total, "mastered": mastered}
