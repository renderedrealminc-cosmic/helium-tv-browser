package org.helium.tv.browser.engine

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.WebExtension
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class ExtensionItem(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val isEnabled: Boolean,
    val rawExtension: WebExtension?
)

class ExtensionManager(
    private val context: Context,
    private val geckoRuntime: GeckoRuntime
) {
    companion object {
        private const val TAG = "ExtensionManager"
        private const val EXTENSIONS_DIR = "web_extensions"

        // Popular recommended extensions for TV Browser
        val RECOMMENDED_EXTENSIONS = listOf(
            Triple("uBlock Origin", "Powerful, lightweight ad & tracker blocker", "https://addons.mozilla.org/firefox/downloads/latest/ublock-origin/addon-latest.xpi"),
            Triple("SponsorBlock", "Skip YouTube video sponsorships and intros automatically", "https://addons.mozilla.org/firefox/downloads/latest/sponsorblock/addon-latest.xpi"),
            Triple("Dark Reader", "Eye-friendly dark theme for every website on big screens", "https://addons.mozilla.org/firefox/downloads/latest/darkreader/addon-latest.xpi"),
            Triple("Violentmonkey / Tampermonkey", "Custom userscripts support for YouTube & web", "https://addons.mozilla.org/firefox/downloads/latest/violentmonkey/addon-latest.xpi")
        )
    }

    private val installedExtensions = mutableListOf<WebExtension>()

    fun getInstalledExtensions(): List<WebExtension> {
        return installedExtensions.toList()
    }

    suspend fun refreshInstalledExtensions(): List<ExtensionItem> = withContext(Dispatchers.Main) {
        val result = mutableListOf<ExtensionItem>()
        try {
            val list = geckoRuntime.webExtensionController.list().poll(5000)
            installedExtensions.clear()
            if (list != null) {
                installedExtensions.addAll(list)
                for (ext in list) {
                    val meta = ext.metaData
                    result.add(
                        ExtensionItem(
                            id = ext.id,
                            name = meta.name ?: ext.id,
                            version = meta.version ?: "1.0",
                            description = meta.description ?: "",
                            isEnabled = (ext.flags and WebExtension.Flags.ALLOW_CONTENT_MESSAGING.toLong()) != 0L,
                            rawExtension = ext
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list extensions", e)
        }
        result
    }

    suspend fun installExtensionFromFile(fileUri: String): Boolean = withContext(Dispatchers.Main) {
        try {
            Log.i(TAG, "Installing extension from URI: $fileUri")
            val extension = geckoRuntime.webExtensionController.install(fileUri).poll(10000)
            if (extension != null) {
                Log.i(TAG, "Successfully installed extension: ${extension.id}")
                installedExtensions.add(extension)
                return@withContext true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to install extension from $fileUri", e)
        }
        false
    }

    suspend fun downloadAndInstallXpi(downloadUrl: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val extDir = File(context.filesDir, EXTENSIONS_DIR)
            if (!extDir.exists()) extDir.mkdirs()

            val tempFile = File(extDir, "ext_${System.currentTimeMillis()}.xpi")
            val connection = URL(downloadUrl).openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.connect()

            if (connection.responseCode in 200..299) {
                connection.inputStream.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                Log.i(TAG, "Downloaded XPI to ${tempFile.absolutePath}, installing...")
                return@withContext installExtensionFromFile("file://${tempFile.absolutePath}")
            } else {
                Log.e(TAG, "Failed downloading extension: HTTP ${connection.responseCode}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading and installing extension from $downloadUrl", e)
        }
        false
    }

    suspend fun uninstallExtension(extension: WebExtension): Boolean = withContext(Dispatchers.Main) {
        try {
            geckoRuntime.webExtensionController.uninstall(extension).poll(5000)
            installedExtensions.remove(extension)
            Log.i(TAG, "Uninstalled extension: ${extension.id}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to uninstall extension: ${extension.id}", e)
            false
        }
    }

    suspend fun toggleExtension(extension: WebExtension, enable: Boolean): Boolean = withContext(Dispatchers.Main) {
        try {
            if (enable) {
                geckoRuntime.webExtensionController.enable(extension, 1).poll(5000)
            } else {
                geckoRuntime.webExtensionController.disable(extension, 1).poll(5000)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed toggling extension: ${extension.id}", e)
            false
        }
    }
}
