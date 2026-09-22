import pytest

FRIENDS_SRT = """1
00:00:01,000 --> 00:00:03,000
There's nothing to tell! He's just some guy I work with.

2
00:00:03,500 --> 00:00:05,000
Come on, you're going out with the guy!

3
00:00:05,500 --> 00:00:08,000
There's gotta be something wrong with him.

4
00:00:08,500 --> 00:00:10,000
All right Joey, be nice.

5
00:00:10,500 --> 00:00:13,000
So does he have a hump? A hump and a hairpiece?
"""


@pytest.mark.asyncio
async def test_health(client):
    r = await client.get("/health")
    assert r.status_code == 200
    assert r.json()["status"] == "ok"
    assert r.json()["llm_provider"] == "heuristic"


@pytest.mark.asyncio
async def test_register_login_me(client):
    r = await client.post("/api/v1/auth/register", json={"email": "a@b.com", "username": "alice", "password": "secret123"})
    assert r.status_code == 201
    token = r.json()["access_token"]
    r = await client.post("/api/v1/auth/login", json={"identifier": "alice", "password": "secret123"})
    assert r.status_code == 200
    r = await client.get("/api/v1/auth/me", headers={"Authorization": f"Bearer {token}"})
    assert r.status_code == 200
    assert r.json()["username"] == "alice"
    assert r.json()["level"] == 1
    r = await client.post("/api/v1/auth/login", json={"identifier": "alice", "password": "wrong"})
    assert r.status_code == 401
    r = await client.post("/api/v1/auth/register", json={"email": "a@b.com", "username": "alice2", "password": "secret123"})
    assert r.status_code == 409


@pytest.mark.asyncio
async def test_guest_and_upgrade(client):
    r = await client.post("/api/v1/auth/guest")
    assert r.status_code == 201
    token = r.json()["access_token"]
    r = await client.post(
        "/api/v1/auth/upgrade",
        json={"email": "real@b.com", "username": "realname", "password": "secret123"},
        headers={"Authorization": f"Bearer {token}"},
    )
    assert r.status_code == 200
    assert r.json()["user"]["username"] == "realname"


@pytest.mark.asyncio
async def test_seeded_catalog(client):
    r = await client.get("/api/v1/series")
    assert r.status_code == 200
    slugs = {s["slug"] for s in r.json()}
    assert "friends" in slugs and "lingoflix-coffee-shop" in slugs
    coffee = next(s for s in r.json() if s["slug"] == "lingoflix-coffee-shop")
    assert coffee["sentence_count"] >= 30
    r = await client.get("/api/v1/series/friends/episodes")
    assert r.status_code == 200
    assert any(e["title"].startswith("The One Where Monica") for e in r.json())


@pytest.mark.asyncio
async def test_import_srt_and_practice_flow(auth_client):
    c = auth_client
    r = await c.post(
        "/api/v1/sentences/import-srt",
        json={"srt_content": FRIENDS_SRT, "series_slug": "friends", "season": 1, "episode": 1, "source_name": "friends.s01e01"},
    )
    assert r.status_code == 200, r.text
    body = r.json()
    assert body["imported"] == 5
    assert body["language"] == "en"
    assert body["episode_id"] is not None
    sentence = body["sentences"][0]

    # Re-import replaces rather than duplicates
    r = await c.post("/api/v1/sentences/import-srt", json={"srt_content": FRIENDS_SRT, "series_slug": "friends", "season": 1, "episode": 1})
    assert r.json()["imported"] == 5
    sentence = r.json()["sentences"][0]  # old ids were replaced
    r = await c.get("/api/v1/sentences", params={"series_slug": "friends", "limit": 100})
    friends_sentences = [s for s in r.json() if s["episode_id"] == body["episode_id"]]
    assert len(friends_sentences) == 5

    # Generate a quiz
    r = await c.post("/api/v1/quiz/generate", json={"text": sentence["text"], "difficulty": "easy", "sentence_id": sentence["id"]})
    assert r.status_code == 200
    quiz = r.json()
    assert quiz["provider"] == "heuristic"
    assert len(quiz["hidden_words"]) == 1

    # Answer correctly
    r = await c.post(
        "/api/v1/quiz/check",
        json={
            "text": quiz["text"],
            "hidden_words": quiz["hidden_words"],
            "user_answer": " ".join(quiz["hidden_words"]),
            "difficulty": "easy",
            "sentence_id": sentence["id"],
            "combo": 0,
        },
    )
    assert r.status_code == 200, r.text
    res = r.json()
    assert res["is_correct"] is True
    assert res["xp_awarded"] == 20
    assert res["srs"]["repetitions"] == 1
    assert any(a["code"] == "first_steps" for a in res["new_achievements"])
    assert res["new_total_xp"] == 20 + 20  # answer + achievement reward

    # Answer wrongly -> lose a heart, combo resets, word goes to vocabulary
    r = await c.post(
        "/api/v1/quiz/check",
        json={"text": quiz["text"], "hidden_words": quiz["hidden_words"], "user_answer": "zzzz", "difficulty": "easy", "sentence_id": sentence["id"], "combo": 1},
    )
    res = r.json()
    assert res["is_correct"] is False
    assert res["hearts"] == 9
    assert res["combo"] == 0
    r = await c.get("/api/v1/vocabulary")
    assert len(r.json()) == 1

    # Stats & leaderboard & achievements
    r = await c.get("/api/v1/stats")
    st = r.json()
    assert st["total_attempts"] == 2 and st["correct_attempts"] == 1
    assert st["streak_count"] == 1
    assert len(st["xp_by_day"]) == 14
    r = await c.get("/api/v1/leaderboard")
    assert r.json()[0]["is_me"] is True
    r = await c.get("/api/v1/achievements")
    assert any(a["unlocked_at"] for a in r.json())

    # Batch practice from the series
    r = await c.post("/api/v1/quiz/batch", json={"series_slug": "friends", "count": 5, "difficulty": "medium"})
    assert r.status_code == 200
    assert 1 <= len(r.json()) <= 5

    # SRS due list: the wrong answer makes it due in 10 min, so nothing due now
    r = await c.get("/api/v1/quiz/review/count")
    assert r.json()["tracked"] == 1


