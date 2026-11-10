package org.helium.tv.browser.engine

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.webkit.RenderProcessGoneDetail
import android.webkit.SafeBrowsingResponse
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.runBlocking
import org.helium.tv.browser.adblock.RustAdBlocker
import org.helium.tv.browser.extensions.ExtensionManager
import org.helium.tv.browser.security.RenderiumSecurityManager
import java.io.ByteArrayInputStream
import java.util.regex.Pattern

/**
 * Chromium WebView Client with Renderium World-Class Security, Rust Adblock,
 * SafeBrowsing malware/phishing interception, Chrome Web Store extension injection,
 * and crash-proof error recovery.
 */
class HeliumWebViewClient(
    private val context: Context,
    private val extensionManager: ExtensionManager,
    private val onPageStarted: (url: String) -> Unit,
    private val onPageFinished: (url: String) -> Unit,
    private val onError: (failingUrl: String, description: String) -> Unit,
    private val onBlockedResource: (count: Int) -> Unit,
    private val onExtensionPrompt: (id: String, name: String) -> Unit
) : WebViewClient() {

    companion object {
        private const val TAG = "RenderiumWebViewClient"
        private val STORE_DETAIL_PATTERN = Pattern.compile(
            "https?://(chromewebstore|chrome)\\.google\\.com/(webstore/)?detail/([^/?#]+)/([a-p]{32})",
            Pattern.CASE_INSENSITIVE
        )
    }

    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
        val url = request?.url?.toString() ?: return null

        try {
            val shouldBlock = runBlocking { RustAdBlocker.shouldBlock(url) }
            if (shouldBlock) {
                val count = RustAdBlocker.getBlockedCount()
                onBlockedResource(count)
                return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Adblock intercept error", t)
        }

        return super.shouldInterceptRequest(view, request)
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val uri = request?.url ?: return false
        val urlStr = uri.toString()
        val scheme = uri.scheme?.lowercase() ?: ""

        // Check if URL is an extension CRX download or installation trigger
        if (urlStr.endsWith(".crx", ignoreCase = true) || urlStr.contains("clients2.google.com/service/update2/crx", ignoreCase = true)) {
            val extId = uri.getQueryParameter("x")?.substringAfter("id%3D")?.substringBefore("%26")
                ?: uri.getQueryParameter("id")
                ?: urlStr.substringAfterLast("/").substringBefore(".crx")
            if (extId.length == 32) {
                onExtensionPrompt(extId, "Chrome Extension")
                return true
            }
        }

        // HTTPS Everywhere: Automatically upgrade plain http to https
        if (scheme == "http") {
            val upgraded = RenderiumSecurityManager.upgradeToHttps(urlStr)
            if (upgraded != urlStr) {
                view?.loadUrl(upgraded)
                return true
            }
        }

        // Keep standard web schemes safely inside the WebView
        if (scheme == "http" || scheme == "https" || scheme == "about" || scheme == "javascript") {
            return false
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
        if (url.isNullOrEmpty()) return

        onPageFinished(url)

        // 1. Inject Rust cosmetic ad-hiding CSS
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

        // 2. Inject Installed Chrome Extensions Content Scripts
        try {
            val scripts = extensionManager.getScriptsToInject(url)
            for (script in scripts) {
                view?.evaluateJavascript(script, null)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Error executing extension content scripts", t)
        }

        // 3. Detect Chrome Web Store Extension Details Page & Inject Liquid Glass Add Button
        try {
            val matcher = STORE_DETAIL_PATTERN.matcher(url)
            if (matcher.find()) {
                val slug = matcher.group(3) ?: "Extension"
                val extId = matcher.group(4) ?: ""
                val cleanName = slug.split("-").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

                val injectScript = """
                    (function() {
                        if (document.getElementById('renderium-ext-banner')) return;
                        var banner = document.createElement('div');
                        banner.id = 'renderium-ext-banner';
                        banner.style.position = 'fixed';
                        banner.style.bottom = '70px';
                        banner.style.right = '20px';
                        banner.style.zIndex = '99999999';
                        banner.style.background = 'rgba(11, 16, 29, 0.95)';
                        banner.style.border = '1.5px solid rgba(56, 189, 248, 0.5)';
                        banner.style.borderRadius = '28dp';
                        banner.style.padding = '12px 20px';
                        banner.style.boxShadow = '0 8px 32px rgba(0, 0, 0, 0.7)';
                        banner.style.cursor = 'pointer';
                        banner.style.display = 'flex';
                        banner.style.alignItems = 'center';
                        banner.style.color = '#FFFFFF';
                        banner.style.fontFamily = '-apple-system, BlinkMacSystemFont, sans-serif';
                        banner.style.fontSize = '14px';
                        banner.style.fontWeight = 'bold';
                        banner.innerHTML = '🧩 Add to Renderium';
                        banner.onclick = function() {
                            if (window.HeliumExtensionBridge) {
                                window.HeliumExtensionBridge.requestInstall('$extId', '$cleanName');
                            }
                        };
                        document.body.appendChild(banner);

                        // Hook into any page Add to Chrome buttons
                        var buttons = document.querySelectorAll('button');
                        for (var i = 0; i < buttons.length; i++) {
                            var text = buttons[i].innerText || '';
                            if (text.includes('Add to Chrome') || text.includes('Add to Brave') || text.includes('Get')) {
                                buttons[i].onclick = function(e) {
                                    e.preventDefault();
                                    e.stopPropagation();
                                    if (window.HeliumExtensionBridge) {
                                        window.HeliumExtensionBridge.requestInstall('$extId', '$cleanName');
                                    }
                                };
                            }
                        }
                    })();
                """.trimIndent()

                view?.evaluateJavascript(injectScript, null)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Error injecting Chrome Web Store banner", t)
        }
    }

    override fun onSafeBrowsingHit(view: WebView?, request: WebResourceRequest?, threatType: Int, callback: SafeBrowsingResponse?) {
        val failingUrl = request?.url?.toString() ?: ""
        Log.w(TAG, "Safe Browsing Threat intercepted ($threatType) on: $failingUrl")
        callback?.backToSafety(true)
        onError(failingUrl, "🛡️ Renderium Security blocked malicious/phishing threat.")
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
        return true
    }
}
