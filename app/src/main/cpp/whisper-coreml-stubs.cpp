// Stub implementations for CoreML functions on Android
// These functions are not available on Android, so we provide empty implementations

extern "C" {
    // CoreML stub functions for Android
    void* whisper_coreml_init(void* /* ctx */) {
        return nullptr;
    }

    void whisper_coreml_free(void* /* ctx */) {
        // No-op
    }

    int whisper_coreml_encode(void* /* ctx */, float* /* mel */, int /* n_mel */, int /* n_ctx */, float* /* out */) {
        return 1; // Return failure since CoreML is not available
    }
}
