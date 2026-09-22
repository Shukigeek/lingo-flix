package com.example.lingoFlix.data.remote

import com.google.gson.annotations.SerializedName

// ---------- Auth ----------
data class RegisterRequest(
    val email: String,
    val username: String,
    val password: String,
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("native_language") val nativeLanguage: String = "he",
    @SerializedName("target_language") val targetLanguage: String = "en"
)

data class LoginRequest(val identifier: String, val password: String)

data class TokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String = "bearer",
    val user: UserDto
)

data class UserDto(
    val id: Int,
    val email: String,
    val username: String,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("native_language") val nativeLanguage: String,
    @SerializedName("target_language") val targetLanguage: String,
    @SerializedName("level_cefr") val levelCefr: String,
    @SerializedName("total_xp") val totalXp: Int,
    val level: Int,
    @SerializedName("xp_in_level") val xpInLevel: Int,
    @SerializedName("streak_count") val streakCount: Int,
    @SerializedName("longest_streak") val longestStreak: Int,
    @SerializedName("daily_goal_xp") val dailyGoalXp: Int,
    val hearts: Int
)

data class UserUpdate(
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("native_language") val nativeLanguage: String? = null,
    @SerializedName("target_language") val targetLanguage: String? = null,
    @SerializedName("daily_goal_xp") val dailyGoalXp: Int? = null
)

// ---------- Catalog ----------
data class SeriesDto(
    val id: Int,
    val slug: String,
    val title: String,
    val description: String?,
    val language: String,
    val genre: String?,
    @SerializedName("difficulty_cefr") val difficultyCefr: String,
    val seasons: Int,
    @SerializedName("poster_url") val posterUrl: String?,
    @SerializedName("episode_count") val episodeCount: Int,
    @SerializedName("sentence_count") val sentenceCount: Int
)

data class EpisodeDto(
    val id: Int,
    @SerializedName("series_id") val seriesId: Int,
    val season: Int,
    val number: Int,
    val title: String,
    val synopsis: String?,
    @SerializedName("youtube_url") val youtubeUrl: String?,
    @SerializedName("sentence_count") val sentenceCount: Int
)

data class SentenceDto(
    val id: Int,
    val text: String,
    val translation: String?,
    val language: String,
    @SerializedName("start_ms") val startMs: Long,
    @SerializedName("end_ms") val endMs: Long,
    @SerializedName("order_index") val orderIndex: Int,
    val speaker: String?,
    @SerializedName("word_count") val wordCount: Int,
    @SerializedName("difficulty_score") val difficultyScore: Double,
    val cefr: String,
    val tags: List<String> = emptyList(),
    @SerializedName("episode_id") val episodeId: Int?
)

data class SrtImportRequest(
    @SerializedName("srt_content") val srtContent: String,
    val language: String? = null,
    @SerializedName("series_slug") val seriesSlug: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    @SerializedName("episode_title") val episodeTitle: String? = null,
    @SerializedName("source_name") val sourceName: String? = null,
    @SerializedName("is_public") val isPublic: Boolean = false
)

data class SrtImportResult(
    val imported: Int,
    val skipped: Int,
    @SerializedName("episode_id") val episodeId: Int?,
    val language: String,
    val sentences: List<SentenceDto>
)

// ---------- Quiz ----------
data class QuizRequest(
    val text: String,
    val difficulty: String = "easy",
    val mode: String = "typing",
    val language: String = "en",
    @SerializedName("native_language") val nativeLanguage: String = "he",
    @SerializedName("use_llm") val useLlm: Boolean = true,
    @SerializedName("sentence_id") val sentenceId: Int? = null
)

data class BatchQuizRequest(
    val difficulty: String = "easy",
    val mode: String = "typing",
    val count: Int = 10,
    @SerializedName("series_slug") val seriesSlug: String? = null,
    @SerializedName("episode_id") val episodeId: Int? = null,
    val cefr: String? = null,
    @SerializedName("only_due") val onlyDue: Boolean = false,
    @SerializedName("use_llm") val useLlm: Boolean = false,
    val language: String? = null
)

data class QuizDto(
    @SerializedName("sentence_id") val sentenceId: Int?,
    val text: String,
    val tokens: List<String>,
    @SerializedName("hidden_indices") val hiddenIndices: List<Int>,
    @SerializedName("hidden_words") val hiddenWords: List<String>,
    @SerializedName("masked_text") val maskedText: String,
    val choices: List<List<String>> = emptyList(),
    @SerializedName("word_bank") val wordBank: List<String> = emptyList(),
    val hint: String?,
    val translation: String?,
    @SerializedName("grammar_note") val grammarNote: String?,
    val cefr: String,
    val difficulty: String,
    val mode: String,
    val provider: String
)

