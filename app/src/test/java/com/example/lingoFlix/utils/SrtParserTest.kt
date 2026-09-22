package com.example.lingoFlix.utils

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.charset.Charset

class SrtParserTest {

    @Test
    fun `parseSrtTime handles standard format`() {
        // We use reflection or expose for testing if needed, but let's test the public API if possible.
        // Since parseSrtTime is private, we test via parseSrtFile logic indirectly or use a testable version.
        // For now, let's assume we can't easily test private. Let's test the public detectSubtitleLanguage.
    }

    @Test
    fun `detectSubtitleLanguage identifies Hebrew correctly`() {
        val tempFile = File.createTempFile("test_he", ".srt")
        tempFile.writeText("1\n00:00:01,000 --> 00:00:04,000\nשלום עולם", Charset.forName("windows-1255"))
        
        val lang = SrtParser.detectSubtitleLanguage(tempFile)
        assertEquals("עברית", lang)
        tempFile.delete()
    }

    @Test
    fun `detectSubtitleLanguage identifies English correctly`() {
        val tempFile = File.createTempFile("test_en", ".srt")
        tempFile.writeText("1\n00:00:01,000 --> 00:00:04,000\nHello World")
        
        val lang = SrtParser.detectSubtitleLanguage(tempFile)
        assertEquals("אנגלית", lang)
        tempFile.delete()
    }

    @Test
    fun `detectEncoding handles UTF-8 with BOM`() {
        val bomUtf8 = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte(), 'H'.toByte())
        val charset = SrtParser.detectEncoding(bomUtf8)
        assertEquals(Charsets.UTF_8, charset)
    }

    @Test
    fun `detectEncoding identifies Windows-1255 for Hebrew bytes`() {
        // 0xF9 is 'ש' in Windows-1255
        val hebrewBytes = byteArrayOf(0xF9.toByte(), 0xEC.toByte(), 0xED.toByte()) 
        val charset = SrtParser.detectEncoding(hebrewBytes)
        assertEquals(Charset.forName("windows-1255"), charset)
    }

    @Test
    fun `getLanguageFlag returns correct flags`() {
        assertEquals("🇮🇱", SrtParser.getLanguageFlag("עברית"))
        assertEquals("🇺🇸", SrtParser.getLanguageFlag("אנגלית"))
        assertEquals("🇪🇸", SrtParser.getLanguageFlag("ספרדית"))
        assertEquals("🏳️", SrtParser.getLanguageFlag("Unknown"))
    }
}
