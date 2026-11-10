package org.helium.tv.browser.adblock

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * High-performance JNI bridge to the Rust Adblock & Content Filtering Core.
 * Robust crash-safe wrapper for both Phone and Android TV.
 */
object RustAdBlocker {
    private const val TAG = "RustAdBlocker"
    private var isNativeLoaded = false
    private var fallbackBlockedCount = 0

    init {
        try {
            System.loadLibrary("helium_adblock")
            nativeInit()
            isNativeLoaded = true
            Log.i(TAG, "Rust helium_adblock native library successfully loaded")
        } catch (t: Throwable) {
            Log.w(TAG, "Native library 'helium_adblock' not available. Using fallback filter.", t)
            isNativeLoaded = false
        }
    }

    suspend fun shouldBlock(url: String, origin: String = "", resourceType: String = "other"): Boolean {
        if (!isNativeLoaded) {
            val blocked = fallbackShouldBlock(url)
            if (blocked) fallbackBlockedCount++
            return blocked
        }
        return try {
            withContext(Dispatchers.Default) {
                nativeShouldBlock(url, origin, resourceType)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Error in nativeShouldBlock, falling back", t)
            fallbackShouldBlock(url)
        }
    }

    suspend fun addRule(rule: String): Boolean {
        if (!isNativeLoaded) return false
        return try {
            withContext(Dispatchers.Default) {
                nativeAddRule(rule)
            }
        } catch (t: Throwable) {
            false
        }
    }

    suspend fun loadFilterRules(rulesContent: String): Int {
        if (!isNativeLoaded) return 0
        return try {
            withContext(Dispatchers.IO) {
                nativeLoadFilterRules(rulesContent)
            }
        } catch (t: Throwable) {
            0
        }
    }

    suspend fun getCosmeticScriptForDomain(domain: String): String {
        if (!isNativeLoaded) return ""
        return try {
            withContext(Dispatchers.Default) {
                nativeGetCosmeticScript(domain)
            }
        } catch (t: Throwable) {
            ""
        }
    }

    fun getBlockedCount(): Int {
        if (!isNativeLoaded) return fallbackBlockedCount
        return try {
            nativeGetBlockedCount()
        } catch (t: Throwable) {
            fallbackBlockedCount
        }
    }

    fun resetBlockedCount() {
        fallbackBlockedCount = 0
        if (isNativeLoaded) {
            try {
                nativeResetBlockedCount()
            } catch (t: Throwable) {
                Log.w(TAG, "Error in nativeResetBlockedCount", t)
            }
        }
    }

    private fun fallbackShouldBlock(url: String): Boolean {
        val lower = url.lowercase()
        val defaultBlocked = listOf(
            "doubleclick.net",
            "googleadservices.com",
            "googlesyndication.com",
            "adnxs.com",
            "taboola.com",
            "outbrain.com",
            "popcash.net",
            "popads.net",
            "/adserver/",
            "/ads/"
        )
        return defaultBlocked.any { lower.contains(it) }
    }

    // JNI Native Declarations
    private external fun nativeInit(): Boolean
    private external fun nativeShouldBlock(url: String, origin: String, resourceType: String): Boolean
    private external fun nativeAddRule(rule: String): Boolean
    private external fun nativeLoadFilterRules(rulesText: String): Int
    private external fun nativeGetCosmeticCss(domain: String): String
    private external fun nativeGetCosmeticScript(domain: String): String
    private external fun nativeGetBlockedCount(): Int
    private external fun nativeResetBlockedCount()
}
