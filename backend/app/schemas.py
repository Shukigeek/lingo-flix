from datetime import datetime, date

from pydantic import BaseModel, EmailStr, Field, field_validator


# ---------- Meta ----------
class AppVersionOut(BaseModel):
    latest_version_code: int
    latest_version_name: str
    download_url: str
    update_notes: str
    force_update: bool


# ---------- Auth ----------
class RegisterRequest(BaseModel):
    email: EmailStr
    username: str = Field(min_length=3, max_length=32, pattern=r"^[A-Za-z0-9_\.\-]+$")
    password: str = Field(min_length=6, max_length=128)
    display_name: str | None = None
    native_language: str = "he"
    target_language: str = "en"


class LoginRequest(BaseModel):
    identifier: str  # email or username
    password: str


class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user: "UserOut"


# ---------- Users ----------
class UserOut(BaseModel):
    id: int
    email: str
    username: str
    display_name: str
    native_language: str
    target_language: str
    level_cefr: str
    total_xp: int
    level: int
    xp_in_level: int
    streak_count: int
    longest_streak: int
    daily_goal_xp: int
    hearts: int
    created_at: datetime

    model_config = {"from_attributes": True}


class UserUpdate(BaseModel):
    display_name: str | None = None
    native_language: str | None = None
    target_language: str | None = None
    level_cefr: str | None = None
    daily_goal_xp: int | None = Field(default=None, ge=10, le=2000)


class StatsOut(BaseModel):
    total_xp: int
    level: int
    xp_in_level: int
    streak_count: int
    longest_streak: int
    today_xp: int
    daily_goal_xp: int
    daily_goal_reached: bool
    total_attempts: int
    correct_attempts: int
    accuracy: float
    sentences_seen: int
    sentences_mastered: int
    due_reviews: int
    vocabulary_size: int
    best_combo: int
    xp_by_day: list["DayXP"]
    accuracy_by_difficulty: dict[str, float]


class DayXP(BaseModel):
    day: date
    xp: int
    attempts: int


# ---------- Series / Episodes / Sentences ----------
class SeriesOut(BaseModel):
    id: int
    slug: str
    title: str
    description: str | None
    language: str
    genre: str | None
    difficulty_cefr: str
    seasons: int
    poster_url: str | None
    episode_count: int = 0
    sentence_count: int = 0

    model_config = {"from_attributes": True}


class SeriesCreate(BaseModel):
    slug: str = Field(min_length=2, max_length=64, pattern=r"^[a-z0-9\-]+$")
    title: str
    description: str | None = None
    language: str = "en"
    genre: str | None = None
    difficulty_cefr: str = "B1"
    seasons: int = 1
    poster_url: str | None = None
    is_public: bool = False


class EpisodeOut(BaseModel):
    id: int
    series_id: int
    season: int
    number: int
    title: str
    synopsis: str | None
    duration_seconds: int | None
    youtube_url: str | None
    sentence_count: int = 0

    model_config = {"from_attributes": True}


class EpisodeCreate(BaseModel):
    season: int = 1
    number: int = 1
    title: str
    synopsis: str | None = None
    youtube_url: str | None = None


class SentenceOut(BaseModel):
    id: int
    text: str
    translation: str | None
    language: str
    start_ms: int
    end_ms: int
    order_index: int
    speaker: str | None
    word_count: int
    difficulty_score: float
    cefr: str
    tags: list[str] = []
    episode_id: int | None

    model_config = {"from_attributes": True}

    @field_validator("tags", mode="before")
    @classmethod
    def split_tags(cls, v):
        if v is None:
            return []
        if isinstance(v, str):
            return [t for t in v.split(",") if t]
        return v


class SentenceCreate(BaseModel):
    text: str = Field(min_length=1, max_length=600)
    translation: str | None = None
    language: str = "en"
    start_ms: int = 0
    end_ms: int = 0
    speaker: str | None = None
    tags: list[str] = []
    is_public: bool = False


class SrtImportRequest(BaseModel):
    srt_content: str
    language: str | None = None
    series_slug: str | None = None
    season: int | None = None
    episode: int | None = None
    episode_title: str | None = None
    source_name: str | None = None
    is_public: bool = False
    min_words: int = 2


class SrtImportResult(BaseModel):
    imported: int
    skipped: int
    episode_id: int | None
    language: str
    sentences: list[SentenceOut]


# ---------- Quiz ----------
class QuizRequest(BaseModel):
    text: str
    difficulty: str = "easy"  # easy|medium|hard
    mode: str = "typing"  # typing|multiple_choice|word_bank|listening
    language: str = "en"
    native_language: str = "he"
    use_llm: bool = True
    sentence_id: int | None = None


class QuizOut(BaseModel):
    sentence_id: int | None
    text: str
    tokens: list[str]
    hidden_indices: list[int]
    hidden_words: list[str]
    masked_text: str
    choices: list[list[str]] = []  # per hidden word (multiple choice)
    word_bank: list[str] = []
    hint: str | None = None
    translation: str | None = None
    grammar_note: str | None = None
    cefr: str
    difficulty: str
    mode: str
    provider: str


class BatchQuizRequest(BaseModel):
    difficulty: str = "easy"
    mode: str = "typing"
    count: int = Field(default=10, ge=1, le=50)
    series_slug: str | None = None
    episode_id: int | None = None
    cefr: str | None = None
    only_due: bool = False
    use_llm: bool = False
    language: str | None = None


