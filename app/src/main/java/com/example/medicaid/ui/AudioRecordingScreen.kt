package com.example.medicaid.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medicaid.data.AudioRecording
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import java.util.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AudioRecordingScreen(
    modifier: Modifier = Modifier,
    viewModel: AudioRecordingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val recordings by viewModel.recordings.collectAsState()

    // Permission handling
    val audioPermissionState = rememberPermissionState(
        android.Manifest.permission.RECORD_AUDIO
    )

    // Timer for recording duration
    var recordingDuration by remember { mutableStateOf(0L) }

    LaunchedEffect(uiState.isRecording) {
        if (uiState.isRecording) {
            while (uiState.isRecording) {
                recordingDuration = viewModel.getRecordingDuration()
                delay(100)
            }
        } else {
            recordingDuration = 0L
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Audio Recorder & Transcriber",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Whisper initialization status
        if (uiState.isInitializingWhisper) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Initializing Whisper AI...",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        // Recording controls
        RecordingControls(
            isRecording = uiState.isRecording,
            recordingDuration = recordingDuration,
            isPermissionGranted = audioPermissionState.status.isGranted,
            onRequestPermission = { audioPermissionState.launchPermissionRequest() },
            onStartRecording = viewModel::startRecording,
            onStopRecording = viewModel::stopRecording,
            onCancelRecording = viewModel::cancelRecording,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Error messages
        uiState.recordingError?.let { error ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        uiState.transcriptionError?.let { error ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // Recordings list
        Text(
            text = "Recordings (${recordings.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(recordings) { recording ->
                RecordingItem(
                    recording = recording,
                    isTranscribing = uiState.transcribingRecordingId == recording.id,
                    isWhisperReady = uiState.isWhisperReady,
                    onTranscribe = viewModel::transcribeRecording,
                    onDelete = viewModel::deleteRecording,
                    onUpdateTranscript = viewModel::updateTranscript
                )
            }
        }
    }
}

@Composable
fun RecordingControls(
    isRecording: Boolean,
    recordingDuration: Long,
    isPermissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isPermissionGranted) {
                Text(
                    text = "Microphone permission is required to record audio",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Button(
                    onClick = onRequestPermission
                ) {
                    Text("Grant Permission")
                }
            } else {
                if (isRecording) {
                    Text(
                        text = "Recording: ${formatDuration(recordingDuration)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onStopRecording,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop")
                            Text("Stop", modifier = Modifier.padding(start = 4.dp))
                        }

                        OutlinedButton(
                            onClick = onCancelRecording
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Cancel")
                            Text("Cancel", modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                } else {
                    FloatingActionButton(
                        onClick = onStartRecording,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "Start Recording",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(milliseconds: Long): String {
    val seconds = milliseconds / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, remainingSeconds)
}