data class AnswerCheckRequest(
    val text: String,
    @SerializedName("hidden_words") val hiddenWords: List<String>,
    @SerializedName("user_answer") val userAnswer: String,
    val language: String = "en",
    val difficulty: String = "easy",
    val mode: String = "typing",
    val combo: Int = 0,
    @SerializedName("sentence_id") val sentenceId: Int? = null,
    val source: String? = null,
    @SerializedName("duration_ms") val durationMs: Long? = null,
    val lenient: Boolean = true
)

data class WordResultDto(val word: String, val correct: Boolean, @SerializedName("user_word") val userWord: String?, val similarity: Double)

data class AchievementDto(
    val code: String,
    val title: String,
    val description: String,
    val icon: String,
    @SerializedName("xp_reward") val xpReward: Int,
    @SerializedName("unlocked_at") val unlockedAt: String?,
    val progress: Int = 0,
    val threshold: Int = 1
)

data class SrsStateDto(
    @SerializedName("sentence_id") val sentenceId: Int,
    val repetitions: Int,
    @SerializedName("interval_days") val intervalDays: Double,
    @SerializedName("ease_factor") val easeFactor: Double,
    @SerializedName("due_at") val dueAt: String,
    val mastery: Double
)

data class AnswerCheckResult(
    @SerializedName("is_correct") val isCorrect: Boolean,
    val similarity: Double,
    @SerializedName("per_word") val perWord: List<WordResultDto>,
    @SerializedName("xp_awarded") val xpAwarded: Int,
    val multiplier: Int,
    val combo: Int,
    @SerializedName("new_total_xp") val newTotalXp: Int,
    @SerializedName("streak_count") val streakCount: Int,
    val hearts: Int,
    @SerializedName("new_achievements") val newAchievements: List<AchievementDto> = emptyList(),
    val feedback: String,
    val srs: SrsStateDto?
)

data class ReviewDto(val sentence: SentenceDto, val srs: SrsStateDto)
data class ReviewCountDto(val due: Int, val tracked: Int, val mastered: Int)

// ---------- Progress ----------
data class DayXpDto(val day: String, val xp: Int, val attempts: Int)

data class StatsDto(
    @SerializedName("total_xp") val totalXp: Int,
    val level: Int,
    @SerializedName("xp_in_level") val xpInLevel: Int,
    @SerializedName("streak_count") val streakCount: Int,
    @SerializedName("longest_streak") val longestStreak: Int,
    @SerializedName("today_xp") val todayXp: Int,
    @SerializedName("daily_goal_xp") val dailyGoalXp: Int,
    @SerializedName("daily_goal_reached") val dailyGoalReached: Boolean,
    @SerializedName("total_attempts") val totalAttempts: Int,
    @SerializedName("correct_attempts") val correctAttempts: Int,
    val accuracy: Double,
    @SerializedName("sentences_seen") val sentencesSeen: Int,
    @SerializedName("sentences_mastered") val sentencesMastered: Int,
    @SerializedName("due_reviews") val dueReviews: Int,
    @SerializedName("vocabulary_size") val vocabularySize: Int,
    @SerializedName("best_combo") val bestCombo: Int,
    @SerializedName("xp_by_day") val xpByDay: List<DayXpDto>,
    @SerializedName("accuracy_by_difficulty") val accuracyByDifficulty: Map<String, Double>
)

data class LeaderboardEntryDto(
    val rank: Int,
    @SerializedName("user_id") val userId: Int,
    val username: String,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("total_xp") val totalXp: Int,
    @SerializedName("streak_count") val streakCount: Int,
    val level: Int,
    @SerializedName("is_me") val isMe: Boolean
)

data class FavoriteDto(
    @SerializedName("clip_id") val clipId: String,
    val text: String,
    val translation: String? = null
)

data class VocabularyDto(
    val id: Int,
    val word: String,
    val language: String,
    val translation: String?,
    val example: String?,
    @SerializedName("times_missed") val timesMissed: Int,
    @SerializedName("times_correct") val timesCorrect: Int,
    val mastery: Double
)

data class DailyChallengeDto(
    val day: String,
    val sentences: List<SentenceDto>,
    val completed: Boolean,
    val score: Int,
    @SerializedName("xp_earned") val xpEarned: Int
)

data class DailyChallengeSubmit(val score: Int, val total: Int)

// ---------- Media ----------
data class ResolveMediaRequest(
    val url: String,
    @SerializedName("prefer_language") val preferLanguage: String = "en",
    @SerializedName("include_subtitles") val includeSubtitles: Boolean = true,
    @SerializedName("auto_subs") val autoSubs: Boolean = true
)

