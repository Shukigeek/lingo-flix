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

import com.example.lingoFlix.utils.LingoLog

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
    
    LingoLog.d("VideoListScreen", "Initializing VideoListScreen")
    
    val rootVideoDir = remember { File(context.filesDir, "videos") }
    if (!rootVideoDir.exists()) rootVideoDir.mkdirs()
    
    var currentDir by remember { mutableStateOf(initialDir ?: rootVideoDir) }
    var isGridView by remember { mutableStateOf(true) }
    var gridColumns by remember { mutableIntStateOf(2) }
    
    val metadataState = metadataDao?.getAllMetadata()?.collectAsState(initial = emptyList())
    val allMetadata = metadataState?.value ?: emptyList()
    val metadataMap = remember(allMetadata) { allMetadata.associateBy { it.filePath } }

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

    val importVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            LingoLog.d("VideoListScreen", "Video picked with OpenDocument: $uri")
            context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            scope.launch(Dispatchers.IO) {
                val name = FileUtils.getFileName(context, uri) ?: "video_${System.currentTimeMillis()}.mp4"
                LingoLog.d("VideoListScreen", "Resolved filename: $name")
                val savedFile = FileUtils.saveVideoToInternalStorage(context, uri, name, currentDir)
                withContext(Dispatchers.Main) {
                    if (savedFile != null) {
                        Toast.makeText(context, "סרטון $name נוסף בהצלחה!", Toast.LENGTH_SHORT).show()
                        currentDir = File(currentDir.absolutePath) // Refresh
                    } else {
                        Toast.makeText(context, "שגיאה בהוספת הסרטון", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val handleBack = {
        if (currentDir != rootVideoDir) {
            currentDir = currentDir.parentFile ?: rootVideoDir
        } else {
            onBack()
        }
    }
    
    BackHandler(enabled = true, onBack = handleBack)

    Scaffold { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
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
                    IconButton(onClick = { importVideoLauncher.launch(arrayOf("video/*")) }) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Add New Video", tint = MaterialTheme.colorScheme.primary)
                    }
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
                            val metadata = metadataMap[file.absolutePath]
                            val relativePath = try { file.relativeTo(rootVideoDir).path } catch (e: Exception) { file.absolutePath }
                            GalleryItem(
                                file = file,
                                metadata = metadata,
                                onVideoSelected = {
                                    if (file.isDirectory) currentDir = file
                                    else videoForQuiz = file
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
                            val metadata = metadataMap[file.absolutePath]
                            val relativePath = try { file.relativeTo(rootVideoDir).path } catch (e: Exception) { file.absolutePath }
                            VideoItem(
                                file = file,
                                metadata = metadata,
                                context = context,
                                onVideoSelected = {
                                    if (file.isDirectory) currentDir = file
                                    else videoForQuiz = file
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
    }

    // Dialogs
    if (videoToEditMetadata != null) {
        val file = videoToEditMetadata!!
        val metadata = metadataMap[file.absolutePath] ?: VideoMetadata(file.absolutePath)
        
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
        NewFolderDialog(
            onConfirm = { name ->
                val newDir = File(currentDir, name)
                if (!newDir.exists()) newDir.mkdirs()
                showNewFolderDialog = false
                currentDir = File(currentDir.absolutePath)
            },
            onDismiss = { showNewFolderDialog = false }
        )
    }

    if (videoForQuiz != null) {
        DifficultySelectionDialog(
            onDifficultySelected = { level ->
                val file = videoForQuiz!!
                videoForQuiz = null
                onToggleDifficulty(level)
                onPracticeRequested(file, true)
            },
            onRegularView = {
                val file = videoForQuiz!!
                videoForQuiz = null
                onPracticeRequested(file, false)
            },
            onDismiss = { videoForQuiz = null }
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
