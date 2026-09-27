package com.example.lingoFlix.util

/**
 * Structured information recovered from a media file name.
 *
 * The importer has nothing else to work with for local files, so this is the
 * only chance to give the user a readable library instead of a list of
 * release-scene file names.
 */
data class ParsedMediaName(
    val title: String,
    val seriesName: String?,
    val season: Int?,
    val episode: Int?,
    val year: Int?,
    val isEpisode: Boolean,
)

/**
 * Turns release-scene file names into something a person can read.
 *
 * Pure Kotlin on purpose: the rules are heuristic and need to be covered by
 * fast JVM unit tests, so no Android type may leak in here.
 *
 * The heuristics are deliberately conservative — a wrong guess is shown to the
 * user, so patterns only fire when the surrounding context confirms them.
 */
object MediaNameParser {

    /**
     * Parses [fileName] into structured metadata.
     *
     * @param parentFolderNames the enclosing folders in path order (outermost
     *   first). Used to recover a series name when the file name only carries
     *   the episode marker, and to authorise the ambiguous 3-digit episode code.
     */
    fun parse(fileName: String, parentFolderNames: List<String> = emptyList()): ParsedMediaName =
        runCatching { parseInternal(fileName, parentFolderNames) }
            .getOrElse { fallback(fileName) }

    // ── Core ────────────────────────────────────────────────────────────

    private fun parseInternal(fileName: String, parents: List<String>): ParsedMediaName {
        val base = stripExtension(fileName)
        if (base.isBlank()) return ParsedMediaName(base.trim(), null, null, null, null, false)

        // Bracketed tags are always release metadata, never part of a title.
        var working = BRACKET_TAG.replace(base, " ")
        working = RESOLUTION_PAIR.replace(working, " ")

        val year = extractYear(working)
        if (year != null) working = working.removeRange(year.second)

        val marker = extractEpisode(working, parents.any(::isSeasonFolder))
        if (marker == null) {
            val title = normalizeCase(clean(working)).ifBlank { base.trim() }
            return ParsedMediaName(title, null, null, null, year?.first, false)
        }

        val beforeMarker = working.substring(0, marker.range.first)
        val afterMarker = working.substring(marker.range.last + 1)

        val seriesName = normalizeCase(clean(beforeMarker))
            .ifBlank { seriesNameFromFolders(parents).orEmpty() }
        val episodeTitle = normalizeCase(clean(afterMarker))

        val title = when {
            episodeTitle.isNotBlank() -> episodeTitle
            seriesName.isNotBlank() -> seriesName
            else -> base.trim()
        }
        return ParsedMediaName(
            title = title,
            seriesName = seriesName.ifBlank { null },
            season = marker.season,
            episode = marker.episode,
            year = year?.first,
            isEpisode = true,
        )
    }

    private fun fallback(fileName: String): ParsedMediaName {
        val title = runCatching { stripExtension(fileName).trim() }.getOrDefault(fileName)
        return ParsedMediaName(title, null, null, null, null, false)
    }

    // ── Extension ───────────────────────────────────────────────────────

    /**
     * Drops the file extension without eating parts of the title.
     *
     * A whitelist is checked first; the generic rule only accepts a short,
     * all-lowercase ASCII suffix, which keeps names such as
     * `Breaking.Bad.S01E01.Pilot` and `The.Matrix.1999` intact.
     */
    private fun stripExtension(fileName: String): String {
        val name = fileName.substringAfterLast('/').substringAfterLast('\\')
        val dot = name.lastIndexOf('.')
        if (dot <= 0) return name
        val ext = name.substring(dot + 1)
        val looksLikeExtension = ext.lowercase() in KNOWN_EXTENSIONS ||
            (ext.length in 2..4 && ext.all { it in 'a'..'z' })
        return if (looksLikeExtension) name.substring(0, dot) else name
    }

    // ── Year ────────────────────────────────────────────────────────────

