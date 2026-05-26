package com.example.lingoFlix.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lingoFlix.model.UserProfile
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import java.io.InputStream

val avatarColors = listOf(
    Color(0xFFE50914), // Netflix Red
    Color(0xFF54B435), // Green
    Color(0xFF3772FF), // Blue
    Color(0xFFF79F1F), // Orange
    Color(0xFF8B5CF6)  // Purple
)

@Composable
fun ProfileSelectionScreen(
    profiles: List<UserProfile>,
    onProfileSelected: (UserProfile) -> Unit,
    onAddProfile: (String, Int, String?) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf("") }
    var selectedAvatarIndex by remember { mutableIntStateOf(0) }
    var selectedAvatarUri by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedAvatarUri = uri.toString()
            selectedAvatarIndex = -1 // Indicates custom URI
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF141414)) // Netflix black
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "?מי לומד היום",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp),
            modifier = Modifier.widthIn(max = 400.dp)
        ) {
            items(profiles) { profile ->
                ProfileItem(profile = profile, onClick = { onProfileSelected(profile) })
            }
            
            item {
                AddProfileItem(onClick = { showAddDialog = true })
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("הוסף פרופיל חדש") },
            text = {
                Column {
                    TextField(
                        value = newProfileName,
                        onValueChange = { newProfileName = it },
                        label = { Text("שם הפרופיל") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("בחר אוואטר:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        avatarColors.forEachIndexed { index, color ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (selectedAvatarIndex == index) 3.dp else 0.dp,
                                        color = if (selectedAvatarIndex == index) Color.Black else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { 
                                        selectedAvatarIndex = index
                                        selectedAvatarUri = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedAvatarIndex == index) {
                                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                        
                        // Custom Gallery Avatar option
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Gray)
                                .border(
                                    width = if (selectedAvatarIndex == -1) 3.dp else 0.dp,
                                    color = if (selectedAvatarIndex == -1) Color.Black else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { galleryLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedAvatarUri != null) {
                                UriImage(uriString = selectedAvatarUri!!, modifier = Modifier.fillMaxSize())
                            } else {
                                Icon(Icons.Default.Image, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            if (selectedAvatarIndex == -1) {
                                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newProfileName.isNotBlank()) {
                        onAddProfile(newProfileName, selectedAvatarIndex, selectedAvatarUri)
                        showAddDialog = false
                        newProfileName = ""
                        selectedAvatarIndex = 0
                        selectedAvatarUri = null
                    }
                }) {
                    Text("צור")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("ביטול")
                }
            }
        )
    }
}

@Composable
fun ProfileItem(profile: UserProfile, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (profile.avatarUri == null) avatarColors.getOrElse(profile.avatarRes) { Color.Gray } else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            if (profile.avatarUri != null) {
                UriImage(uriString = profile.avatarUri, modifier = Modifier.fillMaxSize())
            } else {
                Text(
                    text = profile.name.take(1).uppercase(),
                    color = Color.White,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = profile.name,
            color = Color.LightGray,
            fontSize = 16.sp
        )
    }
}

@Composable
fun AddProfileItem(onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "הוסף פרופיל",
            color = Color.LightGray,
            fontSize = 16.sp
        )
    }
}

@Composable
fun UriImage(uriString: String, modifier: Modifier) {
    val context = LocalContext.current
    val bitmap = remember(uriString) {
        try {
            val uri = Uri.parse(uriString)
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            null
        }
    }
    
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Box(modifier = modifier.background(Color.Gray))
    }
}
