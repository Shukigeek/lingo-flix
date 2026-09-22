import os
from pydantic_settings import BaseSettings
from typing import List

class Settings(BaseSettings):
    PROJECT_NAME: str = "LingoFlix Private Cloud"
    API_V1_STR: str = "/api/v1"

    # Security
    SECRET_KEY: str = os.getenv("SECRET_KEY", "SUPER_SECRET_KEY_FOR_DEV_ONLY")
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 60 * 24 * 7 # 1 week

    # API Keys
    OPENAI_API_KEY: str = os.getenv("OPENAI_API_KEY", "")
    GEMINI_API_KEY: str = os.getenv("GEMINI_API_KEY", "")
    TMDB_API_KEY: str = os.getenv("TMDB_API_KEY", "")

    # Database
    SQLALCHEMY_DATABASE_URL: str = "sqlite:///./lingoflix_v2.db"

    # Paths
    VIDEO_STORAGE_PATH: str = "./storage/videos"
    SUBTITLE_STORAGE_PATH: str = "./storage/subtitles"

    # CORS
    BACKEND_CORS_ORIGINS: List[str] = ["*"]

    class Config:
        case_sensitive = True

settings = Settings()
