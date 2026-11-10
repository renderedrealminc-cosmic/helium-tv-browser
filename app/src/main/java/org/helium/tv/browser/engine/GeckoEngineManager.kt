package org.helium.tv.browser.engine

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import org.helium.tv.browser.utils.UserAgentMode
import org.helium.tv.browser.utils.UserAgentUtils
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.StorageController

class GeckoEngineManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    companion object {
        private const val TAG = "GeckoEngineManager"
        @Volatile
        private var runtimeInstance: GeckoRuntime? = null

        fun getRuntime(context: Context): GeckoRuntime {
            return runtimeInstance ?: synchronized(this) {
                runtimeInstance ?: buildRuntime(context).also { runtimeInstance = it }
            }
        }

        private fun buildRuntime(context: Context): GeckoRuntime {
            val settings = GeckoRuntimeSettings.Builder()
                .useMaxScreenDepth(true)
                .javaScriptEnabled(true)
                .consoleOutput(true)
                .build()

            return GeckoRuntime.create(context.applicationContext, settings)
        }
    }

    val runtime: GeckoRuntime = getRuntime(context)
    val extensionManager: ExtensionManager = ExtensionManager(context, runtime)
    private var currentMode: UserAgentMode = UserAgentMode.TV

    fun createSession(): GeckoSession {
        val sessionSettings = GeckoSessionSettings.Builder()
            .usePrivateMode(false)
            .userAgentOverride(UserAgentUtils.getUserAgent(currentMode))
            .viewportMode(GeckoSessionSettings.VIEWPORT_MODE_DESKTOP)
            .build()

        return GeckoSession(sessionSettings)
    }

    fun setUserAgentMode(session: GeckoSession, mode: UserAgentMode) {
        currentMode = mode
        val ua = UserAgentUtils.getUserAgent(mode)
        session.settings.userAgentOverride = ua
        Log.i(TAG, "Switched UserAgent to: $mode -> $ua")
    }

    fun clearBrowsingData() {
        try {
            runtime.storageController.clearData(StorageController.ClearFlags.ALL)
        } catch (e: Exception) {
            Log.w(TAG, "Failed clearing browsing data", e)
        }
    }

    fun getRuntimeExtensions(): ExtensionManager {
        return extensionManager
    }
}
