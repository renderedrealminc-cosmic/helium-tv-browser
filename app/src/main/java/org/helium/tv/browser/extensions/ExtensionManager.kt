package org.helium.tv.browser.extensions

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

data class ContentScriptRule(
    val matches: List<String>,
    val jsFiles: List<String>
)

data class HeliumExtension(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val iconPath: String? = null,
    var enabled: Boolean = true,
    val contentScripts: List<ContentScriptRule> = emptyList()
)

class ExtensionManager(private val context: Context) {

    companion object {
        private const val TAG = "ExtensionManager"
        private const val PREFS_NAME = "helium_extensions_prefs"
        private const val KEY_INSTALLED = "installed_extensions_json"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Downloads and installs a Chrome extension using its 32-character ID from the Chrome Web Store.
     */
    suspend fun installExtension(extensionId: String, preferredName: String? = null): Result<HeliumExtension> = withContext(Dispatchers.IO) {
        try {
            val downloadUrl = "https://clients2.google.com/service/update2/crx?response=redirect&prodversion=110.0&acceptformat=crx2,crx3&x=id%3D$extensionId%26installsource%3Dondemand%26uc"
            val conn = URL(downloadUrl).openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            conn.connectTimeout = 15000
            conn.readTimeout = 30000
            conn.instanceFollowRedirects = true

            val responseCode = conn.responseCode
            if (responseCode !in 200..299) {
                return@withContext Result.failure(Exception("HTTP error $responseCode downloading extension"))
            }

            val crxBytes = conn.inputStream.use { it.readBytes() }
            if (crxBytes.isEmpty()) {
                return@withContext Result.failure(Exception("Empty response from extension server"))
            }

            // Find PK\x03\x04 zip header
            val zipOffset = findZipHeader(crxBytes)
            if (zipOffset == -1) {
                return@withContext Result.failure(Exception("Valid zip archive header not found in CRX"))
            }

            // Extract to context.filesDir/extensions/{extensionId}
            val extDir = File(context.filesDir, "extensions/$extensionId")
            if (extDir.exists()) {
                extDir.deleteRecursively()
            }
            extDir.mkdirs()

            val zipStream = ZipInputStream(ByteArrayInputStream(crxBytes, zipOffset, crxBytes.size - zipOffset))
            var entry: ZipEntry? = zipStream.nextEntry
            while (entry != null) {
                val file = File(extDir, entry.name)
                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile?.mkdirs()
                    FileOutputStream(file).use { out ->
                        zipStream.copyTo(out)
                    }
                }
                zipStream.closeEntry()
                entry = zipStream.nextEntry
            }

            // Parse manifest.json
            val manifestFile = File(extDir, "manifest.json")
            if (!manifestFile.exists()) {
                return@withContext Result.failure(Exception("Extension does not contain manifest.json"))
            }

            val manifestJson = JSONObject(manifestFile.readText())
            var extName = manifestJson.optString("name", preferredName ?: extensionId)
            val extVersion = manifestJson.optString("version", "1.0.0")
            var extDesc = manifestJson.optString("description", "Helium Browser Extension")

            // Resolve i18n messages if needed (__MSG_...)
            if (extName.startsWith("__MSG_")) {
                extName = resolveI18n(extDir, extName) ?: preferredName ?: extensionId
            }
            if (extDesc.startsWith("__MSG_")) {
                extDesc = resolveI18n(extDir, extDesc) ?: "Helium Browser Extension"
            }

            // Find icon
            var iconPath: String? = null
            val iconsObj = manifestJson.optJSONObject("icons")
            if (iconsObj != null) {
                val iconKey = iconsObj.optString("128", iconsObj.optString("48", iconsObj.optString("16", "")))
                if (iconKey.isNotEmpty() && File(extDir, iconKey).exists()) {
                    iconPath = File(extDir, iconKey).absolutePath
                }
            }

            // Parse content scripts
            val contentScriptsList = mutableListOf<ContentScriptRule>()
            val scriptsArr = manifestJson.optJSONArray("content_scripts")
            if (scriptsArr != null) {
                for (i in 0 until scriptsArr.length()) {
                    val scriptObj = scriptsArr.getJSONObject(i)
                    val matches = mutableListOf<String>()
                    val matchesArr = scriptObj.optJSONArray("matches")
                    if (matchesArr != null) {
                        for (j in 0 until matchesArr.length()) {
                            matches.add(matchesArr.getString(j))
                        }
                    }

                    val jsFiles = mutableListOf<String>()
                    val jsArr = scriptObj.optJSONArray("js")
                    if (jsArr != null) {
                        for (k in 0 until jsArr.length()) {
                            jsFiles.add(jsArr.getString(k))
                        }
                    }

                    if (matches.isNotEmpty() && jsFiles.isNotEmpty()) {
                        contentScriptsList.add(ContentScriptRule(matches, jsFiles))
                    }
                }
            }

            val installed = HeliumExtension(
                id = extensionId,
                name = extName,
                version = extVersion,
                description = extDesc,
                iconPath = iconPath,
                enabled = true,
                contentScripts = contentScriptsList
            )

            saveExtension(installed)
            Result.success(installed)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to install extension $extensionId", t)
            Result.failure(t)
        }
    }

