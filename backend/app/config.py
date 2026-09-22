from functools import lru_cache
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Runtime configuration, read from environment variables or a .env file."""

    model_config = SettingsConfigDict(env_file=".env", env_prefix="LINGO_", extra="ignore")

    app_name: str = "LingoFlix API"
    database_url: str = "sqlite+aiosqlite:///./lingoflix.db"
    secret_key: str = "change-me-in-production-please"
    access_token_minutes: int = 60 * 24 * 30  # 30 days, mobile-friendly
    cors_origins: list[str] = ["*"]

    # LLM providers. Anthropic is preferred; Gemini is a secondary option;
    # if neither key is present the heuristic engine is used.
    anthropic_api_key: str | None = None
    anthropic_model: str = "claude-opus-5"
    anthropic_fast_model: str = "claude-haiku-4-5"
    gemini_api_key: str | None = None
    gemini_model: str = "gemini-2.0-flash"

    # OpenSubtitles (optional)
    opensubtitles_api_key: str | None = None
    opensubtitles_user_agent: str = "LingoFlix v2.0"

    # Media
    media_cache_dir: str = "./media_cache"
    ytdlp_cookies_file: str | None = None

    # Gameplay
    xp_easy: int = 20
    xp_medium: int = 30
    xp_hard: int = 40
    daily_goal_xp: int = 100

    # App auto-update: what the newest Android build is, and where to fetch it (e.g. a GitHub Release URL).
    app_latest_version_code: int = 10
    app_latest_version_name: str = "2.0"
    app_download_url: str = ""
    app_update_notes: str = ""
    app_force_update: bool = False


@lru_cache
def get_settings() -> Settings:
    return Settings()
