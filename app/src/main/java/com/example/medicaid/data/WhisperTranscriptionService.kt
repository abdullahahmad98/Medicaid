package com.example.medicaid.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class WhisperTranscriptionService(private val context: Context) {

    private val whisperNative = WhisperNative()
    private val modelDownloadService = WhisperModelDownloadService(context)
    private val preferencesManager = WhisperPreferencesManager(context)
    private val audioProcessor = AudioProcessor(context)

    private var isInitialized = false
    private var currentModel: WhisperModel? = null

    companion object {
        private const val TAG = "WhisperTranscriptionService"
    }

    suspend fun initializeWhisper(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Initializing Whisper AI...")

            // Get the selected model from preferences
            val selectedModelName = preferencesManager.getSelectedModel()
            val selectedModel = WhisperModel.availableModels.find { it.name == selectedModelName }
                ?: WhisperModel.availableModels.first { it.name == "base" }

            // Check if the model is downloaded
            if (!modelDownloadService.isModelDownloaded(selectedModel)) {
                Log.w(TAG, "Selected model ${selectedModel.name} not downloaded, downloading now...")
                val downloadSuccess = modelDownloadService.downloadModel(selectedModel)
                if (!downloadSuccess) {
                    Log.e(TAG, "Failed to download model ${selectedModel.name}")
                    return@withContext false
                }
            }

            // Get the model path
            val modelPath = modelDownloadService.getModelPath(selectedModel)
            if (modelPath == null) {
                Log.e(TAG, "Model path is null for ${selectedModel.name}")
                return@withContext false
            }

            // Initialize the native Whisper context
            val initSuccess = whisperNative.initializeWhisper(modelPath)
            if (!initSuccess) {
                Log.e(TAG, "Failed to initialize native Whisper context")
                return@withContext false
            }

            isInitialized = true
            currentModel = selectedModel
            Log.i(TAG, "Whisper AI initialized successfully with model: ${selectedModel.name}")

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Whisper: ${e.message}", e)
            false
        }
    }

    suspend fun transcribeAudioFile(audioFilePath: String): TranscriptionResult? = withContext(Dispatchers.IO) {
        if (!isInitialized) {
            Log.e(TAG, "Whisper not initialized")
            return@withContext null
        }

        try {
            val startTime = System.currentTimeMillis()

            Log.i(TAG, "Starting transcription for: $audioFilePath")

            // Convert audio to the format required by Whisper
            val audioData = audioProcessor.convertAudioForWhisper(audioFilePath)
            if (audioData == null) {
                Log.e(TAG, "Failed to convert audio file")
                return@withContext null
            }

            // Get audio sample rate
            val sampleRate = audioProcessor.getAudioSampleRate(audioFilePath)

            // Transcribe using native Whisper
            val transcriptionText = whisperNative.transcribeAudio(audioData, sampleRate)

            val processingTime = System.currentTimeMillis() - startTime

            if (transcriptionText.isNotEmpty()) {
                Log.i(TAG, "Transcription completed in ${processingTime}ms")
                Log.d(TAG, "Transcription result: $transcriptionText")

                TranscriptionResult(
                    text = transcriptionText.trim(),
                    confidence = 0.95f, // Whisper doesn't provide confidence scores directly
                    processingTime = processingTime,
                    modelUsed = currentModel?.name ?: "unknown"
                )
            } else {
                Log.w(TAG, "Transcription returned empty result")
                null
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error transcribing audio: ${e.message}", e)
            null
        }
    }

    suspend fun switchModel(modelName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val newModel = WhisperModel.availableModels.find { it.name == modelName }
            if (newModel == null) {
                Log.e(TAG, "Model not found: $modelName")
                return@withContext false
            }

            // Check if model is downloaded
            if (!modelDownloadService.isModelDownloaded(newModel)) {
                Log.w(TAG, "Model $modelName not downloaded")
                return@withContext false
            }

            // Cleanup current model
            cleanup()

            // Save new model preference
            preferencesManager.setSelectedModel(modelName)

            // Initialize with new model
            val initSuccess = initializeWhisper()
            if (initSuccess) {
                Log.i(TAG, "Successfully switched to model: $modelName")
            } else {
                Log.e(TAG, "Failed to switch to model: $modelName")
            }

            initSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Error switching model", e)
            false
        }
    }

    fun getCurrentModel(): WhisperModel? = currentModel

    fun getModelDownloadService(): WhisperModelDownloadService = modelDownloadService

    fun cleanup() {
        try {
            whisperNative.cleanup()
            isInitialized = false
            currentModel = null
            Log.i(TAG, "Whisper cleanup complete")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }

    fun isReady(): Boolean = isInitialized && whisperNative.isInitialized()
}
