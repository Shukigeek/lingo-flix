package com.example.lingoFlix.data.remote

import android.util.Log
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

/** Builds (and rebuilds, when the base URL changes) the Retrofit client. */
class ApiClient(private val session: SessionManager) {
    private var cachedBaseUrl: String? = null
    private var cachedApi: LingoApi? = null

    val api: LingoApi
        get() {
            val url = session.baseUrl
            val existing = cachedApi
            if (existing != null && cachedBaseUrl == url) return existing
            return build(url).also {
                cachedApi = it
                cachedBaseUrl = url
            }
        }

    private fun build(baseUrl: String): LingoApi {
        val auth = Interceptor { chain ->
            val token = session.token
            val req = if (token.isNullOrBlank()) chain.request() else chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
            chain.proceed(req)
        }
        val logging = HttpLoggingInterceptor { Log.d("LingoApi", it) }.apply { level = HttpLoggingInterceptor.Level.BASIC }
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS) // yt-dlp resolution and LLM calls can be slow
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(auth)
            .addInterceptor(logging)
            .build()
        val gson = GsonBuilder().setLenient().create()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(LingoApi::class.java)
    }
}
