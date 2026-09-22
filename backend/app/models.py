from datetime import datetime, date

from sqlalchemy import (
    Boolean,
    Date,
    DateTime,
    Float,
    ForeignKey,
    Integer,
    String,
    Text,
    UniqueConstraint,
    func,
)
from sqlalchemy.orm import Mapped, mapped_column, relationship

from .database import Base


def utcnow() -> datetime:
    return datetime.utcnow()


class User(Base):
    __tablename__ = "users"

    id: Mapped[int] = mapped_column(primary_key=True)
    email: Mapped[str] = mapped_column(String(255), unique=True, index=True)
    username: Mapped[str] = mapped_column(String(64), unique=True, index=True)
    password_hash: Mapped[str] = mapped_column(String(255))
    display_name: Mapped[str] = mapped_column(String(64), default="Learner")
    native_language: Mapped[str] = mapped_column(String(8), default="he")
    target_language: Mapped[str] = mapped_column(String(8), default="en")
    level_cefr: Mapped[str] = mapped_column(String(4), default="A2")
    total_xp: Mapped[int] = mapped_column(Integer, default=0)
    streak_count: Mapped[int] = mapped_column(Integer, default=0)
    longest_streak: Mapped[int] = mapped_column(Integer, default=0)
    last_activity_date: Mapped[date | None] = mapped_column(Date, nullable=True)
    daily_goal_xp: Mapped[int] = mapped_column(Integer, default=100)
    hearts: Mapped[int] = mapped_column(Integer, default=10)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=utcnow)

    progress: Mapped[list["SentenceProgress"]] = relationship(back_populates="user", cascade="all, delete-orphan")
    attempts: Mapped[list["QuizAttempt"]] = relationship(back_populates="user", cascade="all, delete-orphan")
    achievements: Mapped[list["UserAchievement"]] = relationship(back_populates="user", cascade="all, delete-orphan")
    favorites: Mapped[list["Favorite"]] = relationship(back_populates="user", cascade="all, delete-orphan")
    video_sources: Mapped[list["VideoSource"]] = relationship(back_populates="user", cascade="all, delete-orphan")
    vocabulary: Mapped[list["VocabularyItem"]] = relationship(back_populates="user", cascade="all, delete-orphan")


class Series(Base):
    """A TV series / movie / YouTube channel that sentences come from."""

    __tablename__ = "series"

    id: Mapped[int] = mapped_column(primary_key=True)
    slug: Mapped[str] = mapped_column(String(64), unique=True, index=True)
    title: Mapped[str] = mapped_column(String(128))
    description: Mapped[str | None] = mapped_column(Text, nullable=True)
    language: Mapped[str] = mapped_column(String(8), default="en")
    genre: Mapped[str | None] = mapped_column(String(64), nullable=True)
    difficulty_cefr: Mapped[str] = mapped_column(String(4), default="B1")
    seasons: Mapped[int] = mapped_column(Integer, default=1)
    poster_url: Mapped[str | None] = mapped_column(String(512), nullable=True)
    is_public: Mapped[bool] = mapped_column(Boolean, default=True)
    owner_id: Mapped[int | None] = mapped_column(ForeignKey("users.id"), nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=utcnow)

    episodes: Mapped[list["Episode"]] = relationship(back_populates="series", cascade="all, delete-orphan")


class Episode(Base):
    __tablename__ = "episodes"
    __table_args__ = (UniqueConstraint("series_id", "season", "number", name="uq_episode"),)

    id: Mapped[int] = mapped_column(primary_key=True)
    series_id: Mapped[int] = mapped_column(ForeignKey("series.id"), index=True)
    season: Mapped[int] = mapped_column(Integer, default=1)
    number: Mapped[int] = mapped_column(Integer, default=1)
    title: Mapped[str] = mapped_column(String(160))
    synopsis: Mapped[str | None] = mapped_column(Text, nullable=True)
    duration_seconds: Mapped[int | None] = mapped_column(Integer, nullable=True)
    youtube_url: Mapped[str | None] = mapped_column(String(512), nullable=True)
    stream_url: Mapped[str | None] = mapped_column(String(1024), nullable=True)
    subtitle_hash: Mapped[str | None] = mapped_column(String(32), nullable=True)

    series: Mapped["Series"] = relationship(back_populates="episodes")
    sentences: Mapped[list["Sentence"]] = relationship(back_populates="episode", cascade="all, delete-orphan")


