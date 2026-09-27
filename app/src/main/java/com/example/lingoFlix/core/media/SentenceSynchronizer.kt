package com.example.lingoFlix.core.media

import com.example.lingoFlix.domain.model.Sentence

/**
 * Maps a playback position onto the sentence list.
 *
 * The player asks for this on every frame callback, and a feature-length film
 * easily holds 2000 cues, so a linear scan is not acceptable. All lookups are
 * binary searches over the start times.
 *
 * Callers must pass a list sorted by [Sentence.startMs] with non-overlapping
 * cues; `SubtitleParser` already guarantees both invariants for parsed files.
 */
object SentenceSynchronizer {

    /**
     * Index of the sentence active at [positionMs], or `-1` when the position
     * falls before the first cue or into a gap between cues.
     *
     * A cue is active for `startMs <= positionMs < endMs`, so a position that
     * is exactly the end of one cue and the start of the next resolves to the
     * next cue.
     */
    fun indexAt(sentences: List<Sentence>, positionMs: Long): Int {
        val candidate = nearestIndexAt(sentences, positionMs)
        if (candidate < 0) return -1
        return if (positionMs < sentences[candidate].endMs) candidate else -1
    }

    /** Convenience wrapper around [indexAt] for callers that want the cue itself. */
    fun sentenceAt(sentences: List<Sentence>, positionMs: Long): Sentence? {
        val index = indexAt(sentences, positionMs)
        return if (index >= 0) sentences[index] else null
    }

    /**
     * Index of the sentence that next/previous navigation should treat as the
     * current one: the active cue, or the most recent cue already passed.
     *
     * Returns `-1` only when [positionMs] precedes the first cue, which is the
     * one case where "previous" has no meaning.
     */
    fun nearestIndexAt(sentences: List<Sentence>, positionMs: Long): Int {
        var low = 0
        var high = sentences.size - 1
        var result = -1
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (sentences[mid].startMs <= positionMs) {
                result = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return result
    }
}
