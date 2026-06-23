package com.example.lingoFlix.ui.components

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.lingoFlix.utils.LingoLog

@Composable
fun YoutubePlayer(videoId: String, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            try {
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    webViewClient = WebViewClient()
                    loadUrl("https://www.youtube.com/embed/$videoId?autoplay=1&mute=0")
                }
            } catch (e: Exception) {
                LingoLog.e("YoutubePlayer", "Failed to initialize WebView", e)
                WebView(context)
            }
        },
        modifier = modifier
    )
}
