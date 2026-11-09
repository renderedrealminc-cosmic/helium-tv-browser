package org.helium.tv.browser.adblock

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class FilterListManager(private val context: Context) {
    companion object {
        private const val TAG = "FilterListManager"
        private const val FILTERS_DIR = "adblock_filters"
        
        val DEFAULT_LISTS = listOf(
            "https://easylist.to/easylist/easylist.txt",
            "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/filters.txt",
            "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/badware.txt"
        )
    }

    suspend fun initializeFilters() {
        withContext(Dispatchers.IO) {
            val filterDir = File(context.filesDir, FILTERS_DIR)
            if (!filterDir.exists()) {
                filterDir.mkdirs()
            }

            // Load any previously cached filters
            val cachedFiles = filterDir.listFiles()
            if (cachedFiles != null && cachedFiles.isNotEmpty()) {
                for (file in cachedFiles) {
                    try {
                        val content = file.readText()
                        val count = RustAdBlocker.loadFilterRules(content)
                        Log.i(TAG, "Loaded $count rules from cached filter: ${file.name}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed reading cached filter: ${file.name}", e)
                    }
                }
            } else {
                // Load embedded base rules from assets if available
                loadDefaultAssetRules()
            }
        }
    }

    private suspend fun loadDefaultAssetRules() {
        withContext(Dispatchers.IO) {
            try {
                val baseRules = context.assets.open("default_filters.txt").bufferedReader().use { it.readText() }
                val count = RustAdBlocker.loadFilterRules(baseRules)
                Log.i(TAG, "Loaded $count rules from assets/default_filters.txt")
            } catch (e: Exception) {
                Log.d(TAG, "No default_filters.txt found in assets, skipping base asset list.")
            }
        }
    }

    suspend fun downloadAndUpdateFilterList(urlStr: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL(urlStr)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 20000
            connection.requestMethod = "GET"

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val content = connection.inputStream.bufferedReader().use { it.readText() }
                val count = RustAdBlocker.loadFilterRules(content)

                // Cache locally
                val fileName = "filter_" + urlStr.hashCode().toString() + ".txt"
                val file = File(File(context.filesDir, FILTERS_DIR), fileName)
                file.writeText(content)

                Log.i(TAG, "Successfully downloaded filter ($count rules) from: $urlStr")
                true
            } else {
                Log.w(TAG, "Failed downloading filter: HTTP ${connection.responseCode}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading filter list: $urlStr", e)
            false
        }
    }
}
