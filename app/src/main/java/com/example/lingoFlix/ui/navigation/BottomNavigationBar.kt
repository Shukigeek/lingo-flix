package com.example.lingoFlix.ui.navigation

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun BottomNavigationBar(
    currentScreen: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = Color.White.copy(alpha = 0.9f),
        modifier = Modifier.clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Dashboard, "Dashboard") },
            label = { Text("ראשי") },
            selected = currentScreen == "dashboard",
            onClick = { onNavigate("dashboard") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Explore, "Discovery") },
            label = { Text("גילוי") },
            selected = currentScreen == "discovery",
            onClick = { onNavigate("discovery") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.VideoLibrary, "Videos") },
            label = { Text("הסרטונים שלי") },
            selected = currentScreen == "video_list",
            onClick = { onNavigate("video_list") }
        )
    }
}
