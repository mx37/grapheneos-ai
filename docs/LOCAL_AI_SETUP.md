# Local AI Setup Instructions

This app supports running AI models locally on your device using llama.cpp, providing complete offline functionality without any internet connection.

## Automatic Setup (Recommended)

1. Open the app Settings
2. Select "Local AI (Offline)" as your AI Provider
3. In the "Local AI Models" section, tap on a model to download it
4. Wait for the download to complete (~1-2GB depending on the model)
5. The model will be automatically loaded and ready to use

## Recommended Models for Pixel Devices

The following models are optimized for ARM64 and work well on Pixel devices:

| Model | Size | RAM Required | Best For |
|-------|------|--------------|----------|
| **Qwen 2.5 1.5B** ⭐ | 1.1 GB | 3-4 GB | General tasks, multilingual |
| **Llama 3.2 1B** | 750 MB | 2-3 GB | Quick responses, basic tasks |
| **Llama 3.2 3B** ⭐ | 2 GB | 5-6 GB | Best quality, more capable |
| **Phi-3 Mini** ⭐ | 2.3 GB | 5-6 GB | Reasoning, complex tasks |
| **SmolLM2 1.7B** | 1 GB | 3-4 GB | Fast, efficient |
| **TinyLlama 1.1B** | 670 MB | 2-3 GB | Lowest memory, testing |

⭐ = Recommended for most users

## Building llama.cpp for Android (For Developers)

The prebuilt llama.cpp libraries are already included in the project. If you need to rebuild them:

### Prerequisites
- Android NDK 27+
- CMake 3.22.1+
- Git

### Steps

1. Navigate to llama.cpp directory:
```bash
cd app/src/main/cpp/llama/llama.cpp
```

2. Build for Android:
```bash
export NDK=/path/to/android/sdk/ndk/27.0.12077973
export CMAKE=/path/to/android/sdk/cmake/3.22.1/bin/cmake

mkdir build-android && cd build-android

$CMAKE .. \
    -DCMAKE_TOOLCHAIN_FILE=$NDK/build/cmake/android.toolchain.cmake \
    -DANDROID_ABI=arm64-v8a \
    -DANDROID_PLATFORM=android-26 \
    -DANDROID_STL=c++_shared \
    -DCMAKE_BUILD_TYPE=Release \
    -DGGML_NATIVE=OFF \
    -DGGML_OPENMP=OFF \
    -DGGML_CPU_AARCH64=ON \
    -DLLAMA_BUILD_TESTS=OFF \
    -DLLAMA_BUILD_EXAMPLES=OFF \
    -DLLAMA_BUILD_SERVER=OFF \
    -DLLAMA_CURL=OFF \
    -DBUILD_SHARED_LIBS=ON \
    -DCMAKE_SHARED_LINKER_FLAGS="-Wl,-z,max-page-size=16384"

$CMAKE --build . --config Release -j4 -- llama
```

3. Copy libraries:
```bash
cp bin/libllama.so ../prebuilt/arm64-v8a/
cp bin/libggml*.so ../prebuilt/arm64-v8a/
```

4. Rebuild the app:
```bash
cd ../../../../..
./gradlew assembleDebug
```

## 16KB Page Size Support

The libraries are built with 16KB page size alignment for compatibility with Android 15+ and newer Pixel devices (Pixel 9+). This is achieved via the linker flag:

```cmake
-DCMAKE_SHARED_LINKER_FLAGS="-Wl,-z,max-page-size=16384"
```

## Troubleshooting

### "No local model loaded" error
- Make sure you've downloaded a model in Settings > Local AI Models
- Check that the model file exists in the app's data directory
- Ensure sufficient free storage space

### Slow inference
- Use a smaller model (Llama 3.2 1B or TinyLlama)
- Close other apps to free RAM
- Models run on CPU only; performance depends on device

### Model download fails
- Check internet connection
- Ensure sufficient free storage (2-3GB)
- Try a different, smaller model

## Technical Details

- Native inference via llama.cpp JNI bridge
- GGUF model format with Q4_K_M quantization
- ARM64-v8a only (optimized for Pixel devices)
- NEON SIMD optimizations enabled
- 16KB page size alignment for Android 15+ compatibility
- ChatML prompt format for instruction-tuned models
