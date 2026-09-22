"""Resolve web videos (YouTube, direct MP4/HLS) into something the app can play,
and pull subtitles for them. Uses yt-dlp as a library; network failures degrade
gracefully into an ``error`` field instead of exceptions.
"""
from __future__ import annotations

import asyncio
import logging
import re
from dataclasses import dataclass, field
from urllib.parse import parse_qs, urlparse

import httpx

from ..config import get_settings
from .srt import parse_srt, vtt_to_srt

log = logging.getLogger("lingoflix.media")

YT_ID_RE = re.compile(r"^[A-Za-z0-9_-]{11}$")
DIRECT_EXT = (".mp4", ".m4v", ".webm", ".mkv", ".mov", ".m3u8", ".mpd")


@dataclass
class SubtitleTrack:
    language: str
    kind: str
    srt: str
    line_count: int


@dataclass
class Resolved:
    source_url: str
    kind: str
    title: str | None = None
    video_id: str | None = None
    duration_seconds: int | None = None
    thumbnail_url: str | None = None
    stream_url: str | None = None
    stream_expires_hint: str | None = None
    embeddable: bool = False
    subtitles: list[SubtitleTrack] = field(default_factory=list)
    available_subtitle_languages: list[str] = field(default_factory=list)
    error: str | None = None


def extract_youtube_id(url: str) -> str | None:
    url = url.strip()
    if YT_ID_RE.match(url):
        return url
    try:
        p = urlparse(url)
    except ValueError:
        return None
    host = (p.hostname or "").lower().removeprefix("www.").removeprefix("m.")
    if host == "youtu.be":
        vid = p.path.strip("/").split("/")[0]
        return vid if YT_ID_RE.match(vid) else None
    if host in ("youtube.com", "music.youtube.com", "youtube-nocookie.com"):
        if p.path == "/watch":
            vid = parse_qs(p.query).get("v", [""])[0]
            return vid if YT_ID_RE.match(vid) else None
        for prefix in ("/embed/", "/shorts/", "/live/", "/v/"):
            if p.path.startswith(prefix):
                vid = p.path[len(prefix):].split("/")[0]
                return vid if YT_ID_RE.match(vid) else None
    return None


def classify(url: str) -> str:
    if extract_youtube_id(url):
        return "youtube"
    path = urlparse(url).path.lower()
    if path.endswith(".m3u8"):
        return "hls"
    if path.endswith(".mpd"):
        return "dash"
    if path.endswith(DIRECT_EXT):
        return "direct"
    return "unknown"


def _pick_format(info: dict) -> str | None:
    """Pick a progressive (audio+video) mp4 URL that ExoPlayer can stream directly."""
    formats = info.get("formats") or []
    best = None
    for f in formats:
        if f.get("vcodec") in (None, "none") or f.get("acodec") in (None, "none"):
            continue
        if f.get("ext") not in ("mp4", "webm"):
            continue
        if not f.get("url"):
            continue
        height = f.get("height") or 0
        if height > 1080:
            continue
        if best is None or height > (best.get("height") or 0):
            best = f
    if best:
        return best["url"]
    # HLS manifest fallback (ExoPlayer handles m3u8)
    for f in formats:
        if f.get("protocol", "").startswith("m3u8") and f.get("url"):
            return f["url"]
    return info.get("url")


def _ytdlp_info(url: str, want_subs: bool, auto_subs: bool, langs: list[str]) -> dict:
    import yt_dlp  # imported lazily: heavy

    settings = get_settings()
    opts = {
        "quiet": True,
        "no_warnings": True,
        "skip_download": True,
        "noplaylist": True,
        "writesubtitles": want_subs,
        "writeautomaticsub": want_subs and auto_subs,
        "subtitleslangs": langs,
        "subtitlesformat": "vtt",
        "socket_timeout": 20,
    }
    if settings.ytdlp_cookies_file:
        opts["cookiefile"] = settings.ytdlp_cookies_file
    with yt_dlp.YoutubeDL(opts) as ydl:
        return ydl.extract_info(url, download=False)


async def _download_text(url: str) -> str | None:
    try:
        async with httpx.AsyncClient(timeout=30, follow_redirects=True) as client:
            r = await client.get(url)
            r.raise_for_status()
            return r.text
    except Exception as e:  # noqa: BLE001
        log.warning("subtitle download failed: %s", e)
        return None


def _lang_matches(code: str, wanted: str) -> bool:
    return code == wanted or code.split("-")[0] == wanted.split("-")[0]


async def resolve(url: str, prefer_language: str = "en", include_subtitles: bool = True, auto_subs: bool = True) -> Resolved:
    kind = classify(url)
    res = Resolved(source_url=url, kind=kind)
    if kind in ("direct", "hls", "dash"):
        res.stream_url = url
        res.title = urlparse(url).path.rsplit("/", 1)[-1]
        return res
    if kind == "unknown":
        # Might still be a page yt-dlp understands (Vimeo, news sites...). Try it.
        pass
    vid = extract_youtube_id(url)
    res.video_id = vid
    res.embeddable = vid is not None
    langs = [prefer_language, f"{prefer_language}-orig", "en", "en-US", "en-GB"]
    try:
        info = await asyncio.to_thread(_ytdlp_info, url if not vid else f"https://www.youtube.com/watch?v={vid}", include_subtitles, auto_subs, langs)
    except Exception as e:  # noqa: BLE001
        res.error = f"yt-dlp could not resolve this URL: {e.__class__.__name__}: {str(e)[:200]}"
        if vid:
            res.title = res.title or f"YouTube {vid}"
            res.thumbnail_url = f"https://i.ytimg.com/vi/{vid}/hqdefault.jpg"
        return res

    res.kind = "youtube" if vid else "page"
    res.title = info.get("title")
    res.duration_seconds = int(info["duration"]) if info.get("duration") else None
    res.thumbnail_url = info.get("thumbnail")
    res.stream_url = _pick_format(info)
    res.stream_expires_hint = "Google stream URLs expire after ~6 hours; re-resolve before playing later."

    manual = info.get("subtitles") or {}
    auto = info.get("automatic_captions") or {}
    res.available_subtitle_languages = sorted(set(list(manual.keys()) + [f"auto:{k}" for k in auto.keys()]))
    if not include_subtitles:
        return res

    async def fetch_track(entries: list[dict], code: str, kind_: str) -> SubtitleTrack | None:
        vtt_entry = next((e for e in entries if e.get("ext") == "vtt" and e.get("url")), None) or next(
            (e for e in entries if e.get("url")), None
        )
        if not vtt_entry:
            return None
        raw = await _download_text(vtt_entry["url"])
        if not raw:
            return None
        srt = vtt_to_srt(raw) if "WEBVTT" in raw[:200] or vtt_entry.get("ext") == "vtt" else raw
        lines = parse_srt(srt)
        if not lines:
            return None
        return SubtitleTrack(language=code, kind=kind_, srt=srt, line_count=len(lines))

    picked: SubtitleTrack | None = None
    for code, entries in manual.items():
        if _lang_matches(code, prefer_language):
            picked = await fetch_track(entries, code, "manual")
            if picked:
                break
    if not picked and auto_subs:
        for code, entries in auto.items():
            if _lang_matches(code, prefer_language) and not code.endswith("-orig") or code == f"{prefer_language}-orig":
                picked = await fetch_track(entries, code, "auto")
                if picked:
                    break
    if not picked and manual:
        code, entries = next(iter(manual.items()))
        picked = await fetch_track(entries, code, "manual")
    if picked:
        res.subtitles.append(picked)
    return res