class AnswerCheckRequest(BaseModel):
    text: str
    hidden_words: list[str]
    user_answer: str
    language: str = "en"
    difficulty: str = "easy"
    mode: str = "typing"
    combo: int = 0
    sentence_id: int | None = None
    source: str | None = None
    duration_ms: int | None = None
    lenient: bool = True


class WordResult(BaseModel):
    word: str
    correct: bool
    user_word: str | None
    similarity: float


class AnswerCheckResult(BaseModel):
    is_correct: bool
    similarity: float
    per_word: list[WordResult]
    xp_awarded: int
    multiplier: int
    combo: int
    new_total_xp: int
    streak_count: int
    hearts: int
    new_achievements: list["AchievementOut"] = []
    feedback: str
    srs: "SrsState | None" = None


# ---------- SRS ----------
class SrsState(BaseModel):
    sentence_id: int
    repetitions: int
    interval_days: float
    ease_factor: float
    due_at: datetime
    mastery: float


class ReviewOut(BaseModel):
    sentence: SentenceOut
    srs: SrsState


# ---------- Achievements / Leaderboard ----------
class AchievementOut(BaseModel):
    code: str
    title: str
    description: str
    icon: str
    xp_reward: int
    unlocked_at: datetime | None = None
    progress: int = 0
    threshold: int = 1

    model_config = {"from_attributes": True}


class LeaderboardEntry(BaseModel):
    rank: int
    user_id: int
    username: str
    display_name: str
    total_xp: int
    streak_count: int
    level: int
    is_me: bool = False


# ---------- Favorites ----------
class FavoriteIn(BaseModel):
    clip_id: str
    text: str
    translation: str | None = None


class FavoriteOut(FavoriteIn):
    id: int
    created_at: datetime

    model_config = {"from_attributes": True}


# ---------- Vocabulary ----------
class VocabularyOut(BaseModel):
    id: int
    word: str
    language: str
    translation: str | None
    example: str | None
    times_missed: int
    times_correct: int
    mastery: float

    model_config = {"from_attributes": True}


# ---------- Media ----------
class ResolveMediaRequest(BaseModel):
    url: str
    prefer_language: str = "en"
    include_subtitles: bool = True
    auto_subs: bool = True


class SubtitleTrack(BaseModel):
    language: str
    kind: str  # manual|auto
    srt: str
    line_count: int


class ResolvedMedia(BaseModel):
    source_url: str
    kind: str  # youtube|direct|hls|unknown
    title: str | None
    video_id: str | None
    duration_seconds: int | None
    thumbnail_url: str | None
    stream_url: str | None
    stream_expires_hint: str | None
    embeddable: bool
    subtitles: list[SubtitleTrack]
    available_subtitle_languages: list[str]
    error: str | None = None


class VideoSourceIn(BaseModel):
    kind: str = "youtube"
    title: str
    url: str | None = None
    youtube_id: str | None = None
    language: str = "en"
    duration_seconds: int | None = None
    thumbnail_url: str | None = None
    subtitle_srt: str | None = None


class VideoSourceOut(VideoSourceIn):
    id: int
    progress_index: int
    sentence_count: int
    last_played_at: datetime | None
    created_at: datetime

    model_config = {"from_attributes": True}


class ProgressUpdate(BaseModel):
    progress_index: int


# ---------- LLM ----------
class ExplainRequest(BaseModel):
    text: str
    focus_words: list[str] = []
    language: str = "en"
    native_language: str = "he"


class ExplainOut(BaseModel):
    translation: str
    word_explanations: list["WordExplanation"]
    grammar_notes: list[str]
    idioms: list[str]
    cultural_note: str | None
    difficulty_cefr: str
    provider: str
    cached: bool


class WordExplanation(BaseModel):
    word: str
    translation: str
    part_of_speech: str
    definition: str
    example: str


class SelfTestRequest(BaseModel):
    series_slug: str | None = None
    sample_size: int = Field(default=10, ge=1, le=60)
    difficulty: str = "medium"
    use_llm: bool = True


class SelfTestCase(BaseModel):
    sentence: str
    hidden_words: list[str]
    masked_text: str
    solver_answer: list[str]
    solved: bool
    quality_score: float
    notes: str | None = None


class SelfTestOut(BaseModel):
    id: int
    provider: str
    total: int
    solved: int
    accuracy: float
    avg_quality: float
    verdict: str
    cases: list[SelfTestCase]
    created_at: datetime


class SmartSrtRequest(BaseModel):
    srt_content: str
    language: str | None = None
    native_language: str = "he"
    translate: bool = True
    merge_fragments: bool = True
    rate_difficulty: bool = True
    max_llm_lines: int = Field(default=80, ge=0, le=400)


class SmartSrtLine(BaseModel):
    index: int
    start_ms: int
    end_ms: int
    text: str
    translation: str | None
    cefr: str
    difficulty_score: float
    key_words: list[str]


class SmartSrtOut(BaseModel):
    language: str
    line_count: int
    merged_from: int
    lines: list[SmartSrtLine]
    srt: str  # cleaned SRT
    bilingual_srt: str | None
    provider: str


class DailyChallengeOut(BaseModel):
    day: date
    sentences: list[SentenceOut]
    completed: bool
    score: int
    xp_earned: int


class DailyChallengeSubmit(BaseModel):
    score: int = Field(ge=0)
    total: int = Field(ge=1)


TokenResponse.model_rebuild()
StatsOut.model_rebuild()
AnswerCheckResult.model_rebuild()
ExplainOut.model_rebuild()
