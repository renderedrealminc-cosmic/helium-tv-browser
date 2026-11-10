package org.helium.tv.browser.engine

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.runBlocking
import org.helium.tv.browser.adblock.RustAdBlocker
import java.io.ByteArrayInputStream

/**
 * Chromium WebView Client with Rust Adblock integration and crash-proof error recovery.
 */
class HeliumWebViewClient(
    private val context: Context,
    private val onPageStarted: (url: String) -> Unit,
    private val onPageFinished: (url: String) -> Unit,
    private val onError: (failingUrl: String, description: String) -> Unit,
    private val onBlockedResource: (count: Int) -> Unit
) : WebViewClient() {

    companion object {
        private const val TAG = "HeliumWebViewClient"
    }

    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
        val url = request?.url?.toString() ?: return null

        try {
            val shouldBlock = runBlocking { RustAdBlocker.shouldBlock(url) }
            if (shouldBlock) {
                val count = RustAdBlocker.getBlockedCount()
                onBlockedResource(count)
                // Return empty response to block ads, scripts, and trackers
                return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Adblock intercept error", t)
        }

        return super.shouldInterceptRequest(view, request)
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val uri = request?.url ?: return false
        val scheme = uri.scheme?.lowercase() ?: ""

        // Keep standard web schemes safely inside the WebView
        if (scheme == "http" || scheme == "https" || scheme == "about" || scheme == "javascript") {
            return false // Let WebView load it
        }

        // For non-web schemes (intent://, tel:, etc.), handle safely without crashing
        return try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (t: Throwable) {
            Log.w(TAG, "Cannot handle external scheme: $scheme", t)
            true
        }
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        if (!url.isNullOrEmpty()) {
            onPageStarted(url)
        }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        if (!url.isNullOrEmpty()) {
            onPageFinished(url)
            // Inject Rust cosmetic ad-hiding CSS
            try {
                val host = Uri.parse(url).host ?: ""
                if (host.isNotEmpty()) {
                    val cosmeticCss = runBlocking { RustAdBlocker.getCosmeticScriptForDomain(host) }
                    if (cosmeticCss.isNotEmpty()) {
                        val cleanCss = cosmeticCss.replace("'", "\\'")
                        view?.evaluateJavascript(
                            "(function(){var s=document.createElement('style');s.innerHTML='$cleanCss';document.head.appendChild(s);})();",
                            null
                        )
                    }
                }
            } catch (t: Throwable) {
                // ignore
            }
        }
    }

    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
        super.onReceivedError(view, request, error)
        if (request?.isForMainFrame == true) {
            val failingUrl = request.url?.toString() ?: ""
            val desc = error?.description?.toString() ?: "Connection error"
            onError(failingUrl, desc)
        }
    }

    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
        Log.e(TAG, "WebView render process crashed: didCrash=${detail?.didCrash()}")
        onError(view?.url ?: "", "Render process crashed. Tap Retry to reload.")
        return true // Handled safely without killing the entire app!
    }
}
