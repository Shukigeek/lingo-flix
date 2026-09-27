package com.example.lingoFlix.core.media

import com.example.lingoFlix.domain.model.Sentence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The binary search powers the highlighted subtitle line, so an off-by-one is
 * immediately visible to the user. Boundaries and gaps are pinned explicitly,
 * and the large-list case is verified against a naive linear scan.
 */
class SentenceSynchronizerTest {

    private fun cue(index: Int, startMs: Long, endMs: Long) = Sentence(
        id = index.toLong() + 1,
        subtitleTrackId = 1,
        index = index,
        startMs = startMs,
        endMs = endMs,
        text = "line $index",
    )

    /** 0..1000, gap, 2000..3000, 3000..4000 (touching), gap, 5000..6000. */
    private val sentences = listOf(
        cue(0, 0, 1_000),
        cue(1, 2_000, 3_000),
        cue(2, 3_000, 4_000),
        cue(3, 5_000, 6_000),
    )

    @Test
    fun `empty list has no active index`() {
        assertEquals(-1, SentenceSynchronizer.indexAt(emptyList(), 1_000))
    }

    @Test
    fun `empty list has no nearest index`() {
        assertEquals(-1, SentenceSynchronizer.nearestIndexAt(emptyList(), 1_000))
    }

    @Test
    fun `empty list yields no sentence`() {
        assertNull(SentenceSynchronizer.sentenceAt(emptyList(), 1_000))
    }

    @Test
    fun `position before the first cue is inactive`() {
        val early = listOf(cue(0, 500, 1_000))

        assertEquals(-1, SentenceSynchronizer.indexAt(early, 0))
        assertEquals(-1, SentenceSynchronizer.nearestIndexAt(early, 0))
    }

    @Test
    fun `exact start boundary activates the cue`() {
        assertEquals(1, SentenceSynchronizer.indexAt(sentences, 2_000))
    }

    @Test
    fun `exact end boundary leaves the cue when a gap follows`() {
        assertEquals(-1, SentenceSynchronizer.indexAt(sentences, 1_000))
    }

    @Test
    fun `touching cues resolve to the later one at the shared boundary`() {
        assertEquals(2, SentenceSynchronizer.indexAt(sentences, 3_000))
    }

    @Test
    fun `position inside a cue returns that cue`() {
        assertEquals(0, SentenceSynchronizer.indexAt(sentences, 500))
        assertEquals(3, SentenceSynchronizer.indexAt(sentences, 5_999))
    }

    @Test
    fun `position in a gap has no active cue but keeps the previous one`() {
        assertEquals(-1, SentenceSynchronizer.indexAt(sentences, 1_500))
        assertEquals(0, SentenceSynchronizer.nearestIndexAt(sentences, 1_500))
    }

    @Test
    fun `position after the last cue keeps the last cue as nearest`() {
        assertEquals(-1, SentenceSynchronizer.indexAt(sentences, 10_000))
        assertEquals(3, SentenceSynchronizer.nearestIndexAt(sentences, 10_000))
    }

    @Test
    fun `sentenceAt returns the matching cue`() {
        assertEquals("line 2", SentenceSynchronizer.sentenceAt(sentences, 3_500)?.text)
    }

    @Test
    fun `sentenceAt returns null inside a gap`() {
        assertNull(SentenceSynchronizer.sentenceAt(sentences, 4_500))
    }

    @Test
    fun `single element list behaves at every boundary`() {
        val single = listOf(cue(0, 1_000, 2_000))

        assertEquals(-1, SentenceSynchronizer.indexAt(single, 999))
        assertEquals(0, SentenceSynchronizer.indexAt(single, 1_000))
        assertEquals(0, SentenceSynchronizer.indexAt(single, 1_999))
        assertEquals(-1, SentenceSynchronizer.indexAt(single, 2_000))
        assertEquals(0, SentenceSynchronizer.nearestIndexAt(single, 2_000))
    }

    @Test
    fun `matches a linear scan across a large list`() {
        val large = (0 until 5_000).map { cue(it, it * 1_000L, it * 1_000L + 700L) }

        for (position in longArrayOf(0, 699, 700, 999, 1_000, 2_500_000, 4_999_699, 4_999_999, 9_000_000)) {
            val expected = large.indexOfFirst { position >= it.startMs && position < it.endMs }
            assertEquals(
                "position=$position",
                expected,
                SentenceSynchronizer.indexAt(large, position),
            )
        }
    }

    @Test
    fun `nearest index matches a linear scan across a large list`() {
        val large = (0 until 2_000).map { cue(it, it * 1_000L, it * 1_000L + 700L) }

        for (position in 0L until 20_000L step 137) {
            val expected = large.indexOfLast { it.startMs <= position }
            assertEquals(
                "position=$position",
                expected,
                SentenceSynchronizer.nearestIndexAt(large, position),
            )
        }
    }
}
