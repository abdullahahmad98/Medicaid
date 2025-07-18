package com.example.medicaid.data

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class AudioPlaybackService(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var currentlyPlayingId: String? = null
    private var playbackPosition: Int = 0
    private var isPlaying: Boolean = false
    private var isPaused: Boolean = false

    companion object {
        private const val TAG = "AudioPlaybackService"
    }

    suspend fun playAudio(audioFilePath: String, recordingId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Stop any currently playing audio
            stopPlayback()

            Log.d(TAG, "Starting playback for: $audioFilePath")

            // Check if file exists
            val file = java.io.File(audioFilePath)
            if (!file.exists()) {
                Log.e(TAG, "Audio file does not exist: $audioFilePath")
                return@withContext false
            }

            Log.d(TAG, "File exists, size: ${file.length()} bytes")

            mediaPlayer = MediaPlayer().apply {
                try {
                    setDataSource(audioFilePath)

                    // Use synchronous preparation for better error handling
                    prepare()

                    Log.d(TAG, "MediaPlayer prepared successfully")
                    Log.d(TAG, "Audio duration: ${duration}ms")

                    // Start playback immediately
                    start()

                    this@AudioPlaybackService.isPlaying = true
                    this@AudioPlaybackService.isPaused = false
                    this@AudioPlaybackService.currentlyPlayingId = recordingId
                    this@AudioPlaybackService.playbackPosition = 0

                    Log.d(TAG, "Playback started successfully for recording: $recordingId")

                    setOnCompletionListener { mp ->
                        Log.d(TAG, "Playback completed for recording: $recordingId")
                        this@AudioPlaybackService.cleanup()
                    }

                    setOnErrorListener { mp, what, extra ->
                        Log.e(TAG, "MediaPlayer error during playback: what=$what, extra=$extra")
                        this@AudioPlaybackService.cleanup()
                        false
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Error preparing MediaPlayer: ${e.message}", e)
                    release()
                    throw e
                }
            }

            return@withContext true

        } catch (e: IOException) {
            Log.e(TAG, "IOException playing audio: ${e.message}", e)
            cleanup()
            false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error playing audio: ${e.message}", e)
            cleanup()
            false
        }
    }

    fun pausePlayback() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                    playbackPosition = player.currentPosition
                    isPlaying = false
                    isPaused = true
                    Log.d(TAG, "Playback paused at position: $playbackPosition")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing playback: ${e.message}", e)
        }
    }

    fun resumePlayback() {
        try {
            mediaPlayer?.let { player ->
                if (isPaused) {
                    player.seekTo(playbackPosition)
                    player.start()
                    isPlaying = true
                    isPaused = false
                    Log.d(TAG, "Playback resumed from position: $playbackPosition")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming playback: ${e.message}", e)
        }
    }

    fun stopPlayback() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying || isPaused) {
                    player.stop()
                    Log.d(TAG, "Playback stopped for recording: $currentlyPlayingId")
                }
            }
            cleanup()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping playback: ${e.message}", e)
            cleanup()
        }
    }

    fun seekTo(position: Int) {
        try {
            mediaPlayer?.let { player ->
                player.seekTo(position)
                playbackPosition = position
                Log.d(TAG, "Seeked to position: $position")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking: ${e.message}", e)
        }
    }

    fun getCurrentPosition(): Int {
        return try {
            mediaPlayer?.currentPosition ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current position: ${e.message}", e)
            0
        }
    }

    fun getDuration(): Int {
        return try {
            mediaPlayer?.duration ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "Error getting duration: ${e.message}", e)
            0
        }
    }

    fun isPlaying(): Boolean = isPlaying

    fun isPaused(): Boolean = isPaused

    fun getCurrentlyPlayingId(): String? = currentlyPlayingId

    private fun cleanup() {
        try {
            mediaPlayer?.release()
            mediaPlayer = null
            currentlyPlayingId = null
            playbackPosition = 0
            isPlaying = false
            isPaused = false
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup: ${e.message}", e)
        }
    }

    fun release() {
        cleanup()
    }
}
