package com.example.lingoFlix.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the file-name heuristics. These rules are the only metadata source for
 * locally imported files, so every supported pattern is pinned by a test.
 */
class MediaNameParserTest {

    // ── Movies ──────────────────────────────────────────────────────────

    @Test
    fun `strips release junk from a scene movie name`() {
        val result = MediaNameParser.parse("The.Matrix.1999.1080p.BluRay.x264-GROUP.mkv")

        assertEquals("The Matrix", result.title)
        assertEquals(1999, result.year)
        assertFalse(result.isEpisode)
        assertNull(result.season)
        assertNull(result.episode)
        assertNull(result.seriesName)
    }

    @Test
    fun `keeps a plain movie name untouched`() {
        val result = MediaNameParser.parse("Interstellar.mp4")

        assertEquals("Interstellar", result.title)
        assertNull(result.year)
        assertFalse(result.isEpisode)
    }

    @Test
    fun `reads a year in parentheses`() {
        val result = MediaNameParser.parse("Inception (2010).mkv")

        assertEquals("Inception", result.title)
        assertEquals(2010, result.year)
    }

    @Test
    fun `ignores four digit numbers outside the plausible year range`() {
        val result = MediaNameParser.parse("Movie 1850.mkv")

        assertNull(result.year)
        assertEquals("Movie 1850", result.title)
    }

    @Test
    fun `strips resolution source codec and audio tokens`() {
        val result = MediaNameParser.parse("Some.Movie.2160p.WEB-DL.DD5.1.H.265-GRP.mkv")

        assertEquals("Some Movie", result.title)
    }

    @Test
    fun `strips 4K and HDR tokens`() {
        assertEquals("Movie", MediaNameParser.parse("Movie.4K.HDR.mkv").title)
    }

    @Test
    fun `strips bracketed release tags`() {
        assertEquals("Anime Title", MediaNameParser.parse("Anime.Title.[1080p].[GROUP].mkv").title)
    }

    @Test
    fun `strips a trailing release group after a final dash`() {
        assertEquals("Great Movie", MediaNameParser.parse("Great Movie - RARBG.mkv").title)
    }

    @Test
    fun `keeps hyphenated titles intact`() {
        val result = MediaNameParser.parse("Spider-Man.2002.mkv")

        assertEquals("Spider-Man", result.title)
        assertEquals(2002, result.year)
    }

    @Test
    fun `normalises dots and underscores to single spaces`() {
        assertEquals("My Great Movie", MediaNameParser.parse("My_Great__Movie.mkv").title)
    }

    // ── Episodes ────────────────────────────────────────────────────────

    @Test
    fun `parses the SxxEyy pattern`() {
        val result = MediaNameParser.parse("Friends.S02E04.720p.WEB-DL.mp4")

        assertTrue(result.isEpisode)
        assertEquals("Friends", result.seriesName)
        assertEquals(2, result.season)
        assertEquals(4, result.episode)
        assertEquals("Friends", result.title)
    }

    @Test
    fun `parses the short sXeY pattern case insensitively`() {
        val result = MediaNameParser.parse("Show.s1e2.mkv")

        assertTrue(result.isEpisode)
        assertEquals(1, result.season)
        assertEquals(2, result.episode)
        assertEquals("Show", result.seriesName)
    }

    @Test
    fun `parses the 1x02 pattern`() {
        val result = MediaNameParser.parse("Breaking Bad 1x02.avi")

        assertTrue(result.isEpisode)
        assertEquals("Breaking Bad", result.seriesName)
        assertEquals(1, result.season)
        assertEquals(2, result.episode)
    }

    @Test
    fun `parses the verbose season episode pattern`() {
        val result = MediaNameParser.parse("Show - Season 1 Episode 5.mkv")

        assertTrue(result.isEpisode)
        assertEquals("Show", result.seriesName)
        assertEquals(1, result.season)
        assertEquals(5, result.episode)
        assertEquals("Show", result.title)
    }

    @Test
    fun `uses the text after the marker as the episode title`() {
        val result = MediaNameParser.parse("Lost.S01E03.Tabula.Rasa.720p.mkv")

        assertEquals("Lost", result.seriesName)
        assertEquals("Tabula Rasa", result.title)
        assertEquals(1, result.season)
        assertEquals(3, result.episode)
    }

