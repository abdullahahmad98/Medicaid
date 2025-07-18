package com.example.medicaid.data

import androidx.annotation.Keep

@Keep
data class WhisperModel(
    val name: String,
    val displayName: String,
    val size: Long, // Size in bytes
    val downloadUrl: String,
    val fileName: String,
    val description: String,
    val isDownloaded: Boolean = false,
    val downloadProgress: Float = 0f
) {
    companion object {
        val availableModels = listOf(
            WhisperModel(
                name = "tiny",
                displayName = "Tiny",
                size = 39_000_000L, // ~39 MB
                downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin",
                fileName = "ggml-tiny.bin",
                description = "Fastest model, lower accuracy"
            ),
            WhisperModel(
                name = "tiny.en",
                displayName = "Tiny (English)",
                size = 39_000_000L, // ~39 MB
                downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.en.bin",
                fileName = "ggml-tiny.en.bin",
                description = "Fastest model, English only"
            ),
            WhisperModel(
                name = "base",
                displayName = "Base",
                size = 142_000_000L, // ~142 MB
                downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin",
                fileName = "ggml-base.bin",
                description = "Good balance of speed and accuracy"
            ),
            WhisperModel(
                name = "base.en",
                displayName = "Base (English)",
                size = 142_000_000L, // ~142 MB
                downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.en.bin",
                fileName = "ggml-base.en.bin",
                description = "Good balance, English only"
            ),
            WhisperModel(
                name = "small",
                displayName = "Small",
                size = 466_000_000L, // ~466 MB
                downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small.bin",
                fileName = "ggml-small.bin",
                description = "Better accuracy, slower processing"
            ),
            WhisperModel(
                name = "small.en",
                displayName = "Small (English)",
                size = 466_000_000L, // ~466 MB
                downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small.en.bin",
                fileName = "ggml-small.en.bin",
                description = "Better accuracy, English only"
            ),
            WhisperModel(
                name = "medium",
                displayName = "Medium",
                size = 1_500_000_000L, // ~1.5 GB
                downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-medium.bin",
                fileName = "ggml-medium.bin",
                description = "High accuracy, slower processing"
            ),
            WhisperModel(
                name = "medium.en",
                displayName = "Medium (English)",
                size = 1_500_000_000L, // ~1.5 GB
                downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-medium.en.bin",
                fileName = "ggml-medium.en.bin",
                description = "High accuracy, English only"
            )
        )
    }
}

enum class ModelDownloadStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    DOWNLOADED,
    ERROR
}

@Keep
data class ModelDownloadProgress(
    val modelName: String,
    val progress: Float,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val status: ModelDownloadStatus
)
