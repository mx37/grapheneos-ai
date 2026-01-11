package com.satory.graphenosai.search

import android.util.Log
import com.satory.graphenosai.security.SecureKeyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * Exa AI Search API client.
 * Exa provides semantic search with AI-powered results.
 * Get API key at: https://dashboard.exa.ai/api-keys
 */
class ExaSearchClient(private val keyManager: SecureKeyManager) {

    companion object {
        private const val TAG = "ExaSearchClient"
        private const val EXA_API_URL = "https://api.exa.ai/search"
        private const val TIMEOUT_MS = 15000
        private const val MAX_RESULTS = 5
    }

    /**
     * Perform web search using Exa AI Search API.
     * Returns empty list if no API key is configured.
     */
    suspend fun search(query: String, maxResults: Int = MAX_RESULTS): List<SearchResult> =
        withContext(Dispatchers.IO) {
            val apiKey = keyManager.getExaApiKey()
            
            if (apiKey.isNullOrBlank()) {
                Log.w(TAG, "Exa API key not configured. Get key at dashboard.exa.ai/api-keys")
                return@withContext emptyList()
            }
            
            try {
                val results = searchExa(query, apiKey, maxResults)
                Log.i(TAG, "Exa search returned ${results.size} results")
                return@withContext results
            } catch (e: Exception) {
                Log.e(TAG, "Exa search failed", e)
                return@withContext emptyList()
            }
        }

    private fun searchExa(query: String, apiKey: String, maxResults: Int): List<SearchResult> {
        val url = URL(EXA_API_URL)
        
        val connection = url.openConnection() as HttpsURLConnection
        connection.apply {
            requestMethod = "POST"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("x-api-key", apiKey)
        }
        
        // Build request body
        val requestBody = JSONObject().apply {
            put("query", query)
            put("numResults", maxResults)
            put("type", "auto")  // auto, neural, fast, or deep
            put("text", true)    // Include text content
        }
        
        try {
            // Send request
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(requestBody.toString())
                writer.flush()
            }
            
            val responseCode = connection.responseCode
            if (responseCode != HttpsURLConnection.HTTP_OK) {
                val errorBody = try {
                    connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                } catch (e: Exception) { "Error: ${e.message}" }
                Log.e(TAG, "Exa API error $responseCode: $errorBody")
                return emptyList()
            }
            
            val responseBody = connection.inputStream.bufferedReader().readText()
            return parseExaResponse(responseBody, maxResults)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseExaResponse(responseBody: String, maxResults: Int): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        
        try {
            val json = JSONObject(responseBody)
            val resultsArray = json.optJSONArray("results") ?: return emptyList()
            
            for (i in 0 until minOf(resultsArray.length(), maxResults)) {
                val item = resultsArray.getJSONObject(i)
                
                // Exa returns text content or summary, use whichever is available
                val snippet = item.optString("text", "").take(500).ifBlank {
                    item.optString("summary", "").take(500)
                }
                
                results.add(
                    SearchResult(
                        title = item.optString("title", ""),
                        url = item.optString("url", ""),
                        snippet = snippet
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Exa response", e)
        }
        
        return results
    }
    
    /**
     * Check if Exa API is available.
     */
    fun isConfigured(): Boolean = keyManager.hasExaApiKey()
}
