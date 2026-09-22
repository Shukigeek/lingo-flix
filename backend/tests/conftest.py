import os
import sys
import pathlib

import pytest
import pytest_asyncio
from httpx import ASGITransport, AsyncClient

ROOT = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

os.environ["LINGO_DATABASE_URL"] = "sqlite+aiosqlite:///./test_lingoflix.db"
os.environ["LINGO_SECRET_KEY"] = "test-secret"
os.environ.pop("LINGO_ANTHROPIC_API_KEY", None)
os.environ.pop("LINGO_GEMINI_API_KEY", None)


@pytest_asyncio.fixture
async def client():
    from app.config import get_settings
    from app.database import get_engine, init_db, reset_engine_for_tests, get_session_factory, Base
    from app.main import create_app
    from app.seed import seed_all
    from app.services.achievements import ensure_definitions
    from app.services import llm

    get_settings.cache_clear()
    await reset_engine_for_tests()
    engine = get_engine()
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.drop_all)
    await init_db()
    async with get_session_factory()() as db:
        await ensure_definitions(db)
        await seed_all(db)
    llm.set_provider_for_tests(None)
    app = create_app()
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as c:
        yield c
    await reset_engine_for_tests()
    try:
        os.remove("test_lingoflix.db")
    except FileNotFoundError:
        pass


@pytest_asyncio.fixture
async def auth_client(client):
    r = await client.post(
        "/api/v1/auth/register",
        json={"email": "tester@example.com", "username": "tester", "password": "secret123", "native_language": "he", "target_language": "en"},
    )
    assert r.status_code == 201, r.text
    token = r.json()["access_token"]
    client.headers["Authorization"] = f"Bearer {token}"
    return client
