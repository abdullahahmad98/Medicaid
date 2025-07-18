package com.example.medicaid.data

import java.util.Date

data class AudioRecording(
    val id: String,
    val fileName: String,
    val filePath: String,
    val duration: Long, // in milliseconds
    val dateCreated: Date,
    val transcription: String = "",
    val isTranscribed: Boolean = false
)

data class TranscriptionResult(
    val text: String,
    val confidence: Float,
    val processingTime: Long
)
