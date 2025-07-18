package com.example.medicaid.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicaid.data.AudioRecording
import com.example.medicaid.data.AudioRecordingRepository
import com.example.medicaid.data.AudioRecordingService
import com.example.medicaid.data.WhisperTranscriptionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AudioRecordingUiState(
    val isRecording: Boolean = false,
    val isInitializingWhisper: Boolean = false,
    val isWhisperReady: Boolean = false,
    val transcribingRecordingId: String? = null,
    val recordingError: String? = null,
    val transcriptionError: String? = null
)

class AudioRecordingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AudioRecordingRepository(application)
    private val audioService = AudioRecordingService(application)
    private val whisperService = WhisperTranscriptionService(application)

    private val _uiState = MutableStateFlow(AudioRecordingUiState())
    val uiState: StateFlow<AudioRecordingUiState> = _uiState.asStateFlow()

    private val _recordings = MutableStateFlow<List<AudioRecording>>(emptyList())
    val recordings: StateFlow<List<AudioRecording>> = _recordings.asStateFlow()

    init {
        initializeWhisper()
        loadRecordings()
    }

    private fun initializeWhisper() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isInitializingWhisper = true)

            try {
                val success = whisperService.initializeWhisper()
                _uiState.value = _uiState.value.copy(
                    isInitializingWhisper = false,
                    isWhisperReady = success
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isInitializingWhisper = false,
                    isWhisperReady = false,
                    transcriptionError = "Failed to initialize Whisper: ${e.message}"
                )
            }
        }
    }

    private fun loadRecordings() {
        viewModelScope.launch {
            _recordings.value = repository.getAllRecordings()
        }
    }

    fun startRecording() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    recordingError = null,
                    isRecording = true
                )

                audioService.startRecording()

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRecording = false,
                    recordingError = "Failed to start recording: ${e.message}"
                )
            }
        }
    }

    fun stopRecording() {
        viewModelScope.launch {
            try {
                val recording = audioService.stopRecording()
                if (recording != null) {
                    repository.saveRecording(recording)
                    loadRecordings()
                }

                _uiState.value = _uiState.value.copy(
                    isRecording = false,
                    recordingError = null
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRecording = false,
                    recordingError = "Failed to stop recording: ${e.message}"
                )
            }
        }
    }

    fun cancelRecording() {
        viewModelScope.launch {
            try {
                audioService.cancelRecording()
                _uiState.value = _uiState.value.copy(
                    isRecording = false,
                    recordingError = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRecording = false,
                    recordingError = "Failed to cancel recording: ${e.message}"
                )
            }
        }
    }

    fun transcribeRecording(recording: AudioRecording) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    transcribingRecordingId = recording.id,
                    transcriptionError = null
                )

                val result = whisperService.transcribeAudioFile(recording.filePath)

                if (result != null) {
                    val updatedRecording = recording.copy(
                        transcription = result.text,
                        isTranscribed = true
                    )
                    repository.updateRecording(updatedRecording)
                    loadRecordings()
                }

                _uiState.value = _uiState.value.copy(
                    transcribingRecordingId = null
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    transcribingRecordingId = null,
                    transcriptionError = "Failed to transcribe audio: ${e.message}"
                )
            }
        }
    }

    fun deleteRecording(recordingId: String) {
        viewModelScope.launch {
            try {
                repository.deleteRecording(recordingId)
                loadRecordings()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    recordingError = "Failed to delete recording: ${e.message}"
                )
            }
        }
    }

    fun updateTranscript(recordingId: String, newTranscript: String) {
        viewModelScope.launch {
            try {
                val recording = repository.getRecordingById(recordingId)
                if (recording != null) {
                    val updatedRecording = recording.copy(transcription = newTranscript)
                    repository.updateRecording(updatedRecording)
                    loadRecordings()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    transcriptionError = "Failed to update transcript: ${e.message}"
                )
            }
        }
    }

    fun getRecordingDuration(): Long {
        return audioService.getRecordingDuration()
    }

    override fun onCleared() {
        super.onCleared()
        whisperService.cleanup()
    }
}
