/**
 * JNI bridge for llama.cpp
 * Provides native inference for local LLM models on Android
 * Optimized for ARM64 (Pixel devices with GrapheneOS)
 */

#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>
#include <thread>
#include <atomic>
#include <mutex>

#ifdef LLAMA_CPP_AVAILABLE
#include "llama.h"
#endif

#define TAG "LlamaJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)

/**
 * Check if a byte sequence is valid UTF-8
 * Returns the number of valid bytes from the start
 */
static size_t getValidUtf8Length(const char* data, size_t len) {
    size_t valid_len = 0;
    size_t i = 0;
    
    while (i < len) {
        unsigned char c = (unsigned char)data[i];
        size_t char_len = 0;
        
        if (c < 0x80) {
            // ASCII (0xxxxxxx)
            char_len = 1;
        } else if ((c & 0xE0) == 0xC0) {
            // 2-byte UTF-8 (110xxxxx)
            char_len = 2;
        } else if ((c & 0xF0) == 0xE0) {
            // 3-byte UTF-8 (1110xxxx)
            char_len = 3;
        } else if ((c & 0xF8) == 0xF0) {
            // 4-byte UTF-8 (11110xxx)
            char_len = 4;
        } else if ((c & 0xC0) == 0x80) {
            // Continuation byte without start - skip it
            i++;
            continue;
        } else {
            // Invalid start byte - skip it
            i++;
            continue;
        }
        
        // Check if we have enough bytes for this character
        if (i + char_len > len) {
            // Incomplete multi-byte character at end
            break;
        }
        
        // Validate continuation bytes
        bool valid = true;
        for (size_t j = 1; j < char_len; j++) {
            if ((data[i + j] & 0xC0) != 0x80) {
                valid = false;
                break;
            }
        }
        
        if (valid) {
            valid_len = i + char_len;
            i += char_len;
        } else {
            // Skip invalid sequence
            i++;
        }
    }
    
    return valid_len;
}

/**
 * Create a valid UTF-8 string, handling incomplete sequences
 * incomplete_buffer: stores incomplete UTF-8 bytes for next call
 */
static std::string makeValidUtf8(const std::string& input, std::string& incomplete_buffer) {
    // Prepend any incomplete bytes from last call
    std::string combined = incomplete_buffer + input;
    incomplete_buffer.clear();
    
    if (combined.empty()) {
        return "";
    }
    
    size_t valid_len = getValidUtf8Length(combined.data(), combined.size());
    
    // Store incomplete bytes for next call
    if (valid_len < combined.size()) {
        incomplete_buffer = combined.substr(valid_len);
    }
    
    return combined.substr(0, valid_len);
}

// Global state
#ifdef LLAMA_CPP_AVAILABLE
static llama_model* g_model = nullptr;
static llama_context* g_ctx = nullptr;
#else
static void* g_model = nullptr;
static void* g_ctx = nullptr;
#endif
static std::atomic<bool> g_is_generating{false};
static std::atomic<bool> g_should_stop{false};
static std::mutex g_mutex;
static JavaVM* g_jvm = nullptr;

// Callback for streaming tokens
static jobject g_callback_obj = nullptr;
static jmethodID g_callback_method = nullptr;

extern "C" {

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    g_jvm = vm;
    LOGI("LlamaJNI loaded");
    return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* vm, void* reserved) {
    LOGI("LlamaJNI unloaded");
}

#ifdef LLAMA_CPP_AVAILABLE

/**
 * Initialize llama.cpp backend
 */
JNIEXPORT jboolean JNICALL
Java_com_satory_graphenosai_llm_LlamaCppBridge_nativeInit(
    JNIEnv* env,
    jclass clazz
) {
    std::lock_guard<std::mutex> lock(g_mutex);
    
    llama_backend_init();
    LOGI("llama.cpp backend initialized");
    return JNI_TRUE;
}

/**
 * Load a GGUF model from file path
 */