    /** Returns the release year and the span it occupied, so it can be removed. */
    private fun extractYear(text: String): Pair<Int, IntRange>? {
        for (match in PAREN_YEAR.findAll(text)) {
            val year = match.groupValues[1].toIntOrNull() ?: continue
            if (year in MIN_YEAR..MAX_YEAR) return year to match.range
        }
        for (match in BARE_YEAR.findAll(text)) {
            val year = match.groupValues[1].toIntOrNull() ?: continue
            if (year in MIN_YEAR..MAX_YEAR) return year to match.range
        }
        return null
    }

    // ── Episode ─────────────────────────────────────────────────────────

    private class EpisodeMarker(val season: Int?, val episode: Int, val range: IntRange)

    /**
     * Patterns are tried strongest first. The bare 3-digit code is last and
     * requires a season folder, because on its own it is indistinguishable from
     * a bitrate or a part number.
     */
    private fun extractEpisode(text: String, insideSeasonFolder: Boolean): EpisodeMarker? {
        SXXEYY.find(text)?.let { m ->
            return EpisodeMarker(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.range)
        }
        CROSS_CODE.find(text)?.let { m ->
            return EpisodeMarker(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.range)
        }
        SEASON_EPISODE.find(text)?.let { m ->
            return EpisodeMarker(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.range)
        }
        if (insideSeasonFolder) {
            DASHED_CODE.find(text)?.let { m ->
                val code = m.groupValues[1]
                return EpisodeMarker(code.take(1).toInt(), code.drop(1).toInt(), m.range)
            }
        }
        return null
    }

    private fun isSeasonFolder(name: String): Boolean = SEASON_FOLDER.matches(name)

    /** The nearest enclosing folder that is not itself a season folder. */
    private fun seriesNameFromFolders(parents: List<String>): String? =
        parents.lastOrNull { it.isNotBlank() && !isSeasonFolder(it) }
            ?.let { normalizeCase(clean(it)) }
            ?.takeIf { it.isNotBlank() }

    // ── Cleaning ────────────────────────────────────────────────────────

    /**
     * Removes release-scene noise and normalises separators.
     *
     * Order matters: patterns that contain dots (`H.265`, `DD5.1`) must run
     * before dots are turned into spaces, and the trailing group tag must be
     * removed while its dash is still attached.
     */
    private fun clean(raw: String): String {
        var text = raw
        text = CODEC_WITH_GROUP.replace(text, " ")
        text = DOTTED_JUNK.replace(text, " ")
        text = text.replace('.', ' ').replace('_', ' ')
        text = stripTrailingGroup(text)
        text = text.split(' ')
            .filter { it.isNotBlank() && !isJunkToken(it) }
            .joinToString(" ")
        text = stripTrailingGroup(text)
        return text.trim(*TRIM_CHARS).replace(WHITESPACE, " ").trim()
    }

    /**
     * Drops a trailing release-group tag such as `- RARBG` or `-YIFY`.
     *
     * Only all-caps or digit-bearing tags are removed so hyphenated titles
     * (`Spider-Man`) survive.
     */
    private fun stripTrailingGroup(text: String): String {
        for (pattern in TRAILING_GROUP_PATTERNS) {
            val match = pattern.find(text) ?: continue
            val tag = match.groupValues[1]
            if (tag.none { it.isLowerCase() } || tag.any { it.isDigit() }) {
                return text.removeRange(match.range)
            }
        }
        return text
    }

    private fun isJunkToken(token: String): Boolean {
        val normalized = token.trim(*TOKEN_TRIM_CHARS).lowercase()
        if (normalized.isEmpty()) return false
        return normalized in JUNK_TOKENS || TECHNICAL_TOKEN.matches(normalized)
    }

    /**
     * Title-cases only when the name carries no case information of its own;
     * hand-typed names such as `iRobot` or `The Matrix` are left untouched.
     */
    private fun normalizeCase(text: String): String {
        if (text.isEmpty()) return text
        val hasUpper = text.any { it.isUpperCase() }
        val hasLower = text.any { it.isLowerCase() }
        if (hasUpper && hasLower) return text
        return text.split(' ').joinToString(" ") { word ->
            if (word.isEmpty()) word else word[0].uppercaseChar() + word.substring(1).lowercase()
        }
    }