@pytest.mark.asyncio
async def test_daily_challenge(auth_client):
    c = auth_client
    r = await c.get("/api/v1/daily", params={"size": 5})
    assert r.status_code == 200
    d = r.json()
    assert len(d["sentences"]) == 5
    assert d["completed"] is False
    r = await c.post("/api/v1/daily/complete", json={"score": 5, "total": 5})
    assert r.json()["completed"] is True
    assert r.json()["xp_earned"] == 200
    # idempotent
    r = await c.post("/api/v1/daily/complete", json={"score": 1, "total": 5})
    assert r.json()["xp_earned"] == 200


@pytest.mark.asyncio
async def test_favorites_and_videos(auth_client):
    c = auth_client
    r = await c.put("/api/v1/favorites", json={"clip_id": "friends.mp4|1000", "text": "How you doin'?"})
    assert r.status_code == 200
    r = await c.post("/api/v1/favorites/sync", json=[{"clip_id": "friends.mp4|1000", "text": "x"}, {"clip_id": "b|2", "text": "y"}])
    assert len(r.json()) == 2
    r = await c.delete("/api/v1/favorites/friends.mp4|1000")
    assert r.status_code == 204
    r = await c.get("/api/v1/favorites")
    assert len(r.json()) == 1

    r = await c.post("/api/v1/videos", json={"kind": "youtube", "title": "Test", "youtube_id": "dQw4w9WgXcQ", "subtitle_srt": FRIENDS_SRT})
    assert r.status_code == 201
    vid = r.json()
    assert vid["sentence_count"] == 5
    r = await c.patch(f"/api/v1/videos/{vid['id']}/progress", json={"progress_index": 3})
    assert r.json()["progress_index"] == 3
    # upsert by youtube id
    r = await c.post("/api/v1/videos", json={"kind": "youtube", "title": "Test 2", "youtube_id": "dQw4w9WgXcQ"})
    assert r.status_code == 201
    r = await c.get("/api/v1/videos")
    assert len(r.json()) == 1 and r.json()[0]["title"] == "Test 2"


@pytest.mark.asyncio
async def test_smart_srt_and_self_test_heuristic(auth_client):
    c = auth_client
    r = await c.post("/api/v1/ai/smart-srt", json={"srt_content": FRIENDS_SRT, "translate": True})
    assert r.status_code == 200
    body = r.json()
    assert body["provider"] == "heuristic"
    assert body["line_count"] == 5
    assert all(l["cefr"] in {"A1", "A2", "B1", "B2", "C1", "C2"} for l in body["lines"])
    assert body["bilingual_srt"] is None  # no LLM -> no translations

    r = await c.post("/api/v1/ai/self-test", json={"series_slug": "lingoflix-coffee-shop", "sample_size": 8, "use_llm": False})
    assert r.status_code == 200, r.text
    rep = r.json()
    assert rep["total"] == 8
    assert rep["verdict"] in {"well-formed", "needs-tuning"}
    assert all(len(case["hidden_words"]) >= 1 for case in rep["cases"])
    r = await c.get("/api/v1/ai/self-test/history")
    assert len(r.json()) == 1

    r = await c.get("/api/v1/ai/status")
    assert r.json()["llm_enabled"] is False


@pytest.mark.asyncio
async def test_media_classify_and_validate(client):
    from app.services.media import classify, extract_youtube_id

    assert extract_youtube_id("https://www.youtube.com/watch?v=dQw4w9WgXcQ") == "dQw4w9WgXcQ"
    assert extract_youtube_id("https://youtu.be/dQw4w9WgXcQ?t=5") == "dQw4w9WgXcQ"
    assert extract_youtube_id("https://www.youtube.com/shorts/dQw4w9WgXcQ") == "dQw4w9WgXcQ"
    assert extract_youtube_id("https://example.com/video.mp4") is None
    assert classify("https://cdn.example.com/a/b/movie.mp4") == "direct"
    assert classify("https://cdn.example.com/live/index.m3u8") == "hls"

    r = await client.post("/api/v1/media/resolve", json={"url": "https://cdn.example.com/a/b/movie.mp4", "include_subtitles": False})
    assert r.status_code == 200
    assert r.json()["kind"] == "direct" and r.json()["stream_url"].endswith("movie.mp4")

    r = await client.post("/api/v1/media/srt/validate", params={"srt": FRIENDS_SRT})
    assert r.json()["line_count"] == 5
    r = await client.post("/api/v1/media/srt/shift", params={"srt": FRIENDS_SRT, "offset_ms": 1500})
    assert "00:00:02,500 --> 00:00:04,500" in r.text
