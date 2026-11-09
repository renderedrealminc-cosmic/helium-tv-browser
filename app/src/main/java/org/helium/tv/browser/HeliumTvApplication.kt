package org.helium.tv.browser

import android.app.Application
import android.util.Log
import org.helium.tv.browser.adblock.RustAdBlocker
import org.helium.tv.browser.engine.GeckoEngineManager

class HeliumTvApplication : Application() {
    companion object {
        private const val TAG = "HeliumTvApp"
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Initializing Helium TV Browser...")
        // Pre-warm GeckoRuntime and native Rust adblock engine
        try {
            RustAdBlocker.getBlockedCount()
            GeckoEngineManager.getRuntime(this)
        } catch (e: Exception) {
            Log.e(TAG, "Error during pre-warming", e)
        }
    }
}
