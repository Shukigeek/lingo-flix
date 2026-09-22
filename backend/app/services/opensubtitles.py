"""Minimal OpenSubtitles REST client (api.opensubtitles.com). Requires an API key."""
from __future__ import annotations

import logging

import httpx

from ..config import get_settings

log = logging.getLogger("lingoflix.opensubtitles")
BASE = "https://api.opensubtitles.com/api/v1"


def _headers() -> dict[str, str] | None:
    s = get_settings()
    if not s.opensubtitles_api_key:
        return None
    return {"Api-Key": s.opensubtitles_api_key, "User-Agent": s.opensubtitles_user_agent, "Accept": "application/json"}


async def search(query: str | None = None, moviehash: str | None = None, language: str = "en", season: int | None = None, episode: int | None = None) -> list[dict]:
    headers = _headers()
    if headers is None:
        return []
    params: dict[str, str | int] = {"languages": language}
    if query:
        params["query"] = query
    if moviehash:
        params["moviehash"] = moviehash
    if season is not None:
        params["season_number"] = season
    if episode is not None:
        params["episode_number"] = episode
    try:
        async with httpx.AsyncClient(timeout=30) as client:
            r = await client.get(f"{BASE}/subtitles", params=params, headers=headers)
            r.raise_for_status()
            data = r.json().get("data", [])
    except Exception as e:  # noqa: BLE001
        log.warning("OpenSubtitles search failed: %s", e)
        return []
    out = []
    for item in data:
        attrs = item.get("attributes", {})
        files = attrs.get("files") or []
        if not files:
            continue
        out.append(
            {
                "file_id": files[0].get("file_id"),
                "release": attrs.get("release"),
                "language": attrs.get("language"),
                "downloads": attrs.get("download_count", 0),
                "hearing_impaired": attrs.get("hearing_impaired", False),
                "title": (attrs.get("feature_details") or {}).get("title"),
            }
        )
    out.sort(key=lambda x: -(x["downloads"] or 0))
    return out


async def download(file_id: int) -> str | None:
    headers = _headers()
    if headers is None:
        return None
    try:
        async with httpx.AsyncClient(timeout=30) as client:
            r = await client.post(f"{BASE}/download", json={"file_id": file_id, "sub_format": "srt"}, headers=headers)
            r.raise_for_status()
            link = r.json().get("link")
            if not link:
                return None
            r2 = await client.get(link)
            r2.raise_for_status()
            return r2.text
    except Exception as e:  # noqa: BLE001
        log.warning("OpenSubtitles download failed: %s", e)
        return None
