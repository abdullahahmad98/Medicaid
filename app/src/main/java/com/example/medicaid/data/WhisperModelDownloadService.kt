package com.example.medicaid.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.*
import okio.buffer
import okio.sink
import java.io.File
import java.io.IOException

class WhisperModelDownloadService(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val _downloadProgress = MutableStateFlow<Map<String, ModelDownloadProgress>>(emptyMap())
    val downloadProgress: Flow<Map<String, ModelDownloadProgress>> = _downloadProgress.asStateFlow()

    private val modelsDirectory = File(context.filesDir, "whisper_models")

    companion object {
        private const val TAG = "WhisperModelDownload"
    }

    init {
        if (!modelsDirectory.exists()) {
            modelsDirectory.mkdirs()
        }
    }

    suspend fun downloadModel(model: WhisperModel): Boolean = withContext(Dispatchers.IO) {
        try {
            val modelFile = File(modelsDirectory, model.fileName)

            if (modelFile.exists()) {
                Log.i(TAG, "Model ${model.name} already exists")
                updateProgress(model.name, ModelDownloadStatus.DOWNLOADED, 1.0f, model.size, model.size)
                return@withContext true
            }

            Log.i(TAG, "Starting download for model: ${model.name}")
            updateProgress(model.name, ModelDownloadStatus.DOWNLOADING, 0f, 0L, model.size)

            val request = Request.Builder()
                .url(model.downloadUrl)
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.e(TAG, "Download failed: ${response.code}")
                updateProgress(model.name, ModelDownloadStatus.ERROR, 0f, 0L, model.size)
                return@withContext false
            }

            val responseBody = response.body ?: run {
                Log.e(TAG, "Response body is null")
                updateProgress(model.name, ModelDownloadStatus.ERROR, 0f, 0L, model.size)
                return@withContext false
            }

            val totalBytes = responseBody.contentLength()
            val source = responseBody.source()
            val sink = modelFile.sink().buffer()

            var bytesRead = 0L
            val bufferSize = 8192L

            while (true) {
                val read = source.read(sink.buffer, bufferSize)
                if (read == -1L) break

                bytesRead += read
                sink.emit()

                val progress = if (totalBytes > 0) bytesRead.toFloat() / totalBytes else 0f
                updateProgress(model.name, ModelDownloadStatus.DOWNLOADING, progress, bytesRead, totalBytes)
            }

            sink.close()
            response.close()

            Log.i(TAG, "Download completed for model: ${model.name}")
            updateProgress(model.name, ModelDownloadStatus.DOWNLOADED, 1.0f, totalBytes, totalBytes)

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading model ${model.name}", e)
            updateProgress(model.name, ModelDownloadStatus.ERROR, 0f, 0L, model.size)
            false
        }
    }

    fun isModelDownloaded(model: WhisperModel): Boolean {
        val modelFile = File(modelsDirectory, model.fileName)
        return modelFile.exists() && modelFile.length() > 0
    }

    fun getModelPath(model: WhisperModel): String? {
        val modelFile = File(modelsDirectory, model.fileName)
        return if (modelFile.exists()) modelFile.absolutePath else null
    }

    fun deleteModel(model: WhisperModel): Boolean {
        val modelFile = File(modelsDirectory, model.fileName)
        return if (modelFile.exists()) {
            val deleted = modelFile.delete()
            if (deleted) {
                updateProgress(model.name, ModelDownloadStatus.NOT_DOWNLOADED, 0f, 0L, model.size)
            }
            deleted
        } else {
            true
        }
    }

    fun getDownloadedModels(): List<WhisperModel> {
        return WhisperModel.availableModels.filter { isModelDownloaded(it) }
    }

    fun getTotalStorageUsed(): Long {
        var totalSize = 0L
        WhisperModel.availableModels.forEach { model ->
            val modelFile = File(modelsDirectory, model.fileName)
            if (modelFile.exists()) {
                totalSize += modelFile.length()
            }
        }
        return totalSize
    }

    private fun updateProgress(
        modelName: String,
        status: ModelDownloadStatus,
        progress: Float,
        bytesDownloaded: Long,
        totalBytes: Long
    ) {
        val currentProgress = _downloadProgress.value.toMutableMap()
        currentProgress[modelName] = ModelDownloadProgress(
            modelName = modelName,
            progress = progress,
            bytesDownloaded = bytesDownloaded,
            totalBytes = totalBytes,
            status = status
        )
        _downloadProgress.value = currentProgress
    }
}
