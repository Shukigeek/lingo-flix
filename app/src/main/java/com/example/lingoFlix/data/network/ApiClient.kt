package com.example.lingoFlix.data.network

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

private const val TAG = "ApiClient"

/**
 * Builds a fresh Retrofit client pointed at whatever server base URL the user connected to.
 * Not a singleton on purpose: the user can reconnect to a different server (e.g. Raspberry Pi vs test PC).
 */
object ApiClient {
    fun buildAppApi(baseUrl: String): AppApi? {
        return try {
            val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
            val client = OkHttpClient.Builder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()
            Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(AppApi::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to build API client for base URL $baseUrl", e)
            null
        }
    }
}
