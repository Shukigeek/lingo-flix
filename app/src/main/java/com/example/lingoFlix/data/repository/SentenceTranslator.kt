package com.example.lingoFlix.data.repository

import com.example.lingoFlix.utils.OfflineTranslator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adapter over the existing on-device translation helper.
 *
 * `OfflineTranslator` predates the clean-architecture data layer and has a
 * fixed Hebrew target, so the repository talks to this seam instead: it keeps
 * the unsupported-target rule in one documented place and lets the underlying
 * engine be replaced without touching the repository.
 */
@Singleton
class SentenceTranslator @Inject constructor() {

    /**
     * Translates [text] into [targetLanguage].
     *
     * @return a failure for any target the on-device engine cannot serve,
     *   rather than a plausible-looking wrong translation.
     */
    suspend fun translate(text: String, targetLanguage: String): Result<String> {
        if (text.isBlank()) return Result.failure(IllegalArgumentException("Nothing to translate"))

        val target = targetLanguage.trim().lowercase().substringBefore('-')
        if (target !in HEBREW_CODES) {
            return Result.failure(
                UnsupportedOperationException(
                    "On-device translation currently supports Hebrew targets only, not '$targetLanguage'"
                )
            )
        }

        return runCatching {
            val translated = OfflineTranslator.translate(text, sourceLanguageOf(text))
            // The helper reports failures in-band as a Hebrew error string;
            // surfacing that as a translation would poison the sentence cache.
            check(translated.isNotBlank() && !translated.startsWith(ERROR_PREFIX)) {
                "Translation engine returned an error: $translated"
            }
            translated
        }
    }

    private companion object {
        /** `iw` is the legacy ISO code for Hebrew and is still emitted by some locales. */
        val HEBREW_CODES = setOf("he", "iw", "heb", "hebrew")

        /** Error marker used by `OfflineTranslator.translate`. */
        const val ERROR_PREFIX = "\u05E9\u05D2\u05D9\u05D0\u05D4"

        /**
         * ML Kit needs an explicit source language; the script of the text is a
         * reliable enough signal for the languages this app targets.
         */
        fun sourceLanguageOf(text: String): String {
            for (ch in text) {
                when (ch.code) {
                    in 0x0600..0x06FF -> return "ar"
                    in 0x0400..0x04FF -> return "ru"
                    in 0x3040..0x30FF -> return "ja"
                    in 0x4E00..0x9FFF -> return "zh"
                }
            }
            return "en"
        }
    }
}
