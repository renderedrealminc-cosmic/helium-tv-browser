package org.helium.tv.browser.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Gemini AI Powered TV Browser Assistant.
 * Summarizes web pages, answers TV user queries, and translates content into Hindi.
 */
object GeminiTvAssistant {
    private const val TAG = "GeminiTvAssistant"
    
    // User can customize their Gemini API key in settings or use prompt assistant
    var userApiKey: String = ""

    suspend fun queryGemini(prompt: String, pageContext: String = ""): String = withContext(Dispatchers.IO) {
        if (userApiKey.isBlank()) {
            return@withContext "💡 Gemini AI Assistant Ready!\n\n" +
                    "Aapne poocha: \"$prompt\"\n\n" +
                    "Ise live Google Gemini se connect karne ke liye settings me apni Free Gemini API Key daalein.\n\n" +
                    "Quick Preview Summary:\n" +
                    "• Current web page active aur ready hai.\n" +
                    "• Fast Bing search aur TV remote navigation active hai."
        }

        try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$userApiKey"
            val url = URL(endpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 15000
            conn.readTimeout = 25000
            conn.doOutput = true

            val fullPrompt = if (pageContext.isNotBlank()) {
                "You are an AI assistant on an Android TV web browser. Keep answers concise, high contrast, and easy to read on a 10-foot TV screen.\n\nPage context: $pageContext\n\nUser Question: $prompt"
            } else {
                "You are an AI assistant on an Android TV web browser. Keep answers concise, high contrast, and easy to read on a 10-foot TV screen.\n\nUser Question: $prompt"
            }

            val requestBody = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply { put("text", fullPrompt) })
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(requestBody.toString())
                writer.flush()
            }

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val candidates = json.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text", "No response received.")
                    }
                }
                return@withContext "Response parse nahi ho paya."
            } else {
                val error = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Log.e(TAG, "Gemini API error ($conn.responseCode): $error")
                return@withContext "Gemini API error: HTTP ${conn.responseCode}"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini query failed", e)
            return@withContext "Network error: ${e.localizedMessage}"
        }
    }
}
