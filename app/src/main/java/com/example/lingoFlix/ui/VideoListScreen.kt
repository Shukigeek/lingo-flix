package com.example.lingoFlix.ui

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lingoFlix.data.VideoMetadataDao
import com.example.lingoFlix.model.VideoMetadata
import com.example.lingoFlix.utils.FileUtils
import com.example.lingoFlix.utils.LingoLog
import com.example.lingoFlix.utils.SrtParser
import com.example.lingoFlix.utils.SubtitleGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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
    var columnCount by remember { mutableIntStateOf(1) }
    
    val allMetadataState = metadataDao?.getAllMetadata()?.collectAsState(initial = emptyList())
    val metadataMap = remember(allMetadataState?.value) {
        allMetadataState?.value?.associateBy { it.filePath } ?: emptyMap()
    }
    
    LaunchedEffect(currentDir) {
        onDirChanged(currentDir)
    }
    
    val items = remember(currentDir) {
        currentDir.listFiles()?.filter { it.isDirectory || it.extension != "srt" }
            ?.distinctBy { it.absolutePath }
            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name })) ?: emptyList()
    }

    var videoToRename by remember { mutableStateOf<File?>(null) }
    var videoToDelete by remember { mutableStateOf<File?>(null) }
    var folderToDelete by remember { mutableStateOf<File?>(null) }
    var videoToSearchSubtitles by remember { mutableStateOf<File?>(null) }
    var videoToImportSubtitles by remember { mutableStateOf<File?>(null) }
    var videoToEditMetadata by remember { mutableStateOf<File?>(null) }
    var videoForQuiz by remember { mutableStateOf<File?>(null) }
    var videoToMove by remember { mutableStateOf<File?>(null) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var subtitleSearchQuery by remember { mutableStateOf("") }
    var newFileName by remember { mutableStateOf("") }
    var selectedDifficulty by remember { mutableStateOf("קל") }

    var isGeneratingSubtitles by remember { mutableStateOf(false) }
    var generationProgress by remember { mutableStateOf("") }

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
                Toast.makeText(context, "כתוביות יובאו בהצלחה!", Toast.LENGTH_SHORT).show()
                currentDir = File(currentDir.absolutePath)
            } catch (e: Exception) {
                Toast.makeText(context, "שגיאה בייבוא הכתוביות", Toast.LENGTH_SHORT).show()
            }
            videoToImportSubtitles = null
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
                        modifier = Modifier.widthIn(max = 180.dp)
                    )
                }
                Row {
                    IconButton(onClick = { 
                        columnCount = if (columnCount == 1) 2 else if (columnCount == 2) 3 else 1 
                    }) {
                        Icon(
                            if (columnCount == 1) Icons.Default.ViewModule else if (columnCount == 2) Icons.Default.GridView else Icons.Default.ViewStream,
                            contentDescription = "שנה תצוגה"
                        )
                    }
                    IconButton(onClick = { showNewFolderDialog = true }) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = "New Folder")
                    }
                    IconButton(onClick = onSettingsRequested) {
                        Icon(Icons.Default.AutoStories, contentDescription = "Settings")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("אין סרטונים או תיקיות כאן. תעלה משהו!", color = MaterialTheme.colorScheme.onBackground)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columnCount),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items) { file ->
                        val metadata = metadataMap[file.absolutePath]
                        val relativePath = try { file.relativeTo(rootVideoDir).path } catch(e: Exception) { file.absolutePath }
                        
                        if (columnCount > 1) {
                            GalleryItem(
                                file = file,
                                metadata = metadata,
                                onVideoSelected = {
                                    if (file.isDirectory) currentDir = file
                                    else onVideoSelected(Uri.fromFile(file))
                                },
                                onLongClick = { videoToEditMetadata = file },
                                onMenuClick = { },
                                isLinked = linkedVideos.contains(relativePath),
                                onToggleLink = { onToggleLink(relativePath) },
                                onPractice = { videoForQuiz = file },
                                onRename = { videoToRename = file; newFileName = file.name },
                                onEditMetadata = { videoToEditMetadata = file },
                                onSearchSubtitles = { subtitleSearchQuery = file.nameWithoutExtension; videoToSearchSubtitles = file },
                                onImportSubtitles = { videoToImportSubtitles = file; importSubtitleLauncher.launch("*/*") },
                                onDelete = { if (file.isDirectory) folderToDelete = file else videoToDelete = file },
                                onMove = { videoToMove = file }
                            )
                        } else {
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
                                onRename = { videoToRename = file; newFileName = file.name },
                                onEditMetadata = { videoToEditMetadata = file },
                                onSearchSubtitles = { subtitleSearchQuery = file.nameWithoutExtension; videoToSearchSubtitles = file },
                                onImportSubtitles = { videoToImportSubtitles = file; importSubtitleLauncher.launch("*/*") },
                                onDelete = { if (file.isDirectory) folderToDelete = file else videoToDelete = file },
                                onMove = { videoToMove = file }
                            )
                        }
                    }
                }
            }
        }

        if (isGeneratingSubtitles) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black.copy(alpha = 0.7f)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("מייצר כתוביות בעזרת AI...", color = Color.White, style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(generationProgress, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    // Dialogs
    if (videoToEditMetadata != null) {
        val file = videoToEditMetadata!!
        val metadata = metadataMap[file.absolutePath] ?: VideoMetadata(file.absolutePath)
        EditMetadataDialog(
            initialMetadata = metadata,
            onSave = { updated ->
                scope.launch {
                    metadataDao?.insertMetadata(updated)
                    videoToEditMetadata = null
                }
            },
            onDismiss = { videoToEditMetadata = null }
        )
    }

    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("תיקייה חדשה") },
            text = {
                TextField(value = newFolderName, onValueChange = { newFolderName = it }, label = { Text("שם התיקייה") })
            },
            confirmButton = {
                Button(onClick = {
                    val newDir = File(currentDir, newFolderName)
                    if (!newDir.exists()) newDir.mkdirs()
                    showNewFolderDialog = false
                    newFolderName = ""
                    currentDir = File(currentDir.absolutePath)
                }) { Text("צור") }
            },
            dismissButton = { TextButton(onClick = { showNewFolderDialog = false }) { Text("ביטול") } }
        )
    }

    if (videoForQuiz != null) {
        AlertDialog(
            onDismissRequest = { videoForQuiz = null },
            title = { Text("בחר רמת קושי") },
            text = {
                Column {
                    listOf("קל", "בינוני", "קשה").forEach { level ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { selectedDifficulty = level }.padding(vertical = 8.dp)) {
                            RadioButton(selected = selectedDifficulty == level, onClick = { selectedDifficulty = level })
                            Text(level, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val f = videoForQuiz!!; videoForQuiz = null
                    onToggleDifficulty(selectedDifficulty)
                    onPracticeRequested(f, true)
                }) { Text("התחל") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    val f = videoForQuiz!!; videoForQuiz = null
                    onPracticeRequested(f, false)
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
                            if (folder != videoToMove && !videoToMove!!.absolutePath.startsWith(folder.absolutePath)) { 
                                Text(
                                    text = folderName,
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        val source = videoToMove!!
                                        val target = File(folder, source.name)
                                        if (source.renameTo(target)) {
                                            scope.launch {
                                                val meta = metadataDao?.getMetadataForVideo(source.absolutePath)
                                                if (meta != null) {
                                                    metadataDao.deleteByPath(source.absolutePath)
                                                    metadataDao.insertMetadata(meta.copy(filePath = target.absolutePath))
                                                }
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
                    TextField(value = subtitleSearchQuery, onValueChange = { subtitleSearchQuery = it }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${subtitleSearchQuery}+subtitles+srt"))) }, modifier = Modifier.fillMaxWidth()) { Text("Google") }
                    Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.ktuvit.me/Movie/Search?q=${subtitleSearchQuery}"))) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) { Text("Ktuvit") }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { videoToSearchSubtitles = null }) { Text("סגור") } }
        )
    }

    if (videoToDelete != null) {
        AlertDialog(
            onDismissRequest = { videoToDelete = null },
            title = { Text("מחיקה") },
            text = { Text("למחוק את ${videoToDelete?.name}?") },
            confirmButton = {
                Button(onClick = {
                    val f = videoToDelete!!
                    val srt = File(f.parentFile, "${f.nameWithoutExtension}.srt")
                    if (srt.exists()) srt.delete()
                    if (f.delete()) {
                        scope.launch { metadataDao?.deleteByPath(f.absolutePath) }
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
            text = { Text("למחוק את '${folderToDelete?.name}' וכל תוכנה?") },
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
                    val old = videoToRename!!
                    val renamed = File(old.parentFile, newFileName)
                    if (old.renameTo(renamed)) {
                        scope.launch {
                            val meta = metadataDao?.getMetadataForVideo(old.absolutePath)
                            if (meta != null) {
                                metadataDao.deleteByPath(old.absolutePath)
                                metadataDao.insertMetadata(meta.copy(filePath = renamed.absolutePath))
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
fun EditMetadataDialog(
    initialMetadata: VideoMetadata,
    onSave: (VideoMetadata) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(initialMetadata.title ?: "") }
    var description by remember { mutableStateOf(initialMetadata.description ?: "") }
    var season by remember { mutableStateOf(initialMetadata.season?.toString() ?: "") }
    var episode by remember { mutableStateOf(initialMetadata.episode?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ערוך פרטי סרטון") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(value = title, onValueChange = { title = it }, label = { Text("כותרת") }, modifier = Modifier.fillMaxWidth())
                TextField(value = description, onValueChange = { description = it }, label = { Text("תיאור") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(value = season, onValueChange = { season = it }, label = { Text("עונה") }, modifier = Modifier.weight(1f))
                    TextField(value = episode, onValueChange = { episode = it }, label = { Text("פרק") }, modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(initialMetadata.copy(
                    title = title,
                    description = description,
                    season = season.toIntOrNull(),
                    episode = episode.toIntOrNull()
                ))
            }) { Text("שמור") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ביטול") } }
    )
}
