package com.example.medicaid.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import com.example.medicaid.data.AudioRecording
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun RecordingItem(
    recording: AudioRecording,
    isTranscribing: Boolean,
    isWhisperReady: Boolean,
    onTranscribe: (AudioRecording) -> Unit,
    onDelete: (String) -> Unit,
    onUpdateTranscript: (String, String) -> Unit,
    // Playback parameters
    isPlaying: Boolean = false,
    isPaused: Boolean = false,
    currentPosition: Int = 0,
    duration: Int = 0,
    onPlay: (AudioRecording) -> Unit = {},
    onPause: () -> Unit = {},
    onResume: () -> Unit = {},
    onStop: () -> Unit = {},
    onSeek: (Int) -> Unit = {}
) {
    var showTranscriptDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Swipe state for swipe-to-delete
    var offsetX by remember { mutableStateOf(0f) }
    val maxSwipeDistance = -200f // Maximum swipe distance to reveal delete
    val threshold = -80f // Threshold to show delete button

    // Animation for delete button
    val deleteButtonAlpha by animateFloatAsState(
        targetValue = if (offsetX < threshold) 1f else 0f,
        label = "delete_button_alpha"
    )

    val deleteButtonScale by animateFloatAsState(
        targetValue = if (offsetX < threshold) 1f else 0.8f,
        label = "delete_button_scale"
    )

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Background delete button (revealed when swiped)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    color = MaterialTheme.colorScheme.error,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(end = 16.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            IconButton(
                onClick = {
                    showDeleteDialog = true
                },
                modifier = Modifier
                    .scale(deleteButtonScale)
                    .alpha(deleteButtonAlpha)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Recording",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Main card (swipeable)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            // Snap to delete position or back to normal
                            offsetX = if (offsetX < threshold) {
                                maxSwipeDistance
                            } else {
                                0f
                            }
                        }
                    ) { _, dragAmount ->
                        // Only allow left swipe
                        if (dragAmount.x < 0 || offsetX < 0) {
                            val newOffset = offsetX + dragAmount.x
                            offsetX = newOffset.coerceIn(maxSwipeDistance, 0f)
                        }
                    }
                }
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
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Play button
                        IconButton(
                            onClick = { onPlay(recording) },
                            enabled = !isPlaying && !isPaused,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Play Audio",
                                tint = if (!isPlaying && !isPaused) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Transcribe button
                        if (recording.isTranscribed) {
                            IconButton(
                                onClick = { showTranscriptDialog = true },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    Icons.Default.Description,
                                    contentDescription = "View Transcript",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else {
                            IconButton(
                                onClick = { onTranscribe(recording) },
                                enabled = isWhisperReady && !isTranscribing,
                                modifier = Modifier.size(40.dp)
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
                                               else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Delete button - Make it more visible
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete Recording",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
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

                // Playback Controls
                if (isPlaying || isPaused) {
                    Spacer(modifier = Modifier.height(8.dp))
                    PlaybackControls(
                        isPlaying = isPlaying,
                        isPaused = isPaused,
                        currentPosition = currentPosition,
                        duration = duration,
                        onPause = onPause,
                        onResume = onResume,
                        onStop = onStop,
                        onSeek = onSeek
                    )
                }
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

@Composable
fun PlaybackControls(
    isPlaying: Boolean,
    isPaused: Boolean,
    currentPosition: Int,
    duration: Int,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onSeek: (Int) -> Unit
) {
    Column {
        // Progress bar
        if (duration > 0) {
            val progress = (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatPlaybackTime(currentPosition),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(40.dp)
                )

                Slider(
                    value = progress,
                    onValueChange = { newProgress ->
                        val newPosition = (newProgress * duration).toInt()
                        onSeek(newPosition)
                    },
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = formatPlaybackTime(duration),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(40.dp)
                )
            }
        }

        // Control buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play/Pause button
            IconButton(
                onClick = {
                    if (isPlaying) {
                        onPause()
                    } else if (isPaused) {
                        onResume()
                    }
                }
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Stop button
            IconButton(
                onClick = onStop
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
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

private fun formatPlaybackTime(milliseconds: Int): String {
    val seconds = milliseconds / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, remainingSeconds)
}
