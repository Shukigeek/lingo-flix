package com.example.lingoFlix.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lingoFlix.model.VideoMetadata
import com.example.lingoFlix.utils.FileUtils
import com.example.lingoFlix.utils.LingoLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryItem(
    file: File,
    metadata: VideoMetadata?,
    onVideoSelected: () -> Unit,
    onLongClick: () -> Unit,
    onMenuClick: () -> Unit,
    isLinked: Boolean = false,
    onToggleLink: () -> Unit = {},
    onPractice: () -> Unit,
    onRename: () -> Unit,
    onEditMetadata: () -> Unit,
    onSearchSubtitles: () -> Unit,
    onImportSubtitles: () -> Unit,
    onDelete: () -> Unit,
    onMove: () -> Unit
) {
    val context = LocalContext.current
    var thumbnail by remember { mutableStateOf<Bitmap?>(null) }
    val isDirectory = file.isDirectory
    var showMenu by remember { mutableStateOf(false) }
    
    LaunchedEffect(file) {
        if (!isDirectory) {
            try {
                // Check if we have a saved thumbnail path
                if (metadata?.thumbnailPath != null) {
                    val thumbFile = File(metadata.thumbnailPath)
                    if (thumbFile.exists()) {
                        thumbnail = android.graphics.BitmapFactory.decodeFile(thumbFile.absolutePath)
                    }
                }
                
                // If not found, generate it
                if (thumbnail == null) {
                    withContext(Dispatchers.IO) {
                        val bitmap = FileUtils.getVideoThumbnail(context, file)
                        if (bitmap != null) {
                            thumbnail = bitmap
                            // Optionally save it
                        }
                    }
                }
            } catch (e: Exception) {
                LingoLog.e("GalleryItem", "Error loading thumbnail for ${file.name}", e)
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f)
            .combinedClickable(
                onClick = {
                    LingoLog.d("GalleryItem", "Video selected: ${file.name}")
                    onVideoSelected()
                },
                onLongClick = {
                    LingoLog.d("GalleryItem", "Long click on: ${file.name}")
                    onLongClick()
                }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (isDirectory) {
                    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFFD600).copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color(0xFFFFD600))
                    }
                } else {
                    if (thumbnail != null) {
                        Image(
                            bitmap = thumbnail!!.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Gray.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.White)
                        }
                    }
                    
                    // Link status indicator
                    if (isLinked) {
                        Icon(
                            Icons.Default.Link,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .size(20.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                .padding(2.dp)
                        )
                    }

                    // Subtitle status indicator
                    val srtFile = remember(file) { FileUtils.findBestSrtForVideo(file) }
                    if (srtFile?.exists() == true) {
                        val language = remember(srtFile) { com.example.lingoFlix.utils.SrtParser.detectSubtitleLanguage(srtFile) }
                        val flag = when(language) {
                            "אנגלית" -> "🇺🇸"
                            "עברית" -> "🇮🇱"
                            "ספרדית" -> "🇪🇸"
                            "צרפתית" -> "🇫🇷"
                            "גרמנית" -> "🇩🇪"
                            "איטלקית" -> "🇮🇹"
                            "רוסית" -> "🇷🇺"
                            "ערבית" -> "🇸🇦"
                            else -> "🏳️"
                        }
                        Surface(
                            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                            color = Color(0xFF58CC02),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                                Text(flag, fontSize = 14.sp)
                            }
                        }
                    }

                    // Season/Episode Badge
                    if (metadata?.season != null || metadata?.episode != null) {
                        Surface(
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            val text = buildString {
                                if (metadata.season != null) append("S${metadata.season}")
                                if (metadata.episode != null) append("E${metadata.episode}")
                            }
                            Text(text, color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 4.dp))
                        }
                    }
                }
            }
            
            Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = metadata?.title ?: file.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold
                    )
                    if (metadata?.description != null) {
                        Text(
                            text = metadata.description,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color.Gray
                        )
                    } else if (!isDirectory) {
                        var duration by remember(file) { mutableStateOf<String?>(null) }
                        LaunchedEffect(file) {
                            withContext(Dispatchers.IO) {
                                duration = FileUtils.getVideoDuration(context, file)
                            }
                        }
                        Text(duration ?: "", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.Gray)
                    }
                    VideoActionsMenu(
                        expanded = showMenu,
                        onDismiss = { showMenu = false },
                        isDirectory = isDirectory,
                        isLinked = isLinked,
                        onPractice = onPractice,
                        onToggleLink = onToggleLink,
                        onSearchSubtitles = onSearchSubtitles,
                        onImportSubtitles = onImportSubtitles,
                        onEditMetadata = onEditMetadata,
                        onRename = onRename,
                        onMove = onMove,
                        onDelete = onDelete
                    )
                }
            }
        }
    }
}

