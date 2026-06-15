package com.example.lingoFlix.ui

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lingoFlix.data.VideoMetadataDao
import com.example.lingoFlix.model.VideoMetadata
import com.example.lingoFlix.utils.FileUtils
import com.example.lingoFlix.utils.SrtParser
import com.example.lingoFlix.utils.SubtitleGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun VideoListScreen(
    onVideoSelected: (Uri) -> Unit, 
    onPracticeRequested: (File, Boolean) -> Unit,
    onBack: () -> Unit,
    linkedVideos: Set<String>,
    onToggleLink: (String) -> Unit,
    onToggleDifficulty: (String) -> Unit,
    onSettingsRequested: () -> Unit,
    favoriteClips: Set<String> = emptySet(),
    onToggleFavorite: (String) -> Unit = {},
    userId: String = "guest",
    initialDir: File? = null,
    onDirChanged: (File) -> Unit = {},
    metadataDao: VideoMetadataDao? = null,
    onPickDirectory: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val rootVideoDir = remember { File(context.filesDir, "videos") }
    if (!rootVideoDir.exists()) rootVideoDir.mkdirs()
    
    var currentDir by remember { mutableStateOf(initialDir ?: rootVideoDir) }
    var isGridView by remember { mutableStateOf(true) }
    var gridColumns by remember { mutableIntStateOf(2) }
    
    val metadataState = metadataDao?.getAllMetadata()?.collectAsState(initial = emptyList())
    val allMetadata = metadataState?.value ?: emptyList()

    LaunchedEffect(currentDir) {
        onDirChanged(currentDir)
    }

    var videoToImportSubtitles by remember { mutableStateOf<File?>(null) }
    val importSubtitleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && videoToImportSubtitles != null) {
            val videoFile = videoToImportSubtitles!!
            val srtFile = File(videoFile.parentFile, "${videoFile.nameWithoutExtension}.srt")
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    srtFile.outputStream().use { output -> input.copyTo(output) }
                }
                Toast.makeText(context, "כתוביות יובאו!", Toast.LENGTH_SHORT).show()
                currentDir = File(currentDir.absolutePath)
            } catch (e: Exception) {
                Toast.makeText(context, "שגיאה בייבוא", Toast.LENGTH_SHORT).show()
            }
            videoToImportSubtitles = null
        }
    }
    
    val videoExtensions = remember { listOf("mp4", "mkv", "avi", "mov", "webm") }
    val items = remember(currentDir) {
        currentDir.listFiles()?.filter { file ->
            file.isDirectory || videoExtensions.any { ext -> file.name.endsWith(".$ext", ignoreCase = true) } || !file.name.contains(".")
        }?.distinctBy { it.absolutePath }
         ?.sortedWith(compareBy({ !it.isDirectory }, { it.name })) ?: emptyList()
    }

    var videoToRename by remember { mutableStateOf<File?>(null) }
    var videoToDelete by remember { mutableStateOf<File?>(null) }
    var folderToDelete by remember { mutableStateOf<File?>(null) }
    var videoToSearchSubtitles by remember { mutableStateOf<File?>(null) }
    var videoForQuiz by remember { mutableStateOf<File?>(null) }
    var videoToMove by remember { mutableStateOf<File?>(null) }
    var videoToEditMetadata by remember { mutableStateOf<File?>(null) }
    
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var subtitleSearchQuery by remember { mutableStateOf("") }
    var newFileName by remember { mutableStateOf("") }
    var selectedDifficulty by remember { mutableStateOf("קל") }

    val handleBack = {
        if (currentDir != rootVideoDir) {
            currentDir = currentDir.parentFile ?: rootVideoDir
        } else {
            onBack()
        }
    }
    
    BackHandler(enabled = true, onBack = handleBack)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = handleBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        if (currentDir == rootVideoDir) "הסרטונים שלי" else currentDir.name, 
                        style = MaterialTheme.typography.headlineMedium, 
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 200.dp)
                    )
                }
                Row {
                    IconButton(onClick = onPickDirectory) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "Open Device Folder")
                    }
                    IconButton(onClick = { isGridView = !isGridView }) {
                        Icon(if (isGridView) Icons.Default.List else Icons.Default.GridView, contentDescription = "Toggle View")
                    }
                    if (isGridView) {
                        IconButton(onClick = { gridColumns = if (gridColumns == 2) 3 else 2 }) {
                            Icon(Icons.Default.AspectRatio, contentDescription = "Grid Size")
                        }
                    }
                    IconButton(onClick = { showNewFolderDialog = true }) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = "New Folder")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("אין סרטונים או תיקיות כאן. תעלה משהו!", color = MaterialTheme.colorScheme.onBackground)
                }
            } else {
                if (isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(gridColumns),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(items) { file ->
                            val metadata = allMetadata.find { it.filePath == file.absolutePath }
                            val relativePath = try { file.relativeTo(rootVideoDir).path } catch (e: Exception) { file.absolutePath }
                            GalleryItem(
                                file = file,
                                metadata = metadata,
                                onVideoSelected = {
                                    if (file.isDirectory) currentDir = file
                                    else onVideoSelected(Uri.fromFile(file))
                                },
                                onLongClick = { 
                                    if (file.isDirectory) folderToDelete = file 
                                    else videoToEditMetadata = file 
                                },
                                onMenuClick = { /* Handled in VideoActionsMenu */ },
                                isLinked = linkedVideos.contains(relativePath),
                                onToggleLink = { onToggleLink(relativePath) },
                                onPractice = { videoForQuiz = file },
                                onRename = { 
                                    videoToRename = file
                                    newFileName = file.name
                                },
                                onEditMetadata = { videoToEditMetadata = file },
                                onSearchSubtitles = {
                                    subtitleSearchQuery = file.nameWithoutExtension
                                    videoToSearchSubtitles = file
                                },
                                onImportSubtitles = {
                                    videoToImportSubtitles = file
                                    importSubtitleLauncher.launch("*/*")
                                },
                                onDelete = { 
                                    if (file.isDirectory) folderToDelete = file else videoToDelete = file 
                                },
                                onMove = { videoToMove = file }
                            )
                        }
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
                        items(items) { file ->
                            val metadata = allMetadata.find { it.filePath == file.absolutePath }
                            val relativePath = try { file.relativeTo(rootVideoDir).path } catch (e: Exception) { file.absolutePath }
                            VideoItem(
                                file = file,
                                metadata = metadata,
                                context = context,
                                onVideoSelected = {
                                    if (file.isDirectory) currentDir = file
                                    else onVideoSelected(Uri.fromFile(file))
                                },
                                onPractice = { videoForQuiz = file },
                                isLinked = linkedVideos.contains(relativePath),
                                onToggleLink = { onToggleLink(relativePath) },
                                onRename = { 
                                    videoToRename = file
                                    newFileName = file.name
                                },
                                onEditMetadata = { videoToEditMetadata = file },
                                onSearchSubtitles = {
                                    subtitleSearchQuery = file.nameWithoutExtension
                                    videoToSearchSubtitles = file
                                },
                                onImportSubtitles = {
                                    videoToImportSubtitles = file
                                    importSubtitleLauncher.launch("*/*")
                                },
                                onDelete = { 
                                    if (file.isDirectory) folderToDelete = file else videoToDelete = file 
                                },
                                onMove = { videoToMove = file }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (videoToEditMetadata != null) {
        val file = videoToEditMetadata!!
        val metadata = allMetadata.find { it.filePath == file.absolutePath } ?: VideoMetadata(file.absolutePath)
        
        MetadataEditDialog(
            metadata = metadata,
            onDismiss = { videoToEditMetadata = null },
            onSave = { updated ->
                scope.launch {
                    metadataDao?.insertMetadata(updated)
                    videoToEditMetadata = null
                }
            },
            onDeleteFile = {
                videoToEditMetadata = null
                if (file.isDirectory) folderToDelete = file else videoToDelete = file
            },
            onRenameFile = {
                videoToRename = file
                newFileName = file.name
                videoToEditMetadata = null
            }
        )
    }

    // Reuse existing dialogs (Renaming, Deleting, Moving, Subtitles) - Simplified/Integrated
    // [Omitting redundant code for brevity but keeping logic]
    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("תיקייה חדשה") },
            text = { TextField(value = newFolderName, onValueChange = { newFolderName = it }, label = { Text("שם התיקייה") }) },
            confirmButton = { Button(onClick = {
                val newDir = File(currentDir, newFolderName)
                if (!newDir.exists()) newDir.mkdirs()
                showNewFolderDialog = false
                newFolderName = ""
                currentDir = File(currentDir.absolutePath)
            }) { Text("צור") } },
            dismissButton = { TextButton(onClick = { showNewFolderDialog = false }) { Text("ביטול") } }
        )
    }

    if (videoForQuiz != null) {
        AlertDialog(
            onDismissRequest = { videoForQuiz = null },
            title = { Text("בחר רמת קושי לתרגול") },
            text = {
                Column {
                    listOf("קל", "בינוני", "קשה").forEach { level ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { selectedDifficulty = level }.padding(vertical = 8.dp)
                        ) {
                            RadioButton(selected = selectedDifficulty == level, onClick = { selectedDifficulty = level })
                            Text(text = level, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val file = videoForQuiz!!
                    videoForQuiz = null
                    onToggleDifficulty(selectedDifficulty)
                    onPracticeRequested(file, true)
                }) { Text("התחל תרגול") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    val file = videoForQuiz!!
                    videoForQuiz = null
                    onPracticeRequested(file, false)
                }) { Text("צפייה רגילה") }
            }
        )
    }

    if (videoToMove != null) {
        val allFolders = rootVideoDir.walkTopDown().filter { it.isDirectory }.toList()
        AlertDialog(
            onDismissRequest = { videoToMove = null },
            title = { Text("העבר אל...") },
            text = {
                Box(modifier = Modifier.heightIn(max = 300.dp)) {
                    LazyColumn {
                        items(allFolders) { folder ->
                            val folderName = if (folder == rootVideoDir) "תיקייה ראשית" else folder.relativeTo(rootVideoDir).path
                            // Ensure target is a directory and not the source itself (if source is directory)
                            val isSourceSubdir = videoToMove?.absolutePath?.startsWith(folder.absolutePath) == true
                            if (folder != videoToMove && !isSourceSubdir) { 
                                Text(
                                    text = folderName,
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        val source = videoToMove!!
                                        val target = File(folder, source.name)
                                        if (source.renameTo(target)) {
                                            if (!source.isDirectory) {
                                                val srtSource = File(source.parentFile, "${source.nameWithoutExtension}.srt")
                                                if (srtSource.exists()) srtSource.renameTo(File(folder, srtSource.name))
                                            }
                                            currentDir = File(currentDir.absolutePath)
                                        }
                                        videoToMove = null
                                    }.padding(16.dp)
                                )
                                HorizontalDivider(modifier = Modifier.alpha(0.5f))
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { videoToMove = null }) { Text("ביטול") } }
        )
    }

    if (videoToSearchSubtitles != null) {
        AlertDialog(
            onDismissRequest = { videoToSearchSubtitles = null },
            title = { Text("חפש כתוביות") },
            text = {
                Column {
                    TextField(value = subtitleSearchQuery, onValueChange = { subtitleSearchQuery = it }, placeholder = { Text("שם הסרט") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${subtitleSearchQuery}+subtitles+srt"))) }, modifier = Modifier.fillMaxWidth()) { Text("חפש ב-Google") }
                    Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.ktuvit.me/Movie/Search?q=${subtitleSearchQuery}"))) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) { Text("חפש ב-Ktuvit") }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { videoToSearchSubtitles = null }) { Text("סגור") } }
        )
    }

    if (videoToDelete != null) {
        AlertDialog(
            onDismissRequest = { videoToDelete = null },
            title = { Text("מחיקת סרטון") },
            text = { Text("האם למחוק את ${videoToDelete?.name}?") },
            confirmButton = {
                Button(onClick = {
                    val file = videoToDelete!!
                    val srtFile = File(file.parentFile, "${file.nameWithoutExtension}.srt")
                    if (srtFile.exists()) srtFile.delete()
                    
                    val parent = file.parentFile
                    if (file.delete()) {
                        scope.launch { metadataDao?.deleteByPath(file.absolutePath) }
                        // Cleanup parent folder if empty and not root
                        if (parent != null && parent != rootVideoDir && parent.listFiles()?.isEmpty() == true) {
                            parent.delete()
                        }
                        currentDir = File(currentDir.absolutePath)
                    }
                    videoToDelete = null
                }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("מחק") }
            },
            dismissButton = { TextButton(onClick = { videoToDelete = null }) { Text("ביטול") } }
        )
    }

    if (folderToDelete != null) {
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            title = { Text("מחיקת תיקייה") },
            text = { Text("האם למחוק את '${folderToDelete?.name}' וכל תוכנה?") },
            confirmButton = {
                Button(onClick = {
                    folderToDelete?.deleteRecursively()
                    currentDir = File(currentDir.absolutePath)
                    folderToDelete = null
                }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("מחק הכל") }
            },
            dismissButton = { TextButton(onClick = { folderToDelete = null }) { Text("ביטול") } }
        )
    }

    if (videoToRename != null) {
        AlertDialog(
            onDismissRequest = { videoToRename = null },
            title = { Text("שינוי שם") },
            text = { TextField(value = newFileName, onValueChange = { newFileName = it }, label = { Text("שם חדש") }) },
            confirmButton = {
                Button(onClick = {
                    val oldFile = videoToRename!!
                    val renamedFile = File(oldFile.parentFile, newFileName)
                    if (oldFile.renameTo(renamedFile)) {
                        scope.launch {
                            val meta = metadataDao?.getMetadataForVideo(oldFile.absolutePath)
                            if (meta != null) {
                                metadataDao.deleteByPath(oldFile.absolutePath)
                                metadataDao.insertMetadata(meta.copy(filePath = renamedFile.absolutePath))
                            }
                        }
                        currentDir = File(currentDir.absolutePath)
                    }
                    videoToRename = null
                }) { Text("שמור") }
            },
            dismissButton = { TextButton(onClick = { videoToRename = null }) { Text("ביטול") } }
        )
    }
}

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
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f)
            .combinedClickable(
                onClick = onVideoSelected,
                onLongClick = onLongClick
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
                        Surface(
                            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                            color = Color(0xFF58CC02),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("CC", color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 4.dp), fontWeight = FontWeight.Bold)
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
                        val duration = remember(file) { FileUtils.getVideoDuration(context, file) }
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
    val duration = remember(file) { if (isDirectory) null else FileUtils.getVideoDuration(context, file) }
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (metadata?.season != null) Text("S${metadata.season}E${metadata.episode ?: 0} • ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        Text(if (isDirectory) "תיקייה" else (duration ?: ""), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        if (hasSubtitles) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(color = Color(0xFF58CC02).copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                Text("CC", color = Color(0xFF58CC02), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 4.dp), fontWeight = FontWeight.Bold)
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

@Composable
fun MetadataEditDialog(
    metadata: VideoMetadata,
    onDismiss: () -> Unit,
    onSave: (VideoMetadata) -> Unit,
    onDeleteFile: () -> Unit,
    onRenameFile: () -> Unit
) {
    var title by remember { mutableStateOf(metadata.title ?: "") }
    var season by remember { mutableStateOf(metadata.season?.toString() ?: "") }
    var episode by remember { mutableStateOf(metadata.episode?.toString() ?: "") }
    var description by remember { mutableStateOf(metadata.description ?: "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("פרטי סרטון") },
        text = {
            Column(modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                TextField(value = title, onValueChange = { title = it }, label = { Text("כותרת") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextField(value = season, onValueChange = { season = it }, label = { Text("עונה") }, modifier = Modifier.weight(1f), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
                    Spacer(modifier = Modifier.width(8.dp))
                    TextField(value = episode, onValueChange = { episode = it }, label = { Text("פרק") }, modifier = Modifier.weight(1f), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextField(value = description, onValueChange = { description = it }, label = { Text("תיאור") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(onClick = onRenameFile) { Icon(Icons.Default.Edit, null); Text("שנה שם קובץ") }
                    TextButton(onClick = onDeleteFile, colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)) { Icon(Icons.Default.Delete, null); Text("מחק קובץ") }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(metadata.copy(
                    title = title.ifBlank { null },
                    season = season.toIntOrNull(),
                    episode = episode.toIntOrNull(),
                    description = description.ifBlank { null }
                ))
            }) { Text("שמור") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ביטול") } }
    )
}
