package com.example.lingoFlix.data.subtitle

import java.nio.charset.Charset

/**
 * One parsed subtitle block, before it is persisted as a sentence.
 *
 * Deliberately free of Android types so it can be unit tested on the JVM.
 */
data class ParsedCue(
    val index: Int,
    val startMs: Long,
    val endMs: Long,
    val text: String,
) {
    val durationMs: Long get() = endMs - startMs
    val wordCount: Int get() = text.split(WHITESPACE).count { it.isNotBlank() }

    private companion object {
        val WHITESPACE = Regex("\\s+")
    }
}

/**
 * Parses SRT and WebVTT subtitle files into [ParsedCue]s.
 *
 * This is the single source of truth for subtitle parsing in the app. It is
 * intentionally forgiving: real-world subtitle files are frequently malformed,
 * so a bad block is skipped rather than failing the whole file.
 */
object SubtitleParser {

    /** Cues shorter than this are almost always artefacts, not speech. */
    private const val MIN_CUE_DURATION_MS = 100L

    /** Upper bound for a single subtitle block; longer usually means bad timings. */
    private const val MAX_CUE_DURATION_MS = 30_000L

    private val TIMING_LINE = Regex(
        """(\d{1,3}):(\d{2}):(\d{2})[.,](\d{1,3})\s*-->\s*(\d{1,3}):(\d{2}):(\d{2})[.,](\d{1,3})"""
    )

    /** Two-digit VTT timestamps such as `01:22.500`. */
    private val SHORT_TIMING_LINE = Regex(
        """(\d{1,3}):(\d{2})[.,](\d{1,3})\s*-->\s*(\d{1,3}):(\d{2})[.,](\d{1,3})"""
    )

    private val HTML_TAG = Regex("<[^>]*>")
    private val BRACE_TAG = Regex("\\{[^}]*}")
    private val SPEAKER_PREFIX = Regex("^\\s*-?\\s*[A-Z][A-Z ]{1,20}:\\s*")
    private val SOUND_EFFECT = Regex("[\\[(][^\\])]*[\\])]")
    private val MULTI_SPACE = Regex("[ \\t]+")

    /**
     * Parses raw file bytes, detecting the character encoding first.
     *
     * @param stripSoundEffects removes `[door slams]` style annotations, which
     *   are noise for language learning.
     */
    fun parse(bytes: ByteArray, stripSoundEffects: Boolean = true): List<ParsedCue> {
        if (bytes.isEmpty()) return emptyList()
        val charset = detectCharset(bytes)
        val content = decode(bytes, charset)
        return parseText(content, stripSoundEffects)
    }

    /** Parses already-decoded subtitle text. */
    fun parseText(content: String, stripSoundEffects: Boolean = true): List<ParsedCue> {
        val normalized = content
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .removePrefix("\uFEFF")

        val cues = mutableListOf<ParsedCue>()
        var pendingText = StringBuilder()
        var pendingStart = -1L
        var pendingEnd = -1L

        fun flush() {
            if (pendingStart < 0) return
            val text = clean(pendingText.toString(), stripSoundEffects)
            if (text.isNotBlank()) {
                cues += ParsedCue(cues.size, pendingStart, pendingEnd, text)
            }
            pendingText = StringBuilder()
            pendingStart = -1L
            pendingEnd = -1L
        }

        val lines = normalized.lines()
        for ((i, rawLine) in lines.withIndex()) {
            val line = rawLine.trim()

            val timing = readTiming(line)
            if (timing != null) {
                // A new timing line always terminates the previous block, even
                // when the file is missing the blank separator line.
                flush()
                pendingStart = timing.first
                pendingEnd = timing.second
                continue
            }

            if (line.isEmpty()) {
                flush()
                continue
            }

            if (line.startsWith("WEBVTT") || line.startsWith("NOTE ")) continue

            // A bare number directly followed by a timing line is the next
            // block's sequence number, not dialogue. Files that omit the blank
            // separator line are common, so this look-ahead is required.
            if (line.all { it.isDigit() } && nextMeaningfulLineIsTiming(lines, i)) {
                flush()
                continue
            }

            if (pendingStart < 0) continue

            if (pendingText.isNotEmpty()) pendingText.append(' ')
            pendingText.append(line)
        }
        flush()

        return sanitize(cues)
    }

