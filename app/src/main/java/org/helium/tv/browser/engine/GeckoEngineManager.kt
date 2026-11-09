package org.helium.tv.browser.engine

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import org.helium.tv.browser.utils.UserAgentMode
import org.helium.tv.browser.utils.UserAgentUtils
import org.mozilla.geckoview.ContentBlockingController
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings

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
                .contentBlocking(
                    GeckoRuntimeSettings.CONTENT_BLOCKING_STRICT
                )
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
            .userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_MOBILE)
            .userAgentOverride(UserAgentUtils.getUserAgent(currentMode))
            .suspendMediaWhenInactive(true)
            .viewportMode(GeckoSessionSettings.VIEWPORT_MODE_DESKTOP)
            .build()

        val session = GeckoSession(sessionSettings)
        
        // Strict content & tracker blocking
        val cbSettings = session.settings.contentBlocking
        cbSettings.setAntiTracking(ContentBlockingController.AntiTracking.DEFAULT)
        cbSettings.setEnhancedTrackingProtectionLevel(ContentBlockingController.ContentBlockingLevel.STRICT)

        return session
    }

    fun setUserAgentMode(session: GeckoSession, mode: UserAgentMode) {
        currentMode = mode
        val ua = UserAgentUtils.getUserAgent(mode)
        session.settings.userAgentOverride = ua
        Log.i(TAG, "Switched UserAgent to: $mode -> $ua")
    }

    fun getRuntimeExtensions(): ExtensionManager {
        return extensionManager
    }
}
