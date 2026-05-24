// model/VideoProject.kt
package com.example.lingoFlix.model

data class VideoProject(
    val name: String,        // שם הסרטון בלי סיומת
    val videoPath: String,   // נתיב מלא לקובץ הוידאו
    val srtPath: String      // נתיב מלא לקובץ ה-SRT
)