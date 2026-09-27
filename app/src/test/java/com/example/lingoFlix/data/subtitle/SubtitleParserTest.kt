package com.example.lingoFlix.data.subtitle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.Charset

class SubtitleParserTest {

    @Test
    fun `parses a well formed SRT file`() {
        val srt = """
            1
            00:01:12,200 --> 00:01:14,800
            Where are you going?

            2
            00:01:15,000 --> 00:01:17,500
            I'm going to the store.

            3
            00:01:18,000 --> 00:01:20,000
            Do you need anything?
        """.trimIndent()

        val cues = SubtitleParser.parseText(srt)

        assertEquals(3, cues.size)
        assertEquals("Where are you going?", cues[0].text)
        assertEquals(72_200L, cues[0].startMs)
        assertEquals(74_800L, cues[0].endMs)
        assertEquals("Do you need anything?", cues[2].text)
    }

    @Test
    fun `reindexes cues sequentially from zero`() {
        val srt = """
            17
            00:00:01,000 --> 00:00:02,000
            First

            42
            00:00:03,000 --> 00:00:04,000
            Second
        """.trimIndent()

        val cues = SubtitleParser.parseText(srt)

        assertEquals(listOf(0, 1), cues.map { it.index })
    }

    @Test
    fun `joins multi line cues into one sentence`() {
        val srt = """
            1
            00:00:01,000 --> 00:00:03,000
            I don't think
            that's a good idea.
        """.trimIndent()

        val cues = SubtitleParser.parseText(srt)

        assertEquals(1, cues.size)
        assertEquals("I don't think that's a good idea.", cues[0].text)
    }

    @Test
    fun `strips html and styling tags`() {
        val srt = """
            1
            00:00:01,000 --> 00:00:03,000
            <i>Hello</i> <b>world</b>{\an8}
        """.trimIndent()

        assertEquals("Hello world", SubtitleParser.parseText(srt).single().text)
    }

    @Test
    fun `strips sound effects and speaker labels`() {
        val srt = """
            1
            00:00:01,000 --> 00:00:03,000
            [door slams]

            2
            00:00:04,000 --> 00:00:06,000
            JOHN: Get down!
        """.trimIndent()

        val cues = SubtitleParser.parseText(srt)

        // The sound-effect-only cue disappears entirely.
        assertEquals(1, cues.size)
        assertEquals("Get down!", cues[0].text)
    }

    @Test
    fun `skips malformed blocks but keeps valid ones`() {
        val srt = """
            1
            00:00:01,000 --> 00:00:02,000
            Good one

            2
            this is not a timing line
            Orphaned text

            3
            00:00:05,000 --> 00:00:06,000
            Another good one
        """.trimIndent()

        val cues = SubtitleParser.parseText(srt)

        assertEquals(2, cues.size)
        assertEquals("Good one", cues[0].text)
        assertEquals("Another good one", cues[1].text)
    }

    @Test
    fun `handles missing blank line separators`() {
        val srt = """
            1
            00:00:01,000 --> 00:00:02,000
            First line
            2
            00:00:03,000 --> 00:00:04,000
            Second line
        """.trimIndent()

        val cues = SubtitleParser.parseText(srt)

        assertEquals(2, cues.size)
        assertEquals("First line", cues[0].text)
        assertEquals("Second line", cues[1].text)
    }

    @Test
    fun `trims overlapping cues so timings never collide`() {
        val srt = """
            1
            00:00:01,000 --> 00:00:10,000
            Long one

            2
            00:00:03,000 --> 00:00:05,000
            Overlaps the previous
        """.trimIndent()

        val cues = SubtitleParser.parseText(srt)

        assertEquals(2, cues.size)
        assertTrue(
            "first cue must end before the second starts",
            cues[0].endMs <= cues[1].startMs,
        )
    }

