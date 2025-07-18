package com.example.medicaid.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.medicaid.data.AudioRecording
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RecordingItem(
    recording: AudioRecording,
    isTranscribing: Boolean,
    isWhisperReady: Boolean,
    onTranscribe: (AudioRecording) -> Unit,
    onDelete: (String) -> Unit,
    onUpdateTranscript: (String, String) -> Unit
) {
    var showTranscriptDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Recording info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = recording.fileName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatDate(recording.dateCreated),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Duration: ${formatDuration(recording.duration)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Transcribe button
                    if (recording.isTranscribed) {
                        IconButton(
                            onClick = { showTranscriptDialog = true }
                        ) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = "View Transcript",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        IconButton(
                            onClick = { onTranscribe(recording) },
                            enabled = isWhisperReady && !isTranscribing
                        ) {
                            if (isTranscribing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.Subtitles,
                                    contentDescription = "Transcribe",
                                    tint = if (isWhisperReady) MaterialTheme.colorScheme.primary
                                           else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Delete button
                    IconButton(
                        onClick = { showDeleteDialog = true }
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Transcription status
            if (recording.isTranscribed) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Transcribed",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Transcribed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            } else if (isTranscribing) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Transcribing...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            } else if (!isWhisperReady) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Whisper AI not ready",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // Transcript dialog
    if (showTranscriptDialog) {
        TranscriptDialog(
            recording = recording,
            onDismiss = { showTranscriptDialog = false },
            onUpdateTranscript = onUpdateTranscript
        )
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Recording") },
            text = { Text("Are you sure you want to delete this recording and its transcript?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(recording.id)
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TranscriptDialog(
    recording: AudioRecording,
    onDismiss: () -> Unit,
    onUpdateTranscript: (String, String) -> Unit
) {
    var transcriptText by remember { mutableStateOf(recording.transcription) }
    var isEditing by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Transcript - ${recording.fileName}") },
        text = {
            Column {
                if (isEditing) {
                    OutlinedTextField(
                        value = transcriptText,
                        onValueChange = { transcriptText = it },
                        label = { Text("Transcript") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        maxLines = 10
                    )
                } else {
                    Text(
                        text = transcriptText.ifEmpty { "No transcript available" },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (isEditing) {
                TextButton(
                    onClick = {
                        onUpdateTranscript(recording.id, transcriptText)
                        isEditing = false
                    }
                ) {
                    Text("Save")
                }
            } else {
                TextButton(
                    onClick = { isEditing = true }
                ) {
                    Text("Edit")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (isEditing) {
                        isEditing = false
                        transcriptText = recording.transcription
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text(if (isEditing) "Cancel" else "Close")
            }
        }
    )
}

private fun formatDate(date: Date): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return formatter.format(date)
}

private fun formatDuration(milliseconds: Long): String {
    val seconds = milliseconds / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}