    // ── Constants ───────────────────────────────────────────────────────

    private const val MIN_YEAR = 1900
    private const val MAX_YEAR = 2099

    private val TRIM_CHARS = charArrayOf(' ', '-', '_', '.', ',', '\u2013', '\u2014')
    private val TOKEN_TRIM_CHARS = charArrayOf('(', ')', '[', ']', '{', '}', ',', '\'', '"')

    private val KNOWN_EXTENSIONS = setOf(
        "mp4", "mkv", "avi", "mov", "webm", "m4v", "flv", "wmv", "mpg", "mpeg",
        "ts", "m2ts", "3gp", "ogv", "rmvb", "divx", "vob",
        "srt", "vtt", "ass", "ssa", "sub", "sbv",
    )

    private val WHITESPACE = Regex("\\s+")
    private val BRACKET_TAG = Regex("[\\[{][^\\[\\]{}]*[\\]}]")
    private val RESOLUTION_PAIR = Regex("""(?<![0-9])\d{3,4}x\d{3,4}(?![0-9])""")
    private val PAREN_YEAR = Regex("""\((\d{4})\)""")
    private val BARE_YEAR = Regex("""(?<![0-9])((?:19|20)\d{2})(?![0-9])""")

    private val SXXEYY = Regex("""(?<![a-z0-9])s(\d{1,2})[\s._-]*e(\d{1,3})(?![0-9])""", RegexOption.IGNORE_CASE)
    private val CROSS_CODE = Regex("""(?<![a-z0-9])(\d{1,2})x(\d{2,3})(?![0-9])""", RegexOption.IGNORE_CASE)
    private val SEASON_EPISODE = Regex(
        """season[\s._-]*(\d{1,2})[\s._-]*(?:episode|ep|e)[\s._-]*(\d{1,3})(?![0-9])""",
        RegexOption.IGNORE_CASE,
    )
    private val DASHED_CODE = Regex("""(?<![0-9])-[\s._]*(\d{3})[\s._]*-(?![0-9])""")
    private val SEASON_FOLDER = Regex(
        """\s*(?:season|saison|staffel|series|s)[\s._-]*\d{1,2}\s*""",
        RegexOption.IGNORE_CASE,
    )

    private val CODEC_WITH_GROUP = Regex(
        """(?<![a-z0-9])(?:x26[45]|h\.?26[45]|hevc|avc|xvid|divx)-[a-z0-9_]+""",
        RegexOption.IGNORE_CASE,
    )
    private val DOTTED_JUNK = Regex(
        """(?<![a-z0-9])(?:h\.26[45]|ddp?\+?[57]\.1|aac[\s._]?2\.0|[257]\.[01])(?![a-z0-9])""",
        RegexOption.IGNORE_CASE,
    )
    private val TRAILING_GROUP_PATTERNS = listOf(
        Regex("""\s-\s*([A-Za-z0-9_]{2,})$"""),
        Regex("""(?<=[A-Za-z0-9])-([A-Za-z0-9_]{2,})$"""),
    )

    /** Resolutions, bit depths and channel counts, matched as whole tokens. */
    private val TECHNICAL_TOKEN = Regex("""\d{3,4}[pi]|[0-9]{1,2}k|(?:8|10|12)bit|\d\.\dch""")

    private val JUNK_TOKENS = setOf(
        // Sources
        "bluray", "blu-ray", "bdrip", "brrip", "bdremux", "remux", "webrip",
        "web-dl", "webdl", "web", "dl", "hdtv", "pdtv", "dvdrip", "dvdscr",
        "dvd", "hdrip", "hdr", "hdr10", "sdr", "uhd",
        // Edition markers
        "proper", "repack", "extended", "unrated", "limited", "internal",
        // Codecs
        "x264", "x265", "h264", "h265", "h.264", "h.265", "hevc", "avc",
        "xvid", "divx",
        // Audio
        "aac", "aac2", "ac3", "eac3", "dts", "dts-hd", "truehd", "atmos",
        "mp3", "flac", "opus", "dd", "ddp", "dd5", "ddp5",
    )
}