    private fun findZipHeader(bytes: ByteArray): Int {
        val magic = byteArrayOf(0x50, 0x4B, 0x03, 0x04)
        for (i in 0 until bytes.size - 4) {
            if (bytes[i] == magic[0] && bytes[i + 1] == magic[1] && bytes[i + 2] == magic[2] && bytes[i + 3] == magic[3]) {
                return i
            }
        }
        return -1
    }

    private fun resolveI18n(extDir: File, msgKey: String): String? {
        val keyName = msgKey.removePrefix("__MSG_").removeSuffix("__")
        val messagesFile = File(extDir, "_locales/en/messages.json")
        if (messagesFile.exists()) {
            try {
                val json = JSONObject(messagesFile.readText())
                val item = json.optJSONObject(keyName)
                return item?.optString("message")
            } catch (e: Exception) {
                // ignore
            }
        }
        return null
    }

    fun getInstalledExtensions(): List<HeliumExtension> {
        val jsonStr = prefs.getString(KEY_INSTALLED, "[]") ?: "[]"
        val list = mutableListOf<HeliumExtension>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val scripts = mutableListOf<ContentScriptRule>()
                val scriptsArr = obj.optJSONArray("contentScripts")
                if (scriptsArr != null) {
                    for (j in 0 until scriptsArr.length()) {
                        val sObj = scriptsArr.getJSONObject(j)
                        val mList = mutableListOf<String>()
                        val mArr = sObj.optJSONArray("matches")
                        if (mArr != null) {
                            for (m in 0 until mArr.length()) mList.add(mArr.getString(m))
                        }
                        val jList = mutableListOf<String>()
                        val jArr = sObj.optJSONArray("jsFiles")
                        if (jArr != null) {
                            for (k in 0 until jArr.length()) jList.add(jArr.getString(k))
                        }
                        scripts.add(ContentScriptRule(mList, jList))
                    }
                }
                list.add(
                    HeliumExtension(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        version = obj.getString("version"),
                        description = obj.optString("description", ""),
                        iconPath = obj.optString("iconPath", null),
                        enabled = obj.optBoolean("enabled", true),
                        contentScripts = scripts
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing installed extensions", e)
        }
        return list
    }

    fun saveExtension(extension: HeliumExtension) {
        val current = getInstalledExtensions().filter { it.id != extension.id }.toMutableList()
        current.add(extension)
        persistList(current)
    }

    fun setExtensionEnabled(id: String, enabled: Boolean) {
        val list = getInstalledExtensions().map {
            if (it.id == id) it.copy(enabled = enabled) else it
        }
        persistList(list)
    }

    fun uninstallExtension(id: String) {
        val list = getInstalledExtensions().filter { it.id != id }
        persistList(list)
        try {
            val dir = File(context.filesDir, "extensions/$id")
            if (dir.exists()) dir.deleteRecursively()
        } catch (t: Throwable) {
            Log.w(TAG, "Could not delete extension dir $id", t)
        }
    }

    private fun persistList(list: List<HeliumExtension>) {
        val arr = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("name", item.name)
            obj.put("version", item.version)
            obj.put("description", item.description)
            obj.put("iconPath", item.iconPath ?: "")
            obj.put("enabled", item.enabled)

            val sArr = JSONArray()
            for (rule in item.contentScripts) {
                val rObj = JSONObject()
                val mArr = JSONArray()
                rule.matches.forEach { mArr.put(it) }
                rObj.put("matches", mArr)
                val jArr = JSONArray()
                rule.jsFiles.forEach { jArr.put(it) }
                rObj.put("jsFiles", jArr)
                sArr.put(rObj)
            }
            obj.put("contentScripts", sArr)
            arr.put(obj)
        }
        prefs.edit().putString(KEY_INSTALLED, arr.toString()).apply()
    }

    /**
     * Reads all JavaScript content script code for any enabled extension matching the page URL.
     */
    fun getScriptsToInject(pageUrl: String): List<String> {
        val scripts = mutableListOf<String>()
        val installed = getInstalledExtensions().filter { it.enabled }
        for (ext in installed) {
            val extDir = File(context.filesDir, "extensions/${ext.id}")
            for (rule in ext.contentScripts) {
                if (matchesUrl(rule.matches, pageUrl)) {
                    for (jsRelPath in rule.jsFiles) {
                        val jsFile = File(extDir, jsRelPath)
                        if (jsFile.exists()) {
                            try {
                                scripts.add(jsFile.readText())
                            } catch (e: Exception) {
                                Log.w(TAG, "Error reading content script $jsRelPath", e)
                            }
                        }
                    }
                }
            }
        }
        return scripts
    }

    private fun matchesUrl(patterns: List<String>, url: String): Boolean {
        for (p in patterns) {
            if (p == "<all_urls>") return url.startsWith("http://") || url.startsWith("https://")
            val regexStr = p
                .replace(".", "\\.")
                .replace("*", ".*")
            try {
                if (Pattern.compile(regexStr).matcher(url).find()) return true
            } catch (e: Exception) {
                // ignore invalid pattern
            }
        }
        return false
    }
}
