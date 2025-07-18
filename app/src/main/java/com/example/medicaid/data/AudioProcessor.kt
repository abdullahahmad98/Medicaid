package com.example.medicaid.data

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaMetadataRetriever
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min

class AudioProcessor(private val context: Context) {

    companion object {
        private const val TAG = "AudioProcessor"
        private const val WHISPER_SAMPLE_RATE = 16000
        private const val WHISPER_CHANNELS = 1
        private const val BYTES_PER_SAMPLE = 2
    }

    /**
     * Convert audio file to the format required by Whisper AI (16kHz, mono, float32)
     */
    suspend fun convertAudioForWhisper(audioFilePath: String): FloatArray? = withContext(Dispatchers.IO) {
        try {
            val audioFile = File(audioFilePath)
            if (!audioFile.exists()) {
                Log.e(TAG, "Audio file does not exist: $audioFilePath")
                return@withContext null
            }

            // For now, we'll assume the audio is already in the correct format
            // In a production app, you'd want to use FFmpeg or similar to convert
            val audioData = readWavFile(audioFile)

            if (audioData != null) {
                Log.i(TAG, "Audio converted successfully: ${audioData.size} samples")
                audioData
            } else {
                Log.e(TAG, "Failed to convert audio file")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error converting audio", e)
            null
        }
    }

    /**
     * Read WAV file and convert to float array
     * This is a simplified implementation - in production you'd want more robust audio processing
     */
    private fun readWavFile(file: File): FloatArray? {
        try {
            val fileInputStream = FileInputStream(file)
            val fileSize = file.length().toInt()
            val buffer = ByteArray(fileSize)
            fileInputStream.read(buffer)
            fileInputStream.close()

            // Skip WAV header (44 bytes)
            val headerSize = 44
            if (buffer.size <= headerSize) {
                Log.e(TAG, "File too small to be a valid WAV file")
                return null
            }

            val audioDataSize = buffer.size - headerSize
            val sampleCount = audioDataSize / BYTES_PER_SAMPLE
            val audioData = FloatArray(sampleCount)

            val byteBuffer = ByteBuffer.wrap(buffer, headerSize, audioDataSize)
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN)

            // Convert 16-bit PCM to float32 normalized to [-1, 1]
            for (i in 0 until sampleCount) {
                val sample = byteBuffer.short.toFloat() / 32768.0f
                audioData[i] = sample
            }

            Log.i(TAG, "Successfully read WAV file: $sampleCount samples")
            return audioData

        } catch (e: Exception) {
            Log.e(TAG, "Error reading WAV file", e)
            return null
        }
    }

    /**
     * Get audio duration in seconds
     */
    fun getAudioDuration(audioFilePath: String): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(audioFilePath)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            durationStr?.toLong() ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "Error getting audio duration", e)
            0L
        }
    }

    /**
     * Get audio sample rate
     */
    fun getAudioSampleRate(audioFilePath: String): Int {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(audioFilePath)
            val sampleRateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUDIO_SAMPLE_RATE)
            retriever.release()
            sampleRateStr?.toInt() ?: WHISPER_SAMPLE_RATE
        } catch (e: Exception) {
            Log.e(TAG, "Error getting audio sample rate", e)
            WHISPER_SAMPLE_RATE
        }
    }
}
