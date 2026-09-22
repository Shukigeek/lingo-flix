package com.example.lingoFlix.utils

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class FileUtilsTest {

    @Test
    fun `findBestSrtForVideo matches exact file name`() {
        val parent = File.createTempFile("test_dir", "")
        parent.delete()
        parent.mkdirs()
        
        val video = File(parent, "TheMatrix.mp4")
        video.createNewFile()
        
        val srt = File(parent, "TheMatrix.srt")
        srt.createNewFile()
        
        val found = FileUtils.findBestSrtForVideo(video)
        assertNotNull(found)
        assertEquals(srt.absolutePath, found?.absolutePath)
        
        parent.deleteRecursively()
    }

    @Test
    fun `findBestSrtForVideo matches partial file name`() {
        val parent = File.createTempFile("test_dir", "")
        parent.delete()
        parent.mkdirs()
        
        val video = File(parent, "Friends S01E01.mp4")
        video.createNewFile()
        
        val srt = File(parent, "friends.srt")
        srt.createNewFile()
        
        val found = FileUtils.findBestSrtForVideo(video)
        assertNotNull(found)
        assertEquals(srt.absolutePath, found?.absolutePath)
        
        parent.deleteRecursively()
    }

    @Test
    fun `findBestSrtForVideo returns null if no srt found`() {
        val parent = File.createTempFile("test_dir", "")
        parent.delete()
        parent.mkdirs()
        
        val video = File(parent, "Movie.mp4")
        video.createNewFile()
        
        val found = FileUtils.findBestSrtForVideo(video)
        assertNull(found)
        
        parent.deleteRecursively()
    }
}