JNIEXPORT jboolean JNICALL
Java_com_satory_graphenosai_llm_LlamaCppBridge_nativeLoadModel(
    JNIEnv* env,
    jclass clazz,
    jstring modelPath,
    jint nCtx,
    jint nThreads,
    jboolean useGpu
) {
    std::lock_guard<std::mutex> lock(g_mutex);
    
    // Unload existing model if any
    if (g_ctx) {
        llama_free(g_ctx);
        g_ctx = nullptr;
    }
    if (g_model) {
        llama_model_free(g_model);
        g_model = nullptr;
    }
    
    const char* path = env->GetStringUTFChars(modelPath, nullptr);
    LOGI("Loading model from: %s", path);
    
    // Model parameters
    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = useGpu ? 99 : 0;  // GPU acceleration if available
    
    // Load model
    g_model = llama_model_load_from_file(path, model_params);
    env->ReleaseStringUTFChars(modelPath, path);
    
    if (!g_model) {
        LOGE("Failed to load model");
        return JNI_FALSE;
    }
    
    // Context parameters - optimized for mobile
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = nCtx > 0 ? nCtx : 2048;  // Context size
    ctx_params.n_threads = nThreads > 0 ? nThreads : std::max(1, (int)std::thread::hardware_concurrency() - 1);
    ctx_params.n_threads_batch = ctx_params.n_threads;
    ctx_params.flash_attn_type = LLAMA_FLASH_ATTN_TYPE_AUTO;  // Enable flash attention for efficiency
    
    LOGI("Creating context with n_ctx=%d, n_threads=%d", ctx_params.n_ctx, ctx_params.n_threads);
    
    // Create context
    g_ctx = llama_init_from_model(g_model, ctx_params);
    if (!g_ctx) {
        LOGE("Failed to create context");
        llama_model_free(g_model);
        g_model = nullptr;
        return JNI_FALSE;
    }
    
    LOGI("Model loaded successfully");
    return JNI_TRUE;
}

/**
 * Unload the current model and free memory
 */
JNIEXPORT void JNICALL
Java_com_satory_graphenosai_llm_LlamaCppBridge_nativeUnloadModel(
    JNIEnv* env,
    jclass clazz
) {
    std::lock_guard<std::mutex> lock(g_mutex);
    
    g_should_stop = true;
    
    // Wait for generation to stop
    while (g_is_generating) {
        std::this_thread::sleep_for(std::chrono::milliseconds(10));
    }
    
    if (g_ctx) {
        llama_free(g_ctx);
        g_ctx = nullptr;
    }
    if (g_model) {
        llama_model_free(g_model);
        g_model = nullptr;
    }
    
    LOGI("Model unloaded");
}

/**
 * Check if a model is currently loaded
 */
JNIEXPORT jboolean JNICALL
Java_com_satory_graphenosai_llm_LlamaCppBridge_nativeIsModelLoaded(
    JNIEnv* env,
    jclass clazz
) {
    return g_model != nullptr && g_ctx != nullptr;
}

/**
 * Generate text with streaming callback
 */
