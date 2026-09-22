package com.example.lingoFlix.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SubtitleStyleDialog(
    fontSize: Float,
    onFontSizeChange: (Float) -> Unit,
    fontFamily: String,
    onFontFamilyChange: (String) -> Unit,
    isBold: Boolean,
    onBoldChange: (Boolean) -> Unit,
    colorHex: String,
    onColorChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("עיצוב כתוביות") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("גודל טקסט: ${fontSize.toInt()}")
                Slider(value = fontSize, onValueChange = onFontSizeChange, valueRange = 16f..60f)
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("סוג גופן:")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf("SansSerif", "Serif", "Monospace", "Cursive").forEach { font ->
                        FilterChip(
                            selected = fontFamily == font,
                            onClick = { onFontFamilyChange(font) },
                            label = { Text(font, fontSize = 10.sp) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isBold, onCheckedChange = onBoldChange)
                    Text("טקסט מודגש (Bold)")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("צבע טקסט:")
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf("#FFFFFF", "#FFFF00", "#00FF00", "#FF0000", "#00FFFF", "#FF00FF", "#FFA500", "#A9A9A9").forEach { color ->
                        Box(
                            modifier = Modifier
                                .padding(4.dp).size(40.dp)
                                .background(Color(android.graphics.Color.parseColor(color)), CircleShape)
                                .border(if (colorHex == color) 3.dp else 1.dp, if (colorHex == color) MaterialTheme.colorScheme.primary else Color.LightGray, CircleShape)
                                .clickable { onColorChange(color) }
                        )
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onSave) { Text("שמור") } }
    )
}

@Composable
fun ResumeDialog(
    savedIndex: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("המשך תרגול") },
        text = { Text("נראה שהיית באמצע התרגול. האם להמשיך ממשפט ${savedIndex + 1}?") },
        confirmButton = { Button(onClick = onConfirm) { Text("המשך") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("התחל מהתחלה") } }
    )
}
