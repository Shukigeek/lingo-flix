package com.example.lingoFlix.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import com.example.lingoFlix.utils.FileUtils
import com.example.lingoFlix.utils.SrtParser
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
    onToggleFavorite: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val rootVideoDir = remember { File(context.filesDir, "videos") }
    if (!rootVideoDir.exists()) rootVideoDir.mkdirs()
    
    var currentDir by remember { mutableStateOf(rootVideoDir) }
    
    val items = remember(currentDir) {
        currentDir.listFiles()?.filter { it.isDirectory || it.extension != "srt" }
            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name })) ?: emptyList()
    }

    var videoToRename by remember { mutableStateOf<File?>(null) }
    var videoToDelete by remember { mutableStateOf<File?>(null) }
    var videoToSearchSubtitles by remember { mutableStateOf<File?>(null) }
    var videoToImportSubtitles by remember { mutableStateOf<File?>(null) }
    var videoForQuiz by remember { mutableStateOf<File?>(null) }
    var videoToMove by remember { mutableStateOf<File?>(null) }
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

    if (videoForQuiz != null) {
        AlertDialog(
            onDismissRequest = { videoForQuiz = null },
            title = { Text("בחר רמת קושי לתרגול") },
            text = {
                Column {
                    listOf("קל", "בינוני", "קשה").forEach { level ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedDifficulty = level }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(selected = selectedDifficulty == level, onClick = { selectedDifficulty = level })
                            Text(text = level, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    Text(
                        text = when(selectedDifficulty) {
                            "קל" -> "מילה אחת חסרה בכל משפט"
                            "בינוני" -> "כ-40% מהמילים יוסתרו"
                            else -> "רוב המילים יוסתרו"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val file = videoForQuiz!!
                    videoForQuiz = null
                    onToggleDifficulty(selectedDifficulty)
                    onPracticeRequested(file, true)
                }) {
                    Text("התחל תרגול")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    val file = videoForQuiz!!
                    videoForQuiz = null
                    onPracticeRequested(file, false)
                }) {
                    Text("צפייה רגילה (ללא Quiz)")
                }
            }
        )
    }

    val importSubtitleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && videoToImportSubtitles != null) {
            val videoFile = videoToImportSubtitles!!
            val srtFile = File(videoFile.parentFile, "${videoFile.nameWithoutExtension}.srt")
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    srtFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                Toast.makeText(context, "כתוביות יובאו בהצלחה!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "שגיאה בייבוא הכתוביות", Toast.LENGTH_SHORT).show()
            }
            videoToImportSubtitles = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            repeat(10) {
                Row(modifier = Modifier.alpha(0.04f)) {
                    repeat(5) {
                        Icon(Icons.Default.Gesture, null, modifier = Modifier.size(100.dp).padding(10.dp), tint = MaterialTheme.colorScheme.primary)
                        Icon(Icons.Default.AutoFixNormal, null, modifier = Modifier.size(80.dp).padding(10.dp), tint = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = handleBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text(if (currentDir == rootVideoDir) "הסרטונים שלי" else currentDir.name, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                }
                Row {
                    IconButton(onClick = { showNewFolderDialog = true }) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = "New Folder")
                    }
                    IconButton(onClick = onSettingsRequested) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("אין סרטונים או תיקיות כאן. תעלה משהו!", color = MaterialTheme.colorScheme.onBackground)
                }
            } else {
                LazyColumn {
                    items(items) { file ->
                        VideoItem(
                            file = file,
                            context = context,
                            onVideoSelected = {
                                if (file.isDirectory) {
                                    currentDir = file
                                } else {
                                    onVideoSelected(Uri.fromFile(file))
                                }
                            },
                            onPractice = { videoForQuiz = file },
                            isLinked = linkedVideos.contains(file.name),
                            onToggleLink = { onToggleLink(file.name) },
                            onRename = { 
                                videoToRename = file
                                newFileName = file.name
                            },
                            onSearchSubtitles = {
                                subtitleSearchQuery = file.nameWithoutExtension
                                videoToSearchSubtitles = file
                            },
                            onImportSubtitles = {
                                videoToImportSubtitles = file
                                importSubtitleLauncher.launch("*/*")
                            },
                            onDelete = { videoToDelete = file },
                            onMove = { videoToMove = file }
                        )
                    }
                }
            }
        }
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
                            if (folder != videoToMove) { 
                                Text(
                                    text = folderName,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val source = videoToMove!!
                                            val target = File(folder, source.name)
                                            if (source.renameTo(target)) {
                                                if (!source.isDirectory) {
                                                    val srtSource = File(source.parentFile, "${source.nameWithoutExtension}.srt")
                                                    if (srtSource.exists()) {
                                                        srtSource.renameTo(File(folder, srtSource.name))
                                                    }
                                                }
                                                currentDir = File(currentDir.absolutePath)
                                            }
                                            videoToMove = null
                                        }
                                        .padding(16.dp),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                HorizontalDivider(modifier = Modifier.alpha(0.5f))
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { videoToMove = null }) { Text("ביטול") }
            }
        )
    }

    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("תיקייה חדשה") },
            text = {
                TextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("שם התיקייה") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    val newDir = File(currentDir, newFolderName)
                    if (!newDir.exists()) newDir.mkdirs()
                    showNewFolderDialog = false
                    newFolderName = ""
                    currentDir = File(currentDir.absolutePath)
                }) {
                    Text("צור")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) {
                    Text("ביטול")
                }
            }
        )
    }

    if (videoToSearchSubtitles != null) {
        AlertDialog(
            onDismissRequest = { videoToSearchSubtitles = null },
            title = { Text("חפש כתוביות להורדה") },
            text = {
                Column {
                    Text("הכנס שם סרט לחיפוש באתרים:")
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = subtitleSearchQuery,
                        onValueChange = { subtitleSearchQuery = it },
                        placeholder = { Text("למשל: The Matrix") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${subtitleSearchQuery}+subtitles+srt"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("חפש ב-Google (כללי)")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.ktuvit.me/Movie/Search?q=${subtitleSearchQuery}"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Text("חפש ב-Ktuvit (עברית)")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { videoToSearchSubtitles = null }) {
                    Text("סגור")
                }
            }
        )
    }

    if (videoToDelete != null) {
        AlertDialog(
            onDismissRequest = { videoToDelete = null },
            title = { Text("מחיקת סרטון") },
            text = { Text("מה ברצונך לעשות עם הסרטון ${videoToDelete?.name}?") },
            confirmButton = {
                Button(
                    onClick = {
                        if (videoToDelete?.delete() == true) {
                            currentDir = File(currentDir.absolutePath)
                        }
                        videoToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("מחק לצמיתות")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        videoToDelete?.let { FileUtils.exportVideoToGallery(context, it) }
                        if (videoToDelete?.delete() == true) {
                            currentDir = File(currentDir.absolutePath)
                        }
                        videoToDelete = null
                        Toast.makeText(context, "הסרטון הועבר לגלריה", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("העבר לגלריה")
                    }
                    TextButton(onClick = { videoToDelete = null }) {
                        Text("ביטול")
                    }
                }
            }
        )
    }

    if (videoToRename != null) {
        AlertDialog(
            onDismissRequest = { videoToRename = null },
            title = { Text("ערוך שם קובץ") },
            text = {
                TextField(
                    value = newFileName,
                    onValueChange = { newFileName = it },
                    label = { Text("שם חדש") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    val renamedFile = File(videoToRename!!.parentFile, newFileName)
                    if (videoToRename!!.renameTo(renamedFile)) {
                        currentDir = File(currentDir.absolutePath)
                    }
                    videoToRename = null
                }) {
                    Text("שמור")
                }
            },
            dismissButton = {
                TextButton(onClick = { videoToRename = null }) {
                    Text("ביטול")
                }
            }
        )
    }
}

@Composable
fun VideoItem(
    file: File,
    context: android.content.Context,
    onVideoSelected: (Uri) -> Unit,
    onPractice: () -> Unit,
    isLinked: Boolean,
    onToggleLink: () -> Unit,
    onRename: () -> Unit,
    onSearchSubtitles: () -> Unit,
    onImportSubtitles: () -> Unit,
    onDelete: () -> Unit,
    onMove: () -> Unit
) {
    val isDirectory = file.isDirectory
    val duration = remember(file) { if (isDirectory) null else FileUtils.getVideoDuration(context, file) }
    val srtFile = remember(file) { if (isDirectory) null else File(file.parentFile, "${file.nameWithoutExtension}.srt") }
    val hasSubtitles = srtFile?.exists() ?: false
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { 
                onVideoSelected(Uri.fromFile(file))
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isDirectory) Icons.Default.Folder else Icons.Default.PlayCircle, 
                null, 
                tint = if (isDirectory) Color(0xFFFBC02D) else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        file.name, 
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isLinked && !isDirectory) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Link,
                            contentDescription = "Linked",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isDirectory) "תיקייה" else (duration ?: "אורך לא ידוע"),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    if (hasSubtitles && srtFile != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        val subLang = remember(srtFile) { SrtParser.detectSubtitleLanguage(srtFile) }
                        Surface(
                            color = when (subLang) {
                                "עברית" -> Color(0xFFE8F5E9)
                                "ספרדית" -> Color(0xFFFFF3E0)
                                "רוסית" -> Color(0xFFF3E5F5)
                                "ערבית" -> Color(0xFFFFFDE7)
                                else -> Color(0xFFE3F2FD)
                            },
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "${SrtParser.getLanguageFlag(subLang)} $subLang",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = when (subLang) {
                                    "עברית" -> Color(0xFF2E7D32)
                                    "ספרדית" -> Color(0xFFE65100)
                                    "רוסית" -> Color(0xFF7B1FA2)
                                    "ערבית" -> Color(0xFFFBC02D)
                                    else -> Color(0xFF1976D2)
                                }
                            )
                        }
                    }
                }
            }
            
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.Settings, contentDescription = "Actions")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (hasSubtitles && !isDirectory) {
                        DropdownMenuItem(
                            text = { Text("למד ממשפטים (לפי סדר)", color = MaterialTheme.colorScheme.primary) },
                            leadingIcon = { Icon(Icons.Default.School, null, tint = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                showMenu = false
                                onPractice()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isLinked) "הסר מהמאגר הרנדומלי" else "הוסף למאגר הרנדומלי") },
                            leadingIcon = { Icon(if (isLinked) Icons.Default.LinkOff else Icons.Default.Link, null) },
                            onClick = {
                                showMenu = false
                                onToggleLink()
                            }
                        )
                        HorizontalDivider()
                    }
                    DropdownMenuItem(
                        text = { Text("ערוך שם") },
                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                        onClick = {
                            showMenu = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("העבר לתיקייה") },
                        leadingIcon = { Icon(Icons.Default.DriveFileMove, null) },
                        onClick = {
                            showMenu = false
                            onMove()
                        }
                    )
                    if (!isDirectory) {
                        DropdownMenuItem(
                            text = { Text("חפש כתוביות") },
                            leadingIcon = { Icon(Icons.Default.Subtitles, null) },
                            onClick = {
                                showMenu = false
                                onSearchSubtitles()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("ייבא כתוביות") },
                            leadingIcon = { Icon(Icons.Default.FileOpen, null) },
                            onClick = {
                                showMenu = false
                                onImportSubtitles()
                            }
                        )
                        HorizontalDivider()
                    }
                    DropdownMenuItem(
                        text = { Text("מחק", color = Color.Red) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
