package com.example.lingoFlix.utils

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class SecurityUtilsTest {

    @Test
    fun `saveUserApiKey stores key correctly`() {
        val context = mockk<Context>()
        val prefs = mockk<SharedPreferences>()
        val editor = mockk<SharedPreferences.Editor>()

        every { context.getSharedPreferences(any(), any()) } returns prefs
        every { prefs.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.apply() } returns Unit

        SecurityUtils.saveUserApiKey(context, "testUser", "testKey")

        verify { editor.putString("gemini_api_key_testUser", "testKey") }
        verify { editor.apply() }
    }

    @Test
    fun `getUserApiKey retrieves correct key`() {
        val context = mockk<Context>()
        val prefs = mockk<SharedPreferences>()

        every { context.getSharedPreferences(any(), any()) } returns prefs
        every { prefs.getString("gemini_api_key_testUser", null) } returns "testKey"

        val key = SecurityUtils.getUserApiKey(context, "testUser")
        assertEquals("testKey", key)
    }
}
