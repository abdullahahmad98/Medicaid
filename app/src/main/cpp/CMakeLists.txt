cmake_minimum_required(VERSION 3.22.1)

project("whisper-android")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Find required libraries
find_library(log-lib log)
find_library(android-lib android)

# Whisper.cpp source files
file(GLOB_RECURSE WHISPER_SOURCES
    "${CMAKE_CURRENT_SOURCE_DIR}/whisper.cpp/whisper.cpp"
    "${CMAKE_CURRENT_SOURCE_DIR}/whisper.cpp/ggml.c"
    "${CMAKE_CURRENT_SOURCE_DIR}/whisper.cpp/ggml-alloc.c"
    "${CMAKE_CURRENT_SOURCE_DIR}/whisper.cpp/ggml-backend.c"
    "${CMAKE_CURRENT_SOURCE_DIR}/whisper.cpp/ggml-quants.c"
)

# Create whisper static library
add_library(whisper STATIC ${WHISPER_SOURCES})

target_include_directories(whisper PUBLIC
    ${CMAKE_CURRENT_SOURCE_DIR}/whisper.cpp
)

target_compile_definitions(whisper PRIVATE
    GGML_USE_ACCELERATE=0
    GGML_USE_OPENBLAS=0
    GGML_USE_CUBLAS=0
    GGML_USE_CLBLAST=0
    WHISPER_USE_COREML=0
    WHISPER_USE_METAL=0
)

# JNI wrapper library
add_library(whisper-jni SHARED
    ${CMAKE_CURRENT_SOURCE_DIR}/whisper-jni.cpp
)

target_include_directories(whisper-jni PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/whisper.cpp
)

target_link_libraries(whisper-jni
    whisper
    ${log-lib}
    ${android-lib}
)
