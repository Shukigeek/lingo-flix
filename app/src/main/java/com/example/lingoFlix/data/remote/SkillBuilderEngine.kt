package com.example.lingoFlix.data.remote

import android.content.Context
import com.example.lingoFlix.model.DynamicSkill
import com.example.lingoFlix.utils.LingoLog
import com.example.lingoFlix.utils.SecurityUtils
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * The "Meta-Skill": An engine that uses AI to build other skills.
 */
object SkillBuilderEngine {
    private val client = OkHttpClient()
    private val gson = Gson()
    private const val ANTHROPIC_URL = "https://api.anthropic.com/v1/messages"

    suspend fun buildSkill(context: Context, userId: String, prompt: String): DynamicSkill? = withContext(Dispatchers.IO) {
        val apiKey = SecurityUtils.getAnthropicApiKey(context, userId)
        if (apiKey.isNullOrBlank()) {
            LingoLog.e("SkillBuilder", "Anthropic API Key missing")
            return@withContext null
        }

        val systemPrompt = """
            You are the LingoFlix Skill Builder. Your job is to create a new "Skill" (functionality) for the app.
            A Skill consists of:
            1. Name
            2. Description
            3. Icon (Material Design name)
            4. LogicInstructions (How the app should handle this skill).
            
            Return ONLY a JSON object in the following format:
            {
              "id": "unique_skill_id",
              "name": "Skill Name",
              "description": "What it does",
              "iconName": "IconName",
              "logicInstructions": "Instructions for the AI executor"
            }
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("model", "claude-3-haiku-20240307")
            put("max_tokens", 1024)
            put("system", systemPrompt)
            put("messages", org.json.JSONArray().put(JSONObject().apply {
                put("role", "user")
                put("content", prompt)
            }))
        }

        val request = Request.Builder()
            .url(ANTHROPIC_URL)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string()
            if (response.isSuccessful && body != null) {
                val jsonResponse = JSONObject(body)
                val content = jsonResponse.getJSONArray("content").getJSONObject(0).getString("text")
                return@withContext gson.fromJson(content, DynamicSkill::class.java)
            } else {
                LingoLog.e("SkillBuilder", "Error: ${response.code} - ${response.message}")
            }
        } catch (e: Exception) {
            LingoLog.e("SkillBuilder", "Failed to build skill", e)
        }
        null
    }
}