    /**
     * Enforces invariants the rest of the app relies on: cues are ordered by
     * start time, never overlap, and have a sane duration.
     */
    private fun sanitize(cues: List<ParsedCue>): List<ParsedCue> {
        if (cues.isEmpty()) return cues

        val sorted = cues.sortedBy { it.startMs }
        val result = ArrayList<ParsedCue>(sorted.size)

        for (cue in sorted) {
            var end = cue.endMs
            if (end <= cue.startMs) end = cue.startMs + MIN_CUE_DURATION_MS
            if (end - cue.startMs > MAX_CUE_DURATION_MS) end = cue.startMs + MAX_CUE_DURATION_MS

            // Trim an overlap with the previous cue so binary search over start
            // times stays unambiguous.
            val previous = result.lastOrNull()
            if (previous != null && previous.endMs > cue.startMs) {
                result[result.lastIndex] = previous.copy(
                    endMs = maxOf(cue.startMs, previous.startMs + MIN_CUE_DURATION_MS)
                )
            }

            result += cue.copy(index = result.size, endMs = end)
        }
        return result
    }

    /** True when the next non-blank line after [from] is a timing line. */
    private fun nextMeaningfulLineIsTiming(lines: List<String>, from: Int): Boolean {
        for (j in from + 1 until lines.size) {
            val candidate = lines[j].trim()
            if (candidate.isEmpty()) continue
            return readTiming(candidate) != null
        }
        return false
    }

    private fun readTiming(line: String): Pair<Long, Long>? {
        TIMING_LINE.find(line)?.let { m ->
            val g = m.groupValues
            return toMs(g[1], g[2], g[3], g[4]) to toMs(g[5], g[6], g[7], g[8])
        }
        SHORT_TIMING_LINE.find(line)?.let { m ->
            val g = m.groupValues
            return toMs("0", g[1], g[2], g[3]) to toMs("0", g[4], g[5], g[6])
        }
        return null
    }

    private fun toMs(hours: String, minutes: String, seconds: String, fraction: String): Long {
        // A 1- or 2-digit fraction means tenths/hundredths, not milliseconds.
        val millis = fraction.padEnd(3, '0').take(3).toLong()
        return hours.toLong() * 3_600_000 +
            minutes.toLong() * 60_000 +
            seconds.toLong() * 1_000 +
            millis
    }

    private fun clean(raw: String, stripSoundEffects: Boolean): String {
        var text = raw
            .replace(HTML_TAG, "")
            .replace(BRACE_TAG, "")
        if (stripSoundEffects) {
            text = text.replace(SOUND_EFFECT, "")
        }
        text = text
            .replace(SPEAKER_PREFIX, "")
            .replace(MULTI_SPACE, " ")
            .trim()
        return text
    }

    private fun decode(bytes: ByteArray, charset: Charset): String {
        val text = String(bytes, charset)
        return text.removePrefix("\uFEFF")
    }

    /**
     * Best-effort character set detection.
     *
     * Order matters: byte-order marks are authoritative, then a strict UTF-8
     * validation, and only then the legacy single-byte heuristics. Hebrew SRT
     * files in the wild are very often Windows-1255.
     */
    fun detectCharset(bytes: ByteArray): Charset {
        if (bytes.size >= 3 &&
            bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()
        ) return Charsets.UTF_8
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) return Charsets.UTF_16BE
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) return Charsets.UTF_16LE

        if (isValidUtf8(bytes)) return Charsets.UTF_8

        var hebrew = 0
        var arabic = 0
        var highBytes = 0
        for (b in bytes) {
            val v = b.toInt() and 0xFF
            if (v < 0x80) continue
            highBytes++
            if (v in 0xE0..0xFA) hebrew++
            if (v in 0xC1..0xDF) arabic++
        }

        return when {
            highBytes == 0 -> Charsets.UTF_8
            hebrew > highBytes / 2 -> charsetOrDefault("windows-1255")
            arabic > highBytes / 2 -> charsetOrDefault("windows-1256")
            else -> charsetOrDefault("windows-1252")
        }
    }

    private fun charsetOrDefault(name: String): Charset =
        runCatching { Charset.forName(name) }.getOrDefault(Charsets.UTF_8)

    /** Strict UTF-8 validation, including rejection of overlong sequences. */
    private fun isValidUtf8(bytes: ByteArray): Boolean {
        var i = 0
        var sawMultibyte = false
        while (i < bytes.size) {
            val b = bytes[i].toInt() and 0xFF
            val continuationBytes = when {
                b <= 0x7F -> 0
                b in 0xC2..0xDF -> 1
                b in 0xE0..0xEF -> 2
                b in 0xF0..0xF4 -> 3
                else -> return false
            }
            if (continuationBytes > 0) sawMultibyte = true
            if (i + continuationBytes >= bytes.size) return false
            for (j in 1..continuationBytes) {
                if ((bytes[i + j].toInt() and 0xC0) != 0x80) return false
            }
            i += continuationBytes + 1
        }
        // Pure ASCII is valid UTF-8, and decoding it as UTF-8 is always safe.
        return sawMultibyte || bytes.isNotEmpty()
    }
}
