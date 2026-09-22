package com.example.lingoFlix.ui

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lingoFlix.BuildConfig
import com.example.lingoFlix.data.ServerConnectionManager
import com.example.lingoFlix.data.network.AppVersionResponse
import com.example.lingoFlix.data.network.ApiClient
import com.example.lingoFlix.ui.components.DuoButton
import kotlinx.coroutines.launch

private const val TAG = "ServerUpdateCard"

private sealed class UpdateCheckResult {
    object Idle : UpdateCheckResult()
    object Checking : UpdateCheckResult()
    object UpToDate : UpdateCheckResult()
    data class UpdateAvailable(val info: AppVersionResponse) : UpdateCheckResult()
    data class Failed(val message: String) : UpdateCheckResult()
}

/**
 * Lets the user point the app at their home server (Raspberry Pi later, a plain computer for now)
 * and check whether a newer APK build is published there.
 */
@Composable
fun ServerUpdateCard() {
    val context = LocalContext.current
    val connectionManager = remember { ServerConnectionManager(context) }
    val scope = rememberCoroutineScope()

    var serverAddressInput by remember { mutableStateOf(connectionManager.getSavedServerBaseUrl() ?: "") }
    var savedBaseUrl by remember { mutableStateOf(connectionManager.getSavedServerBaseUrl()) }
    var checkResult by remember { mutableStateOf<UpdateCheckResult>(UpdateCheckResult.Idle) }

    fun checkForUpdatesAgainstConnectedServer(baseUrl: String) {
        checkResult = UpdateCheckResult.Checking
        scope.launch {
            try {
                val api = ApiClient.buildAppApi(baseUrl)
                if (api == null) {
                    checkResult = UpdateCheckResult.Failed("לא ניתן להתחבר לשרת")
                    return@launch
                }
                val info = api.getAppVersion()
                checkResult = if (info.latestVersionCode > BuildConfig.VERSION_CODE) {
                    UpdateCheckResult.UpdateAvailable(info)
                } else {
                    UpdateCheckResult.UpToDate
                }
            } catch (e: Exception) {
                Log.e(TAG, "Update check failed for server $baseUrl", e)
                checkResult = UpdateCheckResult.Failed("בדיקת עדכון נכשלה, ודא שהשרת פועל ושאתה באותה רשת")
            }
        }
    }

    Surface(
        color = Color.White.copy(alpha = 0.85f),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color(0xFF1CB0F6), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("שרת ועדכונים", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF4B4B4B))
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = serverAddressInput,
                onValueChange = { serverAddressInput = it },
                label = { Text("כתובת שרת, למשל 192.168.1.50:8000") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DuoButton(
                    text = "התחבר",
                    onClick = {
                        val normalized = connectionManager.saveServerBaseUrl(serverAddressInput)
                        savedBaseUrl = normalized
                        serverAddressInput = normalized
                        checkResult = UpdateCheckResult.Idle
                    },
                    color = Color(0xFF1CB0F6),
                    darkColor = Color(0xFF1899D6),
                    modifier = Modifier.weight(1f)
                )
                DuoButton(
                    text = "בדוק עדכון",
                    onClick = { savedBaseUrl?.let { checkForUpdatesAgainstConnectedServer(it) } },
                    color = Color(0xFF58CC02),
                    darkColor = Color(0xFF46A302),
                    enabled = savedBaseUrl != null,
                    modifier = Modifier.weight(1f)
                )
            }

            savedBaseUrl?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text("מחובר ל: $it", fontSize = 12.sp, color = Color(0xFF777777))
            }

            when (val result = checkResult) {
                is UpdateCheckResult.Checking -> {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("בודק עדכונים...", fontSize = 13.sp, color = Color(0xFF777777))
                }
                is UpdateCheckResult.UpToDate -> {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("האפליקציה מעודכנת ✅", fontSize = 13.sp, color = Color(0xFF58CC02), fontWeight = FontWeight.Bold)
                }
                is UpdateCheckResult.Failed -> {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(result.message, fontSize = 13.sp, color = Color(0xFFEA2B2B))
                }
                is UpdateCheckResult.UpdateAvailable -> {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "גרסה חדשה זמינה: ${result.info.latestVersionName}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4B4B4B)
                    )
                    if (result.info.updateNotes.isNotBlank()) {
                        Text(result.info.updateNotes, fontSize = 12.sp, color = Color(0xFF777777))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    DuoButton(
                        text = "עדכן עכשיו",
                        onClick = {
                            if (result.info.downloadUrl.isNotBlank()) {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(result.info.downloadUrl)))
                            }
                        },
                        color = Color(0xFFFF9600),
                        darkColor = Color(0xFFCC7A00),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                UpdateCheckResult.Idle -> {}
            }
        }
    }
}