    @Test
    fun `accepts a three digit code only inside a season folder`() {
        val result = MediaNameParser.parse("Show - 102 - Pilot.mkv", listOf("Show", "Season 1"))

        assertTrue(result.isEpisode)
        assertEquals(1, result.season)
        assertEquals(2, result.episode)
        assertEquals("Show", result.seriesName)
        assertEquals("Pilot", result.title)
    }

    @Test
    fun `rejects a three digit code without a season folder`() {
        val result = MediaNameParser.parse("Show - 102 - Pilot.mkv")

        assertFalse(result.isEpisode)
        assertNull(result.episode)
    }

    @Test
    fun `falls back to the nearest non season parent folder for the series name`() {
        val result = MediaNameParser.parse("S03E07.mkv", listOf("TV", "Dexter", "Season 03"))

        assertTrue(result.isEpisode)
        assertEquals("Dexter", result.seriesName)
        assertEquals("Dexter", result.title)
        assertEquals(3, result.season)
        assertEquals(7, result.episode)
    }

    @Test
    fun `leaves the series name null when only season folders are available`() {
        val result = MediaNameParser.parse("S01E02.mkv", listOf("Season 01"))

        assertTrue(result.isEpisode)
        assertNull(result.seriesName)
        assertEquals(2, result.episode)
    }

    @Test
    fun `keeps the year alongside an episode marker`() {
        val result = MediaNameParser.parse("Show.2019.S01E02.mkv")

        assertEquals(2019, result.year)
        assertEquals(1, result.season)
        assertEquals(2, result.episode)
    }

    // ── Casing ──────────────────────────────────────────────────────────

    @Test
    fun `title cases an all uppercase name`() {
        assertEquals("The Godfather", MediaNameParser.parse("THE.GODFATHER.1972.mkv").title)
    }

    @Test
    fun `title cases an all lowercase name`() {
        assertEquals("Friends", MediaNameParser.parse("friends.s02e04.mkv").seriesName)
    }

    @Test
    fun `preserves deliberate mixed casing`() {
        assertEquals("iRobot", MediaNameParser.parse("iRobot.2004.mkv").title)
    }

    // ── Degenerate input ────────────────────────────────────────────────

    @Test
    fun `handles a file without an extension`() {
        val result = MediaNameParser.parse("Interstellar")

        assertEquals("Interstellar", result.title)
        assertFalse(result.isEpisode)
    }

    @Test
    fun `does not treat a trailing capitalised word as an extension`() {
        val result = MediaNameParser.parse("Breaking.Bad.S01E01.Pilot")

        assertEquals("Breaking Bad", result.seriesName)
        assertEquals("Pilot", result.title)
    }

    @Test
    fun `handles an empty name`() {
        val result = MediaNameParser.parse("")

        assertEquals("", result.title)
        assertFalse(result.isEpisode)
        assertNull(result.year)
    }

    @Test
    fun `never throws on punctuation only input`() {
        val result = MediaNameParser.parse("....mkv")

        assertFalse(result.isEpisode)
        assertTrue(result.title.isNotEmpty())
    }

    // ── Unicode ─────────────────────────────────────────────────────────

    @Test
    fun `cleans a hebrew movie name`() {
        val result = MediaNameParser.parse("\u05d4\u05e1\u05e8\u05d8.\u05d4\u05d2\u05d3\u05d5\u05dc.1080p.mkv")

        assertEquals("\u05d4\u05e1\u05e8\u05d8 \u05d4\u05d2\u05d3\u05d5\u05dc", result.title)
        assertFalse(result.isEpisode)
    }

    @Test
    fun `parses a hebrew series with an episode marker`() {
        val result = MediaNameParser.parse("\u05e4\u05d0\u05d5\u05d3\u05d4.S02E03.mkv")

        assertTrue(result.isEpisode)
        assertEquals("\u05e4\u05d0\u05d5\u05d3\u05d4", result.seriesName)
        assertEquals(2, result.season)
        assertEquals(3, result.episode)
    }

    @Test
    fun `strips the path prefix before parsing`() {
        val result = MediaNameParser.parse("/storage/emulated/0/Movies/Arrival.2016.mkv")

        assertEquals("Arrival", result.title)
        assertEquals(2016, result.year)
    }
}
