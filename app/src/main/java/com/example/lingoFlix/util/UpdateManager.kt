package com.example.lingoFlix.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.example.lingoFlix.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Handles checking for, downloading and installing app updates directly from
 * GitHub Releases (the app is distributed outside of Google Play).
 *
 * The GitHub REST API for the latest release is public and requires no auth:
 *   https://api.github.com/repos/<owner>/<repo>/releases/latest
 */
object UpdateManager {

    private const val TAG = "UpdateManager"

    // GitHub repository that hosts the released APKs.
    private const val REPO_OWNER = "Shukigeek"
    private const val REPO_NAME = "lingo-flix"

    private const val LATEST_RELEASE_URL =
        "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest"

    /** Result of a successful update check when a newer version is available. */
    data class UpdateInfo(
        val versionName: String,
        val downloadUrl: String,
        val releaseNotes: String
    )

    /**
     * Queries GitHub for the latest release and returns [UpdateInfo] only when a
     * newer version than the currently installed one is available.
     * Returns null on any error or when the app is already up to date.
     */
    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val json = fetchString(LATEST_RELEASE_URL) ?: return@withContext null
            val obj = JSONObject(json)

            val tag = obj.optString("tag_name").ifBlank { obj.optString("name") }
            val notes = obj.optString("body")

            // Find the first .apk asset in the release.
            val assets = obj.optJSONArray("assets")
            var apkUrl: String? = null
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url")
                        break
                    }
                }
            }

            if (tag.isBlank() || apkUrl.isNullOrBlank()) {
                Log.w(TAG, "No tag or APK asset found in latest release")
                return@withContext null
            }

            val remoteVersion = tag.trimStart('v', 'V').trim()
            if (isNewerVersion(remoteVersion, BuildConfig.VERSION_NAME)) {
                UpdateInfo(remoteVersion, apkUrl, notes)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Update check failed", e)
            null
        }
    }

    /**
     * Downloads the APK from [downloadUrl] into the app cache and returns the file.
     * [onProgress] receives a value in 0..100 (or -1 when total size is unknown).
     */
    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        onProgress: (Int) -> Unit = {}
    ): File? = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            // Clean up any previous downloads.
            dir.listFiles()?.forEach { it.delete() }
            val outFile = File(dir, "lingoflix-update.apk")

            var conn = URL(downloadUrl).openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = true
            conn.connectTimeout = 15000
            conn.readTimeout = 30000
            conn.connect()

            // Manually follow redirects (GitHub redirects to a CDN).
            var redirects = 0
            while (conn.responseCode in listOf(
                    HttpURLConnection.HTTP_MOVED_PERM,
                    HttpURLConnection.HTTP_MOVED_TEMP,
                    HttpURLConnection.HTTP_SEE_OTHER,
                    307, 308
                ) && redirects < 5
            ) {
                val location = conn.getHeaderField("Location")
                conn.disconnect()
                conn = URL(location).openConnection() as HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 30000
                conn.connect()
                redirects++
            }

            val total = conn.contentLength
            conn.inputStream.use { input ->
                outFile.outputStream().use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var downloaded = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (total > 0) {
                            onProgress(((downloaded * 100) / total).toInt())
                        } else {
                            onProgress(-1)
                        }
                    }
                }
            }
            conn.disconnect()
            outFile
        } catch (e: Exception) {
            Log.e(TAG, "APK download failed", e)
            null
        }
    }

    /**
     * Launches the system package installer for the downloaded [apkFile].
     * On Android O+, if the app is not allowed to install unknown apps, the user
     * is sent to the relevant settings screen first.
     */
    fun installApk(context: Context, apkFile: File) {
        // Ensure the user has granted the "install unknown apps" permission.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            val settingsIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(settingsIntent)
            return
        }

        val apkUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(installIntent)
    }

    /**
     * Compares two dotted version names (e.g. "1.2.1"). Returns true when
     * [remote] represents a strictly newer version than [local].
     */
    fun isNewerVersion(remote: String, local: String): Boolean {
        val r = parseVersion(remote)
        val l = parseVersion(local)
        val size = maxOf(r.size, l.size)
        for (i in 0 until size) {
            val rp = r.getOrElse(i) { 0 }
            val lp = l.getOrElse(i) { 0 }
            if (rp != lp) return rp > lp
        }
        return false
    }

    private fun parseVersion(version: String): List<Int> =
        version.split(".", "-", "_")
            .mapNotNull { part -> part.filter { it.isDigit() }.toIntOrNull() }

    private fun fetchString(urlString: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 15000
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "LingoFlix-App")
            }
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                Log.w(TAG, "GitHub API returned ${conn.responseCode}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network request failed", e)
            null
        } finally {
            conn?.disconnect()
        }
    }
}
