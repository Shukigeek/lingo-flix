package com.example.lingoFlix.utils

import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.LongBuffer
import java.nio.channels.FileChannel

/**
 * Hash calculation for OpenSubtitles API.
 * Based on the official algorithm: 64k at start + 64k at end + file size.
 */
object OpenSubtitlesHasher {
    private const val HASH_CHUNK_SIZE = 64 * 1024

    fun computeHash(file: File): String {
        val size = file.length()
        var checksum = size
        
        val fis = FileInputStream(file)
        val channel = fis.channel
        
        try {
            // First 64k
            checksum += computeChecksum(channel, 0, HASH_CHUNK_SIZE)
            // Last 64k
            checksum += computeChecksum(channel, (size - HASH_CHUNK_SIZE).coerceAtLeast(0), HASH_CHUNK_SIZE)
        } finally {
            channel.close()
            fis.close()
        }
        
        return String.format("%016x", checksum)
    }

    private fun computeChecksum(channel: FileChannel, start: Long, size: Int): Long {
        val buffer = ByteBuffer.allocateDirect(size)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        channel.read(buffer, start)
        buffer.flip()
        
        val longBuffer: LongBuffer = buffer.asLongBuffer()
        var sum: Long = 0
        while (longBuffer.hasRemaining()) {
            sum += longBuffer.get()
        }
        return sum
    }
}