JNIEXPORT jstring JNICALL
Java_com_satory_graphenosai_llm_LlamaCppBridge_nativeGenerate(
    JNIEnv* env,
    jclass clazz,
    jstring prompt,
    jint maxTokens,
    jfloat temperature,
    jfloat topP,
    jobject callback
) {
    if (!g_model || !g_ctx) {
        return env->NewStringUTF("[Error: No model loaded]");
    }
    
    g_is_generating = true;
    g_should_stop = false;
    
    const char* promptStr = env->GetStringUTFChars(prompt, nullptr);
    std::string input(promptStr);
    env->ReleaseStringUTFChars(prompt, promptStr);
    
    LOGI("Generating response for prompt length: %zu", input.length());
    
    // Get vocab from model
    const llama_vocab* vocab = llama_model_get_vocab(g_model);
    
    // Tokenize input
    std::vector<llama_token> tokens;
    const int n_ctx = llama_n_ctx(g_ctx);
    tokens.resize(n_ctx);
    
    const int n_tokens = llama_tokenize(
        vocab,
        input.c_str(),
        input.length(),
        tokens.data(),
        tokens.size(),
        true,  // add BOS
        true   // parse special tokens (important for ChatML!)
    );
    
    if (n_tokens < 0) {
        g_is_generating = false;
        return env->NewStringUTF("[Error: Tokenization failed]");
    }
    tokens.resize(n_tokens);
    
    LOGD("Tokenized to %d tokens", n_tokens);
    
    // Clear memory (KV cache)
    llama_memory_t mem = llama_get_memory(g_ctx);
    if (mem) {
        llama_memory_clear(mem, true);
    }
    
    // Evaluate prompt
    llama_batch batch = llama_batch_get_one(tokens.data(), tokens.size());
    if (llama_decode(g_ctx, batch) != 0) {
        g_is_generating = false;
        return env->NewStringUTF("[Error: Prompt evaluation failed]");
    }
    
    // Sampler setup
    llama_sampler* sampler = llama_sampler_chain_init(llama_sampler_chain_default_params());
    llama_sampler_chain_add(sampler, llama_sampler_init_temp(temperature));
    llama_sampler_chain_add(sampler, llama_sampler_init_top_p(topP, 1));
    llama_sampler_chain_add(sampler, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));
    
    // Generate tokens
    std::string result;
    const int max_gen = maxTokens > 0 ? maxTokens : 512;
    
    // Setup callback
    jclass callbackClass = nullptr;
    jmethodID onTokenMethod = nullptr;
    if (callback != nullptr) {
        callbackClass = env->GetObjectClass(callback);
        onTokenMethod = env->GetMethodID(callbackClass, "onToken", "(Ljava/lang/String;)V");
    }
    
    // ChatML stop sequences detection
    std::string recent_output;  // Buffer for detecting multi-token stop sequences
    std::string utf8_buffer;    // Buffer for incomplete UTF-8 sequences
    std::string pending_output; // Buffer for tokens waiting to be sent to callback
    
    for (int i = 0; i < max_gen && !g_should_stop; i++) {
        // Sample next token
        llama_token new_token = llama_sampler_sample(sampler, g_ctx, -1);
        
        // Check for end of sequence (EOS/EOT tokens)
        if (llama_vocab_is_eog(vocab, new_token)) {
            LOGD("EOS token reached at %d", i);
            break;
        }
        
        // Convert token to string
        char buf[256];
        int n = llama_token_to_piece(vocab, new_token, buf, sizeof(buf), 0, true);
        if (n < 0) {
            LOGE("Token to piece failed");
            break;
        }
        
        std::string piece(buf, n);
        
        // Update recent output buffer for stop sequence detection
        recent_output += piece;
        if (recent_output.length() > 50) {
            recent_output = recent_output.substr(recent_output.length() - 50);
        }
        
        // Check for ChatML stop sequences
        bool should_stop = false;
        
        // Check if we've generated <|im_end|> or <|im_start|>
        if (recent_output.find("<|im_end|>") != std::string::npos) {
            LOGD("ChatML end token detected at %d", i);
            // Remove the stop sequence from result if partially added
            size_t pos = result.rfind("<|im_end|>");
            if (pos != std::string::npos) {
                result = result.substr(0, pos);
            }
            pos = result.rfind("<|im_end");
            if (pos != std::string::npos) {
                result = result.substr(0, pos);
            }
            should_stop = true;
        }
        
        if (recent_output.find("<|im_start|>") != std::string::npos) {
            LOGD("ChatML start token detected at %d - stopping", i);
            // Remove any partial start token
            size_t pos = result.rfind("<|im_start|>");
            if (pos != std::string::npos) {
                result = result.substr(0, pos);
            }
            pos = result.rfind("<|im_start");
            if (pos != std::string::npos) {
                result = result.substr(0, pos);
            }
            should_stop = true;
        }
        
        if (should_stop) {
            break;
        }
        
        result += piece;
        pending_output += piece;
        
        // Stream callback with valid UTF-8 only
        if (callback != nullptr && onTokenMethod != nullptr) {
            std::string valid_output = makeValidUtf8(pending_output, utf8_buffer);
            if (!valid_output.empty()) {
                pending_output.clear();
                jstring jpiece = env->NewStringUTF(valid_output.c_str());
                if (jpiece != nullptr) {
                    env->CallVoidMethod(callback, onTokenMethod, jpiece);
                    env->DeleteLocalRef(jpiece);
                }
            }
        }
        
        // Accept sampled token
        llama_sampler_accept(sampler, new_token);
        
        // Decode the new token
        llama_batch new_batch = llama_batch_get_one(&new_token, 1);
        if (llama_decode(g_ctx, new_batch) != 0) {
            LOGE("Decode failed at token %d", i);
            break;
        }
    }
    
    // Flush any remaining pending output to callback
    if (callback != nullptr && onTokenMethod != nullptr && !pending_output.empty()) {
        // Try to send remaining valid UTF-8
        std::string remaining_valid = makeValidUtf8(pending_output, utf8_buffer);
        if (!remaining_valid.empty()) {
            jstring jpiece = env->NewStringUTF(remaining_valid.c_str());
            if (jpiece != nullptr) {
                env->CallVoidMethod(callback, onTokenMethod, jpiece);
                env->DeleteLocalRef(jpiece);
            }
        }
    }
    
    // Final cleanup of any remaining ChatML tokens
    size_t pos;
    while ((pos = result.find("<|im_")) != std::string::npos) {
        result = result.substr(0, pos);
    }
    
    // Ensure result is valid UTF-8 before returning
    std::string dummy_buffer;
    std::string valid_result = makeValidUtf8(result, dummy_buffer);
    
    llama_sampler_free(sampler);
    g_is_generating = false;
    
    LOGI("Generated %zu characters", valid_result.length());
    return env->NewStringUTF(valid_result.c_str());
}

