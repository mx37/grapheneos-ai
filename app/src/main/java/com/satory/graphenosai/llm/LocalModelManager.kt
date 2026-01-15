package com.satory.graphenosai.llm

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Manages downloading and storage of local AI models (GGUF format)
 * Models are optimized for ARM64 Pixel devices running GrapheneOS
 */
class LocalModelManager(private val context: Context) {

    companion object {
        private const val TAG = "LocalModelManager"
        
        // Directory for storing models
        private const val MODELS_DIR = "local_models"
        
        // Buffer size for downloads (256KB for better performance)
        private const val DOWNLOAD_BUFFER_SIZE = 256 * 1024
        
        /**
         * Available models optimized for ARM64/Pixel devices
         * These are quantized models that balance quality and performance
         * 
         * Selection criteria:
         * - Q4_K_M quantization: Best quality/size ratio for mobile
         * - 1B-3B parameters: Optimal for Pixel 6/7/8 with 8-12GB RAM
         * - Instruction-tuned: Better at following prompts
         * - ChatML format: Standard prompt format support
         */
        val AVAILABLE_MODELS = listOf(
            // Qwen2.5 - Excellent multilingual support, very efficient
            LocalModelInfo(
                id = "qwen2.5-1.5b-instruct",
                name = "Qwen 2.5 1.5B",
                description = "Fast, multilingual, great for general tasks",
                sizeBytes = 1_100_000_000L, // ~1.1GB
                downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf",
                filename = "qwen2.5-1.5b-instruct-q4_k_m.gguf",
                contextSize = 4096,
                recommended = true
            ),
            
            // SmolLM2 - Ultra-compact, great for low memory devices
            LocalModelInfo(
                id = "smollm2-1.7b-instruct",
                name = "SmolLM2 1.7B",
                description = "Compact, fast, good for basic tasks",
                sizeBytes = 1_000_000_000L, // ~1GB
                downloadUrl = "https://huggingface.co/HuggingFaceTB/SmolLM2-1.7B-Instruct-GGUF/resolve/main/smollm2-1.7b-instruct-q4_k_m.gguf",
                filename = "smollm2-1.7b-instruct-q4_k_m.gguf",
                contextSize = 2048,
                recommended = false
            ),
            
            // Phi-3 Mini - Microsoft's efficient model
            LocalModelInfo(
                id = "phi-3-mini-4k-instruct",
                name = "Phi-3 Mini",
                description = "Microsoft's efficient model, great reasoning",
                sizeBytes = 2_300_000_000L, // ~2.3GB
                downloadUrl = "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-gguf/resolve/main/Phi-3-mini-4k-instruct-q4.gguf",
                filename = "phi-3-mini-4k-instruct-q4.gguf",
                contextSize = 4096,
                recommended = true
            ),
            
            // Gemma 2 2B - Google's open model
            LocalModelInfo(
                id = "gemma-2-2b-it",
                name = "Gemma 2 2B",
                description = "Google's efficient model, good quality",
                sizeBytes = 1_600_000_000L, // ~1.6GB
                downloadUrl = "https://huggingface.co/google/gemma-2-2b-it-GGUF/resolve/main/gemma-2-2b-it-q4_k_m.gguf",
                filename = "gemma-2-2b-it-q4_k_m.gguf",
                contextSize = 4096,
                recommended = false
            ),
            
            // Llama 3.2 1B - Meta's smallest Llama
            LocalModelInfo(
                id = "llama-3.2-1b-instruct",
                name = "Llama 3.2 1B",
                description = "Meta's compact model, fast inference",
                sizeBytes = 750_000_000L, // ~750MB
                downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf",
                filename = "llama-3.2-1b-instruct-q4_k_m.gguf",
                contextSize = 4096,
                recommended = false
            ),
            
            // Llama 3.2 3B - Better quality, more memory
            LocalModelInfo(
                id = "llama-3.2-3b-instruct",
                name = "Llama 3.2 3B",
                description = "Meta's balanced model, best quality",
                sizeBytes = 2_000_000_000L, // ~2GB
                downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q4_K_M.gguf",
                filename = "llama-3.2-3b-instruct-q4_k_m.gguf",
                contextSize = 4096,
                recommended = true
            ),
            
            // TinyLlama - Ultra small for testing
            LocalModelInfo(
                id = "tinyllama-1.1b-chat",
                name = "TinyLlama 1.1B",
                description = "Very fast, lowest memory usage",
                sizeBytes = 670_000_000L, // ~670MB
                downloadUrl = "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf",
                filename = "tinyllama-1.1b-chat-v1.0-q4_k_m.gguf",
                contextSize = 2048,
                recommended = false
            )
        )
    }
    
    private val modelsDir: File by lazy {
        File(context.filesDir, MODELS_DIR).apply {
            if (!exists()) mkdirs()
        }
    }
    
    /**
     * Get the models directory path
     */
    fun getModelsDirectory(): File = modelsDir
    
    /**
     * Get all downloaded models
     */
    fun getDownloadedModels(): List<LocalModelInfo> {
        return AVAILABLE_MODELS.filter { isModelDownloaded(it.id) }
    }
    
    /**
     * Check if a specific model is downloaded
     */
    fun isModelDownloaded(modelId: String): Boolean {
        val model = AVAILABLE_MODELS.find { it.id == modelId } ?: return false
        val modelFile = File(modelsDir, model.filename)
        return modelFile.exists() && modelFile.length() > 0
    }
    
