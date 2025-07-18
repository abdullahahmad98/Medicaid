package com.example.medicaid.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

class AudioRecordingRepository(private val context: Context) {

    private val gson = Gson()
    private val recordingsFile = File(context.filesDir, "recordings.json")
    private val audioDir = File(context.filesDir, "audio")
    private val transcriptsDir = File(context.filesDir, "transcripts")

    init {
        // Create directories if they don't exist
        if (!audioDir.exists()) {
            audioDir.mkdirs()
        }
        if (!transcriptsDir.exists()) {
            transcriptsDir.mkdirs()
        }
    }

    suspend fun getAllRecordings(): List<AudioRecording> = withContext(Dispatchers.IO) {
        try {
            if (!recordingsFile.exists()) {
                return@withContext emptyList()
            }

            val jsonString = recordingsFile.readText()
            val type = object : TypeToken<List<AudioRecording>>() {}.type
            gson.fromJson(jsonString, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getRecordingById(id: String): AudioRecording? = withContext(Dispatchers.IO) {
        getAllRecordings().find { it.id == id }
    }

    suspend fun saveRecording(recording: AudioRecording) = withContext(Dispatchers.IO) {
        val recordings = getAllRecordings().toMutableList()
        recordings.add(recording)
        saveRecordings(recordings)

        // Save transcript to separate file if it exists
        if (recording.transcription.isNotEmpty()) {
            saveTranscriptToFile(recording.id, recording.transcription)
        }
    }

    suspend fun updateRecording(recording: AudioRecording) = withContext(Dispatchers.IO) {
        val recordings = getAllRecordings().toMutableList()
        val index = recordings.indexOfFirst { it.id == recording.id }

        if (index != -1) {
            recordings[index] = recording
            saveRecordings(recordings)

            // Update transcript file
            if (recording.transcription.isNotEmpty()) {
                saveTranscriptToFile(recording.id, recording.transcription)
            }
        }
    }

    suspend fun deleteRecording(id: String) = withContext(Dispatchers.IO) {
        val recordings = getAllRecordings().toMutableList()
        val recording = recordings.find { it.id == id }

        if (recording != null) {
            // Delete audio file
            val audioFile = File(recording.filePath)
            if (audioFile.exists()) {
                audioFile.delete()
            }

            // Delete transcript file
            val transcriptFile = File(transcriptsDir, "${id}.txt")
            if (transcriptFile.exists()) {
                transcriptFile.delete()
            }

            // Remove from recordings list
            recordings.removeAll { it.id == id }
            saveRecordings(recordings)
        }
    }

    private suspend fun saveRecordings(recordings: List<AudioRecording>) = withContext(Dispatchers.IO) {
        val jsonString = gson.toJson(recordings)
        recordingsFile.writeText(jsonString)
    }

    private suspend fun saveTranscriptToFile(recordingId: String, transcript: String) = withContext(Dispatchers.IO) {
        val transcriptFile = File(transcriptsDir, "${recordingId}.txt")
        transcriptFile.writeText(transcript)
    }

    suspend fun getTranscriptFromFile(recordingId: String): String? = withContext(Dispatchers.IO) {
        val transcriptFile = File(transcriptsDir, "${recordingId}.txt")
        if (transcriptFile.exists()) {
            transcriptFile.readText()
        } else {
            null
        }
    }

    fun getAudioDirectory(): File = audioDir

    fun getTranscriptsDirectory(): File = transcriptsDir

    suspend fun createUniqueAudioFile(extension: String = "wav"): File = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val fileName = "recording_${timestamp}.${extension}"
        File(audioDir, fileName)
    }
}
