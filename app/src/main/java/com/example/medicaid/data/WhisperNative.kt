package com.example.medicaid.data

import android.util.Log

class WhisperNative {
    companion object {
        private const val TAG = "WhisperNative"

        init {
            try {
                System.loadLibrary("whisper-jni")
                Log.i(TAG, "Native library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load native library", e)
            }
        }
    }

    external fun initializeWhisper(modelPath: String): Boolean
    external fun transcribeAudio(audioData: FloatArray, sampleRate: Int): String
    external fun cleanup()
    external fun isInitialized(): Boolean
}