/**
 * Stop ongoing generation
 */
JNIEXPORT void JNICALL
Java_com_satory_graphenosai_llm_LlamaCppBridge_nativeStopGeneration(
    JNIEnv* env,
    jclass clazz
) {
    g_should_stop = true;
    LOGI("Generation stop requested");
}

/**
 * Get model info (name, parameters, etc.)
 */
JNIEXPORT jstring JNICALL
Java_com_satory_graphenosai_llm_LlamaCppBridge_nativeGetModelInfo(
    JNIEnv* env,
    jclass clazz
) {
    if (!g_model) {
        return env->NewStringUTF("{}");
    }
    
    char model_desc[256];
    llama_model_desc(g_model, model_desc, sizeof(model_desc));
    
    const llama_vocab* vocab = llama_model_get_vocab(g_model);
    
    // Build JSON info
    std::string info = "{";
    info += "\"description\":\"" + std::string(model_desc) + "\",";
    info += "\"n_params\":" + std::to_string(llama_model_n_params(g_model)) + ",";
    info += "\"n_ctx\":" + std::to_string(llama_n_ctx(g_ctx)) + ",";
    info += "\"n_vocab\":" + std::to_string(llama_vocab_n_tokens(vocab));
    info += "}";
    
    return env->NewStringUTF(info.c_str());
}

/**
 * Get estimated memory usage in bytes
 */
JNIEXPORT jlong JNICALL
Java_com_satory_graphenosai_llm_LlamaCppBridge_nativeGetMemoryUsage(
    JNIEnv* env,
    jclass clazz
) {
    if (!g_ctx) return 0;
    return (jlong)llama_state_get_size(g_ctx);
}

#else // LLAMA_CPP_AVAILABLE not defined - stub implementations

JNIEXPORT jboolean JNICALL
Java_com_satory_graphenosai_llm_LlamaCppBridge_nativeInit(JNIEnv* env, jclass clazz) {
    LOGI("llama.cpp not available - stub mode");
    return JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_satory_graphenosai_llm_LlamaCppBridge_nativeLoadModel(
    JNIEnv* env, jclass clazz, jstring modelPath, jint nCtx, jint nThreads, jboolean useGpu) {
    LOGE("llama.cpp not available");
    return JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_satory_graphenosai_llm_LlamaCppBridge_nativeUnloadModel(JNIEnv* env, jclass clazz) {
}

JNIEXPORT jboolean JNICALL
Java_com_satory_graphenosai_llm_LlamaCppBridge_nativeIsModelLoaded(JNIEnv* env, jclass clazz) {
    return JNI_FALSE;
}

JNIEXPORT jstring JNICALL
Java_com_satory_graphenosai_llm_LlamaCppBridge_nativeGenerate(
    JNIEnv* env, jclass clazz, jstring prompt, jint maxTokens, jfloat temperature, jfloat topP, jobject callback) {
    return env->NewStringUTF("[Error: llama.cpp not available. Please rebuild with llama.cpp sources.]");
}

JNIEXPORT void JNICALL
Java_com_satory_graphenosai_llm_LlamaCppBridge_nativeStopGeneration(JNIEnv* env, jclass clazz) {
}

JNIEXPORT jstring JNICALL
Java_com_satory_graphenosai_llm_LlamaCppBridge_nativeGetModelInfo(JNIEnv* env, jclass clazz) {
    return env->NewStringUTF("{\"error\":\"llama.cpp not available\"}");
}

JNIEXPORT jlong JNICALL
Java_com_satory_graphenosai_llm_LlamaCppBridge_nativeGetMemoryUsage(JNIEnv* env, jclass clazz) {
    return 0;
}

#endif // LLAMA_CPP_AVAILABLE

} // extern "C"
