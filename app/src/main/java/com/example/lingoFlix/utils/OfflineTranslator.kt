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
        val cleanWord = word.trim().lowercase().replace(Regex("[^a-z]"), "")
        val translation = translate(word, sourceLang)
        
        val builder = StringBuilder()
        builder.append("**המילה:** $word\n")
        builder.append("**תרגום:** $translation\n\n")
        
        if (sourceLang == TranslateLanguage.ENGLISH && cleanWord.length > 2) {
            builder.append("### הטיות ודוגמאות (אופליין):\n")
            
            // Heuristic for English tenses/plural
            val forms = mutableListOf<Pair<String, String>>()
            
            if (cleanWord.endsWith("y")) {
                val base = cleanWord.dropLast(1)
                forms.add("רבים/הווה" to "${base}ies")
                forms.add("עבר" to "${base}ied")
            } else if (cleanWord.endsWith("e")) {
                forms.add("רבים/הווה" to "${cleanWord}s")
                forms.add("עבר" to "${cleanWord}d")
                forms.add("מתמשך" to "${cleanWord.dropLast(1)}ing")
            } else {
                forms.add("רבים/הווה" to "${cleanWord}s")
                forms.add("עבר" to "${cleanWord}ed")
                forms.add("מתמשך" to "${cleanWord}ing")
            }
            
            for ((label, form) in forms) {
                val formTranslation = translate(form, sourceLang)
                builder.append("• **$label ($form):** $formTranslation\n")
            }
            
            builder.append("\n**שימוש במשפט דוגמה:**\n")
            builder.append("I like to $cleanWord every day.\n")
            builder.append("*(אני אוהב/ת $translation כל יום)*\n")
        }
        
        builder.append("\n*(ניתוח אופליין מקומי)*")
        return builder.toString()
    }
}