    /**
     * Get the file path for a model
     */
    fun getModelPath(modelId: String): String? {
        val model = AVAILABLE_MODELS.find { it.id == modelId } ?: return null
        val modelFile = File(modelsDir, model.filename)
        return if (modelFile.exists()) modelFile.absolutePath else null
    }
    
    /**
     * Get model info by ID
     */
    fun getModelInfo(modelId: String): LocalModelInfo? {
        return AVAILABLE_MODELS.find { it.id == modelId }
    }
    
    /**
     * Download a model with progress updates
     * Returns a Flow with download progress (0-100) or error
     */
    fun downloadModel(modelId: String): Flow<DownloadProgress> = flow {
        val model = AVAILABLE_MODELS.find { it.id == modelId }
        if (model == null) {
            emit(DownloadProgress.Error("Model not found: $modelId"))
            return@flow
        }
        
        val modelFile = File(modelsDir, model.filename)
        val tempFile = File(modelsDir, "${model.filename}.tmp")
        
        // Check if already downloaded
        if (modelFile.exists() && modelFile.length() == model.sizeBytes) {
            Log.i(TAG, "Model already downloaded: ${model.name}")
            emit(DownloadProgress.Completed(modelFile.absolutePath))
            return@flow
        }
        
        Log.i(TAG, "Starting download: ${model.name} from ${model.downloadUrl}")
        emit(DownloadProgress.Started(model.name))
        
        try {
            val url = URL(model.downloadUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 30000
            connection.readTimeout = 60000
            connection.setRequestProperty("User-Agent", "GrapheneOS-AI-Assistant/1.0")
            
            // Support resuming downloads
            var downloadedBytes = 0L
            if (tempFile.exists()) {
                downloadedBytes = tempFile.length()
                connection.setRequestProperty("Range", "bytes=$downloadedBytes-")
                Log.i(TAG, "Resuming download from byte $downloadedBytes")
            }
            
            connection.connect()
            
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_PARTIAL) {
                emit(DownloadProgress.Error("Download failed: HTTP $responseCode"))
                return@flow
            }
            
            val totalBytes = if (responseCode == HttpURLConnection.HTTP_PARTIAL) {
                model.sizeBytes
            } else {
                connection.contentLength.toLong().let { if (it > 0) it else model.sizeBytes }
            }
            
            val inputStream = connection.inputStream
            val outputStream = FileOutputStream(tempFile, downloadedBytes > 0)
            
            val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
            var bytesRead: Int
            var totalDownloaded = downloadedBytes
            var lastProgressUpdate = 0
            
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalDownloaded += bytesRead
                
                val progress = ((totalDownloaded.toFloat() / totalBytes) * 100).toInt()
                if (progress > lastProgressUpdate) {
                    lastProgressUpdate = progress
                    emit(DownloadProgress.Downloading(progress, totalDownloaded, totalBytes))
                }
            }
            
            outputStream.close()
            inputStream.close()
            connection.disconnect()
            
            // Rename temp file to final name
            if (tempFile.renameTo(modelFile)) {
                Log.i(TAG, "Download completed: ${model.name}")
                emit(DownloadProgress.Completed(modelFile.absolutePath))
            } else {
                emit(DownloadProgress.Error("Failed to finalize download"))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Download error", e)
            emit(DownloadProgress.Error("Download failed: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Delete a downloaded model
     */
    suspend fun deleteModel(modelId: String): Boolean = withContext(Dispatchers.IO) {
        val model = AVAILABLE_MODELS.find { it.id == modelId } ?: return@withContext false
        val modelFile = File(modelsDir, model.filename)
        val tempFile = File(modelsDir, "${model.filename}.tmp")
        
        var deleted = false
        if (modelFile.exists()) {
            deleted = modelFile.delete()
        }
        if (tempFile.exists()) {
            tempFile.delete()
        }
        
        Log.i(TAG, "Deleted model $modelId: $deleted")
        deleted
    }
    
    /**
     * Get total storage used by downloaded models
     */
    fun getTotalStorageUsed(): Long {
        return modelsDir.listFiles()
            ?.filter { it.isFile && it.extension == "gguf" }
            ?.sumOf { it.length() }
            ?: 0L
    }
    
    /**
     * Get available storage on device
     */
    fun getAvailableStorage(): Long {
        return modelsDir.freeSpace
    }
}

/**
 * Information about a downloadable local model
 */
data class LocalModelInfo(
    val id: String,
    val name: String,
    val description: String,
    val sizeBytes: Long,
    val downloadUrl: String,
    val filename: String,
    val contextSize: Int,
    val recommended: Boolean = false
) {
    /**
     * Format size as human-readable string
     */
    fun formattedSize(): String {
        return when {
            sizeBytes >= 1_000_000_000 -> String.format("%.1f GB", sizeBytes / 1_000_000_000.0)
            sizeBytes >= 1_000_000 -> String.format("%.1f MB", sizeBytes / 1_000_000.0)
            else -> String.format("%.1f KB", sizeBytes / 1_000.0)
        }
    }
}

/**
 * Download progress state
 */
sealed class DownloadProgress {
    data class Started(val modelName: String) : DownloadProgress()
    data class Downloading(val percent: Int, val downloadedBytes: Long, val totalBytes: Long) : DownloadProgress()
    data class Completed(val filePath: String) : DownloadProgress()
    data class Error(val message: String) : DownloadProgress()
}
