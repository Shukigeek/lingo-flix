package com.example.lingoFlix

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.lingoFlix.data.AppDatabase
import com.example.lingoFlix.ui.MainContent
import com.example.lingoFlix.ui.theme.LingoFlixTheme
import com.example.lingoFlix.utils.FileUtils
import com.example.lingoFlix.utils.LingoLog

/**
 * Entry point for LingoFlix.
 * Adheres to the 300-line rule by delegating UI logic to MainContent.
 */
class MainActivity : ComponentActivity() {
    private val className = "MainActivity"
    private val database by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LingoLog.d(className, "onCreate started")
        
        try {
            enableEdgeToEdge()
            setImmersiveMode(window)

            setContent {
                LingoFlixTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Transparent
                    ) {
                        MainContent(this, database)
                    }
                }
            }
            
            handleIntent(intent)
        } catch (e: Exception) {
            LingoLog.e(className, "Critical failure in onCreate", e)
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent?) {
        try {
            if (intent?.action == android.content.Intent.ACTION_SEND) {
                val uri = intent.getParcelableExtra<Uri>(android.content.Intent.EXTRA_STREAM)
                uri?.let { saveVideo(it) }
            }
        } catch (e: Exception) {
            LingoLog.e(className, "Error handling intent", e)
        }
    }

    private fun saveVideo(uri: Uri) {
        try {
            val name = FileUtils.getFileName(this, uri) ?: "video_${System.currentTimeMillis()}.mp4"
            val savedFile = FileUtils.saveVideoToInternalStorage(this, uri, name)
            if (savedFile != null) {
                Toast.makeText(this, "סרטון $name נשמר!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            LingoLog.e(className, "Failed to save video", e)
        }
    }

    private fun setImmersiveMode(window: android.view.Window) {
        try {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.navigationBars())
            controller.show(WindowInsetsCompat.Type.statusBars())
        } catch (e: Exception) {
            LingoLog.e(className, "Error setting immersive mode", e)
        }
    }
}
