package com.satory.graphenosai.llm

/**
 * JNI bridge for llama.cpp native library
 * Provides low-level access to the native inference engine
 */
object LlamaCppBridge {
    
    private var isLibraryLoaded = false
    
    init {
        try {
            // Load dependencies first in correct order
            System.loadLibrary("ggml-base")
            System.loadLibrary("ggml-cpu")
            System.loadLibrary("ggml")
            System.loadLibrary("llama")
            // Then load our JNI bridge
            System.loadLibrary("llama_jni")
            isLibraryLoaded = true
            android.util.Log.i("LlamaCppBridge", "Native library loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            // Log full exception (including stack trace) to help debugging loading issues
            android.util.Log.e("LlamaCppBridge", "Failed to load native library", e)
            isLibraryLoaded = false
        }
    }
    
    /**
     * Check if the native library is available
     */
    fun isAvailable(): Boolean = isLibraryLoaded
    
    /**
     * Initialize the llama.cpp backend
     */
    external fun nativeInit(): Boolean
    
    /**
     * Load a GGUF model from file
     * @param modelPath Full path to the .gguf model file
     * @param nCtx Context size (default 2048)
     * @param nThreads Number of threads (default auto)
     * @param useGpu Whether to use GPU acceleration if available
     */
    external fun nativeLoadModel(
        modelPath: String,
        nCtx: Int = 2048,
        nThreads: Int = 0,
        useGpu: Boolean = false
    ): Boolean
    
    /**
     * Unload the current model and free memory
     */
    external fun nativeUnloadModel()
    
    /**
     * Check if a model is currently loaded
     */
    external fun nativeIsModelLoaded(): Boolean
    
    /**
     * Generate text from a prompt
     * @param prompt The input prompt
     * @param maxTokens Maximum tokens to generate
     * @param temperature Sampling temperature (0.0 - 2.0)
     * @param topP Top-p sampling parameter
     * @param callback Callback for streaming tokens (optional)
     * @return Generated text
     */
    external fun nativeGenerate(
        prompt: String,
        maxTokens: Int = 512,
        temperature: Float = 0.7f,
        topP: Float = 0.9f,
        callback: TokenCallback? = null
    ): String
    
    /**
     * Stop ongoing generation
     */
    external fun nativeStopGeneration()
    
    /**
     * Get model information as JSON
     */
    external fun nativeGetModelInfo(): String
    
    /**
     * Get estimated memory usage in bytes
     */
    external fun nativeGetMemoryUsage(): Long
    
    /**
     * Callback interface for streaming tokens
     */
    interface TokenCallback {
        fun onToken(token: String)
    }
}
