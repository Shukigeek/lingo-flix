import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from .config import get_settings
from .database import get_session_factory, init_db
from .routers import ai, app_info, auth, media, progress, quiz, sentences
from .seed import seed_all
from .services.achievements import ensure_definitions

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s")


@asynccontextmanager
async def lifespan(app: FastAPI):
    await init_db()
    async with get_session_factory()() as db:
        await ensure_definitions(db)
        await seed_all(db)
    yield


def create_app() -> FastAPI:
    settings = get_settings()
    app = FastAPI(
        title=settings.app_name,
        version="2.0.0",
        description=(
            "Backend for LingoFlix: per-user progress, a shared sentence bank built from subtitles, "
            "spaced repetition, LLM-powered smart quizzes with self-testing, and web/YouTube media resolution."
        ),
        lifespan=lifespan,
    )
    app.add_middleware(
        CORSMiddleware,
        allow_origins=settings.cors_origins,
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )
    api_prefix = "/api/v1"
    for r in (auth.router, sentences.router, quiz.router, progress.router, media.router, ai.router, app_info.router):
        app.include_router(r, prefix=api_prefix)

    @app.get("/health", tags=["meta"])
    async def health():
        from .services.llm import get_provider

        return {"status": "ok", "version": "2.0.0", "llm_provider": get_provider().name}

    return app


app = create_app()
