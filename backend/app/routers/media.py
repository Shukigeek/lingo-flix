from fastapi import APIRouter, HTTPException, Query
from fastapi.responses import PlainTextResponse

from ..deps import DB, CurrentUser, OptionalUser
from ..schemas import ResolveMediaRequest, ResolvedMedia, SubtitleTrack
from ..services import media, opensubtitles
from ..services.srt import format_time, parse_srt, shift, to_srt

router = APIRouter(prefix="/media", tags=["media"])


@router.post("/resolve", response_model=ResolvedMedia)
async def resolve(body: ResolveMediaRequest, user: OptionalUser):
    """Turn a YouTube / web URL into a playable stream URL + SRT subtitles."""
    lang = body.prefer_language or (user.target_language if user else "en")
    r = await media.resolve(body.url, lang, body.include_subtitles, body.auto_subs)
    return ResolvedMedia(
        source_url=r.source_url,
        kind=r.kind,
        title=r.title,
        video_id=r.video_id,
        duration_seconds=r.duration_seconds,
        thumbnail_url=r.thumbnail_url,
        stream_url=r.stream_url,
        stream_expires_hint=r.stream_expires_hint,
        embeddable=r.embeddable,
        subtitles=[SubtitleTrack(language=t.language, kind=t.kind, srt=t.srt, line_count=t.line_count) for t in r.subtitles],
        available_subtitle_languages=r.available_subtitle_languages,
        error=r.error,
    )


@router.get("/youtube/{video_id}/subtitles.srt", response_class=PlainTextResponse)
async def youtube_subtitles(video_id: str, lang: str = "en", auto: bool = True):
    r = await media.resolve(f"https://www.youtube.com/watch?v={video_id}", lang, True, auto)
    if not r.subtitles:
        raise HTTPException(404, r.error or "No subtitles available for this video/language")
    return r.subtitles[0].srt


@router.get("/youtube/{video_id}/info")
async def youtube_info(video_id: str):
    r = await media.resolve(f"https://www.youtube.com/watch?v={video_id}", "en", False, False)
    return {
        "video_id": video_id,
        "title": r.title,
        "duration_seconds": r.duration_seconds,
        "thumbnail_url": r.thumbnail_url or f"https://i.ytimg.com/vi/{video_id}/hqdefault.jpg",
        "stream_url": r.stream_url,
        "available_subtitle_languages": r.available_subtitle_languages,
        "error": r.error,
    }


@router.get("/opensubtitles/search")
async def os_search(
    query: str | None = None,
    moviehash: str | None = None,
    language: str = "en",
    season: int | None = None,
    episode: int | None = None,
):
    results = await opensubtitles.search(query, moviehash, language, season, episode)
    return {"configured": opensubtitles._headers() is not None, "results": results}


@router.get("/opensubtitles/download/{file_id}", response_class=PlainTextResponse)
async def os_download(file_id: int):
    srt = await opensubtitles.download(file_id)
    if srt is None:
        raise HTTPException(404, "Could not download subtitle (is LINGO_OPENSUBTITLES_API_KEY set?)")
    return srt


@router.post("/srt/shift", response_class=PlainTextResponse)
async def srt_shift(srt: str, offset_ms: int = Query(..., description="Positive delays subtitles, negative advances them")):
    lines = parse_srt(srt)
    if not lines:
        raise HTTPException(400, "no subtitle lines")
    return to_srt(shift(lines, offset_ms))


@router.post("/srt/validate")
async def srt_validate(srt: str):
    lines = parse_srt(srt)
    overlaps = sum(1 for a, b in zip(lines, lines[1:]) if b.start_ms < a.end_ms)
    return {
        "line_count": len(lines),
        "first_cue": format_time(lines[0].start_ms) if lines else None,
        "last_cue": format_time(lines[-1].end_ms) if lines else None,
        "overlapping_cues": overlaps,
        "avg_words": round(sum(len(l.text.split()) for l in lines) / len(lines), 2) if lines else 0,
    }
