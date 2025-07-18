cmake_minimum_required(VERSION 3.22.1)

project("whisper-android")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Find required libraries
find_library(log-lib log)
find_library(android-lib android)

# Whisper.cpp source files
file(GLOB_RECURSE WHISPER_SOURCES
    "${CMAKE_CURRENT_SOURCE_DIR}/whisper.cpp/src/whisper.cpp"
    "${CMAKE_CURRENT_SOURCE_DIR}/whisper.cpp/ggml/src/ggml.c"
    "${CMAKE_CURRENT_SOURCE_DIR}/whisper.cpp/ggml/src/ggml-alloc.c"
    "${CMAKE_CURRENT_SOURCE_DIR}/whisper.cpp/ggml/src/ggml-backend.cpp"
    "${CMAKE_CURRENT_SOURCE_DIR}/whisper.cpp/ggml/src/ggml-backend-reg.cpp"
    "${CMAKE_CURRENT_SOURCE_DIR}/whisper.cpp/ggml/src/ggml-quants.c"
    "${CMAKE_CURRENT_SOURCE_DIR}/whisper.cpp/ggml/src/ggml-threading.cpp"
    "${CMAKE_CURRENT_SOURCE_DIR}/whisper.cpp/ggml/src/gguf.cpp"
    "${CMAKE_CURRENT_SOURCE_DIR}/whisper-coreml-stubs.cpp"
)

# Create whisper static library
add_library(whisper STATIC ${WHISPER_SOURCES})

target_include_directories(whisper PUBLIC
    ${CMAKE_CURRENT_SOURCE_DIR}/whisper.cpp/include
    ${CMAKE_CURRENT_SOURCE_DIR}/whisper.cpp/ggml/include
    ${CMAKE_CURRENT_SOURCE_DIR}/whisper.cpp/ggml/src
)

target_compile_definitions(whisper PRIVATE
    GGML_USE_ACCELERATE=0
    GGML_USE_OPENBLAS=0
    GGML_USE_CUBLAS=0
    GGML_USE_CLBLAST=0
    WHISPER_USE_COREML=0
    WHISPER_USE_METAL=0
    GGML_VERSION="unknown"
    GGML_COMMIT="unknown"
    WHISPER_VERSION="unknown"
)

# JNI wrapper library
add_library(whisper-jni SHARED
    ${CMAKE_CURRENT_SOURCE_DIR}/whisper-jni.cpp
)

target_include_directories(whisper-jni PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/whisper.cpp/include
    ${CMAKE_CURRENT_SOURCE_DIR}/whisper.cpp/ggml/include
    ${CMAKE_CURRENT_SOURCE_DIR}/whisper.cpp/ggml/src
)

target_link_libraries(whisper-jni
    whisper
    ${log-lib}
    ${android-lib}
)