    @Test
    fun `sorts out of order cues by start time`() {
        val srt = """
            1
            00:00:10,000 --> 00:00:11,000
            Later

            2
            00:00:01,000 --> 00:00:02,000
            Earlier
        """.trimIndent()

        val cues = SubtitleParser.parseText(srt)

        assertEquals("Earlier", cues[0].text)
        assertEquals("Later", cues[1].text)
    }

    @Test
    fun `gives zero length cues a minimum duration`() {
        val srt = """
            1
            00:00:05,000 --> 00:00:05,000
            Instant
        """.trimIndent()

        val cue = SubtitleParser.parseText(srt).single()

        assertTrue("cue must have a positive duration", cue.endMs > cue.startMs)
    }

    @Test
    fun `accepts dots instead of commas for milliseconds`() {
        val srt = """
            1
            00:00:01.500 --> 00:00:02.750
            Dotted timings
        """.trimIndent()

        val cue = SubtitleParser.parseText(srt).single()

        assertEquals(1_500L, cue.startMs)
        assertEquals(2_750L, cue.endMs)
    }

    @Test
    fun `parses WebVTT including short timestamps`() {
        val vtt = """
            WEBVTT

            00:01.000 --> 00:04.000
            Short form timing
        """.trimIndent()

        val cue = SubtitleParser.parseText(vtt).single()

        assertEquals(1_000L, cue.startMs)
        assertEquals(4_000L, cue.endMs)
        assertEquals("Short form timing", cue.text)
    }

    @Test
    fun `returns empty list for empty or garbage input`() {
        assertTrue(SubtitleParser.parseText("").isEmpty())
        assertTrue(SubtitleParser.parseText("just some prose, no timings").isEmpty())
        assertTrue(SubtitleParser.parse(ByteArray(0)).isEmpty())
    }

    @Test
    fun `counts words correctly`() {
        val srt = """
            1
            00:00:01,000 --> 00:00:03,000
            One two three four
        """.trimIndent()

        assertEquals(4, SubtitleParser.parseText(srt).single().wordCount)
    }

    // ── Encoding detection ──────────────────────────────────────────────

    @Test
    fun `detects UTF-8 with BOM`() {
        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        val bytes = bom + "hello".toByteArray(Charsets.UTF_8)

        assertEquals(Charsets.UTF_8, SubtitleParser.detectCharset(bytes))
    }

    @Test
    fun `detects UTF-16 byte order marks`() {
        val be = byteArrayOf(0xFE.toByte(), 0xFF.toByte())
        val le = byteArrayOf(0xFF.toByte(), 0xFE.toByte())

        assertEquals(Charsets.UTF_16BE, SubtitleParser.detectCharset(be))
        assertEquals(Charsets.UTF_16LE, SubtitleParser.detectCharset(le))
    }

    @Test
    fun `detects UTF-8 Hebrew without a BOM`() {
        val bytes = "שלום עולם".toByteArray(Charsets.UTF_8)

        assertEquals(Charsets.UTF_8, SubtitleParser.detectCharset(bytes))
    }

    @Test
    fun `detects Windows-1255 Hebrew`() {
        val cp1255 = Charset.forName("windows-1255")
        val bytes = "שלום עולם מה שלומך היום".toByteArray(cp1255)

        assertEquals(cp1255, SubtitleParser.detectCharset(bytes))
    }

    @Test
    fun `treats pure ASCII as UTF-8`() {
        assertEquals(Charsets.UTF_8, SubtitleParser.detectCharset("plain ascii".toByteArray()))
    }

    @Test
    fun `round trips a Windows-1255 encoded SRT file`() {
        val cp1255 = Charset.forName("windows-1255")
        val srt = """
            1
            00:00:01,000 --> 00:00:03,000
            שלום, מה שלומך היום?
        """.trimIndent()

        val cues = SubtitleParser.parse(srt.toByteArray(cp1255))

        assertEquals(1, cues.size)
        assertEquals("שלום, מה שלומך היום?", cues[0].text)
    }
}
