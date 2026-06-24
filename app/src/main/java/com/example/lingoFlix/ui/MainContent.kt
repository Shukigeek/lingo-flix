package com.example.lingoFlix.ui

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lingoFlix.data.AppDatabase
import com.example.lingoFlix.ui.navigation.BottomNavigationBar
import com.example.lingoFlix.ui.viewmodel.MainViewModel
import com.example.lingoFlix.utils.LingoLog

@Composable
fun MainContent(
    activity: ComponentActivity, 
    database: AppDatabase,
    mainViewModel: MainViewModel = viewModel()
) {
    val navigationStack = mainViewModel.navigationStack
    val currentScreen = navigationStack.lastOrNull() ?: "dashboard"

    LingoLog.d("MainContent", "Rendering screen: $currentScreen")

    BackHandler(enabled = navigationStack.size > 1) {
        try {
            mainViewModel.navigateBack()
        } catch (e: Exception) {
            LingoLog.e("MainContent", "Error navigating back", e)
        }
    }

    Scaffold(
        bottomBar = {
            if (currentScreen in listOf("dashboard", "discovery", "video_list")) {
                BottomNavigationBar(
                    currentScreen = currentScreen,
                    onNavigate = { target -> mainViewModel.navigationStack = listOf(target) }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (currentScreen) {
                "dashboard" -> DashboardRouter(mainViewModel)
                "discovery" -> DiscoveryScreen(mainViewModel.currentUser.id)
                "video_list" -> VideoListRouter(mainViewModel, database)
                "admin_dashboard" -> AdminDashboardScreen(
                    onBack = { mainViewModel.navigateBack() },
                    configDao = database.configDao(),
                    recommendedMediaDao = database.recommendedMediaDao()
                )
            }
        }
    }
}

@Composable
fun DashboardRouter(vm: MainViewModel) {
    // Isolated router for Dashboard
    DashboardScreen(
        onMyVideos = { vm.navigateTo("video_list") },
        onAdminClick = { vm.navigateTo("admin_dashboard") },
        userName = vm.currentUser.name
    )
}

@Composable
fun VideoListRouter(vm: MainViewModel, db: AppDatabase) {
    // Isolated router for Video List
    VideoListScreen(
        onVideoSelected = { /* select logic */ },
        onPracticeRequested = { _, _ -> /* practice logic */ },
        onBack = { vm.navigateBack() },
        linkedVideos = emptySet(), // Should come from VM
        onToggleLink = { /* link logic */ },
        onToggleDifficulty = { vm.quizDifficulty = it },
        onSettingsRequested = { vm.navigateTo("settings") },
        metadataDao = db.videoMetadataDao()
    )
}
