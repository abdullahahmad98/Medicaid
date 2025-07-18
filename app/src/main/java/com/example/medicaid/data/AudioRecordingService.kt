package com.example.medicaid.data

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

class AudioRecordingService(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var recordingStartTime: Long = 0
    private val repository = AudioRecordingRepository(context)

    companion object {
        private const val TAG = "AudioRecordingService"
    }

    suspend fun startRecording(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting audio recording")

            // Create unique file for recording
            recordingFile = repository.createUniqueAudioFile("m4a")

            // Initialize MediaRecorder
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(recordingFile?.absolutePath)

                // Set audio parameters after encoder
                setAudioSamplingRate(44100) // High quality audio
                setAudioEncodingBitRate(128000) // 128kbps for good quality
                setAudioChannels(1) // Mono for speech

                prepare()
                start()
            }

            recordingStartTime = System.currentTimeMillis()
            Log.d(TAG, "Recording started successfully: ${recordingFile?.absolutePath}")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording: ${e.message}", e)
            cleanup()
            false
        }
    }

    suspend fun stopRecording(): AudioRecording? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Stopping audio recording")

            val recorder = mediaRecorder ?: return@withContext null
            val file = recordingFile ?: return@withContext null

            recorder.stop()
            recorder.release()

            val duration = System.currentTimeMillis() - recordingStartTime
            val recordingId = UUID.randomUUID().toString()

            val recording = AudioRecording(
                id = recordingId,
                fileName = file.name,
                filePath = file.absolutePath,
                duration = duration,
                dateCreated = Date(),
                transcription = "",
                isTranscribed = false
            )

            cleanup()

            Log.d(TAG, "Recording stopped successfully: ${file.absolutePath}, Duration: ${duration}ms")
            recording

        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording: ${e.message}", e)
            cleanup()
            null
        }
    }

    suspend fun cancelRecording(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Cancelling audio recording")

            mediaRecorder?.apply {
                stop()
                release()
            }

            // Delete the recording file
            recordingFile?.let { file ->
                if (file.exists()) {
                    file.delete()
                    Log.d(TAG, "Deleted cancelled recording file: ${file.absolutePath}")
                }
            }

            cleanup()
            true

        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling recording: ${e.message}", e)
            cleanup()
            false
        }
    }

    fun getRecordingDuration(): Long {
        return if (recordingStartTime > 0) {
            System.currentTimeMillis() - recordingStartTime
        } else {
            0L
        }
    }

    fun isRecording(): Boolean {
        return mediaRecorder != null && recordingStartTime > 0
    }

    private fun cleanup() {
        mediaRecorder?.release()
        mediaRecorder = null
        recordingFile = null
        recordingStartTime = 0
    }

    fun release() {
        cleanup()
    }
}
