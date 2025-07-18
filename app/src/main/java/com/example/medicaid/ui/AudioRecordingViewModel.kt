package com.example.medicaid.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicaid.data.AudioPlaybackService
import com.example.medicaid.data.AudioRecording
import com.example.medicaid.data.AudioRecordingRepository
import com.example.medicaid.data.AudioRecordingService
import com.example.medicaid.data.WhisperTranscriptionService
import com.example.medicaid.data.WhisperModel
import com.example.medicaid.data.ModelDownloadProgress
import kotlinx.coroutines.delay
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
    val transcriptionError: String? = null,
    val playingRecordingId: String? = null,
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val currentPosition: Int = 0,
    val duration: Int = 0,
    val currentModel: WhisperModel? = null,
    val availableModels: List<WhisperModel> = WhisperModel.availableModels,
    val modelDownloadProgress: Map<String, ModelDownloadProgress> = emptyMap(),
    val showModelSelection: Boolean = false
)

class AudioRecordingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AudioRecordingRepository(application)
    private val audioService = AudioRecordingService(application)
    private val whisperService = WhisperTranscriptionService(application)
    private val playbackService = AudioPlaybackService(application)

    private val _uiState = MutableStateFlow(AudioRecordingUiState())
    val uiState: StateFlow<AudioRecordingUiState> = _uiState.asStateFlow()

    private val _recordings = MutableStateFlow<List<AudioRecording>>(emptyList())
    val recordings: StateFlow<List<AudioRecording>> = _recordings.asStateFlow()

    init {
        initializeWhisper()
        loadRecordings()
        observeModelDownloadProgress()
        loadCurrentModel()
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

    private fun loadCurrentModel() {
        viewModelScope.launch {
            val currentModel = whisperService.getCurrentModel()
            _uiState.value = _uiState.value.copy(currentModel = currentModel)
        }
    }

    private fun observeModelDownloadProgress() {
        viewModelScope.launch {
            whisperService.getModelDownloadService().downloadProgress.collect { progress ->
                _uiState.value = _uiState.value.copy(modelDownloadProgress = progress)
            }
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

    fun playRecording(recording: AudioRecording) {
        viewModelScope.launch {
            try {
                Log.d("AudioViewModel", "Play button clicked for recording: ${recording.fileName}")
                Log.d("AudioViewModel", "Recording file path: ${recording.filePath}")

                // Stop any currently playing audio
                if (_uiState.value.isPlaying || _uiState.value.isPaused) {
                    Log.d("AudioViewModel", "Stopping current playback")
                    stopPlayback()
                }

                val success = playbackService.playAudio(recording.filePath, recording.id)
                Log.d("AudioViewModel", "Playback service returned: $success")

                if (success) {
                    // Give MediaPlayer a moment to get the duration
                    delay(100)
                    val duration = playbackService.getDuration()
                    Log.d("AudioViewModel", "Audio duration: ${duration}ms")

                    _uiState.value = _uiState.value.copy(
                        playingRecordingId = recording.id,
                        isPlaying = true,
                        isPaused = false,
                        duration = duration
                    )

                    Log.d("AudioViewModel", "UI state updated - isPlaying: ${_uiState.value.isPlaying}")

                    // Start position tracking
                    startPositionTracking()
                } else {
                    Log.e("AudioViewModel", "Failed to start playback")
                }
            } catch (e: Exception) {
                Log.e("AudioViewModel", "Exception in playRecording: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    recordingError = "Failed to play audio: ${e.message}"
                )
            }
        }
    }

    fun pausePlayback() {
        playbackService.pausePlayback()
        _uiState.value = _uiState.value.copy(
            isPlaying = false,
            isPaused = true
        )
    }

    fun resumePlayback() {
        playbackService.resumePlayback()
        _uiState.value = _uiState.value.copy(
            isPlaying = true,
            isPaused = false
        )
        startPositionTracking()
    }

    fun stopPlayback() {
        playbackService.stopPlayback()
        _uiState.value = _uiState.value.copy(
            playingRecordingId = null,
            isPlaying = false,
            isPaused = false,
            currentPosition = 0,
            duration = 0
        )
    }

    fun seekTo(position: Int) {
        playbackService.seekTo(position)
        _uiState.value = _uiState.value.copy(
            currentPosition = position
        )
    }

    private fun startPositionTracking() {
        viewModelScope.launch {
            while (_uiState.value.isPlaying) {
                val currentPosition = playbackService.getCurrentPosition()
                val duration = playbackService.getDuration()

                _uiState.value = _uiState.value.copy(
                    currentPosition = currentPosition,
                    duration = duration
                )

                // Check if playback completed
                if (!playbackService.isPlaying() && !playbackService.isPaused()) {
                    _uiState.value = _uiState.value.copy(
                        playingRecordingId = null,
                        isPlaying = false,
                        isPaused = false,
                        currentPosition = 0,
                        duration = 0
                    )
                    break
                }

                delay(200) // Update every 200ms for better performance
            }
        }
    }

    // Model Management Functions
    fun showModelSelection() {
        _uiState.value = _uiState.value.copy(showModelSelection = true)
    }

    fun hideModelSelection() {
        _uiState.value = _uiState.value.copy(showModelSelection = false)
    }

    fun downloadModel(model: WhisperModel) {
        viewModelScope.launch {
            try {
                val success = whisperService.getModelDownloadService().downloadModel(model)
                if (success) {
                    Log.i("AudioViewModel", "Model ${model.name} downloaded successfully")
                } else {
                    Log.e("AudioViewModel", "Failed to download model ${model.name}")
                }
            } catch (e: Exception) {
                Log.e("AudioViewModel", "Error downloading model ${model.name}", e)
            }
        }
    }

    fun selectModel(model: WhisperModel) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isInitializingWhisper = true)

                val success = whisperService.switchModel(model.name)
                if (success) {
                    _uiState.value = _uiState.value.copy(
                        currentModel = model,
                        isInitializingWhisper = false,
                        isWhisperReady = true,
                        showModelSelection = false
                    )
                    Log.i("AudioViewModel", "Successfully switched to model: ${model.name}")
                } else {
                    _uiState.value = _uiState.value.copy(
                        isInitializingWhisper = false,
                        transcriptionError = "Failed to switch to model: ${model.name}"
                    )
                    Log.e("AudioViewModel", "Failed to switch to model: ${model.name}")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isInitializingWhisper = false,
                    transcriptionError = "Error switching model: ${e.message}"
                )
                Log.e("AudioViewModel", "Error switching model", e)
            }
        }
    }

    fun deleteModel(model: WhisperModel) {
        viewModelScope.launch {
            try {
                val success = whisperService.getModelDownloadService().deleteModel(model)
                if (success) {
                    Log.i("AudioViewModel", "Model ${model.name} deleted successfully")
                } else {
                    Log.e("AudioViewModel", "Failed to delete model ${model.name}")
                }
            } catch (e: Exception) {
                Log.e("AudioViewModel", "Error deleting model ${model.name}", e)
            }
        }
    }

    fun clearErrors() {
        _uiState.value = _uiState.value.copy(
            recordingError = null,
            transcriptionError = null
        )
    }

    override fun onCleared() {
        super.onCleared()
        whisperService.cleanup()
        playbackService.release()
    }
}
