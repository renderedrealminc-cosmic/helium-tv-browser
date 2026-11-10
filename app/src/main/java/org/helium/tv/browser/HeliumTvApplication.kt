package org.helium.tv.browser

import android.app.Application
import android.util.Log
import android.webkit.WebView
import org.helium.tv.browser.adblock.RustAdBlocker
import org.helium.tv.browser.engine.TvNativeCore

class HeliumTvApplication : Application() {
    companion object {
        private const val TAG = "HeliumTvApp"
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Initializing Helium TV Browser...")
        // Pre-warm Chromium WebView and native Rust adblock engine
        try {
            RustAdBlocker.getBlockedCount()
            TvNativeCore.getCurrentFps()
            WebView.enableSlowWholeDocumentDraw()
        } catch (t: Throwable) {
            Log.w(TAG, "Non-fatal error during pre-warming", t)
        }
    }
}