@Composable
fun VideoActionsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    isDirectory: Boolean,
    isLinked: Boolean,
    onPractice: () -> Unit,
    onToggleLink: () -> Unit,
    onSearchSubtitles: () -> Unit,
    onImportSubtitles: () -> Unit,
    onEditMetadata: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        if (!isDirectory) {
            DropdownMenuItem(
                text = { Text(if (isLinked) "הסר ממאגר רנדומלי" else "הוסף למאגר רנדומלי") },
                onClick = { onDismiss(); onToggleLink() },
                leadingIcon = { Icon(if (isLinked) Icons.Default.LinkOff else Icons.Default.Link, null) }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("חפש כתוביות") },
                onClick = { onDismiss(); onSearchSubtitles() },
                leadingIcon = { Icon(Icons.Default.Search, null) }
            )
            DropdownMenuItem(
                text = { Text("ייבא כתוביות") },
                onClick = { onDismiss(); onImportSubtitles() },
                leadingIcon = { Icon(Icons.Default.FileOpen, null) }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("ערוך פרטים") },
                onClick = { onDismiss(); onEditMetadata() },
                leadingIcon = { Icon(Icons.Default.EditNote, null) }
            )
        }
        DropdownMenuItem(
            text = { Text("שנה שם") },
            onClick = { onDismiss(); onRename() },
            leadingIcon = { Icon(Icons.Default.Edit, null) }
        )
        DropdownMenuItem(
            text = { Text("העבר אל...") },
            onClick = { onDismiss(); onMove() },
            leadingIcon = { Icon(Icons.Default.DriveFileMove, null) }
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(if (isDirectory) "מחק תיקייה" else "מחק") },
            onClick = { onDismiss(); onDelete() },
            leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) },
            colors = MenuDefaults.itemColors(textColor = Color.Red)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoItem(
    file: File,
    metadata: VideoMetadata?,
    context: android.content.Context,
    onVideoSelected: (Uri) -> Unit,
    onPractice: () -> Unit,
    isLinked: Boolean,
    onToggleLink: () -> Unit,
    onRename: () -> Unit,
    onEditMetadata: () -> Unit,
    onSearchSubtitles: () -> Unit,
    onImportSubtitles: () -> Unit,
    onDelete: () -> Unit,
    onMove: () -> Unit
) {
    val isDirectory = file.isDirectory
    var duration by remember(file) { mutableStateOf<String?>(null) }
    LaunchedEffect(file) {
        if (!isDirectory) {
            withContext(Dispatchers.IO) {
                duration = FileUtils.getVideoDuration(context, file)
            }
        }
    }
    val srtFile = remember(file) { if (isDirectory) null else FileUtils.findBestSrtForVideo(file) }
    val hasSubtitles = srtFile?.exists() ?: false
    var showMenu by remember { mutableStateOf(false) }

    var offsetX by remember { mutableFloatStateOf(0f) }
    val animatedOffset by animateDpAsState(targetValue = offsetX.dp, label = "swipe")

    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp))) {
        // Swipe Background
        Row(modifier = Modifier.matchParentSize().padding(horizontal = 2.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.fillMaxHeight().width(80.dp).background(Color.Red).clickable { offsetX = 0f; onDelete() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().offset(x = animatedOffset)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = { offsetX = if (offsetX < -60f) -80f else 0f },
                        onHorizontalDrag = { _, delta -> offsetX = (offsetX + delta).coerceIn(-80f, 0f) }
                    )
                }
                .combinedClickable(onClick = { onVideoSelected(Uri.fromFile(file)) }, onLongClick = onEditMetadata),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (isDirectory) Icons.Default.Folder else Icons.Default.PlayCircle, null, tint = if (isDirectory) Color(0xFFFFD600) else MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(metadata?.title ?: file.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (metadata?.description?.isNotBlank() == true) {
                        Text(
                            text = metadata.description,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (metadata?.season != null) Text("S${metadata.season}E${metadata.episode ?: 0} • ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        Text(if (isDirectory) "תיקייה" else (duration ?: ""), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        if (hasSubtitles) {
                            val language = remember(srtFile) { com.example.lingoFlix.utils.SrtParser.detectSubtitleLanguage(srtFile!!) }
                            val flag = when(language) {
                                "אנגלית" -> "🇺🇸"
                                "עברית" -> "🇮🇱"
                                "ספרדית" -> "🇪🇸"
                                "צרפתית" -> "🇫🇷"
                                "גרמנית" -> "🇩🇪"
                                "איטלקית" -> "🇮🇹"
                                "רוסית" -> "🇷🇺"
                                "ערבית" -> "🇸🇦"
                                else -> "🏳️"
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(color = Color(0xFF58CC02).copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                                    Text(flag, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
                Box {
                    IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, contentDescription = "תפריט") }
                    VideoActionsMenu(
                        expanded = showMenu,
                        onDismiss = { showMenu = false },
                        isDirectory = isDirectory,
                        isLinked = isLinked,
                        onPractice = onPractice,
                        onToggleLink = onToggleLink,
                        onSearchSubtitles = onSearchSubtitles,
                        onImportSubtitles = onImportSubtitles,
                        onEditMetadata = onEditMetadata,
                        onRename = onRename,
                        onMove = onMove,
                        onDelete = onDelete
                    )
                }
            }
        }
    }
}

