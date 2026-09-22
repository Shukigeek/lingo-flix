import json

from fastapi import APIRouter, HTTPException
from sqlalchemy import func, or_, select

from ..deps import DB, CurrentUser, OptionalUser
from ..models import Episode, SelfTestReport, Sentence, Series
from ..schemas import ExplainOut, ExplainRequest, SelfTestCase, SelfTestOut, SelfTestRequest, SmartSrtLine, SmartSrtOut, SmartSrtRequest
from ..services import llm
from ..services.srt import detect_language, merge_fragments, parse_srt, to_srt

router = APIRouter(prefix="/ai", tags=["ai"])


@router.get("/status")
async def status():
    p = llm.get_provider()
    return {
        "provider": p.name,
        "llm_enabled": p.name != "heuristic",
        "model": getattr(p, "model", None),
        "hint": None if p.name != "heuristic" else "Set LINGO_ANTHROPIC_API_KEY (or LINGO_GEMINI_API_KEY) on the server to enable LLM features.",
    }


@router.post("/explain", response_model=ExplainOut)
async def explain(body: ExplainRequest, db: DB, user: OptionalUser):
    native = user.native_language if user else body.native_language
    data = await llm.explain(body.text, body.focus_words, body.language, native, db=db)
    return ExplainOut(**data)


@router.post("/smart-srt", response_model=SmartSrtOut)
async def smart_srt(body: SmartSrtRequest, db: DB, user: OptionalUser):
    """Clean an SRT, merge broken sentences, rate each line and (optionally) add translations.

    Returns both the cleaned SRT and a bilingual SRT the player can load directly.
    """
    raw_lines = parse_srt(body.srt_content)
    if not raw_lines:
        raise HTTPException(400, "No subtitle lines found")
    lines = merge_fragments(raw_lines) if body.merge_fragments else raw_lines
    lang = body.language or detect_language(" ".join(l.text for l in lines[:200]))
    native = user.native_language if user else body.native_language

    texts = [l.text for l in lines]
    llm_limit = body.max_llm_lines
    rated = await llm.rate_lines(texts[:llm_limit], lang, db=db) if body.rate_difficulty else []
    if len(rated) < len(texts):
        from ..services import quiz_engine as qe

        rated += [(qe.estimate_cefr(t, lang)[0], []) for t in texts[len(rated):]]
    translations: list[str | None] = [None] * len(texts)
    if body.translate:
        translations = await llm.translate_lines(texts[:llm_limit], lang, native, db=db) + [None] * max(0, len(texts) - llm_limit)

    from ..services import quiz_engine as qe

    out_lines = []
    for i, l in enumerate(lines):
        cefr, kws = rated[i]
        score = qe.estimate_cefr(l.text, lang)[1]
        out_lines.append(SmartSrtLine(index=i + 1, start_ms=l.start_ms, end_ms=l.end_ms, text=l.text, translation=translations[i], cefr=cefr, difficulty_score=score, key_words=kws))
    bilingual = to_srt(lines, {i + 1: t for i, t in enumerate(translations) if t}) if any(translations) else None
    return SmartSrtOut(language=lang, line_count=len(lines), merged_from=len(raw_lines), lines=out_lines, srt=to_srt(lines), bilingual_srt=bilingual, provider=llm.get_provider().name)


@router.post("/self-test", response_model=SelfTestOut)
async def self_test(body: SelfTestRequest, user: CurrentUser, db: DB):
    """Let the quiz engine test itself: generate quizzes from a series, solve them blind, judge quality."""
    stmt = select(Sentence).where(or_(Sentence.is_public.is_(True), Sentence.owner_id == user.id), Sentence.word_count >= 4)
    if body.series_slug:
        stmt = stmt.join(Episode, Sentence.episode_id == Episode.id).join(Series, Episode.series_id == Series.id).where(Series.slug == body.series_slug)
    rows = (await db.scalars(stmt.order_by(func.random()).limit(body.sample_size))).all()
    if not rows:
        raise HTTPException(404, "No sentences found for that series. Import an SRT first.")
    lang = rows[0].language
    report = await llm.self_test([r.text for r in rows], body.difficulty, lang, user.native_language, body.use_llm, db=db)
    row = SelfTestReport(
        requested_by=user.id,
        series_slug=body.series_slug,
        provider=report["provider"],
        total=report["total"],
        solved=report["solved"],
        accuracy=report["accuracy"],
        avg_quality=report["avg_quality"],
        details=json.dumps(report["cases"], ensure_ascii=False),
    )
    db.add(row)
    await db.commit()
    await db.refresh(row)
    return SelfTestOut(
        id=row.id,
        provider=report["provider"],
        total=report["total"],
        solved=report["solved"],
        accuracy=report["accuracy"],
        avg_quality=report["avg_quality"],
        verdict=report["verdict"],
        cases=[SelfTestCase(**c) for c in report["cases"]],
        created_at=row.created_at,
    )


@router.get("/self-test/history")
async def self_test_history(user: CurrentUser, db: DB, limit: int = 20):
    rows = (await db.scalars(select(SelfTestReport).order_by(SelfTestReport.created_at.desc()).limit(limit))).all()
    return [
        {
            "id": r.id,
            "series_slug": r.series_slug,
            "provider": r.provider,
            "total": r.total,
            "solved": r.solved,
            "accuracy": r.accuracy,
            "avg_quality": r.avg_quality,
            "created_at": r.created_at,
        }
        for r in rows
    ]
