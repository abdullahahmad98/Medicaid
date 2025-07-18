#include <jni.h>
#include <string>
#include <android/log.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include "whisper.h"

#define LOG_TAG "WhisperJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static whisper_context* g_whisper_context = nullptr;

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_medicaid_data_WhisperNative_initializeWhisper(JNIEnv *env, jobject thiz, jstring model_path) {
    const char* model_path_c = env->GetStringUTFChars(model_path, 0);

    LOGI("Initializing Whisper with model: %s", model_path_c);

    // Initialize whisper context
    struct whisper_context_params cparams = whisper_context_default_params();
    cparams.use_gpu = false; // Disable GPU for now

    g_whisper_context = whisper_init_from_file_with_params(model_path_c, cparams);

    env->ReleaseStringUTFChars(model_path, model_path_c);

    if (g_whisper_context == nullptr) {
        LOGE("Failed to initialize Whisper context");
        return JNI_FALSE;
    }

    LOGI("Whisper initialized successfully");
    return JNI_TRUE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_medicaid_data_WhisperNative_transcribeAudio(JNIEnv *env, jobject thiz,
                                                             jfloatArray audio_data,
                                                             jint sample_rate) {
    if (g_whisper_context == nullptr) {
        LOGE("Whisper context not initialized");
        return env->NewStringUTF("");
    }

    jsize audio_length = env->GetArrayLength(audio_data);
    jfloat* audio_buffer = env->GetFloatArrayElements(audio_data, 0);

    LOGI("Transcribing audio: %d samples at %d Hz", audio_length, sample_rate);

    // Set up whisper parameters
    struct whisper_full_params wparams = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    wparams.print_realtime = false;
    wparams.print_progress = false;
    wparams.print_timestamps = false;
    wparams.print_special = false;
    wparams.translate = false;
    wparams.language = "en";
    wparams.n_threads = 4;
    wparams.offset_ms = 0;
    wparams.duration_ms = 0;

    // Run inference
    int result = whisper_full(g_whisper_context, wparams, audio_buffer, audio_length);

    env->ReleaseFloatArrayElements(audio_data, audio_buffer, 0);

    if (result != 0) {
        LOGE("Whisper transcription failed with code: %d", result);
        return env->NewStringUTF("");
    }

    // Get transcription result
    std::string transcription;
    const int n_segments = whisper_full_n_segments(g_whisper_context);

    for (int i = 0; i < n_segments; ++i) {
        const char* text = whisper_full_get_segment_text(g_whisper_context, i);
        transcription += text;
    }

    LOGI("Transcription completed: %s", transcription.c_str());
    return env->NewStringUTF(transcription.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_medicaid_data_WhisperNative_cleanup(JNIEnv *env, jobject thiz) {
    if (g_whisper_context != nullptr) {
        whisper_free(g_whisper_context);
        g_whisper_context = nullptr;
        LOGI("Whisper context cleaned up");
    }
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_medicaid_data_WhisperNative_isInitialized(JNIEnv *env, jobject thiz) {
    return g_whisper_context != nullptr ? JNI_TRUE : JNI_FALSE;
}
