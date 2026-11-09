package org.helium.tv.browser.engine

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession

class HeliumNavigationDelegate(
    private val context: Context,
    private val onPageStarted: (url: String) -> Unit,
    private val onPageFinished: (url: String) -> Unit,
    private val onTitleChanged: (title: String) -> Unit,
    private val onProgressChanged: (progress: Int) -> Unit
) : GeckoSession.Navigation.Delegate, GeckoSession.ProgressDelegate {

    companion object {
        private const val TAG = "HeliumNavigation"
    }

    override fun onLocationChange(session: GeckoSession, url: String?, perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>) {
        if (url != null) {
            Log.d(TAG, "Location changed: $url")
            onPageStarted(url)
        }
    }

    override fun onLoadError(session: GeckoSession, uri: String?, error: GeckoSession.Navigation.LoadError): GeckoResult<String>? {
        Log.e(TAG, "Page load error for $uri: code=${error.code}, category=${error.category}")
        return null
    }

    override fun onLoadRequest(session: GeckoSession, request: GeckoSession.Navigation.LoadRequest): GeckoResult<Boolean>? {
        val uriStr = request.uri
        val uri = Uri.parse(uriStr)
        val scheme = uri.scheme?.lowercase()

        // Handle standard web schemes
        if (scheme == "http" || scheme == "https" || scheme == "about" || scheme == "javascript") {
            return GeckoResult.fromValue(false) // Let GeckoView handle it
        }

        // Handle external schemes (intent://, market://, etc.)
        try {
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return GeckoResult.fromValue(true) // Handled externally
        } catch (e: Exception) {
            Log.w(TAG, "Could not open external URI: $uriStr", e)
        }

        return GeckoResult.fromValue(false)
    }

    override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
        // Can be used to toggle UI back button state
    }

    override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) {
        // Can be used to toggle UI forward button state
    }

    // ProgressDelegate methods
    override fun onPageStart(session: GeckoSession, url: String) {
        onPageStarted(url)
    }

    override fun onPageStop(session: GeckoSession, success: Boolean) {
        session.navigationDelegate?.let {
            // Document completed
        }
    }

    override fun onProgressChange(session: GeckoSession, progress: Int) {
        onProgressChanged(progress)
    }

    override fun onSecurityChange(session: GeckoSession, securityInfo: GeckoSession.ProgressDelegate.SecurityInformation) {
        // Security state (HTTPS, cert valid)
    }
}
