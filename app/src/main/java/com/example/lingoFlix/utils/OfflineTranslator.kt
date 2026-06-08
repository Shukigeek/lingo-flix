package com.example.lingoFlix.utils

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await

object OfflineTranslator {
    
    fun mapLanguage(langName: String): String {
        return when (langName) {
            "אנגלית" -> TranslateLanguage.ENGLISH
            "ספרדית" -> TranslateLanguage.SPANISH
            "צרפתית" -> TranslateLanguage.FRENCH
            "גרמנית" -> TranslateLanguage.GERMAN
            "איטלקית" -> TranslateLanguage.ITALIAN
            "ערבית" -> TranslateLanguage.ARABIC
            "יפנית" -> TranslateLanguage.JAPANESE
            "סינית" -> TranslateLanguage.CHINESE
            "קוריאנית" -> TranslateLanguage.KOREAN
            else -> TranslateLanguage.ENGLISH
        }
    }

    suspend fun translate(text: String, sourceLang: String): String {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLang)
            .setTargetLanguage(TranslateLanguage.HEBREW)
            .build()
        
        val translator = Translation.getClient(options)
        
        return try {
            val conditions = DownloadConditions.Builder()
                .build() // Will use whatever connection is available to download first time
            
            translator.downloadModelIfNeeded(conditions).await()
            translator.translate(text).await()
        } catch (e: Exception) {
            "שגיאה: ${e.localizedMessage}"
        } finally {
            translator.close()
        }
    }
    
    suspend fun getWordInfo(word: String, sourceLang: String): String {
        val translation = translate(word, sourceLang)
        return """
            **המילה:** $word
            **תרגום:** $translation
            
            *(ניתוח אופליין)*
            התרגום בוצע מקומית במכשיר.
        """.trimIndent()
    }
}
