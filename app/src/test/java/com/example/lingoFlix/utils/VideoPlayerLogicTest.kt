package com.example.lingoFlix.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoPlayerLogicTest {

    @Test
    fun `prepareQuiz with easy difficulty should hide exactly one word`() {
        val text = "The quick brown fox jumps over the lazy dog"
        val (hiddenIndices, words) = VideoPlayerLogic.prepareQuiz(text, "קל", "typing")
        
        assertEquals(1, hiddenIndices.size)
        assertTrue(words.isNotEmpty())
    }

    @Test
    fun `prepareQuiz with medium difficulty should hide approximately 40 percent of words`() {
        val text = "This is a longer sentence with many words to test the medium difficulty setting properly"
        // 16 words, some short. validIndices will be around 13-14.
        val (hiddenIndices, words) = VideoPlayerLogic.prepareQuiz(text, "בינוני", "typing")
        
        val validIndicesCount = words.indices.filter { words[it].length > 1 && words[it].any { c -> c.isLetter() } }.size
        val expectedHideCount = (validIndicesCount * 0.4).toInt().coerceAtLeast(1)
        
        assertEquals(expectedHideCount, hiddenIndices.size)
    }

    @Test
    fun `prepareQuiz with hard difficulty should hide approximately 70 percent of words`() {
        val text = "This is a longer sentence with many words to test the hard difficulty setting properly"
        val (hiddenIndices, words) = VideoPlayerLogic.prepareQuiz(text, "קשה", "typing")
        
        val validIndicesCount = words.indices.filter { words[it].length > 1 && words[it].any { c -> c.isLetter() } }.size
        val expectedHideCount = (validIndicesCount * 0.7).toInt().coerceAtLeast(1)
        
        assertEquals(expectedHideCount, hiddenIndices.size)
    }

    @Test
    fun `prepareQuiz for multiple choice should hide exactly one word`() {
        val text = "The quick brown fox jumps over the lazy dog"
        val (hiddenIndices, _) = VideoPlayerLogic.prepareQuiz(text, "קשה", "multiple_choice")
        
        assertEquals(1, hiddenIndices.size)
    }

    @Test
    fun `getPreferredAudioLang returns correct codes`() {
        assertEquals("he", VideoPlayerLogic.getPreferredAudioLang("עברית"))
        assertEquals("en", VideoPlayerLogic.getPreferredAudioLang("אנגלית"))
        assertEquals("es", VideoPlayerLogic.getPreferredAudioLang("ספרדית"))
    }
}
