package com.example.lingoFlix.ui.player

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.lingoFlix.R
import com.example.lingoFlix.ui.components.ErrorState
import com.example.lingoFlix.ui.components.ImmersiveModeEffect
import com.example.lingoFlix.ui.components.LoadingState
import com.example.lingoFlix.ui.theme.LingoRadius
import com.example.lingoFlix.ui.theme.LingoSpacing
import com.example.lingoFlix.ui.theme.LingoTextStyles
import kotlinx.coroutines.delay

@Composable
fun PlayerRoute(
    onBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Immersive mode: hide system bars while in player
    ImmersiveModeEffect(enabled = true)

    PlayerScreen(
        state = state,
        actions = PlayerActions(
            onBack = onBack,
            onPlayPauseToggle = { /* handled by ExoPlayer */ },
            onSeek = { pos -> viewModel.onPositionChanged(pos) },
            onSeekToSentence = { idx -> viewModel.seekToSentence(idx) },
            onNextSentence = viewModel::nextSentence,
            onPrevSentence = viewModel::prevSentence,
            onToggleLoop = viewModel::toggleLoop,
            onToggleTranslation = viewModel::toggleTranslation,
            onToggleFavorite = viewModel::toggleFavorite,
            onSpeedChange = { /* speed */ },
            onSaveProgress = viewModel::saveProgress,
        ),
    )
}

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    state: PlayerUiState,
    actions: PlayerActions,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showControls by remember { mutableStateOf(true) }

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    LaunchedEffect(state.videoUri) {
        if (state.videoUri.isNotBlank()) {
            val mediaItem = MediaItem.fromUri(Uri.parse(state.videoUri))
            exoPlayer.setMediaItem(mediaItem, state.positionMs)
            exoPlayer.prepare()
        }
    }

    // Position reporting and looping loop
    LaunchedEffect(exoPlayer) {
        while (true) {
            val pos = exoPlayer.currentPosition
            actions.onSeek(pos)
            if (exoPlayer.isPlaying && exoPlayer.duration > 0) {
                actions.onSaveProgress(pos, pos >= exoPlayer.duration - 1000)
            }
            delay(100)
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (state.isLoading) {
            LoadingState(modifier = Modifier.align(Alignment.Center))
            return@Box
        }

        if (state.errorMessage != null) {
            ErrorState(
                message = state.errorMessage,
                onRetry = actions.onBack,
                modifier = Modifier.align(Alignment.Center),
            )
            return@Box
        }

        // ExoPlayer View
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false // We build our own clean learning controls
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Subtitle Overlay (Bottom center)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 120.dp, start = 24.dp, end = 24.dp),
        ) {
            state.currentSentence?.let { sentence ->
                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(LingoRadius.md),
                    modifier = Modifier.align(Alignment.Center),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = LingoSpacing.lg, vertical = LingoSpacing.md),
                    ) {
                        Text(
                            text = sentence.text,
                            style = LingoTextStyles.SubtitlePrimary,
                            color = Color.White,
                        )
                        if (state.isTranslationVisible && !sentence.translation.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(LingoSpacing.xs))
                            Text(
                                text = sentence.translation!!,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }

        // Top Bar & Learning Controls (Fade on tap)
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top bar
                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(horizontal = LingoSpacing.md, vertical = LingoSpacing.sm),
                    ) {
                        IconButton(onClick = actions.onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.common_back),
                                tint = Color.White,
                            )
                        }
                        Spacer(modifier = Modifier.width(LingoSpacing.sm))
                        Text(
                            text = state.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // Bottom Learning Controls Bar
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
                ) {
                    Column(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(LingoSpacing.md),
                    ) {
                        // Progress bar
                        Slider(
                            value = if (state.durationMs > 0) state.positionMs.toFloat() / state.durationMs else 0f,
                            onValueChange = { fraction ->
                                val target = (fraction * state.durationMs).toLong()
                                exoPlayer.seekTo(target)
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                            ),
                        )

                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(top = LingoSpacing.sm),
                        ) {
                            // Prev sentence
                            IconButton(onClick = actions.onPrevSentence) {
                                Icon(Icons.Default.SkipPrevious, stringResource(R.string.player_previous_sentence), tint = Color.White)
                            }

                            // Loop toggle
                            IconButton(
                                onClick = actions.onToggleLoop,
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = if (state.isLooping) MaterialTheme.colorScheme.primary else Color.Transparent,
                                ),
                            ) {
                                Icon(
                                    Icons.Default.Repeat,
                                    stringResource(R.string.player_loop),
                                    tint = if (state.isLooping) Color.White else Color.White,
                                )
                            }

                            // Play / Pause
                            IconButton(
                                onClick = {
                                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                                },
                            ) {
                                Icon(
                                    if (exoPlayer.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                )
                            }

                            // Translation toggle
                            IconButton(onClick = actions.onToggleTranslation) {
                                Icon(
                                    if (state.isTranslationVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    stringResource(R.string.player_translation),
                                    tint = Color.White,
                                )
                            }

                            // Favorite / Save sentence
                            IconButton(onClick = actions.onToggleFavorite) {
                                Icon(
                                    if (state.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                    stringResource(R.string.player_save_sentence),
                                    tint = if (state.isFavorite) MaterialTheme.colorScheme.primary else Color.White,
                                )
                            }

                            // Next sentence
                            IconButton(onClick = actions.onNextSentence) {
                                Icon(Icons.Default.SkipNext, stringResource(R.string.player_next_sentence), tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