class Sentence(Base):
    """A single subtitle line stored in the shared sentence bank."""

    __tablename__ = "sentences"

    id: Mapped[int] = mapped_column(primary_key=True)
    episode_id: Mapped[int | None] = mapped_column(ForeignKey("episodes.id"), nullable=True, index=True)
    owner_id: Mapped[int | None] = mapped_column(ForeignKey("users.id"), nullable=True, index=True)
    text: Mapped[str] = mapped_column(Text)
    normalized: Mapped[str] = mapped_column(Text, index=True)
    translation: Mapped[str | None] = mapped_column(Text, nullable=True)
    language: Mapped[str] = mapped_column(String(8), default="en")
    start_ms: Mapped[int] = mapped_column(Integer, default=0)
    end_ms: Mapped[int] = mapped_column(Integer, default=0)
    order_index: Mapped[int] = mapped_column(Integer, default=0)
    speaker: Mapped[str | None] = mapped_column(String(64), nullable=True)
    word_count: Mapped[int] = mapped_column(Integer, default=0)
    difficulty_score: Mapped[float] = mapped_column(Float, default=0.0)  # 0..1
    cefr: Mapped[str] = mapped_column(String(4), default="A2")
    tags: Mapped[str | None] = mapped_column(String(255), nullable=True)  # comma separated
    is_public: Mapped[bool] = mapped_column(Boolean, default=True)
    times_served: Mapped[int] = mapped_column(Integer, default=0)
    times_correct: Mapped[int] = mapped_column(Integer, default=0)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=utcnow)

    episode: Mapped["Episode | None"] = relationship(back_populates="sentences")
    progress: Mapped[list["SentenceProgress"]] = relationship(back_populates="sentence", cascade="all, delete-orphan")


class SentenceProgress(Base):
    """Per-user spaced-repetition state for a sentence (SM-2)."""

    __tablename__ = "sentence_progress"
    __table_args__ = (UniqueConstraint("user_id", "sentence_id", name="uq_user_sentence"),)

    id: Mapped[int] = mapped_column(primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), index=True)
    sentence_id: Mapped[int] = mapped_column(ForeignKey("sentences.id"), index=True)
    repetitions: Mapped[int] = mapped_column(Integer, default=0)
    interval_days: Mapped[float] = mapped_column(Float, default=0.0)
    ease_factor: Mapped[float] = mapped_column(Float, default=2.5)
    due_at: Mapped[datetime] = mapped_column(DateTime, default=utcnow, index=True)
    last_reviewed_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    correct_count: Mapped[int] = mapped_column(Integer, default=0)
    wrong_count: Mapped[int] = mapped_column(Integer, default=0)
    mastery: Mapped[float] = mapped_column(Float, default=0.0)  # 0..1

    user: Mapped["User"] = relationship(back_populates="progress")
    sentence: Mapped["Sentence"] = relationship(back_populates="progress")


class QuizAttempt(Base):
    __tablename__ = "quiz_attempts"

    id: Mapped[int] = mapped_column(primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), index=True)
    sentence_id: Mapped[int | None] = mapped_column(ForeignKey("sentences.id"), nullable=True, index=True)
    sentence_text: Mapped[str] = mapped_column(Text)
    hidden_words: Mapped[str] = mapped_column(Text)  # JSON list
    user_answer: Mapped[str] = mapped_column(Text)
    is_correct: Mapped[bool] = mapped_column(Boolean, default=False)
    similarity: Mapped[float] = mapped_column(Float, default=0.0)
    difficulty: Mapped[str] = mapped_column(String(16), default="easy")
    mode: Mapped[str] = mapped_column(String(32), default="typing")
    xp_awarded: Mapped[int] = mapped_column(Integer, default=0)
    combo: Mapped[int] = mapped_column(Integer, default=0)
    source: Mapped[str | None] = mapped_column(String(255), nullable=True)  # video name / url
    duration_ms: Mapped[int | None] = mapped_column(Integer, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=utcnow, index=True)

    user: Mapped["User"] = relationship(back_populates="attempts")


class Achievement(Base):
    __tablename__ = "achievements"

    id: Mapped[int] = mapped_column(primary_key=True)
    code: Mapped[str] = mapped_column(String(64), unique=True)
    title: Mapped[str] = mapped_column(String(128))
    description: Mapped[str] = mapped_column(Text)
    icon: Mapped[str] = mapped_column(String(16), default="🏅")
    xp_reward: Mapped[int] = mapped_column(Integer, default=50)
    threshold: Mapped[int] = mapped_column(Integer, default=1)
    metric: Mapped[str] = mapped_column(String(32), default="attempts")  # attempts|correct|streak|xp|sentences|combo|perfect_sessions


class UserAchievement(Base):
    __tablename__ = "user_achievements"
    __table_args__ = (UniqueConstraint("user_id", "achievement_id", name="uq_user_achievement"),)

    id: Mapped[int] = mapped_column(primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), index=True)
    achievement_id: Mapped[int] = mapped_column(ForeignKey("achievements.id"))
    unlocked_at: Mapped[datetime] = mapped_column(DateTime, default=utcnow)

    user: Mapped["User"] = relationship(back_populates="achievements")
    achievement: Mapped["Achievement"] = relationship()


