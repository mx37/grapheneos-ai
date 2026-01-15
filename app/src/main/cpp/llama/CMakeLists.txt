# CMakeLists.txt for native llama.cpp integration
# Optimized for ARM64 (Pixel devices on GrapheneOS)
cmake_minimum_required(VERSION 3.22.1)
project(llama_jni)

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Build type
if(NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Release)
endif()

# 16KB page size compatibility (required for Android 15+ / Pixel 9+)
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,-z,max-page-size=16384")

# llama.cpp prebuilt library path
set(LLAMA_PREBUILT_DIR ${CMAKE_SOURCE_DIR}/prebuilt/${ANDROID_ABI})

# Check for prebuilt library
if(EXISTS ${LLAMA_PREBUILT_DIR}/libllama.so)
    message(STATUS "Using prebuilt llama.cpp from ${LLAMA_PREBUILT_DIR}")
    
    # Import prebuilt libraries
    add_library(ggml-base SHARED IMPORTED)
    set_target_properties(ggml-base PROPERTIES 
        IMPORTED_LOCATION ${LLAMA_PREBUILT_DIR}/libggml-base.so
    )
    
    add_library(ggml-cpu SHARED IMPORTED)
    set_target_properties(ggml-cpu PROPERTIES 
        IMPORTED_LOCATION ${LLAMA_PREBUILT_DIR}/libggml-cpu.so
    )
    
    add_library(ggml SHARED IMPORTED)
    set_target_properties(ggml PROPERTIES 
        IMPORTED_LOCATION ${LLAMA_PREBUILT_DIR}/libggml.so
    )
    
    add_library(llama SHARED IMPORTED)
    set_target_properties(llama PROPERTIES 
        IMPORTED_LOCATION ${LLAMA_PREBUILT_DIR}/libllama.so
    )
    
    # Build JNI wrapper
    add_library(llama_jni SHARED
        ${CMAKE_SOURCE_DIR}/llama_jni.cpp
    )
    
    target_include_directories(llama_jni PRIVATE
        ${LLAMA_PREBUILT_DIR}/include
    )
    
    find_library(log-lib log)
    find_library(android-lib android)
    
    target_link_libraries(llama_jni
        llama
        ggml
        ggml-cpu
        ggml-base
        ${log-lib}
        ${android-lib}
    )
    
    target_compile_definitions(llama_jni PRIVATE
        LLAMA_CPP_AVAILABLE
    )
    
else()
    # No prebuilt library - use stub
    message(STATUS "Prebuilt llama.cpp not found. Building stub library.")
    
    # Build stub library
    add_library(llama_jni SHARED
        ${CMAKE_SOURCE_DIR}/llama_jni_stub.cpp
    )
    
    find_library(log-lib log)
    target_link_libraries(llama_jni ${log-lib})
endif()

# Output library naming
set_target_properties(llama_jni PROPERTIES
    OUTPUT_NAME "llama_jni"
)