data class SubtitleTrackDto(val language: String, val kind: String, val srt: String, @SerializedName("line_count") val lineCount: Int)

data class ResolvedMediaDto(
    @SerializedName("source_url") val sourceUrl: String,
    val kind: String,
    val title: String?,
    @SerializedName("video_id") val videoId: String?,
    @SerializedName("duration_seconds") val durationSeconds: Int?,
    @SerializedName("thumbnail_url") val thumbnailUrl: String?,
    @SerializedName("stream_url") val streamUrl: String?,
    val embeddable: Boolean,
    val subtitles: List<SubtitleTrackDto> = emptyList(),
    @SerializedName("available_subtitle_languages") val availableSubtitleLanguages: List<String> = emptyList(),
    val error: String?
)

data class VideoSourceDto(
    val id: Int? = null,
    val kind: String = "youtube",
    val title: String,
    val url: String? = null,
    @SerializedName("youtube_id") val youtubeId: String? = null,
    val language: String = "en",
    @SerializedName("duration_seconds") val durationSeconds: Int? = null,
    @SerializedName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerializedName("subtitle_srt") val subtitleSrt: String? = null,
    @SerializedName("progress_index") val progressIndex: Int = 0,
    @SerializedName("sentence_count") val sentenceCount: Int = 0
)

data class ProgressUpdate(@SerializedName("progress_index") val progressIndex: Int)

// ---------- AI ----------
data class AiStatusDto(val provider: String, @SerializedName("llm_enabled") val llmEnabled: Boolean, val model: String?, val hint: String?)

data class ExplainRequest(
    val text: String,
    @SerializedName("focus_words") val focusWords: List<String> = emptyList(),
    val language: String = "en",
    @SerializedName("native_language") val nativeLanguage: String = "he"
)

data class WordExplanationDto(val word: String, val translation: String, @SerializedName("part_of_speech") val partOfSpeech: String, val definition: String, val example: String)

data class ExplainDto(
    val translation: String,
    @SerializedName("word_explanations") val wordExplanations: List<WordExplanationDto>,
    @SerializedName("grammar_notes") val grammarNotes: List<String>,
    val idioms: List<String>,
    @SerializedName("cultural_note") val culturalNote: String?,
    @SerializedName("difficulty_cefr") val difficultyCefr: String,
    val provider: String,
    val cached: Boolean
)

data class SmartSrtRequest(
    @SerializedName("srt_content") val srtContent: String,
    val language: String? = null,
    @SerializedName("native_language") val nativeLanguage: String = "he",
    val translate: Boolean = true,
    @SerializedName("merge_fragments") val mergeFragments: Boolean = true,
    @SerializedName("rate_difficulty") val rateDifficulty: Boolean = true
)

data class SmartSrtLineDto(
    val index: Int,
    @SerializedName("start_ms") val startMs: Long,
    @SerializedName("end_ms") val endMs: Long,
    val text: String,
    val translation: String?,
    val cefr: String,
    @SerializedName("difficulty_score") val difficultyScore: Double,
    @SerializedName("key_words") val keyWords: List<String>
)

data class SmartSrtDto(
    val language: String,
    @SerializedName("line_count") val lineCount: Int,
    @SerializedName("merged_from") val mergedFrom: Int,
    val lines: List<SmartSrtLineDto>,
    val srt: String,
    @SerializedName("bilingual_srt") val bilingualSrt: String?,
    val provider: String
)

data class SelfTestRequest(
    @SerializedName("series_slug") val seriesSlug: String? = null,
    @SerializedName("sample_size") val sampleSize: Int = 10,
    val difficulty: String = "medium",
    @SerializedName("use_llm") val useLlm: Boolean = true
)

data class SelfTestCaseDto(
    val sentence: String,
    @SerializedName("hidden_words") val hiddenWords: List<String>,
    @SerializedName("masked_text") val maskedText: String,
    @SerializedName("solver_answer") val solverAnswer: List<String>,
    val solved: Boolean,
    @SerializedName("quality_score") val qualityScore: Double,
    val notes: String?
)

data class SelfTestDto(
    val id: Int,
    val provider: String,
    val total: Int,
    val solved: Int,
    val accuracy: Double,
    @SerializedName("avg_quality") val avgQuality: Double,
    val verdict: String,
    val cases: List<SelfTestCaseDto>
)

data class AppVersionDto(
    @SerializedName("latest_version_code") val latestVersionCode: Int,
    @SerializedName("latest_version_name") val latestVersionName: String,
    @SerializedName("download_url") val downloadUrl: String,
    @SerializedName("update_notes") val updateNotes: String,
    @SerializedName("force_update") val forceUpdate: Boolean
)

data class HealthDto(val status: String, val version: String, @SerializedName("llm_provider") val llmProvider: String)