class Favorite(Base):
    __tablename__ = "favorites"
    __table_args__ = (UniqueConstraint("user_id", "clip_id", name="uq_user_clip"),)

    id: Mapped[int] = mapped_column(primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), index=True)
    clip_id: Mapped[str] = mapped_column(String(255))  # "<video>|<startMs>"
    text: Mapped[str] = mapped_column(Text)
    translation: Mapped[str | None] = mapped_column(Text, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=utcnow)

    user: Mapped["User"] = relationship(back_populates="favorites")


class VideoSource(Base):
    """A video the user linked: local file name, direct URL or YouTube."""

    __tablename__ = "video_sources"

    id: Mapped[int] = mapped_column(primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), index=True)
    kind: Mapped[str] = mapped_column(String(16), default="local")  # local|url|youtube
    title: Mapped[str] = mapped_column(String(255))
    url: Mapped[str | None] = mapped_column(String(1024), nullable=True)
    youtube_id: Mapped[str | None] = mapped_column(String(32), nullable=True)
    language: Mapped[str] = mapped_column(String(8), default="en")
    duration_seconds: Mapped[int | None] = mapped_column(Integer, nullable=True)
    thumbnail_url: Mapped[str | None] = mapped_column(String(1024), nullable=True)
    subtitle_srt: Mapped[str | None] = mapped_column(Text, nullable=True)
    progress_index: Mapped[int] = mapped_column(Integer, default=0)
    sentence_count: Mapped[int] = mapped_column(Integer, default=0)
    last_played_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=utcnow)

    user: Mapped["User"] = relationship(back_populates="video_sources")


class VocabularyItem(Base):
    """Words the user got wrong or saved; feeds the personal word bank."""

    __tablename__ = "vocabulary"
    __table_args__ = (UniqueConstraint("user_id", "word", "language", name="uq_user_word"),)

    id: Mapped[int] = mapped_column(primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), index=True)
    word: Mapped[str] = mapped_column(String(64), index=True)
    language: Mapped[str] = mapped_column(String(8), default="en")
    translation: Mapped[str | None] = mapped_column(String(255), nullable=True)
    example: Mapped[str | None] = mapped_column(Text, nullable=True)
    times_missed: Mapped[int] = mapped_column(Integer, default=0)
    times_correct: Mapped[int] = mapped_column(Integer, default=0)
    mastery: Mapped[float] = mapped_column(Float, default=0.0)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=utcnow)
    updated_at: Mapped[datetime] = mapped_column(DateTime, default=utcnow, onupdate=utcnow)

    user: Mapped["User"] = relationship(back_populates="vocabulary")


class LlmCache(Base):
    """Caches LLM results keyed by (task, hash of input)."""

    __tablename__ = "llm_cache"

    id: Mapped[int] = mapped_column(primary_key=True)
    cache_key: Mapped[str] = mapped_column(String(96), unique=True, index=True)
    task: Mapped[str] = mapped_column(String(32))
    provider: Mapped[str] = mapped_column(String(32))
    payload: Mapped[str] = mapped_column(Text)  # JSON
    created_at: Mapped[datetime] = mapped_column(DateTime, default=utcnow)


class DailyChallenge(Base):
    __tablename__ = "daily_challenges"
    __table_args__ = (UniqueConstraint("user_id", "day", name="uq_user_day"),)

    id: Mapped[int] = mapped_column(primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), index=True)
    day: Mapped[date] = mapped_column(Date, index=True)
    sentence_ids: Mapped[str] = mapped_column(Text)  # JSON list
    completed: Mapped[bool] = mapped_column(Boolean, default=False)
    score: Mapped[int] = mapped_column(Integer, default=0)
    xp_earned: Mapped[int] = mapped_column(Integer, default=0)


class SelfTestReport(Base):
    """Results of the LLM quiz engine testing itself on a sentence pack."""

    __tablename__ = "self_test_reports"

    id: Mapped[int] = mapped_column(primary_key=True)
    requested_by: Mapped[int | None] = mapped_column(ForeignKey("users.id"), nullable=True)
    series_slug: Mapped[str | None] = mapped_column(String(64), nullable=True)
    provider: Mapped[str] = mapped_column(String(32))
    total: Mapped[int] = mapped_column(Integer, default=0)
    solved: Mapped[int] = mapped_column(Integer, default=0)
    accuracy: Mapped[float] = mapped_column(Float, default=0.0)
    avg_quality: Mapped[float] = mapped_column(Float, default=0.0)
    details: Mapped[str] = mapped_column(Text)  # JSON
    created_at: Mapped[datetime] = mapped_column(DateTime, default=utcnow, server_default=func.now())
