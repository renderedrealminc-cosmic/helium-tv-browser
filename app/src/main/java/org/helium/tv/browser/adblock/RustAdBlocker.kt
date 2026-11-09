package org.helium.tv.browser.adblock

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * High-performance JNI bridge to the Rust Adblock & Content Filtering Core.
 */
object RustAdBlocker {
    private const val TAG = "RustAdBlocker"
    private var isNativeLoaded = false

    init {
        try {
            System.loadLibrary("helium_adblock")
            nativeInit()
            isNativeLoaded = true
            Log.i(TAG, "Rust helium_adblock native library successfully loaded")
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "Native library 'helium_adblock' not found yet. Using fallback filter.", e)
            isNativeLoaded = false
        }
    }

    suspend fun shouldBlock(url: String, origin: String = "", resourceType: String = "other"): Boolean {
        if (!isNativeLoaded) {
            // Fallback lightweight blocking check
            return fallbackShouldBlock(url)
        }
        return withContext(Dispatchers.Default) {
            nativeShouldBlock(url, origin, resourceType)
        }
    }

    suspend fun addRule(rule: String): Boolean {
        if (!isNativeLoaded) return false
        return withContext(Dispatchers.Default) {
            nativeAddRule(rule)
        }
    }

    suspend fun loadFilterRules(rulesContent: String): Int {
        if (!isNativeLoaded) return 0
        return withContext(Dispatchers.IO) {
            nativeLoadFilterRules(rulesContent)
        }
    }

    suspend fun getCosmeticScriptForDomain(domain: String): String {
        if (!isNativeLoaded) return ""
        return withContext(Dispatchers.Default) {
            nativeGetCosmeticScript(domain)
        }
    }

    fun getBlockedCount(): Int {
        if (!isNativeLoaded) return 0
        return nativeGetBlockedCount()
    }

    fun resetBlockedCount() {
        if (isNativeLoaded) {
            nativeResetBlockedCount()
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
